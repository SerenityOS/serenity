/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8154069
 * @summary Jaws reads wrong values from comboboxes when no element is selected
 * @run main Bug8154069
 */

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleSelection;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;

public class Bug8154069 {

    private static JFrame frame;
    private static volatile Exception exception = null;

    public static void main(String args[]) throws Exception {
        try {
            try {
                UIManager.setLookAndFeel(new NimbusLookAndFeel());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            SwingUtilities.invokeAndWait(() -> {
                frame = new JFrame();
                String[] petStrings = { "Bird", "Cat" };
                JComboBox<String> cb = new JComboBox<>(petStrings);
                cb.setSelectedIndex(1);  // select Cat
                frame.add(cb);
                frame.pack();
                try {
                    cb.setSelectedIndex(-1);
                    int i = cb.getSelectedIndex();
                    if (i != -1) {
                        throw new RuntimeException("getSelectedIndex is not -1");
                    }
                    Object o = cb.getSelectedItem();
                    if (o != null) {
                        throw new RuntimeException("getSelectedItem is not null");
                    }
                    AccessibleContext ac = cb.getAccessibleContext();
                    AccessibleSelection as = ac.getAccessibleSelection();
                    int count = as.getAccessibleSelectionCount();
                    if (count != 0) {
                        throw new RuntimeException("getAccessibleSelection count is not 0");
                    }
                    Accessible a = as.getAccessibleSelection(0);
                    if (a != null) {
                        throw new RuntimeException("getAccessibleSelection(0) is not null");
                    }
                } catch (Exception e) {
                    exception = e;
                }
            });
            if (exception != null) {
                System.out.println("Test failed: " + exception.getMessage());
                throw exception;
            } else {
                System.out.println("Test passed.");
            }
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (frame != null) { frame.dispose(); }
            });
        }
    }

}
