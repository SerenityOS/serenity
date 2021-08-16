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

/*
  @test
  @key headful
  @bug 8235904
  @run main/othervm/timeout=60 DrawLineTest
*/

import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;

public class DrawLineTest extends Frame {

    volatile static boolean done = false;

    public static void main(String[] args) throws Exception {

        EventQueue.invokeLater(() -> {
           DrawLineTest frame = new DrawLineTest();
           frame.setVisible(true);
           Image img = frame.createVolatileImage(1000, 1000);
           img.getGraphics().drawLine(0, 0, 34005, 34005);
           done = true;
           frame.setVisible(false);
           frame.dispose();
           return;
        });

        int cnt=0;
        while (!done && (cnt++ < 60)) {
            try {
               Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }

        if (!done) {
           // jtreg will shutdown the test properly
           if ((System.getProperty("test.src") != null)) {
               throw new RuntimeException("Test Failed");
           } else {
               // Not to be used in jtreg
               System.out.println("Test failed.");
               Runtime.getRuntime().halt(-1);
           }
        }
        return;
    }
}
