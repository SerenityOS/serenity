/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6705443 8198613
 * @summary tests that we don't crash during exit if font strikes were disposed
 * during the lifetime of the application
 *
 * @run main/othervm -Dsun.java2d.font.reftype=weak StrikeDisposalCrashTest
 * @run main/othervm -Dsun.java2d.font.reftype=weak -Dsun.java2d.noddraw=true StrikeDisposalCrashTest
 */

import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.VolatileImage;

public class StrikeDisposalCrashTest {

    public static void main(String[] args) {
        System.setProperty("sun.java2d.font.reftype", "weak");

        GraphicsDevice gd[] =
            GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();

        Frame frames[] = new Frame[gd.length];
        for (int i = 0; i < frames.length; i++) {
            GraphicsConfiguration gc = gd[i].getDefaultConfiguration();
            Frame f = new Frame("Frame on "+gc, gc);
            f.setSize(100, 100);
            f.setLocation(gc.getBounds().x, gc.getBounds().y);
            f.pack();
            frames[i] = f;
        }

        Font f1 = new Font("Dialog", Font.PLAIN, 10);
        Font f2 = new Font("Dialog", Font.ITALIC, 12);

        for (int i = 0; i < frames.length/2; i++) {
            // making sure the glyphs are cached in the accel. cache on
            // one frame, then the other
            renderText(frames[i], f1);
            renderText(frames[frames.length -1 - i], f1);

            // and now the other way around, with different glyphs
            renderText(frames[frames.length -1 - i], f2);
            renderText(frames[i], f2);
        }

        // try to force strike disposal (note that we have to use
        // -Dsun.java2d.font.reftype=weak to facilitate the disposal)

        System.gc();
        System.runFinalization();
        System.gc();
        System.runFinalization();

        for (Frame f : frames) {
            f.dispose();
        }

        System.err.println("Exiting. If the test crashed after this it FAILED");
    }

    private static final String text =
        "The quick brown fox jumps over the lazy dog 1234567890";
    private static void renderText(Frame frame, Font f1) {
        VolatileImage vi = frame.createVolatileImage(256, 32);
        vi.validate(frame.getGraphicsConfiguration());

        Graphics2D g = vi.createGraphics();
        g.setFont(f1);
        g.drawString(text, 0, vi.getHeight()/2);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                           RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.drawString(text, 0, vi.getHeight()/2);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                           RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
        g.drawString(text, 0, vi.getHeight()/2);
        Toolkit.getDefaultToolkit().sync();
    }
}
