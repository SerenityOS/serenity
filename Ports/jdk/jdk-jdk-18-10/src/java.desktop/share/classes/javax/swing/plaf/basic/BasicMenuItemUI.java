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

import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.*;
import javax.swing.text.View;

import sun.swing.*;

/**
 * BasicMenuItem implementation
 *
 * @author Georges Saab
 * @author David Karlton
 * @author Arnaud Weber
 * @author Fredrik Lagerblad
 */
public class BasicMenuItemUI extends MenuItemUI
{
    /**
     * The instance of {@code JMenuItem}.
     */
    protected JMenuItem menuItem = null;
    /**
     * The color of the selection background.
     */
    protected Color selectionBackground;
    /**
     * The color of the selection foreground.
     */
    protected Color selectionForeground;
    /**
     * The color of the disabled foreground.
     */
    protected Color disabledForeground;
    /**
     * The color of the accelerator foreground.
     */
    protected Color acceleratorForeground;
    /**
     * The color of the accelerator selection.
     */
    protected Color acceleratorSelectionForeground;

    /**
     * Accelerator delimiter string, such as {@code '+'} in {@code 'Ctrl+C'}.
     * @since 1.7
     */
    protected String acceleratorDelimiter;

    /**
     * The gap between the text and the icon.
     */
    protected int defaultTextIconGap;
    /**
     * The accelerator font.
     */
    protected Font acceleratorFont;

    /**
     * The instance of {@code MouseInputListener}.
     */
    protected MouseInputListener mouseInputListener;
    /**
     * The instance of {@code MenuDragMouseListener}.
     */
    protected MenuDragMouseListener menuDragMouseListener;
    /**
     * The instance of {@code MenuKeyListener}.
     */
    protected MenuKeyListener menuKeyListener;
    /**
     * {@code PropertyChangeListener} returned from
     * {@code createPropertyChangeListener}. You should not
     * need to access this field, rather if you want to customize the
     * {@code PropertyChangeListener} override
     * {@code createPropertyChangeListener}.
     *
     * @since 1.6
     * @see #createPropertyChangeListener
     */
    protected PropertyChangeListener propertyChangeListener;
    // BasicMenuUI also uses this.
    Handler handler;
    /**
     * The arrow icon.
     */
    protected Icon arrowIcon = null;
    /**
     * The check icon.
     */
    protected Icon checkIcon = null;
    /**
     * The value represents if the old border is painted.
     */
    protected boolean oldBorderPainted;

    /* diagnostic aids -- should be false for production builds. */
    private static final boolean TRACE =   false; // trace creates and disposes

    private static final boolean VERBOSE = false; // show reuse hits/misses
    private static final boolean DEBUG =   false;  // show bad params, misc.

    /**
     * Constructs a {@code BasicMenuItemUI}.
     */
    public BasicMenuItemUI() {}

    static void loadActionMap(LazyActionMap map) {
        // NOTE: BasicMenuUI also calls into this method.
        map.put(new Actions(Actions.CLICK));
        BasicLookAndFeel.installAudioActionMap(map);
    }

    /**
     * Returns a new instance of {@code BasicMenuItemUI}.
     *
     * @param c a component
     * @return a new instance of {@code BasicMenuItemUI}
     */
    public static ComponentUI createUI(JComponent c) {
        return new BasicMenuItemUI();
    }

    public void installUI(JComponent c) {
        menuItem = (JMenuItem) c;

        installDefaults();
        installComponents(menuItem);
        installListeners();
        installKeyboardActions();
    }

