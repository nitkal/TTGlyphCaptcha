package com.sun.captcha;


import org.jdom.input.SAXBuilder;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;

import javax.imageio.ImageIO;
import javax.xml.stream.XMLStreamException;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;


/**
 * Main class to draw a glyph alphabet
 */
public class DrawTrueTypeAlphabet {
    static GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    static GraphicsDevice gd = ge.getDefaultScreenDevice();
    static GraphicsConfiguration gc = gd.getDefaultConfiguration();
    static BufferedImage image = gc.createCompatibleImage(300, 200);
    static Graphics2D graphics = (Graphics2D) image.getGraphics();
    static AffineTransform transform = new AffineTransform(1, 0, 0, -1, 0, 100);


    static Map<String, TTGlyph2> glyfMap = new HashMap<String, TTGlyph2>();
    static Map<TTGlyph2, List<Point2D.Double>> glyfLineMap = new HashMap<TTGlyph2, List<Point2D.Double>>();


    public static void main(String args[]) throws Exception {


        graphics.setTransform(transform);
        graphics.setBackground(Color.WHITE);
        graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.fillRect(0, -100, 400, 200);

        String[] captchWord = "t/c/w/a/e/g".split("/");


        DrawTrueTypeAlphabet pgm = new DrawTrueTypeAlphabet();
        pgm.parseFontXMLFile();

        double xoffset = 0;
        double prevXoffset = 0;
        TTGlyph2 currentGlyf = null;
        TTGlyph2 prevGlyf = null;
        List<Point2D.Double> listPoints = null;
        double prevGlyphMaxX = 0;
        double prevGlyphMinX = 0;
        double yOffset = 0;
        double distbetnGlyfs = 0;
        for (String eachWord : captchWord) {
            currentGlyf = glyfMap.get(eachWord);
            distbetnGlyfs = pgm.getDistanceBetnGlyfs(currentGlyf, xoffset, prevXoffset, prevGlyf);
            xoffset = xoffset - distbetnGlyfs;

            if (drawGlyph(currentGlyf, xoffset, yOffset, 15)) {
            } else {
                System.out.println("Glyph " + eachWord + "Not drawn");
            }
            prevGlyf = currentGlyf;
            prevXoffset = xoffset;
            prevGlyphMaxX = Math.abs(prevGlyf.maxX);
            prevGlyphMinX = Math.abs(prevGlyf.minX);
            xoffset = Math.abs(xoffset + prevGlyphMaxX - prevGlyphMinX);

        }


        File imageFile = new File(System.getProperty("user.home")+File.separator+ "tmp" + File.separator + "generated3.jpg");

        ImageIO.write(pgm.image, "jpg", imageFile);


    }

    private static double randRotate() {
        double theta = Math.random() / 2;
        return theta;

    }

    public static boolean drawGlyph(TTGlyph2 ttglyph, double xoffset, double yoffset, int scale) {
        if (ttglyph == null) {
            return false;
        }
        Area contourArea = null;
        Double theta = null;
        for (TTGlyph2.Contour contour : ttglyph.contours) {

            GeneralPath.Double glyfPath = new GeneralPath.Double();
            System.out.println("contour.." + contour);
            int pointIndex = contour.glyphPoints.size() - 3;
            TTGlyph2.Contour.GlyphPoint startPoint = null;
            TTGlyph2.Contour.GlyphPoint nextPoint = null;


            for (int i = 0; i <= pointIndex; i++) {
                startPoint = contour.glyphPoints.get(i);

                if (i == pointIndex) {
                    //For the last oncurve point, next point becomes the first point
                    nextPoint = contour.glyphPoints.get(0);
                } else {
                    nextPoint = contour.glyphPoints.get(i + 1);
                }

                //Straight Line (for 2 consecutive on curve points
                if (startPoint.onCurve && nextPoint.onCurve) {
                    //  drawLine(glyfPath,startPoint.xy, nextPoint.xy, xoffset,yoffset, scale);
                    distortStraightLine(glyfPath, scale, startPoint, nextPoint, xoffset);
                    continue;
                }


                Point2D ctrlPoint1 = nextPoint.xy;
                if ((i + 1) == pointIndex) {
                    //For the last oncurve point, next point becomes the first point
                    nextPoint = contour.glyphPoints.get(0);

                } else {
                    nextPoint = contour.glyphPoints.get(i + 2);
                }

                if (nextPoint.onCurve) {
                    //Quad bez Curve - single off curve control point
                    drawBezQuadCurve(glyfPath, startPoint.xy, ctrlPoint1, nextPoint.xy, xoffset, yoffset, scale);
                    //Jump to the next oncurve point
                    i++;

                    continue;
                } else {
                    Point2D ctrlPoint2 = nextPoint.xy;
                    if ((i + 2) == pointIndex) {
                        //For the last oncurve point, next point becomes the first point
                        nextPoint = contour.glyphPoints.get(0);

                    } else {
                        nextPoint = contour.glyphPoints.get(i + 3);
                    }

                    if (nextPoint.onCurve) {
                        //Cubic bez Curve - double off curve control points
                        drawBezCubicCurve(glyfPath, startPoint.xy, ctrlPoint1, ctrlPoint2, nextPoint.xy, xoffset, yoffset, scale);
                        //Jump to the next oncurve point
                        i = i + 2;

                        continue;

                    }
                }

            }

            if (contourArea == null) {
                contourArea = new Area(glyfPath);
            } else {
                contourArea.exclusiveOr(new Area(glyfPath));
                if (theta != null) {
                }
                theta = randRotate();

            }


        }

        graphics.setBackground(Color.white);
        graphics.setPaint(Color.DARK_GRAY);
        graphics.fill(contourArea);

        return true;
    }


