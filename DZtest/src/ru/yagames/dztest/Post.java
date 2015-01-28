package ru.yagames.dztest;

import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.json.JSONObject;

/**
 *
 * @author slava
 */
public class Post {

    private final String url;
    private final HashMap<String, Object> param = new HashMap<String, Object>();
    private ResponseListener rl = null;
    private String line;
    private String pass;
    private String username;

    public Post(final String url) {
        this.url = url;
    }

    public void setAuthParam(String username, String pass) {
        this.username = username;
        this.pass = pass;
    }

    public void put(String s, Object o) {
        param.put(s, o);
    }

    public void setResponseListener(ResponseListener rl) {
        this.rl = rl;
    }

    public void execute() {
        new AsyncTask() {
            boolean success = false;

            @Override
            protected Object doInBackground(Object... arg0) {
                final String basicAuth = "Basic " + Base64.encodeToString((username + ":" + pass).getBytes(), Base64.NO_WRAP);
                try {
                    HttpPost httppost = new HttpPost();
                    httppost.setHeader("Authorization", basicAuth);
                    httppost.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_7_3; en_US) AppleWebKit/536.26 (KHTML, like Gecko) Version/6.0 Safari/8536.25");
                    httppost.setURI(new URI(url));
                    JSONObject json = new JSONObject();
                    json.put("note", "test");
                    StringEntity se = new StringEntity( json.toString());  
                    se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                    httppost.setEntity(se);
                    HttpParams httpParameters = new BasicHttpParams();
                    HttpConnectionParams.setConnectionTimeout(httpParameters, 10000);
                    HttpConnectionParams.setSoTimeout(httpParameters, 10000);
                    DefaultHttpClient dhc = new DefaultHttpClient(httpParameters);
                    Log.v("!", "execute");
                    HttpResponse response = dhc.execute(httppost);
                    Log.v("!", "statuscode=" + response.getStatusLine());
                    if (rl != null) {
                        InputStream is = response.getEntity().getContent();
                        Header contentEncoding = response.getFirstHeader("Content-Encoding");
                        if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
                            is = new GZIPInputStream(is);
                        }
                        line = null;
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                        //Log.v("!", "line is resived.");
                        final StringBuilder sb = new StringBuilder();
                        sb.setLength(0);
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        line = sb.toString();
                        sb.setLength(0);
                        Log.v("!", "line=" + line);
                    }
                   
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object result) {
                if (rl != null) {
                    //Log.v("!", "onResponseEvent line: " + line);
                    rl.onResponseEvent(line);
                }
            }
        }.execute();
    }
}
