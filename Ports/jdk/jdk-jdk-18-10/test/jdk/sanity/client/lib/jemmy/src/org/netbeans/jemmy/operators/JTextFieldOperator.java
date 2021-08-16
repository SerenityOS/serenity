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
import java.awt.event.ActionListener;

import javax.swing.BoundedRangeModel;
import javax.swing.JTextField;

import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.TimeoutExpiredException;

/**
 * <BR><BR>Timeouts used: <BR>
 * JTextComponentOperator.PushKeyTimeout - time between key pressing and
 * releasing during text typing <BR>
 * JTextComponentOperator.BetweenKeysTimeout - time to sleep between two chars
 * typing <BR>
 * JTextComponentOperator.ChangeCaretPositionTimeout - maximum time to chenge
 * caret position <BR>
 * JTextComponentOperator.TypeTextTimeout - maximum time to type text <BR>
 * ComponentOperator.WaitComponentTimeout - time to wait component displayed
 * <BR>
 * ComponentOperator.WaitFocusTimeout - time to wait component focus <BR>
 * JScrollBarOperator.OneScrollClickTimeout - time for one scroll click <BR>
 * JScrollBarOperator.WholeScrollTimeout - time for the whole scrolling <BR>.
 *
 * @see org.netbeans.jemmy.Timeouts
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 *
 */
public class JTextFieldOperator extends JTextComponentOperator {

    /**
     * Constructor.
     *
     * @param b a component
     */
    public JTextFieldOperator(JTextField b) {
        super(b);
    }

    /**
     * Constructs a JTextFieldOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     */
    public JTextFieldOperator(ContainerOperator<?> cont, ComponentChooser chooser, int index) {
        this((JTextField) cont.
                waitSubComponent(new JTextFieldFinder(chooser),
                        index));
        copyEnvironment(cont);
    }