    private static void drawLine(GeneralPath.Double path, Point2D point1, Point2D point2, double xoffset, double yoffset, int scale) {

        try {
            path.lineTo((point2.getX() + xoffset) / scale, point2.getY() / scale);
        } catch (IllegalPathStateException ipe) {
            path.moveTo((point1.getX() + xoffset) / scale, point1.getY() / scale);
            path.lineTo((point2.getX() + xoffset) / scale, point2.getY() / scale);
        }

    }

    private static void drawBezQuadCurve(GeneralPath.Double path, Point2D point1, Point2D controlPoint, Point2D point2, double xoffset, double yoffset, int scale) {

        try {
            path.quadTo((controlPoint.getX() + xoffset) / scale, controlPoint.getY() / scale, (point2.getX() + xoffset) / scale, point2.getY() / scale);
        } catch (IllegalPathStateException ipe) {
            path.moveTo((point1.getX() + xoffset) / scale, point1.getY() / scale);
            path.quadTo((controlPoint.getX() + xoffset) / scale, controlPoint.getY() / scale, (point2.getX() + xoffset) / scale, point2.getY() / scale);
        }

    }


    private static void drawBezCubicCurve(GeneralPath.Double path, Point2D point1, Point2D ctrlPt1, Point2D ctrlPt2, Point2D point2, double xoffset, double yoffset, int scale) {


        try {
            path.curveTo((ctrlPt1.getX() + xoffset) / scale, ctrlPt1.getY() / scale, (ctrlPt2.getX() + xoffset) / scale, ctrlPt2.getY() / scale, (point2.getX() + xoffset) / scale, point2.getY() / scale);
        }
        catch (IllegalPathStateException ipe) {
            path.moveTo((point1.getX() + xoffset) / scale, point1.getY() / scale);
            path.curveTo((ctrlPt1.getX() + xoffset) / scale, ctrlPt1.getY() / scale, (ctrlPt2.getX() + xoffset) / scale, ctrlPt2.getY() / scale, (point2.getX() + xoffset) / scale, point2.getY() / scale);
        }


    }

    private static void distortStraightLine(GeneralPath.Double path, int scale, TTGlyph2.Contour.GlyphPoint startPt, TTGlyph2.Contour.GlyphPoint endPt, double xoffset) {
        double mx = (startPt.xy.getX() + endPt.xy.getX()) / 2;
        double my = (startPt.xy.getY() + endPt.xy.getY()) / 2;
        mx = mx + Math.random() * 150;

        my = my + Math.random() * 150;

        try {
            path.quadTo((mx + xoffset) / scale, my / scale, (endPt.xy.getX() + xoffset) / scale, endPt.xy.getY() / scale);
        } catch (IllegalPathStateException ipe) {
            path.moveTo((startPt.xy.getX() + xoffset) / scale, startPt.xy.getY() / scale);
            path.quadTo((mx + xoffset) / scale, my / scale, (endPt.xy.getX() + xoffset) / scale, endPt.xy.getY() / scale);
        }

    }


