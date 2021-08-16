/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8020983 8024697
 * @summary Test verifies that jpeg writer instances are collected
 *          even if destroy() or reset() methods is not invoked.
 *
 * @run main JpegWriterLeakTest
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

public class JpegWriterLeakTest {

    public static void main(String[] args) {
        final ReferenceQueue<ImageWriter> queue = new ReferenceQueue<>();
        final ArrayList<Reference<? extends ImageWriter>> refs = new ArrayList<>();

        int count = 2;

        do {
            ImageWriter writer =
                    ImageIO.getImageWritersByFormatName("jpeg").next();

            final WeakReference<? extends ImageWriter> ref =
                    new WeakReference<>(writer, queue);

            refs.add(ref);


            try {
                final ImageOutputStream os =
                        ImageIO.createImageOutputStream(new ByteArrayOutputStream());
                writer.setOutput(os);

                writer.write(getImage());


                // NB: dispose() or reset() workarounds the problem.
            } catch (IOException e) {
            } finally {
                writer = null;
            }
            count--;
        } while (count > 0);


        System.out.println("Wait for GC...");

        final long testTimeOut = 60000L;

        final long startTime = System.currentTimeMillis();

        while (!refs.isEmpty()) {
            // check for the test timeout
            final long now = System.currentTimeMillis();

            if (now - startTime > testTimeOut) {
                System.out.println();
                throw new RuntimeException("Test FAILED.");
            }

            System.gc();

            try {
                System.out.print(".");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            };

            Reference<? extends ImageWriter> r = queue.poll();
            if (r != null) {
                System.out.println("Got reference: " + r);
                refs.remove(r);
            }
        }
        System.out.println("Test PASSED.");
    }

    private static BufferedImage getImage() {
        int width = 2500;
        int height = new Random().nextInt(2500) + 1;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = image.createGraphics();
        g.setColor(Color.blue);
        g.fillRect(0, 0, width, height);

        return image;
    }
}
