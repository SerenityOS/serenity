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
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.plaf.TreeUI;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.ComponentSearcher;
import org.netbeans.jemmy.JemmyException;
import org.netbeans.jemmy.JemmyInputException;
import org.netbeans.jemmy.Outputable;
import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.TimeoutExpiredException;
import org.netbeans.jemmy.Timeoutable;
import org.netbeans.jemmy.Timeouts;
import org.netbeans.jemmy.Waitable;
import org.netbeans.jemmy.Waiter;
import org.netbeans.jemmy.drivers.DriverManager;
import org.netbeans.jemmy.drivers.TreeDriver;
import org.netbeans.jemmy.util.EmptyVisualizer;

/**
 * <BR><BR>Timeouts used: <BR>
 * JTreeOperator.WaitNodeExpandedTimeout - time to wait node expanded <BR>
 * JTreeOperator.WaitNodeCollapsedTimeout - time to wait node collapsed <BR>
 * JTreeOperator.WaitAfterNodeExpandedTimeout - time to to sleep after node
 * expanded <BR>
 * JTreeOperator.WaitNextNodeTimeout - time to wait next node displayed <BR>
 * JTreeOperator.WaitNodeVisibleTimeout - time to wait node visible <BR>
 * JTreeOperator.BeforeEditTimeout - time to sleep before edit click <BR>
 * JTreeOperator.WaitEditingTimeout - time to wait node editing <BR>
 * ComponentOperator.WaitComponentTimeout - time to wait component displayed
 * <BR>
 * ComponentOperator.WaitStateTimeout - time to wait for path to be expanded,
 * collapsed, selected, time to wait for a text in a row <BR>
 * WindowWaiter.WaitWindowTimeout - time to wait popup window displayed <BR>
 * JScrollBarOperator.WholeScrollTimeout - time for the whole scrolling <BR>.
 *
 * @see org.netbeans.jemmy.Timeouts
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 *
 */
