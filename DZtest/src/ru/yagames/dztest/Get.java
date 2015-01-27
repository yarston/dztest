package ru.yagames.dztest;

import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.GZIPInputStream;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author slava
 */
public class Get {

    private String url;
    private final HashMap<String, String> param = new HashMap<String, String>();
    private ResponseListener rl = null;
    private ConnectionErrorListener cel = null;
    private String line;
    private int errCode = 0;
    private JSONObject jObj;
    private JSONArray jArr;
    public int tag;
    private AsyncTask at;
    private Timer timer = new Timer();
    public long timeOut = 30000l;
    private final static int ERRCODE_BADRESPONSE = 1;
    private final static int ERRCODE_NOCONNECTION = 2;
    private String pass;
    private String username;
    private String host;

    public Get(final String url) {
        this.url = url;
    }

    public void setAuthParam(String username, String pass) {
        this.username = username;
        this.pass = pass;
    }

    public void reject() {
        if (at != null) {
            cel = null;
            rl = null;
            at.cancel(true);
        }
        timer.cancel();
    }

    public void execute() {
        boolean init = false;
        if (timeOut > 0) {
            timer.schedule(new TimerTask() {

                @Override
                public void run() {
                    reject();
                }
            }, timeOut);
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(url);
        for (HashMap.Entry<String, String> entry : param.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            sb.append(init ? "&" : "?");
            sb.append(key);
            sb.append("=");
            sb.append(value);
            init = true;
        }
        Log.v("!", "url=" + sb.toString());
        at = new AsyncTask() {

            @Override
            protected Object doInBackground(Object... arg0) {
                HttpGet httpget = new HttpGet();
                final String basicAuth = "Basic " + Base64.encodeToString((username + ":" + pass).getBytes(), Base64.NO_WRAP);
                httpget.setHeader("Authorization", basicAuth);
                httpget.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_7_3; en_US) AppleWebKit/536.26 (KHTML, like Gecko) Version/6.0 Safari/8536.25");
                try {
                    httpget.setURI(new URI(sb.toString()));
                    HttpParams httpParameters = new BasicHttpParams();
                    HttpConnectionParams.setConnectionTimeout(httpParameters, 10000);
                    HttpConnectionParams.setSoTimeout(httpParameters, 10000);
                    DefaultHttpClient dhc = new DefaultHttpClient(httpParameters);
                    if (username != null && pass != null) {
                        dhc.getCredentialsProvider().setCredentials(new AuthScope(host, 443), new UsernamePasswordCredentials(username, pass));
                    }
                    Log.v("!", "execute");
                    HttpResponse response = dhc.execute(httpget);
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
                        sb.setLength(0);
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                        }
                        line = sb.toString();
                        sb.setLength(0);
                        Log.v("!", "line=" + line);
                        /*if (rl != null) {
                         rl.onResponseEvent(line);
                         }*/
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                    errCode = ERRCODE_NOCONNECTION;
                    Log.v("!", "err");
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object result) {
                timer.cancel();
                if (errCode == 0) {
                    if (rl != null & line != null) {
                        rl.onResponseEvent(line);
                    }
                } else if (cel != null) {
                    cel.onConnectionError(errCode);
                }
            }
        };
        at.execute();
    }

    public void setResponseListener(ResponseListener rl) {
        this.rl = rl;
    }

    public void setConnectionErrorListener(ConnectionErrorListener cel) {
        this.cel = cel;
    }

    public void put(String key, String value) {
        param.put(key, value);
    }

    public void put(String key, long value) {
        param.put(key, String.valueOf(value));
    }

    public void put(String key, int value) {
        param.put(key, String.valueOf(value));
    }

}
