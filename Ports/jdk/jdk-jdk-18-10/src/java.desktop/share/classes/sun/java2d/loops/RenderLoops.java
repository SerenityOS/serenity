/*
 * Copyright (c) 1999, 2010, Oracle and/or its affiliates. All rights reserved.
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

package sun.java2d.loops;

/*
 * This class stores the various loops that are used by the
 * standard rendering pipelines.  The loops for a given instance
 * of this class will all share the same destination type and the
 * same supported paint and composite operation.
 * Each instance of this class should be shared by all graphics
 * objects that render onto the same type of destination with the
 * same paint and composite combination to reduce the amount of
 * time spent looking up loops appropriate for the current fill
 * technique.
 */
public class RenderLoops {

    public static final int primTypeID = GraphicsPrimitive.makePrimTypeID();

    public DrawLine             drawLineLoop;
    public FillRect             fillRectLoop;
    public DrawRect             drawRectLoop;
    public DrawPolygons         drawPolygonsLoop;
    public DrawPath             drawPathLoop;
    public FillPath             fillPathLoop;
    public FillSpans            fillSpansLoop;
    public FillParallelogram    fillParallelogramLoop;
    public DrawParallelogram    drawParallelogramLoop;
    public DrawGlyphList        drawGlyphListLoop;
    public DrawGlyphListAA      drawGlyphListAALoop;
    public DrawGlyphListLCD     drawGlyphListLCDLoop;
    public DrawGlyphListColor   drawGlyphListColorLoop;
}
