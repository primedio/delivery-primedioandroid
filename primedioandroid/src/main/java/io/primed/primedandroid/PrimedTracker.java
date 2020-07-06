package io.primed.primedandroid;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.json.JSONObject;

import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;
import io.socket.engineio.client.transports.WebSocket;

import android.provider.Settings.Secure;

import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
import static android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;

final public class PrimedTracker {

    private static PrimedTracker sSoleInstance;

    private String public_key;

    private String trackingConnectionString;

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

    private boolean was_in_background;
    private boolean was_in_foreground;

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

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public boolean isForeground() {
        ActivityManager.RunningAppProcessInfo appProcessInfo = new ActivityManager.RunningAppProcessInfo();
        ActivityManager.getMyMemoryState(appProcessInfo);
        return (appProcessInfo.importance == IMPORTANCE_FOREGROUND || appProcessInfo.importance == IMPORTANCE_VISIBLE);
    }

    public void init(String publicKey, String secretKey, String connectionString, Context context, String trackingConnectionString, final int heartbeatInterval) {
        Log.d("PrimedTracker", "Initializing PrimedTracker");

        Primed.getInstance().init(publicKey, secretKey, connectionString);

        String android_id = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);

        this.was_in_background = true;
        this.was_in_background = false;

        this.public_key = publicKey;
        this.did = android_id;
        this.sid = UUID.randomUUID().toString();
        this.trackingConnectionString = trackingConnectionString + "/v1";
        this.context = context;

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
            Log.e("PrimedTracker", e.toString());
        }

        // start HEARTBEAT, please note the HEARTBEAT runnable keeps running in the background, even
        // when the application is not in the foreground. It only emits HEARTBEAT events in the case
        // that the application is in the foreground though - it also resets the HEARTBEAT counter
        // and `sid` if it detects that it is running the background.
        if (heartbeatInterval > 0 && heartbeatRunnable == null) {
            heartbeatRunnable = new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
                @Override
                public void run() {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

                    if (PrimedTracker.getInstance().isForeground()) {
                        was_in_foreground = true;

                        if (was_in_background) {
                            // The `was_in_background` flag tells us the application has been in the
                            // background at some point in the past - we therefore emit a START
                            // event and set the flag to false
                            StartEvent e = new StartEvent();
                            e.uri = "";
                            trackEvent(e);
                            was_in_background = false;
                        }

                        HeartbeatEvent beat = new HeartbeatEvent();
                        trackEvent(beat);
                        heartbeatCount += 1;


                    } else {
                        was_in_background = true;

                        if (was_in_foreground) {
                            // The `was_in_foreground` flag tells us the application has been in the
                            // foreground at some point in the past, but it isn't anymore - we
                            // therefore emit a END event and set the flag to false
                            EndEvent e = new EndEvent();
                            trackEvent(e);
                            was_in_foreground = false;
                        }

                        heartbeatCount = 0;
                        sid = UUID.randomUUID().toString();
                    }

                    Handler sHandler = new Handler(Looper.getMainLooper());
                    sHandler.postDelayed(heartbeatRunnable, heartbeatInterval * 1000);

                }
            };

            Handler sHandler = new Handler(Looper.getMainLooper());
            sHandler.postDelayed(heartbeatRunnable, heartbeatInterval * 1000);

        }

        Primed.getInstance().primedTrackerAvailable = true;

        // Fire off START event upon init
        StartEvent e = new StartEvent();
        e.uri = "";
        this.trackEvent(e);
    }

    public Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            Log.d("PrimedTracker", String.format("connected to %s", trackingConnectionString));

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
        long ts = System.currentTimeMillis();
        String sid = PrimedTracker.getInstance().sid;
        String did = PrimedTracker.getInstance().did;
        String source = "APP";
        String sdkVersion = "0.0.8";

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
            super.createMap();
        }
    }

    final public class CustomEvent extends BaseEvent {
        public String eventType;
        public Map<String, Object> customProperties;

        public void createMap() {
            super.eventName = eventType;
            for (Map.Entry<String, Object> entry : customProperties.entrySet()) {
                super.eventObject.put(entry.getKey(), entry.getValue());
            }

            super.createMap();
        }
    }

    final public class StartEvent extends BaseEvent {
        private String eventName = "start";
        public String uri;
        public Map<String, Object> customProperties;

        public void createMap() {
            String manufacturer = Build.MANUFACTURER;
            String model = Build.MODEL;
            String ua_string = model;
            if (model.startsWith(manufacturer) == false) {
                ua_string = manufacturer + " " + model;
            }

            String release = Build.VERSION.RELEASE;
            ua_string += ";" + release;

            WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            Display display = wm.getDefaultDisplay();
            Point size = new Point();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
                display.getSize(size);
            }

            if (customProperties == null) {
                customProperties = new HashMap();
            }

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.UK);
            String dateString = sdf.format(new Date());

            super.eventName = eventName;
            super.eventObject.put("customProperties", customProperties);
            super.eventObject.put("uri", uri);
            super.eventObject.put("ua",ua_string);
            super.eventObject.put("now", dateString);
            super.eventObject.put("screenWidth", size.x);
            super.eventObject.put("screenHeight", size.y);
            super.eventObject.put("viewPortWidth",  size.x);
            super.eventObject.put("viewPortHeight",  size.y);
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

    final public class ConvertEvent extends BaseEvent {
        private String eventName = "convert";
        public String ruuid;
        public Map<String, Object> data;

        public void createMap() {
            super.eventName = eventName;
            eventObject.put("ruuid", ruuid);

            if (data != null) {
                eventObject.put("data", data);
            }

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



    /**
     * Calls the personalise endpoint and returns a list of results using server side A/B membership
     * and using the default signals: if the <code>PrimedTracker</code> is initialized
     * <code>sid</code> and <code>did</code> signals are automatically sent through. If
     * <code>PrimedTracker</code> is not initialized the signals maps will be empty.
     *
     * @param campaign          the campaign for which to get results
     * @param limit             number of desired results
     * @param callback          callback for results handling
     *
     * @since           0.0.8
     */
    public void personalise(
            String campaign,
            int limit,
            final Primed.PersonaliseCallback callback
    ) {
        Primed.getInstance().personalise(campaign, limit, callback);
    }

    /**
     * Calls the personalise endpoint and returns a list of results using server side A/B membership
     * and provided signals.
     *
     * @param campaign          the campaign for which to get results
     * @param signals           signals to be used for obtaining results
     * @param limit             number of desired results
     * @param callback          callback for results handling
     *
     * @since           0.0.8
     */
    public void personalise(
            String campaign,
            Map<String, Object> signals,
            int limit,
            final Primed.PersonaliseCallback callback
    ) {
        Primed.getInstance().personalise(campaign, signals, limit, callback);
    }

    /**
     * Calls the personalise endpoint and returns a list of results for a given A/B variant label
     *
     * @param campaign          the campaign for which to get results
     * @param limit             number of desired results
     * @param abVariantLabel    force A/B variant
     * @param callback          callback for results handling
     *
     * @since           0.0.8
     */
    public void personalise(
            String campaign,
            int limit,
            String abVariantLabel,
            final Primed.PersonaliseCallback callback
    ) {
        Primed.getInstance().personalise(campaign, limit, abVariantLabel, callback);
    }

    /**
     * Calls the personalise endpoint and returns a list of results for a given A/B variant label
     *
     * @param campaign          the campaign for which to get results
     * @param signals           signals to be used for obtaining results
     * @param limit             number of desired results
     * @param abVariantLabel    force A/B variant
     * @param callback          callback for results handling
     *
     * @since           0.0.8
     */
    public void personalise(
            String campaign,
            Map<String, Object> signals,
            int limit,
            String abVariantLabel,
            final Primed.PersonaliseCallback callback
    ) {
        Primed.getInstance().personalise(campaign, signals, limit, abVariantLabel, callback);
    }

    /**
     * Marks a result, identified using the <code>ruuid</code>, as converted. Typically this means a
     * user clicked a recommendation. Upon clicking the recommendation, this method should be called
     * along with the <code>ruuid</code> belonging to that recommended item to flag it as converted.
     *
     * @param ruuid          the campaign for which to get results
     *
     * @since           0.0.1
     */
    public void convert(String ruuid) {
        Primed.getInstance().convert(ruuid, null);
    }

    /**
     * Marks a result, identified using the <code>ruuid</code>, as converted. Typically this means a
     * user clicked a recommendation. Upon clicking the recommendation, this method should be called
     * along with the <code>ruuid</code> belonging to that recommended item to flag it as converted.
     *
     * This call allows for an additional <code>data</code> payload, which may be specified in the
     * project spec.
     *
     * @param ruuid          the campaign for which to get results
     * @param data           number of desired results
     *
     * @since           0.0.1
     */
    public void convert(String ruuid, Map<String, Object> data) {
        Primed.getInstance().convert(ruuid, data);
    }
}
