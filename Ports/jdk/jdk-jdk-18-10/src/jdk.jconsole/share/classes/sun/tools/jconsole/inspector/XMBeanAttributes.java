/*
 * Copyright (c) 2004, 2012, Oracle and/or its affiliates. All rights reserved.
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


import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

import java.lang.reflect.Array;

import java.util.EventObject;
import java.util.HashMap;
import java.util.WeakHashMap;

import java.util.concurrent.ExecutionException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import javax.management.JMException;
import javax.management.MBeanInfo;
import javax.management.MBeanAttributeInfo;
import javax.management.AttributeList;
import javax.management.Attribute;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.event.ChangeEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import sun.tools.jconsole.MBeansTab;
import sun.tools.jconsole.JConsole;
import sun.tools.jconsole.Messages;
import sun.tools.jconsole.ProxyClient.SnapshotMBeanServerConnection;

/*IMPORTANT :
  There is a deadlock issue there if we don't synchronize well loadAttributes,
  refresh attributes and empty table methods since a UI thread can call
  loadAttributes and at the same time a JMX notification can raise an
  emptyTable. Since there are synchronization in the JMX world it's
  COMPULSORY to not call the JMX world in synchronized blocks */
@SuppressWarnings("serial")
public class XMBeanAttributes extends XTable {

    final Logger LOGGER =
            System.getLogger(XMBeanAttributes.class.getPackage().getName());

    private static final String[] columnNames =
    {Messages.NAME,
     Messages.VALUE};

    private XMBean mbean;
    private MBeanInfo mbeanInfo;
    private MBeanAttributeInfo[] attributesInfo;
    private HashMap<String, Object> attributes;
    private HashMap<String, Object> unavailableAttributes;
    private HashMap<String, Object> viewableAttributes;
    private WeakHashMap<XMBean, HashMap<String, ZoomedCell>> viewersCache =
            new WeakHashMap<XMBean, HashMap<String, ZoomedCell>>();
    private final TableModelListener attributesListener;
    private MBeansTab mbeansTab;
    private TableCellEditor valueCellEditor = new ValueCellEditor();
    private int rowMinHeight = -1;
    private AttributesMouseListener mouseListener = new AttributesMouseListener();

    private static TableCellEditor editor =
            new Utils.ReadOnlyTableCellEditor(new JTextField());

    public XMBeanAttributes(MBeansTab mbeansTab) {
        super();
        this.mbeansTab = mbeansTab;
        ((DefaultTableModel)getModel()).setColumnIdentifiers(columnNames);
        attributesListener = new AttributesListener(this);
        getModel().addTableModelListener(attributesListener);
        getColumnModel().getColumn(NAME_COLUMN).setPreferredWidth(40);

        addMouseListener(mouseListener);
        getTableHeader().setReorderingAllowed(false);
        setColumnEditors();
        addKeyListener(new Utils.CopyKeyAdapter());
    }

    @Override
    public synchronized Component prepareRenderer(TableCellRenderer renderer,
                                                  int row, int column) {
        //In case we have a repaint thread that is in the process of
        //repainting an obsolete table, just ignore the call.
        //It can happen when MBean selection is switched at a very quick rate
        if(row >= getRowCount())
            return null;
        else
            return super.prepareRenderer(renderer, row, column);
    }

    void updateRowHeight(Object obj, int row) {
        ZoomedCell cell = null;
        if(obj instanceof ZoomedCell) {
            cell = (ZoomedCell) obj;
            if(cell.isInited())
                setRowHeight(row, cell.getHeight());
            else
                if(rowMinHeight != - 1)
                    setRowHeight(row, rowMinHeight);
        } else
            if(rowMinHeight != - 1)
                setRowHeight(row, rowMinHeight);
    }

    @Override
    public synchronized TableCellRenderer getCellRenderer(int row,
            int column) {
        //In case we have a repaint thread that is in the process of
        //repainting an obsolete table, just ignore the call.
        //It can happen when MBean selection is switched at a very quick rate
        if (row >= getRowCount()) {
            return null;
        } else {
            if (column == VALUE_COLUMN) {
                Object obj = getModel().getValueAt(row, column);
                if (obj instanceof ZoomedCell) {
                    ZoomedCell cell = (ZoomedCell) obj;
                    if (cell.isInited()) {
                        DefaultTableCellRenderer renderer =
                                (DefaultTableCellRenderer) cell.getRenderer();
                        renderer.setToolTipText(getToolTip(row,column));
                        return renderer;
                    }
                }
            }
            DefaultTableCellRenderer renderer = (DefaultTableCellRenderer)
                super.getCellRenderer(row, column);
            if (!isCellError(row, column)) {
                if (!(isColumnEditable(column) && isWritable(row) &&
                      Utils.isEditableType(getClassName(row)))) {
                    renderer.setForeground(getDefaultColor());
                }
            }
            return renderer;
        }
    }

