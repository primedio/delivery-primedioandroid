package io.primed.primedioandroid;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import okio.BufferedSink;

public class PrimedIO {

    public final OkHttpClient client = new OkHttpClient();
    private Request request;
    private String urlPrimedIO;
    private String public_key;
    private String nonce;
    private String sha512_signature;


    public PrimedIO(String publicKey, String secretKey, String url) {
        String nonce = String.valueOf(new Date().getTime());

        String prepSignature = publicKey + secretKey + nonce;
        String signature = this.createSHA512(prepSignature);

        this.urlPrimedIO = url;
        this.public_key = publicKey;
        this.sha512_signature = signature;
        this.nonce = nonce;
    }

    private String createSHA512(String input) {

        String generatedPassword = null;

        try {
            MessageDigest sh = MessageDigest.getInstance("SHA-512");
            sh.update(input.getBytes());
            StringBuffer sb = new StringBuffer();
            for (byte b : sh.digest()) sb.append(String.format("%1$02x", 0xff & b));
            generatedPassword = sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        return generatedPassword;

    }

    private void get(String url, final HttpCallback cb) {
        Request request = new Request.Builder()
                .url(url)
                .method("GET", new RequestBody() {
                    // don't care much about request body
                    @Override
                    public MediaType contentType() {
                        return null;
                    }

                    @Override
                    public void writeTo(BufferedSink sink) throws IOException {

                    }
                })
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

    private void post(String url, Map<String, String> params, final HttpCallback cb) {
        RequestBody formBody = new FormEncodingBuilder()
                .add("search", "Jurassic Park")
                .build();

        Request request = new Request.Builder()
                .url(url)
                .method("POST",formBody )
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

    public void convert(String ruuid, Map<String, String> data) {
       this.post("url", new HashMap(), new PrimedIO.HttpCallback() {
           @Override
           public void onFailure(Response response, Throwable throwable) {

           }

           @Override
           public void onSuccess(Response response) {

           }
       });
    }

    public void personalize(String campain, Map<String, String> signals, int limit) {

        String signalsString = signals.toString();

        String generateURL = "/api/v1/personalise?";
        generateURL += "campaign=" + campain;
        generateURL += "&limit=" + limit;
        generateURL += "&signals=" + signalsString;

        this.get(generateURL, new PrimedIO.HttpCallback() {
            @Override
            public void onFailure(Response response, Throwable throwable) {

            }

            @Override
            public void onSuccess(Response response) {

            }
        });

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
