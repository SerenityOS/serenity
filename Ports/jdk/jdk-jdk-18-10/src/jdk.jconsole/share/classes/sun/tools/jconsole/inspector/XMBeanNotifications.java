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

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.tree.*;
import java.awt.Font;

import java.text.SimpleDateFormat;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.*;
import java.awt.Dimension;
import java.util.*;
import java.io.*;
import java.lang.reflect.Array;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;

import sun.tools.jconsole.JConsole;
import sun.tools.jconsole.Messages;

@SuppressWarnings("serial")
public class XMBeanNotifications extends JTable implements NotificationListener {

    private static final String[] columnNames = {
        Messages.TIME_STAMP,
        Messages.TYPE,
        Messages.USER_DATA,
        Messages.SEQ_NUM,
        Messages.MESSAGE,
        Messages.EVENT,
        Messages.SOURCE
    };
    private HashMap<ObjectName, XMBeanNotificationsListener> listeners =
            new HashMap<ObjectName, XMBeanNotificationsListener>();
    private volatile boolean subscribed;
    private XMBeanNotificationsListener currentListener;
    public static final String NOTIFICATION_RECEIVED_EVENT =
            "jconsole.xnotification.received";
    private List<NotificationListener> notificationListenersList;
    private volatile boolean enabled;
    private Font normalFont,  boldFont;
    private int rowMinHeight = -1;
    private TableCellEditor userDataEditor = new UserDataCellEditor();
    private NotifMouseListener mouseListener = new NotifMouseListener();
    private SimpleDateFormat timeFormater = new SimpleDateFormat("HH:mm:ss:SSS");
    private static TableCellEditor editor =
            new Utils.ReadOnlyTableCellEditor(new JTextField());

    public XMBeanNotifications() {
        super(new TableSorter(columnNames, 0));
        setColumnSelectionAllowed(false);
        setRowSelectionAllowed(false);
        getTableHeader().setReorderingAllowed(false);
        ArrayList<NotificationListener> l =
                new ArrayList<NotificationListener>(1);
        notificationListenersList = Collections.synchronizedList(l);

        addMouseListener(mouseListener);

        TableColumnModel colModel = getColumnModel();
        colModel.getColumn(0).setPreferredWidth(45);
        colModel.getColumn(1).setPreferredWidth(50);
        colModel.getColumn(2).setPreferredWidth(50);
        colModel.getColumn(3).setPreferredWidth(40);
        colModel.getColumn(4).setPreferredWidth(50);
        colModel.getColumn(5).setPreferredWidth(50);
        setColumnEditors();
        addKeyListener(new Utils.CopyKeyAdapter());
    }

    // Call on EDT
    public void cancelCellEditing() {
        TableCellEditor tce = getCellEditor();
        if (tce != null) {
            tce.cancelCellEditing();
        }
    }

    // Call on EDT
    public void stopCellEditing() {
        TableCellEditor tce = getCellEditor();
        if (tce != null) {
            tce.stopCellEditing();
        }
    }

    // Call on EDT
    @Override
    public boolean isCellEditable(int row, int col) {
        UserDataCell cell = getUserDataCell(row, col);
        if (cell != null) {
            return cell.isMaximized();
        }
        return true;
    }

    // Call on EDT
    @Override
    public void setValueAt(Object value, int row, int column) {
    }

    // Call on EDT
    @Override
    public synchronized Component prepareRenderer(
            TableCellRenderer renderer, int row, int column) {
        //In case we have a repaint thread that is in the process of
        //repainting an obsolete table, just ignore the call.
        //It can happen when MBean selection is switched at a very quick rate
        if (row >= getRowCount()) {
            return null;
        }

        Component comp = super.prepareRenderer(renderer, row, column);

        if (normalFont == null) {
            normalFont = comp.getFont();
            boldFont = normalFont.deriveFont(Font.BOLD);
        }
        UserDataCell cell = getUserDataCell(row, 2);
        if (column == 2 && cell != null) {
            comp.setFont(boldFont);
            int size = cell.getHeight();
            if (size > 0) {
                if (getRowHeight(row) != size) {
                    setRowHeight(row, size);
                }
            }
        } else {
            comp.setFont(normalFont);
        }

        return comp;
    }

