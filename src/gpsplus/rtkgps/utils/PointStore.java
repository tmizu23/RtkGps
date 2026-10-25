package gpsplus.rtkgps.utils;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import gpsplus.rtkgps.MainActivity;

/**
 * Surveyed points, kept across server sessions and app restarts in
 * points.geojson (with a points.csv copy) in the storage directory.
 * Both files are rewritten on each change so they stay valid if the app is killed.
 */
public class PointStore {

    private static final String TAG = PointStore.class.getSimpleName();
    private static final String BASE_NAME = "points";

    public static class Point {
        public final String name;
        public final double lat, lon, height;   // degrees, ellipsoidal height in metres
        public final double sdN, sdE, sdU;      // metres
        public final int nFix, nSat;
        public final long gpsWeek, gpsTow;

        public Point(String name, double lat, double lon, double height,
                     double sdN, double sdE, double sdU, int nFix, int nSat,
                     long gpsWeek, long gpsTow) {
            this.name = name;
            this.lat = lat;
            this.lon = lon;
            this.height = height;
            this.sdN = sdN;
            this.sdE = sdE;
            this.sdU = sdU;
            this.nFix = nFix;
            this.nSat = nSat;
            this.gpsWeek = gpsWeek;
            this.gpsTow = gpsTow;
        }
    }

    private static PointStore sInstance;

    private final File mDirectory;
    private final List<Point> mPoints = new ArrayList<Point>();
    private int mVersion = 0;

    public static synchronized PointStore get() {
        if (sInstance == null) {
            sInstance = new PointStore(MainActivity.getFileStorageDirectory());
        }
        return sInstance;
    }

    private PointStore(File directory) {
        mDirectory = directory;
        load();
    }

    private File getFile(String extension) {
        return new File(mDirectory, BASE_NAME + extension);
    }

    public synchronized List<Point> getPoints() {
        return new ArrayList<Point>(mPoints);
    }

    /** Incremented on each change, so views know when to redraw */
    public synchronized int getVersion() {
        return mVersion;
    }

    public synchronized String getLastName() {
        return mPoints.isEmpty() ? null : mPoints.get(mPoints.size() - 1).name;
    }

    public synchronized void add(Point point) {
        mPoints.add(point);
        changed();
    }

    public synchronized void remove(Point point) {
        if (mPoints.remove(point)) changed();
    }

    /** Keeps the current files under a dated name, then starts an empty list */
    public synchronized void clear() {
        if (mPoints.isEmpty()) return;
        String suffix = new SimpleDateFormat("_yyyyMMdd_HHmmss", Locale.ROOT).format(new Date());
        for (String extension : new String[] {".geojson", ".csv"}) {
            File f = getFile(extension);
            if (f.exists() && !f.renameTo(new File(mDirectory, BASE_NAME + suffix + extension))) {
                Log.e(TAG, "Cannot archive " + f);
            }
        }
        mPoints.clear();
        changed();
    }

    private void changed() {
        mVersion++;
        save();
    }

    private void load() {
        File file = getFile(".geojson");
        if (!file.exists()) return;
        try {
            InputStream in = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            int n = 0;
            while (n < data.length) {
                int r = in.read(data, n, data.length - n);
                if (r < 0) break;
                n += r;
            }
            in.close();
            JSONArray features = new JSONObject(new String(data, 0, n, "UTF-8")).getJSONArray("features");
            for (int i = 0; i < features.length(); i++) {
                JSONObject feature = features.getJSONObject(i);
                JSONArray c = feature.getJSONObject("geometry").getJSONArray("coordinates");
                JSONObject p = feature.getJSONObject("properties");
                mPoints.add(new Point(p.optString("name"), c.getDouble(1), c.getDouble(0),
                        p.optDouble("ellipsoidal_height_m", c.optDouble(2, 0.0)),
                        p.optDouble("sdN_m", 0.0), p.optDouble("sdE_m", 0.0), p.optDouble("sdU_m", 0.0),
                        p.optInt("nFix"), p.optInt("nSat"), p.optLong("gps_week"), p.optLong("gps_tow")));
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Cannot read " + file, e);
        }
    }

    private void save() {
        StringBuilder json = new StringBuilder("{\"type\":\"FeatureCollection\",\"features\":[\n");
        StringBuilder csv = new StringBuilder(
                "name,lat_deg,lon_deg,ellipsoidal_height_m,sdN_m,sdE_m,sdU_m,nFix,nSat,gps_week,gps_tow\n");
        for (int i = 0; i < mPoints.size(); i++) {
            Point p = mPoints.get(i);
            json.append(String.format(Locale.ROOT,
                    "{\"type\":\"Feature\","
                    + "\"geometry\":{\"type\":\"Point\",\"coordinates\":[%.9f,%.9f,%.4f]},"
                    + "\"properties\":{\"name\":%s,\"ellipsoidal_height_m\":%.4f,"
                    + "\"sdN_m\":%.4f,\"sdE_m\":%.4f,\"sdU_m\":%.4f,"
                    + "\"nFix\":%d,\"nSat\":%d,\"gps_week\":%d,\"gps_tow\":%d}}",
                    p.lon, p.lat, p.height, JSONObject.quote(p.name), p.height,
                    p.sdN, p.sdE, p.sdU, p.nFix, p.nSat, p.gpsWeek, p.gpsTow));
            json.append(i < mPoints.size() - 1 ? ",\n" : "\n");
            csv.append(String.format(Locale.ROOT, "%s,%.9f,%.9f,%.4f,%.4f,%.4f,%.4f,%d,%d,%d,%d\n",
                    p.name.replace(',', ' '), p.lat, p.lon, p.height, p.sdN, p.sdE, p.sdU,
                    p.nFix, p.nSat, p.gpsWeek, p.gpsTow));
        }
        json.append("]}\n");
        write(getFile(".geojson"), json.toString());
        write(getFile(".csv"), csv.toString());
    }

    private static void write(File file, String content) {
        try {
            Writer out = new OutputStreamWriter(new FileOutputStream(file), "UTF-8");
            out.write(content);
            out.close();
        } catch (IOException e) {
            Log.e(TAG, "Cannot write " + file, e);
        }
    }
}
