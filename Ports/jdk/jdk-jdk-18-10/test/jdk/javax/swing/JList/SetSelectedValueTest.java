/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5076761
 * @summary Verifies that the selection is cleared when setSelectedValue is
 *          called with null
 * @run main SetSelectedValueTest
 */

import javax.swing.SwingUtilities;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import java.util.Collections;
import java.util.List;

public class SetSelectedValueTest {
    public static void main(String[] args) throws Exception {

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                // Create a JList with 2 elements
                DefaultListModel dlm = new DefaultListModel();
                JList list = new JList<String>(dlm);
                list.setSelectionMode(
                        ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                dlm.addElement("1");
                dlm.addElement("2");

                // Select both the elements added in list
                list.setSelectionInterval(0, 1);
                checkSelectionByList(list, List.of("1", "2"));

                // Set the selected value as null. This should clear the
                // selection
                list.setSelectedValue(null, true);
                checkSelectionByList(list, Collections.emptyList());

                // Select both the elements added in list
                list.setSelectionInterval(0, 1);
                checkSelectionByList(list, List.of("1", "2"));
            }
        });
    }

    static void checkSelectionByList(JList list, List<String> selectionList)
            throws RuntimeException {
        List<String> listSelection = list.getSelectedValuesList();
        if (!listSelection.equals(selectionList)) {
            System.out.println("Expected: " + selectionList);
            System.out.println("Actual: " + listSelection);
            throw new RuntimeException("Wrong selection");
        }
    }
}
