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

import java.awt.Component;
import java.awt.Container;
import java.util.Hashtable;

import javax.swing.JButton;
import javax.swing.JSplitPane;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneDivider;

import org.netbeans.jemmy.Action;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.ComponentSearcher;
import org.netbeans.jemmy.Outputable;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.TimeoutExpiredException;
import org.netbeans.jemmy.Timeoutable;
import org.netbeans.jemmy.Timeouts;
import org.netbeans.jemmy.drivers.DriverManager;
import org.netbeans.jemmy.drivers.ScrollDriver;
import org.netbeans.jemmy.drivers.scrolling.ScrollAdjuster;
import org.netbeans.jemmy.util.EmptyVisualizer;

/**
 * <BR><BR>Timeouts used: <BR>
 * JSplitPaneOperator.ScrollClickTimeout - time for simple scroll click <BR>
 * JSplitPaneOperator.BetweenClickTimeout - time to sleep between scroll clicks
 * <BR>
 * JSplitPaneOperator.WholeScrollTimeout - time for the whole scrolling <BR>
 * ComponentOperator.WaitComponentTimeout - time to wait component displayed
 * <BR>.
 *
 * @see org.netbeans.jemmy.Timeouts
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 *
 * Class to operate with javax.swing.JSplitPane component
 *
 */
