package gpsplus.rtkgps;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.view.MotionEvent;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

import gpsplus.rtkgps.utils.PointStore;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the surveyed points with their names, so the surveyor can see
 * which ground control points are already done. A long press selects a point.
 */
public class StoredPointsOverlay extends Overlay {

    private static final int POINT_COLOR = Color.rgb(255, 64, 64);
    private static final int OUTLINE_COLOR = Color.WHITE;
    private static final int TEXT_COLOR = Color.WHITE;
    private static final int TEXT_HALO_COLOR = Color.BLACK;

    private final float mDensity;
    private final Paint mPointPaint, mOutlinePaint, mTextPaint, mTextHaloPaint;
    private static final float TOUCH_RADIUS_DP = 24.0f;

    public interface OnPointLongPressListener {
        void onPointLongPress(PointStore.Point point);
    }

    private List<PointStore.Point> mPoints = new ArrayList<PointStore.Point>();
    private int mVersion = -1;
    private OnPointLongPressListener mListener;
    private final Point mScreenPoint = new Point();

    public StoredPointsOverlay(float density) {
        super();
        mDensity = density;

        mPointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPointPaint.setColor(POINT_COLOR);
        mPointPaint.setStyle(Paint.Style.FILL);

        mOutlinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mOutlinePaint.setColor(OUTLINE_COLOR);
        mOutlinePaint.setStyle(Paint.Style.STROKE);
        mOutlinePaint.setStrokeWidth(2.0f * density);

        mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(TEXT_COLOR);
        mTextPaint.setTextSize(14.0f * density);

        mTextHaloPaint = new Paint(mTextPaint);
        mTextHaloPaint.setColor(TEXT_HALO_COLOR);
        mTextHaloPaint.setStyle(Paint.Style.STROKE);
        mTextHaloPaint.setStrokeWidth(3.0f * density);
    }

    public void setOnPointLongPressListener(OnPointLongPressListener listener) {
        mListener = listener;
    }

    /** Reloads the points from the store; @return true if they changed and the map needs redrawing */
    public boolean update() {
        final PointStore store = PointStore.get();
        final int version = store.getVersion();
        if (version == mVersion) return false;
        mVersion = version;
        mPoints = store.getPoints();
        return true;
    }

    @Override
    public boolean onLongPress(MotionEvent e, MapView mapView) {
        if (mListener == null) return false;
        final Projection pj = mapView.getProjection();
        PointStore.Point nearest = null;
        double nearestDist = TOUCH_RADIUS_DP * mDensity;
        for (PointStore.Point p : mPoints) {
            pj.toPixels(new GeoPoint(p.lat, p.lon), mScreenPoint);
            final double dist = Math.hypot(mScreenPoint.x - e.getX(), mScreenPoint.y - e.getY());
            if (dist <= nearestDist) {
                nearest = p;
                nearestDist = dist;
            }
        }
        if (nearest == null) return false;
        mListener.onPointLongPress(nearest);
        return true;
    }

    @Override
    public void draw(Canvas canvas, MapView osmv, boolean shadow) {
        if (shadow || mPoints.isEmpty()) return;

        final Projection pj = osmv.getProjection();
        final float radius = 6.0f * mDensity;

        for (PointStore.Point p : mPoints) {
            pj.toPixels(new GeoPoint(p.lat, p.lon), mScreenPoint);
            canvas.drawCircle(mScreenPoint.x, mScreenPoint.y, radius, mPointPaint);
            canvas.drawCircle(mScreenPoint.x, mScreenPoint.y, radius, mOutlinePaint);

            final float textX = mScreenPoint.x + radius + 3.0f * mDensity;
            final float textY = mScreenPoint.y + mTextPaint.getTextSize() / 3;
            canvas.drawText(p.name, textX, textY, mTextHaloPaint);
            canvas.drawText(p.name, textX, textY, mTextPaint);
        }
    }
}
