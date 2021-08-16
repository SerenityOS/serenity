/*
 * Copyright (c) 2006, 2012, Oracle and/or its affiliates. All rights reserved.
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

package sun.tools.jconsole;

import java.awt.*;
import java.beans.*;
import java.lang.reflect.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.*;


/**
 * This class is a temporary workaround for bug 4834918:
 * Win L&F: JInternalFrame should merge with JMenuBar when maximized.
 * It is not a general solution, but intended for use within the
 * limited scope of JConsole when running with XP/Vista styles.
 */
@SuppressWarnings("serial")
public class MaximizableInternalFrame extends JInternalFrame {
    private boolean isXP;
    private JFrame mainFrame;
    private JMenuBar mainMenuBar;
    private String mainTitle;
    private JComponent titlePane;
    private Border normalBorder;
    private PropertyChangeListener pcl;

    public MaximizableInternalFrame(String title, boolean resizable,
                                    boolean closable, boolean maximizable,
                                    boolean iconifiable) {
        super(title, resizable, closable, maximizable, iconifiable);

        init();
    }

    private void init() {
        normalBorder = getBorder();
        isXP = normalBorder.getClass().getName().endsWith("XPBorder");
        if (isXP) {
            setRootPaneCheckingEnabled(false);
            titlePane = ((BasicInternalFrameUI)getUI()).getNorthPane();

            if (pcl == null) {
                pcl = new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent ev) {
                        String prop = ev.getPropertyName();
                        if (prop.equals("icon") ||
                            prop.equals("maximum") ||
                            prop.equals("closed")) {

                            updateFrame();
                        }
                    }
                };
                addPropertyChangeListener(pcl);
            }
        } else if (pcl != null) {
            removePropertyChangeListener(pcl);
            pcl = null;
        }
    }

    private void updateFrame() {
        JFrame mainFrame;
        if (!isXP || (mainFrame = getMainFrame()) == null) {
            return;
        }
        JMenuBar menuBar = getMainMenuBar();
        BasicInternalFrameUI ui = (BasicInternalFrameUI)getUI();
        if (isMaximum() && !isIcon() && !isClosed()) {
            if (ui.getNorthPane() != null) {
                // Merge title bar into menu bar
                mainTitle = mainFrame.getTitle();
                mainFrame.setTitle(mainTitle + " - " + getTitle());
                if (menuBar != null) {
                    // Move buttons to menu bar
                    updateButtonStates();
                    menuBar.add(Box.createGlue());
                    for (Component c : titlePane.getComponents()) {
                        if (c instanceof JButton) {
                            menuBar.add(c);
                        } else if (c instanceof JLabel) {
                            // This is the system menu icon
                            menuBar.add(Box.createHorizontalStrut(3), 0);
                            menuBar.add(c, 1);
                            menuBar.add(Box.createHorizontalStrut(3), 2);
                        }
                    }
                    ui.setNorthPane(null);
                    setBorder(null);
                }
            }
        } else {
            if (ui.getNorthPane() == null) {
                // Restore title bar
                mainFrame.setTitle(mainTitle);
                if (menuBar != null) {
                    // Move buttons back to title bar
                    for (Component c : menuBar.getComponents()) {
                        if (c instanceof JButton || c instanceof JLabel) {
                            titlePane.add(c);
                        } else if (c instanceof Box.Filler) {
                            menuBar.remove(c);
                        }
                    }
                    menuBar.repaint();
                    updateButtonStates();
                    ui.setNorthPane(titlePane);
                    setBorder(normalBorder);
                }
            }
        }
    }

    public void updateUI() {
        boolean isMax = (isXP && getBorder() == null);
        if (isMax) {
            try {
                setMaximum(false);
            } catch (PropertyVetoException ex) { }
        }
        super.updateUI();
        init();
        if (isMax) {
            try {
                setMaximum(true);
            } catch (PropertyVetoException ex) { }
        }
    }

    private JFrame getMainFrame() {
        if (mainFrame == null) {
            JDesktopPane desktop = getDesktopPane();
            if (desktop != null) {
                mainFrame = (JFrame)SwingUtilities.getWindowAncestor(desktop);
            }
        }
        return mainFrame;
    }

    private JMenuBar getMainMenuBar() {
        if (mainMenuBar == null) {
            JFrame mainFrame = getMainFrame();
            if (mainFrame != null) {
                mainMenuBar = mainFrame.getJMenuBar();
                if (mainMenuBar != null &&
                    !(mainMenuBar.getLayout() instanceof FixedMenuBarLayout)) {

                    mainMenuBar.setLayout(new FixedMenuBarLayout(mainMenuBar,
                                                            BoxLayout.X_AXIS));
                }
            }
        }
        return mainMenuBar;
    }

    public void setTitle(String title) {
        if (isXP && isMaximum()) {
            if (getMainFrame() != null) {
                getMainFrame().setTitle(mainTitle + " - " + title);
            }
        }
        super.setTitle(title);
    }


    private class FixedMenuBarLayout extends BoxLayout {
        public FixedMenuBarLayout(Container target, int axis) {
            super(target, axis);
        }

        public void layoutContainer(Container target) {
            super.layoutContainer(target);

            for (Component c : target.getComponents()) {
                if (c instanceof JButton) {
                    int y = (target.getHeight() - c.getHeight()) / 2;
                    c.setLocation(c.getX(), Math.max(2, y));
                }
            }
        }
    }


    // The rest of this class is messy and should not be relied upon. It
    // uses reflection to access private, undocumented, and unsupported
    // classes and fields.
    //
    // Install icon wrappers to display MDI icons when the buttons
    // are in the menubar.
    //
    // Please note that this will very likely fail in a future version
    // of Swing, but at least it should fail silently.
    //
    private static Object WP_MINBUTTON, WP_RESTOREBUTTON, WP_CLOSEBUTTON,
                          WP_MDIMINBUTTON, WP_MDIRESTOREBUTTON, WP_MDICLOSEBUTTON;
    static {
        if (JConsole.IS_WIN) {
            try {
                Class<?> Part =
                    Class.forName("com.sun.java.swing.plaf.windows.TMSchema$Part");
                if (Part != null) {
                    WP_MINBUTTON        = Part.getField("WP_MINBUTTON").get(null);
                    WP_RESTOREBUTTON    = Part.getField("WP_RESTOREBUTTON").get(null);
                    WP_CLOSEBUTTON      = Part.getField("WP_CLOSEBUTTON").get(null);
                    WP_MDIMINBUTTON     = Part.getField("WP_MDIMINBUTTON").get(null);
                    WP_MDIRESTOREBUTTON = Part.getField("WP_MDIRESTOREBUTTON").get(null);
                    WP_MDICLOSEBUTTON   = Part.getField("WP_MDICLOSEBUTTON").get(null);
                }

                for (String str : new String[] { "maximize", "minimize",
                                                 "iconify", "close" }) {
                    String key = "InternalFrame." + str + "Icon";
                    UIManager.put(key,
                                  new MDIButtonIcon(UIManager.getIcon(key)));
                }
            } catch (ClassNotFoundException ex) {
                if (JConsole.debug) {
                    ex.printStackTrace();
                }
            } catch (NoSuchFieldException ex) {
                if (JConsole.debug) {
                    ex.printStackTrace();
                }
            } catch (IllegalAccessException ex) {
                if (JConsole.debug) {
                    ex.printStackTrace();
                }
            }
        }
    }


    // A wrapper class for the title pane button icons.
    // This code should really go in the WindowsIconsFactory class.
    private static class MDIButtonIcon implements Icon {
        Icon windowsIcon;
        Field part;

        MDIButtonIcon(Icon icon) {
            windowsIcon = icon;

            if (WP_MINBUTTON != null) {
                try {
                    part = windowsIcon.getClass().getDeclaredField("part");
                    part.setAccessible(true);
                } catch (NoSuchFieldException ex) {
                    if (JConsole.debug) {
                        ex.printStackTrace();
                    }
                }
            }
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            if (part != null) {
                try {
                    Object v = part.get(windowsIcon);

                    if (c.getParent() instanceof JMenuBar) {
                        // Use MDI icons
                        if (v == WP_MINBUTTON) {
                            part.set(windowsIcon, WP_MDIMINBUTTON);
                        } else if (v == WP_RESTOREBUTTON) {
                            part.set(windowsIcon, WP_MDIRESTOREBUTTON);
                        } else if (v == WP_CLOSEBUTTON) {
                            part.set(windowsIcon, WP_MDICLOSEBUTTON);
                        }
                    } else {
                        // Use regular icons
                        if (v == WP_MDIMINBUTTON) {
                            part.set(windowsIcon, WP_MINBUTTON);
                        } else if (v == WP_MDIRESTOREBUTTON) {
                            part.set(windowsIcon, WP_RESTOREBUTTON);
                        } else if (v == WP_MDICLOSEBUTTON) {
                            part.set(windowsIcon, WP_CLOSEBUTTON);
                        }
                    }
                } catch (IllegalAccessException ex) {
                    if (JConsole.debug) {
                        ex.printStackTrace();
                    }
                }
            }
            windowsIcon.paintIcon(c, g, x, y);
        }

        public int getIconWidth(){
            return windowsIcon.getIconWidth();
        }

        public int getIconHeight() {
            return windowsIcon.getIconHeight();
        }
    }


    // Use reflection to invoke protected methods in BasicInternalFrameTitlePane
    private Method setButtonIcons;
    private Method enableActions;

    private void updateButtonStates() {
        try {
            if (setButtonIcons == null) {
                Class<? extends JComponent> cls = titlePane.getClass();
                Class<?> superCls = cls.getSuperclass();
                setButtonIcons = cls.getDeclaredMethod("setButtonIcons");
                enableActions  = superCls.getDeclaredMethod("enableActions");
                setButtonIcons.setAccessible(true);
                enableActions.setAccessible(true);
            }
            setButtonIcons.invoke(titlePane);
            enableActions.invoke(titlePane);
        } catch (Exception ex) {
            if (JConsole.debug) {
                ex.printStackTrace();
            }
        }
    }
}
