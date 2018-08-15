package io.primed.primedioexample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import java.util.HashMap;
import java.util.Map;

import io.primed.primedioandroid.*;


public class MainActivity extends AppCompatActivity {

    private Primed primed;
    private PrimedTracker primedTracker;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        primed = new Primed("mypubkey", "mysecretkey", "https://gw.staging.primed.io");
        primedTracker = new PrimedTracker("mypubkey", "mysecretkey", "https://gw.staging.primed.io:443", "http://18.191.69.104:5001/v1", 10);

        Button button = (Button) findViewById(R.id.personalizeButton);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Map<String, String> signals = new HashMap<String, String>();

                signals.put("device", "android");
                signals.put("userid", "someuserid");

                primed.personalize("frontpage.article.bottom", signals, 3);
            }
        });

        Button button2 = (Button) findViewById(R.id.convertButton);
        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                primed.convert("RUUID_GO_HERE",null);

            }
        });


    }
}
