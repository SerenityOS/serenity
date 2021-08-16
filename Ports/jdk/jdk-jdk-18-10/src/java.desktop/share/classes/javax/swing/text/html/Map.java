/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package javax.swing.text.html;

import java.awt.Polygon;
import java.io.Serializable;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.swing.text.AttributeSet;

/**
 * Map is used to represent a map element that is part of an HTML document.
 * Once a Map has been created, and any number of areas have been added,
 * you can test if a point falls inside the map via the contains method.
 *
 * @author  Scott Violet
 */
@SuppressWarnings("serial") // Same-version serialization only
class Map implements Serializable {
    /** Name of the Map. */
    private String           name;
    /** An array of AttributeSets. */
    private Vector<AttributeSet>           areaAttributes;
    /** An array of RegionContainments, will slowly grow to match the
     * length of areaAttributes as needed. */
    private Vector<RegionContainment>           areas;

    public Map() {
    }

    public Map(String name) {
        this.name = name;
    }

    /**
     * Returns the name of the Map.
     */
    public String getName() {
        return name;
    }

    /**
     * Defines a region of the Map, based on the passed in AttributeSet.
     */
    public void addArea(AttributeSet as) {
        if (as == null) {
            return;
        }
        if (areaAttributes == null) {
            areaAttributes = new Vector<AttributeSet>(2);
        }
        areaAttributes.addElement(as.copyAttributes());
    }

    /**
     * Removes the previously created area.
     */
    public void removeArea(AttributeSet as) {
        if (as != null && areaAttributes != null) {
            int numAreas = (areas != null) ? areas.size() : 0;
            for (int counter = areaAttributes.size() - 1; counter >= 0;
                 counter--) {
                if (areaAttributes.elementAt(counter).isEqual(as)){
                    areaAttributes.removeElementAt(counter);
                    if (counter < numAreas) {
                        areas.removeElementAt(counter);
                    }
                }
            }
        }
    }

    /**
     * Returns the AttributeSets representing the differet areas of the Map.
     */
    public AttributeSet[] getAreas() {
        int numAttributes = (areaAttributes != null) ? areaAttributes.size() :
                            0;
        if (numAttributes != 0) {
            AttributeSet[]    retValue = new AttributeSet[numAttributes];

            areaAttributes.copyInto(retValue);
            return retValue;
        }
        return null;
    }

    /**
     * Returns the AttributeSet that contains the passed in location,
     * <code>x</code>, <code>y</code>. <code>width</code>, <code>height</code>
     * gives the size of the region the map is defined over. If a matching
     * area is found, the AttribueSet for it is returned.
     */
    public AttributeSet getArea(int x, int y, int width, int height) {
        int      numAttributes = (areaAttributes != null) ?
                                 areaAttributes.size() : 0;

        if (numAttributes > 0) {
            int      numAreas = (areas != null) ? areas.size() : 0;

            if (areas == null) {
                areas = new Vector<RegionContainment>(numAttributes);
            }
            for (int counter = 0; counter < numAttributes; counter++) {
                if (counter >= numAreas) {
                    areas.addElement(createRegionContainment
                            (areaAttributes.elementAt(counter)));
                }
                RegionContainment rc = areas.elementAt(counter);
                if (rc != null && rc.contains(x, y, width, height)) {
                    return areaAttributes.elementAt(counter);
                }
            }
        }
        return null;
    }

    /**
     * Creates and returns an instance of RegionContainment that can be
     * used to test if a particular point lies inside a region.
     */
    protected RegionContainment createRegionContainment
                                  (AttributeSet attributes) {
        Object     shape = attributes.getAttribute(HTML.Attribute.SHAPE);

        if (shape == null) {
            shape = "rect";
        }
        if (shape instanceof String) {
            String                shapeString = ((String)shape).toLowerCase();
            RegionContainment     rc = null;

            try {
                if (shapeString.equals("rect")) {
                    rc = new RectangleRegionContainment(attributes);
                }
                else if (shapeString.equals("circle")) {
                    rc = new CircleRegionContainment(attributes);
                }
                else if (shapeString.equals("poly")) {
                    rc = new PolygonRegionContainment(attributes);
                }
                else if (shapeString.equals("default")) {
                    rc = DefaultRegionContainment.sharedInstance();
                }
            } catch (RuntimeException re) {
                // Something wrong with attributes.
                rc = null;
            }
            return rc;
        }
        return null;
    }

