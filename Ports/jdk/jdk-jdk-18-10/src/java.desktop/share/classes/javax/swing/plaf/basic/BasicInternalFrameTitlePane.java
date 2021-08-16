/*
 * Copyright (c) 1997, 2014, Oracle and/or its affiliates. All rights reserved.
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

import sun.swing.SwingUtilities2;
import java.awt.*;
import java.awt.event.*;
import javax.accessibility.AccessibleContext;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.event.InternalFrameEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyVetoException;
import static java.awt.RenderingHints.KEY_TEXT_ANTIALIASING;
import static java.awt.RenderingHints.KEY_TEXT_LCD_CONTRAST;

import sun.swing.DefaultLookup;

/**
 * The class that manages a basic title bar
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
 * @author Steve Wilson
 */
@SuppressWarnings("serial") // Same-version serialization only
public class BasicInternalFrameTitlePane extends JComponent
{
    /**
     * The instance of {@code JMenuBar}.
     */
    protected JMenuBar menuBar;
    /**
     * The iconify button.
     */
    protected JButton iconButton;
    /**
     * The maximize button.
     */
    protected JButton maxButton;
    /**
     * The close button.
     */
    protected JButton closeButton;

    /**
     * The instance of {@code JMenu}.
     */
    protected JMenu windowMenu;
    /**
     * The instance of {@code JInternalFrame}.
     */
    protected JInternalFrame frame;

    /**
     * The color of a selected title.
     */
    protected Color selectedTitleColor;
    /**
     * The color of a selected text.
     */
    protected Color selectedTextColor;
    /**
     * The color of a not selected title.
     */
    protected Color notSelectedTitleColor;
    /**
     * The color of a not selected text.
     */
    protected Color notSelectedTextColor;

    /**
     * The maximize icon.
     */
    protected Icon maxIcon;
    /**
     * The minimize icon.
     */
    protected Icon minIcon;
    /**
     * The iconify icon.
     */
    protected Icon iconIcon;
    /**
     * The close icon.
     */
    protected Icon closeIcon;

    /**
     * The instance of a {@code PropertyChangeListener}.
     */
    protected PropertyChangeListener propertyChangeListener;

    /**
     * The instance of a {@code CloseAction}.
     */
    protected Action closeAction;
    /**
     * The instance of a {@code MaximizeAction}.
     */
    protected Action maximizeAction;
    /**
     * The instance of an {@code IconifyAction}.
     */
    protected Action iconifyAction;
    /**
     * The instance of a {@code RestoreAction}.
     */
    protected Action restoreAction;
    /**
     * The instance of a {@code MoveAction}.
     */
    protected Action moveAction;
    /**
     * The instance of a {@code SizeAction}.
     */
    protected Action sizeAction;

    // These constants are not used in JDK code
    /**
     * The close button text property.
     */
    protected static final String CLOSE_CMD =
        UIManager.getString("InternalFrameTitlePane.closeButtonText");
    /**
     * The minimize button text property.
     */
    protected static final String ICONIFY_CMD =
        UIManager.getString("InternalFrameTitlePane.minimizeButtonText");
    /**
     * The restore button text property.
     */
    protected static final String RESTORE_CMD =
        UIManager.getString("InternalFrameTitlePane.restoreButtonText");
    /**
     * The maximize button text property.
     */
    protected static final String MAXIMIZE_CMD =
        UIManager.getString("InternalFrameTitlePane.maximizeButtonText");
    /**
     * The move button text property.
     */
    protected static final String MOVE_CMD =
        UIManager.getString("InternalFrameTitlePane.moveButtonText");
    /**
     * The size button text property.
     */
    protected static final String SIZE_CMD =
        UIManager.getString("InternalFrameTitlePane.sizeButtonText");

    private String closeButtonToolTip;
    private String iconButtonToolTip;
    private String restoreButtonToolTip;
    private String maxButtonToolTip;
    private Handler handler;

    /**
     * Constructs a new instance of {@code BasicInternalFrameTitlePane}.
     *
     * @param f an instance of {@code JInternalFrame}
     */
    public BasicInternalFrameTitlePane(JInternalFrame f) {
        frame = f;
        installTitlePane();
    }

