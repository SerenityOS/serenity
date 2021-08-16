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

/*@test
  @bug 6670274
  @summary Incorrect tab titles for JTabbedPane if using HTML (BasicTabbedPanelUI problem)
  @author Alexander Potochkin
  @run main bug6670274
*/

import javax.swing.*;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.text.View;

public class bug6670274 {

    private static void createGui() {
        final JTabbedPane pane = new JTabbedPane();
        TestTabbedPaneUI ui = new TestTabbedPaneUI();
        pane.setUI(ui);

        pane.add("one", new JPanel());
        pane.add("<html><i>Two</i></html>", new JPanel());
        pane.add("three", new JPanel());
        pane.setTitleAt(0, "<html><i>ONE</i></html>");
        check(ui, 0, 1);

        pane.setTitleAt(1, "hello");
        check(ui, 0);

        pane.setTitleAt(0, "<html>html</html>");
        pane.setTitleAt(2, "<html>html</html>");
        check(ui, 0, 2);
    }

    private static void check(TestTabbedPaneUI ui, int... indices) {
        for(int i = 0; i < ui.getTabbedPane().getTabCount(); i++) {
            System.out.print("Checking tab #" + i);
            View view = ui.getTextViewForTab(i);
            boolean found = false;
            for (int j = 0; j < indices.length; j++) {
                if (indices[j]== i) {
                    found = true;
                    break;
                }
            }
            System.out.print("; view = " + view);
            if (found) {
                if (view == null) {
                    throw new RuntimeException("View is unexpectedly null");
                }
            } else if (view != null) {
                throw new RuntimeException("View is unexpectedly not null");
            }
            System.out.println(" ok");
        }
        System.out.println("");
    }


    static class TestTabbedPaneUI extends BasicTabbedPaneUI {
        public View getTextViewForTab(int tabIndex) {
            return super.getTextViewForTab(tabIndex);
        }

        public JTabbedPane getTabbedPane() {
            return tabPane;
        }
    }

    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                bug6670274.createGui();
            }
        });
    }
}
