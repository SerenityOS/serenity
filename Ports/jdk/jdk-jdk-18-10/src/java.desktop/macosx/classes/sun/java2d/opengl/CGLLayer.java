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
import sun.awt.CGraphicsConfig;
import sun.java2d.NullSurfaceData;
import sun.lwawt.LWWindowPeer;
import sun.java2d.SurfaceData;
import sun.lwawt.macosx.CFLayer;

public class CGLLayer extends CFLayer {

    private native long nativeCreateLayer();
    private static native void nativeSetScale(long layerPtr, double scale);
    private static native void validate(long layerPtr, CGLSurfaceData cglsd);
    private static native void blitTexture(long layerPtr);

    private int scale = 1;

    public CGLLayer(LWWindowPeer peer) {
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
        // and blits the buffer to the layer surface (in drawInCGLContext callback)
        CGraphicsConfig gc = (CGraphicsConfig)getGraphicsConfiguration();
        surfaceData = gc.createSurfaceData(this);
        setScale(gc.getDevice().getScaleFactor());
        // the layer holds a reference to the buffer, which in
        // turn has a reference back to this layer
        if (surfaceData instanceof CGLSurfaceData) {
            validate((CGLSurfaceData)surfaceData);
        }

        return surfaceData;
    }

    public void validate(final CGLSurfaceData cglsd) {
        OGLRenderQueue rq = OGLRenderQueue.getInstance();
        rq.lock();
        try {
            execute(ptr -> validate(ptr, cglsd));
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

    private void drawInCGLContext() {
        // tell the flusher thread not to update the intermediate buffer
        // until we are done blitting from it
        OGLRenderQueue rq = OGLRenderQueue.getInstance();
        rq.lock();
        try {
            execute(ptr -> blitTexture(ptr));
        } finally {
            rq.unlock();
        }
    }
}