    /**
     * Installs the title pane.
     */
    protected void installTitlePane() {
        installDefaults();
        installListeners();

        createActions();
        enableActions();
        createActionMap();

        setLayout(createLayout());

        assembleSystemMenu();
        createButtons();
        addSubComponents();

        updateProperties();
    }

    private void updateProperties() {
        putClientProperty(KEY_TEXT_ANTIALIASING,
                frame.getClientProperty(KEY_TEXT_ANTIALIASING));
        putClientProperty(KEY_TEXT_LCD_CONTRAST,
                frame.getClientProperty(KEY_TEXT_LCD_CONTRAST));
    }

    /**
     * Adds subcomponents.
     */
    protected void addSubComponents() {
        add(menuBar);
        add(iconButton);
        add(maxButton);
        add(closeButton);
    }

    /**
     * Creates actions.
     */
    protected void createActions() {
        maximizeAction = new MaximizeAction();
        iconifyAction = new IconifyAction();
        closeAction = new CloseAction();
        restoreAction = new RestoreAction();
        moveAction = new MoveAction();
        sizeAction = new SizeAction();
    }

    ActionMap createActionMap() {
        ActionMap map = new ActionMapUIResource();
        map.put("showSystemMenu", new ShowSystemMenuAction(true));
        map.put("hideSystemMenu", new ShowSystemMenuAction(false));
        return map;
    }

    /**
     * Registers listeners.
     */
    protected void installListeners() {
        if( propertyChangeListener == null ) {
            propertyChangeListener = createPropertyChangeListener();
        }
        frame.addPropertyChangeListener(propertyChangeListener);
    }

    /**
     * Unregisters listeners.
     */
    protected void uninstallListeners() {
        frame.removePropertyChangeListener(propertyChangeListener);
        handler = null;
    }

    /**
     * Installs default properties.
     */
    protected void installDefaults() {
        maxIcon = UIManager.getIcon("InternalFrame.maximizeIcon");
        minIcon = UIManager.getIcon("InternalFrame.minimizeIcon");
        iconIcon = UIManager.getIcon("InternalFrame.iconifyIcon");
        closeIcon = UIManager.getIcon("InternalFrame.closeIcon");

        selectedTitleColor = UIManager.getColor("InternalFrame.activeTitleBackground");
        selectedTextColor = UIManager.getColor("InternalFrame.activeTitleForeground");
        notSelectedTitleColor = UIManager.getColor("InternalFrame.inactiveTitleBackground");
        notSelectedTextColor = UIManager.getColor("InternalFrame.inactiveTitleForeground");
        setFont(UIManager.getFont("InternalFrame.titleFont"));
        closeButtonToolTip =
                UIManager.getString("InternalFrame.closeButtonToolTip");
        iconButtonToolTip =
                UIManager.getString("InternalFrame.iconButtonToolTip");
        restoreButtonToolTip =
                UIManager.getString("InternalFrame.restoreButtonToolTip");
        maxButtonToolTip =
                UIManager.getString("InternalFrame.maxButtonToolTip");
    }

    /**
     * Uninstalls default properties.
     */
    protected void uninstallDefaults() {
    }

    /**
     * Creates buttons.
     */
    protected void createButtons() {
        iconButton = new NoFocusButton(
                     "InternalFrameTitlePane.iconifyButtonAccessibleName",
                     "InternalFrameTitlePane.iconifyButtonOpacity");
        iconButton.addActionListener(iconifyAction);
        if (iconButtonToolTip != null && iconButtonToolTip.length() != 0) {
            iconButton.setToolTipText(iconButtonToolTip);
        }

        maxButton = new NoFocusButton(
                        "InternalFrameTitlePane.maximizeButtonAccessibleName",
                        "InternalFrameTitlePane.maximizeButtonOpacity");
        maxButton.addActionListener(maximizeAction);

        closeButton = new NoFocusButton(
                      "InternalFrameTitlePane.closeButtonAccessibleName",
                      "InternalFrameTitlePane.closeButtonOpacity");
        closeButton.addActionListener(closeAction);
        if (closeButtonToolTip != null && closeButtonToolTip.length() != 0) {
            closeButton.setToolTipText(closeButtonToolTip);
        }

        setButtonIcons();
    }

