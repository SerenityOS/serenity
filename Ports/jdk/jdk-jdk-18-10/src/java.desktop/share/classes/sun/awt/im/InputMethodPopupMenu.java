/*
 * Copyright (c) 2003, 2007, Oracle and/or its affiliates. All rights reserved.
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

package sun.awt.im;

import java.awt.AWTException;
import java.awt.CheckboxMenuItem;
import java.awt.Component;
import java.awt.Container;
import java.awt.PopupMenu;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.im.spi.InputMethodDescriptor;
import java.util.Locale;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * {@code InputMethodPopupMenu} provides the popup selection menu
 */

abstract class InputMethodPopupMenu implements ActionListener {

    // Factory method to provide the menu, depending on the client, i.e.,
    // provide Swing popup menu if client is a swing app, otherwise AWT popup
    // is created.
    static InputMethodPopupMenu getInstance(Component client, String title) {
        if ((client instanceof JFrame) ||
            (client instanceof JDialog)) {
                return new JInputMethodPopupMenu(title);
        } else {
            return new AWTInputMethodPopupMenu(title);
        }
    }

    abstract void show(Component c, int x, int y);

    abstract void removeAll();

    abstract void addSeparator();

    abstract void addToComponent(Component c);

    abstract Object createSubmenu(String label);

    abstract void add(Object menuItem);

    abstract void addMenuItem(String label, String command, String currentSelection);

    abstract void addMenuItem(Object targetMenu, String label, String command,
                              String currentSelection);

    void addOneInputMethodToMenu(InputMethodLocator locator, String currentSelection) {
        InputMethodDescriptor descriptor = locator.getDescriptor();
        String label = descriptor.getInputMethodDisplayName(null, Locale.getDefault());
        String command = locator.getActionCommandString();
        Locale[] locales = null;
        int localeCount;
        try {
            locales = descriptor.getAvailableLocales();
            localeCount = locales.length;
        } catch (AWTException e) {
            // ??? should have better error handling -
            // tell user what happened, then remove this input method from the list.
            // For the time being, just show it disabled.
            localeCount = 0;
        }
        if (localeCount == 0) {
            // could be IIIMP adapter which has lost its connection
            addMenuItem(label, null, currentSelection);
        } else if (localeCount == 1) {
            if (descriptor.hasDynamicLocaleList()) {
                // try to make sure that what the user sees and what
                // we eventually select is consistent even if the locale
                // list changes in the meantime
                label = descriptor.getInputMethodDisplayName(locales[0], Locale.getDefault());
                command = locator.deriveLocator(locales[0]).getActionCommandString();
            }
            addMenuItem(label, command, currentSelection);
        } else {
            Object submenu = createSubmenu(label);
            add(submenu);
            for (int j = 0; j < localeCount; j++) {
                Locale locale = locales[j];
                String subLabel = getLocaleName(locale);
                String subCommand = locator.deriveLocator(locale).getActionCommandString();
                addMenuItem(submenu, subLabel, subCommand, currentSelection);
            }
        }
    }

    /**
     * Returns whether command indicates the same input method as currentSelection,
     * taking into account that command may not specify a locale where currentSelection does.
     */
    static boolean isSelected(String command, String currentSelection) {
        if (command == null || currentSelection == null) {
            return false;
        }
        if (command.equals(currentSelection)) {
            return true;
        }
        // currentSelection may indicate a locale where command does not
        int index = currentSelection.indexOf('\n');
        if (index != -1 && currentSelection.substring(0, index).equals(command)) {
            return true;
        }
        return false;
    }

    /**
     * Returns a localized locale name for input methods with the
     * given locale. It falls back to Locale.getDisplayName() and
     * then to Locale.toString() if no localized locale name is found.
     *
     * @param locale Locale for which localized locale name is obtained
     */
    String getLocaleName(Locale locale) {
        String localeString = locale.toString();
        String localeName = Toolkit.getProperty("AWT.InputMethodLanguage." + localeString, null);
        if (localeName == null) {
            localeName = locale.getDisplayName();
            if (localeName == null || localeName.length() == 0)
                localeName = localeString;
        }
        return localeName;
    }

    // ActionListener implementation
    public void actionPerformed(ActionEvent event) {
        String choice = event.getActionCommand();
        ((ExecutableInputMethodManager)InputMethodManager.getInstance()).changeInputMethod(choice);
    }

}

class JInputMethodPopupMenu extends InputMethodPopupMenu {
    static JPopupMenu delegate = null;

    JInputMethodPopupMenu(String title) {
        synchronized (this) {
            if (delegate == null) {
                delegate = new JPopupMenu(title);
            }
        }
    }

    void show(Component c, int x, int y) {
        delegate.show(c, x, y);
    }

    void removeAll() {
        delegate.removeAll();
    }

    void addSeparator() {
        delegate.addSeparator();
    }

    void addToComponent(Component c) {
    }

    Object createSubmenu(String label) {
        return new JMenu(label);
    }

    void add(Object menuItem) {
        delegate.add((JMenuItem)menuItem);
    }

    void addMenuItem(String label, String command, String currentSelection) {
        addMenuItem(delegate, label, command, currentSelection);
    }

    void addMenuItem(Object targetMenu, String label, String command, String currentSelection) {
        JMenuItem menuItem;
        if (isSelected(command, currentSelection)) {
            menuItem = new JCheckBoxMenuItem(label, true);
        } else {
            menuItem = new JMenuItem(label);
        }
        menuItem.setActionCommand(command);
        menuItem.addActionListener(this);
        menuItem.setEnabled(command != null);
        if (targetMenu instanceof JMenu) {
            ((JMenu)targetMenu).add(menuItem);
        } else {
            ((JPopupMenu)targetMenu).add(menuItem);
        }
    }

}

class AWTInputMethodPopupMenu extends InputMethodPopupMenu {
    static PopupMenu delegate = null;

    AWTInputMethodPopupMenu(String title) {
        synchronized (this) {
            if (delegate == null) {
                delegate = new PopupMenu(title);
            }
        }
    }

    void show(Component c, int x, int y) {
        delegate.show(c, x, y);
    }

    void removeAll() {
        delegate.removeAll();
    }

    void addSeparator() {
        delegate.addSeparator();
    }

    void addToComponent(Component c) {
        c.add(delegate);
    }

    Object createSubmenu(String label) {
        return new Menu(label);
    }

    void add(Object menuItem) {
        delegate.add((MenuItem)menuItem);
    }

    void addMenuItem(String label, String command, String currentSelection) {
        addMenuItem(delegate, label, command, currentSelection);
    }

    void addMenuItem(Object targetMenu, String label, String command, String currentSelection) {
        MenuItem menuItem;
        if (isSelected(command, currentSelection)) {
            menuItem = new CheckboxMenuItem(label, true);
        } else {
            menuItem = new MenuItem(label);
        }
        menuItem.setActionCommand(command);
        menuItem.addActionListener(this);
        menuItem.setEnabled(command != null);
        ((Menu)targetMenu).add(menuItem);
    }
}
