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
/* @test
   @bug 4514858 4164779
   @summary F6, F8 Ctrl-TAB and Ctrl-Shift-TAB in JSplitPane
   @author Andrey Pikalev
   @run main/manual bug4514858
*/

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;


public class bug4514858  implements ActionListener {

    static String intructions = "Test the F6, F8, Ctrl-TAB and Ctrl-Shift-TAB keybinding functionality in JSplitPane\n" +
            "with different LookAndFeels (switch LookAndFeel with the buttoms at the bottom of the\n" +
            "frame \"Test\"):\n\n" +
            "1. Move focus to the button \"Button 1\" in the frame \"Test\". Then press F6 several times.\n" +
            "The focus should cycle between five buttons in order from 1 to 5.\n\n" +
            "2. Move focus to the button \"Button 2\" in the frame \"Test\". Then press F8 three times.\n" +
            "The splitters of the splitpanes should be highlited in order:\n" +
            "\"JSplitPane 3\", \"JSplitPane 2\", \"JSplitPane 1\".\n\n" +
            "3. Move focus to the button \"Button 2\" in the frame \"Test\". Press Ctrl-TAB.\n" +
            "The focus should go to the \"Button 4\". Then press Ctrl-TAB again.\n" +
            "The focus should go to the first enabled button at the bottom of frame.\n\n" +
            "4. Move focus to the button \"Button 4\" in the frame \"Test\". Press Ctrl-Shift-TAB three times.\n" +
            "The focus should go through the button \"Button 3\", then \"Button 1\", then to the last\n" +
            "enabled button at the bottom of frame.";
    static Test test = new Test();
    JFrame fr;
    public static void main(String[] argv) throws Exception {
        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                new bug4514858().createAndShowGUI();
            }
        });
        test.waitTestResult();
    }
    public void createAndShowGUI() {
        fr = new JFrame("Test");

        //-------------------------------------------------------------
        JButton left2 = new JButton("Button 1");

        JButton left3 = new JButton("Button 2");
        JButton right3 = new JButton("Button 3");

        JSplitPane right2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, left3, right3);
        right2.setBorder(new TitledBorder("JSplitPane 3"));

        JSplitPane left1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left2, right2);
        left1.setBorder(new TitledBorder("JSplitPane 2"));

        JButton left4 = new JButton("Button 4");
        JButton right4 = new JButton("Button 5");

        JSplitPane right1 = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left4, right4);
        right1.setBorder(new TitledBorder("JSplitPane 4"));

        JSplitPane sp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, left1, right1);
        sp.setBorder(new TitledBorder("JSplitPane 1"));
        fr.getContentPane().add(sp);

        //-------------------------------------------------------------
        JPanel p = new JPanel();

        JButton metal = new JButton("Metal");
        metal.setActionCommand("Metal");
        metal.setEnabled(isSupportedLAF("javax.swing.plaf.metal.MetalLookAndFeel"));
        metal.addActionListener(this);
        p.add(metal);

        JButton motif = new JButton("Motif");
        motif.setActionCommand("Motif");
        motif.setEnabled(isSupportedLAF("com.sun.java.swing.plaf.motif.MotifLookAndFeel"));
        motif.addActionListener(this);
        p.add(motif);

        JButton windows = new JButton("Windows");
        windows.setActionCommand("Windows");
        windows.setEnabled(isSupportedLAF("com.sun.java.swing.plaf.windows.WindowsLookAndFeel"));
        windows.addActionListener(this);
        p.add(windows);

        fr.getContentPane().add(p, BorderLayout.SOUTH);

        fr.pack();
        fr.setVisible(true);

        JFrame instrFrame = test.createTestFrame("bug4514858 instructions", null, intructions, 250);
        instrFrame.setBounds(fr.getWidth() + 50, fr.getHeight(), 600, 400);
        instrFrame.setVisible(true);
    }

    private boolean isSupportedLAF(String str) {
        try {
            Class c = Class.forName(str);
            LookAndFeel laf = (LookAndFeel)c.newInstance();
            return laf.isSupportedLookAndFeel();
        } catch (Exception e) {
            return false;
        }
    }

    public void actionPerformed(ActionEvent e) {
        String s = e.getActionCommand();
        if (s.equals("Metal")) {
            s = "javax.swing.plaf.metal.MetalLookAndFeel";
        } else if (s.equals("Motif")) {
            s = "com.sun.java.swing.plaf.motif.MotifLookAndFeel";
        } else {
            s = "com.sun.java.swing.plaf.windows.WindowsLookAndFeel";
        }
        try {
            UIManager.setLookAndFeel(s);
            SwingUtilities.updateComponentTreeUI(fr);
            fr.pack();
        } catch(Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
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
            if (topComponent == null) {
                frame.add(BorderLayout.CENTER, instrScrollPane);
            } else {
                servicePanel.add(BorderLayout.CENTER, instrScrollPane);
                frame.add(BorderLayout.CENTER, topComponent);
            }
            servicePanel.add(BorderLayout.SOUTH, testButtonsPanel);

            frame.add(BorderLayout.SOUTH, servicePanel);
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
