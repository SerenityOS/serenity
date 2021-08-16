/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
   @bug 4251301
   @summary Keybinding for show/hide the system menu.
   @author Andrey Pikalev
   @library /test/lib
   @build jdk.test.lib.Platform
   @run main/manual bug4251301
*/

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.*;

import jdk.test.lib.Platform;

public class bug4251301 {
    static Test test = new Test();
    public static void main(String[] args) throws Exception {
        if (Platform.isOSX()) {
            System.out.println("This test is not applicable for MacOS. Passed.");
            return;
        }
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
        Robot robot = new Robot();
        robot.waitForIdle();
        test.waitTestResult();
    }

    public static void createAndShowGUI() {
        final StringBuilder instructions = new StringBuilder();
        instructions.append("Click with your mouse the content area of the internal frame with the title \"IFrame\" ");
        instructions.append("and press Ctrl+Space. \n");
        instructions.append("If the system menu shows up, press Esc. Then system menu should hide. \n");
        instructions.append("If you success then press \"Pass\", else press \"Fail\".\n");

        JDesktopPane dp = new JDesktopPane();
        JInternalFrame jif = new JInternalFrame("IFrame",true,true,true,true);
        dp.add(jif);
        jif.setBounds(20, 20, 220, 100);
        jif.setVisible(true);
        try {
            jif.setSelected(true);
        } catch(PropertyVetoException pve) {
            pve.printStackTrace();
            throw new Error("Occures PropertyVetoException while set selection...");
        }
        JScrollPane dtScrollPane = new JScrollPane(dp);
        JFrame testFrame = test.createTestFrame("Instructions", dtScrollPane, instructions.toString(), 500);
        testFrame.setSize(500, 400);
        testFrame.setVisible(true);
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
