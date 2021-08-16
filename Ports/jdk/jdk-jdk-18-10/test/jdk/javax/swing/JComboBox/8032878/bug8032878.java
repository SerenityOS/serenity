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
 * @bug 8032878 8078855
 * @summary Checks that JComboBox as JTable cell editor processes key events
 *          even where setSurrendersFocusOnKeystroke flag in JTable is false and
 *          that it does not lose the first key press where the flag is true.
 * @run main bug8032878
 */

import java.awt.*;
import java.awt.event.KeyEvent;
import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.plaf.metal.MetalLookAndFeel;

public class bug8032878 implements Runnable {
    private static final String ONE = "one";
    private static final String TWO = "two";
    private static final String THREE = "three";

    private static final String EXPECTED = "one123";

    private final Robot robot;

    private JFrame frame;
    private JComboBox cb;

    private volatile boolean surrender;
    private volatile String text;

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(new MetalLookAndFeel());

        final bug8032878 test = new bug8032878();

        test.test(false);
        test.test(true);
    }

    public bug8032878() throws AWTException {
        robot = new Robot();
        robot.setAutoDelay(100);
    }

    private void setupUI() {
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTable table = new JTable(new String[][] {{ONE}, {TWO}, {THREE}},
                                  new String[] { "#"});
        table.setSurrendersFocusOnKeystroke(surrender);

        cb = new JComboBox(new String[]{ONE, TWO, THREE});
        cb.setEditable(true);
        DefaultCellEditor comboEditor = new DefaultCellEditor(cb);
        comboEditor.setClickCountToStart(1);
        table.getColumnModel().getColumn(0).setCellEditor(comboEditor);
        frame.add(table);

        frame.pack();
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);
    }

    private void test(final boolean flag) throws Exception {
        try {
            surrender = flag;
            SwingUtilities.invokeAndWait(this);

            robot.waitForIdle();
            robot.delay(1000);
            runTest();
            checkResult();
        } finally {
            if (frame != null) {
                SwingUtilities.invokeAndWait(() -> frame.dispose());
            }
        }
    }

    private void runTest() throws Exception {
        robot.waitForIdle();
        // Select 'one'
        robot.keyPress(KeyEvent.VK_TAB);
        robot.keyRelease(KeyEvent.VK_TAB);
        robot.waitForIdle();
        robot.keyPress(KeyEvent.VK_1);
        robot.keyRelease(KeyEvent.VK_1);
        robot.waitForIdle();
        robot.keyPress(KeyEvent.VK_2);
        robot.keyRelease(KeyEvent.VK_2);
        robot.waitForIdle();
        robot.keyPress(KeyEvent.VK_3);
        robot.keyRelease(KeyEvent.VK_3);
        robot.waitForIdle();
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);
        robot.waitForIdle();
    }

    private void checkResult() throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                text = ((JTextComponent) cb.getEditor().getEditorComponent()).getText();
            }
        });
        if (text.equals(EXPECTED)) {
            System.out.println("Test with surrender = " + surrender + " passed");
        } else {
            System.out.println("Test with surrender = " + surrender + " failed");
            throw new RuntimeException("Expected value in JComboBox editor '" +
                    EXPECTED + "' but found '" + text + "'.");
        }
    }


    @Override
    public void run() {
        setupUI();
    }
}
