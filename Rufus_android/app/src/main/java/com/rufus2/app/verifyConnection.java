package com.rufus2.app;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.Log;
import android.os.Handler;

/**
 * Created by jeffreymoskowitz on 8/24/16.
 */
public class verifyConnection {

    // Because Bluetooth doesn't notify you when it's gone out of range
    // I wanted a way to check that the connection is still active.
    // This class uses a countdown timer to make sure there's a response
    // from the bluetooth device within an appropriate amount of time.

    public Handler verifyHandler;

    String tag = "VerifyDebugging";

    protected static final int TIMER_UP = 4;

    static Context verifyContext;

    public void verify(final Context context, Handler handler) {
            Log.i(tag, "entered countdown");

            verifyContext = context;
            verifyHandler = handler;
            new CountDownTimer(1000, 1000) {
                @Override
                public void onFinish() {
                    verifyHandler.obtainMessage(TIMER_UP)
                            .sendToTarget();
                }
                @Override
                public void onTick(long millisUntilFinished) {
                    //CountDownTimer requires this
                }
            }.start();
    }

}
