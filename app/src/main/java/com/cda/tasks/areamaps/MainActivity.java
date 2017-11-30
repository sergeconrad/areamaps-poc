package com.cda.tasks.areamaps;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.CameraUpdateFactory;

import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.maps.android.data.kml.KmlLayer;
import com.google.maps.android.data.kml.KmlContainer;
import com.google.maps.android.data.kml.KmlPlacemark;
import com.google.maps.android.data.kml.KmlPolygon;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.List;

// ************* RELEASE NOTES ********************************
//
// * This is POC only, only minimal testing was done
//   made by Serge Conrad
//
// * GPS (polar) coordinates: longitude:X, latitude:Y
// * KML Point: longitude, latitude
//
// Libraries used:
// Google Maps Android API utility library
// 'com.google.maps.android:android-maps-utils:0.5+'
// https://github.com/googlemaps/android-maps-utils
// The lib. is developed and supported by Google.com
// so it's safe to use it in any production code
//
// It's used to retrieve polygon coordinates (bounds) from 'kml'
// and to get distance of a point to a line segment
//
// KML test data files are in 'raw' resources
// *************************************************************
//
//  Time spent
// **************************************************************
// 1. about 1 hour to set up dev. environment
// I used Android Studio 3.0 on Windows 10 with Visual Studio Emulator for Android
// The Emulator doesn't have Google Play Services installed initially so
// it took time to find workaround how to install the Services.
// 2. about 2.5 hours to get know how to use Google Maps (api keys and so on ..)
// its interfaces and to find library that I used in the project
// 3. about 1 hours to read about "Point in Polygon' problem and to see its different
// implementations
// 4. about 8 hours to coding and the same time to get some information in
// the 'Euclid geometry' field :)
//
// TO DOs
// ****************************************************************
// Almost no error handling (connection errors and so on ...) in the project
// It's just POC

