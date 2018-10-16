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
        void onSuccess(String response);
        void onFailure();
    }

    private static Primed sSoleInstance;

    public final OkHttpClient client = new OkHttpClient();
    public Boolean primedTrackerAvailable;
    private String urlPrimedIO;
    private String public_key;
    private String nonce;
    private String sha512_signature;


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
        String nonce = String.valueOf(System.currentTimeMillis() / 1000l);

        String prepSignature = publicKey + secretKey + nonce;
        String signature = Primed.createSHA512(prepSignature);

        this.urlPrimedIO = url;
        this.public_key = publicKey;
        this.sha512_signature = signature;
        this.nonce = nonce;
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
        Request request = new Request.Builder()
                .url(url)
                .method("GET", null)
                .addHeader("X-Authorization-Key", this.public_key)
                .addHeader("X-Authorization-Signature", this.sha512_signature)
                .addHeader("X-Authorization-Nonce", this.nonce)
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
                .addHeader("X-Authorization-Signature", this.sha512_signature)
                .addHeader("X-Authorization-Nonce", this.nonce)
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

    public void convert(String ruuid) {
        this.convert(ruuid);
    }
    public void convert(String ruuid, Map<String, Object> data) {

        String generateURL = this.urlPrimedIO + "/api/v1/conversion/" + ruuid;

       this.post(generateURL, data, new Primed.HttpCallback() {
           @Override
           public void onFailure(Response response, Throwable throwable) {
               //do nothing?
           }

           @Override
           public void onSuccess(Response response) {
//               String respBody = null;
//               if (response.body() != null) {
//                   try {
//                       respBody = response.body().string();
//                   } catch (IOException e) {
//                       //throw new ApiException(response.message(), e, response.code(), response.headers().toMultimap());
//                   }
//               }
               //do nothing?
           }
       });
    }

    public void personalise(String campaign, Map<String, Object> signals, int limit, String abVariantLabel, final PrimedCallback callback) {

        String signalsString = URLEncoder.encode(this.toJSONString(signals));

        String generateURL = this.urlPrimedIO + "/api/v1/personalise?";
        generateURL += "campaign=" + campaign;
        generateURL += "&limit=" + limit;
        generateURL += "&abvariant=" + abVariantLabel;
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

                        Map<String, Object> params = new HashMap<String, Object>();
                        params.put("guuid", guuid);

                        GsonBuilder gsonMapBuilder = new GsonBuilder();
                        Gson gsonObject = gsonMapBuilder.create();
                        String jsonResponse = gsonObject.toJson(params);

                        event.response = jsonResponse;
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

                callback.onSuccess(respBody);
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
                callback.onSuccess(respBody);
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
