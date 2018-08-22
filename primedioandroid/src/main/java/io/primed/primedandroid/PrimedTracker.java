package io.primed.primedandroid;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.Response;

import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

final public class PrimedTracker {

    private static PrimedTracker sSoleInstance;

    private String public_key;
    private String nonce;
    private String trackingConnectionString;
    private String connectionString;
    private String sid;
    private String did;
    private String sha512_signature;
    private int heartbeatInterval;
    private int heartbeatCount;

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

    public void init(String publicKey, String secretKey, String connectionString, String trackingConnectionString, int heartbeatInterval, String deviceID) {
        String nonce = String.valueOf(new Date().getTime());

        String prepSignature = publicKey + secretKey + nonce;
        String signature = Primed.createSHA512(prepSignature);

        Primed.getInstance().init(publicKey, secretKey, connectionString);

        this.public_key = publicKey;
        this.sha512_signature = signature;
        this.nonce = nonce;
        this.did = deviceID;
        this.sid = UUID.randomUUID().toString();
        this.trackingConnectionString = trackingConnectionString;
        this.connectionString = connectionString;
        this.heartbeatInterval = heartbeatInterval;

        try {
            IO.Options options = new IO.Options();
            options.reconnection = true;
            options.forceNew = true;

            mSocket = IO.socket(trackingConnectionString, options);
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
            Log.d("PrimedTracker", "connect error");
        }
    };

    public Emitter.Listener onError = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {

        }
    };

//    public Emitter.Listener onNewMessage = new Emitter.Listener() {
//        @Override
//        public void call(final Object... args) {
//            JSONObject data = (JSONObject) args[0];
//        }
//    };

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


//            getActivity().runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    JSONObject data = (JSONObject) args[0];
//                }
//            });
        };
    };

    private Socket mSocket;

    public void trackEvent(BaseEvent event) {
        event.createMap();

        Log.d("PrimedTracker", String.format("emitting: %s",event.eventName));

        mSocket.emit("event", event.toJSONObject(), new Ack() {
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
        String sid = PrimedTracker.getInstance().sid;
        String did = PrimedTracker.getInstance().did;
        String source = "app";
        String sdkVersion = "1.0";
        String type = "";

        Map<String, Object> params = new HashMap<String, Object>();
        Map<String, Object> eventObject = new HashMap<String, Object>();

        public String toString() {
            return this.toJSONString();
        }

        public void createMap() {
            params.clear();
            params.put("apiKey", this.apiKey);
            params.put("ts", this.ts);
            params.put("source", this.source.toUpperCase());
            params.put("sid", this.sid);
            params.put("did", this.did);
            params.put("sdkId", 1);
            params.put("sdkVersion", this.sdkVersion);
            params.put("type", this.eventName.toUpperCase());
            params.put("eventObject", eventObject);
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

        public void createMap() {
            super.eventName = eventName;
            super.eventObject.put("scrollDirection", scrollDirection.stringValue);
            super.createMap();
        }
    }

    final public class EnterViewportEvent extends BaseEvent {
        private String eventName = "enterViewPort";
        public String campaign;
        public int[] elements;

        public void createMap() {
            super.eventName = eventName;
            super.eventObject.put("campaign", campaign);
            super.eventObject.put("elements", elements);
            super.createMap();
        }
    }

    final public class ExitViewportEvent extends BaseEvent {
        private String eventName = "exitViewPort";
        public String campaign;
        public int[] elements;

        public void createMap() {
            super.eventName = eventName;
            super.eventObject.put("campaign", campaign);
            super.eventObject.put("elements", elements);
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
        private String eventName = "custom";
        public Map<String, Object> customProperties;

        public void createMap() {
            super.eventName = eventName;
            super.eventObject.put("customProperties", customProperties);
            super.createMap();
        }
    }

    final public class PersonalizeEvent extends BaseEvent {
        private String eventName = "personalise";
        public Response response;

        public void createMap() {
            super.eventName = eventName;

            //TODO: convert response to hashmap
            //eventObject.put("response", response);
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