    /**
     * Sets the button icons.
     */
    protected void setButtonIcons() {
        if(frame.isIcon()) {
            if (minIcon != null) {
                iconButton.setIcon(minIcon);
            }
            if (restoreButtonToolTip != null &&
                    restoreButtonToolTip.length() != 0) {
                iconButton.setToolTipText(restoreButtonToolTip);
            }
            if (maxIcon != null) {
                maxButton.setIcon(maxIcon);
            }
            if (maxButtonToolTip != null && maxButtonToolTip.length() != 0) {
                maxButton.setToolTipText(maxButtonToolTip);
            }
        } else if (frame.isMaximum()) {
            if (iconIcon != null) {
                iconButton.setIcon(iconIcon);
            }
            if (iconButtonToolTip != null && iconButtonToolTip.length() != 0) {
                iconButton.setToolTipText(iconButtonToolTip);
            }
            if (minIcon != null) {
                maxButton.setIcon(minIcon);
            }
            if (restoreButtonToolTip != null &&
                    restoreButtonToolTip.length() != 0) {
                maxButton.setToolTipText(restoreButtonToolTip);
            }
        } else {
            if (iconIcon != null) {
                iconButton.setIcon(iconIcon);
            }
            if (iconButtonToolTip != null && iconButtonToolTip.length() != 0) {
                iconButton.setToolTipText(iconButtonToolTip);
            }
            if (maxIcon != null) {
                maxButton.setIcon(maxIcon);
            }
            if (maxButtonToolTip != null && maxButtonToolTip.length() != 0) {
                maxButton.setToolTipText(maxButtonToolTip);
            }
        }
        if (closeIcon != null) {
            closeButton.setIcon(closeIcon);
        }
    }

    /**
     * Assembles system menu.
     */
    protected void assembleSystemMenu() {
        menuBar = createSystemMenuBar();
        windowMenu = createSystemMenu();
        menuBar.add(windowMenu);
        addSystemMenuItems(windowMenu);
        enableActions();
    }

    /**
     * Adds system menu items to {@code systemMenu}.
     *
     * @param systemMenu an instance of {@code JMenu}
     */
    protected void addSystemMenuItems(JMenu systemMenu) {
        JMenuItem mi = systemMenu.add(restoreAction);
        mi.setMnemonic(getButtonMnemonic("restore"));
        mi = systemMenu.add(moveAction);
        mi.setMnemonic(getButtonMnemonic("move"));
        mi = systemMenu.add(sizeAction);
        mi.setMnemonic(getButtonMnemonic("size"));
        mi = systemMenu.add(iconifyAction);
        mi.setMnemonic(getButtonMnemonic("minimize"));
        mi = systemMenu.add(maximizeAction);
        mi.setMnemonic(getButtonMnemonic("maximize"));
        systemMenu.add(new JSeparator());
        mi = systemMenu.add(closeAction);
        mi.setMnemonic(getButtonMnemonic("close"));
    }

