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
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.EventObject;
import java.util.Hashtable;

import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.plaf.TableUI;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import org.netbeans.jemmy.ComponentChooser;
import org.netbeans.jemmy.ComponentSearcher;
import org.netbeans.jemmy.JemmyException;
import org.netbeans.jemmy.Outputable;
import org.netbeans.jemmy.QueueTool;
import org.netbeans.jemmy.TestOut;
import org.netbeans.jemmy.TimeoutExpiredException;
import org.netbeans.jemmy.Timeoutable;
import org.netbeans.jemmy.Timeouts;
import org.netbeans.jemmy.Waiter;
import org.netbeans.jemmy.drivers.DriverManager;
import org.netbeans.jemmy.drivers.TableDriver;
import org.netbeans.jemmy.util.EmptyVisualizer;

/**
 * <BR><BR>Timeouts used: <BR>
 * JTableOperator.WaitEditingTimeout - time to wait cell editing<BR>
 * ComponentOperator.WaitComponentTimeout - time to wait component displayed
 * <BR>
 * ComponentOperator.WaitStateTimeout - time to wait for cell contents <BR>
 * JTextComponentOperator.ChangeCaretPositionTimeout - maximum time to chenge
 * caret position <BR>
 * JTextComponentOperator.TypeTextTimeout - maximum time to type text <BR>
 * JScrollBarOperator.WholeScrollTimeout - time for the whole scrolling <BR>.
 *
 * @see Timeouts
 *
 * @author Alexandre Iline (alexandre.iline@oracle.com)
 *
 */
