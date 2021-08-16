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
 * @bug 8264287
 * @summary Test implementation of NSAccessibilityComboBox protocol peer
 * @author Artem.Semenov@jetbrains.com
 * @run main/manual AccessibleJComboboxTest
 * @requires (os.family == "windows" | os.family == "mac")
 */

import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.FlowLayout;
import java.util.concurrent.CountDownLatch;

public class AccessibleJComboboxTest extends AccessibleComponentTest {

    @java.lang.Override
    public CountDownLatch createCountDownLatch() {
        return new CountDownLatch(1);
    }

    void createCombobox() {
        INSTRUCTIONS = "INSTRUCTIONS:\n"
                + "Check a11y of JCombobox.\n\n"
                + "Turn screen reader on, and Tab to the combobox.\n\n"
                + "If you can hear combobox selected item tab further and press PASS, otherwise press FAIL.";

        JPanel frame = new JPanel();

        String[] NAMES = {"One", "Two", "Three", "Four", "Five"};
        JComboBox<String> combo = new JComboBox<>(NAMES);

        JLabel label = new JLabel("This is combobox:");
        label.setLabelFor(combo);

        frame.setLayout(new FlowLayout());
        frame.add(label);
        frame.add(combo);
        exceptionString = "AccessibleJCombobox test failed!";
        super.createUI(frame, "AccessibleJComboboxTest");
    }

    public static void main(String[] args) throws Exception {
        AccessibleJComboboxTest test = new AccessibleJComboboxTest();

        countDownLatch = test.createCountDownLatch();
        SwingUtilities.invokeLater(test::createCombobox);
        countDownLatch.await();

        if (!testResult) {
            throw new RuntimeException(a11yTest.exceptionString);
        }
    }
}
