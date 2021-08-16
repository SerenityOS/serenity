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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridLayout;
import java.util.*;
import javax.management.*;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;
import javax.swing.table.*;

import sun.tools.jconsole.Messages;

import static sun.tools.jconsole.Utilities.*;

@SuppressWarnings("serial")
public class XMBeanInfo extends JPanel {

    private static final Color lightYellow = new Color(255, 255, 128);
     private final int NAME_COLUMN = 0;
    private final int VALUE_COLUMN = 1;
    private final String[] columnNames = {
        Messages.NAME,
        Messages.VALUE
    };
    private JTable infoTable = new JTable();
    private JTable descTable = new JTable();
    private JPanel infoBorderPanel = new JPanel(new BorderLayout());
    private JPanel descBorderPanel = new JPanel(new BorderLayout());

    private static class ReadOnlyDefaultTableModel extends DefaultTableModel {

        @Override
        public void setValueAt(Object value, int row, int col) {
        }
    }

    private static class TableRowDivider {

        private String tableRowDividerText;

        public TableRowDivider(String tableRowDividerText) {
            this.tableRowDividerText = tableRowDividerText;
        }

        @Override
        public String toString() {
            return tableRowDividerText;
        }
    }
    private static MBeanInfoTableCellRenderer renderer =
            new MBeanInfoTableCellRenderer();

