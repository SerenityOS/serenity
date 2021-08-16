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

import javax.swing.JCheckBoxMenuItem;

import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.JemmyProperties;

/**
 *
 * <BR><BR>Timeouts used: <BR>
 * JMenuItemOperator.PushMenuTimeout - time between button pressing and
 * releasing<BR>
 * ComponentOperator.WaitComponentTimeout - time to wait button displayed <BR>
 * ComponentOperator.WaitComponentEnabledTimeout - time to wait button enabled
 * <BR>.
 *
 * @see org.netbeans.jemmy.Timeouts
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 *
 */
public class JCheckBoxMenuItemOperator extends JMenuItemOperator {

    /**
     * Constructor.
     *
     * @param item a component.
     */
    public JCheckBoxMenuItemOperator(JCheckBoxMenuItem item) {
        super(item);
        setTimeouts(JemmyProperties.getProperties().getTimeouts());
        setOutput(JemmyProperties.getProperties().getOutput());
    }

    /**
     * Constructs a JCheckBoxMenuItemOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     */
    public JCheckBoxMenuItemOperator(ContainerOperator<?> cont, ComponentChooser chooser, int index) {
        this((JCheckBoxMenuItem) cont.
                waitSubComponent(new JCheckBoxMenuItemFinder(chooser),
                        index));
        copyEnvironment(cont);
    }

    /**
     * Constructs a JCheckBoxMenuItemOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     */
    public JCheckBoxMenuItemOperator(ContainerOperator<?> cont, ComponentChooser chooser) {
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
     */
    public JCheckBoxMenuItemOperator(ContainerOperator<?> cont, String text, int index) {
        this((JCheckBoxMenuItem) waitComponent(cont,
                new JCheckBoxMenuItemByLabelFinder(text,
                        cont.getComparator()),
                index));
        setTimeouts(cont.getTimeouts());
        setOutput(cont.getOutput());
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @param text Button text.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public JCheckBoxMenuItemOperator(ContainerOperator<?> cont, String text) {
        this(cont, text, 0);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @param index Ordinal component index.
     */
    public JCheckBoxMenuItemOperator(ContainerOperator<?> cont, int index) {
        this((JCheckBoxMenuItem) waitComponent(cont,
                new JCheckBoxMenuItemFinder(),
                index));
        copyEnvironment(cont);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     */
    public JCheckBoxMenuItemOperator(ContainerOperator<?> cont) {
        this(cont, 0);
    }

    ////////////////////////////////////////////////////////
    //Mapping                                             //
    /**
     * Maps {@code JCheckBoxMenuItem.getState()} through queue
     */
    public boolean getState() {
        return (runMapping(new MapBooleanAction("getState") {
            @Override
            public boolean map() {
                return ((JCheckBoxMenuItem) getSource()).getState();
            }
        }));
    }

    /**
     * Maps {@code JCheckBoxMenuItem.setState(boolean)} through queue
     */
    public void setState(final boolean b) {
        runMapping(new MapVoidAction("setState") {
            @Override
            public void map() {
                ((JCheckBoxMenuItem) getSource()).setState(b);
            }
        });
    }

    //End of mapping                                      //
    ////////////////////////////////////////////////////////
    /**
     * Allows to find component by text.
     */
    public static class JCheckBoxMenuItemByLabelFinder implements ComponentChooser {

        String label;
        StringComparator comparator;

        /**
         * Constructs JCheckBoxMenuItemByLabelFinder.
         *
         * @param lb a text pattern
         * @param comparator specifies string comparision algorithm.
         */
        public JCheckBoxMenuItemByLabelFinder(String lb, StringComparator comparator) {
            label = lb;
            this.comparator = comparator;
        }

        /**
         * Constructs JCheckBoxMenuItemByLabelFinder.
         *
         * @param lb a text pattern
         */
        public JCheckBoxMenuItemByLabelFinder(String lb) {
            this(lb, Operator.getDefaultStringComparator());
        }

        @Override
        public boolean checkComponent(Component comp) {
            if (comp instanceof JCheckBoxMenuItem) {
                if (((JCheckBoxMenuItem) comp).getText() != null) {
                    return (comparator.equals(((JCheckBoxMenuItem) comp).getText(),
                            label));
                }
            }
            return false;
        }

        @Override
        public String getDescription() {
            return "JCheckBoxMenuItem with text \"" + label + "\"";
        }

        @Override
        public String toString() {
            return "JCheckBoxMenuItemByLabelFinder{" + "label=" + label + ", comparator=" + comparator + '}';
        }
    }

    /**
     * Checks component type.
     */
    public static class JCheckBoxMenuItemFinder extends Finder {

        /**
         * Constructs JCheckBoxMenuItemFinder.
         *
         * @param sf other searching criteria.
         */
        public JCheckBoxMenuItemFinder(ComponentChooser sf) {
            super(JCheckBoxMenuItem.class, sf);
        }

        /**
         * Constructs JCheckBoxMenuItemFinder.
         */
        public JCheckBoxMenuItemFinder() {
            super(JCheckBoxMenuItem.class);
        }
    }
}
