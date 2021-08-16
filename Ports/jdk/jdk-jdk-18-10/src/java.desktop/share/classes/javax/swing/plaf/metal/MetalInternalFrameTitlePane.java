/*
 * Copyright (c) 1998, 2014, Oracle and/or its affiliates. All rights reserved.
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

package javax.swing.plaf.metal;

import sun.swing.SwingUtilities2;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.InternalFrameEvent;
import java.util.EventListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.swing.plaf.basic.BasicInternalFrameTitlePane;


/**
 * Class that manages a JLF title bar
 * @author Steve Wilson
 * @author Brian Beck
 * @since 1.3
 */
@SuppressWarnings("serial") // Superclass is not serializable across versions
public class MetalInternalFrameTitlePane  extends BasicInternalFrameTitlePane {

    /**
     * The value {@code isPalette}
     */
    protected boolean isPalette = false;

    /**
     * The palette close icon.
     */
    protected Icon paletteCloseIcon;

    /**
     * The height of the palette title.
     */
    protected int paletteTitleHeight;

    private static final Border handyEmptyBorder = new EmptyBorder(0,0,0,0);

    /**
     * Key used to lookup Color from UIManager. If this is null,
     * <code>getWindowTitleBackground</code> is used.
     */
    private String selectedBackgroundKey;
    /**
     * Key used to lookup Color from UIManager. If this is null,
     * <code>getWindowTitleForeground</code> is used.
     */
    private String selectedForegroundKey;
    /**
     * Key used to lookup shadow color from UIManager. If this is null,
     * <code>getPrimaryControlDarkShadow</code> is used.
     */
    private String selectedShadowKey;
    /**
     * Boolean indicating the state of the <code>JInternalFrame</code>s
     * closable property at <code>updateUI</code> time.
     */
    private boolean wasClosable;

    int buttonsWidth = 0;

    MetalBumps activeBumps
        = new MetalBumps( 0, 0,
                          MetalLookAndFeel.getPrimaryControlHighlight(),
                          MetalLookAndFeel.getPrimaryControlDarkShadow(),
          (UIManager.get("InternalFrame.activeTitleGradient") != null) ? null :
                          MetalLookAndFeel.getPrimaryControl() );
    MetalBumps inactiveBumps
        = new MetalBumps( 0, 0,
                          MetalLookAndFeel.getControlHighlight(),
                          MetalLookAndFeel.getControlDarkShadow(),
        (UIManager.get("InternalFrame.inactiveTitleGradient") != null) ? null :
                          MetalLookAndFeel.getControl() );
    MetalBumps paletteBumps;

    private Color activeBumpsHighlight = MetalLookAndFeel.
                             getPrimaryControlHighlight();
    private Color activeBumpsShadow = MetalLookAndFeel.
                             getPrimaryControlDarkShadow();

    /**
     * Constructs a new instance of {@code MetalInternalFrameTitlePane}
     *
     * @param f an instance of {@code JInternalFrame}
     */
    public MetalInternalFrameTitlePane(JInternalFrame f) {
        super( f );
    }

    public void addNotify() {
        super.addNotify();
        // This is done here instead of in installDefaults as I was worried
        // that the BasicInternalFrameUI might not be fully initialized, and
        // that if this resets the closable state the BasicInternalFrameUI
        // Listeners that get notified might be in an odd/uninitialized state.
        updateOptionPaneState();
    }

    protected void installDefaults() {
        super.installDefaults();
        setFont( UIManager.getFont("InternalFrame.titleFont") );
        paletteTitleHeight
            = UIManager.getInt("InternalFrame.paletteTitleHeight");
        paletteCloseIcon = UIManager.getIcon("InternalFrame.paletteCloseIcon");
        wasClosable = frame.isClosable();
        selectedForegroundKey = selectedBackgroundKey = null;
        if (MetalLookAndFeel.usingOcean()) {
            setOpaque(true);
        }
    }

    protected void uninstallDefaults() {
        super.uninstallDefaults();
        if (wasClosable != frame.isClosable()) {
            frame.setClosable(wasClosable);
        }
    }

    protected void createButtons() {
        super.createButtons();

        Boolean paintActive = frame.isSelected() ? Boolean.TRUE:Boolean.FALSE;
        iconButton.putClientProperty("paintActive", paintActive);
        iconButton.setBorder(handyEmptyBorder);

        maxButton.putClientProperty("paintActive", paintActive);
        maxButton.setBorder(handyEmptyBorder);

        closeButton.putClientProperty("paintActive", paintActive);
        closeButton.setBorder(handyEmptyBorder);

        // The palette close icon isn't opaque while the regular close icon is.
        // This makes sure palette close buttons have the right background.
        closeButton.setBackground(MetalLookAndFeel.getPrimaryControlShadow());

        if (MetalLookAndFeel.usingOcean()) {
            iconButton.setContentAreaFilled(false);
            maxButton.setContentAreaFilled(false);
            closeButton.setContentAreaFilled(false);
        }
    }

