/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6547241
 * @summary Test verifies that concurrent usage of jpeg writer instance
 *          by number of threads does not cause crash in jpeg library.
 * @run     main ConcurrentWritingTest
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

public class ConcurrentWritingTest extends Thread {

    static ImageWriter w = null;
    static File pwd = new File(".");
    static BufferedImage img;

    private static int MAX_THREADS = 50;
    private static int completeCount = 0;
    private static Object lock = new Object();

    public static void main(String[] args) throws Exception {
        img = createTestImage();

        w = ImageIO.getImageWritersByFormatName("JPEG").next();

        for (int i = 0; i < MAX_THREADS; i++) {
            (new ConcurrentWritingTest()).start();
        }

        // wait for threads
        boolean needWait = true;
        while(needWait) {
            synchronized(lock) {
                needWait = completeCount < MAX_THREADS;
            }
        }
        System.out.println("Test PASSED");
    }

    public void run() {
        try {
            File f = File.createTempFile("writer_", ".jpg", pwd);
            ImageOutputStream ios = ImageIO.createImageOutputStream(f);
            w.setOutput(ios);
            Thread.sleep(70);
            w.write(img);
            Thread.sleep(70);
            w.reset();
        } catch (IllegalStateException e) {
            System.out.println(e);
        } catch (IOException e) {
            System.out.println(e);
        } catch (Throwable e) {
            // Unexpected exception. Test failed.
            throw new RuntimeException("Test failed.", e);
        } finally {
            synchronized(lock) {
                completeCount ++;
            }
        }
    }

    private static BufferedImage createTestImage() {
        int w = 1024;
        int h = 768;

        BufferedImage img = new
            BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        Color[] colors = { Color.red, Color.green, Color.blue };
        float[] dist = {0.0f, 0.5f, 1.0f };
        Point2D center = new Point2D.Float(0.5f * w, 0.5f * h);

        RadialGradientPaint p =
            new RadialGradientPaint(center, 0.5f * w, dist, colors);
        g.setPaint(p);
        g.fillRect(0, 0, w, h);
        g.dispose();

        return img;
    }
}
