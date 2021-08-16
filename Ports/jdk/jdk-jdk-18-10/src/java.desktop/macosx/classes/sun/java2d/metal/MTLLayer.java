/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.metal;

import sun.java2d.NullSurfaceData;
import sun.java2d.SurfaceData;
import sun.lwawt.LWWindowPeer;
import sun.lwawt.macosx.CFLayer;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;

public class MTLLayer extends CFLayer {

    private native long nativeCreateLayer();
    private static native void nativeSetScale(long layerPtr, double scale);

    // Pass the insets to native code to make adjustments in blitTexture
    private static native void nativeSetInsets(long layerPtr, int top, int left);
    private static native void validate(long layerPtr, MTLSurfaceData mtlsd);
    private static native void blitTexture(long layerPtr);

    private int scale = 1;

    public MTLLayer(LWWindowPeer peer) {
        super(0, true);

        setPtr(nativeCreateLayer());
        this.peer = peer;
    }

    public SurfaceData replaceSurfaceData() {
        if (getBounds().isEmpty()) {
            surfaceData = NullSurfaceData.theInstance;
            return surfaceData;
        }

        // the layer redirects all painting to the buffer's graphics
        // and blits the buffer to the layer surface (in display callback)
        MTLGraphicsConfig gc = (MTLGraphicsConfig)getGraphicsConfiguration();
        surfaceData = gc.createSurfaceData(this);
        setScale(gc.getDevice().getScaleFactor());
        if (peer != null) {
            Insets insets = peer.getInsets();
            execute(ptr -> nativeSetInsets(ptr, insets.top, insets.left));
        }
        // the layer holds a reference to the buffer, which in
        // turn has a reference back to this layer
        if (surfaceData instanceof MTLSurfaceData) {
            validate((MTLSurfaceData)surfaceData);
        }

        return surfaceData;
    }

    public void validate(final MTLSurfaceData mtlsd) {
        MTLRenderQueue rq = MTLRenderQueue.getInstance();
        rq.lock();
        try {
            execute(ptr -> validate(ptr, mtlsd));
        } finally {
            rq.unlock();
        }
    }

    @Override
    public void dispose() {
        // break the connection between the layer and the buffer
        validate(null);
        SurfaceData oldData = surfaceData;
        surfaceData = NullSurfaceData.theInstance;;
        if (oldData != null) {
            oldData.flush();
        }
        super.dispose();
    }

    private void setScale(final int _scale) {
        if (scale != _scale) {
            scale = _scale;
            execute(ptr -> nativeSetScale(ptr, scale));
        }
    }

    // ----------------------------------------------------------------------
    // NATIVE CALLBACKS
    // ----------------------------------------------------------------------

    private void drawInMTLContext() {
        // tell the flusher thread not to update the intermediate buffer
        // until we are done blitting from it
        MTLRenderQueue rq = MTLRenderQueue.getInstance();
        rq.lock();
        try {
            execute(ptr -> blitTexture(ptr));
        } finally {
            rq.unlock();
        }
    }
}
