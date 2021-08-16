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
 * @run     main WritingInterruptionTest
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
import javax.imageio.ImageWriteParam;
import javax.imageio.event.IIOWriteProgressListener;
import javax.imageio.stream.ImageOutputStream;


public class WritingInterruptionTest implements IIOWriteProgressListener {

    static File pwd = new File(".");
    static BufferedImage img;

    public static void main(String[] args) {
        img = createTestImage();

        System.out.println("Test abort()....");
        WritingInterruptionTest t = new WritingInterruptionTest(new AbortAction());
        t.doTest();

        System.out.println("Test reset()....");
        t = new WritingInterruptionTest(new ResetAction());
        t.doTest();

        System.out.println("Test dispose()....");
        t = new WritingInterruptionTest(new DisposeAction());
        t.doTest();
     }

    protected abstract static class Action implements Runnable {
        protected ImageWriter target;

        public void setTarget(ImageWriter target) {
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


    Action action;
    ImageOutputStream ios;
    ImageWriter w;

    protected WritingInterruptionTest(Action action) {
        this.action = action;

        w = ImageIO.getImageWritersByFormatName("JPEG").next();

        this.action.setTarget(w);
    }

    public void doTest() {
        try {
            w.addIIOWriteProgressListener(this);
            File f = File.createTempFile("writer_", ".jpg", pwd);
            ImageOutputStream ios = ImageIO.createImageOutputStream(f);
            w.setOutput(ios);
            Thread.sleep(70);
            w.write(img);
            Thread.sleep(70);
        } catch (Exception e) {
            /*
             * we do expect that concurrent attempt to dispose this
             * instance of image writer will be blocked. So, this image
             * should be writen sucessfuly. Otherwise, something went wrong
             * and we need to report test failure.
             */
            throw new RuntimeException("Test FAILED", e);
        } finally {
            /*
             * it would happen that concurrent invocation of dispose() method
             * will be successful. Due to race condition it seems to be possible
             * that dispose operation will be performed after than write() operation
             * leaveled thread lock. In this case all subsequent calls for writer
             * methods should results in IllegalStateException. So, we treat
             * IllegalStateException as success. Everything else means test failure.
             */
            try {
                w.reset();
            } catch (IllegalStateException e) {
                System.out.println("Expected exception was caught: " + e);
            } catch(Exception e) {
                throw new RuntimeException("Test FAILED.", e);
            }
        }
        System.out.println("Test PASSED.");
    }

    // listener medthods
    public void imageStarted(ImageWriter source,
                                  int imageIndex) {} ;

    public void imageProgress(ImageWriter source,
                              float percentageDone)
    {
        if (20f < percentageDone && percentageDone < 80f ) {
            Thread t = new Thread(action);
            t.start();
        }
    };

    public void imageComplete(ImageWriter source) {};

    public void thumbnailStarted(ImageWriter source,
                                 int imageIndex,
                                 int thumbnailIndex) {};

    public void thumbnailProgress(ImageWriter source,
                                  float percentageDone) {};

    public void thumbnailComplete(ImageWriter source) {};

    public void writeAborted(ImageWriter source) {};


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