    /**
     * Installs default properties.
     */
    protected void installDefaults() {
        String prefix = getPropertyPrefix();

        acceleratorFont = UIManager.getFont("MenuItem.acceleratorFont");
        // use default if missing so that BasicMenuItemUI can be used in other
        // LAFs like Nimbus
        if (acceleratorFont == null) {
            acceleratorFont = UIManager.getFont("MenuItem.font");
        }

        Object opaque = UIManager.get(getPropertyPrefix() + ".opaque");
        if (opaque != null) {
            LookAndFeel.installProperty(menuItem, "opaque", opaque);
        }
        else {
            LookAndFeel.installProperty(menuItem, "opaque", Boolean.TRUE);
        }
        if(menuItem.getMargin() == null ||
           (menuItem.getMargin() instanceof UIResource)) {
            menuItem.setMargin(UIManager.getInsets(prefix + ".margin"));
        }

        LookAndFeel.installProperty(menuItem, "iconTextGap", Integer.valueOf(4));
        defaultTextIconGap = menuItem.getIconTextGap();

        LookAndFeel.installBorder(menuItem, prefix + ".border");
        oldBorderPainted = menuItem.isBorderPainted();
        LookAndFeel.installProperty(menuItem, "borderPainted",
                                    UIManager.getBoolean(prefix + ".borderPainted"));
        LookAndFeel.installColorsAndFont(menuItem,
                                         prefix + ".background",
                                         prefix + ".foreground",
                                         prefix + ".font");

        // MenuItem specific defaults
        if (selectionBackground == null ||
            selectionBackground instanceof UIResource) {
            selectionBackground =
                UIManager.getColor(prefix + ".selectionBackground");
        }
        if (selectionForeground == null ||
            selectionForeground instanceof UIResource) {
            selectionForeground =
                UIManager.getColor(prefix + ".selectionForeground");
        }
        if (disabledForeground == null ||
            disabledForeground instanceof UIResource) {
            disabledForeground =
                UIManager.getColor(prefix + ".disabledForeground");
        }
        if (acceleratorForeground == null ||
            acceleratorForeground instanceof UIResource) {
            acceleratorForeground =
                UIManager.getColor(prefix + ".acceleratorForeground");
        }
        if (acceleratorSelectionForeground == null ||
            acceleratorSelectionForeground instanceof UIResource) {
            acceleratorSelectionForeground =
                UIManager.getColor(prefix + ".acceleratorSelectionForeground");
        }
        // Get accelerator delimiter
        acceleratorDelimiter =
            UIManager.getString("MenuItem.acceleratorDelimiter");
        if (acceleratorDelimiter == null) { acceleratorDelimiter = "+"; }
        // Icons
        if (arrowIcon == null ||
            arrowIcon instanceof UIResource) {
            arrowIcon = UIManager.getIcon(prefix + ".arrowIcon");
        }

        if (checkIcon == null ||
            checkIcon instanceof UIResource) {
            checkIcon = UIManager.getIcon(prefix + ".checkIcon");
            //In case of column layout, .checkIconFactory is defined for this UI,
            //the icon is compatible with it and useCheckAndArrow() is true,
            //then the icon is handled by the checkIcon.
            boolean isColumnLayout = MenuItemLayoutHelper.isColumnLayout(
                    BasicGraphicsUtils.isLeftToRight(menuItem), menuItem);
            if (isColumnLayout) {
                MenuItemCheckIconFactory iconFactory =
                        (MenuItemCheckIconFactory) UIManager.get(prefix
                                + ".checkIconFactory");
                if (iconFactory != null
                        && MenuItemLayoutHelper.useCheckAndArrow(menuItem)
                        && iconFactory.isCompatible(checkIcon, prefix)) {
                    checkIcon = iconFactory.getIcon(menuItem);
                }
            }
        }
    }

    /**
     *
     * @param menuItem a menu item
     * @since 1.3
     */
    protected void installComponents(JMenuItem menuItem){
        BasicHTML.updateRenderer(menuItem, menuItem.getText());
    }

    /**
     * Returns a property prefix.
     *
     * @return a property prefix
     */
    protected String getPropertyPrefix() {
        return "MenuItem";
    }

    /**
     * Registers listeners.
     */
    protected void installListeners() {
        if ((mouseInputListener = createMouseInputListener(menuItem)) != null) {
            menuItem.addMouseListener(mouseInputListener);
            menuItem.addMouseMotionListener(mouseInputListener);
        }
        if ((menuDragMouseListener = createMenuDragMouseListener(menuItem)) != null) {
            menuItem.addMenuDragMouseListener(menuDragMouseListener);
        }
        if ((menuKeyListener = createMenuKeyListener(menuItem)) != null) {
            menuItem.addMenuKeyListener(menuKeyListener);
        }
        if ((propertyChangeListener = createPropertyChangeListener(menuItem)) != null) {
            menuItem.addPropertyChangeListener(propertyChangeListener);
        }
    }

    /**
     * Registers keyboard action.
     */
    protected void installKeyboardActions() {
        installLazyActionMap();
        updateAcceleratorBinding();
    }

    void installLazyActionMap() {
        LazyActionMap.installLazyActionMap(menuItem, BasicMenuItemUI.class,
                                           getPropertyPrefix() + ".actionMap");
    }

    public void uninstallUI(JComponent c) {
        menuItem = (JMenuItem)c;
        uninstallDefaults();
        uninstallComponents(menuItem);
        uninstallListeners();
        uninstallKeyboardActions();
        MenuItemLayoutHelper.clearUsedParentClientProperties(menuItem);
        menuItem = null;
    }

    /**
     * Uninstalls default properties.
     */
    protected void uninstallDefaults() {
        LookAndFeel.uninstallBorder(menuItem);
        LookAndFeel.installProperty(menuItem, "borderPainted", oldBorderPainted);
        if (menuItem.getMargin() instanceof UIResource)
            menuItem.setMargin(null);
        if (arrowIcon instanceof UIResource)
            arrowIcon = null;
        if (checkIcon instanceof UIResource)
            checkIcon = null;
    }

    /**
     * Unregisters components.
     *
     * @param menuItem a menu item
     * @since 1.3
     */
    protected void uninstallComponents(JMenuItem menuItem){
        BasicHTML.updateRenderer(menuItem, "");
    }

