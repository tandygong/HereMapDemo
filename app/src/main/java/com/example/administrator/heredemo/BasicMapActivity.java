package com.example.administrator.heredemo;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapFragment;

import java.util.List;

public class BasicMapActivity extends Activity {
    private static String initial_scheme = "";
    private Map map = null;
    private MapFragment mapFragment = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Search for the map fragment to finish setup by calling init().
        mapFragment = (MapFragment) getFragmentManager().findFragmentById(
                R.id.mapfragment);
        mapFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(
                    Error error) {
                if (error == Error.NONE) {
                    onMapFragmentInitializationCompleted();
                } else {
                    Log.e("error:", "ERROR: Cannot initialize Map Fragment: " + error.toString());
                }
            }
        });
    }

    public void goHome(View view) {
        if (map != null) {
            // Change map view to "home" coordinate and zoom level, plus eliminate any rotation or tilt
            map.setCenter(new GeoCoordinate(49.196261, -123.004773, 0.0),
                    Map.Animation.NONE);
            map.setZoomLevel((map.getMaxZoomLevel() + map.getMinZoomLevel()) / 2);
            map.setOrientation(0);
            map.setTilt(0);
            // Reset the map scheme to the initial scheme
            map.setMapScheme(initial_scheme);
            if (!initial_scheme.isEmpty()) {
                map.setMapScheme(initial_scheme);
            }
        }
    }

    private void onMapFragmentInitializationCompleted() {
        // retrieve a reference of the map from the map fragment
        map = mapFragment.getMap();
        // Set the map center coordinate to the Vancouver region (no animation)
        map.setCenter(new GeoCoordinate(49.196261, -123.004773, 0.0),
                Map.Animation.NONE);
        // Set the map zoom level to the average between min and max (no// animation)
        map.setZoomLevel((map.getMaxZoomLevel() + map.getMinZoomLevel()) / 2);
    }

    public void changeScheme(View view) {
        if (map != null) {
            // Local variable representing the current map scheme
            String current = map.getMapScheme();
            // Local array containing string values of available map schemes
            List<String> schemes = map.getMapSchemes();
            // Local variable representing the number of available map schemes
            int total = map.getMapSchemes().size();
            if (initial_scheme.isEmpty()) {
                //save the initial scheme
                initial_scheme = current;
            }
            // If the current scheme is the last element in the array, reset to
            // the scheme at array index 0
            if (schemes.get(total - 1).equals(current))
                map.setMapScheme(schemes.get(0));
            else {
                // If the current scheme is any other element, set to the next
                // scheme in the array
                for (int count = 0; count < total - 1; count++) {
                    if (schemes.get(count).equals(current))
                        map.setMapScheme(schemes.get(count + 1));
                }
            }
        }
    }


}
