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
import java.awt.event.AdjustmentListener;
import java.util.Hashtable;

import javax.swing.BoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JScrollBar;
import javax.swing.plaf.ScrollBarUI;

import org.netbeans.jemmy.Action;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.ComponentSearcher;
import org.netbeans.jemmy.Outputable;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.TimeoutExpiredException;
import org.netbeans.jemmy.Timeoutable;
import org.netbeans.jemmy.Timeouts;
import org.netbeans.jemmy.Waitable;
import org.netbeans.jemmy.drivers.DriverManager;
import org.netbeans.jemmy.drivers.ScrollDriver;
import org.netbeans.jemmy.drivers.scrolling.ScrollAdjuster;
import org.netbeans.jemmy.util.EmptyVisualizer;

/**
 *
 * Operator is supposed to be used to operate with an instance of
 * javax.swing.JScrollBar class. <BR><BR>
 *
 *
 * <BR><BR>Timeouts used: <BR>
 * JScrollBarOperator.OneScrollClickTimeout - time for one scroll click <BR>
 * JScrollBarOperator.WholeScrollTimeout - time for the whole scrolling <BR>
 * JScrollBarOperator.BeforeDropTimeout - to sleep before drop
 * JScrollBarOperator.DragAndDropScrollingDelta - to sleep before drag steps
 * ComponentOperator.WaitComponentTimeout - time to wait component displayed
 * <BR>.
 *
 * @see org.netbeans.jemmy.Timeouts
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 */
public class JScrollBarOperator extends JComponentOperator
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

    private final static long ONE_SCROLL_CLICK_TIMEOUT = 0;
    private final static long WHOLE_SCROLL_TIMEOUT = 60000;
    private final static long BEFORE_DROP_TIMEOUT = 0;
    private final static long DRAG_AND_DROP_SCROLLING_DELTA = 0;

    private Timeouts timeouts;
    private TestOut output;
    private JButtonOperator minButtOperator;
    private JButtonOperator maxButtOperator;

    private ScrollDriver driver;

    /**
     * Constructor.
     *
     * @param b JScrollBar component.
     */
    public JScrollBarOperator(JScrollBar b) {
        super(b);
        driver = DriverManager.getScrollDriver(getClass());
    }

    /**
     * Constructs a JScrollBarOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     */
    public JScrollBarOperator(ContainerOperator<?> cont, ComponentChooser chooser, int index) {
        this((JScrollBar) cont.
                waitSubComponent(new JScrollBarFinder(chooser),
                        index));
        copyEnvironment(cont);
    }

    /**
     * Constructs a JScrollBarOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     */
    public JScrollBarOperator(ContainerOperator<?> cont, ComponentChooser chooser) {
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
    public JScrollBarOperator(ContainerOperator<?> cont, int index) {
        this((JScrollBar) waitComponent(cont,
                new JScrollBarFinder(),
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
    public JScrollBarOperator(ContainerOperator<?> cont) {
        this(cont, 0);
    }

    /**
     * Searches JScrollBar in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @return JScrollBar instance or null if component was not found.
     */
    public static JScrollBar findJScrollBar(Container cont, ComponentChooser chooser, int index) {
        return (JScrollBar) findComponent(cont, new JScrollBarFinder(chooser), index);
    }

    /**
     * Searches 0'th JScrollBar in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return JScrollBar instance or null if component was not found.
     */
    public static JScrollBar findJScrollBar(Container cont, ComponentChooser chooser) {
        return findJScrollBar(cont, chooser, 0);
    }

    /**
     * Searches JScrollBar in container.
     *
     * @param cont Container to search component in.
     * @param index Ordinal component index.
     * @return JScrollBar instance or null if component was not found.
     */
    public static JScrollBar findJScrollBar(Container cont, int index) {
        return findJScrollBar(cont, ComponentSearcher.getTrueChooser(Integer.toString(index) + "'th JScrollBar instance"), index);
    }

    /**
     * Searches 0'th JScrollBar in container.
     *
     * @param cont Container to search component in.
     * @return JScrollBar instance or null if component was not found.
     */
    public static JScrollBar findJScrollBar(Container cont) {
        return findJScrollBar(cont, 0);
    }

    /**
     * Waits JScrollBar in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @return JScrollBar instance or null if component was not displayed.
     * @throws TimeoutExpiredException
     */
    public static JScrollBar waitJScrollBar(Container cont, ComponentChooser chooser, int index) {
        return (JScrollBar) waitComponent(cont, new JScrollBarFinder(chooser), index);
    }

    /**
     * Waits 0'th JScrollBar in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return JScrollBar instance or null if component was not displayed.
     * @throws TimeoutExpiredException
     */
    public static JScrollBar waitJScrollBar(Container cont, ComponentChooser chooser) {
        return waitJScrollBar(cont, chooser, 0);
    }

    /**
     * Waits JScrollBar in container.
     *
     * @param cont Container to search component in.
     * @param index Ordinal component index.
     * @return JScrollBar instance or null if component was not displayed.
     * @throws TimeoutExpiredException
     */
    public static JScrollBar waitJScrollBar(Container cont, int index) {
        return waitJScrollBar(cont, ComponentSearcher.getTrueChooser(Integer.toString(index) + "'th JScrollBar instance"), index);
    }

    /**
     * Waits 0'th JScrollBar in container.
     *
     * @param cont Container to search component in.
     * @return JScrollBar instance or null if component was not displayed.
     * @throws TimeoutExpiredException
     */
    public static JScrollBar waitJScrollBar(Container cont) {
        return waitJScrollBar(cont, 0);
    }

    static {
        Timeouts.initDefault("JScrollBarOperator.OneScrollClickTimeout", ONE_SCROLL_CLICK_TIMEOUT);
        Timeouts.initDefault("JScrollBarOperator.WholeScrollTimeout", WHOLE_SCROLL_TIMEOUT);
        Timeouts.initDefault("JScrollBarOperator.BeforeDropTimeout", BEFORE_DROP_TIMEOUT);
        Timeouts.initDefault("JScrollBarOperator.DragAndDropScrollingDelta", DRAG_AND_DROP_SCROLLING_DELTA);
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
    public void setTimeouts(Timeouts timeouts) {
        this.timeouts = timeouts;
        super.setTimeouts(timeouts);
    }

    @Override
    public Timeouts getTimeouts() {
        return timeouts;
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
     * Does simple scroll click.
     *
     * @param increase a scrolling direction.
     * @throws TimeoutExpiredException
     * @deprecated All scrolling is done through a ScrollDriver registered to
     * this operator type.
     */
    @Deprecated
    public void scroll(boolean increase) {
        scrollToValue(getValue() + (increase ? 1 : -1));
    }

    /**
     * Scrolls scrollbar to the position defined by w parameter. Uses
     * ScrollDriver registered to this operator type.
     *
     * @param w Scrolling is stopped when w.actionProduced(waiterParam) != null
     * @param waiterParam a waiting parameter.
     * @param increase a scrolling direction.
     * @see #scrollTo(JScrollBarOperator.ScrollChecker)
     * @throws TimeoutExpiredException
     */
    public <P> void scrollTo(Waitable<?, P> w, P waiterParam, boolean increase) {
        scrollTo(new WaitableChecker<>(w, waiterParam, increase, this));
    }

    /**
     * Scrolls scrollbar to the position defined by a ScrollChecker
     * implementation.
     *
     * @param checker ScrollChecker implementation defining scrolling direction,
     * and so on.
     * @see ScrollChecker
     * @throws TimeoutExpiredException
     */
    public void scrollTo(ScrollChecker checker) {
        scrollTo(new CheckerAdjustable(checker, this));
    }

    /**
     * Scrolls scrollbar to the position defined by a ScrollAdjuster
     * implementation.
     *
     * @param adj defines scrolling direction, and so on.
     * @throws TimeoutExpiredException
     */
    public void scrollTo(final ScrollAdjuster adj) {
        initOperators();
        produceTimeRestricted(new Action<Void, Void>() {
            @Override
            public Void launch(Void obj) {
                driver.scroll(JScrollBarOperator.this, adj);
                return null;
            }

            @Override
            public String getDescription() {
                return "Scrolling";
            }

            @Override
            public String toString() {
                return "JScrollBarOperator.scrollTo.Action{description = " + getDescription() + '}';
            }
        }, "JScrollBarOperator.WholeScrollTimeout");
    }

    /**
     * Scrolls scroll bar to necessary value.
     *
     * @param value Scroll bar value to scroll to.
     * @throws TimeoutExpiredException
     */
    public void scrollToValue(int value) {
        output.printTrace("Scroll JScrollBar to " + Integer.toString(value)
                + " value\n" + toStringSource());
        output.printGolden("Scroll JScrollBar to " + Integer.toString(value) + " value");
        scrollTo(new ValueScrollAdjuster(value));
    }

    /**
     * Scrolls scroll bar to necessary proportional value.
     *
     * @param proportionalValue Proportional scroll to. Must be >= 0 and <= 1.
     * @throws TimeoutExpiredException
     */
    public void scrollToValue(double proportionalValue) {
        output.printTrace("Scroll JScrollBar to " + Double.toString(proportionalValue)
                + " proportional value\n" + toStringSource());
        output.printGolden("Scroll JScrollBar to " + Double.toString(proportionalValue) + " proportional value");
        scrollTo(new ValueScrollAdjuster((int) (getMinimum()
                + (getMaximum()
                - getVisibleAmount()
                - getMinimum()) * proportionalValue)));
    }

    /**
     * Scrolls to minimum value.
     *
     * @throws TimeoutExpiredException
     */
    public void scrollToMinimum() {
        output.printTrace("Scroll JScrollBar to minimum value\n"
                + toStringSource());
        output.printGolden("Scroll JScrollBar to minimum value");
        initOperators();
        produceTimeRestricted(new Action<Void, Void>() {
            @Override
            public Void launch(Void obj) {
                driver.scrollToMinimum(JScrollBarOperator.this, getOrientation());
                return null;
            }

            @Override
            public String getDescription() {
                return "Scrolling";
            }

            @Override
            public String toString() {
                return "JScrollBarOperator.scrollToMinimum.Action{description = " + getDescription() + '}';
            }
        }, "JScrollBarOperator.WholeScrollTimeout");
    }

    /**
     * Scrolls to maximum value.
     *
     * @throws TimeoutExpiredException
     */
    public void scrollToMaximum() {
        output.printTrace("Scroll JScrollBar to maximum value\n"
                + toStringSource());
        output.printGolden("Scroll JScrollBar to maximum value");
        initOperators();
        produceTimeRestricted(new Action<Void, Void>() {
            @Override
            public Void launch(Void obj) {
                driver.scrollToMaximum(JScrollBarOperator.this, getOrientation());
                return null;
            }

            @Override
            public String getDescription() {
                return "Scrolling";
            }

            @Override
            public String toString() {
                return "JScrollBarOperator.scrollToMaximum.Action{description = " + getDescription() + '}';
            }
        }, "JScrollBarOperator.WholeScrollTimeout");
    }

    /**
     * Returns a button responsible for value decreasing.
     *
     * @return an operator for the decrease button.
     */
    public JButtonOperator getDecreaseButton() {
        initOperators();
        return minButtOperator;
    }

    /**
     * Returns a button responsible for value increasing.
     *
     * @return an operator for the increase button.
     */
    public JButtonOperator getIncreaseButton() {
        initOperators();
        return maxButtOperator;
    }

    @Override
    public Hashtable<String, Object> getDump() {
        Hashtable<String, Object> result = super.getDump();
        result.put(MINIMUM_DPROP, Integer.toString(((JScrollBar) getSource()).getMinimum()));
        result.put(MAXIMUM_DPROP, Integer.toString(((JScrollBar) getSource()).getMaximum()));
        result.put(ORIENTATION_DPROP, (((JScrollBar) getSource()).getOrientation() == JScrollBar.HORIZONTAL)
                ? HORIZONTAL_ORIENTATION_DPROP_VALUE
                : VERTICAL_ORIENTATION_DPROP_VALUE);
        result.put(VALUE_DPROP, Integer.toString(((JScrollBar) getSource()).getValue()));
        return result;
    }

    ////////////////////////////////////////////////////////
    //Mapping                                             //
    /**
     * Maps {@code JScrollBar.addAdjustmentListener(AdjustmentListener)}
     * through queue
     */
    public void addAdjustmentListener(final AdjustmentListener adjustmentListener) {
        runMapping(new MapVoidAction("addAdjustmentListener") {
            @Override
            public void map() {
                ((JScrollBar) getSource()).addAdjustmentListener(adjustmentListener);
            }
        });
    }

    /**
     * Maps {@code JScrollBar.getBlockIncrement()} through queue
     */
    public int getBlockIncrement() {
        return (runMapping(new MapIntegerAction("getBlockIncrement") {
            @Override
            public int map() {
                return ((JScrollBar) getSource()).getBlockIncrement();
            }
        }));
    }

    /**
     * Maps {@code JScrollBar.getBlockIncrement(int)} through queue
     */
    public int getBlockIncrement(final int i) {
        return (runMapping(new MapIntegerAction("getBlockIncrement") {
            @Override
            public int map() {
                return ((JScrollBar) getSource()).getBlockIncrement(i);
            }
        }));
    }

    /**
     * Maps {@code JScrollBar.getMaximum()} through queue
     */
    public int getMaximum() {
        return (runMapping(new MapIntegerAction("getMaximum") {
            @Override
            public int map() {
                return ((JScrollBar) getSource()).getMaximum();
            }
        }));
    }

    /**
     * Maps {@code JScrollBar.getMinimum()} through queue
     */
    public int getMinimum() {
        return (runMapping(new MapIntegerAction("getMinimum") {
            @Override
            public int map() {
                return ((JScrollBar) getSource()).getMinimum();
            }
        }));
    }

    /**
     * Maps {@code JScrollBar.getModel()} through queue
     */
    public BoundedRangeModel getModel() {
        return (runMapping(new MapAction<BoundedRangeModel>("getModel") {
            @Override
            public BoundedRangeModel map() {
                return ((JScrollBar) getSource()).getModel();
            }
        }));
    }

    /**
     * Maps {@code JScrollBar.getOrientation()} through queue
     */
    public int getOrientation() {
        return (runMapping(new MapIntegerAction("getOrientation") {
            @Override
            public int map() {
                return ((JScrollBar) getSource()).getOrientation();
            }
        }));
    }

    /**
     * Maps {@code JScrollBar.getUI()} through queue
     */
    public ScrollBarUI getUI() {
        return (runMapping(new MapAction<ScrollBarUI>("getUI") {
            @Override
            public ScrollBarUI map() {
                return ((JScrollBar) getSource()).getUI();
            }
        }));
    }

    /**
     * Maps {@code JScrollBar.getUnitIncrement()} through queue
     */
    public int getUnitIncrement() {
        return (runMapping(new MapIntegerAction("getUnitIncrement") {
            @Override
            public int map() {
                return ((JScrollBar) getSource()).getUnitIncrement();
            }
        }));
    }

    /**
     * Maps {@code JScrollBar.getUnitIncrement(int)} through queue
     */
    public int getUnitIncrement(final int i) {
        return (runMapping(new MapIntegerAction("getUnitIncrement") {
            @Override
            public int map() {
                return ((JScrollBar) getSource()).getUnitIncrement(i);
            }
        }));
    }

    /**
     * Maps {@code JScrollBar.getValue()} through queue
     */
    public int getValue() {
        return (runMapping(new MapIntegerAction("getValue") {
            @Override
            public int map() {
                return ((JScrollBar) getSource()).getValue();
            }
        }));
    }

    /**
     * Maps {@code JScrollBar.getValueIsAdjusting()} through queue
     */
    public boolean getValueIsAdjusting() {
        return (runMapping(new MapBooleanAction("getValueIsAdjusting") {
            @Override
            public boolean map() {
                return ((JScrollBar) getSource()).getValueIsAdjusting();
            }
        }));
    }

    /**
     * Maps {@code JScrollBar.getVisibleAmount()} through queue
     */
    public int getVisibleAmount() {
        return (runMapping(new MapIntegerAction("getVisibleAmount") {
            @Override
            public int map() {
                return ((JScrollBar) getSource()).getVisibleAmount();
            }
        }));
    }

    /**
     * Maps {@code JScrollBar.removeAdjustmentListener(AdjustmentListener)}
     * through queue
     */
    public void removeAdjustmentListener(final AdjustmentListener adjustmentListener) {
        runMapping(new MapVoidAction("removeAdjustmentListener") {
            @Override
            public void map() {
                ((JScrollBar) getSource()).removeAdjustmentListener(adjustmentListener);
            }
        });
    }

    /**
     * Maps {@code JScrollBar.setBlockIncrement(int)} through queue
     */
    public void setBlockIncrement(final int i) {
        runMapping(new MapVoidAction("setBlockIncrement") {
            @Override
            public void map() {
                ((JScrollBar) getSource()).setBlockIncrement(i);
            }
        });
    }

    /**
     * Maps {@code JScrollBar.setMaximum(int)} through queue
     */
    public void setMaximum(final int i) {
        runMapping(new MapVoidAction("setMaximum") {
            @Override
            public void map() {
                ((JScrollBar) getSource()).setMaximum(i);
            }
        });
    }

    /**
     * Maps {@code JScrollBar.setMinimum(int)} through queue
     */
    public void setMinimum(final int i) {
        runMapping(new MapVoidAction("setMinimum") {
            @Override
            public void map() {
                ((JScrollBar) getSource()).setMinimum(i);
            }
        });
    }

    /**
     * Maps {@code JScrollBar.setModel(BoundedRangeModel)} through queue
     */
    public void setModel(final BoundedRangeModel boundedRangeModel) {
        runMapping(new MapVoidAction("setModel") {
            @Override
            public void map() {
                ((JScrollBar) getSource()).setModel(boundedRangeModel);
            }
        });
    }

    /**
     * Maps {@code JScrollBar.setOrientation(int)} through queue
     */
    public void setOrientation(final int i) {
        runMapping(new MapVoidAction("setOrientation") {
            @Override
            public void map() {
                ((JScrollBar) getSource()).setOrientation(i);
            }
        });
    }

    /**
     * Maps {@code JScrollBar.setUnitIncrement(int)} through queue
     */
    public void setUnitIncrement(final int i) {
        runMapping(new MapVoidAction("setUnitIncrement") {
            @Override
            public void map() {
                ((JScrollBar) getSource()).setUnitIncrement(i);
            }
        });
    }

    /**
     * Maps {@code JScrollBar.setValue(int)} through queue
     */
    public void setValue(final int i) {
        runMapping(new MapVoidAction("setValue") {
            @Override
            public void map() {
                ((JScrollBar) getSource()).setValue(i);
            }
        });
    }

    /**
     * Maps {@code JScrollBar.setValueIsAdjusting(boolean)} through queue
     */
    public void setValueIsAdjusting(final boolean b) {
        runMapping(new MapVoidAction("setValueIsAdjusting") {
            @Override
            public void map() {
                ((JScrollBar) getSource()).setValueIsAdjusting(b);
            }
        });
    }

    /**
     * Maps {@code JScrollBar.setValues(int, int, int, int)} through queue
     */
    public void setValues(final int i, final int i1, final int i2, final int i3) {
        runMapping(new MapVoidAction("setValues") {
            @Override
            public void map() {
                ((JScrollBar) getSource()).setValues(i, i1, i2, i3);
            }
        });
    }

    /**
     * Maps {@code JScrollBar.setVisibleAmount(int)} through queue
     */
    public void setVisibleAmount(final int i) {
        runMapping(new MapVoidAction("setVisibleAmount") {
            @Override
            public void map() {
                ((JScrollBar) getSource()).setVisibleAmount(i);
            }
        });
    }

    //End of mapping                                      //
    ////////////////////////////////////////////////////////
    private void initOperators() {
        if (minButtOperator != null
                && maxButtOperator != null) {
            return;
        }
        ComponentChooser chooser = new ComponentChooser() {
            @Override
            public boolean checkComponent(Component comp) {
                return comp instanceof JButton;
            }

            @Override
            public String getDescription() {
                return "";
            }

            @Override
            public String toString() {
                return "JScrollBarOperator.initOperators.ComponentChooser{description = " + getDescription() + '}';
            }
        };
        ComponentSearcher searcher = new ComponentSearcher((Container) getSource());
        searcher.setOutput(output.createErrorOutput());
        JButton butt0 = (JButton) searcher.findComponent(chooser, 0);
        JButton butt1 = (JButton) searcher.findComponent(chooser, 1);

        if (butt0 == null || butt1 == null) {
            minButtOperator = null;
            maxButtOperator = null;
            return;
        }

        JButton minButt, maxButt;

        if (((JScrollBar) getSource()).getOrientation() == JScrollBar.HORIZONTAL) {
            if (butt0.getX() < butt1.getX()) {
                minButt = butt0;
                maxButt = butt1;
            } else {
                minButt = butt1;
                maxButt = butt0;
            }
        } else if (butt0.getY() < butt1.getY()) {
            minButt = butt0;
            maxButt = butt1;
        } else {
            minButt = butt1;
            maxButt = butt0;
        }
        minButtOperator = new JButtonOperator(minButt);
        maxButtOperator = new JButtonOperator(maxButt);

        minButtOperator.copyEnvironment(this);
        maxButtOperator.copyEnvironment(this);

        minButtOperator.setOutput(output.createErrorOutput());
        maxButtOperator.setOutput(output.createErrorOutput());

        Timeouts times = timeouts.cloneThis();
        times.setTimeout("AbstractButtonOperator.PushButtonTimeout",
                times.getTimeout("JScrollBarOperator.OneScrollClickTimeout"));

        minButtOperator.setTimeouts(times);
        maxButtOperator.setTimeouts(times);

        minButtOperator.setVisualizer(new EmptyVisualizer());
        maxButtOperator.setVisualizer(new EmptyVisualizer());
    }

    /**
     * Interface can be used to define some kind of complicated scrolling rules.
     */
    public interface ScrollChecker {

        /**
         * Defines a scrolling direction.
         *
         * @param oper an operator
         * @see
         * org.netbeans.jemmy.drivers.scrolling.ScrollAdjuster#INCREASE_SCROLL_DIRECTION
         * @see
         * org.netbeans.jemmy.drivers.scrolling.ScrollAdjuster#DECREASE_SCROLL_DIRECTION
         * @see
         * org.netbeans.jemmy.drivers.scrolling.ScrollAdjuster#DO_NOT_TOUCH_SCROLL_DIRECTION
         * @return one of the following values:<BR>
         * ScrollAdjuster.INCREASE_SCROLL_DIRECTION<BR>
         * ScrollAdjuster.DECREASE_SCROLL_DIRECTION<BR>
         * ScrollAdjuster.DO_NOT_TOUCH_SCROLL_DIRECTION<BR>
         */
        public int getScrollDirection(JScrollBarOperator oper);

        /**
         * Scrolling rules decription.
         *
         * @return a description
         */
        public String getDescription();
    }

    private class ValueScrollAdjuster implements ScrollAdjuster {

        int value;

        public ValueScrollAdjuster(int value) {
            this.value = value;
        }

        @Override
        public int getScrollDirection() {
            if (getValue() == value) {
                return ScrollAdjuster.DO_NOT_TOUCH_SCROLL_DIRECTION;
            } else {
                return ((getValue() < value)
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

    private class WaitableChecker<P> implements ScrollAdjuster {

        Waitable<?, P> w;
        P waitParam;
        boolean increase;
        boolean reached = false;

        public WaitableChecker(Waitable<?, P> w, P waitParam, boolean increase, JScrollBarOperator oper) {
            this.w = w;
            this.waitParam = waitParam;
            this.increase = increase;
        }

        @Override
        public int getScrollDirection() {
            if (!reached && w.actionProduced(waitParam) != null) {
                reached = true;
            }
            if (reached) {
                return ScrollAdjuster.DO_NOT_TOUCH_SCROLL_DIRECTION;
            } else {
                return (increase
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
            return w.getDescription();
        }

        @Override
        public String toString() {
            return "WaitableChecker{" + "w=" + w + ", waitParam=" + waitParam + ", increase=" + increase + ", reached=" + reached + '}';
        }
    }

    private class CheckerAdjustable implements ScrollAdjuster {

        ScrollChecker checker;
        JScrollBarOperator oper;

        public CheckerAdjustable(ScrollChecker checker, JScrollBarOperator oper) {
            this.checker = checker;
            this.oper = oper;
        }

        @Override
        public int getScrollDirection() {
            return checker.getScrollDirection(oper);
        }

        @Override
        public int getScrollOrientation() {
            return getOrientation();
        }

        @Override
        public String getDescription() {
            return checker.getDescription();
        }

        @Override
        public String toString() {
            return "CheckerAdjustable{" + "checker=" + checker + ", oper=" + oper + '}';
        }
    }

    /**
     * Checks component type.
     */
    public static class JScrollBarFinder extends Finder {

        /**
         * Constructs JScrollBarFinder.
         *
         * @param sf other searching criteria.
         */
        public JScrollBarFinder(ComponentChooser sf) {
            super(JScrollBar.class, sf);
        }

        /**
         * Constructs JScrollBarFinder.
         */
        public JScrollBarFinder() {
            super(JScrollBar.class);
        }
    }
}
