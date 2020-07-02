package io.primed.primedandroid;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class Primed {

    public interface PrimedCallback {
        void onSuccess(JSONObject response);
        void onFailure();
    }

    private static Primed sSoleInstance;

    public final OkHttpClient client = new OkHttpClient();
    public Boolean primedTrackerAvailable;
    private String urlPrimedIO;
    private String public_key;
    private String secret_key;

    private Primed() {
        if (sSoleInstance != null){
            throw new RuntimeException("Use getInstance() method to get the single instance of this class.");
        }
    }

    public static Primed getInstance(){
        if (sSoleInstance == null){ //if there is no instance available... create new one
            sSoleInstance = new Primed();
        }

        return sSoleInstance;
    }

    public void init(String publicKey, String secretKey, String url) {
        this.urlPrimedIO = url;
        this.public_key = publicKey;
        this.secret_key = secretKey;
    }

    public static String createSHA512(String input) {

        String generatedSHA = null;

        try {
            MessageDigest sh = MessageDigest.getInstance("SHA-512");
            sh.update(input.getBytes());
            StringBuffer sb = new StringBuffer();
            for (byte b : sh.digest()) sb.append(String.format("%1$02x", 0xff & b));
            generatedSHA = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        Log.d("Primed", "SHA: " + generatedSHA);

        return generatedSHA;

    }

    private void get(String url, final HttpCallback cb) {
        String nonce = String.valueOf(System.currentTimeMillis() / 1000l);
        String signature = Primed.createSHA512(this.public_key + this.secret_key + nonce);

        Request request = new Request.Builder()
                .url(url)
                .method("GET", null)
                .addHeader("X-Authorization-Key", this.public_key)
                .addHeader("X-Authorization-Signature", signature)
                .addHeader("X-Authorization-Nonce", nonce)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                cb.onFailure(null, e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (!response.isSuccessful()) {
                    cb.onFailure(response, null);
                    return;
                }
                cb.onSuccess(response);
            }
        });

    }

    private void post(String url, Map<String, Object> params, final HttpCallback cb) {
        String nonce = String.valueOf(System.currentTimeMillis() / 1000l);
        String signature = Primed.createSHA512(this.public_key + this.secret_key + nonce);

        FormEncodingBuilder builder = new FormEncodingBuilder();
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                builder.add(entry.getKey(), entry.getValue().toString());
            }
        }
        RequestBody formBody = builder.build();

        Request request = new Request.Builder()
                .url(url)
                .method("POST", formBody )
                .addHeader("X-Authorization-Key", this.public_key)
                .addHeader("X-Authorization-Signature", signature)
                .addHeader("X-Authorization-Nonce", nonce)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Request request, IOException e) {
                cb.onFailure(null, e);
            }

            @Override
            public void onResponse(Response response) throws IOException {
                if (!response.isSuccessful()) {
                    cb.onFailure(response, null);
                    return;
                }
                cb.onSuccess(response);
            }
        });
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
        this.convert(ruuid, null);
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
        String generateURL = this.urlPrimedIO + "/api/v1/conversion/" + ruuid;

       this.post(generateURL, data, new Primed.HttpCallback() {
           @Override
           public void onFailure(Response response, Throwable throwable) {
               // do nothing
           }

           @Override
           public void onSuccess(Response response) {
               // do nothing
           }
       });
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
            final PrimedCallback callback
    ) {
        this.personalise(campaign, limit, null, callback);
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
            final Primed.PrimedCallback callback
    ) {
        this.personalise(campaign, signals, limit, null, callback);
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
            final PrimedCallback callback
    ) {

        HashMap<String, Object> signals = new HashMap<>();
        if (primedTrackerAvailable == true) {
            signals.put("did", PrimedTracker.getInstance().getDid());
            signals.put("sid", PrimedTracker.getInstance().getSid());
        }

        this.personalise(campaign, signals, limit, abVariantLabel, callback);
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
            final PrimedCallback callback
    ) {

        if (primedTrackerAvailable == true) {
            //this will set the system defaults
            HashMap<String, Object> systemSignals = new HashMap<>();
            signals.put("did", PrimedTracker.getInstance().getDid());
            signals.put("sid", PrimedTracker.getInstance().getSid());

            //Merge them together (will override existing values in the provided signals)
            signals.putAll(systemSignals);
        }

        String signalsString = URLEncoder.encode(this.toJSONString(signals));

        String generateURL = this.urlPrimedIO + "/api/v1/personalise?";
        generateURL += "campaign=" + campaign;
        generateURL += "&limit=" + limit;
        if (abVariantLabel != null ) {
            generateURL += "&abvariant=" + abVariantLabel;
        }
        generateURL += "&signals=" + signalsString;

        this.get(generateURL, new Primed.HttpCallback() {
            @Override
            public void onFailure(Response response, Throwable throwable) {
                callback.onFailure();
            }

            @Override
            public void onSuccess(Response response) {
                if (primedTrackerAvailable == true) {
                    PrimedTracker.PersonaliseEvent event = PrimedTracker.getInstance().new PersonaliseEvent();
                    try {
                        String jsonData = response.body().string();
                        JSONObject responseJSON = new JSONObject(jsonData);

                        String guuid = responseJSON.getString("guuid");
                        event.guuid = guuid;
                    } catch (Exception e) {

                    }
                    PrimedTracker.getInstance().trackEvent(event);
                }
                String respBody = null;

                if (response.body() != null) {
                    try {
                        respBody = response.body().string();
                    } catch (IOException e) {
                        //throw new ApiException(response.message(), e, response.code(), response.headers().toMultimap());
                    }
                }

                //Try to parse the response as json, and perform callback
                try {
                    JSONObject responseJSON = new JSONObject(respBody);
                    callback.onSuccess(responseJSON);
                } catch (Exception e) {
                    //parsing was not successful, pass an empty object
                    callback.onSuccess(new JSONObject());
                }
            }
        });

    }

    public void health(final PrimedCallback callback) {

       String generateURL = this.urlPrimedIO + "/api/v1/health";

       this.get(generateURL, new Primed.HttpCallback() {
            @Override
            public void onFailure(Response response, Throwable throwable) {
                callback.onFailure();
            }

            @Override
            public void onSuccess(Response response) {
                String respBody = null;

                if (response.body() != null) {
                    try {
                        respBody = response.body().string();
                    } catch (IOException e) {
                        //throw new ApiException(response.message(), e, response.code(), response.headers().toMultimap());
                    }
                }

                //Try to parse the response as json, and perform callback
                try {
                    JSONObject responseJSON = new JSONObject(respBody);
                    callback.onSuccess(responseJSON);
                } catch (Exception e) {
                    //parsing was not successful
                    callback.onSuccess(new JSONObject());
                }
            }
        });

    }

    private String toJSONString(Map<String, Object> map) {
        GsonBuilder gsonMapBuilder = new GsonBuilder();

        Gson gsonObject = gsonMapBuilder.create();

        String JSONObject = gsonObject.toJson(map);
        return JSONObject;
    }

    public interface HttpCallback  {

        /**
         * called when the server response was not 2xx or when an exception was thrown in the process
         * @param response - in case of server error (4xx, 5xx) this contains the server response
         *                 in case of IO exception this is null
         * @param throwable - contains the exception. in case of server error (4xx, 5xx) this is null
         */
        public void onFailure(Response response, Throwable throwable);

        /**
         * contains the server response
         * @param response
         */
        public void onSuccess(Response response);
    }
}
