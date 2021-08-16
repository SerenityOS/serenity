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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FocusTraversalPolicy;
import java.awt.Graphics;
import java.awt.KeyboardFocusManager;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.Window;
import java.beans.BeanProperty;
import java.beans.JavaBean;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serial;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleValue;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;
import javax.swing.plaf.DesktopIconUI;
import javax.swing.plaf.InternalFrameUI;

import sun.awt.AppContext;
import sun.swing.SwingUtilities2;

/**
 * A lightweight object that provides many of the features of
 * a native frame, including dragging, closing, becoming an icon,
 * resizing, title display, and support for a menu bar.
 * For task-oriented documentation and examples of using internal frames,
 * see <a
 href="https://docs.oracle.com/javase/tutorial/uiswing/components/internalframe.html" target="_top">How to Use Internal Frames</a>,
 * a section in <em>The Java Tutorial</em>.
 *
 * <p>
 *
 * Generally,
 * you add <code>JInternalFrame</code>s to a <code>JDesktopPane</code>. The UI
 * delegates the look-and-feel-specific actions to the
 * <code>DesktopManager</code>
 * object maintained by the <code>JDesktopPane</code>.
 * <p>
 * The <code>JInternalFrame</code> content pane
 * is where you add child components.
 * As a convenience, the {@code add}, {@code remove}, and {@code setLayout}
 * methods of this class are overridden, so that they delegate calls
 * to the corresponding methods of the {@code ContentPane}.
 * For example, you can add a child component to an internal frame as follows:
 * <pre>
 *       internalFrame.add(child);
 * </pre>
 * And the child will be added to the contentPane.
 * The content pane is actually managed by an instance of
 * <code>JRootPane</code>,
 * which also manages a layout pane, glass pane, and
 * optional menu bar for the internal frame. Please see the
 * <code>JRootPane</code>
 * documentation for a complete description of these components.
 * Refer to {@link javax.swing.RootPaneContainer}
 * for details on adding, removing and setting the <code>LayoutManager</code>
 * of a <code>JInternalFrame</code>.
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
 * @see InternalFrameEvent
 * @see JDesktopPane
 * @see DesktopManager
 * @see JInternalFrame.JDesktopIcon
 * @see JRootPane
 * @see javax.swing.RootPaneContainer
 *
 * @author David Kloba
 * @author Rich Schiavi
 * @since 1.2
 */
