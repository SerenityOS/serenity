/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 7027139
   @summary getFirstIndex() does not return the first index that has changed
   @author Pavel Porvatov
*/

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

public class bug7027139 {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                JTable orderTable = new JTable(new String[][]{
                        {"Item 1 1", "Item 1 2"},
                        {"Item 2 1", "Item 2 2"},
                        {"Item 3 1", "Item 3 2"},
                        {"Item 4 1", "Item 4 2"},
                },
                        new String[]{"Col 1", "Col 2"});

                ListSelectionModel selectionModel = orderTable.getSelectionModel();
                selectionModel.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
                selectionModel.addListSelectionListener(new ListSelectionListener() {
                    public void valueChanged(ListSelectionEvent e) {
                        if (e.getValueIsAdjusting()) {
                            return;
                        }

                        if (e.getFirstIndex() < 0) {
                            throw new RuntimeException("Test bug7027139 failed");
                        }
                    }
                });

                orderTable.selectAll();
            }
        });

        System.out.println("Test bug7027139 passed");
    }
}