    private void setColumnEditors() {
        TableColumnModel tcm = getColumnModel();
        for (int i = 0; i < columnNames.length; i++) {
            TableColumn tc = tcm.getColumn(i);
            if (isColumnEditable(i)) {
                tc.setCellEditor(valueCellEditor);
            } else {
                tc.setCellEditor(editor);
            }
        }
    }

    public void cancelCellEditing() {
        if (LOGGER.isLoggable(Level.TRACE)) {
            LOGGER.log(Level.TRACE, "Cancel Editing Row: "+getEditingRow());
        }
        final TableCellEditor tableCellEditor = getCellEditor();
        if (tableCellEditor != null) {
            tableCellEditor.cancelCellEditing();
        }
    }

    public void stopCellEditing() {
        if (LOGGER.isLoggable(Level.TRACE)) {
            LOGGER.log(Level.TRACE, "Stop Editing Row: "+getEditingRow());
        }
        final TableCellEditor tableCellEditor = getCellEditor();
        if (tableCellEditor != null) {
            tableCellEditor.stopCellEditing();
        }
    }

    @Override
    public final boolean editCellAt(final int row, final int column, EventObject e) {
        if (LOGGER.isLoggable(Level.TRACE)) {
            LOGGER.log(Level.TRACE, "editCellAt(row="+row+", col="+column+
                    ", e="+e+")");
        }
        if (JConsole.isDebug()) {
            System.err.println("edit: "+getValueName(row)+"="+getValue(row));
        }
        boolean retVal = super.editCellAt(row, column, e);
        if (retVal) {
            final TableCellEditor tableCellEditor =
                    getColumnModel().getColumn(column).getCellEditor();
            if (tableCellEditor == valueCellEditor) {
                ((JComponent) tableCellEditor).requestFocus();
            }
        }
        return retVal;
    }

    @Override
    public boolean isCellEditable(int row, int col) {
        // All the cells in non-editable columns are editable
        if (!isColumnEditable(col)) {
            return true;
        }
        // Maximized zoomed cells are editable
        Object obj = getModel().getValueAt(row, col);
        if (obj instanceof ZoomedCell) {
            ZoomedCell cell = (ZoomedCell) obj;
            return cell.isMaximized();
        }
        return true;
    }

    @Override
    public void setValueAt(Object value, int row, int column) {
        if (!isCellError(row, column) && isColumnEditable(column) &&
            isWritable(row) && Utils.isEditableType(getClassName(row))) {
            if (JConsole.isDebug()) {
                System.err.println("validating [row="+row+", column="+column+
                        "]: "+getValueName(row)+"="+value);
            }
            super.setValueAt(value, row, column);
        }
    }

    //Table methods

    public boolean isTableEditable() {
        return true;
    }

    public void setTableValue(Object value, int row) {
    }

    public boolean isColumnEditable(int column) {
        if (column < getColumnCount()) {
            return getColumnName(column).equals(Messages.VALUE);
        }
        else {
            return false;
        }
    }

    public String getClassName(int row) {
        int index = convertRowToIndex(row);
        if (index != -1) {
            return attributesInfo[index].getType();
        }
        else {
            return null;
        }
    }


    public String getValueName(int row) {
        int index = convertRowToIndex(row);
        if (index != -1) {
            return attributesInfo[index].getName();
        }
        else {
            return null;
        }
    }

    public Object getValue(int row) {
        final Object val = ((DefaultTableModel) getModel())
                .getValueAt(row, VALUE_COLUMN);
        return val;
    }