    /**
     * Unregisters listeners.
     */
    protected void uninstallListeners() {
        if (mouseInputListener != null) {
            menuItem.removeMouseListener(mouseInputListener);
            menuItem.removeMouseMotionListener(mouseInputListener);
        }
        if (menuDragMouseListener != null) {
            menuItem.removeMenuDragMouseListener(menuDragMouseListener);
        }
        if (menuKeyListener != null) {
            menuItem.removeMenuKeyListener(menuKeyListener);
        }
        if (propertyChangeListener != null) {
            menuItem.removePropertyChangeListener(propertyChangeListener);
        }

        mouseInputListener = null;
        menuDragMouseListener = null;
        menuKeyListener = null;
        propertyChangeListener = null;
        handler = null;
    }

    /**
     * Unregisters keyboard actions.
     */
    protected void uninstallKeyboardActions() {
        SwingUtilities.replaceUIActionMap(menuItem, null);
        SwingUtilities.replaceUIInputMap(menuItem, JComponent.
                                         WHEN_IN_FOCUSED_WINDOW, null);
    }

    /**
     * Returns an instance of {@code MouseInputListener}.
     *
     * @param c a component
     * @return an instance of {@code MouseInputListener}
     */
    protected MouseInputListener createMouseInputListener(JComponent c) {
        return getHandler();
    }

    /**
     * Returns an instance of {@code MenuDragMouseListener}.
     *
     * @param c a component
     * @return an instance of {@code MenuDragMouseListener}
     */
    protected MenuDragMouseListener createMenuDragMouseListener(JComponent c) {
        return getHandler();
    }

    /**
     * Returns an instance of {@code MenuKeyListener}.
     *
     * @param c a component
     * @return an instance of {@code MenuKeyListener}
     */
    protected MenuKeyListener createMenuKeyListener(JComponent c) {
        return null;
    }

    /**
     * Creates a {@code PropertyChangeListener} which will be added to
     * the menu item.
     * If this method returns null then it will not be added to the menu item.
     *
     * @param c a component
     * @return an instance of a {@code PropertyChangeListener} or null
     * @since 1.6
     */
    protected PropertyChangeListener
                                  createPropertyChangeListener(JComponent c) {
        return getHandler();
    }

    Handler getHandler() {
        if (handler == null) {
            handler = new Handler();
        }
        return handler;
    }

