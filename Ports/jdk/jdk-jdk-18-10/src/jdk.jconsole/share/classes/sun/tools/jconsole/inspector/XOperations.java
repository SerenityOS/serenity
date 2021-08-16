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

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.FlowLayout;
import java.awt.Component;
import java.awt.event.*;
import java.util.*;

import javax.management.*;

import sun.tools.jconsole.MBeansTab;
import sun.tools.jconsole.JConsole;
import sun.tools.jconsole.Messages;

@SuppressWarnings("serial") // JDK implementation class
public abstract class XOperations extends JPanel implements ActionListener {

    public static final String OPERATION_INVOCATION_EVENT =
            "jam.xoperations.invoke.result";
    private java.util.List<NotificationListener> notificationListenersList;
    private Hashtable<JButton, OperationEntry> operationEntryTable;
    private XMBean mbean;
    private MBeanInfo mbeanInfo;
    private MBeansTab mbeansTab;

    public XOperations(MBeansTab mbeansTab) {
        super(new GridLayout(1, 1));
        this.mbeansTab = mbeansTab;
        operationEntryTable = new Hashtable<JButton, OperationEntry>();
        ArrayList<NotificationListener> l =
                new ArrayList<NotificationListener>(1);
        notificationListenersList =
                Collections.synchronizedList(l);
    }

    // Call on EDT
    public void removeOperations() {
        removeAll();
    }

    // Call on EDT
    public void loadOperations(XMBean mbean, MBeanInfo mbeanInfo) {
        this.mbean = mbean;
        this.mbeanInfo = mbeanInfo;
        // add operations information
        MBeanOperationInfo operations[] = mbeanInfo.getOperations();
        invalidate();

        // remove listeners, if any
        Component listeners[] = getComponents();
        for (int i = 0; i < listeners.length; i++) {
            if (listeners[i] instanceof JButton) {
                ((JButton) listeners[i]).removeActionListener(this);
            }
        }

        removeAll();
        setLayout(new BorderLayout());

        JButton methodButton;
        JLabel methodLabel;
        JPanel innerPanelLeft, innerPanelRight;
        JPanel outerPanelLeft, outerPanelRight;
        outerPanelLeft = new JPanel(new GridLayout(operations.length, 1));
        outerPanelRight = new JPanel(new GridLayout(operations.length, 1));

        for (int i = 0; i < operations.length; i++) {
            innerPanelLeft = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            innerPanelRight = new JPanel(new FlowLayout(FlowLayout.LEFT));
            String returnType = operations[i].getReturnType();
            if (returnType == null) {
                methodLabel = new JLabel("null", JLabel.RIGHT);
                if (JConsole.isDebug()) {
                    System.err.println(
                            "WARNING: The operation's return type " +
                            "shouldn't be \"null\". Check how the " +
                            "MBeanOperationInfo for the \"" +
                            operations[i].getName() + "\" operation has " +
                            "been defined in the MBean's implementation code.");
                }
            } else {
                methodLabel = new JLabel(
                        Utils.getReadableClassName(returnType), JLabel.RIGHT);
            }
            innerPanelLeft.add(methodLabel);
            if (methodLabel.getText().length() > 20) {
                methodLabel.setText(methodLabel.getText().
                        substring(methodLabel.getText().
                        lastIndexOf('.') + 1,
                        methodLabel.getText().length()));
            }

            methodButton = new JButton(operations[i].getName());
            methodButton.setToolTipText(operations[i].getDescription());
            boolean callable = isCallable(operations[i].getSignature());
            if (callable) {
                methodButton.addActionListener(this);
            } else {
                methodButton.setEnabled(false);
            }

            MBeanParameterInfo[] signature = operations[i].getSignature();
            OperationEntry paramEntry = new OperationEntry(operations[i],
                    callable,
                    methodButton,
                    this);
            operationEntryTable.put(methodButton, paramEntry);
            innerPanelRight.add(methodButton);
            if (signature.length == 0) {
                innerPanelRight.add(new JLabel("( )", JLabel.CENTER));
            } else {
                innerPanelRight.add(paramEntry);
            }

            outerPanelLeft.add(innerPanelLeft, BorderLayout.WEST);
            outerPanelRight.add(innerPanelRight, BorderLayout.CENTER);
        }
        add(outerPanelLeft, BorderLayout.WEST);
        add(outerPanelRight, BorderLayout.CENTER);
        validate();
    }

    private boolean isCallable(MBeanParameterInfo[] signature) {
        for (int i = 0; i < signature.length; i++) {
            if (!Utils.isEditableType(signature[i].getType())) {
                return false;
            }
        }
        return true;
    }

    // Call on EDT
    public void actionPerformed(final ActionEvent e) {
        performInvokeRequest((JButton) e.getSource());
    }

    void performInvokeRequest(final JButton button) {
        final OperationEntry entryIf = operationEntryTable.get(button);
        new SwingWorker<Object, Void>() {
            @Override
            public Object doInBackground() throws Exception {
                return mbean.invoke(button.getText(),
                        entryIf.getParameters(), entryIf.getSignature());
            }
            @Override
            protected void done() {
                try {
                    Object result = get();
                    // sends result notification to upper level if
                    // there is a return value
                    if (entryIf.getReturnType() != null &&
                            !entryIf.getReturnType().equals(Void.TYPE.getName()) &&
                            !entryIf.getReturnType().equals(Void.class.getName())) {
                        fireChangedNotification(OPERATION_INVOCATION_EVENT, button, result);
                    } else {
                        new ThreadDialog(
                                button,
                                Messages.METHOD_SUCCESSFULLY_INVOKED,
                                Messages.INFO,
                                JOptionPane.INFORMATION_MESSAGE).run();
                    }
                } catch (Throwable t) {
                    t = Utils.getActualException(t);
                    if (JConsole.isDebug()) {
                        t.printStackTrace();
                    }
                    new ThreadDialog(
                            button,
                            Messages.PROBLEM_INVOKING + " " +
                            button.getText() + " : " + t.toString(),
                            Messages.ERROR,
                            JOptionPane.ERROR_MESSAGE).run();
                }
            }
        }.execute();
    }

    public void addOperationsListener(NotificationListener nl) {
        notificationListenersList.add(nl);
    }

    public void removeOperationsListener(NotificationListener nl) {
        notificationListenersList.remove(nl);
    }

    // Call on EDT
    private void fireChangedNotification(
            String type, Object source, Object handback) {
        Notification n = new Notification(type, source, 0);
        for (NotificationListener nl : notificationListenersList) {
            nl.handleNotification(n, handback);
        }
    }

    protected abstract MBeanOperationInfo[] updateOperations(MBeanOperationInfo[] operations);
}
