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

import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
/*
 * @test
 * @bug 8017112
 * @summary JTabbedPane components have inconsistent accessibility tree
 * @run main AccessibleIndexInParentTest
 */

public class AccessibleIndexInParentTest {

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(AccessibleIndexInParentTest::test);
    }

    private static void test() {

        int N = 5;
        JTabbedPane tabbedPane = new JTabbedPane();

        for (int i = 0; i < N; i++) {
            tabbedPane.addTab("Title: " + i, new JLabel("Component: " + i));
        }

        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component child = tabbedPane.getComponentAt(i);

            AccessibleContext ac = child.getAccessibleContext();
            if (ac == null) {
                throw new RuntimeException("Accessible Context is null!");
            }

            int index = ac.getAccessibleIndexInParent();
            Accessible parent = ac.getAccessibleParent();

            if (parent.getAccessibleContext().getAccessibleChild(index) != child) {
                throw new RuntimeException("Wrong getAccessibleIndexInParent!");
            }
        }
    }
}