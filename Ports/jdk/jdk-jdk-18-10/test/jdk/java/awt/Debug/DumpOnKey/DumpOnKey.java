/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AWTException;
import java.awt.Frame;
import java.awt.Robot;
import java.awt.Window;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.PrintStream;

/**
 * @test
 * @key headful
 * @bug 4379403
 * @run main/othervm DumpOnKey false
 * @run main/othervm DumpOnKey -Dsun.awt.nativedebug=true true
 * @run main/othervm DumpOnKey -Dsun.awt.nativedebug=true -Dawtdebug.on=true true
 * @run main/othervm DumpOnKey -Dsun.awt.nativedebug=false -Dawtdebug.on=true false
 * @run main/othervm DumpOnKey -Dsun.awt.nativedebug=true -Dawtdebug.on=false false
 * @run main/othervm DumpOnKey -Dsun.awt.nativedebug=false -Dawtdebug.on=false false
 * @run main/othervm/java.security.policy=dump.policy/secure=java.lang.SecurityManager DumpOnKey -Dsun.awt.nativedebug=true true
 * @run main/othervm/java.security.policy=dump.policy/secure=java.lang.SecurityManager DumpOnKey -Dsun.awt.nativedebug=true -Dawtdebug.on=false false
 */
public final class DumpOnKey {

    private static volatile boolean dumped;

    public static void main(final String[] args) throws AWTException {
        final boolean dump = Boolean.parseBoolean(args[0]);
        final Window w = new Frame() {
            @Override
            public void list(final PrintStream out, final int indent) {
                super.list(out, indent);
                dumped = true;
            }
        };
        w.setSize(200, 200);
        w.setLocationRelativeTo(null);
        w.setVisible(true);

        final Robot robot = new Robot();
        robot.setAutoDelay(50);
        robot.setAutoWaitForIdle(true);
        robot.mouseMove(w.getX() + w.getWidth() / 2,
                        w.getY() + w.getHeight() / 2);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_SHIFT);
        robot.keyPress(KeyEvent.VK_F1);
        robot.keyRelease(KeyEvent.VK_F1);
        robot.keyRelease(KeyEvent.VK_SHIFT);
        robot.keyRelease(KeyEvent.VK_CONTROL);

        w.dispose();
        if (dumped != dump) {
            throw new RuntimeException("Exp:" + dump + ", actual:" + dumped);
        }
    }
}
