/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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

import sun.swing.DefaultLookup;
import sun.swing.UIAction;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.border.*;
import javax.swing.plaf.*;


/**
 * A default L&amp;F implementation of MenuBarUI.  This implementation
 * is a "combined" view/controller.
 *
 * @author Georges Saab
 * @author David Karlton
 * @author Arnaud Weber
 */
public class BasicMenuBarUI extends MenuBarUI  {

    /**
     * The instance of {@code JMenuBar}.
     */
    protected JMenuBar              menuBar = null;

    /**
     * The instance of {@code ContainerListener}.
     */
    protected ContainerListener     containerListener;

    /**
     * The instance of {@code ChangeListener}.
     */
    protected ChangeListener        changeListener;
    private Handler handler;

    /**
     * Constructs a {@code BasicMenuBarUI}.
     */
    public BasicMenuBarUI() {}

    /**
     * Returns a new instance of {@code BasicMenuBarUI}.
     *
     * @param x a component
     * @return a new instance of {@code BasicMenuBarUI}
     */
    public static ComponentUI createUI(JComponent x) {
        return new BasicMenuBarUI();
    }

    static void loadActionMap(LazyActionMap map) {
        map.put(new Actions(Actions.TAKE_FOCUS));
    }

    public void installUI(JComponent c) {
        menuBar = (JMenuBar) c;

        installDefaults();
        installListeners();
        installKeyboardActions();

    }

    /**
     * Installs default properties.
     */
    protected void installDefaults() {
        if (menuBar.getLayout() == null ||
            menuBar.getLayout() instanceof UIResource) {
            menuBar.setLayout(new DefaultMenuLayout(menuBar,BoxLayout.LINE_AXIS));
        }

        LookAndFeel.installProperty(menuBar, "opaque", Boolean.TRUE);
        LookAndFeel.installBorder(menuBar,"MenuBar.border");
        LookAndFeel.installColorsAndFont(menuBar,
                                              "MenuBar.background",
                                              "MenuBar.foreground",
                                              "MenuBar.font");
    }

    /**
     * Registers listeners.
     */
    protected void installListeners() {
        containerListener = createContainerListener();
        changeListener = createChangeListener();

        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenu menu = menuBar.getMenu(i);
            if (menu!=null)
                menu.getModel().addChangeListener(changeListener);
        }
        menuBar.addContainerListener(containerListener);
    }

    /**
     * Registers keyboard actions.
     */
    protected void installKeyboardActions() {
        InputMap inputMap = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);

        SwingUtilities.replaceUIInputMap(menuBar,
                           JComponent.WHEN_IN_FOCUSED_WINDOW, inputMap);

        LazyActionMap.installLazyActionMap(menuBar, BasicMenuBarUI.class,
                                           "MenuBar.actionMap");
    }

    InputMap getInputMap(int condition) {
        if (condition == JComponent.WHEN_IN_FOCUSED_WINDOW) {
            Object[] bindings = (Object[])DefaultLookup.get
                                (menuBar, this, "MenuBar.windowBindings");
            if (bindings != null) {
                return LookAndFeel.makeComponentInputMap(menuBar, bindings);
            }
        }
        return null;
    }

    public void uninstallUI(JComponent c) {
        uninstallDefaults();
        uninstallListeners();
        uninstallKeyboardActions();

        menuBar = null;
    }

    /**
     * Uninstalls default properties.
     */
    protected void uninstallDefaults() {
        if (menuBar!=null) {
            LookAndFeel.uninstallBorder(menuBar);
        }
    }

    /**
     * Unregisters listeners.
     */
    protected void uninstallListeners() {
        menuBar.removeContainerListener(containerListener);

        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenu menu = menuBar.getMenu(i);
            if (menu !=null)
                menu.getModel().removeChangeListener(changeListener);
        }

        containerListener = null;
        changeListener = null;
        handler = null;
    }

    /**
     * Unregisters keyboard actions.
     */
    protected void uninstallKeyboardActions() {
        SwingUtilities.replaceUIInputMap(menuBar, JComponent.
                                       WHEN_IN_FOCUSED_WINDOW, null);
        SwingUtilities.replaceUIActionMap(menuBar, null);
    }

    /**
     * Returns an instance of {@code ContainerListener}.
     *
     * @return an instance of {@code ContainerListener}
     */
    protected ContainerListener createContainerListener() {
        return getHandler();
    }

    /**
     * Returns an instance of {@code ChangeListener}.
     *
     * @return an instance of {@code ChangeListener}
     */
    protected ChangeListener createChangeListener() {
        return getHandler();
    }

    private Handler getHandler() {
        if (handler == null) {
            handler = new Handler();
        }
        return handler;
    }


    public Dimension getMinimumSize(JComponent c) {
        return null;
    }

    public Dimension getMaximumSize(JComponent c) {
        return null;
    }

    private class Handler implements ChangeListener, ContainerListener {
        //
        // ChangeListener
        //
        public void stateChanged(ChangeEvent e) {
            int i,c;
            for(i=0,c = menuBar.getMenuCount() ; i < c ; i++) {
                JMenu menu = menuBar.getMenu(i);
                if(menu !=null && menu.isSelected()) {
                    menuBar.getSelectionModel().setSelectedIndex(i);
                    break;
                }
            }
        }

        //
        // ContainerListener
        //
        public void componentAdded(ContainerEvent e) {
            Component c = e.getChild();
            if (c instanceof JMenu)
                ((JMenu)c).getModel().addChangeListener(changeListener);
        }
        public void componentRemoved(ContainerEvent e) {
            Component c = e.getChild();
            if (c instanceof JMenu)
                ((JMenu)c).getModel().removeChangeListener(changeListener);
        }
    }


    private static class Actions extends UIAction {
        private static final String TAKE_FOCUS = "takeFocus";

        Actions(String key) {
            super(key);
        }

        public void actionPerformed(ActionEvent e) {
            // TAKE_FOCUS
            JMenuBar menuBar = (JMenuBar)e.getSource();
            MenuSelectionManager defaultManager = MenuSelectionManager.defaultManager();
            MenuElement[] me;
            MenuElement[] subElements;
            JMenu menu = menuBar.getMenu(0);
            if (menu!=null) {
                    me = new MenuElement[3];
                    me[0] = (MenuElement) menuBar;
                    me[1] = (MenuElement) menu;
                    me[2] = (MenuElement) menu.getPopupMenu();
                    defaultManager.setSelectedPath(me);
            }
        }
    }
}
