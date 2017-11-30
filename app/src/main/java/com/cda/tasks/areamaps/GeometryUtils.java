package com.cda.tasks.areamaps;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;

import java.util.List;

// Point in Polygon (PIP) and distances Helper
public class GeometryUtils
{
    // Based on https://wrf.ecse.rpi.edu//Research/Short_Notes/pnpoly.html
    // Running semi-infinite ray horizontally (increasing x, fixed y) out from the test point,
    // and count how many edges it crosses
    // At each crossing, the ray switches between inside and outside (Jordan curve theorem)
    // * GPS (polar) coordinates: longitude:X, latitude:Y
    // return true - point is inside, false - outside
    //
    static boolean isPointInsidePolygon(LatLng[] bounds, LatLng point) {
        boolean isInside = false;

        for (int i = 0, j = bounds.length - 1; i < bounds.length; j = i++) {
            if ((bounds[i].latitude > point.latitude) != (bounds[j].latitude > point.latitude)
                    &&
                    (point.longitude < (bounds[j].longitude - bounds[i].longitude)
                            * (point.latitude - bounds[i].latitude)
                            / (bounds[j].latitude-bounds[i].latitude)
                            + bounds[i].longitude)) {
                isInside = !isInside;
            }
        }
        // done
        return isInside;
    }

    // returns shortest distance to polygon in meters
    static double calcPointToPolygonDistance(LatLng point, LatLng[] bounds) {
        // calculated distance to return
        double distance = -1;

        if (point == null || bounds == null) {
            return -1; // error case throw exception
        }

        // iterate all polygon lines
        for (int i = 0; i < bounds.length; i++) {
            // start point
            LatLng start = bounds[i];

            // end point array index
            int endPointIndex = i + 1;
            if (endPointIndex >= bounds.length) {
                endPointIndex = 0;
            }
            // calc a distance to current line
            double currentDistance = PolyUtil.distanceToLine(point, start, bounds[endPointIndex]);

            if (distance == -1 || currentDistance < distance) {
                distance = currentDistance;
            }
        }
        // done
        return distance;
    }
}
