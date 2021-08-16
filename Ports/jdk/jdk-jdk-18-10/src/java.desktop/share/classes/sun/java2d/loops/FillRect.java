/*
 * Copyright (c) 1997, 2021, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @author Charlton Innovations, Inc.
 */

package sun.java2d.loops;

import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;

/**
 *   FillRect
 *   1) draw solid color rectangle onto destination surface
 *   2) must accept output area [x, y, dx, dy]
 *      from within the surface description data for clip rect
 */
public class FillRect extends GraphicsPrimitive
{
    public static final String methodSignature = "FillRect(...)".toString();

    public static final int primTypeID = makePrimTypeID();

    public static FillRect locate(SurfaceType srctype,
                                  CompositeType comptype,
                                  SurfaceType dsttype)
    {
        return (FillRect)
            GraphicsPrimitiveMgr.locate(primTypeID,
                                        srctype, comptype, dsttype);
    }

    protected FillRect(SurfaceType srctype,
                       CompositeType comptype,
                       SurfaceType dsttype)
    {
        super(methodSignature, primTypeID, srctype, comptype, dsttype);
    }

    public FillRect(long pNativePrim,
                    SurfaceType srctype,
                    CompositeType comptype,
                    SurfaceType dsttype)
    {
        super(pNativePrim, methodSignature, primTypeID, srctype, comptype, dsttype);
    }

    /**
     *   All FillRect implementors must have this invoker method
     */
    public native void FillRect(SunGraphics2D sg2d, SurfaceData dest,
                                int x, int y, int w, int h);

    static {
        GraphicsPrimitiveMgr.registerGeneral(new FillRect(null, null, null));
    }

    protected GraphicsPrimitive makePrimitive(SurfaceType srctype,
                                              CompositeType comptype,
                                              SurfaceType dsttype)
    {
        return new General(srctype, comptype, dsttype);
    }

    public static class General extends FillRect {
        public MaskFill fillop;

        public General(SurfaceType srctype,
                       CompositeType comptype,
                       SurfaceType dsttype)
        {
            super(srctype, comptype, dsttype);
            fillop = MaskFill.locate(srctype, comptype, dsttype);
        }

        public void FillRect(SunGraphics2D sg2d, SurfaceData dest,
                             int x, int y, int w, int h)
        {
            fillop.MaskFill(sg2d, dest, sg2d.composite, x, y, w, h, null, 0, 0);
        }
    }

    public GraphicsPrimitive traceWrap() {
        return new TraceFillRect(this);
    }

    private static class TraceFillRect extends FillRect {
        FillRect target;

        public TraceFillRect(FillRect target) {
            super(target.getSourceType(),
                  target.getCompositeType(),
                  target.getDestType());
            this.target = target;
        }

        public GraphicsPrimitive traceWrap() {
            return this;
        }

        public void FillRect(SunGraphics2D sg2d, SurfaceData dest,
                             int x, int y, int w, int h)
        {
            tracePrimitive(target);
            target.FillRect(sg2d, dest, x, y, w, h);
        }
    }
}
