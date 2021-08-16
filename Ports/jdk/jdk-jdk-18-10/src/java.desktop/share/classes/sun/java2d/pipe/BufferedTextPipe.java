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
import sun.font.GlyphList;
import sun.java2d.SunGraphics2D;
import sun.java2d.SurfaceData;
import static sun.java2d.pipe.BufferedOpCodes.*;

import java.lang.annotation.Native;

public abstract class BufferedTextPipe extends GlyphListPipe {

    @Native private static final int BYTES_PER_GLYPH_IMAGE = 8;
    @Native private static final int BYTES_PER_GLYPH_POSITION = 8;

    /**
     * The following offsets are used to pack the parameters in
     * createPackedParams().  (They are also used at the native level when
     * unpacking the params.)
     */
    @Native private static final int OFFSET_CONTRAST  = 8;
    @Native private static final int OFFSET_RGBORDER  = 2;
    @Native private static final int OFFSET_SUBPIXPOS = 1;
    @Native private static final int OFFSET_POSITIONS = 0;

    /**
     * Packs the given parameters into a single int value in order to save
     * space on the rendering queue.  Note that most of these parameters
     * are only used for rendering LCD-optimized text, but conditionalizing
     * this work wouldn't make any impact on performance, so we will pack
     * those parameters even in the non-LCD case.
     */
    private static int createPackedParams(SunGraphics2D sg2d, GlyphList gl) {
        return
            (((gl.usePositions() ? 1 : 0)   << OFFSET_POSITIONS) |
             ((gl.isSubPixPos()  ? 1 : 0)   << OFFSET_SUBPIXPOS) |
             ((gl.isRGBOrder()   ? 1 : 0)   << OFFSET_RGBORDER ) |
             ((sg2d.lcdTextContrast & 0xff) << OFFSET_CONTRAST ));
    }

    protected final RenderQueue rq;

    protected BufferedTextPipe(RenderQueue rq) {
        this.rq = rq;
    }

    @Override
    protected void drawGlyphList(SunGraphics2D sg2d, GlyphList gl) {
        /*
         * The native drawGlyphList() only works with two composite types:
         *    - CompositeType.SrcOver (with any extra alpha), or
         *    - CompositeType.Xor
         */
        Composite comp = sg2d.composite;
        if (comp == AlphaComposite.Src) {
            /*
             * In addition to the composite types listed above, the logic
             * in OGL/D3DSurfaceData.validatePipe() allows for
             * CompositeType.SrcNoEa, but only in the presence of an opaque
             * color.  If we reach this case, we know the color is opaque,
             * and therefore SrcNoEa is the same as SrcOverNoEa, so we
             * override the composite here.
             */
            comp = AlphaComposite.SrcOver;
        }

        rq.lock();
        try {
            validateContext(sg2d, comp);
            enqueueGlyphList(sg2d, gl);
        } finally {
            rq.unlock();
        }
    }

    private void enqueueGlyphList(final SunGraphics2D sg2d,
                                  final GlyphList gl)
    {
        // assert rq.lock.isHeldByCurrentThread();
        RenderBuffer buf = rq.getBuffer();
        final int totalGlyphs = gl.getNumGlyphs();
        int glyphBytesRequired = totalGlyphs * BYTES_PER_GLYPH_IMAGE;
        int posBytesRequired =
            gl.usePositions() ? totalGlyphs * BYTES_PER_GLYPH_POSITION : 0;
        int totalBytesRequired = 24 + glyphBytesRequired + posBytesRequired;

        final long[] images = gl.getImages();
        final float glyphListOrigX = gl.getX() + 0.5f;
        final float glyphListOrigY = gl.getY() + 0.5f;

        // make sure the RenderQueue keeps a hard reference to the FontStrike
        // so that the associated glyph images are not disposed while enqueued
        rq.addReference(gl.getStrike());

        if (totalBytesRequired <= buf.capacity()) {
            if (totalBytesRequired > buf.remaining()) {
                // process the queue first and then enqueue the glyphs
                rq.flushNow();
            }
            rq.ensureAlignment(20);
            buf.putInt(DRAW_GLYPH_LIST);
            // enqueue parameters
            buf.putInt(totalGlyphs);
            buf.putInt(createPackedParams(sg2d, gl));
            buf.putFloat(glyphListOrigX);
            buf.putFloat(glyphListOrigY);
            // now enqueue glyph information
            buf.put(images, 0, totalGlyphs);
            if (gl.usePositions()) {
                float[] positions = gl.getPositions();
                buf.put(positions, 0, 2*totalGlyphs);
            }
        } else {
            // queue is too small to accommodate glyphs; perform
            // the operation directly on the queue flushing thread
            rq.flushAndInvokeNow(new Runnable() {
                public void run() {
                    drawGlyphList(totalGlyphs, gl.usePositions(),
                                  gl.isSubPixPos(), gl.isRGBOrder(),
                                  sg2d.lcdTextContrast,
                                  glyphListOrigX, glyphListOrigY,
                                  images, gl.getPositions());
                }
            });
        }
    }

    /**
     * Called as a separate Runnable when the operation is too large to fit
     * on the RenderQueue.  The OGL/D3D pipelines each have their own (small)
     * native implementation of this method.
     */
    protected abstract void drawGlyphList(int numGlyphs, boolean usePositions,
                                          boolean subPixPos, boolean rgbOrder,
                                          int lcdContrast,
                                          float glOrigX, float glOrigY,
                                          long[] images, float[] positions);

    /**
     * Validates the state in the provided SunGraphics2D object.
     */
    protected abstract void validateContext(SunGraphics2D sg2d,
                                            Composite comp);
}
