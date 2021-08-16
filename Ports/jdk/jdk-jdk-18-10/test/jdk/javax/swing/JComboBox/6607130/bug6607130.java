/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6607130
 * @summary Checks that JComboBox cell editor is hidden if the same
 *          item is selected with keyboard.
 *          Also checks that JComboBox cell editor is hidden if F2 and then
 *          ENTER were pressed.
 * @author Mikhail Lapshin
 */

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.KeyEvent;

public class bug6607130 {
    private JFrame frame;
    private JComboBox cb;
    private Robot robot;

    public static void main(String[] args) throws Exception {
        final bug6607130 test = new bug6607130();
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    test.setupUI();
                }
            });
            test.test();
        } finally {
            if (test.frame != null) {
                test.frame.dispose();
            }
        }
    }

    public bug6607130() throws AWTException {
        robot = new Robot();
    }

    private void setupUI() {
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        DefaultTableModel model = new DefaultTableModel(1, 1);
        JTable table = new JTable(model);

        cb = new JComboBox(new String[]{"one", "two", "three"});
        table.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(cb));
        frame.add(table);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void test() throws Exception {
        robot.waitForIdle();
        test1();
        robot.waitForIdle();
        checkResult("First test");
        test2();
        robot.waitForIdle();
        checkResult("Second test");
    }

    private void test1() throws Exception {
        // Select 'one'
        hitKey(KeyEvent.VK_TAB);
        robot.waitForIdle();
        hitKey(KeyEvent.VK_F2);
        robot.waitForIdle();
        hitKey(KeyEvent.VK_DOWN);
        robot.waitForIdle();
        hitKey(KeyEvent.VK_DOWN);
        robot.waitForIdle();
        hitKey(KeyEvent.VK_ENTER);
        robot.waitForIdle();

        // Select 'one' again
        hitKey(KeyEvent.VK_F2);
        robot.waitForIdle();
        hitKey(KeyEvent.VK_DOWN);
        robot.waitForIdle();
        hitKey(KeyEvent.VK_ENTER);
        robot.waitForIdle();
    }

    private void test2() throws Exception {
        // Press F2 and then press ENTER
        // Editor should be shown and then closed
        hitKey(KeyEvent.VK_F2);
        robot.waitForIdle();
        hitKey(KeyEvent.VK_ENTER);
        robot.waitForIdle();
    }

    private void checkResult(String testName) {
        if (!cb.isShowing()) {
            System.out.println(testName + " passed");
        } else {
            System.out.println(testName + " failed");
            throw new RuntimeException("JComboBox is showing " +
                    "after item selection.");
        }
    }

    public void hitKey(int keycode) {
        robot.keyPress(keycode);
        robot.keyRelease(keycode);
        delay();
    }

    private void delay() {
        try {
            Thread.sleep(1000);
        } catch(InterruptedException ie) {
            ie.printStackTrace();
        }
    }
}
