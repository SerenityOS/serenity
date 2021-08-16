/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) 2021, JetBrains s.r.o.. All rights reserved.
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
 * @bug 8262031
 * @summary Test implementation of NSAccessibilityNavigableStaticTest and NSAccessibilityStaticText protocols peer
 * @author Artem.Semenov@jetbrains.com
 * @run main/manual AccessibleTextTest
 * @requires (os.family == "windows" | os.family == "mac")
 */

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;

import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyAdapter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AccessibleTextTest extends AccessibleComponentTest {
    @Override
    public CountDownLatch createCountDownLatch() {
        return new CountDownLatch(1);
    }

    private void createSimpleLabel() {
        AccessibleComponentTest.INSTRUCTIONS = "INSTRUCTIONS:\n"
                + "Check a11y of JLabel.\n\n"
                + "Turn screen reader on.\n"
                + "On MacOS, use the VO navigation keys to read the label text;\n"
                + "ON Windows with JAWS, use window virtualization (insert+alt+w and arrows) to read the label text;\n"
                + "ON Windows with NVDA, use the browse cursor (insert+num4 or insert+num6) to read the label text;\n\n"
                + "If you can hear text from label tab further and press PASS, otherwise press FAIL.";

        JLabel label = new JLabel("this is a label");

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        panel.add(label);
        exceptionString = "Simple label test failed!";
        super.createUI(panel, "AccessibleTextTest");
    }

    private void createOneLineTexField() {
        AccessibleComponentTest.INSTRUCTIONS = "INSTRUCTIONS:\n"
                + "Check a11y of JTextField.\n\n"
                + "Turn screen reader on and press Tab to move to the text field and type some characters.\n\n"
                + "If you can hear input results according to your screen reader settings, tab further and press PASS, otherwise press FAIL.";

        JTextField textField = new JTextField("some text to edit");

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        panel.add(textField);
        exceptionString = "Simple text field test failed!";
        super.createUI(panel, "AccessibleTextTest");
    }

    private void createPassField() {
        AccessibleComponentTest.INSTRUCTIONS = "INSTRUCTIONS:\n"
                + "Check a11y of JPasswordField.\n\n"
                + "Turn screen reader on and press Tab to move to the password field and type some characters.\n\n"
                + "If you can hear  sounds specific to your screen reader when interacting with password fields, tab further and press PASS, otherwise press FAIL.";

        JPasswordField passwordField = new JPasswordField("12345678");

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        panel.add(passwordField);
        exceptionString = "Simple passfield field test failed!";
        super.createUI(panel, "AccessibleTextTest");
    }

    private void createNamedTextField() {
        AccessibleComponentTest.INSTRUCTIONS = "INSTRUCTIONS:\n"
                + "Check a11y of named JTextField.\n\n"
                + "Turn screen reader on and press Tab to move to the text field.\n\n"
                + "If you can hear in addition to the fact that this text field is also the names of these fields, tab further and press PASS, otherwise press FAIL.";

        JTextField textField = new JTextField("some text 1");
        textField.getAccessibleContext().setAccessibleName("This is the first text field:");

        JLabel label = new JLabel("This is the second text field:");
        JTextField secondTextField = new JTextField("some text 2");
        label.setLabelFor(secondTextField);

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        panel.add(textField);
        panel.add(label);
        panel.add(secondTextField);
        exceptionString = "Named text field test failed!";
        super.createUI(panel, "AccessibleTextTest");
    }

    private void createTextArea() {
        AccessibleComponentTest.INSTRUCTIONS = "INSTRUCTIONS:\n"
                + "Check a11y of JTextArea.\n\n"
                + "Turn screen reader on and press the arrow keys.\n\n"
                + "If you can hear this instructions, tab further and press PASS, otherwise press FAIL.";

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        exceptionString = "Simple text area test failed!";
        super.createUI(panel, "AccessibleTextTest");
    }

    private void createEditableTextArea() {
        AccessibleComponentTest.INSTRUCTIONS = "INSTRUCTIONS:\n"
                + "Check a11y of editable JTextArea.\n\n"
                + "Turn screen reader on and press Tab to move to the text area and type some characters.\n\n"
                + "If you can hear input results according to your screen reader settings, tab further and press PASS, otherwise press FAIL.";

        JTextArea textArea = new JTextArea("some text to edit");
        JLabel label = new JLabel(textArea.getText().length() + " chars");
        label.setLabelFor(textArea);
        textArea.setEditable(true);
        textArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                label.setText(String.valueOf(textArea.getText().length()) + " chars");
            }
        });

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        panel.add(textArea);
        panel.add(label);
        exceptionString = "Editable text area test failed!";
        super.createUI(panel, "AccessibleTextTest");
    }

    private void createTextPane() {
        AccessibleComponentTest.INSTRUCTIONS = "INSTRUCTIONS:\n"
                + "Check a11y of text in JTextPane.\n\n"
                + "Turn screen reader on and press Tab to move to the text pane and press the arrow keys.\n\n"
                + "If you can hear text, tab further and press PASS, otherwise press FAIL.";

        String str = "Line 1\nLine 2\nLine 3";
        JTextPane textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setText(str);
        JTextArea textArea = new JTextArea();

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        panel.add(textPane);
        exceptionString = "Simple text pane test failed!";
        super.createUI(panel, "AccessibleTextTest");
    }

    private void createHTMLTextPane() {
        AccessibleComponentTest.INSTRUCTIONS = "INSTRUCTIONS:\n"
                + "Check a11y of html text in JTextPane.\n\n"
                + "Turn screen reader on and press Tab to move to the text pane and press the arrow keys.\n\n"
                + "If you can hear text, tab further and press PASS, otherwise press FAIL.";

        String str = "<html><h1>Header</h1><ul><li>Item 1</li><li>Item 2</li><li>Item 3</li></ul></html>";
        JTextPane textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setContentType("text/html");
        textPane.setText(str);
        JTextArea textArea = new JTextArea();

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        panel.add(textPane);
        exceptionString = "HTML text pane test failed!";
        super.createUI(panel, "AccessibleTextTest");
    }

    private void createEditableTextPane() {
        AccessibleComponentTest.INSTRUCTIONS = "INSTRUCTIONS:\n"
                + "Check a11y of editable JTextPane.\n\n"
                + "Turn screen reader on and press Tab to move to the text pane and type some characters.\n\n"
                + "If you can hear input results according to your screen reader settings, tab further and press PASS, otherwise press FAIL.";

        JTextPane textPane = new JTextPane();
        textPane.setText("some text to edit");
        JLabel label = new JLabel(textPane.getText().length() + " chars");
        label.setLabelFor(textPane);
        textPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                label.setText(String.valueOf(textPane.getText().length()) + " chars");
            }
        });

        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        panel.add(textPane);
        panel.add(label);
        exceptionString = "Editable text pane test failed!";
        super.createUI(panel, "AccessibleTextTest");
    }

    public static void main(String[] args) throws Exception {
        AccessibleTextTest test = new AccessibleTextTest();

        countDownLatch = test.createCountDownLatch();
        SwingUtilities.invokeAndWait(test::createSimpleLabel);
        AccessibleComponentTest.countDownLatch.await(15, TimeUnit.MINUTES);
        if (!testResult) {
            throw new RuntimeException(exceptionString);
        }

        countDownLatch = test.createCountDownLatch();
        SwingUtilities.invokeAndWait(test::createOneLineTexField);
        AccessibleComponentTest.countDownLatch.await(15, TimeUnit.MINUTES);
        if (!testResult) {
            throw new RuntimeException(exceptionString);
        }

        countDownLatch = test.createCountDownLatch();
        SwingUtilities.invokeAndWait(test::createPassField);
        AccessibleComponentTest.countDownLatch.await(15, TimeUnit.MINUTES);
        if (!testResult) {
            throw new RuntimeException(exceptionString);
        }

        countDownLatch = test.createCountDownLatch();
        SwingUtilities.invokeAndWait(test::createNamedTextField);
        AccessibleComponentTest.countDownLatch.await(15, TimeUnit.MINUTES);
        if (!testResult) {
            throw new RuntimeException(exceptionString);
        }

        countDownLatch = test.createCountDownLatch();
        SwingUtilities.invokeAndWait(test::createTextArea);
        AccessibleComponentTest.countDownLatch.await(15, TimeUnit.MINUTES);
        if (!testResult) {
            throw new RuntimeException(exceptionString);
        }

        countDownLatch = test.createCountDownLatch();
        SwingUtilities.invokeAndWait(test::createEditableTextArea);
        AccessibleComponentTest.countDownLatch.await(15, TimeUnit.MINUTES);
        if (!testResult) {
            throw new RuntimeException(exceptionString);
        }

        countDownLatch = test.createCountDownLatch();
        SwingUtilities.invokeAndWait(test::createTextPane);
        AccessibleComponentTest.countDownLatch.await(15, TimeUnit.MINUTES);
        if (!testResult) {
            throw new RuntimeException(exceptionString);
        }

        countDownLatch = test.createCountDownLatch();
        SwingUtilities.invokeAndWait(test::createHTMLTextPane);
        AccessibleComponentTest.countDownLatch.await(15, TimeUnit.MINUTES);
        if (!testResult) {
            throw new RuntimeException(exceptionString);
        }

        countDownLatch = test.createCountDownLatch();
        SwingUtilities.invokeAndWait(test::createEditableTextPane);
        AccessibleComponentTest.countDownLatch.await(15, TimeUnit.MINUTES);
        if (!testResult) {
            throw new RuntimeException(exceptionString);
        }
    }
}
