/*
 * Copyright (c) 2004, 2014, Oracle and/or its affiliates. All rights reserved.
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

package sun.tools.jconsole.inspector;


// Imports for picking up mouse events from the JTable.

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import sun.tools.jconsole.JConsole;

@SuppressWarnings("serial")
public class TableSorter extends DefaultTableModel implements MouseListener {
    private boolean ascending = true;
    private TableColumnModel columnModel;
    private JTable tableView;
    private Vector<TableModelListener> evtListenerList;
    private int sortColumn = 0;

    private int[] invertedIndex;

    public TableSorter() {
        super();
        evtListenerList = new Vector<TableModelListener>();
    }

    public TableSorter(Object[] columnNames, int numRows) {
        super(columnNames,numRows);
        evtListenerList = new Vector<TableModelListener>();
    }

    @Override
    public void newDataAvailable(TableModelEvent e) {
        super.newDataAvailable(e);
        invertedIndex = new int[getRowCount()];
        for (int i = 0; i < invertedIndex.length; i++) {
            invertedIndex[i] = i;
        }
        sort(this.sortColumn, this.ascending);
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        evtListenerList.add(l);
        super.addTableModelListener(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        evtListenerList.remove(l);
        super.removeTableModelListener(l);
    }

    private void removeListeners() {
        for(TableModelListener tnl : evtListenerList)
            super.removeTableModelListener(tnl);
    }

    private void restoreListeners() {
        for(TableModelListener tnl : evtListenerList)
            super.addTableModelListener(tnl);
    }

    @SuppressWarnings("unchecked")
    private int compare(Object o1, Object o2) {
        // take care of the case where both o1 & o2 are null. Needed to keep
        // the method symmetric. Without this quickSort gives surprising results.
        if (o1 == o2)
            return 0;
        if (o1==null)
            return 1;
        if (o2==null)
            return -1;
        //two object of the same class and that are comparable
        else if ((o1.getClass().equals(o2.getClass())) &&
                 (o1 instanceof Comparable)) {
            return (((Comparable) o1).compareTo(o2));
        }
        else {
            return o1.toString().compareTo(o2.toString());
        }
    }

    private void sort(int column, boolean isAscending) {
        final XMBeanAttributes attrs =
                (tableView instanceof XMBeanAttributes)
                ?(XMBeanAttributes) tableView
                :null;

        // We cannot sort rows when a cell is being
        // edited - so we're going to cancel cell editing here if needed.
        // This might happen when the user is editing a row, and clicks on
        // another row without validating. In that case there are two events
        // that compete: one is the validation of the value that was previously
        // edited, the other is the mouse click that opens the new editor.
        //
        // When we reach here the previous value is already validated, and the
        // old editor is closed, but the new editor might have opened.
        // It's this new editor that wil be cancelled here, if needed.
        //
        if (attrs != null && attrs.isEditing())
            attrs.cancelCellEditing();

        // remove registered listeners
        removeListeners();
        // do the sort

        if (JConsole.isDebug()) {
            System.err.println("sorting table against column="+column
                    +" ascending="+isAscending);
        }
        quickSort(0,getRowCount()-1,column,isAscending);
        // restore registered listeners
        restoreListeners();

        // update row heights in XMBeanAttributes (required by expandable cells)
        if (attrs != null) {
            for (int i = 0; i < getRowCount(); i++) {
                Vector<?> data = dataVector.elementAt(i);
                attrs.updateRowHeight(data.elementAt(1), i);
            }
        }
    }

    private boolean compareS(Object s1, Object s2, boolean isAscending) {
        if (isAscending)
            return (compare(s1,s2) > 0);
        else
            return (compare(s1,s2) < 0);
    }

    private boolean compareG(Object s1, Object s2, boolean isAscending) {
        if (isAscending)
            return (compare(s1,s2) < 0);
        else
            return (compare(s1,s2) > 0);
    }

    private void quickSort(int lo0,int hi0, int key, boolean isAscending) {
        int lo = lo0;
        int hi = hi0;
        Object mid;

        if ( hi0 > lo0)
            {
                mid = getValueAt( ( lo0 + hi0 ) / 2 , key);

                while( lo <= hi )
                    {
                        /* find the first element that is greater than
                         * or equal to the partition element starting
                         * from the left Index.
                         */
                        while( ( lo < hi0 ) &&
                               ( compareS(mid,getValueAt(lo,key), isAscending) ))
                            ++lo;

                        /* find an element that is smaller than or equal to
                         * the partition element starting from the right Index.
                         */
                        while( ( hi > lo0 ) &&
                               ( compareG(mid,getValueAt(hi,key), isAscending) ))
                            --hi;

                        // if the indexes have not crossed, swap
                        if( lo <= hi )
                            {
                                swap(lo, hi, key);
                                ++lo;
                                --hi;
                            }
                    }

                                /* If the right index has not reached the
                                 * left side of array
                                 * must now sort the left partition.
                                 */
                if( lo0 < hi )
                    quickSort(lo0, hi , key, isAscending);

                                /* If the left index has not reached the right
                                 * side of array
                                 * must now sort the right partition.
                                 */
                if( lo <= hi0 )
                    quickSort(lo, hi0 , key, isAscending);
            }
    }

    private Vector<?> getRow(int row) {
        return dataVector.elementAt(row);
    }

    @SuppressWarnings("unchecked")
    private void setRow(Vector<?> data, int row) {
        dataVector.setElementAt(data,row);
    }

    private void swap(int i, int j, int column) {
        Vector<?> data = getRow(i);
        setRow(getRow(j),i);
        setRow(data,j);

        int a = invertedIndex[i];
        invertedIndex[i] = invertedIndex[j];
        invertedIndex[j] = a;
    }

    public void sortByColumn(int column) {
        sortByColumn(column, !ascending);
    }

    public void sortByColumn(int column, boolean ascending) {
        this.ascending = ascending;
        this.sortColumn = column;
        sort(column,ascending);
    }

    public int getIndexOfRow(int row) {
        return invertedIndex[row];
    }

    // Add a mouse listener to the Table to trigger a table sort
    // when a column heading is clicked in the JTable.
    public void addMouseListenerToHeaderInTable(JTable table) {
        tableView = table;
        columnModel = tableView.getColumnModel();
        JTableHeader th = tableView.getTableHeader();
        th.addMouseListener(this);
    }

    public void mouseClicked(MouseEvent e) {
        int viewColumn = columnModel.getColumnIndexAtX(e.getX());
        int column = tableView.convertColumnIndexToModel(viewColumn);
        if (e.getClickCount() == 1 && column != -1) {
            if (tableView instanceof XTable) {
                XTable attrs = (XTable) tableView;
                // inform the table view that the rows are going to be sorted
                // against the values in a given column. This gives the
                // chance to the table view to close its editor - if needed.
                //
                attrs.sortRequested(column);
            }
            tableView.invalidate();
            sortByColumn(column);
            tableView.validate();
            tableView.repaint();
        }
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }
}
