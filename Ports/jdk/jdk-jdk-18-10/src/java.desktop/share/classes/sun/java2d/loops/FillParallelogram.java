/*
 * Copyright (c) 2008, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @author Jim Graham
 */

package sun.java2d.loops;

import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;

/**
 * FillParallelogram
 * 1) fill the area between the 4 edges of a parallelogram
 *    (as specified by an origin and 2 delta vectors)
 */
public class FillParallelogram extends GraphicsPrimitive
{
    public static final String methodSignature =
        "FillParallelogram(...)".toString();

    public static final int primTypeID = makePrimTypeID();

    public static FillParallelogram locate(SurfaceType srctype,
                                           CompositeType comptype,
                                           SurfaceType dsttype)
    {
        return (FillParallelogram)
            GraphicsPrimitiveMgr.locate(primTypeID,
                                        srctype, comptype, dsttype);
    }

    protected FillParallelogram(SurfaceType srctype,
                                CompositeType comptype,
                                SurfaceType dsttype)
    {
        super(methodSignature, primTypeID,
              srctype, comptype, dsttype);
    }

    public FillParallelogram(long pNativePrim,
                             SurfaceType srctype,
                             CompositeType comptype,
                             SurfaceType dsttype)
    {
        super(pNativePrim, methodSignature, primTypeID,
              srctype, comptype, dsttype);
    }

    /**
     * All FillParallelogram implementors must have this invoker method
     */
    public native void FillParallelogram(SunGraphics2D sg2d, SurfaceData dest,
                                         double x0, double y0,
                                         double dx1, double dy1,
                                         double dx2, double dy2);

    public GraphicsPrimitive traceWrap() {
        return new TraceFillParallelogram(this);
    }

    private static class TraceFillParallelogram extends FillParallelogram {
        FillParallelogram target;

        public TraceFillParallelogram(FillParallelogram target) {
            super(target.getSourceType(),
                  target.getCompositeType(),
                  target.getDestType());
            this.target = target;
        }

        public GraphicsPrimitive traceWrap() {
            return this;
        }

        public void FillParallelogram(SunGraphics2D sg2d, SurfaceData dest,
                                      double x0, double y0,
                                      double dx1, double dy1,
                                      double dx2, double dy2)
        {
            tracePrimitive(target);
            target.FillParallelogram(sg2d, dest, x0, y0, dx1, dy1, dx2, dy2);
        }
    }
}
