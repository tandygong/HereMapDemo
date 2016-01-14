package com.example.administrator.heredemo;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.here.android.mpa.ar.ARController;
import com.here.android.mpa.ar.ARController.Error;
import com.here.android.mpa.ar.ARIconObject;
import com.here.android.mpa.ar.CompositeFragment;
import com.here.android.mpa.common.GeoBoundingBox;
import com.here.android.mpa.common.GeoCoordinate;
import com.here.android.mpa.common.Image;
import com.here.android.mpa.common.OnEngineInitListener;
import com.here.android.mpa.mapping.Map;
import com.here.android.mpa.mapping.MapRoute;
import com.here.android.mpa.routing.RouteManager;
import com.here.android.mpa.routing.RouteOptions;
import com.here.android.mpa.routing.RoutePlan;
import com.here.android.mpa.routing.RouteResult;

import java.io.IOException;
import java.util.List;

public class BasicMapActivity extends Activity {
    private static String initial_scheme = "";
    private Map map = null;
    private CompositeFragment compositeFragment = null;
    private MapRoute mapRoute = null;
    private TextView textViewResult = null;

    // ARController is a facade for controlling LiveSight behavior
    private ARController arController;
    // buttons which will allow the user to start LiveSight and add objects
    private Button startButton;
    private Button stopButton;
    private Button toggleObjectButton;
    // the image we will display in LiveSight
    private Image image;
    // ARIconObject represents the image model which LiveSight accepts for display
    private ARIconObject arIconObject;
    private boolean objectAdded;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Search for the composite fragment to finish setup by calling init().
        compositeFragment =
                (CompositeFragment) getFragmentManager().findFragmentById(
                        R.id.compositefragment);
        compositeFragment.init(new OnEngineInitListener() {
            @Override
            public void onEngineInitializationCompleted(
                    OnEngineInitListener.Error error) {
                if (error == OnEngineInitListener.Error.NONE) {
                    // retrieve a reference of the map from the composite fragment
                    map = compositeFragment.getMap();
                    // Set the map center to the Vancouver Downtown region
                    map.setCenter(new GeoCoordinate(49.279483, -123.116906, 0.0),
                            Map.Animation.NONE);
                    // Set the map zoom level to the average between min and max
                    map.setZoomLevel((map.getMaxZoomLevel() +
                            map.getMinZoomLevel()) / 2);
                    // LiveSight setup should be done after fragment init is complete
                    setupLiveSight();
                } else {
                    System.out.println("ERROR: Cannot initialize Composite Fragment");
                }
            }
        });
        // hold references to the buttons for future use
        startButton = (Button) findViewById(R.id.startLiveSight);
        stopButton = (Button) findViewById(R.id.stopLiveSight);
        toggleObjectButton = (Button) findViewById(R.id.toggleObject);


   /*     MapMarker mm = new MapMarker();
        Image myImage=new Image();
        try {
            myImage.setImageResource(R.mipmap.test);
        } catch (IOException e) {
            e.printStackTrace();
        }

        mm.setIcon(myImage);
        mm.setCoordinate(new GeoCoordinate(52.53, 13.23));

        ClusterLayer cl = new ClusterLayer();
        cl.addMarker(mm);
        map.addClusterLayer(cl);*/
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

