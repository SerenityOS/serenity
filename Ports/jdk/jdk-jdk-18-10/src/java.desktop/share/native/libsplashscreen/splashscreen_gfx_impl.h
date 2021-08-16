/*
 * Copyright (c) 2005, 2010, Oracle and/or its affiliates. All rights reserved.
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

#ifndef SPLASHSCREEN_GFX_IMPL_H
#define SPLASHSCREEN_GFX_IMPL_H

#include "splashscreen_gfx.h"

/* here come some very simple macros */

/* advance a pointer p by sizeof(type)*n bytes */
#define INCPN(type,p,n) ((p) = (type*)(p)+(n))

/* advance a pointer by sizeof(type) */
#define INCP(type,p) INCPN(type,(p),1)

/* store a typed value to pointed location */
#define PUT(type,p,v) (*(type*)(p) = (type)(v))

/* load a typed value from pointed location */
#define GET(type,p) (*(type*)p)

/* same as cond<0?-1:0 */
enum
{
    IFNEG_SHIFT_BITS = sizeof(int) * 8 - 1
};

#define IFNEG(cond) ((int)(cond)>>IFNEG_SHIFT_BITS)

/* same as cond<0?n1:n2 */
#define IFNEGPOS(cond,n1,n2) ((IFNEG(cond)&(n1))|((~IFNEG(cond))&(n2)))

/* value shifted left by n bits, negative n is allowed */
#define LSHIFT(value,n) IFNEGPOS((n),(value)>>-(n),(value)<<(n))

/* value shifted right by n bits, negative n is allowed */
#define RSHIFT(value,n) IFNEGPOS(n,(value)<<-(n),(value)>>(n))

/* converts a single i'th component to the specific format defined by format->shift[i] and format->mask[i] */
#define CONVCOMP(quad,format,i) \
    (LSHIFT((quad),(format)->shift[i])&(format)->mask[i])

/* extracts the component defined by format->shift[i] and format->mask[i] from a specific-format value */
#define UNCONVCOMP(value,format,i) \
    (RSHIFT((value)&(format)->mask[i],(format)->shift[i]))

/*  dithers the color using the dither matrices and colormap from format
    indices to dither matrices are passed as arguments */
INLINE unsigned
ditherColor(rgbquad_t value, ImageFormat * format, int row, int col)
{
    int blue = QUAD_BLUE(value);
    int green = QUAD_GREEN(value);
    int red = QUAD_RED(value);

    blue = format->dithers[0].colorTable[blue +
        format->dithers[0].matrix[col & DITHER_MASK][row & DITHER_MASK]];
    green = format->dithers[1].colorTable[green +
        format->dithers[1].matrix[col & DITHER_MASK][row & DITHER_MASK]];
    red = format->dithers[2].colorTable[red +
        format->dithers[2].matrix[col & DITHER_MASK][row & DITHER_MASK]];
    return red + green + blue;
}

/*      blend (lerp between) two rgb quads
        src and dst alpha is ignored
        the algorithm: src*alpha+dst*(1-alpha)=(src-dst)*alpha+dst, rb and g are done separately
*/
INLINE rgbquad_t
blendRGB(rgbquad_t dst, rgbquad_t src, rgbquad_t alpha)
{
    const rgbquad_t a = alpha;
    const rgbquad_t a1 = 0xFF - alpha;

    return MAKE_QUAD(
        (rgbquad_t)((QUAD_RED(src) * a + QUAD_RED(dst) * a1) / 0xFF),
        (rgbquad_t)((QUAD_GREEN(src) * a + QUAD_GREEN(dst) * a1) / 0xFF),
        (rgbquad_t)((QUAD_BLUE(src) * a + QUAD_BLUE(dst) * a1) / 0xFF),
        0);
}

/*      scales rgb quad by alpha. basically similar to what's above. src alpha is retained.
        used for premultiplying alpha

        btw: braindead MSVC6 generates _three_ mul instructions for this function */

INLINE rgbquad_t
premultiplyRGBA(rgbquad_t src)
{
    rgbquad_t srb = src & 0xFF00FF;
    rgbquad_t sg = src & 0xFF00;
    rgbquad_t alpha = src >> QUAD_ALPHA_SHIFT;

    alpha += 1;

    srb *= alpha;
    sg *= alpha;
    srb >>= 8;
    sg >>= 8;

    return (src & 0xFF000000) | (srb & 0xFF00FF) | (sg & 0xFF00);
}

/*      The functions below are inherently ineffective, but the performance seems to be
        more or less adequate for the case of splash screens. They can be optimized later
        if needed. The idea of optimization is to provide inlineable form of putRGBADither and
        getRGBA at least for certain most frequently used visuals. Something like this is
        done in Java 2D ("loops"). This would be possible with C++ templates, but making it
        clean for C would require ugly preprocessor tricks. Leaving it out for later.
*/

/*      convert a single pixel color value from rgbquad according to visual format
        and place it to pointed location
        ordered dithering used when necessary */
