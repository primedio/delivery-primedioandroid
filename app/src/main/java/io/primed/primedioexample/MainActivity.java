package io.primed.primedioexample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.util.HashMap;
import java.util.Map;

import io.primed.primedioandroid.PrimedIO;


public class MainActivity extends AppCompatActivity {

    private PrimedIO primed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        primed = new PrimedIO("client", "secret", "url");

        Map<String, String> signals = new HashMap<String, String>();

        signals.put("device", "android");
        signals.put("userid", "someuserid");

        primed.personalize("frontpage.article.bottom", signals, 3);

    }
}
