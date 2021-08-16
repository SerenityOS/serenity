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

import sun.awt.AppContext;

import java.awt.image.BufferedImage;
import java.lang.ref.SoftReference;

/**
 * Effect
 *
 * @author Created by Jasper Potts (Jun 18, 2007)
 */
abstract class Effect {
    enum EffectType {
        UNDER, BLENDED, OVER
    }

    // =================================================================================================================
    // Abstract Methods

    /**
     * Get the type of this effect, one of UNDER,BLENDED,OVER. UNDER means the result of apply effect should be painted
     * under the src image. BLENDED means the result of apply sffect contains a modified src image so just it should be
     * painted. OVER means the result of apply effect should be painted over the src image.
     *
     * @return The effect type
     */
    abstract EffectType getEffectType();

    /**
     * Get the opacity to use to paint the result effected image if the EffectType is UNDER or OVER.
     *
     * @return The opactity for the effect, 0.0f -> 1.0f
     */
    abstract float getOpacity();

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
     * @return The result of appl
     */
    abstract BufferedImage applyEffect(BufferedImage src, BufferedImage dst, int w, int h);

    // =================================================================================================================
    // Static data cache

    protected static ArrayCache getArrayCache() {
        ArrayCache cache = (ArrayCache)AppContext.getAppContext().get(ArrayCache.class);
        if (cache == null){
            cache = new ArrayCache();
            AppContext.getAppContext().put(ArrayCache.class,cache);
        }
        return cache;
    }

    protected static class ArrayCache {
        private SoftReference<int[]> tmpIntArray = null;
        private SoftReference<byte[]> tmpByteArray1 = null;
        private SoftReference<byte[]> tmpByteArray2 = null;
        private SoftReference<byte[]> tmpByteArray3 = null;

        protected int[] getTmpIntArray(int size) {
            int[] tmp;
            if (tmpIntArray == null || (tmp = tmpIntArray.get()) == null || tmp.length < size) {
                // create new array
                tmp = new int[size];
                tmpIntArray = new SoftReference<int[]>(tmp);
            }
            return tmp;
        }

        protected byte[] getTmpByteArray1(int size) {
            byte[] tmp;
            if (tmpByteArray1 == null || (tmp = tmpByteArray1.get()) == null || tmp.length < size) {
                // create new array
                tmp = new byte[size];
                tmpByteArray1 = new SoftReference<byte[]>(tmp);
            }
            return tmp;
        }

        protected byte[] getTmpByteArray2(int size) {
            byte[] tmp;
            if (tmpByteArray2 == null || (tmp = tmpByteArray2.get()) == null || tmp.length < size) {
                // create new array
                tmp = new byte[size];
                tmpByteArray2 = new SoftReference<byte[]>(tmp);
            }
            return tmp;
        }

        protected byte[] getTmpByteArray3(int size) {
            byte[] tmp;
            if (tmpByteArray3 == null || (tmp = tmpByteArray3.get()) == null || tmp.length < size) {
                // create new array
                tmp = new byte[size];
                tmpByteArray3 = new SoftReference<byte[]>(tmp);
            }
            return tmp;
        }
    }
}
