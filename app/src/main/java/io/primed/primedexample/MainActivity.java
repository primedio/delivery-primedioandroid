package io.primed.primedexample;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;

import java.security.Permission;
import java.util.HashMap;
import java.util.Map;

import io.primed.primedandroid.Primed;
import io.primed.primedandroid.PrimedTracker;
import io.primed.primedioexample.R;


public class MainActivity extends AppCompatActivity {

    private Primed primed;
    private PrimedTracker primedTracker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
//            new AlertDialog.Builder(this).setTitle("permission_title").setMessage("permission_title").setPo
            new AlertDialog.Builder(this).setTitle("permission_title").setMessage("permission_title").setPositiveButton("ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String[] mPermissions = new String[]{Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS};
                    requestPermissions(mPermissions, 1);
                }
            }).setCancelable(false).show();
            return;
        } else {
            initTrackers();
        }

        Button button = (Button) findViewById(R.id.personalizeButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Map<String, Object> signals = new HashMap<String, Object>();

                signals.put("device", "android");
                signals.put("userid", "someuserid");

                primed.personalize("frontpage.recommendations", signals, 3, "A");
            }
        });

        Button button2 = (Button) findViewById(R.id.convertButton);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                primed.convert("RUUID");
                primed.convert("RUUID_GO_HERE",  new HashMap<String, Object>());

            }
        });

        Button button3 = (Button) findViewById(R.id.trackView);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                PrimedTracker.ViewEvent event = primedTracker.new ViewEvent();
//                event.uri = "http://www.testapp.com";
//                event.customProperties = "customProp";
//                primedTracker.trackEvent(event);

                PrimedTracker.HeartbeatEvent beat = primedTracker.new HeartbeatEvent();
                primedTracker.trackEvent(beat);

//                PrimedTracker.ScrollEvent scrollEvent = primedTracker.new ScrollEvent();
//                scrollEvent.scrollDirection = PrimedTracker.ScrollDirection.DOWN;
//                primedTracker.trackEvent(scrollEvent);
            }
        });



    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == 0) {
            initTrackers();
        }
    }

    private void initTrackers() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            TelephonyManager telephonyManager;
            telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

            String deviceId = telephonyManager.getImei();
            primed = new Primed("mypubkey", "mysecretkey", "https://gw.staging.primed.io");
            primedTracker = new PrimedTracker("mypubkey", "mysecretkey", "https://gw.staging.primed.io:443", "http://18.191.69.104:5001/v1", 10, deviceId);
        }
    }
}
