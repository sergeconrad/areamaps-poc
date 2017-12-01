package com.cda.tasks.areamaps;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;
import com.google.maps.android.SphericalUtil;

import java.util.List;

// Point in Polygon (PIP) and distances Helper
public class GeometryUtils
{
    public static LatLng IntersectPoint = null;

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
    //static double calcPointToPolygonDistance(LatLng point, LatLng[] bounds) {
    static DistancePoint calcPointToPolygonDistance(LatLng point, LatLng[] bounds) {
        // calculated distance to return
        double distance = -1;
        double distance2 = -1; // for usage in test

        LatLng p1 = null, p2 = null;

        if (point == null || bounds == null) {
            //return -1; // error case throw exception
            return null;
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
            // calculate shortest distance to current line
            // to find the nearest segment line
            double currentDistance = PolyUtil.distanceToLine(point, start, bounds[endPointIndex]);

            if (distance == -1 || currentDistance < distance) {
                distance = currentDistance;
                distance2 = currentDistance;
                p1 = start;
                p2 = bounds[endPointIndex];
            }
        }

        // locate the closest point on the segment line
        LatLng closestPoint = null;
        if(p1 != null && p2 != null) {
            LatLng closestPoint2 = locateClosestPointLine(point, p1, p2);
            closestPoint = locateClosestPointOnSegment(point, p1, p2);
            Log.d("DIFFERENCE LAT/LNG : ",
                    Double.toString(Math.abs(closestPoint2.latitude - closestPoint.latitude)) + " : "
            + Double.toString(Math.abs(closestPoint2.longitude - closestPoint.longitude)));
        }

        // this call provide more precise results than 'PolyUtil.distanceToLine' called above
        // so use it ???,
        // it tested on 'BAD' sample data -> results exactly the same that we can get
        // using 'http://maps.google.com' measuring distance between two points
        distance = SphericalUtil.computeDistanceBetween(point, closestPoint);
        Log.d("DIFFERENCE DISTANCE : ", Double.toString(Math.abs(distance2 - distance)));

        //return distance;
        return new DistancePoint(distance, closestPoint);
    }

    // these functions below returns equals results (almost)
    //
    // Locate the closest distance point on a line from a given source point
    // https://github.com/googlemaps/android-maps-utils/blob/master/library/src/com/google/maps/android/PolyUtil.java
    //
    static public LatLng locateClosestPointLine(final LatLng p, final LatLng start, final LatLng end) {
        if (start.equals(end)) {
            return start;
        }

        final double plat = Math.toRadians(p.latitude);
        final double plng = Math.toRadians(p.longitude);
        final double startlat = Math.toRadians(start.latitude);
        final double startlng = Math.toRadians(start.longitude);
        final double endlat = Math.toRadians(end.latitude);
        final double endlng = Math.toRadians(end.longitude);

        final double deltalat = endlat - startlat;
        final double deltalng = endlng - startlng;

        final double u = ((plat - startlat) * deltalat + (plng - startlng) * deltalng)
                / (deltalat * deltalat + deltalng * deltalng);

        if (u <= 0.0) {
            return start;
        }
        if (u >= 1.0) {
            return end;
        }
        LatLng closestPoint = new LatLng(start.latitude + (u * (end.latitude - start.latitude)),
                start.longitude + (u * (end.longitude - start.longitude)));
        //
        return closestPoint;
    }
    //
    // based on http://geomalgorithms.com/a02-_lines.html (C++)
    //
    public static LatLng locateClosestPointOnSegment(LatLng p, LatLng p1, LatLng p2) {
        LatLng found = null;

        LatLng v = new LatLng(p2.latitude - p1.latitude, p2.longitude - p1.longitude);
        LatLng w = new LatLng(p.latitude - p1.latitude, p.longitude - p1.longitude);

        double c1 = dot(w,v);
        if(c1 < 0.0) {
            return found = p1;
        }

        double c2 = dot(v,v);
        if(c2 <= c1) {
            return found = p2;
        }

        double b = c1 / c2;
        LatLng x = new LatLng(b*v.latitude, b*v.longitude);
        found = new LatLng(p1.latitude + x.latitude, p1.longitude + x.longitude);

        // done
        return found;
    }

    // helpers
    private static double dot(LatLng p1, LatLng p2) {
        return p1.longitude * p2.longitude + p1.latitude * p2.latitude;
    }
}

// simple stupid pair impl.
class DistancePoint
{
    public double Distance;
    public LatLng Point;

    public DistancePoint(double distance, LatLng point) {
        Distance = distance;
        Point = point;
    }
}

