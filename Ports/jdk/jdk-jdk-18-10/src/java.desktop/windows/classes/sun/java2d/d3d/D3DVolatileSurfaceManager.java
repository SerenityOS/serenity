/*
 * Copyright (c) 2007, 2015, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.d3d;

import java.awt.Component;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.ColorModel;

import sun.awt.AWTAccessor;
import sun.awt.AWTAccessor.ComponentAccessor;
import sun.awt.Win32GraphicsConfig;
import sun.awt.image.SunVolatileImage;
import sun.awt.image.SurfaceManager;
import sun.awt.image.VolatileSurfaceManager;
import sun.awt.windows.WComponentPeer;
import sun.java2d.InvalidPipeException;
import sun.java2d.SurfaceData;
import static sun.java2d.pipe.hw.AccelSurface.*;
import static sun.java2d.d3d.D3DContext.D3DContextCaps.*;
import sun.java2d.windows.GDIWindowSurfaceData;

public class D3DVolatileSurfaceManager
    extends VolatileSurfaceManager
{
    private boolean accelerationEnabled;
    private int restoreCountdown;

    public D3DVolatileSurfaceManager(SunVolatileImage vImg, Object context) {
        super(vImg, context);

        /*
         * We will attempt to accelerate this image only under the
         * following conditions:
         *   - the image is opaque OR
         *   - the image is translucent AND
         *       - the GraphicsConfig supports the FBO extension OR
         *       - the GraphicsConfig has a stored alpha channel
         */
        int transparency = vImg.getTransparency();
        D3DGraphicsDevice gd = (D3DGraphicsDevice)
            vImg.getGraphicsConfig().getDevice();
        accelerationEnabled =
            (transparency == Transparency.OPAQUE) ||
            (transparency == Transparency.TRANSLUCENT &&
             (gd.isCapPresent(CAPS_RT_PLAIN_ALPHA) ||
              gd.isCapPresent(CAPS_RT_TEXTURE_ALPHA)));
    }

    protected boolean isAccelerationEnabled() {
        return accelerationEnabled;
    }
    public void setAccelerationEnabled(boolean accelerationEnabled) {
        this.accelerationEnabled = accelerationEnabled;
    }

    /**
     * Create a pbuffer-based SurfaceData object (or init the backbuffer
     * of an existing window if this is a double buffered GraphicsConfig).
     */
    protected SurfaceData initAcceleratedSurface() {
        SurfaceData sData;
        Component comp = vImg.getComponent();
        final ComponentAccessor acc = AWTAccessor.getComponentAccessor();
        WComponentPeer peer = (comp != null) ? acc.getPeer(comp) : null;

        try {
            boolean forceback = false;
            if (context instanceof Boolean) {
                forceback = ((Boolean)context).booleanValue();
            }

            if (forceback) {
                // peer must be non-null in this case
                sData = D3DSurfaceData.createData(peer, vImg);
            } else {
                D3DGraphicsConfig gc =
                    (D3DGraphicsConfig)vImg.getGraphicsConfig();
                ColorModel cm = gc.getColorModel(vImg.getTransparency());
                int type = vImg.getForcedAccelSurfaceType();
                // if acceleration type is forced (type != UNDEFINED) then
                // use the forced type, otherwise use RT_TEXTURE
                if (type == UNDEFINED) {
                    type = RT_TEXTURE;
                }
                sData = D3DSurfaceData.createData(gc,
                                                  vImg.getWidth(),
                                                  vImg.getHeight(),
                                                  cm, vImg,
                                                  type);
            }
        } catch (NullPointerException ex) {
            sData = null;
        } catch (OutOfMemoryError er) {
            sData = null;
        } catch (InvalidPipeException ipe) {
            sData = null;
        }

        return sData;
    }

    protected boolean isConfigValid(GraphicsConfiguration gc) {
        return ((gc == null) || (gc == vImg.getGraphicsConfig()));
    }

    /**
     * Set the number of iterations for restoreAcceleratedSurface to fail
     * before attempting to restore the accelerated surface.
     *
     * @see #restoreAcceleratedSurface
     * @see #handleVItoScreenOp
     */
    private synchronized void setRestoreCountdown(int count) {
        restoreCountdown = count;
    }

    /**
     * Note that we create a new surface instead of restoring
     * an old one. This will help with D3DContext revalidation.
     */
    @Override
    protected void restoreAcceleratedSurface() {
        synchronized (this) {
            if (restoreCountdown > 0) {
                restoreCountdown--;
                throw new
                    InvalidPipeException("Will attempt to restore surface " +
                                          " in " + restoreCountdown);
            }
        }

        SurfaceData sData = initAcceleratedSurface();
        if (sData != null) {
            sdAccel = sData;
        } else {
            throw new InvalidPipeException("could not restore surface");
            // REMIND: alternatively, we could try this:
//            ((D3DSurfaceData)sdAccel).restoreSurface();
        }
    }

    /**
     * We're asked to restore contents by the accelerated surface, which means
     * that it had been lost.
     */
    @Override
    public SurfaceData restoreContents() {
        acceleratedSurfaceLost();
        return super.restoreContents();
    }

    /**
     * If the destination surface's peer can potentially handle accelerated
     * on-screen rendering then it is likely that the condition which resulted
     * in VI to Screen operation is temporary, so this method sets the
     * restore countdown in hope that the on-screen accelerated rendering will
     * resume. In the meantime the backup surface of the VISM will be used.
     *
     * The countdown is needed because otherwise we may never break out
     * of "do { vi.validate()..} while(vi.lost)" loop since validate() could
     * restore the source surface every time and it will get lost again on the
     * next copy attempt, and we would never get a chance to use the backup
     * surface. By using the countdown we allow the backup surface to be used
     * while the screen surface gets sorted out, or if it for some reason can
     * never be restored.
     *
     * If the destination surface's peer could never do accelerated onscreen
     * rendering then the acceleration for the SurfaceManager associated with
     * the source surface is disabled forever.
     */
    static void handleVItoScreenOp(SurfaceData src, SurfaceData dst) {
        if (src instanceof D3DSurfaceData &&
            dst instanceof GDIWindowSurfaceData)
        {
            D3DSurfaceData d3dsd = (D3DSurfaceData)src;
            SurfaceManager mgr =
                SurfaceManager.getManager((Image)d3dsd.getDestination());
            if (mgr instanceof D3DVolatileSurfaceManager) {
                D3DVolatileSurfaceManager vsm = (D3DVolatileSurfaceManager)mgr;
                if (vsm != null) {
                    d3dsd.setSurfaceLost(true);

                    GDIWindowSurfaceData wsd = (GDIWindowSurfaceData)dst;
                    WComponentPeer p = wsd.getPeer();
                    if (D3DScreenUpdateManager.canUseD3DOnScreen(p,
                            (Win32GraphicsConfig)p.getGraphicsConfiguration(),
                            p.getBackBuffersNum()))
                    {
                        // 10 is only chosen to be greater than the number of
                        // times a sane person would call validate() inside
                        // a validation loop, and to reduce thrashing between
                        // accelerated and backup surfaces
                        vsm.setRestoreCountdown(10);
                    } else {
                        vsm.setAccelerationEnabled(false);
                    }
                }
            }
        }
    }

    @Override
    public void initContents() {
        if (vImg.getForcedAccelSurfaceType() != TEXTURE) {
            super.initContents();
        }
    }
}
