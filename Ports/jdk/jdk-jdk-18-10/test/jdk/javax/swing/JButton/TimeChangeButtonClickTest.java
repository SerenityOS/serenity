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
 * @bug 7096375
 * @summary  Test that Swing does not ignore first click on a JButton after
 * decreasing system's time
 * @run main/manual TimeChangeButtonClickTest
 */
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.concurrent.CountDownLatch;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.JButton;
import javax.swing.JFrame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.TimeUnit;
import javax.swing.Box;
import javax.swing.JLabel;

public class TimeChangeButtonClickTest {

    public static void main(String args[]) throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        TestUI test = new TestUI(latch);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    test.createUI();
                } catch (Exception ex) {
                    throw new RuntimeException("Exception while creating test UI");
                }
            }
        });

        boolean status = latch.await(5, TimeUnit.MINUTES);

        if (!status) {
            System.out.println("Test timed out.");
        }

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                try {
                    test.disposeUI();
                } catch (Exception ex) {
                    throw new RuntimeException("Exception while disposing test UI");
                }
            }
        });

        if (test.testResult == false) {
            throw new RuntimeException("Test Failed.");
        }
    }
}

class TestUI {

    private static JFrame mainFrame;
    private static JPanel mainControlPanel;

    private static JTextArea instructionTextArea;

    private static JPanel resultButtonPanel;
    private static JButton passButton;
    private static JButton failButton;

    private static JPanel testPanel;
    private static JButton testButton;
    private static JLabel buttonPressCountLabel;

    private static GridBagLayout layout;
    private final CountDownLatch latch;
    public boolean testResult = false;
    private int buttonPressCount = 0;

    public TestUI(CountDownLatch latch) throws Exception {
        this.latch = latch;
    }

    public final void createUI() throws Exception {

        mainFrame = new JFrame("TimeChangeButtonClickTest");

        layout = new GridBagLayout();
        mainControlPanel = new JPanel(layout);
        resultButtonPanel = new JPanel(layout);
        testPanel = new JPanel(layout);

        GridBagConstraints gbc = new GridBagConstraints();

        // Create Test instructions
        String instructions
                = "Test 1 : --------------------\n"
                + "1. Click 'Test Button' with left mouse button\n"
                + "Observe : Button Press count change to 1\n"
                + "Test 2 : --------------------\n"
                + "1. Change the system time to one hour less than current time\n"
                + "2. Click 'Test Button' with left mouse button\n"
                + "Observe : Button Press count change to 2\n"
                + "Test 3 : --------------------\n"
                + "1. Change the system time by adding two hours\n"
                + "2. Click 'Test Button' with left mouse button\n"
                + "Observe : Button Press count change to 3\n"
                + "--------------------\n"
                + "Restore the system time.\n"
                + "--------------------\n"
                + "Press 'Pass' if Button Press count is 3, 'Fail' otherwise";

        instructionTextArea = new JTextArea();
        instructionTextArea.setText(instructions);
        instructionTextArea.setEditable(false);
        instructionTextArea.setBorder(BorderFactory.
                createTitledBorder("Test Instructions"));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainControlPanel.add(instructionTextArea, gbc);

        // Create Test Button and label
        testButton = new JButton("Test Button");
        testButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                buttonPressCount++;
                buttonPressCountLabel.setText(
                        "Button Press Count : " + buttonPressCount);
            }
        });

        gbc.gridx = 0;
        gbc.gridy = 0;
        testPanel.add(testButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        testPanel.add(Box.createVerticalStrut(50));

        gbc.gridx = 0;
        gbc.gridy = 2;
        buttonPressCountLabel = new JLabel(
                "Button Press Count : " + buttonPressCount);
        testPanel.add(buttonPressCountLabel, gbc);

        mainControlPanel.add(testPanel);

        // Create resultButtonPanel with Pass, Fail buttons
        passButton = new JButton("Pass");
        passButton.setActionCommand("Pass");
        passButton.addActionListener((ActionEvent e) -> {
            System.out.println("Pass Button pressed!");
            testResult = true;
            latch.countDown();

        });

        failButton = new JButton("Fail");
        failButton.setActionCommand("Fail");
        failButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("Fail Button pressed!");
                testResult = false;
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

    public void disposeUI() {
        mainFrame.setVisible(false);
        mainFrame.dispose();
    }
}

