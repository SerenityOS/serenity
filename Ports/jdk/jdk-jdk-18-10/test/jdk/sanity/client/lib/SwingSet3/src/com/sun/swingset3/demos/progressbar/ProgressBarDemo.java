/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
 */
package com.sun.swingset3.demos.progressbar;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.SoftBevelBorder;

import com.sun.swingset3.DemoProperties;
import com.sun.swingset3.demos.ResourceManager;

/**
 * JProgressBar Demo
 *
 * @version 1.12 11/17/05
 * @author Jeff Dinkins # @author Peter Korn (accessibility support)
 */
@DemoProperties(
        value = "ProgressBar Demo",
        category = "Controls",
        description = "Demonstrates the JProgressBar, a control which displays progress to the user",
        sourceFiles = {
            "com/sun/swingset3/demos/progressbar/ProgressBarDemo.java",
            "com/sun/swingset3/demos/ResourceManager.java",
            "com/sun/swingset3/demos/progressbar/resources/ProgressBarDemo.properties",
            "com/sun/swingset3/demos/progressbar/resources/images/ProgressBarDemo.gif"
        }
)
public class ProgressBarDemo extends JPanel {

    private static final ResourceManager resourceManager = new ResourceManager(ProgressBarDemo.class);
    public static final String STOP_BUTTON = resourceManager.getString("ProgressBarDemo.stop_button");
    public static final String START_BUTTON = resourceManager.getString("ProgressBarDemo.start_button");
    public static final String DEMO_TITLE = ProgressBarDemo.class.getAnnotation(DemoProperties.class).value();

    /**
     * main method allows us to run as a standalone demo.
     *
     * @param args
     */
    public static void main(String[] args) {
        JFrame frame = new JFrame(DEMO_TITLE);

        frame.getContentPane().add(new ProgressBarDemo());
        frame.setPreferredSize(new Dimension(800, 600));
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    /**
     * ProgressBarDemo Constructor
     */
    public ProgressBarDemo() {
        createProgressPanel();
    }

    private final javax.swing.Timer timer = new javax.swing.Timer(18, createTextLoadAction());
    private Action loadAction;
    private Action stopAction;
    private JProgressBar progressBar;
    private JTextArea progressTextArea;

    private void createProgressPanel() {
        setLayout(new BorderLayout());

        JPanel textWrapper = new JPanel(new BorderLayout());
        textWrapper.setBorder(new SoftBevelBorder(BevelBorder.LOWERED));
        textWrapper.setAlignmentX(LEFT_ALIGNMENT);
        progressTextArea = new MyTextArea();

        progressTextArea.getAccessibleContext().setAccessibleName(
                resourceManager.getString("ProgressBarDemo.accessible_text_area_name"));
        progressTextArea.getAccessibleContext().setAccessibleDescription(
                resourceManager.getString("ProgressBarDemo.accessible_text_area_description"));
        textWrapper.add(new JScrollPane(progressTextArea), BorderLayout.CENTER);

        add(textWrapper, BorderLayout.CENTER);

        JPanel progressPanel = new JPanel();
        add(progressPanel, BorderLayout.SOUTH);

        progressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, text.length()) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(300, super.getPreferredSize().height);
            }
        };
        progressBar.getAccessibleContext().setAccessibleName(
                resourceManager.getString("ProgressBarDemo.accessible_text_loading_progress"));

        progressPanel.add(progressBar);
        progressPanel.add(createLoadButton());
        progressPanel.add(createStopButton());
    }

    private JButton createLoadButton() {
        loadAction = new AbstractAction(START_BUTTON) {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadAction.setEnabled(false);
                stopAction.setEnabled(true);
                if (progressBar.getValue() == progressBar.getMaximum()) {
                    progressBar.setValue(0);
                    textLocation = 0;
                    progressTextArea.setText("");
                }
                timer.start();
            }
        };
        return createButton(loadAction);
    }

    private JButton createStopButton() {
        stopAction = new AbstractAction(STOP_BUTTON) {
            @Override
            public void actionPerformed(ActionEvent e) {
                timer.stop();
                loadAction.setEnabled(true);
                stopAction.setEnabled(false);
            }
        };
        return createButton(stopAction);
    }

    private static JButton createButton(Action a) {
        JButton b = new JButton();
        // setting the following client property informs the button to show
        // the action text as it's name. The default is to not show the
        // action text.
        b.putClientProperty("displayActionText", Boolean.TRUE);
        b.setAction(a);
        return b;
    }

    private int textLocation = 0;

    private final String text = resourceManager.getString("ProgressBarDemo.text");

    private Action createTextLoadAction() {
        return new AbstractAction("text load action") {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (progressBar.getValue() < progressBar.getMaximum()) {
                    progressBar.setValue(progressBar.getValue() + 1);
                    progressTextArea.append(text.substring(textLocation, textLocation + 1));
                    textLocation++;
                } else {
                    timer.stop();
                    loadAction.setEnabled(true);
                    stopAction.setEnabled(false);
                }
            }
        };
    }

    private static class MyTextArea extends JTextArea {

        private MyTextArea() {
            super(null, 0, 0);
            setEditable(false);
            setText("");
        }

        @Override
        public float getAlignmentX() {
            return LEFT_ALIGNMENT;
        }

        @Override
        public float getAlignmentY() {
            return TOP_ALIGNMENT;
        }
    }
}
