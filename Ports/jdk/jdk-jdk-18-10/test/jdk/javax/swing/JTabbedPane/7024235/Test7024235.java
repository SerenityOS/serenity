/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Robot;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

/*
 * @test
 * @key headful
 * @bug 7024235
 * @summary Tests JFrame.pack() with the JTabbedPane
 * @run main Test7024235
 */

public class Test7024235 implements Runnable {

    private static final boolean AUTO = null != System.getProperty("test.src", null);

    public static void main(String[] args) throws Exception {
        Test7024235 test = new Test7024235();
        for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            String className = info.getClassName();
            System.out.println("className = " + className);
            UIManager.setLookAndFeel(className);

            test.test();
            try {
                Robot robot = new Robot();
                robot.waitForIdle();
                robot.delay(1000);
            }catch(Exception ex) {
                ex.printStackTrace();
                throw new Error("Unexpected Failure");
            }
            test.test();
        }
    }

    private volatile JTabbedPane pane;
    private volatile boolean passed;

    public void run() {
        if (this.pane == null) {
            this.pane = new JTabbedPane();
            this.pane.addTab("1", new Container());
            this.pane.addTab("2", new JButton());
            this.pane.addTab("3", new JCheckBox());

            JFrame frame = new JFrame();
            frame.add(BorderLayout.WEST, this.pane);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            test("first");
        }
        else {
            test("second");
            if (this.passed || AUTO) { // do not close a frame for manual review
                SwingUtilities.getWindowAncestor(this.pane).dispose();
            }
            this.pane = null;
        }
    }

    private void test() throws Exception {
        SwingUtilities.invokeAndWait(this);
        if (!this.passed && AUTO) { // error reporting only for automatic testing
            throw new Error("TEST FAILED");
        }
    }

    private void test(String step) {
        this.passed = true;
        for (int index = 0; index < this.pane.getTabCount(); index++) {
            Rectangle bounds = this.pane.getBoundsAt(index);
            if (bounds == null) {
                continue;
            }
            int centerX = bounds.x + bounds.width / 2;
            int centerY = bounds.y + bounds.height / 2;
            int actual = this.pane.indexAtLocation(centerX, centerY);
            if (index != actual) {
                System.out.println("name = " + UIManager.getLookAndFeel().getName());
                System.out.println("step = " + step);
                System.out.println("index = " + index);
                System.out.println("bounds = " + bounds);
                System.out.println("indexAtLocation(" + centerX + "," + centerX + ") returns " + actual);
                this.passed = false;
            }
        }
    }
}
