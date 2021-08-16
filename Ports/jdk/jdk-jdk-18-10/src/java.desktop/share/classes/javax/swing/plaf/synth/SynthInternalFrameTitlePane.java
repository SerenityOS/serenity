/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.plaf.synth;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane;

import sun.swing.SwingUtilities2;

/**
 * The class that manages a synth title bar
 *
 * @author David Kloba
 * @author Joshua Outwater
 * @author Steve Wilson
 */
@SuppressWarnings("serial") // Superclass is not serializable across versions
class SynthInternalFrameTitlePane extends BasicInternalFrameTitlePane
        implements SynthUI, PropertyChangeListener {

    protected JPopupMenu systemPopupMenu;
    protected JButton menuButton;

    private SynthStyle style;
    private int titleSpacing;
    private int buttonSpacing;
    // Alignment for the title, one of SwingConstants.(LEADING|TRAILING|CENTER)
    private int titleAlignment;

    public SynthInternalFrameTitlePane(JInternalFrame f) {
        super(f);
    }

    public String getUIClassID() {
        return "InternalFrameTitlePaneUI";
    }

    public SynthContext getContext(JComponent c) {
        return getContext(c, getComponentState(c));
    }

    public SynthContext getContext(JComponent c, int state) {
        return SynthContext.getContext(c, style, state);
    }

    private Region getRegion(JComponent c) {
        return SynthLookAndFeel.getRegion(c);
    }

    private int getComponentState(JComponent c) {
        if (frame != null) {
            if (frame.isSelected()) {
                return SELECTED;
            }
        }
        return SynthLookAndFeel.getComponentState(c);
    }

    protected void addSubComponents() {
        menuButton.setName("InternalFrameTitlePane.menuButton");
        iconButton.setName("InternalFrameTitlePane.iconifyButton");
        maxButton.setName("InternalFrameTitlePane.maximizeButton");
        closeButton.setName("InternalFrameTitlePane.closeButton");

        add(menuButton);
        add(iconButton);
        add(maxButton);
        add(closeButton);
    }

    protected void installListeners() {
        super.installListeners();
        frame.addPropertyChangeListener(this);
        addPropertyChangeListener(this);
    }

    protected void uninstallListeners() {
        frame.removePropertyChangeListener(this);
        removePropertyChangeListener(this);
        super.uninstallListeners();
    }

    private void updateStyle(JComponent c) {
        SynthContext context = getContext(this, ENABLED);
        SynthStyle oldStyle = style;
        style = SynthLookAndFeel.updateStyle(context, this);
        if (style != oldStyle) {
            maxIcon =
                style.getIcon(context,"InternalFrameTitlePane.maximizeIcon");
            minIcon =
                style.getIcon(context,"InternalFrameTitlePane.minimizeIcon");
            iconIcon =
                style.getIcon(context,"InternalFrameTitlePane.iconifyIcon");
            closeIcon =
                style.getIcon(context,"InternalFrameTitlePane.closeIcon");
            titleSpacing = style.getInt(context,
                              "InternalFrameTitlePane.titleSpacing", 2);
            buttonSpacing = style.getInt(context,
                              "InternalFrameTitlePane.buttonSpacing", 2);
            String alignString = (String)style.get(context,
                              "InternalFrameTitlePane.titleAlignment");
            titleAlignment = SwingConstants.LEADING;
            if (alignString != null) {
                alignString = alignString.toUpperCase();
                if (alignString.equals("TRAILING")) {
                    titleAlignment = SwingConstants.TRAILING;
                }
                else if (alignString.equals("CENTER")) {
                    titleAlignment = SwingConstants.CENTER;
                }
            }
        }
    }

    protected void installDefaults() {
        super.installDefaults();
        updateStyle(this);
    }

    protected void uninstallDefaults() {
        SynthContext context = getContext(this, ENABLED);
        style.uninstallDefaults(context);
        style = null;
        JInternalFrame.JDesktopIcon di = frame.getDesktopIcon();
        if(di != null && di.getComponentPopupMenu() == systemPopupMenu) {
            // Release link to systemMenu from the JInternalFrame
            di.setComponentPopupMenu(null);
        }
        super.uninstallDefaults();
    }

    /**
     * A subclass of {@code JPopupMenu} that implements {@code UIResource}.
     */
    @SuppressWarnings("serial") // Superclass is not serializable across versions
    private static class JPopupMenuUIResource extends JPopupMenu implements
        UIResource { }

    protected void assembleSystemMenu() {
        systemPopupMenu = new JPopupMenuUIResource();
        addSystemMenuItems(systemPopupMenu);
        enableActions();
        menuButton = createNoFocusButton();
        updateMenuIcon();
        menuButton.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                try {
                    frame.setSelected(true);
                } catch(PropertyVetoException pve) {
                }
                showSystemMenu();
            }
        });
        JPopupMenu p = frame.getComponentPopupMenu();
        if (p == null || p instanceof UIResource) {
            frame.setComponentPopupMenu(systemPopupMenu);
        }
        if (frame.getDesktopIcon() != null) {
            p = frame.getDesktopIcon().getComponentPopupMenu();
            if (p == null || p instanceof UIResource) {
                frame.getDesktopIcon().setComponentPopupMenu(systemPopupMenu);
            }
        }
        setInheritsPopupMenu(true);
    }

    protected void addSystemMenuItems(JPopupMenu menu) {
        JMenuItem mi = menu.add(restoreAction);
        mi.setMnemonic(getButtonMnemonic("restore"));
        mi = menu.add(moveAction);
        mi.setMnemonic(getButtonMnemonic("move"));
        mi = menu.add(sizeAction);
        mi.setMnemonic(getButtonMnemonic("size"));
        mi = menu.add(iconifyAction);
        mi.setMnemonic(getButtonMnemonic("minimize"));
        mi = menu.add(maximizeAction);
        mi.setMnemonic(getButtonMnemonic("maximize"));
        menu.add(new JSeparator());
        mi = menu.add(closeAction);
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

    protected void showSystemMenu() {
        Insets insets = frame.getInsets();
        if (!frame.isIcon()) {
            systemPopupMenu.show(frame, menuButton.getX(), getY() + getHeight());
        } else {
            systemPopupMenu.show(menuButton,
                getX() - insets.left - insets.right,
                getY() - systemPopupMenu.getPreferredSize().height -
                    insets.bottom - insets.top);
        }
    }

    // SynthInternalFrameTitlePane has no UI, we'll invoke paint on it.
    public void paintComponent(Graphics g) {
        SynthContext context = getContext(this);
        SynthLookAndFeel.update(context, g);
        context.getPainter().paintInternalFrameTitlePaneBackground(context,
                          g, 0, 0, getWidth(), getHeight());
        paint(context, g);
    }

    protected void paint(SynthContext context, Graphics g) {
        String title = frame.getTitle();

        if (title != null) {
            SynthStyle style = context.getStyle();

            g.setColor(style.getColor(context, ColorType.TEXT_FOREGROUND));
            g.setFont(style.getFont(context));

            // Center text vertically.
            FontMetrics fm = SwingUtilities2.getFontMetrics(frame, g);
            int baseline = (getHeight() + fm.getAscent() - fm.getLeading() -
                            fm.getDescent()) / 2;
            JButton lastButton = null;
            if (frame.isIconifiable()) {
                lastButton = iconButton;
            }
            else if (frame.isMaximizable()) {
                lastButton = maxButton;
            }
            else if (frame.isClosable()) {
                lastButton = closeButton;
            }
            int maxX;
            int minX;
            boolean ltr = SynthLookAndFeel.isLeftToRight(frame);
            int titleAlignment = this.titleAlignment;
            if (ltr) {
                if (lastButton != null) {
                    maxX = lastButton.getX() - titleSpacing;
                }
                else {
                    maxX = frame.getWidth() - frame.getInsets().right -
                           titleSpacing;
                }
                minX = menuButton.getX() + menuButton.getWidth() +
                       titleSpacing;
            }
            else {
                if (lastButton != null) {
                    minX = lastButton.getX() + lastButton.getWidth() +
                           titleSpacing;
                }
                else {
                    minX = frame.getInsets().left + titleSpacing;
                }
                maxX = menuButton.getX() - titleSpacing;
                if (titleAlignment == SwingConstants.LEADING) {
                    titleAlignment = SwingConstants.TRAILING;
                }
                else if (titleAlignment == SwingConstants.TRAILING) {
                    titleAlignment = SwingConstants.LEADING;
                }
            }
            String clippedTitle = getTitle(title, fm, maxX - minX);
            if (clippedTitle == title) {
                // String fit, align as necessary.
                if (titleAlignment == SwingConstants.TRAILING) {
                    minX = maxX - style.getGraphicsUtils(context).
                        computeStringWidth(context, g.getFont(), fm, title);
                }
                else if (titleAlignment == SwingConstants.CENTER) {
                    int width = style.getGraphicsUtils(context).
                           computeStringWidth(context, g.getFont(), fm, title);
                    minX = Math.max(minX, (getWidth() - width) / 2);
                    minX = Math.min(maxX - width, minX);
                }
            }
            style.getGraphicsUtils(context).paintText(
                context, g, clippedTitle, minX, baseline - fm.getAscent(), -1);
        }
    }

    public void paintBorder(SynthContext context, Graphics g, int x,
                            int y, int w, int h) {
        context.getPainter().paintInternalFrameTitlePaneBorder(context,
                                                            g, x, y, w, h);
    }

    protected LayoutManager createLayout() {
        SynthContext context = getContext(this);
        LayoutManager lm =
            (LayoutManager)style.get(context, "InternalFrameTitlePane.titlePaneLayout");
        return (lm != null) ? lm : new SynthTitlePaneLayout();
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() == this) {
            if (SynthLookAndFeel.shouldUpdateStyle(evt)) {
                updateStyle(this);
            }
        }
        else {
            // Changes for the internal frame
            if (evt.getPropertyName() == JInternalFrame.FRAME_ICON_PROPERTY) {
                updateMenuIcon();
            }
        }
    }

    /**
     * Resets the menuButton icon to match that of the frame.
     */
    private void updateMenuIcon() {
        Icon frameIcon = frame.getFrameIcon();
        SynthContext context = getContext(this);
        if (frameIcon != null) {
            Dimension maxSize = (Dimension)context.getStyle().get(context,
                                "InternalFrameTitlePane.maxFrameIconSize");
            int maxWidth = 16;
            int maxHeight = 16;
            if (maxSize != null) {
                maxWidth = maxSize.width;
                maxHeight = maxSize.height;
            }
            if ((frameIcon.getIconWidth() > maxWidth ||
                     frameIcon.getIconHeight() > maxHeight) &&
                    (frameIcon instanceof ImageIcon)) {
                frameIcon = new ImageIcon(((ImageIcon)frameIcon).
                             getImage().getScaledInstance(maxWidth, maxHeight,
                             Image.SCALE_SMOOTH));
            }
        }
        menuButton.setIcon(frameIcon);
    }


    class SynthTitlePaneLayout implements LayoutManager {
        public void addLayoutComponent(String name, Component c) {}
        public void removeLayoutComponent(Component c) {}
        public Dimension preferredLayoutSize(Container c)  {
            return minimumLayoutSize(c);
        }

        public Dimension minimumLayoutSize(Container c) {
            SynthContext context = getContext(
                             SynthInternalFrameTitlePane.this);
            int width = 0;
            int height = 0;

            int buttonCount = 0;
            Dimension pref;

            if (frame.isClosable()) {
                pref = closeButton.getPreferredSize();
                width += pref.width;
                height = Math.max(pref.height, height);
                buttonCount++;
            }
            if (frame.isMaximizable()) {
                pref = maxButton.getPreferredSize();
                width += pref.width;
                height = Math.max(pref.height, height);
                buttonCount++;
            }
            if (frame.isIconifiable()) {
                pref = iconButton.getPreferredSize();
                width += pref.width;
                height = Math.max(pref.height, height);
                buttonCount++;
            }
            pref = menuButton.getPreferredSize();
            width += pref.width;
            height = Math.max(pref.height, height);

            width += Math.max(0, (buttonCount - 1) * buttonSpacing);

            FontMetrics fm = SynthInternalFrameTitlePane.this.getFontMetrics(
                                          getFont());
            SynthGraphicsUtils graphicsUtils = context.getStyle().
                                       getGraphicsUtils(context);
            String frameTitle = frame.getTitle();
            int title_w = frameTitle != null ? graphicsUtils.
                               computeStringWidth(context, fm.getFont(),
                               fm, frameTitle) : 0;
            int title_length = frameTitle != null ? frameTitle.length() : 0;

            // Leave room for three characters in the title.
            if (title_length > 3) {
                int subtitle_w = graphicsUtils.computeStringWidth(context,
                    fm.getFont(), fm, frameTitle.substring(0, 3) + "...");
                width += (title_w < subtitle_w) ? title_w : subtitle_w;
            } else {
                width += title_w;
            }

            height = Math.max(fm.getHeight() + 2, height);

            width += titleSpacing + titleSpacing;

            Insets insets = getInsets();
            height += insets.top + insets.bottom;
            width += insets.left + insets.right;
            return new Dimension(width, height);
        }

        private int center(Component c, Insets insets, int x,
                           boolean trailing) {
            Dimension pref = c.getPreferredSize();
            if (trailing) {
                x -= pref.width;
            }
            c.setBounds(x, insets.top +
                        (getHeight() - insets.top - insets.bottom -
                         pref.height) / 2, pref.width, pref.height);
            if (pref.width > 0) {
                if (trailing) {
                    return x - buttonSpacing;
                }
                return x + pref.width + buttonSpacing;
            }
            return x;
        }

        public void layoutContainer(Container c) {
            Insets insets = c.getInsets();
            Dimension pref;

            if (SynthLookAndFeel.isLeftToRight(frame)) {
                center(menuButton, insets, insets.left, false);
                int x = getWidth() - insets.right;
                if (frame.isClosable()) {
                    x = center(closeButton, insets, x, true);
                }
                if (frame.isMaximizable()) {
                    x = center(maxButton, insets, x, true);
                }
                if (frame.isIconifiable()) {
                    x = center(iconButton, insets, x, true);
                }
            }
            else {
                center(menuButton, insets, getWidth() - insets.right,
                       true);
                int x = insets.left;
                if (frame.isClosable()) {
                    x = center(closeButton, insets, x, false);
                }
                if (frame.isMaximizable()) {
                    x = center(maxButton, insets, x, false);
                }
                if (frame.isIconifiable()) {
                    x = center(iconButton, insets, x, false);
                }
            }
        }
    }

    private JButton createNoFocusButton() {
        JButton button = new JButton();
        button.setFocusable(false);
        button.setMargin(new Insets(0,0,0,0));
        return button;
    }
}
