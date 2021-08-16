/*
 * Copyright (c) 2001, 2014, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.awt.Rectangle;
import java.util.*;

/**
 * A <code>SpringLayout</code> lays out the children of its associated container
 * according to a set of constraints.
 * See <a href="https://docs.oracle.com/javase/tutorial/uiswing/layout/spring.html">How to Use SpringLayout</a>
 * in <em>The Java Tutorial</em> for examples of using
 * <code>SpringLayout</code>.
 *
 * <p>
 * Each constraint,
 * represented by a <code>Spring</code> object,
 * controls the vertical or horizontal distance
 * between two component edges.
 * The edges can belong to
 * any child of the container,
 * or to the container itself.
 * For example,
 * the allowable width of a component
 * can be expressed using a constraint
 * that controls the distance between the west (left) and east (right)
 * edges of the component.
 * The allowable <em>y</em> coordinates for a component
 * can be expressed by constraining the distance between
 * the north (top) edge of the component
 * and the north edge of its container.
 *
 * <P>
 * Every child of a <code>SpringLayout</code>-controlled container,
 * as well as the container itself,
 * has exactly one set of constraints
 * associated with it.
 * These constraints are represented by
 * a <code>SpringLayout.Constraints</code> object.
 * By default,
 * <code>SpringLayout</code> creates constraints
 * that make their associated component
 * have the minimum, preferred, and maximum sizes
 * returned by the component's
 * {@link java.awt.Component#getMinimumSize},
 * {@link java.awt.Component#getPreferredSize}, and
 * {@link java.awt.Component#getMaximumSize}
 * methods. The <em>x</em> and <em>y</em> positions are initially not
 * constrained, so that until you constrain them the <code>Component</code>
 * will be positioned at 0,0 relative to the <code>Insets</code> of the
 * parent <code>Container</code>.
 *
 * <p>
 * You can change
 * a component's constraints in several ways.
 * You can
 * use one of the
 * {@link #putConstraint putConstraint}
 * methods
 * to establish a spring
 * linking the edges of two components within the same container.
 * Or you can get the appropriate <code>SpringLayout.Constraints</code>
 * object using
 * {@link #getConstraints getConstraints}
 * and then modify one or more of its springs.
 * Or you can get the spring for a particular edge of a component
 * using {@link #getConstraint getConstraint},
 * and modify it.
 * You can also associate
 * your own <code>SpringLayout.Constraints</code> object
 * with a component by specifying the constraints object
 * when you add the component to its container
 * (using
 * {@link Container#add(Component, Object)}).
 *
 * <p>
 * The <code>Spring</code> object representing each constraint
 * has a minimum, preferred, maximum, and current value.
 * The current value of the spring
 * is somewhere between the minimum and maximum values,
 * according to the formula given in the
 * {@link Spring#sum} method description.
 * When the minimum, preferred, and maximum values are the same,
 * the current value is always equal to them;
 * this inflexible spring is called a <em>strut</em>.
 * You can create struts using the factory method
 * {@link Spring#constant(int)}.
 * The <code>Spring</code> class also provides factory methods
 * for creating other kinds of springs,
 * including springs that depend on other springs.
 *
 * <p>
 * In a <code>SpringLayout</code>, the position of each edge is dependent on
 * the position of just one other edge. If a constraint is subsequently added
 * to create a new binding for an edge, the previous binding is discarded
 * and the edge remains dependent on a single edge.
 * Springs should only be attached
 * between edges of the container and its immediate children; the behavior
 * of the <code>SpringLayout</code> when presented with constraints linking
 * the edges of components from different containers (either internal or
 * external) is undefined.
 *
 * <h2>
 * SpringLayout vs. Other Layout Managers
 * </h2>
 *
 * <blockquote>
 * <hr>
 * <strong>Note:</strong>
 * Unlike many layout managers,
 * <code>SpringLayout</code> doesn't automatically set the location of
 * the components it manages.
 * If you hand-code a GUI that uses <code>SpringLayout</code>,
 * remember to initialize component locations by constraining the west/east
 * and north/south locations.
 * <p>
 * Depending on the constraints you use,
 * you may also need to set the size of the container explicitly.
 * <hr>
 * </blockquote>
 *
 * <p>
 * Despite the simplicity of <code>SpringLayout</code>,
 * it can emulate the behavior of most other layout managers.
 * For some features,
 * such as the line breaking provided by <code>FlowLayout</code>,
 * you'll need to
 * create a special-purpose subclass of the <code>Spring</code> class.
 *
 * <p>
 * <code>SpringLayout</code> also provides a way to solve
 * many of the difficult layout
 * problems that cannot be solved by nesting combinations
 * of <code>Box</code>es. That said, <code>SpringLayout</code> honors the
 * <code>LayoutManager2</code> contract correctly and so can be nested with
 * other layout managers -- a technique that can be preferable to
 * creating the constraints implied by the other layout managers.
 * <p>
 * The asymptotic complexity of the layout operation of a <code>SpringLayout</code>
 * is linear in the number of constraints (and/or components).
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @see Spring
 * @see SpringLayout.Constraints
 *
 * @author      Philip Milne
 * @author      Scott Violet
 * @author      Joe Winchester
 * @since       1.4
 */
