/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
/*
  @test
  @bug 4193219
  @summary
  @author Your Name: Hania Gajewska area=swing
  @run main/manual IconCoord
*/

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

public class IconCoord {
    static Test test = new Test();

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                new IconCoord().createAndShowGUI();
            }
        });
        test.waitTestResult();
    }

    private void createAndShowGUI() {
        StringBuilder instrText = new StringBuilder();
        instrText.append("First, iconify internal frame \"Frame 1\" by clicking on its iconify button.\n");
        instrText.append("Now, maximize the top-level window \"IconCoord\".\n");
        instrText.append("The \"Frame 1\" icon should stay in the lower left corner of the desktop; ");
        instrText.append("if it doesn't, press \"Fail\".\n");
        instrText.append("Now move the icon to the middle of the desktop by dragging it by its ");
        instrText.append("bumpy left side. Then iconify \"Frame 2\" by clicking on its iconify button.\n");
        instrText.append("If the icon for frame two gets placed in the lower left corner of the ");
        instrText.append("desktop (where the icon for \"Frame 1\" used to be before you moved it), ");
        instrText.append("press \"Pass\". Otherwise, press \"Fail\".\n");

        JDesktopPane dt = new JDesktopPane();

        JButton tf;
        JInternalFrame if1 = new JInternalFrame("Frame 1", false, false, false, true);
        JComponent c = (JComponent) if1.getContentPane();
        c.setLayout(new BorderLayout());

        tf = new JButton ("ignore");
        c.add (tf, BorderLayout.NORTH);

        tf = new JButton ("ignore");
        c.add (tf, BorderLayout.CENTER);

        JInternalFrame if2 = new JInternalFrame("Frame 2", false, false, false, true);
        c = (JComponent) if2.getContentPane();
        c.setLayout(new BorderLayout());

        tf = new JButton ("ignore");
        c.add (tf, BorderLayout.NORTH);

        tf = new JButton ("ignore");
        c.add (tf, BorderLayout.CENTER);

        if1.pack();
        if1.setBounds(300, 0, 300, 80);
        if2.pack();
        if2.setBounds(0, 0, 300, 80);
        dt.add(if1);
        dt.add(if2);

        if1.setVisible(true);
        if2.setVisible(true);

        int frameHeight = 500;

        JScrollPane dtScrollPane = new JScrollPane(dt);
        JFrame frame = test.createTestFrame("IconCoord", dtScrollPane, instrText.toString(), 250);
        dt.setPreferredSize(new Dimension(650, frameHeight - 250));
        frame.setSize (600,500);
        frame.setVisible(true);
    }

    static class Test {
        private boolean pass;
        JFrame createTestFrame(String name, Component topComponent, String instructions, int instrHeight) {
            final String PASS = "Pass";
            final String FAIL = "Fail";
            JFrame frame = new JFrame(name);
            frame.setLayout(new BorderLayout());

            JPanel testButtonsPanel = new JPanel();
            testButtonsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

            ActionListener btnAL = new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    switch (event.getActionCommand()) {
                        case PASS:
                            pass();
                            break;
                        default:
                            throw new RuntimeException("Test failed.");
                    }
                }
            };
            JButton passBtn = new JButton(PASS);
            passBtn.addActionListener(btnAL);
            passBtn.setActionCommand(PASS);

            JButton failBtn = new JButton(FAIL);
            failBtn.addActionListener(btnAL);
            failBtn.setActionCommand(FAIL);

            testButtonsPanel.add(BorderLayout.WEST, passBtn);
            testButtonsPanel.add(BorderLayout.EAST, failBtn);

            JTextArea instrText = new JTextArea();
            instrText.setLineWrap(true);
            instrText.setEditable(false);
            JScrollPane instrScrollPane = new JScrollPane(instrText);
            instrScrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, instrHeight));
            instrText.append(instructions);

            JPanel servicePanel = new JPanel();
            servicePanel.setLayout(new BorderLayout());
            servicePanel.add(BorderLayout.CENTER, instrScrollPane);
            servicePanel.add(BorderLayout.SOUTH, testButtonsPanel);

            frame.add(BorderLayout.SOUTH, servicePanel);
            frame.add(BorderLayout.CENTER, topComponent);
            return frame;
        }
        synchronized void pass() {
            pass = true;
            notifyAll();
        }
        synchronized void waitTestResult() throws InterruptedException {
            while (!pass) {
                wait();
            }
        }
    }
}
