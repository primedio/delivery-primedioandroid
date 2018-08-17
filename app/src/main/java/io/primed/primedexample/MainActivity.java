package io.primed.primedexample;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;

import java.util.HashMap;
import java.util.Map;

import io.primed.primedandroid.Primed;
import io.primed.primedandroid.PrimedTracker;


public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //get permission to get the deviceID:
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            String[] mPermissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS};
            requestPermissions(mPermissions, 1);
        } else {
            initTrackers();
        }

        //Add our button listeners
        Button button = (Button) findViewById(R.id.personalizeButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Map<String, Object> signals = new HashMap<String, Object>();

                signals.put("device", "android");
                signals.put("userid", "someuserid");

                //Personalize call, handle the response to personalize your data
                Primed.getInstance().personalize("frontpage.recommendations", signals, 3, "A", new Primed.PrimedCallback() {
                    @Override
                    public void onSuccess(String response) {

                    }

                    @Override
                    public void onFailure() {

                    }
                });
            }
        });

        Button button2 = (Button) findViewById(R.id.convertButton);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //Convert call with callback
                Primed.getInstance().convert("RUUID_GO_HERE",  new HashMap<String, Object>(), new Primed.PrimedCallback() {
                    @Override
                    public void onSuccess(String response) {

                    }

                    @Override
                    public void onFailure() {

                    }
                });

            }
        });

        Button button3 = (Button) findViewById(R.id.trackView);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Click example
                PrimedTracker.ClickEvent event = PrimedTracker.getInstance().new ClickEvent();
                event.x = 1;
                event.y = 1;
                event.interactionType = PrimedTracker.InteractionType.LEFT;
                PrimedTracker.getInstance().trackEvent(event);

                //Scroll example
                PrimedTracker.ScrollEvent scrollEvent = PrimedTracker.getInstance().new ScrollEvent();
                scrollEvent.scrollDirection = PrimedTracker.ScrollDirection.DOWN;
                PrimedTracker.getInstance().trackEvent(scrollEvent);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.READ_PHONE_STATE)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        initTrackers();
                        break;
                    } else {
                        //no permissions granted, present error?
                    }
                }
            }

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == 0) {
            initTrackers();
        }
    }

    private void initTrackers() {
        //We we've granted permission:
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            TelephonyManager telephonyManager;
            telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

            //You need to pass the unique device id to the tracker:
            String deviceId = telephonyManager.getImei();

            //To get only a Primed instance for personalize and convert:
            //primed = new Primed("mypubkey", "mysecretkey", "https://gw.staging.primed.io");
            PrimedTracker.getInstance().init("mypubkey", "mysecretkey", "https://gw.staging.primed.io:443", "http://18.191.69.104:5001/v1", 30, deviceId);
        } else {
            PrimedTracker.getInstance().init("mypubkey", "mysecretkey", "https://gw.staging.primed.io:443", "http://18.191.69.104:5001/v1", 30, "no_device_id");
        }
    }
}
