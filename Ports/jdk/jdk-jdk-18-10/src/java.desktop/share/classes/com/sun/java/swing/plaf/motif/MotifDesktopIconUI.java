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

package com.sun.java.swing.plaf.motif;

import sun.swing.SwingUtilities2;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;
import java.beans.*;
import java.util.EventListener;
import java.io.Serializable;

import sun.awt.AWTAccessor;
import sun.awt.AWTAccessor.MouseEventAccessor;

/**
 * Motif rendition of the component.
 *
 * @author Thomas Ball
 * @author Rich Schiavi
 */
public class MotifDesktopIconUI extends BasicDesktopIconUI
{
    protected DesktopIconActionListener desktopIconActionListener;
    protected DesktopIconMouseListener  desktopIconMouseListener;

    protected Icon       defaultIcon;
    protected IconButton iconButton;
    protected IconLabel  iconLabel;

    // This is only used for its system menu, but we need a reference to it so
    // we can remove its listeners.
    private MotifInternalFrameTitlePane sysMenuTitlePane;

    JPopupMenu systemMenu;
    EventListener mml;

    static final int LABEL_HEIGHT = 18;
    static final int LABEL_DIVIDER = 4;    // padding between icon and label

    static final Font defaultTitleFont =
        new Font(Font.SANS_SERIF, Font.PLAIN, 12);

    public static ComponentUI createUI(JComponent c)    {
        return new MotifDesktopIconUI();
    }

    public MotifDesktopIconUI() {
    }

    protected void installDefaults(){
        super.installDefaults();
        setDefaultIcon(UIManager.getIcon("DesktopIcon.icon"));
        iconButton = createIconButton(defaultIcon);
        // An underhanded way of creating a system popup menu.
        sysMenuTitlePane =  new MotifInternalFrameTitlePane(frame);
        systemMenu = sysMenuTitlePane.getSystemMenu();

        MotifBorders.FrameBorder border = new MotifBorders.FrameBorder(desktopIcon);
        desktopIcon.setLayout(new BorderLayout());
        iconButton.setBorder(border);
        desktopIcon.add(iconButton, BorderLayout.CENTER);
        iconLabel = createIconLabel(frame);
        iconLabel.setBorder(border);
        desktopIcon.add(iconLabel, BorderLayout.SOUTH);
        desktopIcon.setSize(desktopIcon.getPreferredSize());
        desktopIcon.validate();
        JLayeredPane.putLayer(desktopIcon, JLayeredPane.getLayer(frame));
    }

    protected void installComponents(){
    }

    protected void uninstallComponents(){
    }

    protected void installListeners(){
        super.installListeners();
        desktopIconActionListener = createDesktopIconActionListener();
        desktopIconMouseListener = createDesktopIconMouseListener();
        iconButton.addActionListener(desktopIconActionListener);
        iconButton.addMouseListener(desktopIconMouseListener);
        iconLabel.addMouseListener(desktopIconMouseListener);
    }

    JInternalFrame.JDesktopIcon getDesktopIcon(){
      return desktopIcon;
    }

    void setDesktopIcon(JInternalFrame.JDesktopIcon d){
      desktopIcon = d;
    }

    JInternalFrame getFrame(){
      return frame;
    }

    void setFrame(JInternalFrame f){
      frame = f ;
    }

    protected void showSystemMenu(){
      systemMenu.show(iconButton, 0, getDesktopIcon().getHeight());
    }

    protected void hideSystemMenu(){
      systemMenu.setVisible(false);
    }

    protected IconLabel createIconLabel(JInternalFrame frame){
        return new IconLabel(frame);
    }

    protected IconButton createIconButton(Icon i){
        return new IconButton(i);
    }

    protected DesktopIconActionListener createDesktopIconActionListener(){
        return new DesktopIconActionListener();
    }

    protected DesktopIconMouseListener createDesktopIconMouseListener(){
        return new DesktopIconMouseListener();
    }

    protected void uninstallDefaults(){
        super.uninstallDefaults();
        desktopIcon.setLayout(null);
        desktopIcon.remove(iconButton);
        desktopIcon.remove(iconLabel);
    }

    protected void uninstallListeners(){
        super.uninstallListeners();
        iconButton.removeActionListener(desktopIconActionListener);
        iconButton.removeMouseListener(desktopIconMouseListener);
        sysMenuTitlePane.uninstallListeners();
    }

    public Dimension getMinimumSize(JComponent c) {
        JInternalFrame iframe = desktopIcon.getInternalFrame();

        int w = defaultIcon.getIconWidth();
        int h = defaultIcon.getIconHeight() + LABEL_HEIGHT + LABEL_DIVIDER;

        Border border = iframe.getBorder();
        if(border != null) {
            w += border.getBorderInsets(iframe).left +
                border.getBorderInsets(iframe).right;
            h += border.getBorderInsets(iframe).bottom +
                border.getBorderInsets(iframe).top;
        }

        return new Dimension(w, h);
    }

    public Dimension getPreferredSize(JComponent c) {
        return getMinimumSize(c);
    }

    public Dimension getMaximumSize(JComponent c){
        return getMinimumSize(c);
    }

    /**
      * Returns the default desktop icon.
      */
    public Icon getDefaultIcon() {
        return defaultIcon;
    }