    /**
     * Override the parent's method to do nothing. Metal frames do not
     * have system menus.
     */
    protected void assembleSystemMenu() {}

    /**
     * Override the parent's method to do nothing. Metal frames do not
     * have system menus.
     */
    protected void addSystemMenuItems(JMenu systemMenu) {}

    /**
     * Override the parent's method to do nothing. Metal frames do not
     * have system menus.
     */
    protected void showSystemMenu() {}

    /**
     * Override the parent's method avoid creating a menu bar. Metal frames
     * do not have system menus.
     */
    protected void addSubComponents() {
        add(iconButton);
        add(maxButton);
        add(closeButton);
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new MetalPropertyChangeHandler();
    }

    protected LayoutManager createLayout() {
        return new MetalTitlePaneLayout();
    }

    class MetalPropertyChangeHandler
        extends BasicInternalFrameTitlePane.PropertyChangeHandler
    {
        public void propertyChange(PropertyChangeEvent evt) {
            String prop = evt.getPropertyName();
            if( prop.equals(JInternalFrame.IS_SELECTED_PROPERTY) ) {
                Boolean b = (Boolean)evt.getNewValue();
                iconButton.putClientProperty("paintActive", b);
                closeButton.putClientProperty("paintActive", b);
                maxButton.putClientProperty("paintActive", b);
            }
            else if ("JInternalFrame.messageType".equals(prop)) {
                updateOptionPaneState();
                frame.repaint();
            }
            super.propertyChange(evt);
        }
    }

    class MetalTitlePaneLayout extends TitlePaneLayout {
        public void addLayoutComponent(String name, Component c) {}
        public void removeLayoutComponent(Component c) {}
        public Dimension preferredLayoutSize(Container c)  {
            return minimumLayoutSize(c);
        }

        public Dimension minimumLayoutSize(Container c) {
            // Compute width.
            int width = 30;
            if (frame.isClosable()) {
                width += 21;
            }
            if (frame.isMaximizable()) {
                width += 16 + (frame.isClosable() ? 10 : 4);
            }
            if (frame.isIconifiable()) {
                width += 16 + (frame.isMaximizable() ? 2 :
                    (frame.isClosable() ? 10 : 4));
            }
            FontMetrics fm = frame.getFontMetrics(getFont());
            String frameTitle = frame.getTitle();
            int title_w = frameTitle != null ? SwingUtilities2.stringWidth(
                               frame, fm, frameTitle) : 0;
            int title_length = frameTitle != null ? frameTitle.length() : 0;

            if (title_length > 2) {
                int subtitle_w = SwingUtilities2.stringWidth(frame, fm,
                                     frame.getTitle().substring(0, 2) + "...");
                width += (title_w < subtitle_w) ? title_w : subtitle_w;
            }
            else {
                width += title_w;
            }

            // Compute height.
            int height;
            if (isPalette) {
                height = paletteTitleHeight;
            } else {
                int fontHeight = fm.getHeight();
                fontHeight += 7;
                Icon icon = frame.getFrameIcon();
                int iconHeight = 0;
                if (icon != null) {
                    // SystemMenuBar forces the icon to be 16x16 or less.
                    iconHeight = Math.min(icon.getIconHeight(), 16);
                }
                iconHeight += 5;
                height = Math.max(fontHeight, iconHeight);
            }

            return new Dimension(width, height);
        }

        public void layoutContainer(Container c) {
            boolean leftToRight = MetalUtils.isLeftToRight(frame);

            int w = getWidth();
            int x = leftToRight ? w : 0;
            int y = 2;
            int spacing;

            // assumes all buttons have the same dimensions
            // these dimensions include the borders
            int buttonHeight = closeButton.getIcon().getIconHeight();
            int buttonWidth = closeButton.getIcon().getIconWidth();

            if(frame.isClosable()) {
                if (isPalette) {
                    spacing = 3;
                    x += leftToRight ? -spacing -(buttonWidth+2) : spacing;
                    closeButton.setBounds(x, y, buttonWidth+2, getHeight()-4);
                    if( !leftToRight ) x += (buttonWidth+2);
                } else {
                    spacing = 4;
                    x += leftToRight ? -spacing -buttonWidth : spacing;
                    closeButton.setBounds(x, y, buttonWidth, buttonHeight);
                    if( !leftToRight ) x += buttonWidth;
                }
            }

            if(frame.isMaximizable() && !isPalette ) {
                spacing = frame.isClosable() ? 10 : 4;
                x += leftToRight ? -spacing -buttonWidth : spacing;
                maxButton.setBounds(x, y, buttonWidth, buttonHeight);
                if( !leftToRight ) x += buttonWidth;
            }

            if(frame.isIconifiable() && !isPalette ) {
                spacing = frame.isMaximizable() ? 2
                          : (frame.isClosable() ? 10 : 4);
                x += leftToRight ? -spacing -buttonWidth : spacing;
                iconButton.setBounds(x, y, buttonWidth, buttonHeight);
                if( !leftToRight ) x += buttonWidth;
            }

            buttonsWidth = leftToRight ? w - x : x;
        }
    }

