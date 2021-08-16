/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6366813 6459844 8198613
 * @summary Tests that no exception is thrown if a frame is resized just
 * before we create a bufferStrategy
 * @author Dmitri.Trembovetski area=FullScreen/BufferStrategy
 * @run main/othervm BufferStrategyExceptionTest
 */

import java.awt.AWTException;
import java.awt.BufferCapabilities;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.ImageCapabilities;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;

/**
 * The purpose of this test is to make sure that we do not throw an
 * IllegalStateException during the creation of BufferStrategy if
 * a window has been resized just before our creation attempt.
 *
 * We test both windowed and fullscreen mode, although the exception has
 * been observed in full screen mode only.
 */
public class BufferStrategyExceptionTest {
    private static final int TEST_REPS = 20;

    public static void main(String[] args) {
        GraphicsDevice gd =
            GraphicsEnvironment.getLocalGraphicsEnvironment().
                getDefaultScreenDevice();

        for (int i = 0; i < TEST_REPS; i++) {
            TestFrame f = new TestFrame();
            f.pack();
            f.setSize(400, 400);
            f.setVisible(true);
            if (i % 2 == 0) {
                gd.setFullScreenWindow(f);
            }
            // generate a resize event which will invalidate the peer's
            // surface data and hopefully cause an exception during
            // BufferStrategy creation in TestFrame.render()
            Dimension d = f.getSize();
            d.width -= 5; d.height -= 5;
            f.setSize(d);

            f.render();
            gd.setFullScreenWindow(null);
            sleep(100);
            f.dispose();
        }
        System.out.println("Test passed.");
    }

    private static void sleep(long msecs) {
        try {
            Thread.sleep(msecs);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
        }
    }

    private static final BufferedImage bi =
        new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);

    static class TestFrame extends Frame {
        TestFrame() {
            setUndecorated(true);
            setIgnoreRepaint(true);
            setSize(400, 400);
        }

        public void render() {
            ImageCapabilities imgBackBufCap = new ImageCapabilities(true);
            ImageCapabilities imgFrontBufCap = new ImageCapabilities(true);
            BufferCapabilities bufCap =
                new BufferCapabilities(imgFrontBufCap,
                    imgBackBufCap, BufferCapabilities.FlipContents.COPIED);
            try {

                createBufferStrategy(2, bufCap);
            } catch (AWTException ex) {
                createBufferStrategy(2);
            }

            BufferStrategy bs = getBufferStrategy();
            do {
                Graphics g =  bs.getDrawGraphics();
                g.setColor(Color.green);
                g.fillRect(0, 0, getWidth(), getHeight());

                g.setColor(Color.red);
                g.drawString("Rendering test", 20, 20);

                g.drawImage(bi, 50, 50, null);

                g.dispose();
                bs.show();
            } while (bs.contentsLost()||bs.contentsRestored());
        }
    }

}
