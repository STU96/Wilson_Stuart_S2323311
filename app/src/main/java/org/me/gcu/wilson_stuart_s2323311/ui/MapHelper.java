package org.me.gcu.wilson_stuart_s2323311.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import org.me.gcu.wilson_stuart_s2323311.model.LocationPoint;
import org.me.gcu.wilson_stuart_s2323311.model.Situation;

import java.util.HashMap;

// Draws situations on a Google Map: a coloured line from <from> to <to>, plus an icon marker.
// Used by both the full map screen and the small map on the detail screen.
public class MapHelper {

    public static final LatLng SCOTLAND = new LatLng(56.8, -4.2);
    private static final float MAX_ZOOM = 14f;

    private final Context context;
    private final HashMap<String, BitmapDescriptor> iconCache = new HashMap<>();

    public MapHelper(Context context) {
        this.context = context;
    }

    // Adds one situation to the map; returns the marker (or null if the situation has no coordinates)
    public Marker addSituation(GoogleMap map, Situation situation, LatLngBounds.Builder bounds) {
        LatLng start = toLatLng(situation.getLocation().getFrom());
        LatLng end = toLatLng(situation.getLocation().getTo());
        if (start == null) {
            start = end;
        }
        if (start == null) {
            return null;
        }
        int colour = SituationStyle.getColour(context, situation);
        if (end != null && situation.getLocation().isLine()) {
            // "from" and "to" define a line rather than a single point
            map.addPolyline(new PolylineOptions().add(start, end).color(colour).width(12f));
            bounds.include(end);
        }
        bounds.include(start);
        Marker marker = map.addMarker(new MarkerOptions()
                .position(start)
                .anchor(0.5f, 0.5f)
                .title(situation.getTypeLabel())
                .snippet(situation.getDescription())
                .icon(getMarkerIcon(situation)));
        if (marker != null) {
            marker.setTag(situation);   // so a tap on the info window knows which situation it is
        }
        return marker;
    }

    // Moves the camera so every drawn point is visible
    public void fitCamera(GoogleMap map, LatLngBounds.Builder builder, int count) {
        if (count == 0) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(SCOTLAND, 6f));
            return;
        }
        LatLngBounds bounds = builder.build();
        try {
            moveToBounds(map, bounds);
        } catch (IllegalStateException e) {
            // The map has not been laid out yet: wait until it has
            map.setOnMapLoadedCallback(() -> moveToBounds(map, bounds));
        }
    }

    private void moveToBounds(GoogleMap map, LatLngBounds bounds) {
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 120));
        if (map.getCameraPosition().zoom > MAX_ZOOM) {
            map.moveCamera(CameraUpdateFactory.zoomTo(MAX_ZOOM));   // a single point would zoom right in
        }
    }

    private LatLng toLatLng(LocationPoint point) {
        if (point == null || !point.hasCoordinates()) {
            return null;
        }
        return new LatLng(point.getLatitude(), point.getLongitude());
    }

    // Draws the situation's icon in white on a coloured circle; cached so each bitmap is made once
    private BitmapDescriptor getMarkerIcon(Situation situation) {
        int iconRes = SituationStyle.getIconRes(situation.getKind());
        int colour = SituationStyle.getColour(context, situation);
        String key = iconRes + "_" + colour;
        BitmapDescriptor cached = iconCache.get(key);
        if (cached != null) {
            return cached;
        }
        int size = (int) (34 * context.getResources().getDisplayMetrics().density);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint);           // white outline
        paint.setColor(colour);
        canvas.drawCircle(size / 2f, size / 2f, size / 2f - size / 14f, paint);
        Drawable icon = ContextCompat.getDrawable(context, iconRes);
        if (icon != null) {
            icon = icon.mutate();
            icon.setTint(Color.WHITE);
            int padding = size / 5;
            icon.setBounds(padding, padding, size - padding, size - padding);
            icon.draw(canvas);
        }
        BitmapDescriptor descriptor = BitmapDescriptorFactory.fromBitmap(bitmap);
        iconCache.put(key, descriptor);
        return descriptor;
    }
}
