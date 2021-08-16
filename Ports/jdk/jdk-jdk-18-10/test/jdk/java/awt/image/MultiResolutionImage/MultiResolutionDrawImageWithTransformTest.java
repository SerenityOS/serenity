/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.BaseMultiResolutionImage;
import static java.awt.RenderingHints.KEY_RESOLUTION_VARIANT;
import static java.awt.RenderingHints.VALUE_RESOLUTION_VARIANT_SIZE_FIT;
import java.awt.geom.AffineTransform;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import sun.java2d.StateTrackable;
import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;
import sun.java2d.loops.SurfaceType;

/**
 * @test
 * @bug 8073320 8198390
 * @summary Windows HiDPI support
 * @modules java.desktop/sun.java2d java.desktop/sun.java2d.loops
 * @run main MultiResolutionDrawImageWithTransformTest
 */
public class MultiResolutionDrawImageWithTransformTest {

    private static final int SCREEN_SIZE = 400;
    private static final int IMAGE_SIZE = SCREEN_SIZE / 4;
    private static final Color BACKGROUND_COLOR = Color.PINK;
    private static final Color[] COLORS = {
        Color.CYAN, Color.GREEN, Color.BLUE, Color.ORANGE
    };

    public static void main(String[] args) throws Exception {

        int length = COLORS.length;
        BufferedImage[] resolutionVariants = new BufferedImage[length];
        for (int i = 0; i < length; i++) {
            resolutionVariants[i] = createRVImage(getSize(i), COLORS[i]);
        }

        BaseMultiResolutionImage mrImage = new BaseMultiResolutionImage(
                resolutionVariants);

        // scale 1, transform 1, resolution variant 1
        Color color = getImageColor(mrImage, 1, 1);
        if (!getColorForScale(1).equals(color)) {
            throw new RuntimeException("Wrong resolution variant!");
        }

        // scale 1, transform 2, resolution variant 2
        color = getImageColor(mrImage, 1, 2);
        if (!getColorForScale(2).equals(color)) {
            throw new RuntimeException("Wrong resolution variant!");
        }

        // scale 2, transform 1, resolution variant 2
        color = getImageColor(mrImage, 2, 1);
        if (!getColorForScale(2).equals(color)) {
            throw new RuntimeException("Wrong resolution variant!");
        }

        // scale 2, transform 2, resolution variant 4
        color = getImageColor(mrImage, 2, 2);
        if (!getColorForScale(4).equals(color)) {
            throw new RuntimeException("Wrong resolution variant!");
        }
    }

    private static Color getColorForScale(int scale) {
        return COLORS[scale - 1];
    }

    private static Color getImageColor(Image image, double configScale,
            double transformScale) {

        TestSurfaceData surface = new TestSurfaceData(SCREEN_SIZE, SCREEN_SIZE,
                configScale);
        SunGraphics2D g2d = new SunGraphics2D(surface,
                Color.BLACK, Color.BLACK, null);
        g2d.setRenderingHint(KEY_RESOLUTION_VARIANT,
                VALUE_RESOLUTION_VARIANT_SIZE_FIT);
        AffineTransform tx = AffineTransform.getScaleInstance(transformScale,
                transformScale);
        g2d.drawImage(image, tx, null);
        g2d.dispose();

        int backgroundX = (int) (1.5 * image.getWidth(null) * transformScale);
        int backgroundY = (int) (1.5 * image.getHeight(null) * transformScale);
        Color backgroundColor = surface.getColor(backgroundX, backgroundY);
        //surface.show(String.format("Config: %f, transform: %f", configScale, transformScale));
        if (!BACKGROUND_COLOR.equals(backgroundColor)) {
            throw new RuntimeException("Wrong background color!");
        }
        return surface.getColor(IMAGE_SIZE / 4, IMAGE_SIZE / 4);
    }

    private static int getSize(int i) {
        return (i + 1) * IMAGE_SIZE;
    }

    private static BufferedImage createRVImage(int size, Color color) {
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, size, size);
        g.dispose();
        return image;
    }

    static class TestGraphicsConfig extends GraphicsConfiguration {

        private final double scale;

        TestGraphicsConfig(double scale) {
            this.scale = scale;
        }

        @Override
        public GraphicsDevice getDevice() {
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
            return AffineTransform.getScaleInstance(scale, scale);
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

    static class TestSurfaceData extends SurfaceData {

        private final int width;
        private final int height;
        private final GraphicsConfiguration gc;
        private final BufferedImage buffImage;
        private final double scale;

        public TestSurfaceData(int width, int height, double scale) {
            super(StateTrackable.State.DYNAMIC, SurfaceType.Custom, ColorModel.getRGBdefault());
            this.scale = scale;
            gc = new TestGraphicsConfig(scale);
            this.width = (int) Math.ceil(scale * width);
            this.height = (int) Math.ceil(scale * height);
            buffImage = new BufferedImage(this.width, this.height,
                    BufferedImage.TYPE_INT_RGB);

            Graphics imageGraphics = buffImage.createGraphics();
            imageGraphics.setColor(BACKGROUND_COLOR);
            imageGraphics.fillRect(0, 0, this.width, this.height);
            imageGraphics.dispose();
        }

        Color getColor(int x, int y) {
            int sx = (int) Math.ceil(x * scale);
            int sy = (int) Math.ceil(y * scale);
            return new Color(buffImage.getRGB(sx, sy));
        }

        @Override
        public SurfaceData getReplacement() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public GraphicsConfiguration getDeviceConfiguration() {
            return gc;
        }

        @Override
        public Raster getRaster(int x, int y, int w, int h) {
            return buffImage.getRaster();
        }

        @Override
        public Rectangle getBounds() {
            return new Rectangle(0, 0, width, height);
        }

        @Override
        public Object getDestination() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        private void show(String title) {
            Frame frame = new Frame() {

                @Override
                public void paint(Graphics g) {
                    super.paint(g);
                    g.drawImage(buffImage, 0, 0, this);
                    g.setColor(Color.GRAY);
                    g.drawRect(0, 0, width, height);
                    g.drawRect(0, height / 2, width, height / 2);
                    g.drawRect(width / 2, 0, width / 2, height);
                }
            };
            frame.setTitle(title);
            frame.setSize(width, height);
            frame.setVisible(true);
        }
    }
}