@SuppressWarnings("serial") // Same-version serialization only
public class SpringLayout implements LayoutManager2 {
    private Map<Component, Constraints> componentConstraints = new HashMap<Component, Constraints>();

    private Spring cyclicReference = Spring.constant(Spring.UNSET);
    private Set<Spring> cyclicSprings;
    private Set<Spring> acyclicSprings;


    /**
     * Specifies the top edge of a component's bounding rectangle.
     */
    public static final String NORTH  = "North";

    /**
     * Specifies the bottom edge of a component's bounding rectangle.
     */
    public static final String SOUTH  = "South";

    /**
     * Specifies the right edge of a component's bounding rectangle.
     */
    public static final String EAST   = "East";

    /**
     * Specifies the left edge of a component's bounding rectangle.
     */
    public static final String WEST   = "West";

    /**
     * Specifies the horizontal center of a component's bounding rectangle.
     *
     * @since 1.6
     */
    public static final String HORIZONTAL_CENTER   = "HorizontalCenter";

    /**
     * Specifies the vertical center of a component's bounding rectangle.
     *
     * @since 1.6
     */
    public static final String VERTICAL_CENTER   = "VerticalCenter";

    /**
     * Specifies the baseline of a component.
     *
     * @since 1.6
     */
    public static final String BASELINE   = "Baseline";

    /**
     * Specifies the width of a component's bounding rectangle.
     *
     * @since 1.6
     */
    public static final String WIDTH = "Width";

    /**
     * Specifies the height of a component's bounding rectangle.
     *
     * @since 1.6
     */
    public static final String HEIGHT = "Height";

    private static String[] ALL_HORIZONTAL = {WEST, WIDTH, EAST, HORIZONTAL_CENTER};

    private static String[] ALL_VERTICAL = {NORTH, HEIGHT, SOUTH, VERTICAL_CENTER, BASELINE};

    /**
     * A <code>Constraints</code> object holds the
     * constraints that govern the way a component's size and position
     * change in a container controlled by a <code>SpringLayout</code>.
     * A <code>Constraints</code> object is
     * like a <code>Rectangle</code>, in that it
     * has <code>x</code>, <code>y</code>,
     * <code>width</code>, and <code>height</code> properties.
     * In the <code>Constraints</code> object, however,
     * these properties have
     * <code>Spring</code> values instead of integers.
     * In addition,
     * a <code>Constraints</code> object
     * can be manipulated as four edges
     * -- north, south, east, and west --
     * using the <code>constraint</code> property.
     *
     * <p>
     * The following formulas are always true
     * for a <code>Constraints</code> object (here WEST and <code>x</code> are synonyms, as are and NORTH and <code>y</code>):
     *
     * <pre>
     *               EAST = WEST + WIDTH
     *              SOUTH = NORTH + HEIGHT
     *  HORIZONTAL_CENTER = WEST + WIDTH/2
     *    VERTICAL_CENTER = NORTH + HEIGHT/2
     *  ABSOLUTE_BASELINE = NORTH + RELATIVE_BASELINE*
     * </pre>
     * <p>
     * For example, if you have specified the WIDTH and WEST (X) location
     * the EAST is calculated as WEST + WIDTH.  If you instead specified
     * the WIDTH and EAST locations the WEST (X) location is then calculated
     * as EAST - WIDTH.
     * <p>
     * [RELATIVE_BASELINE is a private constraint that is set automatically when
     * the SpringLayout.Constraints(Component) constructor is called or when
     * a constraints object is registered with a SpringLayout object.]
     * <p>
     * <b>Note</b>: In this document,
     * operators represent methods
     * in the <code>Spring</code> class.
     * For example, "a + b" is equal to
     * <code>Spring.sum(a, b)</code>,
     * and "a - b" is equal to
     * <code>Spring.sum(a, Spring.minus(b))</code>.
     * See the
     * {@link Spring Spring API documentation}
     * for further details
     * of spring arithmetic.
     *
     * <p>
     *
     * Because a <code>Constraints</code> object's properties --
     * representing its edges, size, and location -- can all be set
     * independently and yet are interrelated,
     * a <code>Constraints</code> object can become <em>over-constrained</em>.
     * For example, if the <code>WEST</code>, <code>WIDTH</code> and
     * <code>EAST</code> edges are all set, steps must be taken to ensure that
     * the first of the formulas above holds.  To do this, the
     * <code>Constraints</code>
     * object throws away the <em>least recently set</em>
     * constraint so as to make the formulas hold.
     * @since 1.4
     */
    public static class Constraints {
       private Spring x;
       private Spring y;
       private Spring width;
       private Spring height;
       private Spring east;
       private Spring south;
        private Spring horizontalCenter;
        private Spring verticalCenter;
        private Spring baseline;

