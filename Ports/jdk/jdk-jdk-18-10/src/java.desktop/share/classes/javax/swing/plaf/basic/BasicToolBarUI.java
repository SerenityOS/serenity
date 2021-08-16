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

package javax.swing.plaf.basic;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;

import java.beans.*;

import java.util.Hashtable;
import java.util.HashMap;

import javax.swing.border.*;
import javax.swing.plaf.*;
import sun.swing.DefaultLookup;
import sun.swing.UIAction;


/**
 * A Basic L&amp;F implementation of ToolBarUI.  This implementation
 * is a "combined" view/controller.
 *
 * @author Georges Saab
 * @author Jeff Shapiro
 */
public class BasicToolBarUI extends ToolBarUI implements SwingConstants
{
    /**
     * The instance of {@code JToolBar}.
     */
    protected JToolBar toolBar;
    private boolean floating;
    private int floatingX;
    private int floatingY;
    private JFrame floatingFrame;
    private RootPaneContainer floatingToolBar;
    /**
     * The instance of {@code DragWindow}.
     */
    protected DragWindow dragWindow;
    private Container dockingSource;
    private int dockingSensitivity = 0;
    /**
     * The index of the focused component.
     */
    protected int focusedCompIndex = -1;

    /**
     * The background color of the docking border.
     */
    protected Color dockingColor = null;
    /**
     * The background color of the not docking border.
     */
    protected Color floatingColor = null;
    /**
     * The color of the docking border.
     */
    protected Color dockingBorderColor = null;
    /**
     * The color of the not docking border.
     */
    protected Color floatingBorderColor = null;

    /**
     * The instance of a {@code MouseInputListener}.
     */
    protected MouseInputListener dockingListener;
    /**
     * The instance of a {@code PropertyChangeListener}.
     */
    protected PropertyChangeListener propertyListener;

    /**
     * The instance of a {@code ContainerListener}.
     */
    protected ContainerListener toolBarContListener;
    /**
     * The instance of a {@code FocusListener}.
     */
    protected FocusListener toolBarFocusListener;
    private Handler handler;

    /**
     * The layout before floating.
     */
    protected String constraintBeforeFloating = BorderLayout.NORTH;

    // Rollover button implementation.
    private static String IS_ROLLOVER = "JToolBar.isRollover";
    private static Border rolloverBorder;
    private static Border nonRolloverBorder;
    private static Border nonRolloverToggleBorder;
    private boolean rolloverBorders = false;

    private HashMap<AbstractButton, Border> borderTable = new HashMap<AbstractButton, Border>();
    private Hashtable<AbstractButton, Boolean> rolloverTable = new Hashtable<AbstractButton, Boolean>();


    /**
     * As of Java 2 platform v1.3 this previously undocumented field is no
     * longer used.
     * Key bindings are now defined by the LookAndFeel, please refer to
     * the key bindings specification for further details.
     *
     * @deprecated As of Java 2 platform v1.3.
     */
    @Deprecated
    protected KeyStroke upKey;
    /**
     * As of Java 2 platform v1.3 this previously undocumented field is no
     * longer used.
     * Key bindings are now defined by the LookAndFeel, please refer to
     * the key bindings specification for further details.
     *
     * @deprecated As of Java 2 platform v1.3.
     */
    @Deprecated
    protected KeyStroke downKey;
    /**
     * As of Java 2 platform v1.3 this previously undocumented field is no
     * longer used.
     * Key bindings are now defined by the LookAndFeel, please refer to
     * the key bindings specification for further details.
     *
     * @deprecated As of Java 2 platform v1.3.
     */
    @Deprecated
    protected KeyStroke leftKey;
    /**
     * As of Java 2 platform v1.3 this previously undocumented field is no
     * longer used.
     * Key bindings are now defined by the LookAndFeel, please refer to
     * the key bindings specification for further details.
     *
     * @deprecated As of Java 2 platform v1.3.
     */
    @Deprecated
    protected KeyStroke rightKey;


    private static String FOCUSED_COMP_INDEX = "JToolBar.focusedCompIndex";

    /**
     * Constructs a {@code BasicToolBarUI}.
     */
    public BasicToolBarUI() {}

    /**
     * Constructs a new instance of {@code BasicToolBarUI}.
     *
     * @param c a component
     * @return a new instance of {@code BasicToolBarUI}
     */
    public static ComponentUI createUI( JComponent c )
    {
        return new BasicToolBarUI();
    }

    public void installUI( JComponent c )
    {
        toolBar = (JToolBar) c;

        // Set defaults
        installDefaults();
        installComponents();
        installListeners();
        installKeyboardActions();

        // Initialize instance vars
        dockingSensitivity = 0;
        floating = false;
        floatingX = floatingY = 0;
        floatingToolBar = null;

        setOrientation( toolBar.getOrientation() );
        LookAndFeel.installProperty(c, "opaque", Boolean.TRUE);

        if ( c.getClientProperty( FOCUSED_COMP_INDEX ) != null )
        {
            focusedCompIndex = ( (Integer) ( c.getClientProperty( FOCUSED_COMP_INDEX ) ) ).intValue();
        }
    }

    public void uninstallUI( JComponent c )
    {

        // Clear defaults
        uninstallDefaults();
        uninstallComponents();
        uninstallListeners();
        uninstallKeyboardActions();

        // Clear instance vars
        if (isFloating())
            setFloating(false, null);

        floatingToolBar = null;
        dragWindow = null;
        dockingSource = null;

        c.putClientProperty( FOCUSED_COMP_INDEX, Integer.valueOf( focusedCompIndex ) );
    }

