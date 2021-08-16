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

import java.awt.Container;
import java.awt.Scrollbar;
import java.awt.event.AdjustmentListener;

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
public class ScrollbarOperator extends ComponentOperator
        implements Timeoutable, Outputable {

    private final static long ONE_SCROLL_CLICK_TIMEOUT = 0;
    private final static long WHOLE_SCROLL_TIMEOUT = 60000;
    private final static long BEFORE_DROP_TIMEOUT = 0;
    private final static long DRAG_AND_DROP_SCROLLING_DELTA = 0;

    private Timeouts timeouts;
    private TestOut output;

    private ScrollDriver driver;

    /**
     * Constructs a ScrollbarOperator object.
     *
     * @param b a component
     */
    public ScrollbarOperator(Scrollbar b) {
        super(b);
        driver = DriverManager.getScrollDriver(getClass());
    }

    /**
     * Constructs a ScrollbarOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     */
    public ScrollbarOperator(ContainerOperator<?> cont, ComponentChooser chooser, int index) {
        this((Scrollbar) cont.
                waitSubComponent(new ScrollbarFinder(chooser),
                        index));
        copyEnvironment(cont);
    }

    /**
     * Constructs a ScrollbarOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     */
    public ScrollbarOperator(ContainerOperator<?> cont, ComponentChooser chooser) {
        this(cont, chooser, 0);
    }

    /**
     * Constructs a ScrollbarOperator object.
     *
     * @param cont a container
     * @param index an index between appropriate ones.
     */
    public ScrollbarOperator(ContainerOperator<?> cont, int index) {
        this((Scrollbar) waitComponent(cont,
                new ScrollbarFinder(),
                index));
        copyEnvironment(cont);
    }

    /**
     * Constructs a ScrollbarOperator object.
     *
     * @param cont a container
     */
    public ScrollbarOperator(ContainerOperator<?> cont) {
        this(cont, 0);
    }

    /**
     * Finds a scrollbar.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     * @return the scrollbar fitting searching criteria
     */
    public static Scrollbar findScrollbar(Container cont, ComponentChooser chooser, int index) {
        return (Scrollbar) findComponent(cont, new ScrollbarFinder(chooser), index);
    }

    /**
     * Finds a scrollbar.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     * @return the scrollbar fitting searching criteria
     */
    public static Scrollbar findScrollbar(Container cont, ComponentChooser chooser) {
        return findScrollbar(cont, chooser, 0);
    }

    /**
     * Finds a scrollbar.
     *
     * @param cont a container
     * @param index an index between appropriate ones.
     * @return the scrollbar fitting searching criteria
     */
    public static Scrollbar findScrollbar(Container cont, int index) {
        return findScrollbar(cont, ComponentSearcher.getTrueChooser(Integer.toString(index) + "'th Scrollbar instance"), index);
    }

    /**
     * Finds a scrollbar.
     *
     * @param cont a container
     * @return the scrollbar fitting searching criteria
     */
    public static Scrollbar findScrollbar(Container cont) {
        return findScrollbar(cont, 0);
    }

    /**
     * Waits a scrollbar.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     * @return the scrollbar fitting searching criteria
     */
    public static Scrollbar waitScrollbar(Container cont, ComponentChooser chooser, int index) {
        return (Scrollbar) waitComponent(cont, new ScrollbarFinder(chooser), index);
    }

    /**
     * Waits a scrollbar.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     * @return the scrollbar fitting searching criteria
     */
    public static Scrollbar waitScrollbar(Container cont, ComponentChooser chooser) {
        return waitScrollbar(cont, chooser, 0);
    }

    /**
     * Waits a scrollbar.
     *
     * @param cont a container
     * @param index an index between appropriate ones.
     * @return the scrollbar fitting searching criteria
     */
    public static Scrollbar waitScrollbar(Container cont, int index) {
        return waitScrollbar(cont, ComponentSearcher.getTrueChooser(Integer.toString(index) + "'th Scrollbar instance"), index);
    }

    /**
     * Waits a scrollbar.
     *
     * @param cont a container
     * @return the scrollbar fitting searching criteria
     */
    public static Scrollbar waitScrollbar(Container cont) {
        return waitScrollbar(cont, 0);
    }

    static {
        Timeouts.initDefault("ScrollbarOperator.OneScrollClickTimeout", ONE_SCROLL_CLICK_TIMEOUT);
        Timeouts.initDefault("ScrollbarOperator.WholeScrollTimeout", WHOLE_SCROLL_TIMEOUT);
        Timeouts.initDefault("ScrollbarOperator.BeforeDropTimeout", BEFORE_DROP_TIMEOUT);
        Timeouts.initDefault("ScrollbarOperator.DragAndDropScrollingDelta", DRAG_AND_DROP_SCROLLING_DELTA);
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
     * Scrolls scrollbar to the position defined by w parameter. Uses
     * ScrollDriver registered to this operator type.
     *
     * @param w Scrolling is stopped when w.actionProduced(waiterParam) != null
     * @param waiterParam a waiting parameter.
     * @param increase a scrolling direction.
     * @throws TimeoutExpiredException
     */
    public <P> void scrollTo(Waitable<?, P> w, P waiterParam, boolean increase) {
        scrollTo(new WaitableChecker<>(w, waiterParam, increase, this));
    }

    /**
     * Scrolls scrollbar to the position defined by a ScrollAdjuster
     * implementation.
     *
     * @param adj defines scrolling direction, and so on.
     * @throws TimeoutExpiredException
     */
    public void scrollTo(final ScrollAdjuster adj) {
        produceTimeRestricted(new Action<Void, Void>() {
            @Override
            public Void launch(Void obj) {
                driver.scroll(ScrollbarOperator.this, adj);
                return null;
            }

            @Override
            public String getDescription() {
                return "Scrolling";
            }

            @Override
            public String toString() {
                return "ScrollbarOperator.scrollTo.Action{description = " + getDescription() + '}';
            }
        }, "ScrollbarOperator.WholeScrollTimeout");
    }

    /**
     * Scrolls scroll bar to necessary value.
     *
     * @param value Scroll bar value to scroll to.
     * @throws TimeoutExpiredException
     */
    public void scrollToValue(int value) {
        output.printTrace("Scroll Scrollbar to " + Integer.toString(value)
                + " value\n" + toStringSource());
        output.printGolden("Scroll Scrollbar to " + Integer.toString(value) + " value");
        scrollTo(new ValueScrollAdjuster(value));
    }

    /**
     * Scrolls scroll bar to necessary proportional value.
     *
     * @param proportionalValue Proportional scroll to. Must be >= 0 and <= 1.
     * @throws TimeoutExpiredException
     */
    public void scrollToValue(double proportionalValue) {
        output.printTrace("Scroll Scrollbar to " + Double.toString(proportionalValue)
                + " proportional value\n" + toStringSource());
        output.printGolden("Scroll Scrollbar to " + Double.toString(proportionalValue) + " proportional value");
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
        output.printTrace("Scroll Scrollbar to minimum value\n"
                + toStringSource());
        output.printGolden("Scroll Scrollbar to minimum value");
        produceTimeRestricted(new Action<Void, Void>() {
            @Override
            public Void launch(Void obj) {
                driver.scrollToMinimum(ScrollbarOperator.this, getOrientation());
                return null;
            }

            @Override
            public String getDescription() {
                return "Scrolling";
            }

            @Override
            public String toString() {
                return "ScrollbarOperator.scrollToMinimum.Action{description = " + getDescription() + '}';
            }
        }, "ScrollbarOperator.WholeScrollTimeout");
    }

    /**
     * Scrolls to maximum value.
     *
     * @throws TimeoutExpiredException
     */
    public void scrollToMaximum() {
        output.printTrace("Scroll Scrollbar to maximum value\n"
                + toStringSource());
        output.printGolden("Scroll Scrollbar to maximum value");
        produceTimeRestricted(new Action<Void, Void>() {
            @Override
            public Void launch(Void obj) {
                driver.scrollToMaximum(ScrollbarOperator.this, getOrientation());
                return null;
            }

            @Override
            public String getDescription() {
                return "Scrolling";
            }

            @Override
            public String toString() {
                return "ScrollbarOperator.scrollToMaximum.Action{description = " + getDescription() + '}';
            }
        }, "ScrollbarOperator.WholeScrollTimeout");
    }
    ////////////////////////////////////////////////////////
    //Mapping                                             //

    /**
     * Maps {@code Scrollbar.addAdjustmentListener(AdjustmentListener)}
     * through queue
     */
    public void addAdjustmentListener(final AdjustmentListener adjustmentListener) {
        runMapping(new MapVoidAction("addAdjustmentListener") {
            @Override
            public void map() {
                ((Scrollbar) getSource()).addAdjustmentListener(adjustmentListener);
            }
        });
    }

    /**
     * Maps {@code Scrollbar.getBlockIncrement()} through queue
     */
    public int getBlockIncrement() {
        return (runMapping(new MapIntegerAction("getBlockIncrement") {
            @Override
            public int map() {
                return ((Scrollbar) getSource()).getBlockIncrement();
            }
        }));
    }

    /**
     * Maps {@code Scrollbar.getMaximum()} through queue
     */
    public int getMaximum() {
        return (runMapping(new MapIntegerAction("getMaximum") {
            @Override
            public int map() {
                return ((Scrollbar) getSource()).getMaximum();
            }
        }));
    }

    /**
     * Maps {@code Scrollbar.getMinimum()} through queue
     */
    public int getMinimum() {
        return (runMapping(new MapIntegerAction("getMinimum") {
            @Override
            public int map() {
                return ((Scrollbar) getSource()).getMinimum();
            }
        }));
    }

    /**
     * Maps {@code Scrollbar.getOrientation()} through queue
     */
    public int getOrientation() {
        return (runMapping(new MapIntegerAction("getOrientation") {
            @Override
            public int map() {
                return ((Scrollbar) getSource()).getOrientation();
            }
        }));
    }

    /**
     * Maps {@code Scrollbar.getUnitIncrement()} through queue
     */
    public int getUnitIncrement() {
        return (runMapping(new MapIntegerAction("getUnitIncrement") {
            @Override
            public int map() {
                return ((Scrollbar) getSource()).getUnitIncrement();
            }
        }));
    }

    /**
     * Maps {@code Scrollbar.getValue()} through queue
     */
    public int getValue() {
        return (runMapping(new MapIntegerAction("getValue") {
            @Override
            public int map() {
                return ((Scrollbar) getSource()).getValue();
            }
        }));
    }

    /**
     * Maps {@code Scrollbar.getVisibleAmount()} through queue
     */
    public int getVisibleAmount() {
        return (runMapping(new MapIntegerAction("getVisibleAmount") {
            @Override
            public int map() {
                return ((Scrollbar) getSource()).getVisibleAmount();
            }
        }));
    }

    /**
     * Maps {@code Scrollbar.removeAdjustmentListener(AdjustmentListener)}
     * through queue
     */
    public void removeAdjustmentListener(final AdjustmentListener adjustmentListener) {
        runMapping(new MapVoidAction("removeAdjustmentListener") {
            @Override
            public void map() {
                ((Scrollbar) getSource()).removeAdjustmentListener(adjustmentListener);
            }
        });
    }

    /**
     * Maps {@code Scrollbar.setBlockIncrement(int)} through queue
     */
    public void setBlockIncrement(final int i) {
        runMapping(new MapVoidAction("setBlockIncrement") {
            @Override
            public void map() {
                ((Scrollbar) getSource()).setBlockIncrement(i);
            }
        });
    }

    /**
     * Maps {@code Scrollbar.setMaximum(int)} through queue
     */
    public void setMaximum(final int i) {
        runMapping(new MapVoidAction("setMaximum") {
            @Override
            public void map() {
                ((Scrollbar) getSource()).setMaximum(i);
            }
        });
    }

    /**
     * Maps {@code Scrollbar.setMinimum(int)} through queue
     */
    public void setMinimum(final int i) {
        runMapping(new MapVoidAction("setMinimum") {
            @Override
            public void map() {
                ((Scrollbar) getSource()).setMinimum(i);
            }
        });
    }

    /**
     * Maps {@code Scrollbar.setOrientation(int)} through queue
     */
    public void setOrientation(final int i) {
        runMapping(new MapVoidAction("setOrientation") {
            @Override
            public void map() {
                ((Scrollbar) getSource()).setOrientation(i);
            }
        });
    }

    /**
     * Maps {@code Scrollbar.setUnitIncrement(int)} through queue
     */
    public void setUnitIncrement(final int i) {
        runMapping(new MapVoidAction("setUnitIncrement") {
            @Override
            public void map() {
                ((Scrollbar) getSource()).setUnitIncrement(i);
            }
        });
    }

    /**
     * Maps {@code Scrollbar.setValue(int)} through queue
     */
    public void setValue(final int i) {
        runMapping(new MapVoidAction("setValue") {
            @Override
            public void map() {
                ((Scrollbar) getSource()).setValue(i);
            }
        });
    }

    /**
     * Maps {@code Scrollbar.setValues(int, int, int, int)} through queue
     */
    public void setValues(final int i, final int i1, final int i2, final int i3) {
        runMapping(new MapVoidAction("setValues") {
            @Override
            public void map() {
                ((Scrollbar) getSource()).setValues(i, i1, i2, i3);
            }
        });
    }

    /**
     * Maps {@code Scrollbar.setVisibleAmount(int)} through queue
     */
    public void setVisibleAmount(final int i) {
        runMapping(new MapVoidAction("setVisibleAmount") {
            @Override
            public void map() {
                ((Scrollbar) getSource()).setVisibleAmount(i);
            }
        });
    }

    //End of mapping                                      //
    ////////////////////////////////////////////////////////
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

    private class WaitableChecker<R, P> implements ScrollAdjuster {

        Waitable<R, P> w;
        P waitParam;
        boolean increase;
        boolean reached = false;

        public WaitableChecker(Waitable<R, P> w, P waitParam, boolean increase, ScrollbarOperator oper) {
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

    /**
     * Checks component type.
     */
    public static class ScrollbarFinder extends Finder {

        /**
         * Constructs ScrollbarFinder.
         *
         * @param sf other searching criteria.
         */
        public ScrollbarFinder(ComponentChooser sf) {
            super(Scrollbar.class, sf);
        }

        /**
         * Constructs ScrollbarFinder.
         */
        public ScrollbarFinder() {
            super(Scrollbar.class);
        }
    }
}
