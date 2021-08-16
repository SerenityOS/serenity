/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
   @bug 6963870
   @key headful
   @summary Tests that GTKPainter.ListTableFocusBorder.getBorderInsets()
            doesn't return null
   @author Peter Zhelezniakov
   @run main Test6963870
*/

import java.awt.Insets;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;

public class Test6963870 implements Runnable {

    final static String[] UI_NAMES = {
        "List.focusCellHighlightBorder",
        "List.focusSelectedCellHighlightBorder",
        "List.noFocusBorder",
        "Table.focusCellHighlightBorder",
        "Table.focusSelectedCellHighlightBorder",
    };

    public void run() {
        for (String uiName: UI_NAMES) {
            test(uiName);
        }
    }

    void test(String uiName) {
        Border b = UIManager.getBorder(uiName);
        Insets i = b.getBorderInsets(null);
        if (i == null) {
            throw new RuntimeException("getBorderInsets() returns null for " + uiName);
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.gtk.GTKLookAndFeel");
        } catch (Exception e) {
            System.out.println("GTKLookAndFeel cannot be set, skipping this test");
            return;
        }

        SwingUtilities.invokeAndWait(new Test6963870());
    }
}

