/*
 * Copyright (c) 2010, 2014, Oracle and/or its affiliates. All rights reserved.
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

package sun.font;

import java.io.*;

/**
 * Stores glyph-related data, used in the pure-java glyphcache.
 *
 * @author Clemens Eisserer
 */

public class XRGlyphCacheEntry {
    long glyphInfoPtr;

    int lastUsed;
    boolean pinned;

    int xOff;
    int yOff;

    int glyphSet;

    public XRGlyphCacheEntry(long glyphInfoPtr, GlyphList gl) {
        this.glyphInfoPtr = glyphInfoPtr;

        /* TODO: Does it make sence to cache results? */
        xOff = Math.round(getXAdvance());
        yOff = Math.round(getYAdvance());
    }

    public int getXOff() {
        return xOff;
    }

    public int getYOff() {
        return yOff;
    }

    public void setGlyphSet(int glyphSet) {
        this.glyphSet = glyphSet;
    }

    public int getGlyphSet() {
        return glyphSet;
    }

    public static int getGlyphID(long glyphInfoPtr) {
        // We need to access the GlyphID with Unsafe.getAddress() because the
        // corresponding field in the underlying C data-structure is of type
        // 'void*' (see field 'cellInfo' of struct 'GlyphInfo'
        // in src/share/native/sun/font/fontscalerdefs.h).
        // On 64-bit Big-endian architectures it would be wrong to access this
        // field with Unsafe.getInt().
        return (int) StrikeCache.unsafe.getAddress(glyphInfoPtr +
                                                   StrikeCache.cacheCellOffset);
    }

    public static void setGlyphID(long glyphInfoPtr, int id) {
        // We need to access the GlyphID with Unsafe.putAddress() because the
        // corresponding field in the underlying C data-structure is of type
        // 'void*' (see field 'cellInfo' of struct 'GlyphInfo' in
        // src/share/native/sun/font/fontscalerdefs.h).
        // On 64-bit Big-endian architectures it would be wrong to write this
        // field with Unsafe.putInt() because it is also accessed from native
        // code as a 'long'.
        // See Java_sun_java2d_xr_XRBackendNative_XRAddGlyphsNative()
        // in src/solaris/native/sun/java2d/x11/XRBackendNative.c
        StrikeCache.unsafe.putAddress(glyphInfoPtr +
                                      StrikeCache.cacheCellOffset, (long)id);
    }

    public int getGlyphID() {
        return getGlyphID(glyphInfoPtr);
    }

    public void setGlyphID(int id) {
        setGlyphID(glyphInfoPtr, id);
    }

    public float getXAdvance() {
        return StrikeCache.unsafe.getFloat(glyphInfoPtr + StrikeCache.xAdvanceOffset);
    }

    public float getYAdvance() {
        return StrikeCache.unsafe.getFloat(glyphInfoPtr + StrikeCache.yAdvanceOffset);
    }

    public int getSourceRowBytes() {
        return StrikeCache.unsafe.getShort(glyphInfoPtr + StrikeCache.rowBytesOffset);
    }

    public int getWidth() {
        return StrikeCache.unsafe.getShort(glyphInfoPtr + StrikeCache.widthOffset);
    }

    public int getHeight() {
        return StrikeCache.unsafe.getShort(glyphInfoPtr + StrikeCache.heightOffset);
    }

    public void writePixelData(ByteArrayOutputStream os, boolean uploadAsLCD) {
        long pixelDataAddress =
            StrikeCache.unsafe.getAddress(glyphInfoPtr +
                                          StrikeCache.pixelDataOffset);
        if (pixelDataAddress == 0L) {
            return;
        }

        int width = getWidth();
        int height = getHeight();
        int rowBytes = getSourceRowBytes();
        int paddedWidth = getPaddedWidth(uploadAsLCD);

        if (!uploadAsLCD) {
            for (int line = 0; line < height; line++) {
                for(int x = 0; x < paddedWidth; x++) {
                    if(x < width) {
                        os.write(StrikeCache.unsafe.getByte(pixelDataAddress + (line * rowBytes + x)));
                    }else {
                         /*pad to multiple of 4 bytes per line*/
                         os.write(0);
                    }
                }
            }
        } else {
            for (int line = 0; line < height; line++) {
                int rowStart = line * rowBytes;
                int rowBytesWidth = width * 3;
                int srcpix = 0;
                while (srcpix < rowBytesWidth) {
                    os.write(StrikeCache.unsafe.getByte
                          (pixelDataAddress + (rowStart + srcpix + 2)));
                    os.write(StrikeCache.unsafe.getByte
                          (pixelDataAddress + (rowStart + srcpix + 1)));
                    os.write(StrikeCache.unsafe.getByte
                          (pixelDataAddress + (rowStart + srcpix + 0)));
                    os.write(255);
                    srcpix += 3;
                }
            }
        }
    }

    public float getTopLeftXOffset() {
        return StrikeCache.unsafe.getFloat(glyphInfoPtr + StrikeCache.topLeftXOffset);
    }

    public float getTopLeftYOffset() {
        return StrikeCache.unsafe.getFloat(glyphInfoPtr + StrikeCache.topLeftYOffset);
    }

    public long getGlyphInfoPtr() {
        return glyphInfoPtr;
    }

    public boolean isGrayscale(boolean listContainsLCDGlyphs) {
        return getSourceRowBytes() == getWidth() && !(getWidth() == 0 && getHeight() == 0 && listContainsLCDGlyphs);
    }

    public int getPaddedWidth(boolean listContainsLCDGlyphs) {
        int width = getWidth();
        return isGrayscale(listContainsLCDGlyphs) ? (int) Math.ceil(width / 4.0) * 4 : width;
    }

    public int getDestinationRowBytes(boolean listContainsLCDGlyphs) {
        boolean grayscale = isGrayscale(listContainsLCDGlyphs);
        return grayscale ? getPaddedWidth(grayscale) : getWidth() * 4;
    }

    public int getGlyphDataLenth(boolean listContainsLCDGlyphs) {
        return getDestinationRowBytes(listContainsLCDGlyphs) * getHeight();
    }

    public void setPinned() {
        pinned = true;
    }

    public void setUnpinned() {
        pinned = false;
    }

    public int getLastUsed() {
        return lastUsed;
    }

    public void setLastUsed(int lastUsed) {
        this.lastUsed = lastUsed;
    }

    public int getPixelCnt() {
        return getWidth() * getHeight();
    }

    public boolean isPinned() {
        return pinned;
    }
}
