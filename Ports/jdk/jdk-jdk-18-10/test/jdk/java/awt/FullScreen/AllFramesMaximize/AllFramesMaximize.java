/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8190767
 * @key headful
 * @requires os.family == "mac"
 * @summary If JFrame is maximized on OS X, all new JFrames will be maximized by default
 * @compile AllFramesMaximize.java
 * @run main/manual AllFramesMaximize
 */

import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AllFramesMaximize {
    private static JButton passButton;
    private static JButton failButton;
    private static JTextArea instructions;
    private static JFrame mainFrame;
    private static JFrame instructionFrame;
    public static boolean isProgInterruption = false;
    static Thread mainThread = null;
    static int sleepTime = 300000;

    public static void createAndShowJFrame() {
        passButton = new JButton("Pass");
        passButton.setEnabled(true);

        failButton = new JButton("Fail");
        failButton.setEnabled(true);

        instructions = new JTextArea(8, 30);
        instructions.setText(" This is a manual test\n\n" +
                " 1) Click on the maximize button, JFrame will enter fullscreen\n" +
                " 2) Click anywhere on the JFrame\n" +
                " 3) Press Pass if new JFrame didn't open in fullscreen,\n" +
                " 4) Press Fail if new JFrame opened in fullscreen");

        instructionFrame = new JFrame("Test Instructions");
        instructionFrame.setLocationRelativeTo(null);
        instructionFrame.add(passButton);
        instructionFrame.add(failButton);
        instructionFrame.add(instructions);
        instructionFrame.setSize(200,200);
        instructionFrame.setLayout(new FlowLayout());
        instructionFrame.pack();
        instructionFrame.setVisible(true);

        passButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                dispose();
                isProgInterruption = true;
                mainThread.interrupt();
            }
        });

        failButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                dispose();
                isProgInterruption = true;
                mainThread.interrupt();
                throw new RuntimeException("New JFrame opened on a new window!");
            }
        });

        mainFrame = new JFrame();
        JButton button = new JButton("Open Frame");
        mainFrame.getContentPane().add(button);
        button.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        JFrame f = new JFrame();
                        f.setSize(400, 400);
                        f.setVisible(true);
                    }
                });
        mainFrame.setSize(500, 500);
        mainFrame.setVisible(true);
    }

    private static void dispose() {
        mainFrame.dispose();
        instructionFrame.dispose();
    }

    public static void main(String[] args) throws Exception {
        mainThread = Thread.currentThread();
        SwingUtilities.invokeAndWait(AllFramesMaximize::createAndShowJFrame);

        try {
            mainThread.sleep(sleepTime);
        } catch (InterruptedException e) {
        if (!isProgInterruption) {
            throw e;
        }
        } finally {
            SwingUtilities.invokeAndWait(AllFramesMaximize::dispose);
        }

        if (!isProgInterruption) {
        throw new RuntimeException("Timed out after " + sleepTime / 1000
                + " seconds");
        }
    }
}

