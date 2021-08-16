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
 * @bug     6557086 6547241
 * @summary Test verifies that invocation of reset/abort/dispose methods from
 *          another thread does not cause crash in jpeg library.
 * @run     main ReadingInterruptionTest
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageReadParam;
import javax.imageio.event.IIOReadProgressListener;
import javax.imageio.stream.ImageInputStream;


public class ReadingInterruptionTest implements IIOReadProgressListener {

    public static void main(String[] args) {
        createTestFile();

        System.out.println("Test abort()....");
        ReadingInterruptionTest t = new ReadingInterruptionTest(new AbortAction());
        t.doTest();

        System.out.println("Test reset()....");
        t = new ReadingInterruptionTest(new ResetAction());
        t.doTest();

        System.out.println("Test dispose()....");
        t = new ReadingInterruptionTest(new DisposeAction());
        t.doTest();
     }

    protected abstract static class Action implements Runnable {
        protected ImageReader target;

        public void setTarget(ImageReader target) {
            this.target = target;
        }

        public abstract void run();
    }

    protected static class DisposeAction extends Action {
        public void run() {
            try {
                target.dispose();
            } catch (IllegalStateException e) {
                System.out.println("Test PASSED: IllegalStateException was thrown.");
            } catch (Throwable e) {
                throw new RuntimeException("Test FAILED.", e);
            }
        }
    }

    protected static class AbortAction extends Action {
        public void run() {
            try {
                target.abort();
            } catch (IllegalStateException e) {
                System.out.println("Test PASSED: IllegalStateException was thrown.");
            } catch (Throwable e) {
                throw new RuntimeException("Test FAILED.", e);
            }
        }
    }

    protected static class ResetAction extends Action {
        public void run() {
            try {
                target.reset();
            } catch (IllegalStateException e) {
                System.out.println("Test PASSED: IllegalStateException was thrown.");
            } catch (Throwable e) {
                throw new RuntimeException("Test FAILED.", e);
            }
        }
    }

    static File file = new File("IMGP1001.JPG");

    Action action;
    ImageInputStream iis;
    ImageReader reader;

    protected ReadingInterruptionTest(Action action) {
        this.action = action;

        reader = ImageIO.getImageReadersByFormatName("JPEG").next();

        this.action.setTarget(reader);
    }

    public void doTest() {
        try {
            reader.addIIOReadProgressListener(this);
            iis = ImageIO.createImageInputStream(file);
            reader.setInput(iis);
            ImageReadParam p = reader.getDefaultReadParam();
            Thread.sleep(70);
            BufferedImage res = reader.read(0, p);
            Thread.sleep(70);
        } catch (Exception e) {
            /*
             * we do expect that concurrent attempt to dispose this
             * instance of image reader will be blocked. So, this image
             * should be read sucessfuly. Otherwise, something went wrong
             * and we need to report test failure.
             */
            throw new RuntimeException("Test FAILED", e);
        } finally {
            /*
             * it would happen that concurrent invocation of dispose() method
             * will be successful. Due to race condition it seems to be possible
             * that dispose operation will be performed after than read() operation
             * leaveled thread lock. In this case all subsequent calls for reader
             * methods should results in IllegalStateException. So, we treat
             * IllegalStateException as success. Everything else means test failure.
             */
            try {
                reader.reset();
            } catch (IllegalStateException e) {
                System.out.println("Expected exception was caught: " + e);
            } catch(Exception e) {
                throw new RuntimeException("Test FAILED.", e);
            }
        }
        System.out.println("Test PASSED.");
    }

    // listener medthods
    public void imageStarted(ImageReader source,
                                  int imageIndex) {} ;

    public void imageProgress(ImageReader source,
                              float percentageDone)
    {
        if (20f < percentageDone && percentageDone < 80f) {
            Thread t = new Thread(action);
            t.start();
        }
    };

    public void imageComplete(ImageReader source) {};


    public void sequenceStarted(ImageReader source,
                                int minIndex) {};

    public void sequenceComplete(ImageReader source) {};

    public void thumbnailStarted(ImageReader source,
                                 int imageIndex,
                                 int thumbnailIndex) {};

    public void thumbnailProgress(ImageReader source,
                                  float percentageDone) {};

    public void thumbnailComplete(ImageReader source) {};

    public void readAborted(ImageReader source) {};

    private static void createTestFile() {
        int w = 1280;
        int h = 1024;

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

        try {
            System.out.println("Create test image " + file.getAbsolutePath());
            boolean b = ImageIO.write(img, "JPEG", file);
            if (!b) {
                throw new RuntimeException("Failed to create test image.");
            }
        } catch (IOException e) {
            throw new RuntimeException("Test failed", e);
        }
    }
}