        private List<String> horizontalHistory = new ArrayList<String>(2);
        private List<String> verticalHistory = new ArrayList<String>(2);

        // Used for baseline calculations
        private Component c;

       /**
        * Creates an empty <code>Constraints</code> object.
        */
       public Constraints() {
       }

       /**
        * Creates a <code>Constraints</code> object with the
        * specified values for its
        * <code>x</code> and <code>y</code> properties.
        * The <code>height</code> and <code>width</code> springs
        * have <code>null</code> values.
        *
        * @param x  the spring controlling the component's <em>x</em> value
        * @param y  the spring controlling the component's <em>y</em> value
        */
       public Constraints(Spring x, Spring y) {
           setX(x);
           setY(y);
       }

       /**
        * Creates a <code>Constraints</code> object with the
        * specified values for its
        * <code>x</code>, <code>y</code>, <code>width</code>,
        * and <code>height</code> properties.
        * Note: If the <code>SpringLayout</code> class
        * encounters <code>null</code> values in the
        * <code>Constraints</code> object of a given component,
        * it replaces them with suitable defaults.
        *
        * @param x  the spring value for the <code>x</code> property
        * @param y  the spring value for the <code>y</code> property
        * @param width  the spring value for the <code>width</code> property
        * @param height  the spring value for the <code>height</code> property
        */
       public Constraints(Spring x, Spring y, Spring width, Spring height) {
           setX(x);
           setY(y);
           setWidth(width);
           setHeight(height);
       }

        /**
         * Creates a <code>Constraints</code> object with
         * suitable <code>x</code>, <code>y</code>, <code>width</code> and
         * <code>height</code> springs for component, <code>c</code>.
         * The <code>x</code> and <code>y</code> springs are constant
         * springs  initialised with the component's location at
         * the time this method is called. The <code>width</code> and
         * <code>height</code> springs are special springs, created by
         * the <code>Spring.width()</code> and <code>Spring.height()</code>
         * methods, which track the size characteristics of the component
         * when they change.
         *
         * @param c  the component whose characteristics will be reflected by this Constraints object
         * @throws NullPointerException if <code>c</code> is null.
         * @since 1.5
         */
        public Constraints(Component c) {
            this.c = c;
            setX(Spring.constant(c.getX()));
            setY(Spring.constant(c.getY()));
            setWidth(Spring.width(c));
            setHeight(Spring.height(c));
        }

        private void pushConstraint(String name, Spring value, boolean horizontal) {
            boolean valid = true;
            List<String> history = horizontal ? horizontalHistory :
                                                verticalHistory;
            if (history.contains(name)) {
                history.remove(name);
                valid = false;
            } else if (history.size() == 2 && value != null) {
                history.remove(0);
                valid = false;
            }
            if (value != null) {
                history.add(name);
            }
            if (!valid) {
                String[] all = horizontal ? ALL_HORIZONTAL : ALL_VERTICAL;
                for (String s : all) {
                    if (!history.contains(s)) {
                        setConstraint(s, null);
                    }
                }
            }
        }

       private Spring sum(Spring s1, Spring s2) {
           return (s1 == null || s2 == null) ? null : Spring.sum(s1, s2);
       }

       private Spring difference(Spring s1, Spring s2) {
           return (s1 == null || s2 == null) ? null : Spring.difference(s1, s2);
       }

        private Spring scale(Spring s, float factor) {
            return (s == null) ? null : Spring.scale(s, factor);
        }

        private int getBaselineFromHeight(int height) {
            if (height < 0) {
                // Bad Scott, Bad Scott!
                return -c.getBaseline(c.getPreferredSize().width,
                                      -height);
            }
            return c.getBaseline(c.getPreferredSize().width, height);
        }

        private int getHeightFromBaseLine(int baseline) {
            Dimension prefSize = c.getPreferredSize();
            int prefHeight = prefSize.height;
            int prefBaseline = c.getBaseline(prefSize.width, prefHeight);
            if (prefBaseline == baseline) {
                // If prefBaseline < 0, then no baseline, assume preferred
                // height.
                // If prefBaseline == baseline, then specified baseline
                // matches preferred baseline, return preferred height
                return prefHeight;
            }
            // Valid baseline
            switch(c.getBaselineResizeBehavior()) {
            case CONSTANT_DESCENT:
                return prefHeight + (baseline - prefBaseline);
            case CENTER_OFFSET:
                return prefHeight + 2 * (baseline - prefBaseline);
            case CONSTANT_ASCENT:
                // Component baseline and specified baseline will NEVER
                // match, fall through to default
            default: // OTHER
                // No way to map from baseline to height.
            }
            return Integer.MIN_VALUE;
        }

