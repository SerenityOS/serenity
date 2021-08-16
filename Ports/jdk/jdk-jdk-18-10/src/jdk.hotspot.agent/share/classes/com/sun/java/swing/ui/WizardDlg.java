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

public class WizardDlg extends JDialog
{
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

    private class FinishListener
        implements ActionListener
    {

        public void actionPerformed(ActionEvent evt)
        {
            if(finishListener != null)
                finishListener.actionPerformed(evt);
            setVisible(false);
        }

        private FinishListener()
        {
        }

    }

    private class NextListener
        implements ActionListener
    {

        public void actionPerformed(ActionEvent evt)
        {
            cardShowing++;
            if(cardShowing > numCards)
                cardShowing = numCards;
            else
                panesLayout.next(panesPanel);
            if(nextListener != null)
                nextListener.actionPerformed(evt);
            enableBackNextButtons();
        }

        private NextListener()
        {
        }

    }

    private class BackListener
        implements ActionListener
    {

        public void actionPerformed(ActionEvent evt)
        {
            cardShowing--;
            if(cardShowing < 1)
                cardShowing = 1;
            else
                panesLayout.previous(panesPanel);
            if(backListener != null)
                backListener.actionPerformed(evt);
            enableBackNextButtons();
        }

        private BackListener()
        {
        }

    }


    public WizardDlg(JFrame frame, String title, Vector panels, Vector images)
    {
        super(frame, title, true);
        this.title = title;
        this.images = images;
        Container pane = getContentPane();
        pane.setLayout(new BorderLayout());
        panesLayout = new CardLayout();
        panesPanel = new JPanel(panesLayout);
        pane.add(panesPanel, "Center");
        pane.add(createButtonPanel(), "South");
        setPanels(panels);
        pack();
        CommonUI.centerComponent(this);
    }

    public WizardDlg(JFrame frame, String title, Vector panels)
    {
        this(frame, title, panels, null);
    }

    public WizardDlg(String title, Vector panels)
    {
        this(new JFrame(), title, panels, null);
    }

    public void setPanels(Vector panels)
    {
        numCards = panels.size();
        cardShowing = 1;
        this.panels = panels;
        panesPanel.removeAll();
        for(int i = 0; i < numCards; i++)
            panesPanel.add((JPanel)panels.elementAt(i), (Integer.valueOf(i)).toString());

        validate();
        enableBackNextButtons();
    }

    public void reset()
    {
        cardShowing = 1;
        panesLayout.first(panesPanel);
        enableBackNextButtons();
    }

    public void setWestPanel(JPanel panel)
    {
        Container pane = getContentPane();
        pane.add(panel, "West");
    }

    public static void main(String args[])
    {
        JPanel p1 = new JPanel();
        p1.add(new JButton("One"));
        JPanel p2 = new JPanel();
        p2.add(new JButton("Two"));
        JPanel p3 = new JPanel();
        p3.add(new JButton("Three"));
        JPanel p4 = new JPanel();
        p4.add(new JButton("Four"));
        Vector<JPanel> panels = new Vector<>();
        panels.addElement(p1);
        panels.addElement(p2);
        panels.addElement(p3);
        panels.addElement(p4);
        wizardDlg = new WizardDlg("Test Dialog", panels);
        wizardDlg.addFinishListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt)
            {
                System.exit(0);
            }

        }
);
        wizardDlg.addCancelListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt)
            {
                System.exit(0);
            }

        }
);
        wizardDlg.setVisible(true);
    }

    private JPanel createButtonPanel()
    {
        JPanel panel = new JPanel();
        backAction = new BackAction();
        nextAction = new NextAction();
        finishAction = new FinishAction();
        cancelAction = new CancelAction();
        backAction.setEnabled(false);
        finishAction.setEnabled(false);
        backAction.addActionListener(new BackListener());
        nextAction.addActionListener(new NextListener());
        finishAction.addActionListener(new FinishListener());
        cancelAction.addActionListener(new CancelListener());
        panel.add(CommonUI.createButton(backAction));
        panel.add(CommonUI.createButton(nextAction));
        panel.add(CommonUI.createButton(finishAction));
        panel.add(CommonUI.createButton(cancelAction));
        JPanel p2 = new JPanel(new BorderLayout());
        p2.add(panel, "Center");
        p2.add(new JSeparator(), "North");
        return p2;
    }

    private void enableBackNextButtons()
    {
        if(cardShowing == 1)
        {
            backAction.setEnabled(false);
            finishAction.setEnabled(false);
            if(numCards > 1)
            {
                nextAction.setEnabled(true);
            } else
            {
                finishAction.setEnabled(true);
                nextAction.setEnabled(false);
            }
        } else
        if(cardShowing == numCards)
        {
            nextAction.setEnabled(false);
            finishAction.setEnabled(true);
            if(numCards > 1)
                backAction.setEnabled(true);
            else
                backAction.setEnabled(false);
        } else
        {
            backAction.setEnabled(true);
            nextAction.setEnabled(true);
            finishAction.setEnabled(false);
        }
        setTitle();
    }

    private void setTitle()
    {
        JPanel panel = (JPanel)panels.elementAt(cardShowing - 1);
        String newTitle = title;
        String panelTitle = panel.getName();
        if(panelTitle != null && panelTitle.equals(""))
        {
            newTitle = newTitle + " - ";
            newTitle = newTitle + panelTitle;
        }
        super.setTitle(newTitle);
    }

    public synchronized void addFinishListener(ActionListener l)
    {
        finishListener = AWTEventMulticaster.add(finishListener, l);
    }

    public synchronized void removeFinishListener(ActionListener l)
    {
        finishListener = AWTEventMulticaster.remove(finishListener, l);
    }

    public synchronized void addCancelListener(ActionListener l)
    {
        cancelListener = AWTEventMulticaster.add(cancelListener, l);
    }

    public synchronized void removeCancelListener(ActionListener l)
    {
        cancelListener = AWTEventMulticaster.remove(cancelListener, l);
    }

    public synchronized void addNextListener(ActionListener l)
    {
        nextListener = AWTEventMulticaster.add(nextListener, l);
    }

    public synchronized void removeNextListener(ActionListener l)
    {
        nextListener = AWTEventMulticaster.remove(nextListener, l);
    }

    public synchronized void addBackListener(ActionListener l)
    {
        backListener = AWTEventMulticaster.add(backListener, l);
    }

    public synchronized void removeBackListener(ActionListener l)
    {
        backListener = AWTEventMulticaster.remove(backListener, l);
    }

    private CardLayout panesLayout;
    private JPanel panesPanel;
    private DelegateAction backAction;
    private DelegateAction nextAction;
    private DelegateAction finishAction;
    private DelegateAction cancelAction;
    private ActionListener finishListener;
    private ActionListener cancelListener;
    private ActionListener nextListener;
    private ActionListener backListener;
    private int numCards;
    private int cardShowing;
    private String title;
    private Vector panels;
    private Vector images;
    private static WizardDlg wizardDlg;




}
