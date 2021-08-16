/*
 * Copyright (c) 2017, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 8176072
 * @summary Checks whether reading attributes are obtained for Japanese IME
 * @requires (os.family == "windows")
 * @run main/manual JapaneseReadingAttributes
 */

/**
 * This test requires a manual intervention as the keyboard layout has to be
 * changed to Japanese IME. Once the keyboard layout has been selected, click on
 * Start Test to start the automated tests. Will run two passes, first with an
 * enter key in between to generate the yomigana for the first block of
 * characters. The second without the intermediate enter key. Without the fix,
 * there will be a mismatch in the reading attributes obtained.
 */

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Robot;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.event.KeyEvent;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class JapaneseReadingAttributes {
    private static boolean testPassed = false;
    private static boolean startTest = false;

    private static JFrame frame = null;
    private static JLabel lblTestStatus = null;
    private static JTextField textFieldMain = null;
    private static JTextField textFieldReading = null;
    private static String testResult;
    private static String readingPass1;
    private static String readingPass2;

    private static final CountDownLatch testStartLatch = new CountDownLatch(1);

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            setupUI();
        });

        testStartLatch.await();

        if (startTest) {
            glyphTest();

            frame.dispose();

            if (testPassed) {
                System.out.println(testResult);
            } else {
                throw new RuntimeException(testResult);
            }
        } else {
            throw new RuntimeException("User has not executed the test");
        }
    }

    private static void setupUI() {
        String description = " 1. Go to \"Language Preferences -> Add a Language"
                            + "\" and add \"Japanese\"\n"
                            + " 2. Set current IM to \"Japanese\" and IME option to \"Full width Katakana\" \n"
                            + " 3. Try typing in the text field to ensure"
                            + " that Japanese IME has been successfully"
                            + " selected \n"
                            + " 4. Now click on \"Start Test\" button \n";
        String title = "Reading Attributes test Japanese IME (Windows)";

        frame = new JFrame(title);

        JPanel mainPanel = new JPanel(new BorderLayout());

        JPanel textEditPanel = new JPanel(new FlowLayout());

        textFieldMain = new JTextField(20);

        textFieldReading = new JTextField(20);
        textFieldReading.setEditable(false);

        textEditPanel.add(textFieldMain);
        textEditPanel.add(textFieldReading);

        mainPanel.add(textEditPanel, BorderLayout.CENTER);

        JTextArea textArea = new JTextArea(description);
        textArea.setEditable(false);
        final JButton btnStartTest = new JButton("Start Test");
        final JButton btnCancelTest = new JButton("Cancel Test");

        btnStartTest.addActionListener((e) -> {
            btnStartTest.setEnabled(false);
            btnCancelTest.setEnabled(false);
            startTest = true;
            testStartLatch.countDown();
        });

        btnCancelTest.addActionListener((e) -> {
            frame.dispose();
            testStartLatch.countDown();
        });
        mainPanel.add(textArea, BorderLayout.NORTH);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(btnStartTest);
        buttonPanel.add(btnCancelTest);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        lblTestStatus = new JLabel("");
        lblTestStatus.setMinimumSize(new Dimension(250, 20));
        lblTestStatus.setPreferredSize(new Dimension(250, 20));
        lblTestStatus.setVisible(true);
        textEditPanel.add(lblTestStatus);

        frame.add(mainPanel);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocationRelativeTo(null);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                testStartLatch.countDown();
            }
            @Override
            public void windowOpened( WindowEvent e ){
                textFieldMain.requestFocusInWindow();
            }
        });

        textFieldMain.addInputMethodListener(new InputMethodListener() {
            @Override
            public void caretPositionChanged(InputMethodEvent event) {
            }

            @Override
            public void inputMethodTextChanged(InputMethodEvent event) {
                AttributedCharacterIterator itr = event.getText();
                if (itr != null) {
                    int toCopy = event.getCommittedCharacterCount();
                    if (toCopy > 0) {
                        itr.first();
                        StringBuilder yomigana = new StringBuilder(
                                textFieldReading.getText());
                        while (toCopy-- > 0) {
                            if (itr.getIndex() == itr.getRunStart(
                                    AttributedCharacterIterator.Attribute.READING)) {
                                java.text.Annotation annotatedText
                                        = (java.text.Annotation) itr.
                                                getAttribute(AttributedCharacterIterator.Attribute.READING);
                                yomigana.append(annotatedText.getValue());
                            }
                            itr.next();
                        }
                        textFieldReading.setText(yomigana.toString());
                    }
                }
            }
        });

        frame.setVisible(true);
    }

    private static void glyphTest() throws Exception {
        Robot robotKeySimulator = new Robot();
        performTasks(robotKeySimulator);
    }

    public static void performTasks(Robot robotForKeyInput) throws Exception {
        lblTestStatus.setText("Running Tests..");
        robotForKeyInput.setAutoDelay(500);

        ArrayList<Integer> keyCodesToUse = new ArrayList<Integer>();

        keyCodesToUse.add(KeyEvent.VK_A);
        keyCodesToUse.add(KeyEvent.VK_B);
        keyCodesToUse.add(KeyEvent.VK_E);
        keyCodesToUse.add(KeyEvent.VK_SPACE);
        keyCodesToUse.add(KeyEvent.VK_SPACE);
        keyCodesToUse.add(KeyEvent.VK_ENTER);
        keyCodesToUse.add(KeyEvent.VK_S);
        keyCodesToUse.add(KeyEvent.VK_I);
        keyCodesToUse.add(KeyEvent.VK_N);
        keyCodesToUse.add(KeyEvent.VK_Z);
        keyCodesToUse.add(KeyEvent.VK_O);
        keyCodesToUse.add(KeyEvent.VK_U);
        keyCodesToUse.add(KeyEvent.VK_SPACE);
        keyCodesToUse.add(KeyEvent.VK_ENTER);

        textFieldMain.requestFocusInWindow();

        robotForKeyInput.waitForIdle();

        enterInput(robotForKeyInput, keyCodesToUse);

        SwingUtilities.invokeAndWait(() -> {
            readingPass1 = textFieldReading.getText();
        });

        if (setTaskStatus(readingPass1, 1)) {
            keyCodesToUse.remove((Integer) KeyEvent.VK_ENTER);

            enterInput(robotForKeyInput, keyCodesToUse);

            SwingUtilities.invokeAndWait(() -> {
                readingPass2 = textFieldReading.getText();
            });

            if (setTaskStatus(readingPass2, 2)) {
                if (readingPass1.equals(readingPass2)) {
                    testPassed = true;
                    testResult = "Test Passed : Same reading attribute "
                            + "obtained from both passes ";
                    lblTestStatus.setText(testResult);
                } else {
                    testResult = "Test Failed : Reading attribute from Pass 1 <"
                            + readingPass1 + "> != Reading attribute "
                            + "from Pass 2 <" + readingPass2 + ">";
                }
            }
        }
    }

    private static void enterInput(Robot robotKeyInput,
            ArrayList<Integer> keyInputs) {
        textFieldReading.setText("");
        textFieldMain.setText("");

        String strKeyInput = "KeyPress=>";
        int nOfKeyInputs = keyInputs.size();
        for (int i = 0; i < nOfKeyInputs; i++) {
            int keyToUse = keyInputs.get(i);
            robotKeyInput.keyPress(keyToUse);
            robotKeyInput.keyRelease(keyToUse);
            strKeyInput += (Integer.toHexString(keyToUse)) + ":";
        }

        System.out.println(strKeyInput);
    }

    public static boolean setTaskStatus(String readingValue, int passCount) {
        boolean status = false;

        if (!readingValue.isEmpty()) {
            testResult = "Attribute : " + readingValue
                    + "read from pass " + Integer.toString(passCount);
            status = true;
        } else {
            testResult = "Failed to read Reading attribute from pass  "
                    + Integer.toString(passCount);
        }

        lblTestStatus.setText(testResult);

        return status;
    }
}