    private static class MBeanInfoTableCellRenderer
            extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            Component comp = super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);
            if (value instanceof TableRowDivider) {
                JLabel label = new JLabel(value.toString());
                label.setBackground(ensureContrast(lightYellow,
                        label.getForeground()));
                label.setOpaque(true);
                return label;
            }
            return comp;
        }
    }
    private static TableCellEditor editor =
            new MBeanInfoTableCellEditor(new JTextField());

    private static class MBeanInfoTableCellEditor
            extends Utils.ReadOnlyTableCellEditor {

        public MBeanInfoTableCellEditor(JTextField tf) {
            super(tf);
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table, Object value, boolean isSelected,
                int row, int column) {
            Component comp = super.getTableCellEditorComponent(
                    table, value, isSelected, row, column);
            if (value instanceof TableRowDivider) {
                JLabel label = new JLabel(value.toString());
                label.setBackground(ensureContrast(lightYellow,
                        label.getForeground()));
                label.setOpaque(true);
                return label;
            }
            return comp;
        }
    }

    public XMBeanInfo() {
        // Use the grid layout to display the two tables
        //
        super(new GridLayout(2, 1));
        // MBean*Info table
        //
        infoTable.setModel(new ReadOnlyDefaultTableModel());
        infoTable.setRowSelectionAllowed(false);
        infoTable.setColumnSelectionAllowed(false);
        infoTable.getTableHeader().setReorderingAllowed(false);
        ((DefaultTableModel) infoTable.getModel()).setColumnIdentifiers(columnNames);
        infoTable.getColumnModel().getColumn(NAME_COLUMN).setPreferredWidth(140);
        infoTable.getColumnModel().getColumn(NAME_COLUMN).setMaxWidth(140);
        infoTable.getColumnModel().getColumn(NAME_COLUMN).setCellRenderer(renderer);
        infoTable.getColumnModel().getColumn(VALUE_COLUMN).setCellRenderer(renderer);
        infoTable.getColumnModel().getColumn(NAME_COLUMN).setCellEditor(editor);
        infoTable.getColumnModel().getColumn(VALUE_COLUMN).setCellEditor(editor);
        infoTable.addKeyListener(new Utils.CopyKeyAdapter());
        infoTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        JScrollPane infoTableScrollPane = new JScrollPane(infoTable);
        infoBorderPanel.setBorder(
                BorderFactory.createTitledBorder("MBeanInfoPlaceHolder"));
        infoBorderPanel.add(infoTableScrollPane);
        // Descriptor table
        //
        descTable.setModel(new ReadOnlyDefaultTableModel());
        descTable.setRowSelectionAllowed(false);
        descTable.setColumnSelectionAllowed(false);
        descTable.getTableHeader().setReorderingAllowed(false);
        ((DefaultTableModel) descTable.getModel()).setColumnIdentifiers(columnNames);
        descTable.getColumnModel().getColumn(NAME_COLUMN).setPreferredWidth(140);
        descTable.getColumnModel().getColumn(NAME_COLUMN).setMaxWidth(140);
        descTable.getColumnModel().getColumn(NAME_COLUMN).setCellRenderer(renderer);
        descTable.getColumnModel().getColumn(VALUE_COLUMN).setCellRenderer(renderer);
        descTable.getColumnModel().getColumn(NAME_COLUMN).setCellEditor(editor);
        descTable.getColumnModel().getColumn(VALUE_COLUMN).setCellEditor(editor);
        descTable.addKeyListener(new Utils.CopyKeyAdapter());
        descTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        JScrollPane descTableScrollPane = new JScrollPane(descTable);
        descBorderPanel.setBorder(
            BorderFactory.createTitledBorder(Messages.DESCRIPTOR));
        descBorderPanel.add(descTableScrollPane);
        // Add the two tables to the grid
        //
        add(infoBorderPanel);
        add(descBorderPanel);
    }

    // Call on EDT
    public void emptyInfoTable() {
        DefaultTableModel tableModel = (DefaultTableModel) infoTable.getModel();
        while (tableModel.getRowCount() > 0) {
            tableModel.removeRow(0);
        }
    }

    // Call on EDT
    public void emptyDescTable() {
        DefaultTableModel tableModel = (DefaultTableModel) descTable.getModel();
        while (tableModel.getRowCount() > 0) {
            tableModel.removeRow(0);
        }
    }

    // Call on EDT
    private void addDescriptor(Descriptor desc, String text) {
        if (desc != null && desc.getFieldNames().length > 0) {
            DefaultTableModel tableModel = (DefaultTableModel) descTable.getModel();
            Object rowData[] = new Object[2];
            rowData[0] = new TableRowDivider(text);
            rowData[1] = new TableRowDivider("");
            tableModel.addRow(rowData);
            for (String fieldName : desc.getFieldNames()) {
                rowData[0] = fieldName;
                Object fieldValue = desc.getFieldValue(fieldName);
                if (fieldValue instanceof boolean[]) {
                    rowData[1] = Arrays.toString((boolean[]) fieldValue);
                } else if (fieldValue instanceof byte[]) {
                    rowData[1] = Arrays.toString((byte[]) fieldValue);
                } else if (fieldValue instanceof char[]) {
                    rowData[1] = Arrays.toString((char[]) fieldValue);
                } else if (fieldValue instanceof double[]) {
                    rowData[1] = Arrays.toString((double[]) fieldValue);
                } else if (fieldValue instanceof float[]) {
                    rowData[1] = Arrays.toString((float[]) fieldValue);
                } else if (fieldValue instanceof int[]) {
                    rowData[1] = Arrays.toString((int[]) fieldValue);
                } else if (fieldValue instanceof long[]) {
                    rowData[1] = Arrays.toString((long[]) fieldValue);
                } else if (fieldValue instanceof short[]) {
                    rowData[1] = Arrays.toString((short[]) fieldValue);
                } else if (fieldValue instanceof Object[]) {
                    rowData[1] = Arrays.toString((Object[]) fieldValue);
                } else {
                    rowData[1] = fieldValue;
                }
                tableModel.addRow(rowData);
            }
            tableModel.newDataAvailable(new TableModelEvent(tableModel));
        }
    }

    // Call on EDT
    public void addMBeanInfo(XMBean mbean, MBeanInfo mbeanInfo) {
        emptyInfoTable();
        emptyDescTable();
        ((TitledBorder) infoBorderPanel.getBorder()).setTitle(
                Messages.MBEAN_INFO);
        String text = Messages.INFO + ":";
        DefaultTableModel tableModel = (DefaultTableModel) infoTable.getModel();
        Object rowData[] = new Object[2];
        rowData[0] = new TableRowDivider(text);
        rowData[1] = new TableRowDivider("");
        tableModel.addRow(rowData);
        rowData[0] = Messages.OBJECT_NAME;
        rowData[1] = mbean.getObjectName();
        tableModel.addRow(rowData);
        rowData[0] = Messages.CLASS_NAME;
        rowData[1] = mbeanInfo.getClassName();
        tableModel.addRow(rowData);
        rowData[0] = Messages.DESCRIPTION;
        rowData[1] = mbeanInfo.getDescription();
        tableModel.addRow(rowData);
        addDescriptor(mbeanInfo.getDescriptor(), text);
        // MBeanConstructorInfo
        //
        int i = 0;
        for (MBeanConstructorInfo mbci : mbeanInfo.getConstructors()) {
            addMBeanConstructorInfo(mbci,
                    Messages.CONSTRUCTOR + "-" + i + ":");
            // MBeanParameterInfo
            //
            int j = 0;
            for (MBeanParameterInfo mbpi : mbci.getSignature()) {
                addMBeanParameterInfo(mbpi,
                        Messages.PARAMETER + "-" + i + "-" + j + ":");
                j++;
            }
            i++;
        }
        tableModel.newDataAvailable(new TableModelEvent(tableModel));
    }

    // Call on EDT
    public void addMBeanAttributeInfo(MBeanAttributeInfo mbai) {
        emptyInfoTable();
        emptyDescTable();
        ((TitledBorder) infoBorderPanel.getBorder()).setTitle(
                Messages.MBEAN_ATTRIBUTE_INFO);
        String text = Messages.ATTRIBUTE + ":";
        DefaultTableModel tableModel = (DefaultTableModel) infoTable.getModel();
        Object rowData[] = new Object[2];
        rowData[0] = new TableRowDivider(text);
        rowData[1] = new TableRowDivider("");
        tableModel.addRow(rowData);
        rowData[0] = Messages.NAME;
        rowData[1] = mbai.getName();
        tableModel.addRow(rowData);
        rowData[0] = Messages.DESCRIPTION;
        rowData[1] = mbai.getDescription();
        tableModel.addRow(rowData);
        rowData[0] = Messages.READABLE;
        rowData[1] = mbai.isReadable();
        tableModel.addRow(rowData);
        rowData[0] = Messages.WRITABLE;
        rowData[1] = mbai.isWritable();
        tableModel.addRow(rowData);
        rowData[0] = Messages.IS;
        rowData[1] = mbai.isIs();
        tableModel.addRow(rowData);
        rowData[0] = Messages.TYPE;
        rowData[1] = mbai.getType();
        tableModel.addRow(rowData);
        addDescriptor(mbai.getDescriptor(), text);
        tableModel.newDataAvailable(new TableModelEvent(tableModel));
    }

    // Call on EDT
    public void addMBeanOperationInfo(MBeanOperationInfo mboi) {
        emptyInfoTable();
        emptyDescTable();
        ((TitledBorder) infoBorderPanel.getBorder()).setTitle(
                Messages.MBEAN_OPERATION_INFO);
        String text = Messages.OPERATION + ":";
        DefaultTableModel tableModel = (DefaultTableModel) infoTable.getModel();
        Object rowData[] = new Object[2];
        rowData[0] = new TableRowDivider(text);
        rowData[1] = new TableRowDivider("");
        tableModel.addRow(rowData);
        rowData[0] = Messages.NAME;
        rowData[1] = mboi.getName();
        tableModel.addRow(rowData);
        rowData[0] = Messages.DESCRIPTION;
        rowData[1] = mboi.getDescription();
        tableModel.addRow(rowData);
        rowData[0] = Messages.IMPACT;
        switch (mboi.getImpact()) {
            case MBeanOperationInfo.INFO:
                rowData[1] = Messages.INFO_CAPITALIZED;
                break;
            case MBeanOperationInfo.ACTION:
                rowData[1] = Messages.ACTION_CAPITALIZED;
                break;
            case MBeanOperationInfo.ACTION_INFO:
                rowData[1] = Messages.ACTION_INFO_CAPITALIZED;
                break;
            case MBeanOperationInfo.UNKNOWN:
                rowData[1] = Messages.UNKNOWN_CAPITALIZED;
                break;
        }
        tableModel.addRow(rowData);
        rowData[0] = Messages.RETURN_TYPE;
        rowData[1] = mboi.getReturnType();
        tableModel.addRow(rowData);
        addDescriptor(mboi.getDescriptor(), text);
        // MBeanParameterInfo
        //
        int i = 0;
        for (MBeanParameterInfo mbpi : mboi.getSignature()) {
            addMBeanParameterInfo(mbpi,
                    Messages.PARAMETER + "-" + i++ + ":");
        }
        tableModel.newDataAvailable(new TableModelEvent(tableModel));
    }

    // Call on EDT
    public void addMBeanNotificationInfo(MBeanNotificationInfo mbni) {
        emptyInfoTable();
        emptyDescTable();
        ((TitledBorder) infoBorderPanel.getBorder()).setTitle(
                Messages.MBEAN_NOTIFICATION_INFO);
        String text = Messages.NOTIFICATION + ":";
        DefaultTableModel tableModel = (DefaultTableModel) infoTable.getModel();
        Object rowData[] = new Object[2];
        rowData[0] = new TableRowDivider(text);
        rowData[1] = new TableRowDivider("");
        tableModel.addRow(rowData);
        rowData[0] = Messages.NAME;
        rowData[1] = mbni.getName();
        tableModel.addRow(rowData);
        rowData[0] = Messages.DESCRIPTION;
        rowData[1] = mbni.getDescription();
        tableModel.addRow(rowData);
        rowData[0] = Messages.NOTIF_TYPES;
        rowData[1] = Arrays.toString(mbni.getNotifTypes());
        tableModel.addRow(rowData);
        addDescriptor(mbni.getDescriptor(), text);
        tableModel.newDataAvailable(new TableModelEvent(tableModel));
    }

    // Call on EDT
    private void addMBeanConstructorInfo(MBeanConstructorInfo mbci, String text) {
        DefaultTableModel tableModel = (DefaultTableModel) infoTable.getModel();
        Object rowData[] = new Object[2];
        rowData[0] = new TableRowDivider(text);
        rowData[1] = new TableRowDivider("");
        tableModel.addRow(rowData);
        rowData[0] = Messages.NAME;
        rowData[1] = mbci.getName();
        tableModel.addRow(rowData);
        rowData[0] = Messages.DESCRIPTION;
        rowData[1] = mbci.getDescription();
        tableModel.addRow(rowData);
        addDescriptor(mbci.getDescriptor(), text);
        tableModel.newDataAvailable(new TableModelEvent(tableModel));
    }

    // Call on EDT
    private void addMBeanParameterInfo(MBeanParameterInfo mbpi, String text) {
        DefaultTableModel tableModel = (DefaultTableModel) infoTable.getModel();
        Object rowData[] = new Object[2];
        rowData[0] = new TableRowDivider(text);
        rowData[1] = new TableRowDivider("");
        tableModel.addRow(rowData);
        rowData[0] = Messages.NAME;
        rowData[1] = mbpi.getName();
        tableModel.addRow(rowData);
        rowData[0] = Messages.DESCRIPTION;
        rowData[1] = mbpi.getDescription();
        tableModel.addRow(rowData);
        rowData[0] = Messages.TYPE;
        rowData[1] = mbpi.getType();
        tableModel.addRow(rowData);
        addDescriptor(mbpi.getDescriptor(), text);
        tableModel.newDataAvailable(new TableModelEvent(tableModel));
    }
}
