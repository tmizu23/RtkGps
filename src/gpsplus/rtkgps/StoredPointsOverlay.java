package gpsplus.rtkgps;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the points stored in the current session with their names,
 * so the surveyor can see which ground control points are already done.
 */
public class StoredPointsOverlay extends Overlay {

    private static final int POINT_COLOR = Color.rgb(255, 64, 64);
    private static final int OUTLINE_COLOR = Color.WHITE;
    private static final int TEXT_COLOR = Color.WHITE;
    private static final int TEXT_HALO_COLOR = Color.BLACK;

    private final float mDensity;
    private final Paint mPointPaint, mOutlinePaint, mTextPaint, mTextHaloPaint;
    private List<RtkNaviService.StoredPoint> mPoints = new ArrayList<RtkNaviService.StoredPoint>();
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

    /** @return true if the points changed and the map needs redrawing */
    public boolean setPoints(List<RtkNaviService.StoredPoint> points) {
        final boolean changed = points.size() != mPoints.size();
        mPoints = points;
        return changed;
    }

    @Override
    public void draw(Canvas canvas, MapView osmv, boolean shadow) {
        if (shadow || mPoints.isEmpty()) return;

        final Projection pj = osmv.getProjection();
        final float radius = 6.0f * mDensity;

        for (RtkNaviService.StoredPoint p : mPoints) {
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
