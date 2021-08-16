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
 * @bug 8267388
 * @summary Test implementation of NSAccessibilityTable protocol peer
 * @author Artem.Semenov@jetbrains.com
 * @run main/manual AccessibleJTableTest
 * @requires (os.family == "windows" | os.family == "mac")
 */

import javax.swing.JTable;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

import java.awt.FlowLayout;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AccessibleJTableTest extends AccessibleComponentTest {
    private static final String[] columnNames = {"One", "Two", "Three"};
    private static final String[][] data = {
            {"One1", "Two1", "Three1"},
            {"One2", "Two2", "Three2"},
            {"One3", "Two3", "Three3"},
            {"One4", "Two4", "Three4"},
            {"One5", "Two5", "Three5"}
    };

    @Override
    public CountDownLatch createCountDownLatch() {
        return new CountDownLatch(1);
    }

    public void  createUI() {
        INSTRUCTIONS = "INSTRUCTIONS:\n"
                + "Check a11y of JTable.\n\n"
                + "Turn screen reader on, and Tab to the table.\n"
                + "On Windows press the arrow buttons to move through the table.\n\n"
                + "On MacOS, use the up and down arrow buttons to move through rows, and VoiceOver fast navigation to move through columns.\n\n"
                + "If you can hear table cells ctrl+tab further and press PASS, otherwise press FAIL.\n";

        JTable table = new JTable(data, columnNames);
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        JScrollPane scrollPane = new JScrollPane(table);
        panel.add(scrollPane);
        panel.setFocusable(false);
        exceptionString = "AccessibleJTable test failed!";
        super.createUI(panel, "AccessibleJTableTest");
    }

    public void  createUINamed() {
        INSTRUCTIONS = "INSTRUCTIONS:\n"
                + "Check a11y of named JTable.\n\n"
                + "Turn screen reader on, and Tab to the table.\n"
                + "Press the ctrl+tab button to move to second table.\n\n"
                + "If you can hear second table name: \"second table\" - ctrl+tab further and press PASS, otherwise press FAIL.\n";

        JTable table = new JTable(data, columnNames);
        JTable secondTable = new JTable(data, columnNames);
        secondTable.getAccessibleContext().setAccessibleName("Second table");
        JPanel panel = new JPanel();
        panel.setLayout(new FlowLayout());
        JScrollPane scrollPane = new JScrollPane(table);
        JScrollPane secondScrollPane = new JScrollPane(secondTable);
        panel.add(scrollPane);
        panel.add(secondScrollPane);
        panel.setFocusable(false);
        exceptionString = "AccessibleJTable test failed!";
        super.createUI(panel, "AccessibleJTableTest");
    }

    public static void main(String[] args) throws Exception {
        AccessibleJTableTest test = new AccessibleJTableTest();

        countDownLatch = test.createCountDownLatch();
        SwingUtilities.invokeAndWait(test::createUI);
        countDownLatch.await(15, TimeUnit.MINUTES);
        if (!testResult) {
            throw new RuntimeException(exceptionString);
        }

        countDownLatch = test.createCountDownLatch();
        SwingUtilities.invokeAndWait(test::createUINamed);
        countDownLatch.await(15, TimeUnit.MINUTES);
        if (!testResult) {
            throw new RuntimeException(exceptionString);
        }
    }
}