    /**
     * Paints palette.
     *
     * @param g a instance of {@code Graphics}
     */
    public void paintPalette(Graphics g)  {
        boolean leftToRight = MetalUtils.isLeftToRight(frame);

        int width = getWidth();
        int height = getHeight();

        if (paletteBumps == null) {
            paletteBumps
                = new MetalBumps(0, 0,
                                 MetalLookAndFeel.getPrimaryControlHighlight(),
                                 MetalLookAndFeel.getPrimaryControlInfo(),
                                 MetalLookAndFeel.getPrimaryControlShadow() );
        }

        Color background = MetalLookAndFeel.getPrimaryControlShadow();
        Color darkShadow = MetalLookAndFeel.getPrimaryControlDarkShadow();

        g.setColor(background);
        g.fillRect(0, 0, width, height);

        g.setColor( darkShadow );
        g.drawLine ( 0, height - 1, width, height -1);

        int xOffset = leftToRight ? 4 : buttonsWidth + 4;
        int bumpLength = width - buttonsWidth -2*4;
        int bumpHeight = getHeight()  - 4;
        paletteBumps.setBumpArea( bumpLength, bumpHeight );
        paletteBumps.paintIcon( this, g, xOffset, 2);
    }

    public void paintComponent(Graphics g)  {
        if(isPalette) {
            paintPalette(g);
            return;
        }

        boolean leftToRight = MetalUtils.isLeftToRight(frame);
        boolean isSelected = frame.isSelected();

        int width = getWidth();
        int height = getHeight();

        Color background = null;
        Color foreground = null;
        Color shadow = null;

        MetalBumps bumps;
        String gradientKey;

        if (isSelected) {
            if (!MetalLookAndFeel.usingOcean()) {
                closeButton.setContentAreaFilled(true);
                maxButton.setContentAreaFilled(true);
                iconButton.setContentAreaFilled(true);
            }
            if (selectedBackgroundKey != null) {
                background = UIManager.getColor(selectedBackgroundKey);
            }
            if (background == null) {
                background = MetalLookAndFeel.getWindowTitleBackground();
            }
            if (selectedForegroundKey != null) {
                foreground = UIManager.getColor(selectedForegroundKey);
            }
            if (selectedShadowKey != null) {
                shadow = UIManager.getColor(selectedShadowKey);
            }
            if (shadow == null) {
                shadow = MetalLookAndFeel.getPrimaryControlDarkShadow();
            }
            if (foreground == null) {
                foreground = MetalLookAndFeel.getWindowTitleForeground();
            }
            activeBumps.setBumpColors(activeBumpsHighlight, activeBumpsShadow,
                        UIManager.get("InternalFrame.activeTitleGradient") !=
                                      null ? null : background);
            bumps = activeBumps;
            gradientKey = "InternalFrame.activeTitleGradient";
        } else {
            if (!MetalLookAndFeel.usingOcean()) {
                closeButton.setContentAreaFilled(false);
                maxButton.setContentAreaFilled(false);
                iconButton.setContentAreaFilled(false);
            }
            background = MetalLookAndFeel.getWindowTitleInactiveBackground();
            foreground = MetalLookAndFeel.getWindowTitleInactiveForeground();
            shadow = MetalLookAndFeel.getControlDarkShadow();
            bumps = inactiveBumps;
            gradientKey = "InternalFrame.inactiveTitleGradient";
        }

        if (!MetalUtils.drawGradient(this, g, gradientKey, 0, 0, width,
                                     height, true)) {
            g.setColor(background);
            g.fillRect(0, 0, width, height);
        }

        g.setColor( shadow );
        g.drawLine ( 0, height - 1, width, height -1);
        g.drawLine ( 0, 0, 0 ,0);
        g.drawLine ( width - 1, 0 , width -1, 0);


        int titleLength;
        int xOffset = leftToRight ? 5 : width - 5;
        String frameTitle = frame.getTitle();

        Icon icon = frame.getFrameIcon();
        if ( icon != null ) {
            if( !leftToRight )
                xOffset -= icon.getIconWidth();
            int iconY = ((height / 2) - (icon.getIconHeight() /2));
            icon.paintIcon(frame, g, xOffset, iconY);
            xOffset += leftToRight ? icon.getIconWidth() + 5 : -5;
        }

        if(frameTitle != null) {
            Font f = getFont();
            g.setFont(f);
            FontMetrics fm = SwingUtilities2.getFontMetrics(frame, g, f);
            int fHeight = fm.getHeight();

            g.setColor(foreground);

            int yOffset = ( (height - fm.getHeight() ) / 2 ) + fm.getAscent();

            Rectangle rect = new Rectangle(0, 0, 0, 0);
            if (frame.isIconifiable()) { rect = iconButton.getBounds(); }
            else if (frame.isMaximizable()) { rect = maxButton.getBounds(); }
            else if (frame.isClosable()) { rect = closeButton.getBounds(); }
            int titleW;

            if( leftToRight ) {
              if (rect.x == 0) {
                rect.x = frame.getWidth()-frame.getInsets().right-2;
              }
              titleW = rect.x - xOffset - 4;
              frameTitle = getTitle(frameTitle, fm, titleW);
            } else {
              titleW = xOffset - rect.x - rect.width - 4;
              frameTitle = getTitle(frameTitle, fm, titleW);
              xOffset -= SwingUtilities2.stringWidth(frame, fm, frameTitle);
            }

            titleLength = SwingUtilities2.stringWidth(frame, fm, frameTitle);
            SwingUtilities2.drawString(frame, g, frameTitle, xOffset, yOffset);
            xOffset += leftToRight ? titleLength + 5  : -5;
        }

        int bumpXOffset;
        int bumpLength;
        if( leftToRight ) {
            bumpLength = width - buttonsWidth - xOffset - 5;
            bumpXOffset = xOffset;
        } else {
            bumpLength = xOffset - buttonsWidth - 5;
            bumpXOffset = buttonsWidth + 5;
        }
        int bumpYOffset = 3;
        int bumpHeight = getHeight() - (2 * bumpYOffset);
        bumps.setBumpArea( bumpLength, bumpHeight );
        bumps.paintIcon(this, g, bumpXOffset, bumpYOffset);
    }

