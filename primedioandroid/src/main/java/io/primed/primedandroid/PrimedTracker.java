package io.primed.primedandroid;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.primed.primedioandroid.BuildConfig;
import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.WebSocket;

import android.provider.Settings.Secure;

final public class PrimedTracker {

    private static PrimedTracker sSoleInstance;

    private String public_key;
    private String nonce;
    private String trackingConnectionString;
    private String connectionString;
    private String sha512_signature;
    private int heartbeatInterval;
    private int heartbeatCount;

    private String sid;
    private String did;

    public Context context;
    public Map<String, Object> customBasicProperties;

    public String getDid() {
        return did;
    }

    public String getSid() {
        return sid;
    }

    Runnable heartbeatRunnable;

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

    public void init(String publicKey, String secretKey, String connectionString, Context context,  String trackingConnectionString, int heartbeatInterval) {
        String nonce = String.valueOf(new Date().getTime());

        String prepSignature = publicKey + secretKey + nonce;
        String signature = Primed.createSHA512(prepSignature);

        Primed.getInstance().init(publicKey, secretKey, connectionString);

        String android_id = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);

        this.public_key = publicKey;
        this.sha512_signature = signature;
        this.nonce = nonce;
        this.did = android_id;
        this.sid = UUID.randomUUID().toString();
        this.trackingConnectionString = trackingConnectionString + "/v1";
        this.connectionString = connectionString;
        this.context = context;
        this.heartbeatInterval = heartbeatInterval;

