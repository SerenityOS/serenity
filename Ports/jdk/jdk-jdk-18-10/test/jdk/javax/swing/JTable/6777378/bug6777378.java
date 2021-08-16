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
   @bug 6777378
   @summary NullPointerException in XPDefaultRenderer.paint()
   @author Alexander Potochkin
   @run main bug6777378
*/

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.event.MouseEvent;
import java.awt.event.InputEvent;
import java.awt.*;

public class bug6777378 {
    private static JFrame frame;
    private static JTableHeader header;

    public static void main(String[] args) throws Exception {
        try {
            Robot robot = new Robot();
            robot.setAutoDelay(20);
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        UIManager.setLookAndFeel(new MetalLookAndFeel());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    JTable table = new JTable(new AbstractTableModel() {
                        public int getRowCount() {
                            return 10;
                        }

                        public int getColumnCount() {
                            return 10;
                        }

                        public Object getValueAt(int rowIndex, int columnIndex) {
                            return "" + rowIndex + " " + columnIndex;
                        }
                    });

                    header = new JTableHeader(table.getColumnModel());
                    header.setToolTipText("hello");

                    frame = new JFrame();
                    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                    frame.add(header);

                    frame.setSize(300, 300);
                    frame.setVisible(true);
                }
            });
            robot.waitForIdle();
            Point point = header.getLocationOnScreen();
            robot.mouseMove(point.x + 20, point.y + 50);
            robot.mouseMove(point.x + 30, point.y + 50);
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
         }
    }
}
