/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Window;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JToolTip;
import javax.swing.plaf.ToolTipUI;

import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.ComponentSearcher;
import org.netbeans.jemmy.JemmyException;
import org.netbeans.jemmy.TimeoutExpiredException;
import org.netbeans.jemmy.Timeouts;
import org.netbeans.jemmy.Waitable;
import org.netbeans.jemmy.Waiter;

/**
 * <BR>
 * <BR>
 * Timeouts used: <BR>
 * {@code ComponentOperator.WaitComponentTimeout} - time to wait component
 * displayed.<BR>
 * {@code ComponentOperator.WaitStateTimeout} - time to wait for tip text. <BR>
 *
 * @see org.netbeans.jemmy.Timeouts
 */
public class JToolTipOperator extends JComponentOperator {

    private static long WAIT_TOOLTIP_TIMEOUT = 60000;

    /**
     * Identifier for a "tip text" property.
     *
     * @see #getDump()
     */
    public static final String TIP_TEXT_DPROP = "TipText";

    static {
        Timeouts.initDefault("JToolTipOperator.WaitToolTipTimeout", WAIT_TOOLTIP_TIMEOUT);
    }
    /**
     * Constructs a JToolTipOperator object, waiting for a shown
     * JToolTip.
     */
    public JToolTipOperator() {
        this(TRUE_CHOOSER);
    }

    /**
     * Constructs a JToolTipOperator object for a given JToolTip component.
     *
     * @param toolTip
     *            a component
     */
    public JToolTipOperator(JToolTip toolTip) {
        super(toolTip);
    }

    /**
     * Constructs a JToolTipOperator object waiting for the JToolTip having a
     * given tip text (compared using default string comparator).
     *
     * @param tipText
     *            tip text.
     * @see #getDefaultStringComparator()
     */
    public JToolTipOperator(String tipText) {
        this(waitJToolTip(new JToolTipByTipTextFinder(tipText,
                getDefaultStringComparator())));
    }

    /**
     * Constructs a JToolTipOperator object waiting for the JToolTip
     * associated with the given component. Uses {@code comp}'s timeout and
     * output for waiting. Copies environment from {@code comp}.
     *
     * @param comp
     *            component on which tool tip associated
     * @see #copyEnvironment(org.netbeans.jemmy.operators.Operator)
     */
    public JToolTipOperator(ComponentOperator comp) {
        this(comp, TRUE_CHOOSER);
    }

    /**
     * Constructs a JToolTipOperator object waiting for the JToolTip
     * conforming to the given component chooser.
     *
     * @param chooser
     *            a component chooser specifying searching criteria.
     */
    public JToolTipOperator(ComponentChooser chooser) {
        this(null, chooser);
    }

    /**
     * Constructs a JToolTipOperator object waiting for the JToolTip
     * associated with the given component and conforming to the given
     * component chooser. Uses {@code comp}'s timeout and output for waiting.
     * Copies environment from {@code comp}.
     *
     * @param comp
     *            component on which tool tip associated
     * @param chooser
     *            a component chooser specifying searching criteria.
     * @see #copyEnvironment(org.netbeans.jemmy.operators.Operator)
     */
    public JToolTipOperator(ComponentOperator comp, ComponentChooser chooser) {
        this(waitJToolTip(comp, chooser));
        if(comp != null) {
            copyEnvironment(comp);
        }
    }

    /**
     * Constructs a JToolTipOperator object waiting for the JToolTip
     * associated with the given component and having the given tip text.
     * Uses {@code comp}'s string comparator for tip text comparison, timeout
     * and output for waiting. Copies environment from {@code comp}.
     *
     * @param comp
     *            component on which tool tip associated
     * @param tipText
     *            tip text
     * @see #getComparator()
     * @see #copyEnvironment(org.netbeans.jemmy.operators.Operator)
     */
    public JToolTipOperator(ComponentOperator comp, String tipText) {
        this(waitJToolTip(comp,
                new JToolTipByTipTextFinder(tipText, comp.getComparator())));
        copyEnvironment(comp);
    }

    /**
     * Searches for the JToolTip associated with the given component and
     * conforming to the given component chooser. Uses {@code comp}'s timeout
     * and output for waiting.
     *
     * @param comp
     *            component on which tool tip associated
     * @param chooser
     *            a component chooser specifying searching criteria.
     * @return JToolTip instance or null if component was not found.
     */
    public static JToolTip findJToolTip(ComponentOperator comp,
            ComponentChooser chooser) {
        List<Window> windowList;
        if(comp != null && comp.getWindow() != null) {
            windowList = new ArrayList<>(
                    Arrays.asList(comp.getWindow().getOwnedWindows()));
            windowList.add(comp.getWindow());
        } else {
            windowList = new ArrayList<>(
                    Arrays.asList(WindowOperator.getWindows()));
        }
        ComponentChooser toolTipChooser = new JToolTipFinder(chooser);
        for (Window w : windowList) {
            ComponentSearcher searcher = new ComponentSearcher(w);
            Component[] components = searcher.findComponents(toolTipChooser);
            if (components.length > 0) {
                if(comp!= null && comp.getSource() != null) {
                    if(comp.getSource().equals(
                            ((JToolTip) components[0]).getComponent())) {
                        return (JToolTip) components[0];
                    }
                } else {
                    return (JToolTip) components[0];
                }
            }
        }
        return null;
    }