public class JTreeOperator extends JComponentOperator
        implements Timeoutable, Outputable {

    /**
     * Identifier for a "root" property.
     *
     * @see #getDump
     */
    public static final String ROOT_DPROP = "Root";

    /**
     * Identifier for a "node" properties.
     *
     * @see #getDump
     */
    public static final String NODE_PREFIX_DPROP = "Node";

    /**
     * Identifier for a "first selected" property.
     *
     * @see #getDump
     */
    public static final String SELECTION_FIRST_DPROP = "First selected";

    /**
     * Identifier for a "last selected" property.
     *
     * @see #getDump
     */
    public static final String SELECTION_LAST_DPROP = "Last selected";

    private final static long WAIT_NODE_EXPANDED_TIMEOUT = 60000;
    private final static long WAIT_NODE_COLLAPSED_TIMEOUT = 60000;
    private final static long WAIT_AFTER_NODE_EXPANDED_TIMEOUT = 0;
    private final static long WAIT_NEXT_NODE_TIMEOUT = 60000;
    private final static long WAIT_NODE_VISIBLE_TIMEOUT = 60000;
    private final static long BEFORE_EDIT_TIMEOUT = 1000;
    private final static long WAIT_EDITING_TIMEOUT = 60000;

    private TestOut output;
    private Timeouts timeouts;
    private TreeDriver driver;

    /**
     * Constructor.
     *
     * @param b a component
     */
    public JTreeOperator(JTree b) {
        super(b);
        driver = DriverManager.getTreeDriver(getClass());
    }

    /**
     * Constructs a JTreeOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     */
    public JTreeOperator(ContainerOperator<?> cont, ComponentChooser chooser, int index) {
        this((JTree) cont.
                waitSubComponent(new JTreeFinder(chooser),
                        index));
        copyEnvironment(cont);
    }

    /**
     * Constructs a JTreeOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     */
    public JTreeOperator(ContainerOperator<?> cont, ComponentChooser chooser) {
        this(cont, chooser, 0);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @param text Text of a row which is currently selected.
     * @param row a row index to check text in. If equals to -1, selected row is
     * checked.
     * @param index Ordinal component index.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public JTreeOperator(ContainerOperator<?> cont, String text, int row, int index) {
        this((JTree) waitComponent(cont,
                new JTreeByItemFinder(text, row,
                        cont.getComparator()),
                index));
        copyEnvironment(cont);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @param text Text of a row which is currently selected.
     * @param index Ordinal component index.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public JTreeOperator(ContainerOperator<?> cont, String text, int index) {
        this(cont, text, -1, index);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @param text Text of a row which is currently selected.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public JTreeOperator(ContainerOperator<?> cont, String text) {
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
    public JTreeOperator(ContainerOperator<?> cont, int index) {
        this((JTree) waitComponent(cont,
                new JTreeFinder(),
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
    public JTreeOperator(ContainerOperator<?> cont) {
        this(cont, 0);
    }

    /**
     * Searches JTree in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @return JTree instance or null if component was not found.
     */
    public static JTree findJTree(Container cont, ComponentChooser chooser, int index) {
        return (JTree) findComponent(cont, new JTreeFinder(chooser), index);
    }

    /**
     * Searches 0'th JTree in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return JTree instance or null if component was not found.
     */
    public static JTree findJTree(Container cont, ComponentChooser chooser) {
        return findJTree(cont, chooser, 0);
    }

    /**
     * Searches JTree by item.
     *
     * @param cont Container to search component in.
     * @param text Item text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param rowIndex Index of row to compare text. If -1, selected row is
     * checked.
     * @param index Ordinal component index.
     * @return JTree instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static JTree findJTree(Container cont, String text, boolean ce, boolean ccs, int rowIndex, int index) {
        return findJTree(cont, new JTreeByItemFinder(text, rowIndex, new DefaultStringComparator(ce, ccs)), index);
    }

    /**
     * Searches JTree by item.
     *
     * @param cont Container to search component in.
     * @param text Item text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param rowIndex Index of row to compare text. If -1, selected row is
     * checked.
     * @return JTree instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static JTree findJTree(Container cont, String text, boolean ce, boolean ccs, int rowIndex) {
        return findJTree(cont, text, ce, ccs, rowIndex, 0);
    }

    /**
     * Waits JTree in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @return JTree instance or null if component was not found.
     * @throws TimeoutExpiredException
     */
    public static JTree waitJTree(Container cont, ComponentChooser chooser, int index) {
        return (JTree) waitComponent(cont, new JTreeFinder(chooser), index);
    }

    /**
     * Waits 0'th JTree in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return JTree instance or null if component was not found.
     * @throws TimeoutExpiredException
     */
    public static JTree waitJTree(Container cont, ComponentChooser chooser) {
        return waitJTree(cont, chooser, 0);
    }

    /**
     * Waits JTree by item.
     *
     * @param cont Container to search component in.
     * @param text Item text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param rowIndex Index of row to compare text. If -1, selected row is
     * checked.
     * @param index Ordinal component index.
     * @return JTree instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public static JTree waitJTree(Container cont, String text, boolean ce, boolean ccs, int rowIndex, int index) {
        return waitJTree(cont, new JTreeByItemFinder(text, rowIndex, new DefaultStringComparator(ce, ccs)), index);
    }

    /**
     * Waits JTree by item.
     *
     * @param cont Container to search component in.
     * @param text Item text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param rowIndex Index of row to compare text. If -1, selected row is
     * checked.
     * @return JTree instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public static JTree waitJTree(Container cont, String text, boolean ce, boolean ccs, int rowIndex) {
        return waitJTree(cont, text, ce, ccs, rowIndex, 0);
    }

    static {
        Timeouts.initDefault("JTreeOperator.WaitNodeExpandedTimeout", WAIT_NODE_EXPANDED_TIMEOUT);
        Timeouts.initDefault("JTreeOperator.WaitNodeCollapsedTimeout", WAIT_NODE_COLLAPSED_TIMEOUT);
        Timeouts.initDefault("JTreeOperator.WaitAfterNodeExpandedTimeout", WAIT_AFTER_NODE_EXPANDED_TIMEOUT);
        Timeouts.initDefault("JTreeOperator.WaitNextNodeTimeout", WAIT_NEXT_NODE_TIMEOUT);
        Timeouts.initDefault("JTreeOperator.WaitNodeVisibleTimeout", WAIT_NODE_VISIBLE_TIMEOUT);
        Timeouts.initDefault("JTreeOperator.BeforeEditTimeout", BEFORE_EDIT_TIMEOUT);
        Timeouts.initDefault("JTreeOperator.WaitEditingTimeout", WAIT_EDITING_TIMEOUT);
    }

    @Override
    public void setTimeouts(Timeouts times) {
        this.timeouts = times;
        super.setTimeouts(timeouts);
    }

    @Override
    public Timeouts getTimeouts() {
        return timeouts;
    }

    @Override
    public void setOutput(TestOut out) {
        output = out;
        super.setOutput(output.createErrorOutput());
    }

    @Override
    public TestOut getOutput() {
        return output;
    }

    @Override
    public void copyEnvironment(Operator anotherOperator) {
        super.copyEnvironment(anotherOperator);
        driver
                = (TreeDriver) DriverManager.
                getDriver(DriverManager.TREE_DRIVER_ID,
                        getClass(),
                        anotherOperator.getProperties());
    }

    /**
     * Expands path.
     *
     * @param path a path to be expanded.
     * @throws TimeoutExpiredException
     */
    public void doExpandPath(TreePath path) {
        if (path != null) {
            output.printLine("Expanding \"" + path.getPathComponent(path.getPathCount() - 1).toString()
                    + "\" node");
            output.printGolden("Expanding \"" + path.getPathComponent(path.getPathCount() - 1).toString()
                    + "\" node");
            driver.expandItem(this, getRowForPath(path));
            waitExpanded(path);
        } else {
            throw (new NoSuchPathException());
        }
    }

    /**
     * Expands path on row.
     *
     * @param row a row index to be expanded.
     * @throws TimeoutExpiredException
     */
    public void doExpandRow(int row) {
        output.printLine("Expanding " + Integer.toString(row)
                + " row");
        output.printGolden("Expanding " + Integer.toString(row)
                + " row");
        driver.expandItem(this, row);
        waitExpanded(row);
    }

    /**
     * Ensures that the node identified by path is currently viewable.
     *
     * @param path a path to be made visible.
     * @throws TimeoutExpiredException
     */
    public void doMakeVisible(TreePath path) {
        if (path != null) {
            output.printLine("Making \"" + path.toString() + "\" path visible");
            output.printGolden("Making path visible");
            makeVisible(path);
            waitVisible(path);
        } else {
            throw (new NoSuchPathException());
        }
    }

    /**
     * Returns number of child.
     *
     * @param node a node to count children of.
     * @return a number of children.
     */
    public int getChildCount(final Object node) {
        return runMapping(new MapIntegerAction("getChildCount") {
            @Override
            public int map() {
                return ((JTree) getSource()).getModel().getChildCount(node);
            }
        });
    }

    /**
     * Returns node children.
     *
     * @param node a node to get children of.
     * @return an array of node children.
     */
    public Object[] getChildren(final Object node) {
        return (Object[]) runMapping(new MapAction<Object>("getChildren") {
            @Override
            public Object map() {
                TreeModel md = ((JTree) getSource()).getModel();
                Object[] result = new Object[md.getChildCount(node)];
                for (int i = 0; i < md.getChildCount(node); i++) {
                    result[i] = md.getChild(node, i);
                }
                return result;
            }
        });
    }

    /**
     * Returns node child.
     *
     * @param node a node to get a child of.
     * @param index a child index.
     * @return a node child.
     */
    public Object getChild(final Object node, final int index) {
        return runMapping(new MapAction<Object>("getChild") {
            @Override
            public Object map() {
                return ((JTree) getSource()).getModel().getChild(node, index);
            }
        });
    }

    /**
     * Returns number of child.
     *
     * @param path a path indicating a node to count children of.
     * @return a number of children.
     */
    public int getChildCount(TreePath path) {
        if (path != null) {
            return (getChildCount(path.
                    getLastPathComponent()));
        } else {
            throw (new NoSuchPathException());
        }
    }

    /**
     * Constructs new path from a path and index's subnode of it last node.
     *
     * @param path a path indicating a node to get a child of.
     * @param index a child node index.
     * @return a number of children.
     */
    public TreePath getChildPath(TreePath path, int index) {
        if (path != null) {
            return (path.
                    pathByAddingChild(getChild(path.
                            getLastPathComponent(), index)));
        } else {
            throw (new NoSuchPathException());
        }
    }

    /**
     * Constructs new paths from a path and all subnodes of it last node.
     *
     * @param path a path indicating a node to get children of.
     * @return a number of children.
     */
    public TreePath[] getChildPaths(TreePath path) {
        if (path != null) {
            Object[] children = getChildren(path.
                    getLastPathComponent());
            TreePath[] result = new TreePath[children.length];
            for (int i = 0; i < children.length; i++) {
                result[i] = path.
                        pathByAddingChild(children[i]);
            }
            return result;
        } else {
            throw (new NoSuchPathException());
        }
    }

    /**
     * Returns the root of the tree.
     *
     * @return tree root.
     * @throws TimeoutExpiredException
     */
    public Object getRoot() {
        Waiter<Object, Void> rootWaiter = new Waiter<>(new Waitable<Object, Void>() {
            @Override
            public Object actionProduced(Void obj) {
                Object root = getModel().getRoot();
                if (root == null || root.toString().equals("null")) {
                    return null;
                } else {
                    return root;
                }
            }

            @Override
            public String getDescription() {
                return "Wait root node";
            }

            @Override
            public String toString() {
                return "JTreeOperator.getRoot.Waitable{description = " + getDescription() + '}';
            }
        });
        rootWaiter.setTimeoutsToCloneOf(timeouts, "JTreeOperator.WaitNodeVisibleTimeout");
        rootWaiter.setOutput(output.createErrorOutput());
        try {
            return rootWaiter.waitAction(null);
        } catch (InterruptedException e) {
            output.printStackTrace(e);
            return null;
        }
    }

    /**
     * Searches path in tree.
     *
     * @param chooser TreePathChooser implementation.
     * @return a path fitting the criteria.
     * @see TreePathChooser
     * @see #findPath
     * @throws TimeoutExpiredException
     */
    public TreePath findPath(TreePathChooser chooser) {
        output.printLine("Search for a tree path " + chooser.getDescription());
        output.printGolden("Search for a tree path");
        TreePath rootPath = new TreePath(getRoot());
        if (chooser.checkPath(rootPath, 0)) {
            return rootPath;
        }
        Waiter<Object[], Object[]> loadedWaiter = new Waiter<>(new Waitable<Object[], Object[]>() {
            // fields used in getDescription() method
            TreePath currentPath;
            String requestedPath;

            @Override
            public Object[] actionProduced(Object[] obj) {
                TreePathChooser chsr = (TreePathChooser) obj[0];
                requestedPath = chsr.getDescription();
                TreePath path = (TreePath) obj[1];
                currentPath = path;
                Object[] result = new Object[2];
                Object[] children = getChildren(path.getLastPathComponent());
                for (int j = 0; j < children.length; j++) {
                    result[0] = path.pathByAddingChild(children[j]);
                    if (chsr.checkPath((TreePath) result[0], j)) {
                        result[1] = Boolean.TRUE;
                        return result;
                    }
                    if (chsr.hasAsParent((TreePath) result[0], j)) {
                        result[1] = Boolean.FALSE;
                        return result;
                    }
                }
                return null;
            }

            @Override
            public String getDescription() {
                return "Wait next node loaded under parent " + currentPath + " when requested was " + requestedPath;
            }

            @Override
            public String toString() {
                return "JTreeOperator.findPath.Waitable{description = " + getDescription() + '}';
            }
        });
        loadedWaiter.setTimeoutsToCloneOf(timeouts, "JTreeOperator.WaitNextNodeTimeout");
        loadedWaiter.setOutput(output.createErrorOutput());
        return findPathPrimitive(rootPath, chooser, loadedWaiter);
    }

    /**
     * Searches index'th row by row chooser.
     *
     * @param chooser a path searching criteria.
     * @param index a child index.
     * @return Row index or -1 if search was insuccessful.
     * @see JTreeOperator.TreeRowChooser
     */
    public int findRow(TreeRowChooser chooser, int index) {
        int count = 0;
        for (int i = 0; i < getRowCount(); i++) {
            if (chooser.checkRow(this, i)) {
                if (count == index) {
                    return i;
                } else {
                    count++;
                }
            }
        }
        return -1;
    }

    /**
     * Searches a row by row chooser.
     *
     * @param chooser a path searching criteria.
     * @return Row index or -1 if search was insuccessful.
     * @see JTreeOperator.TreeRowChooser
     */
    public int findRow(TreeRowChooser chooser) {
        return findRow(chooser, 0);
    }

    /**
     * Searches index'th row by substring.
     *
     * @param item Substring.
     * @param comparator a string comparision algorithm
     * @param index an ordinal row index between ones matching the criteria
     * @return Row index or -1 if search was insuccessful.
     */
    public int findRow(String item, StringComparator comparator, int index) {
        return findRow(new BySubStringTreeRowChooser(item, comparator), index);
    }

    /**
     * Searches index'th row by substring.
     *
     * @param item Substring.
     * @param ce Compare exactly
     * @param cc Compare case sensitivelly.
     * @param index an ordinal row index between ones matching the criteria
     * @return Row index or -1 if search was insuccessful.
     * @deprecated Use findRow(String, int) or findRow(String, StringComparator,
     * int)
     */
    @Deprecated
    public int findRow(String item, boolean ce, boolean cc, int index) {
        return (findRow(item,
                new DefaultStringComparator(ce, cc),
                index));
    }

    /**
     * Searches index'th row by substring. Uses StringComparator assigned to
     * this object.
     *
     * @param item Substring.
     * @param index an ordinal row index between ones matching the criteria
     * @return Row index or -1 if search was insuccessful.
     */
    public int findRow(String item, int index) {
        return (findRow(item,
                getComparator(),
                index));
    }

    /**
     * Searches a row by substring.
     *
     * @param item Substring.
     * @param comparator a string comparision algorithm
     * @return Row index or -1 if search was insuccessful.
     */
    public int findRow(String item, StringComparator comparator) {
        return findRow(item, comparator, 0);
    }

    /**
     * Searches a row by substring.
     *
     * @param item Substring.
     * @param ce Compare exactly
     * @param cc Compare case sensitivelly.
     * @return Row index or -1 if search was insuccessful.
     * @deprecated Use findRow(String) or findRow(String, StringComparator)
     */
    @Deprecated
    public int findRow(String item, boolean ce, boolean cc) {
        return findRow(item, ce, cc, 0);
    }

    /**
     * Searches a row by substring. Uses StringComparator assigned to this
     * object.
     *
     * @param item Substring.
     * @return Row index or -1 if search was insuccessful.
     */
    public int findRow(String item) {
        return (findRow(item,
                getComparator(),
                0));
    }

    /**
     * Searches index'th row by rendered component.
     *
     * @param chooser Component checking object.
     * @param index an ordinal row index between ones matching the criteria
     * @return Row index or -1 if search was insuccessful.
     */
    public int findRow(ComponentChooser chooser, int index) {
        return findRow(new ByRenderedComponentTreeRowChooser(chooser), index);
    }

    /**
     * Searches a row by rendered component.
     *
     * @param chooser Component checking object.
     * @return Row index or -1 if search was insuccessful.
     */
    public int findRow(ComponentChooser chooser) {
        return findRow(chooser, 0);
    }

    /**
     * Searches path in tree. Can be used to find one of the nodes with the same
     * text. Example:<BR>
     * <pre>
     * root
     * +-+node
     * | +--subnode
     * +-+node
     * | +--subnode
     * | +--subnode
     * ...
     * String[] names = {"node", "subnode"};<BR>
     * int[] indexes = {1, 0};<BR>
     * </pre> TreePath path = findPath(names, indexes, true, true);<BR>
     * "path" will points to the second (from the top) "subnode" node.
     *
     * @param names Node texts array.
     * @param indexes Nodes indexes.
     * @param comparator a string comparision algorithm
     * @return a tree path matching the criteria
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @see #findPath
     * @throws TimeoutExpiredException
     */
    public TreePath findPath(String[] names, int[] indexes, StringComparator comparator) {
        return findPath(new StringArrayPathChooser(names, indexes, comparator));
    }

    /**
     * Searches path in tree. Can be used to find one of the nodes with the same
     * text. Example:<BR>
     * <pre>
     * root
     * +-+node
     * | +--subnode
     * +-+node
     * | +--subnode
     * | +--subnode
     * ...
     * String[] names = {"node", "subnode"};<BR>
     * int[] indexes = {1, 0};<BR>
     * </pre> TreePath path = findPath(names, indexes, true, true);<BR>
     * "path" will points to the second (from the top) "subnode" node.
     *
     * @param names Node texts array.
     * @param indexes Nodes indexes.
     * @param ce Compare exactly.
     * @param ccs Compare case sensitively.
     * @return a tree path matching the criteria
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @see #findPath
     * @throws TimeoutExpiredException
     * @deprecated Use findPath(String[], int[]) or findCellRow(String[], int[],
     * StringComparator)
     */
    @Deprecated
    public TreePath findPath(String[] names, int[] indexes, boolean ce, boolean ccs) {
        return findPath(names, indexes, new DefaultStringComparator(ce, ccs));
    }

    /**
     * Searches path in tree. Uses StringComparator assigned to this object.
     *
     * @param names Node texts array.
     * @param indexes Nodes indexes.
     * @return a tree path matching the criteria
     * @see #findPath
     * @throws TimeoutExpiredException
     */
    public TreePath findPath(String[] names, int[] indexes) {
        return findPath(names, indexes, getComparator());
    }

    /**
     * Searches path in tree.
     *
     * @param names Node texts array.
     * @param comparator a string comparision algorithm
     * @return a tree path matching the criteria
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @see #findPath
     * @throws TimeoutExpiredException
     */
    public TreePath findPath(String[] names, StringComparator comparator) {
        int[] indexes = new int[0];
        return findPath(names, indexes, comparator);
    }

    /**
     * Searches path in tree.
     *
     * @param names Node texts array.
     * @param ce Compare exactly.
     * @param ccs Compare case sensitively.
     * @return a tree path matching the criteria
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @see #findPath
     * @throws TimeoutExpiredException
     * @deprecated Use findPath(String[]) or findCellRow(String[],
     * StringComparator)
     */
    @Deprecated
    public TreePath findPath(String[] names, boolean ce, boolean ccs) {
        int[] indexes = new int[0];
        return findPath(names, indexes, ce, ccs);
    }

    /**
     * Searches path in tree. Uses StringComparator assigned to this object.
     *
     * @param names Node texts array.
     * @return a tree path matching the criteria
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @see #findPath
     * @throws TimeoutExpiredException
     */
    public TreePath findPath(String[] names) {
        int[] indexes = new int[0];
        return findPath(names, indexes, getComparator());
    }

    /**
     * Searches path in tree.
     *
     * @param path String representing tree path. Path components should be
     * devided by "delim" parameter.
     * @param indexes String representing indexes to search path components.
     * Indexes should be devided by "delim" parameter.
     * @param delim Path components delimiter.
     * @param comparator a string comparision algorithm
     * @return a tree path matching the criteria
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @see #findPath
     * @throws TimeoutExpiredException
     */
    public TreePath findPath(String path, String indexes, String delim, StringComparator comparator) {
        String[] indexStrings = parseString(indexes, delim);
        int[] indInts = new int[indexStrings.length];
        for (int i = 0; i < indexStrings.length; i++) {
            indInts[i] = Integer.parseInt(indexStrings[i]);
        }
        return findPath(parseString(path, delim), indInts, comparator);
    }

    /**
     * Searches path in tree.
     *
     * @param path String representing tree path. Path components should be
     * devided by "delim" parameter.
     * @param indexes String representing indexes to search path components.
     * Indexes should be devided by "delim" parameter.
     * @param delim Path components delimiter.
     * @param ce Compare exactly.
     * @param ccs Compare case sensitively.
     * @return a tree path matching the criteria
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @see #findPath
     * @throws TimeoutExpiredException
     * @deprecated Use findPath(String, String, String) or findCellRow(String,
     * String, String, StringComparator)
     */
    @Deprecated
    public TreePath findPath(String path, String indexes, String delim, boolean ce, boolean ccs) {
        return findPath(path, indexes, delim, new DefaultStringComparator(ce, ccs));
    }

    /**
     * Searches path in tree. Uses StringComparator assigned to this object.
     *
     * @param path String representing tree path. Path components should be
     * devided by "delim" parameter.
     * @param indexes String representing indexes to search path components.
     * Indexes should be devided by "delim" parameter.
     * @param delim Path components delimiter.
     * @return a tree path matching the criteria
     * @see #findPath
     * @throws TimeoutExpiredException
     */
    public TreePath findPath(String path, String indexes, String delim) {
        return findPath(path, indexes, delim, getComparator());
    }

    /**
     * Searches path in tree.
     *
     * @param path String representing tree path. Path components should be
     * devided by "delim" parameter.
     * @param delim Path components delimiter.
     * @param comparator a string comparision algorithm
     * @return a tree path matching the criteria
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @see #findPath
     * @throws TimeoutExpiredException
     */
    public TreePath findPath(String path, String delim, StringComparator comparator) {
        return findPath(parseString(path, delim), comparator);
    }

    /**
     * Searches path in tree.
     *
     * @param path String representing tree path.
     * @param comparator a string comparision algorithm
     * @return a tree path matching the criteria
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @see #findPath
     * @throws TimeoutExpiredException
     */
    public TreePath findPath(String path, StringComparator comparator) {
        return findPath(parseString(path), comparator);
    }

    /**
     * Searches path in tree.
     *
     * @param path String representing tree path. Path components should be
     * devided by "delim" parameter.
     * @param delim Path components delimiter.
     * @param ce Compare exactly.
     * @param ccs Compare case sensitively.
     * @return a tree path matching the criteria
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @see #findPath
     * @throws TimeoutExpiredException
     * @deprecated Use findPath(String, String) or findCellRow(String, String,
     * StringComparator)
     */
    @Deprecated
    public TreePath findPath(String path, String delim, boolean ce, boolean ccs) {
        return findPath(parseString(path, delim), ce, ccs);
    }

    /**
     * Searches path in tree. Uses StringComparator assigned to this object.
     *
     * @param path String representing tree path. Path components should be
     * devided by "delim" parameter.
     * @param delim Path components delimiter.
     * @return a tree path matching the criteria
     * @see #findPath
     * @throws TimeoutExpiredException
     */
    public TreePath findPath(String path, String delim) {
        return findPath(parseString(path, delim));
    }

    /**
     * Searches path in tree. Uses StringComparator assigned to this object.
     * Uses PathParser assigned to this object.
     *
     * @param path String representing tree path.
     * @return a tree path matching the criteria
     * @see #findPath
     * @throws TimeoutExpiredException
     */
    public TreePath findPath(String path) {
        return findPath(parseString(path));
    }

    /**
     * Ensures that the node identified by the specified path is collapsed and
     * viewable.
     *
     * @param path a path to collapse.
     * @throws TimeoutExpiredException
     */
    public void doCollapsePath(TreePath path) {
        if (path != null) {
            output.printLine("Collapsing \"" + path.toString() + "\" path");
            output.printGolden("Collapsing path");
            driver.collapseItem(this, getRowForPath(path));
            if (getVerification()) {
                waitCollapsed(path);
            }
        } else {
            throw (new NoSuchPathException());
        }
    }

    /**
     * Ensures that the node in the specified row is collapsed.
     *
     * @param row a row index to collapse.
     * @throws TimeoutExpiredException
     */
    public void doCollapseRow(int row) {
        output.printLine("Collapsing \"" + Integer.toString(row) + "\" row");
        output.printGolden("Collapsing path");
        driver.collapseItem(this, row);
        if (getVerification()) {
            waitCollapsed(row);
        }
    }

    /**
     * Selects the path.
     *
     * @param path a path to select.
     */
    public void selectPath(final TreePath path) {
        if (path != null) {
            output.printLine("Selecting \"" + path.toString() + "\" path");
            output.printGolden("Selecting path");
            scrollToPath(path);
            getQueueTool().invokeSmoothly(new QueueTool.QueueAction<Void>("Path selecting") {
                @Override
                public Void launch() {
                    driver.selectItem(JTreeOperator.this, getRowForPath(path));
                    return null;
                }
            });
            if (getVerification()) {
                waitSelected(path);
            }
        } else {
            throw (new NoSuchPathException());
        }
    }

    /**
     * Selects the node in the specified row.
     *
     * @param row an index of row to select.
     */
    public void selectRow(int row) {
        output.printLine("Collapsing \"" + Integer.toString(row) + "\" row");
        output.printGolden("Collapsing path");
        driver.selectItem(this, row);
        if (getVerification()) {
            waitSelected(row);
        }
    }

    /**
     * Selects some pathes. If verification mode is on, checks that right paths
     * have been selected.
     *
     * @param paths a paths to select.
     */
    public void selectPaths(TreePath[] paths) {
        output.printLine("Selecting paths:");
        int[] rows = new int[paths.length];
        for (int i = 0; i < paths.length; i++) {
            output.printLine("    " + paths[i].toString());
            rows[i] = getRowForPath(paths[i]);
        }
        output.printGolden("Selecting paths");
        driver.selectItems(this, rows);
        if (getVerification()) {
            waitSelected(paths);
        }
    }

    /**
     * Retuns points which can be used to click on path.
     *
     * @param path a tree path to click on.
     * @return a Point in component's coordinate system.
     */
    public Point getPointToClick(TreePath path) {
        if (path != null) {
            Rectangle rect = getPathBounds(path);
            if (rect != null) {
                return (new Point((int) (rect.getX() + rect.getWidth() / 2),
                        (int) (rect.getY() + rect.getHeight() / 2)));
            } else {
                throw (new NoSuchPathException(path));
            }
        } else {
            throw (new NoSuchPathException());
        }
    }

    /**
     * Retuns points which can be used to click on path.
     *
     * @param row a row index to click on.
     * @return a Point in component's coordinate system.
     */
    public Point getPointToClick(int row) {
        Rectangle rect = getRowBounds(row);
        if (rect != null) {
            return (new Point((int) (rect.getX() + rect.getWidth() / 2),
                    (int) (rect.getY() + rect.getHeight() / 2)));
        } else {
            throw (new NoSuchPathException(row));
        }
    }

    /**
     * Clicks on the node.
     *
     * @param path a path to click on.
     * @param clickCount a number of clicks
     * @param mouseButton InputEvent.BUTTON1/2/3_MASK value
     * @param modifiers Combination of InputEvent.*_MASK values
     * @throws TimeoutExpiredException
     */
    public void clickOnPath(TreePath path, int clickCount, int mouseButton, int modifiers) {
        if (path != null) {
            output.printLine("Click on \"" + path.toString()
                    + "\" path");
            output.printGolden("Click on path");
            makeComponentVisible();
            if (path.getParentPath() != null) {
                expandPath(path.getParentPath());
            }
            makeVisible(path);
            scrollToPath(path);
            Point point = getPointToClick(path);
            clickMouse((int) point.getX(), (int) point.getY(), clickCount, mouseButton, modifiers);
        } else {
            throw (new NoSuchPathException());
        }
    }

    /**
     * Clicks on the node.
     *
     * @param path a path to click on.
     * @param clickCount a number of clicks
     * @param mouseButton InputEvent.BUTTON1/2/3_MASK value
     * @throws TimeoutExpiredException
     */
    public void clickOnPath(TreePath path, int clickCount, int mouseButton) {
        clickOnPath(path, clickCount, mouseButton, 0);
    }

    /**
     * Clicks on the node.
     *
     * @param path a path to click on.
     * @param clickCount a number of clicks
     * @throws TimeoutExpiredException
     */
    public void clickOnPath(TreePath path, int clickCount) {
        clickOnPath(path, clickCount, getDefaultMouseButton());
    }

    /**
     * Clicks on the node.
     *
     * @param path a path to click on.
     * @throws TimeoutExpiredException
     */
    public void clickOnPath(TreePath path) {
        clickOnPath(path, 1);
    }

    /**
     * Calls popup on the specified pathes.
     *
     * @param paths an array of paths to select before invoking popup on one of
     * them
     * @param mouseButton a mouse button tused to call popup.
     * @return an opened popup menu.
     * @throws TimeoutExpiredException
     */
    public JPopupMenu callPopupOnPaths(TreePath[] paths, int mouseButton) {
        if (paths.length == 1) {
            output.printLine("Call popup on \"" + paths[0].toString()
                    + "\" path");
            output.printGolden("Call popup on path");
        } else {
            output.printLine("Call popup on some pathes:");
            for (TreePath path : paths) {
                output.printLine("    " + path.toString());
            }
            output.printGolden("Call popup on paths");
        }
        makeComponentVisible();
        for (TreePath path : paths) {
            if (path.getParentPath() != null) {
                expandPath(path.getParentPath());
            }
        }
        selectPaths(paths);
        scrollToPath(paths[paths.length - 1]);
        Point point = getPointToClick(paths[paths.length - 1]);
        return (JPopupMenuOperator.callPopup(this,
                (int) point.getX(),
                (int) point.getY(),
                mouseButton));
    }

    /**
     * Calls popup on the specified pathes.
     *
     * @param paths an array of paths to select before invoking popup on one of
     * them
     * @return an opened popup menu.
     * @throws TimeoutExpiredException
     */
    public JPopupMenu callPopupOnPaths(TreePath[] paths) {
        return callPopupOnPaths(paths, getPopupMouseButton());
    }

    /**
     * Calls popup on the specified path.
     *
     * @param path a path to invoking popup on.
     * @param mouseButton a mouse button tused to call popup.
     * @return an opened popup menu.
     * @throws TimeoutExpiredException
     */
    public JPopupMenu callPopupOnPath(TreePath path, int mouseButton) {
        if (path != null) {
            TreePath[] paths = {path};
            return callPopupOnPaths(paths, mouseButton);
        } else {
            throw (new NoSuchPathException());
        }
    }

    /**
     * Calls popup on the specified path.
     *
     * @param path a path to invoking popup on.
     * @return an opened popup menu.
     * @throws TimeoutExpiredException
     */
    public JPopupMenu callPopupOnPath(TreePath path) {
        return callPopupOnPath(path, getPopupMouseButton());
    }

    /**
     * Scrolls to a path if the tree is on a JScrollPane component.
     *
     * @param path a tree path to scroll to.
     */
    public void scrollToPath(TreePath path) {
        if (path != null) {
            output.printTrace("Scroll JTree to path \"" + path.toString() + "\"\n    : "
                    + toStringSource());
            output.printGolden("Scroll JTree to path \"" + path.toString() + "\"");
            makeComponentVisible();
            //try to find JScrollPane under.
            JScrollPane scroll = (JScrollPane) getContainer(new JScrollPaneOperator.JScrollPaneFinder(ComponentSearcher.
                    getTrueChooser("JScrollPane")));
            if (scroll == null) {
                return;
            }
            JScrollPaneOperator scroller = new JScrollPaneOperator(scroll);
            scroller.copyEnvironment(this);
            scroller.setVisualizer(new EmptyVisualizer());
            Rectangle rect = getPathBounds(path);
            if (rect != null) {
                scroller.scrollToComponentRectangle(getSource(),
                        (int) rect.getX(),
                        (int) rect.getY(),
                        (int) rect.getWidth(),
                        (int) rect.getHeight());
            } else {
                throw (new NoSuchPathException(path));
            }
        } else {
            throw (new NoSuchPathException());
        }
    }

    /**
     * Scrolls to a row if the tree is on a JScrollPane component.
     *
     * @param row a row index to scroll to.
     */
    public void scrollToRow(int row) {
        scrollToPath(getPathForRow(row));
    }

    /**
     * Turns path to the editing mode.
     *
     * @param path a tree path to click on.
     * @throws TimeoutExpiredException
     */
    public void clickForEdit(TreePath path) {
        driver.startEditing(this, getRowForPath(path),
                timeouts.create("JTreeOperator.WaitEditingTimeout"));
    }

    /**
     * Ask renderer for component to be displayed.
     *
     * @param path a path indicating the rendered node.
     * @param isSelected True if the specified cell is selected.
     * @param isExpanded True if the specified cell is expanded.
     * @param cellHasFocus True if the specified cell has the focus.
     * @return Component to be displayed.
     */
    public Component getRenderedComponent(TreePath path, boolean isSelected, boolean isExpanded, boolean cellHasFocus) {
        if (path != null) {
            return (getCellRenderer().
                    getTreeCellRendererComponent((JTree) getSource(),
                            path.getLastPathComponent(),
                            isSelected,
                            isExpanded,
                            getModel().isLeaf(path.getLastPathComponent()),
                            getRowForPath(path),
                            cellHasFocus));
        } else {
            throw (new NoSuchPathException());
        }
    }

    /**
     * Ask renderer for component to be displayed. Uses isPathSelected(TreePath)
     * to determine whether path is selected. Uses isExpanded(TreePath) to
     * determine whether path is expanded.
     *
     * @param path a path indicating the rendered node.
     * @return Component to be displayed.
     */
    public Component getRenderedComponent(TreePath path) {
        return (getRenderedComponent(path,
                isPathSelected(path),
                isExpanded(path),
                false));
    }

    /**
     * Changes text of last path component.
     *
     * @param path a path indicating the node to change value for.
     * @param newNodeText a new node value
     * @deprecated Use changePathObject(TreePath, Object) instead.
     * @see #changePathObject(TreePath, Object)
     * @throws TimeoutExpiredException
     */
    @Deprecated
    public void changePathText(TreePath path, String newNodeText) {
        changePathObject(path, newNodeText);
    }

    /**
     * Changes last path component using getCellEditor() editor.
     *
     * @param path a path indicating the node to change value for.
     * @param newValue a new node value
     * @throws TimeoutExpiredException
     */
    public void changePathObject(TreePath path, Object newValue) {
        scrollToPath(path);
        driver.editItem(this, getRowForPath(path), newValue,
                timeouts.create("JTreeOperator.WaitEditingTimeout"));
    }

    /**
     * Waits path to be expanded.
     *
     * @param path a path to wait expanded.
     */
    public void waitExpanded(final TreePath path) {
        if (path != null) {
            getOutput().printLine("Wait \"" + path.toString() + "\" path to be expanded in component \n    : "
                    + toStringSource());
            getOutput().printGolden("Wait \"" + path.toString() + "\" path to be expanded");
            waitState(new ComponentChooser() {
                @Override
                public boolean checkComponent(Component comp) {
                    return isExpanded(path);
                }

                @Override
                public String getDescription() {
                    return "Has \"" + path.toString() + "\" path expanded";
                }

                @Override
                public String toString() {
                    return "JTreeOperator.waitExpanded.ComponentChooser{description = " + getDescription() + '}';
                }
            });
        } else {
            throw (new NoSuchPathException());
        }
    }

    /**
     * Waits row to be expanded.
     *
     * @param row a row index to wait expanded.
     */
    public void waitExpanded(final int row) {
        getOutput().printLine("Wait " + Integer.toString(row) + "'th row to be expanded in component \n    : "
                + toStringSource());
        getOutput().printGolden("Wait " + Integer.toString(row) + "'th row to be expanded");
        waitState(new ComponentChooser() {
            @Override
            public boolean checkComponent(Component comp) {
                return isExpanded(row);
            }

            @Override
            public String getDescription() {
                return "Has " + Integer.toString(row) + "'th row expanded";
            }

            @Override
            public String toString() {
                return "JTreeOperator.waitExpanded.ComponentChooser{description = " + getDescription() + '}';
            }
        });
    }

    /**
     * Waits path to be collapsed.
     *
     * @param path a path to wait collapsed.
     */
    public void waitCollapsed(final TreePath path) {
        if (path != null) {
            getOutput().printLine("Wait \"" + path.toString() + "\" path to be collapsed in component \n    : "
                    + toStringSource());
            getOutput().printGolden("Wait \"" + path.toString() + "\" path to be collapsed");
            waitState(new ComponentChooser() {
                @Override
                public boolean checkComponent(Component comp) {
                    return isCollapsed(path);
                }

                @Override
                public String getDescription() {
                    return "Has \"" + path.toString() + "\" path collapsed";
                }

                @Override
                public String toString() {
                    return "JTreeOperator.waitCollapsed.ComponentChooser{description = " + getDescription() + '}';
                }
            });
        } else {
            throw (new NoSuchPathException());
        }
    }

    /**
     * Waits row to be collapsed.
     *
     * @param row a row index to wait collapsed.
     */
    public void waitCollapsed(final int row) {
        getOutput().printLine("Wait " + Integer.toString(row) + "'th row to be collapsed in component \n    : "
                + toStringSource());
        getOutput().printGolden("Wait " + Integer.toString(row) + "'th row to be collapsed");
        waitState(new ComponentChooser() {
            @Override
            public boolean checkComponent(Component comp) {
                return isCollapsed(row);
            }

            @Override
            public String getDescription() {
                return "Has " + Integer.toString(row) + "'th row collapsed";
            }

            @Override
            public String toString() {
                return "JTreeOperator.waitCollapsed.ComponentChooser{description = " + getDescription() + '}';
            }
        });
    }

    /**
     * Waits path to be visible.
     *
     * @param path a path to wait visible.
     */
    public void waitVisible(final TreePath path) {
        if (path != null) {
            getOutput().printLine("Wait \"" + path.toString() + "\" path to be visible in component \n    : "
                    + toStringSource());
            getOutput().printGolden("Wait \"" + path.toString() + "\" path to be visible");
            waitState(new ComponentChooser() {
                @Override
                public boolean checkComponent(Component comp) {
                    return isVisible(path);
                }

                @Override
                public String getDescription() {
                    return "Has \"" + path.toString() + "\" path visible";
                }

                @Override
                public String toString() {
                    return "JTreeOperator.waitVisible.ComponentChooser{description = " + getDescription() + '}';
                }
            });
        } else {
            throw (new NoSuchPathException());
        }
    }

    /**
     * Waits some paths to be selected.
     *
     * @param paths an array of paths to be selected.
     */
    public void waitSelected(final TreePath[] paths) {
        getOutput().printLine("Wait right selection in component \n    : "
                + toStringSource());
        getOutput().printGolden("Wait right selection");
        waitState(new ComponentChooser() {
            @Override
            public boolean checkComponent(Component comp) {
                TreePath[] rpaths = getSelectionModel().getSelectionPaths();
                if (rpaths != null) {
                    for (int i = 0; i < rpaths.length; i++) {
                        if (!rpaths[i].equals(paths[i])) {
                            return false;
                        }
                    }
                    return true;
                } else {
                    return false;
                }
            }

            @Override
            public String getDescription() {
                return "Has right selection";
            }

            @Override
            public String toString() {
                return "JTreeOperator.waitSelected.ComponentChooser{description = " + getDescription() + '}';
            }
        });
    }

    /**
     * Waits path to be selected.
     *
     * @param path a tree path to be selected.
     */
    public void waitSelected(final TreePath path) {
        waitSelected(new TreePath[]{path});
    }

    /**
     * Waits rows to be selected.
     *
     * @param rows an indices of rows to be selected.
     */
    public void waitSelected(int[] rows) {
        TreePath[] paths = new TreePath[rows.length];
        for (int i = 0; i < rows.length; i++) {
            paths[i] = getPathForRow(rows[i]);
        }
        waitSelected(paths);
    }

    /**
     * Waits row to be selected.
     *
     * @param row an index of a row to be selected.
     */
    public void waitSelected(int row) {
        waitSelected(new int[]{row});
    }

    /**
     * Wat for text in certain row.
     *
     * @param rowText Text to be compared with row text be
     * {@code getComparator()} comparator.
     * @param row Row index. If -1, selected one is checked.
     */
    public void waitRow(String rowText, int row) {
        getOutput().printLine("Wait \"" + rowText + "\" text in "
                + Integer.toString(row) + "'th line in component \n    : "
                + toStringSource());
        getOutput().printGolden("Wait \"" + rowText + " \" text in "
                + Integer.toString(row) + "'th line");
        waitState(new JTreeByItemFinder(rowText, row, getComparator()));
    }

    public Object chooseSubnode(Object parent, String text, int index, StringComparator comparator) {
        int count = -1;
        Object node;
        for (int i = 0; i < getChildCount(parent); i++) {
            try {
                node = getChild(parent, i);
            } catch (JemmyException e) {
                if (e.getInnerThrowable() instanceof IndexOutOfBoundsException) {
                    // tree probably re-generated because we haven't found child with specified index
                    return null;
                } else {
                    throw e;
                }
            }
            if (comparator.equals(node.toString(),
                    text)) {
                count++;
                if (count == index) {
                    return node;
                }
            }
        }
        return null;
    }

    public Object chooseSubnode(Object parent, String text, StringComparator comparator) {
        return chooseSubnode(parent, text, 0, comparator);
    }

    public Object chooseSubnode(Object parent, String text, int index) {
        return chooseSubnode(parent, text, index, getComparator());
    }

    public Object chooseSubnode(Object parent, String text) {
        return chooseSubnode(parent, text, 0, getComparator());
    }

    /**
     * Returns information about component.
     */
    @Override
    public Hashtable<String, Object> getDump() {
        Hashtable<String, Object> result = super.getDump();
        Object root = ((JTree) getSource()).getModel().getRoot();
        // only if root is not hidden
        result.put(ROOT_DPROP, root.toString());
        addChildrenToDump(result, NODE_PREFIX_DPROP, root, new TreePath(root));
        int minSelection = ((JTree) getSource()).getMinSelectionRow();
        if (minSelection >= 0) {
            Object minObject = ((JTree) getSource()).
                    getPathForRow(minSelection).
                    getLastPathComponent();
            result.put(SELECTION_FIRST_DPROP, minObject.toString());
            int maxSelection = ((JTree) getSource()).getMaxSelectionRow();
            if (maxSelection > minSelection) {
                Object maxObject = ((JTree) getSource()).
                        getPathForRow(maxSelection).
                        getLastPathComponent();
                result.put(SELECTION_LAST_DPROP, maxObject.toString());
            }
        }
        return result;
    }

    ////////////////////////////////////////////////////////
    //Mapping                                             //
    /**
     * Maps {@code JTree.addSelectionInterval(int, int)} through queue
     */
    public void addSelectionInterval(final int i, final int i1) {
        runMapping(new MapVoidAction("addSelectionInterval") {
            @Override
            public void map() {
                ((JTree) getSource()).addSelectionInterval(i, i1);
            }
        });
    }

    /**
     * Maps {@code JTree.addSelectionPath(TreePath)} through queue
     */
    public void addSelectionPath(final TreePath treePath) {
        runMapping(new MapVoidAction("addSelectionPath") {
            @Override
            public void map() {
                ((JTree) getSource()).addSelectionPath(treePath);
            }
        });
    }

    /**
     * Maps {@code JTree.addSelectionPaths(TreePath[])} through queue
     */
    public void addSelectionPaths(final TreePath[] treePath) {
        runMapping(new MapVoidAction("addSelectionPaths") {
            @Override
            public void map() {
                ((JTree) getSource()).addSelectionPaths(treePath);
            }
        });
    }

    /**
     * Maps {@code JTree.addSelectionRow(int)} through queue
     */
    public void addSelectionRow(final int i) {
        runMapping(new MapVoidAction("addSelectionRow") {
            @Override
            public void map() {
                ((JTree) getSource()).addSelectionRow(i);
            }
        });
    }

    /**
     * Maps {@code JTree.addSelectionRows(int[])} through queue
     */
    public void addSelectionRows(final int[] i) {
        runMapping(new MapVoidAction("addSelectionRows") {
            @Override
            public void map() {
                ((JTree) getSource()).addSelectionRows(i);
            }
        });
    }

    /**
     * Maps {@code JTree.addTreeExpansionListener(TreeExpansionListener)}
     * through queue
     */
    public void addTreeExpansionListener(final TreeExpansionListener treeExpansionListener) {
        runMapping(new MapVoidAction("addTreeExpansionListener") {
            @Override
            public void map() {
                ((JTree) getSource()).addTreeExpansionListener(treeExpansionListener);
            }
        });
    }

    /**
     * Maps {@code JTree.addTreeSelectionListener(TreeSelectionListener)}
     * through queue
     */
    public void addTreeSelectionListener(final TreeSelectionListener treeSelectionListener) {
        runMapping(new MapVoidAction("addTreeSelectionListener") {
            @Override
            public void map() {
                ((JTree) getSource()).addTreeSelectionListener(treeSelectionListener);
            }
        });
    }

    /**
     * Maps {@code JTree.addTreeWillExpandListener(TreeWillExpandListener)}
     * through queue
     */
    public void addTreeWillExpandListener(final TreeWillExpandListener treeWillExpandListener) {
        runMapping(new MapVoidAction("addTreeWillExpandListener") {
            @Override
            public void map() {
                ((JTree) getSource()).addTreeWillExpandListener(treeWillExpandListener);
            }
        });
    }

    /**
     * Maps {@code JTree.cancelEditing()} through queue
     */
    public void cancelEditing() {
        runMapping(new MapVoidAction("cancelEditing") {
            @Override
            public void map() {
                ((JTree) getSource()).cancelEditing();
            }
        });
    }

    /**
     * Maps {@code JTree.clearSelection()} through queue
     */
    public void clearSelection() {
        runMapping(new MapVoidAction("clearSelection") {
            @Override
            public void map() {
                ((JTree) getSource()).clearSelection();
            }
        });
    }

    /**
     * Maps {@code JTree.collapsePath(TreePath)} through queue
     */
    public void collapsePath(final TreePath treePath) {
        runMapping(new MapVoidAction("collapsePath") {
            @Override
            public void map() {
                ((JTree) getSource()).collapsePath(treePath);
            }
        });
    }

    /**
     * Maps {@code JTree.collapseRow(int)} through queue
     */
    public void collapseRow(final int i) {
        runMapping(new MapVoidAction("collapseRow") {
            @Override
            public void map() {
                ((JTree) getSource()).collapseRow(i);
            }
        });
    }

    /**
     * Maps
     * {@code JTree.convertValueToText(Object, boolean, boolean, boolean, int, boolean)}
     * through queue
     */
    public String convertValueToText(final Object object, final boolean b, final boolean b1, final boolean b2, final int i, final boolean b3) {
        return (runMapping(new MapAction<String>("convertValueToText") {
            @Override
            public String map() {
                return ((JTree) getSource()).convertValueToText(object, b, b1, b2, i, b3);
            }
        }));
    }

    /**
     * Maps {@code JTree.expandPath(TreePath)} through queue
     */
    public void expandPath(final TreePath treePath) {
        runMapping(new MapVoidAction("expandPath") {
            @Override
            public void map() {
                ((JTree) getSource()).expandPath(treePath);
            }
        });
    }

    /**
     * Maps {@code JTree.expandRow(int)} through queue
     */
    public void expandRow(final int i) {
        runMapping(new MapVoidAction("expandRow") {
            @Override
            public void map() {
                ((JTree) getSource()).expandRow(i);
            }
        });
    }

    /**
     * Maps {@code JTree.fireTreeCollapsed(TreePath)} through queue
     */
    public void fireTreeCollapsed(final TreePath treePath) {
        runMapping(new MapVoidAction("fireTreeCollapsed") {
            @Override
            public void map() {
                ((JTree) getSource()).fireTreeCollapsed(treePath);
            }
        });
    }

    /**
     * Maps {@code JTree.fireTreeExpanded(TreePath)} through queue
     */
    public void fireTreeExpanded(final TreePath treePath) {
        runMapping(new MapVoidAction("fireTreeExpanded") {
            @Override
            public void map() {
                ((JTree) getSource()).fireTreeExpanded(treePath);
            }
        });
    }

    /**
     * Maps {@code JTree.fireTreeWillCollapse(TreePath)} through queue
     */
    public void fireTreeWillCollapse(final TreePath treePath) {
        runMapping(new MapVoidAction("fireTreeWillCollapse") {
            @Override
            public void map() throws ExpandVetoException {
                ((JTree) getSource()).fireTreeWillCollapse(treePath);
            }
        });
    }

    /**
     * Maps {@code JTree.fireTreeWillExpand(TreePath)} through queue
     */
    public void fireTreeWillExpand(final TreePath treePath) {
        runMapping(new MapVoidAction("fireTreeWillExpand") {
            @Override
            public void map() throws ExpandVetoException {
                ((JTree) getSource()).fireTreeWillExpand(treePath);
            }
        });
    }

    /**
     * Maps {@code JTree.getCellEditor()} through queue
     */
    public TreeCellEditor getCellEditor() {
        return (runMapping(new MapAction<TreeCellEditor>("getCellEditor") {
            @Override
            public TreeCellEditor map() {
                return ((JTree) getSource()).getCellEditor();
            }
        }));
    }

    /**
     * Maps {@code JTree.getCellRenderer()} through queue
     */
    public TreeCellRenderer getCellRenderer() {
        return (runMapping(new MapAction<TreeCellRenderer>("getCellRenderer") {
            @Override
            public TreeCellRenderer map() {
                return ((JTree) getSource()).getCellRenderer();
            }
        }));
    }

    /**
     * Maps {@code JTree.getClosestPathForLocation(int, int)} through queue
     */
    public TreePath getClosestPathForLocation(final int i, final int i1) {
        return (runMapping(new MapAction<TreePath>("getClosestPathForLocation") {
            @Override
            public TreePath map() {
                return ((JTree) getSource()).getClosestPathForLocation(i, i1);
            }
        }));
    }

    /**
     * Maps {@code JTree.getClosestRowForLocation(int, int)} through queue
     */
    public int getClosestRowForLocation(final int i, final int i1) {
        return (runMapping(new MapIntegerAction("getClosestRowForLocation") {
            @Override
            public int map() {
                return ((JTree) getSource()).getClosestRowForLocation(i, i1);
            }
        }));
    }

    /**
     * Maps {@code JTree.getEditingPath()} through queue
     */
    public TreePath getEditingPath() {
        return (runMapping(new MapAction<TreePath>("getEditingPath") {
            @Override
            public TreePath map() {
                return ((JTree) getSource()).getEditingPath();
            }
        }));
    }

    /**
     * Maps {@code JTree.getExpandedDescendants(TreePath)} through queue
     */
    public Enumeration<TreePath> getExpandedDescendants(final TreePath treePath) {
        return (runMapping(new MapAction<Enumeration<TreePath>>("getExpandedDescendants") {
            @Override
            public Enumeration<TreePath> map() {
                return ((JTree) getSource()).getExpandedDescendants(treePath);
            }
        }));
    }

    /**
     * Maps {@code JTree.getInvokesStopCellEditing()} through queue
     */
    public boolean getInvokesStopCellEditing() {
        return (runMapping(new MapBooleanAction("getInvokesStopCellEditing") {
            @Override
            public boolean map() {
                return ((JTree) getSource()).getInvokesStopCellEditing();
            }
        }));
    }

    /**
     * Maps {@code JTree.getLastSelectedPathComponent()} through queue
     */
    public Object getLastSelectedPathComponent() {
        return (runMapping(new MapAction<Object>("getLastSelectedPathComponent") {
            @Override
            public Object map() {
                return ((JTree) getSource()).getLastSelectedPathComponent();
            }
        }));
    }

    /**
     * Maps {@code JTree.getLeadSelectionPath()} through queue
     */
    public TreePath getLeadSelectionPath() {
        return (runMapping(new MapAction<TreePath>("getLeadSelectionPath") {
            @Override
            public TreePath map() {
                return ((JTree) getSource()).getLeadSelectionPath();
            }
        }));
    }

    /**
     * Maps {@code JTree.getLeadSelectionRow()} through queue
     */
    public int getLeadSelectionRow() {
        return (runMapping(new MapIntegerAction("getLeadSelectionRow") {
            @Override
            public int map() {
                return ((JTree) getSource()).getLeadSelectionRow();
            }
        }));
    }

    /**
     * Maps {@code JTree.getMaxSelectionRow()} through queue
     */
    public int getMaxSelectionRow() {
        return (runMapping(new MapIntegerAction("getMaxSelectionRow") {
            @Override
            public int map() {
                return ((JTree) getSource()).getMaxSelectionRow();
            }
        }));
    }

    /**
     * Maps {@code JTree.getMinSelectionRow()} through queue
     */
    public int getMinSelectionRow() {
        return (runMapping(new MapIntegerAction("getMinSelectionRow") {
            @Override
            public int map() {
                return ((JTree) getSource()).getMinSelectionRow();
            }
        }));
    }

    /**
     * Maps {@code JTree.getModel()} through queue
     */
    public TreeModel getModel() {
        return (runMapping(new MapAction<TreeModel>("getModel") {
            @Override
            public TreeModel map() {
                return ((JTree) getSource()).getModel();
            }
        }));
    }

    /**
     * Maps {@code JTree.getPathBounds(TreePath)} through queue
     */
    public Rectangle getPathBounds(final TreePath treePath) {
        return (runMapping(new MapAction<Rectangle>("getPathBounds") {
            @Override
            public Rectangle map() {
                return ((JTree) getSource()).getPathBounds(treePath);
            }
        }));
    }

    /**
     * Maps {@code JTree.getPathForLocation(int, int)} through queue
     */
    public TreePath getPathForLocation(final int i, final int i1) {
        return (runMapping(new MapAction<TreePath>("getPathForLocation") {
            @Override
            public TreePath map() {
                return ((JTree) getSource()).getPathForLocation(i, i1);
            }
        }));
    }

    /**
     * Maps {@code JTree.getPathForRow(int)} through queue
     */
    public TreePath getPathForRow(final int i) {
        return (runMapping(new MapAction<TreePath>("getPathForRow") {
            @Override
            public TreePath map() {
                return ((JTree) getSource()).getPathForRow(i);
            }
        }));
    }

    /**
     * Maps {@code JTree.getPreferredScrollableViewportSize()} through queue
     */
    public Dimension getPreferredScrollableViewportSize() {
        return (runMapping(new MapAction<Dimension>("getPreferredScrollableViewportSize") {
            @Override
            public Dimension map() {
                return ((JTree) getSource()).getPreferredScrollableViewportSize();
            }
        }));
    }

    /**
     * Maps {@code JTree.getRowBounds(int)} through queue
     */
    public Rectangle getRowBounds(final int i) {
        return (runMapping(new MapAction<Rectangle>("getRowBounds") {
            @Override
            public Rectangle map() {
                return ((JTree) getSource()).getRowBounds(i);
            }
        }));
    }

    /**
     * Maps {@code JTree.getRowCount()} through queue
     */
    public int getRowCount() {
        return (runMapping(new MapIntegerAction("getRowCount") {
            @Override
            public int map() {
                return ((JTree) getSource()).getRowCount();
            }
        }));
    }

    /**
     * Maps {@code JTree.getRowForLocation(int, int)} through queue
     */
    public int getRowForLocation(final int i, final int i1) {
        return (runMapping(new MapIntegerAction("getRowForLocation") {
            @Override
            public int map() {
                return ((JTree) getSource()).getRowForLocation(i, i1);
            }
        }));
    }

    /**
     * Maps {@code JTree.getRowForPath(TreePath)} through queue
     */
    public int getRowForPath(final TreePath treePath) {
        return (runMapping(new MapIntegerAction("getRowForPath") {
            @Override
            public int map() {
                return ((JTree) getSource()).getRowForPath(treePath);
            }
        }));
    }

    /**
     * Maps {@code JTree.getRowHeight()} through queue
     */
    public int getRowHeight() {
        return (runMapping(new MapIntegerAction("getRowHeight") {
            @Override
            public int map() {
                return ((JTree) getSource()).getRowHeight();
            }
        }));
    }

    /**
     * Maps {@code JTree.getScrollableBlockIncrement(Rectangle, int, int)}
     * through queue
     */
    public int getScrollableBlockIncrement(final Rectangle rectangle, final int i, final int i1) {
        return (runMapping(new MapIntegerAction("getScrollableBlockIncrement") {
            @Override
            public int map() {
                return ((JTree) getSource()).getScrollableBlockIncrement(rectangle, i, i1);
            }
        }));
    }

    /**
     * Maps {@code JTree.getScrollableTracksViewportHeight()} through queue
     */
    public boolean getScrollableTracksViewportHeight() {
        return (runMapping(new MapBooleanAction("getScrollableTracksViewportHeight") {
            @Override
            public boolean map() {
                return ((JTree) getSource()).getScrollableTracksViewportHeight();
            }
        }));
    }

    /**
     * Maps {@code JTree.getScrollableTracksViewportWidth()} through queue
     */
    public boolean getScrollableTracksViewportWidth() {
        return (runMapping(new MapBooleanAction("getScrollableTracksViewportWidth") {
            @Override
            public boolean map() {
                return ((JTree) getSource()).getScrollableTracksViewportWidth();
            }
        }));
    }

    /**
     * Maps {@code JTree.getScrollableUnitIncrement(Rectangle, int, int)}
     * through queue
     */
    public int getScrollableUnitIncrement(final Rectangle rectangle, final int i, final int i1) {
        return (runMapping(new MapIntegerAction("getScrollableUnitIncrement") {
            @Override
            public int map() {
                return ((JTree) getSource()).getScrollableUnitIncrement(rectangle, i, i1);
            }
        }));
    }

    /**
     * Maps {@code JTree.getScrollsOnExpand()} through queue
     */
    public boolean getScrollsOnExpand() {
        return (runMapping(new MapBooleanAction("getScrollsOnExpand") {
            @Override
            public boolean map() {
                return ((JTree) getSource()).getScrollsOnExpand();
            }
        }));
    }

    /**
     * Maps {@code JTree.getSelectionCount()} through queue
     */
    public int getSelectionCount() {
        return (runMapping(new MapIntegerAction("getSelectionCount") {
            @Override
            public int map() {
                return ((JTree) getSource()).getSelectionCount();
            }
        }));
    }

    /**
     * Maps {@code JTree.getSelectionModel()} through queue
     */
    public TreeSelectionModel getSelectionModel() {
        return (runMapping(new MapAction<TreeSelectionModel>("getSelectionModel") {
            @Override
            public TreeSelectionModel map() {
                return ((JTree) getSource()).getSelectionModel();
            }
        }));
    }

    /**
     * Maps {@code JTree.getSelectionPath()} through queue
     */
    public TreePath getSelectionPath() {
        return (runMapping(new MapAction<TreePath>("getSelectionPath") {
            @Override
            public TreePath map() {
                return ((JTree) getSource()).getSelectionPath();
            }
        }));
    }

    /**
     * Maps {@code JTree.getSelectionPaths()} through queue
     */
    public TreePath[] getSelectionPaths() {
        return ((TreePath[]) runMapping(new MapAction<Object>("getSelectionPaths") {
            @Override
            public Object map() {
                return ((JTree) getSource()).getSelectionPaths();
            }
        }));
    }

    /**
     * Maps {@code JTree.getSelectionRows()} through queue
     */
    public int[] getSelectionRows() {
        return ((int[]) runMapping(new MapAction<Object>("getSelectionRows") {
            @Override
            public Object map() {
                return ((JTree) getSource()).getSelectionRows();
            }
        }));
    }

    /**
     * Maps {@code JTree.getShowsRootHandles()} through queue
     */
    public boolean getShowsRootHandles() {
        return (runMapping(new MapBooleanAction("getShowsRootHandles") {
            @Override
            public boolean map() {
                return ((JTree) getSource()).getShowsRootHandles();
            }
        }));
    }

    /**
     * Maps {@code JTree.getUI()} through queue
     */
    public TreeUI getUI() {
        return (runMapping(new MapAction<TreeUI>("getUI") {
            @Override
            public TreeUI map() {
                return ((JTree) getSource()).getUI();
            }
        }));
    }

    /**
     * Maps {@code JTree.getVisibleRowCount()} through queue
     */
    public int getVisibleRowCount() {
        return (runMapping(new MapIntegerAction("getVisibleRowCount") {
            @Override
            public int map() {
                return ((JTree) getSource()).getVisibleRowCount();
            }
        }));
    }

    /**
     * Maps {@code JTree.hasBeenExpanded(TreePath)} through queue
     */
    public boolean hasBeenExpanded(final TreePath treePath) {
        return (runMapping(new MapBooleanAction("hasBeenExpanded") {
            @Override
            public boolean map() {
                return ((JTree) getSource()).hasBeenExpanded(treePath);
            }
        }));
    }

    /**
     * Maps {@code JTree.isCollapsed(int)} through queue
     */
    public boolean isCollapsed(final int i) {
        return (runMapping(new MapBooleanAction("isCollapsed") {
            @Override
            public boolean map() {
                return ((JTree) getSource()).isCollapsed(i);
            }
        }));
    }

    /**
     * Maps {@code JTree.isCollapsed(TreePath)} through queue
     */
    public boolean isCollapsed(final TreePath treePath) {
        return (runMapping(new MapBooleanAction("isCollapsed") {
            @Override
            public boolean map() {
                return ((JTree) getSource()).isCollapsed(treePath);
            }
        }));
    }

    /**
     * Maps {@code JTree.isEditable()} through queue
     */
    public boolean isEditable() {
        return (runMapping(new MapBooleanAction("isEditable") {
            @Override
            public boolean map() {
                return ((JTree) getSource()).isEditable();
            }
        }));
    }

    /**
     * Maps {@code JTree.isEditing()} through queue
     */
    public boolean isEditing() {
        return (runMapping(new MapBooleanAction("isEditing") {
            @Override
            public boolean map() {
                return ((JTree) getSource()).isEditing();
            }
        }));
    }

    /**
     * Maps {@code JTree.isExpanded(int)} through queue
     */
    public boolean isExpanded(final int i) {
        return (runMapping(new MapBooleanAction("isExpanded") {
            @Override
            public boolean map() {
                return ((JTree) getSource()).isExpanded(i);
            }
        }));
    }

    /**
     * Maps {@code JTree.isExpanded(TreePath)} through queue
     */
    public boolean isExpanded(final TreePath treePath) {
        return (runMapping(new MapBooleanAction("isExpanded") {
            @Override
            public boolean map() {
                return ((JTree) getSource()).isExpanded(treePath);
            }
        }));
    }

    /**
     * Maps {@code JTree.isFixedRowHeight()} through queue
     */
    public boolean isFixedRowHeight() {
        return (runMapping(new MapBooleanAction("isFixedRowHeight") {
            @Override
            public boolean map() {
                return ((JTree) getSource()).isFixedRowHeight();
            }
        }));
    }

    /**
     * Maps {@code JTree.isLargeModel()} through queue
     */
    public boolean isLargeModel() {
        return (runMapping(new MapBooleanAction("isLargeModel") {
            @Override
            public boolean map() {
                return ((JTree) getSource()).isLargeModel();
            }
        }));
    }

    /**
     * Maps {@code JTree.isPathEditable(TreePath)} through queue
     */
    public boolean isPathEditable(final TreePath treePath) {
        return (runMapping(new MapBooleanAction("isPathEditable") {
            @Override
            public boolean map() {
                return ((JTree) getSource()).isPathEditable(treePath);
            }
        }));
    }

    /**
     * Maps {@code JTree.isPathSelected(TreePath)} through queue
     */
    public boolean isPathSelected(final TreePath treePath) {
        return (runMapping(new MapBooleanAction("isPathSelected") {
            @Override
            public boolean map() {
                return ((JTree) getSource()).isPathSelected(treePath);
            }
        }));
    }

    /**
     * Maps {@code JTree.isRootVisible()} through queue
     */
    public boolean isRootVisible() {
        return (runMapping(new MapBooleanAction("isRootVisible") {
            @Override
            public boolean map() {
                return ((JTree) getSource()).isRootVisible();
            }
        }));
    }

    /**
     * Maps {@code JTree.isRowSelected(int)} through queue
     */
    public boolean isRowSelected(final int i) {
        return (runMapping(new MapBooleanAction("isRowSelected") {
            @Override
            public boolean map() {
                return ((JTree) getSource()).isRowSelected(i);
            }
        }));
    }

    /**
     * Maps {@code JTree.isSelectionEmpty()} through queue
     */
    public boolean isSelectionEmpty() {
        return (runMapping(new MapBooleanAction("isSelectionEmpty") {
            @Override
            public boolean map() {
                return ((JTree) getSource()).isSelectionEmpty();
            }
        }));
    }

    /**
     * Maps {@code JTree.isVisible(TreePath)} through queue
     */
    public boolean isVisible(final TreePath treePath) {
        return (runMapping(new MapBooleanAction("isVisible") {
            @Override
            public boolean map() {
                return ((JTree) getSource()).isVisible(treePath);
            }
        }));
    }

    /**
     * Maps {@code JTree.makeVisible(TreePath)} through queue
     */
    public void makeVisible(final TreePath treePath) {
        runMapping(new MapVoidAction("makeVisible") {
            @Override
            public void map() {
                ((JTree) getSource()).makeVisible(treePath);
            }
        });
    }

    /**
     * Maps {@code JTree.removeSelectionInterval(int, int)} through queue
     */
    public void removeSelectionInterval(final int i, final int i1) {
        runMapping(new MapVoidAction("removeSelectionInterval") {
            @Override
            public void map() {
                ((JTree) getSource()).removeSelectionInterval(i, i1);
            }
        });
    }

    /**
     * Maps {@code JTree.removeSelectionPath(TreePath)} through queue
     */
    public void removeSelectionPath(final TreePath treePath) {
        runMapping(new MapVoidAction("removeSelectionPath") {
            @Override
            public void map() {
                ((JTree) getSource()).removeSelectionPath(treePath);
            }
        });
    }

    /**
     * Maps {@code JTree.removeSelectionPaths(TreePath[])} through queue
     */
    public void removeSelectionPaths(final TreePath[] treePath) {
        runMapping(new MapVoidAction("removeSelectionPaths") {
            @Override
            public void map() {
                ((JTree) getSource()).removeSelectionPaths(treePath);
            }
        });
    }

    /**
     * Maps {@code JTree.removeSelectionRow(int)} through queue
     */
    public void removeSelectionRow(final int i) {
        runMapping(new MapVoidAction("removeSelectionRow") {
            @Override
            public void map() {
                ((JTree) getSource()).removeSelectionRow(i);
            }
        });
    }

    /**
     * Maps {@code JTree.removeSelectionRows(int[])} through queue
     */
    public void removeSelectionRows(final int[] i) {
        runMapping(new MapVoidAction("removeSelectionRows") {
            @Override
            public void map() {
                ((JTree) getSource()).removeSelectionRows(i);
            }
        });
    }

    /**
     * Maps
     * {@code JTree.removeTreeExpansionListener(TreeExpansionListener)}
     * through queue
     */
    public void removeTreeExpansionListener(final TreeExpansionListener treeExpansionListener) {
        runMapping(new MapVoidAction("removeTreeExpansionListener") {
            @Override
            public void map() {
                ((JTree) getSource()).removeTreeExpansionListener(treeExpansionListener);
            }
        });
    }

    /**
     * Maps
     * {@code JTree.removeTreeSelectionListener(TreeSelectionListener)}
     * through queue
     */
    public void removeTreeSelectionListener(final TreeSelectionListener treeSelectionListener) {
        runMapping(new MapVoidAction("removeTreeSelectionListener") {
            @Override
            public void map() {
                ((JTree) getSource()).removeTreeSelectionListener(treeSelectionListener);
            }
        });
    }

    /**
     * Maps
     * {@code JTree.removeTreeWillExpandListener(TreeWillExpandListener)}
     * through queue
     */
    public void removeTreeWillExpandListener(final TreeWillExpandListener treeWillExpandListener) {
        runMapping(new MapVoidAction("removeTreeWillExpandListener") {
            @Override
            public void map() {
                ((JTree) getSource()).removeTreeWillExpandListener(treeWillExpandListener);
            }
        });
    }

    /**
     * Maps {@code JTree.scrollPathToVisible(TreePath)} through queue
     */
    public void scrollPathToVisible(final TreePath treePath) {
        runMapping(new MapVoidAction("scrollPathToVisible") {
            @Override
            public void map() {
                ((JTree) getSource()).scrollPathToVisible(treePath);
            }
        });
    }

    /**
     * Maps {@code JTree.scrollRowToVisible(int)} through queue
     */
    public void scrollRowToVisible(final int i) {
        runMapping(new MapVoidAction("scrollRowToVisible") {
            @Override
            public void map() {
                ((JTree) getSource()).scrollRowToVisible(i);
            }
        });
    }

    /**
     * Maps {@code JTree.setCellEditor(TreeCellEditor)} through queue
     */
    public void setCellEditor(final TreeCellEditor treeCellEditor) {
        runMapping(new MapVoidAction("setCellEditor") {
            @Override
            public void map() {
                ((JTree) getSource()).setCellEditor(treeCellEditor);
            }
        });
    }

    /**
     * Maps {@code JTree.setCellRenderer(TreeCellRenderer)} through queue
     */
    public void setCellRenderer(final TreeCellRenderer treeCellRenderer) {
        runMapping(new MapVoidAction("setCellRenderer") {
            @Override
            public void map() {
                ((JTree) getSource()).setCellRenderer(treeCellRenderer);
            }
        });
    }

    /**
     * Maps {@code JTree.setEditable(boolean)} through queue
     */
    public void setEditable(final boolean b) {
        runMapping(new MapVoidAction("setEditable") {
            @Override
            public void map() {
                ((JTree) getSource()).setEditable(b);
            }
        });
    }

    /**
     * Maps {@code JTree.setInvokesStopCellEditing(boolean)} through queue
     */
    public void setInvokesStopCellEditing(final boolean b) {
        runMapping(new MapVoidAction("setInvokesStopCellEditing") {
            @Override
            public void map() {
                ((JTree) getSource()).setInvokesStopCellEditing(b);
            }
        });
    }

    /**
     * Maps {@code JTree.setLargeModel(boolean)} through queue
     */
    public void setLargeModel(final boolean b) {
        runMapping(new MapVoidAction("setLargeModel") {
            @Override
            public void map() {
                ((JTree) getSource()).setLargeModel(b);
            }
        });
    }

    /**
     * Maps {@code JTree.setModel(TreeModel)} through queue
     */
    public void setModel(final TreeModel treeModel) {
        runMapping(new MapVoidAction("setModel") {
            @Override
            public void map() {
                ((JTree) getSource()).setModel(treeModel);
            }
        });
    }

    /**
     * Maps {@code JTree.setRootVisible(boolean)} through queue
     */
    public void setRootVisible(final boolean b) {
        runMapping(new MapVoidAction("setRootVisible") {
            @Override
            public void map() {
                ((JTree) getSource()).setRootVisible(b);
            }
        });
    }

    /**
     * Maps {@code JTree.setRowHeight(int)} through queue
     */
    public void setRowHeight(final int i) {
        runMapping(new MapVoidAction("setRowHeight") {
            @Override
            public void map() {
                ((JTree) getSource()).setRowHeight(i);
            }
        });
    }

    /**
     * Maps {@code JTree.setScrollsOnExpand(boolean)} through queue
     */
    public void setScrollsOnExpand(final boolean b) {
        runMapping(new MapVoidAction("setScrollsOnExpand") {
            @Override
            public void map() {
                ((JTree) getSource()).setScrollsOnExpand(b);
            }
        });
    }

    /**
     * Maps {@code JTree.setSelectionInterval(int, int)} through queue
     */
    public void setSelectionInterval(final int i, final int i1) {
        runMapping(new MapVoidAction("setSelectionInterval") {
            @Override
            public void map() {
                ((JTree) getSource()).setSelectionInterval(i, i1);
            }
        });
    }

    /**
     * Maps {@code JTree.setSelectionModel(TreeSelectionModel)} through queue
     */
    public void setSelectionModel(final TreeSelectionModel treeSelectionModel) {
        runMapping(new MapVoidAction("setSelectionModel") {
            @Override
            public void map() {
                ((JTree) getSource()).setSelectionModel(treeSelectionModel);
            }
        });
    }

    /**
     * Maps {@code JTree.setSelectionPath(TreePath)} through queue
     */
    public void setSelectionPath(final TreePath treePath) {
        runMapping(new MapVoidAction("setSelectionPath") {
            @Override
            public void map() {
                ((JTree) getSource()).setSelectionPath(treePath);
            }
        });
    }

    /**
     * Maps {@code JTree.setSelectionPaths(TreePath[])} through queue
     */
    public void setSelectionPaths(final TreePath[] treePath) {
        runMapping(new MapVoidAction("setSelectionPaths") {
            @Override
            public void map() {
                ((JTree) getSource()).setSelectionPaths(treePath);
            }
        });
    }

    /**
     * Maps {@code JTree.setSelectionRow(int)} through queue
     */
    public void setSelectionRow(final int i) {
        runMapping(new MapVoidAction("setSelectionRow") {
            @Override
            public void map() {
                ((JTree) getSource()).setSelectionRow(i);
            }
        });
    }

    /**
     * Maps {@code JTree.setSelectionRows(int[])} through queue
     */
    public void setSelectionRows(final int[] i) {
        runMapping(new MapVoidAction("setSelectionRows") {
            @Override
            public void map() {
                ((JTree) getSource()).setSelectionRows(i);
            }
        });
    }

    /**
     * Maps {@code JTree.setShowsRootHandles(boolean)} through queue
     */
    public void setShowsRootHandles(final boolean b) {
        runMapping(new MapVoidAction("setShowsRootHandles") {
            @Override
            public void map() {
                ((JTree) getSource()).setShowsRootHandles(b);
            }
        });
    }

    /**
     * Maps {@code JTree.setUI(TreeUI)} through queue
     */
    public void setUI(final TreeUI treeUI) {
        runMapping(new MapVoidAction("setUI") {
            @Override
            public void map() {
                ((JTree) getSource()).setUI(treeUI);
            }
        });
    }

    /**
     * Maps {@code JTree.setVisibleRowCount(int)} through queue
     */
    public void setVisibleRowCount(final int i) {
        runMapping(new MapVoidAction("setVisibleRowCount") {
            @Override
            public void map() {
                ((JTree) getSource()).setVisibleRowCount(i);
            }
        });
    }

    /**
     * Maps {@code JTree.startEditingAtPath(TreePath)} through queue
     */
    public void startEditingAtPath(final TreePath treePath) {
        runMapping(new MapVoidAction("startEditingAtPath") {
            @Override
            public void map() {
                ((JTree) getSource()).startEditingAtPath(treePath);
            }
        });
    }

    /**
     * Maps {@code JTree.stopEditing()} through queue
     */
    public boolean stopEditing() {
        return (runMapping(new MapBooleanAction("stopEditing") {
            @Override
            public boolean map() {
                return ((JTree) getSource()).stopEditing();
            }
        }));
    }

    /**
     * Maps {@code JTree.treeDidChange()} through queue
     */
    public void treeDidChange() {
        runMapping(new MapVoidAction("treeDidChange") {
            @Override
            public void map() {
                ((JTree) getSource()).treeDidChange();
            }
        });
    }

    //End of mapping                                      //
    ////////////////////////////////////////////////////////
    /**
     * Iterface to choose tree row. Defines criteria to distinguish row.
     */
    public interface TreeRowChooser {

        /**
         * Should be true if row is good.
         *
         * @param oper Operator used to search item.
         * @param row Row be checked.
         * @return true if the row fits the criteria
         */
        public boolean checkRow(JTreeOperator oper, int row);

        /**
         * Row description.
         *
         * @return a criteria description.
         */
        public String getDescription();
    }

    private TreePath findPathPrimitive(TreePath path, TreePathChooser chooser, Waiter<Object[], Object[]> loadedWaiter) {
        if (!isExpanded(path)) {
            if (!isPathSelected(path)) {
                clickOnPath(path);
            }
            expandPath(path);
        }
        Object[] waitParam = {chooser, path};
        Object[] waitResult = null;
        try {
            waitResult = loadedWaiter.waitAction(waitParam);
        } catch (InterruptedException e) {
            output.printStackTrace(e);
            return null;
        }
        TreePath nextPath = (TreePath) waitResult[0];
        boolean found = (Boolean) waitResult[1];
        if (found) {
            return nextPath;
        } else {
            return findPathPrimitive(nextPath, chooser, loadedWaiter);
        }
    }

    private String[] addChildrenToDump(Hashtable<String, Object> table, String title, Object node, TreePath path) {
        if (((JTree) getSource()).isExpanded(path)) {
            Object[] subNodes = getChildren(node);
            String[] names = addToDump(table, title, subNodes);
            for (int i = 0; i < subNodes.length; i++) {
                addChildrenToDump(table, names[i], subNodes[i], path.pathByAddingChild(subNodes[i]));
            }
            return names;
        } else {
            return new String[0];
        }
    }

    private static String pathToString(String[] path) {
        StringBuilder desc = new StringBuilder("[ ");
        for (String aPath : path) {
            desc.append(aPath).append(", ");
        }
        if (desc.length() > 2) {
            desc.setLength(desc.length() - 2);
        }
        desc.append(" ]");
        return desc.toString();
    }

    /**
     * Can be throught during item selecting if list does not have item
     * requested.
     */
    public class NoSuchPathException extends JemmyInputException {

        private static final long serialVersionUID = 42L;

        /**
         * Constructor.
         */
        public NoSuchPathException() {
            super("Unknown/null/invalid tree path.", null);
        }

        /**
         * Constructor.
         *
         * @param path a nonexistent path.
         */
        public NoSuchPathException(String[] path) {
            super("No such path as \"" + pathToString(path) + "\"", getSource());
        }

        /**
         * Constructor.
         *
         * @param index a nonexistent line index.
         */
        public NoSuchPathException(int index) {
            super("Tree does not contain " + index + "'th line", getSource());
        }

        /**
         * Constructor.
         *
         * @param path a nonexistent path.
         */
        public NoSuchPathException(TreePath path) {
            super("No such path as \"" + path.toString() + "\"", getSource());
        }
    }

    /**
     * Specifies criteria for path searching.
     */
    public interface TreePathChooser {

        /**
         * Checks if the path fits the criteria.
         *
         * @param path TreePath to check.
         * @param indexInParent Index of the "path" in path's parent.
         * @return true if the path fits the criteria
         */
        public boolean checkPath(TreePath path, int indexInParent);

        /**
         * Checks if the path has another path as a parent.
         *
         * @param path TreePath to check.
         * @param indexInParent Index of the "path" in path's parent.
         * @return true if path looked for is a child/grandchild of a path
         * passed as a parameter.
         */
        public boolean hasAsParent(TreePath path, int indexInParent);

        /**
         * Returns the description.
         *
         * @return a description.
         */
        public String getDescription();
    }

    /**
     * Specifies searching criteria basing on nodes' text.
     */
    class StringArrayPathChooser implements TreePathChooser {

        String[] arr;
        int[] indices;
        StringComparator comparator;

        /**
         * Constructs StringArrayPathChooser.
         *
         * @param arr a node text array. First element defines a text of a first
         * node under a tree root, second element - a children of the first
         * node, ...
         * @param indices indexes of nodes in nodes' parents.
         * @param comparator String comparision criteria.
         */
        StringArrayPathChooser(String[] arr, int[] indices, StringComparator comparator) {
            this.arr = arr;
            this.comparator = comparator;
            this.indices = indices;
        }

        @Override
        public boolean checkPath(TreePath path, int indexInParent) {
            return (path.getPathCount() - 1 == arr.length
                    && hasAsParent(path, indexInParent));
        }

        @Override
        public boolean hasAsParent(TreePath path, int indexInParent) {
            Object[] comps = path.getPath();
            Object node;
            for (int i = 1; i < comps.length; i++) {
                if (arr.length < path.getPathCount() - 1) {
                    return false;
                }
                /*
                if(!comparator.equals(comps[i].toString(), arr[i - 1])) {
                    return false;
                }
                 */
                if (indices.length >= path.getPathCount() - 1) {
                    node = chooseSubnode(comps[i - 1], arr[i - 1], indices[i - 1], comparator);
                } else {
                    node = chooseSubnode(comps[i - 1], arr[i - 1], comparator);
                }
                if (node != comps[i]) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String getDescription() {
            return pathToString(arr);
        }

        @Override
        public String toString() {
            return "StringArrayPathChooser{" + "arr=" + arr + ", indices=" + indices + ", comparator=" + comparator + '}';
        }
    }

    private static class BySubStringTreeRowChooser implements TreeRowChooser {

        String subString;
        StringComparator comparator;

        public BySubStringTreeRowChooser(String subString, StringComparator comparator) {
            this.subString = subString;
            this.comparator = comparator;
        }

        @Override
        public boolean checkRow(JTreeOperator oper, int row) {
            return (comparator.equals(oper.getPathForRow(row).getLastPathComponent().toString(),
                    subString));
        }

        @Override
        public String getDescription() {
            return "Row containing \"" + subString + "\" string";
        }

        @Override
        public String toString() {
            return "BySubStringTreeRowChooser{" + "subString=" + subString + ", comparator=" + comparator + '}';
        }
    }

    private static class ByRenderedComponentTreeRowChooser implements TreeRowChooser {

        ComponentChooser chooser;

        public ByRenderedComponentTreeRowChooser(ComponentChooser chooser) {
            this.chooser = chooser;
        }

        @Override
        public boolean checkRow(JTreeOperator oper, int row) {
            return chooser.checkComponent(oper.getRenderedComponent(oper.getPathForRow(row)));
        }

        @Override
        public String getDescription() {
            return chooser.getDescription();
        }

        @Override
        public String toString() {
            return "ByRenderedComponentTreeRowChooser{" + "chooser=" + chooser + '}';
        }
    }

    /**
     * Checks component type.
     */
    public static class JTreeFinder extends Finder {

        /**
         * Constructs JTreeFinder.
         *
         * @param sf other searching criteria.
         */
        public JTreeFinder(ComponentChooser sf) {
            super(JTree.class, sf);
        }

        /**
         * Constructs JTreeFinder.
         */
        public JTreeFinder() {
            super(JTree.class);
        }
    }

    /**
     * Allows to find component by node text.
     */
    public static class JTreeByItemFinder implements ComponentChooser {

        String label;
        int rowIndex;
        StringComparator comparator;

        /**
         * Constructs JTreeByItemFinder.
         *
         * @param lb a text pattern
         * @param ii row index to check. If equal to -1, selected row is
         * checked.
         * @param comparator specifies string comparision algorithm.
         */
        public JTreeByItemFinder(String lb, int ii, StringComparator comparator) {
            label = lb;
            rowIndex = ii;
            this.comparator = comparator;
        }

        /**
         * Constructs JTreeByItemFinder.
         *
         * @param lb a text pattern
         * @param ii row index to check. If equal to -1, selected row is
         * checked.
         */
        public JTreeByItemFinder(String lb, int ii) {
            this(lb, ii, Operator.getDefaultStringComparator());
        }

        @Override
        public boolean checkComponent(Component comp) {
            if (comp instanceof JTree) {
                if (label == null) {
                    return true;
                }
                if (((JTree) comp).getRowCount() > rowIndex) {
                    int ii = rowIndex;
                    if (ii == -1) {
                        int[] rows = ((JTree) comp).getSelectionRows();
                        if (rows != null && rows.length > 0) {
                            ii = rows[0];
                        } else {
                            return false;
                        }
                    }
                    TreePath path = ((JTree) comp).getPathForRow(ii);
                    if (path != null) {
                        return (comparator.equals(path.getPathComponent(path.getPathCount() - 1).toString(),
                                label));
                    }
                }
            }
            return false;
        }

        @Override
        public String getDescription() {
            return ("JTree with text \"" + label + "\" in "
                    + rowIndex + "'th row");
        }

        @Override
        public String toString() {
            return "JTreeByItemFinder{" + "label=" + label + ", rowIndex=" + rowIndex + ", comparator=" + comparator + '}';
        }
    }
}