    //tool tip only for editable column
    @Override
    public String getToolTip(int row, int column) {
        if (isCellError(row, column)) {
            return (String) unavailableAttributes.get(getValueName(row));
        }
        if (isColumnEditable(column)) {
            Object value = getValue(row);
            String tip = null;
            if (value != null) {
                tip = value.toString();
                if(isAttributeViewable(row, VALUE_COLUMN))
                    tip = Messages.DOUBLE_CLICK_TO_EXPAND_FORWARD_SLASH_COLLAPSE+
                        ". " + tip;
            }

            return tip;
        }

        if(column == NAME_COLUMN) {
            int index = convertRowToIndex(row);
            if (index != -1) {
                return attributesInfo[index].getDescription();
            }
        }
        return null;
    }

    public synchronized boolean isWritable(int row) {
        int index = convertRowToIndex(row);
        if (index != -1) {
            return (attributesInfo[index].isWritable());
        }
        else {
            return false;
        }
    }

    /**
     * Override JTable method in order to make any call to this method
     * atomic with TableModel elements.
     */
    @Override
    public synchronized int getRowCount() {
        return super.getRowCount();
    }

    public synchronized boolean isReadable(int row) {
        int index = convertRowToIndex(row);
        if (index != -1) {
            return (attributesInfo[index].isReadable());
        }
        else {
            return false;
        }
    }

    public synchronized boolean isCellError(int row, int col) {
        return (isColumnEditable(col) &&
                (unavailableAttributes.containsKey(getValueName(row))));
    }

    public synchronized boolean isAttributeViewable(int row, int col) {
        boolean isViewable = false;
        if(col == VALUE_COLUMN) {
            Object obj = getModel().getValueAt(row, col);
            if(obj instanceof ZoomedCell)
                isViewable = true;
        }

        return isViewable;
    }

    // Call this in EDT
    public void loadAttributes(final XMBean mbean, final MBeanInfo mbeanInfo) {

        final SwingWorker<Runnable,Void> load =
                new SwingWorker<Runnable,Void>() {
            @Override
            protected Runnable doInBackground() throws Exception {
                return doLoadAttributes(mbean,mbeanInfo);
            }

            @Override
            protected void done() {
                try {
                    final Runnable updateUI = get();
                    if (updateUI != null) updateUI.run();
                } catch (RuntimeException x) {
                    throw x;
                } catch (ExecutionException x) {
                    if(JConsole.isDebug()) {
                       System.err.println(
                               "Exception raised while loading attributes: "
                               +x.getCause());
                       x.printStackTrace();
                    }
                } catch (InterruptedException x) {
                    if(JConsole.isDebug()) {
                       System.err.println(
                            "Interrupted while loading attributes: "+x);
                       x.printStackTrace();
                    }
                }
            }

        };
        mbeansTab.workerAdd(load);
    }