         private Spring heightToRelativeBaseline(Spring s) {
            return new Spring.SpringMap(s) {
                 protected int map(int i) {
                    return getBaselineFromHeight(i);
                 }

                 protected int inv(int i) {
                     return getHeightFromBaseLine(i);
                 }
            };
        }

        private Spring relativeBaselineToHeight(Spring s) {
            return new Spring.SpringMap(s) {
                protected int map(int i) {
                    return getHeightFromBaseLine(i);
                 }

                 protected int inv(int i) {
                    return getBaselineFromHeight(i);
                 }
            };
        }

        private boolean defined(List<?> history, String s1, String s2) {
            return history.contains(s1) && history.contains(s2);
        }

       /**
        * Sets the <code>x</code> property,
        * which controls the <code>x</code> value
        * of a component's location.
        *
        * @param x the spring controlling the <code>x</code> value
        *          of a component's location
        *
        * @see #getX
        * @see SpringLayout.Constraints
        */
       public void setX(Spring x) {
           this.x = x;
           pushConstraint(WEST, x, true);
       }

       /**
        * Returns the value of the <code>x</code> property.
        *
        * @return the spring controlling the <code>x</code> value
        *         of a component's location
        *
        * @see #setX
        * @see SpringLayout.Constraints
        */
       public Spring getX() {
           if (x == null) {
               if (defined(horizontalHistory, EAST, WIDTH)) {
                   x = difference(east, width);
               } else if (defined(horizontalHistory, HORIZONTAL_CENTER, WIDTH)) {
                   x = difference(horizontalCenter, scale(width, 0.5f));
               } else if (defined(horizontalHistory, HORIZONTAL_CENTER, EAST)) {
                   x = difference(scale(horizontalCenter, 2f), east);
               }
           }
           return x;
       }

       /**
        * Sets the <code>y</code> property,
        * which controls the <code>y</code> value
        * of a component's location.
        *
        * @param y the spring controlling the <code>y</code> value
        *          of a component's location
        *
        * @see #getY
        * @see SpringLayout.Constraints
        */
       public void setY(Spring y) {
           this.y = y;
           pushConstraint(NORTH, y, false);
       }

       /**
        * Returns the value of the <code>y</code> property.
        *
        * @return the spring controlling the <code>y</code> value
        *         of a component's location
        *
        * @see #setY
        * @see SpringLayout.Constraints
        */
       public Spring getY() {
           if (y == null) {
               if (defined(verticalHistory, SOUTH, HEIGHT)) {
                   y = difference(south, height);
               } else if (defined(verticalHistory, VERTICAL_CENTER, HEIGHT)) {
                   y = difference(verticalCenter, scale(height, 0.5f));
               } else if (defined(verticalHistory, VERTICAL_CENTER, SOUTH)) {
                   y = difference(scale(verticalCenter, 2f), south);
               } else if (defined(verticalHistory, BASELINE, HEIGHT)) {
                   y = difference(baseline, heightToRelativeBaseline(height));
               } else if (defined(verticalHistory, BASELINE, SOUTH)) {
                   y = scale(difference(baseline, heightToRelativeBaseline(south)), 2f);
/*
               } else if (defined(verticalHistory, BASELINE, VERTICAL_CENTER)) {
                   y = scale(difference(baseline, heightToRelativeBaseline(scale(verticalCenter, 2))), 1f/(1-2*0.5f));
*/
               }
           }
           return y;
       }

       /**
        * Sets the <code>width</code> property,
        * which controls the width of a component.
        *
        * @param width the spring controlling the width of this
        * <code>Constraints</code> object
        *
        * @see #getWidth
        * @see SpringLayout.Constraints
        */
       public void setWidth(Spring width) {
           this.width = width;
           pushConstraint(WIDTH, width, true);
       }

       /**
        * Returns the value of the <code>width</code> property.
        *
        * @return the spring controlling the width of a component
        *
        * @see #setWidth
        * @see SpringLayout.Constraints
        */
       public Spring getWidth() {
           if (width == null) {
               if (horizontalHistory.contains(EAST)) {
                   width = difference(east, getX());
               } else if (horizontalHistory.contains(HORIZONTAL_CENTER)) {
                   width = scale(difference(horizontalCenter, getX()), 2f);
               }
           }
           return width;
       }

       /**
        * Sets the <code>height</code> property,
        * which controls the height of a component.
        *
        * @param height the spring controlling the height of this <code>Constraints</code>
        * object
        *
        * @see #getHeight
        * @see SpringLayout.Constraints
        */
       public void setHeight(Spring height) {
           this.height = height;
           pushConstraint(HEIGHT, height, false);
       }

