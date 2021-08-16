/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation. Oracle designates this
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
package org.netbeans.jemmy.operators;

import java.awt.Component;
import java.awt.Container;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Hashtable;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.MenuElement;
import javax.swing.MenuSelectionManager;
import javax.swing.SingleSelectionModel;
import javax.swing.plaf.MenuBarUI;

import org.netbeans.jemmy.Action;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.ComponentSearcher;
import org.netbeans.jemmy.Outputable;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.TimeoutExpiredException;
import org.netbeans.jemmy.Timeoutable;
import org.netbeans.jemmy.Timeouts;
import org.netbeans.jemmy.drivers.DriverManager;
import org.netbeans.jemmy.drivers.MenuDriver;
import org.netbeans.jemmy.util.Platform;

/**
 * <BR><BR>Timeouts used: <BR>
 * JMenuOperator.WaitBeforePopupTimeout - time to sleep before popup expanding
 * <BR>
 * JMenuOperator.WaitPopupTimeout - time to wait popup displayed <BR>
 * ComponentOperator.WaitComponentTimeout - time to wait button displayed <BR>.
 *
 * @see org.netbeans.jemmy.Timeouts
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 *
 */
public class JMenuBarOperator extends JComponentOperator
        implements Outputable, Timeoutable {

    /**
     * Identifier for a "submenu" properties.
     *
     * @see #getDump
     */
    public static final String SUBMENU_PREFIX_DPROP = "Submenu";

    private TestOut output;
    private Timeouts timeouts;
    private MenuDriver driver;

    /**
     * Constructor.
     *
     * @param b a component
     */
    public JMenuBarOperator(JMenuBar b) {
        super(b);
        driver = DriverManager.getMenuDriver(getClass());
    }

    /**
     * Constructs a JMenuBarOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     */
    public JMenuBarOperator(ContainerOperator<?> cont, ComponentChooser chooser, int index) {
        this((JMenuBar) cont.
                waitSubComponent(new JMenuBarFinder(chooser),
                        index));
        copyEnvironment(cont);
    }

    /**
     * Constructs a JMenuBarOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     */
    public JMenuBarOperator(ContainerOperator<?> cont, ComponentChooser chooser) {
        this(cont, chooser, 0);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont Operator pointing a container to search component in.
     * @throws TimeoutExpiredException
     */
    public JMenuBarOperator(ContainerOperator<?> cont) {
        this((JMenuBar) waitComponent(cont,
                new JMenuBarFinder(),
                0));
        copyEnvironment(cont);
    }

    /**
     * Searches JMenuBar in frame.
     *
     * @param frame a container
     * @return found JMenuBar
     */
    public static JMenuBar findJMenuBar(JFrame frame) {
        return findJMenuBar((Container) frame);
    }

    /**
     * Searches JMenuBar in dialog.
     *
     * @param dialog a container
     * @return found JMenuBar
     */
    public static JMenuBar findJMenuBar(JDialog dialog) {
        return findJMenuBar((Container) dialog);
    }

    /**
     * Searches JMenuBar in container.
     *
     * @param cont a container
     * @return found JMenuBar
     * @throws TimeoutExpiredException
     */
    public static JMenuBar waitJMenuBar(Container cont) {
        return (JMenuBar) waitComponent(cont, new JMenuBarFinder());
    }

    /**
     * Waits JMenuBar in frame.
     *
     * @param frame a container
     * @return found JMenuBar
     * @throws TimeoutExpiredException
     */
    public static JMenuBar waitJMenuBar(JFrame frame) {
        return waitJMenuBar((Container) frame);
    }

    /**
     * Waits JMenuBar in dialog.
     *
     * @param dialog a container
     * @return found JMenuBar
     * @throws TimeoutExpiredException
     */
    public static JMenuBar waitJMenuBar(JDialog dialog) {
        return waitJMenuBar((Container) dialog);
    }

    /**
     * Waits JMenuBar in container.
     *
     * @param cont a container
     * @return found JMenuBar
     */
    public static JMenuBar findJMenuBar(Container cont) {
        return (JMenuBar) findComponent(cont, new JMenuBarFinder());
    }

    static {
        //necessary to init timeouts
        JMenuOperator.performInit();
    }

    @Override
    public void setOutput(TestOut out) {
        super.setOutput(out);
        output = out;
    }

    @Override
    public TestOut getOutput() {
        return output;
    }

    @Override
    public void setTimeouts(Timeouts times) {
        super.setTimeouts(times);
        timeouts = times;
    }

    @Override
    public Timeouts getTimeouts() {
        return timeouts;
    }

    @Override
    public void copyEnvironment(Operator anotherOperator) {
        super.copyEnvironment(anotherOperator);
        driver = DriverManager.getMenuDriver(this);
    }

    /**
     * Pushes menu.
     *
     * @param choosers Array of choosers to find menuItems to push.
     * @return Last pushed JMenuItem.
     * @throws TimeoutExpiredException
     */
    public JMenuItem pushMenu(final ComponentChooser[] choosers) {
        makeComponentVisible();
        return ((JMenuItem) produceTimeRestricted(new Action<Object, Void>() {
            @Override
            public Object launch(Void obj) {
                //TDB 1.5 menu workaround
                getQueueTool().waitEmpty();
                Object result = driver.pushMenu(JMenuBarOperator.this,
                        JMenuOperator.converChoosers(choosers));
                getQueueTool().waitEmpty();
                return result;
            }

            @Override
            public String getDescription() {
                return JMenuOperator.createDescription(choosers);
            }

            @Override
            public String toString() {
                return "JMenuBarOperator.pushMenu.Action{description = " + getDescription() + '}';
            }
        }, "JMenuOperator.PushMenuTimeout"));
    }

    /**
     * Executes {@code pushMenu(choosers)} in a separate thread.
     *
     * @param choosers Array of choosers to find menuItems to push.
     * @see #pushMenu(ComponentChooser[])
     */
    public void pushMenuNoBlock(final ComponentChooser[] choosers) {
        makeComponentVisible();
        produceNoBlocking(new NoBlockingAction<Object, Void>("Menu pushing") {
            @Override
            public Object doAction(Void param) {
                //TDB 1.5 menu workaround
                getQueueTool().waitEmpty();
                Object result = driver.pushMenu(JMenuBarOperator.this,
                        JMenuOperator.converChoosers(choosers));
                getQueueTool().waitEmpty();
                return result;
            }
        });
    }

    /**
     * Pushes menu.
     *
     * @param names an array of menu texts.
     * @param comparator a string comparision algorithm
     * @return Last pushed JMenuItem.
     * @throws TimeoutExpiredException
     */
    public JMenuItem pushMenu(String[] names, StringComparator comparator) {
        return pushMenu(JMenuItemOperator.createChoosers(names, comparator));
    }

    /**
     * Pushes menu.
     *
     * @param names Menu items texts.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @return Last pushed JMenuItem.
     * @throws TimeoutExpiredException
     * @deprecated Use pushMenu(String[]) or pushMenu(String[],
     * StringComparator)
     */
    @Deprecated
    public JMenuItem pushMenu(String[] names, boolean ce, boolean ccs) {
        return pushMenu(names, new DefaultStringComparator(ce, ccs));
    }

    /**
     * Executes {@code pushMenu(names, ce, ccs)} in a separate thread.
     *
     * @param names an array of menu texts.
     * @param comparator a string comparision algorithm
     */
    public void pushMenuNoBlock(String[] names, StringComparator comparator) {
        pushMenuNoBlock(JMenuItemOperator.createChoosers(names, comparator));
    }

    /**
     * Executes {@code pushMenu(names, ce, ccs)} in a separate thread.
     *
     * @param names Menu items texts.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @see #pushMenu(String[], boolean,boolean)
     * @deprecated Use pushMenuNoBlock(String[]) or pushMenuNoBlock(String[],
     * StringComparator)
     */
    @Deprecated
    public void pushMenuNoBlock(String[] names, boolean ce, boolean ccs) {
        pushMenuNoBlock(names, new DefaultStringComparator(ce, ccs));
    }

    /**
     * Pushes menu.
     *
     * @param names Menu items texts.
     * @return Last pushed JMenuItem.
     * @throws TimeoutExpiredException
     */
    public JMenuItem pushMenu(String[] names) {
        return pushMenu(names, getComparator());
    }

    /**
     * Executes {@code pushMenu(names)} in a separate thread.
     *
     * @param names Menu items texts.
     * @see #pushMenu(String[])
     */
    public void pushMenuNoBlock(String[] names) {
        pushMenuNoBlock(names, getComparator());
    }

    /**
     * Pushes menu.
     *
     * @param path a menu path.
     * @param delim a path delimiter.
     * @param comparator a string comparision algorithm
     * @return Last pushed JMenuItem.
     * @throws TimeoutExpiredException
     */
    public JMenuItem pushMenu(String path, String delim, StringComparator comparator) {
        return pushMenu(parseString(path, delim), comparator);
    }

    /**
     * Pushes menu. Uses PathParser assigned to this operator.
     *
     * @param path a menu path.
     * @param comparator a string comparision algorithm
     * @return Last pushed JMenuItem.
     * @throws TimeoutExpiredException
     */
    public JMenuItem pushMenu(String path, StringComparator comparator) {
        return pushMenu(parseString(path), comparator);
    }

    /**
     * Pushes menu.
     *
     * @param path String menupath representation ("File/New", for example).
     * @param delim String menupath divider ("/").
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @return Last pushed JMenuItem.
     * @throws TimeoutExpiredException
     * @deprecated Use pushMenu(String, String) or pushMenu(String, String,
     * StringComparator)
     */
    @Deprecated
    public JMenuItem pushMenu(String path, String delim, boolean ce, boolean ccs) {
        return pushMenu(parseString(path, delim), ce, ccs);
    }

    /**
     * Executes {@code pushMenu(names, delim, comparator)} in a separate
     * thread.
     *
     * @param path a menu path.
     * @param delim a path delimiter.
     * @param comparator a string comparision algorithm
     */
    public void pushMenuNoBlock(String path, String delim, StringComparator comparator) {
        pushMenuNoBlock(parseString(path, delim), comparator);
    }

    /**
     * Executes {@code pushMenu(names, comparator)} in a separate thread.
     * Uses PathParser assigned to this operator.
     *
     * @param path a menu path.
     * @param comparator a string comparision algorithm
     */
    public void pushMenuNoBlock(String path, StringComparator comparator) {
        pushMenuNoBlock(parseString(path), comparator);
    }

    /**
     * Executes {@code pushMenu(path, delim, ce, ccs)} in a separate
     * thread.
     *
     * @param path String menupath representation ("File/New", for example).
     * @param delim String menupath divider ("/").
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @see #pushMenu
     * @deprecated Use pushMenuNoBlock(String, String) or
     * pushMenuNoBlock(String, String, StringComparator)
     */
    @Deprecated
    public void pushMenuNoBlock(String path, String delim, boolean ce, boolean ccs) {
        pushMenuNoBlock(parseString(path, delim), ce, ccs);
    }

    /**
     * Pushes menu.
     *
     * @param path String menupath representation ("File/New", for example).
     * @param delim String menupath divider ("/").
     * @return Last pushed JMenuItem.
     * @throws TimeoutExpiredException
     */
    public JMenuItem pushMenu(String path, String delim) {
        return pushMenu(parseString(path, delim));
    }

    /**
     * Pushes menu. Uses PathParser assigned to this operator.
     *
     * @param path String menupath representation ("File/New", for example).
     * @return Last pushed JMenuItem.
     * @throws TimeoutExpiredException
     */
    public JMenuItem pushMenu(String path) {
        return pushMenu(parseString(path));
    }

    /**
     * Executes {@code pushMenu(path, delim)} in a separate thread.
     *
     * @param path String menupath representation ("File/New", for example).
     * @param delim String menupath divider ("/").
     */
    public void pushMenuNoBlock(String path, String delim) {
        pushMenuNoBlock(parseString(path, delim));
    }

    /**
     * Executes {@code pushMenu(path)} in a separate thread.
     *
     * @param path String menupath representation ("File/New", for example).
     */
    public void pushMenuNoBlock(String path) {
        pushMenuNoBlock(parseString(path));
    }

    public JMenuItemOperator[] showMenuItems(ComponentChooser[] choosers) {
        if (choosers == null || choosers.length == 0) {
            return JMenuItemOperator.getMenuItems((MenuElement) getSource(), this);
        } else {
            return JMenuItemOperator.getMenuItems((JMenu) pushMenu(choosers), this);
        }
    }

    /**
     * Shows submenu of menu specified by a {@code path} parameter.
     *
     * @param path an array of menu texts.
     * @param comparator a string comparision algorithm
     * @return an array of operators created tor items from the submenu.
     * @throws TimeoutExpiredException
     */
    public JMenuItemOperator[] showMenuItems(String[] path, StringComparator comparator) {
        if (path == null || path.length == 0) {
            return JMenuItemOperator.getMenuItems((MenuElement) getSource(), this);
        } else {
            return JMenuItemOperator.getMenuItems((JMenu) pushMenu(path, comparator), this);
        }
    }

    /**
     * Shows submenu of menu specified by a {@code path} parameter. Uses
     * StringComparator assigned to the operator.
     *
     * @param path an array of menu texts.
     * @return an array of operators created tor items from the submenu.
     * @throws TimeoutExpiredException
     */
    public JMenuItemOperator[] showMenuItems(String[] path) {
        return showMenuItems(path, getComparator());
    }

    /**
     * Shows submenu of menu specified by a {@code path} parameter.
     *
     * @param path a string identifying the menu path.
     * @param delim a path delimiter.
     * @param comparator a string comparision algorithm
     * @return an array of operators created tor items from the submenu.
     * @throws TimeoutExpiredException
     */
    public JMenuItemOperator[] showMenuItems(String path, String delim, StringComparator comparator) {
        return showMenuItems(parseString(path, delim), comparator);
    }

    /**
     * Shows submenu of menu specified by a {@code path} parameter. Uses
     * PathParser assigned to this operator.
     *
     * @param path a string identifying the menu path.
     * @param comparator a string comparision algorithm
     * @return an array of operators created tor items from the submenu.
     * @throws TimeoutExpiredException
     */
    public JMenuItemOperator[] showMenuItems(String path, StringComparator comparator) {
        return showMenuItems(parseString(path), comparator);
    }

    /**
     * Shows submenu of menu specified by a {@code path} parameter. Uses
     * StringComparator assigned to the operator.
     *
     * @param path a string identifying the menu path.
     * @param delim a path delimiter.
     * @return an array of operators created tor items from the submenu.
     * @throws TimeoutExpiredException
     */
    public JMenuItemOperator[] showMenuItems(String path, String delim) {
        return showMenuItems(path, delim, getComparator());
    }

    /**
     * Shows submenu of menu specified by a {@code path} parameter. Uses
     * PathParser assigned to this operator. Uses StringComparator assigned to
     * the operator.
     *
     * @param path a string identifying the menu path.
     * @return an array of operators created tor items from the submenu.
     * @throws TimeoutExpiredException
     */
    public JMenuItemOperator[] showMenuItems(String path) {
        return showMenuItems(path, getComparator());
    }

    public JMenuItemOperator showMenuItem(ComponentChooser[] choosers) {
        ComponentChooser[] parentPath = getParentPath(choosers);
        JMenu menu;
        ContainerOperator<?> menuCont;
        if (parentPath.length > 0) {
            menu = (JMenu) pushMenu(getParentPath(choosers));
            menuCont = new ContainerOperator<>(menu.getPopupMenu());
            menuCont.copyEnvironment(this);
        } else {
            menuCont = this;
        }
        JMenuItemOperator result = new JMenuItemOperator(menuCont, choosers[choosers.length - 1]);
        result.copyEnvironment(this);
        return result;
    }

    /**
     * Expends all menus to show menu item specified by a {@code path}
     * parameter.
     *
     * @param path an array of menu texts.
     * @param comparator a string comparision algorithm
     * @return an operator for the last menu item in path.
     * @throws TimeoutExpiredException
     */
    public JMenuItemOperator showMenuItem(String[] path, StringComparator comparator) {
        String[] parentPath = getParentPath(path);
        JMenu menu;
        ContainerOperator<?> menuCont;
        if (parentPath.length > 0) {
            menu = (JMenu) pushMenu(getParentPath(path), comparator);
            menuCont = new ContainerOperator<>(menu.getPopupMenu());
            menuCont.copyEnvironment(this);
        } else {
            menuCont = this;
        }
        JMenuItemOperator result;
        // isVisible() on items returns false on mac, so we need a special searcher.
        if (Platform.isOSX()) {
            ComponentSearcher searcher = new ComponentSearcher((Container) menuCont.getSource());
            searcher.setOutput(output);
            Component c = searcher.findComponent(new JMenuItemOperator.JMenuItemByLabelFinder(path[path.length - 1], getComparator()));
            result = new JMenuItemOperator((JMenuItem) c);
        } else {
            result = new JMenuItemOperator(menuCont, path[path.length - 1]);
        }
        result.copyEnvironment(this);
        return result;
    }

    /**
     * Expands all menus to show menu item specified by a {@code path}
     * parameter.
     *
     * @param path an array of menu texts.
     * @return an operator for the last menu item in path.
     * @throws TimeoutExpiredException
     */
    public JMenuItemOperator showMenuItem(String[] path) {
        return showMenuItem(path, getComparator());
    }

    /**
     * Expands all menus to show menu item specified by a {@code path}
     * parameter.
     *
     * @param path a string identifying the menu path.
     * @param delim a path delimiter.
     * @param comparator a string comparision algorithm
     * @return an operator for the last menu item in path.
     * @throws TimeoutExpiredException
     */
    public JMenuItemOperator showMenuItem(String path, String delim, StringComparator comparator) {
        return showMenuItem(parseString(path, delim), comparator);
    }

    /**
     * Expands all menus to show menu item specified by a {@code path}
     * parameter. Uses PathParser assigned to this operator.
     *
     * @param path a string identifying the menu path.
     * @param comparator a string comparision algorithm
     * @return an operator for the last menu item in path.
     * @throws TimeoutExpiredException
     */
    public JMenuItemOperator showMenuItem(String path, StringComparator comparator) {
        return showMenuItem(parseString(path), comparator);
    }

    /**
     * Expands all menus to show menu item specified by a {@code path}
     * parameter. Uses StringComparator assigned to the operator.
     *
     * @param path a string identifying the menu path.
     * @param delim a path delimiter.
     * @return an operator for the last menu item in path.
     * @throws TimeoutExpiredException
     */
    public JMenuItemOperator showMenuItem(String path, String delim) {
        return showMenuItem(path, delim, getComparator());
    }

    /**
     * Expands all menus to show menu item specified by a {@code path}
     * parameter. Uses PathParser assigned to this operator. Uses
     * StringComparator assigned to the operator.
     *
     * @param path a string identifying the menu path.
     * @return an array of operators created tor items from the submenu.
     * @throws TimeoutExpiredException
     */
    public JMenuItemOperator showMenuItem(String path) {
        return showMenuItem(path, getComparator());
    }

    /**
     * Closes all expanded submenus.
     */
    public void closeSubmenus() {
        JMenu menu = (JMenu) findSubComponent(new ComponentChooser() {
            @Override
            public boolean checkComponent(Component comp) {
                return (comp instanceof JMenu
                        && ((JMenu) comp).isPopupMenuVisible());
            }

            @Override
            public String getDescription() {
                return "Expanded JMenu";
            }

            @Override
            public String toString() {
                return "JMenuBarOperator.closeSubmenus.ComponentChooser{description = " + getDescription() + '}';
            }
        });
        if (menu != null) {
            JMenuOperator oper = new JMenuOperator(menu);
            oper.copyEnvironment(this);
            oper.push();
        }
    }

    @Override
    public Hashtable<String, Object> getDump() {
        Hashtable<String, Object> result = super.getDump();
        String[] items = new String[((JMenuBar) getSource()).getMenuCount()];
        for (int i = 0; i < ((JMenuBar) getSource()).getMenuCount(); i++) {
            if (((JMenuBar) getSource()).getMenu(i) != null) {
                items[i] = ((JMenuBar) getSource()).getMenu(i).getText();
            } else {
                items[i] = "null";
            }
        }
        addToDump(result, SUBMENU_PREFIX_DPROP, items);
        return result;
    }

    ////////////////////////////////////////////////////////
    //Mapping                                             //
    /**
     * Maps {@code JMenuBar.add(JMenu)} through queue
     */
    public JMenu add(final JMenu jMenu) {
        return (runMapping(new MapAction<JMenu>("add") {
            @Override
            public JMenu map() {
                return ((JMenuBar) getSource()).add(jMenu);
            }
        }));
    }

    /**
     * Maps {@code JMenuBar.getComponentIndex(Component)} through queue
     */
    public int getComponentIndex(final Component component) {
        return (runMapping(new MapIntegerAction("getComponentIndex") {
            @Override
            public int map() {
                return ((JMenuBar) getSource()).getComponentIndex(component);
            }
        }));
    }

    /**
     * Maps {@code JMenuBar.getHelpMenu()} through queue
     */
    public JMenu getHelpMenu() {
        return (runMapping(new MapAction<JMenu>("getHelpMenu") {
            @Override
            public JMenu map() {
                return ((JMenuBar) getSource()).getHelpMenu();
            }
        }));
    }

    /**
     * Maps {@code JMenuBar.getMargin()} through queue
     */
    public Insets getMargin() {
        return (runMapping(new MapAction<Insets>("getMargin") {
            @Override
            public Insets map() {
                return ((JMenuBar) getSource()).getMargin();
            }
        }));
    }

    /**
     * Maps {@code JMenuBar.getMenu(int)} through queue
     */
    public JMenu getMenu(final int i) {
        return (runMapping(new MapAction<JMenu>("getMenu") {
            @Override
            public JMenu map() {
                return ((JMenuBar) getSource()).getMenu(i);
            }
        }));
    }

    /**
     * Maps {@code JMenuBar.getMenuCount()} through queue
     */
    public int getMenuCount() {
        return (runMapping(new MapIntegerAction("getMenuCount") {
            @Override
            public int map() {
                return ((JMenuBar) getSource()).getMenuCount();
            }
        }));
    }

    /**
     * Maps {@code JMenuBar.getSelectionModel()} through queue
     */
    public SingleSelectionModel getSelectionModel() {
        return (runMapping(new MapAction<SingleSelectionModel>("getSelectionModel") {
            @Override
            public SingleSelectionModel map() {
                return ((JMenuBar) getSource()).getSelectionModel();
            }
        }));
    }

    /**
     * Maps {@code JMenuBar.getSubElements()} through queue
     */
    public MenuElement[] getSubElements() {
        return ((MenuElement[]) runMapping(new MapAction<Object>("getSubElements") {
            @Override
            public Object map() {
                return ((JMenuBar) getSource()).getSubElements();
            }
        }));
    }

    /**
     * Maps {@code JMenuBar.getUI()} through queue
     */
    public MenuBarUI getUI() {
        return (runMapping(new MapAction<MenuBarUI>("getUI") {
            @Override
            public MenuBarUI map() {
                return ((JMenuBar) getSource()).getUI();
            }
        }));
    }

    /**
     * Maps {@code JMenuBar.isBorderPainted()} through queue
     */
    public boolean isBorderPainted() {
        return (runMapping(new MapBooleanAction("isBorderPainted") {
            @Override
            public boolean map() {
                return ((JMenuBar) getSource()).isBorderPainted();
            }
        }));
    }

    /**
     * Maps {@code JMenuBar.isSelected()} through queue
     */
    public boolean isSelected() {
        return (runMapping(new MapBooleanAction("isSelected") {
            @Override
            public boolean map() {
                return ((JMenuBar) getSource()).isSelected();
            }
        }));
    }

    /**
     * Maps {@code JMenuBar.menuSelectionChanged(boolean)} through queue
     */
    public void menuSelectionChanged(final boolean b) {
        runMapping(new MapVoidAction("menuSelectionChanged") {
            @Override
            public void map() {
                ((JMenuBar) getSource()).menuSelectionChanged(b);
            }
        });
    }

    /**
     * Maps
     * {@code JMenuBar.processKeyEvent(KeyEvent, MenuElement[], MenuSelectionManager)}
     * through queue
     */
    public void processKeyEvent(final KeyEvent keyEvent, final MenuElement[] menuElement, final MenuSelectionManager menuSelectionManager) {
        runMapping(new MapVoidAction("processKeyEvent") {
            @Override
            public void map() {
                ((JMenuBar) getSource()).processKeyEvent(keyEvent, menuElement, menuSelectionManager);
            }
        });
    }

    /**
     * Maps
     * {@code JMenuBar.processMouseEvent(MouseEvent, MenuElement[], MenuSelectionManager)}
     * through queue
     */
    public void processMouseEvent(final MouseEvent mouseEvent, final MenuElement[] menuElement, final MenuSelectionManager menuSelectionManager) {
        runMapping(new MapVoidAction("processMouseEvent") {
            @Override
            public void map() {
                ((JMenuBar) getSource()).processMouseEvent(mouseEvent, menuElement, menuSelectionManager);
            }
        });
    }

    /**
     * Maps {@code JMenuBar.setBorderPainted(boolean)} through queue
     */
    public void setBorderPainted(final boolean b) {
        runMapping(new MapVoidAction("setBorderPainted") {
            @Override
            public void map() {
                ((JMenuBar) getSource()).setBorderPainted(b);
            }
        });
    }

    /**
     * Maps {@code JMenuBar.setHelpMenu(JMenu)} through queue
     */
    public void setHelpMenu(final JMenu jMenu) {
        runMapping(new MapVoidAction("setHelpMenu") {
            @Override
            public void map() {
                ((JMenuBar) getSource()).setHelpMenu(jMenu);
            }
        });
    }

    /**
     * Maps {@code JMenuBar.setMargin(Insets)} through queue
     */
    public void setMargin(final Insets insets) {
        runMapping(new MapVoidAction("setMargin") {
            @Override
            public void map() {
                ((JMenuBar) getSource()).setMargin(insets);
            }
        });
    }

    /**
     * Maps {@code JMenuBar.setSelected(Component)} through queue
     */
    public void setSelected(final Component component) {
        runMapping(new MapVoidAction("setSelected") {
            @Override
            public void map() {
                ((JMenuBar) getSource()).setSelected(component);
            }
        });
    }

    /**
     * Maps {@code JMenuBar.setSelectionModel(SingleSelectionModel)}
     * through queue
     */
    public void setSelectionModel(final SingleSelectionModel singleSelectionModel) {
        runMapping(new MapVoidAction("setSelectionModel") {
            @Override
            public void map() {
                ((JMenuBar) getSource()).setSelectionModel(singleSelectionModel);
            }
        });
    }

    /**
     * Maps {@code JMenuBar.setUI(MenuBarUI)} through queue
     */
    public void setUI(final MenuBarUI menuBarUI) {
        runMapping(new MapVoidAction("setUI") {
            @Override
            public void map() {
                ((JMenuBar) getSource()).setUI(menuBarUI);
            }
        });
    }

    //End of mapping                                      //
    ////////////////////////////////////////////////////////
    /**
     * Checks component type.
     */
    public static class JMenuBarFinder extends Finder {

        /**
         * Constructs JMenuBarFinder.
         *
         * @param sf other searching criteria.
         */
        public JMenuBarFinder(ComponentChooser sf) {
            super(JMenuBar.class, sf);
        }

        /**
         * Constructs JMenuBarFinder.
         */
        public JMenuBarFinder() {
            super(JMenuBar.class);
        }
    }

}
