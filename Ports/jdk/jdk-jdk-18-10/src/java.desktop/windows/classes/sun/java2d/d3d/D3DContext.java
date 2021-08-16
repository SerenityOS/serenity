/*
 * Copyright (c) 2007, 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.annotation.Native;

import sun.java2d.pipe.BufferedContext;
import sun.java2d.pipe.RenderBuffer;
import sun.java2d.pipe.RenderQueue;
import sun.java2d.pipe.hw.ContextCapabilities;

import static sun.java2d.pipe.BufferedOpCodes.INVALIDATE_CONTEXT;
import static sun.java2d.pipe.BufferedOpCodes.SET_SCRATCH_SURFACE;

/**
 * Note that the RenderQueue lock must be acquired before calling any of
 * the methods in this class.
 */
final class D3DContext extends BufferedContext {

    private final D3DGraphicsDevice device;

    D3DContext(RenderQueue rq, D3DGraphicsDevice device) {
        super(rq);
        this.device = device;
    }

    /**
     * Invalidates the currentContext field to ensure that we properly
     * revalidate the D3DContext (make it current, etc.) next time through
     * the validate() method.  This is typically invoked from methods
     * that affect the current context state (e.g. disposing a context or
     * surface).
     */
    static void invalidateCurrentContext() {
        // assert D3DRenderQueue.getInstance().lock.isHeldByCurrentThread();

        // invalidate the current Java-level context so that we
        // revalidate everything the next time around
        if (currentContext != null) {
            currentContext.invalidateContext();
            currentContext = null;
        }

        // invalidate the context reference at the native level, and
        // then flush the queue so that we have no pending operations
        // dependent on the current context
        D3DRenderQueue rq = D3DRenderQueue.getInstance();
        rq.ensureCapacity(4);
        rq.getBuffer().putInt(INVALIDATE_CONTEXT);
        rq.flushNow();
    }

    /**
     * Sets the current context on the native level to be the one passed as
     * the argument.
     * If the context is not the same as the defaultContext the latter
     * will be reset to null.
     *
     * This call is needed when copying from a SW surface to a Texture
     * (the upload test) or copying from d3d to SW surface to make sure we
     * have the correct current context.
     *
     * @param d3dc the context to be made current on the native level
     */
    static void setScratchSurface(D3DContext d3dc) {
        // assert D3DRenderQueue.getInstance().lock.isHeldByCurrentThread();

        // invalidate the current context
        if (d3dc != currentContext) {
            currentContext = null;
        }

        // set the scratch context
        D3DRenderQueue rq = D3DRenderQueue.getInstance();
        RenderBuffer buf = rq.getBuffer();
        rq.ensureCapacity(8);
        buf.putInt(SET_SCRATCH_SURFACE);
        buf.putInt(d3dc.getDevice().getScreen());
    }

    D3DGraphicsDevice getDevice() {
        return device;
    }

    static class D3DContextCaps extends ContextCapabilities {
        /**
         * Indicates the presence of pixel shaders (v2.0 or greater).
         * This cap will only be set if the hardware supports the minimum number
         * of texture units.
         */
    @Native static final int CAPS_LCD_SHADER       = (FIRST_PRIVATE_CAP << 0);
        /**
         * Indicates the presence of pixel shaders (v2.0 or greater).
         * This cap will only be set if the hardware meets our
         * minimum requirements.
         */
    @Native static final int CAPS_BIOP_SHADER      = (FIRST_PRIVATE_CAP << 1);
        /**
         * Indicates that the device was successfully initialized and can
         * be safely used.
         */
    @Native static final int CAPS_DEVICE_OK        = (FIRST_PRIVATE_CAP << 2);
        /**
         * Indicates that the device has all of the necessary capabilities
         * to support the Antialiasing Pixel Shader program.
         */
    @Native static final int CAPS_AA_SHADER        = (FIRST_PRIVATE_CAP << 3);

        D3DContextCaps(int caps, String adapterId) {
            super(caps, adapterId);
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder(super.toString());
            if ((caps & CAPS_LCD_SHADER) != 0) {
                buf.append("CAPS_LCD_SHADER|");
            }
            if ((caps & CAPS_BIOP_SHADER) != 0) {
                buf.append("CAPS_BIOP_SHADER|");
            }
            if ((caps & CAPS_AA_SHADER) != 0) {
                buf.append("CAPS_AA_SHADER|");
            }
            if ((caps & CAPS_DEVICE_OK) != 0) {
                buf.append("CAPS_DEVICE_OK|");
            }
            return buf.toString();
        }
    }
}