    // Call on EDT
    @Override
    public synchronized TableCellRenderer getCellRenderer(int row, int column) {
        //In case we have a repaint thread that is in the process of
        //repainting an obsolete table, just ignore the call.
        //It can happen when MBean selection is switched at a very quick rate
        if (row >= getRowCount()) {
            return null;
        }

        DefaultTableCellRenderer renderer;
        String toolTip = null;
        UserDataCell cell = getUserDataCell(row, column);
        if (cell != null && cell.isInited()) {
            renderer = (DefaultTableCellRenderer) cell.getRenderer();
        } else {
            renderer =
                    (DefaultTableCellRenderer) super.getCellRenderer(row, column);
        }

        if (cell != null) {
            toolTip = Messages.DOUBLE_CLICK_TO_EXPAND_FORWARD_SLASH_COLLAPSE+
                    ". " + cell.toString();
        } else {
            Object val =
                    ((DefaultTableModel) getModel()).getValueAt(row, column);
            if (val != null) {
                toolTip = val.toString();
            }
        }

        renderer.setToolTipText(toolTip);

        return renderer;
    }

    // Call on EDT
    private UserDataCell getUserDataCell(int row, int column) {
        Object obj = ((DefaultTableModel) getModel()).getValueAt(row, column);
        if (obj instanceof UserDataCell) {
            return (UserDataCell) obj;
        }
        return null;
    }

    synchronized void dispose() {
        listeners.clear();
    }

    public long getReceivedNotifications(XMBean mbean) {
        XMBeanNotificationsListener listener =
                listeners.get(mbean.getObjectName());
        if (listener == null) {
            return 0;
        } else {
            return listener.getReceivedNotifications();
        }
    }

    public synchronized boolean clearCurrentNotifications() {
        emptyTable();
        if (currentListener != null) {
            currentListener.clear();
            return true;
        } else {
            return false;
        }
    }

    public synchronized boolean unregisterListener(DefaultMutableTreeNode node) {
        XMBean mbean = (XMBean) ((XNodeInfo) node.getUserObject()).getData();
        return unregister(mbean.getObjectName());
    }

    public synchronized void registerListener(DefaultMutableTreeNode node)
            throws InstanceNotFoundException, IOException {
        XMBean mbean = (XMBean) ((XNodeInfo) node.getUserObject()).getData();
        if (!subscribed) {
            try {
                mbean.getMBeanServerConnection().addNotificationListener(
                        MBeanServerDelegate.DELEGATE_NAME, this, null, null);
                subscribed = true;
            } catch (Exception e) {
                if (JConsole.isDebug()) {
                    System.err.println("Error adding listener for delegate:");
                    e.printStackTrace();
                }
            }
        }
        XMBeanNotificationsListener listener =
                listeners.get(mbean.getObjectName());
        if (listener == null) {
            listener = new XMBeanNotificationsListener(
                    this, mbean, node, columnNames);
            listeners.put(mbean.getObjectName(), listener);
        } else {
            if (!listener.isRegistered()) {
                emptyTable();
                listener.register(node);
            }
        }
        enabled = true;
        currentListener = listener;
    }

    public synchronized void handleNotification(
            Notification notif, Object handback) {
        try {
            if (notif instanceof MBeanServerNotification) {
                ObjectName mbean =
                        ((MBeanServerNotification) notif).getMBeanName();
                if (notif.getType().indexOf("JMX.mbean.unregistered") >= 0) {
                    unregister(mbean);
                }
            }
        } catch (Exception e) {
            if (JConsole.isDebug()) {
                System.err.println("Error unregistering notification:");
                e.printStackTrace();
            }
        }
    }

    public synchronized void disableNotifications() {
        emptyTable();
        currentListener = null;
        enabled = false;
    }

    private synchronized boolean unregister(ObjectName mbean) {
        XMBeanNotificationsListener listener = listeners.get(mbean);
        if (listener != null && listener.isRegistered()) {
            listener.unregister();
            return true;
        } else {
            return false;
        }
    }

    public void addNotificationsListener(NotificationListener nl) {
        notificationListenersList.add(nl);
    }

    public void removeNotificationsListener(NotificationListener nl) {
        notificationListenersList.remove(nl);
    }

    // Call on EDT
    void fireNotificationReceived(
            XMBeanNotificationsListener listener, XMBean mbean,
            DefaultMutableTreeNode node, Object[] rowData, long received) {
        if (enabled) {
            DefaultTableModel tableModel = (DefaultTableModel) getModel();
            if (listener == currentListener) {
                tableModel.insertRow(0, rowData);
                repaint();
            }
        }
        Notification notif =
                new Notification(NOTIFICATION_RECEIVED_EVENT, this, 0);
        notif.setUserData(received);
        for (NotificationListener nl : notificationListenersList) {
            nl.handleNotification(notif, node);
        }
    }

