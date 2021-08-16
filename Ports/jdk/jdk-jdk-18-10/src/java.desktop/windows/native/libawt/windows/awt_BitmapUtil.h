/*
 * Copyright (c) 2006, 2009, Oracle and/or its affiliates. All rights reserved.
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

#ifndef AWT_BITMAP_UTIL_H
#define AWT_BITMAP_UTIL_H

class BitmapUtil {
public:
    /**
     * Creates B&W Bitmap with transparency mask from specified ARGB input data
     * 0 for opaque pixels, 1 for transparent.
     * MSDN article for ICONINFO says that 'for color icons, this mask only
     * defines the AND bitmask of the icon'. That's wrong! If mask bit for
     * specific pixel is 0, the pixel is drawn opaque, otherwise it's XORed
     * with background.
     */
    static HBITMAP CreateTransparencyMaskFromARGB(int width, int height, int* imageData);

    /**
     * Creates 32-bit ARGB V4 Bitmap (Win95-compatible) from specified ARGB input data
     * The color for transparent pixels (those with 0 alpha) is reset to 0 (BLACK)
     * to prevent errors on systems prior to XP.
     */
    static HBITMAP CreateV4BitmapFromARGB(int width, int height, int* imageData);

    /**
     * Creates 32-bit premultiplied ARGB V4 Bitmap (Win95-compatible) from
     * specified ARGB Pre input data.
     */
    static HBITMAP CreateBitmapFromARGBPre(int width, int height,
                                           int srcStride,
                                           int* imageData);

    /**
     * Transforms the given bitmap into an HRGN representing the transparency
     * of the bitmap.
     */
    static HRGN BitmapToRgn(HBITMAP hBitmap);

    /**
     * Makes a copy of the given bitmap. Blends every pixel of the source
     * with the given blendColor and alpha. If alpha == 0, the function
     * simply makes a plain copy of the source without any blending.
     */
    static HBITMAP BlendCopy(HBITMAP hSrcBitmap, COLORREF blendColor, BYTE alpha);

    /**
     * Creates a 32 bit ARGB bitmap. Returns the bitmap handle.
     * The pointer to the bitmap data is stored into bitmapBitsPtr.
     */
    static HBITMAP CreateARGBBitmap(int width, int height, void ** bitmapBitsPtr);
};

#endif
