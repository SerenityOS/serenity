/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import static java.awt.image.ImageObserver.*;
import java.io.File;
import javax.imageio.ImageIO;
/*
 * @test
 * @bug 8065627
 * @summary Animated GIFs fail to display on a HiDPI display
 * @author Alexander Scherbatiy
 * @run main MultiResolutionImageObserverTest
 */

public class MultiResolutionImageObserverTest {

    private static final int TIMEOUT = 2000;

    public static void main(String[] args) throws Exception {

        generateImages();
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        Image image = Toolkit.getDefaultToolkit().getImage(IMAGE_NAME_1X);

        LoadImageObserver sizeObserver
                = new LoadImageObserver(WIDTH | HEIGHT);
        toolkit.prepareImage(image, -1, -1, sizeObserver);
        waitForImageLoading(sizeObserver, "The first observer is not called");

        LoadImageObserver bitsObserver
                = new LoadImageObserver(SOMEBITS | FRAMEBITS | ALLBITS);

        BufferedImage buffImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = (Graphics2D) buffImage.createGraphics();
        g2d.scale(2, 2);
        g2d.drawImage(image, 0, 0, bitsObserver);
        waitForImageLoading(bitsObserver, "The second observer is not called!");
        g2d.dispose();
    }

    private static void waitForImageLoading(LoadImageObserver observer,
            String errorMessage) throws Exception {

        long endTime = System.currentTimeMillis() + TIMEOUT;

        while (!observer.loaded && System.currentTimeMillis() < endTime) {
            Thread.sleep(TIMEOUT / 100);
        }

        if (!observer.loaded) {
            throw new RuntimeException(errorMessage);
        }
    }

    private static final String IMAGE_NAME_1X = "image.png";
    private static final String IMAGE_NAME_2X = "image@2x.png";

    private static void generateImages() throws Exception {
        generateImage(1);
        generateImage(2);
    }

    private static void generateImage(int scale) throws Exception {
        BufferedImage image = new BufferedImage(
                scale * 200, scale * 300,
                BufferedImage.TYPE_INT_RGB);
        Graphics g = image.createGraphics();
        g.setColor(scale == 1 ? Color.GREEN : Color.BLUE);
        g.fillRect(0, 0, scale * 200, scale * 300);
        File file = new File(scale == 1 ? IMAGE_NAME_1X : IMAGE_NAME_2X);
        ImageIO.write(image, "png", file);
        g.dispose();
    }

    private static class LoadImageObserver implements ImageObserver {

        private final int infoflags;
        private volatile boolean loaded;

        public LoadImageObserver(int flags) {
            this.infoflags = flags;
        }

        @Override
        public boolean imageUpdate(Image img, int flags, int x, int y, int width, int height) {

            if ((flags & infoflags) != 0) {
                loaded = true;
            }

            return !loaded;
        }
    }
}
