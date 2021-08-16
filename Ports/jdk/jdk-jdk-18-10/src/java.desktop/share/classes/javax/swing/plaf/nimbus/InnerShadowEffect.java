/*
 * Copyright (c) 2005, 2006, Oracle and/or its affiliates. All rights reserved.
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
package javax.swing.plaf.nimbus;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.Arrays;

/**
 * InnerShadowEffect - This effect currently only works with ARGB type buffered
 * images.
 *
 * @author Created by Jasper Potts (Jun 18, 2007)
 */
class InnerShadowEffect extends ShadowEffect {

    // =================================================================================================================
    // Effect Methods

    /**
     * Get the type of this effect, one of UNDER,BLENDED,OVER. UNDER means the result of apply effect should be painted
     * under the src image. BLENDED means the result of apply sffect contains a modified src image so just it should be
     * painted. OVER means the result of apply effect should be painted over the src image.
     *
     * @return The effect type
     */
    Effect.EffectType getEffectType() {
        return Effect.EffectType.OVER;
    }

    /**
     * Apply the effect to the src image generating the result . The result image may or may not contain the source
     * image depending on what the effect type is.
     *
     * @param src The source image for applying the effect to
     * @param dst The dstination image to paint effect result into. If this is null then a new image will be created
     * @param w   The width of the src image to apply effect to, this allow the src and dst buffers to be bigger than
     *            the area the need effect applied to it
     * @param h   The height of the src image to apply effect to, this allow the src and dst buffers to be bigger than
     *            the area the need effect applied to it
     * @return Image with the result of the effect
     */
    BufferedImage applyEffect(BufferedImage src, BufferedImage dst, int w, int h) {
        if (src == null || src.getType() != BufferedImage.TYPE_INT_ARGB){
            throw new IllegalArgumentException("Effect only works with " +
                    "source images of type BufferedImage.TYPE_INT_ARGB.");
        }
        if (dst != null && dst.getType() != BufferedImage.TYPE_INT_ARGB){
            throw new IllegalArgumentException("Effect only works with " +
                    "destination images of type BufferedImage.TYPE_INT_ARGB.");
        }
        // calculate offset
        double trangleAngle = Math.toRadians(angle - 90);
        int offsetX = (int) (Math.sin(trangleAngle) * distance);
        int offsetY = (int) (Math.cos(trangleAngle) * distance);
        // clac expanded size
        int tmpOffX = offsetX + size;
        int tmpOffY = offsetX + size;
        int tmpW = w + offsetX + size + size;
        int tmpH = h + offsetX + size;
        // create tmp buffers
        int[] lineBuf = getArrayCache().getTmpIntArray(w);
        byte[] srcAlphaBuf = getArrayCache().getTmpByteArray1(tmpW * tmpH);
        Arrays.fill(srcAlphaBuf, (byte) 0xFF);
        byte[] tmpBuf1 = getArrayCache().getTmpByteArray2(tmpW * tmpH);
        byte[] tmpBuf2 = getArrayCache().getTmpByteArray3(tmpW * tmpH);
        // extract src image alpha channel and inverse and offset
        Raster srcRaster = src.getRaster();
        for (int y = 0; y < h; y++) {
            int dy = (y + tmpOffY);
            int offset = dy * tmpW;
            srcRaster.getDataElements(0, y, w, 1, lineBuf);
            for (int x = 0; x < w; x++) {
                int dx = x + tmpOffX;
                srcAlphaBuf[offset + dx] = (byte) ((255 - ((lineBuf[x] & 0xFF000000) >>> 24)) & 0xFF);
            }
        }
        // blur
        float[] kernel = EffectUtils.createGaussianKernel(size * 2);
        EffectUtils.blur(srcAlphaBuf, tmpBuf2, tmpW, tmpH, kernel, size * 2); // horizontal pass
        EffectUtils.blur(tmpBuf2, tmpBuf1, tmpH, tmpW, kernel, size * 2);// vertical pass
        //rescale
        float spread = Math.min(1 / (1 - (0.01f * this.spread)), 255);
        for (int i = 0; i < tmpBuf1.length; i++) {
            int val = (int) (((int) tmpBuf1[i] & 0xFF) * spread);
            tmpBuf1[i] = (val > 255) ? (byte) 0xFF : (byte) val;
        }
        // create color image with shadow color and greyscale image as alpha
        if (dst == null) dst = new BufferedImage(w, h,
                BufferedImage.TYPE_INT_ARGB);
        WritableRaster shadowRaster = dst.getRaster();
        int red = color.getRed(), green = color.getGreen(), blue = color.getBlue();
        for (int y = 0; y < h; y++) {
            int srcY = y + tmpOffY;
            int offset = srcY * tmpW;
            int shadowOffset = (srcY - offsetY) * tmpW;
            for (int x = 0; x < w; x++) {
                int srcX = x + tmpOffX;
                int origianlAlphaVal = 255 - ((int) srcAlphaBuf[offset + srcX] & 0xFF);
                int shadowVal = (int) tmpBuf1[shadowOffset + (srcX - offsetX)] & 0xFF;
                int alphaVal = Math.min(origianlAlphaVal, shadowVal);
                lineBuf[x] = ((byte) alphaVal & 0xFF) << 24 | red << 16 | green << 8 | blue;
            }
            shadowRaster.setDataElements(0, y, w, 1, lineBuf);
        }
        return dst;
    }
}
