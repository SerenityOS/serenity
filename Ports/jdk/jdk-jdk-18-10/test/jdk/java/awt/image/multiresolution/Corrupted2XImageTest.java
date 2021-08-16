/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8142406
 * @author a.stepanov
 * @summary [HiDPI] [macosx] check that for a pair of images
 *          (image.ext, image@2x.ext) the 1st one is loaded
 *          in case if the 2nd is corrupted
 *
 * @requires (os.family == "mac")
 *
 * @library /lib/client/
 * @build ExtendedRobot
 * @run main Corrupted2XImageTest
 */

import java.awt.*;
import java.awt.image.*;
import java.io.*;

import javax.imageio.ImageIO;

public class Corrupted2XImageTest extends Frame {

    private static final int SZ = 200;
    private static final Color C = Color.BLUE;

    private final String format, name1x, name2x;

    public Corrupted2XImageTest(String format) throws IOException {

        this.format = format;
        name1x = "test." + format;
        name2x = "test@2x." + format;
        createFiles();
    }

    private void UI() {

        setTitle(format);
        setSize(SZ, SZ);
        setResizable(false);
        setLocation(50, 50);
        setVisible(true);
    }

    @Override
    public void paint(Graphics g) {

        Image img = Toolkit.getDefaultToolkit().getImage(
            new File(name1x).getAbsolutePath());
        g.drawImage(img, 0, 0, this);
    }

    private void createFiles() throws IOException {

        BufferedImage img =
            new BufferedImage(SZ, SZ, BufferedImage.TYPE_INT_RGB);
        Graphics g = img.getGraphics();
        g.setColor(C);
        g.fillRect(0, 0, SZ, SZ);
        ImageIO.write(img, format, new File(name1x));

        // corrupted @2x "image" - just a text file
        Writer writer = new BufferedWriter(new OutputStreamWriter(
            new FileOutputStream(new File(name2x)), "utf-8"));
        writer.write("corrupted \"image\"");
        writer.close();
    }

    // need this for jpg
    private static boolean cmpColors(Color c1, Color c2) {

        int tol = 10;
        return (
            Math.abs(c2.getRed()   - c1.getRed()  ) < tol &&
            Math.abs(c2.getGreen() - c1.getGreen()) < tol &&
            Math.abs(c2.getBlue()  - c1.getBlue() ) < tol);
    }

    private void doTest() throws Exception {

        ExtendedRobot r = new ExtendedRobot();
        System.out.println("format: " + format);
        r.waitForIdle(1000);
        EventQueue.invokeAndWait(this::UI);
        r.waitForIdle(1000);
        Point loc = getLocationOnScreen();
        Color c = r.getPixelColor(loc.x + SZ / 2, loc.y + SZ / 2);
        if (!cmpColors(c, C)) {
            throw new RuntimeException("test failed, color = " + c); }
        System.out.println("ok");
        dispose();
    }

    public static void main(String[] args) throws Exception {

        // formats supported by Toolkit.getImage()
        for (String format : new String[]{"gif", "jpg", "png"}) {
            (new Corrupted2XImageTest(format)).doTest();
        }
    }
}