    /**
     * Installs default properties.
     */
    protected void installDefaults( )
    {
        LookAndFeel.installBorder(toolBar,"ToolBar.border");
        LookAndFeel.installColorsAndFont(toolBar,
                                              "ToolBar.background",
                                              "ToolBar.foreground",
                                              "ToolBar.font");
        // Toolbar specific defaults
        if ( dockingColor == null || dockingColor instanceof UIResource )
            dockingColor = UIManager.getColor("ToolBar.dockingBackground");
        if ( floatingColor == null || floatingColor instanceof UIResource )
            floatingColor = UIManager.getColor("ToolBar.floatingBackground");
        if ( dockingBorderColor == null ||
             dockingBorderColor instanceof UIResource )
            dockingBorderColor = UIManager.getColor("ToolBar.dockingForeground");
        if ( floatingBorderColor == null ||
             floatingBorderColor instanceof UIResource )
            floatingBorderColor = UIManager.getColor("ToolBar.floatingForeground");

        // ToolBar rollover button borders
        Object rolloverProp = toolBar.getClientProperty( IS_ROLLOVER );
        if (rolloverProp == null) {
            rolloverProp = UIManager.get("ToolBar.isRollover");
        }
        if ( rolloverProp != null ) {
            rolloverBorders = ((Boolean)rolloverProp).booleanValue();
        }

        if (rolloverBorder == null) {
            rolloverBorder = createRolloverBorder();
        }
        if (nonRolloverBorder == null) {
            nonRolloverBorder = createNonRolloverBorder();
        }
        if (nonRolloverToggleBorder == null) {
            nonRolloverToggleBorder = createNonRolloverToggleBorder();
        }


        setRolloverBorders( isRolloverBorders() );
    }

    /**
     * Uninstalls default properties.
     */
    protected void uninstallDefaults( )
    {
        LookAndFeel.uninstallBorder(toolBar);
        dockingColor = null;
        floatingColor = null;
        dockingBorderColor = null;
        floatingBorderColor = null;

        installNormalBorders(toolBar);

        rolloverBorder = null;
        nonRolloverBorder = null;
        nonRolloverToggleBorder = null;
    }

    /**
     * Registers components.
     */
    protected void installComponents( )
    {
    }

    /**
     * Unregisters components.
     */
    protected void uninstallComponents( )
    {
    }

    /**
     * Registers listeners.
     */
    protected void installListeners( )
    {
        dockingListener = createDockingListener( );

        if ( dockingListener != null )
        {
            toolBar.addMouseMotionListener( dockingListener );
            toolBar.addMouseListener( dockingListener );
        }

        propertyListener = createPropertyListener();  // added in setFloating
        if (propertyListener != null) {
            toolBar.addPropertyChangeListener(propertyListener);
        }

        toolBarContListener = createToolBarContListener();
        if ( toolBarContListener != null ) {
            toolBar.addContainerListener( toolBarContListener );
        }

        toolBarFocusListener = createToolBarFocusListener();

        if ( toolBarFocusListener != null )
        {
            // Put focus listener on all components in toolbar
            Component[] components = toolBar.getComponents();

            for (Component component : components) {
                component.addFocusListener(toolBarFocusListener);
            }
        }
    }

    /**
     * Unregisters listeners.
     */
    protected void uninstallListeners( )
    {
        if ( dockingListener != null )
        {
            toolBar.removeMouseMotionListener(dockingListener);
            toolBar.removeMouseListener(dockingListener);

            dockingListener = null;
        }

        if ( propertyListener != null )
        {
            toolBar.removePropertyChangeListener(propertyListener);
            propertyListener = null;  // removed in setFloating
        }

        if ( toolBarContListener != null )
        {
            toolBar.removeContainerListener( toolBarContListener );
            toolBarContListener = null;
        }

        if ( toolBarFocusListener != null )
        {
            // Remove focus listener from all components in toolbar
            Component[] components = toolBar.getComponents();

            for (Component component : components) {
                component.removeFocusListener(toolBarFocusListener);
            }

            toolBarFocusListener = null;
        }
        handler = null;
    }

