/*
 * Copyright (c) 2007, 2013, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.pipe;

import java.awt.AlphaComposite;
import java.awt.Composite;
import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;
import sun.java2d.loops.CompositeType;
import sun.java2d.loops.MaskFill;
import sun.java2d.loops.SurfaceType;
import static sun.java2d.pipe.BufferedOpCodes.*;

/**
 * The MaskFill operation is expressed as:
 *   dst = ((src <MODE> dst) * pathA) + (dst * (1 - pathA))
 *
 * The OGL/D3D implementation of the MaskFill operation differs from the above
 * equation because it is not possible to perform such a complex operation in
 * OpenGL/Direct3D (without the use of advanced techniques like fragment
 * shaders and multitexturing).  Therefore, the BufferedMaskFill operation
 * is expressed as:
 *   dst = (src * pathA) <SrcOver> dst
 *
 * This simplified formula is only equivalent to the "true" MaskFill equation
 * in the following situations:
 *   - <MODE> is SrcOver
 *   - <MODE> is Src, extra alpha == 1.0, and the source paint is opaque
 *
 * Therefore, we register BufferedMaskFill primitives for only the SurfaceType
 * and CompositeType restrictions mentioned above.  In addition, for the
 * SrcNoEa case we must override the incoming composite with a SrcOver (no
 * extra alpha) instance, so that we set up the OpenGL/Direct3D blending
 * mode to match the BufferedMaskFill equation.
 */
public abstract class BufferedMaskFill extends MaskFill {

    protected final RenderQueue rq;

    protected BufferedMaskFill(RenderQueue rq,
                               SurfaceType srcType,
                               CompositeType compType,
                               SurfaceType dstType)
    {
        super(srcType, compType, dstType);
        this.rq = rq;
    }

    @Override
    public void MaskFill(SunGraphics2D sg2d, SurfaceData sData,
                         Composite comp,
                         final int x, final int y, final int w, final int h,
                         final byte[] mask,
                         final int maskoff, final int maskscan)
    {
        AlphaComposite acomp = (AlphaComposite)comp;
        if (acomp.getRule() != AlphaComposite.SRC_OVER) {
            comp = AlphaComposite.SrcOver;
        }

        rq.lock();
        try {
            validateContext(sg2d, comp, BufferedContext.USE_MASK);

            // we adjust the mask length so that the mask ends on a
            // 4-byte boundary
            int maskBytesRequired;
            if (mask != null) {
                // we adjust the mask length so that the mask ends on a
                // 4-byte boundary
                maskBytesRequired = (mask.length + 3) & (~3);
            } else {
                // mask not needed
                maskBytesRequired = 0;
            }
            int totalBytesRequired = 32 + maskBytesRequired;

            RenderBuffer buf = rq.getBuffer();
            if (totalBytesRequired <= buf.capacity()) {
                if (totalBytesRequired > buf.remaining()) {
                    // process the queue first and then enqueue the mask
                    rq.flushNow();
                }

                buf.putInt(MASK_FILL);
                // enqueue parameters
                buf.putInt(x).putInt(y).putInt(w).putInt(h);
                buf.putInt(maskoff);
                buf.putInt(maskscan);
                buf.putInt(maskBytesRequired);
                if (mask != null) {
                    // enqueue the mask
                    int padding = maskBytesRequired - mask.length;
                    buf.put(mask);
                    if (padding != 0) {
                        buf.position(buf.position() + padding);
                    }
                }
            } else {
                // queue is too small to accommodate entire mask; perform
                // the operation directly on the queue flushing thread
                rq.flushAndInvokeNow(new Runnable() {
                    public void run() {
                        maskFill(x, y, w, h,
                                 maskoff, maskscan, mask.length, mask);
                    }
                });
            }
        } finally {
            rq.unlock();
        }
    }

    /**
     * Called as a separate Runnable when the operation is too large to fit
     * on the RenderQueue.  The OGL/D3D pipelines each have their own (small)
     * native implementation of this method.
     */
    protected abstract void maskFill(int x, int y, int w, int h,
                                     int maskoff, int maskscan, int masklen,
                                     byte[] mask);

    /**
     * Validates the state in the provided SunGraphics2D object and sets up
     * any special resources for this operation (e.g. enabling gradient
     * shading).
     */
    protected abstract void validateContext(SunGraphics2D sg2d,
                                            Composite comp, int ctxflags);
}
