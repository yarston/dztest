package ru.yagames.dztest;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.net.ssl.HttpsURLConnection;
//import com.

/**
 *
 * @author slava
 */
public class ImageProcessor {

    private static final Map<String, byte[]> cache = Collections.synchronizedMap(new LinkedHashMap<String, byte[]>(10, 1.5f, true));//Last argument true for LRU ordering
    private static final ExecutorService executor = Executors.newFixedThreadPool(5);
    private static final Handler handler = new Handler();
    private static final MemoryCache memoryCache = new MemoryCache();

    public static void load(String url, ImageView iv) {
        byte[] compressed = memoryCache.get(url);
        Bitmap bmp = null;
        if (compressed == null) {
            executor.submit(new PhotosLoader(iv, url));
        } else {
            bmp = BitmapFactory.decodeByteArray(compressed, 0, compressed.length);
        }
        Log.v("!", "imgurl=" + url);
        handler.post(new IVDisplayer(bmp, iv));
    }

    public static class PhotosLoader implements Runnable {

        private final ImageView iv;
        private final String url;

        PhotosLoader(ImageView image, String url) {
            this.iv = image;
            this.url = url;
        }

        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            Bitmap bmp = null;
            byte[] b = getImageCompressedBytes(url);
            Options opts = new Options();
            opts.inMutable = true;
            bmp = BitmapFactory.decodeByteArray(b, 0, b.length, opts);
            handler.post(new IVDisplayer(bmp, iv));
            memoryCache.put(url, b);
        }
    }

    static class IVDisplayer implements Runnable {

        private final Bitmap bmp;
        private final ImageView iv;

        public IVDisplayer(Bitmap bmp, ImageView iv) {
            this.bmp = bmp;
            this.iv = iv;
        }

        public void run() {
            iv.setImageBitmap(bmp);
        }
    }

    private static byte[] getImageCompressedBytes(String url) {
        //Log.v("!", "try image load from url " + url);
        try {
            HttpURLConnection http = null;
            URL imageUrl = new URL(url);
            if (imageUrl.getProtocol().toLowerCase().equals("https")) {
                HttpsURLConnection https = (HttpsURLConnection) imageUrl.openConnection();
                http = https;
            } else {
                http = (HttpURLConnection) imageUrl.openConnection();
            }
            InputStream is = http.getInputStream();
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int len = 0;
            while ((len = is.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            return byteBuffer.toByteArray();
        } catch (Throwable ex) {
            ex.printStackTrace();
            if (ex instanceof OutOfMemoryError) {
                //  memoryCache.clear();
            }
            return null;
        }
    }

    private static class MemoryCache {

        private long size = 0;//current allocated size
        private long limit;//max memory in bytes

        public MemoryCache() {
            //use 25% of available heap size
            limit = Runtime.getRuntime().maxMemory();
            //Log.v("!", "heap="+limit);
            limit /= 4;
        }

        public byte[] get(String id) {
            try {
                //if (!cache.containsKey(id)) {
                //    return null;
                //   }//NullPointerException sometimes happen here http://code.google.com/p/osmdroid/issues/detail?id=78 
                return cache.get(id);
            } catch (NullPointerException ex) {
                ex.printStackTrace();
                return null;
            }
        }

        public void put(String id, byte[] compresed) {
            try {
                if (cache.containsKey(id)) {
                    size -= cache.get(id).length;
                }
                cache.put(id, compresed);
                size += compresed.length;
                checkSize();
            } catch (Throwable th) {
                th.printStackTrace();
            }
        }

        private void checkSize() {
            if (size > limit) {
                Iterator<Map.Entry<String, byte[]>> iter = cache.entrySet().iterator();//least recently accessed item will be the first one iterated  
                while (iter.hasNext()) {
                    Map.Entry<String, byte[]> entry = iter.next();
                    size -= entry.getValue().length;
                    iter.remove();
                    if (size <= limit) {
                        break;
                    }
                }
            }
        }

        public void clear() {
            try {
                cache.clear();
                size = 0;
            } catch (NullPointerException ex) {
                ex.printStackTrace();
            }
        }
    }
}
