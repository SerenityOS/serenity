/*
 * Copyright (c) 2009, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6822057 7124400 8059848 8198613
 *
 * @summary Test verifies that list of supported graphics configurations
 *          can not be changed via modification of elements of an array
 *          returned by getConfiguration() method.
 *
 * @run     main CloneConfigsTest
 * @run     main/othervm -Dsun.java2d.d3d=true CloneConfigsTest
 * @run     main/othervm -Dsun.java2d.noddraw=true CloneConfigsTest
 */

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;

public class CloneConfigsTest {

    public static void main(String[] args) {
        GraphicsEnvironment env =
                GraphicsEnvironment.getLocalGraphicsEnvironment();

        GraphicsDevice[] devices = env.getScreenDevices();

        GraphicsConfiguration c = new TestConfig();

        for (GraphicsDevice gd : devices) {
            System.out.println("Device: " + gd);

            GraphicsConfiguration[] configs = gd.getConfigurations();

            for (int i = 0; i < configs.length; i++) {
                GraphicsConfiguration gc  = configs[i];
                System.out.println("\tConfig: " + gc);

                configs[i] = c;
            }

            // verify whether array of configs was modified
            configs = gd.getConfigurations();
            for (GraphicsConfiguration gc : configs) {
                if (gc == c) {
                    throw new RuntimeException("Test failed.");
                }
            }
            System.out.println("Test passed.");
        }
    }

    private static class TestConfig extends GraphicsConfiguration {

        @Override
        public GraphicsDevice getDevice() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public BufferedImage createCompatibleImage(int width, int height) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ColorModel getColorModel() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public ColorModel getColorModel(int transparency) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public AffineTransform getDefaultTransform() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public AffineTransform getNormalizingTransform() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Rectangle getBounds() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

    }

}
