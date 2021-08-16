/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.ColorModel;

import sun.awt.X11ComponentPeer;
import sun.java2d.SurfaceData;

public abstract class GLXSurfaceData extends OGLSurfaceData {

    protected X11ComponentPeer peer;
    private GLXGraphicsConfig graphicsConfig;

    private native void initOps(OGLGraphicsConfig gc, X11ComponentPeer peer,
                                long aData);

    protected GLXSurfaceData(X11ComponentPeer peer, GLXGraphicsConfig gc,
                             ColorModel cm, int type)
    {
        super(gc, cm, type);
        this.peer = peer;
        this.graphicsConfig = gc;
        initOps(gc, peer, graphicsConfig.getAData());
    }

    public GraphicsConfiguration getDeviceConfiguration() {
        return graphicsConfig;
    }

    /**
     * Creates a SurfaceData object representing the primary (front) buffer
     * of an on-screen Window.
     */
    public static GLXWindowSurfaceData createData(X11ComponentPeer peer) {
        GLXGraphicsConfig gc = getGC(peer);
        return new GLXWindowSurfaceData(peer, gc);
    }

    /**
     * Creates a SurfaceData object representing the back buffer of a
     * double-buffered on-screen Window.
     */
    public static GLXOffScreenSurfaceData createData(X11ComponentPeer peer,
                                                     Image image,
                                                     int type)
    {
        GLXGraphicsConfig gc = getGC(peer);
        Rectangle r = peer.getBounds();
        if (type == FLIP_BACKBUFFER) {
            return new GLXOffScreenSurfaceData(peer, gc, r.width, r.height,
                                               image, peer.getColorModel(),
                                               FLIP_BACKBUFFER);
        } else {
            return new GLXVSyncOffScreenSurfaceData(peer, gc, r.width, r.height,
                                                    image, peer.getColorModel(),
                                                    type);
        }
    }

    /**
     * Creates a SurfaceData object representing an off-screen buffer (either
     * a FBO or Texture).
     */
    public static GLXOffScreenSurfaceData createData(GLXGraphicsConfig gc,
                                                     int width, int height,
                                                     ColorModel cm,
                                                     Image image, int type)
    {
        return new GLXOffScreenSurfaceData(null, gc, width, height,
                                           image, cm, type);
    }

    public static GLXGraphicsConfig getGC(X11ComponentPeer peer) {
        if (peer != null) {
            return (GLXGraphicsConfig)peer.getGraphicsConfiguration();
        } else {
            // REMIND: this should rarely (never?) happen, but what if
            //         default config is not GLX?
            GraphicsEnvironment env =
                GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gd = env.getDefaultScreenDevice();
            return (GLXGraphicsConfig)gd.getDefaultConfiguration();
        }
    }

    public static class GLXWindowSurfaceData extends GLXSurfaceData {
        protected final int scale;

        public GLXWindowSurfaceData(X11ComponentPeer peer,
                                    GLXGraphicsConfig gc)
        {
            super(peer, gc, peer.getColorModel(), WINDOW);
            scale = gc.getScale();
        }

        public SurfaceData getReplacement() {
            return peer.getSurfaceData();
        }

        public Rectangle getBounds() {
            Rectangle r = peer.getBounds();
            r.x = r.y = 0;
            r.width = (int) Math.ceil(r.width * scale);
            r.height = (int) Math.ceil(r.height * scale);
            return r;
        }

        /**
         * Returns destination Component associated with this SurfaceData.
         */
        public Object getDestination() {
            return peer.getTarget();
        }

        @Override
        public double getDefaultScaleX() {
            return scale;
        }

        @Override
        public double getDefaultScaleY() {
            return scale;
        }
    }

    /**
     * A surface which implements a v-synced flip back-buffer with COPIED
     * FlipContents.
     *
     * This surface serves as a back-buffer to the outside world, while
     * it is actually an offscreen surface. When the BufferStrategy this surface
     * belongs to is showed, it is first copied to the real private
     * FLIP_BACKBUFFER, which is then flipped.
     */
    public static class GLXVSyncOffScreenSurfaceData extends
        GLXOffScreenSurfaceData
    {
        private GLXOffScreenSurfaceData flipSurface;

        public GLXVSyncOffScreenSurfaceData(X11ComponentPeer peer,
                                            GLXGraphicsConfig gc,
                                            int width, int height,
                                            Image image, ColorModel cm,
                                            int type)
        {
            super(peer, gc, width, height, image, cm, type);
            flipSurface = GLXSurfaceData.createData(peer, image, FLIP_BACKBUFFER);
        }

        public SurfaceData getFlipSurface() {
            return flipSurface;
        }

        @Override
        public void flush() {
            flipSurface.flush();
            super.flush();
        }

    }

    public static class GLXOffScreenSurfaceData extends GLXSurfaceData {

        private Image offscreenImage;
        private int width, height;
        private final int scale;

        public GLXOffScreenSurfaceData(X11ComponentPeer peer,
                                       GLXGraphicsConfig gc,
                                       int width, int height,
                                       Image image, ColorModel cm,
                                       int type)
        {
            super(peer, gc, cm, type);

            scale = gc.getDevice().getScaleFactor();
            this.width = width * scale;
            this.height = height * scale;
            offscreenImage = image;

            initSurface(this.width, this.height);
        }

        public SurfaceData getReplacement() {
            return restoreContents(offscreenImage);
        }

        public Rectangle getBounds() {
            if (type == FLIP_BACKBUFFER) {
                Rectangle r = peer.getBounds();
                r.x = r.y = 0;
                r.width = (int) Math.ceil(r.width * scale);
                r.height = (int) Math.ceil(r.height * scale);
                return r;
            } else {
                return new Rectangle(width, height);
            }
        }

        /**
         * Returns destination Image associated with this SurfaceData.
         */
        public Object getDestination() {
            return offscreenImage;
        }

        @Override
        public double getDefaultScaleX() {
            return scale;
        }

        @Override
        public double getDefaultScaleY() {
            return scale;
        }
    }
}
