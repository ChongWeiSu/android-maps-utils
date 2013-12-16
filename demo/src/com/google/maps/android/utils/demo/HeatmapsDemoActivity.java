package com.google.maps.android.utils.demo;

import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.maps.model.TileProvider;
import com.google.maps.android.geometry.Bounds;
import com.google.maps.android.heatmaps.HeatmapConstants;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.HeatmapUtil;
import com.google.maps.android.heatmaps.LatLngWrapper;
import com.google.maps.android.quadtree.PointQuadTree;
import com.google.maps.android.utils.demo.model.MyItem;

import org.json.JSONException;

import java.io.InputStream;
import java.util.List;

public class HeatmapsDemoActivity extends BaseDemoActivity {

    /** where sydney is */
    private final LatLng SYDNEY = new LatLng(-33.865955, 151.195891);

    /** Quad tree of points*/
    private PointQuadTree mTree;

    @Override
    // TODO: move a lot of this into a nicer HeatmapLayer class?
    protected void startDemo() {
        //getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(SYDNEY, 16));

        getMap().moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(51.503186, -0.126446), 10));

        LatLngWrapper[] list = new LatLngWrapper[10];

        try {
            list = readItems();
        } catch (JSONException e) {
            Toast.makeText(this, "Problem reading list of markers.", Toast.LENGTH_LONG).show();
        }

        // Calculate appropriate quadtree bounds
        int minX = (int)list[0].getPoint().x;
        int maxX = (int)list[0].getPoint().x + 1;
        int minY = (int)list[0].getPoint().y;
        int maxY = (int)list[0].getPoint().y + 1;

        //TODO: Are int bounds accurate enough? (small heatmaps + max val calc?)
        for (LatLngWrapper l: list) {
            int x = (int)l.getPoint().x;
            int y = (int)l.getPoint().y;
            // Extend bounds if necessary
            if (x < minX) minX = x;
            if (x + 1 > maxX) maxX = x + 1;
            if (y < minY) minY = y;
            if (y + 1 > maxY) maxY = y + 1;
        }

        // Make the quad tree
        //Bounds treeBounds = new Bounds(230, 240, 150, 160);
        Bounds treeBounds = new Bounds(minX, maxX, minY, maxY);
        mTree = new PointQuadTree<LatLngWrapper>(treeBounds);

        // Add points to quad tree
        for (LatLngWrapper l: list) {
            mTree.add(l);
        }

        // Calculate reasonable maximum intensity for color scale (user can also specify)
        // Get screen dimensions
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int screenDim = dm.widthPixels > dm.heightPixels ? dm.widthPixels : dm.heightPixels;

        double maxIntensity = HeatmapUtil.getMaxVal(list, treeBounds,
                HeatmapConstants.DEFAULT_HEATMAP_RADIUS, screenDim);
        Log.e("MAX", "MaxIntensity = " + maxIntensity);

        // Create a heatmap tile provider, that will generate the overlay tiles
        TileProvider heatmapTileProvider = new HeatmapTileProvider(mTree, treeBounds,
                HeatmapConstants.DEFAULT_HEATMAP_RADIUS, HeatmapConstants.DEFAULT_HEATMAP_GRADIENT,
                HeatmapConstants.DEFAULT_HEATMAP_OPACITY, maxIntensity);
        // Add the tile overlay to the map
        getMap().addTileOverlay(new TileOverlayOptions().tileProvider(heatmapTileProvider));

        //draw marker where the stuff is supposed to be
        //getMap().addMarker(new MarkerOptions()
                //.position(SYDNEY));
    }


    // Copied from ClusteringDemoActivity
    private LatLngWrapper[] readItems() throws JSONException {
        InputStream inputStream = getResources().openRawResource(R.raw.radar_search);
        List<MyItem> items = new MyItemReader().read(inputStream);

        LatLngWrapper[] list = new LatLngWrapper[items.size()];
        int i;
        for (i = 0; i < items.size(); i++) {
            MyItem temp = items.get(i);
            list[i] = new LatLngWrapper(temp.getPosition());
        }
        return list;
    }

}