       /**
        * Returns the value of the <code>height</code> property.
        *
        * @return the spring controlling the height of a component
        *
        * @see #setHeight
        * @see SpringLayout.Constraints
        */
       public Spring getHeight() {
           if (height == null) {
               if (verticalHistory.contains(SOUTH)) {
                   height = difference(south, getY());
               } else if (verticalHistory.contains(VERTICAL_CENTER)) {
                   height = scale(difference(verticalCenter, getY()), 2f);
               } else if (verticalHistory.contains(BASELINE)) {
                   height = relativeBaselineToHeight(difference(baseline, getY()));
               }
           }
           return height;
       }

       private void setEast(Spring east) {
           this.east = east;
           pushConstraint(EAST, east, true);
       }

       private Spring getEast() {
           if (east == null) {
               east = sum(getX(), getWidth());
           }
           return east;
       }

       private void setSouth(Spring south) {
           this.south = south;
           pushConstraint(SOUTH, south, false);
       }

       private Spring getSouth() {
           if (south == null) {
               south = sum(getY(), getHeight());
           }
           return south;
       }

        private Spring getHorizontalCenter() {
            if (horizontalCenter == null) {
                horizontalCenter = sum(getX(), scale(getWidth(), 0.5f));
            }
            return horizontalCenter;
        }

        private void setHorizontalCenter(Spring horizontalCenter) {
            this.horizontalCenter = horizontalCenter;
            pushConstraint(HORIZONTAL_CENTER, horizontalCenter, true);
        }

        private Spring getVerticalCenter() {
            if (verticalCenter == null) {
                verticalCenter = sum(getY(), scale(getHeight(), 0.5f));
            }
            return verticalCenter;
        }

        private void setVerticalCenter(Spring verticalCenter) {
            this.verticalCenter = verticalCenter;
            pushConstraint(VERTICAL_CENTER, verticalCenter, false);
        }

        private Spring getBaseline() {
            if (baseline == null) {
                baseline = sum(getY(), heightToRelativeBaseline(getHeight()));
            }
            return baseline;
        }

        private void setBaseline(Spring baseline) {
            this.baseline = baseline;
            pushConstraint(BASELINE, baseline, false);
        }

       /**
        * Sets the spring controlling the specified edge.
        * The edge must have one of the following values:
        * <code>SpringLayout.NORTH</code>,
        * <code>SpringLayout.SOUTH</code>,
        * <code>SpringLayout.EAST</code>,
        * <code>SpringLayout.WEST</code>,
        * <code>SpringLayout.HORIZONTAL_CENTER</code>,
        * <code>SpringLayout.VERTICAL_CENTER</code>,
        * <code>SpringLayout.BASELINE</code>,
        * <code>SpringLayout.WIDTH</code> or
        * <code>SpringLayout.HEIGHT</code>.
        * For any other <code>String</code> value passed as the edge,
        * no action is taken. For a <code>null</code> edge, a
        * <code>NullPointerException</code> is thrown.
        * <p>
        * <b>Note:</b> This method can affect {@code x} and {@code y} values
        * previously set for this {@code Constraints}.
        *
        * @param edgeName the edge to be set
        * @param s the spring controlling the specified edge
        *
        * @throws NullPointerException if <code>edgeName</code> is <code>null</code>
        *
        * @see #getConstraint
        * @see #NORTH
        * @see #SOUTH
        * @see #EAST
        * @see #WEST
        * @see #HORIZONTAL_CENTER
        * @see #VERTICAL_CENTER
        * @see #BASELINE
        * @see #WIDTH
        * @see #HEIGHT
        * @see SpringLayout.Constraints
        */
       public void setConstraint(String edgeName, Spring s) {
           edgeName = edgeName.intern();
           if (edgeName == WEST) {
               setX(s);
           } else if (edgeName == NORTH) {
               setY(s);
           } else if (edgeName == EAST) {
               setEast(s);
           } else if (edgeName == SOUTH) {
               setSouth(s);
           } else if (edgeName == HORIZONTAL_CENTER) {
               setHorizontalCenter(s);
           } else if (edgeName == WIDTH) {
               setWidth(s);
           } else if (edgeName == HEIGHT) {
               setHeight(s);
           } else if (edgeName == VERTICAL_CENTER) {
               setVerticalCenter(s);
           } else if (edgeName == BASELINE) {
               setBaseline(s);
           }
       }