        try {
            IO.Options options = new IO.Options();
            options.reconnection = true;
            options.forceNew = true;
            options.transports = new String[] { WebSocket.NAME };

            mSocket = IO.socket(this.trackingConnectionString, options);
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
            Log.d("PrimedTracker", String.format("connected to %s", trackingConnectionString));

            //start heartbeat:
            if (heartbeatInterval > 0 && heartbeatRunnable == null) {
                heartbeatCount = 1;

                heartbeatRunnable = new Runnable() {
                    @Override
                    public void run() {
                        HeartbeatEvent beat = new HeartbeatEvent();
                        trackEvent(beat);

                        Handler sHandler = new Handler(Looper.getMainLooper());
                        sHandler.postDelayed(heartbeatRunnable, heartbeatInterval * 1000);
                    }
                };

                Handler sHandler = new Handler(Looper.getMainLooper());
                sHandler.postDelayed(heartbeatRunnable, heartbeatInterval * 1000);

            }
        }
    };


    public Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.d("PrimedTracker", "disconnected");
        }
    };

    public Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.d("PrimedTracker", String.format("error connecting to %s", trackingConnectionString));
        }
    };

    public Emitter.Listener onError = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.d("PrimedTracker", "message error");
        }
    };


    private Emitter.Listener onNewMessage = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Handler sHandler = new Handler(Looper.getMainLooper());
            sHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                }
            }, 0);

        };
    };

    private Socket mSocket;

    public void trackEvent(BaseEvent event) {
        event.createMap();

        event.eventName = event.eventName.toUpperCase();

        Log.d("PrimedTracker", String.format("emitting: %s", event.eventName));

        JSONObject obj = event.toJSONObject();

        mSocket.emit("event", obj);
    }

    public class BaseEvent {
        public String eventName = "";
        String apiKey = public_key;
        String ts = String.valueOf(System.currentTimeMillis());
        String sid = PrimedTracker.getInstance().sid;
        String did = PrimedTracker.getInstance().did;
        String source = "APP";
        String sdkVersion = "0.0.5";

        Map<String, Object> params = new HashMap<String, Object>();
        Map<String, Object> eventObject = new HashMap<String, Object>();

        public String toString() {
            return this.toJSONString();
        }

        public void createMap() {
            params.clear();
            params.put("apikey", this.apiKey);
            params.put("ts", this.ts);
            params.put("source", this.source.toUpperCase());
            params.put("sid", this.sid);
            params.put("did", this.did);
            params.put("sdkId", 1);
            params.put("sdkVersion", this.sdkVersion);
            params.put("type", this.eventName.toUpperCase());
            params.put("eventObject", eventObject);
            if (customBasicProperties != null) {
                params.put("customProperties", customBasicProperties);
            }
        }

        public JSONObject toJSONObject() {
            this.createMap();
            JSONObject obj = new JSONObject(params);
            return obj;
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
        private String eventName = "click";
        public int x;
        public int y;
        public InteractionType interactionType;

        public void createMap() {
            super.eventName = eventName;
            super.eventObject.put("x", x);
            super.eventObject.put("y", y);
            super.eventObject.put("interactionType", interactionType.stringValue);
            super.createMap();
        }
    }

    final public class ViewEvent extends BaseEvent {
        private String eventName = "view";
        public String uri;
        public Map<String, Object> customProperties;

        public void createMap() {
            super.eventName = eventName;
            super.eventObject.put("customProperties", customProperties);
            super.eventObject.put("uri", uri);
            super.createMap();
        }
    }

    final public class ScrollEvent extends BaseEvent {
        private String eventName = "scroll";
        public ScrollDirection scrollDirection;
        public int distance;

        public void createMap() {
            super.eventName = eventName;
            super.eventObject.put("scrollDirection", scrollDirection.stringValue);
            super.createMap();
        }
    }

    final public class EnterViewportEvent extends BaseEvent {
        private String eventName = "enterViewPort";
        public Map<String, Object> customProperties;

        public void createMap() {
            super.eventName = eventName;
            super.eventObject.put("customProperties", customProperties);
            super.createMap();
        }
    }

    final public class ExitViewportEvent extends BaseEvent {
        private String eventName = "exitViewPort";
        public Map<String, Object> customProperties;

        public void createMap() {
            super.eventName = eventName;
            super.eventObject.put("customProperties", customProperties);
            super.createMap();
        }
    }

    final public class PositionChangeEvent extends BaseEvent {
        private String eventName = "positionchange";
        public float latitude;
        public float longitude;
        public float accuracy;

        public void createMap() {
            super.eventName = eventName;
            super.eventObject.put("latitude",latitude);
            super.eventObject.put("longitude",longitude);
            super.eventObject.put("accuracy",accuracy);
            super.createMap();
        }
    }

    final public class HeartbeatEvent extends BaseEvent {
        private String eventName = "heartbeat";

        public void createMap() {
            super.eventName = eventName;
            super.eventObject.put("i", heartbeatCount);
            heartbeatCount += 1;
            super.createMap();
        }
    }

    final public class CustomEvent extends BaseEvent {
        private String eventType;
        public Map<String, Object> customProperties;

        public void createMap() {
            super.eventName = eventType;
            super.eventObject.put("customProperties", customProperties);
            super.createMap();
        }
    }

    final public class StartEvent extends BaseEvent {
        private String eventName = "start";
        public String uri;
        public Map<String, Object> customProperties;

        public void createMap() {
            String manufacturer = android.os.Build.MANUFACTURER;
            String model = android.os.Build.MODEL;
            String result = model;
            if (model.startsWith(manufacturer) == false) {
                result = manufacturer + " " + model;
            }

            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);

            super.eventName = eventName;
            super.eventObject.put("customProperties", customProperties);
            super.eventObject.put("uri", uri);
            super.eventObject.put("ua",result);
            super.eventObject.put("now", String.valueOf(new Date(System.currentTimeMillis())));
            super.eventObject.put("screenWidth", String.valueOf(size.x));
            super.eventObject.put("screenHeight",  String.valueOf(size.y));
            super.eventObject.put("viewPortWidth",  String.valueOf(size.x));
            super.eventObject.put("viewPortHeight",  String.valueOf(size.y));
            super.createMap();
        }
    }

    final public class EndEvent extends BaseEvent {
        private String eventName = "end";
        public Map<String, Object> customProperties;

        public void createMap() {
            super.eventName = eventName;
            super.eventObject.put("customProperties", customProperties);
            super.createMap();
        }
    }

    final public class PersonaliseEvent extends BaseEvent {
        private String eventName = "personalise";
        public String guuid;

        public void createMap() {
            super.eventName = eventName;
            eventObject.put("guuid", guuid);
            super.createMap();
        }
    }

    public enum InteractionType {
        LEFT("LEFT",0),
        RIGHT("RIGHT",1),
        MIDDLE("MIDDLE",2),
        OTHER("OTHER",3),
        LONGPRESS("LONGPRESS",4);

        private String stringValue;
        private int intValue;
        private InteractionType(String toString, int value) {
            stringValue = toString;
            intValue = value;
        }

        @Override
        public String toString() {
            return stringValue;
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
