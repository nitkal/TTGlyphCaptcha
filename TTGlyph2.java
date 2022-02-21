package com.sun.captcha;

import java.awt.geom.Point2D;
import java.util.*;


public class TTGlyph2 {
    public final String name;
    public final double minX;
    public final double minY;
    public final double maxX;
    public final double maxY;
    public final List<Contour> contours;
    public final Point2D minXPoint;
    public final Point2D maxXPoint2D;
    //public final Map<ApproximateDouble, List<Double>> yxMap;


    public TTGlyph2(String name, double minx, double miny, double maxx, double maxy, List<Contour> contours, Point2D minX, Point2D maxX) {
        this.name = name;
        this.minX = minx;
        this.maxX = maxx;
        this.minY = miny;
        this.maxY = maxy;
        this.contours = contours;
        this.minXPoint = minX;
        this.maxXPoint2D = maxX;
       
    }


    static class Contour {
        public final List<GlyphPoint> glyphPoints;



        public Contour(List<GlyphPoint> glyphPoints) {
            this.glyphPoints = glyphPoints;
        }


        static class GlyphPoint {
            public final Point2D xy;
            public final boolean onCurve;


            public GlyphPoint(Point2D xy, boolean onCurve) {
                this.xy = xy;
                this.onCurve = onCurve;
            }

        }

    }




}

/*class ApproximateDouble {
    double actualValue;
    ApproximateDouble(double value){
        this.actualValue = value;
    }
    @Override
    public boolean equals(Object doubleVal){
      if(doubleVal != null &&   (doubleVal instanceof ApproximateDouble)){


      if (((ApproximateDouble)doubleVal).actualValue == this.actualValue ||
              (Math.abs(((ApproximateDouble)doubleVal).actualValue - this.actualValue) < 10 )) {
          return true;
          }

      }
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }



}   */