@JavaBean(defaultProperty = "JMenuBar", description = "A frame container which is contained within another window.")
@SwingContainer(delegate = "getContentPane")
@SuppressWarnings("serial") // Same-version serialization only
public class JInternalFrame extends JComponent implements
        Accessible, WindowConstants,
        RootPaneContainer
{
    /**
     * @see #getUIClassID
     * @see #readObject
     */
    private static final String uiClassID = "InternalFrameUI";

    /**
     * The <code>JRootPane</code> instance that manages the
     * content pane
     * and optional menu bar for this internal frame, as well as the
     * glass pane.
     *
     * @see JRootPane
     * @see RootPaneContainer
     */
    protected JRootPane rootPane;

    /**
     * If true then calls to <code>add</code> and <code>setLayout</code>
     * will be forwarded to the <code>contentPane</code>. This is initially
     * false, but is set to true when the <code>JInternalFrame</code> is
     * constructed.
     *
     * @see #isRootPaneCheckingEnabled
     * @see #setRootPaneCheckingEnabled
     * @see javax.swing.RootPaneContainer
     */
    protected boolean rootPaneCheckingEnabled = false;

    /** The frame can be closed. */
    protected boolean closable;
    /** The frame has been closed. */
    protected boolean isClosed;
    /** The frame can be expanded to the size of the desktop pane. */
    protected boolean maximizable;
    /**
     * The frame has been expanded to its maximum size.
     * @see #maximizable
     */
    protected boolean isMaximum;
    /**
     * The frame can "iconified" (shrunk down and displayed as
     * an icon-image).
     * @see JInternalFrame.JDesktopIcon
     * @see #setIconifiable
     */
    protected boolean iconable;
    /**
     * The frame has been iconified.
     * @see #isIcon()
     */
    protected boolean isIcon;
    /** The frame's size can be changed. */
    protected boolean resizable;
    /** The frame is currently selected. */
    protected boolean isSelected;
    /** The icon shown in the top-left corner of this internal frame. */
    protected Icon frameIcon;
    /** The title displayed in this internal frame's title bar. */
    protected String  title;
    /**
     * The icon that is displayed when this internal frame is iconified.
     * @see #iconable
     */
    protected JDesktopIcon desktopIcon;

    private Cursor lastCursor;

    private boolean opened;

    private Rectangle normalBounds = null;

    private int defaultCloseOperation = DISPOSE_ON_CLOSE;

    /**
     * Contains the Component that focus is to go when
     * <code>restoreSubcomponentFocus</code> is invoked, that is,
     * <code>restoreSubcomponentFocus</code> sets this to the value returned
     * from <code>getMostRecentFocusOwner</code>.
     */
    private Component lastFocusOwner;

    /** Bound property name. */
    public static final String CONTENT_PANE_PROPERTY = "contentPane";
    /** Bound property name. */
    public static final String MENU_BAR_PROPERTY = "JMenuBar";
    /** Bound property name. */
    public static final String TITLE_PROPERTY = "title";
    /** Bound property name. */
    public static final String LAYERED_PANE_PROPERTY = "layeredPane";
    /** Bound property name. */
    public static final String ROOT_PANE_PROPERTY = "rootPane";
    /** Bound property name. */
    public static final String GLASS_PANE_PROPERTY = "glassPane";
    /** Bound property name. */
    public static final String FRAME_ICON_PROPERTY = "frameIcon";

    /**
     * Constrained property name indicated that this frame has
     * selected status.
     */
    public static final String IS_SELECTED_PROPERTY = "selected";
    /** Constrained property name indicating that the internal frame is closed. */
    public static final String IS_CLOSED_PROPERTY = "closed";
    /** Constrained property name indicating that the internal frame is maximized. */
    public static final String IS_MAXIMUM_PROPERTY = "maximum";
    /** Constrained property name indicating that the internal frame is iconified. */
    public static final String IS_ICON_PROPERTY = "icon";

    private static final Object PROPERTY_CHANGE_LISTENER_KEY =
        new StringBuilder("InternalFramePropertyChangeListener");

    private static void addPropertyChangeListenerIfNecessary() {
        if (AppContext.getAppContext().get(PROPERTY_CHANGE_LISTENER_KEY) ==
            null) {
            PropertyChangeListener focusListener =
                new FocusPropertyChangeListener();

            AppContext.getAppContext().put(PROPERTY_CHANGE_LISTENER_KEY,
                focusListener);

            KeyboardFocusManager.getCurrentKeyboardFocusManager().
                addPropertyChangeListener(focusListener);
        }
    }

    private static class FocusPropertyChangeListener implements
        PropertyChangeListener {
        public void propertyChange(PropertyChangeEvent e) {
            if (e.getPropertyName() == "permanentFocusOwner") {
                updateLastFocusOwner((Component)e.getNewValue());
            }
        }
    }

    private static void updateLastFocusOwner(Component component) {
        if (component != null) {
            Component parent = component;
            while (parent != null && !(parent instanceof Window)) {
                if (parent instanceof JInternalFrame) {
                    // Update lastFocusOwner for parent.
                    ((JInternalFrame)parent).setLastFocusOwner(component);
                }
                parent = parent.getParent();
            }
        }
    }

    /**
     * Creates a non-resizable, non-closable, non-maximizable,
     * non-iconifiable <code>JInternalFrame</code> with no title.
     */
    public JInternalFrame() {
        this("", false, false, false, false);
    }

    /**
     * Creates a non-resizable, non-closable, non-maximizable,
     * non-iconifiable <code>JInternalFrame</code> with the specified title.
     * Note that passing in a <code>null</code> <code>title</code> results in
     * unspecified behavior and possibly an exception.
     *
     * @param title  the non-<code>null</code> <code>String</code>
     *     to display in the title bar
     */
    public JInternalFrame(String title) {
        this(title, false, false, false, false);
    }

    /**
     * Creates a non-closable, non-maximizable, non-iconifiable
     * <code>JInternalFrame</code> with the specified title
     * and resizability.
     *
     * @param title      the <code>String</code> to display in the title bar
     * @param resizable  if <code>true</code>, the internal frame can be resized
     */
    public JInternalFrame(String title, boolean resizable) {
        this(title, resizable, false, false, false);
    }

    /**
     * Creates a non-maximizable, non-iconifiable <code>JInternalFrame</code>
     * with the specified title, resizability, and
     * closability.
     *
     * @param title      the <code>String</code> to display in the title bar
     * @param resizable  if <code>true</code>, the internal frame can be resized
     * @param closable   if <code>true</code>, the internal frame can be closed
     */
    public JInternalFrame(String title, boolean resizable, boolean closable) {
        this(title, resizable, closable, false, false);
    }

    /**
     * Creates a non-iconifiable <code>JInternalFrame</code>
     * with the specified title,
     * resizability, closability, and maximizability.
     *
     * @param title       the <code>String</code> to display in the title bar
     * @param resizable   if <code>true</code>, the internal frame can be resized
     * @param closable    if <code>true</code>, the internal frame can be closed
     * @param maximizable if <code>true</code>, the internal frame can be maximized
     */
    public JInternalFrame(String title, boolean resizable, boolean closable,
                          boolean maximizable) {
        this(title, resizable, closable, maximizable, false);
    }

    /**
     * Creates a <code>JInternalFrame</code> with the specified title,
     * resizability, closability, maximizability, and iconifiability.
     * All <code>JInternalFrame</code> constructors use this one.
     *
     * @param title       the <code>String</code> to display in the title bar
     * @param resizable   if <code>true</code>, the internal frame can be resized
     * @param closable    if <code>true</code>, the internal frame can be closed
     * @param maximizable if <code>true</code>, the internal frame can be maximized
     * @param iconifiable if <code>true</code>, the internal frame can be iconified
     */
    public JInternalFrame(String title, boolean resizable, boolean closable,
                                boolean maximizable, boolean iconifiable) {

        setRootPane(createRootPane());
        setLayout(new BorderLayout());
        this.title = title;
        this.resizable = resizable;
        this.closable = closable;
        this.maximizable = maximizable;
        isMaximum = false;
        this.iconable = iconifiable;
        isIcon = false;
        setVisible(false);
        setRootPaneCheckingEnabled(true);
        desktopIcon = new JDesktopIcon(this);
        updateUI();
        sun.awt.SunToolkit.checkAndSetPolicy(this);
        addPropertyChangeListenerIfNecessary();
    }

    /**
     * Called by the constructor to set up the <code>JRootPane</code>.
     * @return  a new <code>JRootPane</code>
     * @see JRootPane
     */
    protected JRootPane createRootPane() {
        return new JRootPane();
    }

    /**
     * Returns the look-and-feel object that renders this component.
     *
     * @return the <code>InternalFrameUI</code> object that renders
     *          this component
     */
    public InternalFrameUI getUI() {
        return (InternalFrameUI)ui;
    }

    /**
     * Sets the UI delegate for this <code>JInternalFrame</code>.
     * @param ui  the UI delegate
     */
    @BeanProperty(hidden = true, visualUpdate = true, description
            = "The UI object that implements the Component's LookAndFeel.")
    public void setUI(InternalFrameUI ui) {
        boolean checkingEnabled = isRootPaneCheckingEnabled();
        try {
            setRootPaneCheckingEnabled(false);
            super.setUI(ui);
        }
        finally {
            setRootPaneCheckingEnabled(checkingEnabled);
        }
    }

    /**
     * Notification from the <code>UIManager</code> that the look and feel
     * has changed.
     * Replaces the current UI object with the latest version from the
     * <code>UIManager</code>.
     *
     * @see JComponent#updateUI
     */
    public void updateUI() {
        setUI((InternalFrameUI)UIManager.getUI(this));
        invalidate();
        if (desktopIcon != null) {
            desktopIcon.updateUIWhenHidden();
        }
    }

    /* This method is called if <code>updateUI</code> was called
     * on the associated
     * JDesktopIcon.  It's necessary to avoid infinite recursion.
     */
    void updateUIWhenHidden() {
        setUI((InternalFrameUI)UIManager.getUI(this));
        invalidate();
        Component[] children = getComponents();
        if (children != null) {
            for (Component child : children) {
                SwingUtilities.updateComponentTreeUI(child);
            }
        }
    }


    /**
     * Returns the name of the look-and-feel
     * class that renders this component.
     *
     * @return the string "InternalFrameUI"
     *
     * @see JComponent#getUIClassID
     * @see UIDefaults#getUI
     */
    @BeanProperty(bound = false, description
            = "UIClassID")
    public String getUIClassID() {
        return uiClassID;
    }

    /**
     * Returns whether calls to <code>add</code> and
     * <code>setLayout</code> are forwarded to the <code>contentPane</code>.
     *
     * @return true if <code>add</code> and <code>setLayout</code>
     *         are forwarded; false otherwise
     *
     * @see #addImpl
     * @see #setLayout
     * @see #setRootPaneCheckingEnabled
     * @see javax.swing.RootPaneContainer
     */
    protected boolean isRootPaneCheckingEnabled() {
        return rootPaneCheckingEnabled;
    }

    /**
     * Sets whether calls to <code>add</code> and
     * <code>setLayout</code> are forwarded to the <code>contentPane</code>.
     *
     * @param enabled  true if <code>add</code> and <code>setLayout</code>
     *        are forwarded, false if they should operate directly on the
     *        <code>JInternalFrame</code>.
     *
     * @see #addImpl
     * @see #setLayout
     * @see #isRootPaneCheckingEnabled
     * @see javax.swing.RootPaneContainer
     */
    @BeanProperty(hidden = true, description
            = "Whether the add and setLayout methods are forwarded")
    protected void setRootPaneCheckingEnabled(boolean enabled) {
        rootPaneCheckingEnabled = enabled;
    }

    /**
     * Adds the specified child <code>Component</code>.
     * This method is overridden to conditionally forward calls to the
     * <code>contentPane</code>.
     * By default, children are added to the <code>contentPane</code> instead
     * of the frame, refer to {@link javax.swing.RootPaneContainer} for
     * details.
     *
     * @param comp the component to be enhanced
     * @param constraints the constraints to be respected
     * @param index the index
     * @exception IllegalArgumentException if <code>index</code> is invalid
     * @exception IllegalArgumentException if adding the container's parent
     *                  to itself
     * @exception IllegalArgumentException if adding a window to a container
     *
     * @see #setRootPaneCheckingEnabled
     * @see javax.swing.RootPaneContainer
     */
    protected void addImpl(Component comp, Object constraints, int index) {
        if(isRootPaneCheckingEnabled()) {
            getContentPane().add(comp, constraints, index);
        }
        else {
            super.addImpl(comp, constraints, index);
        }
    }

    /**
     * Removes the specified component from the container. If
     * <code>comp</code> is not a child of the <code>JInternalFrame</code>
     * this will forward the call to the <code>contentPane</code>.
     *
     * @param comp the component to be removed
     * @throws NullPointerException if <code>comp</code> is null
     * @see #add
     * @see javax.swing.RootPaneContainer
     */
    public void remove(Component comp) {
        int oldCount = getComponentCount();
        super.remove(comp);
        if (oldCount == getComponentCount()) {
            getContentPane().remove(comp);
        }
    }


    /**
     * Ensures that, by default, the layout of this component cannot be set.
     * Overridden to conditionally forward the call to the
     * <code>contentPane</code>.
     * Refer to {@link javax.swing.RootPaneContainer} for
     * more information.
     *
     * @param manager the <code>LayoutManager</code>
     * @see #setRootPaneCheckingEnabled
     */
    public void setLayout(LayoutManager manager) {
        if(isRootPaneCheckingEnabled()) {
            getContentPane().setLayout(manager);
        }
        else {
            super.setLayout(manager);
        }
    }


//////////////////////////////////////////////////////////////////////////
/// Property Methods
//////////////////////////////////////////////////////////////////////////

    /**
     * Returns the current <code>JMenuBar</code> for this
     * <code>JInternalFrame</code>, or <code>null</code>
     * if no menu bar has been set.
     * @return the current menu bar, or <code>null</code> if none has been set
     *
     * @deprecated As of Swing version 1.0.3,
     * replaced by <code>getJMenuBar()</code>.
     */
    @Deprecated
    public JMenuBar getMenuBar() {
      return getRootPane().getMenuBar();
    }

    /**
     * Returns the current <code>JMenuBar</code> for this
     * <code>JInternalFrame</code>, or <code>null</code>
     * if no menu bar has been set.
     *
     * @return  the <code>JMenuBar</code> used by this internal frame
     * @see #setJMenuBar
     */
    public JMenuBar getJMenuBar() {
        return getRootPane().getJMenuBar();
    }

    /**
     * Sets the <code>menuBar</code> property for this <code>JInternalFrame</code>.
     *
     * @param m  the <code>JMenuBar</code> to use in this internal frame
     * @see #getJMenuBar
     * @deprecated As of Swing version 1.0.3
     *  replaced by <code>setJMenuBar(JMenuBar m)</code>.
     */
    @Deprecated
    public void setMenuBar(JMenuBar m) {
        JMenuBar oldValue = getMenuBar();
        getRootPane().setJMenuBar(m);
        firePropertyChange(MENU_BAR_PROPERTY, oldValue, m);
    }

    /**
     * Sets the <code>menuBar</code> property for this <code>JInternalFrame</code>.
     *
     * @param m  the <code>JMenuBar</code> to use in this internal frame
     * @see #getJMenuBar
     */
    @BeanProperty(preferred = true, description
            = "The menu bar for accessing pulldown menus from this internal frame.")
    public void setJMenuBar(JMenuBar m){
        JMenuBar oldValue = getMenuBar();
        getRootPane().setJMenuBar(m);
        firePropertyChange(MENU_BAR_PROPERTY, oldValue, m);
    }

    // implements javax.swing.RootPaneContainer
    /**
     * Returns the content pane for this internal frame.
     * @return the content pane
     */
    public Container getContentPane() {
        return getRootPane().getContentPane();
    }


    /**
     * Sets this <code>JInternalFrame</code>'s <code>contentPane</code>
     * property.
     *
     * @param c  the content pane for this internal frame
     *
     * @exception java.awt.IllegalComponentStateException (a runtime
     *           exception) if the content pane parameter is <code>null</code>
     * @see RootPaneContainer#getContentPane
     */
    @BeanProperty(hidden = true, description
            = "The client area of the internal frame where child components are normally inserted.")
    public void setContentPane(Container c) {
        Container oldValue = getContentPane();
        getRootPane().setContentPane(c);
        firePropertyChange(CONTENT_PANE_PROPERTY, oldValue, c);
    }

    /**
     * Returns the layered pane for this internal frame.
     *
     * @return a <code>JLayeredPane</code> object
     * @see RootPaneContainer#setLayeredPane
     * @see RootPaneContainer#getLayeredPane
     */
    public JLayeredPane getLayeredPane() {
        return getRootPane().getLayeredPane();
    }

    /**
     * Sets this <code>JInternalFrame</code>'s
     * <code>layeredPane</code> property.
     *
     * @param layered the <code>JLayeredPane</code> for this internal frame
     *
     * @exception java.awt.IllegalComponentStateException (a runtime
     *           exception) if the layered pane parameter is <code>null</code>
     * @see RootPaneContainer#setLayeredPane
     */
    @BeanProperty(hidden = true, description
            = "The pane which holds the various desktop layers.")
    public void setLayeredPane(JLayeredPane layered) {
        JLayeredPane oldValue = getLayeredPane();
        getRootPane().setLayeredPane(layered);
        firePropertyChange(LAYERED_PANE_PROPERTY, oldValue, layered);
    }

    /**
     * Returns the glass pane for this internal frame.
     *
     * @return the glass pane
     * @see RootPaneContainer#setGlassPane
     */
    public Component getGlassPane() {
        return getRootPane().getGlassPane();
    }

    /**
     * Sets this <code>JInternalFrame</code>'s
     * <code>glassPane</code> property.
     *
     * @param glass the glass pane for this internal frame
     * @see RootPaneContainer#getGlassPane
     */
    @BeanProperty(hidden = true, description
            = "A transparent pane used for menu rendering.")
    public void setGlassPane(Component glass) {
        Component oldValue = getGlassPane();
        getRootPane().setGlassPane(glass);
        firePropertyChange(GLASS_PANE_PROPERTY, oldValue, glass);
    }

    /**
     * Returns the <code>rootPane</code> object for this internal frame.
     *
     * @return the <code>rootPane</code> property
     * @see RootPaneContainer#getRootPane
     */
    @BeanProperty(hidden = true, description
            = "The root pane used by this internal frame.")
    public JRootPane getRootPane() {
        return rootPane;
    }


    /**
     * Sets the <code>rootPane</code> property
     * for this <code>JInternalFrame</code>.
     * This method is called by the constructor.
     *
     * @param root  the new <code>JRootPane</code> object
     */
    protected void setRootPane(JRootPane root) {
        if(rootPane != null) {
            remove(rootPane);
        }
        JRootPane oldValue = getRootPane();
        rootPane = root;
        if(rootPane != null) {
            boolean checkingEnabled = isRootPaneCheckingEnabled();
            try {
                setRootPaneCheckingEnabled(false);
                add(rootPane, BorderLayout.CENTER);
            }
            finally {
                setRootPaneCheckingEnabled(checkingEnabled);
            }
        }
        firePropertyChange(ROOT_PANE_PROPERTY, oldValue, root);
    }

    /**
     * Sets whether this <code>JInternalFrame</code> can be closed by
     * some user action.
     * @param b a boolean value, where <code>true</code> means this internal frame can be closed
     */
    @BeanProperty(preferred = true, description
            = "Indicates whether this internal frame can be closed.")
    public void setClosable(boolean b) {
        Boolean oldValue = closable ? Boolean.TRUE : Boolean.FALSE;
        Boolean newValue = b ? Boolean.TRUE : Boolean.FALSE;
        closable = b;
        firePropertyChange("closable", oldValue, newValue);
    }

    /**
     * Returns whether this <code>JInternalFrame</code> can be closed by
     * some user action.
     * @return <code>true</code> if this internal frame can be closed
     */
    public boolean isClosable() {
        return closable;
    }

    /**
     * Returns whether this <code>JInternalFrame</code> is currently closed.
     * @return <code>true</code> if this internal frame is closed, <code>false</code> otherwise
     */
    public boolean isClosed() {
        return isClosed;
    }

    /**
     * Closes this internal frame if the argument is <code>true</code>.
     * Do not invoke this method with a <code>false</code> argument;
     * the result of invoking <code>setClosed(false)</code>
     * is unspecified.
     *
     * <p>
     *
     * If the internal frame is already closed,
     * this method does nothing and returns immediately.
     * Otherwise,
     * this method begins by firing
     * an <code>INTERNAL_FRAME_CLOSING</code> event.
     * Then this method sets the <code>closed</code> property to <code>true</code>
     * unless a listener vetoes the property change.
     * This method finishes by making the internal frame
     * invisible and unselected,
     * and then firing an <code>INTERNAL_FRAME_CLOSED</code> event.
     *
     * <p>
     *
     * <b>Note:</b>
     * To reuse an internal frame that has been closed,
     * you must add it to a container
     * (even if you never removed it from its previous container).
     * Typically, this container will be the <code>JDesktopPane</code>
     * that previously contained the internal frame.
     *
     * @param b must be <code>true</code>
     *
     * @exception PropertyVetoException when the attempt to set the
     *            property is vetoed by the <code>JInternalFrame</code>
     *
     * @see #isClosed()
     * @see #setDefaultCloseOperation
     * @see #dispose
     * @see javax.swing.event.InternalFrameEvent#INTERNAL_FRAME_CLOSING
     */
    @BeanProperty(description
            = "Indicates whether this internal frame has been closed.")
    public void setClosed(boolean b) throws PropertyVetoException {
        if (isClosed == b) {
            return;
        }

        Boolean oldValue = isClosed ? Boolean.TRUE : Boolean.FALSE;
        Boolean newValue = b ? Boolean.TRUE : Boolean.FALSE;
        if (b) {
          fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_CLOSING);
        }
        fireVetoableChange(IS_CLOSED_PROPERTY, oldValue, newValue);
        isClosed = b;
        if (isClosed) {
          setVisible(false);
        }
        firePropertyChange(IS_CLOSED_PROPERTY, oldValue, newValue);
        if (isClosed) {
          dispose();
        } else if (!opened) {
          /* this bogus -- we haven't defined what
             setClosed(false) means. */
          //        fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_OPENED);
          //            opened = true;
        }
    }

    /**
     * Sets whether the <code>JInternalFrame</code> can be resized by some
     * user action.
     *
     * @param b  a boolean, where <code>true</code> means this internal frame can be resized
     */
    @BeanProperty(preferred = true, description
            = "Determines whether this internal frame can be resized by the user.")
    public void setResizable(boolean b) {
        Boolean oldValue = resizable ? Boolean.TRUE : Boolean.FALSE;
        Boolean newValue = b ? Boolean.TRUE : Boolean.FALSE;
        resizable = b;
        firePropertyChange("resizable", oldValue, newValue);
    }

    /**
     * Returns whether the <code>JInternalFrame</code> can be resized
     * by some user action.
     *
     * @return <code>true</code> if this internal frame can be resized, <code>false</code> otherwise
     */
    public boolean isResizable() {
        // don't allow resizing when maximized.
        return isMaximum ? false : resizable;
    }

    /**
     * Sets the <code>iconable</code> property,
     * which must be <code>true</code>
     * for the user to be able to
     * make the <code>JInternalFrame</code> an icon.
     * Some look and feels might not implement iconification;
     * they will ignore this property.
     *
     * @param b  a boolean, where <code>true</code> means this internal frame can be iconified
     */
    @BeanProperty(preferred = true, description
            = "Determines whether this internal frame can be iconified.")
    public void setIconifiable(boolean b) {
        Boolean oldValue = iconable ? Boolean.TRUE : Boolean.FALSE;
        Boolean newValue = b ? Boolean.TRUE : Boolean.FALSE;
        iconable = b;
        firePropertyChange("iconable", oldValue, newValue);
    }

    /**
     * Gets the <code>iconable</code> property,
     * which by default is <code>false</code>.
     *
     * @return the value of the <code>iconable</code> property.
     *
     * @see #setIconifiable
     */
    public boolean isIconifiable() {
        return iconable;
    }

    /**
     * Returns whether the <code>JInternalFrame</code> is currently iconified.
     *
     * @return <code>true</code> if this internal frame is iconified
     */
    public boolean isIcon() {
        return isIcon;
    }

    /**
     * Iconifies or de-iconifies this internal frame,
     * if the look and feel supports iconification.
     * If the internal frame's state changes to iconified,
     * this method fires an <code>INTERNAL_FRAME_ICONIFIED</code> event.
     * If the state changes to de-iconified,
     * an <code>INTERNAL_FRAME_DEICONIFIED</code> event is fired.
     *
     * @param b a boolean, where <code>true</code> means to iconify this internal frame and
     *          <code>false</code> means to de-iconify it
     * @exception PropertyVetoException when the attempt to set the
     *            property is vetoed by the <code>JInternalFrame</code>
     *
     * @see InternalFrameEvent#INTERNAL_FRAME_ICONIFIED
     * @see InternalFrameEvent#INTERNAL_FRAME_DEICONIFIED
     */
    @BeanProperty(description
            = "The image displayed when this internal frame is minimized.")
    public void setIcon(boolean b) throws PropertyVetoException {
        if (isIcon == b) {
            return;
        }

        /* If an internal frame is being iconified before it has a
           parent, (e.g., client wants it to start iconic), create the
           parent if possible so that we can place the icon in its
           proper place on the desktop. I am not sure the call to
           validate() is necessary, since we are not going to display
           this frame yet */
        firePropertyChange("ancestor", null, getParent());

        Boolean oldValue = isIcon ? Boolean.TRUE : Boolean.FALSE;
        Boolean newValue = b ? Boolean.TRUE : Boolean.FALSE;
        fireVetoableChange(IS_ICON_PROPERTY, oldValue, newValue);
        isIcon = b;
        firePropertyChange(IS_ICON_PROPERTY, oldValue, newValue);
        if (b)
          fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_ICONIFIED);
        else
          fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_DEICONIFIED);
    }

    /**
     * Sets the <code>maximizable</code> property,
     * which determines whether the <code>JInternalFrame</code>
     * can be maximized by
     * some user action.
     * Some look and feels might not support maximizing internal frames;
     * they will ignore this property.
     *
     * @param b <code>true</code> to specify that this internal frame should be maximizable; <code>false</code> to specify that it should not be
     */
    @BeanProperty(preferred = true, description
            = "Determines whether this internal frame can be maximized.")
    public void setMaximizable(boolean b) {
        Boolean oldValue = maximizable ? Boolean.TRUE : Boolean.FALSE;
        Boolean newValue = b ? Boolean.TRUE : Boolean.FALSE;
        maximizable = b;
        firePropertyChange("maximizable", oldValue, newValue);
    }

    /**
     * Gets the value of the <code>maximizable</code> property.
     *
     * @return the value of the <code>maximizable</code> property
     * @see #setMaximizable
     */
    public boolean isMaximizable() {
        return maximizable;
    }

    /**
     * Returns whether the <code>JInternalFrame</code> is currently maximized.
     *
     * @return <code>true</code> if this internal frame is maximized, <code>false</code> otherwise
     */
    public boolean isMaximum() {
        return isMaximum;
    }

    /**
     * Maximizes and restores this internal frame.  A maximized frame is resized to
     * fully fit the <code>JDesktopPane</code> area associated with the
     * <code>JInternalFrame</code>.
     * A restored frame's size is set to the <code>JInternalFrame</code>'s
     * actual size.
     *
     * @param b  a boolean, where <code>true</code> maximizes this internal frame and <code>false</code>
     *           restores it
     * @exception PropertyVetoException when the attempt to set the
     *            property is vetoed by the <code>JInternalFrame</code>
     */
    @BeanProperty(description
            = "Indicates whether this internal frame is maximized.")
    public void setMaximum(boolean b) throws PropertyVetoException {
        if (isMaximum == b) {
            return;
        }

        Boolean oldValue = isMaximum ? Boolean.TRUE : Boolean.FALSE;
        Boolean newValue = b ? Boolean.TRUE : Boolean.FALSE;
        fireVetoableChange(IS_MAXIMUM_PROPERTY, oldValue, newValue);
        /* setting isMaximum above the event firing means that
           property listeners that, for some reason, test it will
           get it wrong... See, for example, getNormalBounds() */
        isMaximum = b;
        firePropertyChange(IS_MAXIMUM_PROPERTY, oldValue, newValue);
    }

    /**
     * Returns the title of the <code>JInternalFrame</code>.
     *
     * @return a <code>String</code> containing this internal frame's title
     * @see #setTitle
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets the <code>JInternalFrame</code> title. <code>title</code>
     * may have a <code>null</code> value.
     * @see #getTitle
     *
     * @param title  the <code>String</code> to display in the title bar
     */
    @BeanProperty(preferred = true, description
            = "The text displayed in the title bar.")
    public void setTitle(String title) {
        String oldValue = this.title;
        this.title = title;
        firePropertyChange(TITLE_PROPERTY, oldValue, title);
    }

    /**
     * Selects or deselects the internal frame
     * if it's showing.
     * A <code>JInternalFrame</code> normally draws its title bar
     * differently if it is
     * the selected frame, which indicates to the user that this
     * internal frame has the focus.
     * When this method changes the state of the internal frame
     * from deselected to selected, it fires an
     * <code>InternalFrameEvent.INTERNAL_FRAME_ACTIVATED</code> event.
     * If the change is from selected to deselected,
     * an <code>InternalFrameEvent.INTERNAL_FRAME_DEACTIVATED</code> event
     * is fired.
     *
     * @param selected  a boolean, where <code>true</code> means this internal frame
     *                  should become selected (currently active)
     *                  and <code>false</code> means it should become deselected
     * @exception PropertyVetoException when the attempt to set the
     *            property is vetoed by the <code>JInternalFrame</code>
     *
     * @see #isShowing
     * @see InternalFrameEvent#INTERNAL_FRAME_ACTIVATED
     * @see InternalFrameEvent#INTERNAL_FRAME_DEACTIVATED
     */
    @BeanProperty(description
            = "Indicates whether this internal frame is currently the active frame.")
    public void setSelected(boolean selected) throws PropertyVetoException {
       // The InternalFrame may already be selected, but the focus
       // may be outside it, so restore the focus to the subcomponent
       // which previously had it. See Bug 4302764.
        if (selected && isSelected) {
            restoreSubcomponentFocus();
            return;
        }
        // The internal frame or the desktop icon must be showing to allow
        // selection.  We may deselect even if neither is showing.
        if ((isSelected == selected) || (selected &&
            (isIcon ? !desktopIcon.isShowing() : !isShowing()))) {
            return;
        }

        Boolean oldValue = isSelected ? Boolean.TRUE : Boolean.FALSE;
        Boolean newValue = selected ? Boolean.TRUE : Boolean.FALSE;
        fireVetoableChange(IS_SELECTED_PROPERTY, oldValue, newValue);

        /* We don't want to leave focus in the previously selected
           frame, so we have to set it to *something* in case it
           doesn't get set in some other way (as if a user clicked on
           a component that doesn't request focus).  If this call is
           happening because the user clicked on a component that will
           want focus, then it will get transfered there later.

           We test for parent.isShowing() above, because AWT throws a
           NPE if you try to request focus on a lightweight before its
           parent has been made visible */

        if (selected) {
            restoreSubcomponentFocus();
        }

        isSelected = selected;
        firePropertyChange(IS_SELECTED_PROPERTY, oldValue, newValue);
        if (isSelected)
          fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_ACTIVATED);
        else {
          fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_DEACTIVATED);
          try {
              java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
                                               new sun.awt.UngrabEvent(this));
          } catch (SecurityException e) {
              this.dispatchEvent(new sun.awt.UngrabEvent(this));
          }
        }
        repaint();
    }

    /**
     * Returns whether the <code>JInternalFrame</code> is the
     * currently "selected" or active frame.
     *
     * @return <code>true</code> if this internal frame is currently selected (active)
     * @see #setSelected
     */
    public boolean isSelected() {
        return isSelected;
    }

    /**
     * Sets an image to be displayed in the titlebar of this internal frame (usually
     * in the top-left corner).
     * Some look and feels might not support displaying an icon in the titlebar.
     *
     * This image is not the <code>desktopIcon</code> object, which
     * is the image displayed in the <code>JDesktop</code> when
     * this internal frame is iconified.
     *
     * Passing <code>null</code> to this function is valid,
     * but the look and feel can choose the appropriate behavior
     * for that situation, such as displaying no icon
     * or a default icon for the look and feel.
     *
     * @param icon the <code>Icon</code> to display in the title bar
     * @see #getFrameIcon
     */
    @BeanProperty(description
            = "The icon shown in the top-left corner of this internal frame.")
  public void setFrameIcon(Icon icon) {
        Icon oldIcon = frameIcon;
        frameIcon = icon;
        firePropertyChange(FRAME_ICON_PROPERTY, oldIcon, icon);
    }

    /**
     * Returns the image displayed in the title bar of this internal frame (usually
     * in the top-left corner).
     *
     * @return the <code>Icon</code> displayed in the title bar
     * @see #setFrameIcon
     */
    public Icon getFrameIcon()  {
        return frameIcon;
    }

    /**
      * Convenience method that moves this component to position 0 if its
      * parent is a <code>JLayeredPane</code>.
      */
    public void moveToFront() {
        if (isIcon()) {
            if (getDesktopIcon().getParent() instanceof JLayeredPane) {
                ((JLayeredPane)getDesktopIcon().getParent()).
                    moveToFront(getDesktopIcon());
            }
        }
        else if (getParent() instanceof JLayeredPane) {
            ((JLayeredPane)getParent()).moveToFront(this);
        }
    }

    /**
      * Convenience method that moves this component to position -1 if its
      * parent is a <code>JLayeredPane</code>.
      */
    public void moveToBack() {
        if (isIcon()) {
            if (getDesktopIcon().getParent() instanceof JLayeredPane) {
                ((JLayeredPane)getDesktopIcon().getParent()).
                    moveToBack(getDesktopIcon());
            }
        }
        else if (getParent() instanceof JLayeredPane) {
            ((JLayeredPane)getParent()).moveToBack(this);
        }
    }

    /**
     * Returns the last <code>Cursor</code> that was set by the
     * <code>setCursor</code> method that is not a resizable
     * <code>Cursor</code>.
     *
     * @return the last non-resizable <code>Cursor</code>
     * @since 1.6
     */
    @BeanProperty(bound = false)
    public Cursor getLastCursor() {
        return lastCursor;
    }

    /**
     * {@inheritDoc}
     * @since 1.6
     */
    public void setCursor(Cursor cursor) {
        if (cursor == null) {
            lastCursor = null;
            super.setCursor(cursor);
            return;
        }
        int type = cursor.getType();
        if (!(type == Cursor.SW_RESIZE_CURSOR  ||
              type == Cursor.SE_RESIZE_CURSOR  ||
              type == Cursor.NW_RESIZE_CURSOR  ||
              type == Cursor.NE_RESIZE_CURSOR  ||
              type == Cursor.N_RESIZE_CURSOR   ||
              type == Cursor.S_RESIZE_CURSOR   ||
              type == Cursor.W_RESIZE_CURSOR   ||
              type == Cursor.E_RESIZE_CURSOR)) {
            lastCursor = cursor;
        }
        super.setCursor(cursor);
    }

    /**
     * Convenience method for setting the layer attribute of this component.
     *
     * @param layer  an <code>Integer</code> object specifying this
     *          frame's desktop layer
     * @throws NullPointerException if {@code layer} is {@code null}
     * @see JLayeredPane
     */
    @BeanProperty(bound = false, expert = true, description
            = "Specifies what desktop layer is used.")
    public void setLayer(Integer layer) {
        if(getParent() != null && getParent() instanceof JLayeredPane) {
            // Normally we want to do this, as it causes the LayeredPane
            // to draw properly.
            JLayeredPane p = (JLayeredPane)getParent();
            p.setLayer(this, layer.intValue(), p.getPosition(this));
        } else {
             // Try to do the right thing
             JLayeredPane.putLayer(this, layer.intValue());
             if(getParent() != null)
                 getParent().repaint(getX(), getY(), getWidth(), getHeight());
        }
    }

    /**
     * Convenience method for setting the layer attribute of this component.
     * The method <code>setLayer(Integer)</code> should be used for
     * layer values predefined in <code>JLayeredPane</code>.
     * When using <code>setLayer(int)</code>, care must be taken not to
     * accidentally clash with those values.
     *
     * @param layer  an integer specifying this internal frame's desktop layer
     *
     * @since 1.3
     *
     * @see #setLayer(Integer)
     * @see JLayeredPane
     */
    @BeanProperty(bound = false, expert = true, description
            = "Specifies what desktop layer is used.")
    public void setLayer(int layer) {
      this.setLayer(Integer.valueOf(layer));
    }

    /**
     * Convenience method for getting the layer attribute of this component.
     *
     * @return  an <code>Integer</code> object specifying this
     *          frame's desktop layer
     * @see JLayeredPane
      */
    public int getLayer() {
        return JLayeredPane.getLayer(this);
    }

    /**
      * Convenience method that searches the ancestor hierarchy for a
      * <code>JDesktop</code> instance. If <code>JInternalFrame</code>
      * finds none, the <code>desktopIcon</code> tree is searched.
      *
      * @return the <code>JDesktopPane</code> this internal frame belongs to,
      *         or <code>null</code> if none is found
      */
    @BeanProperty(bound = false)
    public JDesktopPane getDesktopPane() {
        Container p;

        // Search upward for desktop
        p = getParent();
        while(p != null && !(p instanceof JDesktopPane))
            p = p.getParent();

        if(p == null) {
           // search its icon parent for desktop
           p = getDesktopIcon().getParent();
           while(p != null && !(p instanceof JDesktopPane))
                p = p.getParent();
        }

        return (JDesktopPane)p;
    }

    /**
     * Sets the <code>JDesktopIcon</code> associated with this
     * <code>JInternalFrame</code>.
     *
     * @param d the <code>JDesktopIcon</code> to display on the desktop
     * @see #getDesktopIcon
     */
    @BeanProperty(description
            = "The icon shown when this internal frame is minimized.")
    public void setDesktopIcon(JDesktopIcon d) {
        JDesktopIcon oldValue = getDesktopIcon();
        desktopIcon = d;
        firePropertyChange("desktopIcon", oldValue, d);
    }

    /**
     * Returns the <code>JDesktopIcon</code> used when this
     * <code>JInternalFrame</code> is iconified.
     *
     * @return the <code>JDesktopIcon</code> displayed on the desktop
     * @see #setDesktopIcon
     */
    public JDesktopIcon getDesktopIcon() {
        return desktopIcon;
    }

    /**
     * If the <code>JInternalFrame</code> is not in maximized state, returns
     * <code>getBounds()</code>; otherwise, returns the bounds that the
     * <code>JInternalFrame</code> would be restored to.
     *
     * @return a <code>Rectangle</code> containing the bounds of this
     *          frame when in the normal state
     * @since 1.3
     */
    public Rectangle getNormalBounds() {

      /* we used to test (!isMaximum) here, but since this
         method is used by the property listener for the
         IS_MAXIMUM_PROPERTY, it ended up getting the wrong
         answer... Since normalBounds get set to null when the
         frame is restored, this should work better */

      if (normalBounds != null) {
        return normalBounds;
      } else {
        return getBounds();
      }
    }

    /**
     * Sets the normal bounds for this internal frame, the bounds that
     * this internal frame would be restored to from its maximized state.
     * This method is intended for use only by desktop managers.
     *
     * @param r the bounds that this internal frame should be restored to
     * @since 1.3
     */
    public void setNormalBounds(Rectangle r) {
        normalBounds = r;
    }

    /**
     * If this <code>JInternalFrame</code> is active,
     * returns the child that has focus.
     * Otherwise, returns <code>null</code>.
     *
     * @return the component with focus, or <code>null</code> if no children have focus
     * @since 1.3
     */
    public Component getFocusOwner() {
        if (isSelected()) {
            return lastFocusOwner;
        }
        return null;
    }

    /**
     * Returns the child component of this <code>JInternalFrame</code>
     * that will receive the
     * focus when this <code>JInternalFrame</code> is selected.
     * If this <code>JInternalFrame</code> is
     * currently selected, this method returns the same component as
     * the <code>getFocusOwner</code> method.
     * If this <code>JInternalFrame</code> is not selected,
     * then the child component that most recently requested focus will be
     * returned. If no child component has ever requested focus, then this
     * <code>JInternalFrame</code>'s initial focusable component is returned.
     * If no such
     * child exists, then this <code>JInternalFrame</code>'s default component
     * to focus is returned.
     *
     * @return the child component that will receive focus when this
     *         <code>JInternalFrame</code> is selected
     * @see #getFocusOwner
     * @see #isSelected
     * @since 1.4
     */
    @BeanProperty(bound = false)
    public Component getMostRecentFocusOwner() {
        if (isSelected()) {
            return getFocusOwner();
        }

        if (lastFocusOwner != null) {
            return lastFocusOwner;
        }

        FocusTraversalPolicy policy = getFocusTraversalPolicy();
        if (policy instanceof InternalFrameFocusTraversalPolicy) {
            return ((InternalFrameFocusTraversalPolicy)policy).
                getInitialComponent(this);
        }

        Component toFocus = policy.getDefaultComponent(this);
        if (toFocus != null) {
            return toFocus;
        }
        return getContentPane();
    }

    /**
     * Requests the internal frame to restore focus to the
     * last subcomponent that had focus. This is used by the UI when
     * the user selected this internal frame --
     * for example, by clicking on the title bar.
     *
     * @since 1.3
     */
    public void restoreSubcomponentFocus() {
        if (isIcon()) {
            SwingUtilities2.compositeRequestFocus(getDesktopIcon());
        }
        else {
            Component component = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
            if ((component == null) || !SwingUtilities.isDescendingFrom(component, this)) {
                // FocusPropertyChangeListener will eventually update
                // lastFocusOwner. As focus requests are asynchronous
                // lastFocusOwner may be accessed before it has been correctly
                // updated. To avoid any problems, lastFocusOwner is immediately
                // set, assuming the request will succeed.
                setLastFocusOwner(getMostRecentFocusOwner());
                if (lastFocusOwner == null) {
                    // Make sure focus is restored somewhere, so that
                    // we don't leave a focused component in another frame while
                    // this frame is selected.
                    setLastFocusOwner(getContentPane());
                }
                lastFocusOwner.requestFocus();
            }
        }
    }

    private void setLastFocusOwner(Component component) {
        lastFocusOwner = component;
    }

    /**
     * Moves and resizes this component.  Unlike other components,
     * this implementation also forces re-layout, so that frame
     * decorations such as the title bar are always redisplayed.
     *
     * @param x  an integer giving the component's new horizontal position
     *           measured in pixels from the left of its container
     * @param y  an integer giving the component's new vertical position,
     *           measured in pixels from the bottom of its container
     * @param width  an integer giving the component's new width in pixels
     * @param height an integer giving the component's new height in pixels
     */
    @SuppressWarnings("deprecation")
    public void reshape(int x, int y, int width, int height) {
        super.reshape(x, y, width, height);
        validate();
        repaint();
    }