    /**
     * Searches for a JToolTip.
     *
     * @return JToolTip instance or null if component was not found.
     */
    public static JToolTip findJToolTip() {
        return findJToolTip(null);
    }

    /**
     * Searches for the JToolTip associated with the given component. Uses
     * {@code comp}'s timeout and output for waiting.
     *
     * @param comp
     *            component on which tool tip associated
     * @return JToolTip instance or null if component was not found.
     */
    public static JToolTip findJToolTip(ComponentOperator comp) {
        return findJToolTip(comp, TRUE_CHOOSER);
    }

    /**
     * Searches for the JToolTip associated with the given component and
     * looking for given tip text using specified string comparator options.
     * Uses {@code comp}'s timeout and output for waiting.
     *
     * @param comp
     *            component on which tool tip associated
     * @param tipText
     *            Tip text.
     * @param ce
     *            Compare text exactly.
     * @param ccs
     *            Compare text case sensitively.
     * @return JToolTip instance or null if component was not found.
     * @see DefaultStringComparator
     * @see JToolTipByTipTextFinder
     */
    public static JToolTip findJToolTip(ComponentOperator comp, String tipText,
            boolean ce, boolean ccs) {
        return findJToolTip(comp, new JToolTipByTipTextFinder(tipText,
                new DefaultStringComparator(ce, ccs)));
    }

    /**
     * Waits for a JToolTip.
     *
     * @return JToolTip instance.
     * @see TimeoutExpiredException
     */
    public static JToolTip waitJToolTip() {
        return waitJToolTip(TRUE_CHOOSER);
    }


    /**
     * Waits for the first JToolTip associated with the given component.
     *
     * @param comp
     *            component on which tool tip associated
     * @return JToolTip instance.
     * @see TimeoutExpiredException
     */
    public static JToolTip waitJToolTip(ComponentOperator comp) {
        return waitJToolTip(comp, TRUE_CHOOSER);
    }

    /**
     * Waits for the JToolTip conforming to the given component
     * chooser.
     *
     * @param chooser
     *            a component chooser specifying searching criteria.
     * @return JToolTip instance.
     * @see TimeoutExpiredException
     */
    public static JToolTip waitJToolTip(ComponentChooser chooser) {
        return waitJToolTip(null, chooser);
    }

    /**
     * Waits for the JToolTip associated with the given component and
     * conforming to the specified component chooser.
     *
     * @param comp
     *            component on which tool tip associated
     * @param chooser
     *            a component chooser specifying searching criteria.
     * @return JToolTip instance.
     * @see TimeoutExpiredException
     */
    public static JToolTip waitJToolTip(ComponentOperator comp,
            ComponentChooser chooser) {
        Waitable<JToolTip, Void> waitable = new Waitable<JToolTip, Void>() {
            @Override
            public JToolTip actionProduced(Void obj) {
                return findJToolTip(comp, chooser);
            }

            @Override
            public String getDescription() {
                return "Wait for JTooltip to be displayed for Component = "
                        + comp + ", " + "chooser = " + chooser;
            }

            @Override
            public String toString() {
                return "JToolTipOperator.waitJToolTip.Waitable{description = "
                        + getDescription() + '}';
            }
        };
        Waiter<JToolTip, Void> stateWaiter = new Waiter<>(waitable);
        stateWaiter.setTimeoutsToCloneOf(Operator.getEnvironmentOperator().
                getTimeouts(), "JToolTipOperator.WaitToolTipTimeout");
        stateWaiter.setOutput(Operator.getEnvironmentOperator().
                getOutput().createErrorOutput());
        try {
            return stateWaiter.waitAction(null);
        } catch (InterruptedException e) {
           throw new JemmyException("Waiting of " + waitable.getDescription()
               + " state has been interrupted!");
        }
    }

    /**
     * Waits for the JToolTip associated with the given component and having
     * the given tip text compared using given string comparator options.
     *
     * @param comp
     *            component on which tool tip associated
     * @param tipText
     *            Tip text.
     * @param ce
     *            Compare text exactly.
     * @param ccs
     *            Compare text case sensitively.
     * @return JToolTip instance.
     * @see TimeoutExpiredException
     */
    public static JToolTip waitJToolTip(ComponentOperator comp, String tipText,
            boolean ce, boolean ccs) {
        return waitJToolTip(comp, new JToolTipByTipTextFinder(tipText,
                new DefaultStringComparator(ce, ccs)));
    }

