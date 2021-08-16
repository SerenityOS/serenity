/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 8256019
 * @summary Verifies if JLabel HTML text support translucent text colors
 * @run main/manual TestTranslucentLabelText
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.concurrent.CountDownLatch;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JTextArea;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

public class TestTranslucentLabelText {
    private static Color background = new Color(0, 150, 0);
    private static Color foreground = new Color(255, 255, 255, 120);
    private static Font font = new Font("Sans Serif", Font.PLAIN, 24);
    private static JFrame frame;
    static boolean testResult;
    static CountDownLatch latch;
    private static Thread mainThread;
    private static boolean testPassed;
    private static boolean testGeneratedInterrupt;

    private static void doTest(Runnable action) {
        String description
                = " A frame with 2 labels will be shown in middle of screen.\n"
                + " Left side label text should be opaque.\n "
                + " Right side label text should be translucent.\n"
                + " If Right side label text is translucent, press PASS else press FAIL";

        final JDialog dialog = new JDialog();
        dialog.setTitle("JLabelTranslucentTest");
        JTextArea textArea = new JTextArea(description);
        textArea.setEditable(false);
        final JButton testButton = new JButton("Start Test");
        final JButton passButton = new JButton("PASS");
        passButton.setEnabled(false);
        passButton.addActionListener((e) -> {
            dialog.dispose();
            frame.dispose();
            pass();
        });
        final JButton failButton = new JButton("FAIL");
        failButton.setEnabled(false);
        failButton.addActionListener((e) -> {
            dialog.dispose();
            frame.dispose();
            fail();
        });
        testButton.addActionListener((e) -> {
            testButton.setEnabled(false);
            action.run();
            passButton.setEnabled(true);
            failButton.setEnabled(true);
        });
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(textArea, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(testButton);
        buttonPanel.add(passButton);
        buttonPanel.add(failButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        dialog.add(mainPanel);
        dialog.pack();
        dialog.setVisible(true);
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("main dialog closing");
                testGeneratedInterrupt = false;
                frame.dispose();
                mainThread.interrupt();
            }
        });
    }

    public static synchronized void pass() {
        testPassed = true;
        testGeneratedInterrupt = true;
        mainThread.interrupt();
    }

    public static synchronized void fail() {
        testPassed = false;
        testGeneratedInterrupt = true;
        mainThread.interrupt();
    }


    private static JLabel create(String text)
    {
        JLabel label = new JLabel(text);
        label.setOpaque(true);
        label.setBackground(background);
        label.setForeground(foreground);
        label.setFont(font);
        label.setPreferredSize(new Dimension(200, 40));
        frame.add(label);

        return label;
    }

    private static void runTest() {
        frame = new JFrame();
        frame.setUndecorated(true);
        frame.setLayout(new FlowLayout());

        //JLabel l1 = create("Test1");
        //JLabel opqLabel = create("<html>Test2</html>");
        JLabel opqLabel = create("<html><p style=\"color:rgba(255, 0, 0, 1.00)\">TestLabel</p></html>");
        JLabel tranLabel = create("<html><p style=\"color:rgba(255, 0, 0, 0.5)\">TestLabel</p></html>");

        frame.pack();

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.toFront();
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            doTest(TestTranslucentLabelText::runTest);
        });
        mainThread = Thread.currentThread();
        try {
            Thread.sleep(180000);
        } catch (InterruptedException e) {
            if (!testPassed && testGeneratedInterrupt) {
                throw new RuntimeException("" +
                   "Label HTML text does not support translucent text colors");
            }
        }
        if (!testGeneratedInterrupt) {
            throw new RuntimeException("user has not executed the test");
        }

    }


}