public class MainActivity extends AppCompatActivity
        implements OnMapReadyCallback
        , GoogleMap.OnMapClickListener
{
    private GoogleMap mGoogleMap =  null;
    // Kml polygon boundaries
    private LatLng[] mPolyBounds;

    private Marker mCurrPosMarker = null;

    // flag showing which kml file was loaded
    // it's used with button to start reloading new kml file
    private  boolean mIsAllowedKmlLoaded = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get map fragment
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        // load map
        mapFragment.getMapAsync(this);

        // button to load kml
        final Button loadBtn = (Button) findViewById(R.id.loadbutton);
        loadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mGoogleMap != null) {
                    mGoogleMap.clear();

                    String newButtonText;
                    int fileResIdToLoad;
                    if(mIsAllowedKmlLoaded) {
                        fileResIdToLoad = R.raw.bad;
                        mIsAllowedKmlLoaded = false;
                        newButtonText = "Load 'Allowed'";
                    } else {
                        fileResIdToLoad = R.raw.allowed;
                        mIsAllowedKmlLoaded = true;
                        newButtonText = "Load 'Bad'";
                    }
                    loadBtn.setText(newButtonText);
                    loadAndDrawKmlFile(fileResIdToLoad);
                }
            }
        });
    }

    // -- OnMapReadyCallback impl --
    @Override
    public void onMapReady(GoogleMap googleMap) {
        // initialize google map
        mGoogleMap = googleMap;
        mGoogleMap.setOnMapClickListener(this);

        // enable zoom buttons
        mGoogleMap.getUiSettings().setZoomControlsEnabled(true);
        loadAndDrawKmlFile(R.raw.allowed);
        /*
        try {
            // create kml layer of *.kml file raw resource
            //KmlLayer kmlLayer = new KmlLayer(mGoogleMap, R.raw.allowed, getApplicationContext());
            KmlLayer kmlLayer = new KmlLayer(mGoogleMap, R.raw.allowed, getApplicationContext());
            // read polygon boundaries (coordinates) from the layer
            mPolyBounds = readPolygonBoundsFromKml(kmlLayer);
            // draw the polygon on the map
            drawPolygon(mPolyBounds);
            moveCameraToPolygon(mPolyBounds);


            //kmlLayer.addLayerToMap();
            //moveCameraToKml(kmlLayer);

            //kmlLayer.setOnFeatureClickListener(new KmlLayer.OnFeatureClickListener(){
            //    @Override
            //    public void onFeatureClick(Feature feature) {
            //        Toast.makeText(MainActivity.this,
            //                "You are inside the area", // + feature.getId(),
            //                Toast.LENGTH_SHORT).show();
            //    }
            //});
        }
        catch (IOException ex2) {
            String message = ex2.getMessage();
        }
        catch (XmlPullParserException ex) {
            int line = ex.getLineNumber();
        }
        */

        // done
        return;
    }

    // Map Click listener
    @Override
    public void onMapClick(LatLng latLng) {
        Log.d("Map Clicked ", latLng.toString());

        // TO DO : make Pip detection
        boolean isInside = GeometryUtils.isPointInsidePolygon(mPolyBounds, latLng);
        double distance = 0;

         String toastText = "You are ";
        // clear current and closest markers
        //
        if(mCurrPosMarker != null) {
            // remove previous position marker
            mCurrPosMarker.remove();
        }

        if(!isInside) {
            // outside polygon area click
            // get distance/point
            distance = GeometryUtils.calcPointToPolygonDistance(latLng, mPolyBounds);
            // prepare toast notification message
            toastText += "outside the area\n" + latLng.toString() + "\nShortest distance to the area is " + distance + " meters";
        } else {
            toastText += "inside the area\n" + latLng.toString();
        }

        // add marker at the click point
        MarkerOptions opt = new MarkerOptions().position(latLng);
        String title = !isInside ? String.valueOf(distance) + " m. away" : "you are here";
        opt.title(title);
        mCurrPosMarker = mGoogleMap.addMarker(opt);

        // finally show the notification message
        Toast.makeText(MainActivity.this, toastText, Toast.LENGTH_SHORT).show();

        // done
        return;
    }

    private void loadAndDrawKmlFile(int kmlResId) {
        try {
            //KmlLayer kmlLayer = new KmlLayer(mGoogleMap, R.raw.allowed, getApplicationContext());
            KmlLayer kmlLayer = new KmlLayer(mGoogleMap, kmlResId, getApplicationContext());
            // read polygon boundaries (coordinates) from the layer
            mPolyBounds = readPolygonBoundsFromKml(kmlLayer);
            // draw the polygon on the map
            drawPolygon(mPolyBounds);
            moveCameraToPolygon(mPolyBounds);
        }
        catch (IOException ex2) {
            String message = ex2.getMessage();
        }
        catch (XmlPullParserException ex) {
            int line = ex.getLineNumber();
        }
        return;
    }

    private LatLng[] readPolygonBoundsFromKml(KmlLayer kmlLayer) {

        LatLng[] boundsArray = null;
        // start reading
        try {
            // at start we have to add the layer to the map
            // to be able to get polygon boundaries, without that
            // code be;ow doesn't work
            kmlLayer.addLayerToMap();

            // get a polygon
            KmlContainer container = kmlLayer.getContainers().iterator().next();
            KmlPlacemark placemark = container.getPlacemarks().iterator().next();
            KmlPolygon polygon = (KmlPolygon) placemark.getGeometry();

            // get the polygon bounds
            List<LatLng> bounds = polygon.getOuterBoundaryCoordinates();
            boundsArray = new LatLng[bounds.size()];
            boundsArray = bounds.toArray(boundsArray);

            // cleanup
            // we'll draw polygon by hand to be able
            // to catch 'all surface map' clicks
            kmlLayer.removeLayerFromMap();
        }
        catch (IOException ioEx) {
        }
        catch (XmlPullParserException xmlEx) {
        }
        // done
        return boundsArray;
    }
    private void drawPolygon(LatLng[] bounds) {

        PolygonOptions options = new PolygonOptions();
        for(int i = 0; i < bounds.length; ++i) {
            options.add(bounds[i]);
        }

        Polygon polygon = mGoogleMap.addPolygon(options);

        // done
        return;
    }
    private void moveCameraToPolygon(LatLng[] bounds) {
        //Create LatLngBounds of the outer coordinates of the polygon
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng latLng : bounds) {
            builder.include(latLng);
        }
        // move camera
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), width/2, height/2, 1));

        // done
        return;
    }
}

