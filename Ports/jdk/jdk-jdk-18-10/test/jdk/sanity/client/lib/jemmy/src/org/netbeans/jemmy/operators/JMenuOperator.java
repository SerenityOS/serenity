/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Hashtable;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.event.MenuListener;

import org.netbeans.jemmy.Action;
import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.Outputable;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.TimeoutExpiredException;
import org.netbeans.jemmy.Timeoutable;
import org.netbeans.jemmy.Timeouts;
import org.netbeans.jemmy.drivers.DescriptablePathChooser;
import org.netbeans.jemmy.drivers.DriverManager;
import org.netbeans.jemmy.drivers.MenuDriver;

/**
 * <BR><BR>Timeouts used: <BR>
 * JMenuOperator.WaitBeforePopupTimeout - time to sleep before popup expanding
 * <BR>
 * JMenuOperator.WaitPopupTimeout - time to wait popup displayed <BR>
 * JMenuOperator.PushMenuTimeout - time for the whole menu operation<BR>
 * JMenuItemOperator.PushMenuTimeout - time between button pressing and
 * releasing<BR>
 * ComponentOperator.WaitComponentTimeout - time to wait button displayed <BR>
 * ComponentOperator.WaitComponentEnabledTimeout - time to wait button enabled
 * <BR>.
 *
 * @see org.netbeans.jemmy.Timeouts
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 *
 */
