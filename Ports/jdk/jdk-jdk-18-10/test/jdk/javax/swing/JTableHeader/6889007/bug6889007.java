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

/*
   @test
  @key headful
   @bug 6889007
   @summary No resize cursor during hovering mouse over JTable
   @author Alexander Potochkin
*/

import javax.swing.*;
import javax.swing.plaf.basic.BasicTableHeaderUI;
import javax.swing.table.JTableHeader;
import java.awt.*;

public class bug6889007 {

    public static void main(String[] args) throws Exception {
        Robot robot = new Robot();
        robot.setAutoDelay(20);

        final JFrame frame = new JFrame();
        frame.setUndecorated(true);

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

                JTableHeader th = new JTableHeader();
                th.setColumnModel(new JTable(20, 5).getColumnModel());

                th.setUI(new MyTableHeaderUI());

                frame.add(th);
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            }
        });
        robot.waitForIdle();
        Point point = frame.getLocationOnScreen();
        int shift = 10;
        int x = point.x;
        int y = point.y + frame.getHeight()/2;
        for(int i = -shift; i < frame.getWidth() + 2*shift; i++) {
            robot.mouseMove(x++, y);
        }
        robot.waitForIdle();
        // 9 is a magic test number
        if (MyTableHeaderUI.getTestValue() != 9) {
            throw new RuntimeException("Unexpected test number "
                    + MyTableHeaderUI.getTestValue());
        }
        System.out.println("ok");
    }

    static class MyTableHeaderUI extends BasicTableHeaderUI {
        private static int testValue;

        protected void rolloverColumnUpdated(int oldColumn, int newColumn) {
            increaseTestValue(newColumn);
            Cursor cursor = Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
            if (oldColumn != -1 && newColumn != -1 &&
                    header.getCursor() != cursor) {
                throw new RuntimeException("Wrong type of cursor!");
            }
        }

        private static synchronized void increaseTestValue(int increment) {
            testValue += increment;
        }

        public static synchronized int getTestValue() {
            return testValue;
        }
    }
}