    /**
     * Creates and returns an array of integers from the String
     * <code>stringCoords</code>. If one of the values represents a
     * % the returned value with be negative. If a parse error results
     * from trying to parse one of the numbers null is returned.
     */
    protected static int[] extractCoords(Object stringCoords) {
        if (stringCoords == null || !(stringCoords instanceof String)) {
            return null;
        }

        StringTokenizer    st = new StringTokenizer((String)stringCoords,
                                                    ", \t\n\r");
        int[]              retValue = null;
        int                numCoords = 0;

        while(st.hasMoreElements()) {
            String         token = st.nextToken();
            int            scale;

            if (token.endsWith("%")) {
                scale = -1;
                token = token.substring(0, token.length() - 1);
            }
            else {
                scale = 1;
            }
            try {
                int       intValue = Integer.parseInt(token);

                if (retValue == null) {
                    retValue = new int[4];
                }
                else if(numCoords == retValue.length) {
                    int[]    temp = new int[retValue.length * 2];

                    System.arraycopy(retValue, 0, temp, 0, retValue.length);
                    retValue = temp;
                }
                retValue[numCoords++] = intValue * scale;
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
        if (numCoords > 0 && numCoords != retValue.length) {
            int[]    temp = new int[numCoords];

            System.arraycopy(retValue, 0, temp, 0, numCoords);
            retValue = temp;
        }
        return retValue;
    }


    /**
     * Defines the interface used for to check if a point is inside a
     * region.
     */
    interface RegionContainment {
        /**
         * Returns true if the location <code>x</code>, <code>y</code>
         * falls inside the region defined in the receiver.
         * <code>width</code>, <code>height</code> is the size of
         * the enclosing region.
         */
        public boolean contains(int x, int y, int width, int height);
    }


    /**
     * Used to test for containment in a rectangular region.
     */
    static class RectangleRegionContainment implements RegionContainment {
        /** Will be non-null if one of the values is a percent, and any value
         * that is non null indicates it is a percent
         * (order is x, y, width, height). */
        float[]       percents;
        /** Last value of width passed in. */
        int           lastWidth;
        /** Last value of height passed in. */
        int           lastHeight;
        /** Top left. */
        int           x0;
        int           y0;
        /** Bottom right. */
        int           x1;
        int           y1;

        public RectangleRegionContainment(AttributeSet as) {
            int[]    coords = Map.extractCoords(as.getAttribute(HTML.
                                                           Attribute.COORDS));

            percents = null;
            if (coords == null || coords.length != 4) {
                throw new RuntimeException("Unable to parse rectangular area");
            }
            else {
                x0 = coords[0];
                y0 = coords[1];
                x1 = coords[2];
                y1 = coords[3];
                if (x0 < 0 || y0 < 0 || x1 < 0 || y1 < 0) {
                    percents = new float[4];
                    lastWidth = lastHeight = -1;
                    for (int counter = 0; counter < 4; counter++) {
                        if (coords[counter] < 0) {
                            percents[counter] = Math.abs
                                        (coords[counter]) / 100.0f;
                        }
                        else {
                            percents[counter] = -1.0f;
                        }
                    }
                }
            }
        }

        public boolean contains(int x, int y, int width, int height) {
            if (percents == null) {
                return contains(x, y);
            }
            if (lastWidth != width || lastHeight != height) {
                lastWidth = width;
                lastHeight = height;
                if (percents[0] != -1.0f) {
                    x0 = (int)(percents[0] * width);
                }
                if (percents[1] != -1.0f) {
                    y0 = (int)(percents[1] * height);
                }
                if (percents[2] != -1.0f) {
                    x1 = (int)(percents[2] * width);
                }
                if (percents[3] != -1.0f) {
                    y1 = (int)(percents[3] * height);
                }
            }
            return contains(x, y);
        }

        public boolean contains(int x, int y) {
            return ((x >= x0 && x <= x1) &&
                    (y >= y0 && y <= y1));
        }
    }


    /**
     * Used to test for containment in a polygon region.
     */
    static class PolygonRegionContainment extends Polygon implements
                 RegionContainment {
        /** If any value is a percent there will be an entry here for the
         * percent value. Use percentIndex to find out the index for it. */
        float[]           percentValues;
        int[]             percentIndexs;
        /** Last value of width passed in. */
        int               lastWidth;
        /** Last value of height passed in. */
        int               lastHeight;

        public PolygonRegionContainment(AttributeSet as) {
            int[]    coords = Map.extractCoords(as.getAttribute(HTML.Attribute.
                                                                COORDS));

            if (coords == null || coords.length == 0 ||
                coords.length % 2 != 0) {
                throw new RuntimeException("Unable to parse polygon area");
            }
            else {
                int        numPercents = 0;

                lastWidth = lastHeight = -1;
                for (int counter = coords.length - 1; counter >= 0;
                     counter--) {
                    if (coords[counter] < 0) {
                        numPercents++;
                    }
                }

                if (numPercents > 0) {
                    percentIndexs = new int[numPercents];
                    percentValues = new float[numPercents];
                    for (int counter = coords.length - 1, pCounter = 0;
                         counter >= 0; counter--) {
                        if (coords[counter] < 0) {
                            percentValues[pCounter] = coords[counter] /
                                                      -100.0f;
                            percentIndexs[pCounter] = counter;
                            pCounter++;
                        }
                    }
                }
                else {
                    percentIndexs = null;
                    percentValues = null;
                }
                npoints = coords.length / 2;
                xpoints = new int[npoints];
                ypoints = new int[npoints];

                for (int counter = 0; counter < npoints; counter++) {
                    xpoints[counter] = coords[counter + counter];
                    ypoints[counter] = coords[counter + counter + 1];
                }
            }
        }

        public boolean contains(int x, int y, int width, int height) {
            if (percentValues == null || (lastWidth == width &&
                                          lastHeight == height)) {
                return contains(x, y);
            }
            // Force the bounding box to be recalced.
            bounds = null;
            lastWidth = width;
            lastHeight = height;
            float fWidth = (float)width;
            float fHeight = (float)height;
            for (int counter = percentValues.length - 1; counter >= 0;
                 counter--) {
                if (percentIndexs[counter] % 2 == 0) {
                    // x
                    xpoints[percentIndexs[counter] / 2] =
                            (int)(percentValues[counter] * fWidth);
                }
                else {
                    // y
                    ypoints[percentIndexs[counter] / 2] =
                            (int)(percentValues[counter] * fHeight);
                }
            }
            return contains(x, y);
        }
    }


    /**
     * Used to test for containment in a circular region.
     */
    static class CircleRegionContainment implements RegionContainment {
        /** X origin of the circle. */
        int           x;
        /** Y origin of the circle. */
        int           y;
        /** Radius of the circle. */
        int           radiusSquared;
        /** Non-null indicates one of the values represents a percent. */
        float[]       percentValues;
        /** Last value of width passed in. */
        int           lastWidth;
        /** Last value of height passed in. */
        int           lastHeight;

        public CircleRegionContainment(AttributeSet as) {
            int[]    coords = Map.extractCoords(as.getAttribute(HTML.Attribute.
                                                                COORDS));

            if (coords == null || coords.length != 3) {
                throw new RuntimeException("Unable to parse circular area");
            }
            x = coords[0];
            y = coords[1];
            radiusSquared = coords[2] * coords[2];
            if (coords[0] < 0 || coords[1] < 0 || coords[2] < 0) {
                lastWidth = lastHeight = -1;
                percentValues = new float[3];
                for (int counter = 0; counter < 3; counter++) {
                    if (coords[counter] < 0) {
                        percentValues[counter] = coords[counter] /
                                                 -100.0f;
                    }
                    else {
                        percentValues[counter] = -1.0f;
                    }
                }
            }
            else {
                percentValues = null;
            }
        }

        public boolean contains(int x, int y, int width, int height) {
            if (percentValues != null && (lastWidth != width ||
                                          lastHeight != height)) {
                int      newRad = Math.min(width, height) / 2;

                lastWidth = width;
                lastHeight = height;
                if (percentValues[0] != -1.0f) {
                    this.x = (int)(percentValues[0] * width);
                }
                if (percentValues[1] != -1.0f) {
                    this.y = (int)(percentValues[1] * height);
                }
                if (percentValues[2] != -1.0f) {
                    radiusSquared = (int)(percentValues[2] *
                                   Math.min(width, height));
                    radiusSquared *= radiusSquared;
                }
            }
            return (((x - this.x) * (x - this.x) +
                     (y - this.y) * (y - this.y)) <= radiusSquared);
        }
    }


    /**
     * An implementation that will return true if the x, y location is
     * inside a rectangle defined by origin 0, 0, and width equal to
     * width passed in, and height equal to height passed in.
     */
    static class DefaultRegionContainment implements RegionContainment {
        /** A global shared instance. */
        static DefaultRegionContainment  si = null;

        public static DefaultRegionContainment sharedInstance() {
            if (si == null) {
                si = new DefaultRegionContainment();
            }
            return si;
        }

        public boolean contains(int x, int y, int width, int height) {
            return (x <= width && x >= 0 && y >= 0 && y <= width);
        }
    }
}