public class JMenuOperator extends JMenuItemOperator
        implements Outputable, Timeoutable {

    /**
     * Identifier for a "submenu" properties.
     *
     * @see #getDump
     */
    public static final String SUBMENU_PREFIX_DPROP = "Submenu";

    private final static long WAIT_POPUP_TIMEOUT = 60000;
    private final static long WAIT_BEFORE_POPUP_TIMEOUT = 0;
    private final static long PUSH_MENU_TIMEOUT = 60000;

    private Timeouts timeouts;
    private TestOut output;
    private MenuDriver driver;

    /**
     * Constructor.
     *
     * @param menu a component
     */
    public JMenuOperator(JMenu menu) {
        super(menu);
        driver = DriverManager.getMenuDriver(this);
    }

    /**
     * Constructs a JMenuOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     */
    public JMenuOperator(ContainerOperator<?> cont, ComponentChooser chooser, int index) {
        this((JMenu) cont.
                waitSubComponent(new JMenuFinder(chooser),
                        index));
        copyEnvironment(cont);
    }

    /**
     * Constructs a JMenuOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     */
    public JMenuOperator(ContainerOperator<?> cont, ComponentChooser chooser) {
        this(cont, chooser, 0);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @param text Button text.
     * @param index Ordinal component index.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public JMenuOperator(ContainerOperator<?> cont, String text, int index) {
        this((JMenu) waitComponent(cont,
                new JMenuByLabelFinder(text,
                        cont.getComparator()),
                index));
        copyEnvironment(cont);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @param text Button text.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public JMenuOperator(ContainerOperator<?> cont, String text) {
        this(cont, text, 0);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @param index Ordinal component index.
     * @throws TimeoutExpiredException
     */
    public JMenuOperator(ContainerOperator<?> cont, int index) {
        this((JMenu) waitComponent(cont,
                new JMenuFinder(),
                index));
        copyEnvironment(cont);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @throws TimeoutExpiredException
     */
    public JMenuOperator(ContainerOperator<?> cont) {
        this(cont, 0);
    }

    /**
     * Searches JMenu in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @return JMenu instance or null if component was not found.
     */
    public static JMenu findJMenu(Container cont, ComponentChooser chooser, int index) {
        return (JMenu) findComponent(cont, new JMenuFinder(chooser), index);
    }

    /**
     * Searches 0'th JMenu in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return JMenu instance or null if component was not found.
     */
    public static JMenu findJMenu(Container cont, ComponentChooser chooser) {
        return findJMenu(cont, chooser, 0);
    }

    /**
     * Searches JMenu by text.
     *
     * @param cont Container to search component in.
     * @param text Button text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param index Ordinal component index.
     * @return JMenu instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static JMenu findJMenu(Container cont, String text, boolean ce, boolean ccs, int index) {
        return (findJMenu(cont,
                new JMenuByLabelFinder(text,
                        new DefaultStringComparator(ce, ccs)),
                index));
    }

    /**
     * Searches JMenu by text.
     *
     * @param cont Container to search component in.
     * @param text Button text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @return JMenu instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static JMenu findJMenu(Container cont, String text, boolean ce, boolean ccs) {
        return findJMenu(cont, text, ce, ccs, 0);
    }

    /**
     * Waits JMenu in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @return JMenu instance.
     * @throws TimeoutExpiredException
     */
    public static JMenu waitJMenu(final Container cont, final ComponentChooser chooser, final int index) {
        return (JMenu) waitComponent(cont, new JMenuFinder(chooser), index);
    }

    /**
     * Waits 0'th JMenu in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return JMenu instance.
     * @throws TimeoutExpiredException
     */
    public static JMenu waitJMenu(Container cont, ComponentChooser chooser) {
        return waitJMenu(cont, chooser, 0);
    }

    /**
     * Waits JMenu by text.
     *
     * @param cont Container to search component in.
     * @param text Button text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param index Ordinal component index.
     * @return JMenu instance.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public static JMenu waitJMenu(Container cont, String text, boolean ce, boolean ccs, int index) {
        return (waitJMenu(cont,
                new JMenuByLabelFinder(text,
                        new DefaultStringComparator(ce, ccs)),
                index));
    }

    /**
     * Waits JMenu by text.
     *
     * @param cont Container to search component in.
     * @param text Button text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @return JMenu instance.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public static JMenu waitJMenu(Container cont, String text, boolean ce, boolean ccs) {
        return waitJMenu(cont, text, ce, ccs, 0);
    }

    public static void performInit() {
        Timeouts.initDefault("JMenuOperator.WaitBeforePopupTimeout", WAIT_BEFORE_POPUP_TIMEOUT);
        Timeouts.initDefault("JMenuOperator.WaitPopupTimeout", WAIT_POPUP_TIMEOUT);
        Timeouts.initDefault("JMenuOperator.PushMenuTimeout", PUSH_MENU_TIMEOUT);
    }

    static {
        performInit();
    }

    @Override
    public void setTimeouts(Timeouts timeouts) {
        super.setTimeouts(timeouts);
        this.timeouts = timeouts;
    }

    @Override
    public Timeouts getTimeouts() {
        return timeouts;
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
        return ((JMenuItem) produceTimeRestricted(new Action<Object, Void>() {
            @Override
            public Object launch(Void obj) {
                //TDB 1.5 menu workaround
                getQueueTool().waitEmpty();
                Object result = driver.pushMenu(JMenuOperator.this, converChoosers(choosers));
                getQueueTool().waitEmpty();
                return result;
            }

            @Override
            public String getDescription() {
                return createDescription(choosers);
            }

            @Override
            public String toString() {
                return "JMenuOperator.pushMenu.Action{description = " + getDescription() + '}';
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
        produceNoBlocking(new NoBlockingAction<Object, Void>("Menu pushing") {
            @Override
            public Object doAction(Void param) {
                //TDB 1.5 menu workaround
                getQueueTool().waitEmpty();
                Object result = driver.pushMenu(JMenuOperator.this, converChoosers(choosers));
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
     * @throws TimeoutExpiredException
     * @return Last pushed JMenuItem.
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
     * Pushes menu. Uses StringComparator assigned to this object,
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
        output.printLine("Pushing " + path + " menu in \n    " + toStringSource());
        output.printGolden("Pushing " + path + " menu in \n    " + toStringSource());
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
        output.printLine("Pushing " + path + " menu in \n    " + toStringSource());
        output.printGolden("Pushing " + path + " menu in \n    " + toStringSource());
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
     * @deprecated Use pushMenuNoBlock(String) or pushMenuNoBlock(String,
     * StringComparator)
     */
    @Deprecated
    public JMenuItem pushMenu(String path, String delim, boolean ce, boolean ccs) {
        return pushMenu(path, delim, new DefaultStringComparator(ce, ccs));
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
        output.printLine("Pushing " + path + " menu in \n    " + toStringSource());
        output.printGolden("Pushing " + path + " menu in \n    " + toStringSource());
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
        output.printLine("Pushing " + path + " menu in \n    " + toStringSource());
        output.printGolden("Pushing " + path + " menu in \n    " + toStringSource());
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
        pushMenuNoBlock(parseString(path, delim), new DefaultStringComparator(ce, ccs));
    }

    /**
     * Pushes menu. Uses StringComparator assigned to this object,
     *
     * @param path String menupath representation ("File/New", for example).
     * @param delim String menupath divider ("/").
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @return Last pushed JMenuItem.
     * @throws TimeoutExpiredException
     */
    public JMenuItem pushMenu(String path, String delim) {
        output.printLine("Pushing " + path + " menu in \n    " + toStringSource());
        output.printGolden("Pushing " + path + " menu in \n    " + toStringSource());
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
        output.printLine("Pushing " + path + " menu in \n    " + toStringSource());
        output.printGolden("Pushing " + path + " menu in \n    " + toStringSource());
        return pushMenu(parseString(path));
    }

    /**
     * Executes {@code pushMenu(path, delim)} in a separate thread.
     *
     * @param path String menupath representation ("File/New", for example).
     * @param delim String menupath divider ("/").
     */
    public void pushMenuNoBlock(String path, String delim) {
        output.printLine("Pushing " + path + " menu in \n    " + toStringSource());
        output.printGolden("Pushing " + path + " menu in \n    " + toStringSource());
        pushMenuNoBlock(parseString(path, delim));
    }

    /**
     * Executes {@code pushMenu(path)} in a separate thread.
     *
     * @param path String menupath representation ("File/New", for example).
     */
    public void pushMenuNoBlock(String path) {
        output.printLine("Pushing " + path + " menu in \n    " + toStringSource());
        output.printGolden("Pushing " + path + " menu in \n    " + toStringSource());
        pushMenuNoBlock(parseString(path));
    }

    public JMenuItemOperator[] showMenuItems(ComponentChooser[] choosers) {
        return JMenuItemOperator.getMenuItems((JMenu) pushMenu(choosers), this);
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
        return showMenuItems(JMenuItemOperator.createChoosers(path, comparator));
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
        if (parentPath.length > 0) {
            menu = (JMenu) pushMenu(parentPath);
        } else {
            push();
            menu = (JMenu) getSource();
        }
        JPopupMenuOperator popup = new JPopupMenuOperator(menu.getPopupMenu());
        popup.copyEnvironment(this);
        JMenuItemOperator result = new JMenuItemOperator(popup, choosers[choosers.length - 1]);
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
        if (parentPath.length > 0) {
            menu = (JMenu) pushMenu(parentPath, comparator);
        } else {
            push();
            menu = (JMenu) getSource();
        }
        JPopupMenuOperator popup = new JPopupMenuOperator(menu.getPopupMenu());
        popup.copyEnvironment(this);
        JMenuItemOperator result = new JMenuItemOperator(popup, path[path.length - 1]);
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

    @Override
    public Hashtable<String, Object> getDump() {
        Hashtable<String, Object> result = super.getDump();
        String[] items = new String[((JMenu) getSource()).getItemCount()];
        for (int i = 0; i < ((JMenu) getSource()).getItemCount(); i++) {
            if (((JMenu) getSource()).getItem(i) != null
                    && ((JMenu) getSource()).getItem(i).getText() != null) {
                items[i] = ((JMenu) getSource()).getItem(i).getText();
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
     * Maps {@code JMenu.add(String)} through queue
     */
    public JMenuItem add(final String string) {
        return (runMapping(new MapAction<JMenuItem>("add") {
            @Override
            public JMenuItem map() {
                return ((JMenu) getSource()).add(string);
            }
        }));
    }

    /**
     * Maps {@code JMenu.add(Action)} through queue
     */
    public JMenuItem add(final javax.swing.Action action) {
        return (runMapping(new MapAction<JMenuItem>("add") {
            @Override
            public JMenuItem map() {
                return ((JMenu) getSource()).add(action);
            }
        }));
    }

    /**
     * Maps {@code JMenu.add(JMenuItem)} through queue
     */
    public JMenuItem add(final JMenuItem jMenuItem) {
        return (runMapping(new MapAction<JMenuItem>("add") {
            @Override
            public JMenuItem map() {
                return ((JMenu) getSource()).add(jMenuItem);
            }
        }));
    }

    /**
     * Maps {@code JMenu.addMenuListener(MenuListener)} through queue
     */
    public void addMenuListener(final MenuListener menuListener) {
        runMapping(new MapVoidAction("addMenuListener") {
            @Override
            public void map() {
                ((JMenu) getSource()).addMenuListener(menuListener);
            }
        });
    }

    /**
     * Maps {@code JMenu.addSeparator()} through queue
     */
    public void addSeparator() {
        runMapping(new MapVoidAction("addSeparator") {
            @Override
            public void map() {
                ((JMenu) getSource()).addSeparator();
            }
        });
    }

    /**
     * Maps {@code JMenu.getDelay()} through queue
     */
    public int getDelay() {
        return (runMapping(new MapIntegerAction("getDelay") {
            @Override
            public int map() {
                return ((JMenu) getSource()).getDelay();
            }
        }));
    }

    /**
     * Maps {@code JMenu.getItem(int)} through queue
     */
    public JMenuItem getItem(final int i) {
        return (runMapping(new MapAction<JMenuItem>("getItem") {
            @Override
            public JMenuItem map() {
                return ((JMenu) getSource()).getItem(i);
            }
        }));
    }

    /**
     * Maps {@code JMenu.getItemCount()} through queue
     */
    public int getItemCount() {
        return (runMapping(new MapIntegerAction("getItemCount") {
            @Override
            public int map() {
                return ((JMenu) getSource()).getItemCount();
            }
        }));
    }

    /**
     * Maps {@code JMenu.getMenuComponent(int)} through queue
     */
    public Component getMenuComponent(final int i) {
        return (runMapping(new MapAction<Component>("getMenuComponent") {
            @Override
            public Component map() {
                return ((JMenu) getSource()).getMenuComponent(i);
            }
        }));
    }

    /**
     * Maps {@code JMenu.getMenuComponentCount()} through queue
     */
    public int getMenuComponentCount() {
        return (runMapping(new MapIntegerAction("getMenuComponentCount") {
            @Override
            public int map() {
                return ((JMenu) getSource()).getMenuComponentCount();
            }
        }));
    }

    /**
     * Maps {@code JMenu.getMenuComponents()} through queue
     */
    public Component[] getMenuComponents() {
        return ((Component[]) runMapping(new MapAction<Object>("getMenuComponents") {
            @Override
            public Object map() {
                return ((JMenu) getSource()).getMenuComponents();
            }
        }));
    }

    /**
     * Maps {@code JMenu.getPopupMenu()} through queue
     */
    public JPopupMenu getPopupMenu() {
        return (runMapping(new MapAction<JPopupMenu>("getPopupMenu") {
            @Override
            public JPopupMenu map() {
                return ((JMenu) getSource()).getPopupMenu();
            }
        }));
    }

    /**
     * Maps {@code JMenu.insert(String, int)} through queue
     */
    public void insert(final String string, final int i) {
        runMapping(new MapVoidAction("insert") {
            @Override
            public void map() {
                ((JMenu) getSource()).insert(string, i);
            }
        });
    }

    /**
     * Maps {@code JMenu.insert(Action, int)} through queue
     */
    public JMenuItem insert(final javax.swing.Action action, final int i) {
        return (runMapping(new MapAction<JMenuItem>("insert") {
            @Override
            public JMenuItem map() {
                return ((JMenu) getSource()).insert(action, i);
            }
        }));
    }

    /**
     * Maps {@code JMenu.insert(JMenuItem, int)} through queue
     */
    public JMenuItem insert(final JMenuItem jMenuItem, final int i) {
        return (runMapping(new MapAction<JMenuItem>("insert") {
            @Override
            public JMenuItem map() {
                return ((JMenu) getSource()).insert(jMenuItem, i);
            }
        }));
    }

    /**
     * Maps {@code JMenu.insertSeparator(int)} through queue
     */
    public void insertSeparator(final int i) {
        runMapping(new MapVoidAction("insertSeparator") {
            @Override
            public void map() {
                ((JMenu) getSource()).insertSeparator(i);
            }
        });
    }

    /**
     * Maps {@code JMenu.isMenuComponent(Component)} through queue
     */
    public boolean isMenuComponent(final Component component) {
        return (runMapping(new MapBooleanAction("isMenuComponent") {
            @Override
            public boolean map() {
                return ((JMenu) getSource()).isMenuComponent(component);
            }
        }));
    }

    /**
     * Maps {@code JMenu.isPopupMenuVisible()} through queue
     */
    public boolean isPopupMenuVisible() {
        return (runMapping(new MapBooleanAction("isPopupMenuVisible") {
            @Override
            public boolean map() {
                return ((JMenu) getSource()).isPopupMenuVisible();
            }
        }));
    }

    /**
     * Maps {@code JMenu.isTearOff()} through queue
     */
    public boolean isTearOff() {
        return (runMapping(new MapBooleanAction("isTearOff") {
            @Override
            public boolean map() {
                return ((JMenu) getSource()).isTearOff();
            }
        }));
    }

    /**
     * Maps {@code JMenu.isTopLevelMenu()} through queue
     */
    public boolean isTopLevelMenu() {
        return (runMapping(new MapBooleanAction("isTopLevelMenu") {
            @Override
            public boolean map() {
                return ((JMenu) getSource()).isTopLevelMenu();
            }
        }));
    }

    /**
     * Maps {@code JMenu.remove(JMenuItem)} through queue
     */
    public void remove(final JMenuItem jMenuItem) {
        runMapping(new MapVoidAction("remove") {
            @Override
            public void map() {
                ((JMenu) getSource()).remove(jMenuItem);
            }
        });
    }

    /**
     * Maps {@code JMenu.removeMenuListener(MenuListener)} through queue
     */
    public void removeMenuListener(final MenuListener menuListener) {
        runMapping(new MapVoidAction("removeMenuListener") {
            @Override
            public void map() {
                ((JMenu) getSource()).removeMenuListener(menuListener);
            }
        });
    }

    /**
     * Maps {@code JMenu.setDelay(int)} through queue
     */
    public void setDelay(final int i) {
        runMapping(new MapVoidAction("setDelay") {
            @Override
            public void map() {
                ((JMenu) getSource()).setDelay(i);
            }
        });
    }

    /**
     * Maps {@code JMenu.setMenuLocation(int, int)} through queue
     */
    public void setMenuLocation(final int i, final int i1) {
        runMapping(new MapVoidAction("setMenuLocation") {
            @Override
            public void map() {
                ((JMenu) getSource()).setMenuLocation(i, i1);
            }
        });
    }

    /**
     * Maps {@code JMenu.setPopupMenuVisible(boolean)} through queue
     */
    public void setPopupMenuVisible(final boolean b) {
        runMapping(new MapVoidAction("setPopupMenuVisible") {
            @Override
            public void map() {
                ((JMenu) getSource()).setPopupMenuVisible(b);
            }
        });
    }

    //End of mapping                                      //
    ////////////////////////////////////////////////////////
    static String createDescription(ComponentChooser[] choosers) {
        StringBuilder description = new StringBuilder("(");
        for (int i = 0; i < choosers.length; i++) {
            if (i > 0) {
                description.append(", ");
            }
            description.append(choosers[i].getDescription());
        }
        description.append(")");
        return "Menu pushing: " + description.toString();
    }

    static DescriptablePathChooser converChoosers(final ComponentChooser[] choosers) {
        return (new DescriptablePathChooser() {
            @Override
            public boolean checkPathComponent(int depth, Object component) {
                return choosers[depth].checkComponent((Component) component);
            }

            @Override
            public int getDepth() {
                return choosers.length;
            }

            @Override
            public String getDescription() {
                return createDescription(choosers);
            }

            @Override
            public String toString() {
                return "JMenuOperator.converChoosers.DescriptablePathChooser{description = " + getDescription() + '}';
            }
        });
    }

    /**
     * Allows to find component by text.
     */
    public static class JMenuByLabelFinder implements ComponentChooser {

        String label;
        StringComparator comparator;

        /**
         * Constructs JMenuByLabelFinder.
         *
         * @param lb a text pattern
         * @param comparator specifies string comparision algorithm.
         */
        public JMenuByLabelFinder(String lb, StringComparator comparator) {
            label = lb;
            this.comparator = comparator;
        }

        /**
         * Constructs JMenuByLabelFinder.
         *
         * @param lb a text pattern
         */
        public JMenuByLabelFinder(String lb) {
            this(lb, Operator.getDefaultStringComparator());
        }

        @Override
        public boolean checkComponent(Component comp) {
            if (comp instanceof JMenu) {
                if (((JMenu) comp).getText() != null) {
                    return (comparator.equals(((JMenu) comp).getText(),
                            label));
                }
            }
            return false;
        }

        @Override
        public String getDescription() {
            return "JMenu with text \"" + label + "\"";
        }

        @Override
        public String toString() {
            return "JMenuByLabelFinder{" + "label=" + label + ", comparator=" + comparator + '}';
        }
    }

    /**
     * Checks component type.
     */
    public static class JMenuFinder extends Finder {

        /**
         * Constructs JMenuFinder.
         *
         * @param sf other searching criteria.
         */
        public JMenuFinder(ComponentChooser sf) {
            super(JMenu.class, sf);
        }

        /**
         * Constructs JMenuFinder.
         */
        public JMenuFinder() {
            super(JMenu.class);
        }
    }
}
