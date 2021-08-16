/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
 *
 */


package com.sun.java.swing.ui;

import com.sun.java.swing.action.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import javax.swing.*;

// Referenced classes of package com.sun.java.swing.ui:
//            CommonUI

public class TabsDlg extends JDialog
{
    private class ApplyListener
        implements ActionListener
    {

        public void actionPerformed(ActionEvent evt)
        {
            if(applyListener != null)
            {
                applyListener.actionPerformed(evt);
                enableApplyButton(false);
            }
        }

        private ApplyListener()
        {
        }

    }

    private class CancelListener
        implements ActionListener
    {

        public void actionPerformed(ActionEvent evt)
        {
            if(cancelListener != null)
                cancelListener.actionPerformed(evt);
            setVisible(false);
        }

        private CancelListener()
        {
        }

    }

    private class OkListener
        implements ActionListener
    {

        public void actionPerformed(ActionEvent evt)
        {
            if(okListener != null)
                okListener.actionPerformed(evt);
            setVisible(false);
        }

        private OkListener()
        {
        }

    }


    public TabsDlg(String title, Vector panels)
    {
        super(new JFrame(), title, true);
        okListener = null;
        cancelListener = null;
        applyListener = null;
        Container pane = getContentPane();
        pane.setLayout(new BorderLayout());
        tabsPanel = new JTabbedPane();
        int numPanels = panels.size();
        for(int i = 0; i < numPanels; i++)
        {
            JPanel panel = (JPanel)panels.elementAt(i);
            tabsPanel.addTab(panel.getName(), panel);
        }

        pane.add(tabsPanel, "Center");
        pane.add(createButtonPanel(), "South");
        pack();
        CommonUI.centerComponent(this);
    }

    public static void main(String args[])
    {
        JPanel p1 = new JPanel();
        p1.add(new JButton("One"));
        p1.setName("One");
        JPanel p2 = new JPanel();
        p2.add(new JButton("Two"));
        p2.setName("Two");
        JPanel p3 = new JPanel();
        p3.add(new JButton("Three"));
        p3.setName("Three");
        JPanel p4 = new JPanel();
        p4.add(new JButton("Four"));
        p4.setName("Four");
        Vector<JPanel> panels = new Vector<>();
        panels.addElement(p1);
        panels.addElement(p2);
        panels.addElement(p3);
        panels.addElement(p4);
        tabsDlg = new TabsDlg("Test Dialog", panels);
        tabsDlg.addOkListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt)
            {
                System.exit(0);
            }

        }
);
        tabsDlg.addCancelListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt)
            {
                System.exit(0);
            }

        }
);
        tabsDlg.setVisible(true);
    }

    private JPanel createButtonPanel()
    {
        JPanel panel = new JPanel();
        okAction = new OkAction();
        cancelAction = new CancelAction();
        applyAction = new ApplyAction();
        okAction.addActionListener(new OkListener());
        cancelAction.addActionListener(new CancelListener());
        applyAction.addActionListener(new ApplyListener());
        panel.add(CommonUI.createButton(okAction));
        panel.add(CommonUI.createButton(cancelAction));
        panel.add(CommonUI.createButton(applyAction));
        JPanel p2 = new JPanel(new BorderLayout());
        p2.add(panel, "Center");
        p2.add(new JSeparator(), "North");
        return p2;
    }

    public void enableApplyButton(boolean enabled)
    {
        applyAction.setEnabled(enabled);
    }

    public synchronized void addOkListener(ActionListener l)
    {
        okListener = AWTEventMulticaster.add(okListener, l);
    }

    public synchronized void removeOkListener(ActionListener l)
    {
        okListener = AWTEventMulticaster.remove(okListener, l);
    }

    public synchronized void addCancelListener(ActionListener l)
    {
        cancelListener = AWTEventMulticaster.add(cancelListener, l);
    }

    public synchronized void removeCancelListener(ActionListener l)
    {
        cancelListener = AWTEventMulticaster.remove(cancelListener, l);
    }

    public synchronized void addApplyListener(ActionListener l)
    {
        applyListener = AWTEventMulticaster.add(applyListener, l);
    }

    public synchronized void removeApplyListener(ActionListener l)
    {
        applyListener = AWTEventMulticaster.remove(applyListener, l);
    }

    private JTabbedPane tabsPanel;
    private DelegateAction okAction;
    private DelegateAction cancelAction;
    private DelegateAction applyAction;
    private ActionListener okListener;
    private ActionListener cancelListener;
    private ActionListener applyListener;
    private static TabsDlg tabsDlg;



}
