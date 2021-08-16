/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
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

package javax.swing;

import java.applet.Applet;
import java.awt.AWTEvent;
import java.awt.AWTKeyStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.beans.BeanProperty;
import java.beans.JavaBean;
import java.beans.PropertyChangeListener;
import java.beans.Transient;
import java.beans.VetoableChangeListener;
import java.beans.VetoableChangeSupport;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectInputValidation;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleComponent;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleExtendedComponent;
import javax.accessibility.AccessibleKeyBinding;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.EventListenerList;
import javax.swing.plaf.ComponentUI;

import sun.awt.AWTAccessor;
import sun.awt.SunToolkit;
import sun.swing.SwingAccessor;
import sun.swing.SwingUtilities2;

import static javax.swing.ClientPropertyKey.JComponent_ANCESTOR_NOTIFIER;
import static javax.swing.ClientPropertyKey.JComponent_INPUT_VERIFIER;
import static javax.swing.ClientPropertyKey.JComponent_TRANSFER_HANDLER;

/**
 * The base class for all Swing components except top-level containers.
 * To use a component that inherits from <code>JComponent</code>,
 * you must place the component in a containment hierarchy
 * whose root is a top-level Swing container.
 * Top-level Swing containers --
 * such as <code>JFrame</code>, <code>JDialog</code>,
 * and <code>JApplet</code> --
 * are specialized components
 * that provide a place for other Swing components to paint themselves.
 * For an explanation of containment hierarchies, see
 * <a
 href="https://docs.oracle.com/javase/tutorial/uiswing/components/toplevel.html">Swing Components and the Containment Hierarchy</a>,
 * a section in <em>The Java Tutorial</em>.
 *
 * <p>
 * The <code>JComponent</code> class provides:
 * <ul>
 * <li>The base class for both standard and custom components
 *     that use the Swing architecture.
 * <li>A "pluggable look and feel" (L&amp;F) that can be specified by the
 *     programmer or (optionally) selected by the user at runtime.
 *     The look and feel for each component is provided by a
 *     <em>UI delegate</em> -- an object that descends from
 *     {@link javax.swing.plaf.ComponentUI}.
 *     See <a
 * href="https://docs.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html">How
 *     to Set the Look and Feel</a>
 *     in <em>The Java Tutorial</em>
 *     for more information.
 * <li>Comprehensive keystroke handling.
 *     See the document <a
 * href="https://docs.oracle.com/javase/tutorial/uiswing/misc/keybinding.html">How to Use Key Bindings</a>,
 *     an article in <em>The Java Tutorial</em>,
 *     for more information.
 * <li>Support for tool tips --
 *     short descriptions that pop up when the cursor lingers
 *     over a component.
 *     See <a
 * href="https://docs.oracle.com/javase/tutorial/uiswing/components/tooltip.html">How
 *     to Use Tool Tips</a>
 *     in <em>The Java Tutorial</em>
 *     for more information.
 * <li>Support for accessibility.
 *     <code>JComponent</code> contains all of the methods in the
 *     <code>Accessible</code> interface,
 *     but it doesn't actually implement the interface.  That is the
 *     responsibility of the individual classes
 *     that extend <code>JComponent</code>.
 * <li>Support for component-specific properties.
 *     With the {@link #putClientProperty}
 *     and {@link #getClientProperty} methods,
 *     you can associate name-object pairs
 *     with any object that descends from <code>JComponent</code>.
 * <li>An infrastructure for painting
 *     that includes double buffering and support for borders.
 *     For more information see <a
 * href="http://www.oracle.com/technetwork/java/painting-140037.html#swing">Painting</a> and
 * <a href="https://docs.oracle.com/javase/tutorial/uiswing/components/border.html">How
 *     to Use Borders</a>,
 *     both of which are sections in <em>The Java Tutorial</em>.
 * </ul>
 * For more information on these subjects, see the
 * {@link javax.swing Swing package description}
 * and <em>The Java Tutorial</em> section
 * <a href="https://docs.oracle.com/javase/tutorial/uiswing/components/jcomponent.html">The JComponent Class</a>.
 * <p>
 * <code>JComponent</code> and its subclasses document default values
 * for certain properties.  For example, <code>JTable</code> documents the
 * default row height as 16.  Each <code>JComponent</code> subclass
 * that has a <code>ComponentUI</code> will create the
 * <code>ComponentUI</code> as part of its constructor.  In order
 * to provide a particular look and feel each
 * <code>ComponentUI</code> may set properties back on the
 * <code>JComponent</code> that created it.  For example, a custom
 * look and feel may require <code>JTable</code>s to have a row
 * height of 24. The documented defaults are the value of a property
 * BEFORE the <code>ComponentUI</code> has been installed.  If you
 * need a specific value for a particular property you should
 * explicitly set it.
 * <p>
 * In release 1.4, the focus subsystem was rearchitected.
 * For more information, see
 * <a href="https://docs.oracle.com/javase/tutorial/uiswing/misc/focus.html">
 * How to Use the Focus Subsystem</a>,
 * a section in <em>The Java Tutorial</em>.
 * <p>
 * <strong>Warning:</strong> Swing is not thread safe. For more
 * information see <a
 * href="package-summary.html#threading">Swing's Threading
 * Policy</a>.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @see KeyStroke
 * @see Action
 * @see #setBorder
 * @see #registerKeyboardAction
 * @see JOptionPane
 * @see #setDebugGraphicsOptions
 * @see #setToolTipText
 * @see #setAutoscrolls
 *
 * @author Hans Muller
 * @author Arnaud Weber
 * @since 1.2
 */