       /**
        * Returns the value of the specified edge, which may be
        * a derived value, or even <code>null</code>.
        * The edge must have one of the following values:
        * <code>SpringLayout.NORTH</code>,
        * <code>SpringLayout.SOUTH</code>,
        * <code>SpringLayout.EAST</code>,
        * <code>SpringLayout.WEST</code>,
        * <code>SpringLayout.HORIZONTAL_CENTER</code>,
        * <code>SpringLayout.VERTICAL_CENTER</code>,
        * <code>SpringLayout.BASELINE</code>,
        * <code>SpringLayout.WIDTH</code> or
        * <code>SpringLayout.HEIGHT</code>.
        * For any other <code>String</code> value passed as the edge,
        * <code>null</code> will be returned. Throws
        * <code>NullPointerException</code> for a <code>null</code> edge.
        *
        * @param edgeName the edge whose value
        *                 is to be returned
        *
        * @return the spring controlling the specified edge, may be <code>null</code>
        *
        * @throws NullPointerException if <code>edgeName</code> is <code>null</code>
        *
        * @see #setConstraint
        * @see #NORTH
        * @see #SOUTH
        * @see #EAST
        * @see #WEST
        * @see #HORIZONTAL_CENTER
        * @see #VERTICAL_CENTER
        * @see #BASELINE
        * @see #WIDTH
        * @see #HEIGHT
        * @see SpringLayout.Constraints
        */
       public Spring getConstraint(String edgeName) {
           edgeName = edgeName.intern();
           return (edgeName == WEST)  ? getX() :
                   (edgeName == NORTH) ? getY() :
                   (edgeName == EAST)  ? getEast() :
                   (edgeName == SOUTH) ? getSouth() :
                   (edgeName == WIDTH)  ? getWidth() :
                   (edgeName == HEIGHT) ? getHeight() :
                   (edgeName == HORIZONTAL_CENTER) ? getHorizontalCenter() :
                   (edgeName == VERTICAL_CENTER)  ? getVerticalCenter() :
                   (edgeName == BASELINE) ? getBaseline() :
                  null;
       }

       /*pp*/ void reset() {
           Spring[] allSprings = {x, y, width, height, east, south,
               horizontalCenter, verticalCenter, baseline};
           for (Spring s : allSprings) {
               if (s != null) {
                   s.setValue(Spring.UNSET);
               }
           }
       }
   }

   private static class SpringProxy extends Spring {
       private String edgeName;
       private Component c;
       private SpringLayout l;

       public SpringProxy(String edgeName, Component c, SpringLayout l) {
           this.edgeName = edgeName;
           this.c = c;
           this.l = l;
       }

       private Spring getConstraint() {
           return l.getConstraints(c).getConstraint(edgeName);
       }

       public int getMinimumValue() {
           return getConstraint().getMinimumValue();
       }

       public int getPreferredValue() {
           return getConstraint().getPreferredValue();
       }

       public int getMaximumValue() {
           return getConstraint().getMaximumValue();
       }

       public int getValue() {
           return getConstraint().getValue();
       }

       public void setValue(int size) {
           getConstraint().setValue(size);
       }

       /*pp*/ boolean isCyclic(SpringLayout l) {
           return l.isCyclic(getConstraint());
       }

       public String toString() {
           return "SpringProxy for " + edgeName + " edge of " + c.getName() + ".";
       }
    }

    /**
     * Constructs a new <code>SpringLayout</code>.
     */
    public SpringLayout() {}

    private void resetCyclicStatuses() {
        cyclicSprings = new HashSet<Spring>();
        acyclicSprings = new HashSet<Spring>();
    }

    private void setParent(Container p) {
        resetCyclicStatuses();
        Constraints pc = getConstraints(p);

        pc.setX(Spring.constant(0));
        pc.setY(Spring.constant(0));
        // The applyDefaults() method automatically adds width and
        // height springs that delegate their calculations to the
        // getMinimumSize(), getPreferredSize() and getMaximumSize()
        // methods of the relevant component. In the case of the
        // parent this will cause an infinite loop since these
        // methods, in turn, delegate their calculations to the
        // layout manager. Check for this case and replace the
        // the springs that would cause this problem with a
        // constant springs that supply default values.
        Spring width = pc.getWidth();
        if (width instanceof Spring.WidthSpring && ((Spring.WidthSpring)width).c == p) {
            pc.setWidth(Spring.constant(0, 0, Integer.MAX_VALUE));
        }
        Spring height = pc.getHeight();
        if (height instanceof Spring.HeightSpring && ((Spring.HeightSpring)height).c == p) {
            pc.setHeight(Spring.constant(0, 0, Integer.MAX_VALUE));
        }
    }

    /*pp*/ boolean isCyclic(Spring s) {
        if (s == null) {
            return false;
        }
        if (cyclicSprings.contains(s)) {
            return true;
        }
        if (acyclicSprings.contains(s)) {
            return false;
        }
        cyclicSprings.add(s);
        boolean result = s.isCyclic(this);
        if (!result) {
            acyclicSprings.add(s);
            cyclicSprings.remove(s);
        }
        else {
            System.err.println(s + " is cyclic. ");
        }
        return result;
    }

    private Spring abandonCycles(Spring s) {
        return isCyclic(s) ? cyclicReference : s;
    }

    // LayoutManager methods.

    /**
     * Has no effect,
     * since this layout manager does not
     * use a per-component string.
     */
    public void addLayoutComponent(String name, Component c) {}

    /**
     * Removes the constraints associated with the specified component.
     *
     * @param c the component being removed from the container
     */
    public void removeLayoutComponent(Component c) {
        componentConstraints.remove(c);
    }