    InputMap createInputMap(int condition) {
        if (condition == JComponent.WHEN_IN_FOCUSED_WINDOW) {
            return new ComponentInputMapUIResource(menuItem);
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    void updateAcceleratorBinding() {
        KeyStroke accelerator = menuItem.getAccelerator();
        InputMap windowInputMap = SwingUtilities.getUIInputMap(
                       menuItem, JComponent.WHEN_IN_FOCUSED_WINDOW);

        if (windowInputMap != null) {
            windowInputMap.clear();
        }
        if (accelerator != null) {
            if (windowInputMap == null) {
                windowInputMap = createInputMap(JComponent.
                                                WHEN_IN_FOCUSED_WINDOW);
                SwingUtilities.replaceUIInputMap(menuItem,
                           JComponent.WHEN_IN_FOCUSED_WINDOW, windowInputMap);
            }
            windowInputMap.put(accelerator, "doClick");

            int modifiers = accelerator.getModifiers();
            if (((modifiers & InputEvent.ALT_DOWN_MASK) != 0) &&
                    ((modifiers & InputEvent.ALT_GRAPH_DOWN_MASK) != 0)) {
                //When both ALT and ALT_GRAPH are set, add the ALT only
                // modifier keystroke which is used for left ALT key.
                // Unsetting the ALT_GRAPH will do that as ALT is already set
                modifiers &= ~InputEvent.ALT_GRAPH_DOWN_MASK;
                modifiers &= ~InputEvent.ALT_GRAPH_MASK;
                KeyStroke keyStroke = KeyStroke.getKeyStroke(accelerator.getKeyCode(),
                        modifiers, accelerator.isOnKeyRelease());
                windowInputMap.put(keyStroke, "doClick");
            } else if (((modifiers & InputEvent.ALT_DOWN_MASK) != 0) && (
                    (modifiers & InputEvent.ALT_GRAPH_DOWN_MASK) == 0)) {
                //When only ALT modifier is set, add the ALT + ALT_GRAPH
                // modifier keystroke which is used for right ALT key
                modifiers |= InputEvent.ALT_GRAPH_DOWN_MASK;
                KeyStroke keyStroke = KeyStroke.getKeyStroke(accelerator.getKeyCode(),
                        modifiers, accelerator.isOnKeyRelease());
                windowInputMap.put(keyStroke, "doClick");
            } else if ((modifiers & InputEvent.ALT_GRAPH_DOWN_MASK) != 0) {
                //When only ALT_GRAPH is set, remove the ALT_GRAPH only
                // modifier and add the ALT and ALT+ALT_GRAPH modifiers
                // keystroke which are used for left ALT key and right ALT
                // respectively
                modifiers &= ~InputEvent.ALT_GRAPH_DOWN_MASK;
                modifiers &= ~InputEvent.ALT_GRAPH_MASK;

                modifiers |= InputEvent.ALT_DOWN_MASK;
                KeyStroke keyStroke = KeyStroke.getKeyStroke(accelerator.getKeyCode(),
                        modifiers, accelerator.isOnKeyRelease());
                windowInputMap.put(keyStroke, "doClick");

                //Add ALT+ALT_GRAPH modifier which is used for right ALT key
                modifiers |= InputEvent.ALT_GRAPH_DOWN_MASK;
                keyStroke = KeyStroke.getKeyStroke(accelerator.getKeyCode(),
                        modifiers, accelerator.isOnKeyRelease());
                windowInputMap.put(keyStroke, "doClick");
            }
        }
    }

    public Dimension getMinimumSize(JComponent c) {
        Dimension d = null;
        View v = (View) c.getClientProperty(BasicHTML.propertyKey);
        if (v != null) {
            d = getPreferredSize(c);
            d.width -= v.getPreferredSpan(View.X_AXIS) - v.getMinimumSpan(View.X_AXIS);
        }
        return d;
    }

    public Dimension getPreferredSize(JComponent c) {
        return getPreferredMenuItemSize(c,
                                        checkIcon,
                                        arrowIcon,
                                        defaultTextIconGap);
    }

    public Dimension getMaximumSize(JComponent c) {
        Dimension d = null;
        View v = (View) c.getClientProperty(BasicHTML.propertyKey);
        if (v != null) {
            d = getPreferredSize(c);
            d.width += v.getMaximumSpan(View.X_AXIS) - v.getPreferredSpan(View.X_AXIS);
        }
        return d;
    }

    /**
     * Returns the preferred size of a menu item.
     *
     * @param c a component
     * @param checkIcon a check icon
     * @param arrowIcon an arrow icon
     * @param defaultTextIconGap a gap between a text and an icon
     * @return the preferred size of a menu item
     */
    protected Dimension getPreferredMenuItemSize(JComponent c,
                                                 Icon checkIcon,
                                                 Icon arrowIcon,
                                                 int defaultTextIconGap) {

        // The method also determines the preferred width of the
        // parent popup menu (through DefaultMenuLayout class).
        // The menu width equals to the maximal width
        // among child menu items.

        // Menu item width will be a sum of the widest check icon, label,
        // arrow icon and accelerator text among neighbor menu items.
        // For the latest menu item we will know the maximal widths exactly.
        // It will be the widest menu item and it will determine
        // the width of the parent popup menu.

        // !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
        // There is a conceptual problem: if user sets preferred size manually
        // for a menu item, this method won't be called for it
        // (see JComponent.getPreferredSize()),
        // maximal widths won't be calculated, other menu items won't be able
        // to take them into account and will be layouted in such a way,
        // as there is no the item with manual preferred size.
        // But after the first paint() method call, all maximal widths
        // will be correctly calculated and layout of some menu items
        // can be changed. For example, it can cause a shift of
        // the icon and text when user points a menu item by mouse.

        JMenuItem mi = (JMenuItem) c;
        MenuItemLayoutHelper lh = new MenuItemLayoutHelper(mi, checkIcon,
                arrowIcon, MenuItemLayoutHelper.createMaxRect(), defaultTextIconGap,
                acceleratorDelimiter, BasicGraphicsUtils.isLeftToRight(mi),
                mi.getFont(), acceleratorFont,
                MenuItemLayoutHelper.useCheckAndArrow(menuItem),
                getPropertyPrefix());

        Dimension result = new Dimension();

        // Calculate the result width
        result.width = lh.getLeadingGap();
        MenuItemLayoutHelper.addMaxWidth(lh.getCheckSize(),
                lh.getAfterCheckIconGap(), result);
        // Take into account mimimal text offset.
        if ((!lh.isTopLevelMenu())
                && (lh.getMinTextOffset() > 0)
                && (result.width < lh.getMinTextOffset())) {
            result.width = lh.getMinTextOffset();
        }
        MenuItemLayoutHelper.addMaxWidth(lh.getLabelSize(), lh.getGap(), result);
        MenuItemLayoutHelper.addMaxWidth(lh.getAccSize(), lh.getGap(), result);
        MenuItemLayoutHelper.addMaxWidth(lh.getArrowSize(), lh.getGap(), result);

        // Calculate the result height
        result.height = MenuItemLayoutHelper.max(lh.getCheckSize().getHeight(),
                lh.getLabelSize().getHeight(), lh.getAccSize().getHeight(),
                lh.getArrowSize().getHeight());

        // Take into account menu item insets
        Insets insets = lh.getMenuItem().getInsets();
        if(insets != null) {
            result.width += insets.left + insets.right;
            result.height += insets.top + insets.bottom;
        }

        // if the width is even, bump it up one. This is critical
        // for the focus dash line to draw properly
        if(result.width%2 == 0) {
            result.width++;
        }

        // if the height is even, bump it up one. This is critical
        // for the text to center properly
        if(result.height%2 == 0
                && Boolean.TRUE !=
                    UIManager.get(getPropertyPrefix() + ".evenHeight")) {
            result.height++;
        }

        return result;
    }

    /**
     * We draw the background in paintMenuItem()
     * so override update (which fills the background of opaque
     * components by default) to just call paint().
     *
     */
    public void update(Graphics g, JComponent c) {
        paint(g, c);
    }

    public void paint(Graphics g, JComponent c) {
        paintMenuItem(g, c, checkIcon, arrowIcon,
                      selectionBackground, selectionForeground,
                      defaultTextIconGap);
    }

    /**
     * Paints a menu item.
     *
     * @param g an instance of {@code Graphics}
     * @param c a component
     * @param checkIcon a check icon
     * @param arrowIcon an arrow icon
     * @param background a background color
     * @param foreground a foreground color
     * @param defaultTextIconGap a gap between a text and an icon
     */
    protected void paintMenuItem(Graphics g, JComponent c,
                                     Icon checkIcon, Icon arrowIcon,
                                     Color background, Color foreground,
                                     int defaultTextIconGap) {
        // Save original graphics font and color
        Font holdf = g.getFont();
        Color holdc = g.getColor();

        JMenuItem mi = (JMenuItem) c;
        g.setFont(mi.getFont());

        Rectangle viewRect = new Rectangle(0, 0, mi.getWidth(), mi.getHeight());
        applyInsets(viewRect, mi.getInsets());

        MenuItemLayoutHelper lh = new MenuItemLayoutHelper(mi, checkIcon,
                arrowIcon, viewRect, defaultTextIconGap, acceleratorDelimiter,
                BasicGraphicsUtils.isLeftToRight(mi), mi.getFont(),
                acceleratorFont, MenuItemLayoutHelper.useCheckAndArrow(menuItem),
                getPropertyPrefix());
        MenuItemLayoutHelper.LayoutResult lr = lh.layoutMenuItem();

        paintBackground(g, mi, background);
        paintCheckIcon(g, lh, lr, holdc, foreground);
        paintIcon(g, lh, lr, holdc);
        paintText(g, lh, lr);
        paintAccText(g, lh, lr);
        paintArrowIcon(g, lh, lr, foreground);

        // Restore original graphics font and color
        g.setColor(holdc);
        g.setFont(holdf);
    }

    private void paintIcon(Graphics g, MenuItemLayoutHelper lh,
                           MenuItemLayoutHelper.LayoutResult lr, Color holdc) {
        if (lh.getIcon() != null) {
            Icon icon;
            ButtonModel model = lh.getMenuItem().getModel();
            if (!model.isEnabled()) {
                icon = lh.getMenuItem().getDisabledIcon();
            } else if (model.isPressed() && model.isArmed()) {
                icon = lh.getMenuItem().getPressedIcon();
                if (icon == null) {
                    // Use default icon
                    icon = lh.getMenuItem().getIcon();
                }
            } else {
                icon = lh.getMenuItem().getIcon();
            }

            if (icon != null) {
                icon.paintIcon(lh.getMenuItem(), g, lr.getIconRect().x,
                        lr.getIconRect().y);
                g.setColor(holdc);
            }
        }
    }

    private void paintCheckIcon(Graphics g, MenuItemLayoutHelper lh,
                                MenuItemLayoutHelper.LayoutResult lr,
                                Color holdc, Color foreground) {
        if (lh.getCheckIcon() != null) {
            ButtonModel model = lh.getMenuItem().getModel();
            if (model.isArmed() || (lh.getMenuItem() instanceof JMenu
                    && model.isSelected())) {
                g.setColor(foreground);
            } else {
                g.setColor(holdc);
            }
            if (lh.useCheckAndArrow()) {
                lh.getCheckIcon().paintIcon(lh.getMenuItem(), g,
                        lr.getCheckRect().x, lr.getCheckRect().y);
            }
            g.setColor(holdc);
        }
    }

    private void paintAccText(Graphics g, MenuItemLayoutHelper lh,
                              MenuItemLayoutHelper.LayoutResult lr) {
        if (!lh.getAccText().isEmpty()) {
            ButtonModel model = lh.getMenuItem().getModel();
            g.setFont(lh.getAccFontMetrics().getFont());
            if (!model.isEnabled()) {
                // *** paint the accText disabled
                if (disabledForeground != null) {
                    g.setColor(disabledForeground);
                    SwingUtilities2.drawString(lh.getMenuItem(), g,
                        lh.getAccText(), lr.getAccRect().x,
                        lr.getAccRect().y + lh.getAccFontMetrics().getAscent());
                } else {
                    g.setColor(lh.getMenuItem().getBackground().brighter());
                    SwingUtilities2.drawString(lh.getMenuItem(), g,
                        lh.getAccText(), lr.getAccRect().x,
                        lr.getAccRect().y + lh.getAccFontMetrics().getAscent());
                    g.setColor(lh.getMenuItem().getBackground().darker());
                    SwingUtilities2.drawString(lh.getMenuItem(), g,
                        lh.getAccText(), lr.getAccRect().x - 1,
                        lr.getAccRect().y + lh.getFontMetrics().getAscent() - 1);
                }
            } else {
                // *** paint the accText normally
                if (model.isArmed()
                        || (lh.getMenuItem() instanceof JMenu
                        && model.isSelected())) {
                    g.setColor(acceleratorSelectionForeground);
                } else {
                    g.setColor(acceleratorForeground);
                }
                SwingUtilities2.drawString(lh.getMenuItem(), g, lh.getAccText(),
                        lr.getAccRect().x, lr.getAccRect().y +
                        lh.getAccFontMetrics().getAscent());
            }
        }
    }

    private void paintText(Graphics g, MenuItemLayoutHelper lh,
                           MenuItemLayoutHelper.LayoutResult lr) {
        if (!lh.getText().isEmpty()) {
            if (lh.getHtmlView() != null) {
                // Text is HTML
                lh.getHtmlView().paint(g, lr.getTextRect());
            } else {
                // Text isn't HTML
                paintText(g, lh.getMenuItem(), lr.getTextRect(), lh.getText());
            }
        }
    }

    private void paintArrowIcon(Graphics g, MenuItemLayoutHelper lh,
                                MenuItemLayoutHelper.LayoutResult lr,
                                Color foreground) {
        if (lh.getArrowIcon() != null) {
            ButtonModel model = lh.getMenuItem().getModel();
            if (model.isArmed() || (lh.getMenuItem() instanceof JMenu
                                && model.isSelected())) {
                g.setColor(foreground);
            }
            if (lh.useCheckAndArrow()) {
                lh.getArrowIcon().paintIcon(lh.getMenuItem(), g,
                        lr.getArrowRect().x, lr.getArrowRect().y);
            }
        }
    }

    private void applyInsets(Rectangle rect, Insets insets) {
        if(insets != null) {
            rect.x += insets.left;
            rect.y += insets.top;
            rect.width -= (insets.right + rect.x);
            rect.height -= (insets.bottom + rect.y);
        }
    }

    /**
     * Draws the background of the menu item.
     *
     * @param g the paint graphics
     * @param menuItem menu item to be painted
     * @param bgColor selection background color
     * @since 1.4
     */
    protected void paintBackground(Graphics g, JMenuItem menuItem, Color bgColor) {
        ButtonModel model = menuItem.getModel();
        Color oldColor = g.getColor();
        int menuWidth = menuItem.getWidth();
        int menuHeight = menuItem.getHeight();

        if(menuItem.isOpaque()) {
            if (model.isArmed()|| (menuItem instanceof JMenu && model.isSelected())) {
                g.setColor(bgColor);
                g.fillRect(0,0, menuWidth, menuHeight);
            } else {
                g.setColor(menuItem.getBackground());
                g.fillRect(0,0, menuWidth, menuHeight);
            }
            g.setColor(oldColor);
        }
        else if (model.isArmed() || (menuItem instanceof JMenu &&
                                     model.isSelected())) {
            g.setColor(bgColor);
            g.fillRect(0,0, menuWidth, menuHeight);
            g.setColor(oldColor);
        }
    }

    /**
     * Renders the text of the current menu item.
     *
     * @param g graphics context
     * @param menuItem menu item to render
     * @param textRect bounding rectangle for rendering the text
     * @param text string to render
     * @since 1.4
     */
    protected void paintText(Graphics g, JMenuItem menuItem, Rectangle textRect, String text) {
        ButtonModel model = menuItem.getModel();
        FontMetrics fm = SwingUtilities2.getFontMetrics(menuItem, g);
        int mnemIndex = menuItem.getDisplayedMnemonicIndex();

        if(!model.isEnabled()) {
            // *** paint the text disabled
            if ( UIManager.get("MenuItem.disabledForeground") instanceof Color ) {
                g.setColor( UIManager.getColor("MenuItem.disabledForeground") );
                SwingUtilities2.drawStringUnderlineCharAt(menuItem, g,text,
                          mnemIndex, textRect.x,  textRect.y + fm.getAscent());
            } else {
                g.setColor(menuItem.getBackground().brighter());
                SwingUtilities2.drawStringUnderlineCharAt(menuItem, g, text,
                           mnemIndex, textRect.x, textRect.y + fm.getAscent());
                g.setColor(menuItem.getBackground().darker());
                SwingUtilities2.drawStringUnderlineCharAt(menuItem, g,text,
                           mnemIndex,  textRect.x - 1, textRect.y +
                           fm.getAscent() - 1);
            }
        } else {
            // *** paint the text normally
            if (model.isArmed()|| (menuItem instanceof JMenu && model.isSelected())) {
                g.setColor(selectionForeground); // Uses protected field.
            }
            SwingUtilities2.drawStringUnderlineCharAt(menuItem, g,text,
                           mnemIndex, textRect.x, textRect.y + fm.getAscent());
        }
    }

    /**
     * Returns a menu element path.
     *
     * @return a menu element path
     */
    public MenuElement[] getPath() {
        MenuSelectionManager m = MenuSelectionManager.defaultManager();
        MenuElement[] oldPath = m.getSelectedPath();
        MenuElement[] newPath;
        int i = oldPath.length;
        if (i == 0)
            return new MenuElement[0];
        Component parent = menuItem.getParent();
        if (oldPath[i-1].getComponent() == parent) {
            // The parent popup menu is the last so far
            newPath = new MenuElement[i+1];
            System.arraycopy(oldPath, 0, newPath, 0, i);
            newPath[i] = menuItem;
        } else {
            // A sibling menuitem is the current selection
            //
            //  This probably needs to handle 'exit submenu into
            // a menu item.  Search backwards along the current
            // selection until you find the parent popup menu,
            // then copy up to that and add yourself...
            int j;
            for (j = oldPath.length-1; j >= 0; j--) {
                if (oldPath[j].getComponent() == parent)
                    break;
            }
            newPath = new MenuElement[j+2];
            System.arraycopy(oldPath, 0, newPath, 0, j+1);
            newPath[j+1] = menuItem;
            /*
            System.out.println("Sibling condition -- ");
            System.out.println("Old array : ");
            printMenuElementArray(oldPath, false);
            System.out.println("New array : ");
            printMenuElementArray(newPath, false);
            */
        }
        return newPath;
    }

    void printMenuElementArray(MenuElement[] path, boolean dumpStack) {
        System.out.println("Path is(");
        int i, j;
        for(i=0,j=path.length; i<j ;i++){
            for (int k=0; k<=i; k++)
                System.out.print("  ");
            MenuElement me = path[i];
            if(me instanceof JMenuItem)
                System.out.println(((JMenuItem)me).getText() + ", ");
            else if (me == null)
                System.out.println("NULL , ");
            else
                System.out.println("" + me + ", ");
        }
        System.out.println(")");

        if (dumpStack == true)
            Thread.dumpStack();
    }
    /**
     * Mouse input handler.
     * This class exists only for backward compatibility.
     * All its functionality has been moved into Handler.
     * @deprecated
     */
    @Deprecated(since = "17")
    protected class MouseInputHandler implements MouseInputListener {
        // NOTE: This class exists only for backward compatibility. All
        // its functionality has been moved into Handler. If you need to add
        // new functionality add it to the Handler, but make sure this
        // class calls into the Handler.

        /**
         * Constructs a {@code MouseInputHandler}.
         */
        protected MouseInputHandler() {}

        /** {@inheritDoc} */
        public void mouseClicked(MouseEvent e) {
            getHandler().mouseClicked(e);
        }
        /** {@inheritDoc} */
        public void mousePressed(MouseEvent e) {
            getHandler().mousePressed(e);
        }
        /** {@inheritDoc} */
        public void mouseReleased(MouseEvent e) {
            getHandler().mouseReleased(e);
        }
        /** {@inheritDoc} */
        public void mouseEntered(MouseEvent e) {
            getHandler().mouseEntered(e);
        }
        /** {@inheritDoc} */
        public void mouseExited(MouseEvent e) {
            getHandler().mouseExited(e);
        }
        /** {@inheritDoc} */
        public void mouseDragged(MouseEvent e) {
            getHandler().mouseDragged(e);
        }
        /** {@inheritDoc} */
        public void mouseMoved(MouseEvent e) {
            getHandler().mouseMoved(e);
        }
    }


    private static class Actions extends UIAction {
        private static final String CLICK = "doClick";

        Actions(String key) {
            super(key);
        }

        public void actionPerformed(ActionEvent e) {
            JMenuItem mi = (JMenuItem)e.getSource();
            MenuSelectionManager.defaultManager().clearSelectedPath();
            mi.doClick();
        }
    }

    boolean doNotCloseOnMouseClick() {
        if (menuItem instanceof JCheckBoxMenuItem) {
            String property = "CheckBoxMenuItem.doNotCloseOnMouseClick";
            return SwingUtilities2.getBoolean(menuItem, property);
        } else if (menuItem instanceof JRadioButtonMenuItem) {
            String property = "RadioButtonMenuItem.doNotCloseOnMouseClick";
            return SwingUtilities2.getBoolean(menuItem, property);
        }
        return false;
    }

    /**
     * Call this method when a menu item is to be activated.
     * This method handles some of the details of menu item activation
     * such as clearing the selected path and messaging the
     * JMenuItem's doClick() method.
     *
     * @param msm  A MenuSelectionManager. The visual feedback and
     *             internal bookkeeping tasks are delegated to
     *             this MenuSelectionManager. If <code>null</code> is
     *             passed as this argument, the
     *             <code>MenuSelectionManager.defaultManager</code> is
     *             used.
     * @see MenuSelectionManager
     * @see JMenuItem#doClick(int)
     * @since 1.4
     */
    protected void doClick(MenuSelectionManager msm) {
        // Auditory cue
        if (! isInternalFrameSystemMenu()) {
            BasicLookAndFeel.playSound(menuItem, getPropertyPrefix() +
                                       ".commandSound");
        }
        if (!doNotCloseOnMouseClick()) {
            // Visual feedback
            if (msm == null) {
                msm = MenuSelectionManager.defaultManager();
            }

            msm.clearSelectedPath();
        }
        menuItem.doClick(0);
    }

    /**
     * This is to see if the menu item in question is part of the
     * system menu on an internal frame.
     * The Strings that are being checked can be found in
     * MetalInternalFrameTitlePaneUI.java,
     * WindowsInternalFrameTitlePaneUI.java, and
     * MotifInternalFrameTitlePaneUI.java.
     *
     * @since 1.4
     */
    private boolean isInternalFrameSystemMenu() {
        String actionCommand = menuItem.getActionCommand();
        if ((actionCommand == "Close") ||
            (actionCommand == "Minimize") ||
            (actionCommand == "Restore") ||
            (actionCommand == "Maximize")) {
          return true;
        } else {
          return false;
        }
    }


    // BasicMenuUI subclasses this.
    class Handler implements MenuDragMouseListener,
                          MouseInputListener, PropertyChangeListener {
        //
        // MouseInputListener
        //
        public void mouseClicked(MouseEvent e) {}
        public void mousePressed(MouseEvent e) {
        }
        public void mouseReleased(MouseEvent e) {
            if (!menuItem.isEnabled()) {
                return;
            }
            MenuSelectionManager manager =
                MenuSelectionManager.defaultManager();
            Point p = e.getPoint();
            if(p.x >= 0 && p.x < menuItem.getWidth() &&
               p.y >= 0 && p.y < menuItem.getHeight()) {
                doClick(manager);
            } else {
                manager.processMouseEvent(e);
            }
        }
        @SuppressWarnings("deprecation")
        public void mouseEntered(MouseEvent e) {
            MenuSelectionManager manager = MenuSelectionManager.defaultManager();
            int modifiers = e.getModifiers();
            // 4188027: drag enter/exit added in JDK 1.1.7A, JDK1.2
            if ((modifiers & (InputEvent.BUTTON1_MASK |
                              InputEvent.BUTTON2_MASK | InputEvent.BUTTON3_MASK)) !=0 ) {
                MenuSelectionManager.defaultManager().processMouseEvent(e);
            } else {
            manager.setSelectedPath(getPath());
             }
        }
        @SuppressWarnings("deprecation")
        public void mouseExited(MouseEvent e) {
            MenuSelectionManager manager = MenuSelectionManager.defaultManager();

            int modifiers = e.getModifiers();
            // 4188027: drag enter/exit added in JDK 1.1.7A, JDK1.2
            if ((modifiers & (InputEvent.BUTTON1_MASK |
                              InputEvent.BUTTON2_MASK | InputEvent.BUTTON3_MASK)) !=0 ) {
                MenuSelectionManager.defaultManager().processMouseEvent(e);
            } else {

                MenuElement[] path = manager.getSelectedPath();
                if (path.length > 1 && path[path.length-1] == menuItem) {
                    MenuElement[] newPath = new MenuElement[path.length-1];
                    int i,c;
                    for(i=0,c=path.length-1;i<c;i++)
                        newPath[i] = path[i];
                    manager.setSelectedPath(newPath);
                }
                }
        }

        public void mouseDragged(MouseEvent e) {
            MenuSelectionManager.defaultManager().processMouseEvent(e);
        }
        public void mouseMoved(MouseEvent e) {
        }

        //
        // MenuDragListener
        //
        public void menuDragMouseEntered(MenuDragMouseEvent e) {
            MenuSelectionManager manager = e.getMenuSelectionManager();
            MenuElement[] path = e.getPath();
            manager.setSelectedPath(path);
        }
        public void menuDragMouseDragged(MenuDragMouseEvent e) {
            MenuSelectionManager manager = e.getMenuSelectionManager();
            MenuElement[] path = e.getPath();
            manager.setSelectedPath(path);
        }
        public void menuDragMouseExited(MenuDragMouseEvent e) {}
        public void menuDragMouseReleased(MenuDragMouseEvent e) {
            if (!menuItem.isEnabled()) {
                return;
            }
            MenuSelectionManager manager = e.getMenuSelectionManager();
            MenuElement[] path = e.getPath();
            Point p = e.getPoint();
            if (p.x >= 0 && p.x < menuItem.getWidth() &&
                    p.y >= 0 && p.y < menuItem.getHeight()) {
                doClick(manager);
            } else {
                manager.clearSelectedPath();
            }
        }


        //
        // PropertyChangeListener
        //
        public void propertyChange(PropertyChangeEvent e) {
            String name = e.getPropertyName();

            if (name == "labelFor" || name == "displayedMnemonic" ||
                name == "accelerator") {
                updateAcceleratorBinding();
            } else if (name == "text" || "font" == name || "foreground" == name
                    || SwingUtilities2.isScaleChanged(e)) {
                // remove the old html view client property if one
                // existed, and install a new one if the text installed
                // into the JLabel is html source.
                JMenuItem lbl = ((JMenuItem) e.getSource());
                String text = lbl.getText();
                BasicHTML.updateRenderer(lbl, text);
            } else if (name  == "iconTextGap") {
                defaultTextIconGap = ((Number)e.getNewValue()).intValue();
            }
        }
    }
}
