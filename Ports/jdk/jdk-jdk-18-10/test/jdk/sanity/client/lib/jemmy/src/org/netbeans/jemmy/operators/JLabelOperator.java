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

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.plaf.LabelUI;

import org.netbeans.jemmy.ComponentChooser;

/**
 * <BR><BR>Timeouts used: <BR>
 * ComponentOperator.WaitComponentTimeout - time to wait component displayed
 * <BR>
 * ComponentOperator.WaitStateTimeout - time to wait for text <BR>.
 *
 * @see org.netbeans.jemmy.Timeouts
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 *
 */
public class JLabelOperator extends JComponentOperator {

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
    public JLabelOperator(JLabel b) {
        super(b);
    }

    /**
     * Constructs a JLabelOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     */
    public JLabelOperator(ContainerOperator<?> cont, ComponentChooser chooser, int index) {
        this((JLabel) cont.
                waitSubComponent(new JLabelFinder(chooser),
                        index));
        copyEnvironment(cont);
    }

    /**
     * Constructs a JLabelOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     */
    public JLabelOperator(ContainerOperator<?> cont, ComponentChooser chooser) {
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
     *
     */
    public JLabelOperator(ContainerOperator<?> cont, String text, int index) {
        this((JLabel) waitComponent(cont,
                new JLabelByLabelFinder(text,
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
     *
     */
    public JLabelOperator(ContainerOperator<?> cont, String text) {
        this(cont, text, 0);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @param index Ordinal component index.
     *
     */
    public JLabelOperator(ContainerOperator<?> cont, int index) {
        this((JLabel) waitComponent(cont,
                new JLabelFinder(),
                index));
        copyEnvironment(cont);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     *
     */
    public JLabelOperator(ContainerOperator<?> cont) {
        this(cont, 0);
    }

    /**
     * Searches JLabel in container.
     *
     * @param cont Container to search component in.
     * @param chooser a component chooser specifying searching criteria.
     * @param index Ordinal component index.
     * @return JLabel instance or null if component was not found.
     */
    public static JLabel findJLabel(Container cont, ComponentChooser chooser, int index) {
        return (JLabel) findComponent(cont, new JLabelFinder(chooser), index);
    }

    /**
     * Searches JLabel in container.
     *
     * @param cont Container to search component in.
     * @param chooser a component chooser specifying searching criteria.
     * @return JLabel instance or null if component was not found.
     */
    public static JLabel findJLabel(Container cont, ComponentChooser chooser) {
        return findJLabel(cont, chooser, 0);
    }

    /**
     * Searches JLabel by text.
     *
     * @param cont Container to search component in.
     * @param text Component text.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param index Ordinal component index.
     * @return JLabel instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static JLabel findJLabel(Container cont, String text, boolean ce, boolean ccs, int index) {
        return findJLabel(cont, new JLabelByLabelFinder(text, new DefaultStringComparator(ce, ccs)), index);
    }

    /**
     * Searches JLabel by text.
     *
     * @param cont Container to search component in.
     * @param text Component text.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @return JLabel instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static JLabel findJLabel(Container cont, String text, boolean ce, boolean ccs) {
        return findJLabel(cont, text, ce, ccs, 0);
    }

    /**
     * Waits JLabel in container.
     *
     * @param cont Container to search component in.
     * @param chooser a component chooser specifying searching criteria.
     * @param index Ordinal component index.
     * @return JLabel instance.
     *
     */
    public static JLabel waitJLabel(final Container cont, final ComponentChooser chooser, final int index) {
        return (JLabel) waitComponent(cont, new JLabelFinder(chooser), index);
    }

    /**
     * Waits JLabel in container.
     *
     * @param cont Container to search component in.
     * @param chooser a component chooser specifying searching criteria.
     * @return JLabel instance.
     *
     */
    public static JLabel waitJLabel(Container cont, ComponentChooser chooser) {
        return waitJLabel(cont, chooser, 0);
    }

    /**
     * Waits JLabel by text.
     *
     * @param cont Container to search component in.
     * @param text Component text.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param index Ordinal component index.
     * @return JLabel instance.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     *
     */
    public static JLabel waitJLabel(Container cont, String text, boolean ce, boolean ccs, int index) {
        return waitJLabel(cont, new JLabelByLabelFinder(text, new DefaultStringComparator(ce, ccs)), index);
    }

    /**
     * Waits JLabel by text.
     *
     * @param cont Container to search component in.
     * @param text Component text.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @return JLabel instance.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     *
     */
    public static JLabel waitJLabel(Container cont, String text, boolean ce, boolean ccs) {
        return waitJLabel(cont, text, ce, ccs, 0);
    }

    /**
     * Waits for text. Uses getComparator() comparator.
     *
     * @param text Text to wait for.
     */
    public void waitText(String text) {
        getOutput().printLine("Wait \"" + text + "\" text in component \n    : "
                + toStringSource());
        getOutput().printGolden("Wait \"" + text + "\" text");
        waitState(new JLabelByLabelFinder(text, getComparator()));
    }

    /**
     * Returns information about component.
     */
    @Override
    public Hashtable<String, Object> getDump() {
        Hashtable<String, Object> result = super.getDump();
        if (((JLabel) getSource()).getText() != null) {
            result.put(TEXT_DPROP, ((JLabel) getSource()).getText());
        } else {
            result.put(TEXT_DPROP, "null");
        }
        return result;
    }

    ////////////////////////////////////////////////////////
    //Mapping                                             //
    /**
     * Maps {@code JLabel.getDisabledIcon()} through queue
     */
    public Icon getDisabledIcon() {
        return (runMapping(new MapAction<Icon>("getDisabledIcon") {
            @Override
            public Icon map() {
                return ((JLabel) getSource()).getDisabledIcon();
            }
        }));
    }

    /**
     * Maps {@code JLabel.getDisplayedMnemonic()} through queue
     */
    public int getDisplayedMnemonic() {
        return (runMapping(new MapIntegerAction("getDisplayedMnemonic") {
            @Override
            public int map() {
                return ((JLabel) getSource()).getDisplayedMnemonic();
            }
        }));
    }

    /**
     * Maps {@code JLabel.getHorizontalAlignment()} through queue
     */
    public int getHorizontalAlignment() {
        return (runMapping(new MapIntegerAction("getHorizontalAlignment") {
            @Override
            public int map() {
                return ((JLabel) getSource()).getHorizontalAlignment();
            }
        }));
    }

    /**
     * Maps {@code JLabel.getHorizontalTextPosition()} through queue
     */
    public int getHorizontalTextPosition() {
        return (runMapping(new MapIntegerAction("getHorizontalTextPosition") {
            @Override
            public int map() {
                return ((JLabel) getSource()).getHorizontalTextPosition();
            }
        }));
    }

    /**
     * Maps {@code JLabel.getIcon()} through queue
     */
    public Icon getIcon() {
        return (runMapping(new MapAction<Icon>("getIcon") {
            @Override
            public Icon map() {
                return ((JLabel) getSource()).getIcon();
            }
        }));
    }

    /**
     * Maps {@code JLabel.getIconTextGap()} through queue
     */
    public int getIconTextGap() {
        return (runMapping(new MapIntegerAction("getIconTextGap") {
            @Override
            public int map() {
                return ((JLabel) getSource()).getIconTextGap();
            }
        }));
    }

    /**
     * Maps {@code JLabel.getLabelFor()} through queue
     */
    public Component getLabelFor() {
        return (runMapping(new MapAction<Component>("getLabelFor") {
            @Override
            public Component map() {
                return ((JLabel) getSource()).getLabelFor();
            }
        }));
    }

    /**
     * Maps {@code JLabel.getText()} through queue
     */
    public String getText() {
        return (runMapping(new MapAction<String>("getText") {
            @Override
            public String map() {
                return ((JLabel) getSource()).getText();
            }
        }));
    }

    /**
     * Maps {@code JLabel.getUI()} through queue
     */
    public LabelUI getUI() {
        return (runMapping(new MapAction<LabelUI>("getUI") {
            @Override
            public LabelUI map() {
                return ((JLabel) getSource()).getUI();
            }
        }));
    }

    /**
     * Maps {@code JLabel.getVerticalAlignment()} through queue
     */
    public int getVerticalAlignment() {
        return (runMapping(new MapIntegerAction("getVerticalAlignment") {
            @Override
            public int map() {
                return ((JLabel) getSource()).getVerticalAlignment();
            }
        }));
    }

    /**
     * Maps {@code JLabel.getVerticalTextPosition()} through queue
     */
    public int getVerticalTextPosition() {
        return (runMapping(new MapIntegerAction("getVerticalTextPosition") {
            @Override
            public int map() {
                return ((JLabel) getSource()).getVerticalTextPosition();
            }
        }));
    }

    /**
     * Maps {@code JLabel.setDisabledIcon(Icon)} through queue
     */
    public void setDisabledIcon(final Icon icon) {
        runMapping(new MapVoidAction("setDisabledIcon") {
            @Override
            public void map() {
                ((JLabel) getSource()).setDisabledIcon(icon);
            }
        });
    }

    /**
     * Maps {@code JLabel.setDisplayedMnemonic(char)} through queue
     */
    public void setDisplayedMnemonic(final char c) {
        runMapping(new MapVoidAction("setDisplayedMnemonic") {
            @Override
            public void map() {
                ((JLabel) getSource()).setDisplayedMnemonic(c);
            }
        });
    }

    /**
     * Maps {@code JLabel.setDisplayedMnemonic(int)} through queue
     */
    public void setDisplayedMnemonic(final int i) {
        runMapping(new MapVoidAction("setDisplayedMnemonic") {
            @Override
            public void map() {
                ((JLabel) getSource()).setDisplayedMnemonic(i);
            }
        });
    }

    /**
     * Maps {@code JLabel.setHorizontalAlignment(int)} through queue
     */
    public void setHorizontalAlignment(final int i) {
        runMapping(new MapVoidAction("setHorizontalAlignment") {
            @Override
            public void map() {
                ((JLabel) getSource()).setHorizontalAlignment(i);
            }
        });
    }

    /**
     * Maps {@code JLabel.setHorizontalTextPosition(int)} through queue
     */
    public void setHorizontalTextPosition(final int i) {
        runMapping(new MapVoidAction("setHorizontalTextPosition") {
            @Override
            public void map() {
                ((JLabel) getSource()).setHorizontalTextPosition(i);
            }
        });
    }

    /**
     * Maps {@code JLabel.setIcon(Icon)} through queue
     */
    public void setIcon(final Icon icon) {
        runMapping(new MapVoidAction("setIcon") {
            @Override
            public void map() {
                ((JLabel) getSource()).setIcon(icon);
            }
        });
    }

    /**
     * Maps {@code JLabel.setIconTextGap(int)} through queue
     */
    public void setIconTextGap(final int i) {
        runMapping(new MapVoidAction("setIconTextGap") {
            @Override
            public void map() {
                ((JLabel) getSource()).setIconTextGap(i);
            }
        });
    }

    /**
     * Maps {@code JLabel.setLabelFor(Component)} through queue
     */
    public void setLabelFor(final Component component) {
        runMapping(new MapVoidAction("setLabelFor") {
            @Override
            public void map() {
                ((JLabel) getSource()).setLabelFor(component);
            }
        });
    }

    /**
     * Maps {@code JLabel.setText(String)} through queue
     */
    public void setText(final String string) {
        runMapping(new MapVoidAction("setText") {
            @Override
            public void map() {
                ((JLabel) getSource()).setText(string);
            }
        });
    }

    /**
     * Maps {@code JLabel.setUI(LabelUI)} through queue
     */
    public void setUI(final LabelUI labelUI) {
        runMapping(new MapVoidAction("setUI") {
            @Override
            public void map() {
                ((JLabel) getSource()).setUI(labelUI);
            }
        });
    }

    /**
     * Maps {@code JLabel.setVerticalAlignment(int)} through queue
     */
    public void setVerticalAlignment(final int i) {
        runMapping(new MapVoidAction("setVerticalAlignment") {
            @Override
            public void map() {
                ((JLabel) getSource()).setVerticalAlignment(i);
            }
        });
    }

    /**
     * Maps {@code JLabel.setVerticalTextPosition(int)} through queue
     */
    public void setVerticalTextPosition(final int i) {
        runMapping(new MapVoidAction("setVerticalTextPosition") {
            @Override
            public void map() {
                ((JLabel) getSource()).setVerticalTextPosition(i);
            }
        });
    }

    //End of mapping                                      //
    ////////////////////////////////////////////////////////
    /**
     * Allows to find component by text.
     */
    public static class JLabelByLabelFinder implements ComponentChooser {

        String label;
        StringComparator comparator;

        /**
         * Constructs JLabelByLabelFinder.
         *
         * @param lb a text pattern
         * @param comparator specifies string comparision algorithm.
         */
        public JLabelByLabelFinder(String lb, StringComparator comparator) {
            label = lb;
            this.comparator = comparator;
        }

        /**
         * Constructs JLabelByLabelFinder.
         *
         * @param lb a text pattern
         */
        public JLabelByLabelFinder(String lb) {
            this(lb, Operator.getDefaultStringComparator());
        }

        @Override
        public boolean checkComponent(Component comp) {
            if (comp instanceof JLabel) {
                if (((JLabel) comp).getText() != null) {
                    return (comparator.equals(((JLabel) comp).getText(),
                            label));
                }
            }
            return false;
        }

        @Override
        public String getDescription() {
            return "JLabel with text \"" + label + "\"";
        }

        @Override
        public String toString() {
            return "JLabelByLabelFinder{" + "label=" + label + ", comparator=" + comparator + '}';
        }
    }

    /**
     * Checks component type.
     */
    public static class JLabelFinder extends Finder {

        /**
         * Constructs JLabelFinder.
         *
         * @param sf other searching criteria.
         */
        public JLabelFinder(ComponentChooser sf) {
            super(JLabel.class, sf);
        }

        /**
         * Constructs JLabelFinder.
         */
        public JLabelFinder() {
            super(JLabel.class);
        }
    }
}
