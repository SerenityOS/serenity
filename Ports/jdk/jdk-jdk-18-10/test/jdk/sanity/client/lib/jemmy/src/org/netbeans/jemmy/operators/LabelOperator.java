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
import java.awt.Label;
import java.util.Hashtable;

import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.TimeoutExpiredException;

/**
 * <BR><BR>Timeouts used: <BR>
 * ComponentOperator.WaitComponentTimeout - time to wait component displayed
 * <BR>.
 *
 * @see org.netbeans.jemmy.Timeouts
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 *
 */
public class LabelOperator extends ComponentOperator {

    /**
     * Identifier for a "text" property.
     *
     * @see #getDump
     */
    public static final String TEXT_DPROP = "Text";

    /**
     * Constructor.
     *
     * @param b a component
     */
    public LabelOperator(Label b) {
        super(b);
    }

    /**
     * Constructs a LabelOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     */
    public LabelOperator(ContainerOperator<?> cont, ComponentChooser chooser, int index) {
        this((Label) cont.
                waitSubComponent(new LabelFinder(chooser),
                        index));
        copyEnvironment(cont);
    }

    /**
     * Constructs a LabelOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     */
    public LabelOperator(ContainerOperator<?> cont, ComponentChooser chooser) {
        this(cont, chooser, 0);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @param text Button text.
     * @param index Ordinal component index.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public LabelOperator(ContainerOperator<?> cont, String text, int index) {
        this((Label) waitComponent(cont,
                new LabelByLabelFinder(text,
                        cont.getComparator()),
                index));
        copyEnvironment(cont);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @param text Button text.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public LabelOperator(ContainerOperator<?> cont, String text) {
        this(cont, text, 0);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @param index Ordinal component index.
     * @throws TimeoutExpiredException
     */
    public LabelOperator(ContainerOperator<?> cont, int index) {
        this((Label) waitComponent(cont,
                new LabelFinder(),
                index));
        copyEnvironment(cont);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @throws TimeoutExpiredException
     */
    public LabelOperator(ContainerOperator<?> cont) {
        this(cont, 0);
    }

    /**
     * Searches Label in container.
     *
     * @param cont Container to search component in.
     * @param chooser a component chooser specifying searching criteria.
     * @param index Ordinal component index.
     * @return Label instance or null if component was not found.
     */
    public static Label findLabel(Container cont, ComponentChooser chooser, int index) {
        return (Label) findComponent(cont, new LabelFinder(chooser), index);
    }

    /**
     * Searches Label in container.
     *
     * @param cont Container to search component in.
     * @param chooser a component chooser specifying searching criteria.
     * @return Label instance or null if component was not found.
     */
    public static Label findLabel(Container cont, ComponentChooser chooser) {
        return findLabel(cont, chooser, 0);
    }

    /**
     * Searches Label by text.
     *
     * @param cont Container to search component in.
     * @param text Component text.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param index Ordinal component index.
     * @return Label instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static Label findLabel(Container cont, String text, boolean ce, boolean ccs, int index) {
        return findLabel(cont, new LabelByLabelFinder(text, new DefaultStringComparator(ce, ccs)), index);
    }

    /**
     * Searches Label by text.
     *
     * @param cont Container to search component in.
     * @param text Component text.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @return Label instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static Label findLabel(Container cont, String text, boolean ce, boolean ccs) {
        return findLabel(cont, text, ce, ccs, 0);
    }

    /**
     * Waits Label in container.
     *
     * @param cont Container to search component in.
     * @param chooser a component chooser specifying searching criteria.
     * @param index Ordinal component index.
     * @return Label instance.
     * @throws TimeoutExpiredException
     */
    public static Label waitLabel(final Container cont, final ComponentChooser chooser, final int index) {
        return (Label) waitComponent(cont, new LabelFinder(chooser), index);
    }

    /**
     * Waits Label in container.
     *
     * @param cont Container to search component in.
     * @param chooser a component chooser specifying searching criteria.
     * @return Label instance.
     * @throws TimeoutExpiredException
     */
    public static Label waitLabel(Container cont, ComponentChooser chooser) {
        return waitLabel(cont, chooser, 0);
    }

    /**
     * Waits Label by text.
     *
     * @param cont Container to search component in.
     * @param text Component text.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param index Ordinal component index.
     * @return Label instance.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public static Label waitLabel(Container cont, String text, boolean ce, boolean ccs, int index) {
        return waitLabel(cont, new LabelByLabelFinder(text, new DefaultStringComparator(ce, ccs)), index);
    }

    /**
     * Waits Label by text.
     *
     * @param cont Container to search component in.
     * @param text Component text.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @return Label instance.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public static Label waitLabel(Container cont, String text, boolean ce, boolean ccs) {
        return waitLabel(cont, text, ce, ccs, 0);
    }

    @Override
    public Hashtable<String, Object> getDump() {
        Hashtable<String, Object> result = super.getDump();
        if (((Label) getSource()).getText() != null) {
            result.put(TEXT_DPROP, ((Label) getSource()).getText());
        } else {
            result.put(TEXT_DPROP, "null");
        }
        return result;
    }

    ////////////////////////////////////////////////////////
    //Mapping                                             //
    /**
     * Maps {@code Label.getAlignment()} through queue
     */
    public int getAlignment() {
        return (runMapping(new MapIntegerAction("getAlignment") {
            @Override
            public int map() {
                return ((Label) getSource()).getAlignment();
            }
        }));
    }

    /**
     * Maps {@code Label.getText()} through queue
     */
    public String getText() {
        return (runMapping(new MapAction<String>("getText") {
            @Override
            public String map() {
                return ((Label) getSource()).getText();
            }
        }));
    }

    /**
     * Maps {@code Label.setAlignment(int)} through queue
     */
    public void setAlignment(final int i) {
        runMapping(new MapVoidAction("setAlignment") {
            @Override
            public void map() {
                ((Label) getSource()).setAlignment(i);
            }
        });
    }

    /**
     * Maps {@code Label.setText(String)} through queue
     */
    public void setText(final String string) {
        runMapping(new MapVoidAction("setText") {
            @Override
            public void map() {
                ((Label) getSource()).setText(string);
            }
        });
    }

    //End of mapping                                      //
    ////////////////////////////////////////////////////////
    /**
     * Allows to find component by LabelByLabelFinder.
     */
    public static class LabelByLabelFinder implements ComponentChooser {

        String label;
        StringComparator comparator;

        /**
         * Constructs LabelByLabelFinder.
         *
         * @param lb a text pattern
         * @param comparator specifies string comparison algorithm.
         */
        public LabelByLabelFinder(String lb, StringComparator comparator) {
            label = lb;
            this.comparator = comparator;
        }

        /**
         * Constructs LabelByLabelFinder.
         *
         * @param lb a text pattern
         */
        public LabelByLabelFinder(String lb) {
            this(lb, Operator.getDefaultStringComparator());
        }

        @Override
        public boolean checkComponent(Component comp) {
            if (comp instanceof Label) {
                if (((Label) comp).getText() != null) {
                    return (comparator.equals(((Label) comp).getText(),
                            label));
                }
            }
            return false;
        }

        @Override
        public String getDescription() {
            return "Label with text \"" + label + "\"";
        }

        @Override
        public String toString() {
            return "LabelByLabelFinder{" + "label=" + label + ", comparator=" + comparator + '}';
        }
    }

    /**
     * Checks component type.
     */
    public static class LabelFinder extends Finder {

        /**
         * Constructs LabelFinder.
         *
         * @param sf other searching criteria.
         */
        public LabelFinder(ComponentChooser sf) {
            super(Label.class, sf);
        }

        /**
         * Constructs LabelFinder.
         */
        public LabelFinder() {
            super(Label.class);
        }
    }
}