///////////////////////////
// Frame/Window equivalents
///////////////////////////

    /**
     * Adds the specified listener to receive internal
     * frame events from this internal frame.
     *
     * @param l the internal frame listener
     */
    public void addInternalFrameListener(InternalFrameListener l) {  // remind: sync ??
      listenerList.add(InternalFrameListener.class, l);
      // remind: needed?
      enableEvents(0);   // turn on the newEventsOnly flag in Component.
    }

    /**
     * Removes the specified internal frame listener so that it no longer
     * receives internal frame events from this internal frame.
     *
     * @param l the internal frame listener
     */
    public void removeInternalFrameListener(InternalFrameListener l) {  // remind: sync??
      listenerList.remove(InternalFrameListener.class, l);
    }

    /**
     * Returns an array of all the <code>InternalFrameListener</code>s added
     * to this <code>JInternalFrame</code> with
     * <code>addInternalFrameListener</code>.
     *
     * @return all of the <code>InternalFrameListener</code>s added or an empty
     *         array if no listeners have been added
     * @since 1.4
     *
     * @see #addInternalFrameListener
     */
    @BeanProperty(bound = false)
    public InternalFrameListener[] getInternalFrameListeners() {
        return listenerList.getListeners(InternalFrameListener.class);
    }

    // remind: name ok? all one method ok? need to be synchronized?
    /**
     * Fires an internal frame event.
     *
     * @param id  the type of the event being fired; one of the following:
     * <ul>
     * <li><code>InternalFrameEvent.INTERNAL_FRAME_OPENED</code>
     * <li><code>InternalFrameEvent.INTERNAL_FRAME_CLOSING</code>
     * <li><code>InternalFrameEvent.INTERNAL_FRAME_CLOSED</code>
     * <li><code>InternalFrameEvent.INTERNAL_FRAME_ICONIFIED</code>
     * <li><code>InternalFrameEvent.INTERNAL_FRAME_DEICONIFIED</code>
     * <li><code>InternalFrameEvent.INTERNAL_FRAME_ACTIVATED</code>
     * <li><code>InternalFrameEvent.INTERNAL_FRAME_DEACTIVATED</code>
     * </ul>
     * If the event type is not one of the above, nothing happens.
     */
    protected void fireInternalFrameEvent(int id){
      Object[] listeners = listenerList.getListenerList();
      InternalFrameEvent e = null;
      for (int i = listeners.length -2; i >=0; i -= 2){
        if (listeners[i] == InternalFrameListener.class){
          if (e == null){
            e = new InternalFrameEvent(this, id);
            //      System.out.println("InternalFrameEvent: " + e.paramString());
          }
          switch(e.getID()) {
          case InternalFrameEvent.INTERNAL_FRAME_OPENED:
            ((InternalFrameListener)listeners[i+1]).internalFrameOpened(e);
            break;
          case InternalFrameEvent.INTERNAL_FRAME_CLOSING:
            ((InternalFrameListener)listeners[i+1]).internalFrameClosing(e);
            break;
          case InternalFrameEvent.INTERNAL_FRAME_CLOSED:
            ((InternalFrameListener)listeners[i+1]).internalFrameClosed(e);
            break;
          case InternalFrameEvent.INTERNAL_FRAME_ICONIFIED:
            ((InternalFrameListener)listeners[i+1]).internalFrameIconified(e);
            break;
          case InternalFrameEvent.INTERNAL_FRAME_DEICONIFIED:
            ((InternalFrameListener)listeners[i+1]).internalFrameDeiconified(e);
            break;
          case InternalFrameEvent.INTERNAL_FRAME_ACTIVATED:
            ((InternalFrameListener)listeners[i+1]).internalFrameActivated(e);
            break;
          case InternalFrameEvent.INTERNAL_FRAME_DEACTIVATED:
            ((InternalFrameListener)listeners[i+1]).internalFrameDeactivated(e);
            break;
          default:
            break;
          }
        }
      }
      /* we could do it off the event, but at the moment, that's not how
         I'm implementing it */
      //      if (id == InternalFrameEvent.INTERNAL_FRAME_CLOSING) {
      //          doDefaultCloseAction();
      //      }
    }

    /**
     * Fires an
     * <code>INTERNAL_FRAME_CLOSING</code> event
     * and then performs the action specified by
     * the internal frame's default close operation.
     * This method is typically invoked by the
     * look-and-feel-implemented action handler
     * for the internal frame's close button.
     *
     * @since 1.3
     * @see #setDefaultCloseOperation
     * @see javax.swing.event.InternalFrameEvent#INTERNAL_FRAME_CLOSING
     */
    public void doDefaultCloseAction() {
        fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_CLOSING);
        switch(defaultCloseOperation) {
          case DO_NOTHING_ON_CLOSE:
            break;
          case HIDE_ON_CLOSE:
            setVisible(false);
            if (isSelected())
                try {
                    setSelected(false);
                } catch (PropertyVetoException pve) {}

            /* should this activate the next frame? that's really
               desktopmanager's policy... */
            break;
          case DISPOSE_ON_CLOSE:
              try {
                fireVetoableChange(IS_CLOSED_PROPERTY, Boolean.FALSE,
                                   Boolean.TRUE);
                isClosed = true;
                setVisible(false);
                firePropertyChange(IS_CLOSED_PROPERTY, Boolean.FALSE,
                                   Boolean.TRUE);
                dispose();
              } catch (PropertyVetoException pve) {}
              break;
          default:
              break;
        }
    }

    /**
     * Sets the operation that will happen by default when
     * the user initiates a "close" on this internal frame.
     * The possible choices are:
     * <br><br>
     * <dl>
     * <dt><code>DO_NOTHING_ON_CLOSE</code>
     * <dd> Do nothing.
     *      This requires the program to handle the operation
     *      in the <code>internalFrameClosing</code> method
     *      of a registered <code>InternalFrameListener</code> object.
     * <dt><code>HIDE_ON_CLOSE</code>
     * <dd> Automatically make the internal frame invisible.
     * <dt><code>DISPOSE_ON_CLOSE</code>
     * <dd> Automatically dispose of the internal frame.
     * </dl>
     * <p>
     * The default value is <code>DISPOSE_ON_CLOSE</code>.
     * Before performing the specified close operation,
     * the internal frame fires
     * an <code>INTERNAL_FRAME_CLOSING</code> event.
     *
     * @param operation one of the following constants defined in
     *                  <code>javax.swing.WindowConstants</code>
     *                  (an interface implemented by
     *                  <code>JInternalFrame</code>):
     *                  <code>DO_NOTHING_ON_CLOSE</code>,
     *                  <code>HIDE_ON_CLOSE</code>, or
     *                  <code>DISPOSE_ON_CLOSE</code>
     *
     * @see #addInternalFrameListener
     * @see #getDefaultCloseOperation
     * @see #setVisible
     * @see #dispose
     * @see InternalFrameEvent#INTERNAL_FRAME_CLOSING
     */
    public void setDefaultCloseOperation(int operation) {
        this.defaultCloseOperation = operation;
    }

   /**
    * Returns the default operation that occurs when the user
    * initiates a "close" on this internal frame.
    * @return the operation that will occur when the user closes the internal
    *         frame
    * @see #setDefaultCloseOperation
    */
    public int getDefaultCloseOperation() {
        return defaultCloseOperation;
    }

    /**
     * Causes subcomponents of this <code>JInternalFrame</code>
     * to be laid out at their preferred size.  Internal frames that are
     * iconized or maximized are first restored and then packed.  If the
     * internal frame is unable to be restored its state is not changed
     * and will not be packed.
     *
     * @see       java.awt.Window#pack
     */
    public void pack() {
        try {
            if (isIcon()) {
                setIcon(false);
            } else if (isMaximum()) {
                setMaximum(false);
            }
        } catch(PropertyVetoException e) {
            return;
        }
        setSize(getPreferredSize());
        validate();
    }

    /**
     * If the internal frame is not visible,
     * brings the internal frame to the front,
     * makes it visible,
     * and attempts to select it.
     * The first time the internal frame is made visible,
     * this method also fires an <code>INTERNAL_FRAME_OPENED</code> event.
     * This method does nothing if the internal frame is already visible.
     * Invoking this method
     * has the same result as invoking
     * <code>setVisible(true)</code>.
     *
     * @see #moveToFront
     * @see #setSelected
     * @see InternalFrameEvent#INTERNAL_FRAME_OPENED
     * @see #setVisible
     */
    @SuppressWarnings("deprecation")
    public void show() {
        // bug 4312922
        if (isVisible()) {
            //match the behavior of setVisible(true): do nothing
            return;
        }

        // bug 4149505
        if (!opened) {
          fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_OPENED);
          opened = true;
        }

        /* icon default visibility is false; set it to true so that it shows
           up when user iconifies frame */
        getDesktopIcon().setVisible(true);

        toFront();
        super.show();

        if (isIcon) {
            return;
        }

        if (!isSelected()) {
            try {
                setSelected(true);
            } catch (PropertyVetoException pve) {}
        }
    }

    @SuppressWarnings("deprecation")
    public void hide() {
        if (isIcon()) {
            getDesktopIcon().setVisible(false);
        }
        super.hide();
    }

    /**
     * Makes this internal frame
     * invisible, unselected, and closed.
     * If the frame is not already closed,
     * this method fires an
     * <code>INTERNAL_FRAME_CLOSED</code> event.
     * The results of invoking this method are similar to
     * <code>setClosed(true)</code>,
     * but <code>dispose</code> always succeeds in closing
     * the internal frame and does not fire
     * an <code>INTERNAL_FRAME_CLOSING</code> event.
     *
     * @see javax.swing.event.InternalFrameEvent#INTERNAL_FRAME_CLOSED
     * @see #setVisible
     * @see #setSelected
     * @see #setClosed
     */
    public void dispose() {
        if (isVisible()) {
            setVisible(false);
        }
        if (!isClosed) {
          firePropertyChange(IS_CLOSED_PROPERTY, Boolean.FALSE, Boolean.TRUE);
          isClosed = true;
        }
        fireInternalFrameEvent(InternalFrameEvent.INTERNAL_FRAME_CLOSED);
        try {
            java.awt.Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
                    new sun.awt.UngrabEvent(this));
        } catch (SecurityException e) {
            this.dispatchEvent(new sun.awt.UngrabEvent(this));
        }
    }

    /**
     * Brings this internal frame to the front.
     * Places this internal frame  at the top of the stacking order
     * and makes the corresponding adjustment to other visible internal
     * frames.
     *
     * @see       java.awt.Window#toFront
     * @see       #moveToFront
     */
    public void toFront() {
        moveToFront();
    }

    /**
     * Sends this internal frame to the back.
     * Places this internal frame at the bottom of the stacking order
     * and makes the corresponding adjustment to other visible
     * internal frames.
     *
     * @see       java.awt.Window#toBack
     * @see       #moveToBack
     */
    public void toBack() {
        moveToBack();
    }

    /**
     * Does nothing because <code>JInternalFrame</code>s must always be roots of a focus
     * traversal cycle.
     *
     * @param focusCycleRoot this value is ignored
     * @see #isFocusCycleRoot
     * @see java.awt.Container#setFocusTraversalPolicy
     * @see java.awt.Container#getFocusTraversalPolicy
     * @since 1.4
     */
    public final void setFocusCycleRoot(boolean focusCycleRoot) {
    }

    /**
     * Always returns <code>true</code> because all <code>JInternalFrame</code>s must be
     * roots of a focus traversal cycle.
     *
     * @return <code>true</code>
     * @see #setFocusCycleRoot
     * @see java.awt.Container#setFocusTraversalPolicy
     * @see java.awt.Container#getFocusTraversalPolicy
     * @since 1.4
     */
    public final boolean isFocusCycleRoot() {
        return true;
    }

    /**
     * Always returns <code>null</code> because <code>JInternalFrame</code>s
     * must always be roots of a focus
     * traversal cycle.
     *
     * @return <code>null</code>
     * @see java.awt.Container#isFocusCycleRoot()
     * @since 1.4
     */
    @BeanProperty(bound = false)
    public final Container getFocusCycleRootAncestor() {
        return null;
    }

    /**
     * Gets the warning string that is displayed with this internal frame.
     * Since an internal frame is always secure (since it's fully
     * contained within a window that might need a warning string)
     * this method always returns <code>null</code>.
     * @return    <code>null</code>
     * @see       java.awt.Window#getWarningString
     */
    @BeanProperty(bound = false)
    public final String getWarningString() {
        return null;
    }

    /**
     * See <code>readObject</code> and <code>writeObject</code>
     * in <code>JComponent</code> for more
     * information about serialization in Swing.
     */
    @Serial
    private void writeObject(ObjectOutputStream s) throws IOException {
        s.defaultWriteObject();
        if (getUIClassID().equals(uiClassID)) {
            byte count = JComponent.getWriteObjCounter(this);
            JComponent.setWriteObjCounter(this, --count);
            if (count == 0 && ui != null) {
                boolean old = isRootPaneCheckingEnabled();
                try {
                    setRootPaneCheckingEnabled(false);
                    ui.installUI(this);
                } finally {
                    setRootPaneCheckingEnabled(old);
                }
            }
        }
    }

    /* Called from the JComponent's EnableSerializationFocusListener to
     * do any Swing-specific pre-serialization configuration.
     */
    void compWriteObjectNotify() {
      // need to disable rootpane checking for InternalFrame: 4172083
      boolean old = isRootPaneCheckingEnabled();
      try {
        setRootPaneCheckingEnabled(false);
        super.compWriteObjectNotify();
      }
      finally {
        setRootPaneCheckingEnabled(old);
      }
    }

    /**
     * Returns a string representation of this <code>JInternalFrame</code>.
     * This method
     * is intended to be used only for debugging purposes, and the
     * content and format of the returned string may vary between
     * implementations. The returned string may be empty but may not
     * be <code>null</code>.
     *
     * @return  a string representation of this <code>JInternalFrame</code>
     */
    protected String paramString() {
        String rootPaneString = (rootPane != null ?
                                 rootPane.toString() : "");
        String rootPaneCheckingEnabledString = (rootPaneCheckingEnabled ?
                                                "true" : "false");
        String closableString = (closable ? "true" : "false");
        String isClosedString = (isClosed ? "true" : "false");
        String maximizableString = (maximizable ? "true" : "false");
        String isMaximumString = (isMaximum ? "true" : "false");
        String iconableString = (iconable ? "true" : "false");
        String isIconString = (isIcon ? "true" : "false");
        String resizableString = (resizable ? "true" : "false");
        String isSelectedString = (isSelected ? "true" : "false");
        String frameIconString = (frameIcon != null ?
                                  frameIcon.toString() : "");
        String titleString = (title != null ?
                              title : "");
        String desktopIconString = (desktopIcon != null ?
                                    desktopIcon.toString() : "");
        String openedString = (opened ? "true" : "false");
        String defaultCloseOperationString;
        if (defaultCloseOperation == HIDE_ON_CLOSE) {
            defaultCloseOperationString = "HIDE_ON_CLOSE";
        } else if (defaultCloseOperation == DISPOSE_ON_CLOSE) {
            defaultCloseOperationString = "DISPOSE_ON_CLOSE";
        } else if (defaultCloseOperation == DO_NOTHING_ON_CLOSE) {
            defaultCloseOperationString = "DO_NOTHING_ON_CLOSE";
        } else defaultCloseOperationString = "";

        return super.paramString() +
        ",closable=" + closableString +
        ",defaultCloseOperation=" + defaultCloseOperationString +
        ",desktopIcon=" + desktopIconString +
        ",frameIcon=" + frameIconString +
        ",iconable=" + iconableString +
        ",isClosed=" + isClosedString +
        ",isIcon=" + isIconString +
        ",isMaximum=" + isMaximumString +
        ",isSelected=" + isSelectedString +
        ",maximizable=" + maximizableString +
        ",opened=" + openedString +
        ",resizable=" + resizableString +
        ",rootPane=" + rootPaneString +
        ",rootPaneCheckingEnabled=" + rootPaneCheckingEnabledString +
        ",title=" + titleString;
    }

    // ======= begin optimized frame dragging defence code ==============

    boolean isDragging = false;
    boolean danger = false;

    /**
     * Overridden to allow optimized painting when the
     * internal frame is being dragged.
     */
    protected void paintComponent(Graphics g) {
      if (isDragging) {
        //         System.out.println("ouch");
         danger = true;
      }

      super.paintComponent(g);
   }

    // ======= end optimized frame dragging defence code ==============

