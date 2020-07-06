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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Primed {

    public interface PersonaliseCallback {
        void onSuccess(ResultSet resultSet);
        void onFailure(Throwable throwable);
    }

    public interface HealthCallback {
        void onSuccess();
        void onFailure(Throwable throwable);
    }

    private static Primed sSoleInstance;

    private final OkHttpClient client = new OkHttpClient();
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

    private static String createSHA512(String input) {

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
    public void convert(final String ruuid, final Map<String, Object> data) {
        String generateURL = this.urlPrimedIO + "/api/v1/conversion/" + ruuid;

        this.post(generateURL, data, new Primed.HttpCallback() {
           @Override
           public void onFailure(Response response, Throwable throwable) {
               // do nothing
           }

           @Override
           public void onSuccess(Response response) {
               if (primedTrackerAvailable == true) {
                   // If we are using the PrimedTracker, we also emit a CONVERT event
                   PrimedTracker.ConvertEvent event = PrimedTracker.getInstance().new ConvertEvent();
                   event.ruuid = ruuid;

                   if (data != null) {
                       event.data = data;
                   }

                   PrimedTracker.getInstance().trackEvent(event);
               }
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
            final PersonaliseCallback callback
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
            final PersonaliseCallback callback
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
            final PersonaliseCallback callback
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
            final PersonaliseCallback callback
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
                callback.onFailure(throwable);
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

                    String guuid = responseJSON.getString("guuid");

                    if (primedTrackerAvailable == true) {
                        // If we are using the PrimedTracker, we also emit a PERSONALISE event
                        PrimedTracker.PersonaliseEvent event = PrimedTracker.getInstance().new PersonaliseEvent();
                        event.guuid = guuid;
                        PrimedTracker.getInstance().trackEvent(event);
                    }

                    ResultSet rs = new ResultSet(guuid);

                    JSONArray res = responseJSON.getJSONArray("results");
                    for (int i = 0 ; i < res.length(); i++) {
                        JSONObject resJsonObj = res.getJSONObject(i);

                        HashMap<String, Object> value_hashmap = new Gson().fromJson(
                                resJsonObj.getJSONObject("target").getJSONObject("value").toString(),
                                HashMap.class
                        );

                        Result result = new Result(resJsonObj.getString("ruuid"), value_hashmap);

                        rs.addResult(result);
                    }

                    callback.onSuccess(rs);
                } catch (Exception e) {
                    callback.onFailure(e);
                }
            }
        });

    }

    public void health(final HealthCallback callback) {

       String generateURL = this.urlPrimedIO + "/api/v1/health";

       this.get(generateURL, new Primed.HttpCallback() {
            @Override
            public void onFailure(Response response, Throwable throwable) {
                callback.onFailure(throwable);
            }

            @Override
            public void onSuccess(Response response) {
                if (response.code() == 200) {
                    callback.onSuccess();
                } else {
                    callback.onFailure(new Exception("Health check failed"));
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

    private interface HttpCallback  {

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

    public class Result {
        public String ruuid;
        public Map<String, Object> value;

        public Result(String ruuid, Map<String, Object> value) {
            this.ruuid = ruuid;
            this.value = value;
        }

        @Override
        public String toString() {
            return String.format(ruuid + ": " + value.toString());
        }
    }

    public class ResultSet {
        public String guuid;
        public List<Result> results;

        public ResultSet(String guuid) {
            this.guuid = guuid;
            this.results = new ArrayList<Result>();
        }

        public void addResult(Result result) {
            this.results.add(result);
        }

    }
}