@JavaBean(defaultProperty = "UIClassID")
@SuppressWarnings("serial") // Same-version serialization only
public abstract class JComponent extends Container implements Serializable,
                                              TransferHandler.HasGetTransferHandler
{
    /**
     * @see #getUIClassID
     * @see #writeObject
     */
    private static final String uiClassID = "ComponentUI";

    /**
     * @see #readObject
     */
    private static final Hashtable<ObjectInputStream, ReadObjectCallback> readObjectCallbacks =
            new Hashtable<ObjectInputStream, ReadObjectCallback>(1);

    /**
     * Keys to use for forward focus traversal when the JComponent is
     * managing focus.
     */
    private static Set<KeyStroke> managingFocusForwardTraversalKeys;

    /**
     * Keys to use for backward focus traversal when the JComponent is
     * managing focus.
     */
    private static Set<KeyStroke> managingFocusBackwardTraversalKeys;

    // Following are the possible return values from getObscuredState.
    private static final int NOT_OBSCURED = 0;
    private static final int PARTIALLY_OBSCURED = 1;
    private static final int COMPLETELY_OBSCURED = 2;

    /**
     * Set to true when DebugGraphics has been loaded.
     */
    static boolean DEBUG_GRAPHICS_LOADED;

    /**
     * Key used to look up a value from the AppContext to determine the
     * JComponent the InputVerifier is running for. That is, if
     * AppContext.get(INPUT_VERIFIER_SOURCE_KEY) returns non-null, it
     * indicates the EDT is calling into the InputVerifier from the
     * returned component.
     */
    private static final Object INPUT_VERIFIER_SOURCE_KEY =
            new StringBuilder("InputVerifierSourceKey");

    /* The following fields support set methods for the corresponding
     * java.awt.Component properties.
     */
    private boolean isAlignmentXSet;
    private float alignmentX;
    private boolean isAlignmentYSet;
    private float alignmentY;

    /**
     * Backing store for JComponent properties and listeners
     */

    /** The look and feel delegate for this component. */
    protected transient ComponentUI ui;
    /** A list of event listeners for this component. */
    protected EventListenerList listenerList = new EventListenerList();

    private transient ArrayTable clientProperties;
    private VetoableChangeSupport vetoableChangeSupport;
    /**
     * Whether or not autoscroll has been enabled.
     */
    private boolean autoscrolls;
    private Border border;
    private int flags;

    /* Input verifier for this component */
    private InputVerifier inputVerifier = null;

    private boolean verifyInputWhenFocusTarget = true;

    /**
     * Set in <code>_paintImmediately</code>.
     * Will indicate the child that initiated the painting operation.
     * If <code>paintingChild</code> is opaque, no need to paint
     * any child components after <code>paintingChild</code>.
     * Test used in <code>paintChildren</code>.
     */
    transient Component         paintingChild;

    /**
     * Constant used for <code>registerKeyboardAction</code> that
     * means that the command should be invoked when
     * the component has the focus.
     */
    public static final int WHEN_FOCUSED = 0;

    /**
     * Constant used for <code>registerKeyboardAction</code> that
     * means that the command should be invoked when the receiving
     * component is an ancestor of the focused component or is
     * itself the focused component.
     */
    public static final int WHEN_ANCESTOR_OF_FOCUSED_COMPONENT = 1;

    /**
     * Constant used for <code>registerKeyboardAction</code> that
     * means that the command should be invoked when
     * the receiving component is in the window that has the focus
     * or is itself the focused component.
     */
    public static final int WHEN_IN_FOCUSED_WINDOW = 2;

    /**
     * Constant used by some of the APIs to mean that no condition is defined.
     */
    public static final int UNDEFINED_CONDITION = -1;

    /**
     * The key used by <code>JComponent</code> to access keyboard bindings.
     */
    private static final String KEYBOARD_BINDINGS_KEY = "_KeyboardBindings";

    /**
     * An array of <code>KeyStroke</code>s used for
     * <code>WHEN_IN_FOCUSED_WINDOW</code> are stashed
     * in the client properties under this string.
     */
    private static final String WHEN_IN_FOCUSED_WINDOW_BINDINGS = "_WhenInFocusedWindow";

    /**
     * The comment to display when the cursor is over the component,
     * also known as a "value tip", "flyover help", or "flyover label".
     */
    public static final String TOOL_TIP_TEXT_KEY = "ToolTipText";

    private static final String NEXT_FOCUS = "nextFocus";

    /**
     * <code>JPopupMenu</code> assigned to this component
     * and all of its children
     */
    private JPopupMenu popupMenu;

    /** Private flags **/
    private static final int IS_DOUBLE_BUFFERED                       =  0;
    private static final int ANCESTOR_USING_BUFFER                    =  1;
    private static final int IS_PAINTING_TILE                         =  2;
    private static final int IS_OPAQUE                                =  3;
    private static final int KEY_EVENTS_ENABLED                       =  4;
    private static final int FOCUS_INPUTMAP_CREATED                   =  5;
    private static final int ANCESTOR_INPUTMAP_CREATED                =  6;
    private static final int WIF_INPUTMAP_CREATED                     =  7;
    private static final int ACTIONMAP_CREATED                        =  8;
    private static final int CREATED_DOUBLE_BUFFER                    =  9;
    // bit 10 is free
    private static final int IS_PRINTING                              = 11;
    private static final int IS_PRINTING_ALL                          = 12;
    private static final int IS_REPAINTING                            = 13;
    /** Bits 14-21 are used to handle nested writeObject calls. **/
    private static final int WRITE_OBJ_COUNTER_FIRST                  = 14;
    private static final int RESERVED_1                               = 15;
    private static final int RESERVED_2                               = 16;
    private static final int RESERVED_3                               = 17;
    private static final int RESERVED_4                               = 18;
    private static final int RESERVED_5                               = 19;
    private static final int RESERVED_6                               = 20;
    private static final int WRITE_OBJ_COUNTER_LAST                   = 21;

    private static final int REQUEST_FOCUS_DISABLED                   = 22;
    private static final int INHERITS_POPUP_MENU                      = 23;
    private static final int OPAQUE_SET                               = 24;
    private static final int AUTOSCROLLS_SET                          = 25;
    private static final int FOCUS_TRAVERSAL_KEYS_FORWARD_SET         = 26;
    private static final int FOCUS_TRAVERSAL_KEYS_BACKWARD_SET        = 27;

    private transient AtomicBoolean revalidateRunnableScheduled = new AtomicBoolean(false);

    /**
     * Temporary rectangles.
     */
    private static java.util.List<Rectangle> tempRectangles = new java.util.ArrayList<Rectangle>(11);

    /** Used for <code>WHEN_FOCUSED</code> bindings. */
    private InputMap focusInputMap;
    /** Used for <code>WHEN_ANCESTOR_OF_FOCUSED_COMPONENT</code> bindings. */
    private InputMap ancestorInputMap;
    /** Used for <code>WHEN_IN_FOCUSED_KEY</code> bindings. */
    private ComponentInputMap windowInputMap;

    /** ActionMap. */
    private ActionMap actionMap;

    /** Key used to store the default locale in an AppContext **/
    private static final String defaultLocale = "JComponent.defaultLocale";

    private static Component componentObtainingGraphicsFrom;
    private static Object componentObtainingGraphicsFromLock = new
            StringBuilder("componentObtainingGraphicsFrom");

    /**
     * AA text hints.
     */
    private transient Object aaHint;
    private transient Object lcdRenderingHint;

    static {
        SwingAccessor.setJComponentAccessor(new SwingAccessor.JComponentAccessor() {

            @Override
            public boolean getFlag(JComponent comp, int aFlag) {
                return comp.getFlag(aFlag);
            }

            @Override
            public void compWriteObjectNotify(JComponent comp) {
                comp.compWriteObjectNotify();
            }
        });
    }

    static Graphics safelyGetGraphics(Component c) {
        return safelyGetGraphics(c, SwingUtilities.getRoot(c));
    }

    static Graphics safelyGetGraphics(Component c, Component root) {
        synchronized(componentObtainingGraphicsFromLock) {
            componentObtainingGraphicsFrom = root;
            Graphics g = c.getGraphics();
            componentObtainingGraphicsFrom = null;
            return g;
        }
    }

    static void getGraphicsInvoked(Component root) {
        if (!JComponent.isComponentObtainingGraphicsFrom(root)) {
            JRootPane rootPane = ((RootPaneContainer)root).getRootPane();
            if (rootPane != null) {
                rootPane.disableTrueDoubleBuffering();
            }
        }
    }


    /**
     * Returns true if {@code c} is the component the graphics is being
     * requested of. This is intended for use when getGraphics is invoked.
     */
    private static boolean isComponentObtainingGraphicsFrom(Component c) {
        synchronized(componentObtainingGraphicsFromLock) {
            return (componentObtainingGraphicsFrom == c);
        }
    }

    /**
     * Returns the Set of <code>KeyStroke</code>s to use if the component
     * is managing focus for forward focus traversal.
     */
    @SuppressWarnings("deprecation")
    static Set<KeyStroke> getManagingFocusForwardTraversalKeys() {
        synchronized(JComponent.class) {
            if (managingFocusForwardTraversalKeys == null) {
                managingFocusForwardTraversalKeys = new HashSet<KeyStroke>(1);
                managingFocusForwardTraversalKeys.add(
                    KeyStroke.getKeyStroke(KeyEvent.VK_TAB,
                                           InputEvent.CTRL_MASK));
            }
        }
        return managingFocusForwardTraversalKeys;
    }

    /**
     * Returns the Set of <code>KeyStroke</code>s to use if the component
     * is managing focus for backward focus traversal.
     */
    @SuppressWarnings("deprecation")
    static Set<KeyStroke> getManagingFocusBackwardTraversalKeys() {
        synchronized(JComponent.class) {
            if (managingFocusBackwardTraversalKeys == null) {
                managingFocusBackwardTraversalKeys = new HashSet<KeyStroke>(1);
                managingFocusBackwardTraversalKeys.add(
                    KeyStroke.getKeyStroke(KeyEvent.VK_TAB,
                                           InputEvent.SHIFT_MASK |
                                           InputEvent.CTRL_MASK));
            }
        }
        return managingFocusBackwardTraversalKeys;
    }

    private static Rectangle fetchRectangle() {
        synchronized(tempRectangles) {
            Rectangle rect;
            int size = tempRectangles.size();
            if (size > 0) {
                rect = tempRectangles.remove(size - 1);
            }
            else {
                rect = new Rectangle(0, 0, 0, 0);
            }
            return rect;
        }
    }

    private static void recycleRectangle(Rectangle rect) {
        synchronized(tempRectangles) {
            tempRectangles.add(rect);
        }
    }

    /**
     * Sets whether or not <code>getComponentPopupMenu</code> should delegate
     * to the parent if this component does not have a <code>JPopupMenu</code>
     * assigned to it.
     * <p>
     * The default value for this is false, but some <code>JComponent</code>
     * subclasses that are implemented as a number of <code>JComponent</code>s
     * may set this to true.
     * <p>
     * This is a bound property.
     *
     * @param value whether or not the JPopupMenu is inherited
     * @see #setComponentPopupMenu
     * @since 1.5
     */
    @BeanProperty(description
            = "Whether or not the JPopupMenu is inherited")
    public void setInheritsPopupMenu(boolean value) {
        boolean oldValue = getFlag(INHERITS_POPUP_MENU);
        setFlag(INHERITS_POPUP_MENU, value);
        firePropertyChange("inheritsPopupMenu", oldValue, value);
    }

    /**
     * Returns true if the JPopupMenu should be inherited from the parent.
     *
     * @return true if the JPopupMenu should be inherited from the parent
     * @see #setComponentPopupMenu
     * @since 1.5
     */
    public boolean getInheritsPopupMenu() {
        return getFlag(INHERITS_POPUP_MENU);
    }

    /**
     * Sets the <code>JPopupMenu</code> for this <code>JComponent</code>.
     * The UI is responsible for registering bindings and adding the necessary
     * listeners such that the <code>JPopupMenu</code> will be shown at
     * the appropriate time. When the <code>JPopupMenu</code> is shown
     * depends upon the look and feel: some may show it on a mouse event,
     * some may enable a key binding.
     * <p>
     * If <code>popup</code> is null, and <code>getInheritsPopupMenu</code>
     * returns true, then <code>getComponentPopupMenu</code> will be delegated
     * to the parent. This provides for a way to make all child components
     * inherit the popupmenu of the parent.
     * <p>
     * This is a bound property.
     *
     * @param popup - the popup that will be assigned to this component
     *                may be null
     * @see #getComponentPopupMenu
     * @since 1.5
     */
    @BeanProperty(preferred = true, description
            = "Popup to show")
    public void setComponentPopupMenu(JPopupMenu popup) {
        if(popup != null) {
            enableEvents(AWTEvent.MOUSE_EVENT_MASK);
        }
        JPopupMenu oldPopup = this.popupMenu;
        this.popupMenu = popup;
        firePropertyChange("componentPopupMenu", oldPopup, popup);
    }

    /**
     * Returns <code>JPopupMenu</code> that assigned for this component.
     * If this component does not have a <code>JPopupMenu</code> assigned
     * to it and <code>getInheritsPopupMenu</code> is true, this
     * will return <code>getParent().getComponentPopupMenu()</code> (assuming
     * the parent is valid.)
     *
     * @return <code>JPopupMenu</code> assigned for this component
     *         or <code>null</code> if no popup assigned
     * @see #setComponentPopupMenu
     * @since 1.5
     */
    @SuppressWarnings("removal")
    public JPopupMenu getComponentPopupMenu() {

        if(!getInheritsPopupMenu()) {
            return popupMenu;
        }

        if(popupMenu == null) {
            // Search parents for its popup
            Container parent = getParent();
            while (parent != null) {
                if(parent instanceof JComponent) {
                    return ((JComponent)parent).getComponentPopupMenu();
                }
                if(parent instanceof Window ||
                   parent instanceof Applet) {
                    // Reached toplevel, break and return null
                    break;
                }
                parent = parent.getParent();
            }
            return null;
        }

        return popupMenu;
    }

    /**
     * Default <code>JComponent</code> constructor.  This constructor does
     * very little initialization beyond calling the <code>Container</code>
     * constructor.  For example, the initial layout manager is
     * <code>null</code>. It does, however, set the component's locale
     * property to the value returned by
     * <code>JComponent.getDefaultLocale</code>.
     *
     * @see #getDefaultLocale
     */
    public JComponent() {
        super();
        // We enable key events on all JComponents so that accessibility
        // bindings will work everywhere. This is a partial fix to BugID
        // 4282211.
        enableEvents(AWTEvent.KEY_EVENT_MASK);
        if (isManagingFocus()) {
            LookAndFeel.installProperty(this,
                                        "focusTraversalKeysForward",
                                  getManagingFocusForwardTraversalKeys());
            LookAndFeel.installProperty(this,
                                        "focusTraversalKeysBackward",
                                  getManagingFocusBackwardTraversalKeys());
        }

        super.setLocale( JComponent.getDefaultLocale() );
    }


    /**
     * This method is called to update the UI property to a value from the
     * current look and feel.
     * {@code JComponent} subclasses must override this method
     * like this:
     * <pre>
     *   public void updateUI() {
     *      setUI((SliderUI)UIManager.getUI(this);
     *   }
     *  </pre>
     *
     * @implSpec The default implementation of this method does nothing.
     *
     * @see #setUI
     * @see UIManager#getLookAndFeel
     * @see UIManager#getUI
     */
    public void updateUI() {}

    /**
     * Returns the look and feel delegate that renders this component.
     *
     * @return the {@code ComponentUI} object that renders this component
     * @since 9
     */
    @Transient
    public ComponentUI getUI() {
        return ui;
    }

    /**
     * Sets the look and feel delegate for this component.
     * <code>JComponent</code> subclasses generally override this method
     * to narrow the argument type. For example, in <code>JSlider</code>:
     * <pre>
     * public void setUI(SliderUI newUI) {
     *     super.setUI(newUI);
     * }
     *  </pre>
     * <p>
     * Additionally <code>JComponent</code> subclasses must provide a
     * <code>getUI</code> method that returns the correct type.  For example:
     * <pre>
     * public SliderUI getUI() {
     *     return (SliderUI)ui;
     * }
     * </pre>
     *
     * @param newUI the new UI delegate
     * @see #updateUI
     * @see UIManager#getLookAndFeel
     * @see UIManager#getUI
     */
    @BeanProperty(hidden = true, visualUpdate = true, description
            = "The component's look and feel delegate.")
    protected void setUI(ComponentUI newUI) {
        /* We do not check that the UI instance is different
         * before allowing the switch in order to enable the
         * same UI instance *with different default settings*
         * to be installed.
         */

        uninstallUIAndProperties();

        // aaText shouldn't persist between look and feels, reset it.
        aaHint = UIManager.getDefaults().get(
                RenderingHints.KEY_TEXT_ANTIALIASING);
        lcdRenderingHint = UIManager.getDefaults().get(
                RenderingHints.KEY_TEXT_LCD_CONTRAST);
        ComponentUI oldUI = ui;
        ui = newUI;
        if (ui != null) {
            ui.installUI(this);
        }

        firePropertyChange("UI", oldUI, newUI);
        revalidate();
        repaint();
    }

    /**
     * Uninstalls the UI, if any, and any client properties designated
     * as being specific to the installed UI - instances of
     * {@code UIClientPropertyKey}.
     */
    private void uninstallUIAndProperties() {
        if (ui != null) {
            ui.uninstallUI(this);
            //clean UIClientPropertyKeys from client properties
            if (clientProperties != null) {
                synchronized(clientProperties) {
                    Object[] clientPropertyKeys =
                        clientProperties.getKeys(null);
                    if (clientPropertyKeys != null) {
                        for (Object key : clientPropertyKeys) {
                            if (key instanceof UIClientPropertyKey) {
                                putClientProperty(key, null);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the <code>UIDefaults</code> key used to
     * look up the name of the <code>swing.plaf.ComponentUI</code>
     * class that defines the look and feel
     * for this component.  Most applications will never need to
     * call this method.  Subclasses of <code>JComponent</code> that support
     * pluggable look and feel should override this method to
     * return a <code>UIDefaults</code> key that maps to the
     * <code>ComponentUI</code> subclass that defines their look and feel.
     *
     * @return the <code>UIDefaults</code> key for a
     *          <code>ComponentUI</code> subclass
     * @see UIDefaults#getUI
     */
    @BeanProperty(bound = false, expert = true, description
            = "UIClassID")
    public String getUIClassID() {
        return uiClassID;
    }


    /**
     * Returns the graphics object used to paint this component.
     * If <code>DebugGraphics</code> is turned on we create a new
     * <code>DebugGraphics</code> object if necessary.
     * Otherwise we just configure the
     * specified graphics object's foreground and font.
     *
     * @param g the original <code>Graphics</code> object
     * @return a <code>Graphics</code> object configured for this component
     */
    protected Graphics getComponentGraphics(Graphics g) {
        Graphics componentGraphics = g;
        if (ui != null && DEBUG_GRAPHICS_LOADED) {
            if ((DebugGraphics.debugComponentCount() != 0) &&
                    (shouldDebugGraphics() != 0) &&
                    !(g instanceof DebugGraphics)) {
                componentGraphics = new DebugGraphics(g,this);
            }
        }
        componentGraphics.setColor(getForeground());
        componentGraphics.setFont(getFont());

        return componentGraphics;
    }


    /**
     * Calls the UI delegate's paint method, if the UI delegate
     * is non-<code>null</code>.  We pass the delegate a copy of the
     * <code>Graphics</code> object to protect the rest of the
     * paint code from irrevocable changes
     * (for example, <code>Graphics.translate</code>).
     * <p>
     * If you override this in a subclass you should not make permanent
     * changes to the passed in <code>Graphics</code>. For example, you
     * should not alter the clip <code>Rectangle</code> or modify the
     * transform. If you need to do these operations you may find it
     * easier to create a new <code>Graphics</code> from the passed in
     * <code>Graphics</code> and manipulate it. Further, if you do not
     * invoke super's implementation you must honor the opaque property, that is
     * if this component is opaque, you must completely fill in the background
     * in an opaque color. If you do not honor the opaque property you
     * will likely see visual artifacts.
     * <p>
     * The passed in <code>Graphics</code> object might
     * have a transform other than the identify transform
     * installed on it.  In this case, you might get
     * unexpected results if you cumulatively apply
     * another transform.
     *
     * @param g the <code>Graphics</code> object to protect
     * @see #paint
     * @see ComponentUI
     */
    protected void paintComponent(Graphics g) {
        if (ui != null) {
            Graphics scratchGraphics = (g == null) ? null : g.create();
            try {
                ui.update(scratchGraphics, this);
            }
            finally {
                scratchGraphics.dispose();
            }
        }
    }

    /**
     * Paints this component's children.
     * If <code>shouldUseBuffer</code> is true,
     * no component ancestor has a buffer and
     * the component children can use a buffer if they have one.
     * Otherwise, one ancestor has a buffer currently in use and children
     * should not use a buffer to paint.
     * @param g  the <code>Graphics</code> context in which to paint
     * @see #paint
     * @see java.awt.Container#paint
     */
    protected void paintChildren(Graphics g) {
        Graphics sg = g;

        synchronized(getTreeLock()) {
            int i = getComponentCount() - 1;
            if (i < 0) {
                return;
            }
            // If we are only to paint to a specific child, determine
            // its index.
            if (paintingChild != null &&
                (paintingChild instanceof JComponent) &&
                paintingChild.isOpaque()) {
                for (; i >= 0; i--) {
                    if (getComponent(i) == paintingChild){
                        break;
                    }
                }
            }
            Rectangle tmpRect = fetchRectangle();
            boolean checkSiblings = (!isOptimizedDrawingEnabled() &&
                                     checkIfChildObscuredBySibling());
            Rectangle clipBounds = null;
            if (checkSiblings) {
                clipBounds = sg.getClipBounds();
                if (clipBounds == null) {
                    clipBounds = new Rectangle(0, 0, getWidth(),
                                               getHeight());
                }
            }
            boolean printing = getFlag(IS_PRINTING);
            final Window window = SwingUtilities.getWindowAncestor(this);
            final boolean isWindowOpaque = window == null || window.isOpaque();
            for (; i >= 0 ; i--) {
                Component comp = getComponent(i);
                if (comp == null) {
                    continue;
                }

                final boolean isJComponent = comp instanceof JComponent;

                // Enable painting of heavyweights in non-opaque windows.
                // See 6884960
                if ((!isWindowOpaque || isJComponent ||
                            isLightweightComponent(comp)) && comp.isVisible())
                {
                    Rectangle cr;

                    cr = comp.getBounds(tmpRect);
                    Shape clip = g.getClip();
                    boolean hitClip = (clip != null)
                            ? clip.intersects(cr.x, cr.y, cr.width, cr.height)
                            : true;

                    if (hitClip) {
                        if (checkSiblings && i > 0) {
                            int x = cr.x;
                            int y = cr.y;
                            int width = cr.width;
                            int height = cr.height;
                            SwingUtilities.computeIntersection
                                (clipBounds.x, clipBounds.y,
                                 clipBounds.width, clipBounds.height, cr);

                            if(getObscuredState(i, cr.x, cr.y, cr.width,
                                          cr.height) == COMPLETELY_OBSCURED) {
                                continue;
                            }
                            cr.x = x;
                            cr.y = y;
                            cr.width = width;
                            cr.height = height;
                        }
                        Graphics cg = sg.create(cr.x, cr.y, cr.width,
                                                cr.height);
                        cg.setColor(comp.getForeground());
                        cg.setFont(comp.getFont());
                        boolean shouldSetFlagBack = false;
                        try {
                            if(isJComponent) {
                                if(getFlag(ANCESTOR_USING_BUFFER)) {
                                    ((JComponent)comp).setFlag(
                                                 ANCESTOR_USING_BUFFER,true);
                                    shouldSetFlagBack = true;
                                }
                                if(getFlag(IS_PAINTING_TILE)) {
                                    ((JComponent)comp).setFlag(
                                                 IS_PAINTING_TILE,true);
                                    shouldSetFlagBack = true;
                                }
                                if(!printing) {
                                    comp.paint(cg);
                                }
                                else {
                                    if (!getFlag(IS_PRINTING_ALL)) {
                                        comp.print(cg);
                                    }
                                    else {
                                        comp.printAll(cg);
                                    }
                                }
                            } else {
                                // The component is either lightweight, or
                                // heavyweight in a non-opaque window
                                if (!printing) {
                                    comp.paint(cg);
                                }
                                else {
                                    if (!getFlag(IS_PRINTING_ALL)) {
                                        comp.print(cg);
                                    }
                                    else {
                                        comp.printAll(cg);
                                    }
                                }
                            }
                        } finally {
                            cg.dispose();
                            if(shouldSetFlagBack) {
                                ((JComponent)comp).setFlag(
                                             ANCESTOR_USING_BUFFER,false);
                                ((JComponent)comp).setFlag(
                                             IS_PAINTING_TILE,false);
                            }
                        }
                    }
                }

            }
            recycleRectangle(tmpRect);
        }
    }

    /**
     * Paints the component's border.
     * <p>
     * If you override this in a subclass you should not make permanent
     * changes to the passed in <code>Graphics</code>. For example, you
     * should not alter the clip <code>Rectangle</code> or modify the
     * transform. If you need to do these operations you may find it
     * easier to create a new <code>Graphics</code> from the passed in
     * <code>Graphics</code> and manipulate it.
     *
     * @param g  the <code>Graphics</code> context in which to paint
     *
     * @see #paint
     * @see #setBorder
     */
    protected void paintBorder(Graphics g) {
        Border border = getBorder();
        if (border != null) {
            border.paintBorder(this, g, 0, 0, getWidth(), getHeight());
        }
    }


    /**
     * Calls <code>paint</code>.  Doesn't clear the background but see
     * <code>ComponentUI.update</code>, which is called by
     * <code>paintComponent</code>.
     *
     * @param g the <code>Graphics</code> context in which to paint
     * @see #paint
     * @see #paintComponent
     * @see javax.swing.plaf.ComponentUI
     */
    public void update(Graphics g) {
        paint(g);
    }


    /**
     * Invoked by Swing to draw components.
     * Applications should not invoke <code>paint</code> directly,
     * but should instead use the <code>repaint</code> method to
     * schedule the component for redrawing.
     * <p>
     * This method actually delegates the work of painting to three
     * protected methods: <code>paintComponent</code>,
     * <code>paintBorder</code>,
     * and <code>paintChildren</code>.  They're called in the order
     * listed to ensure that children appear on top of component itself.
     * Generally speaking, the component and its children should not
     * paint in the insets area allocated to the border. Subclasses can
     * just override this method, as always.  A subclass that just
     * wants to specialize the UI (look and feel) delegate's
     * <code>paint</code> method should just override
     * <code>paintComponent</code>.
     *
     * @param g  the <code>Graphics</code> context in which to paint
     * @see #paintComponent
     * @see #paintBorder
     * @see #paintChildren
     * @see #getComponentGraphics
     * @see #repaint
     */
    public void paint(Graphics g) {
        boolean shouldClearPaintFlags = false;

        if ((getWidth() <= 0) || (getHeight() <= 0)) {
            return;
        }

        Graphics componentGraphics = getComponentGraphics(g);
        Graphics co = componentGraphics.create();
        try {
            RepaintManager repaintManager = RepaintManager.currentManager(this);
            Rectangle clipRect = co.getClipBounds();
            int clipX;
            int clipY;
            int clipW;
            int clipH;
            if (clipRect == null) {
                clipX = clipY = 0;
                clipW = getWidth();
                clipH = getHeight();
            }
            else {
                clipX = clipRect.x;
                clipY = clipRect.y;
                clipW = clipRect.width;
                clipH = clipRect.height;
            }

            if(clipW > getWidth()) {
                clipW = getWidth();
            }
            if(clipH > getHeight()) {
                clipH = getHeight();
            }

            if(getParent() != null && !(getParent() instanceof JComponent)) {
                adjustPaintFlags();
                shouldClearPaintFlags = true;
            }

            int bw,bh;
            boolean printing = getFlag(IS_PRINTING);
            if (!printing && repaintManager.isDoubleBufferingEnabled() &&
                !getFlag(ANCESTOR_USING_BUFFER) && isDoubleBuffered() &&
                (getFlag(IS_REPAINTING) || repaintManager.isPainting()))
            {
                repaintManager.beginPaint();
                try {
                    repaintManager.paint(this, this, co, clipX, clipY, clipW,
                                         clipH);
                } finally {
                    repaintManager.endPaint();
                }
            }
            else {
                // Will ocassionaly happen in 1.2, especially when printing.
                if (clipRect == null) {
                    co.setClip(clipX, clipY, clipW, clipH);
                }

                if (!rectangleIsObscured(clipX,clipY,clipW,clipH)) {
                    if (!printing) {
                        paintComponent(co);
                        paintBorder(co);
                    }
                    else {
                        printComponent(co);
                        printBorder(co);
                    }
                }
                if (!printing) {
                    paintChildren(co);
                }
                else {
                    printChildren(co);
                }
            }
        } finally {
            co.dispose();
            if(shouldClearPaintFlags) {
                setFlag(ANCESTOR_USING_BUFFER,false);
                setFlag(IS_PAINTING_TILE,false);
                setFlag(IS_PRINTING,false);
                setFlag(IS_PRINTING_ALL,false);
            }
        }
    }

    // paint forcing use of the double buffer.  This is used for historical
    // reasons: JViewport, when scrolling, previously directly invoked paint
    // while turning off double buffering at the RepaintManager level, this
    // codes simulates that.
    void paintForceDoubleBuffered(Graphics g) {
        RepaintManager rm = RepaintManager.currentManager(this);
        Rectangle clip = g.getClipBounds();
        rm.beginPaint();
        setFlag(IS_REPAINTING, true);
        try {
            rm.paint(this, this, g, clip.x, clip.y, clip.width, clip.height);
        } finally {
            rm.endPaint();
            setFlag(IS_REPAINTING, false);
        }
    }

    /**
     * Returns true if this component, or any of its ancestors, are in
     * the processing of painting.
     */
    boolean isPainting() {
        Container component = this;
        while (component != null) {
            if (component instanceof JComponent &&
                   ((JComponent)component).getFlag(ANCESTOR_USING_BUFFER)) {
                return true;
            }
            component = component.getParent();
        }
        return false;
    }

    private void adjustPaintFlags() {
        JComponent jparent;
        Container parent;
        for(parent = getParent() ; parent != null ; parent =
            parent.getParent()) {
            if(parent instanceof JComponent) {
                jparent = (JComponent) parent;
                if(jparent.getFlag(ANCESTOR_USING_BUFFER))
                  setFlag(ANCESTOR_USING_BUFFER, true);
                if(jparent.getFlag(IS_PAINTING_TILE))
                  setFlag(IS_PAINTING_TILE, true);
                if(jparent.getFlag(IS_PRINTING))
                  setFlag(IS_PRINTING, true);
                if(jparent.getFlag(IS_PRINTING_ALL))
                  setFlag(IS_PRINTING_ALL, true);
                break;
            }
        }
    }

    /**
     * Invoke this method to print the component. This method invokes
     * <code>print</code> on the component.
     *
     * @param g the <code>Graphics</code> context in which to paint
     * @see #print
     * @see #printComponent
     * @see #printBorder
     * @see #printChildren
     */
    public void printAll(Graphics g) {
        setFlag(IS_PRINTING_ALL, true);
        try {
            print(g);
        }
        finally {
            setFlag(IS_PRINTING_ALL, false);
        }
    }

    /**
     * Invoke this method to print the component to the specified
     * <code>Graphics</code>. This method will result in invocations
     * of <code>printComponent</code>, <code>printBorder</code> and
     * <code>printChildren</code>. It is recommended that you override
     * one of the previously mentioned methods rather than this one if
     * your intention is to customize the way printing looks. However,
     * it can be useful to override this method should you want to prepare
     * state before invoking the superclass behavior. As an example,
     * if you wanted to change the component's background color before
     * printing, you could do the following:
     * <pre>
     *     public void print(Graphics g) {
     *         Color orig = getBackground();
     *         setBackground(Color.WHITE);
     *
     *         // wrap in try/finally so that we always restore the state
     *         try {
     *             super.print(g);
     *         } finally {
     *             setBackground(orig);
     *         }
     *     }
     * </pre>
     * <p>
     * Alternatively, or for components that delegate painting to other objects,
     * you can query during painting whether or not the component is in the
     * midst of a print operation. The <code>isPaintingForPrint</code> method provides
     * this ability and its return value will be changed by this method: to
     * <code>true</code> immediately before rendering and to <code>false</code>
     * immediately after. With each change a property change event is fired on
     * this component with the name <code>"paintingForPrint"</code>.
     * <p>
     * This method sets the component's state such that the double buffer
     * will not be used: painting will be done directly on the passed in
     * <code>Graphics</code>.
     *
     * @param g the <code>Graphics</code> context in which to paint
     * @see #printComponent
     * @see #printBorder
     * @see #printChildren
     * @see #isPaintingForPrint
     */
    public void print(Graphics g) {
        setFlag(IS_PRINTING, true);
        firePropertyChange("paintingForPrint", false, true);
        try {
            paint(g);
        }
        finally {
            setFlag(IS_PRINTING, false);
            firePropertyChange("paintingForPrint", true, false);
        }
    }

    /**
     * This is invoked during a printing operation. This is implemented to
     * invoke <code>paintComponent</code> on the component. Override this
     * if you wish to add special painting behavior when printing.
     *
     * @param g the <code>Graphics</code> context in which to paint
     * @see #print
     * @since 1.3
     */
    protected void printComponent(Graphics g) {
        paintComponent(g);
    }

    /**
     * Prints this component's children. This is implemented to invoke
     * <code>paintChildren</code> on the component. Override this if you
     * wish to print the children differently than painting.
     *
     * @param g the <code>Graphics</code> context in which to paint
     * @see #print
     * @since 1.3
     */
    protected void printChildren(Graphics g) {
        paintChildren(g);
    }

    /**
     * Prints the component's border. This is implemented to invoke
     * <code>paintBorder</code> on the component. Override this if you
     * wish to print the border differently that it is painted.
     *
     * @param g the <code>Graphics</code> context in which to paint
     * @see #print
     * @since 1.3
     */
    protected void printBorder(Graphics g) {
        paintBorder(g);
    }

    /**
     *  Returns true if the component is currently painting a tile.
     *  If this method returns true, paint will be called again for another
     *  tile. This method returns false if you are not painting a tile or
     *  if the last tile is painted.
     *  Use this method to keep some state you might need between tiles.
     *
     *  @return  true if the component is currently painting a tile,
     *          false otherwise
     */
    @BeanProperty(bound = false)
    public boolean isPaintingTile() {
        return getFlag(IS_PAINTING_TILE);
    }

    /**
     * Returns <code>true</code> if the current painting operation on this
     * component is part of a <code>print</code> operation. This method is
     * useful when you want to customize what you print versus what you show
     * on the screen.
     * <p>
     * You can detect changes in the value of this property by listening for
     * property change events on this component with name
     * <code>"paintingForPrint"</code>.
     * <p>
     * Note: This method provides complimentary functionality to that provided
     * by other high level Swing printing APIs. However, it deals strictly with
     * painting and should not be confused as providing information on higher
     * level print processes. For example, a {@link javax.swing.JTable#print()}
     * operation doesn't necessarily result in a continuous rendering of the
     * full component, and the return value of this method can change multiple
     * times during that operation. It is even possible for the component to be
     * painted to the screen while the printing process is ongoing. In such a
     * case, the return value of this method is <code>true</code> when, and only
     * when, the table is being painted as part of the printing process.
     *
     * @return true if the current painting operation on this component
     *         is part of a print operation
     * @see #print
     * @since 1.6
     */
    @BeanProperty(bound = false)
    public final boolean isPaintingForPrint() {
        return getFlag(IS_PRINTING);
    }

    /**
     * In release 1.4, the focus subsystem was rearchitected.
     * For more information, see
     * <a href="https://docs.oracle.com/javase/tutorial/uiswing/misc/focus.html">
     * How to Use the Focus Subsystem</a>,
     * a section in <em>The Java Tutorial</em>.
     * <p>
     * Changes this <code>JComponent</code>'s focus traversal keys to
     * CTRL+TAB and CTRL+SHIFT+TAB. Also prevents
     * <code>SortingFocusTraversalPolicy</code> from considering descendants
     * of this JComponent when computing a focus traversal cycle.
     *
     * @return false
     * @see java.awt.Component#setFocusTraversalKeys
     * @see SortingFocusTraversalPolicy
     * @deprecated As of 1.4, replaced by
     *   <code>Component.setFocusTraversalKeys(int, Set)</code> and
     *   <code>Container.setFocusCycleRoot(boolean)</code>.
     */
    @Deprecated
    @BeanProperty(bound = false)
    public boolean isManagingFocus() {
        return false;
    }

    private void registerNextFocusableComponent() {
        registerNextFocusableComponent(getNextFocusableComponent());
    }

    private void registerNextFocusableComponent(Component
                                                nextFocusableComponent) {
        if (nextFocusableComponent == null) {
            return;
        }

        Container nearestRoot =
            (isFocusCycleRoot()) ? this : getFocusCycleRootAncestor();
        FocusTraversalPolicy policy = nearestRoot.getFocusTraversalPolicy();
        if (!(policy instanceof LegacyGlueFocusTraversalPolicy)) {
            policy = new LegacyGlueFocusTraversalPolicy(policy);
            nearestRoot.setFocusTraversalPolicy(policy);
        }
        ((LegacyGlueFocusTraversalPolicy)policy).
            setNextFocusableComponent(this, nextFocusableComponent);
    }

    private void deregisterNextFocusableComponent() {
        Component nextFocusableComponent = getNextFocusableComponent();
        if (nextFocusableComponent == null) {
            return;
        }

        Container nearestRoot =
            (isFocusCycleRoot()) ? this : getFocusCycleRootAncestor();
        if (nearestRoot == null) {
            return;
        }
        FocusTraversalPolicy policy = nearestRoot.getFocusTraversalPolicy();
        if (policy instanceof LegacyGlueFocusTraversalPolicy) {
            ((LegacyGlueFocusTraversalPolicy)policy).
                unsetNextFocusableComponent(this, nextFocusableComponent);
        }
    }

    /**
     * In release 1.4, the focus subsystem was rearchitected.
     * For more information, see
     * <a href="https://docs.oracle.com/javase/tutorial/uiswing/misc/focus.html">
     * How to Use the Focus Subsystem</a>,
     * a section in <em>The Java Tutorial</em>.
     * <p>
     * Overrides the default <code>FocusTraversalPolicy</code> for this
     * <code>JComponent</code>'s focus traversal cycle by unconditionally
     * setting the specified <code>Component</code> as the next
     * <code>Component</code> in the cycle, and this <code>JComponent</code>
     * as the specified <code>Component</code>'s previous
     * <code>Component</code> in the cycle.
     *
     * @param aComponent the <code>Component</code> that should follow this
     *        <code>JComponent</code> in the focus traversal cycle
     *
     * @see #getNextFocusableComponent
     * @see java.awt.FocusTraversalPolicy
     * @deprecated As of 1.4, replaced by <code>FocusTraversalPolicy</code>
     */
    @Deprecated
    public void setNextFocusableComponent(Component aComponent) {
        boolean displayable = isDisplayable();
        if (displayable) {
            deregisterNextFocusableComponent();
        }
        putClientProperty(NEXT_FOCUS, aComponent);
        if (displayable) {
            registerNextFocusableComponent(aComponent);
        }
    }

    /**
     * In release 1.4, the focus subsystem was rearchitected.
     * For more information, see
     * <a href="https://docs.oracle.com/javase/tutorial/uiswing/misc/focus.html">
     * How to Use the Focus Subsystem</a>,
     * a section in <em>The Java Tutorial</em>.
     * <p>
     * Returns the <code>Component</code> set by a prior call to
     * <code>setNextFocusableComponent(Component)</code> on this
     * <code>JComponent</code>.
     *
     * @return the <code>Component</code> that will follow this
     *        <code>JComponent</code> in the focus traversal cycle, or
     *        <code>null</code> if none has been explicitly specified
     *
     * @see #setNextFocusableComponent
     * @deprecated As of 1.4, replaced by <code>FocusTraversalPolicy</code>.
     */
    @Deprecated
    public Component getNextFocusableComponent() {
        return (Component)getClientProperty(NEXT_FOCUS);
    }

    /**
     * Provides a hint as to whether or not this <code>JComponent</code>
     * should get focus. This is only a hint, and it is up to consumers that
     * are requesting focus to honor this property. This is typically honored
     * for mouse operations, but not keyboard operations. For example, look
     * and feels could verify this property is true before requesting focus
     * during a mouse operation. This would often times be used if you did
     * not want a mouse press on a <code>JComponent</code> to steal focus,
     * but did want the <code>JComponent</code> to be traversable via the
     * keyboard. If you do not want this <code>JComponent</code> focusable at
     * all, use the <code>setFocusable</code> method instead.
     * <p>
     * Please see
     * <a href="https://docs.oracle.com/javase/tutorial/uiswing/misc/focus.html">
     * How to Use the Focus Subsystem</a>,
     * a section in <em>The Java Tutorial</em>,
     * for more information.
     *
     * @param requestFocusEnabled indicates whether you want this
     *        <code>JComponent</code> to be focusable or not
     * @see <a href="../../java/awt/doc-files/FocusSpec.html">Focus Specification</a>
     * @see java.awt.Component#setFocusable
     */
    public void setRequestFocusEnabled(boolean requestFocusEnabled) {
        setFlag(REQUEST_FOCUS_DISABLED, !requestFocusEnabled);
    }

    /**
     * Returns <code>true</code> if this <code>JComponent</code> should
     * get focus; otherwise returns <code>false</code>.
     * <p>
     * Please see
     * <a href="https://docs.oracle.com/javase/tutorial/uiswing/misc/focus.html">
     * How to Use the Focus Subsystem</a>,
     * a section in <em>The Java Tutorial</em>,
     * for more information.
     *
     * @return <code>true</code> if this component should get focus,
     *     otherwise returns <code>false</code>
     * @see #setRequestFocusEnabled
     * @see <a href="../../java/awt/doc-files/FocusSpec.html">Focus
     *      Specification</a>
     * @see java.awt.Component#isFocusable
     */
    public boolean isRequestFocusEnabled() {
        return !getFlag(REQUEST_FOCUS_DISABLED);
    }

    /**
     * Requests that this <code>Component</code> gets the input focus.
     * Refer to {@link java.awt.Component#requestFocus()
     * Component.requestFocus()} for a complete description of
     * this method.
     * <p>
     * Note that the use of this method is discouraged because
     * its behavior is platform dependent. Instead we recommend the
     * use of {@link #requestFocusInWindow() requestFocusInWindow()}.
     * If you would like more information on focus, see
     * <a href="https://docs.oracle.com/javase/tutorial/uiswing/misc/focus.html">
     * How to Use the Focus Subsystem</a>,
     * a section in <em>The Java Tutorial</em>.
     *
     * @see java.awt.Component#requestFocusInWindow()
     * @see java.awt.Component#requestFocusInWindow(boolean)
     * @since 1.4
     */
    public void requestFocus() {
        super.requestFocus();
    }

    /**
     * Requests that this <code>Component</code> gets the input focus.
     * Refer to {@link java.awt.Component#requestFocus(boolean)
     * Component.requestFocus(boolean)} for a complete description of
     * this method.
     * <p>
     * Note that the use of this method is discouraged because
     * its behavior is platform dependent. Instead we recommend the
     * use of {@link #requestFocusInWindow(boolean)
     * requestFocusInWindow(boolean)}.
     * If you would like more information on focus, see
     * <a href="https://docs.oracle.com/javase/tutorial/uiswing/misc/focus.html">
     * How to Use the Focus Subsystem</a>,
     * a section in <em>The Java Tutorial</em>.
     *
     * @param temporary boolean indicating if the focus change is temporary
     * @return <code>false</code> if the focus change request is guaranteed to
     *         fail; <code>true</code> if it is likely to succeed
     * @see java.awt.Component#requestFocusInWindow()
     * @see java.awt.Component#requestFocusInWindow(boolean)
     * @since 1.4
     */
    public boolean requestFocus(boolean temporary) {
        return super.requestFocus(temporary);
    }

    /**
     * Requests that this <code>Component</code> gets the input focus.
     * Refer to {@link java.awt.Component#requestFocusInWindow()
     * Component.requestFocusInWindow()} for a complete description of
     * this method.
     * <p>
     * If you would like more information on focus, see
     * <a href="https://docs.oracle.com/javase/tutorial/uiswing/misc/focus.html">
     * How to Use the Focus Subsystem</a>,
     * a section in <em>The Java Tutorial</em>.
     *
     * @return <code>false</code> if the focus change request is guaranteed to
     *         fail; <code>true</code> if it is likely to succeed
     * @see java.awt.Component#requestFocusInWindow()
     * @see java.awt.Component#requestFocusInWindow(boolean)
     * @since 1.4
     */
    public boolean requestFocusInWindow() {
        return super.requestFocusInWindow();
    }

    /**
     * Requests that this <code>Component</code> gets the input focus.
     * Refer to {@link java.awt.Component#requestFocusInWindow(boolean)
     * Component.requestFocusInWindow(boolean)} for a complete description of
     * this method.
     * <p>
     * If you would like more information on focus, see
     * <a href="https://docs.oracle.com/javase/tutorial/uiswing/misc/focus.html">
     * How to Use the Focus Subsystem</a>,
     * a section in <em>The Java Tutorial</em>.
     *
     * @param temporary boolean indicating if the focus change is temporary
     * @return <code>false</code> if the focus change request is guaranteed to
     *         fail; <code>true</code> if it is likely to succeed
     * @see java.awt.Component#requestFocusInWindow()
     * @see java.awt.Component#requestFocusInWindow(boolean)
     * @since 1.4
     */
    protected boolean requestFocusInWindow(boolean temporary) {
        return super.requestFocusInWindow(temporary);
    }

    /**
     * Requests that this Component get the input focus, and that this
     * Component's top-level ancestor become the focused Window. This component
     * must be displayable, visible, and focusable for the request to be
     * granted.
     * <p>
     * This method is intended for use by focus implementations. Client code
     * should not use this method; instead, it should use
     * <code>requestFocusInWindow()</code>.
     *
     * @see #requestFocusInWindow()
     */
    public void grabFocus() {
        requestFocus();
    }

    /**
     * Sets the value to indicate whether input verifier for the
     * current focus owner will be called before this component requests
     * focus. The default is true. Set to false on components such as a
     * Cancel button or a scrollbar, which should activate even if the
     * input in the current focus owner is not "passed" by the input
     * verifier for that component.
     *
     * @param verifyInputWhenFocusTarget value for the
     *        <code>verifyInputWhenFocusTarget</code> property
     * @see InputVerifier
     * @see #setInputVerifier
     * @see #getInputVerifier
     * @see #getVerifyInputWhenFocusTarget
     *
     * @since 1.3
     */
    @BeanProperty(description
            = "Whether the Component verifies input before accepting focus.")
    public void setVerifyInputWhenFocusTarget(boolean
                                              verifyInputWhenFocusTarget) {
        boolean oldVerifyInputWhenFocusTarget =
            this.verifyInputWhenFocusTarget;
        this.verifyInputWhenFocusTarget = verifyInputWhenFocusTarget;
        firePropertyChange("verifyInputWhenFocusTarget",
                           oldVerifyInputWhenFocusTarget,
                           verifyInputWhenFocusTarget);
    }

    /**
     * Returns the value that indicates whether the input verifier for the
     * current focus owner will be called before this component requests
     * focus.
     *
     * @return value of the <code>verifyInputWhenFocusTarget</code> property
     *
     * @see InputVerifier
     * @see #setInputVerifier
     * @see #getInputVerifier
     * @see #setVerifyInputWhenFocusTarget
     *
     * @since 1.3
     */
    public boolean getVerifyInputWhenFocusTarget() {
        return verifyInputWhenFocusTarget;
    }


    /**
     * Gets the <code>FontMetrics</code> for the specified <code>Font</code>.
     *
     * @param font the font for which font metrics is to be
     *          obtained
     * @return the font metrics for <code>font</code>
     * @throws NullPointerException if <code>font</code> is null
     * @since 1.5
     */
    public FontMetrics getFontMetrics(Font font) {
        return SwingUtilities2.getFontMetrics(this, font);
    }


    /**
     * Sets the preferred size of this component.
     * If <code>preferredSize</code> is <code>null</code>, the UI will
     * be asked for the preferred size.
     */
    @BeanProperty(preferred = true, description
            = "The preferred size of the component.")
    public void setPreferredSize(Dimension preferredSize) {
        super.setPreferredSize(preferredSize);
    }


    /**
     * If the <code>preferredSize</code> has been set to a
     * non-<code>null</code> value just returns it.
     * If the UI delegate's <code>getPreferredSize</code>
     * method returns a non <code>null</code> value then return that;
     * otherwise defer to the component's layout manager.
     *
     * @return the value of the <code>preferredSize</code> property
     * @see #setPreferredSize
     * @see ComponentUI
     */
    @Transient
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        Dimension size = null;
        if (ui != null) {
            size = ui.getPreferredSize(this);
        }
        return (size != null) ? size : super.getPreferredSize();
    }


    /**
     * Sets the maximum size of this component to a constant
     * value.  Subsequent calls to <code>getMaximumSize</code> will always
     * return this value; the component's UI will not be asked
     * to compute it.  Setting the maximum size to <code>null</code>
     * restores the default behavior.
     *
     * @param maximumSize a <code>Dimension</code> containing the
     *          desired maximum allowable size
     * @see #getMaximumSize
     */
    @BeanProperty(description
            = "The maximum size of the component.")
    public void setMaximumSize(Dimension maximumSize) {
        super.setMaximumSize(maximumSize);
    }


    /**
     * If the maximum size has been set to a non-<code>null</code> value
     * just returns it.  If the UI delegate's <code>getMaximumSize</code>
     * method returns a non-<code>null</code> value then return that;
     * otherwise defer to the component's layout manager.
     *
     * @return the value of the <code>maximumSize</code> property
     * @see #setMaximumSize
     * @see ComponentUI
     */
    @Transient
    public Dimension getMaximumSize() {
        if (isMaximumSizeSet()) {
            return super.getMaximumSize();
        }
        Dimension size = null;
        if (ui != null) {
            size = ui.getMaximumSize(this);
        }
        return (size != null) ? size : super.getMaximumSize();
    }


    /**
     * Sets the minimum size of this component to a constant
     * value.  Subsequent calls to <code>getMinimumSize</code> will always
     * return this value; the component's UI will not be asked
     * to compute it.  Setting the minimum size to <code>null</code>
     * restores the default behavior.
     *
     * @param minimumSize the new minimum size of this component
     * @see #getMinimumSize
     */
    @BeanProperty(description
            = "The minimum size of the component.")
    public void setMinimumSize(Dimension minimumSize) {
        super.setMinimumSize(minimumSize);
    }

    /**
     * If the minimum size has been set to a non-<code>null</code> value
     * just returns it.  If the UI delegate's <code>getMinimumSize</code>
     * method returns a non-<code>null</code> value then return that; otherwise
     * defer to the component's layout manager.
     *
     * @return the value of the <code>minimumSize</code> property
     * @see #setMinimumSize
     * @see ComponentUI
     */
    @Transient
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet()) {
            return super.getMinimumSize();
        }
        Dimension size = null;
        if (ui != null) {
            size = ui.getMinimumSize(this);
        }
        return (size != null) ? size : super.getMinimumSize();
    }

    /**
     * Gives the UI delegate an opportunity to define the precise
     * shape of this component for the sake of mouse processing.
     *
     * @return true if this component logically contains x,y
     * @see java.awt.Component#contains(int, int)
     * @see ComponentUI
     */
    public boolean contains(int x, int y) {
        return (ui != null) ? ui.contains(this, x, y) : super.contains(x, y);
    }

    /**
     * Sets the border of this component.  The <code>Border</code> object is
     * responsible for defining the insets for the component
     * (overriding any insets set directly on the component) and
     * for optionally rendering any border decorations within the
     * bounds of those insets.  Borders should be used (rather
     * than insets) for creating both decorative and non-decorative
     * (such as margins and padding) regions for a swing component.
     * Compound borders can be used to nest multiple borders within a
     * single component.
     * <p>
     * Although technically you can set the border on any object
     * that inherits from <code>JComponent</code>, the look and
     * feel implementation of many standard Swing components
     * doesn't work well with user-set borders.  In general,
     * when you want to set a border on a standard Swing
     * component other than <code>JPanel</code> or <code>JLabel</code>,
     * we recommend that you put the component in a <code>JPanel</code>
     * and set the border on the <code>JPanel</code>.
     * <p>
     * This is a bound property.
     *
     * @param border the border to be rendered for this component
     * @see Border
     * @see CompoundBorder
     */
    @BeanProperty(preferred = true, visualUpdate = true, description
            = "The component's border.")
    public void setBorder(Border border) {
        Border         oldBorder = this.border;

        this.border = border;
        firePropertyChange("border", oldBorder, border);
        if (border != oldBorder) {
            if (border == null || oldBorder == null ||
                !(border.getBorderInsets(this).equals(oldBorder.getBorderInsets(this)))) {
                revalidate();
            }
            repaint();
        }
    }

    /**
     * Returns the border of this component or <code>null</code> if no
     * border is currently set.
     *
     * @return the border object for this component
     * @see #setBorder
     */
    public Border getBorder() {
        return border;
    }

    /**
     * If a border has been set on this component, returns the
     * border's insets; otherwise calls <code>super.getInsets</code>.
     *
     * @return the value of the insets property
     * @see #setBorder
     */
    @BeanProperty(expert = true)
    public Insets getInsets() {
        if (border != null) {
            return border.getBorderInsets(this);
        }
        return super.getInsets();
    }

    /**
     * Returns an <code>Insets</code> object containing this component's inset
     * values.  The passed-in <code>Insets</code> object will be reused
     * if possible.
     * Calling methods cannot assume that the same object will be returned,
     * however.  All existing values within this object are overwritten.
     * If <code>insets</code> is null, this will allocate a new one.
     *
     * @param insets the <code>Insets</code> object, which can be reused
     * @return the <code>Insets</code> object
     * @see #getInsets
     */
    public Insets getInsets(Insets insets) {
        if (insets == null) {
            insets = new Insets(0, 0, 0, 0);
        }
        if (border != null) {
            if (border instanceof AbstractBorder) {
                return ((AbstractBorder)border).getBorderInsets(this, insets);
            } else {
                // Can't reuse border insets because the Border interface
                // can't be enhanced.
                return border.getBorderInsets(this);
            }
        } else {
            // super.getInsets() always returns an Insets object with
            // all of its value zeroed.  No need for a new object here.
            insets.left = insets.top = insets.right = insets.bottom = 0;
            return insets;
        }
    }

    /**
     * Overrides <code>Container.getAlignmentY</code> to return
     * the vertical alignment.
     *
     * @return the value of the <code>alignmentY</code> property
     * @see #setAlignmentY
     * @see java.awt.Component#getAlignmentY
     */
    public float getAlignmentY() {
        if (isAlignmentYSet) {
            return alignmentY;
        }
        return super.getAlignmentY();
    }

    /**
     * Sets the vertical alignment.
     *
     * @param alignmentY  the new vertical alignment
     * @see #getAlignmentY
     */
    @BeanProperty(description
            = "The preferred vertical alignment of the component.")
    public void setAlignmentY(float alignmentY) {
        this.alignmentY = validateAlignment(alignmentY);
        isAlignmentYSet = true;
    }


    /**
     * Overrides <code>Container.getAlignmentX</code> to return
     * the horizontal alignment.
     *
     * @return the value of the <code>alignmentX</code> property
     * @see #setAlignmentX
     * @see java.awt.Component#getAlignmentX
     */
    public float getAlignmentX() {
        if (isAlignmentXSet) {
            return alignmentX;
        }
        return super.getAlignmentX();
    }

    /**
     * Sets the horizontal alignment.
     *
     * @param alignmentX  the new horizontal alignment
     * @see #getAlignmentX
     */
    @BeanProperty(description
            = "The preferred horizontal alignment of the component.")
    public void setAlignmentX(float alignmentX) {
        this.alignmentX = validateAlignment(alignmentX);
        isAlignmentXSet = true;
    }

    private float validateAlignment(float alignment) {
        return alignment > 1.0f ? 1.0f : alignment < 0.0f ? 0.0f : alignment;
    }

    /**
     * Sets the input verifier for this component.
     *
     * @param inputVerifier the new input verifier
     * @since 1.3
     * @see InputVerifier
     */
    @BeanProperty(description
            = "The component's input verifier.")
    public void setInputVerifier(InputVerifier inputVerifier) {
        InputVerifier oldInputVerifier = (InputVerifier)getClientProperty(
                                         JComponent_INPUT_VERIFIER);
        putClientProperty(JComponent_INPUT_VERIFIER, inputVerifier);
        firePropertyChange("inputVerifier", oldInputVerifier, inputVerifier);
    }

    /**
     * Returns the input verifier for this component.
     *
     * @return the <code>inputVerifier</code> property
     * @since 1.3
     * @see InputVerifier
     */
    public InputVerifier getInputVerifier() {
        return (InputVerifier)getClientProperty(JComponent_INPUT_VERIFIER);
    }

    /**
     * Returns this component's graphics context, which lets you draw
     * on a component. Use this method to get a <code>Graphics</code> object and
     * then invoke operations on that object to draw on the component.
     * @return this components graphics context
     */
    @BeanProperty(bound = false)
    public Graphics getGraphics() {
        if (DEBUG_GRAPHICS_LOADED && shouldDebugGraphics() != 0) {
            DebugGraphics graphics = new DebugGraphics(super.getGraphics(),
                                                       this);
            return graphics;
        }
        return super.getGraphics();
    }


    /** Enables or disables diagnostic information about every graphics
      * operation performed within the component or one of its children.
      *
      * @param debugOptions  determines how the component should display
      *         the information;  one of the following options:
      * <ul>
      * <li>DebugGraphics.LOG_OPTION - causes a text message to be printed.
      * <li>DebugGraphics.FLASH_OPTION - causes the drawing to flash several
      * times.
      * <li>DebugGraphics.BUFFERED_OPTION - creates an
      *         <code>ExternalWindow</code> that displays the operations
      *         performed on the View's offscreen buffer.
      * <li>DebugGraphics.NONE_OPTION disables debugging.
      * <li>A value of 0 causes no changes to the debugging options.
      * </ul>
      * <code>debugOptions</code> is bitwise OR'd into the current value
      */
    @BeanProperty(bound = false, preferred = true, enumerationValues = {
            "DebugGraphics.NONE_OPTION",
            "DebugGraphics.LOG_OPTION",
            "DebugGraphics.FLASH_OPTION",
            "DebugGraphics.BUFFERED_OPTION"}, description
            = "Diagnostic options for graphics operations.")
    public void setDebugGraphicsOptions(int debugOptions) {
        DebugGraphics.setDebugOptions(this, debugOptions);
    }

    /** Returns the state of graphics debugging.
      *
      * @return a bitwise OR'd flag of zero or more of the following options:
      * <ul>
      * <li>DebugGraphics.LOG_OPTION - causes a text message to be printed.
      * <li>DebugGraphics.FLASH_OPTION - causes the drawing to flash several
      * times.
      * <li>DebugGraphics.BUFFERED_OPTION - creates an
      *         <code>ExternalWindow</code> that displays the operations
      *         performed on the View's offscreen buffer.
      * <li>DebugGraphics.NONE_OPTION disables debugging.
      * <li>A value of 0 causes no changes to the debugging options.
      * </ul>
      * @see #setDebugGraphicsOptions
      */
    public int getDebugGraphicsOptions() {
        return DebugGraphics.getDebugOptions(this);
    }


    /**
     * Returns true if debug information is enabled for this
     * <code>JComponent</code> or one of its parents.
     */
    int shouldDebugGraphics() {
        return DebugGraphics.shouldComponentDebug(this);
    }

    /**
     * This method is now obsolete, please use a combination of
     * <code>getActionMap()</code> and <code>getInputMap()</code> for
     * similar behavior. For example, to bind the <code>KeyStroke</code>
     * <code>aKeyStroke</code> to the <code>Action</code> <code>anAction</code>
     * now use:
     * <pre>
     *   component.getInputMap().put(aKeyStroke, aCommand);
     *   component.getActionMap().put(aCommmand, anAction);
     * </pre>
     * The above assumes you want the binding to be applicable for
     * <code>WHEN_FOCUSED</code>. To register bindings for other focus
     * states use the <code>getInputMap</code> method that takes an integer.
     * <p>
     * Register a new keyboard action.
     * <code>anAction</code> will be invoked if a key event matching
     * <code>aKeyStroke</code> occurs and <code>aCondition</code> is verified.
     * The <code>KeyStroke</code> object defines a
     * particular combination of a keyboard key and one or more modifiers
     * (alt, shift, ctrl, meta).
     * <p>
     * The <code>aCommand</code> will be set in the delivered event if
     * specified.
     * <p>
     * The <code>aCondition</code> can be one of:
     * <blockquote>
     * <DL>
     * <DT>WHEN_FOCUSED
     * <DD>The action will be invoked only when the keystroke occurs
     *     while the component has the focus.
     * <DT>WHEN_IN_FOCUSED_WINDOW
     * <DD>The action will be invoked when the keystroke occurs while
     *     the component has the focus or if the component is in the
     *     window that has the focus. Note that the component need not
     *     be an immediate descendent of the window -- it can be
     *     anywhere in the window's containment hierarchy. In other
     *     words, whenever <em>any</em> component in the window has the focus,
     *     the action registered with this component is invoked.
     * <DT>WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
     * <DD>The action will be invoked when the keystroke occurs while the
     *     component has the focus or if the component is an ancestor of
     *     the component that has the focus.
     * </DL>
     * </blockquote>
     * <p>
     * The combination of keystrokes and conditions lets you define high
     * level (semantic) action events for a specified keystroke+modifier
     * combination (using the KeyStroke class) and direct to a parent or
     * child of a component that has the focus, or to the component itself.
     * In other words, in any hierarchical structure of components, an
     * arbitrary key-combination can be immediately directed to the
     * appropriate component in the hierarchy, and cause a specific method
     * to be invoked (usually by way of adapter objects).
     * <p>
     * If an action has already been registered for the receiving
     * container, with the same charCode and the same modifiers,
     * <code>anAction</code> will replace the action.
     *
     * @param anAction  the <code>Action</code> to be registered
     * @param aCommand  the command to be set in the delivered event
     * @param aKeyStroke the <code>KeyStroke</code> to bind to the action
     * @param aCondition the condition that needs to be met, see above
     * @see KeyStroke
     */
    public void registerKeyboardAction(ActionListener anAction,String aCommand,KeyStroke aKeyStroke,int aCondition) {

        InputMap inputMap = getInputMap(aCondition, true);

        if (inputMap != null) {
            ActionMap actionMap = getActionMap(true);
            ActionStandin action = new ActionStandin(anAction, aCommand);
            inputMap.put(aKeyStroke, action);
            if (actionMap != null) {
                actionMap.put(action, action);
            }
        }
    }

    /**
     * Registers any bound <code>WHEN_IN_FOCUSED_WINDOW</code> actions with
     * the <code>KeyboardManager</code>. If <code>onlyIfNew</code>
     * is true only actions that haven't been registered are pushed
     * to the <code>KeyboardManager</code>;
     * otherwise all actions are pushed to the <code>KeyboardManager</code>.
     *
     * @param onlyIfNew  if true, only actions that haven't been registered
     *          are pushed to the <code>KeyboardManager</code>
     */
    private void registerWithKeyboardManager(boolean onlyIfNew) {
        InputMap inputMap = getInputMap(WHEN_IN_FOCUSED_WINDOW, false);
        KeyStroke[] strokes;
        @SuppressWarnings("unchecked")
        Hashtable<KeyStroke, KeyStroke> registered =
                (Hashtable<KeyStroke, KeyStroke>)getClientProperty
                                (WHEN_IN_FOCUSED_WINDOW_BINDINGS);

        if (inputMap != null) {
            // Push any new KeyStrokes to the KeyboardManager.
            strokes = inputMap.allKeys();
            if (strokes != null) {
                for (int counter = strokes.length - 1; counter >= 0;
                     counter--) {
                    if (!onlyIfNew || registered == null ||
                        registered.get(strokes[counter]) == null) {
                        registerWithKeyboardManager(strokes[counter]);
                    }
                    if (registered != null) {
                        registered.remove(strokes[counter]);
                    }
                }
            }
        }
        else {
            strokes = null;
        }
        // Remove any old ones.
        if (registered != null && registered.size() > 0) {
            Enumeration<KeyStroke> keys = registered.keys();

            while (keys.hasMoreElements()) {
                KeyStroke ks = keys.nextElement();
                unregisterWithKeyboardManager(ks);
            }
            registered.clear();
        }
        // Updated the registered Hashtable.
        if (strokes != null && strokes.length > 0) {
            if (registered == null) {
                registered = new Hashtable<KeyStroke, KeyStroke>(strokes.length);
                putClientProperty(WHEN_IN_FOCUSED_WINDOW_BINDINGS, registered);
            }
            for (int counter = strokes.length - 1; counter >= 0; counter--) {
                registered.put(strokes[counter], strokes[counter]);
            }
        }
        else {
            putClientProperty(WHEN_IN_FOCUSED_WINDOW_BINDINGS, null);
        }
    }

    /**
     * Unregisters all the previously registered
     * <code>WHEN_IN_FOCUSED_WINDOW</code> <code>KeyStroke</code> bindings.
     */
    private void unregisterWithKeyboardManager() {
        @SuppressWarnings("unchecked")
        Hashtable<KeyStroke, KeyStroke> registered =
                (Hashtable<KeyStroke, KeyStroke>)getClientProperty
                                (WHEN_IN_FOCUSED_WINDOW_BINDINGS);

        if (registered != null && registered.size() > 0) {
            Enumeration<KeyStroke> keys = registered.keys();

            while (keys.hasMoreElements()) {
                KeyStroke ks = keys.nextElement();
                unregisterWithKeyboardManager(ks);
            }
        }
        putClientProperty(WHEN_IN_FOCUSED_WINDOW_BINDINGS, null);
    }

    /**
     * Invoked from <code>ComponentInputMap</code> when its bindings change.
     * If <code>inputMap</code> is the current <code>windowInputMap</code>
     * (or a parent of the window <code>InputMap</code>)
     * the <code>KeyboardManager</code> is notified of the new bindings.
     *
     * @param inputMap the map containing the new bindings
     */
    void componentInputMapChanged(ComponentInputMap inputMap) {
        InputMap km = getInputMap(WHEN_IN_FOCUSED_WINDOW, false);

        while (km != inputMap && km != null) {
            km = km.getParent();
        }
        if (km != null) {
            registerWithKeyboardManager(false);
        }
    }

    private void registerWithKeyboardManager(KeyStroke aKeyStroke) {
        KeyboardManager.getCurrentManager().registerKeyStroke(aKeyStroke,this);
    }

    private void unregisterWithKeyboardManager(KeyStroke aKeyStroke) {
        KeyboardManager.getCurrentManager().unregisterKeyStroke(aKeyStroke,
                                                                this);
    }

    /**
     * This method is now obsolete, please use a combination of
     * <code>getActionMap()</code> and <code>getInputMap()</code> for
     * similar behavior.
     *
     * @param anAction  action to be registered to given keystroke and condition
     * @param aKeyStroke  a {@code KeyStroke}
     * @param aCondition  the condition to be associated with given keystroke
     *                    and action
     * @see #getActionMap
     * @see #getInputMap(int)
     */
    public void registerKeyboardAction(ActionListener anAction,KeyStroke aKeyStroke,int aCondition) {
        registerKeyboardAction(anAction,null,aKeyStroke,aCondition);
    }

    /**
     * This method is now obsolete. To unregister an existing binding
     * you can either remove the binding from the
     * <code>ActionMap/InputMap</code>, or place a dummy binding the
     * <code>InputMap</code>. Removing the binding from the
     * <code>InputMap</code> allows bindings in parent <code>InputMap</code>s
     * to be active, whereas putting a dummy binding in the
     * <code>InputMap</code> effectively disables
     * the binding from ever happening.
     * <p>
     * Unregisters a keyboard action.
     * This will remove the binding from the <code>ActionMap</code>
     * (if it exists) as well as the <code>InputMap</code>s.
     *
     * @param aKeyStroke  the keystroke for which to unregister its
     *                    keyboard action
     */
    public void unregisterKeyboardAction(KeyStroke aKeyStroke) {
        ActionMap am = getActionMap(false);
        for (int counter = 0; counter < 3; counter++) {
            InputMap km = getInputMap(counter, false);
            if (km != null) {
                Object actionID = km.get(aKeyStroke);

                if (am != null && actionID != null) {
                    am.remove(actionID);
                }
                km.remove(aKeyStroke);
            }
        }
    }

    /**
     * Returns the <code>KeyStrokes</code> that will initiate
     * registered actions.
     *
     * @return an array of <code>KeyStroke</code> objects
     * @see #registerKeyboardAction
     */
    @BeanProperty(bound = false)
    public KeyStroke[] getRegisteredKeyStrokes() {
        int[] counts = new int[3];
        KeyStroke[][] strokes = new KeyStroke[3][];

        for (int counter = 0; counter < 3; counter++) {
            InputMap km = getInputMap(counter, false);
            strokes[counter] = (km != null) ? km.allKeys() : null;
            counts[counter] = (strokes[counter] != null) ?
                               strokes[counter].length : 0;
        }
        KeyStroke[] retValue = new KeyStroke[counts[0] + counts[1] +
                                            counts[2]];
        for (int counter = 0, last = 0; counter < 3; counter++) {
            if (counts[counter] > 0) {
                System.arraycopy(strokes[counter], 0, retValue, last,
                                 counts[counter]);
                last += counts[counter];
            }
        }
        return retValue;
    }

    /**
     * Returns the condition that determines whether a registered action
     * occurs in response to the specified keystroke.
     * <p>
     * For Java 2 platform v1.3, a <code>KeyStroke</code> can be associated
     * with more than one condition.
     * For example, 'a' could be bound for the two
     * conditions <code>WHEN_FOCUSED</code> and
     * <code>WHEN_IN_FOCUSED_WINDOW</code> condition.
     *
     * @param aKeyStroke  the keystroke for which to request an
     *                    action-keystroke condition
     * @return the action-keystroke condition
     */
    public int getConditionForKeyStroke(KeyStroke aKeyStroke) {
        for (int counter = 0; counter < 3; counter++) {
            InputMap inputMap = getInputMap(counter, false);
            if (inputMap != null && inputMap.get(aKeyStroke) != null) {
                return counter;
            }
        }
        return UNDEFINED_CONDITION;
    }

    /**
     * Returns the object that will perform the action registered for a
     * given keystroke.
     *
     * @param aKeyStroke  the keystroke for which to return a listener
     * @return the <code>ActionListener</code>
     *          object invoked when the keystroke occurs
     */
    public ActionListener getActionForKeyStroke(KeyStroke aKeyStroke) {
        ActionMap am = getActionMap(false);

        if (am == null) {
            return null;
        }
        for (int counter = 0; counter < 3; counter++) {
            InputMap inputMap = getInputMap(counter, false);
            if (inputMap != null) {
                Object actionBinding = inputMap.get(aKeyStroke);

                if (actionBinding != null) {
                    Action action = am.get(actionBinding);
                    if (action instanceof ActionStandin) {
                        return ((ActionStandin)action).actionListener;
                    }
                    return action;
                }
            }
        }
        return null;
    }

    /**
     * Unregisters all the bindings in the first tier <code>InputMaps</code>
     * and <code>ActionMap</code>. This has the effect of removing any
     * local bindings, and allowing the bindings defined in parent
     * <code>InputMap/ActionMaps</code>
     * (the UI is usually defined in the second tier) to persist.
     */
    public void resetKeyboardActions() {
        // Keys
        for (int counter = 0; counter < 3; counter++) {
            InputMap inputMap = getInputMap(counter, false);

            if (inputMap != null) {
                inputMap.clear();
            }
        }

        // Actions
        ActionMap am = getActionMap(false);

        if (am != null) {
            am.clear();
        }
    }

    /**
     * Sets the <code>InputMap</code> to use under the condition
     * <code>condition</code> to
     * <code>map</code>. A <code>null</code> value implies you
     * do not want any bindings to be used, even from the UI. This will
     * not reinstall the UI <code>InputMap</code> (if there was one).
     * <code>condition</code> has one of the following values:
     * <ul>
     * <li><code>WHEN_IN_FOCUSED_WINDOW</code>
     * <li><code>WHEN_FOCUSED</code>
     * <li><code>WHEN_ANCESTOR_OF_FOCUSED_COMPONENT</code>
     * </ul>
     * If <code>condition</code> is <code>WHEN_IN_FOCUSED_WINDOW</code>
     * and <code>map</code> is not a <code>ComponentInputMap</code>, an
     * <code>IllegalArgumentException</code> will be thrown.
     * Similarly, if <code>condition</code> is not one of the values
     * listed, an <code>IllegalArgumentException</code> will be thrown.
     *
     * @param condition one of the values listed above
     * @param map  the <code>InputMap</code> to use for the given condition
     * @exception IllegalArgumentException if <code>condition</code> is
     *          <code>WHEN_IN_FOCUSED_WINDOW</code> and <code>map</code>
     *          is not an instance of <code>ComponentInputMap</code>; or
     *          if <code>condition</code> is not one of the legal values
     *          specified above
     * @since 1.3
     */
    public final void setInputMap(int condition, InputMap map) {
        switch (condition) {
        case WHEN_IN_FOCUSED_WINDOW:
            if (map != null && !(map instanceof ComponentInputMap)) {
                throw new IllegalArgumentException("WHEN_IN_FOCUSED_WINDOW InputMaps must be of type ComponentInputMap");
            }
            windowInputMap = (ComponentInputMap)map;
            setFlag(WIF_INPUTMAP_CREATED, true);
            registerWithKeyboardManager(false);
            break;
        case WHEN_ANCESTOR_OF_FOCUSED_COMPONENT:
            ancestorInputMap = map;
            setFlag(ANCESTOR_INPUTMAP_CREATED, true);
            break;
        case WHEN_FOCUSED:
            focusInputMap = map;
            setFlag(FOCUS_INPUTMAP_CREATED, true);
            break;
        default:
            throw new IllegalArgumentException("condition must be one of JComponent.WHEN_IN_FOCUSED_WINDOW, JComponent.WHEN_FOCUSED or JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT");
        }
    }

    /**
     * Returns the <code>InputMap</code> that is used during
     * <code>condition</code>.
     *
     * @param condition one of WHEN_IN_FOCUSED_WINDOW, WHEN_FOCUSED,
     *        WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
     * @return the <code>InputMap</code> for the specified
     *          <code>condition</code>
     * @since 1.3
     */
    public final InputMap getInputMap(int condition) {
        return getInputMap(condition, true);
    }

    /**
     * Returns the <code>InputMap</code> that is used when the
     * component has focus.
     * This is convenience method for <code>getInputMap(WHEN_FOCUSED)</code>.
     *
     * @return the <code>InputMap</code> used when the component has focus
     * @since 1.3
     */
    public final InputMap getInputMap() {
        return getInputMap(WHEN_FOCUSED, true);
    }

    /**
     * Sets the <code>ActionMap</code> to <code>am</code>. This does not set
     * the parent of the <code>am</code> to be the <code>ActionMap</code>
     * from the UI (if there was one), it is up to the caller to have done this.
     *
     * @param am  the new <code>ActionMap</code>
     * @since 1.3
     */
    public final void setActionMap(ActionMap am) {
        actionMap = am;
        setFlag(ACTIONMAP_CREATED, true);
    }

    /**
     * Returns the <code>ActionMap</code> used to determine what
     * <code>Action</code> to fire for particular <code>KeyStroke</code>
     * binding. The returned <code>ActionMap</code>, unless otherwise
     * set, will have the <code>ActionMap</code> from the UI set as the parent.
     *
     * @return the <code>ActionMap</code> containing the key/action bindings
     * @since 1.3
     */
    public final ActionMap getActionMap() {
        return getActionMap(true);
    }

    /**
     * Returns the <code>InputMap</code> to use for condition
     * <code>condition</code>.  If the <code>InputMap</code> hasn't
     * been created, and <code>create</code> is
     * true, it will be created.
     *
     * @param condition one of the following values:
     * <ul>
     * <li>JComponent.FOCUS_INPUTMAP_CREATED
     * <li>JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
     * <li>JComponent.WHEN_IN_FOCUSED_WINDOW
     * </ul>
     * @param create if true, create the <code>InputMap</code> if it
     *          is not already created
     * @return the <code>InputMap</code> for the given <code>condition</code>;
     *          if <code>create</code> is false and the <code>InputMap</code>
     *          hasn't been created, returns <code>null</code>
     * @exception IllegalArgumentException if <code>condition</code>
     *          is not one of the legal values listed above
     */
    final InputMap getInputMap(int condition, boolean create) {
        switch (condition) {
        case WHEN_FOCUSED:
            if (getFlag(FOCUS_INPUTMAP_CREATED)) {
                return focusInputMap;
            }
            // Hasn't been created yet.
            if (create) {
                InputMap km = new InputMap();
                setInputMap(condition, km);
                return km;
            }
            break;
        case WHEN_ANCESTOR_OF_FOCUSED_COMPONENT:
            if (getFlag(ANCESTOR_INPUTMAP_CREATED)) {
                return ancestorInputMap;
            }
            // Hasn't been created yet.
            if (create) {
                InputMap km = new InputMap();
                setInputMap(condition, km);
                return km;
            }
            break;
        case WHEN_IN_FOCUSED_WINDOW:
            if (getFlag(WIF_INPUTMAP_CREATED)) {
                return windowInputMap;
            }
            // Hasn't been created yet.
            if (create) {
                ComponentInputMap km = new ComponentInputMap(this);
                setInputMap(condition, km);
                return km;
            }
            break;
        default:
            throw new IllegalArgumentException("condition must be one of JComponent.WHEN_IN_FOCUSED_WINDOW, JComponent.WHEN_FOCUSED or JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT");
        }
        return null;
    }

    /**
     * Finds and returns the appropriate <code>ActionMap</code>.
     *
     * @param create if true, create the <code>ActionMap</code> if it
     *          is not already created
     * @return the <code>ActionMap</code> for this component; if the
     *          <code>create</code> flag is false and there is no
     *          current <code>ActionMap</code>, returns <code>null</code>
     */
    final ActionMap getActionMap(boolean create) {
        if (getFlag(ACTIONMAP_CREATED)) {
            return actionMap;
        }
        // Hasn't been created.
        if (create) {
            ActionMap am = new ActionMap();
            setActionMap(am);
            return am;
        }
        return null;
    }

    /**
     * Returns the baseline.  The baseline is measured from the top of
     * the component.  This method is primarily meant for
     * <code>LayoutManager</code>s to align components along their
     * baseline.  A return value less than 0 indicates this component
     * does not have a reasonable baseline and that
     * <code>LayoutManager</code>s should not align this component on
     * its baseline.
     * <p>
     * This method calls into the <code>ComponentUI</code> method of the
     * same name.  If this component does not have a <code>ComponentUI</code>
     * -1 will be returned.  If a value &gt;= 0 is
     * returned, then the component has a valid baseline for any
     * size &gt;= the minimum size and <code>getBaselineResizeBehavior</code>
     * can be used to determine how the baseline changes with size.
     *
     * @throws IllegalArgumentException {@inheritDoc}
     * @see #getBaselineResizeBehavior
     * @see java.awt.FontMetrics
     * @since 1.6
     */
    public int getBaseline(int width, int height) {
        // check size.
        super.getBaseline(width, height);
        if (ui != null) {
            return ui.getBaseline(this, width, height);
        }
        return -1;
    }

    /**
     * Returns an enum indicating how the baseline of the component
     * changes as the size changes.  This method is primarily meant for
     * layout managers and GUI builders.
     * <p>
     * This method calls into the <code>ComponentUI</code> method of
     * the same name.  If this component does not have a
     * <code>ComponentUI</code>
     * <code>BaselineResizeBehavior.OTHER</code> will be
     * returned.  Subclasses should
     * never return <code>null</code>; if the baseline can not be
     * calculated return <code>BaselineResizeBehavior.OTHER</code>.  Callers
     * should first ask for the baseline using
     * <code>getBaseline</code> and if a value &gt;= 0 is returned use
     * this method.  It is acceptable for this method to return a
     * value other than <code>BaselineResizeBehavior.OTHER</code> even if
     * <code>getBaseline</code> returns a value less than 0.
     *
     * @see #getBaseline(int, int)
     * @since 1.6
     */
    @BeanProperty(bound = false)
    public BaselineResizeBehavior getBaselineResizeBehavior() {
        if (ui != null) {
            return ui.getBaselineResizeBehavior(this);
        }
        return BaselineResizeBehavior.OTHER;
    }

    /**
     * In release 1.4, the focus subsystem was rearchitected.
     * For more information, see
     * <a href="https://docs.oracle.com/javase/tutorial/uiswing/misc/focus.html">
     * How to Use the Focus Subsystem</a>,
     * a section in <em>The Java Tutorial</em>.
     * <p>
     * Requests focus on this <code>JComponent</code>'s
     * <code>FocusTraversalPolicy</code>'s default <code>Component</code>.
     * If this <code>JComponent</code> is a focus cycle root, then its
     * <code>FocusTraversalPolicy</code> is used. Otherwise, the
     * <code>FocusTraversalPolicy</code> of this <code>JComponent</code>'s
     * focus-cycle-root ancestor is used.
     *
     * @return true if this component can request to get the input focus,
     *              false if it can not
     * @see java.awt.FocusTraversalPolicy#getDefaultComponent
     * @deprecated As of 1.4, replaced by
     * <code>FocusTraversalPolicy.getDefaultComponent(Container).requestFocus()</code>
     */
    @Deprecated
    public boolean requestDefaultFocus() {
        Container nearestRoot =
            (isFocusCycleRoot()) ? this : getFocusCycleRootAncestor();
        if (nearestRoot == null) {
            return false;
        }
        Component comp = nearestRoot.getFocusTraversalPolicy().
            getDefaultComponent(nearestRoot);
        if (comp != null) {
            comp.requestFocus();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Makes the component visible or invisible.
     * Overrides <code>Component.setVisible</code>.
     *
     * @param aFlag  true to make the component visible; false to
     *          make it invisible
     */
    @BeanProperty(hidden = true, visualUpdate = true)
    public void setVisible(boolean aFlag) {
        if (aFlag != isVisible()) {
            super.setVisible(aFlag);
            if (aFlag) {
                Container parent = getParent();
                if (parent != null) {
                    Rectangle r = getBounds();
                    parent.repaint(r.x, r.y, r.width, r.height);
                }
                revalidate();
            }
        }
    }

    /**
     * Sets whether or not this component is enabled.
     * A component that is enabled may respond to user input,
     * while a component that is not enabled cannot respond to
     * user input.  Some components may alter their visual
     * representation when they are disabled in order to
     * provide feedback to the user that they cannot take input.
     * <p>Note: Disabling a component does not disable its children.
     *
     * <p>Note: Disabling a lightweight component does not prevent it from
     * receiving MouseEvents.
     *
     * @param enabled true if this component should be enabled, false otherwise
     * @see java.awt.Component#isEnabled
     * @see java.awt.Component#isLightweight
     */
    @BeanProperty(expert = true, preferred = true, visualUpdate = true, description
            = "The enabled state of the component.")
    public void setEnabled(boolean enabled) {
        boolean oldEnabled = isEnabled();
        super.setEnabled(enabled);
        firePropertyChange("enabled", oldEnabled, enabled);
        if (enabled != oldEnabled) {
            repaint();
        }
    }

    /**
     * Sets the foreground color of this component.  It is up to the
     * look and feel to honor this property, some may choose to ignore
     * it.
     *
     * @param fg  the desired foreground <code>Color</code>
     * @see java.awt.Component#getForeground
     */
    @BeanProperty(preferred = true, visualUpdate = true, description
            = "The foreground color of the component.")
    public void setForeground(Color fg) {
        Color oldFg = getForeground();
        super.setForeground(fg);
        if ((oldFg != null) ? !oldFg.equals(fg) : ((fg != null) && !fg.equals(oldFg))) {
            // foreground already bound in AWT1.2
            repaint();
        }
    }

    /**
     * Sets the background color of this component.  The background
     * color is used only if the component is opaque, and only
     * by subclasses of <code>JComponent</code> or
     * <code>ComponentUI</code> implementations.  Direct subclasses of
     * <code>JComponent</code> must override
     * <code>paintComponent</code> to honor this property.
     * <p>
     * It is up to the look and feel to honor this property, some may
     * choose to ignore it.
     *
     * @param bg the desired background <code>Color</code>
     * @see java.awt.Component#getBackground
     * @see #setOpaque
     */
    @BeanProperty(preferred = true, visualUpdate = true, description
            = "The background color of the component.")
    public void setBackground(Color bg) {
        Color oldBg = getBackground();
        super.setBackground(bg);
        if ((oldBg != null) ? !oldBg.equals(bg) : ((bg != null) && !bg.equals(oldBg))) {
            // background already bound in AWT1.2
            repaint();
        }
    }

    /**
     * Sets the font for this component.
     *
     * @param font the desired <code>Font</code> for this component
     * @see java.awt.Component#getFont
     */
    @BeanProperty(preferred = true, visualUpdate = true, description
            = "The font for the component.")
    public void setFont(Font font) {
        Font oldFont = getFont();
        super.setFont(font);
        // font already bound in AWT1.2
        if (font != oldFont) {
            revalidate();
            repaint();
        }
    }

    /**
     * Returns the default locale used to initialize each JComponent's
     * locale property upon creation.
     *
     * The default locale has "AppContext" scope so that applets (and
     * potentially multiple lightweight applications running in a single VM)
     * can have their own setting. An applet can safely alter its default
     * locale because it will have no affect on other applets (or the browser).
     *
     * @return the default <code>Locale</code>.
     * @see #setDefaultLocale
     * @see java.awt.Component#getLocale
     * @see #setLocale
     * @since 1.4
     */
    public static Locale getDefaultLocale() {
        Locale l = (Locale) SwingUtilities.appContextGet(defaultLocale);
        if( l == null ) {
            //REMIND(bcb) choosing the default value is more complicated
            //than this.
            l = Locale.getDefault();
            JComponent.setDefaultLocale( l );
        }
        return l;
    }


    /**
     * Sets the default locale used to initialize each JComponent's locale
     * property upon creation.  The initial value is the VM's default locale.
     *
     * The default locale has "AppContext" scope so that applets (and
     * potentially multiple lightweight applications running in a single VM)
     * can have their own setting. An applet can safely alter its default
     * locale because it will have no affect on other applets (or the browser).
     * Passing {@code null} will reset the current locale back
     * to VM's default locale.
     *
     * @param l the desired default <code>Locale</code> for new components.
     * @see #getDefaultLocale
     * @see java.awt.Component#getLocale
     * @see #setLocale
     * @since 1.4
     */
    public static void setDefaultLocale( Locale l ) {
        SwingUtilities.appContextPut(defaultLocale, l);
    }


    /**
     * Processes any key events that the component itself
     * recognizes.  This is called after the focus
     * manager and any interested listeners have been
     * given a chance to steal away the event.  This
     * method is called only if the event has not
     * yet been consumed.  This method is called prior
     * to the keyboard UI logic.
     * <p>
     * This method is implemented to do nothing.  Subclasses would
     * normally override this method if they process some
     * key events themselves.  If the event is processed,
     * it should be consumed.
     *
     * @param e the event to be processed
     */
    protected void processComponentKeyEvent(KeyEvent e) {
    }

    /** Overrides <code>processKeyEvent</code> to process events. **/
    protected void processKeyEvent(KeyEvent e) {
      boolean result;
      boolean shouldProcessKey;

      // This gives the key event listeners a crack at the event
      super.processKeyEvent(e);

      // give the component itself a crack at the event
      if (! e.isConsumed()) {
          processComponentKeyEvent(e);
      }

      shouldProcessKey = KeyboardState.shouldProcess(e);

      if(e.isConsumed()) {
        return;
      }

      if (shouldProcessKey && processKeyBindings(e, e.getID() ==
                                                 KeyEvent.KEY_PRESSED)) {
          e.consume();
      }
    }

    /**
     * Invoked to process the key bindings for <code>ks</code> as the result
     * of the <code>KeyEvent</code> <code>e</code>. This obtains
     * the appropriate <code>InputMap</code>,
     * gets the binding, gets the action from the <code>ActionMap</code>,
     * and then (if the action is found and the component
     * is enabled) invokes <code>notifyAction</code> to notify the action.
     *
     * @param ks  the <code>KeyStroke</code> queried
     * @param e the <code>KeyEvent</code>
     * @param condition one of the following values:
     * <ul>
     * <li>JComponent.WHEN_FOCUSED
     * <li>JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
     * <li>JComponent.WHEN_IN_FOCUSED_WINDOW
     * </ul>
     * @param pressed true if the key is pressed
     * @return true if there was a binding to an action, and the action
     *         was enabled
     *
     * @since 1.3
     */
    @SuppressWarnings({"deprecation", "removal"})
    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e,
                                        int condition, boolean pressed) {
        InputMap map = getInputMap(condition, false);
        ActionMap am = getActionMap(false);

        if(map != null && am != null && isEnabled()) {
            Object binding = map.get(ks);
            Action action = (binding == null) ? null : am.get(binding);
            if (action != null) {
                return SwingUtilities.notifyAction(action, ks, e, this,
                                                   e.getModifiers());
            }
        }
        return false;
    }

    /**
     * This is invoked as the result of a <code>KeyEvent</code>
     * that was not consumed by the <code>FocusManager</code>,
     * <code>KeyListeners</code>, or the component. It will first try
     * <code>WHEN_FOCUSED</code> bindings,
     * then <code>WHEN_ANCESTOR_OF_FOCUSED_COMPONENT</code> bindings,
     * and finally <code>WHEN_IN_FOCUSED_WINDOW</code> bindings.
     *
     * @param e the unconsumed <code>KeyEvent</code>
     * @param pressed true if the key is pressed
     * @return true if there is a key binding for <code>e</code>
     */
    @SuppressWarnings({"deprecation", "removal"})
    boolean processKeyBindings(KeyEvent e, boolean pressed) {
      if (!SwingUtilities.isValidKeyEventForKeyBindings(e)) {
          return false;
      }
      // Get the KeyStroke
      // There may be two keystrokes associated with a low-level key event;
      // in this case a keystroke made of an extended key code has a priority.
      KeyStroke ks;
      KeyStroke ksE = null;

      if (e.getID() == KeyEvent.KEY_TYPED) {
          ks = KeyStroke.getKeyStroke(e.getKeyChar());
      }
      else {
          ks = KeyStroke.getKeyStroke(e.getKeyCode(),e.getModifiers(),
                                    (pressed ? false:true));
          if (e.getKeyCode() != e.getExtendedKeyCode()) {
              ksE = KeyStroke.getKeyStroke(e.getExtendedKeyCode(),e.getModifiers(),
                                    (pressed ? false:true));
          }
      }

      // Do we have a key binding for e?
      // If we have a binding by an extended code, use it.
      // If not, check for regular code binding.
      if(ksE != null && processKeyBinding(ksE, e, WHEN_FOCUSED, pressed)) {
          return true;
      }
      if(processKeyBinding(ks, e, WHEN_FOCUSED, pressed))
          return true;

      /* We have no key binding. Let's try the path from our parent to the
       * window excluded. We store the path components so we can avoid
       * asking the same component twice.
       */
      Container parent = this;
      while (parent != null && !(parent instanceof Window) &&
             !(parent instanceof Applet)) {
          if(parent instanceof JComponent) {
              if(ksE != null && ((JComponent)parent).processKeyBinding(ksE, e,
                               WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, pressed))
                  return true;
              if(((JComponent)parent).processKeyBinding(ks, e,
                               WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, pressed))
                  return true;
          }
          // This is done so that the children of a JInternalFrame are
          // given precedence for WHEN_IN_FOCUSED_WINDOW bindings before
          // other components WHEN_IN_FOCUSED_WINDOW bindings. This also gives
          // more precedence to the WHEN_IN_FOCUSED_WINDOW bindings of the
          // JInternalFrame's children vs the
          // WHEN_ANCESTOR_OF_FOCUSED_COMPONENT bindings of the parents.
          // maybe generalize from JInternalFrame (like isFocusCycleRoot).
          if ((parent instanceof JInternalFrame) &&
              JComponent.processKeyBindingsForAllComponents(e,parent,pressed)){
              return true;
          }
          parent = parent.getParent();
      }

      /* No components between the focused component and the window is
       * actually interested by the key event. Let's try the other
       * JComponent in this window.
       */
      if(parent != null) {
        return JComponent.processKeyBindingsForAllComponents(e,parent,pressed);
      }
      return false;
    }

    static boolean processKeyBindingsForAllComponents(KeyEvent e,
                                      Container container, boolean pressed) {
        while (true) {
            if (KeyboardManager.getCurrentManager().fireKeyboardAction(
                                e, pressed, container)) {
                return true;
            }
            if (container instanceof Popup.HeavyWeightWindow) {
                container = ((Window)container).getOwner();
            }
            else {
                return false;
            }
        }
    }

    /**
     * Registers the text to display in a tool tip.
     * The text displays when the cursor lingers over the component.
     * <p>
     * See <a href="https://docs.oracle.com/javase/tutorial/uiswing/components/tooltip.html">How to Use Tool Tips</a>
     * in <em>The Java Tutorial</em>
     * for further documentation.
     *
     * @param text  the string to display; if the text is <code>null</code>,
     *              the tool tip is turned off for this component
     * @see #TOOL_TIP_TEXT_KEY
     */
    @BeanProperty(bound = false, preferred = true, description
            = "The text to display in a tool tip.")
    public void setToolTipText(String text) {
        String oldText = getToolTipText();
        putClientProperty(TOOL_TIP_TEXT_KEY, text);
        ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
        if (text != null) {
            if (oldText == null) {
                toolTipManager.registerComponent(this);
            }
        } else {
            toolTipManager.unregisterComponent(this);
        }
    }

    /**
     * Returns the tooltip string that has been set with
     * <code>setToolTipText</code>.
     *
     * @return the text of the tool tip
     * @see #TOOL_TIP_TEXT_KEY
     */
    public String getToolTipText() {
        return (String)getClientProperty(TOOL_TIP_TEXT_KEY);
    }


    /**
     * Returns the string to be used as the tooltip for <i>event</i>.
     * By default this returns any string set using
     * <code>setToolTipText</code>.  If a component provides
     * more extensive API to support differing tooltips at different locations,
     * this method should be overridden.
     *
     * @param event the {@code MouseEvent} that initiated the
     *              {@code ToolTip} display
     * @return a string containing the  tooltip
     */
    public String getToolTipText(MouseEvent event) {
        return getToolTipText();
    }

    /**
     * Returns the tooltip location in this component's coordinate system.
     * If <code>null</code> is returned, Swing will choose a location.
     * The default implementation returns <code>null</code>.
     *
     * @param event  the <code>MouseEvent</code> that caused the
     *          <code>ToolTipManager</code> to show the tooltip
     * @return always returns <code>null</code>
     */
    public Point getToolTipLocation(MouseEvent event) {
        return null;
    }

    /**
     * Returns the preferred location to display the popup menu in this
     * component's coordinate system. It is up to the look and feel to
     * honor this property, some may choose to ignore it.
     * If {@code null}, the look and feel will choose a suitable location.
     *
     * @param event the {@code MouseEvent} that triggered the popup to be
     *        shown, or {@code null} if the popup is not being shown as the
     *        result of a mouse event
     * @return location to display the {@code JPopupMenu}, or {@code null}
     * @since 1.5
     */
    public Point getPopupLocation(MouseEvent event) {
        return null;
    }


    /**
     * Returns the instance of <code>JToolTip</code> that should be used
     * to display the tooltip.
     * Components typically would not override this method,
     * but it can be used to
     * cause different tooltips to be displayed differently.
     *
     * @return the <code>JToolTip</code> used to display this toolTip
     */
    public JToolTip createToolTip() {
        JToolTip tip = new JToolTip();
        tip.setComponent(this);
        return tip;
    }

    /**
     * Forwards the <code>scrollRectToVisible()</code> message to the
     * <code>JComponent</code>'s parent. Components that can service
     * the request, such as <code>JViewport</code>,
     * override this method and perform the scrolling.
     *
     * @param aRect the visible <code>Rectangle</code>
     * @see JViewport
     */
    public void scrollRectToVisible(Rectangle aRect) {
        Container parent;
        int dx = getX(), dy = getY();

        for (parent = getParent();
                 !(parent == null) &&
                 !(parent instanceof JComponent) &&
                 !(parent instanceof CellRendererPane);
             parent = parent.getParent()) {
             Rectangle bounds = parent.getBounds();

             dx += bounds.x;
             dy += bounds.y;
        }

        if (!(parent == null) && !(parent instanceof CellRendererPane)) {
            aRect.x += dx;
            aRect.y += dy;

            ((JComponent)parent).scrollRectToVisible(aRect);
            aRect.x -= dx;
            aRect.y -= dy;
        }
    }

    /**
     * Sets the <code>autoscrolls</code> property.
     * If <code>true</code> mouse dragged events will be
     * synthetically generated when the mouse is dragged
     * outside of the component's bounds and mouse motion
     * has paused (while the button continues to be held
     * down). The synthetic events make it appear that the
     * drag gesture has resumed in the direction established when
     * the component's boundary was crossed.  Components that
     * support autoscrolling must handle <code>mouseDragged</code>
     * events by calling <code>scrollRectToVisible</code> with a
     * rectangle that contains the mouse event's location.  All of
     * the Swing components that support item selection and are
     * typically displayed in a <code>JScrollPane</code>
     * (<code>JTable</code>, <code>JList</code>, <code>JTree</code>,
     * <code>JTextArea</code>, and <code>JEditorPane</code>)
     * already handle mouse dragged events in this way.  To enable
     * autoscrolling in any other component, add a mouse motion
     * listener that calls <code>scrollRectToVisible</code>.
     * For example, given a <code>JPanel</code>, <code>myPanel</code>:
     * <pre>
     * MouseMotionListener doScrollRectToVisible = new MouseMotionAdapter() {
     *     public void mouseDragged(MouseEvent e) {
     *        Rectangle r = new Rectangle(e.getX(), e.getY(), 1, 1);
     *        ((JPanel)e.getSource()).scrollRectToVisible(r);
     *    }
     * };
     * myPanel.addMouseMotionListener(doScrollRectToVisible);
     * </pre>
     * The default value of the <code>autoScrolls</code>
     * property is <code>false</code>.
     *
     * @param autoscrolls if true, synthetic mouse dragged events
     *   are generated when the mouse is dragged outside of a component's
     *   bounds and the mouse button continues to be held down; otherwise
     *   false
     * @see #getAutoscrolls
     * @see JViewport
     * @see JScrollPane
     */
    @BeanProperty(bound = false, expert = true, description
            = "Determines if this component automatically scrolls its contents when dragged.")
    public void setAutoscrolls(boolean autoscrolls) {
        setFlag(AUTOSCROLLS_SET, true);
        if (this.autoscrolls != autoscrolls) {
            this.autoscrolls = autoscrolls;
            if (autoscrolls) {
                enableEvents(AWTEvent.MOUSE_EVENT_MASK);
                enableEvents(AWTEvent.MOUSE_MOTION_EVENT_MASK);
            }
            else {
                Autoscroller.stop(this);
            }
        }
    }

    /**
     * Gets the <code>autoscrolls</code> property.
     *
     * @return the value of the <code>autoscrolls</code> property
     * @see JViewport
     * @see #setAutoscrolls
     */
    public boolean getAutoscrolls() {
        return autoscrolls;
    }

    /**
     * Sets the {@code TransferHandler}, which provides support for transfer
     * of data into and out of this component via cut/copy/paste and drag
     * and drop. This may be {@code null} if the component does not support
     * data transfer operations.
     * <p>
     * If the new {@code TransferHandler} is not {@code null}, this method
     * also installs a <b>new</b> {@code DropTarget} on the component to
     * activate drop handling through the {@code TransferHandler} and activate
     * any built-in support (such as calculating and displaying potential drop
     * locations). If you do not wish for this component to respond in any way
     * to drops, you can disable drop support entirely either by removing the
     * drop target ({@code setDropTarget(null)}) or by de-activating it
     * ({@code getDropTaget().setActive(false)}).
     * <p>
     * If the new {@code TransferHandler} is {@code null}, this method removes
     * the drop target.
     * <p>
     * Under two circumstances, this method does not modify the drop target:
     * First, if the existing drop target on this component was explicitly
     * set by the developer to a {@code non-null} value. Second, if the
     * system property {@code suppressSwingDropSupport} is {@code true}. The
     * default value for the system property is {@code false}.
     * <p>
     * Please see
     * <a href="https://docs.oracle.com/javase/tutorial/uiswing/dnd/index.html">
     * How to Use Drag and Drop and Data Transfer</a>,
     * a section in <em>The Java Tutorial</em>, for more information.
     *
     * @param newHandler the new {@code TransferHandler}
     *
     * @see TransferHandler
     * @see #getTransferHandler
     * @since 1.4
     */
    @BeanProperty(hidden = true, description
            = "Mechanism for transfer of data to and from the component")
    public void setTransferHandler(TransferHandler newHandler) {
        TransferHandler oldHandler = (TransferHandler)getClientProperty(
                                      JComponent_TRANSFER_HANDLER);
        putClientProperty(JComponent_TRANSFER_HANDLER, newHandler);

        SwingUtilities.installSwingDropTargetAsNecessary(this, newHandler);
        firePropertyChange("transferHandler", oldHandler, newHandler);
    }

    /**
     * Gets the <code>transferHandler</code> property.
     *
     * @return  the value of the <code>transferHandler</code> property
     *
     * @see TransferHandler
     * @see #setTransferHandler
     * @since 1.4
     */
    public TransferHandler getTransferHandler() {
        return (TransferHandler)getClientProperty(JComponent_TRANSFER_HANDLER);
    }

    /**
     * Calculates a custom drop location for this type of component,
     * representing where a drop at the given point should insert data.
     * <code>null</code> is returned if this component doesn't calculate
     * custom drop locations. In this case, <code>TransferHandler</code>
     * will provide a default <code>DropLocation</code> containing just
     * the point.
     *
     * @param p the point to calculate a drop location for
     * @return the drop location, or <code>null</code>
     */
    TransferHandler.DropLocation dropLocationForPoint(Point p) {
        return null;
    }

    /**
     * Called to set or clear the drop location during a DnD operation.
     * In some cases, the component may need to use its internal selection
     * temporarily to indicate the drop location. To help facilitate this,
     * this method returns and accepts as a parameter a state object.
     * This state object can be used to store, and later restore, the selection
     * state. Whatever this method returns will be passed back to it in
     * future calls, as the state parameter. If it wants the DnD system to
     * continue storing the same state, it must pass it back every time.
     * Here's how this is used:
     * <p>
     * Let's say that on the first call to this method the component decides
     * to save some state (because it is about to use the selection to show
     * a drop index). It can return a state object to the caller encapsulating
     * any saved selection state. On a second call, let's say the drop location
     * is being changed to something else. The component doesn't need to
     * restore anything yet, so it simply passes back the same state object
     * to have the DnD system continue storing it. Finally, let's say this
     * method is messaged with <code>null</code>. This means DnD
     * is finished with this component for now, meaning it should restore
     * state. At this point, it can use the state parameter to restore
     * said state, and of course return <code>null</code> since there's
     * no longer anything to store.
     *
     * @param location the drop location (as calculated by
     *        <code>dropLocationForPoint</code>) or <code>null</code>
     *        if there's no longer a valid drop location
     * @param state the state object saved earlier for this component,
     *        or <code>null</code>
     * @param forDrop whether or not the method is being called because an
     *        actual drop occurred
     * @return any saved state for this component, or <code>null</code> if none
     */
    Object setDropLocation(TransferHandler.DropLocation location,
                           Object state,
                           boolean forDrop) {

        return null;
    }

    /**
     * Called to indicate to this component that DnD is done.
     * Needed by <code>JTree</code>.
     */
    void dndDone() {
    }

    /**
     * Processes mouse events occurring on this component by
     * dispatching them to any registered
     * <code>MouseListener</code> objects, refer to
     * {@link java.awt.Component#processMouseEvent(MouseEvent)}
     * for a complete description of this method.
     *
     * @param       e the mouse event
     * @see         java.awt.Component#processMouseEvent
     * @since       1.5
     */
    protected void processMouseEvent(MouseEvent e) {
        if (autoscrolls && e.getID() == MouseEvent.MOUSE_RELEASED) {
            Autoscroller.stop(this);
        }
        super.processMouseEvent(e);
    }

    /**
     * Processes mouse motion events, such as MouseEvent.MOUSE_DRAGGED.
     *
     * @param e the <code>MouseEvent</code>
     * @see MouseEvent
     */
    protected void processMouseMotionEvent(MouseEvent e) {
        boolean dispatch = true;
        if (autoscrolls && e.getID() == MouseEvent.MOUSE_DRAGGED) {
            // We don't want to do the drags when the mouse moves if we're
            // autoscrolling.  It makes it feel spastic.
            dispatch = !Autoscroller.isRunning(this);
            Autoscroller.processMouseDragged(e);
        }
        if (dispatch) {
            super.processMouseMotionEvent(e);
        }
    }

    // Inner classes can't get at this method from a super class
    void superProcessMouseMotionEvent(MouseEvent e) {
        super.processMouseMotionEvent(e);
    }

    /**
     * This is invoked by the <code>RepaintManager</code> if
     * <code>createImage</code> is called on the component.
     *
     * @param newValue true if the double buffer image was created from this component
     */
    void setCreatedDoubleBuffer(boolean newValue) {
        setFlag(CREATED_DOUBLE_BUFFER, newValue);
    }

    /**
     * Returns true if the <code>RepaintManager</code>
     * created the double buffer image from the component.
     *
     * @return true if this component had a double buffer image, false otherwise
     */
    boolean getCreatedDoubleBuffer() {
        return getFlag(CREATED_DOUBLE_BUFFER);
    }

    /**
     * <code>ActionStandin</code> is used as a standin for
     * <code>ActionListeners</code> that are
     * added via <code>registerKeyboardAction</code>.
     */
    final class ActionStandin implements Action {
        private final ActionListener actionListener;
        private final String command;
        // This will be non-null if actionListener is an Action.
        private final Action action;

        ActionStandin(ActionListener actionListener, String command) {
            this.actionListener = actionListener;
            if (actionListener instanceof Action) {
                this.action = (Action)actionListener;
            }
            else {
                this.action = null;
            }
            this.command = command;
        }

        public Object getValue(String key) {
            if (key != null) {
                if (key.equals(Action.ACTION_COMMAND_KEY)) {
                    return command;
                }
                if (action != null) {
                    return action.getValue(key);
                }
                if (key.equals(NAME)) {
                    return "ActionStandin";
                }
            }
            return null;
        }

        public boolean isEnabled() {
            if (actionListener == null) {
                // This keeps the old semantics where
                // registerKeyboardAction(null) would essentialy remove
                // the binding. We don't remove the binding from the
                // InputMap as that would still allow parent InputMaps
                // bindings to be accessed.
                return false;
            }
            if (action == null) {
                return true;
            }
            return action.isEnabled();
        }

        public void actionPerformed(ActionEvent ae) {
            if (actionListener != null) {
                actionListener.actionPerformed(ae);
            }
        }

        // We don't allow any values to be added.
        public void putValue(String key, Object value) {}

        // Does nothing, our enabledness is determiend from our asociated
        // action.
        public void setEnabled(boolean b) { }

        public void addPropertyChangeListener
                    (PropertyChangeListener listener) {}
        public void removePropertyChangeListener
                          (PropertyChangeListener listener) {}
    }


    // This class is used by the KeyboardState class to provide a single
    // instance that can be stored in the AppContext.
    static final class IntVector {
        int[] array = null;
        int count = 0;
        int capacity = 0;

        int size() {
            return count;
        }

        int elementAt(int index) {
            return array[index];
        }

        void addElement(int value) {
            if (count == capacity) {
                capacity = (capacity + 2) * 2;
                int[] newarray = new int[capacity];
                if (count > 0) {
                    System.arraycopy(array, 0, newarray, 0, count);
                }
                array = newarray;
            }
            array[count++] = value;
        }

        void setElementAt(int value, int index) {
            array[index] = value;
        }
    }

    @SuppressWarnings("serial")
    static class KeyboardState implements Serializable {
        private static final Object keyCodesKey =
            JComponent.KeyboardState.class;

        // Get the array of key codes from the AppContext.
        static IntVector getKeyCodeArray() {
            IntVector iv =
                (IntVector)SwingUtilities.appContextGet(keyCodesKey);
            if (iv == null) {
                iv = new IntVector();
                SwingUtilities.appContextPut(keyCodesKey, iv);
            }
            return iv;
        }

        static void registerKeyPressed(int keyCode) {
            IntVector kca = getKeyCodeArray();
            int count = kca.size();
            int i;
            for(i=0;i<count;i++) {
                if(kca.elementAt(i) == -1){
                    kca.setElementAt(keyCode, i);
                    return;
                }
            }
            kca.addElement(keyCode);
        }

        static void registerKeyReleased(int keyCode) {
            IntVector kca = getKeyCodeArray();
            int count = kca.size();
            int i;
            for(i=0;i<count;i++) {
                if(kca.elementAt(i) == keyCode) {
                    kca.setElementAt(-1, i);
                    return;
                }
            }
        }

        static boolean keyIsPressed(int keyCode) {
            IntVector kca = getKeyCodeArray();
            int count = kca.size();
            int i;
            for(i=0;i<count;i++) {
                if(kca.elementAt(i) == keyCode) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Updates internal state of the KeyboardState and returns true
         * if the event should be processed further.
         */
        static boolean shouldProcess(KeyEvent e) {
            switch (e.getID()) {
            case KeyEvent.KEY_PRESSED:
                if (!keyIsPressed(e.getKeyCode())) {
                    registerKeyPressed(e.getKeyCode());
                }
                return true;
            case KeyEvent.KEY_RELEASED:
                // We are forced to process VK_PRINTSCREEN separately because
                // the Windows doesn't generate the key pressed event for
                // printscreen and it block the processing of key release
                // event for printscreen.
                if (keyIsPressed(e.getKeyCode()) || e.getKeyCode()==KeyEvent.VK_PRINTSCREEN) {
                    registerKeyReleased(e.getKeyCode());
                    return true;
                }
                return false;
            case KeyEvent.KEY_TYPED:
                return true;
            default:
                // Not a known KeyEvent type, bail.
                return false;
            }
      }
    }

    static final sun.awt.RequestFocusController focusController =
        new sun.awt.RequestFocusController() {
            public boolean acceptRequestFocus(Component from, Component to,
                                              boolean temporary, boolean focusedWindowChangeAllowed,
                                              FocusEvent.Cause cause)
            {
                if ((to == null) || !(to instanceof JComponent)) {
                    return true;
                }

                if ((from == null) || !(from instanceof JComponent)) {
                    return true;
                }

                JComponent target = (JComponent) to;
                if (!target.getVerifyInputWhenFocusTarget()) {
                    return true;
                }

                JComponent jFocusOwner = (JComponent)from;
                InputVerifier iv = jFocusOwner.getInputVerifier();

                if (iv == null) {
                    return true;
                } else {
                    Object currentSource = SwingUtilities.appContextGet(
                            INPUT_VERIFIER_SOURCE_KEY);
                    if (currentSource == jFocusOwner) {
                        // We're currently calling into the InputVerifier
                        // for this component, so allow the focus change.
                        return true;
                    }
                    SwingUtilities.appContextPut(INPUT_VERIFIER_SOURCE_KEY,
                                                 jFocusOwner);
                    try {
                        return iv.shouldYieldFocus(jFocusOwner, target);
                    } finally {
                        if (currentSource != null) {
                            // We're already in the InputVerifier for
                            // currentSource. By resetting the currentSource
                            // we ensure that if the InputVerifier for
                            // currentSource does a requestFocus, we don't
                            // try and run the InputVerifier again.
                            SwingUtilities.appContextPut(
                                INPUT_VERIFIER_SOURCE_KEY, currentSource);
                        } else {
                            SwingUtilities.appContextRemove(
                                INPUT_VERIFIER_SOURCE_KEY);
                        }
                    }
                }
            }
        };

    /*
     * --- Accessibility Support ---
     */

    /**
     * @deprecated As of JDK version 1.1,
     * replaced by <code>java.awt.Component.setEnabled(boolean)</code>.
     */
    @Deprecated
    public void enable() {
        if (isEnabled() != true) {
            super.enable();
            if (accessibleContext != null) {
                accessibleContext.firePropertyChange(
                    AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                    null, AccessibleState.ENABLED);
            }
        }
    }

    /**
     * @deprecated As of JDK version 1.1,
     * replaced by <code>java.awt.Component.setEnabled(boolean)</code>.
     */
    @Deprecated
    public void disable() {
        if (isEnabled() != false) {
            super.disable();
            if (accessibleContext != null) {
                accessibleContext.firePropertyChange(
                    AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                    AccessibleState.ENABLED, null);
            }
        }
    }

    /**
     * Inner class of JComponent used to provide default support for
     * accessibility.  This class is not meant to be used directly by
     * application developers, but is instead meant only to be
     * subclassed by component developers.
     * <p>
     * <strong>Warning:</strong>
     * Serialized objects of this class will not be compatible with
     * future Swing releases. The current serialization support is
     * appropriate for short term storage or RMI between applications running
     * the same version of Swing.  As of 1.4, support for long term storage
     * of all JavaBeans
     * has been added to the <code>java.beans</code> package.
     * Please see {@link java.beans.XMLEncoder}.
     */
    @SuppressWarnings("serial") // Same-version serialization only
    public abstract class AccessibleJComponent extends AccessibleAWTContainer
       implements AccessibleExtendedComponent
    {
        /**
         * Though the class is abstract, this should be called by
         * all sub-classes.
         */
        protected AccessibleJComponent() {
            super();
        }

        /**
         * Number of PropertyChangeListener objects registered. It's used
         * to add/remove ContainerListener and FocusListener to track
         * target JComponent's state
         */
        private transient volatile int propertyListenersCount = 0;

        /**
         * This field duplicates the function of the accessibleAWTFocusHandler field
         * in java.awt.Component.AccessibleAWTComponent, so it has been deprecated.
         */
        @Deprecated
        protected FocusListener accessibleFocusHandler = null;

        /**
         * Fire PropertyChange listener, if one is registered,
         * when children added/removed.
         */
        protected class AccessibleContainerHandler
            implements ContainerListener {

            /**
             * Constructs an {@code AccessibleContainerHandler}.
             */
            protected AccessibleContainerHandler() {}
            public void componentAdded(ContainerEvent e) {
                Component c = e.getChild();
                if (c != null && c instanceof Accessible) {
                    AccessibleJComponent.this.firePropertyChange(
                        AccessibleContext.ACCESSIBLE_CHILD_PROPERTY,
                        null, c.getAccessibleContext());
                }
            }
            public void componentRemoved(ContainerEvent e) {
                Component c = e.getChild();
                if (c != null && c instanceof Accessible) {
                    AccessibleJComponent.this.firePropertyChange(
                        AccessibleContext.ACCESSIBLE_CHILD_PROPERTY,
                        c.getAccessibleContext(), null);
                }
            }
        }

        /**
         * Fire PropertyChange listener, if one is registered,
         * when focus events happen
         * @since 1.3
         * @deprecated This class is no longer used or needed.
         * {@code java.awt.Component.AccessibleAWTComponent} provides
         * the same functionality and it is handled in {@code Component}.
         */
        @Deprecated
        protected class AccessibleFocusHandler implements FocusListener {
           /**
            * Constructs an {@code AccessibleFocusHandler}.
            */
           protected AccessibleFocusHandler() {}
           public void focusGained(FocusEvent event) {
               if (accessibleContext != null) {
                    accessibleContext.firePropertyChange(
                        AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                        null, AccessibleState.FOCUSED);
                }
            }
            public void focusLost(FocusEvent event) {
                if (accessibleContext != null) {
                    accessibleContext.firePropertyChange(
                        AccessibleContext.ACCESSIBLE_STATE_PROPERTY,
                        AccessibleState.FOCUSED, null);
                }
            }
        } // inner class AccessibleFocusHandler


        /**
         * Adds a PropertyChangeListener to the listener list.
         *
         * @param listener  the PropertyChangeListener to be added
         */
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            super.addPropertyChangeListener(listener);
        }

        /**
         * Removes a PropertyChangeListener from the listener list.
         * This removes a PropertyChangeListener that was registered
         * for all properties.
         *
         * @param listener  the PropertyChangeListener to be removed
         */
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            super.removePropertyChangeListener(listener);
        }



        /**
         * Recursively search through the border hierarchy (if it exists)
         * for a TitledBorder with a non-null title.  This does a depth
         * first search on first the inside borders then the outside borders.
         * The assumption is that titles make really pretty inside borders
         * but not very pretty outside borders in compound border situations.
         * It's rather arbitrary, but hopefully decent UI programmers will
         * not create multiple titled borders for the same component.
         *
         * @param b  the {@code Border} for which to retrieve its title
         * @return the border's title as a {@code String}, null if it has
         *         no title
         */
        protected String getBorderTitle(Border b) {
            String s;
            if (b instanceof TitledBorder) {
                return ((TitledBorder) b).getTitle();
            } else if (b instanceof CompoundBorder) {
                s = getBorderTitle(((CompoundBorder) b).getInsideBorder());
                if (s == null) {
                    s = getBorderTitle(((CompoundBorder) b).getOutsideBorder());
                }
                return s;
            } else {
                return null;
            }
        }

        // AccessibleContext methods
        //
        /**
         * Gets the accessible name of this object.  This should almost never
         * return java.awt.Component.getName(), as that generally isn't
         * a localized name, and doesn't have meaning for the user.  If the
         * object is fundamentally a text object (such as a menu item), the
         * accessible name should be the text of the object (for example,
         * "save").
         * If the object has a tooltip, the tooltip text may also be an
         * appropriate String to return.
         *
         * @return the localized name of the object -- can be null if this
         *         object does not have a name
         * @see AccessibleContext#setAccessibleName
         */
        public String getAccessibleName() {
            String name = accessibleName;

            // fallback to the client name property
            //
            if (name == null) {
                name = (String)getClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY);
            }

            // fallback to the titled border if it exists
            //
            if (name == null) {
                name = getBorderTitle(getBorder());
            }

            // fallback to the label labeling us if it exists
            //
            if (name == null) {
                Object o = getClientProperty(JLabel.LABELED_BY_PROPERTY);
                if (o instanceof Accessible) {
                    AccessibleContext ac = ((Accessible) o).getAccessibleContext();
                    if (ac != null) {
                        name = ac.getAccessibleName();
                    }
                }
            }
            return name;
        }

        /**
         * Gets the accessible description of this object.  This should be
         * a concise, localized description of what this object is - what
         * is its meaning to the user.  If the object has a tooltip, the
         * tooltip text may be an appropriate string to return, assuming
         * it contains a concise description of the object (instead of just
         * the name of the object - for example a "Save" icon on a toolbar that
         * had "save" as the tooltip text shouldn't return the tooltip
         * text as the description, but something like "Saves the current
         * text document" instead).
         *
         * @return the localized description of the object -- can be null if
         * this object does not have a description
         * @see AccessibleContext#setAccessibleDescription
         */
        public String getAccessibleDescription() {
            String description = accessibleDescription;

            // fallback to the client description property
            //
            if (description == null) {
                description = (String)getClientProperty(AccessibleContext.ACCESSIBLE_DESCRIPTION_PROPERTY);
            }

            // fallback to the tool tip text if it exists
            //
            if (description == null) {
                try {
                    description = getToolTipText();
                } catch (Exception e) {
                    // Just in case the subclass overrode the
                    // getToolTipText method and actually
                    // requires a MouseEvent.
                    // [[[FIXME:  WDW - we probably should require this
                    // method to take a MouseEvent and just pass it on
                    // to getToolTipText.  The swing-feedback traffic
                    // leads me to believe getToolTipText might change,
                    // though, so I was hesitant to make this change at
                    // this time.]]]
                }
            }

            // fallback to the label labeling us if it exists
            //
            if (description == null) {
                Object o = getClientProperty(JLabel.LABELED_BY_PROPERTY);
                if (o instanceof Accessible) {
                    AccessibleContext ac = ((Accessible) o).getAccessibleContext();
                    if (ac != null) {
                        description = ac.getAccessibleDescription();
                    }
                }
            }

            return description;
        }

        /**
         * Gets the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the
         * object
         * @see AccessibleRole
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.SWING_COMPONENT;
        }

        /**
         * Gets the state of this object.
         *
         * @return an instance of AccessibleStateSet containing the current
         * state set of the object
         * @see AccessibleState
         */
        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet states = super.getAccessibleStateSet();
            if (JComponent.this.isOpaque()) {
                states.add(AccessibleState.OPAQUE);
            }
            return states;
        }

        /**
         * Returns the number of accessible children in the object.  If all
         * of the children of this object implement Accessible, than this
         * method should return the number of children of this object.
         *
         * @return the number of accessible children in the object.
         */
        public int getAccessibleChildrenCount() {
            return super.getAccessibleChildrenCount();
        }

        /**
         * Returns the nth Accessible child of the object.
         *
         * @param i zero-based index of child
         * @return the nth Accessible child of the object
         */
        public Accessible getAccessibleChild(int i) {
            return super.getAccessibleChild(i);
        }

        // ----- AccessibleExtendedComponent

        /**
         * Returns the AccessibleExtendedComponent
         *
         * @return the AccessibleExtendedComponent
         */
        AccessibleExtendedComponent getAccessibleExtendedComponent() {
            return this;
        }

        /**
         * Returns the tool tip text
         *
         * @return the tool tip text, if supported, of the object;
         * otherwise, null
         * @since 1.4
         */
        public String getToolTipText() {
            return JComponent.this.getToolTipText();
        }

        /**
         * Returns the titled border text
         *
         * @return the titled border text, if supported, of the object;
         * otherwise, null
         * @since 1.4
         */
        public String getTitledBorderText() {
            Border border = JComponent.this.getBorder();
            if (border instanceof TitledBorder) {
                return ((TitledBorder)border).getTitle();
            } else {
                return null;
            }
        }

        /**
         * Returns key bindings associated with this object
         *
         * @return the key bindings, if supported, of the object;
         * otherwise, null
         * @see AccessibleKeyBinding
         * @since 1.4
         */
        public AccessibleKeyBinding getAccessibleKeyBinding(){
            // Try to get the linked label's mnemonic if it exists
            Object o = getClientProperty(JLabel.LABELED_BY_PROPERTY);
            if (o instanceof Accessible){
                AccessibleContext ac = ((Accessible) o).getAccessibleContext();
                if (ac != null){
                    AccessibleComponent comp = ac.getAccessibleComponent();
                    if (! (comp instanceof AccessibleExtendedComponent))
                        return null;
                    return ((AccessibleExtendedComponent)comp).getAccessibleKeyBinding();
                }
            }
            return null;
        }
    } // inner class AccessibleJComponent


    /**
     * Returns an <code>ArrayTable</code> used for
     * key/value "client properties" for this component. If the
     * <code>clientProperties</code> table doesn't exist, an empty one
     * will be created.
     *
     * @return an ArrayTable
     * @see #putClientProperty
     * @see #getClientProperty
     */
    private ArrayTable getClientProperties() {
        if (clientProperties == null) {
            clientProperties = new ArrayTable();
        }
        return clientProperties;
    }


    /**
     * Returns the value of the property with the specified key.  Only
     * properties added with <code>putClientProperty</code> will return
     * a non-<code>null</code> value.
     *
     * @param key the being queried
     * @return the value of this property or <code>null</code>
     * @see #putClientProperty
     */
    public final Object getClientProperty(Object key) {
        if (key == RenderingHints.KEY_TEXT_ANTIALIASING) {
            return aaHint;
        } else if (key == RenderingHints.KEY_TEXT_LCD_CONTRAST) {
            return lcdRenderingHint;
        }
         if(clientProperties == null) {
            return null;
        } else {
            synchronized(clientProperties) {
                return clientProperties.get(key);
            }
        }
    }

    /**
     * Adds an arbitrary key/value "client property" to this component.
     * <p>
     * The <code>get/putClientProperty</code> methods provide access to
     * a small per-instance hashtable. Callers can use get/putClientProperty
     * to annotate components that were created by another module.
     * For example, a
     * layout manager might store per child constraints this way. For example:
     * <pre>
     * componentA.putClientProperty("to the left of", componentB);
     * </pre>
     * If value is <code>null</code> this method will remove the property.
     * Changes to client properties are reported with
     * <code>PropertyChange</code> events.
     * The name of the property (for the sake of PropertyChange
     * events) is <code>key.toString()</code>.
     * <p>
     * The <code>clientProperty</code> dictionary is not intended to
     * support large
     * scale extensions to JComponent nor should be it considered an
     * alternative to subclassing when designing a new component.
     *
     * @param key the new client property key
     * @param value the new client property value; if <code>null</code>
     *          this method will remove the property
     * @see #getClientProperty
     * @see #addPropertyChangeListener
     */
    public final void putClientProperty(Object key, Object value) {
        if (key == RenderingHints.KEY_TEXT_ANTIALIASING) {
            aaHint = value;
            return;
        } else if (key == RenderingHints.KEY_TEXT_LCD_CONTRAST) {
            lcdRenderingHint = value;
            return;
        }
        if (value == null && clientProperties == null) {
            // Both the value and ArrayTable are null, implying we don't
            // have to do anything.
            return;
        }
        ArrayTable clientProperties = getClientProperties();
        Object oldValue;
        synchronized(clientProperties) {
            oldValue = clientProperties.get(key);
            if (value != null) {
                clientProperties.put(key, value);
            } else if (oldValue != null) {
                clientProperties.remove(key);
            } else {
                // old == new == null
                return;
            }
        }
        clientPropertyChanged(key, oldValue, value);
        firePropertyChange(key.toString(), oldValue, value);
    }

    // Invoked from putClientProperty.  This is provided for subclasses
    // in Swing.
    void clientPropertyChanged(Object key, Object oldValue,
                               Object newValue) {
    }


    /*
     * Sets the property with the specified name to the specified value if
     * the property has not already been set by the client program.
     * This method is used primarily to set UI defaults for properties
     * with primitive types, where the values cannot be marked with
     * UIResource.
     * @see LookAndFeel#installProperty
     * @param propertyName String containing the name of the property
     * @param value Object containing the property value
     */
    void setUIProperty(String propertyName, Object value) {
        if ("opaque".equals(propertyName)) {
            if (!getFlag(OPAQUE_SET)) {
                setOpaque(((Boolean)value).booleanValue());
                setFlag(OPAQUE_SET, false);
            }
        } else if ("autoscrolls".equals(propertyName)) {
            if (!getFlag(AUTOSCROLLS_SET)) {
                setAutoscrolls(((Boolean)value).booleanValue());
                setFlag(AUTOSCROLLS_SET, false);
            }
        } else if ("focusTraversalKeysForward".equals(propertyName)) {
            @SuppressWarnings("unchecked")
            Set<AWTKeyStroke> strokeSet = (Set<AWTKeyStroke>) value;
            if (!getFlag(FOCUS_TRAVERSAL_KEYS_FORWARD_SET)) {
                super.setFocusTraversalKeys(KeyboardFocusManager.
                                            FORWARD_TRAVERSAL_KEYS,
                                            strokeSet);
            }
        } else if ("focusTraversalKeysBackward".equals(propertyName)) {
            @SuppressWarnings("unchecked")
            Set<AWTKeyStroke> strokeSet = (Set<AWTKeyStroke>) value;
            if (!getFlag(FOCUS_TRAVERSAL_KEYS_BACKWARD_SET)) {
                super.setFocusTraversalKeys(KeyboardFocusManager.
                                            BACKWARD_TRAVERSAL_KEYS,
                                            strokeSet);
            }
        } else {
            throw new IllegalArgumentException("property \""+
                                               propertyName+ "\" cannot be set using this method");
        }
    }


    /**
     * Sets the focus traversal keys for a given traversal operation for this
     * Component.
     * Refer to
     * {@link java.awt.Component#setFocusTraversalKeys}
     * for a complete description of this method.
     * <p>
     * This method may throw a {@code ClassCastException} if any {@code Object}
     * in {@code keystrokes} is not an {@code AWTKeyStroke}.
     *
     * @param id one of KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
     *        KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, or
     *        KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS
     * @param keystrokes the Set of AWTKeyStroke for the specified operation
     * @see java.awt.KeyboardFocusManager#FORWARD_TRAVERSAL_KEYS
     * @see java.awt.KeyboardFocusManager#BACKWARD_TRAVERSAL_KEYS
     * @see java.awt.KeyboardFocusManager#UP_CYCLE_TRAVERSAL_KEYS
     * @throws IllegalArgumentException if id is not one of
     *         KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
     *         KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, or
     *         KeyboardFocusManager.UP_CYCLE_TRAVERSAL_KEYS, or if keystrokes
     *         contains null, or if any keystroke represents a KEY_TYPED event,
     *         or if any keystroke already maps to another focus traversal
     *         operation for this Component
     * @since 1.5
     */
    public void
        setFocusTraversalKeys(int id, Set<? extends AWTKeyStroke> keystrokes)
    {
        if (id == KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS) {
            setFlag(FOCUS_TRAVERSAL_KEYS_FORWARD_SET,true);
        } else if (id == KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS) {
            setFlag(FOCUS_TRAVERSAL_KEYS_BACKWARD_SET,true);
        }
        super.setFocusTraversalKeys(id,keystrokes);
    }

    /* --- Transitional java.awt.Component Support ---
     * The methods and fields in this section will migrate to
     * java.awt.Component in the next JDK release.
     */

    /**
     * Returns true if this component is lightweight, that is, if it doesn't
     * have a native window system peer.
     *
     * @param c  the {@code Component} to be checked
     * @return true if this component is lightweight
     */
    public static boolean isLightweightComponent(Component c) {
        // TODO we cannot call c.isLightweight() because it is incorrectly
        // overriden in DelegateContainer on osx.
        return AWTAccessor.getComponentAccessor().isLightweight(c);
    }


    /**
     * @deprecated As of JDK 5,
     * replaced by <code>Component.setBounds(int, int, int, int)</code>.
     * <p>
     * Moves and resizes this component.
     *
     * @param x  the new horizontal location
     * @param y  the new vertical location
     * @param w  the new width
     * @param h  the new height
     * @see java.awt.Component#setBounds
     */
    @Deprecated
    public void reshape(int x, int y, int w, int h) {
        super.reshape(x, y, w, h);
    }


    /**
     * Stores the bounds of this component into "return value"
     * <code>rv</code> and returns <code>rv</code>.
     * If <code>rv</code> is <code>null</code> a new <code>Rectangle</code>
     * is allocated.  This version of <code>getBounds</code> is useful
     * if the caller wants to avoid allocating a new <code>Rectangle</code>
     * object on the heap.
     *
     * @param rv the return value, modified to the component's bounds
     * @return <code>rv</code>; if <code>rv</code> is <code>null</code>
     *          return a newly created <code>Rectangle</code> with this
     *          component's bounds
     */
    public Rectangle getBounds(Rectangle rv) {
        if (rv == null) {
            return new Rectangle(getX(), getY(), getWidth(), getHeight());
        }
        else {
            rv.setBounds(getX(), getY(), getWidth(), getHeight());
            return rv;
        }
    }


    /**
     * Stores the width/height of this component into "return value"
     * <code>rv</code> and returns <code>rv</code>.
     * If <code>rv</code> is <code>null</code> a new <code>Dimension</code>
     * object is allocated.  This version of <code>getSize</code>
     * is useful if the caller wants to avoid allocating a new
     * <code>Dimension</code> object on the heap.
     *
     * @param rv the return value, modified to the component's size
     * @return <code>rv</code>
     */
    public Dimension getSize(Dimension rv) {
        if (rv == null) {
            return new Dimension(getWidth(), getHeight());
        }
        else {
            rv.setSize(getWidth(), getHeight());
            return rv;
        }
    }


    /**
     * Stores the x,y origin of this component into "return value"
     * <code>rv</code> and returns <code>rv</code>.
     * If <code>rv</code> is <code>null</code> a new <code>Point</code>
     * is allocated.  This version of <code>getLocation</code> is useful
     * if the caller wants to avoid allocating a new <code>Point</code>
     * object on the heap.
     *
     * @param rv the return value, modified to the component's location
     * @return <code>rv</code>
     */
    public Point getLocation(Point rv) {
        if (rv == null) {
            return new Point(getX(), getY());
        }
        else {
            rv.setLocation(getX(), getY());
            return rv;
        }
    }


    /**
     * Returns the current x coordinate of the component's origin.
     * This method is preferable to writing
     * <code>component.getBounds().x</code>, or
     * <code>component.getLocation().x</code> because it doesn't cause any
     * heap allocations.
     *
     * @return the current x coordinate of the component's origin
     */
    @BeanProperty(bound = false)
    public int getX() { return super.getX(); }


    /**
     * Returns the current y coordinate of the component's origin.
     * This method is preferable to writing
     * <code>component.getBounds().y</code>, or
     * <code>component.getLocation().y</code> because it doesn't cause any
     * heap allocations.
     *
     * @return the current y coordinate of the component's origin
     */
    @BeanProperty(bound = false)
    public int getY() { return super.getY(); }


    /**
     * Returns the current width of this component.
     * This method is preferable to writing
     * <code>component.getBounds().width</code>, or
     * <code>component.getSize().width</code> because it doesn't cause any
     * heap allocations.
     *
     * @return the current width of this component
     */
    @BeanProperty(bound = false)
    public int getWidth() { return super.getWidth(); }


    /**
     * Returns the current height of this component.
     * This method is preferable to writing
     * <code>component.getBounds().height</code>, or
     * <code>component.getSize().height</code> because it doesn't cause any
     * heap allocations.
     *
     * @return the current height of this component
     */
    @BeanProperty(bound = false)
    public int getHeight() { return super.getHeight(); }

    /**
     * Returns true if this component is completely opaque.
     * <p>
     * An opaque component paints every pixel within its
     * rectangular bounds. A non-opaque component paints only a subset of
     * its pixels or none at all, allowing the pixels underneath it to
     * "show through".  Therefore, a component that does not fully paint
     * its pixels provides a degree of transparency.
     * <p>
     * Subclasses that guarantee to always completely paint their contents
     * should override this method and return true.
     *
     * @return true if this component is completely opaque
     * @see #setOpaque
     */
    public boolean isOpaque() {
        return getFlag(IS_OPAQUE);
    }

    /**
     * If true the component paints every pixel within its bounds.
     * Otherwise, the component may not paint some or all of its
     * pixels, allowing the underlying pixels to show through.
     * <p>
     * The default value of this property is false for <code>JComponent</code>.
     * However, the default value for this property on most standard
     * <code>JComponent</code> subclasses (such as <code>JButton</code> and
     * <code>JTree</code>) is look-and-feel dependent.
     *
     * @param isOpaque  true if this component should be opaque
     * @see #isOpaque
     */
    @BeanProperty(expert = true, description
            = "The component's opacity")
    public void setOpaque(boolean isOpaque) {
        boolean oldValue = getFlag(IS_OPAQUE);
        setFlag(IS_OPAQUE, isOpaque);
        setFlag(OPAQUE_SET, true);
        firePropertyChange("opaque", oldValue, isOpaque);
    }


    /**
     * If the specified rectangle is completely obscured by any of this
     * component's opaque children then returns true.  Only direct children
     * are considered, more distant descendants are ignored.  A
     * <code>JComponent</code> is opaque if
     * <code>JComponent.isOpaque()</code> returns true, other lightweight
     * components are always considered transparent, and heavyweight components
     * are always considered opaque.
     *
     * @param x  x value of specified rectangle
     * @param y  y value of specified rectangle
     * @param width  width of specified rectangle
     * @param height height of specified rectangle
     * @return true if the specified rectangle is obscured by an opaque child
     */
    boolean rectangleIsObscured(int x,int y,int width,int height)
    {
        int numChildren = getComponentCount();

        for(int i = 0; i < numChildren; i++) {
            Component child = getComponent(i);
            int cx, cy, cw, ch;

            cx = child.getX();
            cy = child.getY();
            cw = child.getWidth();
            ch = child.getHeight();

            if (x >= cx && (x + width) <= (cx + cw) &&
                y >= cy && (y + height) <= (cy + ch) && child.isVisible()) {

                if(child instanceof JComponent) {
//                  System.out.println("A) checking opaque: " + ((JComponent)child).isOpaque() + "  " + child);
//                  System.out.print("B) ");
//                  Thread.dumpStack();
                    return child.isOpaque();
                } else {
                    /** Sometimes a heavy weight can have a bound larger than its peer size
                     *  so we should always draw under heavy weights
                     */
                    return false;
                }
            }
        }

        return false;
    }


    /**
     * Returns the <code>Component</code>'s "visible rect rectangle" -  the
     * intersection of the visible rectangles for the component <code>c</code>
     * and all of its ancestors.  The return value is stored in
     * <code>visibleRect</code>.
     *
     * @param c  the component
     * @param visibleRect  a <code>Rectangle</code> computed as the
     *          intersection of all visible rectangles for the component
     *          <code>c</code> and all of its ancestors -- this is the
     *          return value for this method
     * @see #getVisibleRect
     */
    @SuppressWarnings("removal")
    static final void computeVisibleRect(Component c, Rectangle visibleRect) {
        Container p = c.getParent();
        Rectangle bounds = c.getBounds();

        if (p == null || p instanceof Window || p instanceof Applet) {
            visibleRect.setBounds(0, 0, bounds.width, bounds.height);
        } else {
            computeVisibleRect(p, visibleRect);
            visibleRect.x -= bounds.x;
            visibleRect.y -= bounds.y;
            SwingUtilities.computeIntersection(0,0,bounds.width,bounds.height,visibleRect);
        }
    }


    /**
     * Returns the <code>Component</code>'s "visible rect rectangle" -  the
     * intersection of the visible rectangles for this component
     * and all of its ancestors.  The return value is stored in
     * <code>visibleRect</code>.
     *
     * @param visibleRect a <code>Rectangle</code> computed as the
     *          intersection of all visible rectangles for this
     *          component and all of its ancestors -- this is the return
     *          value for this method
     * @see #getVisibleRect
     */
    public void computeVisibleRect(Rectangle visibleRect) {
        computeVisibleRect(this, visibleRect);
    }


    /**
     * Returns the <code>Component</code>'s "visible rectangle" -  the
     * intersection of this component's visible rectangle,
     * <code>new Rectangle(0, 0, getWidth(), getHeight())</code>,
     * and all of its ancestors' visible rectangles.
     *
     * @return the visible rectangle
     */
    @BeanProperty(bound = false)
    public Rectangle getVisibleRect() {
        Rectangle visibleRect = new Rectangle();

        computeVisibleRect(visibleRect);
        return visibleRect;
    }

    /**
     * Support for reporting bound property changes for boolean properties.
     * This method can be called when a bound property has changed and it will
     * send the appropriate PropertyChangeEvent to any registered
     * PropertyChangeListeners.
     *
     * @param propertyName the property whose value has changed
     * @param oldValue the property's previous value
     * @param newValue the property's new value
     */
    public void firePropertyChange(String propertyName,
                                   boolean oldValue, boolean newValue) {
        super.firePropertyChange(propertyName, oldValue, newValue);
    }


    /**
     * Support for reporting bound property changes for integer properties.
     * This method can be called when a bound property has changed and it will
     * send the appropriate PropertyChangeEvent to any registered
     * PropertyChangeListeners.
     *
     * @param propertyName the property whose value has changed
     * @param oldValue the property's previous value
     * @param newValue the property's new value
     */
    public void firePropertyChange(String propertyName,
                                      int oldValue, int newValue) {
        super.firePropertyChange(propertyName, oldValue, newValue);
    }

    // XXX This method is implemented as a workaround to a JLS issue with ambiguous
    // methods. This should be removed once 4758654 is resolved.
    public void firePropertyChange(String propertyName, char oldValue, char newValue) {
        super.firePropertyChange(propertyName, oldValue, newValue);
    }

    /**
     * Supports reporting constrained property changes.
     * This method can be called when a constrained property has changed
     * and it will send the appropriate <code>PropertyChangeEvent</code>
     * to any registered <code>VetoableChangeListeners</code>.
     *
     * @param propertyName  the name of the property that was listened on
     * @param oldValue  the old value of the property
     * @param newValue  the new value of the property
     * @exception java.beans.PropertyVetoException when the attempt to set the
     *          property is vetoed by the component
     */
    protected void fireVetoableChange(String propertyName, Object oldValue, Object newValue)
        throws java.beans.PropertyVetoException
    {
        if (vetoableChangeSupport == null) {
            return;
        }
        vetoableChangeSupport.fireVetoableChange(propertyName, oldValue, newValue);
    }


    /**
     * Adds a <code>VetoableChangeListener</code> to the listener list.
     * The listener is registered for all properties.
     *
     * @param listener  the <code>VetoableChangeListener</code> to be added
     */
    public synchronized void addVetoableChangeListener(VetoableChangeListener listener) {
        if (vetoableChangeSupport == null) {
            vetoableChangeSupport = new java.beans.VetoableChangeSupport(this);
        }
        vetoableChangeSupport.addVetoableChangeListener(listener);
    }


    /**
     * Removes a <code>VetoableChangeListener</code> from the listener list.
     * This removes a <code>VetoableChangeListener</code> that was registered
     * for all properties.
     *
     * @param listener  the <code>VetoableChangeListener</code> to be removed
     */
    public synchronized void removeVetoableChangeListener(VetoableChangeListener listener) {
        if (vetoableChangeSupport == null) {
            return;
        }
        vetoableChangeSupport.removeVetoableChangeListener(listener);
    }


    /**
     * Returns an array of all the vetoable change listeners
     * registered on this component.
     *
     * @return all of the component's <code>VetoableChangeListener</code>s
     *         or an empty
     *         array if no vetoable change listeners are currently registered
     *
     * @see #addVetoableChangeListener
     * @see #removeVetoableChangeListener
     *
     * @since 1.4
     */
    @BeanProperty(bound = false)
    public synchronized VetoableChangeListener[] getVetoableChangeListeners() {
        if (vetoableChangeSupport == null) {
            return new VetoableChangeListener[0];
        }
        return vetoableChangeSupport.getVetoableChangeListeners();
    }


    /**
     * Returns the top-level ancestor of this component (either the
     * containing <code>Window</code> or <code>Applet</code>),
     * or <code>null</code> if this component has not
     * been added to any container.
     *
     * @return the top-level <code>Container</code> that this component is in,
     *          or <code>null</code> if not in any container
     */
    @BeanProperty(bound = false)
    @SuppressWarnings("removal")
    public Container getTopLevelAncestor() {
        for(Container p = this; p != null; p = p.getParent()) {
            if(p instanceof Window || p instanceof Applet) {
                return p;
            }
        }
        return null;
    }

    private AncestorNotifier getAncestorNotifier() {
        return (AncestorNotifier)
            getClientProperty(JComponent_ANCESTOR_NOTIFIER);
    }

    /**
     * Registers <code>listener</code> so that it will receive
     * <code>AncestorEvents</code> when it or any of its ancestors
     * move or are made visible or invisible.
     * Events are also sent when the component or its ancestors are added
     * or removed from the containment hierarchy.
     *
     * @param listener  the <code>AncestorListener</code> to register
     * @see AncestorEvent
     */
    public void addAncestorListener(AncestorListener listener) {
        AncestorNotifier ancestorNotifier = getAncestorNotifier();
        if (ancestorNotifier == null) {
            ancestorNotifier = new AncestorNotifier(this);
            putClientProperty(JComponent_ANCESTOR_NOTIFIER,
                              ancestorNotifier);
        }
        ancestorNotifier.addAncestorListener(listener);
    }

    /**
     * Unregisters <code>listener</code> so that it will no longer receive
     * <code>AncestorEvents</code>.
     *
     * @param listener  the <code>AncestorListener</code> to be removed
     * @see #addAncestorListener
     */
    public void removeAncestorListener(AncestorListener listener) {
        AncestorNotifier ancestorNotifier = getAncestorNotifier();
        if (ancestorNotifier == null) {
            return;
        }
        ancestorNotifier.removeAncestorListener(listener);
        if (ancestorNotifier.listenerList.getListenerList().length == 0) {
            ancestorNotifier.removeAllListeners();
            putClientProperty(JComponent_ANCESTOR_NOTIFIER, null);
        }
    }

    /**
     * Returns an array of all the ancestor listeners
     * registered on this component.
     *
     * @return all of the component's <code>AncestorListener</code>s
     *         or an empty
     *         array if no ancestor listeners are currently registered
     *
     * @see #addAncestorListener
     * @see #removeAncestorListener
     *
     * @since 1.4
     */
    @BeanProperty(bound = false)
    public AncestorListener[] getAncestorListeners() {
        AncestorNotifier ancestorNotifier = getAncestorNotifier();
        if (ancestorNotifier == null) {
            return new AncestorListener[0];
        }
        return ancestorNotifier.getAncestorListeners();
    }

    /**
     * Returns an array of all the objects currently registered
     * as <code><em>Foo</em>Listener</code>s
     * upon this <code>JComponent</code>.
     * <code><em>Foo</em>Listener</code>s are registered using the
     * <code>add<em>Foo</em>Listener</code> method.
     *
     * <p>
     *
     * You can specify the <code>listenerType</code> argument
     * with a class literal,
     * such as
     * <code><em>Foo</em>Listener.class</code>.
     * For example, you can query a
     * <code>JComponent</code> <code>c</code>
     * for its mouse listeners with the following code:
     * <pre>MouseListener[] mls = (MouseListener[])(c.getListeners(MouseListener.class));</pre>
     * If no such listeners exist, this method returns an empty array.
     *
     * @param listenerType the type of listeners requested; this parameter
     *          should specify an interface that descends from
     *          <code>java.util.EventListener</code>
     * @return an array of all objects registered as
     *          <code><em>Foo</em>Listener</code>s on this component,
     *          or an empty array if no such
     *          listeners have been added
     * @exception ClassCastException if <code>listenerType</code>
     *          doesn't specify a class or interface that implements
     *          <code>java.util.EventListener</code>
     *
     * @since 1.3
     *
     * @see #getVetoableChangeListeners
     * @see #getAncestorListeners
     */
    @SuppressWarnings("unchecked") // Casts to (T[])
    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        T[] result;
        if (listenerType == AncestorListener.class) {
            // AncestorListeners are handled by the AncestorNotifier
            result = (T[])getAncestorListeners();
        }
        else if (listenerType == VetoableChangeListener.class) {
            // VetoableChangeListeners are handled by VetoableChangeSupport
            result = (T[])getVetoableChangeListeners();
        }
        else if (listenerType == PropertyChangeListener.class) {
            // PropertyChangeListeners are handled by PropertyChangeSupport
            result = (T[])getPropertyChangeListeners();
        }
        else {
            result = listenerList.getListeners(listenerType);
        }

        if (result.length == 0) {
            return super.getListeners(listenerType);
        }
        return result;
    }

    /**
     * Notifies this component that it now has a parent component.
     * When this method is invoked, the chain of parent components is
     * set up with <code>KeyboardAction</code> event listeners.
     * This method is called by the toolkit internally and should
     * not be called directly by programs.
     *
     * @see #registerKeyboardAction
     */
    public void addNotify() {
        super.addNotify();
        firePropertyChange("ancestor", null, getParent());

        registerWithKeyboardManager(false);
        registerNextFocusableComponent();
    }


    /**
     * Notifies this component that it no longer has a parent component.
     * When this method is invoked, any <code>KeyboardAction</code>s
     * set up in the chain of parent components are removed.
     * This method is called by the toolkit internally and should
     * not be called directly by programs.
     *
     * @see #registerKeyboardAction
     */
    public void removeNotify() {
        super.removeNotify();
        // This isn't strictly correct.  The event shouldn't be
        // fired until *after* the parent is set to null.  But
        // we only get notified before that happens
        firePropertyChange("ancestor", getParent(), null);

        unregisterWithKeyboardManager();
        deregisterNextFocusableComponent();

        if (getCreatedDoubleBuffer()) {
            RepaintManager.currentManager(this).resetDoubleBuffer();
            setCreatedDoubleBuffer(false);
        }
        if (autoscrolls) {
            Autoscroller.stop(this);
        }
    }


    /**
     * Adds the specified region to the dirty region list if the component
     * is showing.  The component will be repainted after all of the
     * currently pending events have been dispatched.
     *
     * @param tm  this parameter is not used
     * @param x  the x value of the dirty region
     * @param y  the y value of the dirty region
     * @param width  the width of the dirty region
     * @param height  the height of the dirty region
     * @see #isPaintingOrigin()
     * @see java.awt.Component#isShowing
     * @see RepaintManager#addDirtyRegion
     */
    public void repaint(long tm, int x, int y, int width, int height) {
        RepaintManager.currentManager(SunToolkit.targetToAppContext(this))
                      .addDirtyRegion(this, x, y, width, height);
    }


    /**
     * Adds the specified region to the dirty region list if the component
     * is showing.  The component will be repainted after all of the
     * currently pending events have been dispatched.
     *
     * @param  r a <code>Rectangle</code> containing the dirty region
     * @see #isPaintingOrigin()
     * @see java.awt.Component#isShowing
     * @see RepaintManager#addDirtyRegion
     */
    public void repaint(Rectangle r) {
        repaint(0,r.x,r.y,r.width,r.height);
    }


    /**
     * Supports deferred automatic layout.
     * <p>
     * Calls <code>invalidate</code> and then adds this component's
     * <code>validateRoot</code> to a list of components that need to be
     * validated.  Validation will occur after all currently pending
     * events have been dispatched.  In other words after this method
     * is called,  the first validateRoot (if any) found when walking
     * up the containment hierarchy of this component will be validated.
     * By default, <code>JRootPane</code>, <code>JScrollPane</code>,
     * and <code>JTextField</code> return true
     * from <code>isValidateRoot</code>.
     * <p>
     * This method will automatically be called on this component
     * when a property value changes such that size, location, or
     * internal layout of this component has been affected.  This automatic
     * updating differs from the AWT because programs generally no
     * longer need to invoke <code>validate</code> to get the contents of the
     * GUI to update.
     *
     * @see java.awt.Component#invalidate
     * @see java.awt.Container#validate
     * @see #isValidateRoot
     * @see RepaintManager#addInvalidComponent
     */
    public void revalidate() {
        if (getParent() == null) {
            // Note: We don't bother invalidating here as once added
            // to a valid parent invalidate will be invoked (addImpl
            // invokes addNotify which will invoke invalidate on the
            // new Component). Also, if we do add a check to isValid
            // here it can potentially be called before the constructor
            // which was causing some people grief.
            return;
        }
        if (SunToolkit.isDispatchThreadForAppContext(this)) {
            invalidate();
            RepaintManager.currentManager(this).addInvalidComponent(this);
        }
        else {
            // To avoid a flood of Runnables when constructing GUIs off
            // the EDT, a flag is maintained as to whether or not
            // a Runnable has been scheduled.
            if (revalidateRunnableScheduled.getAndSet(true)) {
                return;
            }
            SunToolkit.executeOnEventHandlerThread(this, () -> {
                revalidateRunnableScheduled.set(false);
                revalidate();
            });
        }
    }

    /**
     * If this method returns true, <code>revalidate</code> calls by
     * descendants of this component will cause the entire tree
     * beginning with this root to be validated.
     * Returns false by default.  <code>JScrollPane</code> overrides
     * this method and returns true.
     *
     * @return always returns false
     * @see #revalidate
     * @see java.awt.Component#invalidate
     * @see java.awt.Container#validate
     * @see java.awt.Container#isValidateRoot
     */
    @Override
    public boolean isValidateRoot() {
        return false;
    }


    /**
     * Returns true if this component tiles its children -- that is, if
     * it can guarantee that the children will not overlap.  The
     * repainting system is substantially more efficient in this
     * common case.  <code>JComponent</code> subclasses that can't make this
     * guarantee, such as <code>JLayeredPane</code>,
     * should override this method to return false.
     *
     * @return always returns true
     */
    @BeanProperty(bound = false)
    public boolean isOptimizedDrawingEnabled() {
        return true;
    }

    /**
     * Returns {@code true} if a paint triggered on a child component should cause
     * painting to originate from this Component, or one of its ancestors.
     * <p>
     * Calling {@link #repaint} or {@link #paintImmediately(int, int, int, int)}
     * on a Swing component will result in calling
     * the {@link JComponent#paintImmediately(int, int, int, int)} method of
     * the first ancestor which {@code isPaintingOrigin()} returns {@code true}, if there are any.
     * <p>
     * {@code JComponent} subclasses that need to be painted when any of their
     * children are repainted should override this method to return {@code true}.
     *
     * @return always returns {@code false}
     *
     * @see #paintImmediately(int, int, int, int)
     */
    protected boolean isPaintingOrigin() {
        return false;
    }

    /**
     * Paints the specified region in this component and all of its
     * descendants that overlap the region, immediately.
     * <p>
     * It's rarely necessary to call this method.  In most cases it's
     * more efficient to call repaint, which defers the actual painting
     * and can collapse redundant requests into a single paint call.
     * This method is useful if one needs to update the display while
     * the current event is being dispatched.
     * <p>
     * This method is to be overridden when the dirty region needs to be changed
     * for components that are painting origins.
     *
     * @param x  the x value of the region to be painted
     * @param y  the y value of the region to be painted
     * @param w  the width of the region to be painted
     * @param h  the height of the region to be painted
     * @see #repaint
     * @see #isPaintingOrigin()
     */
    public void paintImmediately(int x,int y,int w, int h) {
        Component c = this;
        Component parent;

        if(!isShowing()) {
            return;
        }

        JComponent paintingOigin = SwingUtilities.getPaintingOrigin(this);
        if (paintingOigin != null) {
            Rectangle rectangle = SwingUtilities.convertRectangle(
                    c, new Rectangle(x, y, w, h), paintingOigin);
            paintingOigin.paintImmediately(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
            return;
        }

        while(!c.isOpaque()) {
            parent = c.getParent();
            if(parent != null) {
                x += c.getX();
                y += c.getY();
                c = parent;
            } else {
                break;
            }

            if(!(c instanceof JComponent)) {
                break;
            }
        }
        if(c instanceof JComponent) {
            ((JComponent)c)._paintImmediately(x,y,w,h);
        } else {
            c.repaint(x,y,w,h);
        }
    }

    /**
     * Paints the specified region now.
     *
     * @param r a <code>Rectangle</code> containing the region to be painted
     */
    public void paintImmediately(Rectangle r) {
        paintImmediately(r.x,r.y,r.width,r.height);
    }

    /**
     * Returns whether this component should be guaranteed to be on top.
     * For example, it would make no sense for <code>Menu</code>s to pop up
     * under another component, so they would always return true.
     * Most components will want to return false, hence that is the default.
     *
     * @return always returns false
     */
    // package private
    boolean alwaysOnTop() {
        return false;
    }

    void setPaintingChild(Component paintingChild) {
        this.paintingChild = paintingChild;
    }

    @SuppressWarnings("removal")
    void _paintImmediately(int x, int y, int w, int h) {
        Graphics g;
        Container c;
        Rectangle b;

        int tmpX, tmpY, tmpWidth, tmpHeight;
        int offsetX=0,offsetY=0;

        boolean hasBuffer = false;

        JComponent bufferedComponent = null;
        JComponent paintingComponent = this;

        RepaintManager repaintManager = RepaintManager.currentManager(this);
        // parent Container's up to Window or Applet. First container is
        // the direct parent. Note that in testing it was faster to
        // alloc a new Vector vs keeping a stack of them around, and gc
        // seemed to have a minimal effect on this.
        java.util.List<Component> path = new java.util.ArrayList<Component>(7);
        int pIndex = -1;
        int pCount = 0;

        tmpX = tmpY = tmpWidth = tmpHeight = 0;

        Rectangle paintImmediatelyClip = fetchRectangle();
        paintImmediatelyClip.x = x;
        paintImmediatelyClip.y = y;
        paintImmediatelyClip.width = w;
        paintImmediatelyClip.height = h;


        // System.out.println("1) ************* in _paintImmediately for " + this);

        boolean ontop = alwaysOnTop() && isOpaque();
        if (ontop) {
            SwingUtilities.computeIntersection(0, 0, getWidth(), getHeight(),
                                               paintImmediatelyClip);
            if (paintImmediatelyClip.width == 0) {
                recycleRectangle(paintImmediatelyClip);
                return;
            }
        }
        Component child;
        for (c = this, child = null;
             c != null && !(c instanceof Window) && !(c instanceof Applet);
             child = c, c = c.getParent()) {
                JComponent jc = (c instanceof JComponent) ? (JComponent)c :
                                null;
                path.add(c);
                if(!ontop && jc != null && !jc.isOptimizedDrawingEnabled()) {
                    boolean resetPC;

                    // Children of c may overlap, three possible cases for the
                    // painting region:
                    // . Completely obscured by an opaque sibling, in which
                    //   case there is no need to paint.
                    // . Partially obscured by a sibling: need to start
                    //   painting from c.
                    // . Otherwise we aren't obscured and thus don't need to
                    //   start painting from parent.
                    if (c != this) {
                        if (jc.isPaintingOrigin()) {
                            resetPC = true;
                        }
                        else {
                            Component[] children = c.getComponents();
                            int i = 0;
                            for (; i<children.length; i++) {
                                if (children[i] == child) break;
                            }
                            switch (jc.getObscuredState(i,
                                            paintImmediatelyClip.x,
                                            paintImmediatelyClip.y,
                                            paintImmediatelyClip.width,
                                            paintImmediatelyClip.height)) {
                            case NOT_OBSCURED:
                                resetPC = false;
                                break;
                            case COMPLETELY_OBSCURED:
                                recycleRectangle(paintImmediatelyClip);
                                return;
                            default:
                                resetPC = true;
                                break;
                            }
                        }
                    }
                    else {
                        resetPC = false;
                    }

                    if (resetPC) {
                        // Get rid of any buffer since we draw from here and
                        // we might draw something larger
                        paintingComponent = jc;
                        pIndex = pCount;
                        offsetX = offsetY = 0;
                        hasBuffer = false;
                    }
                }
                pCount++;

                // look to see if the parent (and therefor this component)
                // is double buffered
                if(repaintManager.isDoubleBufferingEnabled() && jc != null &&
                                  jc.isDoubleBuffered()) {
                    hasBuffer = true;
                    bufferedComponent = jc;
                }

                // if we aren't on top, include the parent's clip
                if (!ontop) {
                    int bx = c.getX();
                    int by = c.getY();
                    tmpWidth = c.getWidth();
                    tmpHeight = c.getHeight();
                    SwingUtilities.computeIntersection(tmpX,tmpY,tmpWidth,tmpHeight,paintImmediatelyClip);
                    paintImmediatelyClip.x += bx;
                    paintImmediatelyClip.y += by;
                    offsetX += bx;
                    offsetY += by;
                }
        }

        // If the clip width or height is negative, don't bother painting
        if(c == null || !c.isDisplayable() ||
                        paintImmediatelyClip.width <= 0 ||
                        paintImmediatelyClip.height <= 0) {
            recycleRectangle(paintImmediatelyClip);
            return;
        }

        paintingComponent.setFlag(IS_REPAINTING, true);

        paintImmediatelyClip.x -= offsetX;
        paintImmediatelyClip.y -= offsetY;

        // Notify the Components that are going to be painted of the
        // child component to paint to.
        if(paintingComponent != this) {
            Component comp;
            int i = pIndex;
            for(; i > 0 ; i--) {
                comp = path.get(i);
                if(comp instanceof JComponent) {
                    ((JComponent)comp).setPaintingChild(path.get(i-1));
                }
            }
        }
        try {
            if ((g = safelyGetGraphics(paintingComponent, c)) != null) {
                try {
                    if (hasBuffer) {
                        RepaintManager rm = RepaintManager.currentManager(
                                bufferedComponent);
                        rm.beginPaint();
                        try {
                            rm.paint(paintingComponent, bufferedComponent, g,
                                    paintImmediatelyClip.x,
                                    paintImmediatelyClip.y,
                                    paintImmediatelyClip.width,
                                    paintImmediatelyClip.height);
                        } finally {
                            rm.endPaint();
                        }
                    } else {
                        g.setClip(paintImmediatelyClip.x, paintImmediatelyClip.y,
                                paintImmediatelyClip.width, paintImmediatelyClip.height);
                        paintingComponent.paint(g);
                    }
                } finally {
                    g.dispose();
                }
            }
        }
        finally {
            // Reset the painting child for the parent components.
            if(paintingComponent != this) {
                Component comp;
                int i = pIndex;
                for(; i > 0 ; i--) {
                    comp = path.get(i);
                    if(comp instanceof JComponent) {
                        ((JComponent)comp).setPaintingChild(null);
                    }
                }
            }
            paintingComponent.setFlag(IS_REPAINTING, false);
        }
        recycleRectangle(paintImmediatelyClip);
    }

    /**
     * Paints to the specified graphics.  This does not set the clip and it
     * does not adjust the Graphics in anyway, callers must do that first.
     * This method is package-private for RepaintManager.PaintManager and
     * its subclasses to call, it is NOT intended for general use outside
     * of that.
     */
    void paintToOffscreen(Graphics g, int x, int y, int w, int h, int maxX,
                          int maxY) {
        try {
            setFlag(ANCESTOR_USING_BUFFER, true);
            if ((y + h) < maxY || (x + w) < maxX) {
                setFlag(IS_PAINTING_TILE, true);
            }
            if (getFlag(IS_REPAINTING)) {
                // Called from paintImmediately (RepaintManager) to fill
                // repaint request
                paint(g);
            } else {
                // Called from paint() (AWT) to repair damage
                if(!rectangleIsObscured(x, y, w, h)) {
                    paintComponent(g);
                    paintBorder(g);
                }
                paintChildren(g);
            }
        } finally {
            setFlag(ANCESTOR_USING_BUFFER, false);
            setFlag(IS_PAINTING_TILE, false);
        }
    }

    /**
     * Returns whether or not the region of the specified component is
     * obscured by a sibling.
     *
     * @return NOT_OBSCURED if non of the siblings above the Component obscure
     *         it, COMPLETELY_OBSCURED if one of the siblings completely
     *         obscures the Component or PARTIALLY_OBSCURED if the Component is
     *         only partially obscured.
     */
    private int getObscuredState(int compIndex, int x, int y, int width,
                                 int height) {
        int retValue = NOT_OBSCURED;
        Rectangle tmpRect = fetchRectangle();

        for (int i = compIndex - 1 ; i >= 0 ; i--) {
            Component sibling = getComponent(i);
            if (!sibling.isVisible()) {
                continue;
            }
            Rectangle siblingRect;
            boolean opaque;
            if (sibling instanceof JComponent) {
                opaque = sibling.isOpaque();
                if (!opaque) {
                    if (retValue == PARTIALLY_OBSCURED) {
                        continue;
                    }
                }
            }
            else {
                opaque = true;
            }
            siblingRect = sibling.getBounds(tmpRect);
            if (opaque && x >= siblingRect.x && (x + width) <=
                     (siblingRect.x + siblingRect.width) &&
                     y >= siblingRect.y && (y + height) <=
                     (siblingRect.y + siblingRect.height)) {
                recycleRectangle(tmpRect);
                return COMPLETELY_OBSCURED;
            }
            else if (retValue == NOT_OBSCURED &&
                     !((x + width <= siblingRect.x) ||
                       (y + height <= siblingRect.y) ||
                       (x >= siblingRect.x + siblingRect.width) ||
                       (y >= siblingRect.y + siblingRect.height))) {
                retValue = PARTIALLY_OBSCURED;
            }
        }
        recycleRectangle(tmpRect);
        return retValue;
    }

    /**
     * Returns true, which implies that before checking if a child should
     * be painted it is first check that the child is not obscured by another
     * sibling. This is only checked if <code>isOptimizedDrawingEnabled</code>
     * returns false.
     *
     * @return always returns true
     */
    boolean checkIfChildObscuredBySibling() {
        return true;
    }


    private void setFlag(int aFlag, boolean aValue) {
        if(aValue) {
            flags |= (1 << aFlag);
        } else {
            flags &= ~(1 << aFlag);
        }
    }
    private boolean getFlag(int aFlag) {
        int mask = (1 << aFlag);
        return ((flags & mask) == mask);
    }
    // These functions must be static so that they can be called from
    // subclasses inside the package, but whose inheritance hierarhcy includes
    // classes outside of the package below JComponent (e.g., JTextArea).
    static void setWriteObjCounter(JComponent comp, byte count) {
        comp.flags = (comp.flags & ~(0xFF << WRITE_OBJ_COUNTER_FIRST)) |
                     (count << WRITE_OBJ_COUNTER_FIRST);
    }
    static byte getWriteObjCounter(JComponent comp) {
        return (byte)((comp.flags >> WRITE_OBJ_COUNTER_FIRST) & 0xFF);
    }

    /** Buffering **/

    /**
     *  Sets whether this component should use a buffer to paint.
     *  If set to true, all the drawing from this component will be done
     *  in an offscreen painting buffer. The offscreen painting buffer will
     *  the be copied onto the screen.
     *  If a <code>Component</code> is buffered and one of its ancestor
     *  is also buffered, the ancestor buffer will be used.
     *
     *  @param aFlag if true, set this component to be double buffered
     */
    public void setDoubleBuffered(boolean aFlag) {
        setFlag(IS_DOUBLE_BUFFERED,aFlag);
    }

    /**
     * Returns whether this component should use a buffer to paint.
     *
     * @return true if this component is double buffered, otherwise false
     */
    public boolean isDoubleBuffered() {
        return getFlag(IS_DOUBLE_BUFFERED);
    }

    /**
     * Returns the <code>JRootPane</code> ancestor for this component.
     *
     * @return the <code>JRootPane</code> that contains this component,
     *          or <code>null</code> if no <code>JRootPane</code> is found
     */
    @BeanProperty(bound = false)
    public JRootPane getRootPane() {
        return SwingUtilities.getRootPane(this);
    }


    /** Serialization **/

    /**
     * This is called from Component by way of reflection. Do NOT change
     * the name unless you change the code in Component as well.
     */
    void compWriteObjectNotify() {
        byte count = JComponent.getWriteObjCounter(this);
        JComponent.setWriteObjCounter(this, (byte)(count + 1));
        if (count != 0) {
            return;
        }

        uninstallUIAndProperties();

        /* JTableHeader is in a separate package, which prevents it from
         * being able to override this package-private method the way the
         * other components can.  We don't want to make this method protected
         * because it would introduce public-api for a less-than-desirable
         * serialization scheme, so we compromise with this 'instanceof' hack
         * for now.
         */
        if (getToolTipText() != null ||
            this instanceof javax.swing.table.JTableHeader) {
            ToolTipManager.sharedInstance().unregisterComponent(JComponent.this);
        }
    }

    /**
     * This object is the <code>ObjectInputStream</code> callback
     * that's called after a complete graph of objects (including at least
     * one <code>JComponent</code>) has been read.
     *  It sets the UI property of each Swing component
     * that was read to the current default with <code>updateUI</code>.
     * <p>
     * As each  component is read in we keep track of the current set of
     * root components here, in the roots vector.  Note that there's only one
     * <code>ReadObjectCallback</code> per <code>ObjectInputStream</code>,
     * they're stored in the static <code>readObjectCallbacks</code>
     * hashtable.
     *
     * @see java.io.ObjectInputStream#registerValidation
     * @see SwingUtilities#updateComponentTreeUI
     */
    private class ReadObjectCallback implements ObjectInputValidation
    {
        private final Vector<JComponent> roots = new Vector<JComponent>(1);
        private final ObjectInputStream inputStream;

        ReadObjectCallback(ObjectInputStream s) throws Exception {
            inputStream = s;
            s.registerValidation(this, 0);
        }

        /**
         * This is the method that's called after the entire graph
         * of objects has been read in.  It initializes
         * the UI property of all of the copmonents with
         * <code>SwingUtilities.updateComponentTreeUI</code>.
         */
        public void validateObject() throws InvalidObjectException {
            try {
                for (JComponent root : roots) {
                    SwingUtilities.updateComponentTreeUI(root);
                }
            }
            finally {
                readObjectCallbacks.remove(inputStream);
            }
        }

        /**
         * If <code>c</code> isn't a descendant of a component we've already
         * seen, then add it to the roots <code>Vector</code>.
         *
         * @param c the <code>JComponent</code> to add
         */
        private void registerComponent(JComponent c)
        {
            /* If the Component c is a descendant of one of the
             * existing roots (or it IS an existing root), we're done.
             */
            for (JComponent root : roots) {
                for(Component p = c; p != null; p = p.getParent()) {
                    if (p == root) {
                        return;
                    }
                }
            }

            /* Otherwise: if Component c is an ancestor of any of the
             * existing roots then remove them and add c (the "new root")
             * to the roots vector.
             */
            for(int i = 0; i < roots.size(); i++) {
                JComponent root = roots.elementAt(i);
                for(Component p = root.getParent(); p != null; p = p.getParent()) {
                    if (p == c) {
                        roots.removeElementAt(i--); // !!
                        break;
                    }
                }
            }

            roots.addElement(c);
        }
    }


    /**
     * We use the <code>ObjectInputStream</code> "registerValidation"
     * callback to update the UI for the entire tree of components
     * after they've all been read in.
     *
     * @param s  the <code>ObjectInputStream</code> from which to read
     */
    @Serial
    private void readObject(ObjectInputStream s)
        throws IOException, ClassNotFoundException
    {
        ObjectInputStream.GetField f = s.readFields();

        isAlignmentXSet = f.get("isAlignmentXSet", false);
        alignmentX = validateAlignment(f.get("alignmentX", 0f));
        isAlignmentYSet = f.get("isAlignmentYSet", false);
        alignmentY = validateAlignment(f.get("alignmentY", 0f));
        listenerList = (EventListenerList) f.get("listenerList", null);
        vetoableChangeSupport = (VetoableChangeSupport) f.get("vetoableChangeSupport", null);
        autoscrolls = f.get("autoscrolls", false);
        border = (Border) f.get("border", null);
        flags = f.get("flags", 0);
        inputVerifier = (InputVerifier) f.get("inputVerifier", null);
        verifyInputWhenFocusTarget = f.get("verifyInputWhenFocusTarget", false);
        popupMenu = (JPopupMenu) f.get("popupMenu", null);
        focusInputMap = (InputMap) f.get("focusInputMap", null);
        ancestorInputMap = (InputMap) f.get("ancestorInputMap", null);
        windowInputMap = (ComponentInputMap) f.get("windowInputMap", null);
        actionMap = (ActionMap) f.get("actionMap", null);

        /* If there's no ReadObjectCallback for this stream yet, that is, if
         * this is the first call to JComponent.readObject() for this
         * graph of objects, then create a callback and stash it
         * in the readObjectCallbacks table.  Note that the ReadObjectCallback
         * constructor takes care of calling s.registerValidation().
         */
        ReadObjectCallback cb = readObjectCallbacks.get(s);
        if (cb == null) {
            try {
                readObjectCallbacks.put(s, cb = new ReadObjectCallback(s));
            }
            catch (Exception e) {
                throw new IOException(e.toString());
            }
        }
        cb.registerComponent(this);

        // Read back the client properties.
        int cpCount = s.readInt();
        if (cpCount > 0) {
            clientProperties = new ArrayTable();
            for (int counter = 0; counter < cpCount; counter++) {
                clientProperties.put(s.readObject(),
                                     s.readObject());
            }
        }
        if (getToolTipText() != null) {
            ToolTipManager.sharedInstance().registerComponent(this);
        }
        setWriteObjCounter(this, (byte)0);
        revalidateRunnableScheduled = new AtomicBoolean(false);
    }


    /**
     * Before writing a <code>JComponent</code> to an
     * <code>ObjectOutputStream</code> we temporarily uninstall its UI.
     * This is tricky to do because we want to uninstall
     * the UI before any of the <code>JComponent</code>'s children
     * (or its <code>LayoutManager</code> etc.) are written,
     * and we don't want to restore the UI until the most derived
     * <code>JComponent</code> subclass has been stored.
     *
     * @param s the <code>ObjectOutputStream</code> in which to write
     */
    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        if (getUIClassID().equals(uiClassID)) {
            byte count = JComponent.getWriteObjCounter(this);
            JComponent.setWriteObjCounter(this, --count);
            if (count == 0 && ui != null) {
                ui.installUI(this);
            }
        }
        ArrayTable.writeArrayTable(s, clientProperties);
    }


    /**
     * Returns a string representation of this <code>JComponent</code>.
     * This method
     * is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be <code>null</code>.
     *
     * @return  a string representation of this <code>JComponent</code>
     */
    protected String paramString() {
        String preferredSizeString = (isPreferredSizeSet() ?
                                      getPreferredSize().toString() : "");
        String minimumSizeString = (isMinimumSizeSet() ?
                                    getMinimumSize().toString() : "");
        String maximumSizeString = (isMaximumSizeSet() ?
                                    getMaximumSize().toString() : "");
        String borderString = (border == null ? ""
                               : (border == this ? "this" : border.toString()));

        return super.paramString() +
        ",alignmentX=" + alignmentX +
        ",alignmentY=" + alignmentY +
        ",border=" + borderString +
        ",flags=" + flags +             // should beef this up a bit
        ",maximumSize=" + maximumSizeString +
        ",minimumSize=" + minimumSizeString +
        ",preferredSize=" + preferredSizeString;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Deprecated
    public void hide() {
        boolean showing = isShowing();
        super.hide();
        if (showing) {
            Container parent = getParent();
            if (parent != null) {
                Rectangle r = getBounds();
                parent.repaint(r.x, r.y, r.width, r.height);
            }
            revalidate();
        }
    }

}
