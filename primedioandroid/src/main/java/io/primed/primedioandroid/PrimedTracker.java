package io.primed.primedioandroid;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;
import com.github.nkzawa.socketio.client.IO;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;
import java.util.Date;

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
    }

    private Socket mSocket;
    {
        try {
            mSocket = IO.socket("http://primed.io");

            //mSocket.on("new message", this.onNewMessage);
            mSocket.connect();

        } catch (URISyntaxException e) {}
    }

    private void sendEvent(BaseEvent event) {
        String message = event.toString();

        mSocket.emit("new event", message);
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
        String apiKey = "";
        Date ts = new Date();
        String sid;
        String did;
        String source = "app";
        String sdkVersion = "1.0";
        String type;

        public String toString() {
            return "";
        }
    }

    public void onClickEvent(ClickEvent event) {

    }

    final public class ClickEvent extends BaseEvent {
        int x;
        int y;
        String interactionType;
    }

    final public class ViewEvent extends BaseEvent {
        String uri;
        String customProperties;
    }
}