    // Don't call this in EDT, but execute returned Runnable inside
    // EDT - typically in the done() method of a SwingWorker
    // This method can return null.
    private Runnable doLoadAttributes(final XMBean mbean, MBeanInfo infoOrNull)
        throws JMException, IOException {
        // To avoid deadlock with events coming from the JMX side,
        // we retrieve all JMX stuff in a non synchronized block.

        if(mbean == null) return null;
        final MBeanInfo curMBeanInfo =
                (infoOrNull==null)?mbean.getMBeanInfo():infoOrNull;

        final MBeanAttributeInfo[] attrsInfo = curMBeanInfo.getAttributes();
        final HashMap<String, Object> attrs =
            new HashMap<String, Object>(attrsInfo.length);
        final HashMap<String, Object> unavailableAttrs =
            new HashMap<String, Object>(attrsInfo.length);
        final HashMap<String, Object> viewableAttrs =
            new HashMap<String, Object>(attrsInfo.length);
        AttributeList list = null;

        try {
            list = mbean.getAttributes(attrsInfo);
        }catch(Exception e) {
            if (JConsole.isDebug()) {
                System.err.println("Error calling getAttributes() on MBean \"" +
                                   mbean.getObjectName() + "\". JConsole will " +
                                   "try to get them individually calling " +
                                   "getAttribute() instead. Exception:");
                e.printStackTrace(System.err);
            }
            list = new AttributeList();
            //Can't load all attributes, do it one after each other.
            for(int i = 0; i < attrsInfo.length; i++) {
                String name = null;
                try {
                    name = attrsInfo[i].getName();
                    Object value =
                        mbean.getMBeanServerConnection().
                        getAttribute(mbean.getObjectName(), name);
                    list.add(new Attribute(name, value));
                }catch(Exception ex) {
                    if(attrsInfo[i].isReadable()) {
                        unavailableAttrs.put(name,
                                Utils.getActualException(ex).toString());
                    }
                }
            }
        }
        try {
            int att_length = list.size();
            for (int i=0;i<att_length;i++) {
                Attribute attribute = (Attribute) list.get(i);
                if(isViewable(attribute)) {
                    viewableAttrs.put(attribute.getName(),
                                           attribute.getValue());
                }
                else
                    attrs.put(attribute.getName(),attribute.getValue());

            }
            // if not all attributes are accessible,
            // check them one after the other.
            if (att_length < attrsInfo.length) {
                for (int i=0;i<attrsInfo.length;i++) {
                    MBeanAttributeInfo attributeInfo = attrsInfo[i];
                    if (!attrs.containsKey(attributeInfo.getName()) &&
                        !viewableAttrs.containsKey(attributeInfo.
                                                        getName()) &&
                        !unavailableAttrs.containsKey(attributeInfo.
                                                           getName())) {
                        if (attributeInfo.isReadable()) {
                            // getAttributes didn't help resolving the
                            // exception.
                            // We must call it again to understand what
                            // went wrong.
                            try {
                                Object v =
                                    mbean.getMBeanServerConnection().getAttribute(
                                    mbean.getObjectName(), attributeInfo.getName());
                                //What happens if now it is ok?
                                // Be pragmatic, add it to readable...
                                attrs.put(attributeInfo.getName(),
                                               v);
                            }catch(Exception e) {
                                //Put the exception that will be displayed
                                // in tooltip
                                unavailableAttrs.put(attributeInfo.getName(),
                                        Utils.getActualException(e).toString());
                            }
                        }
                    }
                }
            }
        }
        catch(Exception e) {
            //sets all attributes unavailable except the writable ones
            for (int i=0;i<attrsInfo.length;i++) {
                MBeanAttributeInfo attributeInfo = attrsInfo[i];
                if (attributeInfo.isReadable()) {
                    unavailableAttrs.put(attributeInfo.getName(),
                                              Utils.getActualException(e).
                                              toString());
                }
            }
        }
        //end of retrieval

        //one update at a time
        return new Runnable() {
            public void run() {
                synchronized (XMBeanAttributes.this) {
                    XMBeanAttributes.this.mbean = mbean;
                    XMBeanAttributes.this.mbeanInfo = curMBeanInfo;
                    XMBeanAttributes.this.attributesInfo = attrsInfo;
                    XMBeanAttributes.this.attributes = attrs;
                    XMBeanAttributes.this.unavailableAttributes = unavailableAttrs;
                    XMBeanAttributes.this.viewableAttributes = viewableAttrs;

                    DefaultTableModel tableModel =
                            (DefaultTableModel) getModel();

                    // add attribute information
                    emptyTable(tableModel);

                    addTableData(tableModel,
                            mbean,
                            attrsInfo,
                            attrs,
                            unavailableAttrs,
                            viewableAttrs);

                    // update the model with the new data
                    tableModel.newDataAvailable(new TableModelEvent(tableModel));
                    // re-register for change events
                    tableModel.addTableModelListener(attributesListener);
                }
            }
        };
    }

    void collapse(String attributeName, final Component c) {
        final int row = getSelectedRow();
        Object obj = getModel().getValueAt(row, VALUE_COLUMN);
        if(obj instanceof ZoomedCell) {
            cancelCellEditing();
            ZoomedCell cell = (ZoomedCell) obj;
            cell.reset();
            setRowHeight(row,
                         cell.getHeight());
            editCellAt(row,
                       VALUE_COLUMN);
            invalidate();
            repaint();
        }
    }

    ZoomedCell updateZoomedCell(int row,
                                int col) {
        Object obj = getModel().getValueAt(row, VALUE_COLUMN);
        ZoomedCell cell = null;
        if(obj instanceof ZoomedCell) {
            cell = (ZoomedCell) obj;
            if(!cell.isInited()) {
                Object elem = cell.getValue();
                String attributeName =
                    (String) getModel().getValueAt(row,
                                                   NAME_COLUMN);
                Component comp = mbeansTab.getDataViewer().
                        createAttributeViewer(elem, mbean, attributeName, this);
                if(comp != null){
                    if(rowMinHeight == -1)
                        rowMinHeight = getRowHeight(row);

                    cell.init(super.getCellRenderer(row, col),
                              comp,
                              rowMinHeight);

                    XDataViewer.registerForMouseEvent(
                            comp, mouseListener);
                } else
                    return cell;
            }

            cell.switchState();
            setRowHeight(row,
                         cell.getHeight());

            if(!cell.isMaximized()) {
                cancelCellEditing();
                //Back to simple editor.
                editCellAt(row,
                           VALUE_COLUMN);
            }

            invalidate();
            repaint();
        }
        return cell;
    }