    private static int getButtonMnemonic(String button) {
        try {
            return Integer.parseInt(UIManager.getString(
                    "InternalFrameTitlePane." + button + "Button.mnemonic"));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Returns a new instance of {@code JMenu}.
     *
     * @return a new instance of {@code JMenu}
     */
    protected JMenu createSystemMenu() {
        return new JMenu("    ");
    }

    /**
     * Returns a new instance of {@code JMenuBar}.
     *
     * @return a new instance of {@code JMenuBar}
     */
    protected JMenuBar createSystemMenuBar() {
        menuBar = new SystemMenuBar();
        menuBar.setBorderPainted(false);
        return menuBar;
    }

    /**
     * Shows system menu.
     */
    protected void showSystemMenu(){
        //      windowMenu.setPopupMenuVisible(true);
      //      windowMenu.setVisible(true);
      windowMenu.doClick();
    }

    public void paintComponent(Graphics g)  {
        paintTitleBackground(g);

        if(frame.getTitle() != null) {
            boolean isSelected = frame.isSelected();
            Font f = g.getFont();
            g.setFont(getFont());
            if(isSelected)
                g.setColor(selectedTextColor);
            else
                g.setColor(notSelectedTextColor);

            // Center text vertically.
            FontMetrics fm = SwingUtilities2.getFontMetrics(frame, g);
            int baseline = (getHeight() + fm.getAscent() - fm.getLeading() -
                    fm.getDescent()) / 2;

            int titleX;
            Rectangle r = new Rectangle(0, 0, 0, 0);
            if (frame.isIconifiable())  r = iconButton.getBounds();
            else if (frame.isMaximizable())  r = maxButton.getBounds();
            else if (frame.isClosable())  r = closeButton.getBounds();
            int titleW;

            String title = frame.getTitle();
            if( BasicGraphicsUtils.isLeftToRight(frame) ) {
              if (r.x == 0)  r.x = frame.getWidth()-frame.getInsets().right;
              titleX = menuBar.getX() + menuBar.getWidth() + 2;
              titleW = r.x - titleX - 3;
              title = getTitle(frame.getTitle(), fm, titleW);
            } else {
                titleX = menuBar.getX() - 2
                         - SwingUtilities2.stringWidth(frame,fm,title);
            }

            SwingUtilities2.drawString(frame, g, title, titleX, baseline);
            g.setFont(f);
        }
    }

   /**
    * Invoked from paintComponent.
    * Paints the background of the titlepane.  All text and icons will
    * then be rendered on top of this background.
    * @param g the graphics to use to render the background
    * @since 1.4
    */
    protected void paintTitleBackground(Graphics g) {
        boolean isSelected = frame.isSelected();

        if(isSelected)
            g.setColor(selectedTitleColor);
        else
            g.setColor(notSelectedTitleColor);
        g.fillRect(0, 0, getWidth(), getHeight());
    }

    /**
     * Returns the title.
     *
     * @param text a text
     * @param fm an instance of {@code FontMetrics}
     * @param availTextWidth an available text width
     * @return the title.
     */
    protected String getTitle(String text, FontMetrics fm, int availTextWidth) {
        return SwingUtilities2.clipStringIfNecessary(
                           frame, fm, text, availTextWidth);
    }

    /**
     * Post a WINDOW_CLOSING-like event to the frame, so that it can
     * be treated like a regular {@code Frame}.
     *
     * @param frame an instance of {@code JInternalFrame}
     */
    protected void postClosingEvent(JInternalFrame frame) {
        InternalFrameEvent e = new InternalFrameEvent(
            frame, InternalFrameEvent.INTERNAL_FRAME_CLOSING);
        // Try posting event, unless there's a SecurityManager.
        try {
            Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(e);
        } catch (SecurityException se) {
            frame.dispatchEvent(e);
        }
    }

    /**
     * Enables actions.
     */
    protected void enableActions() {
        restoreAction.setEnabled(frame.isMaximum() || frame.isIcon());
        maximizeAction.setEnabled(
            (frame.isMaximizable() && !frame.isMaximum() && !frame.isIcon()) ||
            (frame.isMaximizable() && frame.isIcon()));
        iconifyAction.setEnabled(frame.isIconifiable() && !frame.isIcon());
        closeAction.setEnabled(frame.isClosable());
        sizeAction.setEnabled(false);
        moveAction.setEnabled(false);
    }

    private Handler getHandler() {
        if (handler == null) {
            handler = new Handler();
        }
        return handler;
    }

    /**
     * Returns an instance of {@code PropertyChangeListener}.
     *
     * @return an instance of {@code PropertyChangeListener}
     */
    protected PropertyChangeListener createPropertyChangeListener() {
        return getHandler();
    }

    /**
     * Returns a layout manager.
     *
     * @return a layout manager
     */
    protected LayoutManager createLayout() {
        return getHandler();
    }


    private class Handler implements LayoutManager, PropertyChangeListener {
        //
        // PropertyChangeListener
        //
        public void propertyChange(PropertyChangeEvent evt) {
            String prop = evt.getPropertyName();

            if (prop == JInternalFrame.IS_SELECTED_PROPERTY) {
                repaint();
                return;
            }

            if (prop == JInternalFrame.IS_ICON_PROPERTY ||
                    prop == JInternalFrame.IS_MAXIMUM_PROPERTY) {
                setButtonIcons();
                enableActions();
                return;
            }

            if ("closable" == prop) {
                if (evt.getNewValue() == Boolean.TRUE) {
                    add(closeButton);
                } else {
                    remove(closeButton);
                }
            } else if ("maximizable" == prop) {
                if (evt.getNewValue() == Boolean.TRUE) {
                    add(maxButton);
                } else {
                    remove(maxButton);
                }
            } else if ("iconable" == prop) {
                if (evt.getNewValue() == Boolean.TRUE) {
                    add(iconButton);
                } else {
                    remove(iconButton);
                }
            }
            enableActions();

            revalidate();
            repaint();
        }


        //
        // LayoutManager
        //
        public void addLayoutComponent(String name, Component c) {}
        public void removeLayoutComponent(Component c) {}
        public Dimension preferredLayoutSize(Container c) {
            return minimumLayoutSize(c);
        }

        public Dimension minimumLayoutSize(Container c) {
            // Calculate width.
            int width = 22;

            if (frame.isClosable()) {
                width += 19;
            }
            if (frame.isMaximizable()) {
                width += 19;
            }
            if (frame.isIconifiable()) {
                width += 19;
            }

            FontMetrics fm = frame.getFontMetrics(getFont());
            String frameTitle = frame.getTitle();
            int title_w = frameTitle != null ? SwingUtilities2.stringWidth(
                               frame, fm, frameTitle) : 0;
            int title_length = frameTitle != null ? frameTitle.length() : 0;

            // Leave room for three characters in the title.
            if (title_length > 3) {
                int subtitle_w = SwingUtilities2.stringWidth(
                    frame, fm, frameTitle.substring(0, 3) + "...");
                width += (title_w < subtitle_w) ? title_w : subtitle_w;
            } else {
                width += title_w;
            }

            // Calculate height.
            Icon icon = frame.getFrameIcon();
            int fontHeight = fm.getHeight();
            fontHeight += 2;
            int iconHeight = 0;
            if (icon != null) {
                // SystemMenuBar forces the icon to be 16x16 or less.
                iconHeight = Math.min(icon.getIconHeight(), 16);
            }
            iconHeight += 2;

            int height = Math.max( fontHeight, iconHeight );

            Dimension dim = new Dimension(width, height);

            // Take into account the border insets if any.
            if (getBorder() != null) {
                Insets insets = getBorder().getBorderInsets(c);
                dim.height += insets.top + insets.bottom;
                dim.width += insets.left + insets.right;
            }
            return dim;
        }

        public void layoutContainer(Container c) {
            boolean leftToRight = BasicGraphicsUtils.isLeftToRight(frame);

            int w = getWidth();
            int h = getHeight();
            int x;

            int buttonHeight = closeButton.getIcon().getIconHeight();

            Icon icon = frame.getFrameIcon();
            int iconHeight = 0;
            if (icon != null) {
                iconHeight = icon.getIconHeight();
            }
            x = (leftToRight) ? 2 : w - 16 - 2;
            menuBar.setBounds(x, (h - iconHeight) / 2, 16, 16);

            x = (leftToRight) ? w - 16 - 2 : 2;

            if (frame.isClosable()) {
                closeButton.setBounds(x, (h - buttonHeight) / 2, 16, 14);
                x += (leftToRight) ? -(16 + 2) : 16 + 2;
            }

            if (frame.isMaximizable()) {
                maxButton.setBounds(x, (h - buttonHeight) / 2, 16, 14);
                x += (leftToRight) ? -(16 + 2) : 16 + 2;
            }

            if (frame.isIconifiable()) {
                iconButton.setBounds(x, (h - buttonHeight) / 2, 16, 14);
            }
        }
    }

    /**
     * This class should be treated as a &quot;protected&quot; inner class.
     * Instantiate it only within subclasses of <code>Foo</code>.
     */
    public class PropertyChangeHandler implements PropertyChangeListener {
        /**
         * Constructs a {@code PropertyChangeHandler}.
         */
        public PropertyChangeHandler() {}

        // NOTE: This class exists only for backward compatibility. All
        // its functionality has been moved into Handler. If you need to add
        // new functionality add it to the Handler, but make sure this
        // class calls into the Handler.
        public void propertyChange(PropertyChangeEvent evt) {
            getHandler().propertyChange(evt);
        }
    }

    /**
     * This class should be treated as a &quot;protected&quot; inner class.
     * Instantiate it only within subclasses of <code>Foo</code>.
     */
    public class TitlePaneLayout implements LayoutManager {
        // NOTE: This class exists only for backward compatibility. All
        // its functionality has been moved into Handler. If you need to add
        // new functionality add it to the Handler, but make sure this
        // class calls into the Handler.
        /**
         * Constructs a {@code TitlePaneLayout}.
         */
        public TitlePaneLayout() {}

        public void addLayoutComponent(String name, Component c) {
            getHandler().addLayoutComponent(name, c);
        }

        public void removeLayoutComponent(Component c) {
            getHandler().removeLayoutComponent(c);
        }

        public Dimension preferredLayoutSize(Container c)  {
            return getHandler().preferredLayoutSize(c);
        }

        public Dimension minimumLayoutSize(Container c) {
            return getHandler().minimumLayoutSize(c);
        }

        public void layoutContainer(Container c) {
            getHandler().layoutContainer(c);
        }
    }

    /**
     * This class should be treated as a &quot;protected&quot; inner class.
     * Instantiate it only within subclasses of <code>Foo</code>.
     */
    public class CloseAction extends AbstractAction {
        /**
         * Constructs a new instance of a {@code CloseAction}.
         */
        public CloseAction() {
            super(UIManager.getString(
                    "InternalFrameTitlePane.closeButtonText"));
        }

        public void actionPerformed(ActionEvent e) {
            if(frame.isClosable()) {
                frame.doDefaultCloseAction();
            }
        }
    } // end CloseAction

    /**
     * This class should be treated as a &quot;protected&quot; inner class.
     * Instantiate it only within subclasses of <code>Foo</code>.
     */
    public class MaximizeAction extends AbstractAction {
        /**
         * Constructs a new instance of a {@code MaximizeAction}.
         */
        public MaximizeAction() {
            super(UIManager.getString(
                    "InternalFrameTitlePane.maximizeButtonText"));
        }

        public void actionPerformed(ActionEvent evt) {
            if (frame.isMaximizable()) {
                if (frame.isMaximum() && frame.isIcon()) {
                    try {
                        frame.setIcon(false);
                    } catch (PropertyVetoException e) { }
                } else if (!frame.isMaximum()) {
                    try {
                        frame.setMaximum(true);
                    } catch (PropertyVetoException e) { }
                } else {
                    try {
                        frame.setMaximum(false);
                    } catch (PropertyVetoException e) { }
                }
            }
        }
    }

    /**
     * This class should be treated as a &quot;protected&quot; inner class.
     * Instantiate it only within subclasses of <code>Foo</code>.
     */
    public class IconifyAction extends AbstractAction {
        /**
         * Constructs a new instance of an {@code IconifyAction}.
         */
        public IconifyAction() {
            super(UIManager.getString(
                    "InternalFrameTitlePane.minimizeButtonText"));
        }

        public void actionPerformed(ActionEvent e) {
            if(frame.isIconifiable()) {
              if(!frame.isIcon()) {
                try { frame.setIcon(true); } catch (PropertyVetoException e1) { }
              } else{
                try { frame.setIcon(false); } catch (PropertyVetoException e1) { }
              }
            }
        }
    } // end IconifyAction

    /**
     * This class should be treated as a &quot;protected&quot; inner class.
     * Instantiate it only within subclasses of <code>Foo</code>.
     */
    public class RestoreAction extends AbstractAction {
        /**
         * Constructs a new instance of a {@code RestoreAction}.
         */
        public RestoreAction() {
            super(UIManager.getString(
                    "InternalFrameTitlePane.restoreButtonText"));
        }

        public void actionPerformed(ActionEvent evt) {
            if (frame.isMaximizable() && frame.isMaximum() && frame.isIcon()) {
                try {
                    frame.setIcon(false);
                } catch (PropertyVetoException e) { }
            } else if (frame.isMaximizable() && frame.isMaximum()) {
                try {
                    frame.setMaximum(false);
                } catch (PropertyVetoException e) { }
            } else if (frame.isIconifiable() && frame.isIcon()) {
                try {
                    frame.setIcon(false);
                } catch (PropertyVetoException e) { }
            }
        }
    }

    /**
     * This class should be treated as a &quot;protected&quot; inner class.
     * Instantiate it only within subclasses of <code>Foo</code>.
     */
    public class MoveAction extends AbstractAction {
        /**
         * Constructs a new instance of a {@code MoveAction}.
         */
        public MoveAction() {
            super(UIManager.getString(
                    "InternalFrameTitlePane.moveButtonText"));
        }

        public void actionPerformed(ActionEvent e) {
            // This action is currently undefined
        }
    } // end MoveAction

    /*
     * Handles showing and hiding the system menu.
     */
    private class ShowSystemMenuAction extends AbstractAction {
        private boolean show;   // whether to show the menu

        public ShowSystemMenuAction(boolean show) {
            this.show = show;
        }

        public void actionPerformed(ActionEvent e) {
            if (show) {
                windowMenu.doClick();
            } else {
                windowMenu.setVisible(false);
            }
        }
    }

    /**
     * This class should be treated as a &quot;protected&quot; inner class.
     * Instantiate it only within subclasses of <code>Foo</code>.
     */
    public class SizeAction extends AbstractAction {
        /**
         * Constructs a new instance of a {@code SizeAction}.
         */
        public SizeAction() {
            super(UIManager.getString(
                    "InternalFrameTitlePane.sizeButtonText"));
        }

        public void actionPerformed(ActionEvent e) {
            // This action is currently undefined
        }
    } // end SizeAction


    /**
     * This class should be treated as a &quot;protected&quot; inner class.
     * Instantiate it only within subclasses of <code>Foo</code>.
     */
    @SuppressWarnings("deprecation")
    public class SystemMenuBar extends JMenuBar {
        /**
         * Constructs a {@code SystemMenuBar}.
         */
        public SystemMenuBar() {}
        public boolean isFocusTraversable() { return false; }
        public void requestFocus() {}
        public void paint(Graphics g) {
            Icon icon = frame.getFrameIcon();
            if (icon == null) {
              icon = (Icon)DefaultLookup.get(frame, frame.getUI(),
                      "InternalFrame.icon");
            }
            if (icon != null) {
                // Resize to 16x16 if necessary.
                if (icon instanceof ImageIcon && (icon.getIconWidth() > 16 || icon.getIconHeight() > 16)) {
                    Image img = ((ImageIcon)icon).getImage();
                    ((ImageIcon)icon).setImage(img.getScaledInstance(16, 16, Image.SCALE_SMOOTH));
                }
                icon.paintIcon(this, g, 0, 0);
            }
        }

        public boolean isOpaque() {
            return true;
        }
    } // end SystemMenuBar


    private class NoFocusButton extends JButton {
        private String uiKey;
        public NoFocusButton(String uiKey, String opacityKey) {
            setFocusPainted(false);
            setMargin(new Insets(0,0,0,0));
            this.uiKey = uiKey;

            Object opacity = UIManager.get(opacityKey);
            if (opacity instanceof Boolean) {
                setOpaque(((Boolean)opacity).booleanValue());
            }
        }
        @SuppressWarnings("deprecation")
        public boolean isFocusTraversable() { return false; }
        public void requestFocus() {}
        public AccessibleContext getAccessibleContext() {
            AccessibleContext ac = super.getAccessibleContext();
            if (uiKey != null) {
                ac.setAccessibleName(UIManager.getString(uiKey));
                uiKey = null;
            }
            return ac;
        }
    }  // end NoFocusButton

}   // End Title Pane Class