    private static Dimension addInsets(int width, int height, Container p) {
        Insets i = p.getInsets();
        return new Dimension(width + i.left + i.right, height + i.top + i.bottom);
    }

    public Dimension minimumLayoutSize(Container parent) {
        setParent(parent);
        Constraints pc = getConstraints(parent);
        return addInsets(abandonCycles(pc.getWidth()).getMinimumValue(),
                         abandonCycles(pc.getHeight()).getMinimumValue(),
                         parent);
    }

    public Dimension preferredLayoutSize(Container parent) {
        setParent(parent);
        Constraints pc = getConstraints(parent);
        return addInsets(abandonCycles(pc.getWidth()).getPreferredValue(),
                         abandonCycles(pc.getHeight()).getPreferredValue(),
                         parent);
    }

    // LayoutManager2 methods.

    public Dimension maximumLayoutSize(Container parent) {
        setParent(parent);
        Constraints pc = getConstraints(parent);
        return addInsets(abandonCycles(pc.getWidth()).getMaximumValue(),
                         abandonCycles(pc.getHeight()).getMaximumValue(),
                         parent);
    }

    /**
     * If <code>constraints</code> is an instance of
     * <code>SpringLayout.Constraints</code>,
     * associates the constraints with the specified component.
     *
     * @param   component the component being added
     * @param   constraints the component's constraints
     *
     * @see SpringLayout.Constraints
     */
    public void addLayoutComponent(Component component, Object constraints) {
        if (constraints instanceof Constraints) {
            putConstraints(component, (Constraints)constraints);
        }
    }

    /**
     * Returns 0.5f (centered).
     */
    public float getLayoutAlignmentX(Container p) {
        return 0.5f;
    }

    /**
     * Returns 0.5f (centered).
     */
    public float getLayoutAlignmentY(Container p) {
        return 0.5f;
    }

    public void invalidateLayout(Container p) {}

    // End of LayoutManger2 methods

   /**
     * Links edge <code>e1</code> of component <code>c1</code> to
     * edge <code>e2</code> of component <code>c2</code>,
     * with a fixed distance between the edges. This
     * constraint will cause the assignment
     * <pre>
     *     value(e1, c1) = value(e2, c2) + pad</pre>
     * to take place during all subsequent layout operations.
     *
     * @param   e1 the edge of the dependent
     * @param   c1 the component of the dependent
     * @param   pad the fixed distance between dependent and anchor
     * @param   e2 the edge of the anchor
     * @param   c2 the component of the anchor
     *
     * @see #putConstraint(String, Component, Spring, String, Component)
     */
    public void putConstraint(String e1, Component c1, int pad, String e2, Component c2) {
        putConstraint(e1, c1, Spring.constant(pad), e2, c2);
    }

    /**
     * Links edge <code>e1</code> of component <code>c1</code> to
     * edge <code>e2</code> of component <code>c2</code>. As edge
     * <code>(e2, c2)</code> changes value, edge <code>(e1, c1)</code> will
     * be calculated by taking the (spring) sum of <code>(e2, c2)</code>
     * and <code>s</code>.
     * Each edge must have one of the following values:
     * <code>SpringLayout.NORTH</code>,
     * <code>SpringLayout.SOUTH</code>,
     * <code>SpringLayout.EAST</code>,
     * <code>SpringLayout.WEST</code>,
     * <code>SpringLayout.VERTICAL_CENTER</code>,
     * <code>SpringLayout.HORIZONTAL_CENTER</code> or
     * <code>SpringLayout.BASELINE</code>.
     *
     * @param   e1 the edge of the dependent
     * @param   c1 the component of the dependent
     * @param   s the spring linking dependent and anchor
     * @param   e2 the edge of the anchor
     * @param   c2 the component of the anchor
     *
     * @see #putConstraint(String, Component, int, String, Component)
     * @see #NORTH
     * @see #SOUTH
     * @see #EAST
     * @see #WEST
     * @see #VERTICAL_CENTER
     * @see #HORIZONTAL_CENTER
     * @see #BASELINE
     */
    public void putConstraint(String e1, Component c1, Spring s, String e2, Component c2) {
        putConstraint(e1, c1, Spring.sum(s, getConstraint(e2, c2)));
    }

    private void putConstraint(String e, Component c, Spring s) {
        if (s != null) {
            getConstraints(c).setConstraint(e, s);
        }
     }

    private Constraints applyDefaults(Component c, Constraints constraints) {
        if (constraints == null) {
            constraints = new Constraints();
        }
        if (constraints.c == null) {
            constraints.c = c;
        }
        if (constraints.horizontalHistory.size() < 2) {
            applyDefaults(constraints, WEST, Spring.constant(0), WIDTH,
                          Spring.width(c), constraints.horizontalHistory);
        }
        if (constraints.verticalHistory.size() < 2) {
            applyDefaults(constraints, NORTH, Spring.constant(0), HEIGHT,
                          Spring.height(c), constraints.verticalHistory);
        }
        return constraints;
    }