    // This is called by XSheet when the "refresh" button is pressed.
    // In this case we will commit any pending attribute values by
    // calling 'stopCellEditing'.
    //
    public void refreshAttributes() {
         refreshAttributes(true);
    }

    // refreshAttributes(false) is called by tableChanged().
    // in this case we must not call stopCellEditing, because it's already
    // been called - e.g.
    // lostFocus/mousePressed -> stopCellEditing -> setValueAt -> tableChanged
    //                        -> refreshAttributes(false)
    //
    // Can be called in EDT - as long as the implementation of
    // mbeansTab.getCachedMBeanServerConnection() and mbsc.flush() doesn't
    // change
    //
    private void refreshAttributes(final boolean stopCellEditing) {
         SwingWorker<Void,Void> sw = new SwingWorker<Void,Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                SnapshotMBeanServerConnection mbsc =
                mbeansTab.getSnapshotMBeanServerConnection();
                mbsc.flush();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    if (stopCellEditing) stopCellEditing();
                    loadAttributes(mbean, mbeanInfo);
                } catch (Exception x) {
                    if (JConsole.isDebug()) {
                        x.printStackTrace();
                    }
                }
            }
         };
         mbeansTab.workerAdd(sw);
     }
    // We need to call stop editing here - otherwise edits are lost
    // when resizing the table.
    //
    @Override
    public void columnMarginChanged(ChangeEvent e) {
        if (isEditing()) stopCellEditing();
        super.columnMarginChanged(e);
    }

    // We need to call stop editing here - otherwise the edited value
    // is transferred to the wrong row...
    //
    @Override
    void sortRequested(int column) {
        if (isEditing()) stopCellEditing();
        super.sortRequested(column);
    }


    @Override
    public synchronized void emptyTable() {
         emptyTable((DefaultTableModel)getModel());
     }

    // Call this in synchronized block.
    private void emptyTable(DefaultTableModel model) {
         model.removeTableModelListener(attributesListener);
         super.emptyTable();
    }

    private boolean isViewable(Attribute attribute) {
        Object data = attribute.getValue();
        return XDataViewer.isViewableValue(data);

    }

    synchronized void removeAttributes() {
        if (attributes != null) {
            attributes.clear();
        }
        if (unavailableAttributes != null) {
            unavailableAttributes.clear();
        }
        if (viewableAttributes != null) {
            viewableAttributes.clear();
        }
        mbean = null;
    }

    private ZoomedCell getZoomedCell(XMBean mbean, String attribute, Object value) {
        synchronized (viewersCache) {
            HashMap<String, ZoomedCell> viewers;
            if (viewersCache.containsKey(mbean)) {
                viewers = viewersCache.get(mbean);
            } else {
                viewers = new HashMap<String, ZoomedCell>();
            }
            ZoomedCell cell;
            if (viewers.containsKey(attribute)) {
                cell = viewers.get(attribute);
                cell.setValue(value);
                if (cell.isMaximized() && cell.getType() != XDataViewer.NUMERIC) {
                    // Plotters are the only viewers with auto update capabilities.
                    // Other viewers need to be updated manually.
                    Component comp =
                        mbeansTab.getDataViewer().createAttributeViewer(
                            value, mbean, attribute, XMBeanAttributes.this);
                    cell.init(cell.getMinRenderer(), comp, cell.getMinHeight());
                    XDataViewer.registerForMouseEvent(comp, mouseListener);
                }
            } else {
                cell = new ZoomedCell(value);
                viewers.put(attribute, cell);
            }
            viewersCache.put(mbean, viewers);
            return cell;
        }
    }

    //will be called in a synchronized block
    protected void addTableData(DefaultTableModel tableModel,
                                XMBean mbean,
                                MBeanAttributeInfo[] attributesInfo,
                                HashMap<String, Object> attributes,
                                HashMap<String, Object> unavailableAttributes,
                                HashMap<String, Object> viewableAttributes) {

        Object rowData[] = new Object[2];
        int col1Width = 0;
        int col2Width = 0;
        for (int i = 0; i < attributesInfo.length; i++) {
            rowData[0] = (attributesInfo[i].getName());
            if (unavailableAttributes.containsKey(rowData[0])) {
                rowData[1] = Messages.UNAVAILABLE;
            } else if (viewableAttributes.containsKey(rowData[0])) {
                rowData[1] = viewableAttributes.get(rowData[0]);
                if (!attributesInfo[i].isWritable() ||
                    !Utils.isEditableType(attributesInfo[i].getType())) {
                    rowData[1] = getZoomedCell(mbean, (String) rowData[0], rowData[1]);
                }
            } else {
                rowData[1] = attributes.get(rowData[0]);
            }

            tableModel.addRow(rowData);

            //Update column width
            //
            String str = null;
            if(rowData[0] != null) {
                str = rowData[0].toString();
                if(str.length() > col1Width)
                    col1Width = str.length();
            }
            if(rowData[1] != null) {
                str = rowData[1].toString();
                if(str.length() > col2Width)
                    col2Width = str.length();
            }
        }
        updateColumnWidth(col1Width, col2Width);
    }

    private void updateColumnWidth(int col1Width, int col2Width) {
        TableColumnModel colModel = getColumnModel();

        //Get the column at index pColumn, and set its preferred width.
        col1Width = col1Width * 7;
        col2Width = col2Width * 7;
        if(col1Width + col2Width <
           (int) getPreferredScrollableViewportSize().getWidth())
            col2Width = (int) getPreferredScrollableViewportSize().getWidth()
                - col1Width;

        colModel.getColumn(NAME_COLUMN).setPreferredWidth(50);
    }

    class AttributesMouseListener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            if(e.getButton() == MouseEvent.BUTTON1) {
                if(e.getClickCount() >= 2) {

                    int row = XMBeanAttributes.this.getSelectedRow();
                    int col = XMBeanAttributes.this.getSelectedColumn();
                    if(col != VALUE_COLUMN) return;
                    if(col == -1 || row == -1) return;

                    XMBeanAttributes.this.updateZoomedCell(row, col);
                }
            }
        }
    }

    class ValueCellEditor extends XTextFieldEditor {
        // implements javax.swing.table.TableCellEditor
        @Override
        public Component getTableCellEditorComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     int row,
                                                     int column) {
            Object val = value;
            if(column == VALUE_COLUMN) {
                Object obj = getModel().getValueAt(row,
                                                   column);
                if(obj instanceof ZoomedCell) {
                    ZoomedCell cell = (ZoomedCell) obj;
                    if(cell.getRenderer() instanceof MaximizedCellRenderer) {
                        MaximizedCellRenderer zr =
                            (MaximizedCellRenderer) cell.getRenderer();
                        return zr.getComponent();
                    }
                } else {
                    Component comp = super.getTableCellEditorComponent(
                            table, val, isSelected, row, column);
                    if (isCellError(row, column) ||
                        !isWritable(row) ||
                        !Utils.isEditableType(getClassName(row))) {
                        textField.setEditable(false);
                    }
                    return comp;
                }
            }
            return super.getTableCellEditorComponent(table,
                                                     val,
                                                     isSelected,
                                                     row,
                                                     column);
        }
        @Override
        public boolean stopCellEditing() {
            int editingRow = getEditingRow();
            int editingColumn = getEditingColumn();
            if (editingColumn == VALUE_COLUMN) {
                Object obj = getModel().getValueAt(editingRow, editingColumn);
                if (obj instanceof ZoomedCell) {
                    ZoomedCell cell = (ZoomedCell) obj;
                    if (cell.isMaximized()) {
                        this.cancelCellEditing();
                        return true;
                    }
                }
            }
            return super.stopCellEditing();
        }
    }

    class MaximizedCellRenderer extends  DefaultTableCellRenderer {
        Component comp;
        MaximizedCellRenderer(Component comp) {
            this.comp = comp;
            Dimension d = comp.getPreferredSize();
            if (d.getHeight() > 220) {
                comp.setPreferredSize(new Dimension((int) d.getWidth(), 220));
            }
        }
        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            return comp;
        }
        public Component getComponent() {
            return comp;
        }
    }

    class ZoomedCell {
        TableCellRenderer minRenderer;
        MaximizedCellRenderer maxRenderer;
        int minHeight;
        boolean minimized = true;
        boolean init = false;
        int type;
        Object value;
        ZoomedCell(Object value) {
            type = XDataViewer.getViewerType(value);
            this.value = value;
        }

        boolean isInited() {
            return init;
        }

        Object getValue() {
            return value;
        }

        void setValue(Object value) {
            this.value = value;
        }

        void init(TableCellRenderer minRenderer,
                  Component maxComponent,
                  int minHeight) {
            this.minRenderer = minRenderer;
            this.maxRenderer = new MaximizedCellRenderer(maxComponent);

            this.minHeight = minHeight;
            init = true;
        }

        int getType() {
            return type;
        }

        void reset() {
            init = false;
            minimized = true;
        }

        void switchState() {
            minimized = !minimized;
        }
        boolean isMaximized() {
            return !minimized;
        }
        void minimize() {
            minimized = true;
        }

        void maximize() {
            minimized = false;
        }

        int getHeight() {
            if(minimized) return minHeight;
            else
                return (int) maxRenderer.getComponent().
                    getPreferredSize().getHeight() ;
        }

        int getMinHeight() {
            return minHeight;
        }

        @Override
        public String toString() {

            if(value == null) return null;

            if(value.getClass().isArray()) {
                String name =
                    Utils.getArrayClassName(value.getClass().getName());
                int length = Array.getLength(value);
                return name + "[" + length +"]";
            }

            if(value instanceof CompositeData ||
               value instanceof TabularData)
                return value.getClass().getName();

            return value.toString();
        }

        TableCellRenderer getRenderer() {
            if(minimized) return minRenderer;
            else return maxRenderer;
        }

        TableCellRenderer getMinRenderer() {
            return minRenderer;
        }
    }

    class AttributesListener implements  TableModelListener {

        private Component component;

        public AttributesListener(Component component) {
            this.component = component;
        }

        // Call this in EDT
        public void tableChanged(final TableModelEvent e) {
            // only post changes to the draggable column
            if (isColumnEditable(e.getColumn())) {
                final TableModel model = (TableModel)e.getSource();
                Object tableValue = model.getValueAt(e.getFirstRow(),
                                                 e.getColumn());

                if (LOGGER.isLoggable(Level.TRACE)) {
                    LOGGER.log(Level.TRACE,
                        "tableChanged: firstRow="+e.getFirstRow()+
                        ", lastRow="+e.getLastRow()+", column="+e.getColumn()+
                        ", value="+tableValue);
                }
                // if it's a String, try construct new value
                // using the defined type.
                if (tableValue instanceof String) {
                    try {
                        tableValue =
                            Utils.createObjectFromString(getClassName(e.getFirstRow()), // type
                            (String)tableValue);// value
                    } catch (Throwable ex) {
                        popupAndLog(ex,"tableChanged",
                                    Messages.PROBLEM_SETTING_ATTRIBUTE);
                    }
                }
                final String attributeName = getValueName(e.getFirstRow());
                final Attribute attribute =
                      new Attribute(attributeName,tableValue);
                setAttribute(attribute, "tableChanged");
            }
        }

        // Call this in EDT
        private void setAttribute(final Attribute attribute, final String method) {
            final SwingWorker<Void,Void> setAttribute =
                    new SwingWorker<Void,Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    try {
                        if (JConsole.isDebug()) {
                            System.err.println("setAttribute("+
                                    attribute.getName()+
                                "="+attribute.getValue()+")");
                        }
                        mbean.setAttribute(attribute);
                    } catch (Throwable ex) {
                        popupAndLog(ex,method,Messages.PROBLEM_SETTING_ATTRIBUTE);
                    }
                    return null;
                }
                @Override
                protected void done() {
                    try {
                        get();
                    } catch (Exception x) {
                        if (JConsole.isDebug())
                            x.printStackTrace();
                    }
                    refreshAttributes(false);
                }

            };
            mbeansTab.workerAdd(setAttribute);
        }

        // Call this outside EDT
        private void popupAndLog(Throwable ex, String method, String title) {
            ex = Utils.getActualException(ex);
            if (JConsole.isDebug()) ex.printStackTrace();

            String message = (ex.getMessage() != null) ? ex.getMessage()
                    : ex.toString();
            EventQueue.invokeLater(
                    new ThreadDialog(component, message+"\n",
                                     title,
                                     JOptionPane.ERROR_MESSAGE));
        }
    }
}
