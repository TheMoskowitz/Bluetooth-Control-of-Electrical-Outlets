
// D11 > Rx
// D10 > Tx


#include <SoftwareSerial.h>

SoftwareSerial Genotronex(10, 11); // RX, TX

// You can see here which pins everything is connected to
const int ledpin=13;
const int outlet1=4;
const int outlet3=6;
const int outlet4=7;
// Variable to hold the numbers being received from the app
int BluetoothData;
// Some variables to record the status of the outlets
boolean one = false;
boolean three = false;
boolean four = false;

void setup() {
  Serial.begin(9600);
  Genotronex.begin(9600);
  Genotronex.println("Arduino Bluetooth On");
  pinMode(ledpin,OUTPUT);
  pinMode(outlet1,OUTPUT);
  pinMode(outlet3,OUTPUT);
  pinMode(outlet4,OUTPUT);
}

void loop() {
 if (Genotronex.available()){
    BluetoothData=Genotronex.read();
  if (BluetoothData=='1'){   // When a '1' is received
    digitalWrite(outlet1,1);
    one = true;
    Genotronex.println("Outlet 1 On!");
  } else if (BluetoothData=='2'){   // When a '2' is received
    digitalWrite(outlet1,0);
    one = false;
    Genotronex.println("Outlet 1 Off!");
  } else if (BluetoothData=='5'){   // When a '5' is received
    digitalWrite(outlet3,1);
    three = true;
    Genotronex.println("Outlet 3 On!");
  } else if (BluetoothData=='6'){   // When a '6' is received
    digitalWrite(outlet3,0);
    three = false;
    Genotronex.println("Outlet 3 is Off!");
  } else if (BluetoothData=='7'){   // When a '7' is received
    digitalWrite(outlet4,1);
    four = true;
    Genotronex.println("Outlet 4 On!");
  } else if (BluetoothData=='8'){   // When an '8' is received
    digitalWrite(outlet4,0);
    four = false;
    Genotronex.println("Outlet 4 is Off!");
  } else if (BluetoothData=='9'){   // When a '9' is received
    Genotronex.println(check());
  }
}
// This delay is to prepare for the next incoming data.
// It could be lowered a bit if you don't like the slow response time.
// But I prefer to keep it like this because I don't want to flip the outlet switches too quickly.
delay(100);
}

// This checks the current state of the outlets and 
// puts together a string containing that information.
// The string is then sent to the android app.
String check() {
    String s = "check";
    if (one) {
      s += 1;
    }
    if (three) {
      s += 3;
    }
    if (four) {
      s += 4;
    }
    return s;
}