    /**
     * If {@code b} is {@code true}, sets palette icons.
     *
     * @param b if {@code true}, sets palette icons
     */
    public void setPalette(boolean b) {
        isPalette = b;

        if (isPalette) {
            closeButton.setIcon(paletteCloseIcon);
         if( frame.isMaximizable() )
                remove(maxButton);
            if( frame.isIconifiable() )
                remove(iconButton);
        } else {
            closeButton.setIcon(closeIcon);
            if( frame.isMaximizable() )
                add(maxButton);
            if( frame.isIconifiable() )
                add(iconButton);
        }
        revalidate();
        repaint();
    }

    /**
     * Updates any state dependent upon the JInternalFrame being shown in
     * a <code>JOptionPane</code>.
     */
    private void updateOptionPaneState() {
        int type = -2;
        boolean closable = wasClosable;
        Object obj = frame.getClientProperty("JInternalFrame.messageType");

        if (obj == null) {
            // Don't change the closable state unless in an JOptionPane.
            return;
        }
        if (obj instanceof Integer) {
            type = ((Integer) obj).intValue();
        }
        switch (type) {
        case JOptionPane.ERROR_MESSAGE:
            selectedBackgroundKey =
                              "OptionPane.errorDialog.titlePane.background";
            selectedForegroundKey =
                              "OptionPane.errorDialog.titlePane.foreground";
            selectedShadowKey = "OptionPane.errorDialog.titlePane.shadow";
            closable = false;
            break;
        case JOptionPane.QUESTION_MESSAGE:
            selectedBackgroundKey =
                            "OptionPane.questionDialog.titlePane.background";
            selectedForegroundKey =
                    "OptionPane.questionDialog.titlePane.foreground";
            selectedShadowKey =
                          "OptionPane.questionDialog.titlePane.shadow";
            closable = false;
            break;
        case JOptionPane.WARNING_MESSAGE:
            selectedBackgroundKey =
                              "OptionPane.warningDialog.titlePane.background";
            selectedForegroundKey =
                              "OptionPane.warningDialog.titlePane.foreground";
            selectedShadowKey = "OptionPane.warningDialog.titlePane.shadow";
            closable = false;
            break;
        case JOptionPane.INFORMATION_MESSAGE:
        case JOptionPane.PLAIN_MESSAGE:
            selectedBackgroundKey = selectedForegroundKey = selectedShadowKey =
                                    null;
            closable = false;
            break;
        default:
            selectedBackgroundKey = selectedForegroundKey = selectedShadowKey =
                                    null;
            break;
        }
        if (closable != frame.isClosable()) {
            frame.setClosable(closable);
        }
    }
}
