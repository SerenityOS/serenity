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
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.management.IntrospectionException;
import javax.management.NotificationListener;
import javax.management.MBeanInfo;
import javax.management.InstanceNotFoundException;
import javax.management.ReflectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.Notification;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.border.LineBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import sun.tools.jconsole.*;
import sun.tools.jconsole.inspector.XNodeInfo.Type;

@SuppressWarnings("serial")
public class XSheet extends JPanel
        implements ActionListener, NotificationListener {

    private JPanel mainPanel;
    private JPanel southPanel;
    // Node being currently displayed
    private volatile DefaultMutableTreeNode currentNode;
    // MBean being currently displayed
    private volatile XMBean mbean;
    // XMBeanAttributes container
    private XMBeanAttributes mbeanAttributes;
    // XMBeanOperations container
    private XMBeanOperations mbeanOperations;
    // XMBeanNotifications container
    private XMBeanNotifications mbeanNotifications;
    // XMBeanInfo container
    private XMBeanInfo mbeanInfo;
    // Refresh JButton (mbean attributes case)
    private JButton refreshButton;
    // Subscribe/Unsubscribe/Clear JButton (mbean notifications case)
    private JButton clearButton,  subscribeButton,  unsubscribeButton;
    // Reference to MBeans tab
    private MBeansTab mbeansTab;

    public XSheet(MBeansTab mbeansTab) {
        this.mbeansTab = mbeansTab;
        setupScreen();
    }

    public void dispose() {
        clear();
        XDataViewer.dispose(mbeansTab);
        mbeanNotifications.dispose();
    }

    private void setupScreen() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Color.GRAY));
        // add main panel to XSheet
        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        // add south panel to XSheet
        southPanel = new JPanel();
        add(southPanel, BorderLayout.SOUTH);
        // create the refresh button
        refreshButton = new JButton(Messages.MBEANS_TAB_REFRESH_ATTRIBUTES_BUTTON);
        refreshButton.setMnemonic(Resources.getMnemonicInt(Messages.MBEANS_TAB_REFRESH_ATTRIBUTES_BUTTON));
        refreshButton.setToolTipText(Messages.MBEANS_TAB_REFRESH_ATTRIBUTES_BUTTON_TOOLTIP);
        refreshButton.addActionListener(this);
        // create the clear button
        clearButton = new JButton(Messages.MBEANS_TAB_CLEAR_NOTIFICATIONS_BUTTON);
        clearButton.setMnemonic(Resources.getMnemonicInt(Messages.MBEANS_TAB_CLEAR_NOTIFICATIONS_BUTTON));
        clearButton.setToolTipText(Messages.MBEANS_TAB_CLEAR_NOTIFICATIONS_BUTTON_TOOLTIP);
        clearButton.addActionListener(this);
        // create the subscribe button
        subscribeButton = new JButton(Messages.MBEANS_TAB_SUBSCRIBE_NOTIFICATIONS_BUTTON);
        subscribeButton.setMnemonic(Resources.getMnemonicInt(Messages.MBEANS_TAB_SUBSCRIBE_NOTIFICATIONS_BUTTON));
        subscribeButton.setToolTipText(Messages.MBEANS_TAB_SUBSCRIBE_NOTIFICATIONS_BUTTON_TOOLTIP);
        subscribeButton.addActionListener(this);
        // create the unsubscribe button
        unsubscribeButton = new JButton(Messages.MBEANS_TAB_UNSUBSCRIBE_NOTIFICATIONS_BUTTON);
        unsubscribeButton.setMnemonic(Resources.getMnemonicInt(Messages.MBEANS_TAB_UNSUBSCRIBE_NOTIFICATIONS_BUTTON));
        unsubscribeButton.setToolTipText(Messages.MBEANS_TAB_UNSUBSCRIBE_NOTIFICATIONS_BUTTON_TOOLTIP);
        unsubscribeButton.addActionListener(this);
        // create XMBeanAttributes container
        mbeanAttributes = new XMBeanAttributes(mbeansTab);
        // create XMBeanOperations container
        mbeanOperations = new XMBeanOperations(mbeansTab);
        mbeanOperations.addOperationsListener(this);
        // create XMBeanNotifications container
        mbeanNotifications = new XMBeanNotifications();
        mbeanNotifications.addNotificationsListener(this);
        // create XMBeanInfo container
        mbeanInfo = new XMBeanInfo();
    }

    private boolean isSelectedNode(DefaultMutableTreeNode n, DefaultMutableTreeNode cn) {
        return (cn == n);
    }

    // Call on EDT
    private void showErrorDialog(Object message, String title) {
        new ThreadDialog(this, message, title, JOptionPane.ERROR_MESSAGE).run();
    }

    public boolean isMBeanNode(DefaultMutableTreeNode node) {
        Object userObject = node.getUserObject();
        if (userObject instanceof XNodeInfo) {
            XNodeInfo uo = (XNodeInfo) userObject;
            return uo.getType().equals(Type.MBEAN);
        }
        return false;
    }

    // Call on EDT
    public synchronized void displayNode(DefaultMutableTreeNode node) {
        clear();
        displayEmptyNode();
        if (node == null) {
            return;
        }
        currentNode = node;
        Object userObject = node.getUserObject();
        if (userObject instanceof XNodeInfo) {
            XNodeInfo uo = (XNodeInfo) userObject;
            switch (uo.getType()) {
                case MBEAN:
                    displayMBeanNode(node);
                    break;
                case NONMBEAN:
                    displayEmptyNode();
                    break;
                case ATTRIBUTES:
                    displayMBeanAttributesNode(node);
                    break;
                case OPERATIONS:
                    displayMBeanOperationsNode(node);
                    break;
                case NOTIFICATIONS:
                    displayMBeanNotificationsNode(node);
                    break;
                case ATTRIBUTE:
                case OPERATION:
                case NOTIFICATION:
                    displayMetadataNode(node);
                    break;
                default:
                    displayEmptyNode();
                    break;
            }
        } else {
            displayEmptyNode();
        }
    }

    // Call on EDT
    private void displayMBeanNode(final DefaultMutableTreeNode node) {
        final XNodeInfo uo = (XNodeInfo) node.getUserObject();
        if (!uo.getType().equals(Type.MBEAN)) {
            return;
        }
        mbean = (XMBean) uo.getData();
        SwingWorker<MBeanInfo, Void> sw = new SwingWorker<MBeanInfo, Void>() {
            @Override
            public MBeanInfo doInBackground() throws InstanceNotFoundException,
                    IntrospectionException, ReflectionException, IOException {
                return mbean.getMBeanInfo();
            }
            @Override
            protected void done() {
                try {
                    MBeanInfo mbi = get();
                    if (mbi != null) {
                        if (!isSelectedNode(node, currentNode)) {
                            return;
                        }
                        mbeanInfo.addMBeanInfo(mbean, mbi);
                        invalidate();
                        mainPanel.removeAll();
                        mainPanel.add(mbeanInfo, BorderLayout.CENTER);
                        southPanel.setVisible(false);
                        southPanel.removeAll();
                        validate();
                        repaint();
                    }
                } catch (Exception e) {
                    Throwable t = Utils.getActualException(e);
                    if (JConsole.isDebug()) {
                        System.err.println("Couldn't get MBeanInfo for MBean [" +
                                mbean.getObjectName() + "]");
                        t.printStackTrace();
                    }
                    showErrorDialog(t.toString(),
                            Messages.PROBLEM_DISPLAYING_MBEAN);
                }
            }
        };
        sw.execute();
    }

    // Call on EDT
    private void displayMetadataNode(final DefaultMutableTreeNode node) {
        final XNodeInfo uo = (XNodeInfo) node.getUserObject();
        final XMBeanInfo mbi = mbeanInfo;
        switch (uo.getType()) {
            case ATTRIBUTE:
                SwingWorker<MBeanAttributeInfo, Void> sw =
                        new SwingWorker<MBeanAttributeInfo, Void>() {
                            @Override
                            public MBeanAttributeInfo doInBackground() {
                                Object attrData = uo.getData();
                                mbean = (XMBean) ((Object[]) attrData)[0];
                                MBeanAttributeInfo mbai =
                                        (MBeanAttributeInfo) ((Object[]) attrData)[1];
                                mbeanAttributes.loadAttributes(mbean, new MBeanInfo(
                                        null, null, new MBeanAttributeInfo[]{mbai},
                                        null, null, null));
                                return mbai;
                            }
                            @Override
                            protected void done() {
                                try {
                                    MBeanAttributeInfo mbai = get();
                                    if (!isSelectedNode(node, currentNode)) {
                                        return;
                                    }
                                    invalidate();
                                    mainPanel.removeAll();
                                    JPanel attributePanel =
                                            new JPanel(new BorderLayout());
                                    JPanel attributeBorderPanel =
                                            new JPanel(new BorderLayout());
                                    attributeBorderPanel.setBorder(
                                            BorderFactory.createTitledBorder(
                                            Messages.ATTRIBUTE_VALUE));
                                    JPanel attributeValuePanel =
                                            new JPanel(new BorderLayout());
                                    attributeValuePanel.setBorder(
                                            LineBorder.createGrayLineBorder());
                                    attributeValuePanel.add(mbeanAttributes.getTableHeader(),
                                            BorderLayout.PAGE_START);
                                    attributeValuePanel.add(mbeanAttributes,
                                            BorderLayout.CENTER);
                                    attributeBorderPanel.add(attributeValuePanel,
                                            BorderLayout.CENTER);
                                    JPanel refreshButtonPanel = new JPanel();
                                    refreshButtonPanel.add(refreshButton);
                                    attributeBorderPanel.add(refreshButtonPanel,
                                            BorderLayout.SOUTH);
                                    refreshButton.setEnabled(true);
                                    attributePanel.add(attributeBorderPanel,
                                            BorderLayout.NORTH);
                                    mbi.addMBeanAttributeInfo(mbai);
                                    attributePanel.add(mbi, BorderLayout.CENTER);
                                    mainPanel.add(attributePanel,
                                            BorderLayout.CENTER);
                                    southPanel.setVisible(false);
                                    southPanel.removeAll();
                                    validate();
                                    repaint();
                                } catch (Exception e) {
                                    Throwable t = Utils.getActualException(e);
                                    if (JConsole.isDebug()) {
                                        System.err.println("Problem displaying MBean " +
                                                "attribute for MBean [" +
                                                mbean.getObjectName() + "]");
                                        t.printStackTrace();
                                    }
                                    showErrorDialog(t.toString(),
                                            Messages.PROBLEM_DISPLAYING_MBEAN);
                                }
                            }
                        };
                sw.execute();
                break;
            case OPERATION:
                Object operData = uo.getData();
                mbean = (XMBean) ((Object[]) operData)[0];
                MBeanOperationInfo mboi =
                        (MBeanOperationInfo) ((Object[]) operData)[1];
                mbeanOperations.loadOperations(mbean,
                        new MBeanInfo(null, null, null, null,
                        new MBeanOperationInfo[]{mboi}, null));
                invalidate();
                mainPanel.removeAll();
                JPanel operationPanel = new JPanel(new BorderLayout());
                JPanel operationBorderPanel = new JPanel(new BorderLayout());
                operationBorderPanel.setBorder(BorderFactory.createTitledBorder(
                        Messages.OPERATION_INVOCATION));
                operationBorderPanel.add(new JScrollPane(mbeanOperations));
                operationPanel.add(operationBorderPanel, BorderLayout.NORTH);
                mbi.addMBeanOperationInfo(mboi);
                operationPanel.add(mbi, BorderLayout.CENTER);
                mainPanel.add(operationPanel, BorderLayout.CENTER);
                southPanel.setVisible(false);
                southPanel.removeAll();
                validate();
                repaint();
                break;
            case NOTIFICATION:
                Object notifData = uo.getData();
                invalidate();
                mainPanel.removeAll();
                mbi.addMBeanNotificationInfo((MBeanNotificationInfo) notifData);
                mainPanel.add(mbi, BorderLayout.CENTER);
                southPanel.setVisible(false);
                southPanel.removeAll();
                validate();
                repaint();
                break;
        }
    }

    // Call on EDT
    private void displayMBeanAttributesNode(final DefaultMutableTreeNode node) {
        final XNodeInfo uo = (XNodeInfo) node.getUserObject();
        if (!uo.getType().equals(Type.ATTRIBUTES)) {
            return;
        }
        mbean = (XMBean) uo.getData();
        final XMBean xmb = mbean;
        SwingWorker<MBeanInfo,Void> sw = new SwingWorker<MBeanInfo,Void>() {
            @Override
            public MBeanInfo doInBackground() throws InstanceNotFoundException,
                    IntrospectionException, ReflectionException, IOException {
                MBeanInfo mbi = xmb.getMBeanInfo();
                return mbi;
            }
            @Override
            protected void done() {
                try {
                    MBeanInfo mbi = get();
                    if (mbi != null && mbi.getAttributes() != null &&
                            mbi.getAttributes().length > 0) {

                        mbeanAttributes.loadAttributes(xmb, mbi);

                        if (!isSelectedNode(node, currentNode)) {
                            return;
                        }
                        invalidate();
                        mainPanel.removeAll();
                        JPanel borderPanel = new JPanel(new BorderLayout());
                        borderPanel.setBorder(BorderFactory.createTitledBorder(
                                Messages.ATTRIBUTE_VALUES));
                        borderPanel.add(new JScrollPane(mbeanAttributes));
                        mainPanel.add(borderPanel, BorderLayout.CENTER);
                        // add the refresh button to the south panel
                        southPanel.removeAll();
                        southPanel.add(refreshButton, BorderLayout.SOUTH);
                        southPanel.setVisible(true);
                        refreshButton.setEnabled(true);
                        validate();
                        repaint();
                    }
                } catch (Exception e) {
                    Throwable t = Utils.getActualException(e);
                    if (JConsole.isDebug()) {
                        System.err.println("Problem displaying MBean " +
                                "attributes for MBean [" +
                                mbean.getObjectName() + "]");
                        t.printStackTrace();
                    }
                    showErrorDialog(t.toString(),
                            Messages.PROBLEM_DISPLAYING_MBEAN);
                }
            }
        };
        sw.execute();
    }

    // Call on EDT
    private void displayMBeanOperationsNode(final DefaultMutableTreeNode node) {
        final XNodeInfo uo = (XNodeInfo) node.getUserObject();
        if (!uo.getType().equals(Type.OPERATIONS)) {
            return;
        }
        mbean = (XMBean) uo.getData();
        SwingWorker<MBeanInfo, Void> sw = new SwingWorker<MBeanInfo, Void>() {
            @Override
            public MBeanInfo doInBackground() throws InstanceNotFoundException,
                    IntrospectionException, ReflectionException, IOException {
                return mbean.getMBeanInfo();
            }
            @Override
            protected void done() {
                try {
                    MBeanInfo mbi = get();
                    if (mbi != null) {
                        if (!isSelectedNode(node, currentNode)) {
                            return;
                        }
                        mbeanOperations.loadOperations(mbean, mbi);
                        invalidate();
                        mainPanel.removeAll();
                        JPanel borderPanel = new JPanel(new BorderLayout());
                        borderPanel.setBorder(BorderFactory.createTitledBorder(
                                Messages.OPERATION_INVOCATION));
                        borderPanel.add(new JScrollPane(mbeanOperations));
                        mainPanel.add(borderPanel, BorderLayout.CENTER);
                        southPanel.setVisible(false);
                        southPanel.removeAll();
                        validate();
                        repaint();
                    }
                } catch (Exception e) {
                    Throwable t = Utils.getActualException(e);
                    if (JConsole.isDebug()) {
                        System.err.println("Problem displaying MBean " +
                                "operations for MBean [" +
                                mbean.getObjectName() + "]");
                        t.printStackTrace();
                    }
                    showErrorDialog(t.toString(),
                            Messages.PROBLEM_DISPLAYING_MBEAN);
                }
            }
        };
        sw.execute();
    }

    // Call on EDT
    private void displayMBeanNotificationsNode(DefaultMutableTreeNode node) {
        final XNodeInfo uo = (XNodeInfo) node.getUserObject();
        if (!uo.getType().equals(Type.NOTIFICATIONS)) {
            return;
        }
        mbean = (XMBean) uo.getData();
        mbeanNotifications.loadNotifications(mbean);
        updateNotifications();
        invalidate();
        mainPanel.removeAll();
        JPanel borderPanel = new JPanel(new BorderLayout());
        borderPanel.setBorder(BorderFactory.createTitledBorder(
                Messages.NOTIFICATION_BUFFER));
        borderPanel.add(new JScrollPane(mbeanNotifications));
        mainPanel.add(borderPanel, BorderLayout.CENTER);
        // add the subscribe/unsubscribe/clear buttons to the south panel
        southPanel.removeAll();
        southPanel.add(subscribeButton, BorderLayout.WEST);
        southPanel.add(unsubscribeButton, BorderLayout.CENTER);
        southPanel.add(clearButton, BorderLayout.EAST);
        southPanel.setVisible(true);
        subscribeButton.setEnabled(true);
        unsubscribeButton.setEnabled(true);
        clearButton.setEnabled(true);
        validate();
        repaint();
    }

    // Call on EDT
    private void displayEmptyNode() {
        invalidate();
        mainPanel.removeAll();
        southPanel.removeAll();
        validate();
        repaint();
    }

    /**
     * Subscribe button action.
     */
    private void registerListener() {
        new SwingWorker<Void, Void>() {
            @Override
            public Void doInBackground()
                    throws InstanceNotFoundException, IOException {
                mbeanNotifications.registerListener(currentNode);
                return null;
            }
            @Override
            protected void done() {
                try {
                    get();
                    updateNotifications();
                    validate();
                } catch (Exception e) {
                    Throwable t = Utils.getActualException(e);
                    if (JConsole.isDebug()) {
                        System.err.println("Problem adding listener");
                        t.printStackTrace();
                    }
                    showErrorDialog(t.getMessage(),
                            Messages.PROBLEM_ADDING_LISTENER);
                }
            }
        }.execute();
    }

    /**
     * Unsubscribe button action.
     */
    private void unregisterListener() {
        new SwingWorker<Boolean, Void>() {
            @Override
            public Boolean doInBackground() {
                return mbeanNotifications.unregisterListener(currentNode);
            }
            @Override
            protected void done() {
                try {
                    if (get()) {
                        updateNotifications();
                        validate();
                    }
                } catch (Exception e) {
                    Throwable t = Utils.getActualException(e);
                    if (JConsole.isDebug()) {
                        System.err.println("Problem removing listener");
                        t.printStackTrace();
                    }
                    showErrorDialog(t.getMessage(),
                            Messages.PROBLEM_REMOVING_LISTENER);
                }
            }
        }.execute();
    }

    /**
     * Refresh button action.
     */
    private void refreshAttributes() {
        mbeanAttributes.refreshAttributes();
    }

    // Call on EDT
    private void updateNotifications() {
        if (mbeanNotifications.isListenerRegistered(mbean)) {
            long received = mbeanNotifications.getReceivedNotifications(mbean);
            updateReceivedNotifications(currentNode, received, false);
        } else {
            clearNotifications();
        }
    }

    /**
     * Update notification node label in MBean tree: "Notifications[received]".
     */
    // Call on EDT
    private void updateReceivedNotifications(
            DefaultMutableTreeNode emitter, long received, boolean bold) {
        String text = Messages.NOTIFICATIONS + "[" + received + "]";
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) mbeansTab.getTree().getLastSelectedPathComponent();
        if (bold && emitter != selectedNode) {
            text = "<html><b>" + text + "</b></html>";
        }
        updateNotificationsNodeLabel(emitter, text);
    }

    /**
     * Update notification node label in MBean tree: "Notifications".
     */
    // Call on EDT
    private void clearNotifications() {
        updateNotificationsNodeLabel(currentNode,
                Messages.NOTIFICATIONS);
    }

    /**
     * Update notification node label in MBean tree: "Notifications[0]".
     */
    // Call on EDT
    private void clearNotifications0() {
        updateNotificationsNodeLabel(currentNode,
                Messages.NOTIFICATIONS + "[0]");
    }

    /**
     * Update the label of the supplied MBean tree node.
     */
    // Call on EDT
    private void updateNotificationsNodeLabel(
            DefaultMutableTreeNode node, String label) {
        synchronized (mbeansTab.getTree()) {
            invalidate();
            XNodeInfo oldUserObject = (XNodeInfo) node.getUserObject();
            XNodeInfo newUserObject = new XNodeInfo(
                    oldUserObject.getType(), oldUserObject.getData(),
                    label, oldUserObject.getToolTipText());
            node.setUserObject(newUserObject);
            DefaultTreeModel model =
                    (DefaultTreeModel) mbeansTab.getTree().getModel();
            model.nodeChanged(node);
            validate();
            repaint();
        }
    }

    /**
     * Clear button action.
     */
    // Call on EDT
    private void clearCurrentNotifications() {
        mbeanNotifications.clearCurrentNotifications();
        if (mbeanNotifications.isListenerRegistered(mbean)) {
            // Update notifs in MBean tree "Notifications[0]".
            //
            // Notification buffer has been cleared with a listener been
            // registered so add "[0]" at the end of the node label.
            //
            clearNotifications0();
        } else {
            // Update notifs in MBean tree "Notifications".
            //
            // Notification buffer has been cleared without a listener been
            // registered so don't add "[0]" at the end of the node label.
            //
            clearNotifications();
        }
    }

    // Call on EDT
    private void clear() {
        mbeanAttributes.stopCellEditing();
        mbeanAttributes.emptyTable();
        mbeanAttributes.removeAttributes();
        mbeanOperations.removeOperations();
        mbeanNotifications.stopCellEditing();
        mbeanNotifications.emptyTable();
        mbeanNotifications.disableNotifications();
        mbean = null;
        currentNode = null;
    }

    /**
     * Notification listener: handles asynchronous reception
     * of MBean operation results and MBean notifications.
     */
    // Call on EDT
    public void handleNotification(Notification e, Object handback) {
        // Operation result
        if (e.getType().equals(XOperations.OPERATION_INVOCATION_EVENT)) {
            final Object message;
            if (handback == null) {
                JTextArea textArea = new JTextArea("null");
                textArea.setEditable(false);
                textArea.setEnabled(true);
                textArea.setRows(textArea.getLineCount());
                message = textArea;
            } else {
                Component comp = mbeansTab.getDataViewer().
                        createOperationViewer(handback, mbean);
                if (comp == null) {
                    JTextArea textArea = new JTextArea(handback.toString());
                    textArea.setEditable(false);
                    textArea.setEnabled(true);
                    textArea.setRows(textArea.getLineCount());
                    JScrollPane scrollPane = new JScrollPane(textArea);
                    Dimension d = scrollPane.getPreferredSize();
                    if (d.getWidth() > 400 || d.getHeight() > 250) {
                        scrollPane.setPreferredSize(new Dimension(400, 250));
                    }
                    message = scrollPane;
                } else {
                    if (!(comp instanceof JScrollPane)) {
                        comp = new JScrollPane(comp);
                    }
                    Dimension d = comp.getPreferredSize();
                    if (d.getWidth() > 400 || d.getHeight() > 250) {
                        comp.setPreferredSize(new Dimension(400, 250));
                    }
                    message = comp;
                }
            }
            new ThreadDialog(
                    (Component) e.getSource(),
                    message,
                    Messages.OPERATION_RETURN_VALUE,
                    JOptionPane.INFORMATION_MESSAGE).run();
        } // Got notification
        else if (e.getType().equals(
                XMBeanNotifications.NOTIFICATION_RECEIVED_EVENT)) {
            DefaultMutableTreeNode emitter = (DefaultMutableTreeNode) handback;
            Long received = (Long) e.getUserData();
            updateReceivedNotifications(emitter, received.longValue(), true);
        }
    }

    /**
     * Action listener: handles actions in panel buttons
     */
    // Call on EDT
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JButton) {
            JButton button = (JButton) e.getSource();
            // Refresh button
            if (button == refreshButton) {
                refreshAttributes();
                return;
            }
            // Clear button
            if (button == clearButton) {
                clearCurrentNotifications();
                return;
            }
            // Subscribe button
            if (button == subscribeButton) {
                registerListener();
                return;
            }
            // Unsubscribe button
            if (button == unsubscribeButton) {
                unregisterListener();
                return;
            }
        }
    }
}
