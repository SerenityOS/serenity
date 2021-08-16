/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6648018
 * @summary Tests that we don't run into infinite validation loop when copying
            a VolatileImage to the screen
 * @author Dmitri.Trembovetski@sun.com: area=Graphics
 * @run main/othervm InfiniteValidationLoopTest
 * @run main/othervm -Dsun.java2d.d3d=false InfiniteValidationLoopTest
 */
import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import static java.awt.image.VolatileImage.*;
import java.awt.image.VolatileImage;
import java.util.concurrent.CountDownLatch;

public class InfiniteValidationLoopTest extends Frame {
    private static volatile boolean failed = false;
    private static final int LOOP_THRESHOLD = 50;
    private static volatile CountDownLatch latch;
    private VolatileImage vi;

    public InfiniteValidationLoopTest() {
        super("InfiniteValidationLoopTest");
    }

    @Override
    public void paint(Graphics g) {
        try {
            runTest(g);
        } finally {
            latch.countDown();
        }
    }

    private void runTest(Graphics g) {
        int status = IMAGE_OK;
        int count1 = 0;
        do {
            GraphicsConfiguration gc = getGraphicsConfiguration();
            int count2 = 0;
            while (vi == null || (status = vi.validate(gc)) != IMAGE_OK) {
                if (++count2 > LOOP_THRESHOLD) {
                    System.err.println("Infinite loop detected: count2="+count2);
                    failed = true;
                    return;
                }
                if (vi == null || status == IMAGE_INCOMPATIBLE) {
                    if (vi != null) { vi.flush(); vi = null; }
                    vi = gc.createCompatibleVolatileImage(100, 100);
                    continue;
                }
                if (status == IMAGE_RESTORED) {
                    Graphics gg = vi.getGraphics();
                    gg.setColor(Color.green);
                    gg.fillRect(0, 0, vi.getWidth(), vi.getHeight());
                    break;
                }
            }
            g.drawImage(vi, getInsets().left, getInsets().top, null);
            if (++count1 > LOOP_THRESHOLD) {
                System.err.println("Infinite loop detected: count1="+count1);
                failed = true;
                return;
            }
        } while (vi.contentsLost());
    }

    public static void main(String[] args) {
        latch = new CountDownLatch(1);
        InfiniteValidationLoopTest t1 = new InfiniteValidationLoopTest();
        t1.pack();
        t1.setSize(200, 200);
        t1.setVisible(true);
        try { latch.await(); } catch (InterruptedException ex) {}
        t1.dispose();

        latch = new CountDownLatch(1);
        t1 = new InfiniteValidationLoopTest();
        t1.pack();
        t1.setSize(50, 50);
        t1.setVisible(true);
        try { latch.await(); } catch (InterruptedException ex) {}
        t1.dispose();

        if (failed) {
            throw new
                RuntimeException("Failed: infinite validattion loop detected");
        }
        System.out.println("Test PASSED");
    }
}
