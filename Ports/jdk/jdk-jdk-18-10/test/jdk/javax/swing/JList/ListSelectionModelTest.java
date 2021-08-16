/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8191436
 * @summary Tests JListSelectionModel setSelection functionality
 */

import javax.swing.JList;
import javax.swing.DefaultListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager.LookAndFeelInfo;

import static javax.swing.UIManager.getInstalledLookAndFeels;
import static javax.swing.UIManager.setLookAndFeel;

import java.util.Arrays;

public class ListSelectionModelTest {

    public static void main(String[] args) throws Exception {

        final LookAndFeelInfo[] lookAndFeelInfoArray =
                getInstalledLookAndFeels();

        for (LookAndFeelInfo lookAndFeelInfo : lookAndFeelInfoArray) {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    try {
                        CreateGUIAndTest(lookAndFeelInfo.getClassName());
                    } catch (Exception e) {
                        e.printStackTrace();
                        throw new RuntimeException("Exception while running test");
                    }
                }
            });
        }
    }

    static void CreateGUIAndTest(String lookAndFeel) throws Exception{
        setLookAndFeel(lookAndFeel);
        DefaultListModel listModel = new DefaultListModel();
        for (int j = 0; j < 10; j++) {
            listModel.add(j, "Item: " + j);
        }

        JList list = new JList(listModel);

        ListSelectionModel selectionModel = list.getSelectionModel();
        selectionModel.setSelectionMode(
                ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        selectionModel.setSelectionInterval(0, 1);
        checkSelection(list, new int[]{0, 1}, lookAndFeel);

        selectionModel.setSelectionMode(
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        selectionModel.setSelectionInterval(0, 2);
        checkSelection(list, new int[]{0, 1, 2}, lookAndFeel);

        selectionModel.addSelectionInterval(5, 7);
        checkSelection(list, new int[]{0, 1, 2, 5, 6, 7}, lookAndFeel);

        selectionModel
                .setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        checkSelection(list, new int[]{0, 1, 2}, lookAndFeel);

        selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        checkSelection(list, new int[]{0}, lookAndFeel);

        selectionModel
                .setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        selectionModel.addSelectionInterval(4, 5);
        checkSelection(list, new int[]{4, 5}, lookAndFeel);

        selectionModel.addSelectionInterval(0, 2);
        checkSelection(list, new int[]{0, 1, 2}, lookAndFeel);

        selectionModel.setSelectionMode(
                ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        selectionModel.addSelectionInterval(6, 7);
        checkSelection(list, new int[]{0, 1, 2, 6, 7}, lookAndFeel);

        System.out.println("Test passed for " + lookAndFeel);
    }

    static void checkSelection(JList list, int[] selectionArray,
                               String lookAndFeel) throws RuntimeException {
        int[] listSelection = list.getSelectedIndices();
        if (listSelection.length != selectionArray.length) {
            System.out.println("Expected: " + Arrays.toString(selectionArray));
            System.out.println("Actual: " + Arrays.toString(listSelection));
            throw new RuntimeException("Wrong selection for " + lookAndFeel);
        }

        Arrays.sort(listSelection);
        Arrays.sort(selectionArray);

        if (!Arrays.equals(listSelection, selectionArray)) {
            System.out.println("Expected: " + Arrays.toString(selectionArray));
            System.out.println("Actual: " + Arrays.toString(listSelection));
            throw new RuntimeException("Wrong selection for " + lookAndFeel);
        }
    }
}
