/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.TextComponent;
import java.awt.event.TextListener;
import java.util.Hashtable;

import org.netbeans.jemmy.Action;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.Outputable;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.Timeoutable;
import org.netbeans.jemmy.Timeouts;
import org.netbeans.jemmy.drivers.DriverManager;
import org.netbeans.jemmy.drivers.TextDriver;

/**
 * This operator type covers java.awt.TextArea component.
 *
 *
 * @see org.netbeans.jemmy.Timeouts
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 *
 */
public class TextComponentOperator extends ComponentOperator
        implements Timeoutable, Outputable {

    /**
     * Identifier for a "text" property.
     *
     * @see #getDump
     */
    public static final String TEXT_DPROP = "Text";

    /**
     * Identifier for a "selected text" property.
     *
     * @see #getDump
     */
    public static final String SELECTED_TEXT_DPROP = "Selected text";

    private final static long PUSH_KEY_TIMEOUT = 0;
    private final static long BETWEEN_KEYS_TIMEOUT = 0;
    private final static long CHANGE_CARET_POSITION_TIMEOUT = 60000;
    private final static long TYPE_TEXT_TIMEOUT = 60000;

    private Timeouts timeouts;
    private TestOut output;

    private TextDriver driver;

    /**
     * Constructor.
     *
     * @param b The {@code java.awt.TextComponent} managed by this
     * instance.
     */
    public TextComponentOperator(TextComponent b) {
        super(b);
        driver = DriverManager.getTextDriver(getClass());
    }

    /**
     * Constructs a TextComponentOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     */
    public TextComponentOperator(ContainerOperator<?> cont, ComponentChooser chooser, int index) {
        this((TextComponent) cont.
                waitSubComponent(new TextComponentFinder(chooser),
                        index));
        copyEnvironment(cont);
    }

    /**
     * Constructs a TextComponentOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     */
    public TextComponentOperator(ContainerOperator<?> cont, ComponentChooser chooser) {
        this(cont, chooser, 0);
    }

    /**
     * Constructor. Waits for a component in a container to show. The component
     * is identified as the {@code index+1}'th
     * {@code java.awt.TextComponent} that shows, lies below the container
     * in the display containment hierarchy, and that has the desired text. Uses
     * cont's timeout and output for waiting and to init this operator.
     *
     * @param cont The operator for a container containing the sought for
     * textComponent.
     * @param text TextComponent text.
     * @param index Ordinal component index. The first component has
     * {@code index} 0.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public TextComponentOperator(ContainerOperator<?> cont, String text, int index) {
        this((TextComponent) waitComponent(cont,
                new TextComponentByTextFinder(text,
                        cont.getComparator()),
                index));
        copyEnvironment(cont);
    }

    /**
     * Constructor. Waits for a component in a container to show. The component
     * is identified as the first {@code java.awt.TextComponent} that
     * shows, lies below the container in the display containment hierarchy, and
     * that has the desired text. Uses cont's timeout and output for waiting and
     * to init this operator.
     *
     * @param cont The operator for a container containing the sought for
     * textComponent.
     * @param text TextComponent text.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public TextComponentOperator(ContainerOperator<?> cont, String text) {
        this(cont, text, 0);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont The operator for a container containing the sought for
     * textComponent.
     * @param index Ordinal component index.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public TextComponentOperator(ContainerOperator<?> cont, int index) {
        this((TextComponent) waitComponent(cont,
                new TextComponentFinder(),
                index));
        copyEnvironment(cont);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont The operator for a container containing the sought for
     * textComponent.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public TextComponentOperator(ContainerOperator<?> cont) {
        this(cont, 0);
    }

    /**
     * Searches TextComponent in a container.
     *
     * @param cont Container in which to search for the component. The container
     * lies above the component in the display containment hierarchy. The
     * containment need not be direct.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation,
     * defining and applying search criteria.
     * @param index Ordinal component index. The first {@code index} is 0.
     * @return TextComponent instance or null if component was not found.
     */
    public static TextComponent findTextComponent(Container cont, ComponentChooser chooser, int index) {
        return (TextComponent) findComponent(cont, new TextComponentFinder(chooser), index);
    }

    /**
     * Searches for the first TextComponent in a container.
     *
     * @param cont Container in which to search for the component. The container
     * lies above the component in the display containment hierarchy. The
     * containment need not be direct.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation,
     * defining and applying search criteria.
     * @return TextComponent instance or null if component was not found.
     */
    public static TextComponent findTextComponent(Container cont, ComponentChooser chooser) {
        return findTextComponent(cont, chooser, 0);
    }

    /**
     * Searches TextComponent by text.
     *
     * @param cont Container to search component in.
     * @param text TextComponent text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param index Ordinal component index.
     * @return TextComponent instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static TextComponent findTextComponent(Container cont, String text, boolean ce, boolean ccs, int index) {
        return findTextComponent(cont, new TextComponentByTextFinder(text, new DefaultStringComparator(ce, ccs)), index);
    }

    /**
     * Searches TextComponent by text.
     *
     * @param cont Container to search component in.
     * @param text TextComponent text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @return TextComponent instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static TextComponent findTextComponent(Container cont, String text, boolean ce, boolean ccs) {
        return findTextComponent(cont, text, ce, ccs, 0);
    }

    /**
     * Waits TextComponent in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @return TextComponent instance.
     */
    public static TextComponent waitTextComponent(Container cont, ComponentChooser chooser, int index) {
        return (TextComponent) waitComponent(cont, new TextComponentFinder(chooser), index);
    }

    /**
     * Waits 0'th TextComponent in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return TextComponent instance.
     */
    public static TextComponent waitTextComponent(Container cont, ComponentChooser chooser) {
        return waitTextComponent(cont, chooser, 0);
    }

    /**
     * Waits TextComponent by text.
     *
     * @param cont Container to search component in.
     * @param text TextComponent text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param index Ordinal component index.
     * @return TextComponent instance.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static TextComponent waitTextComponent(Container cont, String text, boolean ce, boolean ccs, int index) {
        return waitTextComponent(cont, new TextComponentByTextFinder(text, new DefaultStringComparator(ce, ccs)), index);
    }

    /**
     * Waits TextComponent by text.
     *
     * @param cont Container to search component in.
     * @param text TextComponent text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @return TextComponent instance.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static TextComponent waitTextComponent(Container cont, String text, boolean ce, boolean ccs) {
        return waitTextComponent(cont, text, ce, ccs, 0);
    }

    static {
        Timeouts.initDefault("TextComponentOperator.PushKeyTimeout", PUSH_KEY_TIMEOUT);
        Timeouts.initDefault("TextComponentOperator.BetweenKeysTimeout", BETWEEN_KEYS_TIMEOUT);
        Timeouts.initDefault("TextComponentOperator.ChangeCaretPositionTimeout", CHANGE_CARET_POSITION_TIMEOUT);
        Timeouts.initDefault("TextComponentOperator.TypeTextTimeout", TYPE_TEXT_TIMEOUT);
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
                = (TextDriver) DriverManager.
                getDriver(DriverManager.TEXT_DRIVER_ID,
                        getClass(),
                        anotherOperator.getProperties());
    }

    /**
     * Changes caret position.
     *
     * @param position Position to move caret to.
     *
     */
    public void changeCaretPosition(final int position) {
        makeComponentVisible();
        produceTimeRestricted(new Action<Void, Void>() {
            @Override
            public Void launch(Void obj) {
                driver.changeCaretPosition(TextComponentOperator.this, position);
                return null;
            }

            @Override
            public String getDescription() {
                return "Caret moving";
            }

            @Override
            public String toString() {
                return "TextComponentOperator.changeCaretPosition.Action{description = " + getDescription() + '}';
            }
        }, "TextComponentOperator.ChangeCaretPositionTimeout");
    }

    /**
     * Selects a part of text.
     *
     * @param startPosition Start caret position
     * @param finalPosition Final caret position
     *
     */
    public void selectText(final int startPosition, final int finalPosition) {
        makeComponentVisible();
        produceTimeRestricted(new Action<Void, Void>() {
            @Override
            public Void launch(Void obj) {
                driver.selectText(TextComponentOperator.this, startPosition, finalPosition);
                return null;
            }

            @Override
            public String getDescription() {
                return "Text selecting";
            }

            @Override
            public String toString() {
                return "TextComponentOperator.selectText.Action{description = " + getDescription() + '}';
            }
        }, "TextComponentOperator.TypeTextTimeout");
    }

    /**
     * Finds start text position.
     *
     * @param text Text to be searched.
     * @param index Index of text instance (first instance has index 0)
     * @return Caret position correspondent to text start.
     */
    public int getPositionByText(String text, int index) {
        String allText = getText();
        int position = 0;
        int ind = 0;
        while ((position = allText.indexOf(text, position)) >= 0) {
            if (ind == index) {
                return position;
            } else {
                ind++;
            }
            position = position + text.length();
        }
        return -1;
    }

    /**
     * Finds start text position.
     *
     * @param text Text to be searched.
     * @return Caret position correspondent to text start.
     */
    public int getPositionByText(String text) {
        return getPositionByText(text, 0);
    }

    /**
     * Clears text.
     *
     */
    public void clearText() {
        output.printLine("Clearing text in text component\n    : "
                + toStringSource());
        output.printGolden("Clearing text in text component");
        makeComponentVisible();
        produceTimeRestricted(new Action<Void, Void>() {
            @Override
            public Void launch(Void obj) {
                driver.clearText(TextComponentOperator.this);
                return null;
            }

            @Override
            public String getDescription() {
                return "Text clearing";
            }

            @Override
            public String toString() {
                return "TextComponentOperator.clearText.Action{description = " + getDescription() + '}';
            }
        }, "TextComponentOperator.TypeTextTimeout");
    }

    /**
     * Types text starting from known position.
     *
     * @param text Text to be typed.
     * @param caretPosition Position to start type text
     */
    public void typeText(final String text, final int caretPosition) {
        output.printLine("Typing text \"" + text + "\" from "
                + Integer.toString(caretPosition) + " position "
                + "in text component\n    : "
                + toStringSource());
        output.printGolden("Typing text \"" + text + "\" in text component");
        makeComponentVisible();
        produceTimeRestricted(new Action<Void, Void>() {
            @Override
            public Void launch(Void obj) {
                driver.typeText(TextComponentOperator.this, text, caretPosition);
                return null;
            }

            @Override
            public String getDescription() {
                return "Text typing";
            }

            @Override
            public String toString() {
                return "TextComponentOperator.typeText.Action{description = " + getDescription() + '}';
            }
        }, "TextComponentOperator.TypeTextTimeout");
    }

    /**
     * Types text starting from known position.
     *
     * @param text Text to be typed.
     */
    public void typeText(String text) {
        typeText(text, getCaretPosition());
    }

    /**
     * Requests a focus, clears text, types new one and pushes Enter.
     *
     * @param text New text value. Shouldn't include final '\n'.
     *
     */
    public void enterText(final String text) {
        makeComponentVisible();
        produceTimeRestricted(new Action<Void, Void>() {
            @Override
            public Void launch(Void obj) {
                driver.enterText(TextComponentOperator.this, text);
                return null;
            }

            @Override
            public String getDescription() {
                return "Text entering";
            }

            @Override
            public String toString() {
                return "TextComponentOperator.enterText.Action{description = " + getDescription() + '}';
            }
        }, "TextComponentOperator.TypeTextTimeout");
    }

    @Override
    public Hashtable<String, Object> getDump() {
        Hashtable<String, Object> result = super.getDump();
        result.put(TEXT_DPROP, ((TextComponent) getSource()).getText());
        String selected = ((TextComponent) getSource()).getSelectedText();
        result.put(SELECTED_TEXT_DPROP, (selected != null) ? selected : "");
        return result;
    }

    ////////////////////////////////////////////////////////
    //Mapping                                             //
    /**
     * Maps {@code TextComponent.addTextListener(TextListener)} through queue
     */
    public void addTextListener(final TextListener textListener) {
        runMapping(new MapVoidAction("addTextListener") {
            @Override
            public void map() {
                ((TextComponent) getSource()).addTextListener(textListener);
            }
        });
    }

    /**
     * Maps {@code TextComponent.getCaretPosition()} through queue
     */
    public int getCaretPosition() {
        return (runMapping(new MapIntegerAction("getCaretPosition") {
            @Override
            public int map() {
                return ((TextComponent) getSource()).getCaretPosition();
            }
        }));
    }

    /**
     * Maps {@code TextComponent.getSelectedText()} through queue
     */
    public String getSelectedText() {
        return (runMapping(new MapAction<String>("getSelectedText") {
            @Override
            public String map() {
                return ((TextComponent) getSource()).getSelectedText();
            }
        }));
    }

    /**
     * Maps {@code TextComponent.getSelectionEnd()} through queue
     */
    public int getSelectionEnd() {
        return (runMapping(new MapIntegerAction("getSelectionEnd") {
            @Override
            public int map() {
                return ((TextComponent) getSource()).getSelectionEnd();
            }
        }));
    }

    /**
     * Maps {@code TextComponent.getSelectionStart()} through queue
     */
    public int getSelectionStart() {
        return (runMapping(new MapIntegerAction("getSelectionStart") {
            @Override
            public int map() {
                return ((TextComponent) getSource()).getSelectionStart();
            }
        }));
    }

    /**
     * Maps {@code TextComponent.getText()} through queue
     */
    public String getText() {
        return (runMapping(new MapAction<String>("getText") {
            @Override
            public String map() {
                return ((TextComponent) getSource()).getText();
            }
        }));
    }

    /**
     * Maps {@code TextComponent.isEditable()} through queue
     */
    public boolean isEditable() {
        return (runMapping(new MapBooleanAction("isEditable") {
            @Override
            public boolean map() {
                return ((TextComponent) getSource()).isEditable();
            }
        }));
    }

    /**
     * Maps {@code TextComponent.removeTextListener(TextListener)} through queue
     */
    public void removeTextListener(final TextListener textListener) {
        runMapping(new MapVoidAction("removeTextListener") {
            @Override
            public void map() {
                ((TextComponent) getSource()).removeTextListener(textListener);
            }
        });
    }

    /**
     * Maps {@code TextComponent.select(int, int)} through queue
     */
    public void select(final int i, final int i1) {
        runMapping(new MapVoidAction("select") {
            @Override
            public void map() {
                ((TextComponent) getSource()).select(i, i1);
            }
        });
    }

    /**
     * Maps {@code TextComponent.selectAll()} through queue
     */
    public void selectAll() {
        runMapping(new MapVoidAction("selectAll") {
            @Override
            public void map() {
                ((TextComponent) getSource()).selectAll();
            }
        });
    }

    /**
     * Maps {@code TextComponent.setCaretPosition(int)} through queue
     */
    public void setCaretPosition(final int i) {
        runMapping(new MapVoidAction("setCaretPosition") {
            @Override
            public void map() {
                ((TextComponent) getSource()).setCaretPosition(i);
            }
        });
    }

    /**
     * Maps {@code TextComponent.setEditable(boolean)} through queue
     */
    public void setEditable(final boolean b) {
        runMapping(new MapVoidAction("setEditable") {
            @Override
            public void map() {
                ((TextComponent) getSource()).setEditable(b);
            }
        });
    }

    /**
     * Maps {@code TextComponent.setSelectionEnd(int)} through queue
     */
    public void setSelectionEnd(final int i) {
        runMapping(new MapVoidAction("setSelectionEnd") {
            @Override
            public void map() {
                ((TextComponent) getSource()).setSelectionEnd(i);
            }
        });
    }

    /**
     * Maps {@code TextComponent.setSelectionStart(int)} through queue
     */
    public void setSelectionStart(final int i) {
        runMapping(new MapVoidAction("setSelectionStart") {
            @Override
            public void map() {
                ((TextComponent) getSource()).setSelectionStart(i);
            }
        });
    }

    /**
     * Maps {@code TextComponent.setText(String)} through queue
     */
    public void setText(final String string) {
        runMapping(new MapVoidAction("setText") {
            @Override
            public void map() {
                ((TextComponent) getSource()).setText(string);
            }
        });
    }

    //End of mapping                                      //
    ////////////////////////////////////////////////////////
    /**
     * Return a TextDriver used by this component.
     *
     * @return a driver got by the operator during creation.
     */
    protected TextDriver getTextDriver() {
        return driver;
    }

    /**
     * Allows to find component by text.
     */
    public static class TextComponentByTextFinder implements ComponentChooser {

        String label;
        StringComparator comparator;

        /**
         * Constructs TextComponentByTextFinder.
         *
         * @param lb a text pattern
         * @param comparator specifies string comparision algorithm.
         */
        public TextComponentByTextFinder(String lb, StringComparator comparator) {
            label = lb;
            this.comparator = comparator;
        }

        /**
         * Constructs TextComponentByTextFinder.
         *
         * @param lb a text pattern
         */
        public TextComponentByTextFinder(String lb) {
            this(lb, Operator.getDefaultStringComparator());
        }

        @Override
        public boolean checkComponent(Component comp) {
            if (comp instanceof TextComponent) {
                if (((TextComponent) comp).getText() != null) {
                    return (comparator.equals(((TextComponent) comp).getText(),
                            label));
                }
            }
            return false;
        }

        @Override
        public String getDescription() {
            return "TextComponent with text \"" + label + "\"";
        }

        @Override
        public String toString() {
            return "TextComponentByTextFinder{" + "label=" + label + ", comparator=" + comparator + '}';
        }
    }

    /**
     * Checks component type.
     */
    public static class TextComponentFinder extends Finder {

        /**
         * Constructs TextComponentFinder.
         *
         * @param sf other searching criteria.
         */
        public TextComponentFinder(ComponentChooser sf) {
            super(TextComponent.class, sf);
        }

        /**
         * Constructs TextComponentFinder.
         */
        public TextComponentFinder() {
            super(TextComponent.class);
        }
    }
}