    private void onCompositeFragmentInitializationCompleted() {
        // retrieve a reference of the map from the map fragment
        map = compositeFragment.getMap();
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

    private RouteManager.Listener routeManagerListener =
            new RouteManager.Listener() {
                public void onCalculateRouteFinished(RouteManager.Error errorCode,
                                                     List<RouteResult> result) {

                    if (errorCode == RouteManager.Error.NONE &&
                            result.get(0).getRoute() != null) {
                        // create a map route object and place it on the map
                        mapRoute = new MapRoute(result.get(0).getRoute());
                        map.addMapObject(mapRoute);
                        // Get the bounding box containing the route and zoom in
                        GeoBoundingBox gbb = result.get(0).getRoute().getBoundingBox();
                        map.zoomTo(gbb, Map.Animation.NONE,
                                Map.MOVE_PRESERVE_ORIENTATION);
                        textViewResult.setText(
                                String.format("Route calculated with %d maneuvers.",
                                        result.get(0).getRoute().getManeuvers().size()));
                    } else {
                        textViewResult.setText(
                                String.format("Route calculation failed: %s",
                                        errorCode.toString()));
                    }
                }

                public void onProgress(int percentage) {
                    textViewResult.setText(
                            String.format("... %d percent done ...", percentage));
                }
            };


    // Functionality for taps of the "Get Directions" button
    public void getDirections(View view) {
        // 1. clear previous results
        textViewResult.setText("");
        if (map != null && mapRoute != null) {
            map.removeMapObject(mapRoute);
            mapRoute = null;
        }
        // 2. Initialize RouteManager
        RouteManager routeManager = new RouteManager();
        // 3. Select routing options via RoutingMode
        RoutePlan routePlan = new RoutePlan();
        RouteOptions routeOptions = new RouteOptions();
        routeOptions.setTransportMode(RouteOptions.TransportMode.CAR);
        routeOptions.setRouteType(RouteOptions.Type.FASTEST);
        routePlan.setRouteOptions(routeOptions);
        // 4. Select Waypoints for your routes
        // START: Burnaby
        routePlan.addWaypoint(new GeoCoordinate(49.1966286, -123.0053635));
        // END: YVR Airport
        routePlan.addWaypoint(new GeoCoordinate(49.1947289, -123.1762924));
        // 5. Retrieve Routing information via RouteManagerListener
        RouteManager.Error error =
                routeManager.calculateRoute(routePlan, routeManagerListener);
        if (error != RouteManager.Error.NONE) {
            Toast.makeText(getApplicationContext(),
                    "Route calculation failed with: " + error.toString(),
                    Toast.LENGTH_SHORT)
                    .show();
        }
    }


    private void setupLiveSight() {
// ARController should not be used until fragment init has completed
        arController = compositeFragment.getARController();
// tells LiveSight to display icons while viewing the map (pitch down)
        arController.setUseDownIconsOnMap(true);
// tells LiveSight to use a static mock location instead of the devices GPS fix
        arController.setAlternativeCenter(new GeoCoordinate(49.279483, -123.116906, 0.0));
    }

    public void startLiveSight(View view) {

        if (arController != null) {
// triggers the transition from Map mode to LiveSight mode
            Error error = arController.start();
            if (error == Error.NONE) {
                startButton.setVisibility(View.GONE);
                stopButton.setVisibility(View.VISIBLE);
            } else {
                Toast.makeText(getApplicationContext(),
                        "Error starting LiveSight: " + error.toString(), Toast.LENGTH_LONG);
            }
        }
    }

    public void stopLiveSight(View view) {
        if (arController != null) {
// exits LiveSight mode and returns to Map mode
            Error error = arController.stop(true);
            if (error == Error.NONE) {
                startButton.setVisibility(View.VISIBLE);
                stopButton.setVisibility(View.GONE);
            } else {
                Toast.makeText(getApplicationContext(),
                        "Error stopping LiveSight: " + error.toString(), Toast.LENGTH_LONG);
            }
        }
    }

    public void toggleObject(View view) {
        if (arController != null) {
            if (!objectAdded) {
                if (arIconObject == null) {
                    image = new com.here.android.mpa.common.Image();
                    try {
                        image.setImageResource(R.mipmap.test);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // creates a new icon object which uses the same image in up and down  views
                    arIconObject = new ARIconObject(new GeoCoordinate(49.276744,
                            -123.112049, 2.0),
                            (View) null, image);
                }
                // adds the icon object to LiveSight to be rendered
                arController.addARObject(arIconObject);
                objectAdded = true;
                toggleObjectButton.setText("Remove Object");
            } else {
                // removes the icon object from LiveSight, it will no longer be rendered
                arController.removeARObject(arIconObject);
                objectAdded = false;
                toggleObjectButton.setText("Add Object");
            }
        }
    }

}