    /**
     * Waits for the given tip text. Uses {@linkplain #getComparator()}
     * comparator.
     *
     * @param tipText
     *            Tip text to wait for.
     * @see TimeoutExpiredException
     */
    public void waitTipText(String tipText) {
        getOutput().printLine("Wait \"" + tipText
                + "\" tip text in JToolTip \n    : " + toStringSource());
        getOutput().printGolden("Wait \"" + tipText + "\" tip text");
        waitState(new JToolTipByTipTextFinder(tipText, getComparator()));
    }

    /**
     * Returns information about the component.
     *
     * @return Map of component properties.
     */
    @Override
    public Hashtable<String, Object> getDump() {
        Hashtable<String, Object> result = super.getDump();
        String tipText = getTipText();
        if (tipText != null) {
            result.put(TIP_TEXT_DPROP, tipText);
        } else {
            result.put(TIP_TEXT_DPROP, "null");
        }
        return result;
    }

    ////////////////////////////////////////////////////////
    // Mapping //

    /**
     * Maps {@linkplain JToolTip#getTipText()} through queue
     *
     * @return
     */
    public String getTipText() {
        return runMapping(new MapAction<String>("getTipText") {
            @Override
            public String map() {
                return ((JToolTip) getSource()).getTipText();
            }
        });
    }

    /**
     * Maps {@linkplain JToolTip#getComponent()} through queue
     *
     * @return
     */
    public JComponent getComponent() {
        return runMapping(new MapAction<JComponent>("getComponent") {
            @Override
            public JComponent map() {
                return ((JToolTip) getSource()).getComponent();
            }
        });
    }

    /**
     * Maps {@linkplain JToolTip#getUI()} through queue
     *
     * @return
     */
    public ToolTipUI getUI() {
        return runMapping(new MapAction<ToolTipUI>("getUI") {
            @Override
            public ToolTipUI map() {
                return ((JToolTip) getSource()).getUI();
            }
        });
    }

    /**
     * Maps {@linkplain JToolTip#setTipText(String)} through queue
     *
     * @param tipText
     */
    public void setTipText(final String tipText) {
        runMapping(new MapVoidAction("setTipText") {
            @Override
            public void map() {
                ((JToolTip) getSource()).setTipText(tipText);
            }
        });
    }

    /**
     * Maps {@linkplain JToolTip#setComponent(JComponent)} through queue
     *
     * @param component
     */
    public void setComponent(final JComponent component) {
        runMapping(new MapVoidAction("setComponent") {
            @Override
            public void map() {
                ((JToolTip) getSource()).setComponent(component);
            }
        });
    }
    // End of mapping //
    ////////////////////////////////////////////////////////


    /**
     * Allows to find JToolTip by tip text.
     */
    public static class JToolTipByTipTextFinder implements ComponentChooser {

        String tipText;
        StringComparator comparator;

        /**
         * Constructs JToolTipByTipTextFinder.
         *
         * @param tipText
         *            a tip text pattern
         * @param comparator
         *            specifies string comparison algorithm.
         */
        public JToolTipByTipTextFinder(String tipText,
                StringComparator comparator) {
            this.tipText = tipText;
            this.comparator = comparator;
        }

        /**
         * Constructs JToolTipByTipTextFinder.
         *
         * @param tipText
         *            a tip text pattern
         */
        public JToolTipByTipTextFinder(String tipText) {
            this(tipText, Operator.getDefaultStringComparator());
        }

        @Override
        public boolean checkComponent(Component comp) {
            if (comp instanceof JToolTip) {
                if (((JToolTip) comp).getTipText() != null) {
                    return (comparator.equals(((JToolTip) comp).getTipText(),
                            tipText));
                }
            }
            return false;
        }

        @Override
        public String getDescription() {
            return "JToolTip with tip text \"" + tipText + "\"";
        }

        @Override
        public String toString() {
            return "JToolTipByTipTextFinder{" + "tipText=" + tipText
                    + ", comparator=" + comparator + '}';
        }
    }

    /**
     * Allows to find JToolTips among components.
     */
    public static class JToolTipFinder extends Finder {

        /**
         * Constructs JToolTipFinder chaining another component chooser.
         *
         * @param sf
         *            other searching criteria.
         */
        public JToolTipFinder(ComponentChooser sf) {
            super(JToolTip.class, sf);
        }

        /**
         * Constructs JToolTipFinder.
         */
        public JToolTipFinder() {
            super(JToolTip.class);
        }
    }

    private static final ComponentChooser TRUE_CHOOSER = ComponentSearcher
            .getTrueChooser("Any JToolTip");
}
