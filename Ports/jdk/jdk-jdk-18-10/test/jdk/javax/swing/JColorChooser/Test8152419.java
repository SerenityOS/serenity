/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
* @bug 8152419
* @summary To Verify JColorChooser tab selection
* @run main/manual Test8152419
 */

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.CountDownLatch;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

public class Test8152419 {

    public static void main(String args[]) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        JColorChooserTest test = new JColorChooserTest(latch);
        Thread T1 = new Thread(test);
        T1.start();

        // wait for latch to complete
        try {
            latch.await();
        } catch (InterruptedException ie) {
            throw ie;
        }

        if (test.testResult == false) {
            throw new RuntimeException("User Clicked Fail!");
        }
    }
}

class JColorChooserTest implements Runnable {

    private static GridBagLayout layout;
    private static JPanel mainControlPanel;
    private static JPanel resultButtonPanel;
    private static JTextArea instructionTextArea;
    private static JButton passButton;
    private static JButton failButton;
    private static JFrame mainFrame;
    private static JColorChooser colorChooser;
    private final CountDownLatch latch;
    public volatile boolean testResult = false;

    public JColorChooserTest(CountDownLatch latch) throws Exception {
        this.latch = latch;
    }

    @Override
    public void run() {

        try {
            createUI();
        } catch (Exception ex) {
            if (mainFrame != null) {
                mainFrame.dispose();
            }
            latch.countDown();
            throw new RuntimeException("createUI Failed: " + ex.getMessage());
        }

    }

    public final void createUI() throws Exception {

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                mainFrame = new JFrame("JColorChooser Test");
                layout = new GridBagLayout();
                mainControlPanel = new JPanel(layout);
                resultButtonPanel = new JPanel(layout);

                GridBagConstraints gbc = new GridBagConstraints();
                String instructions
                        = "INSTRUCTIONS:"
                        + "\n Select HSV, HSL, RGB or CMYK tab."
                        + "\n If able to shift and view JColorChooser tabs"
                        + "\n then click Pass, else Fail.";

                instructionTextArea = new JTextArea();
                instructionTextArea.setText(instructions);
                instructionTextArea.setEnabled(true);

                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                mainControlPanel.add(instructionTextArea, gbc);

                UIManager.put("FormattedTextField.border",
                        new EmptyBorder(0, 10, 0, 10));
                colorChooser = new JColorChooser(Color.BLUE);
                gbc.gridx = 0;
                gbc.gridy = 1;
                mainControlPanel.add(colorChooser, gbc);

                passButton = new JButton("Pass");
                passButton.setActionCommand("Pass");
                passButton.addActionListener((ActionEvent e) -> {
                    testResult = true;
                    mainFrame.dispose();
                    latch.countDown();

                });
                failButton = new JButton("Fail");
                failButton.setActionCommand("Fail");
                failButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        testResult = false;
                        mainFrame.dispose();
                        latch.countDown();
                    }
                });
                gbc.gridx = 0;
                gbc.gridy = 0;
                resultButtonPanel.add(passButton, gbc);
                gbc.gridx = 1;
                gbc.gridy = 0;
                resultButtonPanel.add(failButton, gbc);

                gbc.gridx = 0;
                gbc.gridy = 2;
                mainControlPanel.add(resultButtonPanel, gbc);

                mainFrame.add(mainControlPanel);
                mainFrame.pack();
                mainFrame.setVisible(true);
            }
        });

    }
}

