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

/* @test
 * @key headful
 * @bug 6647340
 * @summary Checks that iconified internal frame follows
 *          the main frame borders properly.
 * @run main bug6647340
 */

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyVetoException;

public class bug6647340 {
    private JFrame frame;
    private volatile Point location;
    private volatile Point iconloc;
    private JInternalFrame jif;
    private static Robot robot;

    public static void main(String[] args) throws Exception {
        robot = new Robot();
        final bug6647340 test = new bug6647340();
        try {
            SwingUtilities.invokeAndWait(() -> test.setupUI());
            robot.waitForIdle();
            robot.delay(1000);
            test.test();
        } finally {
            if (test.frame != null) {
                SwingUtilities.invokeAndWait(() -> test.frame.dispose());
            }
        }
    }

    private void setupUI() {
        frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JDesktopPane desktop = new JDesktopPane();
        frame.add(desktop);

        jif = new JInternalFrame("Internal Frame", true, true, true, true);
        jif.setBounds(20, 20, 200, 100);
        desktop.add(jif);
        jif.setVisible(true);

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setBounds((screen.width - 400) / 2, (screen.height - 400) / 2, 400, 400);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void test() throws Exception {
        test1();

        robot.waitForIdle();
        robot.delay(500);
        check1();
        robot.waitForIdle();

        test2();
        robot.waitForIdle();
        robot.delay(500);
        check2();
    }

    private void test1() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            setIcon(true);
            location = jif.getDesktopIcon().getLocation();
            Dimension size = frame.getSize();
            frame.setSize(size.width + 100, size.height + 100);
        });
    }

    private void test2() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            setIcon(false);
        });
        robot.waitForIdle();
        robot.delay(500);

        SwingUtilities.invokeAndWait(() -> {
            Dimension size = frame.getSize();
            frame.setSize(size.width - 100, size.height - 100);
        });
        robot.waitForIdle();
        robot.delay(500);

        SwingUtilities.invokeAndWait(() -> {
            setIcon(true);
        });
    }

    private void check1() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            iconloc = jif.getDesktopIcon().getLocation();
        });
        if (!iconloc.equals(location)) {
            System.out.println("First test passed");
        } else {
            throw new RuntimeException("Icon isn't shifted with the frame bounds");
        }
    }

    private void check2() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            iconloc = jif.getDesktopIcon().getLocation();
        });
        if (iconloc.equals(location)) {
            System.out.println("Second test passed");
        } else {
            throw new RuntimeException("Icon isn't located near the frame bottom");
        }
    }

    private void setIcon(boolean b) {
        try {
            jif.setIcon(b);
        } catch (PropertyVetoException e) {
            e.printStackTrace();
        }
    }
}
