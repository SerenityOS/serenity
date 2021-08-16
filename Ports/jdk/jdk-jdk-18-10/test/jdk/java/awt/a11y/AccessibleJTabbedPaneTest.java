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
 * @bug 8264303
 * @summary Test implementation of NSAccessibilityTabPanel protocol peer
 * @author Artem.Semenov@jetbrains.com
 * @run main/manual AccessibleJTabbedPaneTest
 * @requires (os.family == "windows" | os.family == "mac")
 */

import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import javax.swing.SwingUtilities;

import java.util.concurrent.CountDownLatch;

public class AccessibleJTabbedPaneTest extends AccessibleComponentTest {

    @Override
    public CountDownLatch createCountDownLatch() {
        return new CountDownLatch(1);
    }

    void createTabPane() {
        INSTRUCTIONS = "INSTRUCTIONS:\n"
                + "Check a11y of JTabbedPane.\n\n"
                + "Turn screen reader on, and tab to the JTabbedPane.\n"
                + "Use page up and page down arrow buttons to move through the tabs.\n\n"
                + "If you can hear selected tab names tab further and press PASS, otherwise press FAIL.\n";

        JTabbedPane tabbedPane = new JTabbedPane();

        JPanel panel1 = new JPanel();
        String[] names = {"One", "Two", "Three", "Four", "Five"};
        JList list = new JList(names);
        JLabel fieldName = new JLabel("Text field:");
        JTextField textField = new JTextField("some text");
        fieldName.setLabelFor(textField);
        panel1.add(fieldName);
        panel1.add(textField);
        panel1.add(list);
        tabbedPane.addTab("Tab 1", panel1);
        JPanel panel2 = new JPanel();
        for (int i = 0; i < 5; i++) {
            panel2.add(new JCheckBox("CheckBox " + String.valueOf(i + 1)));
        }
        tabbedPane.addTab("tab 2", panel2);
        JPanel panel = new JPanel();
        panel.add(tabbedPane);

        exceptionString = "AccessibleJTabbedPane test failed!";
        createUI(panel, "AccessibleJTabbedPaneTest");
    }

    public static void main(String[] args) throws Exception {
        AccessibleJTabbedPaneTest test = new AccessibleJTabbedPaneTest();

        countDownLatch = test.createCountDownLatch();
        SwingUtilities.invokeLater(test::createTabPane);
        countDownLatch.await();

        if (!testResult) {
            throw new RuntimeException(a11yTest.exceptionString);
        }
    }
}