    // Call on EDT
    private void updateModel(List<Object[]> data) {
        emptyTable();
        DefaultTableModel tableModel = (DefaultTableModel) getModel();
        for (Object[] rowData : data) {
            tableModel.addRow(rowData);
        }
    }

    public synchronized boolean isListenerRegistered(XMBean mbean) {
        XMBeanNotificationsListener listener =
                listeners.get(mbean.getObjectName());
        if (listener == null) {
            return false;
        }
        return listener.isRegistered();
    }

    // Call on EDT
    public synchronized void loadNotifications(XMBean mbean) {
        XMBeanNotificationsListener listener =
                listeners.get(mbean.getObjectName());
        emptyTable();
        if (listener != null) {
            enabled = true;
            List<Object[]> data = listener.getData();
            updateModel(data);
            currentListener = listener;
            validate();
            repaint();
        } else {
            enabled = false;
        }
    }

    // Call on EDT
    private void setColumnEditors() {
        TableColumnModel tcm = getColumnModel();
        for (int i = 0; i < columnNames.length; i++) {
            TableColumn tc = tcm.getColumn(i);
            if (i == 2) {
                tc.setCellEditor(userDataEditor);
            } else {
                tc.setCellEditor(editor);
            }
        }
    }

    // Call on EDT
    public boolean isTableEditable() {
        return true;
    }

    // Call on EDT
    public synchronized void emptyTable() {
        DefaultTableModel model = (DefaultTableModel) getModel();
        //invalidate();
        while (model.getRowCount() > 0) {
            model.removeRow(0);
        }
        validate();
    }

    // Call on EDT
    synchronized void updateUserDataCell(int row, int col) {
        Object obj = getModel().getValueAt(row, 2);
        if (obj instanceof UserDataCell) {
            UserDataCell cell = (UserDataCell) obj;
            if (!cell.isInited()) {
                if (rowMinHeight == -1) {
                    rowMinHeight = getRowHeight(row);
                }
                cell.init(super.getCellRenderer(row, col), rowMinHeight);
            }

            cell.switchState();
            setRowHeight(row, cell.getHeight());

            if (!cell.isMaximized()) {
                cancelCellEditing();
                //Back to simple editor.
                editCellAt(row, 2);
            }

            invalidate();
            repaint();
        }
    }

    class UserDataCellRenderer extends DefaultTableCellRenderer {

        Component comp;

