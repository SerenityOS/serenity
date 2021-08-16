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
 * @bug 7108280
 * @summary Verifies that getSelectedValue works fine without crash when
 *          the setSelectionInterval was called with indices outside the
 *          range of data present in DataModel
 * @run main GetSelectedValueTest
 */

import javax.swing.SwingUtilities;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import java.util.Objects;

public class GetSelectedValueTest {
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

                // Set the selection interval from 0-2 (3 elements instead
                // of 2). The getSelectedValue should return the first
                // selected element
                list.setSelectionInterval(0, 2);
                checkSelectedIndex(list, "1");

                //here the smallest selection index is bigger than number of
                // elements in list. This should return null.
                list.setSelectionInterval(4, 5);
                checkSelectedIndex(list,null);
            }
        });
    }

    static void checkSelectedIndex(JList list, Object value)
            throws RuntimeException {
        Object selectedObject = list.getSelectedValue();
        if (!Objects.equals(value, selectedObject)) {
            System.out.println("Expected: " + value);
            System.out.println("Actual: " + selectedObject);
            throw new RuntimeException("Wrong selection");
        }
    }
}
