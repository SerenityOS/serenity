/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package sun.java2d.opengl;

import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.ColorModel;

import sun.java2d.SurfaceData;

public abstract class CGLSurfaceData extends OGLSurfaceData {

    private final int scale;
    final int width;
    final int height;
    private final CGLGraphicsConfig graphicsConfig;

    private native void initOps(OGLGraphicsConfig gc, long pConfigInfo,
                                long pPeerData, long layerPtr, int xoff,
                                int yoff, boolean isOpaque);

    private CGLSurfaceData(CGLLayer layer, CGLGraphicsConfig gc,
                           ColorModel cm, int type, int width, int height) {
        super(gc, cm, type);
        // TEXTURE shouldn't be scaled, it is used for managed BufferedImages.
        scale = type == TEXTURE ? 1 : gc.getDevice().getScaleFactor();
        this.width = width * scale;
        this.height = height * scale;
        this.graphicsConfig = gc;

        long layerPtr = 0L;
        boolean isOpaque = true;
        if (layer != null) {
            layerPtr = layer.getPointer();
            isOpaque = layer.isOpaque();
        }
        // TODO delete the native code: pPeerData, xoff, yoff are always zero
        initOps(gc, gc.getNativeConfigInfo(), 0, layerPtr, 0, 0, isOpaque);
    }

    @Override //SurfaceData
    public GraphicsConfiguration getDeviceConfiguration() {
        return graphicsConfig;
    }

    /**
     * Creates a SurfaceData object representing the intermediate buffer
     * between the Java2D flusher thread and the AppKit thread.
     */
    public static CGLLayerSurfaceData createData(CGLLayer layer) {
        CGLGraphicsConfig gc = (CGLGraphicsConfig) layer.getGraphicsConfiguration();
        Rectangle r = layer.getBounds();
        return new CGLLayerSurfaceData(layer, gc, r.width, r.height);
    }

    /**
     * Creates a SurfaceData object representing an off-screen buffer (either a
     * FBO or Texture).
     */
    public static CGLOffScreenSurfaceData createData(CGLGraphicsConfig gc,
            int width, int height, ColorModel cm, Image image, int type) {
        return new CGLOffScreenSurfaceData(gc, width, height, image, cm, type);
    }

    @Override
    public double getDefaultScaleX() {
        return scale;
    }

    @Override
    public double getDefaultScaleY() {
        return scale;
    }

    @Override
    public Rectangle getBounds() {
        return new Rectangle(width, height);
    }

    protected native void clearWindow();

    /**
     * A surface which implements an intermediate buffer between
     * the Java2D flusher thread and the AppKit thread.
     *
     * This surface serves as a buffer attached to a CGLLayer and
     * the layer redirects all painting to the buffer's graphics.
     */
    public static class CGLLayerSurfaceData extends CGLSurfaceData {

        private final CGLLayer layer;

        private CGLLayerSurfaceData(CGLLayer layer, CGLGraphicsConfig gc,
                                    int width, int height) {
            super(layer, gc, gc.getColorModel(), FBOBJECT, width, height);
            this.layer = layer;
            initSurface(this.width, this.height);
        }

        @Override
        public SurfaceData getReplacement() {
            return layer.getSurfaceData();
        }

        @Override
        boolean isOnScreen() {
            return true;
        }

        @Override
        public Object getDestination() {
            return layer.getDestination();
        }

        @Override
        public int getTransparency() {
            return layer.getTransparency();
        }

        @Override
        public void invalidate() {
            super.invalidate();
            clearWindow();
        }
    }

    /**
     * SurfaceData object representing an off-screen buffer (either a FBO or
     * Texture).
     */
    public static class CGLOffScreenSurfaceData extends CGLSurfaceData {

        private final Image offscreenImage;

        private CGLOffScreenSurfaceData(CGLGraphicsConfig gc, int width,
                                        int height, Image image,
                                        ColorModel cm, int type) {
            super(null, gc, cm, type, width, height);
            offscreenImage = image;
            initSurface(this.width, this.height);
        }

        @Override
        public SurfaceData getReplacement() {
            return restoreContents(offscreenImage);
        }

        /**
         * Returns destination Image associated with this SurfaceData.
         */
        @Override
        public Object getDestination() {
            return offscreenImage;
        }
    }
}