/////////////////
// Accessibility support
////////////////

    /**
     * Gets the <code>AccessibleContext</code> associated with this
     * <code>JInternalFrame</code>.
     * For internal frames, the <code>AccessibleContext</code>
     * takes the form of an
     * <code>AccessibleJInternalFrame</code> object.
     * A new <code>AccessibleJInternalFrame</code> instance is created if necessary.
     *
     * @return an <code>AccessibleJInternalFrame</code> that serves as the
     *         <code>AccessibleContext</code> of this
     *         <code>JInternalFrame</code>
     * @see AccessibleJInternalFrame
     */
    @BeanProperty(bound = false)
    public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
            accessibleContext = new AccessibleJInternalFrame();
        }
        return accessibleContext;
    }

    /**
     * This class implements accessibility support for the
     * <code>JInternalFrame</code> class.  It provides an implementation of the
     * Java Accessibility API appropriate to internal frame user-interface
     * elements.
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
    protected class AccessibleJInternalFrame extends AccessibleJComponent
        implements AccessibleValue {

        /**
         * Constructs an {@code AccessibleJInternalFrame}.
         */
        protected AccessibleJInternalFrame() {}

        /**
         * Get the accessible name of this object.
         *
         * @return the localized name of the object -- can be <code>null</code> if this
         * object does not have a name
         * @see #setAccessibleName
         */
        public String getAccessibleName() {
            String name = accessibleName;

            if (name == null) {
                name = (String)getClientProperty(AccessibleContext.ACCESSIBLE_NAME_PROPERTY);
            }
            if (name == null) {
                name = getTitle();
            }
            return name;
        }

        /**
         * Get the role of this object.
         *
         * @return an instance of AccessibleRole describing the role of the
         * object
         * @see AccessibleRole
         */
        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.INTERNAL_FRAME;
        }

        /**
         * Gets the AccessibleValue associated with this object.  In the
         * implementation of the Java Accessibility API for this class,
         * returns this object, which is responsible for implementing the
         * <code>AccessibleValue</code> interface on behalf of itself.
         *
         * @return this object
         */
        public AccessibleValue getAccessibleValue() {
            return this;
        }


        //
        // AccessibleValue methods
        //

        /**
         * Get the value of this object as a Number.
         *
         * @return value of the object -- can be <code>null</code> if this object does not
         * have a value
         */
        public Number getCurrentAccessibleValue() {
            return Integer.valueOf(getLayer());
        }

        /**
         * Set the value of this object as a Number.
         *
         * @return <code>true</code> if the value was set
         */
        public boolean setCurrentAccessibleValue(Number n) {
            // TIGER - 4422535
            if (n == null) {
                return false;
            }
            setLayer(Integer.valueOf(n.intValue()));
            return true;
        }

        /**
         * Get the minimum value of this object as a Number.
         *
         * @return Minimum value of the object; <code>null</code> if this object does not
         * have a minimum value
         */
        public Number getMinimumAccessibleValue() {
            return Integer.MIN_VALUE;
        }

        /**
         * Get the maximum value of this object as a Number.
         *
         * @return Maximum value of the object; <code>null</code> if this object does not
         * have a maximum value
         */
        public Number getMaximumAccessibleValue() {
            return Integer.MAX_VALUE;
        }

    } // AccessibleJInternalFrame

    /**
     * This component represents an iconified version of a
     * <code>JInternalFrame</code>.
     * This API should NOT BE USED by Swing applications, as it will go
     * away in future versions of Swing as its functionality is moved into
     * <code>JInternalFrame</code>.  This class is public only so that
     * UI objects can display a desktop icon.  If an application
     * wants to display a desktop icon, it should create a
     * <code>JInternalFrame</code> instance and iconify it.
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
     * @author David Kloba
     */
    @SuppressWarnings("serial") // Same-version serialization only
    public static class JDesktopIcon extends JComponent implements Accessible
    {
        JInternalFrame internalFrame;

        /**
         * Creates an icon for an internal frame.
         *
         * @param f  the <code>JInternalFrame</code>
         *              for which the icon is created
         */
        public JDesktopIcon(JInternalFrame f) {
            setVisible(false);
            setInternalFrame(f);
            updateUI();
        }

        /**
         * Returns the look-and-feel object that renders this component.
         *
         * @return the <code>DesktopIconUI</code> object that renders
         *              this component
         */
        public DesktopIconUI getUI() {
            return (DesktopIconUI)ui;
        }

        /**
         * Sets the look-and-feel object that renders this component.
         *
         * @param ui  the <code>DesktopIconUI</code> look-and-feel object
         * @see UIDefaults#getUI
         */
        public void setUI(DesktopIconUI ui) {
            super.setUI(ui);
        }

        /**
         * Returns the <code>JInternalFrame</code> that this
         * <code>DesktopIcon</code> is associated with.
         *
         * @return the <code>JInternalFrame</code> with which this icon
         *              is associated
         */
        public JInternalFrame getInternalFrame() {
            return internalFrame;
        }

        /**
         * Sets the <code>JInternalFrame</code> with which this
         * <code>DesktopIcon</code> is associated.
         *
         * @param f  the <code>JInternalFrame</code> with which this icon
         *              is associated
         */
        public void setInternalFrame(JInternalFrame f) {
            internalFrame = f;
        }

        /**
         * Convenience method to ask the icon for the <code>Desktop</code>
         * object it belongs to.
         *
         * @return the <code>JDesktopPane</code> that contains this
         *           icon's internal frame, or <code>null</code> if none found
         */
        public JDesktopPane getDesktopPane() {
            if(getInternalFrame() != null)
                return getInternalFrame().getDesktopPane();
            return null;
        }

        /**
         * Notification from the <code>UIManager</code> that the look and feel
         * has changed.
         * Replaces the current UI object with the latest version from the
         * <code>UIManager</code>.
         *
         * @see JComponent#updateUI
         */
        public void updateUI() {
            boolean hadUI = (ui != null);
            setUI((DesktopIconUI)UIManager.getUI(this));
            invalidate();

            Dimension r = getPreferredSize();
            setSize(r.width, r.height);


            if (internalFrame != null && internalFrame.getUI() != null) {  // don't do this if UI not created yet
                SwingUtilities.updateComponentTreeUI(internalFrame);
            }
        }

        /* This method is called if updateUI was called on the associated
         * JInternalFrame.  It's necessary to avoid infinite recursion.
         */
        void updateUIWhenHidden() {
            /* Update this UI and any associated internal frame */
            setUI((DesktopIconUI)UIManager.getUI(this));

            Dimension r = getPreferredSize();
            setSize(r.width, r.height);

            invalidate();
            Component[] children = getComponents();
            if (children != null) {
                for (Component child : children) {
                    SwingUtilities.updateComponentTreeUI(child);
                }
            }
        }

        /**
         * Returns the name of the look-and-feel
         * class that renders this component.
         *
         * @return the string "DesktopIconUI"
         * @see JComponent#getUIClassID
         * @see UIDefaults#getUI
         */
        public String getUIClassID() {
            return "DesktopIconUI";
        }
        ////////////////
        // Serialization support
        ////////////////
        @Serial
        private void writeObject(ObjectOutputStream s) throws IOException {
            s.defaultWriteObject();
            if (getUIClassID().equals("DesktopIconUI")) {
                byte count = JComponent.getWriteObjCounter(this);
                JComponent.setWriteObjCounter(this, --count);
                if (count == 0 && ui != null) {
                    ui.installUI(this);
                }
            }
        }

       /////////////////
       // Accessibility support
       ////////////////

        /**
         * Gets the AccessibleContext associated with this JDesktopIcon.
         * For desktop icons, the AccessibleContext takes the form of an
         * AccessibleJDesktopIcon.
         * A new AccessibleJDesktopIcon instance is created if necessary.
         *
         * @return an AccessibleJDesktopIcon that serves as the
         *         AccessibleContext of this JDesktopIcon
         */
        public AccessibleContext getAccessibleContext() {
            if (accessibleContext == null) {
                accessibleContext = new AccessibleJDesktopIcon();
            }
            return accessibleContext;
        }

        /**
         * This class implements accessibility support for the
         * <code>JInternalFrame.JDesktopIcon</code> class.  It provides an
         * implementation of the Java Accessibility API appropriate to
         * desktop icon user-interface elements.
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
        protected class AccessibleJDesktopIcon extends AccessibleJComponent
            implements AccessibleValue {

            /**
             * Constructs an {@code AccessibleJDesktopIcon}.
             */
            protected AccessibleJDesktopIcon() {}

            /**
             * Gets the role of this object.
             *
             * @return an instance of AccessibleRole describing the role of the
             * object
             * @see AccessibleRole
             */
            public AccessibleRole getAccessibleRole() {
                return AccessibleRole.DESKTOP_ICON;
            }

            /**
             * Gets the AccessibleValue associated with this object.  In the
             * implementation of the Java Accessibility API for this class,
             * returns this object, which is responsible for implementing the
             * <code>AccessibleValue</code> interface on behalf of itself.
             *
             * @return this object
             */
            public AccessibleValue getAccessibleValue() {
                return this;
            }

            //
            // AccessibleValue methods
            //

            /**
             * Gets the value of this object as a <code>Number</code>.
             *
             * @return value of the object -- can be <code>null</code> if this object does not
             * have a value
             */
            public Number getCurrentAccessibleValue() {
                AccessibleContext a = JDesktopIcon.this.getInternalFrame().getAccessibleContext();
                AccessibleValue v = a.getAccessibleValue();
                if (v != null) {
                    return v.getCurrentAccessibleValue();
                } else {
                    return null;
                }
            }

            /**
             * Sets the value of this object as a <code>Number</code>.
             *
             * @return <code>true</code> if the value was set
             */
            public boolean setCurrentAccessibleValue(Number n) {
                // TIGER - 4422535
                if (n == null) {
                    return false;
                }
                AccessibleContext a = JDesktopIcon.this.getInternalFrame().getAccessibleContext();
                AccessibleValue v = a.getAccessibleValue();
                if (v != null) {
                    return v.setCurrentAccessibleValue(n);
                } else {
                    return false;
                }
            }

            /**
             * Gets the minimum value of this object as a <code>Number</code>.
             *
             * @return minimum value of the object; <code>null</code> if this object does not
             * have a minimum value
             */
            public Number getMinimumAccessibleValue() {
                AccessibleContext a = JDesktopIcon.this.getInternalFrame().getAccessibleContext();
                if (a instanceof AccessibleValue) {
                    return ((AccessibleValue)a).getMinimumAccessibleValue();
                } else {
                    return null;
                }
            }

            /**
             * Gets the maximum value of this object as a <code>Number</code>.
             *
             * @return maximum value of the object; <code>null</code> if this object does not
             * have a maximum value
             */
            public Number getMaximumAccessibleValue() {
                AccessibleContext a = JDesktopIcon.this.getInternalFrame().getAccessibleContext();
                if (a instanceof AccessibleValue) {
                    return ((AccessibleValue)a).getMaximumAccessibleValue();
                } else {
                    return null;
                }
            }

        } // AccessibleJDesktopIcon
    }
}
