package io.primed.primedexample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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
                Primed.getInstance().personalise("frontpage.recommendations", signals, 3, "A", new Primed.PersonaliseCallback() {
                    @Override
                    public void onSuccess(Primed.ResultSet resultSet) {
                        Log.i("MainActivity", resultSet.guuid);

                        for (Primed.Result res : resultSet.results) {
                            Log.i("MainActivity", res.toString());
                        }
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.e("MainActivity", throwable.toString());
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

                //CustomEvent example
                PrimedTracker.CustomEvent customEvent = PrimedTracker.getInstance().new CustomEvent();
                customEvent.eventType = "mycustomevent";

                //Populate customProperties
                Map<String, Object> props = new HashMap<>();
                props.put("itemId", "abc123");

                customEvent.customProperties = props;
                PrimedTracker.getInstance().trackEvent(customEvent);
            }
        });

        Button button4 = (Button) findViewById(R.id.button4);
        button4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Health example, 200 = ok
                Primed.getInstance().health(new Primed.HealthCallback() {
                    @Override
                    public void onSuccess() {

                    }

                    @Override
                    public void onFailure(Throwable throwable) {

                    }
                });
            }
        });
    }

    private void initTrackers() {


        try {
            Properties properties = new Properties();
            InputStream inputStream = getAssets().open("local.properties");
            properties.load(inputStream);
            inputStream.close();

            String publicKey = properties.getProperty("publicKey");
            String secretKey = properties.getProperty("secretKey");
            String gwURL = properties.getProperty("gwURL");
            String collectorURL = properties.getProperty("collectorURL");

            System.out.println(publicKey + ", " + secretKey + ", " + gwURL + ", " + collectorURL);

//            To get only a Primed instance for personalize and convert:
//            Primed.getInstance().init(publicKey, secretKey, gwURL);


            PrimedTracker.getInstance().init(publicKey, secretKey, gwURL, MainActivity.this,collectorURL, 10);
            String did = PrimedTracker.getInstance().getDid();
            String sid = PrimedTracker.getInstance().getSid();
        } catch (IOException e) {
            e.printStackTrace();
        }



    }
}
