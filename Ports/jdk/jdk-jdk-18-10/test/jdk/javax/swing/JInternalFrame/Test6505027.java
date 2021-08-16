/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6505027
 * @summary Tests focus problem inside internal frame
 * @author Sergey Malenkov
 * @library ..
 */

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.InputEvent;
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

public class Test6505027 {

    private static final boolean INTERNAL = true;
    private static final boolean TERMINATE = true;

    private static final int WIDTH = 450;
    private static final int HEIGHT = 200;
    private static final int OFFSET = 10;

    private static final String[] COLUMNS = { "Size", "Shape" }; // NON-NLS: column names
    private static final String[] ITEMS = { "a", "b", "c", "d" }; // NON-NLS: combobox content
    private static final String KEY = "terminateEditOnFocusLost"; // NON-NLS: property name

    public static void main(String[] args) throws Throwable {
        SwingTest.start(Test6505027.class);
    }

    private final JTable table = new JTable(new DefaultTableModel(COLUMNS, 2));

    public Test6505027(JFrame main) {
        Container container = main;
        if (INTERNAL) {
            JInternalFrame frame = new JInternalFrame();
            frame.setBounds(OFFSET, OFFSET, WIDTH, HEIGHT);
            frame.setVisible(true);

            JDesktopPane desktop = new JDesktopPane();
            desktop.add(frame, new Integer(1));

            container.add(desktop);
            container = frame;
        }
        if (TERMINATE) {
            this.table.putClientProperty(KEY, Boolean.TRUE);
        }
        TableColumn column = this.table.getColumn(COLUMNS[1]);
        column.setCellEditor(new DefaultCellEditor(new JComboBox(ITEMS)));

        container.add(BorderLayout.NORTH, new JTextField());
        container.add(BorderLayout.CENTER, new JScrollPane(this.table));
    }

    public void press() throws AWTException {
        Point point = this.table.getCellRect(1, 1, false).getLocation();
        SwingUtilities.convertPointToScreen(point, this.table);

        Robot robot = new Robot();
        robot.setAutoDelay(50);
        robot.mouseMove(point.x + 1, point.y + 1);
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
    }

    public static void validate() {
        Component component = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (!component.getClass().equals(JComboBox.class)) {
            throw new Error("unexpected focus owner: " + component);
        }
    }
}