    private void applyDefaults(Constraints constraints, String name1,
                               Spring spring1, String name2, Spring spring2,
                               List<String> history) {
        if (history.size() == 0) {
            constraints.setConstraint(name1, spring1);
            constraints.setConstraint(name2, spring2);
        } else {
            // At this point there must be exactly one constraint defined already.
            // Check width/height first.
            if (constraints.getConstraint(name2) == null) {
                constraints.setConstraint(name2, spring2);
            } else {
                // If width/height is already defined, install a default for x/y.
                constraints.setConstraint(name1, spring1);
            }
            // Either way, leave the user's constraint topmost on the stack.
            Collections.rotate(history, 1);
        }
    }

    private void putConstraints(Component component, Constraints constraints) {
        componentConstraints.put(component, applyDefaults(component, constraints));
    }

    /**
     * Returns the constraints for the specified component.
     * Note that,
     * unlike the <code>GridBagLayout</code>
     * <code>getConstraints</code> method,
     * this method does not clone constraints.
     * If no constraints
     * have been associated with this component,
     * this method
     * returns a default constraints object positioned at
     * 0,0 relative to the parent's Insets and its width/height
     * constrained to the minimum, maximum, and preferred sizes of the
     * component. The size characteristics
     * are not frozen at the time this method is called;
     * instead this method returns a constraints object
     * whose characteristics track the characteristics
     * of the component as they change.
     *
     * @param       c the component whose constraints will be returned
     *
     * @return      the constraints for the specified component
     */
    public Constraints getConstraints(Component c) {
       Constraints result = componentConstraints.get(c);
       if (result == null) {
           if (c instanceof javax.swing.JComponent) {
                Object cp = ((javax.swing.JComponent)c).getClientProperty(SpringLayout.class);
                if (cp instanceof Constraints) {
                    return applyDefaults(c, (Constraints)cp);
                }
            }
            result = new Constraints();
            putConstraints(c, result);
       }
       return result;
    }

    /**
     * Returns the spring controlling the distance between
     * the specified edge of
     * the component and the top or left edge of its parent. This
     * method, instead of returning the current binding for the
     * edge, returns a proxy that tracks the characteristics
     * of the edge even if the edge is subsequently rebound.
     * Proxies are intended to be used in builder environments
     * where it is useful to allow the user to define the
     * constraints for a layout in any order. Proxies do, however,
     * provide the means to create cyclic dependencies amongst
     * the constraints of a layout. Such cycles are detected
     * internally by <code>SpringLayout</code> so that
     * the layout operation always terminates.
     *
     * @param edgeName must be one of
     * <code>SpringLayout.NORTH</code>,
     * <code>SpringLayout.SOUTH</code>,
     * <code>SpringLayout.EAST</code>,
     * <code>SpringLayout.WEST</code>,
     * <code>SpringLayout.VERTICAL_CENTER</code>,
     * <code>SpringLayout.HORIZONTAL_CENTER</code> or
     * <code>SpringLayout.BASELINE</code>
     * @param c the component whose edge spring is desired
     *
     * @return a proxy for the spring controlling the distance between the
     *         specified edge and the top or left edge of its parent
     *
     * @see #NORTH
     * @see #SOUTH
     * @see #EAST
     * @see #WEST
     * @see #VERTICAL_CENTER
     * @see #HORIZONTAL_CENTER
     * @see #BASELINE
     */
    public Spring getConstraint(String edgeName, Component c) {
        // The interning here is unnecessary; it was added for efficiency.
        edgeName = edgeName.intern();
        return new SpringProxy(edgeName, c, this);
    }

    public void layoutContainer(Container parent) {
        setParent(parent);

        int n = parent.getComponentCount();
        getConstraints(parent).reset();
        for (int i = 0 ; i < n ; i++) {
            getConstraints(parent.getComponent(i)).reset();
        }

        Insets insets = parent.getInsets();
        Constraints pc = getConstraints(parent);
        abandonCycles(pc.getX()).setValue(0);
        abandonCycles(pc.getY()).setValue(0);
        abandonCycles(pc.getWidth()).setValue(parent.getWidth() -
                                              insets.left - insets.right);
        abandonCycles(pc.getHeight()).setValue(parent.getHeight() -
                                               insets.top - insets.bottom);

        for (int i = 0 ; i < n ; i++) {
            Component c = parent.getComponent(i);
            Constraints cc = getConstraints(c);
            int x = abandonCycles(cc.getX()).getValue();
            int y = abandonCycles(cc.getY()).getValue();
            int width = abandonCycles(cc.getWidth()).getValue();
            int height = abandonCycles(cc.getHeight()).getValue();
            c.setBounds(insets.left + x, insets.top + y, width, height);
        }
    }
}
