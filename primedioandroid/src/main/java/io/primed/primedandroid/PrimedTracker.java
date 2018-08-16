package io.primed.primedandroid;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Ack;
import com.github.nkzawa.socketio.client.Socket;
import com.github.nkzawa.socketio.client.IO;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

final public class PrimedTracker {

    private static PrimedTracker sSoleInstance;

    private String public_key;
    private String nonce;
    private String trackingConnectionString;
    private String connectionString;
    private String sha512_signature;
    private int heartbeatInterval;
    private int heartbeatCount;

    Timer heartbeatTimer;

    private PrimedTracker() {
        if (sSoleInstance != null){
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    public static PrimedTracker getInstance(){
        if (sSoleInstance == null){ //if there is no instance available... create new one
            sSoleInstance = new PrimedTracker();
        }

        return sSoleInstance;
    }

    public void init(String publicKey, String secretKey, String connectionString, String trackingConnectionString, int heartbeatInterval, String deviceID) {
        String nonce = String.valueOf(new Date().getTime());

        String prepSignature = publicKey + secretKey + nonce;
        String signature = Primed.createSHA512(prepSignature);

        Primed.getInstance().init(publicKey, secretKey, connectionString);

        this.public_key = publicKey;
        this.sha512_signature = signature;
        this.nonce = nonce;
        this.trackingConnectionString = trackingConnectionString;
        this.connectionString = connectionString;
        this.heartbeatInterval = heartbeatInterval;

        try {
            mSocket = IO.socket(trackingConnectionString);
            mSocket.on(Socket.EVENT_MESSAGE, onNewMessage);
            mSocket.on(Socket.EVENT_CONNECT, onConnect);
            mSocket.on(Socket.EVENT_DISCONNECT, onDisconnect);
            mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
            mSocket.on(Socket.EVENT_CONNECT_TIMEOUT, onConnectError);
            mSocket.on(Socket.EVENT_ERROR, onError);
            mSocket.connect();
        } catch (URISyntaxException e) {

        }

        Primed.getInstance().primedTrackerAvailable = true;
    }

    public Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.d("PrimedTracker", "connected");

            //start heartbeat:
            if (heartbeatInterval > 0) {
                heartbeatCount = 1;

                heartbeatTimer = new Timer();
                heartbeatTimer.schedule(new HeartbeatTask(), heartbeatInterval * 1000);
            }
        }
    };

    class HeartbeatTask extends TimerTask {
        public void run() {
            if (mSocket.connected()) {
                //this seems to disconnect the socket, thread issues?
                //HeartbeatEvent beat = new HeartbeatEvent();
                //trackEvent(beat);
            }
            //heartbeatTimer.cancel(); //Terminate the timer thread?
        }
    }

    public Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.d("PrimedTracker", "disconnected");
        }
    };

    public Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.d("PrimedTracker", "connect error");
        }
    };

    public Emitter.Listener onError = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {

        }
    };

    public Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            JSONObject data = (JSONObject) args[0];
        }
    };

    private Socket mSocket;

    public void trackEvent(BaseEvent event) {
        event.createMap();

        Log.d("PrimedTracker", String.format("emitting: %s",event.eventName));

        mSocket.emit("event", event.params, new Ack() {
            @Override
            public void call(Object... args) {

            }
        });

    }

    public class BaseEvent {
        String deviceId = "";

        public String eventName = "";
        String apiKey = public_key;
        String ts = String.valueOf(System.currentTimeMillis() / 1000l);
        String sid = UUID.randomUUID().toString();
        String did = deviceId;
        String source = "app";
        String sdkVersion = "1.0";
        String type = "";

        Map<String, Object> params = new HashMap<String, Object>();
        Map<String, Object> eventObject = new HashMap<String, Object>();;

        public String toString() {
            return this.toJSONString();
        }

        public void createMap() {
            params.clear();
            eventObject.clear();
            params.put("apiKey", this.apiKey);
            params.put("ts", this.ts);
            params.put("source", this.source);
            params.put("sid", this.source);
            params.put("did", this.source);
            params.put("sdkId", 1);
            params.put("sdkVersion", this.sdkVersion);
            params.put("type", this.eventName.toUpperCase());
            params.put("eventObject", eventObject);
        }

        public String toJSONString() {
            this.createMap();
            GsonBuilder gsonMapBuilder = new GsonBuilder();

            Gson gsonObject = gsonMapBuilder.create();

            String JSONObject = gsonObject.toJson(params);
            return JSONObject;
        }
    }

    final public class ClickEvent extends BaseEvent {
        public String eventName = "click";
        public int x;
        public int y;
        public String interactionType;

        public void createMap() {
            super.eventName = eventName;
            eventObject.put("x", x);
            eventObject.put("y", y);
            eventObject.put("interactionType", interactionType);
            super.createMap();
        }
    }

    final public class ViewEvent extends BaseEvent {
        public String eventName = "view";
        public String uri;
        public String customProperties;

        public void createMap() {
            super.eventName = eventName;
            eventObject.put("customProperties", customProperties);
            eventObject.put("uri", uri);
            super.createMap();
        }
    }

    final public class ScrollEvent extends BaseEvent {
        public String eventName = "scroll";
        public ScrollDirection scrollDirection;

        public void createMap() {
            super.eventName = eventName;
            eventObject.put("scrollDirection", scrollDirection);
            super.createMap();
        }
    }

    final public class EnterViewportEvent extends BaseEvent {
        public String eventName = "enterViewPort";
        public String campaign;
        public int[] elements;

        public void createMap() {
            super.eventName = eventName;
            eventObject.put("campaign", campaign);
            eventObject.put("elements", elements);
            super.createMap();
        }
    }

    final public class ExitViewportEvent extends BaseEvent {
        public String eventName = "exitViewPort";
        public String campaign;
        public int[] elements;

        public void createMap() {
            super.eventName = eventName;
            eventObject.put("campaign", campaign);
            eventObject.put("elements", elements);
            super.createMap();
        }
    }

    final public class PositionChangeEvent extends BaseEvent {
        public String eventName = "positionchange";
        public float latitude;
        public float longitude;
        public float accuracy;

        public void createMap() {
            super.eventName = eventName;
            eventObject.put("latitude",latitude);
            eventObject.put("longitude",longitude);
            eventObject.put("accuracy",accuracy);
            super.createMap();
        }
    }

    final public class HeartbeatEvent extends BaseEvent {
        public String eventName = "heartbeat";

        public void createMap() {
            super.eventName = eventName;
            eventObject.put("i", heartbeatCount);
            heartbeatCount += 1;
            super.createMap();
        }
    }

    final public class CustomEvent extends BaseEvent {
        public String eventName = "custom";
        public Map<String, Object> customProperties;

        public void createMap() {
            super.eventName = eventName;
            eventObject.put("customProperties", customProperties);
            super.createMap();
        }
    }

    final public class PersonalizeEvent extends BaseEvent {
        public String eventName = "personalise";
        public Response response;

        public void createMap() {
            super.eventName = eventName;

            //TODO: convert response to hashmap
            //eventObject.put("response", response);
            super.createMap();
        }
    }

    public enum ScrollDirection {
        UP("UP", 0),
        DOWN("DOWN", 1);

        private String stringValue;
        private int intValue;
        private ScrollDirection(String toString, int value) {
            stringValue = toString;
            intValue = value;
        }

        @Override
        public String toString() {
            return stringValue;
        }
    }
}
