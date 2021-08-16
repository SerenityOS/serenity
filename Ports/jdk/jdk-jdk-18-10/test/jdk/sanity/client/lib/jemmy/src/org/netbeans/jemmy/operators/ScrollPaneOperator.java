/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
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
package org.netbeans.jemmy.operators;

import java.awt.Adjustable;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.ScrollPane;
import javax.swing.SwingUtilities;
import org.netbeans.jemmy.Action;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.ComponentSearcher;
import org.netbeans.jemmy.JemmyException;
import org.netbeans.jemmy.Outputable;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.TimeoutExpiredException;
import org.netbeans.jemmy.Timeoutable;
import org.netbeans.jemmy.Timeouts;
import org.netbeans.jemmy.drivers.DriverManager;
import org.netbeans.jemmy.drivers.ScrollDriver;
import org.netbeans.jemmy.drivers.scrolling.ScrollAdjuster;

/**
 * <BR><BR>Timeouts used: <BR>
 * ScrollbarOperator.WholeScrollTimeout - time for one scroll click <BR>
 * ComponentOperator.WaitComponentTimeout - time to wait component displayed
 * <BR>.
 *
 * @see org.netbeans.jemmy.Timeouts
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 *
 */
public class ScrollPaneOperator extends ContainerOperator<ScrollPane>
        implements Timeoutable, Outputable {

    private static int X_POINT_RECT_SIZE = 6;
    private static int Y_POINT_RECT_SIZE = 4;
    private Timeouts timeouts;
    private TestOut output;
    private ScrollDriver driver;

    /**
     * Constructor.
     *
     * @param b The {@code java.awt.ScrollPane} managed by this instance.
     */
    public ScrollPaneOperator(ScrollPane b) {
        super(b);
        driver = DriverManager.getScrollDriver(getClass());
    }

    /**
     * Constructs a ScrollPaneOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     */
    public ScrollPaneOperator(ContainerOperator<?> cont, ComponentChooser chooser, int index) {
        this((ScrollPane) cont.
                waitSubComponent(new ScrollPaneFinder(chooser),
                        index));
        copyEnvironment(cont);
    }

    /**
     * Constructs a ScrollPaneOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     */
    public ScrollPaneOperator(ContainerOperator<?> cont, ComponentChooser chooser) {
        this(cont, chooser, 0);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont Operator pointing a container to search component in.
     * @param index Ordinal component index.
     * @throws TimeoutExpiredException
     */
    public ScrollPaneOperator(ContainerOperator<?> cont, int index) {
        this((ScrollPane) waitComponent(cont,
                new ScrollPaneFinder(),
                index));
        copyEnvironment(cont);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont Operator pointing a container to search component in.
     * @throws TimeoutExpiredException
     */
    public ScrollPaneOperator(ContainerOperator<?> cont) {
        this(cont, 0);
    }

    /**
     * Searches ScrollPane in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @return ScrollPane instance or null if component was not found.
     */
    public static ScrollPane findScrollPane(Container cont, ComponentChooser chooser, int index) {
        return (ScrollPane) findComponent(cont, new ScrollPaneFinder(chooser), index);
    }

    /**
     * Searches 0'th ScrollPane in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return ScrollPane instance or null if component was not found.
     */
    public static ScrollPane findScrollPane(Container cont, ComponentChooser chooser) {
        return findScrollPane(cont, chooser, 0);
    }

    /**
     * Searches ScrollPane in container.
     *
     * @param cont Container to search component in.
     * @param index Ordinal component index.
     * @return ScrollPane instance or null if component was not found.
     */
    public static ScrollPane findScrollPane(Container cont, int index) {
        return findScrollPane(cont, ComponentSearcher.getTrueChooser(Integer.toString(index) + "'th ScrollPane instance"), index);
    }

    /**
     * Searches 0'th ScrollPane in container.
     *
     * @param cont Container to search component in.
     * @return ScrollPane instance or null if component was not found.
     */
    public static ScrollPane findScrollPane(Container cont) {
        return findScrollPane(cont, 0);
    }

    /**
     * Searches ScrollPane object which component lies on.
     *
     * @param comp Component to find ScrollPane under.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return ScrollPane instance or null if component was not found.
     */
    public static ScrollPane findScrollPaneUnder(Component comp, ComponentChooser chooser) {
        return (ScrollPane) findContainerUnder(comp, new ScrollPaneFinder(chooser));
    }

    /**
     * Searches ScrollPane object which component lies on.
     *
     * @param comp Component to find ScrollPane under.
     * @return ScrollPane instance or null if component was not found.
     */
    public static ScrollPane findScrollPaneUnder(Component comp) {
        return findScrollPaneUnder(comp, new ScrollPaneFinder());
    }

    /**
     * Waits ScrollPane in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @return ScrollPane instance or null if component was not displayed.
     * @throws TimeoutExpiredException
     */
    public static ScrollPane waitScrollPane(Container cont, ComponentChooser chooser, int index) {
        return (ScrollPane) waitComponent(cont, new ScrollPaneFinder(chooser), index);
    }

    /**
     * Waits 0'th ScrollPane in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return ScrollPane instance or null if component was not displayed.
     * @throws TimeoutExpiredException
     */
    public static ScrollPane waitScrollPane(Container cont, ComponentChooser chooser) {
        return waitScrollPane(cont, chooser, 0);
    }

    /**
     * Waits ScrollPane in container.
     *
     * @param cont Container to search component in.
     * @param index Ordinal component index.
     * @return ScrollPane instance or null if component was not displayed.
     * @throws TimeoutExpiredException
     */
    public static ScrollPane waitScrollPane(Container cont, int index) {
        return waitScrollPane(cont, ComponentSearcher.getTrueChooser(Integer.toString(index) + "'th ScrollPane instance"), index);
    }

    /**
     * Waits 0'th ScrollPane in container.
     *
     * @param cont Container to search component in.
     * @return ScrollPane instance or null if component was not displayed.
     * @throws TimeoutExpiredException
     */
    public static ScrollPane waitScrollPane(Container cont) {
        return waitScrollPane(cont, 0);
    }

    static {
        try {
            Class.forName("org.netbeans.jemmy.operators.ScrollbarOperator");
        } catch (Exception e) {
            throw (new JemmyException("Exception", e));
        }
    }

    @Override
    public void setTimeouts(Timeouts timeouts) {
        super.setTimeouts(timeouts);
        this.timeouts = timeouts;
    }

    @Override
    public Timeouts getTimeouts() {
        return timeouts;
    }

    @Override
    public void setOutput(TestOut out) {
        output = out;
        super.setOutput(output.createErrorOutput());
    }

    @Override
    public TestOut getOutput() {
        return output;
    }

    @Override
    public void copyEnvironment(Operator anotherOperator) {
        super.copyEnvironment(anotherOperator);
        driver
                = (ScrollDriver) DriverManager.
                getDriver(DriverManager.SCROLL_DRIVER_ID,
                        getClass(),
                        anotherOperator.getProperties());
    }

    /**
     * Sets both values.
     *
     * @param x a horizontal value.
     * @param y a vertical value.
     */
    public void setValues(int x, int y) {
        getHAdjustable().setValue(x);
        getVAdjustable().setValue(y);
    }

    /**
     * Scrools to the position defined by a ScrollAdjuster instance.
     *
     * @param adj specifies the position.
     */
    public void scrollTo(final ScrollAdjuster adj) {
        produceTimeRestricted(new Action<Void, Void>() {
            @Override
            public Void launch(Void obj) {
                driver.scroll(ScrollPaneOperator.this, adj);
                return null;
            }

            @Override
            public String getDescription() {
                return "Scrolling";
            }

            @Override
            public String toString() {
                return "ScrollPaneOperator.scrollTo.Action{description = " + getDescription() + '}';
            }
        }, "ScrollbarOperator.WholeScrollTimeout");
    }

    /**
     * Scrolls horizontal scroll bar.
     *
     * @param value Value to scroll horizontal scroll bar to.
     * @throws TimeoutExpiredException
     */
    public void scrollToHorizontalValue(final int value) {
        output.printTrace("Scroll ScrollPane to " + Integer.toString(value) + " horizontal value \n"
                + toStringSource());
        output.printGolden("Scroll ScrollPane to " + Integer.toString(value) + " horizontal value");
        scrollTo(new ValueScrollAdjuster(value,
                Adjustable.HORIZONTAL,
                getHAdjustable()));
    }

    /**
     * Scrolls horizontal scroll bar.
     *
     * @param proportionalValue Proportional value to scroll horizontal scroll
     * bar to.
     * @throws TimeoutExpiredException
     */
    public void scrollToHorizontalValue(double proportionalValue) {
        output.printTrace("Scroll ScrollPane to " + Double.toString(proportionalValue) + " proportional horizontal value \n"
                + toStringSource());
        output.printGolden("Scroll ScrollPane to " + Double.toString(proportionalValue) + " proportional horizontal value");
        Adjustable adj = getHAdjustable();
        scrollTo(new ValueScrollAdjuster((int) (adj.getMinimum()
                + (adj.getMaximum()
                - adj.getVisibleAmount()
                - adj.getMinimum()) * proportionalValue),
                Adjustable.VERTICAL,
                getVAdjustable()));
    }

    /**
     * Scrolls vertical scroll bar.
     *
     * @param value Value to scroll vertical scroll bar to.
     * @throws TimeoutExpiredException
     */
    public void scrollToVerticalValue(final int value) {
        output.printTrace("Scroll ScrollPane to " + Integer.toString(value) + " vertical value \n"
                + toStringSource());
        output.printGolden("Scroll ScrollPane to " + Integer.toString(value) + " vertical value");
        scrollTo(new ValueScrollAdjuster(value,
                Adjustable.VERTICAL,
                getVAdjustable()));
    }

    /**
     * Scrolls vertical scroll bar.
     *
     * @param proportionalValue Value to scroll vertical scroll bar to.
     * @throws TimeoutExpiredException
     */
    public void scrollToVerticalValue(double proportionalValue) {
        output.printTrace("Scroll ScrollPane to " + Double.toString(proportionalValue) + " proportional vertical value \n"
                + toStringSource());
        output.printGolden("Scroll ScrollPane to " + Double.toString(proportionalValue) + " proportional vertical value");
        Adjustable adj = getVAdjustable();
        scrollTo(new ValueScrollAdjuster((int) (adj.getMinimum()
                + (adj.getMaximum()
                - adj.getVisibleAmount()
                - adj.getMinimum()) * proportionalValue),
                Adjustable.VERTICAL,
                getVAdjustable()));
    }

    /**
     * Scrolls both scroll bars.
     *
     * @param valueX Value to scroll horizontal scroll bar to.
     * @param valueY Value to scroll vertical scroll bar to.
     * @throws TimeoutExpiredException
     */
    public void scrollToValues(int valueX, int valueY) {
        scrollToVerticalValue(valueX);
        scrollToHorizontalValue(valueX);
    }

    /**
     * Scrolls both scroll bars.
     *
     * @param proportionalValueX Value to scroll horizontal scroll bar to.
     * @param proportionalValueY Value to scroll vertical scroll bar to.
     * @throws TimeoutExpiredException
     */
    public void scrollToValues(double proportionalValueX, double proportionalValueY) {
        scrollToVerticalValue(proportionalValueX);
        scrollToHorizontalValue(proportionalValueY);
    }

    /**
     * Scrolls pane to top.
     *
     * @throws TimeoutExpiredException
     */
    public void scrollToTop() {
        output.printTrace("Scroll ScrollPane to top\n"
                + toStringSource());
        output.printGolden("Scroll ScrollPane to top");
        produceTimeRestricted(new Action<Void, Void>() {
            @Override
            public Void launch(Void obj) {
                driver.scrollToMinimum(ScrollPaneOperator.this, Adjustable.VERTICAL);
                return null;
            }

            @Override
            public String getDescription() {
                return "Scrolling";
            }

            @Override
            public String toString() {
                return "ScrollPaneOperator.scrollToTop.Action{description = " + getDescription() + '}';
            }
        }, "ScrollbarOperator.WholeScrollTimeout");
    }

    /**
     * Scrolls pane to bottom.
     *
     * @throws TimeoutExpiredException
     */
    public void scrollToBottom() {
        output.printTrace("Scroll ScrollPane to bottom\n"
                + toStringSource());
        output.printGolden("Scroll ScrollPane to bottom");
        produceTimeRestricted(new Action<Void, Void>() {
            @Override
            public Void launch(Void obj) {
                driver.scrollToMaximum(ScrollPaneOperator.this, Adjustable.VERTICAL);
                return null;
            }

            @Override
            public String getDescription() {
                return "Scrolling";
            }

            @Override
            public String toString() {
                return "ScrollPaneOperator.scrollToBottom.Action{description = " + getDescription() + '}';
            }
        }, "ScrollbarOperator.WholeScrollTimeout");
    }

    /**
     * Scrolls pane to left.
     *
     * @throws TimeoutExpiredException
     */
    public void scrollToLeft() {
        output.printTrace("Scroll ScrollPane to left\n"
                + toStringSource());
        output.printGolden("Scroll ScrollPane to left");
        produceTimeRestricted(new Action<Void, Void>() {
            @Override
            public Void launch(Void obj) {
                driver.scrollToMinimum(ScrollPaneOperator.this, Adjustable.HORIZONTAL);
                return null;
            }

            @Override
            public String getDescription() {
                return "Scrolling";
            }

            @Override
            public String toString() {
                return "ScrollPaneOperator.scrollToLeft.Action{description = " + getDescription() + '}';
            }
        }, "ScrollbarOperator.WholeScrollTimeout");
    }

    /**
     * Scrolls pane to right.
     *
     * @throws TimeoutExpiredException
     */
    public void scrollToRight() {
        output.printTrace("Scroll ScrollPane to right\n"
                + toStringSource());
        output.printGolden("Scroll ScrollPane to right");
        produceTimeRestricted(new Action<Void, Void>() {
            @Override
            public Void launch(Void obj) {
                driver.scrollToMaximum(ScrollPaneOperator.this, Adjustable.HORIZONTAL);
                return null;
            }

            @Override
            public String getDescription() {
                return "Scrolling";
            }

            @Override
            public String toString() {
                return "ScrollPaneOperator.scrollToRight.Action{description = " + getDescription() + '}';
            }
        }, "ScrollbarOperator.WholeScrollTimeout");
    }

    /**
     * Scrolls pane to rectangle..
     *
     * @param comp a subcomponent defining coordinate system.
     * @param x coordinate
     * @param y coordinate
     * @param width rectangle width
     * @param height rectangle height
     * @throws TimeoutExpiredException
     */
    public void scrollToComponentRectangle(Component comp, int x, int y, int width, int height) {
        scrollTo(new ComponentRectChecker(comp, x, y, width, height, Adjustable.HORIZONTAL));
        scrollTo(new ComponentRectChecker(comp, x, y, width, height, Adjustable.VERTICAL));
    }

    /**
     * Scrolls pane to point.
     *
     * @param comp a subcomponent defining coordinate system.
     * @param x coordinate
     * @param y coordinate
     * @throws TimeoutExpiredException
     */
    public void scrollToComponentPoint(Component comp, int x, int y) {
        scrollToComponentRectangle(comp,
                x - X_POINT_RECT_SIZE,
                y - Y_POINT_RECT_SIZE,
                2 * X_POINT_RECT_SIZE,
                2 * Y_POINT_RECT_SIZE);
    }

    /**
     * Scrolls pane to component on this pane. Component should lay on the
     * ScrollPane view.
     *
     * @param comp Component to scroll to.
     * @throws TimeoutExpiredException
     */
    public void scrollToComponent(final Component comp) {
        String componentToString = runMapping(
                new Operator.MapAction<String>("comp.toString()") {
            @Override
            public String map() {
                return comp.toString();
            }
        }
        );
        output.printTrace("Scroll ScrollPane " + toStringSource()
                + "\nto component " + componentToString);
        output.printGolden("Scroll ScrollPane to " + comp.getClass().getName() + " component.");
        scrollToComponentRectangle(comp, 0, 0, comp.getWidth(), comp.getHeight());
    }

    /**
     * Checks if component's rectangle is inside view port (no scrolling
     * necessary).
     *
     * @param comp a subcomponent defining coordinate system.
     * @param x coordinate
     * @param y coordinate
     * @param width rectangle width
     * @param height rectangle height
     * @return true if pointed subcomponent rectangle is inside the scrolling
     * area.
     */
    public boolean checkInside(Component comp, int x, int y, int width, int height) {
        Point toPoint = SwingUtilities.
                convertPoint(comp, x, y, getSource());
        if (toPoint.x < getHAdjustable().getValue()) {
            return false;
        }
        if (comp.getWidth() > getSource().getWidth()) {
            if (toPoint.x > 0) {
                return false;
            }
        } else if (toPoint.x + comp.getWidth()
                > getHAdjustable().getValue() + getSource().getWidth()) {
            return false;
        }
        if (toPoint.y < getVAdjustable().getValue()) {
            return false;
        }
        if (comp.getHeight() > getSource().getHeight()) {
            if (toPoint.y > 0) {
                return false;
            }
        } else if (toPoint.y + comp.getHeight()
                > getVAdjustable().getValue() + getSource().getHeight()) {
            return false;
        }
        return true;
    }

    /**
     * Checks if component is inside view port (no scrolling necessary).
     *
     * @param comp a subcomponent defining coordinate system.
     * @return true if pointed subcomponent is inside the scrolling area.
     */
    public boolean checkInside(Component comp) {
        return checkInside(comp, 0, 0, comp.getWidth(), comp.getHeight());
    }

    /**
     * Tells if a scrollbar is visible.
     *
     * @param orientation {@code Adjustable.HORIZONTAL} or
     * {@code Adjustable.VERTICAL}
     * @return trus if the bar is visible.
     */
    public boolean isScrollbarVisible(int orientation) {
        if (orientation == Adjustable.HORIZONTAL) {
            return getViewportSize().getHeight() < getHeight() - getHScrollbarHeight();
        } else if (orientation == Adjustable.VERTICAL) {
            return getViewportSize().getWidth() < getWidth() - getVScrollbarWidth();
        } else {
            return false;
        }
    }

    ////////////////////////////////////////////////////////
    //Mapping                                             //
    /**
     * Maps {@code ScrollPane.getHAdjustable()} through queue
     */
    public Adjustable getHAdjustable() {
        return (runMapping(new MapAction<Adjustable>("getHAdjustable") {
            @Override
            public Adjustable map() {
                return ((ScrollPane) getSource()).getHAdjustable();
            }
        }));
    }

    /**
     * Maps {@code ScrollPane.getHScrollbarHeight()} through queue
     */
    public int getHScrollbarHeight() {
        return (runMapping(new MapIntegerAction("getHScrollbarHeight") {
            @Override
            public int map() {
                return ((ScrollPane) getSource()).getHScrollbarHeight();
            }
        }));
    }

    /**
     * Maps {@code ScrollPane.getScrollPosition()} through queue
     */
    public Point getScrollPosition() {
        return (runMapping(new MapAction<Point>("getScrollPosition") {
            @Override
            public Point map() {
                return ((ScrollPane) getSource()).getScrollPosition();
            }
        }));
    }

    /**
     * Maps {@code ScrollPane.getScrollbarDisplayPolicy()} through queue
     */
    public int getScrollbarDisplayPolicy() {
        return (runMapping(new MapIntegerAction("getScrollbarDisplayPolicy") {
            @Override
            public int map() {
                return ((ScrollPane) getSource()).getScrollbarDisplayPolicy();
            }
        }));
    }

    /**
     * Maps {@code ScrollPane.getVAdjustable()} through queue
     */
    public Adjustable getVAdjustable() {
        return (runMapping(new MapAction<Adjustable>("getVAdjustable") {
            @Override
            public Adjustable map() {
                return ((ScrollPane) getSource()).getVAdjustable();
            }
        }));
    }

    /**
     * Maps {@code ScrollPane.getVScrollbarWidth()} through queue
     */
    public int getVScrollbarWidth() {
        return (runMapping(new MapIntegerAction("getVScrollbarWidth") {
            @Override
            public int map() {
                return ((ScrollPane) getSource()).getVScrollbarWidth();
            }
        }));
    }

    /**
     * Maps {@code ScrollPane.getViewportSize()} through queue
     */
    public Dimension getViewportSize() {
        return (runMapping(new MapAction<Dimension>("getViewportSize") {
            @Override
            public Dimension map() {
                return ((ScrollPane) getSource()).getViewportSize();
            }
        }));
    }

    /**
     * Maps {@code ScrollPane.paramString()} through queue
     */
    public String paramString() {
        return (runMapping(new MapAction<String>("paramString") {
            @Override
            public String map() {
                return ((ScrollPane) getSource()).paramString();
            }
        }));
    }

    /**
     * Maps {@code ScrollPane.setScrollPosition(int, int)} through queue
     */
    public void setScrollPosition(final int i, final int i1) {
        runMapping(new MapVoidAction("setScrollPosition") {
            @Override
            public void map() {
                ((ScrollPane) getSource()).setScrollPosition(i, i1);
            }
        });
    }

    /**
     * Maps {@code ScrollPane.setScrollPosition(Point)} through queue
     */
    public void setScrollPosition(final Point point) {
        runMapping(new MapVoidAction("setScrollPosition") {
            @Override
            public void map() {
                ((ScrollPane) getSource()).setScrollPosition(point);
            }
        });
    }

    //End of mapping                                      //
    ////////////////////////////////////////////////////////
    private static class ValueScrollAdjuster implements ScrollAdjuster {

        int value;
        int orientation;
        Adjustable adj;

        public ValueScrollAdjuster(int value, int orientation, Adjustable adj) {
            this.value = value;
            this.orientation = orientation;
            this.adj = adj;
        }

        @Override
        public int getScrollDirection() {
            if (adj.getValue() == value) {
                return ScrollAdjuster.DO_NOT_TOUCH_SCROLL_DIRECTION;
            } else {
                return ((adj.getValue() < value)
                        ? ScrollAdjuster.INCREASE_SCROLL_DIRECTION
                        : ScrollAdjuster.DECREASE_SCROLL_DIRECTION);
            }
        }

        @Override
        public int getScrollOrientation() {
            return orientation;
        }

        @Override
        public String getDescription() {
            return "Scroll to " + Integer.toString(value) + " value";
        }

        @Override
        public String toString() {
            return "ValueScrollAdjuster{" + "value=" + value + ", orientation=" + orientation + ", adj=" + adj + '}';
        }
    }

    private class ComponentRectChecker implements ScrollAdjuster {

        Component comp;
        int x;
        int y;
        int width;
        int height;
        int orientation;

        public ComponentRectChecker(Component comp, int x, int y, int width, int height, int orientation) {
            this.comp = comp;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.orientation = orientation;
        }

        @Override
        public int getScrollDirection() {
            int sp = (orientation == Adjustable.HORIZONTAL)
                    ? (int) getScrollPosition().getX()
                    : (int) getScrollPosition().getY();
            Point pnt = SwingUtilities.convertPoint(comp, x, y, ((Container) getSource()).getComponents()[0]);
            int cp = (orientation == Adjustable.HORIZONTAL)
                    ? pnt.x
                    : pnt.y;
            int sl = (orientation == Adjustable.HORIZONTAL)
                    ? (int) getViewportSize().getWidth()
                    : (int) getViewportSize().getHeight();
            int cl = (orientation == Adjustable.HORIZONTAL)
                    ? width
                    : height;
            if (cp <= sp) {
                return ScrollAdjuster.DECREASE_SCROLL_DIRECTION;
            } else if ((cp + cl) > (sp + sl)
                    && cp > sp) {
                return ScrollAdjuster.INCREASE_SCROLL_DIRECTION;
            } else {
                return ScrollAdjuster.DO_NOT_TOUCH_SCROLL_DIRECTION;
            }
        }

        @Override
        public int getScrollOrientation() {
            return orientation;
        }

        @Override
        public String getDescription() {
            return "";
        }

        @Override
        public String toString() {
            return "ComponentRectChecker{" + "comp=" + comp + ", x=" + x + ", y=" + y + ", width=" + width + ", height=" + height + ", orientation=" + orientation + '}';
        }
    }

    /**
     * Checks component type.
     */
    public static class ScrollPaneFinder extends Finder {

        /**
         * Constructs ScrollPaneFinder.
         *
         * @param sf other searching criteria.
         */
        public ScrollPaneFinder(ComponentChooser sf) {
            super(ScrollPane.class, sf);
        }

        /**
         * Constructs ScrollPaneFinder.
         */
        public ScrollPaneFinder() {
            super(ScrollPane.class);
        }
    }
}
