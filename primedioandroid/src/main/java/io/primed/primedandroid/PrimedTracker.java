package io.primed.primedandroid;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;
import com.github.nkzawa.socketio.client.IO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

final public class PrimedTracker {

    private String public_key;
    private String nonce;
    private String trackingConnectionString;
    private String connectionString;
    private String sha512_signature;

    public PrimedTracker(String publicKey, String secretKey, String connectionString, String trackingConnectionString, int heartbeatInterval) {
        String nonce = String.valueOf(new Date().getTime());

        String prepSignature = publicKey + secretKey + nonce;
        String signature = Primed.createSHA512(prepSignature);

        this.public_key = publicKey;
        this.sha512_signature = signature;
        this.nonce = nonce;
        this.trackingConnectionString = trackingConnectionString;
        this.connectionString = connectionString;

        try {
            mSocket = IO.socket(this.trackingConnectionString);
            //mSocket.on("new message", this.onNewMessage);
            mSocket.connect();
        } catch (URISyntaxException e) {

        }
    }


    private Socket mSocket;

    public void sendEvent(BaseEvent event) {
        String message = event.toString();

        mSocket.emit(event.eventName, event.toString());
    }

    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            JSONObject data = (JSONObject) args[0];
            String username;
            String message;
            try {
                username = data.getString("username");
                message = data.getString("message");
            } catch (JSONException e) {
                return;
            }
        }
    };

    public class BaseEvent {
        public String eventName = "";
        String apiKey = public_key;
        String ts = String.valueOf(System.currentTimeMillis() / 1000l);
        String sid;
        String did;
        String source = "app";
        String sdkVersion = "1.0";
        String type;

        public String toString() {
            return this.toJSONString(this.toMap());
        }

        public Map<String, Object> toMap() {
            Map<String, Object> params = new HashMap<String, Object>();
            params.put("apiKey", this.apiKey);
            params.put("ts", this.ts);
            params.put("source", this.source);
            params.put("sdkVersion", this.sdkVersion);
            params.put("type", this.eventName);
            return params;
        }

        public String toJSONString(Map<String, Object> map) {
            GsonBuilder gsonMapBuilder = new GsonBuilder();

            Gson gsonObject = gsonMapBuilder.create();

            String JSONObject = gsonObject.toJson(map);
            return JSONObject;
        }
    }

    final public class ClickEvent extends BaseEvent {
        public int x;
        public int y;
        public String interactionType;
        public String eventName = "click";

        public Map<String, Object> toMap() {
            Map<String, Object> params = super.toMap();
            params.put("x", x);
            params.put("y", y);
            params.put("interactionType", interactionType);
            return params;
        }


    }

    final public class ViewEvent extends BaseEvent {
        public String eventName = "view";
        public String uri;
        public String customProperties;

        public Map<String, Object> toMap() {
            Map<String, Object> params = super.toMap();
            params.put("customProperties", customProperties);
            params.put("uri", uri);
            return params;
        }
    }
}
