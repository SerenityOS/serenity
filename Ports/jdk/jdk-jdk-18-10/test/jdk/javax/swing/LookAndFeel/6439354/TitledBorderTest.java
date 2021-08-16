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
 * @bug 8153056 8152647 6439354
 * @summary Verify TitleBorder appearance Color/Visibility for WLAF
 * @requires (os.family == "windows")
 * @run main/manual TitledBorderTest
 */
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.CountDownLatch;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class TitledBorderTest {

    public static void main(String args[]) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        TitledBorder test = new TitledBorder(latch);
        Thread T1 = new Thread(test);
        T1.start();

        // wait for latch to complete
        latch.await();

        if (test.testResult == false) {
            throw new RuntimeException("User Clicked Fail!"
                    + " TitledBorder Not Valid");
        }
    }
}

class TitledBorder implements Runnable {

    private static GridBagLayout layout;
    private static JPanel mainControlPanel;
    private static JPanel resultButtonPanel;
    private static JTextArea instructionTextArea;
    private static JButton passButton;
    private static JButton failButton;
    private static JFrame mainFrame;
    private final CountDownLatch latch;
    public boolean testResult = false;

    public TitledBorder(CountDownLatch latch) throws Exception {
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

        UIManager.setLookAndFeel("com.sun.java.swing.plaf."
                + "windows.WindowsLookAndFeel");
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                mainFrame = new JFrame("Window LAF TitledBorder Test");
                layout = new GridBagLayout();
                mainControlPanel = new JPanel(layout);
                resultButtonPanel = new JPanel(layout);

                GridBagConstraints gbc = new GridBagConstraints();
                String instructions
                        = "INSTRUCTIONS:"
                        + "\n set Windows Theme to HighContrast#1."
                        + "\n (ControlPanel->Personalization->High Contrast#1)"
                        + "\n If Titled Border(Border Line) is visible then test"
                        + " passes else failed.";

                instructionTextArea = new JTextArea();
                instructionTextArea.setText(instructions);
                instructionTextArea.setEnabled(false);
                instructionTextArea.setDisabledTextColor(Color.black);
                instructionTextArea.setBackground(Color.white);

                gbc.gridx = 0;
                gbc.gridy = 0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                mainControlPanel.add(instructionTextArea, gbc);

                mainControlPanel.setBorder(BorderFactory.
                        createTitledBorder("Titled Border"));

                passButton = new JButton("Pass");
                passButton.setActionCommand("Pass");
                passButton.addActionListener((ActionEvent e) -> {
                    System.out.println("Pass Button pressed!");
                    testResult = true;
                    mainFrame.dispose();
                    latch.countDown();

                });
                failButton = new JButton("Fail");
                failButton.setActionCommand("Fail");
                failButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        System.out.println("Fail Button pressed!");
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
                gbc.gridy = 1;
                mainControlPanel.add(resultButtonPanel, gbc);

                mainFrame.add(mainControlPanel);
                mainFrame.pack();
                mainFrame.setVisible(true);
            }
        });

    }
}
