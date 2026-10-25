package gpsplus.rtkgps.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Stored points of a session as a GeoJSON FeatureCollection.
 * The whole file is rewritten on each point so it stays valid if the app is killed.
 */
public class GeoJsonPoints {
    private final File mFile;
    private final List<String> mFeatures = new ArrayList<String>();

    public GeoJsonPoints(String directory, String fileName) {
        mFile = new File(directory, fileName);
    }

    public boolean addPoint(String name, double lon, double lat, double z, double sdN, double sdE, double sdU,
                            long gps_week, long gps_tow, int nSat, int nFix)
    {
        mFeatures.add(String.format(Locale.ROOT,
                "{\"type\":\"Feature\","
                + "\"geometry\":{\"type\":\"Point\",\"coordinates\":[%.9f,%.9f,%.4f]},"
                + "\"properties\":{\"name\":\"%s\",\"ellipsoidal_height_m\":%.4f,"
                + "\"sdN_m\":%.4f,\"sdE_m\":%.4f,\"sdU_m\":%.4f,"
                + "\"nFix\":%d,\"nSat\":%d,\"gps_week\":%d,\"gps_tow\":%d}}",
                lon, lat, z, escape(name), z, sdN, sdE, sdU, nFix, nSat, gps_week, gps_tow));

        StringBuilder json = new StringBuilder("{\"type\":\"FeatureCollection\",\"features\":[\n");
        for (int i = 0; i < mFeatures.size(); i++) {
            json.append(mFeatures.get(i)).append(i < mFeatures.size() - 1 ? ",\n" : "\n");
        }
        json.append("]}\n");

        try {
            Writer out = new OutputStreamWriter(new FileOutputStream(mFile), "UTF-8");
            out.write(json.toString());
            out.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') sb.append('\\').append(c);
            else if (c < 0x20) sb.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
            else sb.append(c);
        }
        return sb.toString();
    }
}
