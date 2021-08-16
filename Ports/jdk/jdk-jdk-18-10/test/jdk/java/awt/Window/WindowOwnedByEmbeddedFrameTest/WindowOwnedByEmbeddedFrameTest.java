/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8130655
 * @summary Tests that window owned by EmbeddedFrame can receive keyboard input
 * @requires (os.family == "mac")
 * @modules java.desktop/sun.awt
 * @library ../../regtesthelpers
 * @build Util
 * @run main WindowOwnedByEmbeddedFrameTest
 */

import sun.awt.EmbeddedFrame;

import java.awt.Robot;
import java.awt.TextField;
import java.awt.Window;
import java.awt.event.KeyEvent;

import test.java.awt.regtesthelpers.Util;

public class WindowOwnedByEmbeddedFrameTest {
    private static TextField textField;
    private static EmbeddedFrame embeddedFrame;
    private static Window window;

    public static void main(String[] args) {
        try {
            Robot robot = Util.createRobot();
            robot.setAutoDelay(50);

            embeddedFrame = createEmbeddedFrame();

            textField = new TextField("");

            window = new Window(embeddedFrame);
            window.setSize(200, 200);
            window.setLocationRelativeTo(null);
            window.add(textField);
            window.setVisible(true);

            Util.waitForIdle(robot);

            Util.clickOnComp(textField, robot);
            Util.waitForIdle(robot);

            robot.keyPress(KeyEvent.VK_T);
            robot.keyRelease(KeyEvent.VK_T);
            Util.waitForIdle(robot);

            robot.keyPress(KeyEvent.VK_E);
            robot.keyRelease(KeyEvent.VK_E);
            Util.waitForIdle(robot);

            robot.keyPress(KeyEvent.VK_S);
            robot.keyRelease(KeyEvent.VK_S);
            Util.waitForIdle(robot);

            robot.keyPress(KeyEvent.VK_T);
            robot.keyRelease(KeyEvent.VK_T);
            Util.waitForIdle(robot);

            if ("".equals(textField.getText())) {
                throw new RuntimeException("Keyboard input in text field isn't possible");
            }
        } finally {
            if (embeddedFrame != null) {
                embeddedFrame.dispose();
            }
            if (window != null) {
                window.dispose();
            }
        }
    }

    private static EmbeddedFrame createEmbeddedFrame() {
        try {
            return (EmbeddedFrame) Class.forName("sun.lwawt.macosx.CEmbeddedFrame").newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Cannot create EmbeddedFrame", e);
        }
    }
}

