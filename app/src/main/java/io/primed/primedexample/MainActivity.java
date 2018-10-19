package io.primed.primedexample;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import io.primed.primedandroid.Primed;
import io.primed.primedandroid.PrimedTracker;


public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Init Primed trackers
        initTrackers();

        //Add our button listeners
        Button button = (Button) findViewById(R.id.personalizeButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Map<String, Object> signals = new HashMap<String, Object>();

                signals.put("device", "android");
                signals.put("userid", "someuserid");

                //Personalise call, handle the response to personalise your data
                Primed.getInstance().personalise("frontpage.recommendations", signals, 3, "A", new Primed.PrimedCallback() {
                    @Override
                    public void onSuccess(JSONObject responseObject) {

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
                Map<String, Object> data = new HashMap<String, Object>();

                data.put("device", "android");
                data.put("userid", "someuserid");
                //Convert call with callback
                Primed.getInstance().convert("RUUID_GO_HERE", data);

            }
        });

        Button button3 = (Button) findViewById(R.id.trackView);
        button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                PrimedTracker.StartEvent e = PrimedTracker.getInstance().new StartEvent();
                e.uri = "http://test";
                PrimedTracker.getInstance().trackEvent(e);

                //Click example
                PrimedTracker.ClickEvent event = PrimedTracker.getInstance().new ClickEvent();
                event.x = 1;
                event.y = 1;
                event.interactionType = PrimedTracker.InteractionType.LEFT;
                PrimedTracker.getInstance().trackEvent(event);

                //Scroll example
                PrimedTracker.ScrollEvent scrollEvent = PrimedTracker.getInstance().new ScrollEvent();
                scrollEvent.scrollDirection = PrimedTracker.ScrollDirection.DOWN;
                scrollEvent.distance = 12; // pixels
                PrimedTracker.getInstance().trackEvent(scrollEvent);
            }
        });

        Button button4 = (Button) findViewById(R.id.button4);
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Health example, 200 = ok
                Primed.getInstance().health(new Primed.PrimedCallback() {
                    @Override
                    public void onSuccess(JSONObject responseObject) {

                    }

                    @Override
                    public void onFailure() {

                    }
                });
            }
        });
    }

    private void initTrackers() {

        //To get only a Primed instance for personalize and convert:
        Primed.getInstance().init("mypubkey", "mysecretkey", "https://gw.staging.primed.io:443");
        PrimedTracker.getInstance().init("mypubkey", "mysecretkey", "https://gw.staging.primed.io:443", MainActivity.this,"https://collector.staging.primed.io", 30);
        String did = PrimedTracker.getInstance().getDid();
        String sid = PrimedTracker.getInstance().getSid();

    }
}
