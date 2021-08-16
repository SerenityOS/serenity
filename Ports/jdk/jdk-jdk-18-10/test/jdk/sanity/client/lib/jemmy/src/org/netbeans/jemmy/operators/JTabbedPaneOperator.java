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

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.util.Hashtable;

import javax.swing.Icon;
import javax.swing.JTabbedPane;
import javax.swing.SingleSelectionModel;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.TabbedPaneUI;

import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.JemmyInputException;
import org.netbeans.jemmy.Outputable;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.TimeoutExpiredException;
import org.netbeans.jemmy.drivers.DriverManager;
import org.netbeans.jemmy.drivers.ListDriver;

/**
 * <BR><BR>Timeouts used: <BR>
 * ComponentOperator.WaitComponentTimeout - time to wait component displayed
 * <BR>.
 *
 * @see org.netbeans.jemmy.Timeouts
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 *
 */
public class JTabbedPaneOperator extends JComponentOperator
        implements Outputable {

    /**
     * Identifier for a "selected page" property.
     *
     * @see #getDump
     */
    public static final String SELECTED_PAGE_DPROP = "Selected";

    /**
     * Identifier for a "page" properties.
     *
     * @see #getDump
     */
    public static final String PAGE_PREFIX_DPROP = "Page";

    private TestOut output;
    private ListDriver driver;

    /**
     * Constructor.
     *
     * @param b a component
     */
    public JTabbedPaneOperator(JTabbedPane b) {
        super(b);
        driver = DriverManager.getListDriver(getClass());
    }

    /**
     * Constructs a JTabbedPaneOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     */
    public JTabbedPaneOperator(ContainerOperator<?> cont, ComponentChooser chooser, int index) {
        this((JTabbedPane) cont.
                waitSubComponent(new JTabbedPaneFinder(chooser),
                        index));
        copyEnvironment(cont);
    }

    /**
     * Constructs a JTabbedPaneOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     */
    public JTabbedPaneOperator(ContainerOperator<?> cont, ComponentChooser chooser) {
        this(cont, chooser, 0);
    }

    /**
     * Constructor. Waits component by tab title first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @param text Tab title.
     * @param tabIndex a page index to check. if equal to -1, selected page is
     * checked.
     * @param index Ordinal component index.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public JTabbedPaneOperator(ContainerOperator<?> cont, String text, int tabIndex, int index) {
        this((JTabbedPane) waitComponent(cont,
                new JTabbedPaneByItemFinder(text, tabIndex,
                        cont.getComparator()),
                index));
        copyEnvironment(cont);
    }

    /**
     * Constructor. Waits component by activetab title first. Uses cont's
     * timeout and output for waiting and to init operator.
     *
     * @param cont a container
     * @param text Title of tab which is currently selected.
     * @param index Ordinal component index.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public JTabbedPaneOperator(ContainerOperator<?> cont, String text, int index) {
        this(cont, text, -1, index);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @param text Title of tab which is currently selected.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public JTabbedPaneOperator(ContainerOperator<?> cont, String text) {
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
    public JTabbedPaneOperator(ContainerOperator<?> cont, int index) {
        this((JTabbedPane) waitComponent(cont,
                new JTabbedPaneFinder(),
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
    public JTabbedPaneOperator(ContainerOperator<?> cont) {
        this(cont, 0);
    }

    /**
     * Searches JTabbedPane in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @return JTabbedPane instance or null if component was not found.
     */
    public static JTabbedPane findJTabbedPane(Container cont, ComponentChooser chooser, int index) {
        return (JTabbedPane) findComponent(cont, new JTabbedPaneFinder(chooser), index);
    }

    /**
     * Searches 0'th JTabbedPane in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return JTabbedPane instance or null if component was not found.
     */
    public static JTabbedPane findJTabbedPane(Container cont, ComponentChooser chooser) {
        return findJTabbedPane(cont, chooser, 0);
    }

    /**
     * Searches JTabbedPane by tab title.
     *
     * @param cont Container to search component in.
     * @param text Tooltip text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param itemIndex Tab index. if -1 selected one is checked.
     * @param index Ordinal component index.
     * @return JTabbedPane instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static JTabbedPane findJTabbedPane(Container cont, String text, boolean ce, boolean ccs, int itemIndex, int index) {
        return findJTabbedPane(cont, new JTabbedPaneByItemFinder(text, itemIndex, new DefaultStringComparator(ce, ccs)), index);
    }

    /**
     * Searches JTabbedPane by tab title.
     *
     * @param cont Container to search component in.
     * @param text Tooltip text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param itemIndex Tab index. if -1 selected one is checked.
     * @return JTabbedPane instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static JTabbedPane findJTabbedPane(Container cont, String text, boolean ce, boolean ccs, int itemIndex) {
        return findJTabbedPane(cont, text, ce, ccs, itemIndex, 0);
    }

    /**
     * Searches JTabbedPane object which component lies on.
     *
     * @param comp Component to find JTabbedPane under.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return JTabbedPane instance or null if component was not found.
     */
    public static JTabbedPane findJTabbedPaneUnder(Component comp, ComponentChooser chooser) {
        return (JTabbedPane) findContainerUnder(comp, new JTabbedPaneFinder(chooser));
    }

    /**
     * Searches JTabbedPane object which component lies on.
     *
     * @param comp Component to find JTabbedPane under.
     * @return JTabbedPane instance or null if component was not found.
     */
    public static JTabbedPane findJTabbedPaneUnder(Component comp) {
        return findJTabbedPaneUnder(comp, new JTabbedPaneFinder());
    }

    /**
     * Waits JTabbedPane in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @return JTabbedPane instance.
     * @throws TimeoutExpiredException
     */
    public static JTabbedPane waitJTabbedPane(Container cont, ComponentChooser chooser, int index) {
        return (JTabbedPane) waitComponent(cont, new JTabbedPaneFinder(chooser), index);
    }

    /**
     * Waits 0'th JTabbedPane in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return JTabbedPane instance.
     * @throws TimeoutExpiredException
     */
    public static JTabbedPane waitJTabbedPane(Container cont, ComponentChooser chooser) {
        return waitJTabbedPane(cont, chooser, 0);
    }

    /**
     * Waits JTabbedPane by tab title.
     *
     * @param cont Container to search component in.
     * @param text Tooltip text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param itemIndex Tab index. if -1 selected one is checked.
     * @param index Ordinal component index.
     * @return JTabbedPane instance.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public static JTabbedPane waitJTabbedPane(Container cont, String text, boolean ce, boolean ccs, int itemIndex, int index) {
        return waitJTabbedPane(cont, new JTabbedPaneByItemFinder(text, itemIndex, new DefaultStringComparator(ce, ccs)), index);
    }

    /**
     * Waits JTabbedPane by tab title.
     *
     * @param cont Container to search component in.
     * @param text Tooltip text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param itemIndex Tab index. if -1 selected one is checked.
     * @return JTabbedPane instance.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public static JTabbedPane waitJTabbedPane(Container cont, String text, boolean ce, boolean ccs, int itemIndex) {
        return waitJTabbedPane(cont, text, ce, ccs, itemIndex, 0);
    }

    @Override
    public void setOutput(TestOut output) {
        super.setOutput(output.createErrorOutput());
        this.output = output;
    }

    @Override
    public TestOut getOutput() {
        return output;
    }

    @Override
    public void copyEnvironment(Operator anotherOperator) {
        super.copyEnvironment(anotherOperator);
        driver
                = (ListDriver) DriverManager.
                getDriver(DriverManager.LIST_DRIVER_ID,
                        getClass(),
                        anotherOperator.getProperties());
    }

    /**
     * Searches tab index by tab title.
     *
     * @param chooser page searching criteria
     * @return a page index.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @deprecated Use findPage(String) or findPage(String, StringComparator)
     */
    @Deprecated
    public int findPage(TabPageChooser chooser) {
        for (int i = 0; i < getTabCount(); i++) {
            if (chooser.checkPage(this, i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Searches tab index by tab title.
     *
     * @param title a page title.
     * @param comparator a string comparision algorithm
     * @return a page index.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @deprecated Use findPage(String) or findPage(String, StringComparator)
     */
    @Deprecated
    public int findPage(String title, StringComparator comparator) {
        return findPage(new BySubStringTabPageChooser(title, comparator));
    }

    /**
     * Searches tab index by tab title. isCaptionEqual method is used to compare
     * page title with match.
     *
     * @param title a page title.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @return a page index.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @deprecated Use findPage(String) or findPage(String, StringComparator)
     */
    @Deprecated
    public int findPage(String title, boolean ce, boolean ccs) {
        return findPage(title, new DefaultStringComparator(ce, ccs));
    }

    /**
     * Searches tab index by tab title. isCaptionEqual method is used to compare
     * page title with match. Uses StringComparator assigned to this object.
     *
     * @param title a page title.
     * @return a page index.
     */
    public int findPage(String title) {
        return findPage(title, getComparator());
    }

    /**
     * Selects tab.
     *
     * @param index a page ordinal index.
     * @return a root corresponding to the page.
     */
    public Component selectPage(int index) {
        output.printLine("Selecting " + index + "'th page in tabbed pane\n    :" + toStringSource());
        makeComponentVisible();
        driver.selectItem(this, index);
        if (getVerification()) {
            waitSelected(index);
        }
        return getComponentAt(index);
    }

    /**
     * Selects tab.
     *
     * @param chooser page searching criteria
     * @return a root corresponding to the page.
     */
    public Component selectPage(TabPageChooser chooser) {
        output.printLine("Selecting \"" + chooser.getDescription()
                + "\" page in tabbed pane\n    :" + toStringSource());
        return selectPage(waitPage(chooser));
    }

    /**
     * Selects tab.
     *
     * @param title a page title.
     * @param comparator a string comparision algorithm
     * @return a root corresponding to the page.
     */
    public Component selectPage(String title, StringComparator comparator) {
        return selectPage(new BySubStringTabPageChooser(title, comparator));
    }

    /**
     * Selects tab by tab title.
     *
     * @param title a page title.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @deprecated Use selectPage(String) or selectPage(String,
     * StringComparator)
     * @return a root corresponding to the page.
     */
    @Deprecated
    public Component selectPage(String title, boolean ce, boolean ccs) {
        return selectPage(title, new DefaultStringComparator(ce, ccs));
    }

    /**
     * Selects tab by tab title. Uses StringComparator assigned to this object.
     *
     * @param title a page title.
     * @return a root corresponding to the page.
     */
    public Component selectPage(String title) {
        return selectPage(title, getComparator());
    }

    /**
     * Wait for a page to exist.
     *
     * @param chooser page searching criteria
     * @return a page index.
     */
    public int waitPage(final TabPageChooser chooser) {
        waitState(new ComponentChooser() {
            @Override
            public boolean checkComponent(Component comp) {
                return findPage(chooser) > -1;
            }

            @Override
            public String getDescription() {
                return "Tabbed with " + chooser.getDescription() + " page.";
            }

            @Override
            public String toString() {
                return "JTabbedPaneOperator.waitPage.Action{description = " + getDescription() + '}';
            }
        });
        return findPage(chooser);
    }

    /**
     * Wait for a page to exist.
     *
     * @param title a page title.
     * @param comparator a string comparision algorithm
     * @return a page index.
     */
    public int waitPage(String title, StringComparator comparator) {
        return waitPage(new BySubStringTabPageChooser(title, comparator));
    }

    /**
     * Wait for a page to exist.
     *
     * @param title a page title.
     * @return a page index.
     */
    public int waitPage(String title) {
        return waitPage(title, getComparator());
    }

    /**
     * Waits for a page to be selected.
     *
     * @param pageIndex an index of a page to be selected.
     */
    public void waitSelected(final int pageIndex) {
        getOutput().printLine("Wait " + Integer.toString(pageIndex) + "'th page to be "
                + " selected in component \n    : "
                + toStringSource());
        getOutput().printGolden("Wait " + Integer.toString(pageIndex) + "'th page to be "
                + " selected");
        waitState(new ComponentChooser() {
            @Override
            public boolean checkComponent(Component comp) {
                return getSelectedIndex() == pageIndex;
            }

            @Override
            public String getDescription() {
                return Integer.toString(pageIndex) + "'th page has been selected";
            }

            @Override
            public String toString() {
                return "JTabbedPaneOperator.waitSelected.ComponentChooser{description = " + getDescription() + '}';
            }
        });
    }

    /**
     * Waits for a page to be selected.
     *
     * @param pageTitle a title of a page to be selected.
     */
    public void waitSelected(final String pageTitle) {
        waitSelected(findPage(pageTitle));
    }

    @Override
    public Hashtable<String, Object> getDump() {
        Hashtable<String, Object> result = super.getDump();
        if (((JTabbedPane) getSource()).getSelectedIndex() != -1) {
            result.put(SELECTED_PAGE_DPROP, ((JTabbedPane) getSource()).
                    getTitleAt(((JTabbedPane) getSource()).getSelectedIndex()));
        }
        String[] pages = new String[((JTabbedPane) getSource()).getTabCount()];
        for (int i = 0; i < ((JTabbedPane) getSource()).getTabCount(); i++) {
            pages[i] = ((JTabbedPane) getSource()).getTitleAt(i);
        }
        addToDump(result, PAGE_PREFIX_DPROP, pages);
        return result;
    }

    ////////////////////////////////////////////////////////
    //Mapping                                             //
    /**
     * Maps {@code JTabbedPane.addChangeListener(ChangeListener)} through queue
     */
    public void addChangeListener(final ChangeListener changeListener) {
        runMapping(new MapVoidAction("addChangeListener") {
            @Override
            public void map() {
                ((JTabbedPane) getSource()).addChangeListener(changeListener);
            }
        });
    }

    /**
     * Maps {@code JTabbedPane.addTab(String, Component)} through queue
     */
    public void addTab(final String string, final Component component) {
        runMapping(new MapVoidAction("addTab") {
            @Override
            public void map() {
                ((JTabbedPane) getSource()).addTab(string, component);
            }
        });
    }

    /**
     * Maps {@code JTabbedPane.addTab(String, Icon, Component)} through queue
     */
    public void addTab(final String string, final Icon icon, final Component component) {
        runMapping(new MapVoidAction("addTab") {
            @Override
            public void map() {
                ((JTabbedPane) getSource()).addTab(string, icon, component);
            }
        });
    }

    /**
     * Maps {@code JTabbedPane.addTab(String, Icon, Component, String)}
     * through queue
     */
    public void addTab(final String string, final Icon icon, final Component component, final String string1) {
        runMapping(new MapVoidAction("addTab") {
            @Override
            public void map() {
                ((JTabbedPane) getSource()).addTab(string, icon, component, string1);
            }
        });
    }

    /**
     * Maps {@code JTabbedPane.getBackgroundAt(int)} through queue
     */
    public Color getBackgroundAt(final int i) {
        return (runMapping(new MapAction<Color>("getBackgroundAt") {
            @Override
            public Color map() {
                return ((JTabbedPane) getSource()).getBackgroundAt(i);
            }
        }));
    }

    /**
     * Maps {@code JTabbedPane.getBoundsAt(int)} through queue
     */
    public Rectangle getBoundsAt(final int i) {
        return (runMapping(new MapAction<Rectangle>("getBoundsAt") {
            @Override
            public Rectangle map() {
                return ((JTabbedPane) getSource()).getBoundsAt(i);
            }
        }));
    }

    /**
     * Maps {@code JTabbedPane.getComponentAt(int)} through queue
     */
    public Component getComponentAt(final int i) {
        return (runMapping(new MapAction<Component>("getComponentAt") {
            @Override
            public Component map() {
                return ((JTabbedPane) getSource()).getComponentAt(i);
            }
        }));
    }

    /**
     * Maps {@code JTabbedPane.getDisabledIconAt(int)} through queue
     */
    public Icon getDisabledIconAt(final int i) {
        return (runMapping(new MapAction<Icon>("getDisabledIconAt") {
            @Override
            public Icon map() {
                return ((JTabbedPane) getSource()).getDisabledIconAt(i);
            }
        }));
    }

    /**
     * Maps {@code JTabbedPane.getForegroundAt(int)} through queue
     */
    public Color getForegroundAt(final int i) {
        return (runMapping(new MapAction<Color>("getForegroundAt") {
            @Override
            public Color map() {
                return ((JTabbedPane) getSource()).getForegroundAt(i);
            }
        }));
    }

    /**
     * Maps {@code JTabbedPane.getIconAt(int)} through queue
     */
    public Icon getIconAt(final int i) {
        return (runMapping(new MapAction<Icon>("getIconAt") {
            @Override
            public Icon map() {
                return ((JTabbedPane) getSource()).getIconAt(i);
            }
        }));
    }

    /**
     * Maps {@code JTabbedPane.getModel()} through queue
     */
    public SingleSelectionModel getModel() {
        return (runMapping(new MapAction<SingleSelectionModel>("getModel") {
            @Override
            public SingleSelectionModel map() {
                return ((JTabbedPane) getSource()).getModel();
            }
        }));
    }

    /**
     * Maps {@code JTabbedPane.getSelectedComponent()} through queue
     */
    public Component getSelectedComponent() {
        return (runMapping(new MapAction<Component>("getSelectedComponent") {
            @Override
            public Component map() {
                return ((JTabbedPane) getSource()).getSelectedComponent();
            }
        }));
    }

    /**
     * Maps {@code JTabbedPane.getSelectedIndex()} through queue
     */
    public int getSelectedIndex() {
        return (runMapping(new MapIntegerAction("getSelectedIndex") {
            @Override
            public int map() {
                return ((JTabbedPane) getSource()).getSelectedIndex();
            }
        }));
    }

    /**
     * Maps {@code JTabbedPane.getTabCount()} through queue
     */
    public int getTabCount() {
        return (runMapping(new MapIntegerAction("getTabCount") {
            @Override
            public int map() {
                return ((JTabbedPane) getSource()).getTabCount();
            }
        }));
    }

    /**
     * Maps {@code JTabbedPane.getTabPlacement()} through queue
     */
    public int getTabPlacement() {
        return (runMapping(new MapIntegerAction("getTabPlacement") {
            @Override
            public int map() {
                return ((JTabbedPane) getSource()).getTabPlacement();
            }
        }));
    }

    /**
     * Maps {@code JTabbedPane.getTabRunCount()} through queue
     */
    public int getTabRunCount() {
        return (runMapping(new MapIntegerAction("getTabRunCount") {
            @Override
            public int map() {
                return ((JTabbedPane) getSource()).getTabRunCount();
            }
        }));
    }

    /**
     * Maps {@code JTabbedPane.getTitleAt(int)} through queue
     */
    public String getTitleAt(final int i) {
        return (runMapping(new MapAction<String>("getTitleAt") {
            @Override
            public String map() {
                return ((JTabbedPane) getSource()).getTitleAt(i);
            }
        }));
    }

    /**
     * Maps {@code JTabbedPane.getUI()} through queue
     */
    public TabbedPaneUI getUI() {
        return (runMapping(new MapAction<TabbedPaneUI>("getUI") {
            @Override
            public TabbedPaneUI map() {
                return ((JTabbedPane) getSource()).getUI();
            }
        }));
    }

    /**
     * Maps {@code JTabbedPane.indexOfComponent(Component)} through queue
     */
    public int indexOfComponent(final Component component) {
        return (runMapping(new MapIntegerAction("indexOfComponent") {
            @Override
            public int map() {
                return ((JTabbedPane) getSource()).indexOfComponent(component);
            }
        }));
    }

    /**
     * Maps {@code JTabbedPane.indexOfTab(String)} through queue
     */
    public int indexOfTab(final String string) {
        return (runMapping(new MapIntegerAction("indexOfTab") {
            @Override
            public int map() {
                return ((JTabbedPane) getSource()).indexOfTab(string);
            }
        }));
    }

    /**
     * Maps {@code JTabbedPane.indexOfTab(Icon)} through queue
     */
    public int indexOfTab(final Icon icon) {
        return (runMapping(new MapIntegerAction("indexOfTab") {
            @Override
            public int map() {
                return ((JTabbedPane) getSource()).indexOfTab(icon);
            }
        }));
    }

    /**
     * Maps
     * {@code JTabbedPane.insertTab(String, Icon, Component, String, int)}
     * through queue
     */
    public void insertTab(final String string, final Icon icon, final Component component, final String string1, final int i) {
        runMapping(new MapVoidAction("insertTab") {
            @Override
            public void map() {
                ((JTabbedPane) getSource()).insertTab(string, icon, component, string1, i);
            }
        });
    }

    /**
     * Maps {@code JTabbedPane.isEnabledAt(int)} through queue
     */
    public boolean isEnabledAt(final int i) {
        return (runMapping(new MapBooleanAction("isEnabledAt") {
            @Override
            public boolean map() {
                return ((JTabbedPane) getSource()).isEnabledAt(i);
            }
        }));
    }

    /**
     * Maps {@code JTabbedPane.removeChangeListener(ChangeListener)}
     * through queue
     */
    public void removeChangeListener(final ChangeListener changeListener) {
        runMapping(new MapVoidAction("removeChangeListener") {
            @Override
            public void map() {
                ((JTabbedPane) getSource()).removeChangeListener(changeListener);
            }
        });
    }

    /**
     * Maps {@code JTabbedPane.removeTabAt(int)} through queue
     */
    public void removeTabAt(final int i) {
        runMapping(new MapVoidAction("removeTabAt") {
            @Override
            public void map() {
                ((JTabbedPane) getSource()).removeTabAt(i);
            }
        });
    }

    /**
     * Maps {@code JTabbedPane.setBackgroundAt(int, Color)} through queue
     */
    public void setBackgroundAt(final int i, final Color color) {
        runMapping(new MapVoidAction("setBackgroundAt") {
            @Override
            public void map() {
                ((JTabbedPane) getSource()).setBackgroundAt(i, color);
            }
        });
    }

    /**
     * Maps {@code JTabbedPane.setComponentAt(int, Component)} through queue
     */
    public void setComponentAt(final int i, final Component component) {
        runMapping(new MapVoidAction("setComponentAt") {
            @Override
            public void map() {
                ((JTabbedPane) getSource()).setComponentAt(i, component);
            }
        });
    }

    /**
     * Maps {@code JTabbedPane.setDisabledIconAt(int, Icon)} through queue
     */
    public void setDisabledIconAt(final int i, final Icon icon) {
        runMapping(new MapVoidAction("setDisabledIconAt") {
            @Override
            public void map() {
                ((JTabbedPane) getSource()).setDisabledIconAt(i, icon);
            }
        });
    }

    /**
     * Maps {@code JTabbedPane.setEnabledAt(int, boolean)} through queue
     */
    public void setEnabledAt(final int i, final boolean b) {
        runMapping(new MapVoidAction("setEnabledAt") {
            @Override
            public void map() {
                ((JTabbedPane) getSource()).setEnabledAt(i, b);
            }
        });
    }

    /**
     * Maps {@code JTabbedPane.setForegroundAt(int, Color)} through queue
     */
    public void setForegroundAt(final int i, final Color color) {
        runMapping(new MapVoidAction("setForegroundAt") {
            @Override
            public void map() {
                ((JTabbedPane) getSource()).setForegroundAt(i, color);
            }
        });
    }

    /**
     * Maps {@code JTabbedPane.setIconAt(int, Icon)} through queue
     */
    public void setIconAt(final int i, final Icon icon) {
        runMapping(new MapVoidAction("setIconAt") {
            @Override
            public void map() {
                ((JTabbedPane) getSource()).setIconAt(i, icon);
            }
        });
    }

    /**
     * Maps {@code JTabbedPane.setModel(SingleSelectionModel)} through queue
     */
    public void setModel(final SingleSelectionModel singleSelectionModel) {
        runMapping(new MapVoidAction("setModel") {
            @Override
            public void map() {
                ((JTabbedPane) getSource()).setModel(singleSelectionModel);
            }
        });
    }

    /**
     * Maps {@code JTabbedPane.setSelectedComponent(Component)} through queue
     */
    public void setSelectedComponent(final Component component) {
        runMapping(new MapVoidAction("setSelectedComponent") {
            @Override
            public void map() {
                ((JTabbedPane) getSource()).setSelectedComponent(component);
            }
        });
    }

    /**
     * Maps {@code JTabbedPane.setSelectedIndex(int)} through queue
     */
    public void setSelectedIndex(final int i) {
        runMapping(new MapVoidAction("setSelectedIndex") {
            @Override
            public void map() {
                ((JTabbedPane) getSource()).setSelectedIndex(i);
            }
        });
    }

    /**
     * Maps {@code JTabbedPane.setTabPlacement(int)} through queue
     */
    public void setTabPlacement(final int i) {
        runMapping(new MapVoidAction("setTabPlacement") {
            @Override
            public void map() {
                ((JTabbedPane) getSource()).setTabPlacement(i);
            }
        });
    }

    /**
     * Maps {@code JTabbedPane.setTitleAt(int, String)} through queue
     */
    public void setTitleAt(final int i, final String string) {
        runMapping(new MapVoidAction("setTitleAt") {
            @Override
            public void map() {
                ((JTabbedPane) getSource()).setTitleAt(i, string);
            }
        });
    }

    /**
     * Maps {@code JTabbedPane.setUI(TabbedPaneUI)} through queue
     */
    public void setUI(final TabbedPaneUI tabbedPaneUI) {
        runMapping(new MapVoidAction("setUI") {
            @Override
            public void map() {
                ((JTabbedPane) getSource()).setUI(tabbedPaneUI);
            }
        });
    }

    //End of mapping                                      //
    ////////////////////////////////////////////////////////
    /**
     * Specifies criteria for a tab page searching.
     */
    public interface TabPageChooser {

        /**
         * Should be true if a page is good.
         *
         * @param oper Operator used to search item.
         * @param index Index of a page be checked.
         * @return true if a page fits the criteria.
         */
        public boolean checkPage(JTabbedPaneOperator oper, int index);

        /**
         * Page description.
         *
         * @return a description.
         */
        public String getDescription();
    }

    /**
     * Exception is thrown if a nonexistent page is trying to be selected.
     */
    public class NoSuchPageException extends JemmyInputException {

        private static final long serialVersionUID = 42L;

        /**
         * Constructor.
         *
         * @param item nonexistent page title.
         */
        public NoSuchPageException(String item) {
            super("No such page as \"" + item + "\"", getSource());
        }
    }

    /**
     * Allows to find component by page title.
     */
    public static class JTabbedPaneByItemFinder implements ComponentChooser {

        String title;
        int itemIndex;
        StringComparator comparator;

        /**
         * Constructs JTabbedPaneByItemFinder.
         *
         * @param lb a text pattern
         * @param ii page index to check. If equal to -1, selected page is
         * checked.
         * @param comparator specifies string comparision algorithm.
         */
        public JTabbedPaneByItemFinder(String lb, int ii, StringComparator comparator) {
            title = lb;
            itemIndex = ii;
            this.comparator = comparator;
        }

        /**
         * Constructs JTabbedPaneByItemFinder.
         *
         * @param lb a text pattern
         * @param ii page index to check. If equal to -1, selected page is
         * checked.
         */
        public JTabbedPaneByItemFinder(String lb, int ii) {
            this(lb, ii, Operator.getDefaultStringComparator());
        }

        @Override
        public boolean checkComponent(Component comp) {
            if (comp instanceof JTabbedPane) {
                if (title == null) {
                    return true;
                }
                JTabbedPaneOperator tpo = new JTabbedPaneOperator((JTabbedPane) comp);
                if (tpo.getTabCount() > itemIndex) {
                    int ii = itemIndex;
                    if (ii == -1) {
                        ii = tpo.getSelectedIndex();
                        if (ii == -1) {
                            return false;
                        }
                    }
                    return (comparator.equals(tpo.getTitleAt(ii),
                            title));
                }
            }
            return false;
        }

        @Override
        public String getDescription() {
            return ("JTabbedPane with text \"" + title + "\" in "
                    + itemIndex + "'th item");
        }

        @Override
        public String toString() {
            return "JTabbedPaneByItemFinder{" + "title=" + title + ", itemIndex=" + itemIndex + ", comparator=" + comparator + '}';
        }
    }

    /**
     * Checks component type.
     */
    public static class JTabbedPaneFinder extends Finder {

        /**
         * Constructs JTabbedPaneFinder.
         *
         * @param sf other searching criteria.
         */
        public JTabbedPaneFinder(ComponentChooser sf) {
            super(JTabbedPane.class, sf);
        }

        /**
         * Constructs JTabbedPaneFinder.
         */
        public JTabbedPaneFinder() {
            super(JTabbedPane.class);
        }
    }

    private static class BySubStringTabPageChooser implements TabPageChooser {

        String title;
        StringComparator comparator;

        public BySubStringTabPageChooser(String title, StringComparator comparator) {
            this.title = title;
            this.comparator = comparator;
        }

        @Override
        public boolean checkPage(JTabbedPaneOperator oper, int index) {
            return (comparator.equals(oper.getTitleAt(index),
                    title));
        }

        @Override
        public String getDescription() {
            return "Page having \"" + title + "\" title.";
        }

        @Override
        public String toString() {
            return "BySubStringTabPageChooser{" + "title=" + title + ", comparator=" + comparator + '}';
        }
    }

}