    private static void parseFontXMLFile() throws XMLStreamException, JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        Document document = builder.build(new File(System.getProperty("user.home") + File.separator + "font" + File.separator +
                "Times_New_Roman_Italic.ttx"));
        Element rootElement = document.getRootElement();
        List<Element> ttGlyphElements = rootElement.getChild("glyf").getChildren("TTGlyph");
        Point2D minX = null;
        Point2D maxX = null;

        for (Element ttGlyphElement : ttGlyphElements) {

            String xMin = ttGlyphElement.getAttributeValue("xMin");
            String yMin = ttGlyphElement.getAttributeValue("yMin");
            String xMax = ttGlyphElement.getAttributeValue("xMax");
            String yMax = ttGlyphElement.getAttributeValue("yMax");

            List<Element> contourElements = ttGlyphElement.getChildren("contour");
            List<TTGlyph2.Contour> contourList = new ArrayList<TTGlyph2.Contour>();

            for (Element contourElement : contourElements) {

                List<Element> pointElements = contourElement.getChildren("pt");
                List<TTGlyph2.Contour.GlyphPoint> gPoints = new ArrayList<TTGlyph2.Contour.GlyphPoint>();
                for (Element pointElement : pointElements) {
                    String xval = pointElement.getAttributeValue("x");
                    String yval = pointElement.getAttributeValue("y");
                    if (xval != null && yval != null) {
                        Double dx = Double.parseDouble(xval);
                        Double dy = Double.parseDouble(yval);
                        Point2D point = new Point2D.Double(dx, dy);
                        boolean onCurve = pointElement.getAttributeValue("on").equals("1");
                        TTGlyph2.Contour.GlyphPoint gpoint = new TTGlyph2.Contour.GlyphPoint(point, onCurve);
                        gPoints.add(gpoint);

                        if (xMin != null && xMin.equals(xval)) {
                            minX = point;
                        }

                        if (xMax != null && xMax.equals(xval)) {
                            maxX = point;
                        }

                    }
                }
                TTGlyph2.Contour contour = new TTGlyph2.Contour(gPoints);
                contourList.add(contour);
            }

            double dblxMin = (xMin == null) ? 0 : Double.parseDouble(xMin);
            double dblyMin = (yMin == null) ? 0 : Double.parseDouble(yMin);
            double dblxMax = (xMax == null) ? 0 : Double.parseDouble(xMax);
            double dblyMax = (yMax == null) ? 0 : Double.parseDouble(yMax);


            TTGlyph2 glyph = new TTGlyph2(ttGlyphElement.getAttributeValue("name"), dblxMin, dblyMin, dblxMax, dblyMax, contourList, minX, maxX);
            glyfMap.put(ttGlyphElement.getAttributeValue("name"), glyph);

        }


    }

    static class CoOrds {
        final double a;
        final double b;

        CoOrds(double a, double b) {
            this.a = a;
            this.b = b;
        }
    }



    private double getDistanceBetnGlyfs(TTGlyph2 currentGlyph, double currentOffset, double prevOffset, TTGlyph2 prevGlyph) {
        if (currentGlyph == null || prevGlyph == null) {
            return 0;
        }
        double minDist = 1000 + currentOffset;
        for (TTGlyph2.Contour contour : currentGlyph.contours) {
            for (TTGlyph2.Contour.GlyphPoint point : contour.glyphPoints) {
                // List<Double> xList = prevGlyph.yxMap.get(new ApproximateDouble(point.xy.getY()));
                List<Double> xList = getXList(prevGlyph, point.xy.getY());
                if (xList != null) {
                    double dist = 0;
                    for (double x : xList) {
                        dist = point.xy.getX() + currentOffset - x - prevOffset;
                        if (dist > 0 && dist < minDist) {

                            minDist = dist;

                        }

                    }
                }
            }
        }
        return minDist;

    }

    private List<Double> getXList(TTGlyph2 prevGlyf, Double yVal) {
        if (prevGlyf == null) {
            return null;
        }
        List<Double> xList = new ArrayList<Double>();
        for (TTGlyph2.Contour contour : prevGlyf.contours) {
            for (TTGlyph2.Contour.GlyphPoint point : contour.glyphPoints) {
                if (Math.abs(point.xy.getY() - yVal) < 10) {
                    xList.add(point.xy.getX());
                }
            }
        }
        return xList;
    }

}