    /**
     * Registers keyboard actions.
     */
    protected void installKeyboardActions( )
    {
        InputMap km = getInputMap(JComponent.
                                  WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        SwingUtilities.replaceUIInputMap(toolBar, JComponent.
                                         WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                                         km);

    LazyActionMap.installLazyActionMap(toolBar, BasicToolBarUI.class,
            "ToolBar.actionMap");
    }

    InputMap getInputMap(int condition) {
        if (condition == JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT) {
            return (InputMap)DefaultLookup.get(toolBar, this,
                    "ToolBar.ancestorInputMap");
        }
        return null;
    }

    static void loadActionMap(LazyActionMap map) {
        map.put(new Actions(Actions.NAVIGATE_RIGHT));
        map.put(new Actions(Actions.NAVIGATE_LEFT));
        map.put(new Actions(Actions.NAVIGATE_UP));
        map.put(new Actions(Actions.NAVIGATE_DOWN));
    }

    /**
     * Unregisters keyboard actions.
     */
    protected void uninstallKeyboardActions( )
    {
        SwingUtilities.replaceUIActionMap(toolBar, null);
        SwingUtilities.replaceUIInputMap(toolBar, JComponent.
                                         WHEN_ANCESTOR_OF_FOCUSED_COMPONENT,
                                         null);
    }

    /**
     * Navigates the focused component.
     *
     * @param direction a direction
     */
    @SuppressWarnings("deprecation")
    protected void navigateFocusedComp(int direction)
    {
        int nComp = toolBar.getComponentCount();
        int j;

        switch ( direction )
        {
            case EAST:
            case SOUTH:

                if ( focusedCompIndex < 0 || focusedCompIndex >= nComp ) break;

                j = focusedCompIndex + 1;

                while ( j != focusedCompIndex )
                {
                    if ( j >= nComp ) j = 0;
                    Component comp = toolBar.getComponentAtIndex( j++ );

                    if ( comp != null && comp.isFocusTraversable() && comp.isEnabled() )
                    {
                        comp.requestFocus();
                        break;
                    }
                }

                break;

            case WEST:
            case NORTH:

                if ( focusedCompIndex < 0 || focusedCompIndex >= nComp ) break;

                j = focusedCompIndex - 1;

                while ( j != focusedCompIndex )
                {
                    if ( j < 0 ) j = nComp - 1;
                    Component comp = toolBar.getComponentAtIndex( j-- );

                    if ( comp != null && comp.isFocusTraversable() && comp.isEnabled() )
                    {
                        comp.requestFocus();
                        break;
                    }
                }

                break;

            default:
                break;
        }
    }

    /**
     * Creates a rollover border for toolbar components. The
     * rollover border will be installed if rollover borders are
     * enabled.
     * <p>
     * Override this method to provide an alternate rollover border.
     *
     * @return a rollover border for toolbar components
     * @since 1.4
     */
    protected Border createRolloverBorder() {
        Object border = UIManager.get("ToolBar.rolloverBorder");
        if (border != null) {
            return (Border)border;
        }
        UIDefaults table = UIManager.getLookAndFeelDefaults();
        return new CompoundBorder(new BasicBorders.RolloverButtonBorder(
                                           table.getColor("controlShadow"),
                                           table.getColor("controlDkShadow"),
                                           table.getColor("controlHighlight"),
                                           table.getColor("controlLtHighlight")),
                                  new BasicBorders.RolloverMarginBorder());
    }

    /**
     * Creates the non rollover border for toolbar components. This
     * border will be installed as the border for components added
     * to the toolbar if rollover borders are not enabled.
     * <p>
     * Override this method to provide an alternate rollover border.
     *
     * @return the non rollover border for toolbar components
     * @since 1.4
     */
    protected Border createNonRolloverBorder() {
        Object border = UIManager.get("ToolBar.nonrolloverBorder");
        if (border != null) {
            return (Border)border;
        }
        UIDefaults table = UIManager.getLookAndFeelDefaults();
        return new CompoundBorder(new BasicBorders.ButtonBorder(
                                           table.getColor("Button.shadow"),
                                           table.getColor("Button.darkShadow"),
                                           table.getColor("Button.light"),
                                           table.getColor("Button.highlight")),
                                  new BasicBorders.RolloverMarginBorder());
    }

    /**
     * Creates a non rollover border for Toggle buttons in the toolbar.
     */
    private Border createNonRolloverToggleBorder() {
        UIDefaults table = UIManager.getLookAndFeelDefaults();
        return new CompoundBorder(new BasicBorders.RadioButtonBorder(
                                           table.getColor("ToggleButton.shadow"),
                                           table.getColor("ToggleButton.darkShadow"),
                                           table.getColor("ToggleButton.light"),
                                           table.getColor("ToggleButton.highlight")),
                                  new BasicBorders.RolloverMarginBorder());
    }

    /**
     * No longer used, use BasicToolBarUI.createFloatingWindow(JToolBar)
     *
     * @param toolbar an instance of {@code JToolBar}
     * @return an instance of {@code JFrame}
     * @see #createFloatingWindow
     * @deprecated It is recommended that {@link BasicToolBarUI#createFloatingWindow(JToolBar)}
     *             be used instead
     */
    @Deprecated(since = "17")
    protected JFrame createFloatingFrame(JToolBar toolbar) {
        Window window = SwingUtilities.getWindowAncestor(toolbar);
        @SuppressWarnings("serial") // anonymous class
        JFrame frame = new JFrame(toolbar.getName(),
                                  (window != null) ? window.getGraphicsConfiguration() : null) {
            // Override createRootPane() to automatically resize
            // the frame when contents change
            protected JRootPane createRootPane() {
                @SuppressWarnings("serial") // anonymous class
                JRootPane rootPane = new JRootPane() {
                    private boolean packing = false;

                    public void validate() {
                        super.validate();
                        if (!packing) {
                            packing = true;
                            pack();
                            packing = false;
                        }
                    }
                };
                rootPane.setOpaque(true);
                return rootPane;
            }
        };
        frame.getRootPane().setName("ToolBar.FloatingFrame");
        frame.setResizable(false);
        WindowListener wl = createFrameListener();
        frame.addWindowListener(wl);
        return frame;
    }

    /**
     * Creates a window which contains the toolbar after it has been
     * dragged out from its container
     *
     * @param toolbar an instance of {@code JToolBar}
     * @return a {@code RootPaneContainer} object, containing the toolbar
     * @since 1.4
     */
    protected RootPaneContainer createFloatingWindow(JToolBar toolbar) {
        @SuppressWarnings("serial") // Superclass is not serializable across versions
        class ToolBarDialog extends JDialog {
            public ToolBarDialog(Frame owner, String title, boolean modal) {
                super(owner, title, modal);
            }

            public ToolBarDialog(Dialog owner, String title, boolean modal) {
                super(owner, title, modal);
            }

            // Override createRootPane() to automatically resize
            // the frame when contents change
            protected JRootPane createRootPane() {
                @SuppressWarnings("serial") // anonymous class
                JRootPane rootPane = new JRootPane() {
                    private boolean packing = false;

                    public void validate() {
                        super.validate();
                        if (!packing) {
                            packing = true;
                            pack();
                            packing = false;
                        }
                    }
                };
                rootPane.setOpaque(true);
                return rootPane;
            }
        }

        JDialog dialog;
        Window window = SwingUtilities.getWindowAncestor(toolbar);
        if (window instanceof Frame) {
            dialog = new ToolBarDialog((Frame)window, toolbar.getName(), false);
        } else if (window instanceof Dialog) {
            dialog = new ToolBarDialog((Dialog)window, toolbar.getName(), false);
        } else {
            dialog = new ToolBarDialog((Frame)null, toolbar.getName(), false);
        }

        dialog.getRootPane().setName("ToolBar.FloatingWindow");
        dialog.setTitle(toolbar.getName());
        dialog.setResizable(false);
        WindowListener wl = createFrameListener();
        dialog.addWindowListener(wl);
        return dialog;
    }

    /**
     * Returns an instance of {@code DragWindow}.
     *
     * @param toolbar an instance of {@code JToolBar}
     * @return an instance of {@code DragWindow}
     */
    protected DragWindow createDragWindow(JToolBar toolbar) {
        Window frame = null;
        if(toolBar != null) {
            Container p;
            for(p = toolBar.getParent() ; p != null && !(p instanceof Window) ;
                p = p.getParent());
            if(p != null && p instanceof Window)
                frame = (Window) p;
        }
        if(floatingToolBar == null) {
            floatingToolBar = createFloatingWindow(toolBar);
        }
        if (floatingToolBar instanceof Window) frame = (Window) floatingToolBar;
        DragWindow dragWindow = new DragWindow(frame);
        return dragWindow;
    }

    /**
     * Returns a flag to determine whether rollover button borders
     * are enabled.
     *
     * @return true if rollover borders are enabled; false otherwise
     * @see #setRolloverBorders
     * @since 1.4
     */
    public boolean isRolloverBorders() {
        return rolloverBorders;
    }

    /**
     * Sets the flag for enabling rollover borders on the toolbar and it will
     * also install the appropriate border depending on the state of the flag.
     *
     * @param rollover if true, rollover borders are installed.
     *        Otherwise non-rollover borders are installed
     * @see #isRolloverBorders
     * @since 1.4
     */
    public void setRolloverBorders( boolean rollover ) {
        rolloverBorders = rollover;

        if ( rolloverBorders )  {
            installRolloverBorders( toolBar );
        } else  {
            installNonRolloverBorders( toolBar );
        }
    }

    /**
     * Installs rollover borders on all the child components of the JComponent.
     * <p>
     * This is a convenience method to call <code>setBorderToRollover</code>
     * for each child component.
     *
     * @param c container which holds the child components (usually a JToolBar)
     * @see #setBorderToRollover
     * @since 1.4
     */
    protected void installRolloverBorders ( JComponent c )  {
        // Put rollover borders on buttons
        Component[] components = c.getComponents();

        for (Component component : components) {
            if (component instanceof JComponent) {
                ((JComponent) component).updateUI();
                setBorderToRollover(component);
            }
        }
    }

    /**
     * Installs non-rollover borders on all the child components of the JComponent.
     * A non-rollover border is the border that is installed on the child component
     * while it is in the toolbar.
     * <p>
     * This is a convenience method to call <code>setBorderToNonRollover</code>
     * for each child component.
     *
     * @param c container which holds the child components (usually a JToolBar)
     * @see #setBorderToNonRollover
     * @since 1.4
     */
    protected void installNonRolloverBorders ( JComponent c )  {
        // Put non-rollover borders on buttons. These borders reduce the margin.
        Component[] components = c.getComponents();

        for (Component component : components) {
            if (component instanceof JComponent) {
                ((JComponent) component).updateUI();
                setBorderToNonRollover(component);
            }
        }
    }

    /**
     * Installs normal borders on all the child components of the JComponent.
     * A normal border is the original border that was installed on the child
     * component before it was added to the toolbar.
     * <p>
     * This is a convenience method to call <code>setBorderNormal</code>
     * for each child component.
     *
     * @param c container which holds the child components (usually a JToolBar)
     * @see #setBorderToNonRollover
     * @since 1.4
     */
    protected void installNormalBorders ( JComponent c )  {
        // Put back the normal borders on buttons
        Component[] components = c.getComponents();

        for (Component component : components) {
            setBorderToNormal(component);
        }
    }

    /**
     * Sets the border of the component to have a rollover border which
     * was created by the {@link #createRolloverBorder} method.
     *
     * @param c component which will have a rollover border installed
     * @see #createRolloverBorder
     * @since 1.4
     */
    protected void setBorderToRollover(Component c) {
        if (c instanceof AbstractButton) {
            AbstractButton b = (AbstractButton)c;

            Border border = borderTable.get(b);
            if (border == null || border instanceof UIResource) {
                borderTable.put(b, b.getBorder());
            }

            // Only set the border if its the default border
            if (b.getBorder() instanceof UIResource) {
                b.setBorder(getRolloverBorder(b));
            }

            rolloverTable.put(b, b.isRolloverEnabled()?
                              Boolean.TRUE: Boolean.FALSE);
            b.setRolloverEnabled(true);
        }
    }

    /**
     * Returns a rollover border for the button.
     *
     * @param b the button to calculate the rollover border for
     * @return the rollover border
     * @see #setBorderToRollover
     * @since 1.6
     */
    protected Border getRolloverBorder(AbstractButton b) {
        return rolloverBorder;
    }

    /**
     * Sets the border of the component to have a non-rollover border which
     * was created by the {@link #createNonRolloverBorder} method.
     *
     * @param c component which will have a non-rollover border installed
     * @see #createNonRolloverBorder
     * @since 1.4
     */
    protected void setBorderToNonRollover(Component c) {
        if (c instanceof AbstractButton) {
            AbstractButton b = (AbstractButton)c;

            Border border = borderTable.get(b);
            if (border == null || border instanceof UIResource) {
                borderTable.put(b, b.getBorder());
            }

            // Only set the border if its the default border
            if (b.getBorder() instanceof UIResource) {
                b.setBorder(getNonRolloverBorder(b));
            }
            rolloverTable.put(b, b.isRolloverEnabled()?
                              Boolean.TRUE: Boolean.FALSE);
            b.setRolloverEnabled(false);
        }
    }

    /**
     * Returns a non-rollover border for the button.
     *
     * @param b the button to calculate the non-rollover border for
     * @return the non-rollover border
     * @see #setBorderToNonRollover
     * @since 1.6
     */
    protected Border getNonRolloverBorder(AbstractButton b) {
        if (b instanceof JToggleButton) {
            return nonRolloverToggleBorder;
        } else {
            return nonRolloverBorder;
        }
    }

    /**
     * Sets the border of the component to have a normal border.
     * A normal border is the original border that was installed on the child
     * component before it was added to the toolbar.
     *
     * @param c component which will have a normal border re-installed
     * @see #createNonRolloverBorder
     * @since 1.4
     */
    protected void setBorderToNormal(Component c) {
        if (c instanceof AbstractButton) {
            AbstractButton b = (AbstractButton)c;

            Border border = borderTable.remove(b);
            b.setBorder(border);

            Boolean value = rolloverTable.remove(b);
            if (value != null) {
                b.setRolloverEnabled(value.booleanValue());
            }
        }
    }

    /**
     * Sets the floating location.
     *
     * @param x an X coordinate
     * @param y an Y coordinate
     */
    public void setFloatingLocation(int x, int y) {
        floatingX = x;
        floatingY = y;
    }

    /**
     * Returns {@code true} if the {@code JToolBar} is floating
     *
     * @return {@code true} if the {@code JToolBar} is floating
     */
    public boolean isFloating() {
        return floating;
    }

    /**
     * Sets the floating property.
     *
     * @param b {@code true} if the {@code JToolBar} is floating
     * @param p the position
     */
    @SuppressWarnings("deprecation")
    public void setFloating(boolean b, Point p) {
        if (toolBar.isFloatable()) {
            boolean visible = false;
            Window ancestor = SwingUtilities.getWindowAncestor(toolBar);
            if (ancestor != null) {
                visible = ancestor.isVisible();
            }
            if (dragWindow != null)
                dragWindow.setVisible(false);
            this.floating = b;
            if (floatingToolBar == null) {
                floatingToolBar = createFloatingWindow(toolBar);
            }
            if (b == true)
            {
                if (dockingSource == null)
                {
                    dockingSource = toolBar.getParent();
                    dockingSource.remove(toolBar);
                }
                constraintBeforeFloating = calculateConstraint();
                if ( propertyListener != null )
                    UIManager.addPropertyChangeListener( propertyListener );
                floatingToolBar.getContentPane().add(toolBar,BorderLayout.CENTER);
                if (floatingToolBar instanceof Window) {
                    ((Window)floatingToolBar).pack();
                    ((Window)floatingToolBar).setLocation(floatingX, floatingY);
                    if (visible) {
                        ((Window)floatingToolBar).show();
                    } else {
                        ancestor.addWindowListener(new WindowAdapter() {
                            public void windowOpened(WindowEvent e) {
                                ((Window)floatingToolBar).show();
                            }
                        });
                    }
                }
            } else {
                if (floatingToolBar == null)
                    floatingToolBar = createFloatingWindow(toolBar);
                if (floatingToolBar instanceof Window) ((Window)floatingToolBar).setVisible(false);
                floatingToolBar.getContentPane().remove(toolBar);
                String constraint = getDockingConstraint(dockingSource,
                                                         p);
                if (constraint == null) {
                    constraint = BorderLayout.NORTH;
                }
                int orientation = mapConstraintToOrientation(constraint);
                setOrientation(orientation);
                if (dockingSource== null)
                    dockingSource = toolBar.getParent();
                if ( propertyListener != null )
                    UIManager.removePropertyChangeListener( propertyListener );
                dockingSource.add(constraint, toolBar);
            }
            dockingSource.invalidate();
            Container dockingSourceParent = dockingSource.getParent();
            if (dockingSourceParent != null)
                dockingSourceParent.validate();
            dockingSource.repaint();
        }
    }

    private int mapConstraintToOrientation(String constraint)
    {
        int orientation = toolBar.getOrientation();

        if ( constraint != null )
        {
            if ( constraint.equals(BorderLayout.EAST) || constraint.equals(BorderLayout.WEST) )
                orientation = JToolBar.VERTICAL;
            else if ( constraint.equals(BorderLayout.NORTH) || constraint.equals(BorderLayout.SOUTH) )
                orientation = JToolBar.HORIZONTAL;
        }

        return orientation;
    }

    /**
     * Sets the tool bar's orientation.
     *
     * @param orientation the new orientation
     */
    public void setOrientation(int orientation)
    {
        toolBar.setOrientation( orientation );

        if (dragWindow !=null)
            dragWindow.setOrientation(orientation);
    }

    /**
     * Gets the color displayed when over a docking area
     *
     * @return the color displayed when over a docking area
     */
    public Color getDockingColor() {
        return dockingColor;
    }

    /**
     * Sets the color displayed when over a docking area
     *
     * @param c the new color
     */
   public void setDockingColor(Color c) {
        this.dockingColor = c;
    }

    /**
     * Gets the color displayed when over a floating area
     *
     * @return the color displayed when over a floating area
     */
    public Color getFloatingColor() {
        return floatingColor;
    }

    /**
     * Sets the color displayed when over a floating area
     *
     * @param c the new color
     */
    public void setFloatingColor(Color c) {
        this.floatingColor = c;
    }

    private boolean isBlocked(Component comp, Object constraint) {
        if (comp instanceof Container) {
            Container cont = (Container)comp;
            LayoutManager lm = cont.getLayout();
            if (lm instanceof BorderLayout) {
                BorderLayout blm = (BorderLayout)lm;
                Component c = blm.getLayoutComponent(cont, constraint);
                return (c != null && c != toolBar);
            }
        }
        return false;
    }

    /**
     * Returns {@code true} if the {@code JToolBar} can dock at the given position.
     *
     * @param c a component
     * @param p a position
     * @return {@code true} if the {@code JToolBar} can dock at the given position
     */
    public boolean canDock(Component c, Point p) {
        return (p != null && getDockingConstraint(c, p) != null);
    }

    private String calculateConstraint() {
        String constraint = null;
        LayoutManager lm = dockingSource.getLayout();
        if (lm instanceof BorderLayout) {
            constraint = (String)((BorderLayout)lm).getConstraints(toolBar);
        }
        return (constraint != null) ? constraint : constraintBeforeFloating;
    }



    private String getDockingConstraint(Component c, Point p) {
        if (p == null) return constraintBeforeFloating;
        if (c.contains(p)) {
            dockingSensitivity = (toolBar.getOrientation() == JToolBar.HORIZONTAL)
                                                ? toolBar.getSize().height
                                                : toolBar.getSize().width;
            // North  (Base distance on height for now!)
            if (p.y < dockingSensitivity && !isBlocked(c, BorderLayout.NORTH)) {
                return BorderLayout.NORTH;
            }
            // East  (Base distance on height for now!)
            if (p.x >= c.getWidth() - dockingSensitivity && !isBlocked(c, BorderLayout.EAST)) {
                return BorderLayout.EAST;
            }
            // West  (Base distance on height for now!)
            if (p.x < dockingSensitivity && !isBlocked(c, BorderLayout.WEST)) {
                return BorderLayout.WEST;
            }
            if (p.y >= c.getHeight() - dockingSensitivity && !isBlocked(c, BorderLayout.SOUTH)) {
                return BorderLayout.SOUTH;
            }
        }
        return null;
    }

    /**
     * The method is used to drag {@code DragWindow} during the {@code JToolBar}
     * is being dragged.
     *
     * @param position the relative to the {@code JTollBar} position
     * @param origin the screen position of {@code JToolBar} before dragging
     */
    @SuppressWarnings("deprecation")
    protected void dragTo(Point position, Point origin)
    {
        if (toolBar.isFloatable())
        {
          try
          {
            if (dragWindow == null)
                dragWindow = createDragWindow(toolBar);
            Point offset = dragWindow.getOffset();
            if (offset == null) {
                Dimension size = toolBar.getPreferredSize();
                offset = new Point(size.width/2, size.height/2);
                dragWindow.setOffset(offset);
            }
            Point global = new Point(origin.x+ position.x,
                                     origin.y+position.y);
            Point dragPoint = new Point(global.x- offset.x,
                                        global.y- offset.y);
            if (dockingSource == null)
                dockingSource = toolBar.getParent();
                constraintBeforeFloating = calculateConstraint();
            Point dockingPosition = dockingSource.getLocationOnScreen();
            Point comparisonPoint = new Point(global.x-dockingPosition.x,
                                              global.y-dockingPosition.y);
            if (canDock(dockingSource, comparisonPoint)) {
                dragWindow.setBackground(getDockingColor());
                String constraint = getDockingConstraint(dockingSource,
                                                         comparisonPoint);
                int orientation = mapConstraintToOrientation(constraint);
                dragWindow.setOrientation(orientation);
                dragWindow.setBorderColor(dockingBorderColor);
            } else {
                dragWindow.setBackground(getFloatingColor());
                dragWindow.setBorderColor(floatingBorderColor);
                dragWindow.setOrientation(toolBar.getOrientation());
            }

            dragWindow.setLocation(dragPoint.x, dragPoint.y);
            if (dragWindow.isVisible() == false) {
                Dimension size = toolBar.getPreferredSize();
                dragWindow.setSize(size.width, size.height);
                dragWindow.show();
            }
          }
          catch ( IllegalComponentStateException e )
          {
          }
        }
    }

    /**
     * The method is called at end of dragging to place the frame in either
     * its original place or in its floating frame.
     *
     * @param position the relative to the {@code JTollBar} position
     * @param origin the screen position of {@code JToolBar} before dragging
     */
    protected void floatAt(Point position, Point origin)
    {
        if(toolBar.isFloatable())
        {
          try
          {
            Point offset = dragWindow.getOffset();
            if (offset == null) {
                offset = position;
                dragWindow.setOffset(offset);
            }
            Point global = new Point(origin.x+ position.x,
                                     origin.y+position.y);
            setFloatingLocation(global.x-offset.x,
                                global.y-offset.y);
            if (dockingSource != null) {
                Point dockingPosition = dockingSource.getLocationOnScreen();
                Point comparisonPoint = new Point(global.x-dockingPosition.x,
                                                  global.y-dockingPosition.y);
                if (canDock(dockingSource, comparisonPoint)) {
                    setFloating(false, comparisonPoint);
                } else {
                    setFloating(true, null);
                }
            } else {
                setFloating(true, null);
            }
            dragWindow.setOffset(null);
          }
          catch ( IllegalComponentStateException e )
          {
          }
        }
    }

    private Handler getHandler() {
        if (handler == null) {
            handler = new Handler();
        }
        return handler;
    }

    /**
     * Returns an instance of {@code ContainerListener}.
     *
     * @return an instance of {@code ContainerListener}
     */
    protected ContainerListener createToolBarContListener( )
    {
        return getHandler();
    }

    /**
     * Returns an instance of {@code FocusListener}.
     *
     * @return an instance of {@code FocusListener}
     */
    protected FocusListener createToolBarFocusListener( )
    {
        return getHandler();
    }

    /**
     * Returns an instance of {@code PropertyChangeListener}.
     *
     * @return an instance of {@code PropertyChangeListener}
     */
    protected PropertyChangeListener createPropertyListener()
    {
        return getHandler();
    }

    /**
     * Returns an instance of {@code MouseInputListener}.
     *
     * @return an instance of {@code MouseInputListener}
     */
    protected MouseInputListener createDockingListener( ) {
        getHandler().tb = toolBar;
        return getHandler();
    }

    /**
     * Constructs a new instance of {@code WindowListener}.
     *
     * @return a new instance of {@code WindowListener}
     */
    protected WindowListener createFrameListener() {
        return new FrameListener();
    }

    /**
     * Paints the contents of the window used for dragging.
     *
     * @param g Graphics to paint to.
     * @throws NullPointerException is <code>g</code> is null
     * @since 1.5
     */
    protected void paintDragWindow(Graphics g) {
        g.setColor(dragWindow.getBackground());
        int w = dragWindow.getWidth();
        int h = dragWindow.getHeight();
        g.fillRect(0, 0, w, h);
        g.setColor(dragWindow.getBorderColor());
        g.drawRect(0, 0, w - 1, h - 1);
    }


    private static class Actions extends UIAction {
        private static final String NAVIGATE_RIGHT = "navigateRight";
        private static final String NAVIGATE_LEFT = "navigateLeft";
        private static final String NAVIGATE_UP = "navigateUp";
        private static final String NAVIGATE_DOWN = "navigateDown";

        public Actions(String name) {
            super(name);
        }

        public void actionPerformed(ActionEvent evt) {
            String key = getName();
            JToolBar toolBar = (JToolBar)evt.getSource();
            BasicToolBarUI ui = (BasicToolBarUI)BasicLookAndFeel.getUIOfType(
                     toolBar.getUI(), BasicToolBarUI.class);

            if (NAVIGATE_RIGHT == key) {
                ui.navigateFocusedComp(EAST);
            } else if (NAVIGATE_LEFT == key) {
                ui.navigateFocusedComp(WEST);
            } else if (NAVIGATE_UP == key) {
                ui.navigateFocusedComp(NORTH);
            } else if (NAVIGATE_DOWN == key) {
                ui.navigateFocusedComp(SOUTH);
            }
        }
    }


    private class Handler implements ContainerListener,
            FocusListener, MouseInputListener, PropertyChangeListener {

        //
        // ContainerListener
        //
        public void componentAdded(ContainerEvent evt) {
            Component c = evt.getChild();

            if (toolBarFocusListener != null) {
                c.addFocusListener(toolBarFocusListener);
            }

            if (isRolloverBorders()) {
                setBorderToRollover(c);
            } else {
                setBorderToNonRollover(c);
            }
        }

        public void componentRemoved(ContainerEvent evt) {
            Component c = evt.getChild();

            if (toolBarFocusListener != null) {
                c.removeFocusListener(toolBarFocusListener);
            }

            // Revert the button border
            setBorderToNormal(c);
        }


        //
        // FocusListener
        //
        public void focusGained(FocusEvent evt) {
            Component c = evt.getComponent();
            focusedCompIndex = toolBar.getComponentIndex(c);
        }

        public void focusLost(FocusEvent evt) { }


        //
        // MouseInputListener (DockingListener)
        //
        JToolBar tb;
        boolean isDragging = false;
        Point origin = null;

        public void mousePressed(MouseEvent evt) {
            if (!tb.isEnabled()) {
                return;
            }
            isDragging = false;
        }

        public void mouseReleased(MouseEvent evt) {
            if (!tb.isEnabled()) {
                return;
            }
            if (isDragging) {
                Point position = evt.getPoint();
                if (origin == null)
                    origin = evt.getComponent().getLocationOnScreen();
                floatAt(position, origin);
            }
            origin = null;
            isDragging = false;
        }

        public void mouseDragged(MouseEvent evt) {
            if (!tb.isEnabled()) {
                return;
            }
            isDragging = true;
            Point position = evt.getPoint();
            if (origin == null) {
                origin = evt.getComponent().getLocationOnScreen();
            }
            dragTo(position, origin);
        }

        public void mouseClicked(MouseEvent evt) {}
        public void mouseEntered(MouseEvent evt) {}
        public void mouseExited(MouseEvent evt) {}
        public void mouseMoved(MouseEvent evt) {}


        //
        // PropertyChangeListener
        //
        public void propertyChange(PropertyChangeEvent evt) {
            String propertyName = evt.getPropertyName();
            if (propertyName == "lookAndFeel") {
                toolBar.updateUI();
            } else if (propertyName == "orientation") {
                // Search for JSeparator components and change it's orientation
                // to match the toolbar and flip it's orientation.
                Component[] components = toolBar.getComponents();
                int orientation = ((Integer)evt.getNewValue()).intValue();
                JToolBar.Separator separator;

                for (int i = 0; i < components.length; ++i) {
                    if (components[i] instanceof JToolBar.Separator) {
                        separator = (JToolBar.Separator)components[i];
                        if ((orientation == JToolBar.HORIZONTAL)) {
                            separator.setOrientation(JSeparator.VERTICAL);
                        } else {
                            separator.setOrientation(JSeparator.HORIZONTAL);
                        }
                        Dimension size = separator.getSeparatorSize();
                        if (size != null && size.width != size.height) {
                            // Flip the orientation.
                            Dimension newSize =
                                new Dimension(size.height, size.width);
                            separator.setSeparatorSize(newSize);
                        }
                    }
                }
            } else if (propertyName == IS_ROLLOVER) {
                installNormalBorders(toolBar);
                setRolloverBorders(((Boolean)evt.getNewValue()).booleanValue());
            }
        }
    }

    /**
     * The class listens for window events.
     */
    protected class FrameListener extends WindowAdapter {
        /**
         * Constructs a {@code FrameListener}.
         */
        protected FrameListener() {}

        public void windowClosing(WindowEvent w) {
            if (toolBar.isFloatable()) {
                if (dragWindow != null)
                    dragWindow.setVisible(false);
                floating = false;
                if (floatingToolBar == null)
                    floatingToolBar = createFloatingWindow(toolBar);
                if (floatingToolBar instanceof Window) ((Window)floatingToolBar).setVisible(false);
                floatingToolBar.getContentPane().remove(toolBar);
                String constraint = constraintBeforeFloating;
                if (toolBar.getOrientation() == JToolBar.HORIZONTAL) {
                    if (constraint == "West" || constraint == "East") {
                        constraint = "North";
                    }
                } else {
                    if (constraint == "North" || constraint == "South") {
                        constraint = "West";
                    }
                }
                if (dockingSource == null)
                    dockingSource = toolBar.getParent();
                if (propertyListener != null)
                    UIManager.removePropertyChangeListener(propertyListener);
                dockingSource.add(toolBar, constraint);
                dockingSource.invalidate();
                Container dockingSourceParent = dockingSource.getParent();
                if (dockingSourceParent != null)
                        dockingSourceParent.validate();
                dockingSource.repaint();
            }
        }

    }

    /**
     * The class listens for component events.
     */
    protected class ToolBarContListener implements ContainerListener {
        // NOTE: This class exists only for backward compatibility. All
        // its functionality has been moved into Handler. If you need to add
        // new functionality add it to the Handler, but make sure this
        // class calls into the Handler.
        /**
         * Constructs a {@code ToolBarContListener}.
         */
        protected ToolBarContListener() {}

        public void componentAdded( ContainerEvent e )  {
            getHandler().componentAdded(e);
        }

        public void componentRemoved( ContainerEvent e ) {
            getHandler().componentRemoved(e);
        }

    }

    /**
     * The class listens for focus events.
     */
    protected class ToolBarFocusListener implements FocusListener {
        // NOTE: This class exists only for backward compatibility. All
        // its functionality has been moved into Handler. If you need to add
        // new functionality add it to the Handler, but make sure this
        // class calls into the Handler.
        /**
         * Constructs a {@code ToolBarFocusListener}.
         */
        protected ToolBarFocusListener() {}

        public void focusGained( FocusEvent e ) {
            getHandler().focusGained(e);
            }

        public void focusLost( FocusEvent e ) {
            getHandler().focusLost(e);
            }
    }

    /**
     * The class listens for property changed events.
     */
    protected class PropertyListener implements PropertyChangeListener {
        // NOTE: This class exists only for backward compatibility. All
        // its functionality has been moved into Handler. If you need to add
        // new functionality add it to the Handler, but make sure this
        // class calls into the Handler.
        /**
         * Constructs a {@code PropertyListener}.
         */
        protected PropertyListener() {}

        public void propertyChange( PropertyChangeEvent e ) {
            getHandler().propertyChange(e);
            }
    }

    /**
     * This class should be treated as a &quot;protected&quot; inner class.
     * Instantiate it only within subclasses of BasicToolBarUI.
     */
    public class DockingListener implements MouseInputListener {
        // NOTE: This class exists only for backward compatibility. All
        // its functionality has been moved into Handler. If you need to add
        // new functionality add it to the Handler, but make sure this
        // class calls into the Handler.
        /**
         * The instance of {@code JToolBar}.
         */
        protected JToolBar toolBar;
        /**
         * {@code true} if the {@code JToolBar} is being dragged.
         */
        protected boolean isDragging = false;
        /**
         * The origin point.
         */
        protected Point origin = null;

        /**
         * Constructs a new instance of {@code DockingListener}.
         *
         * @param t an instance of {@code JToolBar}
         */
        public DockingListener(JToolBar t) {
            this.toolBar = t;
            getHandler().tb = t;
        }

        public void mouseClicked(MouseEvent e) {
        getHandler().mouseClicked(e);
    }

        public void mousePressed(MouseEvent e) {
        getHandler().tb = toolBar;
        getHandler().mousePressed(e);
        isDragging = getHandler().isDragging;
        }

        public void mouseReleased(MouseEvent e) {
        getHandler().tb = toolBar;
        getHandler().isDragging = isDragging;
        getHandler().origin = origin;
        getHandler().mouseReleased(e);
        isDragging = getHandler().isDragging;
        origin = getHandler().origin;
        }

        public void mouseEntered(MouseEvent e) {
        getHandler().mouseEntered(e);
    }

        public void mouseExited(MouseEvent e) {
        getHandler().mouseExited(e);
    }

        public void mouseDragged(MouseEvent e) {
        getHandler().tb = toolBar;
        getHandler().origin = origin;
        getHandler().mouseDragged(e);
        isDragging = getHandler().isDragging;
        origin = getHandler().origin;
        }

        public void mouseMoved(MouseEvent e) {
        getHandler().mouseMoved(e);
        }
    }

    /**
     * The window which appears during dragging the {@code JToolBar}.
     */
    @SuppressWarnings("serial") // Same-version serialization only
    protected class DragWindow extends Window
    {
        Color borderColor = Color.gray;
        int orientation = toolBar.getOrientation();
        Point offset; // offset of the mouse cursor inside the DragWindow

        DragWindow(Window w) {
            super(w);
        }

    /**
     * Returns the orientation of the toolbar window when the toolbar is
     * floating. The orientation is either one of <code>JToolBar.HORIZONTAL</code>
     * or <code>JToolBar.VERTICAL</code>.
     *
     * @return the orientation of the toolbar window
     * @since 1.6
     */
    public int getOrientation() {
        return orientation;
    }

        /**
         * Sets the orientation.
         *
         * @param o the new orientation
         */
        public void setOrientation(int o) {
            if(isShowing()) {
                if (o == this.orientation)
                    return;
                this.orientation = o;
                Dimension size = getSize();
                setSize(new Dimension(size.height, size.width));
                if (offset!=null) {
                    if( BasicGraphicsUtils.isLeftToRight(toolBar) ) {
                        setOffset(new Point(offset.y, offset.x));
                    } else if( o == JToolBar.HORIZONTAL ) {
                        setOffset(new Point( size.height-offset.y, offset.x));
                    } else {
                        setOffset(new Point(offset.y, size.width-offset.x));
                    }
                }
                repaint();
            }
        }

        /**
         * Returns the offset.
         *
         * @return the offset
         */
        public Point getOffset() {
            return offset;
        }

        /**
         * Sets the offset.
         *
         * @param p the new offset
         */
        public void setOffset(Point p) {
            this.offset = p;
        }

        /**
         * Sets the border color.
         *
         * @param c the new border color
         */
        public void setBorderColor(Color c) {
            if (this.borderColor == c)
                return;
            this.borderColor = c;
            repaint();
        }

        /**
         * Returns the border color.
         *
         * @return the border color
         */
        public Color getBorderColor() {
            return this.borderColor;
        }

        public void paint(Graphics g) {
            paintDragWindow(g);
            // Paint the children
            super.paint(g);
        }
        public Insets getInsets() {
            return new Insets(1,1,1,1);
        }
    }
}
