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
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;
import java.util.Hashtable;

import javax.swing.AbstractButton;
import javax.swing.ButtonModel;
import javax.swing.Icon;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ButtonUI;

import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.JemmyException;
import org.netbeans.jemmy.Outputable;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.TimeoutExpiredException;
import org.netbeans.jemmy.Timeoutable;
import org.netbeans.jemmy.Timeouts;
import org.netbeans.jemmy.drivers.ButtonDriver;
import org.netbeans.jemmy.drivers.DriverManager;

/**
 *
 * <BR><BR>Timeouts used: <BR>
 * AbstractButtonOperator.PushButtonTimeout - time between button pressing and
 * releasing<BR>
 * ComponentOperator.WaitComponentTimeout - time to wait button displayed <BR>
 * ComponentOperator.WaitComponentEnabledTimeout - time to wait button enabled
 * <BR>
 * ComponentOperator.WaitStateTimeout - time to wait for text <BR>.
 *
 * @see org.netbeans.jemmy.Timeouts
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 *
 */
public class AbstractButtonOperator extends JComponentOperator
        implements Timeoutable, Outputable {

    /**
     * Identifier for a text property.
     *
     * @see #getDump
     */
    public static final String TEXT_DPROP = "Text";

    /**
     * Identifier for a selected text property.
     *
     * @see #getDump
     */
    public static final String IS_SELECTED_DPROP = "Selected";

    /**
     * Default value for AbstractButtonOperator.PushButtonTimeout timeout.
     */
    private final static long PUSH_BUTTON_TIMEOUT = 0;

    private Timeouts timeouts;
    private TestOut output;

    ButtonDriver driver;

    /**
     * Constructor.
     *
     * @param b The {@code java.awt.AbstractButton} managed by this
     * instance.
     */
    public AbstractButtonOperator(AbstractButton b) {
        super(b);
        driver = DriverManager.getButtonDriver(getClass());
    }

    /**
     * Constructs an AbstractButtonOperator object.
     *
     * @param cont container
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     */
    public AbstractButtonOperator(ContainerOperator<?> cont, ComponentChooser chooser, int index) {
        this((AbstractButton) cont.
                waitSubComponent(new AbstractButtonFinder(chooser),
                        index));
        copyEnvironment(cont);
    }

    /**
     * Constructs an AbstractButtonOperator object.
     *
     * @param cont container
     * @param chooser a component chooser specifying searching criteria.
     */
    public AbstractButtonOperator(ContainerOperator<?> cont, ComponentChooser chooser) {
        this(cont, chooser, 0);
    }

    /**
     * Constructor. Waits for a component in a container to show. The component
     * is identified as the {@code index+1}'th
     * {@code javax.swing.AbstractButton} that shows, lies below the
     * container in the display containment hierarchy, and that has the desired
     * text. Uses cont's timeout and output for waiting and to init this
     * operator.
     *
     * @param cont The operator for a container containing the sought for
     * button.
     * @param text Button text.
     * @param index Ordinal component index. The first component has
     * {@code index} 0.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public AbstractButtonOperator(ContainerOperator<?> cont, String text, int index) {
        this((AbstractButton) waitComponent(cont,
                new AbstractButtonByLabelFinder(text,
                        cont.getComparator()),
                index));
        copyEnvironment(cont);
    }

    /**
     * Constructor. Waits for a component in a container to show. The component
     * is identified as the first {@code javax.swing.AbstractButton} that
     * shows, lies below the container in the display containment hierarchy, and
     * that has the desired text. Uses cont's timeout and output for waiting and
     * to init this operator.
     *
     * @param cont The operator for a container containing the sought for
     * button.
     * @param text Button text.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public AbstractButtonOperator(ContainerOperator<?> cont, String text) {
        this(cont, text, 0);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont The operator for a container containing the sought for
     * button.
     * @param index Ordinal component index.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public AbstractButtonOperator(ContainerOperator<?> cont, int index) {
        this((AbstractButton) waitComponent(cont,
                new AbstractButtonFinder(),
                index));
        copyEnvironment(cont);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont The operator for a container containing the sought for
     * button.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public AbstractButtonOperator(ContainerOperator<?> cont) {
        this(cont, 0);
    }

    /**
     * Searches AbstractButton in a container.
     *
     * @param cont Container in which to search for the component. The container
     * lies above the component in the display containment hierarchy. The
     * containment need not be direct.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation,
     * defining and applying search criteria.
     * @param index Ordinal component index. The first {@code index} is 0.
     * @return AbstractButton instance or null if component was not found.
     */
    public static AbstractButton findAbstractButton(Container cont, ComponentChooser chooser, int index) {
        return (AbstractButton) findComponent(cont, new AbstractButtonFinder(chooser), index);
    }

    /**
     * Searches for the first AbstractButton in a container.
     *
     * @param cont Container in which to search for the component. The container
     * lies above the component in the display containment hierarchy. The
     * containment need not be direct.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation,
     * defining and applying search criteria.
     * @return AbstractButton instance or null if component was not found.
     */
    public static AbstractButton findAbstractButton(Container cont, ComponentChooser chooser) {
        return findAbstractButton(cont, chooser, 0);
    }

    /**
     * Searches AbstractButton by text.
     *
     * @param cont Container to search component in.
     * @param text Button text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param index Ordinal component index.
     * @return AbstractButton instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static AbstractButton findAbstractButton(Container cont, String text, boolean ce, boolean ccs, int index) {
        return findAbstractButton(cont, new AbstractButtonByLabelFinder(text, new DefaultStringComparator(ce, ccs)), index);
    }

    /**
     * Searches AbstractButton by text.
     *
     * @param cont Container to search component in.
     * @param text Button text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @return AbstractButton instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static AbstractButton findAbstractButton(Container cont, String text, boolean ce, boolean ccs) {
        return findAbstractButton(cont, text, ce, ccs, 0);
    }

    /**
     * Waits AbstractButton in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @return AbstractButton instance.
     * @throws TimeoutExpiredException
     */
    public static AbstractButton waitAbstractButton(Container cont, ComponentChooser chooser, int index) {
        return (AbstractButton) waitComponent(cont, new AbstractButtonFinder(chooser), index);
    }

    /**
     * Waits 0'th AbstractButton in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return AbstractButton instance.
     * @throws TimeoutExpiredException
     */
    public static AbstractButton waitAbstractButton(Container cont, ComponentChooser chooser) {
        return waitAbstractButton(cont, chooser, 0);
    }

    /**
     * Waits AbstractButton by text.
     *
     * @param cont Container to search component in.
     * @param text Button text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param index Ordinal component index.
     * @return AbstractButton instance.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public static AbstractButton waitAbstractButton(Container cont, String text, boolean ce, boolean ccs, int index) {
        return waitAbstractButton(cont, new AbstractButtonByLabelFinder(text, new DefaultStringComparator(ce, ccs)), index);
    }

    /**
     * Waits AbstractButton by text.
     *
     * @param cont Container to search component in.
     * @param text Button text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @return AbstractButton instance.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public static AbstractButton waitAbstractButton(Container cont, String text, boolean ce, boolean ccs) {
        return waitAbstractButton(cont, text, ce, ccs, 0);
    }

    static {
        Timeouts.initDefault("AbstractButtonOperator.PushButtonTimeout", PUSH_BUTTON_TIMEOUT);
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
        driver = DriverManager.getButtonDriver(this);
    }

    /**
     * Pushs the button using a ButtonDriver registered for this operator.
     */
    public void push() {
        output.printLine("Push button\n    :" + toStringSource());
        output.printGolden("Push button");
        makeComponentVisible();
        try {
            waitComponentEnabled();
        } catch (InterruptedException e) {
            throw (new JemmyException("Interrupted", e));
        }
        driver.push(this);
    }

    /**
     * Runs {@code push()} method in a separate thread.
     */
    public void pushNoBlock() {
        produceNoBlocking(new NoBlockingAction<Void, Void>("Button pushing") {
            @Override
            public Void doAction(Void param) {
                push();
                return null;
            }
        });
    }

    /**
     * Changes selection if necessary. Uses {@code push()} method in order
     * to do so.
     *
     * @param selected a button selection.
     */
    public void changeSelection(boolean selected) {
        if (isSelected() != selected) {
            push();
        }
        if (getVerification()) {
            waitSelected(selected);
        }
    }

    /**
     * Runs {@code changeSelection(boolean)} method in a separate thread.
     *
     * @param selected a button selection.
     */
    public void changeSelectionNoBlock(boolean selected) {
        produceNoBlocking(new NoBlockingAction<Void, Boolean>("Button selection changing") {
            @Override
            public Void doAction(Boolean param) {
                changeSelection(param);
                return null;
            }
        }, selected ? Boolean.TRUE : Boolean.FALSE);
    }

    /**
     * Press the button by mouse.
     *
     * @throws TimeoutExpiredException
     */
    public void press() {
        output.printLine("Press button\n    :" + toStringSource());
        output.printGolden("Press button");
        makeComponentVisible();
        try {
            waitComponentEnabled();
        } catch (InterruptedException e) {
            throw (new JemmyException("Interrupted", e));
        }
        driver.press(this);
    }

    /**
     * Releases the button by mouse.
     *
     * @throws TimeoutExpiredException
     */
    public void release() {
        output.printLine("Release button\n    :" + toStringSource());
        output.printGolden("Release button");
        try {
            waitComponentEnabled();
        } catch (InterruptedException e) {
            throw (new JemmyException("Interrupted", e));
        }
        driver.release(this);
    }

    /**
     * Waits for button to be selected.
     *
     * @param selected a button selection.
     */
    public void waitSelected(final boolean selected) {
        getOutput().printLine("Wait button to be selected \n    : "
                + toStringSource());
        getOutput().printGolden("Wait button to be selected");
        waitState(new ComponentChooser() {
            @Override
            public boolean checkComponent(Component comp) {
                return isSelected() == selected;
            }

            @Override
            public String getDescription() {
                return ("Items has been "
                        + (selected ? "" : "un") + "selected");
            }

            @Override
            public String toString() {
                return "waitSelected.ComponentChooser{description = " + getDescription() + '}';
            }
        });
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
        waitState(new AbstractButtonByLabelFinder(text, getComparator()));
    }

    /**
     * Returns information about component.
     */
    @Override
    public Hashtable<String, Object> getDump() {
        Hashtable<String, Object> result = super.getDump();
        if (((AbstractButton) getSource()).getText() != null) {
            result.put(TEXT_DPROP, ((AbstractButton) getSource()).getText());
        }
        result.put(IS_SELECTED_DPROP, ((AbstractButton) getSource()).isSelected() ? "true" : "false");
        return result;
    }

    ////////////////////////////////////////////////////////
    //Mapping                                             //
    /**
     * Maps {@code AbstractButton.addActionListener(ActionListener)}
     * through queue
     */
    public void addActionListener(final ActionListener actionListener) {
        runMapping(new MapVoidAction("addActionListener") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).addActionListener(actionListener);
            }
        });
    }

    /**
     * Maps {@code AbstractButton.addChangeListener(ChangeListener)}
     * through queue
     */
    public void addChangeListener(final ChangeListener changeListener) {
        runMapping(new MapVoidAction("addChangeListener") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).addChangeListener(changeListener);
            }
        });
    }

    /**
     * Maps {@code AbstractButton.addItemListener(ItemListener)} through queue
     */
    public void addItemListener(final ItemListener itemListener) {
        runMapping(new MapVoidAction("addItemListener") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).addItemListener(itemListener);
            }
        });
    }

    /**
     * Maps {@code AbstractButton.doClick()} through queue
     */
    public void doClick() {
        runMapping(new MapVoidAction("doClick") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).doClick();
            }
        });
    }

    /**
     * Maps {@code AbstractButton.doClick(int)} through queue
     */
    public void doClick(final int i) {
        runMapping(new MapVoidAction("doClick") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).doClick(i);
            }
        });
    }

    /**
     * Maps {@code AbstractButton.getActionCommand()} through queue
     */
    public String getActionCommand() {
        return (runMapping(new MapAction<String>("getActionCommand") {
            @Override
            public String map() {
                return ((AbstractButton) getSource()).getActionCommand();
            }
        }));
    }

    /**
     * Maps {@code AbstractButton.getDisabledIcon()} through queue
     */
    public Icon getDisabledIcon() {
        return (runMapping(new MapAction<Icon>("getDisabledIcon") {
            @Override
            public Icon map() {
                return ((AbstractButton) getSource()).getDisabledIcon();
            }
        }));
    }

    /**
     * Maps {@code AbstractButton.getDisabledSelectedIcon()} through queue
     */
    public Icon getDisabledSelectedIcon() {
        return (runMapping(new MapAction<Icon>("getDisabledSelectedIcon") {
            @Override
            public Icon map() {
                return ((AbstractButton) getSource()).getDisabledSelectedIcon();
            }
        }));
    }

    /**
     * Maps {@code AbstractButton.getHorizontalAlignment()} through queue
     */
    public int getHorizontalAlignment() {
        return (runMapping(new MapIntegerAction("getHorizontalAlignment") {
            @Override
            public int map() {
                return ((AbstractButton) getSource()).getHorizontalAlignment();
            }
        }));
    }

    /**
     * Maps {@code AbstractButton.getHorizontalTextPosition()} through queue
     */
    public int getHorizontalTextPosition() {
        return (runMapping(new MapIntegerAction("getHorizontalTextPosition") {
            @Override
            public int map() {
                return ((AbstractButton) getSource()).getHorizontalTextPosition();
            }
        }));
    }

    /**
     * Maps {@code AbstractButton.getIcon()} through queue
     */
    public Icon getIcon() {
        return (runMapping(new MapAction<Icon>("getIcon") {
            @Override
            public Icon map() {
                return ((AbstractButton) getSource()).getIcon();
            }
        }));
    }

    /**
     * Maps {@code AbstractButton.getMargin()} through queue
     */
    public Insets getMargin() {
        return (runMapping(new MapAction<Insets>("getMargin") {
            @Override
            public Insets map() {
                return ((AbstractButton) getSource()).getMargin();
            }
        }));
    }

    /**
     * Maps {@code AbstractButton.getMnemonic()} through queue
     */
    public int getMnemonic() {
        return (runMapping(new MapIntegerAction("getMnemonic") {
            @Override
            public int map() {
                return ((AbstractButton) getSource()).getMnemonic();
            }
        }));
    }

    /**
     * Maps {@code AbstractButton.getModel()} through queue
     */
    public ButtonModel getModel() {
        return (runMapping(new MapAction<ButtonModel>("getModel") {
            @Override
            public ButtonModel map() {
                return ((AbstractButton) getSource()).getModel();
            }
        }));
    }

    /**
     * Maps {@code AbstractButton.getPressedIcon()} through queue
     */
    public Icon getPressedIcon() {
        return (runMapping(new MapAction<Icon>("getPressedIcon") {
            @Override
            public Icon map() {
                return ((AbstractButton) getSource()).getPressedIcon();
            }
        }));
    }

    /**
     * Maps {@code AbstractButton.getRolloverIcon()} through queue
     */
    public Icon getRolloverIcon() {
        return (runMapping(new MapAction<Icon>("getRolloverIcon") {
            @Override
            public Icon map() {
                return ((AbstractButton) getSource()).getRolloverIcon();
            }
        }));
    }

    /**
     * Maps {@code AbstractButton.getRolloverSelectedIcon()} through queue
     */
    public Icon getRolloverSelectedIcon() {
        return (runMapping(new MapAction<Icon>("getRolloverSelectedIcon") {
            @Override
            public Icon map() {
                return ((AbstractButton) getSource()).getRolloverSelectedIcon();
            }
        }));
    }

    /**
     * Maps {@code AbstractButton.getSelectedIcon()} through queue
     */
    public Icon getSelectedIcon() {
        return (runMapping(new MapAction<Icon>("getSelectedIcon") {
            @Override
            public Icon map() {
                return ((AbstractButton) getSource()).getSelectedIcon();
            }
        }));
    }

    /**
     * Maps {@code AbstractButton.getSelectedObjects()} through queue
     */
    public Object[] getSelectedObjects() {
        return ((Object[]) runMapping(new MapAction<Object>("getSelectedObjects") {
            @Override
            public Object map() {
                return ((AbstractButton) getSource()).getSelectedObjects();
            }
        }));
    }

    /**
     * Maps {@code AbstractButton.getText()} through queue
     */
    public String getText() {
        return (runMapping(new MapAction<String>("getText") {
            @Override
            public String map() {
                return ((AbstractButton) getSource()).getText();
            }
        }));
    }

    /**
     * Maps {@code AbstractButton.getUI()} through queue
     */
    public ButtonUI getUI() {
        return (runMapping(new MapAction<ButtonUI>("getUI") {
            @Override
            public ButtonUI map() {
                return ((AbstractButton) getSource()).getUI();
            }
        }));
    }

    /**
     * Maps {@code AbstractButton.getVerticalAlignment()} through queue
     */
    public int getVerticalAlignment() {
        return (runMapping(new MapIntegerAction("getVerticalAlignment") {
            @Override
            public int map() {
                return ((AbstractButton) getSource()).getVerticalAlignment();
            }
        }));
    }

    /**
     * Maps {@code AbstractButton.getVerticalTextPosition()} through queue
     */
    public int getVerticalTextPosition() {
        return (runMapping(new MapIntegerAction("getVerticalTextPosition") {
            @Override
            public int map() {
                return ((AbstractButton) getSource()).getVerticalTextPosition();
            }
        }));
    }

    /**
     * Maps {@code AbstractButton.isBorderPainted()} through queue
     */
    public boolean isBorderPainted() {
        return (runMapping(new MapBooleanAction("isBorderPainted") {
            @Override
            public boolean map() {
                return ((AbstractButton) getSource()).isBorderPainted();
            }
        }));
    }

    /**
     * Maps {@code AbstractButton.isContentAreaFilled()} through queue
     */
    public boolean isContentAreaFilled() {
        return (runMapping(new MapBooleanAction("isContentAreaFilled") {
            @Override
            public boolean map() {
                return ((AbstractButton) getSource()).isContentAreaFilled();
            }
        }));
    }

    /**
     * Maps {@code AbstractButton.isFocusPainted()} through queue
     */
    public boolean isFocusPainted() {
        return (runMapping(new MapBooleanAction("isFocusPainted") {
            @Override
            public boolean map() {
                return ((AbstractButton) getSource()).isFocusPainted();
            }
        }));
    }

    /**
     * Maps {@code AbstractButton.isRolloverEnabled()} through queue
     */
    public boolean isRolloverEnabled() {
        return (runMapping(new MapBooleanAction("isRolloverEnabled") {
            @Override
            public boolean map() {
                return ((AbstractButton) getSource()).isRolloverEnabled();
            }
        }));
    }

    /**
     * Maps {@code AbstractButton.isSelected()} through queue
     */
    public boolean isSelected() {
        return (runMapping(new MapBooleanAction("isSelected") {
            @Override
            public boolean map() {
                return ((AbstractButton) getSource()).isSelected();
            }
        }));
    }

    /**
     * Maps {@code AbstractButton.removeActionListener(ActionListener)}
     * through queue
     */
    public void removeActionListener(final ActionListener actionListener) {
        runMapping(new MapVoidAction("removeActionListener") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).removeActionListener(actionListener);
            }
        });
    }

    /**
     * Maps {@code AbstractButton.removeChangeListener(ChangeListener)}
     * through queue
     */
    public void removeChangeListener(final ChangeListener changeListener) {
        runMapping(new MapVoidAction("removeChangeListener") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).removeChangeListener(changeListener);
            }
        });
    }

    /**
     * Maps {@code AbstractButton.removeItemListener(ItemListener)} through queue
     */
    public void removeItemListener(final ItemListener itemListener) {
        runMapping(new MapVoidAction("removeItemListener") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).removeItemListener(itemListener);
            }
        });
    }

    /**
     * Maps {@code AbstractButton.setActionCommand(String)} through queue
     */
    public void setActionCommand(final String string) {
        runMapping(new MapVoidAction("setActionCommand") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).setActionCommand(string);
            }
        });
    }

    /**
     * Maps {@code AbstractButton.setBorderPainted(boolean)} through queue
     */
    public void setBorderPainted(final boolean b) {
        runMapping(new MapVoidAction("setBorderPainted") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).setBorderPainted(b);
            }
        });
    }

    /**
     * Maps {@code AbstractButton.setContentAreaFilled(boolean)} through queue
     */
    public void setContentAreaFilled(final boolean b) {
        runMapping(new MapVoidAction("setContentAreaFilled") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).setContentAreaFilled(b);
            }
        });
    }

    /**
     * Maps {@code AbstractButton.setDisabledIcon(Icon)} through queue
     */
    public void setDisabledIcon(final Icon icon) {
        runMapping(new MapVoidAction("setDisabledIcon") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).setDisabledIcon(icon);
            }
        });
    }

    /**
     * Maps {@code AbstractButton.setDisabledSelectedIcon(Icon)} through queue
     */
    public void setDisabledSelectedIcon(final Icon icon) {
        runMapping(new MapVoidAction("setDisabledSelectedIcon") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).setDisabledSelectedIcon(icon);
            }
        });
    }

    /**
     * Maps {@code AbstractButton.setFocusPainted(boolean)} through queue
     */
    public void setFocusPainted(final boolean b) {
        runMapping(new MapVoidAction("setFocusPainted") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).setFocusPainted(b);
            }
        });
    }

    /**
     * Maps {@code AbstractButton.setHorizontalAlignment(int)} through queue
     */
    public void setHorizontalAlignment(final int i) {
        runMapping(new MapVoidAction("setHorizontalAlignment") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).setHorizontalAlignment(i);
            }
        });
    }

    /**
     * Maps {@code AbstractButton.setHorizontalTextPosition(int)} through queue
     */
    public void setHorizontalTextPosition(final int i) {
        runMapping(new MapVoidAction("setHorizontalTextPosition") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).setHorizontalTextPosition(i);
            }
        });
    }

    /**
     * Maps {@code AbstractButton.setIcon(Icon)} through queue
     */
    public void setIcon(final Icon icon) {
        runMapping(new MapVoidAction("setIcon") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).setIcon(icon);
            }
        });
    }

    /**
     * Maps {@code AbstractButton.setMargin(Insets)} through queue
     */
    public void setMargin(final Insets insets) {
        runMapping(new MapVoidAction("setMargin") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).setMargin(insets);
            }
        });
    }

    /**
     * Maps {@code AbstractButton.setMnemonic(char)} through queue
     */
    public void setMnemonic(final char c) {
        runMapping(new MapVoidAction("setMnemonic") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).setMnemonic(c);
            }
        });
    }

    /**
     * Maps {@code AbstractButton.setMnemonic(int)} through queue
     */
    public void setMnemonic(final int i) {
        runMapping(new MapVoidAction("setMnemonic") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).setMnemonic(i);
            }
        });
    }

    /**
     * Maps {@code AbstractButton.setModel(ButtonModel)} through queue
     */
    public void setModel(final ButtonModel buttonModel) {
        runMapping(new MapVoidAction("setModel") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).setModel(buttonModel);
            }
        });
    }

    /**
     * Maps {@code AbstractButton.setPressedIcon(Icon)} through queue
     */
    public void setPressedIcon(final Icon icon) {
        runMapping(new MapVoidAction("setPressedIcon") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).setPressedIcon(icon);
            }
        });
    }

    /**
     * Maps {@code AbstractButton.setRolloverEnabled(boolean)} through queue
     */
    public void setRolloverEnabled(final boolean b) {
        runMapping(new MapVoidAction("setRolloverEnabled") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).setRolloverEnabled(b);
            }
        });
    }

    /**
     * Maps {@code AbstractButton.setRolloverIcon(Icon)} through queue
     */
    public void setRolloverIcon(final Icon icon) {
        runMapping(new MapVoidAction("setRolloverIcon") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).setRolloverIcon(icon);
            }
        });
    }

    /**
     * Maps {@code AbstractButton.setRolloverSelectedIcon(Icon)} through queue
     */
    public void setRolloverSelectedIcon(final Icon icon) {
        runMapping(new MapVoidAction("setRolloverSelectedIcon") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).setRolloverSelectedIcon(icon);
            }
        });
    }

    /**
     * Maps {@code AbstractButton.setSelected(boolean)} through queue
     */
    public void setSelected(final boolean b) {
        runMapping(new MapVoidAction("setSelected") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).setSelected(b);
            }
        });
    }

    /**
     * Maps {@code AbstractButton.setSelectedIcon(Icon)} through queue
     */
    public void setSelectedIcon(final Icon icon) {
        runMapping(new MapVoidAction("setSelectedIcon") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).setSelectedIcon(icon);
            }
        });
    }

    /**
     * Maps {@code AbstractButton.setText(String)} through queue
     */
    public void setText(final String string) {
        runMapping(new MapVoidAction("setText") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).setText(string);
            }
        });
    }

    /**
     * Maps {@code AbstractButton.setUI(ButtonUI)} through queue
     */
    public void setUI(final ButtonUI buttonUI) {
        runMapping(new MapVoidAction("setUI") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).setUI(buttonUI);
            }
        });
    }

    /**
     * Maps {@code AbstractButton.setVerticalAlignment(int)} through queue
     */
    public void setVerticalAlignment(final int i) {
        runMapping(new MapVoidAction("setVerticalAlignment") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).setVerticalAlignment(i);
            }
        });
    }

    /**
     * Maps {@code AbstractButton.setVerticalTextPosition(int)} through queue
     */
    public void setVerticalTextPosition(final int i) {
        runMapping(new MapVoidAction("setVerticalTextPosition") {
            @Override
            public void map() {
                ((AbstractButton) getSource()).setVerticalTextPosition(i);
            }
        });
    }

    //End of mapping                                      //
    ////////////////////////////////////////////////////////
    /**
     * Allows to find component by text.
     */
    public static class AbstractButtonByLabelFinder implements ComponentChooser {

        String label;
        StringComparator comparator;

        /**
         * Constructs AbstractButtonByLabelFinder.
         *
         * @param lb a text pattern
         * @param comparator specifies string comparision algorithm.
         */
        public AbstractButtonByLabelFinder(String lb, StringComparator comparator) {
            label = lb;
            this.comparator = comparator;
        }

        /**
         * Constructs AbstractButtonByLabelFinder.
         *
         * @param lb a text pattern
         */
        public AbstractButtonByLabelFinder(String lb) {
            this(lb, Operator.getDefaultStringComparator());
        }

        @Override
        public boolean checkComponent(Component comp) {
            if (comp instanceof AbstractButton) {
                if (((AbstractButton) comp).getText() != null) {
                    return (comparator.equals(((AbstractButton) comp).getText(),
                            label));
                }
            }
            return false;
        }

        @Override
        public String getDescription() {
            return "AbstractButton with text \"" + label + "\"";
        }

        @Override
        public String toString() {
            return "AbstractButtonByLabelFinder{" + "label=" + label + ", comparator=" + comparator + '}';
        }
    }

    /**
     * Checks component type.
     */
    public static class AbstractButtonFinder extends Finder {

        /**
         * Constructs AbstractButtonFinder.
         *
         * @param sf other searching criteria.
         */
        public AbstractButtonFinder(ComponentChooser sf) {
            super(AbstractButton.class, sf);
        }

        /**
         * Constructs AbstractButtonFinder.
         */
        public AbstractButtonFinder() {
            super(AbstractButton.class);
        }
    }
}
