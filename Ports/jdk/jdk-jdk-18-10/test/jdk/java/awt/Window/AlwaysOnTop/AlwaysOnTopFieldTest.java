/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Robot;
import java.awt.Window;
/**
 * @test
 * @key headful
 * @bug 7081594
 * @author Alexander Scherbatiy
 * @summary Windows owned by an always-on-top window DO NOT automatically become always-on-top
 * @run main AlwaysOnTopFieldTest
 */
public class AlwaysOnTopFieldTest {

    public static void main(String[] args) {
        Robot robot;
        try {
            robot = new Robot();
        } catch(Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Unexpected failure");
        }

        Window window = new Frame("Window 1");
        window.setSize(200, 200);
        window.setAlwaysOnTop(true);
        window.setVisible(true);
        robot.waitForIdle();

        Dialog dialog = new Dialog(window, "Owned dialog 1");
        dialog.setSize(200, 200);
        dialog.setLocation(100, 100);
        dialog.setVisible(true);
        robot.waitForIdle();

        try {
            if (!window.isAlwaysOnTop()) {
                throw new RuntimeException("Window has wrong isAlwaysOnTop value");
            }
            if (!dialog.isAlwaysOnTop()) {
                throw new RuntimeException("Dialog has wrong isAlwaysOnTop value");
            }
        } finally {
            window.dispose();
            dialog.dispose();
        }

        window = new Frame("Window 2");
        window.setSize(200, 200);
        window.setVisible(true);
        robot.waitForIdle();


        dialog = new Dialog(window, "Owned dialog 2");
        dialog.setSize(200, 200);
        dialog.setLocation(100, 100);
        dialog.setVisible(true);
        robot.waitForIdle();

        window.setAlwaysOnTop(true);
        robot.waitForIdle();

        try {
            if (!window.isAlwaysOnTop()) {
                throw new RuntimeException("Window has wrong isAlwaysOnTop value");
            }
            if (!dialog.isAlwaysOnTop()) {
                throw new RuntimeException("Dialog has wrong isAlwaysOnTop value");
            }
        } finally {
            window.dispose();
            dialog.dispose();
        }
    }
}
