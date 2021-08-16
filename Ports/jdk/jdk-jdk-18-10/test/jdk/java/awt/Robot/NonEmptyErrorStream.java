/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Frame;
import java.awt.Robot;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.concurrent.TimeUnit;

import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

/**
 * @test
 * @bug 8226806
 * @key headful
 * @library /test/lib
 * @summary checks for unexpected output in stderr and stdout
 */
public final class NonEmptyErrorStream {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
                    NonEmptyErrorStream.class.getSimpleName(),"run");
            Process p = pb.start();
            OutputAnalyzer output = new OutputAnalyzer(p);
            p.waitFor(20, TimeUnit.SECONDS);
            p.destroy();
            output.shouldHaveExitValue(0);
            output.stdoutShouldBeEmpty();
            output.stderrShouldBeEmpty();
            return;
        }
        Frame frame = new Frame();
        frame.setSize(400, 300);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        Robot robot = new Robot();
        robot.setAutoDelay(50);
        robot.waitForIdle();
        frame.toFront();

        Rectangle rect = frame.getBounds();
        int x = (int) rect.getCenterX();
        int y = (int) rect.getCenterY();

        for (int i = 0; i < 20; i++) {
            robot.getPixelColor(x, y);
            robot.createScreenCapture(rect);
        }
        for (int i = 0; i < 20; i++) {
            robot.mouseMove(x + 50, y + 50);
            robot.mouseMove(x - 50, y - 50);
        }
        for (int i = 0; i < 20; i++) {
            robot.keyPress(KeyEvent.VK_ESCAPE);
            robot.keyRelease(KeyEvent.VK_ESCAPE);
        }
        for (int i = 0; i < 20; i++) {
            robot.mousePress(InputEvent.BUTTON1_MASK);
            robot.mouseRelease(InputEvent.BUTTON1_MASK);
        }
        robot.waitForIdle();
        frame.dispose();
    }
}
