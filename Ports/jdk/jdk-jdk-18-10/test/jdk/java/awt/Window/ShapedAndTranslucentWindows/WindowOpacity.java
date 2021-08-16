/*
 * Copyright (c) 2008, 2017, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @key headful
  @bug 6594131 8186617
  @summary Tests the Window.get/setOpacity() methods
*/

import java.awt.AWTException;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Robot;

public class WindowOpacity {

    public static void main(String[] args) throws Exception {
        GraphicsDevice gd =
                GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getDefaultScreenDevice();
        if (!gd.isWindowTranslucencySupported(
                GraphicsDevice.WindowTranslucency.TRANSLUCENT)) {
            System.out.println(
                    "Either the Toolkit or the native system does not support"
                            + " controlling the window opacity level.");
            return;
        }
        Frame f = new Frame("Opacity test");
        try {
            test(f);
        } finally {
            f.dispose();
        }
    }

    private static void test(final Frame f) throws AWTException {
        boolean passed;

        f.setUndecorated(true);
        float curOpacity = f.getOpacity();
        if (curOpacity < 1.0f || curOpacity > 1.0f) {
            throw new RuntimeException(
                    "getOpacity() reports the initial opacity level "
                            + "other than 1.0: " + curOpacity);
        }


        passed = false;
        try {
            f.setOpacity(-0.5f);
        } catch (IllegalArgumentException e) {
            passed = true;
        }
        if (!passed) {
            throw new RuntimeException(
                    "setOpacity() allows passing negative opacity level.");
        }


        passed = false;
        try {
            f.setOpacity(1.5f);
        } catch (IllegalArgumentException e) {
            passed = true;
        }
        if (!passed) {
            throw new RuntimeException(
                    "setOpacity() allows passing opacity level greater than 1.0.");
        }


        f.setOpacity(0.5f);
        curOpacity = f.getOpacity();
        if (curOpacity < 0.5f || curOpacity > 0.5f) {
            throw new RuntimeException(
                    "setOpacity() reports the opacity level that "
                            + "differs from the value set with "
                            + "setWindowOpacity: " + curOpacity);
        }


        f.setOpacity(0.75f);
        curOpacity = f.getOpacity();
        if (curOpacity < 0.75f || curOpacity > 0.75f) {
            throw new RuntimeException(
                    "getOpacity() reports the opacity level that "
                            + "differs from the value set with "
                            + "setWindowOpacity the second time: "
                            + curOpacity);
        }


        f.setBounds(100, 100, 300, 200);
        f.setVisible(true);
        Robot robot = new Robot();
        robot.waitForIdle();

        curOpacity = f.getOpacity();
        if (curOpacity < 0.75f || curOpacity > 0.75f) {
            throw new RuntimeException(
                    "getOpacity() reports the opacity level that "
                            + "differs from the value set with "
                            + "setWindowOpacity before showing the frame: "
                            + curOpacity);
        }
        f.setOpacity(0.5f);
        robot.waitForIdle();
        curOpacity = f.getOpacity();
        if (curOpacity < 0.5f || curOpacity > 0.5f) {
            throw new RuntimeException(
                    "getOpacity() reports the opacity level that "
                            + "differs from the value set with "
                            + "setWindowOpacity after showing the frame: "
                            + curOpacity);
        }
    }
}
