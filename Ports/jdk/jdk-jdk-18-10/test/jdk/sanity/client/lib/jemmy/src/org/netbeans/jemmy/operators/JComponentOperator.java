/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.beans.VetoableChangeListener;
import java.util.Hashtable;

import javax.accessibility.AccessibleContext;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JRootPane;
import javax.swing.JToolTip;
import javax.swing.KeyStroke;
import javax.swing.border.Border;
import javax.swing.event.AncestorListener;

import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.ComponentSearcher;
import org.netbeans.jemmy.Outputable;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.TimeoutExpiredException;
import org.netbeans.jemmy.Timeoutable;
import org.netbeans.jemmy.Timeouts;

/**
 * <BR><BR>Timeouts used: <BR>
 * JComponentOperator.WaitToolTipTimeout - time to wait tool tip displayed <BR>
 * JComponentOperator.ShowToolTipTimeout - time to show tool tip <BR>
 * ComponentOperator.WaitComponentTimeout - time to wait component displayed
 * <BR>.
 *
 * @see org.netbeans.jemmy.Timeouts
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 *
 */
public class JComponentOperator extends ContainerOperator<Container>
        implements Timeoutable, Outputable {

    /**
     * Identifier for a "tooltip text" property.
     *
     * @see #getDump
     */
    public static final String TOOLTIP_TEXT_DPROP = "Tooltip text";
    public static final String A11Y_DATA = "Accessible data (yes/no)";
    public static final String A11Y_NAME_DPROP = "Accessible name";
    public static final String A11Y_DESCRIPTION_DPROP = "Accessible decription";

    private final static long WAIT_TOOL_TIP_TIMEOUT = 10000;
    private final static long SHOW_TOOL_TIP_TIMEOUT = 0;

    private Timeouts timeouts;
    private TestOut output;

    /**
     * Constructor.
     *
     * @param b a component
     */
    public JComponentOperator(JComponent b) {
        super(b);
    }

    /**
     * Constructs a JComponentOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     */
    public JComponentOperator(ContainerOperator<?> cont, ComponentChooser chooser, int index) {
        this((JComponent) cont.
                waitSubComponent(new JComponentFinder(chooser),
                        index));
        copyEnvironment(cont);
    }

    /**
     * Constructs a JComponentOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     */
    public JComponentOperator(ContainerOperator<?> cont, ComponentChooser chooser) {
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
    public JComponentOperator(ContainerOperator<?> cont, int index) {
        this((JComponent) waitComponent(cont,
                new JComponentFinder(ComponentSearcher.getTrueChooser("Any JComponent")),
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
    public JComponentOperator(ContainerOperator<?> cont) {
        this(cont, 0);
    }

    /**
     * Searches JComponent in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @return JComponent instance or null if component was not found.
     */
    public static JComponent findJComponent(Container cont, ComponentChooser chooser, int index) {
        return (JComponent) findComponent(cont, new JComponentFinder(chooser), index);
    }

    /**
     * Searches 0'th JComponent in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return JComponent instance or null if component was not found.
     */
    public static JComponent findJComponent(Container cont, ComponentChooser chooser) {
        return findJComponent(cont, chooser, 0);
    }

    /**
     * Searches JComponent by tooltip text.
     *
     * @param cont Container to search component in.
     * @param toolTipText Tooltip text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param index Ordinal component index.
     * @return JComponent instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static JComponent findJComponent(Container cont, String toolTipText, boolean ce, boolean ccs, int index) {
        return findJComponent(cont, new JComponentByTipFinder(toolTipText, new DefaultStringComparator(ce, ccs)), index);
    }

    /**
     * Searches JComponent by tooltip text.
     *
     * @param cont Container to search component in.
     * @param toolTipText Tooltip text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @return JComponent instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static JComponent findJComponent(Container cont, String toolTipText, boolean ce, boolean ccs) {
        return findJComponent(cont, toolTipText, ce, ccs, 0);
    }

    /**
     * Waits JComponent in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @return JComponent instance or null if component was not found.
     * @throws TimeoutExpiredException
     */
    public static JComponent waitJComponent(Container cont, ComponentChooser chooser, final int index) {
        return (JComponent) waitComponent(cont, new JComponentFinder(chooser), index);
    }

    /**
     * Waits 0'th JComponent in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return JComponent instance or null if component was not found.
     * @throws TimeoutExpiredException
     */
    public static JComponent waitJComponent(Container cont, ComponentChooser chooser) {
        return waitJComponent(cont, chooser, 0);
    }

    /**
     * Waits JComponent by tooltip text.
     *
     * @param cont Container to search component in.
     * @param toolTipText Tooltip text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param index Ordinal component index.
     * @return JComponent instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public static JComponent waitJComponent(Container cont, String toolTipText, boolean ce, boolean ccs, int index) {
        return waitJComponent(cont, new JComponentByTipFinder(toolTipText, new DefaultStringComparator(ce, ccs)), index);
    }

    /**
     * Waits JComponent by tooltip text.
     *
     * @param cont Container to search component in.
     * @param toolTipText Tooltip text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @return JComponent instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public static JComponent waitJComponent(Container cont, String toolTipText, boolean ce, boolean ccs) {
        return waitJComponent(cont, toolTipText, ce, ccs, 0);
    }

    static {
        Timeouts.initDefault("JComponentOperator.WaitToolTipTimeout", WAIT_TOOL_TIP_TIMEOUT);
        Timeouts.initDefault("JComponentOperator.ShowToolTipTimeout", SHOW_TOOL_TIP_TIMEOUT);
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
    public int getCenterXForClick() {
        Rectangle rect = getVisibleRect();
        return ((int) rect.getX()
                + (int) rect.getWidth() / 2);
    }

    @Override
    public int getCenterYForClick() {
        Rectangle rect = getVisibleRect();
        return ((int) rect.getY()
                + (int) rect.getHeight() / 2);
    }

    /**
     * Showes tool tip.
     *
     * @return JToolTip component.
     * @throws TimeoutExpiredException
     */
    public JToolTip showToolTip() {
        enterMouse();
        moveMouse(getCenterXForClick(),
                getCenterYForClick());
        return waitToolTip();
    }

    public JToolTip waitToolTip() {
        return JToolTipOperator.waitJToolTip(this);
    }

    /**
     * Looks for a first window-like container.
     *
     * @return either WindowOperator of JInternalFrameOperator
     */
    public ContainerOperator<?> getWindowContainerOperator() {
        Component resultComp;
        if (getSource() instanceof Window) {
            resultComp = getSource();
        } else {
            resultComp = getContainer(new ComponentChooser() {
                @Override
                public boolean checkComponent(Component comp) {
                    return (comp instanceof Window
                            || comp instanceof JInternalFrame);
                }

                @Override
                public String getDescription() {
                    return "";
                }
            });
        }
        ContainerOperator<?> result;
        if (resultComp instanceof Window) {
            result = new WindowOperator((Window) resultComp);
        } else {
            result = new ContainerOperator<>((Container) resultComp);
        }
        result.copyEnvironment(this);
        return result;
    }

    @Override
    public Hashtable<String, Object> getDump() {
        Hashtable<String, Object> result = super.getDump();
        if (getToolTipText() != null) {
            result.put(TOOLTIP_TEXT_DPROP, getToolTipText());
        }
        //System.out.println("Dump a11y = " + System.getProperty("jemmy.dump.a11y"));
        if (System.getProperty("jemmy.dump.a11y") != null
                && System.getProperty("jemmy.dump.a11y").equals("on")) {
            AccessibleContext a11y = getSource().getAccessibleContext();
            if (a11y != null) {
                result.put(A11Y_DATA, "yes");
                String accName = (a11y.getAccessibleName() == null) ? "null" : a11y.getAccessibleName();
                String accDesc = (a11y.getAccessibleDescription() == null) ? "null" : a11y.getAccessibleDescription();
                result.put(A11Y_NAME_DPROP, accName);
                result.put(A11Y_DESCRIPTION_DPROP, accDesc);
            } else {
                result.put(A11Y_DATA, "no");
            }
        }
        return result;
    }

    ////////////////////////////////////////////////////////
    //Mapping                                             //
    /**
     * Maps {@code JComponent.addAncestorListener(AncestorListener)}
     * through queue
     */
    public void addAncestorListener(final AncestorListener ancestorListener) {
        runMapping(new MapVoidAction("addAncestorListener") {
            @Override
            public void map() {
                ((JComponent) getSource()).addAncestorListener(ancestorListener);
            }
        });
    }

    /**
     * Maps
     * {@code JComponent.addVetoableChangeListener(VetoableChangeListener)}
     * through queue
     */
    public void addVetoableChangeListener(final VetoableChangeListener vetoableChangeListener) {
        runMapping(new MapVoidAction("addVetoableChangeListener") {
            @Override
            public void map() {
                ((JComponent) getSource()).addVetoableChangeListener(vetoableChangeListener);
            }
        });
    }

    /**
     * Maps {@code JComponent.computeVisibleRect(Rectangle)} through queue
     */
    public void computeVisibleRect(final Rectangle rectangle) {
        runMapping(new MapVoidAction("computeVisibleRect") {
            @Override
            public void map() {
                ((JComponent) getSource()).computeVisibleRect(rectangle);
            }
        });
    }

    /**
     * Maps {@code JComponent.createToolTip()} through queue
     */
    public JToolTip createToolTip() {
        return (runMapping(new MapAction<JToolTip>("createToolTip") {
            @Override
            public JToolTip map() {
                return ((JComponent) getSource()).createToolTip();
            }
        }));
    }

    /**
     * Maps {@code JComponent.firePropertyChange(String, byte, byte)}
     * through queue
     */
    public void firePropertyChange(final String string, final byte b, final byte b1) {
        runMapping(new MapVoidAction("firePropertyChange") {
            @Override
            public void map() {
                getSource().firePropertyChange(string, b, b1);
            }
        });
    }

    /**
     * Maps {@code JComponent.firePropertyChange(String, char, char)}
     * through queue
     */
    public void firePropertyChange(final String string, final char c, final char c1) {
        runMapping(new MapVoidAction("firePropertyChange") {
            @Override
            public void map() {
                getSource().firePropertyChange(string, c, c1);
            }
        });
    }

    /**
     * Maps {@code JComponent.firePropertyChange(String, double, double)}
     * through queue
     */
    public void firePropertyChange(final String string, final double d, final double d1) {
        runMapping(new MapVoidAction("firePropertyChange") {
            @Override
            public void map() {
                getSource().firePropertyChange(string, d, d1);
            }
        });
    }

    /**
     * Maps {@code JComponent.firePropertyChange(String, float, float)}
     * through queue
     */
    public void firePropertyChange(final String string, final float f, final float f1) {
        runMapping(new MapVoidAction("firePropertyChange") {
            @Override
            public void map() {
                getSource().firePropertyChange(string, f, f1);
            }
        });
    }

    /**
     * Maps {@code JComponent.firePropertyChange(String, int, int)} through queue
     */
    public void firePropertyChange(final String string, final int i, final int i1) {
        runMapping(new MapVoidAction("firePropertyChange") {
            @Override
            public void map() {
                ((JComponent) getSource()).firePropertyChange(string, i, i1);
            }
        });
    }

    /**
     * Maps {@code JComponent.firePropertyChange(String, long, long)}
     * through queue
     */
    public void firePropertyChange(final String string, final long l, final long l1) {
        runMapping(new MapVoidAction("firePropertyChange") {
            @Override
            public void map() {
                getSource().firePropertyChange(string, l, l1);
            }
        });
    }

    /**
     * Maps {@code JComponent.firePropertyChange(String, short, short)}
     * through queue
     */
    public void firePropertyChange(final String string, final short s, final short s1) {
        runMapping(new MapVoidAction("firePropertyChange") {
            @Override
            public void map() {
                getSource().firePropertyChange(string, s, s1);
            }
        });
    }

    /**
     * Maps {@code JComponent.firePropertyChange(String, boolean, boolean)}
     * through queue
     */
    public void firePropertyChange(final String string, final boolean b, final boolean b1) {
        runMapping(new MapVoidAction("firePropertyChange") {
            @Override
            public void map() {
                ((JComponent) getSource()).firePropertyChange(string, b, b1);
            }
        });
    }

    /**
     * Maps {@code JComponent.getAccessibleContext()} through queue
     */
    public AccessibleContext getAccessibleContext() {
        return (runMapping(new MapAction<AccessibleContext>("getAccessibleContext") {
            @Override
            public AccessibleContext map() {
                return getSource().getAccessibleContext();
            }
        }));
    }

    /**
     * Maps {@code JComponent.getActionForKeyStroke(KeyStroke)} through queue
     */
    public ActionListener getActionForKeyStroke(final KeyStroke keyStroke) {
        return (runMapping(new MapAction<ActionListener>("getActionForKeyStroke") {
            @Override
            public ActionListener map() {
                return ((JComponent) getSource()).getActionForKeyStroke(keyStroke);
            }
        }));
    }

    /**
     * Maps {@code JComponent.getAutoscrolls()} through queue
     */
    public boolean getAutoscrolls() {
        return (runMapping(new MapBooleanAction("getAutoscrolls") {
            @Override
            public boolean map() {
                return ((JComponent) getSource()).getAutoscrolls();
            }
        }));
    }

    /**
     * Maps {@code JComponent.getBorder()} through queue
     */
    public Border getBorder() {
        return (runMapping(new MapAction<Border>("getBorder") {
            @Override
            public Border map() {
                return ((JComponent) getSource()).getBorder();
            }
        }));
    }

    /**
     * Maps {@code JComponent.getClientProperty(Object)} through queue
     */
    public Object getClientProperty(final Object object) {
        return (runMapping(new MapAction<Object>("getClientProperty") {
            @Override
            public Object map() {
                return ((JComponent) getSource()).getClientProperty(object);
            }
        }));
    }

    /**
     * Maps {@code JComponent.getConditionForKeyStroke(KeyStroke)} through queue
     */
    public int getConditionForKeyStroke(final KeyStroke keyStroke) {
        return (runMapping(new MapIntegerAction("getConditionForKeyStroke") {
            @Override
            public int map() {
                return ((JComponent) getSource()).getConditionForKeyStroke(keyStroke);
            }
        }));
    }

    /**
     * Maps {@code JComponent.getDebugGraphicsOptions()} through queue
     */
    public int getDebugGraphicsOptions() {
        return (runMapping(new MapIntegerAction("getDebugGraphicsOptions") {
            @Override
            public int map() {
                return ((JComponent) getSource()).getDebugGraphicsOptions();
            }
        }));
    }

    /**
     * Maps {@code JComponent.getInsets(Insets)} through queue
     */
    public Insets getInsets(final Insets insets) {
        return (runMapping(new MapAction<Insets>("getInsets") {
            @Override
            public Insets map() {
                return ((JComponent) getSource()).getInsets(insets);
            }
        }));
    }

    /**
     * Maps {@code JComponent.getNextFocusableComponent()} through queue
     */
    @Deprecated
    public Component getNextFocusableComponent() {
        return (runMapping(new MapAction<Component>("getNextFocusableComponent") {
            @Override
            public Component map() {
                return ((JComponent) getSource()).getNextFocusableComponent();
            }
        }));
    }

    /**
     * Maps {@code JComponent.getRegisteredKeyStrokes()} through queue
     */
    public KeyStroke[] getRegisteredKeyStrokes() {
        return ((KeyStroke[]) runMapping(new MapAction<Object>("getRegisteredKeyStrokes") {
            @Override
            public Object map() {
                return ((JComponent) getSource()).getRegisteredKeyStrokes();
            }
        }));
    }

    /**
     * Maps {@code JComponent.getRootPane()} through queue
     */
    public JRootPane getRootPane() {
        return (runMapping(new MapAction<JRootPane>("getRootPane") {
            @Override
            public JRootPane map() {
                return ((JComponent) getSource()).getRootPane();
            }
        }));
    }

    /**
     * Maps {@code JComponent.getToolTipLocation(MouseEvent)} through queue
     */
    public Point getToolTipLocation(final MouseEvent mouseEvent) {
        return (runMapping(new MapAction<Point>("getToolTipLocation") {
            @Override
            public Point map() {
                return ((JComponent) getSource()).getToolTipLocation(mouseEvent);
            }
        }));
    }

    /**
     * Maps {@code JComponent.getToolTipText()} through queue
     */
    public String getToolTipText() {
        return (runMapping(new MapAction<String>("getToolTipText") {
            @Override
            public String map() {
                return ((JComponent) getSource()).getToolTipText();
            }
        }));
    }

    /**
     * Maps {@code JComponent.getToolTipText(MouseEvent)} through queue
     */
    public String getToolTipText(final MouseEvent mouseEvent) {
        return (runMapping(new MapAction<String>("getToolTipText") {
            @Override
            public String map() {
                return ((JComponent) getSource()).getToolTipText(mouseEvent);
            }
        }));
    }

    /**
     * Maps {@code JComponent.getTopLevelAncestor()} through queue
     */
    public Container getTopLevelAncestor() {
        return (runMapping(new MapAction<Container>("getTopLevelAncestor") {
            @Override
            public Container map() {
                return ((JComponent) getSource()).getTopLevelAncestor();
            }
        }));
    }

    /**
     * Maps {@code JComponent.getUIClassID()} through queue
     */
    public String getUIClassID() {
        return (runMapping(new MapAction<String>("getUIClassID") {
            @Override
            public String map() {
                return ((JComponent) getSource()).getUIClassID();
            }
        }));
    }

    /**
     * Maps {@code JComponent.getVisibleRect()} through queue
     */
    public Rectangle getVisibleRect() {
        return (runMapping(new MapAction<Rectangle>("getVisibleRect") {
            @Override
            public Rectangle map() {
                return ((JComponent) getSource()).getVisibleRect();
            }
        }));
    }

    /**
     * Maps {@code JComponent.grabFocus()} through queue
     */
    public void grabFocus() {
        runMapping(new MapVoidAction("grabFocus") {
            @Override
            public void map() {
                ((JComponent) getSource()).grabFocus();
            }
        });
    }

    /**
     * Maps {@code JComponent.isFocusCycleRoot()} through queue
     */
    public boolean isFocusCycleRoot() {
        return (runMapping(new MapBooleanAction("isFocusCycleRoot") {
            @Override
            public boolean map() {
                return ((JComponent) getSource()).isFocusCycleRoot();
            }
        }));
    }

    /**
     * Maps {@code JComponent.isManagingFocus()} through queue
     */
    @Deprecated
    public boolean isManagingFocus() {
        return (runMapping(new MapBooleanAction("isManagingFocus") {
            @Override
            public boolean map() {
                return ((JComponent) getSource()).isManagingFocus();
            }
        }));
    }

    /**
     * Maps {@code JComponent.isOptimizedDrawingEnabled()} through queue
     */
    public boolean isOptimizedDrawingEnabled() {
        return (runMapping(new MapBooleanAction("isOptimizedDrawingEnabled") {
            @Override
            public boolean map() {
                return ((JComponent) getSource()).isOptimizedDrawingEnabled();
            }
        }));
    }

    /**
     * Maps {@code JComponent.isPaintingTile()} through queue
     */
    public boolean isPaintingTile() {
        return (runMapping(new MapBooleanAction("isPaintingTile") {
            @Override
            public boolean map() {
                return ((JComponent) getSource()).isPaintingTile();
            }
        }));
    }

    /**
     * Maps {@code JComponent.isRequestFocusEnabled()} through queue
     */
    public boolean isRequestFocusEnabled() {
        return (runMapping(new MapBooleanAction("isRequestFocusEnabled") {
            @Override
            public boolean map() {
                return ((JComponent) getSource()).isRequestFocusEnabled();
            }
        }));
    }

    /**
     * Maps {@code JComponent.isValidateRoot()} through queue
     */
    public boolean isValidateRoot() {
        return (runMapping(new MapBooleanAction("isValidateRoot") {
            @Override
            public boolean map() {
                return ((JComponent) getSource()).isValidateRoot();
            }
        }));
    }

    /**
     * Maps {@code JComponent.paintImmediately(int, int, int, int)} through queue
     */
    public void paintImmediately(final int i, final int i1, final int i2, final int i3) {
        runMapping(new MapVoidAction("paintImmediately") {
            @Override
            public void map() {
                ((JComponent) getSource()).paintImmediately(i, i1, i2, i3);
            }
        });
    }

    /**
     * Maps {@code JComponent.paintImmediately(Rectangle)} through queue
     */
    public void paintImmediately(final Rectangle rectangle) {
        runMapping(new MapVoidAction("paintImmediately") {
            @Override
            public void map() {
                ((JComponent) getSource()).paintImmediately(rectangle);
            }
        });
    }

    /**
     * Maps {@code JComponent.putClientProperty(Object, Object)} through queue
     */
    public void putClientProperty(final Object object, final Object object1) {
        runMapping(new MapVoidAction("putClientProperty") {
            @Override
            public void map() {
                ((JComponent) getSource()).putClientProperty(object, object1);
            }
        });
    }

    /**
     * Maps
     * {@code JComponent.registerKeyboardAction(ActionListener, String, KeyStroke, int)}
     * through queue
     */
    public void registerKeyboardAction(final ActionListener actionListener, final String string, final KeyStroke keyStroke, final int i) {
        runMapping(new MapVoidAction("registerKeyboardAction") {
            @Override
            public void map() {
                ((JComponent) getSource()).registerKeyboardAction(actionListener, string, keyStroke, i);
            }
        });
    }

    /**
     * Maps
     * {@code JComponent.registerKeyboardAction(ActionListener, KeyStroke, int)}
     * through queue
     */
    public void registerKeyboardAction(final ActionListener actionListener, final KeyStroke keyStroke, final int i) {
        runMapping(new MapVoidAction("registerKeyboardAction") {
            @Override
            public void map() {
                ((JComponent) getSource()).registerKeyboardAction(actionListener, keyStroke, i);
            }
        });
    }

    /**
     * Maps {@code JComponent.removeAncestorListener(AncestorListener)}
     * through queue
     */
    public void removeAncestorListener(final AncestorListener ancestorListener) {
        runMapping(new MapVoidAction("removeAncestorListener") {
            @Override
            public void map() {
                ((JComponent) getSource()).removeAncestorListener(ancestorListener);
            }
        });
    }

    /**
     * Maps
     * {@code JComponent.removeVetoableChangeListener(VetoableChangeListener)}
     * through queue
     */
    public void removeVetoableChangeListener(final VetoableChangeListener vetoableChangeListener) {
        runMapping(new MapVoidAction("removeVetoableChangeListener") {
            @Override
            public void map() {
                ((JComponent) getSource()).removeVetoableChangeListener(vetoableChangeListener);
            }
        });
    }

    /**
     * Maps {@code JComponent.repaint(Rectangle)} through queue
     */
    public void repaint(final Rectangle rectangle) {
        runMapping(new MapVoidAction("repaint") {
            @Override
            public void map() {
                ((JComponent) getSource()).repaint(rectangle);
            }
        });
    }

    /**
     * Maps {@code JComponent.requestDefaultFocus()} through queue
     */
    @Deprecated
    public boolean requestDefaultFocus() {
        return (runMapping(new MapBooleanAction("requestDefaultFocus") {
            @Override
            public boolean map() {
                return ((JComponent) getSource()).requestDefaultFocus();
            }
        }));
    }

    /**
     * Maps {@code JComponent.resetKeyboardActions()} through queue
     */
    public void resetKeyboardActions() {
        runMapping(new MapVoidAction("resetKeyboardActions") {
            @Override
            public void map() {
                ((JComponent) getSource()).resetKeyboardActions();
            }
        });
    }

    /**
     * Maps {@code JComponent.revalidate()} through queue
     */
    public void revalidate() {
        runMapping(new MapVoidAction("revalidate") {
            @Override
            public void map() {
                getSource().revalidate();
            }
        });
    }

    /**
     * Maps {@code JComponent.scrollRectToVisible(Rectangle)} through queue
     */
    public void scrollRectToVisible(final Rectangle rectangle) {
        runMapping(new MapVoidAction("scrollRectToVisible") {
            @Override
            public void map() {
                ((JComponent) getSource()).scrollRectToVisible(rectangle);
            }
        });
    }

    /**
     * Maps {@code JComponent.setAlignmentX(float)} through queue
     */
    public void setAlignmentX(final float f) {
        runMapping(new MapVoidAction("setAlignmentX") {
            @Override
            public void map() {
                ((JComponent) getSource()).setAlignmentX(f);
            }
        });
    }

    /**
     * Maps {@code JComponent.setAlignmentY(float)} through queue
     */
    public void setAlignmentY(final float f) {
        runMapping(new MapVoidAction("setAlignmentY") {
            @Override
            public void map() {
                ((JComponent) getSource()).setAlignmentY(f);
            }
        });
    }

    /**
     * Maps {@code JComponent.setAutoscrolls(boolean)} through queue
     */
    public void setAutoscrolls(final boolean b) {
        runMapping(new MapVoidAction("setAutoscrolls") {
            @Override
            public void map() {
                ((JComponent) getSource()).setAutoscrolls(b);
            }
        });
    }

    /**
     * Maps {@code JComponent.setBorder(Border)} through queue
     */
    public void setBorder(final Border border) {
        runMapping(new MapVoidAction("setBorder") {
            @Override
            public void map() {
                ((JComponent) getSource()).setBorder(border);
            }
        });
    }

    /**
     * Maps {@code JComponent.setDebugGraphicsOptions(int)} through queue
     */
    public void setDebugGraphicsOptions(final int i) {
        runMapping(new MapVoidAction("setDebugGraphicsOptions") {
            @Override
            public void map() {
                ((JComponent) getSource()).setDebugGraphicsOptions(i);
            }
        });
    }

    /**
     * Maps {@code JComponent.setDoubleBuffered(boolean)} through queue
     */
    public void setDoubleBuffered(final boolean b) {
        runMapping(new MapVoidAction("setDoubleBuffered") {
            @Override
            public void map() {
                ((JComponent) getSource()).setDoubleBuffered(b);
            }
        });
    }

    /**
     * Maps {@code JComponent.setMaximumSize(Dimension)} through queue
     */
    public void setMaximumSize(final Dimension dimension) {
        runMapping(new MapVoidAction("setMaximumSize") {
            @Override
            public void map() {
                getSource().setMaximumSize(dimension);
            }
        });
    }

    /**
     * Maps {@code JComponent.setMinimumSize(Dimension)} through queue
     */
    public void setMinimumSize(final Dimension dimension) {
        runMapping(new MapVoidAction("setMinimumSize") {
            @Override
            public void map() {
                getSource().setMinimumSize(dimension);
            }
        });
    }

    /**
     * Maps {@code JComponent.setNextFocusableComponent(Component)} through queue
     */
    @Deprecated
    public void setNextFocusableComponent(final Component component) {
        runMapping(new MapVoidAction("setNextFocusableComponent") {
            @Override
            public void map() {
                ((JComponent) getSource()).setNextFocusableComponent(component);
            }
        });
    }

    /**
     * Maps {@code JComponent.setOpaque(boolean)} through queue
     */
    public void setOpaque(final boolean b) {
        runMapping(new MapVoidAction("setOpaque") {
            @Override
            public void map() {
                ((JComponent) getSource()).setOpaque(b);
            }
        });
    }

    /**
     * Maps {@code JComponent.setPreferredSize(Dimension)} through queue
     */
    public void setPreferredSize(final Dimension dimension) {
        runMapping(new MapVoidAction("setPreferredSize") {
            @Override
            public void map() {
                getSource().setPreferredSize(dimension);
            }
        });
    }

    /**
     * Maps {@code JComponent.setRequestFocusEnabled(boolean)} through queue
     */
    public void setRequestFocusEnabled(final boolean b) {
        runMapping(new MapVoidAction("setRequestFocusEnabled") {
            @Override
            public void map() {
                ((JComponent) getSource()).setRequestFocusEnabled(b);
            }
        });
    }

    /**
     * Maps {@code JComponent.setToolTipText(String)} through queue
     */
    public void setToolTipText(final String string) {
        runMapping(new MapVoidAction("setToolTipText") {
            @Override
            public void map() {
                ((JComponent) getSource()).setToolTipText(string);
            }
        });
    }

    /**
     * Maps {@code JComponent.unregisterKeyboardAction(KeyStroke)} through queue
     */
    public void unregisterKeyboardAction(final KeyStroke keyStroke) {
        runMapping(new MapVoidAction("unregisterKeyboardAction") {
            @Override
            public void map() {
                ((JComponent) getSource()).unregisterKeyboardAction(keyStroke);
            }
        });
    }

    /**
     * Maps {@code JComponent.updateUI()} through queue
     */
    public void updateUI() {
        runMapping(new MapVoidAction("updateUI") {
            @Override
            public void map() {
                ((JComponent) getSource()).updateUI();
            }
        });
    }

    //End of mapping                                      //
    ////////////////////////////////////////////////////////
    /**
     * Allows to find component by tooltip.
     */
    public static class JComponentByTipFinder implements ComponentChooser {

        String label;

        StringComparator comparator;

        /**
         * Constructs JComponentByTipFinder.
         *
         * @param lb a text pattern
         * @param comparator specifies string comparision algorithm.
         */
        public JComponentByTipFinder(String lb, StringComparator comparator) {
            label = lb;
            this.comparator = comparator;
        }

        /**
         * Constructs JComponentByTipFinder.
         *
         * @param lb a text pattern
         */
        public JComponentByTipFinder(String lb) {
            this(lb, Operator.getDefaultStringComparator());
        }

        @Override
        public boolean checkComponent(Component comp) {
            if (comp instanceof JComponent) {
                if (((JComponent) comp).getToolTipText() != null) {
                    return (comparator.equals(((JComponent) comp).getToolTipText(),
                            label));
                }
            }
            return false;
        }

        @Override
        public String getDescription() {
            return "JComponent with tool tip \"" + label + "\"";
        }

        @Override
        public String toString() {
            return "JComponentByTipFinder{" + "label=" + label + ", comparator=" + comparator + '}';
        }
    }

    /**
     * Checks component type.
     */
    public static class JComponentFinder extends Finder {

        /**
         * Constructs JComponentFinder.
         *
         * @param sf other searching criteria.
         */
        public JComponentFinder(ComponentChooser sf) {
            super(JComponent.class, sf);
        }

        /**
         * Constructs JComponentFinder.
         */
        public JComponentFinder() {
            super(JComponent.class);
        }
    }

}
