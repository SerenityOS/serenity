/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8041725
 * @summary JList selection colors are not UIResource instances in Nimbus L&F
 * @author Anton Litvinov
 */

import java.awt.*;
import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.nimbus.*;

public class bug8041725 {
    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(new NimbusLookAndFeel());
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                JFrame frame = new JFrame("bug8041725");
                frame.setSize(200, 200);
                JList list = new JList(new String[]{"Item1", "Item2", "Item3"});
                frame.getContentPane().add(list);
                frame.pack();
                frame.setVisible(true);

                System.err.println("Test #1: No items are selected, list is enabled.");
                testSelectionColors(list);

                System.err.println("Test #2: No items are selected, list is disabled.");
                list.setEnabled(false);
                testSelectionColors(list);

                System.err.println("Test #3: One item is selected, list is disabled.");
                list.setSelectedIndex(0);
                testSelectionColors(list);

                System.err.println("Test #4: One item is selected, list is enabled.");
                list.setEnabled(true);
                testSelectionColors(list);

                frame.dispose();
            }
        });
    }

    private static void testSelectionColors(JList list) {
        Color selBackColor = list.getSelectionBackground();
        if (!(selBackColor instanceof UIResource)) {
            throw new RuntimeException(String.format(
                "JList.getSelectionBackground() returned instance of '%s' instead of UIResource.",
                selBackColor.getClass()));
        }
        Color selForeColor = list.getSelectionForeground();
        if (!(selForeColor instanceof UIResource)) {
            throw new RuntimeException(String.format(
                "JList.getSelectionForeground() returned instance of '%s' instead of UIResource.",
                selForeColor.getClass()));
        }
    }
}