INLINE void
putRGBADither(rgbquad_t value, void *ptr, ImageFormat * format,
        int row, int col)
{
    if (format->premultiplied) {
        value = premultiplyRGBA(value);
    }
    if (format->dithers) {
        value = format->colorIndex[ditherColor(value, format, row, col)];
    }
    else {
        value = CONVCOMP(value, format, 0) | CONVCOMP(value, format, 1) |
            CONVCOMP(value, format, 2) | CONVCOMP(value, format, 3);
    }
    switch (format->byteOrder) {
    case BYTE_ORDER_LSBFIRST:
        switch (format->depthBytes) {   /* lack of *break*'s is intentional */
        case 4:
            PUT(byte_t, ptr, value & 0xff);
            value >>= 8;
            INCP(byte_t, ptr);
        case 3:
            PUT(byte_t, ptr, value & 0xff);
            value >>= 8;
            INCP(byte_t, ptr);
        case 2:
            PUT(byte_t, ptr, value & 0xff);
            value >>= 8;
            INCP(byte_t, ptr);
        case 1:
            PUT(byte_t, ptr, value & 0xff);
        }
        break;
    case BYTE_ORDER_MSBFIRST:
        switch (format->depthBytes) {   /* lack of *break*'s is intentional */
        case 4:
            PUT(byte_t, ptr, (value >> 24) & 0xff);
            INCP(byte_t, ptr);
        case 3:
            PUT(byte_t, ptr, (value >> 16) & 0xff);
            INCP(byte_t, ptr);
        case 2:
            PUT(byte_t, ptr, (value >> 8) & 0xff);
            INCP(byte_t, ptr);
        case 1:
            PUT(byte_t, ptr, value & 0xff);
        }
        break;
    case BYTE_ORDER_NATIVE:
        switch (format->depthBytes) {
        case 4:
            PUT(rgbquad_t, ptr, value);
            break;
        case 3:                /* not supported, LSB or MSB should always be specified */
            PUT(byte_t, ptr, 0xff); /* Put a stub value */
            INCP(byte_t, ptr);
            PUT(byte_t, ptr, 0xff);
            INCP(byte_t, ptr);
            PUT(byte_t, ptr, 0xff);
            break;
        case 2:
            PUT(word_t, ptr, value);
            break;
        case 1:
            PUT(byte_t, ptr, value);
            break;
        }
    }
}

/* load a single pixel color value and un-convert it to rgbquad according to visual format */
INLINE rgbquad_t
getRGBA(void *ptr, ImageFormat * format)
{
    /*
       FIXME: color is not un-alpha-premultiplied on get
       this is not required by current code, but it makes the implementation inconsistent
       i.e. put(get) will not work right for alpha-premultiplied images */

    /* get the value basing on depth and byte order */
    rgbquad_t value = 0;

    switch (format->byteOrder) {
    case BYTE_ORDER_LSBFIRST:
        switch (format->depthBytes) {
        case 4:
            value |= GET(byte_t, ptr);
            value <<= 8;
            INCP(byte_t, ptr);
        case 3:
            value |= GET(byte_t, ptr);
            value <<= 8;
            INCP(byte_t, ptr);
        case 2:
            value |= GET(byte_t, ptr);
            value <<= 8;
            INCP(byte_t, ptr);
        case 1:
            value |= GET(byte_t, ptr);
        }
        break;
    case BYTE_ORDER_MSBFIRST:
        switch (format->depthBytes) {   /* lack of *break*'s is intentional */
        case 4:
            value |= (GET(byte_t, ptr) << 24);
            INCP(byte_t, ptr);
        case 3:
            value |= (GET(byte_t, ptr) << 16);
            INCP(byte_t, ptr);
        case 2:
            value |= (GET(byte_t, ptr) << 8);
            INCP(byte_t, ptr);
        case 1:
            value |= GET(byte_t, ptr);
        }
        break;
    case BYTE_ORDER_NATIVE:
        switch (format->depthBytes) {
        case 4:
            value = GET(rgbquad_t, ptr);
            break;
        case 3:                /* not supported, LSB or MSB should always be specified */
            value = 0xFFFFFFFF; /*return a stub value */
            break;
        case 2:
            value = (rgbquad_t) GET(word_t, ptr);
            break;
        case 1:
            value = (rgbquad_t) GET(byte_t, ptr);
            break;
        }
        break;
    }
    /* now un-convert the value */
    if (format->colorMap) {
        if (value == format->transparentColor)
            return 0;
        else
            return format->colorMap[value];
    }
    else {
        return UNCONVCOMP(value, format, 0) | UNCONVCOMP(value, format, 1) |
            UNCONVCOMP(value, format, 2) | UNCONVCOMP(value, format, 3) |
            format->fixedBits;
    }
}

/* fill the line with the specified color according to visual format */
INLINE void
fillLine(rgbquad_t color, void *pDst, int incDst, int n,
        ImageFormat * dstFormat, int row, int col)
{
    int i;

    for (i = 0; i < n; ++i) {
        putRGBADither(color, pDst, dstFormat, row, col++);
        INCPN(byte_t, pDst, incDst);
    }
}

/* find the shift for specified mask, also verify the mask is valid */
INLINE int
getMaskShift(rgbquad_t mask, int *pShift, int *pnumBits)
{
    int shift = 0, numBits = 0;

    /* check the mask is not empty */
    if (!mask)
        return 0;
    /* calculate the shift */
    while ((mask & 1) == 0) {
        ++shift;
        mask >>= 1;
    }
    /* check the mask is contigious */
    if ((mask & (mask + 1)) != 0)
        return 0;
    /* calculate the number of bits */
    do {
        ++numBits;
        mask >>= 1;
    } while ((mask & 1) != 0);
    *pShift = shift;
    *pnumBits = numBits;
    return 1;
}

#endif
