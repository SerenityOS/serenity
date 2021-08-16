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
  @bug 8041490
  @summary tests that the WarningWindow's surface is invalidated on dispose
  @author Petr Pchelko
  @run main/othervm/policy=policy  -Djava.security.manager WarningWindowDisposeCrashTest
*/



import java.awt.*;

public class WarningWindowDisposeCrashTest {
    public static void main(String[] args) throws Exception {
        Frame f = new Frame();
        f.setVisible(true);
        Robot robot;
        try{
            robot = new Robot();
            robot.waitForIdle();
        }catch(Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException("Cannot create Robot");
        }
        Thread.sleep(1000);
        f.dispose();
        // If the bug is present VM could crash after this call
        for (int i = 0; i < 1000; i++) Toolkit.getDefaultToolkit().sync();
    }
}
