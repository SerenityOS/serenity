/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @summary unit test for getPointerInfo() from MouseInfo class
  @author dav@sparc.spb.su: area=
  @bug 4009555
  @run main GetPointerInfoTest
*/

import java.awt.*;

/**
 * Simply check the result on non-null and results are correct.
 */
public class GetPointerInfoTest {
    private static final String successStage = "Test stage completed.Passed.";

    public static void main(String[] args) throws Exception {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gds = ge.getScreenDevices();
        int gdslen = gds.length;
        System.out.println("There are " + gdslen + " Graphics Devices");
        if (gdslen == 0) {
            System.out.println("Nothing to be done.");
            return;
        }
        Robot robot = new Robot(gds[0]);
        robot.setAutoDelay(0);
        robot.setAutoWaitForIdle(true);
        robot.delay(10);
        robot.waitForIdle();
        Point p = new Point(101, 99);
        robot.mouseMove(p.x, p.y);

        PointerInfo pi = MouseInfo.getPointerInfo();
        if (pi == null) {
            throw new RuntimeException("Test failed. getPointerInfo() returned null value.");
        } else {
            System.out.println(successStage);
        }
        Point piLocation = pi.getLocation();

        if (piLocation.x != p.x || piLocation.y != p.y) {
            throw new RuntimeException("Test failed.getPointerInfo() returned incorrect result.");
        } else {
            System.out.println(successStage);
        }

        System.out.println("Test PASSED.");
    }
}