public class JSplitPaneOperator extends JComponentOperator
        implements Timeoutable, Outputable {

    /**
     * Identifier for a "minimum" property.
     *
     * @see #getDump
     */
    public static final String MINIMUM_DPROP = "Minimum";

    /**
     * Identifier for a "maximum" property.
     *
     * @see #getDump
     */
    public static final String MAXIMUM_DPROP = "Maximum";

    /**
     * Identifier for a "value" property.
     *
     * @see #getDump
     */
    public static final String VALUE_DPROP = "Value";

    /**
     * Identifier for a "orientation" property.
     *
     * @see #getDump
     */
    public static final String ORIENTATION_DPROP = "Orientation";

    /**
     * Identifier for a "HORIZONTAL" value of "orientation" property.
     *
     * @see #getDump
     */
    public static final String HORIZONTAL_ORIENTATION_DPROP_VALUE = "HORIZONTAL";

    /**
     * Identifier for a "VERTICAL" value of "orientation" property.
     *
     * @see #getDump
     */
    public static final String VERTICAL_ORIENTATION_DPROP_VALUE = "VERTICAL";

    /**
     * Identifier for a "one touch expendable" property.
     *
     * @see #getDump
     */
    public static final String IS_ONE_TOUCH_EXPANDABLE_DPROP = "One touch expandable";

    private final static long SCROLL_CLICK_TIMEOUT = 0;
    private final static long BETWEEN_CLICK_TIMEOUT = 0;
    private final static long WHOLE_SCROLL_TIMEOUT = 60000;

    private Timeouts timeouts;
    private TestOut output;
    private ContainerOperator<?> divider;
    private ScrollDriver driver;

    /**
     * Constructor.
     *
     * @param b JSplitPane component.
     */
    public JSplitPaneOperator(JSplitPane b) {
        super(b);
        driver = DriverManager.getScrollDriver(getClass());
    }

    /**
     * Constructs a JSplitPaneOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     */
    public JSplitPaneOperator(ContainerOperator<?> cont, ComponentChooser chooser, int index) {
        this((JSplitPane) cont.
                waitSubComponent(new JSplitPaneFinder(chooser),
                        index));
        copyEnvironment(cont);
    }

    /**
     * Constructs a JSplitPaneOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     */
    public JSplitPaneOperator(ContainerOperator<?> cont, ComponentChooser chooser) {
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
    public JSplitPaneOperator(ContainerOperator<?> cont, int index) {
        this((JSplitPane) waitComponent(cont,
                new JSplitPaneFinder(),
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
    public JSplitPaneOperator(ContainerOperator<?> cont) {
        this(cont, 0);
    }

    /**
     * Searches JSplitPane in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @return JSplitPane instance or null if component was not found.
     */
    public static JSplitPane findJSplitPane(Container cont, ComponentChooser chooser, int index) {
        return (JSplitPane) findComponent(cont, new JSplitPaneFinder(chooser), index);
    }

    /**
     * Searches 0'th JSplitPane in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return JSplitPane instance or null if component was not found.
     */
    public static JSplitPane findJSplitPane(Container cont, ComponentChooser chooser) {
        return findJSplitPane(cont, chooser, 0);
    }

    /**
     * Searches JSplitPane in container.
     *
     * @param cont Container to search component in.
     * @param index Ordinal component index.
     * @return JSplitPane instance or null if component was not found.
     */
    public static JSplitPane findJSplitPane(Container cont, int index) {
        return findJSplitPane(cont, ComponentSearcher.getTrueChooser(Integer.toString(index) + "'th JSplitPane instance"), index);
    }

    /**
     * Searches 0'th JSplitPane in container.
     *
     * @param cont Container to search component in.
     * @return JSplitPane instance or null if component was not found.
     */
    public static JSplitPane findJSplitPane(Container cont) {
        return findJSplitPane(cont, 0);
    }

    /**
     * Searches JSplitPane object which component lies on.
     *
     * @param comp Component to find JSplitPane under.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return JSplitPane instance or null if component was not found.
     */
    public static JSplitPane findJSplitPaneUnder(Component comp, ComponentChooser chooser) {
        return (JSplitPane) findContainerUnder(comp, new JSplitPaneFinder(chooser));
    }

    /**
     * Searches JSplitPane object which component lies on.
     *
     * @param comp Component to find JSplitPane under.
     * @return JSplitPane instance or null if component was not found.
     */
    public static JSplitPane findJSplitPaneUnder(Component comp) {
        return findJSplitPaneUnder(comp, new JSplitPaneFinder());
    }

    /**
     * Waits JSplitPane in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @return JSplitPane instance or null if component was not displayed.
     * @throws TimeoutExpiredException
     */
    public static JSplitPane waitJSplitPane(Container cont, ComponentChooser chooser, int index) {
        return (JSplitPane) waitComponent(cont, new JSplitPaneFinder(chooser), index);
    }

    /**
     * Waits 0'th JSplitPane in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return JSplitPane instance or null if component was not displayed.
     * @throws TimeoutExpiredException
     */
    public static JSplitPane waitJSplitPane(Container cont, ComponentChooser chooser) {
        return waitJSplitPane(cont, chooser, 0);
    }

    /**
     * Waits JSplitPane in container.
     *
     * @param cont Container to search component in.
     * @param index Ordinal component index.
     * @return JSplitPane instance or null if component was not displayed.
     * @throws TimeoutExpiredException
     */
    public static JSplitPane waitJSplitPane(Container cont, int index) {
        return waitJSplitPane(cont, ComponentSearcher.getTrueChooser(Integer.toString(index) + "'th JSplitPane instance"), index);
    }

    /**
     * Waits 0'th JSplitPane in container.
     *
     * @param cont Container to search component in.
     * @return JSplitPane instance or null if component was not displayed.
     * @throws TimeoutExpiredException
     */
    public static JSplitPane waitJSplitPane(Container cont) {
        return waitJSplitPane(cont, 0);
    }

    static {
        Timeouts.initDefault("JSplitPaneOperator.ScrollClickTimeout", SCROLL_CLICK_TIMEOUT);
        Timeouts.initDefault("JSplitPaneOperator.BetweenClickTimeout", BETWEEN_CLICK_TIMEOUT);
        Timeouts.initDefault("JSplitPaneOperator.WholeScrollTimeout", WHOLE_SCROLL_TIMEOUT);
    }

    @Override
    public void setTimeouts(Timeouts timeouts) {
        this.timeouts = timeouts;
        Timeouts times = timeouts;
        times.setTimeout("ComponentOperator.BeforeDragTimeout",
                0);
        times.setTimeout("ComponentOperator.AfterDragTimeout",
                times.getTimeout("JSplitPaneOperator.ScrollClickTimeout"));
        super.setTimeouts(times);
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
     * Searches divider inside split pane.
     *
     * @return an operator for the divider.
     */
    public BasicSplitPaneDivider findDivider() {
        return ((BasicSplitPaneDivider) waitSubComponent(new ComponentChooser() {
            @Override
            public boolean checkComponent(Component comp) {
                return comp instanceof BasicSplitPaneDivider;
            }

            @Override
            public String getDescription() {
                return "";
            }

            @Override
            public String toString() {
                return "JSplitPaneOperator.findDivider.ComponentChooser{description = " + getDescription() + '}';
            }
        }));
    }

    /**
     * Searches divider inside split pane.
     *
     * @return an operator for the divider.
     */
    public ContainerOperator<?> getDivider() {
        if (divider == null) {
            divider = new ContainerOperator<>(findDivider());
            divider.copyEnvironment(this);
            divider.setOutput(getOutput().createErrorOutput());
        }
        return divider;
    }

    /**
     * Scrolls to the position defined by a ScrollAdjuster implementation.
     *
     * @param adj defines scrolling direction, and so on.
     * @throws TimeoutExpiredException
     */
    public void scrollTo(final ScrollAdjuster adj) {
        produceTimeRestricted(new Action<Void, Void>() {
            @Override
            public Void launch(Void obj) {
                driver.scroll(JSplitPaneOperator.this, adj);
                return null;
            }

            @Override
            public String getDescription() {
                return "Moving a divider";
            }

            @Override
            public String toString() {
                return "JSplitPaneOperator.scrollTo.Action{description = " + getDescription() + '}';
            }
        }, "JSplitPaneOperator.WholeScrollTimeout");
    }

    /**
     * Changes divider location.
     *
     * @param dividerLocation location to move divider to.
     */
    public void moveDivider(int dividerLocation) {
        output.printTrace("Move JSplitPane divider to " + Integer.toString(dividerLocation)
                + " location. JSplitPane :    \n" + toStringSource());
        output.printGolden("Move JSplitPane divider to " + Integer.toString(dividerLocation)
                + " location");
        scrollTo(new ValueScrollAdjuster(dividerLocation));
    }

    /**
     * Changes divider location.
     *
     * @param proportionalLocation Proportional location. Should be great then 0
     * and less then 1.
     */
    public void moveDivider(double proportionalLocation) {
        output.printTrace("Move JSplitPane divider to " + Double.toString(proportionalLocation)
                + " proportional location. JSplitPane :    \n" + toStringSource());
        output.printGolden("Move JSplitPane divider to " + Double.toString(proportionalLocation)
                + " proportional location");
        scrollTo(new ValueScrollAdjuster(getMinimumDividerLocation()
                + (int) (proportionalLocation
                * (getMaximumDividerLocation() - getMinimumDividerLocation()))));
    }

    /**
     * Moves the divider all the way to the left/top.
     */
    public void moveToMinimum() {
        output.printTrace("Scroll JSplitPane to minimum. JSplitPane :    \n" + toStringSource());
        output.printGolden("Scroll JSplitPane to minimum.");
        produceTimeRestricted(new Action<Void, Void>() {
            @Override
            public Void launch(Void obj) {
                driver.scrollToMinimum(JSplitPaneOperator.this, getOrientation());
                return null;
            }

            @Override
            public String getDescription() {
                return "Scrolling";
            }

            @Override
            public String toString() {
                return "JSplitPaneOperator.moveToMinimum.Action{description = " + getDescription() + '}';
            }
        }, "JSplitPaneOperator.WholeScrollTimeout");
    }

    /**
     * Moves the divider all the way to the right/bottom.
     */
    public void moveToMaximum() {
        output.printTrace("Scroll JSplitPane to maximum. JSplitPane :    \n" + toStringSource());
        output.printGolden("Scroll JSplitPane to maximum.");
        produceTimeRestricted(new Action<Void, Void>() {
            @Override
            public Void launch(Void obj) {
                driver.scrollToMaximum(JSplitPaneOperator.this, getOrientation());
                return null;
            }

            @Override
            public String getDescription() {
                return "Scrolling";
            }

            @Override
            public String toString() {
                return "JSplitPaneOperator.moveToMaximum.Action{description = " + getDescription() + '}';
            }
        }, "JSplitPaneOperator.WholeScrollTimeout");
    }

    /**
     * Pushes one time right(bottom) expand button.
     *
     * @throws TimeoutExpiredException
     */
    public void expandRight() {
        String mess = "Expand ";
        if (getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
            mess = mess + "right";
        } else {
            mess = mess + "bottom";
        }
        output.printTrace(mess + " JSplitPane side. JSplitPane :    \n" + toStringSource());
        output.printGolden(mess + " JSplitPane side.");
        expandTo(0);
    }

    /**
     * Pushes one time left(top) expand button.
     *
     * @throws TimeoutExpiredException
     */
    public void expandLeft() {
        String mess = "Expand ";
        if (getOrientation() == JSplitPane.HORIZONTAL_SPLIT) {
            mess = mess + "left";
        } else {
            mess = mess + "top";
        }
        output.printTrace(mess + " JSplitPane side. JSplitPane :    \n" + toStringSource());
        output.printGolden(mess + " JSplitPane side.");
        expandTo(1);
    }

    @Override
    public Hashtable<String, Object> getDump() {
        Hashtable<String, Object> result = super.getDump();
        result.put(MINIMUM_DPROP, Integer.toString(((JSplitPane) getSource()).getMinimumDividerLocation()));
        result.put(MAXIMUM_DPROP, Integer.toString(((JSplitPane) getSource()).getMaximumDividerLocation()));
        result.put(ORIENTATION_DPROP, (((JSplitPane) getSource()).getOrientation() == JSplitPane.HORIZONTAL_SPLIT)
                ? HORIZONTAL_ORIENTATION_DPROP_VALUE
                : VERTICAL_ORIENTATION_DPROP_VALUE);
        result.put(VALUE_DPROP, Integer.toString(((JSplitPane) getSource()).getDividerLocation()));
        result.put(IS_ONE_TOUCH_EXPANDABLE_DPROP, ((JSplitPane) getSource()).isOneTouchExpandable() ? "true" : "false");
        return result;
    }

    ////////////////////////////////////////////////////////
    //Mapping                                             //
    /**
     * Maps {@code JSplitPane.getBottomComponent()} through queue
     */
    public Component getBottomComponent() {
        return (runMapping(new MapAction<Component>("getBottomComponent") {
            @Override
            public Component map() {
                return ((JSplitPane) getSource()).getBottomComponent();
            }
        }));
    }

    /**
     * Maps {@code JSplitPane.getDividerLocation()} through queue
     */
    public int getDividerLocation() {
        return (runMapping(new MapIntegerAction("getDividerLocation") {
            @Override
            public int map() {
                return ((JSplitPane) getSource()).getDividerLocation();
            }
        }));
    }

    /**
     * Maps {@code JSplitPane.getDividerSize()} through queue
     */
    public int getDividerSize() {
        return (runMapping(new MapIntegerAction("getDividerSize") {
            @Override
            public int map() {
                return ((JSplitPane) getSource()).getDividerSize();
            }
        }));
    }

    /**
     * Maps {@code JSplitPane.getLastDividerLocation()} through queue
     */
    public int getLastDividerLocation() {
        return (runMapping(new MapIntegerAction("getLastDividerLocation") {
            @Override
            public int map() {
                return ((JSplitPane) getSource()).getLastDividerLocation();
            }
        }));
    }

    /**
     * Maps {@code JSplitPane.getLeftComponent()} through queue
     */
    public Component getLeftComponent() {
        return (runMapping(new MapAction<Component>("getLeftComponent") {
            @Override
            public Component map() {
                return ((JSplitPane) getSource()).getLeftComponent();
            }
        }));
    }

    /**
     * Maps {@code JSplitPane.getMaximumDividerLocation()} through queue
     */
    public int getMaximumDividerLocation() {
        return (runMapping(new MapIntegerAction("getMaximumDividerLocation") {
            @Override
            public int map() {
                return ((JSplitPane) getSource()).getMaximumDividerLocation();
            }
        }));
    }

    /**
     * Maps {@code JSplitPane.getMinimumDividerLocation()} through queue
     */
    public int getMinimumDividerLocation() {
        return (runMapping(new MapIntegerAction("getMinimumDividerLocation") {
            @Override
            public int map() {
                return ((JSplitPane) getSource()).getMinimumDividerLocation();
            }
        }));
    }

    /**
     * Maps {@code JSplitPane.getOrientation()} through queue
     */
    public int getOrientation() {
        return (runMapping(new MapIntegerAction("getOrientation") {
            @Override
            public int map() {
                return ((JSplitPane) getSource()).getOrientation();
            }
        }));
    }

    /**
     * Maps {@code JSplitPane.getRightComponent()} through queue
     */
    public Component getRightComponent() {
        return (runMapping(new MapAction<Component>("getRightComponent") {
            @Override
            public Component map() {
                return ((JSplitPane) getSource()).getRightComponent();
            }
        }));
    }

    /**
     * Maps {@code JSplitPane.getTopComponent()} through queue
     */
    public Component getTopComponent() {
        return (runMapping(new MapAction<Component>("getTopComponent") {
            @Override
            public Component map() {
                return ((JSplitPane) getSource()).getTopComponent();
            }
        }));
    }

    /**
     * Maps {@code JSplitPane.getUI()} through queue
     */
    public SplitPaneUI getUI() {
        return (runMapping(new MapAction<SplitPaneUI>("getUI") {
            @Override
            public SplitPaneUI map() {
                return ((JSplitPane) getSource()).getUI();
            }
        }));
    }

    /**
     * Maps {@code JSplitPane.isContinuousLayout()} through queue
     */
    public boolean isContinuousLayout() {
        return (runMapping(new MapBooleanAction("isContinuousLayout") {
            @Override
            public boolean map() {
                return ((JSplitPane) getSource()).isContinuousLayout();
            }
        }));
    }

    /**
     * Maps {@code JSplitPane.isOneTouchExpandable()} through queue
     */
    public boolean isOneTouchExpandable() {
        return (runMapping(new MapBooleanAction("isOneTouchExpandable") {
            @Override
            public boolean map() {
                return ((JSplitPane) getSource()).isOneTouchExpandable();
            }
        }));
    }

    /**
     * Maps {@code JSplitPane.resetToPreferredSizes()} through queue
     */
    public void resetToPreferredSizes() {
        runMapping(new MapVoidAction("resetToPreferredSizes") {
            @Override
            public void map() {
                ((JSplitPane) getSource()).resetToPreferredSizes();
            }
        });
    }

    /**
     * Maps {@code JSplitPane.setBottomComponent(Component)} through queue
     */
    public void setBottomComponent(final Component component) {
        runMapping(new MapVoidAction("setBottomComponent") {
            @Override
            public void map() {
                ((JSplitPane) getSource()).setBottomComponent(component);
            }
        });
    }

    /**
     * Maps {@code JSplitPane.setContinuousLayout(boolean)} through queue
     */
    public void setContinuousLayout(final boolean b) {
        runMapping(new MapVoidAction("setContinuousLayout") {
            @Override
            public void map() {
                ((JSplitPane) getSource()).setContinuousLayout(b);
            }
        });
    }

    /**
     * Maps {@code JSplitPane.setDividerLocation(double)} through queue
     */
    public void setDividerLocation(final double d) {
        runMapping(new MapVoidAction("setDividerLocation") {
            @Override
            public void map() {
                ((JSplitPane) getSource()).setDividerLocation(d);
            }
        });
    }

    /**
     * Maps {@code JSplitPane.setDividerLocation(int)} through queue
     */
    public void setDividerLocation(final int i) {
        runMapping(new MapVoidAction("setDividerLocation") {
            @Override
            public void map() {
                ((JSplitPane) getSource()).setDividerLocation(i);
            }
        });
    }

    /**
     * Maps {@code JSplitPane.setDividerSize(int)} through queue
     */
    public void setDividerSize(final int i) {
        runMapping(new MapVoidAction("setDividerSize") {
            @Override
            public void map() {
                ((JSplitPane) getSource()).setDividerSize(i);
            }
        });
    }

    /**
     * Maps {@code JSplitPane.setLastDividerLocation(int)} through queue
     */
    public void setLastDividerLocation(final int i) {
        runMapping(new MapVoidAction("setLastDividerLocation") {
            @Override
            public void map() {
                ((JSplitPane) getSource()).setLastDividerLocation(i);
            }
        });
    }

    /**
     * Maps {@code JSplitPane.setLeftComponent(Component)} through queue
     */
    public void setLeftComponent(final Component component) {
        runMapping(new MapVoidAction("setLeftComponent") {
            @Override
            public void map() {
                ((JSplitPane) getSource()).setLeftComponent(component);
            }
        });
    }

    /**
     * Maps {@code JSplitPane.setOneTouchExpandable(boolean)} through queue
     */
    public void setOneTouchExpandable(final boolean b) {
        runMapping(new MapVoidAction("setOneTouchExpandable") {
            @Override
            public void map() {
                ((JSplitPane) getSource()).setOneTouchExpandable(b);
            }
        });
    }

    /**
     * Maps {@code JSplitPane.setOrientation(int)} through queue
     */
    public void setOrientation(final int i) {
        runMapping(new MapVoidAction("setOrientation") {
            @Override
            public void map() {
                ((JSplitPane) getSource()).setOrientation(i);
            }
        });
    }

    /**
     * Maps {@code JSplitPane.setRightComponent(Component)} through queue
     */
    public void setRightComponent(final Component component) {
        runMapping(new MapVoidAction("setRightComponent") {
            @Override
            public void map() {
                ((JSplitPane) getSource()).setRightComponent(component);
            }
        });
    }

    /**
     * Maps {@code JSplitPane.setTopComponent(Component)} through queue
     */
    public void setTopComponent(final Component component) {
        runMapping(new MapVoidAction("setTopComponent") {
            @Override
            public void map() {
                ((JSplitPane) getSource()).setTopComponent(component);
            }
        });
    }

    /**
     * Maps {@code JSplitPane.setUI(SplitPaneUI)} through queue
     */
    public void setUI(final SplitPaneUI splitPaneUI) {
        runMapping(new MapVoidAction("setUI") {
            @Override
            public void map() {
                ((JSplitPane) getSource()).setUI(splitPaneUI);
            }
        });
    }

    //End of mapping                                      //
    ////////////////////////////////////////////////////////
    private void expandTo(int index) {
        makeComponentVisible();
        JButtonOperator bo
                = new JButtonOperator((JButton) getDivider().
                        waitSubComponent(new JButtonOperator.JButtonFinder(ComponentSearcher.
                                getTrueChooser("JButton")),
                                index));
        bo.copyEnvironment(getDivider());
        bo.setVisualizer(new EmptyVisualizer());
        bo.push();
    }

    /**
     * Checks component type.
     */
    public static class JSplitPaneFinder extends Finder {

        /**
         * Constructs JSplitPaneFinder.
         *
         * @param sf other searching criteria.
         */
        public JSplitPaneFinder(ComponentChooser sf) {
            super(JSplitPane.class, sf);
        }

        /**
         * Constructs JSplitPaneFinder.
         */
        public JSplitPaneFinder() {
            super(JSplitPane.class);
        }
    }

    private class ValueScrollAdjuster implements ScrollAdjuster {

        int value;

        public ValueScrollAdjuster(int value) {
            this.value = value;
        }

        @Override
        public int getScrollDirection() {
            if (getDividerLocation() == value) {
                return ScrollAdjuster.DO_NOT_TOUCH_SCROLL_DIRECTION;
            } else {
                return ((getDividerLocation() < value)
                        ? ScrollAdjuster.INCREASE_SCROLL_DIRECTION
                        : ScrollAdjuster.DECREASE_SCROLL_DIRECTION);
            }
        }

        @Override
        public int getScrollOrientation() {
            return getOrientation();
        }

        @Override
        public String getDescription() {
            return "Scroll to " + Integer.toString(value) + " value";
        }

        @Override
        public String toString() {
            return "ValueScrollAdjuster{" + "value=" + value + '}';
        }
    }
}