    /**
      * Sets the icon used as the default desktop icon.
      */
    public void setDefaultIcon(Icon newIcon) {
        defaultIcon = newIcon;
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    protected class IconLabel extends JPanel {
        JInternalFrame frame;

        IconLabel(JInternalFrame f) {
            super();
            this.frame = f;
            setFont(defaultTitleFont);

            // Forward mouse events to titlebar for moves.
            addMouseMotionListener(new MouseMotionListener() {
                public void mouseDragged(MouseEvent e) {
                    forwardEventToParent(e);
                }
                public void mouseMoved(MouseEvent e) {
                    forwardEventToParent(e);
                }
            });
            addMouseListener(new MouseListener() {
                public void mouseClicked(MouseEvent e) {
                    forwardEventToParent(e);
                }
                public void mousePressed(MouseEvent e) {
                    forwardEventToParent(e);
                }
                public void mouseReleased(MouseEvent e) {
                    forwardEventToParent(e);
                }
                public void mouseEntered(MouseEvent e) {
                    forwardEventToParent(e);
                }
                public void mouseExited(MouseEvent e) {
                    forwardEventToParent(e);
                }
            });
        }
        @SuppressWarnings("deprecation")
        void forwardEventToParent(MouseEvent e) {
            MouseEvent newEvent = new MouseEvent(
                getParent(), e.getID(), e.getWhen(), e.getModifiers(),
                e.getX(), e.getY(), e.getXOnScreen(),
                e.getYOnScreen(), e.getClickCount(),
                e.isPopupTrigger(), MouseEvent.NOBUTTON);
            MouseEventAccessor meAccessor = AWTAccessor.getMouseEventAccessor();
            meAccessor.setCausedByTouchEvent(newEvent,
                meAccessor.isCausedByTouchEvent(e));
            getParent().dispatchEvent(newEvent);
        }

        @SuppressWarnings("deprecation")
        public boolean isFocusTraversable() {
            return false;
        }

        public Dimension getMinimumSize() {
            return new Dimension(defaultIcon.getIconWidth() + 1,
                                 LABEL_HEIGHT + LABEL_DIVIDER);
        }

        public Dimension getPreferredSize() {
            String title = frame.getTitle();
            FontMetrics fm = frame.getFontMetrics(defaultTitleFont);
            int w = 4;
            if (title != null) {
                w += SwingUtilities2.stringWidth(frame, fm, title);
            }
            return new Dimension(w, LABEL_HEIGHT + LABEL_DIVIDER);
        }

        public void paint(Graphics g) {
            super.paint(g);

            // touch-up frame
            int maxX = getWidth() - 1;
            Color shadow =
                UIManager.getColor("inactiveCaptionBorder").darker().darker();
            g.setColor(shadow);
            g.setClip(0, 0, getWidth(), getHeight());
            g.drawLine(maxX - 1, 1, maxX - 1, 1);
            g.drawLine(maxX, 0, maxX, 0);

            // fill background
            g.setColor(UIManager.getColor("inactiveCaption"));
            g.fillRect(2, 1, maxX - 3, LABEL_HEIGHT + 1);

            // draw text -- clipping to truncate text like CDE/Motif
            g.setClip(2, 1, maxX - 4, LABEL_HEIGHT);
            int y = LABEL_HEIGHT - SwingUtilities2.getFontMetrics(frame, g).
                                                   getDescent();
            g.setColor(UIManager.getColor("inactiveCaptionText"));
            String title = frame.getTitle();
            if (title != null) {
                SwingUtilities2.drawString(frame, g, title, 4, y);
            }
        }
    }

    @SuppressWarnings("serial") // Superclass is not serializable across versions
    protected class IconButton extends JButton {
        Icon icon;

        IconButton(Icon icon) {
            super(icon);
            this.icon = icon;
            // Forward mouse events to titlebar for moves.
            addMouseMotionListener(new MouseMotionListener() {
                public void mouseDragged(MouseEvent e) {
                    forwardEventToParent(e);
                }
                public void mouseMoved(MouseEvent e) {
                    forwardEventToParent(e);
                }
            });
            addMouseListener(new MouseListener() {
                public void mouseClicked(MouseEvent e) {
                    forwardEventToParent(e);
                }
                public void mousePressed(MouseEvent e) {
                    forwardEventToParent(e);
                }
                public void mouseReleased(MouseEvent e) {
                    if (!systemMenu.isShowing()) {
                        forwardEventToParent(e);
                    }
                }
                public void mouseEntered(MouseEvent e) {
                    forwardEventToParent(e);
                }
                public void mouseExited(MouseEvent e) {
                    forwardEventToParent(e);
                }
            });
        }
        @SuppressWarnings("deprecation")
        void forwardEventToParent(MouseEvent e) {
            MouseEvent newEvent = new MouseEvent(
                getParent(), e.getID(), e.getWhen(), e.getModifiers(),
                e.getX(), e.getY(), e.getXOnScreen(), e.getYOnScreen(),
                e.getClickCount(), e.isPopupTrigger(), MouseEvent.NOBUTTON );
            MouseEventAccessor meAccessor = AWTAccessor.getMouseEventAccessor();
            meAccessor.setCausedByTouchEvent(newEvent,
                meAccessor.isCausedByTouchEvent(e));
            getParent().dispatchEvent(newEvent);
        }

        @SuppressWarnings("deprecation")
        public boolean isFocusTraversable() {
            return false;
        }
    }


    protected class DesktopIconActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e){
            systemMenu.show(iconButton, 0, getDesktopIcon().getHeight());
        }
    }

    protected class DesktopIconMouseListener extends MouseAdapter {
        // if we drag or move we should deengage the popup
        public void mousePressed(MouseEvent e){
            if (e.getClickCount() > 1) {
                try {
                    getFrame().setIcon(false);
                } catch (PropertyVetoException e2){ }
                systemMenu.setVisible(false);
                /* the mouse release will not get reported correctly,
                   because the icon will no longer be in the hierarchy;
                   maybe that should be fixed, but until it is, we need
                   to do the required cleanup here. */
                getFrame().getDesktopPane().getDesktopManager().endDraggingFrame((JComponent)e.getSource());
            }
        }
    }
}
