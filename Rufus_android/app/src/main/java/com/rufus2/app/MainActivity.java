package com.rufus2.app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import android.speech.RecognizerIntent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends ActionBarActivity {

    //rufus refers to the central image. Circle1 etc... are the ImageView buttons at the bottom of the app screen
    ImageView rufus, circle1, circle2, circle3, circle4;
    ImageView rufusBorder, circle1Border, circle2Border, circle3Border, circle4Border;
    BluetoothAdapter btAdapter;
    //In an earlier incarnation the app found all bluetooth devices and let you pick which one to connect to. This version has the correct device hardcoded in so there's no fumbling with bluetooth screens however I've kept the option in here in case I want to add more bluetooth devices in the future.
    Set<BluetoothDevice> devicesArray;
    ArrayList<String> pairedDevices;
    ArrayList<BluetoothDevice> devices;
    BluetoothDevice device;

    //Replace this with your device's UUID
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //Replace this with your device's MAC address
    public static final String BLUETOOTH_ID = "20:14:04:15:90:69";


    protected static final int SUCCESS_CONNECT = 0;
    protected static final int MESSAGE_READ = 1;
    protected static final int MESSAGE_WRITE = 2;
    protected static final int FAIL_CONNECT = 3;
    IntentFilter filter;
    BroadcastReceiver receiver;
    String tag = "debugging";
    //this variable is to keep track of when the app is connected to the device. It does this imperfectly though because I didn't want the app checking the bluetooth connection every minute to verify that it's still connected and wasting the battery. Instead it checks if it's still connected in onResume and whenever you use the buttons.
    private boolean status = false;
    //this variable keeps track of whether the last message sent has received a reply from the arduino
    private boolean responded = false;
    //check the arduinoResponse method for the use of this one
    private boolean response = false;
    //this will be used to make sure we're not running a bunch of unnecessary countdowns if you press multiple buttons in a short time span, more info in arduinoResponse
    private boolean countdownRunning = false;

    private boolean circle1Clicked = false;
    //circle2 aka outlet2 isn't used because the arduino is plugged into that outlet so it needs to be on all the time or nothing will work.
    //private boolean circle2Clicked = true;
    private boolean circle3Clicked = false;
    private boolean circle4Clicked = false;

    //this variable is just to differentiate between the first time onResume is called (after onCreate) and every other time.
    //the first time I want you to have to press the central button to connect, but whenever onResume is called after that (if you
    //open another app and then return to it for instance) I want the connection and outlet checking to happen automatically.
    private boolean shouldExecuteOnResume = false;

    public static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;
    public static final int BLUETOOTH_REQUEST_CODE = 2345;

    ConnectedThread connectedThread;
    Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            Log.i(tag, "in handler");
            super.handleMessage(msg);
            switch (msg.what) {
                case SUCCESS_CONNECT:
                    // DO something
                    Toast.makeText(getApplicationContext(), "All is very well and good sir", Toast.LENGTH_SHORT).show();
                    Log.i(tag, "connected");
                    status = true;
                    rufusBorder.setVisibility(View.VISIBLE);
                    Log.i("handler", "status is " + status);
                    connectedThread = new ConnectedThread((BluetoothSocket)msg.obj);
                    connectedThread.start();
                    checkOutlets();
                    break;
                case FAIL_CONNECT:
                    Toast.makeText(getApplicationContext(), "Could not connect to bluetooth device", Toast.LENGTH_LONG).show();
                    Log.i(tag, "failed to connect");
                    status = false;
                    rufusBorder.setVisibility(View.INVISIBLE);
                    break;
                case MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    String string = new String(readBuf);
                    responded = true;
                    status = true;
                    rufusBorder.setVisibility(View.VISIBLE);
                    if (string.contains("check")) {
                        Log.i(tag, "check response received - " + string);
                        if (string.contains("1")) {
                            circle1Border.setVisibility(View.VISIBLE);
                            circle1Clicked = true;
                        }
                        if (string.contains("3")) {
                            circle3Border.setVisibility(View.VISIBLE);
                            circle3Clicked = true;
                        }
                        if (string.contains("4")) {
                            circle4Border.setVisibility(View.VISIBLE);
                            circle4Clicked = true;
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), string, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    String str = new String(writeBuf);
                    Toast.makeText(getApplicationContext(), str, Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //initialize the variables
        init();

        //check if the bluetooth adapter has been enabled
        if(btAdapter == null){
            Toast.makeText(this, "No bluetooth detected", Toast.LENGTH_LONG).show();
            finish();
        } else {
            if(!btAdapter.isEnabled()){
                turnOnBT();
            }

            getPairedDevices();
            startDiscovery();

        }

    }

    private void startDiscovery() {
        // TODO Auto-generated method stub
        btAdapter.cancelDiscovery();
        btAdapter.startDiscovery();
    }

    //This handles all the button presses (image clicks technically)
    public void imageClick(View v) {
           switch (v.getId()) {
               case R.id.rufus:
                   Log.i(tag, "status is " + status);
                   if (!status) {
                            if (btAdapter.isDiscovering()) {
                                btAdapter.cancelDiscovery();
                                }
                            getPairedDevices();
                            if (pairedDevices.contains("HC-06")) {
                                    Log.i("paired devices", "Arduino Bluetooth Found");
                                    //note, the bluetooth arduino ID is the MAC address
                                    //when you are scanning bluetooth devices, the name comes up as "HC-06"
                                    //Since I'm only using this with one specific bluetooth module, I hardcoded it
                                    device = btAdapter.getRemoteDevice(BLUETOOTH_ID);
                                    ConnectThread connect = new ConnectThread(device);
                                    connect.start();
                                    Log.i(tag, "in click listener");
                                }
                         } else {
                       startVoiceRecognitionActivity();
                   }
                   break;
               case R.id.circle1:
                   turnOutletOn(1);
                   break;
               case R.id.circle2:
                   //do nothing because outlet 2 is permanently on
                   break;
               case R.id.circle3:
                   turnOutletOn(3);
                   break;
               case R.id.circle4:
                   turnOutletOn(4);
                   break;
           }


    }
    //Most of this comes straight from the android bluetooth documentation
    private class ConnectThread extends Thread {

        private final BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device) {
            //Use a temporary object that is later assigned to mmSocket because mmSocket is final
            BluetoothSocket tmp = null;
            Log.i(tag, "construct");
            //Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                //MY_UUID is the app's UUID string, also used by server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.i(tag, "get socket failed");

            }
            mmSocket = tmp;
        }

        public void run() {
            //Cancel discovery because it will slow down the connection
            btAdapter.cancelDiscovery();
            Log.i(tag, "connect - run");
            try {
                //Connect the device through the socket. This will block until it succeeds or throws an exception
                mmSocket.connect();
                Log.i(tag, "connect - succeeded");
            } catch (IOException connectException) {
                Log.i(tag, "connect failed");
                mHandler.obtainMessage(FAIL_CONNECT, mmSocket)
                        .sendToTarget();
                //Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                }
                return;
            }
            //Do work to manage the connection (in a separate thread)
            mHandler.obtainMessage(SUCCESS_CONNECT, mmSocket)
                    .sendToTarget();
        }
        //Will cancel an in-progress connection, and close the socket
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private void init() {
        // TODO Auto-generated method stub
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        pairedDevices = new ArrayList<String>();
        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        devices = new ArrayList<BluetoothDevice>();

        rufus = (ImageView) findViewById(R.id.rufus);
        rufusBorder = (ImageView) findViewById(R.id.rufusBorder);

        circle1 = (ImageView) findViewById(R.id.circle1);
        circle2 = (ImageView) findViewById(R.id.circle2);
        circle3 = (ImageView) findViewById(R.id.circle3);
        circle4 = (ImageView) findViewById(R.id.circle4);

        circle1Border = (ImageView) findViewById(R.id.circle1Border);
        circle2Border = (ImageView) findViewById(R.id.circle2Border);
        circle3Border = (ImageView) findViewById(R.id.circle3Border);
        circle4Border = (ImageView) findViewById(R.id.circle4Border);



        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // TODO Auto-generated method stub
                String action = intent.getAction();

                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    devices.add(device);
                    if (!pairedDevices.contains(device)) {
                        getPairedDevices();
                    }

                } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    //let it do its discovery
                    //if you have a problem, with the discovery you may want to do some debugging here
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    //ditto here
                    //if it finishes its discovery but doesn't change its state, debugging may be required
                } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                    if (btAdapter.getState() == btAdapter.STATE_OFF) {
                        turnOnBT();
                    }
                }

            }
        };

        registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(receiver, filter);
    }

    //Most of this comes straight from the android bluetooth documentation
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer;  // buffer store for the stream, without this you will receive the message one character at the time and feel like an idiot (I speak from experience)
            int bytes; // bytes returned from read()
            ArrayList<Integer> arr_byte = new ArrayList<Integer>();

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read();
                    if (bytes == 0x0A) {

                    } else if (bytes == 0x0D) {
                        buffer = new byte[arr_byte.size()];
                        for (int i = 0; i < arr_byte.size(); i++) {
                            buffer[i] = arr_byte.get(i).byteValue();
                        }
                        // Send the obtained bytes to the UI activity
                        mHandler.obtainMessage(MESSAGE_READ, buffer.length, -1, buffer)
                                .sendToTarget();
                        arr_byte = new ArrayList<Integer>();
                    } else {
                        arr_byte.add(bytes);
                    }

                } catch (IOException e) {
                    break;
                }
            }
        }

        // Call this from the main activity to send data to the remote device
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
            }


        }

        // Call this from the main activity to shutdown the connection
        public void cancel() {
            try {
                mmSocket.close();
                status = false;
                showDisconnected();
            } catch (IOException e) {
            }
        }
    }

    private void turnOnBT() {
            // TODO Auto-generated method stub
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, BLUETOOTH_REQUEST_CODE);
    }

    private void getPairedDevices() {
            // TODO Auto-generated method stub
            devicesArray = btAdapter.getBondedDevices();
            if (devicesArray.size() > 0) {
                for (BluetoothDevice device : devicesArray) {
                    pairedDevices.add(device.getName());

                }
            }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
            // TODO Auto-generated method stub
            super.onActivityResult(requestCode, resultCode, data);
            if (resultCode == RESULT_CANCELED && requestCode == BLUETOOTH_REQUEST_CODE) {
                Toast.makeText(getApplicationContext(), "Bluetooth must be enabled to continue", Toast.LENGTH_SHORT).show();
                finish();
            } else if (resultCode == RESULT_OK && requestCode == VOICE_RECOGNITION_REQUEST_CODE) {
                ArrayList matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                if (matches.contains("turn one on")) {
                    if (status) {
                        if (!circle1Clicked) {
                            String s = "1";
                            connectedThread.write(s.getBytes());
                            circle1Border.setVisibility(View.VISIBLE);
                            circle1Clicked = true;
                        }
                    }
                } else if (matches.contains("turn one off")) {
                    if (status) {
                        if (circle1Clicked) {
                            String s = "2";
                            connectedThread.write(s.getBytes());
                            circle1Border.setVisibility(View.INVISIBLE);
                            circle1Clicked = false;
                        }
                    }
                //Again, I've skipped two because that outlet has to be on all the time to power the arduino
                } else if (matches.contains("turn three on")) {
                    if (status) {
                        if (!circle3Clicked) {
                            String s = "5";
                            connectedThread.write(s.getBytes());
                            circle3Border.setVisibility(View.VISIBLE);
                            circle3Clicked = true;
                        }
                    }
                } else if (matches.contains("turn three off")) {
                    if (status) {
                        if (circle3Clicked) {
                            String s = "6";
                            connectedThread.write(s.getBytes());
                            circle3Border.setVisibility(View.INVISIBLE);
                            circle3Clicked = false;
                        }
                    }
                } else if (matches.contains("turn four on")) {
                    if (status) {
                        if (!circle4Clicked) {
                            String s = "7";
                            connectedThread.write(s.getBytes());
                            circle4Border.setVisibility(View.VISIBLE);
                            circle4Clicked = true;
                        }
                    }
                } else if (matches.contains("turn four off")) {
                    if (status) {
                        if (circle4Clicked) {
                            String s = "8";
                            connectedThread.write(s.getBytes());
                            circle4Border.setVisibility(View.INVISIBLE);
                            circle4Clicked = false;
                        }
                    }
                }
            }
    }

    private void turnOutletOn(int i) {
        if (status) {
            switch (i) {
                case 1:
                    if (status) {
                        if (!circle1Clicked) {
                            String s = "1";
                            connectedThread.write(s.getBytes());
                            circle1Border.setVisibility(View.VISIBLE);
                            circle1Clicked = true;
                        } else {
                            String s = "2";
                            connectedThread.write(s.getBytes());
                            circle1Border.setVisibility(View.INVISIBLE);
                            circle1Clicked = false;
                        }
                    }
                    break;
                case 2:
                    //left empty because outlet two is always on
                    break;
                case 3:
                    if (status) {
                        if (!circle3Clicked) {
                            String s = "5";
                            connectedThread.write(s.getBytes());
                            circle3Border.setVisibility(View.VISIBLE);
                            circle3Clicked = true;
                        } else {
                            String s = "6";
                            connectedThread.write(s.getBytes());
                            circle3Border.setVisibility(View.INVISIBLE);
                            circle3Clicked = false;
                        }
                    }
                    break;
                case 4:
                    if (status) {
                        if (!circle4Clicked) {
                            String s = "7";
                            connectedThread.write(s.getBytes());
                            circle4Border.setVisibility(View.VISIBLE);
                            circle4Clicked = true;
                        } else {
                            String s = "8";
                            connectedThread.write(s.getBytes());
                            circle4Border.setVisibility(View.INVISIBLE);
                            circle4Clicked = false;
                        }
                    }
                    break;
            }

            if (!arduinoResponded()) {
                status = false;
            }
        } else {
            status = false;
            showDisconnected();
        }
    }

    //This will be called if the app realizes the connection is no longer working, for instance if you walk out of range and then try to use it
    private void showDisconnected() {
        rufusBorder.setVisibility(View.INVISIBLE);
        circle1Border.setVisibility(View.INVISIBLE);
        circle3Border.setVisibility(View.INVISIBLE);
        circle4Border.setVisibility(View.INVISIBLE);
    }

    // After you used the central image to connect to the bluetooth device, it is now free so we can use it for voice recognition
    private void startVoiceRecognitionActivity() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
                "Speech recognition demo");
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    }

    //this checks which outlets are currently turned on and in doing so, necessarily checks the connection as well
    private void checkOutlets() {
        //I'm sending a 9 to the arduino instead of a char like 'c' simply because I already had the arduino coded for ints
        String s = "9";
        connectedThread.write(s.getBytes());
        if (arduinoResponded()) {
            //the arduino did respond but we reset response variable so it can be used again, status is already set to true in MESSAGE_READ above so no need to change it here
            //if it doesn't respond then the work of showing that it is disconnected and changing the status will be done in the arduinoResponded method
            response = false;
        }
    }

    //this is a little countdown method that will verify the response message from the arduino has arrived and will be called anytime I write a message to it that needs a response
    //this is needed because when you go out of range of the bluetooth device, your phone doesn't know that it's out of range. It will continue letting you send messages, unaware that they're not being received.
    //To avoid that, this sets a timer to make sure a response message is received from the device once a message is sent so the app can make sure it's still connected.
    //If it's no longer connected, you'll know because once the timer runs out, none of the buttons (including the main central one that keeps track of your connection status) will be lit.
    private boolean arduinoResponded() {
        if (!countdownRunning) {
            countdownRunning = true;
            Log.i(tag, "entered countdown");
            new CountDownTimer(4000, 1000) {
                @Override
                public void onFinish() {
                    if (responded) {
                        response = true;
                        //resetting responded variable to false here so it can be used again
                        responded = false;
                        countdownRunning = false;
                        Log.i(tag, "response received in time");
                    } else {
                        response = false;
                        status = false;
                        showDisconnected();
                        countdownRunning = false;
                        Log.i(tag, "didn't get a response in time");
                    }
                }

                @Override
                public void onTick(long millisUntilFinished) {
                    //CountDownTimer requires this
                }
            }.start();
            return response;

        }
        //return true if another countdown is already running because a true response won't have any effect, only a false response will have an effect (the app will conclude its out of range or otherwise disconnected from the bluetooth device)
        //this way multiple unnecessary countdowns are prevented
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        // Unregister the receiver when the app isn't being used
        super.onPause();
        try {
            unregisterReceiver(receiver);
        } catch (Exception e) {
            Log.i("Exception", e.getMessage());
        }
        shouldExecuteOnResume = true;
    }

    //this onResume method is important because if you open the app and use it but then walk out of the range of the bluetooth device, then the next time you try to open it up, without this method the app will think it's still connected and won't understand why it's not working
    //this verifies that it's connected, makes sure the outlet imageviews are lit correctly and if it's not connected, manages the connection
    @Override
    protected void onResume() {
        super.onResume();

        //I did it this way to avoid this code being executed along with onCreate() as it normally does in the lifecycle
        //clearly it won't be able to checkOutlets until everything's been set up
        if (shouldExecuteOnResume) {
            checkOutlets();

            if (!status) {
                showDisconnected();

                //re-register the receiver after we unregistered it in onPause()
                registerReceiver(receiver, filter);
                filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
                registerReceiver(receiver, filter);
                filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
                registerReceiver(receiver, filter);
                filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                registerReceiver(receiver, filter);

                //make sure everything's cool with the bluetooth adapter
                if (btAdapter.getState() != BluetoothAdapter.STATE_ON) {
                    if (btAdapter.isDiscovering()) {
                        btAdapter.cancelDiscovery();
                    }
                    turnOnBT();
                }

                if (!pairedDevices.contains("HC-06")) {
                    getPairedDevices();
                    startDiscovery();
                }

                device = btAdapter.getRemoteDevice(BLUETOOTH_ID);
                ConnectThread connect = new ConnectThread(device);
                connect.start();
                Log.i(tag, "in click listener");

                checkOutlets();
            }
        }

    }

}
