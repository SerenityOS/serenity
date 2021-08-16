/*
 * Copyright (c) 2012, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7024749 8019990
 * @summary JDK7 b131---a crash in: Java_sun_awt_windows_ThemeReader_isGetThemeTransitionDurationDefined+0x75
 * @library ../../regtesthelpers
 * @build Util
 * @author Oleg Pekhovskiy: area=awt.toplevel
   @run main bug7024749
 */

import java.awt.*;
import test.java.awt.regtesthelpers.Util;

public class bug7024749 {
    public static void main(String[] args) {
        final Frame f = new Frame("F");
        f.setBounds(0,0,200,200);
        f.setEnabled(false); // <- disable the top-level
        f.setVisible(true);

        Window w = new Window(f);
        w.setBounds(300,300,300,300);
        w.add(new TextField(20));
        w.setVisible(true);

        Robot robot = Util.createRobot();
        robot.setAutoDelay(1000);
        Util.waitForIdle(robot);
        robot.delay(1000);
        Util.clickOnTitle(f, robot);
        Util.waitForIdle(robot);

        f.dispose();
        System.out.println("Test passed!");
    }
}
