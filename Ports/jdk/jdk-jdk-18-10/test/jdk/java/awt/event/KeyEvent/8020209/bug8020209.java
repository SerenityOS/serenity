/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @key headful
 * @bug 8020209
 * @summary [macosx] Mac OS X key event confusion for "COMMAND PLUS"
 * @author leonid.romanov@oracle.com
 * @library /test/lib
 * @build jdk.test.lib.Platform
 * @run main bug8020209
 */

import java.awt.*;
import java.awt.event.*;

import jdk.test.lib.Platform;

public class bug8020209 {
    static volatile int listenerCallCounter = 0;

    static AWTKeyStroke keyStrokes[] = {
        AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_DECIMAL,  InputEvent.META_MASK),
        AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_EQUALS,   InputEvent.META_MASK),
        AWTKeyStroke.getAWTKeyStroke(KeyEvent.VK_ESCAPE,   InputEvent.CTRL_MASK),
    };

    public static void main(String[] args) throws Exception {
        if (!Platform.isOSX()) {
            System.out.println("This test is for MacOS only. Automatically passed on other platforms.");
            return;
        }

        System.setProperty("apple.laf.useScreenMenuBar", "true");

        Robot robot = new Robot();
        robot.setAutoDelay(50);

        createAndShowGUI();
        robot.waitForIdle();

        for (int i = 0; i < keyStrokes.length; ++i) {
            AWTKeyStroke ks = keyStrokes[i];

            int modKeyCode = getModKeyCode(ks.getModifiers());
            robot.keyPress(modKeyCode);

            robot.keyPress(ks.getKeyCode());
            robot.keyRelease(ks.getKeyCode());

            robot.keyRelease(modKeyCode);

            robot.waitForIdle();

            if (listenerCallCounter != 4) {
                throw new Exception("Test failed: KeyListener for '" + ks.toString() +
                        "' called " + listenerCallCounter + " times instead of 4!");
            }

            listenerCallCounter = 0;
        }

    }

    private static void createAndShowGUI() {
        Frame frame = new Frame("Test");
        frame.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                listenerCallCounter++;
            }

            @Override
            public void keyReleased(KeyEvent e) {
                listenerCallCounter++;
            }
        });

        frame.pack();
        frame.setVisible(true);
    }

    private static int getModKeyCode(int mod) {
        if ((mod & (InputEvent.SHIFT_DOWN_MASK | InputEvent.SHIFT_MASK)) != 0) {
            return KeyEvent.VK_SHIFT;
        }

        if ((mod & (InputEvent.CTRL_DOWN_MASK | InputEvent.CTRL_MASK)) != 0) {
            return KeyEvent.VK_CONTROL;
        }

        if ((mod & (InputEvent.ALT_DOWN_MASK | InputEvent.ALT_MASK)) != 0) {
            return KeyEvent.VK_ALT;
        }

        if ((mod & (InputEvent.META_DOWN_MASK | InputEvent.META_MASK)) != 0) {
            return KeyEvent.VK_META;
        }

        return 0;
    }
}