    /**
     * Constructs a JTextFieldOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     */
    public JTextFieldOperator(ContainerOperator<?> cont, ComponentChooser chooser) {
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
    public JTextFieldOperator(ContainerOperator<?> cont, String text, int index) {
        this((JTextField) waitComponent(cont,
                new JTextFieldFinder(new JTextComponentOperator.JTextComponentByTextFinder(text,
                        cont.getComparator())),
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
    public JTextFieldOperator(ContainerOperator<?> cont, String text) {
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
    public JTextFieldOperator(ContainerOperator<?> cont, int index) {
        this((JTextField) waitComponent(cont,
                new JTextFieldFinder(),
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
    public JTextFieldOperator(ContainerOperator<?> cont) {
        this(cont, 0);
    }

    /**
     * Searches JTextField in container.
     *
     * @param cont Container to search component in.
     * @param chooser a component chooser specifying searching criteria.
     * @param index Ordinal component index.
     * @return JTextField instance or null if component was not found.
     */
    public static JTextField findJTextField(Container cont, ComponentChooser chooser, int index) {
        return (JTextField) findJTextComponent(cont, new JTextFieldFinder(chooser), index);
    }

    /**
     * Searches JTextField in container.
     *
     * @param cont Container to search component in.
     * @param chooser a component chooser specifying searching criteria.
     * @return JTextField instance or null if component was not found.
     */
    public static JTextField findJTextField(Container cont, ComponentChooser chooser) {
        return findJTextField(cont, chooser, 0);
    }

    /**
     * Searches JTextField by text.
     *
     * @param cont Container to search component in.
     * @param text Component text.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param index Ordinal component index.
     * @return JTextField instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static JTextField findJTextField(Container cont, String text, boolean ce, boolean ccs, int index) {
        return (findJTextField(cont,
                new JTextFieldFinder(new JTextComponentOperator.JTextComponentByTextFinder(text,
                        new DefaultStringComparator(ce, ccs))),
                index));
    }

    /**
     * Searches JTextField by text.
     *
     * @param cont Container to search component in.
     * @param text Component text.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @return JTextField instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static JTextField findJTextField(Container cont, String text, boolean ce, boolean ccs) {
        return findJTextField(cont, text, ce, ccs, 0);
    }

    /**
     * Waits JTextField in container.
     *
     * @param cont Container to search component in.
     * @param chooser a component chooser specifying searching criteria.
     * @param index Ordinal component index.
     * @return JTextField instance.
     * @throws TimeoutExpiredException
     */
    public static JTextField waitJTextField(Container cont, ComponentChooser chooser, int index) {
        return (JTextField) waitJTextComponent(cont, new JTextFieldFinder(chooser), index);
    }

    /**
     * Waits JTextField in container.
     *
     * @param cont Container to search component in.
     * @param chooser a component chooser specifying searching criteria.
     * @return JTextField instance.
     * @throws TimeoutExpiredException
     */
    public static JTextField waitJTextField(Container cont, ComponentChooser chooser) {
        return waitJTextField(cont, chooser, 0);
    }

    /**
     * Waits JTextField by text.
     *
     * @param cont Container to search component in.
     * @param text Component text.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param index Ordinal component index.
     * @return JTextField instance.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public static JTextField waitJTextField(Container cont, String text, boolean ce, boolean ccs, int index) {
        return (waitJTextField(cont,
                new JTextFieldFinder(new JTextComponentOperator.JTextComponentByTextFinder(text,
                        new DefaultStringComparator(ce, ccs))),
                index));
    }

    /**
     * Waits JTextField by text.
     *
     * @param cont Container to search component in.
     * @param text Component text.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @return JTextField instance.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public static JTextField waitJTextField(Container cont, String text, boolean ce, boolean ccs) {
        return waitJTextField(cont, text, ce, ccs, 0);
    }

    /**
     * Wait some text to be displayed starting from certain position.
     *
     * @param text a text to wait.
     * @param position start position.
     */
    @Override
    public void waitText(String text, int position) {
        super.waitText(removeNewLines(text), position);
    }

    /**
     * Wait some text to be displayed.
     *
     * @param text a text to wait.
     */
    @Override
    public void waitText(String text) {
        super.waitText(removeNewLines(text));
    }

    ////////////////////////////////////////////////////////
    //Mapping                                             //
    /**
     * Maps {@code JTextField.addActionListener(ActionListener)} through queue
     */
    public void addActionListener(final ActionListener actionListener) {
        runMapping(new MapVoidAction("addActionListener") {
            @Override
            public void map() {
                ((JTextField) getSource()).addActionListener(actionListener);
            }
        });
    }

    /**
     * Maps {@code JTextField.getColumns()} through queue
     */
    public int getColumns() {
        return (runMapping(new MapIntegerAction("getColumns") {
            @Override
            public int map() {
                return ((JTextField) getSource()).getColumns();
            }
        }));
    }

    /**
     * Maps {@code JTextField.getHorizontalAlignment()} through queue
     */
    public int getHorizontalAlignment() {
        return (runMapping(new MapIntegerAction("getHorizontalAlignment") {
            @Override
            public int map() {
                return ((JTextField) getSource()).getHorizontalAlignment();
            }
        }));
    }

    /**
     * Maps {@code JTextField.getHorizontalVisibility()} through queue
     */
    public BoundedRangeModel getHorizontalVisibility() {
        return (runMapping(new MapAction<BoundedRangeModel>("getHorizontalVisibility") {
            @Override
            public BoundedRangeModel map() {
                return ((JTextField) getSource()).getHorizontalVisibility();
            }
        }));
    }

    /**
     * Maps {@code JTextField.getScrollOffset()} through queue
     */
    public int getScrollOffset() {
        return (runMapping(new MapIntegerAction("getScrollOffset") {
            @Override
            public int map() {
                return ((JTextField) getSource()).getScrollOffset();
            }
        }));
    }

    /**
     * Maps {@code JTextField.postActionEvent()} through queue
     */
    public void postActionEvent() {
        runMapping(new MapVoidAction("postActionEvent") {
            @Override
            public void map() {
                ((JTextField) getSource()).postActionEvent();
            }
        });
    }

    /**
     * Maps {@code JTextField.removeActionListener(ActionListener)} through queue
     */
    public void removeActionListener(final ActionListener actionListener) {
        runMapping(new MapVoidAction("removeActionListener") {
            @Override
            public void map() {
                ((JTextField) getSource()).removeActionListener(actionListener);
            }
        });
    }

    /**
     * Maps {@code JTextField.setActionCommand(String)} through queue
     */
    public void setActionCommand(final String string) {
        runMapping(new MapVoidAction("setActionCommand") {
            @Override
            public void map() {
                ((JTextField) getSource()).setActionCommand(string);
            }
        });
    }

    /**
     * Maps {@code JTextField.setColumns(int)} through queue
     */
    public void setColumns(final int i) {
        runMapping(new MapVoidAction("setColumns") {
            @Override
            public void map() {
                ((JTextField) getSource()).setColumns(i);
            }
        });
    }

    /**
     * Maps {@code JTextField.setHorizontalAlignment(int)} through queue
     */
    public void setHorizontalAlignment(final int i) {
        runMapping(new MapVoidAction("setHorizontalAlignment") {
            @Override
            public void map() {
                ((JTextField) getSource()).setHorizontalAlignment(i);
            }
        });
    }

    /**
     * Maps {@code JTextField.setScrollOffset(int)} through queue
     */
    public void setScrollOffset(final int i) {
        runMapping(new MapVoidAction("setScrollOffset") {
            @Override
            public void map() {
                ((JTextField) getSource()).setScrollOffset(i);
            }
        });
    }

    //End of mapping                                      //
    ////////////////////////////////////////////////////////
    private String removeNewLines(String text) {
        StringBuffer buff = new StringBuffer(text);
        int i = 0;
        while (i < buff.length()) {
            if (buff.charAt(i) != '\n') {
                i++;
            } else {
                buff.deleteCharAt(i);
            }
        }
        return buff.toString();
    }

    /**
     * Checks component type.
     */
    public static class JTextFieldFinder extends Finder {

        /**
         * Constructs JTextFieldFinder.
         *
         * @param sf other searching criteria.
         */
        public JTextFieldFinder(ComponentChooser sf) {
            super(JTextField.class, sf);
        }

        /**
         * Constructs JTextFieldFinder.
         */
        public JTextFieldFinder() {
            super(JTextField.class);
        }
    }
}
