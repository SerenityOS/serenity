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
 * @test
 * @bug 8212226
 * @summary Check that base image gets selected during painting
 *          if other resolution variants are not ready
 * @run main MultiResolutionImageSelectionTest
 */

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.AbstractMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.awt.image.ImageProducer;
import java.awt.image.MultiResolutionImage;
import java.util.Arrays;
import java.util.List;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;

public class MultiResolutionImageSelectionTest {
    final static BufferedImage GOOD_IMAGE = new BufferedImage(200, 200,
            TYPE_INT_RGB);

    final static Image BAD_IMAGE= new Image() {
        @Override
        public int getWidth(ImageObserver observer) {
            return -1;
        }

        @Override
        public int getHeight(ImageObserver observer) {
            return 1;
        }

        @Override
        public ImageProducer getSource() {
            return null;
        }

        @Override
        public Graphics getGraphics() {
            return null;
        }

        @Override
        public Object getProperty(String name, ImageObserver observer) {
            return null;
        }
    };

    public static void main(String[] args) {
        Graphics g = GOOD_IMAGE.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, GOOD_IMAGE.getWidth(), GOOD_IMAGE.getHeight());
        g.dispose();

        MultiResolutionImage mri = new AbstractMultiResolutionImage() {
            @Override
            protected Image getBaseImage() {
                return GOOD_IMAGE;
            }

            @Override
            public Image getResolutionVariant(double destImageWidth, double destImageHeight) {
                if ((int)destImageHeight == 200) {
                    return GOOD_IMAGE;
                } else {
                    return BAD_IMAGE;
                }
            }

            @Override
            public List<Image> getResolutionVariants() {
                return Arrays.asList(BAD_IMAGE, GOOD_IMAGE, BAD_IMAGE);
            }
        };

        BufferedImage target = new BufferedImage(500, 500, TYPE_INT_RGB);
        Graphics2D g2d = target.createGraphics();
        g2d.drawImage((Image) mri, 0, 0, 500, 500, null);
        g2d.dispose();
        if (Color.RED.getRGB() != target.getRGB(1, 1)) {
            throw new RuntimeException("Wrong resolution variant was used");
        }
    }
}