        UserDataCellRenderer(Component comp) {
            this.comp = comp;
            Dimension d = comp.getPreferredSize();
            if (d.getHeight() > 200) {
                comp.setPreferredSize(new Dimension((int) d.getWidth(), 200));
            }
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
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

    class UserDataCell {

        TableCellRenderer minRenderer;
        UserDataCellRenderer maxRenderer;
        int minHeight;
        boolean minimized = true;
        boolean init = false;
        Object userData;

        UserDataCell(Object userData, Component max) {
            this.userData = userData;
            this.maxRenderer = new UserDataCellRenderer(max);

        }

        @Override
        public String toString() {
            if (userData == null) {
                return null;
            }
            if (userData.getClass().isArray()) {
                String name =
                        Utils.getArrayClassName(userData.getClass().getName());
                int length = Array.getLength(userData);
                return name + "[" + length + "]";
            }

            if (userData instanceof CompositeData ||
                    userData instanceof TabularData) {
                return userData.getClass().getName();
            }

            return userData.toString();
        }

        boolean isInited() {
            return init;
        }

        void init(TableCellRenderer minRenderer, int minHeight) {
            this.minRenderer = minRenderer;
            this.minHeight = minHeight;
            init = true;
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
            if (minimized) {
                return minHeight;
            } else {
                return (int) maxRenderer.getComponent().
                        getPreferredSize().getHeight();
            }
        }

        TableCellRenderer getRenderer() {
            if (minimized) {
                return minRenderer;
            } else {
                return maxRenderer;
            }
        }
    }

    class NotifMouseListener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
                if (e.getClickCount() >= 2) {
                    int row = XMBeanNotifications.this.getSelectedRow();
                    int col = XMBeanNotifications.this.getSelectedColumn();
                    if (col != 2) {
                        return;
                    }
                    if (col == -1 || row == -1) {
                        return;
                    }

                    XMBeanNotifications.this.updateUserDataCell(row, col);
                }
            }
        }
    }

    class UserDataCellEditor extends XTextFieldEditor {
        // implements javax.swing.table.TableCellEditor
        @Override
        public Component getTableCellEditorComponent(
                JTable table,
                Object value,
                boolean isSelected,
                int row,
                int column) {
            Object val = value;
            if (column == 2) {
                Object obj = getModel().getValueAt(row, column);
                if (obj instanceof UserDataCell) {
                    UserDataCell cell = (UserDataCell) obj;
                    if (cell.getRenderer() instanceof UserDataCellRenderer) {
                        UserDataCellRenderer zr =
                                (UserDataCellRenderer) cell.getRenderer();
                        return zr.getComponent();
                    }
                } else {
                    Component comp = super.getTableCellEditorComponent(
                            table, val, isSelected, row, column);
                    textField.setEditable(false);
                    return comp;
                }
            }
            return super.getTableCellEditorComponent(
                    table,
                    val,
                    isSelected,
                    row,
                    column);
        }

        @Override
        public boolean stopCellEditing() {
            int editingRow = getEditingRow();
            int editingColumn = getEditingColumn();
            if (editingColumn == 2) {
                Object obj = getModel().getValueAt(editingRow, editingColumn);
                if (obj instanceof UserDataCell) {
                    UserDataCell cell = (UserDataCell) obj;
                    if (cell.isMaximized()) {
                        cancelCellEditing();
                        return true;
                    }
                }
            }
            return super.stopCellEditing();
        }
    }

    class XMBeanNotificationsListener implements NotificationListener {

        private XMBean xmbean;
        private DefaultMutableTreeNode node;
        private volatile long received;
        private XMBeanNotifications notifications;
        private volatile boolean unregistered;
        private ArrayList<Object[]> data = new ArrayList<Object[]>();

        public XMBeanNotificationsListener(
                XMBeanNotifications notifications,
                XMBean xmbean,
                DefaultMutableTreeNode node,
                String[] columnNames) {
            this.notifications = notifications;
            this.xmbean = xmbean;
            this.node = node;
            register(node);
        }

        public synchronized List<Object[]> getData() {
            return data;
        }

        public synchronized void clear() {
            data.clear();
            received = 0;
        }

        public synchronized boolean isRegistered() {
            return !unregistered;
        }

        public synchronized void unregister() {
            try {
                xmbean.getMBeanServerConnection().removeNotificationListener(
                        xmbean.getObjectName(), this, null, null);
            } catch (Exception e) {
                if (JConsole.isDebug()) {
                    System.err.println("Error removing listener:");
                    e.printStackTrace();
                }
            }
            unregistered = true;
        }

        public synchronized long getReceivedNotifications() {
            return received;
        }

        public synchronized void register(DefaultMutableTreeNode node) {
            clear();
            this.node = node;
            try {
                xmbean.getMBeanServerConnection().addNotificationListener(
                        xmbean.getObjectName(), this, null, null);
                unregistered = false;
            } catch (Exception e) {
                if (JConsole.isDebug()) {
                    System.err.println("Error adding listener:");
                    e.printStackTrace();
                }
            }
        }

        public synchronized void handleNotification(
                final Notification n, Object hb) {
            EventQueue.invokeLater(new Runnable() {

                public void run() {
                    synchronized (XMBeanNotificationsListener.this) {
                        try {
                            if (unregistered) {
                                return;
                            }
                            Date receivedDate = new Date(n.getTimeStamp());
                            String time = timeFormater.format(receivedDate);

                            Object userData = n.getUserData();
                            Component comp = null;
                            UserDataCell cell = null;
                            if ((comp = XDataViewer.createNotificationViewer(userData)) != null) {
                                XDataViewer.registerForMouseEvent(comp, mouseListener);
                                cell = new UserDataCell(userData, comp);
                            }

                            Object[] rowData = {
                                time,
                                n.getType(),
                                (cell == null ? userData : cell),
                                n.getSequenceNumber(),
                                n.getMessage(),
                                n,
                                n.getSource()
                            };
                            received++;
                            data.add(0, rowData);

                            notifications.fireNotificationReceived(
                                    XMBeanNotificationsListener.this,
                                    xmbean, node, rowData, received);
                        } catch (Exception e) {
                            if (JConsole.isDebug()) {
                                System.err.println("Error handling notification:");
                                e.printStackTrace();
                            }
                        }
                    }
                }
            });
        }
    }
}