public class JTableOperator extends JComponentOperator
        implements Outputable, Timeoutable {

    /**
     * Identifier for a "cell" property.
     *
     * @see #getDump
     */
    public static final String CELL_PREFIX_DPROP = "Cell";

    /**
     * Identifier for a "column" property.
     *
     * @see #getDump
     */
    public static final String COLUMN_PREFIX_DPROP = "Column";

    /**
     * Identifier for a "selected column" property.
     *
     * @see #getDump
     */
    public static final String SELECTED_COLUMN_PREFIX_DPROP = "SelectedColumn";

    /**
     * Identifier for a "selected row" property.
     *
     * @see #getDump
     */
    public static final String SELECTED_ROW_PREFIX_DPROP = "SelectedRow";

    /**
     * Identifier for a "column count" property.
     *
     * @see #getDump
     */
    public static final String COLUMN_COUNT_DPROP = "Column count";

    /**
     * Identifier for a "row count" property.
     *
     * @see #getDump
     */
    public static final String ROW_COUNT_DPROP = "Row count";

    private final static long WAIT_EDITING_TIMEOUT = 60000;

    private TestOut output;
    private Timeouts timeouts;

    TableDriver driver;

    /**
     * Constructor.
     *
     * @param b a component
     */
    public JTableOperator(JTable b) {
        super(b);
        driver = DriverManager.getTableDriver(getClass());
    }

    /**
     * Constructs a JTableOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     * @param index an index between appropriate ones.
     */
    public JTableOperator(ContainerOperator<?> cont, ComponentChooser chooser, int index) {
        this((JTable) cont.
                waitSubComponent(new JTableFinder(chooser),
                        index));
        copyEnvironment(cont);
    }

    /**
     * Constructs a JTableOperator object.
     *
     * @param cont a container
     * @param chooser a component chooser specifying searching criteria.
     */
    public JTableOperator(ContainerOperator<?> cont, ComponentChooser chooser) {
        this(cont, chooser, 0);
    }

    /**
     * Constructor. Waits by cell text first. Uses cont's timeout and output for
     * waiting and to init operator.
     *
     * @param cont a container
     * @param text Text of a cell which is currently selected.
     * @param row a cell row to check.
     * @param column a cell row to check.
     * @param index Ordinal component index.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public JTableOperator(ContainerOperator<?> cont, String text, int row, int column, int index) {
        this((JTable) waitComponent(cont,
                new JTableByCellFinder(text, row, column,
                        cont.getComparator()),
                index));
        copyEnvironment(cont);
    }

    /**
     * Constructor. Waits by cell text first. Uses cont's timeout and output for
     * waiting and to init operator.
     *
     * @param cont a container
     * @param text Text of a cell which is currently selected.
     * @param row a cell row to check.
     * @param column a cell row to check.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public JTableOperator(ContainerOperator<?> cont, String text, int row, int column) {
        this(cont, text, row, column, 0);
    }

    /**
     * Constructor. Waits by text in selected cell first. Uses cont's timeout
     * and output for waiting and to init operator.
     *
     * @param cont a container
     * @param text Text of a cell which is currently selected.
     * @param index Ordinal component index.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public JTableOperator(ContainerOperator<?> cont, String text, int index) {
        this(cont, text, -1, -1, index);
    }

    /**
     * Constructor. Waits component in container first. Uses cont's timeout and
     * output for waiting and to init operator.
     *
     * @param cont a container
     * @param text Text of a cell which is currently selected.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public JTableOperator(ContainerOperator<?> cont, String text) {
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
    public JTableOperator(ContainerOperator<?> cont, int index) {
        this((JTable) waitComponent(cont,
                new JTableFinder(),
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
    public JTableOperator(ContainerOperator<?> cont) {
        this(cont, 0);
    }

    ////////////////////////////////////////////////////////
    //Static finds                                        //
    ////////////////////////////////////////////////////////
    /**
     * Searches JTable in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @return JTable instance or null if component was not found.
     */
    public static JTable findJTable(Container cont, ComponentChooser chooser, int index) {
        return (JTable) findComponent(cont, new JTableFinder(chooser), index);
    }

    /**
     * Searches 0'th JTable in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return JTable instance or null if component was not found.
     */
    public static JTable findJTable(Container cont, ComponentChooser chooser) {
        return findJTable(cont, chooser, 0);
    }

    /**
     * Searches JTable by cell.
     *
     * @param cont Container to search component in.
     * @param text Item text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param row Index of row to compare text. If -1, selected row is checked.
     * @param column Index of column to compare text. If -1, selected column is
     * checked.
     * @param index Ordinal component index.
     * @return JTable instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static JTable findJTable(Container cont, String text, boolean ce, boolean ccs, int row, int column, int index) {
        return findJTable(cont, new JTableByCellFinder(text, row, column, new DefaultStringComparator(ce, ccs)), index);
    }

    /**
     * Searches JTable by cell.
     *
     * @param cont Container to search component in.
     * @param text Item text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param row Index of row to compare text. If -1, selected row is checked.
     * @param column Index of column to compare text. If -1, selected column is
     * checked.
     * @return JTable instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     */
    public static JTable findJTable(Container cont, String text, boolean ce, boolean ccs, int row, int column) {
        return findJTable(cont, text, ce, ccs, row, column, 0);
    }

    /**
     * Waits JTable in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @param index Ordinal component index.
     * @return JTable instance or null if component was not found.
     * @throws TimeoutExpiredException
     */
    public static JTable waitJTable(Container cont, ComponentChooser chooser, int index) {
        return (JTable) waitComponent(cont, new JTableFinder(chooser), index);
    }

    /**
     * Waits 0'th JTable in container.
     *
     * @param cont Container to search component in.
     * @param chooser org.netbeans.jemmy.ComponentChooser implementation.
     * @return JTable instance or null if component was not found.
     * @throws TimeoutExpiredException
     */
    public static JTable waitJTable(Container cont, ComponentChooser chooser) {
        return waitJTable(cont, chooser, 0);
    }

    /**
     * Waits JTable by cell.
     *
     * @param cont Container to search component in.
     * @param text Item text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param row Index of row to compare text. If -1, selected row is checked.
     * @param column Index of column to compare text. If -1, selected column is
     * checked.
     * @param index Ordinal component index.
     * @return JTable instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public static JTable waitJTable(Container cont, String text, boolean ce, boolean ccs, int row, int column, int index) {
        return waitJTable(cont, new JTableByCellFinder(text, row, column, new DefaultStringComparator(ce, ccs)), index);
    }

    /**
     * Waits JTable by cell.
     *
     * @param cont Container to search component in.
     * @param text Item text. If null, contents is not checked.
     * @param ce Compare text exactly.
     * @param ccs Compare text case sensitively.
     * @param row Index of row to compare text. If -1, selected row is checked.
     * @param column Index of column to compare text. If -1, selected column is
     * checked.
     * @return JTable instance or null if component was not found.
     * @see ComponentOperator#isCaptionEqual(String, String, boolean, boolean)
     * @throws TimeoutExpiredException
     */
    public static JTable waitJTable(Container cont, String text, boolean ce, boolean ccs, int row, int column) {
        return waitJTable(cont, text, ce, ccs, row, column, 0);
    }

    static {
        Timeouts.initDefault("JTableOperator.WaitEditingTimeout", WAIT_EDITING_TIMEOUT);
    }

    ////////////////////////////////////////////////////////
    //Environment                                         //
    ////////////////////////////////////////////////////////
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
        super.setOutput(output);
    }

    @Override
    public TestOut getOutput() {
        return output;
    }

    @Override
    public void copyEnvironment(Operator anotherOperator) {
        super.copyEnvironment(anotherOperator);
        driver
                = (TableDriver) DriverManager.
                getDriver(DriverManager.TABLE_DRIVER_ID,
                        getClass(),
                        anotherOperator.getProperties());
    }

    ////////////////////////////////////////////////////////
    //Find methods                                        //
    ////////////////////////////////////////////////////////
    //text, comparator and index
    /**
     * Searches cell coordinates.
     *
     * @param text a text pattern
     * @param comparator a string comparision algorithm
     * @param index an ordinal cell index
     * @return Point indicating coordinates (x - column, y - row)
     */
    public Point findCell(String text, StringComparator comparator, int index) {
        return (findCell(new BySubStringTableCellChooser(text,
                comparator),
                index));
    }

    /**
     * Searches cell coordinates in the specified rows and columns.
     *
     * @param text a text pattern
     * @param comparator a string comparision algorithm
     * @param rows rows to search in
     * @param columns columns to search in
     * @param index an ordinal cell index
     * @return Point indicating coordinates (x - column, y - row)
     */
    public Point findCell(String text, StringComparator comparator, int[] rows, int[] columns, int index) {
        return findCell(new BySubStringTableCellChooser(text, comparator), rows, columns, index);
    }

    /**
     * Searches cell row index.
     *
     * @param text a text pattern
     * @param comparator a string comparision algorithm
     * @param index an ordinal cell index
     * @return a row index.
     */
    public int findCellRow(String text, StringComparator comparator, int index) {
        return findCell(text, comparator, index).y;
    }

    /**
     * Searches cell row index. Searching is performed between cells in one
     * column.
     *
     * @param text a text pattern
     * @param comparator a string comparision algorithm
     * @param column a column index to search in
     * @param index an ordinal cell index
     * @return a row index.
     */
    public int findCellRow(String text, StringComparator comparator, int column, int index) {
        return findCell(text, comparator, null, new int[]{column}, index).y;
    }

    /**
     * Searches cell column visible index.
     *
     * @param text a text pattern
     * @param comparator a string comparision algorithm
     * @param index an ordinal cell index
     * @return a column index.
     */
    public int findCellColumn(String text, StringComparator comparator, int index) {
        return findCell(text, comparator, index).x;
    }

    /**
     * Searches cell column index. Searching is performed between cells in one
     * row.
     *
     * @param text a text pattern
     * @param comparator a string comparision algorithm
     * @param row a row index to search in
     * @param index an ordinal cell index
     * @return a column index.
     */
    public int findCellColumn(String text, StringComparator comparator, int row, int index) {
        return findCell(text, comparator, new int[]{row}, null, index).x;
    }

    //booleans - deprecated
    /**
     * Searches cell row by cell text.
     *
     * @param text Text to search by.
     * @param ce Compare exactly.
     * @param ccs Compare case sensitively.
     * @param index Ordinal index in suitable cells.
     * @return a row index.
     * @see #findCellRow(String, int)
     * @deprecated Use findCellRow(String, int) or findCellRow(String,
     * StringComparator, int)
     */
    @Deprecated
    public int findCellRow(String text, boolean ce, boolean ccs, int index) {
        return findCell(text, ce, ccs, index).y;
    }

    /**
     * Searches cell column by cell text.
     *
     * @param text Text to search by.
     * @param ce Compare exactly.
     * @param ccs Compare case sensitively.
     * @param index Ordinal index in suitable cells.
     * @return a column index.
     * @see #findCellColumn(String, int)
     * @deprecated Use findCellColumn(String, int) or findCellColumn(String,
     * StringComparator, int)
     */
    @Deprecated
    public int findCellColumn(String text, boolean ce, boolean ccs, int index) {
        return findCell(text, ce, ccs, index).x;
    }

    /**
     * Searches first cell row by cell text.
     *
     * @param text Text to search by.
     * @param ce Compare exactly.
     * @param ccs Compare case sensitively.
     * @return a row index.
     * @see #findCellRow(String)
     * @deprecated Use findCellRow(String) or findCellRow(String,
     * StringComparator)
     */
    @Deprecated
    public int findCellRow(String text, boolean ce, boolean ccs) {
        return findCellRow(text, ce, ccs, 0);
    }

    /**
     * Searches first cell column by cell text.
     *
     * @param text Text to search by.
     * @param ce Compare exactly.
     * @param ccs Compare case sensitively.
     * @return a column index.
     * @see #findCellColumn(String)
     * @deprecated Use findCellColumn(String) or findCellColumn(String,
     * StringComparator)
     */
    @Deprecated
    public int findCellColumn(String text, boolean ce, boolean ccs) {
        return findCellColumn(text, ce, ccs, 0);
    }

    //text and comparator only
    /**
     * Searches cell row index.
     *
     * @param text a text pattern
     * @param comparator a string comparision algorithm
     * @return a row index.
     */
    public int findCellRow(String text, StringComparator comparator) {
        return findCellRow(text, comparator, 0);
    }

    /**
     * Searches cell column visible index.
     *
     * @param text a text pattern
     * @param comparator a string comparision algorithm
     * @return a column index.
     */
    public int findCellColumn(String text, StringComparator comparator) {
        return findCellColumn(text, comparator, 0);
    }

    //text and index
    /**
     * Searches cell row by cell text.
     *
     * @param text Text to search by.
     * @param index Ordinal index in suitable cells.
     * @return a row index.
     */
    public int findCellRow(String text, int index) {
        return findCell(text, index).y;
    }

    /**
     * Searches cell row index. Searching is performed between cells in one
     * column.
     *
     * @param text a text pattern
     * @param column a column index to search in
     * @param index Ordinal index in suitable cells.
     * @return a row index.
     */
    public int findCellRow(String text, int column, int index) {
        return findCell(text, null, new int[]{column}, index).y;
    }

    /**
     * Searches cell column by cell text.
     *
     * @param text Text to search by.
     * @param index Ordinal index in suitable cells.
     * @return a column index.
     */
    public int findCellColumn(String text, int index) {
        return findCell(text, index).x;
    }

    /**
     * Searches cell column index. Searching is performed between cells in one
     * row.
     *
     * @param text a text pattern
     * @param row a row index to search in
     * @param index Ordinal index in suitable cells.
     * @return a column index.
     */
    public int findCellColumn(String text, int row, int index) {
        return findCell(text, new int[]{row}, null, index).x;
    }

    /**
     * Searches cell coordinates.
     *
     * @param text a text pattern
     * @param index an ordinal cell index
     * @return Point indicating coordinates (x - column, y - row)
     */
    public Point findCell(String text, int index) {
        return findCell(text, getComparator(), index);
    }

    /**
     * Searches cell coordinates in the specified rows and columns.
     *
     * @param text a text pattern
     * @param rows rows to search in
     * @param columns columns to search in
     * @param index an ordinal cell index
     * @return Point indicating coordinates (x - column, y - row)
     */
    public Point findCell(String text, int[] rows, int[] columns, int index) {
        return findCell(new BySubStringTableCellChooser(text, getComparator()), rows, columns, index);
    }

    //text only
    /**
     * Searches first cell row by cell text.
     *
     * @param text Text to search by.
     * @return a row index.
     */
    public int findCellRow(String text) {
        return findCellRow(text, 0);
    }

    /**
     * Searches first cell column by cell text.
     *
     * @param text Text to search by.
     * @return a column index.
     */
    public int findCellColumn(String text) {
        return findCellColumn(text, 0);
    }

    //component chooser and index
    /**
     * Searches cell row by rendered component.
     *
     * @param chooser Component verifying object.
     * @param index Ordinal index in suitable cells.
     * @return a row index.
     */
    public int findCellRow(ComponentChooser chooser, int index) {
        return findCell(chooser, index).y;
    }

    /**
     * Searches cell row index. Searching is performed between cells in one
     * column.
     *
     * @param chooser Component verifying object.
     * @param column a column index to search in
     * @param index Ordinal index in suitable cells.
     * @return a row index.
     */
    public int findCellRow(ComponentChooser chooser, int column, int index) {
        return findCell(chooser, null, new int[]{column}, index).y;
    }

    /**
     * Searches cell column by rendered component.
     *
     * @param chooser Component verifying object.
     * @param index Ordinal index in suitable cells.
     * @return a column index.
     */
    public int findCellColumn(ComponentChooser chooser, int index) {
        return findCell(chooser, index).x;
    }

    /**
     * Searches cell column index. Searching is performed between cells in one
     * row.
     *
     * @param chooser Component verifying object.
     * @param row a row index to search in
     * @param index Ordinal index in suitable cells.
     * @return a column index.
     */
    public int findCellColumn(ComponentChooser chooser, int row, int index) {
        return findCell(chooser, new int[]{row}, null, index).x;
    }

    /**
     * Searches cell coordinates.
     *
     * @param chooser Component verifying object.
     * @param index an ordinal cell index
     * @return Point indicating coordinates (x - column, y - row)
     */
    public Point findCell(ComponentChooser chooser, int index) {
        return findCell(new ByRenderedComponentTableCellChooser(chooser), index);
    }

    /**
     * Searches cell coordinates.
     *
     * @param chooser Component verifying object.
     * @param rows rows to search in
     * @param columns columns to search in
     * @param index an ordinal cell index
     * @return Point indicating coordinates (x - column, y - row)
     */
    public Point findCell(ComponentChooser chooser, int[] rows, int[] columns, int index) {
        return findCell(new ByRenderedComponentTableCellChooser(chooser), rows, columns, index);
    }

    //component chooser only
    /**
     * Searches cell row by rendered component.
     *
     * @param chooser Component verifying object.
     * @return a row index.
     */
    public int findCellRow(ComponentChooser chooser) {
        return findCellRow(chooser, 0);
    }

    /**
     * Searches cell column by rendered component.
     *
     * @param chooser Component verifying object.
     * @return a column index.
     */
    public int findCellColumn(ComponentChooser chooser) {
        return findCellColumn(chooser, 0);
    }

    /**
     * Searches cell coordinates.
     *
     * @param chooser Component verifying object.
     * @return Point indicating coordinates (x - column, y - row)
     */
    public Point findCell(ComponentChooser chooser) {
        return findCell(chooser, 0);
    }

    //cell chooser and index
    /**
     * Searches cell row by TableCellChooser.
     *
     * @param chooser Cell verifying object.
     * @param index Ordinal index in suitable cells.
     * @return a row index.
     */
    public int findCellRow(TableCellChooser chooser, int index) {
        return findCell(chooser, index).y;
    }

    /**
     * Searches cell row index. Searching is performed between cells in one
     * column.
     *
     * @param chooser Cell verifying object.
     * @param column a column index to search in
     * @param index Ordinal index in suitable cells.
     * @return a row index.
     */
    public int findCellRow(TableCellChooser chooser, int column, int index) {
        return findCell(chooser, null, new int[]{column}, index).y;
    }

    /**
     * Searches cell column by TableCellChooser.
     *
     * @param chooser ComponentCell verifying object.
     * @param index Ordinal index in suitable cells.
     * @return a column index.
     */
    public int findCellColumn(TableCellChooser chooser, int index) {
        return findCell(chooser, index).x;
    }

    /**
     * Searches cell column index. Searching is performed between cells in one
     * row.
     *
     * @param chooser Cell verifying object.
     * @param row a row index to search in
     * @param index Ordinal index in suitable cells.
     * @return a column index.
     */
    public int findCellColumn(TableCellChooser chooser, int row, int index) {
        return findCell(chooser, new int[]{row}, null, index).x;
    }

    /**
     * Searches cell coordinates.
     *
     * @param chooser cell verifying object.
     * @param index an ordinal cell index
     * @return Point indicating coordinates (x - column, y - row)
     */
    public Point findCell(TableCellChooser chooser, int index) {
        return findCell(chooser, null, null, index);
    }

    /**
     * Searches cell coordinates in the specified rows and columns.
     *
     * @param chooser cell verifying object.
     * @param rows rows to search in
     * @param columns columns to search in
     * @param index an ordinal cell index
     * @return Point indicating coordinates (x - column, y - row)
     */
    public Point findCell(TableCellChooser chooser, int[] rows, int[] columns, int index) {
        TableModel model = getModel();
        int[] realRows;
        if (rows != null) {
            realRows = rows;
        } else {
            realRows = new int[model.getRowCount()];
            for (int i = 0; i < model.getRowCount(); i++) {
                realRows[i] = i;
            }
        }
        int[] realColumns;
        if (columns != null) {
            realColumns = columns;
        } else {
            realColumns = new int[model.getColumnCount()];
            for (int i = 0; i < model.getColumnCount(); i++) {
                realColumns[i] = i;
            }
        }
        int count = 0;
        for (int realRow : realRows) {
            for (int realColumn : realColumns) {
                if (chooser.checkCell(this, realRow, realColumn)) {
                    if (count == index) {
                        return new Point(realColumn, realRow);
                    } else {
                        count++;
                    }
                }
            }
        }
        return new Point(-1, -1);
    }

    //cell chooser only
    /**
     * Searches cell row by TableCellChooser.
     *
     * @param chooser Cell verifying object.
     * @return a row index.
     */
    public int findCellRow(TableCellChooser chooser) {
        return findCellRow(chooser, 0);
    }

    /**
     * Searches cell column by TableCellChooser.
     *
     * @param chooser Cell verifying object.
     * @return a column index.
     */
    public int findCellColumn(TableCellChooser chooser) {
        return findCellColumn(chooser, 0);
    }

    /**
     * Searches cell coordinates.
     *
     * @param chooser cell verifying object.
     * @return Point indicating coordinates (x - column, y - row)
     */
    public Point findCell(TableCellChooser chooser) {
        return findCell(chooser, 0);
    }

    ////////////////////////////////////////////////////////
    //Actions                                             //
    ////////////////////////////////////////////////////////
    /**
     * Does mouse click on the cell.
     *
     * @param row a row index
     * @param column a column index
     * @param clickCount a number of clicks
     * @param button a mouse button (value of InputEvent.BUTTON*_MASK field)
     * @param modifiers Modifiers (combination of InputEvent.*_MASK values)
     */
    public void clickOnCell(final int row, final int column, final int clickCount, final int button, final int modifiers) {
        output.printLine("Click on ("
                + Integer.toString(row) + ", "
                + Integer.toString(column)
                + ") cell");
        output.printGolden("Click on cell");
        makeComponentVisible();
        scrollToCell(row, column);
        getQueueTool().invokeSmoothly(new QueueTool.QueueAction<Void>("Path selecting") {
            @Override
            public Void launch() {
                Point point = getPointToClick(row, column);
                clickMouse(point.x, point.y, clickCount, button, modifiers);
                return null;
            }
        });
    }

    /**
     * Does mouse click on the cell with no modifiers.
     *
     * @param row a row index
     * @param column a column index
     * @param clickCount a number of clicks
     * @param button a mouse button (value of InputEvent.BUTTON*_MASK field)
     */
    public void clickOnCell(int row, int column, int clickCount, int button) {
        clickOnCell(row, column, clickCount, button, 0);
    }

    /**
     * Does mouse click on the cell by default mouse button with no modifiers.
     *
     * @param row a row index
     * @param column a column index
     * @param clickCount a number of clicks
     */
    public void clickOnCell(int row, int column, int clickCount) {
        clickOnCell(row, column, clickCount, getDefaultMouseButton());
    }

    /**
     * Does single mouse click on the cell.
     *
     * @param row a row index
     * @param column a column index
     */
    public void clickOnCell(int row, int column) {
        clickOnCell(row, column, 1);
    }

    /**
     * Double clicks on cell to turns it to the editing mode.
     *
     * @param row a row index
     * @param column a column index
     * @throws TimeoutExpiredException
     */
    public void clickForEdit(int row, int column) {
        clickOnCell(row, column, 2);
    }

    /**
     * Changes text of the cell pointed by row and column indexes.
     *
     * @param row a row index
     * @param column a column index
     * @param newText a text to enter
     * @deprecated Use changeCellObject(int, int, Object) instead.
     * @see #changeCellObject(int, int, Object)
     * @throws TimeoutExpiredException
     */
    @Deprecated
    public void changeCellText(int row, int column, String newText) {
        changeCellObject(row, column, newText);
    }

    /**
     * Changes value of the cell pointed by row and column indexes. uses editor
     * defined by setCellEditor method.
     *
     * @param row a row index
     * @param column a column index
     * @param newValue an object to chenge cell value to.
     * @throws TimeoutExpiredException
     */
    public void changeCellObject(int row, int column, Object newValue) {
        driver.editCell(this, row, column, newValue);
    }

    /**
     * Scrolls to a cell if the table lies on a JScrollPane component.
     *
     * @param row a row index
     * @param column a column index
     * @throws TimeoutExpiredException
     */
    public void scrollToCell(int row, int column) {
        output.printTrace("Scroll JTable to (" + Integer.toString(row)
                + "," + Integer.toString(column) + ") cell\n    : "
                + toStringSource());
        output.printGolden("Scroll JTable to (" + Integer.toString(row)
                + "," + Integer.toString(column) + ")");
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
        Rectangle rect = getCellRect(row, column, false);
        scroller.scrollToComponentRectangle(getSource(),
                (int) rect.getX(),
                (int) rect.getY(),
                (int) rect.getWidth(),
                (int) rect.getHeight());
    }

    /**
     * Selects a specified cell.
     *
     * @param row a row index
     * @param column a column index
     */
    public void selectCell(int row, int column) {
        driver.selectCell(this, row, column);
    }

    /**
     * Searches a column by name.
     *
     * @param name a text pattern
     * @param comparator a string comparision algorithm
     * @return a column index
     */
    public int findColumn(String name, StringComparator comparator) {
        int columnCount = getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            if (comparator.equals(getColumnName(i),
                    name)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Searches a column by name.
     *
     * @param name a text pattern
     * @return a column index
     */
    public int findColumn(String name) {
        return findColumn(name, getComparator());
    }

    /**
     * Calls popup menu on specified cell.
     *
     * @param row a row index
     * @param column a column index
     * @return the menu
     */
    public JPopupMenu callPopupOnCell(int row, int column) {
        output.printLine("Call popup on (" + row + ", " + column + ") cell");
        output.printGolden("Call popup on cell");
        makeComponentVisible();
        Point point = getPointToClick(row, column);
        return (JPopupMenuOperator.callPopup(getSource(),
                (int) point.getX(),
                (int) point.getY(),
                getPopupMouseButton()));
    }

    ////////////////////////////////////////////////////////
    //Gets                                                //
    ////////////////////////////////////////////////////////
    /**
     * Ask renderer for component to be displayed.
     *
     * @param row cell row index.
     * @param column cell column visible index.
     * @param isSelected True if the specified cell was selected.
     * @param cellHasFocus True if the specified cell has the focus.
     * @return Component to be displayed.
     */
    public Component getRenderedComponent(int row, int column, boolean isSelected, boolean cellHasFocus) {
        return (getCellRenderer(row, column).
                getTableCellRendererComponent((JTable) getSource(),
                        getValueAt(row, column),
                        isSelected,
                        cellHasFocus,
                        row,
                        column));
    }

    /**
     * Ask renderer for component to be displayed. Uses
     * isCellSelected(itemIndex) to determine whether cell is selected. Supposes
     * item do not have focus.
     *
     * @param row cell row index.
     * @param column cell column visible index.
     * @return Component to be displayed.
     */
    public Component getRenderedComponent(int row, int column) {
        return (getRenderedComponent(row, column, isCellSelected(row, column),
                false));
    }

    /**
     * Returns a point at the center of the cell rectangle.
     *
     * @param row a row index
     * @param column a column index
     * @return a Point in component's coordinate system.
     */
    public Point getPointToClick(int row, int column) {
        Rectangle rect = getCellRect(row, column, false);
        return (new Point((int) (rect.getX() + rect.getWidth() / 2),
                (int) (rect.getY() + rect.getHeight() / 2)));
    }

    /**
     * Creates an operator for a teble header assigned to this table.
     *
     * @return an JTableHeaderOperator operator
     */
    public JTableHeaderOperator getHeaderOperator() {
        return new JTableHeaderOperator(getTableHeader());
    }

    /**
     * Waits for an editor.
     *
     * @param chooser a chhoser specifying criteria to distinguish cell editor.
     * @param row a row index
     * @param column a column index
     * @return a component displayed over the cell and fitting the criteria
     * specified by {@code chooser}
     */
    public Component waitCellComponent(ComponentChooser chooser, int row, int column) {
        CellComponentWaiter waiter = new CellComponentWaiter(chooser, row, column);
        waiter.setOutput(getOutput());
        waiter.setTimeoutsToCloneOf(getTimeouts(), "JTableOperator.WaitEditingTimeout");
        try {
            return waiter.waitAction(null);
        } catch (InterruptedException e) {
            throw (new JemmyException("Waiting has been interrupted", e));
        }
    }

    /**
     * Waits for certain cell contents.
     *
     * @param cellText Text comparing to cell text by
     * {@code getComparator()} comparator.
     * @param row cell row index. If -1, selected one is checked.
     * @param column cell column visible index. If -1, selected one is checked.
     */
    public void waitCell(String cellText, int row, int column) {
        getOutput().printLine("Wait \"" + cellText + "\" text at ("
                + Integer.toString(row) + ","
                + Integer.toString(column) + ")"
                + " position in component \n    : "
                + toStringSource());
        getOutput().printGolden("Wait  \"" + cellText + "\" text at ("
                + Integer.toString(row) + ","
                + Integer.toString(column) + ")"
                + " position");
        waitState(new JTableByCellFinder(cellText, row, column, getComparator()));
    }

    /**
     * Returns information about component.
     */
    @Override
    public Hashtable<String, Object> getDump() {
        Hashtable<String, Object> result = super.getDump();
        TableModel model = ((JTable) getSource()).getModel();
        int colCount = model.getColumnCount();
        int rowCount = model.getRowCount();
        String[][] items = new String[rowCount][colCount];
        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < colCount; j++) {
                if (model.getValueAt(i, j) != null) {
                    items[i][j] = model.getValueAt(i, j).toString();
                } else {
                    items[i][j] = "null";
                }
            }
        }
        addToDump(result, CELL_PREFIX_DPROP, items);
        String[] columns = new String[colCount];
        for (int j = 0; j < colCount; j++) {
            columns[j] = ((JTable) getSource()).getColumnName(j);
        }
        addToDump(result, COLUMN_PREFIX_DPROP, columns);
        int[] selColNums = ((JTable) getSource()).getSelectedColumns();
        String[] selColumns = new String[selColNums.length];
        for (int j = 0; j < selColNums.length; j++) {
            selColumns[j] = Integer.toString(selColNums[j]);
        }
        addToDump(result, SELECTED_COLUMN_PREFIX_DPROP, selColumns);
        int[] selRowNums = ((JTable) getSource()).getSelectedRows();
        String[] selRows = new String[selRowNums.length];
        for (int i = 0; i < selRowNums.length; i++) {
            selRows[i] = Integer.toString(selRowNums[i]);
        }
        addToDump(result, SELECTED_ROW_PREFIX_DPROP, selRows);
        result.put(COLUMN_COUNT_DPROP, Integer.toString(colCount));
        result.put(ROW_COUNT_DPROP, Integer.toString(rowCount));
        return result;
    }

    ////////////////////////////////////////////////////////
    //Mapping                                             //
    /**
     * Maps {@code JTable.addColumn(TableColumn)} through queue
     */
    public void addColumn(final TableColumn tableColumn) {
        runMapping(new MapVoidAction("addColumn") {
            @Override
            public void map() {
                ((JTable) getSource()).addColumn(tableColumn);
            }
        });
    }

    /**
     * Maps {@code JTable.addColumnSelectionInterval(int, int)} through queue
     */
    public void addColumnSelectionInterval(final int i, final int i1) {
        runMapping(new MapVoidAction("addColumnSelectionInterval") {
            @Override
            public void map() {
                ((JTable) getSource()).addColumnSelectionInterval(i, i1);
            }
        });
    }

    /**
     * Maps {@code JTable.addRowSelectionInterval(int, int)} through queue
     */
    public void addRowSelectionInterval(final int i, final int i1) {
        runMapping(new MapVoidAction("addRowSelectionInterval") {
            @Override
            public void map() {
                ((JTable) getSource()).addRowSelectionInterval(i, i1);
            }
        });
    }

    /**
     * Maps {@code JTable.changeSelection(int, int, boolean, boolean)} through queue
     */
    public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
        runMapping(new MapVoidAction("clearSelection") {
            @Override
            public void map() {
                ((JTable) getSource()).changeSelection(rowIndex, columnIndex, toggle, extend);
            }
        });
    }

    /**
     * Maps {@code JTable.clearSelection()} through queue
     */
    public void clearSelection() {
        runMapping(new MapVoidAction("clearSelection") {
            @Override
            public void map() {
                ((JTable) getSource()).clearSelection();
            }
        });
    }

    /**
     * Maps {@code JTable.columnAdded(TableColumnModelEvent)} through queue
     */
    public void columnAdded(final TableColumnModelEvent tableColumnModelEvent) {
        runMapping(new MapVoidAction("columnAdded") {
            @Override
            public void map() {
                ((JTable) getSource()).columnAdded(tableColumnModelEvent);
            }
        });
    }

    /**
     * Maps {@code JTable.columnAtPoint(Point)} through queue
     */
    public int columnAtPoint(final Point point) {
        return (runMapping(new MapIntegerAction("columnAtPoint") {
            @Override
            public int map() {
                return ((JTable) getSource()).columnAtPoint(point);
            }
        }));
    }

    /**
     * Maps {@code JTable.columnMarginChanged(ChangeEvent)} through queue
     */
    public void columnMarginChanged(final ChangeEvent changeEvent) {
        runMapping(new MapVoidAction("columnMarginChanged") {
            @Override
            public void map() {
                ((JTable) getSource()).columnMarginChanged(changeEvent);
            }
        });
    }

    /**
     * Maps {@code JTable.columnMoved(TableColumnModelEvent)} through queue
     */
    public void columnMoved(final TableColumnModelEvent tableColumnModelEvent) {
        runMapping(new MapVoidAction("columnMoved") {
            @Override
            public void map() {
                ((JTable) getSource()).columnMoved(tableColumnModelEvent);
            }
        });
    }

    /**
     * Maps {@code JTable.columnRemoved(TableColumnModelEvent)} through queue
     */
    public void columnRemoved(final TableColumnModelEvent tableColumnModelEvent) {
        runMapping(new MapVoidAction("columnRemoved") {
            @Override
            public void map() {
                ((JTable) getSource()).columnRemoved(tableColumnModelEvent);
            }
        });
    }

    /**
     * Maps {@code JTable.columnSelectionChanged(ListSelectionEvent)}
     * through queue
     */
    public void columnSelectionChanged(final ListSelectionEvent listSelectionEvent) {
        runMapping(new MapVoidAction("columnSelectionChanged") {
            @Override
            public void map() {
                ((JTable) getSource()).columnSelectionChanged(listSelectionEvent);
            }
        });
    }

    /**
     * Maps {@code JTable.convertColumnIndexToModel(int)} through queue
     */
    public int convertColumnIndexToModel(final int i) {
        return (runMapping(new MapIntegerAction("convertColumnIndexToModel") {
            @Override
            public int map() {
                return ((JTable) getSource()).convertColumnIndexToModel(i);
            }
        }));
    }

    /**
     * Maps {@code JTable.convertColumnIndexToView(int)} through queue
     */
    public int convertColumnIndexToView(final int i) {
        return (runMapping(new MapIntegerAction("convertColumnIndexToView") {
            @Override
            public int map() {
                return ((JTable) getSource()).convertColumnIndexToView(i);
            }
        }));
    }

    /**
     * Maps {@code JTable.createDefaultColumnsFromModel()} through queue
     */
    public void createDefaultColumnsFromModel() {
        runMapping(new MapVoidAction("createDefaultColumnsFromModel") {
            @Override
            public void map() {
                ((JTable) getSource()).createDefaultColumnsFromModel();
            }
        });
    }

    /**
     * Maps {@code JTable.editCellAt(int, int)} through queue
     */
    public boolean editCellAt(final int i, final int i1) {
        return (runMapping(new MapBooleanAction("editCellAt") {
            @Override
            public boolean map() {
                return ((JTable) getSource()).editCellAt(i, i1);
            }
        }));
    }

    /**
     * Maps {@code JTable.editCellAt(int, int, EventObject)} through queue
     */
    public boolean editCellAt(final int i, final int i1, final EventObject eventObject) {
        return (runMapping(new MapBooleanAction("editCellAt") {
            @Override
            public boolean map() {
                return ((JTable) getSource()).editCellAt(i, i1, eventObject);
            }
        }));
    }

    /**
     * Maps {@code JTable.editingCanceled(ChangeEvent)} through queue
     */
    public void editingCanceled(final ChangeEvent changeEvent) {
        runMapping(new MapVoidAction("editingCanceled") {
            @Override
            public void map() {
                ((JTable) getSource()).editingCanceled(changeEvent);
            }
        });
    }

    /**
     * Maps {@code JTable.editingStopped(ChangeEvent)} through queue
     */
    public void editingStopped(final ChangeEvent changeEvent) {
        runMapping(new MapVoidAction("editingStopped") {
            @Override
            public void map() {
                ((JTable) getSource()).editingStopped(changeEvent);
            }
        });
    }

    /**
     * Maps {@code JTable.getAutoCreateColumnsFromModel()} through queue
     */
    public boolean getAutoCreateColumnsFromModel() {
        return (runMapping(new MapBooleanAction("getAutoCreateColumnsFromModel") {
            @Override
            public boolean map() {
                return ((JTable) getSource()).getAutoCreateColumnsFromModel();
            }
        }));
    }

    /**
     * Maps {@code JTable.getAutoResizeMode()} through queue
     */
    public int getAutoResizeMode() {
        return (runMapping(new MapIntegerAction("getAutoResizeMode") {
            @Override
            public int map() {
                return ((JTable) getSource()).getAutoResizeMode();
            }
        }));
    }

    /**
     * Maps {@code JTable.getCellEditor()} through queue
     */
    public TableCellEditor getCellEditor() {
        return (runMapping(new MapAction<TableCellEditor>("getCellEditor") {
            @Override
            public TableCellEditor map() {
                return ((JTable) getSource()).getCellEditor();
            }
        }));
    }

    /**
     * Maps {@code JTable.getCellEditor(int, int)} through queue
     */
    public TableCellEditor getCellEditor(final int i, final int i1) {
        return (runMapping(new MapAction<TableCellEditor>("getCellEditor") {
            @Override
            public TableCellEditor map() {
                return ((JTable) getSource()).getCellEditor(i, i1);
            }
        }));
    }

    /**
     * Maps {@code JTable.getCellRect(int, int, boolean)} through queue
     */
    public Rectangle getCellRect(final int i, final int i1, final boolean b) {
        return (runMapping(new MapAction<Rectangle>("getCellRect") {
            @Override
            public Rectangle map() {
                return ((JTable) getSource()).getCellRect(i, i1, b);
            }
        }));
    }

    /**
     * Maps {@code JTable.getCellRenderer(int, int)} through queue
     */
    public TableCellRenderer getCellRenderer(final int i, final int i1) {
        return (runMapping(new MapAction<TableCellRenderer>("getCellRenderer") {
            @Override
            public TableCellRenderer map() {
                return ((JTable) getSource()).getCellRenderer(i, i1);
            }
        }));
    }

    /**
     * Maps {@code JTable.getCellSelectionEnabled()} through queue
     */
    public boolean getCellSelectionEnabled() {
        return (runMapping(new MapBooleanAction("getCellSelectionEnabled") {
            @Override
            public boolean map() {
                return ((JTable) getSource()).getCellSelectionEnabled();
            }
        }));
    }

    /**
     * Maps {@code JTable.getColumn(Object)} through queue
     */
    public TableColumn getColumn(final Object object) {
        return (runMapping(new MapAction<TableColumn>("getColumn") {
            @Override
            public TableColumn map() {
                return ((JTable) getSource()).getColumn(object);
            }
        }));
    }

    /**
     * Maps {@code JTable.getColumnClass(int)} through queue
     */
    public Class<?> getColumnClass(final int i) {
        return (runMapping(new MapAction<Class<?>>("getColumnClass") {
            @Override
            public Class<?> map() {
                return ((JTable) getSource()).getColumnClass(i);
            }
        }));
    }

    /**
     * Maps {@code JTable.getColumnCount()} through queue
     */
    public int getColumnCount() {
        return (runMapping(new MapIntegerAction("getColumnCount") {
            @Override
            public int map() {
                return ((JTable) getSource()).getColumnCount();
            }
        }));
    }

    /**
     * Maps {@code JTable.getColumnModel()} through queue
     */
    public TableColumnModel getColumnModel() {
        return (runMapping(new MapAction<TableColumnModel>("getColumnModel") {
            @Override
            public TableColumnModel map() {
                return ((JTable) getSource()).getColumnModel();
            }
        }));
    }

    /**
     * Maps {@code JTable.getColumnName(int)} through queue
     */
    public String getColumnName(final int i) {
        return (runMapping(new MapAction<String>("getColumnName") {
            @Override
            public String map() {
                return ((JTable) getSource()).getColumnName(i);
            }
        }));
    }

    /**
     * Maps {@code JTable.getColumnSelectionAllowed()} through queue
     */
    public boolean getColumnSelectionAllowed() {
        return (runMapping(new MapBooleanAction("getColumnSelectionAllowed") {
            @Override
            public boolean map() {
                return ((JTable) getSource()).getColumnSelectionAllowed();
            }
        }));
    }

    /**
     * Maps {@code JTable.getDefaultEditor(Class)} through queue
     */
    public TableCellEditor getDefaultEditor(final Class<?> clss) {
        return (runMapping(new MapAction<TableCellEditor>("getDefaultEditor") {
            @Override
            public TableCellEditor map() {
                return ((JTable) getSource()).getDefaultEditor(clss);
            }
        }));
    }

    /**
     * Maps {@code JTable.getDefaultRenderer(Class)} through queue
     */
    public TableCellRenderer getDefaultRenderer(final Class<?> clss) {
        return (runMapping(new MapAction<TableCellRenderer>("getDefaultRenderer") {
            @Override
            public TableCellRenderer map() {
                return ((JTable) getSource()).getDefaultRenderer(clss);
            }
        }));
    }

    /**
     * Maps {@code JTable.getEditingColumn()} through queue
     */
    public int getEditingColumn() {
        return (runMapping(new MapIntegerAction("getEditingColumn") {
            @Override
            public int map() {
                return ((JTable) getSource()).getEditingColumn();
            }
        }));
    }

    /**
     * Maps {@code JTable.getEditingRow()} through queue
     */
    public int getEditingRow() {
        return (runMapping(new MapIntegerAction("getEditingRow") {
            @Override
            public int map() {
                return ((JTable) getSource()).getEditingRow();
            }
        }));
    }

    /**
     * Maps {@code JTable.getEditorComponent()} through queue
     */
    public Component getEditorComponent() {
        return (runMapping(new MapAction<Component>("getEditorComponent") {
            @Override
            public Component map() {
                return ((JTable) getSource()).getEditorComponent();
            }
        }));
    }

    /**
     * Maps {@code JTable.getGridColor()} through queue
     */
    public Color getGridColor() {
        return (runMapping(new MapAction<Color>("getGridColor") {
            @Override
            public Color map() {
                return ((JTable) getSource()).getGridColor();
            }
        }));
    }

    /**
     * Maps {@code JTable.getIntercellSpacing()} through queue
     */
    public Dimension getIntercellSpacing() {
        return (runMapping(new MapAction<Dimension>("getIntercellSpacing") {
            @Override
            public Dimension map() {
                return ((JTable) getSource()).getIntercellSpacing();
            }
        }));
    }

    /**
     * Maps {@code JTable.getModel()} through queue
     */
    public TableModel getModel() {
        return (runMapping(new MapAction<TableModel>("getModel") {
            @Override
            public TableModel map() {
                return ((JTable) getSource()).getModel();
            }
        }));
    }

    /**
     * Maps {@code JTable.getPreferredScrollableViewportSize()} through queue
     */
    public Dimension getPreferredScrollableViewportSize() {
        return (runMapping(new MapAction<Dimension>("getPreferredScrollableViewportSize") {
            @Override
            public Dimension map() {
                return ((JTable) getSource()).getPreferredScrollableViewportSize();
            }
        }));
    }

    /**
     * Maps {@code JTable.getRowCount()} through queue
     */
    public int getRowCount() {
        return (runMapping(new MapIntegerAction("getRowCount") {
            @Override
            public int map() {
                return ((JTable) getSource()).getRowCount();
            }
        }));
    }

    /**
     * Maps {@code JTable.getRowHeight()} through queue
     */
    public int getRowHeight() {
        return (runMapping(new MapIntegerAction("getRowHeight") {
            @Override
            public int map() {
                return ((JTable) getSource()).getRowHeight();
            }
        }));
    }

    /**
     * Maps {@code JTable.getRowMargin()} through queue
     */
    public int getRowMargin() {
        return (runMapping(new MapIntegerAction("getRowMargin") {
            @Override
            public int map() {
                return ((JTable) getSource()).getRowMargin();
            }
        }));
    }

    /**
     * Maps {@code JTable.getRowSelectionAllowed()} through queue
     */
    public boolean getRowSelectionAllowed() {
        return (runMapping(new MapBooleanAction("getRowSelectionAllowed") {
            @Override
            public boolean map() {
                return ((JTable) getSource()).getRowSelectionAllowed();
            }
        }));
    }

    /**
     * Maps {@code JTable.getScrollableBlockIncrement(Rectangle, int, int)}
     * through queue
     */
    public int getScrollableBlockIncrement(final Rectangle rectangle, final int i, final int i1) {
        return (runMapping(new MapIntegerAction("getScrollableBlockIncrement") {
            @Override
            public int map() {
                return ((JTable) getSource()).getScrollableBlockIncrement(rectangle, i, i1);
            }
        }));
    }

    /**
     * Maps {@code JTable.getScrollableTracksViewportHeight()} through queue
     */
    public boolean getScrollableTracksViewportHeight() {
        return (runMapping(new MapBooleanAction("getScrollableTracksViewportHeight") {
            @Override
            public boolean map() {
                return ((JTable) getSource()).getScrollableTracksViewportHeight();
            }
        }));
    }

    /**
     * Maps {@code JTable.getScrollableTracksViewportWidth()} through queue
     */
    public boolean getScrollableTracksViewportWidth() {
        return (runMapping(new MapBooleanAction("getScrollableTracksViewportWidth") {
            @Override
            public boolean map() {
                return ((JTable) getSource()).getScrollableTracksViewportWidth();
            }
        }));
    }

    /**
     * Maps {@code JTable.getScrollableUnitIncrement(Rectangle, int, int)}
     * through queue
     */
    public int getScrollableUnitIncrement(final Rectangle rectangle, final int i, final int i1) {
        return (runMapping(new MapIntegerAction("getScrollableUnitIncrement") {
            @Override
            public int map() {
                return ((JTable) getSource()).getScrollableUnitIncrement(rectangle, i, i1);
            }
        }));
    }

    /**
     * Maps {@code JTable.getSelectedColumn()} through queue
     */
    public int getSelectedColumn() {
        return (runMapping(new MapIntegerAction("getSelectedColumn") {
            @Override
            public int map() {
                return ((JTable) getSource()).getSelectedColumn();
            }
        }));
    }

    /**
     * Maps {@code JTable.getSelectedColumnCount()} through queue
     */
    public int getSelectedColumnCount() {
        return (runMapping(new MapIntegerAction("getSelectedColumnCount") {
            @Override
            public int map() {
                return ((JTable) getSource()).getSelectedColumnCount();
            }
        }));
    }

    /**
     * Maps {@code JTable.getSelectedColumns()} through queue
     */
    public int[] getSelectedColumns() {
        return ((int[]) runMapping(new MapAction<Object>("getSelectedColumns") {
            @Override
            public Object map() {
                return ((JTable) getSource()).getSelectedColumns();
            }
        }));
    }

    /**
     * Maps {@code JTable.getSelectedRow()} through queue
     */
    public int getSelectedRow() {
        return (runMapping(new MapIntegerAction("getSelectedRow") {
            @Override
            public int map() {
                return ((JTable) getSource()).getSelectedRow();
            }
        }));
    }

    /**
     * Maps {@code JTable.getSelectedRowCount()} through queue
     */
    public int getSelectedRowCount() {
        return (runMapping(new MapIntegerAction("getSelectedRowCount") {
            @Override
            public int map() {
                return ((JTable) getSource()).getSelectedRowCount();
            }
        }));
    }

    /**
     * Maps {@code JTable.getSelectedRows()} through queue
     */
    public int[] getSelectedRows() {
        return ((int[]) runMapping(new MapAction<Object>("getSelectedRows") {
            @Override
            public Object map() {
                return ((JTable) getSource()).getSelectedRows();
            }
        }));
    }

    /**
     * Maps {@code JTable.getSelectionBackground()} through queue
     */
    public Color getSelectionBackground() {
        return (runMapping(new MapAction<Color>("getSelectionBackground") {
            @Override
            public Color map() {
                return ((JTable) getSource()).getSelectionBackground();
            }
        }));
    }

    /**
     * Maps {@code JTable.getSelectionForeground()} through queue
     */
    public Color getSelectionForeground() {
        return (runMapping(new MapAction<Color>("getSelectionForeground") {
            @Override
            public Color map() {
                return ((JTable) getSource()).getSelectionForeground();
            }
        }));
    }

    /**
     * Maps {@code JTable.getSelectionModel()} through queue
     */
    public ListSelectionModel getSelectionModel() {
        return (runMapping(new MapAction<ListSelectionModel>("getSelectionModel") {
            @Override
            public ListSelectionModel map() {
                return ((JTable) getSource()).getSelectionModel();
            }
        }));
    }

    /**
     * Maps {@code JTable.getShowHorizontalLines()} through queue
     */
    public boolean getShowHorizontalLines() {
        return (runMapping(new MapBooleanAction("getShowHorizontalLines") {
            @Override
            public boolean map() {
                return ((JTable) getSource()).getShowHorizontalLines();
            }
        }));
    }

    /**
     * Maps {@code JTable.getShowVerticalLines()} through queue
     */
    public boolean getShowVerticalLines() {
        return (runMapping(new MapBooleanAction("getShowVerticalLines") {
            @Override
            public boolean map() {
                return ((JTable) getSource()).getShowVerticalLines();
            }
        }));
    }

    /**
     * Maps {@code JTable.getTableHeader()} through queue
     */
    public JTableHeader getTableHeader() {
        return (runMapping(new MapAction<JTableHeader>("getTableHeader") {
            @Override
            public JTableHeader map() {
                return ((JTable) getSource()).getTableHeader();
            }
        }));
    }

    /**
     * Maps {@code JTable.getUI()} through queue
     */
    public TableUI getUI() {
        return (runMapping(new MapAction<TableUI>("getUI") {
            @Override
            public TableUI map() {
                return ((JTable) getSource()).getUI();
            }
        }));
    }

    /**
     * Maps {@code JTable.getValueAt(int, int)} through queue
     */
    public Object getValueAt(final int i, final int i1) {
        return (runMapping(new MapAction<Object>("getValueAt") {
            @Override
            public Object map() {
                return ((JTable) getSource()).getValueAt(i, i1);
            }
        }));
    }

    /**
     * Maps {@code JTable.isCellEditable(int, int)} through queue
     */
    public boolean isCellEditable(final int i, final int i1) {
        return (runMapping(new MapBooleanAction("isCellEditable") {
            @Override
            public boolean map() {
                return ((JTable) getSource()).isCellEditable(i, i1);
            }
        }));
    }

    /**
     * Maps {@code JTable.isCellSelected(int, int)} through queue
     */
    public boolean isCellSelected(final int i, final int i1) {
        return (runMapping(new MapBooleanAction("isCellSelected") {
            @Override
            public boolean map() {
                return ((JTable) getSource()).isCellSelected(i, i1);
            }
        }));
    }

    /**
     * Maps {@code JTable.isColumnSelected(int)} through queue
     */
    public boolean isColumnSelected(final int i) {
        return (runMapping(new MapBooleanAction("isColumnSelected") {
            @Override
            public boolean map() {
                return ((JTable) getSource()).isColumnSelected(i);
            }
        }));
    }

    /**
     * Maps {@code JTable.isEditing()} through queue
     */
    public boolean isEditing() {
        return (runMapping(new MapBooleanAction("isEditing") {
            @Override
            public boolean map() {
                return ((JTable) getSource()).isEditing();
            }
        }));
    }

    /**
     * Maps {@code JTable.isRowSelected(int)} through queue
     */
    public boolean isRowSelected(final int i) {
        return (runMapping(new MapBooleanAction("isRowSelected") {
            @Override
            public boolean map() {
                return ((JTable) getSource()).isRowSelected(i);
            }
        }));
    }

    /**
     * Maps {@code JTable.moveColumn(int, int)} through queue
     */
    public void moveColumn(final int i, final int i1) {
        runMapping(new MapVoidAction("moveColumn") {
            @Override
            public void map() {
                ((JTable) getSource()).moveColumn(i, i1);
            }
        });
    }

    /**
     * Maps {@code JTable.prepareEditor(TableCellEditor, int, int)} through queue
     */
    public Component prepareEditor(final TableCellEditor tableCellEditor, final int i, final int i1) {
        return (runMapping(new MapAction<Component>("prepareEditor") {
            @Override
            public Component map() {
                return ((JTable) getSource()).prepareEditor(tableCellEditor, i, i1);
            }
        }));
    }

    /**
     * Maps {@code JTable.prepareRenderer(TableCellRenderer, int, int)}
     * through queue
     */
    public Component prepareRenderer(final TableCellRenderer tableCellRenderer, final int i, final int i1) {
        return (runMapping(new MapAction<Component>("prepareRenderer") {
            @Override
            public Component map() {
                return ((JTable) getSource()).prepareRenderer(tableCellRenderer, i, i1);
            }
        }));
    }

    /**
     * Maps {@code JTable.removeColumn(TableColumn)} through queue
     */
    public void removeColumn(final TableColumn tableColumn) {
        runMapping(new MapVoidAction("removeColumn") {
            @Override
            public void map() {
                ((JTable) getSource()).removeColumn(tableColumn);
            }
        });
    }

    /**
     * Maps {@code JTable.removeColumnSelectionInterval(int, int)} through queue
     */
    public void removeColumnSelectionInterval(final int i, final int i1) {
        runMapping(new MapVoidAction("removeColumnSelectionInterval") {
            @Override
            public void map() {
                ((JTable) getSource()).removeColumnSelectionInterval(i, i1);
            }
        });
    }

    /**
     * Maps {@code JTable.removeEditor()} through queue
     */
    public void removeEditor() {
        runMapping(new MapVoidAction("removeEditor") {
            @Override
            public void map() {
                ((JTable) getSource()).removeEditor();
            }
        });
    }

    /**
     * Maps {@code JTable.removeRowSelectionInterval(int, int)} through queue
     */
    public void removeRowSelectionInterval(final int i, final int i1) {
        runMapping(new MapVoidAction("removeRowSelectionInterval") {
            @Override
            public void map() {
                ((JTable) getSource()).removeRowSelectionInterval(i, i1);
            }
        });
    }

    /**
     * Maps {@code JTable.rowAtPoint(Point)} through queue
     */
    public int rowAtPoint(final Point point) {
        return (runMapping(new MapIntegerAction("rowAtPoint") {
            @Override
            public int map() {
                return ((JTable) getSource()).rowAtPoint(point);
            }
        }));
    }

    /**
     * Maps {@code JTable.selectAll()} through queue
     */
    public void selectAll() {
        runMapping(new MapVoidAction("selectAll") {
            @Override
            public void map() {
                ((JTable) getSource()).selectAll();
            }
        });
    }

    /**
     * Maps {@code JTable.setAutoCreateColumnsFromModel(boolean)} through queue
     */
    public void setAutoCreateColumnsFromModel(final boolean b) {
        runMapping(new MapVoidAction("setAutoCreateColumnsFromModel") {
            @Override
            public void map() {
                ((JTable) getSource()).setAutoCreateColumnsFromModel(b);
            }
        });
    }

    /**
     * Maps {@code JTable.setAutoResizeMode(int)} through queue
     */
    public void setAutoResizeMode(final int i) {
        runMapping(new MapVoidAction("setAutoResizeMode") {
            @Override
            public void map() {
                ((JTable) getSource()).setAutoResizeMode(i);
            }
        });
    }

    /**
     * Maps {@code JTable.setCellEditor(TableCellEditor)} through queue
     */
    public void setCellEditor(final TableCellEditor tableCellEditor) {
        runMapping(new MapVoidAction("setCellEditor") {
            @Override
            public void map() {
                ((JTable) getSource()).setCellEditor(tableCellEditor);
            }
        });
    }

    /**
     * Maps {@code JTable.setCellSelectionEnabled(boolean)} through queue
     */
    public void setCellSelectionEnabled(final boolean b) {
        runMapping(new MapVoidAction("setCellSelectionEnabled") {
            @Override
            public void map() {
                ((JTable) getSource()).setCellSelectionEnabled(b);
            }
        });
    }

    /**
     * Maps {@code JTable.setColumnModel(TableColumnModel)} through queue
     */
    public void setColumnModel(final TableColumnModel tableColumnModel) {
        runMapping(new MapVoidAction("setColumnModel") {
            @Override
            public void map() {
                ((JTable) getSource()).setColumnModel(tableColumnModel);
            }
        });
    }

    /**
     * Maps {@code JTable.setColumnSelectionAllowed(boolean)} through queue
     */
    public void setColumnSelectionAllowed(final boolean b) {
        runMapping(new MapVoidAction("setColumnSelectionAllowed") {
            @Override
            public void map() {
                ((JTable) getSource()).setColumnSelectionAllowed(b);
            }
        });
    }

    /**
     * Maps {@code JTable.setColumnSelectionInterval(int, int)} through queue
     */
    public void setColumnSelectionInterval(final int i, final int i1) {
        runMapping(new MapVoidAction("setColumnSelectionInterval") {
            @Override
            public void map() {
                ((JTable) getSource()).setColumnSelectionInterval(i, i1);
            }
        });
    }

    /**
     * Maps {@code JTable.setDefaultEditor(Class, TableCellEditor)} through queue
     */
    public void setDefaultEditor(final Class<?> clss, final TableCellEditor tableCellEditor) {
        runMapping(new MapVoidAction("setDefaultEditor") {
            @Override
            public void map() {
                ((JTable) getSource()).setDefaultEditor(clss, tableCellEditor);
            }
        });
    }

    /**
     * Maps {@code JTable.setDefaultRenderer(Class, TableCellRenderer)}
     * through queue
     */
    public void setDefaultRenderer(final Class<?> clss, final TableCellRenderer tableCellRenderer) {
        runMapping(new MapVoidAction("setDefaultRenderer") {
            @Override
            public void map() {
                ((JTable) getSource()).setDefaultRenderer(clss, tableCellRenderer);
            }
        });
    }

    /**
     * Maps {@code JTable.setEditingColumn(int)} through queue
     */
    public void setEditingColumn(final int i) {
        runMapping(new MapVoidAction("setEditingColumn") {
            @Override
            public void map() {
                ((JTable) getSource()).setEditingColumn(i);
            }
        });
    }

    /**
     * Maps {@code JTable.setEditingRow(int)} through queue
     */
    public void setEditingRow(final int i) {
        runMapping(new MapVoidAction("setEditingRow") {
            @Override
            public void map() {
                ((JTable) getSource()).setEditingRow(i);
            }
        });
    }

    /**
     * Maps {@code JTable.setGridColor(Color)} through queue
     */
    public void setGridColor(final Color color) {
        runMapping(new MapVoidAction("setGridColor") {
            @Override
            public void map() {
                ((JTable) getSource()).setGridColor(color);
            }
        });
    }

    /**
     * Maps {@code JTable.setIntercellSpacing(Dimension)} through queue
     */
    public void setIntercellSpacing(final Dimension dimension) {
        runMapping(new MapVoidAction("setIntercellSpacing") {
            @Override
            public void map() {
                ((JTable) getSource()).setIntercellSpacing(dimension);
            }
        });
    }

    /**
     * Maps {@code JTable.setModel(TableModel)} through queue
     */
    public void setModel(final TableModel tableModel) {
        runMapping(new MapVoidAction("setModel") {
            @Override
            public void map() {
                ((JTable) getSource()).setModel(tableModel);
            }
        });
    }

    /**
     * Maps {@code JTable.setPreferredScrollableViewportSize(Dimension)}
     * through queue
     */
    public void setPreferredScrollableViewportSize(final Dimension dimension) {
        runMapping(new MapVoidAction("setPreferredScrollableViewportSize") {
            @Override
            public void map() {
                ((JTable) getSource()).setPreferredScrollableViewportSize(dimension);
            }
        });
    }

    /**
     * Maps {@code JTable.setRowHeight(int)} through queue
     */
    public void setRowHeight(final int i) {
        runMapping(new MapVoidAction("setRowHeight") {
            @Override
            public void map() {
                ((JTable) getSource()).setRowHeight(i);
            }
        });
    }

    /**
     * Maps {@code JTable.setRowMargin(int)} through queue
     */
    public void setRowMargin(final int i) {
        runMapping(new MapVoidAction("setRowMargin") {
            @Override
            public void map() {
                ((JTable) getSource()).setRowMargin(i);
            }
        });
    }

    /**
     * Maps {@code JTable.setRowSelectionAllowed(boolean)} through queue
     */
    public void setRowSelectionAllowed(final boolean b) {
        runMapping(new MapVoidAction("setRowSelectionAllowed") {
            @Override
            public void map() {
                ((JTable) getSource()).setRowSelectionAllowed(b);
            }
        });
    }

    /**
     * Maps {@code JTable.setRowSelectionInterval(int, int)} through queue
     */
    public void setRowSelectionInterval(final int i, final int i1) {
        runMapping(new MapVoidAction("setRowSelectionInterval") {
            @Override
            public void map() {
                ((JTable) getSource()).setRowSelectionInterval(i, i1);
            }
        });
    }

    /**
     * Maps {@code JTable.setSelectionBackground(Color)} through queue
     */
    public void setSelectionBackground(final Color color) {
        runMapping(new MapVoidAction("setSelectionBackground") {
            @Override
            public void map() {
                ((JTable) getSource()).setSelectionBackground(color);
            }
        });
    }

    /**
     * Maps {@code JTable.setSelectionForeground(Color)} through queue
     */
    public void setSelectionForeground(final Color color) {
        runMapping(new MapVoidAction("setSelectionForeground") {
            @Override
            public void map() {
                ((JTable) getSource()).setSelectionForeground(color);
            }
        });
    }

    /**
     * Maps {@code JTable.setSelectionMode(int)} through queue
     */
    public void setSelectionMode(final int i) {
        runMapping(new MapVoidAction("setSelectionMode") {
            @Override
            public void map() {
                ((JTable) getSource()).setSelectionMode(i);
            }
        });
    }

    /**
     * Maps {@code JTable.setSelectionModel(ListSelectionModel)} through queue
     */
    public void setSelectionModel(final ListSelectionModel listSelectionModel) {
        runMapping(new MapVoidAction("setSelectionModel") {
            @Override
            public void map() {
                ((JTable) getSource()).setSelectionModel(listSelectionModel);
            }
        });
    }

    /**
     * Maps {@code JTable.setShowGrid(boolean)} through queue
     */
    public void setShowGrid(final boolean b) {
        runMapping(new MapVoidAction("setShowGrid") {
            @Override
            public void map() {
                ((JTable) getSource()).setShowGrid(b);
            }
        });
    }

    /**
     * Maps {@code JTable.setShowHorizontalLines(boolean)} through queue
     */
    public void setShowHorizontalLines(final boolean b) {
        runMapping(new MapVoidAction("setShowHorizontalLines") {
            @Override
            public void map() {
                ((JTable) getSource()).setShowHorizontalLines(b);
            }
        });
    }

    /**
     * Maps {@code JTable.setShowVerticalLines(boolean)} through queue
     */
    public void setShowVerticalLines(final boolean b) {
        runMapping(new MapVoidAction("setShowVerticalLines") {
            @Override
            public void map() {
                ((JTable) getSource()).setShowVerticalLines(b);
            }
        });
    }

    /**
     * Maps {@code JTable.setTableHeader(JTableHeader)} through queue
     */
    public void setTableHeader(final JTableHeader jTableHeader) {
        runMapping(new MapVoidAction("setTableHeader") {
            @Override
            public void map() {
                ((JTable) getSource()).setTableHeader(jTableHeader);
            }
        });
    }

    /**
     * Maps {@code JTable.setUI(TableUI)} through queue
     */
    public void setUI(final TableUI tableUI) {
        runMapping(new MapVoidAction("setUI") {
            @Override
            public void map() {
                ((JTable) getSource()).setUI(tableUI);
            }
        });
    }

    /**
     * Maps {@code JTable.setValueAt(Object, int, int)} through queue
     */
    public void setValueAt(final Object object, final int i, final int i1) {
        runMapping(new MapVoidAction("setValueAt") {
            @Override
            public void map() {
                ((JTable) getSource()).setValueAt(object, i, i1);
            }
        });
    }

    /**
     * Maps {@code JTable.tableChanged(TableModelEvent)} through queue
     */
    public void tableChanged(final TableModelEvent tableModelEvent) {
        runMapping(new MapVoidAction("tableChanged") {
            @Override
            public void map() {
                ((JTable) getSource()).tableChanged(tableModelEvent);
            }
        });
    }

    /**
     * Maps {@code JTable.valueChanged(ListSelectionEvent)} through queue
     */
    public void valueChanged(final ListSelectionEvent listSelectionEvent) {
        runMapping(new MapVoidAction("valueChanged") {
            @Override
            public void map() {
                ((JTable) getSource()).valueChanged(listSelectionEvent);
            }
        });
    }

    //End of mapping                                      //
    ////////////////////////////////////////////////////////
    private Point findCell(String text, boolean ce, boolean ccs, int index) {
        return findCell(text, new DefaultStringComparator(ce, ccs), index);
    }

    /**
     * Iterface to choose table cell.
     */
    public interface TableCellChooser {

        /**
         * Should be true if item is good.
         *
         * @param oper Operator used to search item.
         * @param row Row be checked.
         * @param column Column be checked.
         * @return true if cell fits the criteria
         */
        public boolean checkCell(JTableOperator oper, int row, int column);

        /**
         * Item description.
         *
         * @return the description.
         */
        public String getDescription();
    }

    private static class BySubStringTableCellChooser implements TableCellChooser {

        String subString;
        StringComparator comparator;

        public BySubStringTableCellChooser(String subString, StringComparator comparator) {
            this.subString = subString;
            this.comparator = comparator;
        }

        @Override
        public boolean checkCell(JTableOperator oper, int row, int column) {
            Object value = ((JTable) oper.getSource()).getModel().getValueAt(row, column);
            return (comparator.equals((value != null)
                    ? value.toString()
                    : null,
                    subString));
        }

        @Override
        public String getDescription() {
            return "Cell containing \"" + subString + "\" string";
        }

        @Override
        public String toString() {
            return "BySubStringTableCellChooser{" + "subString=" + subString + ", comparator=" + comparator + '}';
        }
    }

    private static class ByRenderedComponentTableCellChooser implements TableCellChooser {

        ComponentChooser chooser;

        public ByRenderedComponentTableCellChooser(ComponentChooser chooser) {
            this.chooser = chooser;
        }

        @Override
        public boolean checkCell(JTableOperator oper, int row, int column) {
            return chooser.checkComponent(oper.getRenderedComponent(row, column));
        }

        @Override
        public String getDescription() {
            return chooser.getDescription();
        }

        @Override
        public String toString() {
            return "ByRenderedComponentTableCellChooser{" + "chooser=" + chooser + '}';
        }
    }

    /**
     * Allows to find component by cell text.
     */
    public static class JTableByCellFinder implements ComponentChooser {

        String label;
        int row;
        int column;
        StringComparator comparator;

        /**
         * Constructs JTableByCellFinder.
         *
         * @param lb a text pattern
         * @param r a row index to look in. If equal to -1, selected row is
         * checked.
         * @param c a column index to look in. If equal to -1, selected column
         * is checked.
         * @param comparator specifies string comparision algorithm.
         */
        public JTableByCellFinder(String lb, int r, int c, StringComparator comparator) {
            label = lb;
            row = r;
            column = c;
            this.comparator = comparator;
        }

        /**
         * Constructs JTableByCellFinder.
         *
         * @param lb a text pattern
         * @param r a row index to look in. If equal to -1, selected row is
         * checked.
         * @param c a column index to look in. If equal to -1, selected column
         * is checked.
         */
        public JTableByCellFinder(String lb, int r, int c) {
            this(lb, r, c, Operator.getDefaultStringComparator());
        }

        @Override
        public boolean checkComponent(Component comp) {
            if (comp instanceof JTable) {
                if (label == null) {
                    return true;
                }
                if (((JTable) comp).getRowCount() > row && ((JTable) comp).getColumnCount() > column) {
                    int r = row;
                    if (r == -1) {
                        int[] rows = ((JTable) comp).getSelectedRows();
                        if (rows.length != 0) {
                            r = rows[0];
                        } else {
                            return false;
                        }
                    }
                    int c = column;
                    if (c == -1) {
                        int[] columns = ((JTable) comp).getSelectedColumns();
                        if (columns.length != 0) {
                            c = columns[0];
                        } else {
                            return false;
                        }
                    }
                    Object value = ((JTable) comp).getValueAt(r, c);
                    if (value == null) {
                        return false;
                    }
                    return (comparator.equals(value.toString(),
                            label));
                }
            }
            return false;
        }

        @Override
        public String getDescription() {
            return ("JTable with text \"" + label + "\" in ("
                    + row + ", "
                    + column + ") cell");
        }

        @Override
        public String toString() {
            return "JTableByCellFinder{" + "label=" + label + ", row=" + row + ", column=" + column + ", comparator=" + comparator + '}';
        }
    }

    /**
     * Checks component type.
     */
    public static class JTableFinder extends Finder {

        /**
         * Constructs JTableFinder.
         *
         * @param sf other searching criteria.
         */
        public JTableFinder(ComponentChooser sf) {
            super(JTable.class, sf);
        }

        /**
         * Constructs JTableFinder.
         */
        public JTableFinder() {
            super(JTable.class);
        }
    }

    private class CellComponentWaiter extends Waiter<Component, Void> {

        private ComponentChooser chooser;
        private int row, column;

        public CellComponentWaiter(ComponentChooser chooser, int row, int column) {
            this.chooser = chooser;
            this.row = row;
            this.column = column;
        }

        @Override
        public Component actionProduced(Void obj) {
            Point pnt = getPointToClick(row, column);
            Component comp = getComponentAt(pnt.x, pnt.y);
            if (comp != null
                    && chooser.checkComponent(comp)) {
                return comp;
            } else {
                return null;
            }
        }

        @Override
        public String getDescription() {
            return chooser.getDescription();
        }

        @Override
        public String toString() {
            return "CellComponentWaiter{" + "chooser=" + chooser + ", row=" + row + ", column=" + column + '}';
        }
    }
}
