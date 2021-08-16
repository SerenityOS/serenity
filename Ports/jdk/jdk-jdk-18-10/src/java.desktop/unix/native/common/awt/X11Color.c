/*
 * Copyright (c) 1995, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *      Image dithering and rendering code for X11.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <sys/time.h>
#include <sys/resource.h>
#ifndef HEADLESS
#include <X11/Xlib.h>
#include <X11/Xatom.h>
#include <X11/Xutil.h>
#endif /* !HEADLESS */
#include "awt_p.h"
#include "java_awt_Color.h"
#include "java_awt_SystemColor.h"
#include "java_awt_color_ColorSpace.h"
#include "java_awt_Transparency.h"
#include "java_awt_image_DataBuffer.h"
#include "img_colors.h"
#include "imageInitIDs.h"
#include "dither.h"

#include <jni.h>
#include <jni_util.h>

#ifdef DEBUG
static int debug_colormap = 0;
#endif

#define MAX_PALETTE8_SIZE (256)
#define MAX_PALETTE12_SIZE (4096)
#define MAX_PALETTE_SIZE MAX_PALETTE12_SIZE

/* returns the absolute value x */
#define ABS(x) ((x) < 0 ? -(x) : (x))

#define CLIP(val,min,max)       ((val < min) ? min : ((val > max) ? max : val))

#define RGBTOGRAY(r, g, b) ((int) (.299 * r + .587 * g + .114 * b + 0.5))

enum {
    FREE_COLOR          = 0,
    LIKELY_COLOR        = 1,
    UNAVAILABLE_COLOR   = 2,
    ALLOCATED_COLOR     = 3
};

/*
 * Constants to control the filling of the colormap.
 * By default, try to allocate colors in the default colormap until
 * CMAP_ALLOC_DEFAULT colors are being used (by Java and/or other
 * applications).
 * For cases where the default colormap may already have a large
 * number of colors in it, make sure that we ourselves try to add
 * at least CMAP_ALLOC_MIN new colors, even if we need to allocate
 * more than the DEFAULT to do that.
 * Under no circumstances will the colormap be filled to more than
 * CMAP_ALLOC_MAX colors.
 */
#define CMAP_ALLOC_MIN          100     /* minimum number of colors to "add" */
#define CMAP_ALLOC_DEFAULT      200     /* default number of colors in cmap */
#define CMAP_ALLOC_MAX          245     /* maximum number of colors in cmap */

#define getVirtCubeSize()       (LOOKUPSIZE)

unsigned char img_bwgamma[256];
uns_ordered_dither_array img_oda_alpha;

#ifdef NEED_IMAGE_CONVERT
ImgConvertFcn DirectImageConvert;
ImgConvertFcn Dir16IcmOpqUnsImageConvert;
ImgConvertFcn Dir16IcmTrnUnsImageConvert;
ImgConvertFcn Dir16IcmOpqSclImageConvert;
ImgConvertFcn Dir16DcmOpqUnsImageConvert;
ImgConvertFcn Dir16DcmTrnUnsImageConvert;
ImgConvertFcn Dir16DcmOpqSclImageConvert;
ImgConvertFcn Dir32IcmOpqUnsImageConvert;
ImgConvertFcn Dir32IcmTrnUnsImageConvert;
ImgConvertFcn Dir32IcmOpqSclImageConvert;
ImgConvertFcn Dir32DcmOpqUnsImageConvert;
ImgConvertFcn Dir32DcmTrnUnsImageConvert;
ImgConvertFcn Dir32DcmOpqSclImageConvert;

ImgConvertFcn PseudoImageConvert;
ImgConvertFcn PseudoFSImageConvert;
ImgConvertFcn FSColorIcmOpqUnsImageConvert;
ImgConvertFcn FSColorDcmOpqUnsImageConvert;
ImgConvertFcn OrdColorIcmOpqUnsImageConvert;
ImgConvertFcn OrdColorDcmOpqUnsImageConvert;

#endif /* NEED_IMAGE_CONVERT */

#ifndef HEADLESS
/*
 * Find the best color.
 */
int
awt_color_matchTC(int r, int g, int b, AwtGraphicsConfigDataPtr awt_data)
{
    r = CLIP(r, 0, 255);
    g = CLIP(g, 0, 255);
    b = CLIP(b, 0, 255);
    return (((r >> awt_data->awtImage->clrdata.rScale)
                << awt_data->awtImage->clrdata.rOff) |
            ((g >> awt_data->awtImage->clrdata.gScale)
                << awt_data->awtImage->clrdata.gOff) |
            ((b >> awt_data->awtImage->clrdata.bScale)
                << awt_data->awtImage->clrdata.bOff));
}

int
awt_color_matchGS(int r, int g, int b, AwtGraphicsConfigDataPtr awt_data)
{
    r = CLIP(r, 0, 255);
    g = CLIP(g, 0, 255);
    b = CLIP(b, 0, 255);
    return awt_data->color_data->img_grays[RGBTOGRAY(r, g, b)];
}

int
awt_color_match(int r, int g, int b, AwtGraphicsConfigDataPtr awt_data)
{
    int besti = 0;
    int mindist, i, t, d;
    ColorEntry *p = awt_data->color_data->awt_Colors;

    r = CLIP(r, 0, 255);
    g = CLIP(g, 0, 255);
    b = CLIP(b, 0, 255);

    /* look for pure gray match */
    if ((r == g) && (g == b)) {
      mindist = 256;
      for (i = 0 ; i < awt_data->awt_num_colors ; i++, p++)
        if (p->flags == ALLOCATED_COLOR) {
          if (! ((p->r == p->g) && (p->g == p->b)) )
              continue;
          d = ABS(p->r - r);
          if (d == 0)
              return i;
          if (d < mindist) {
              besti = i;
              mindist = d;
          }
        }
      return besti;
    }

    /* look for non-pure gray match */
    mindist = 256 * 256 * 256;
    for (i = 0 ; i < awt_data->awt_num_colors ; i++, p++)
        if (p->flags == ALLOCATED_COLOR) {
            t = p->r - r;
            d = t * t;
            if (d >= mindist)
                continue;
            t = p->g - g;
            d += t * t;
            if (d >= mindist)
                continue;
            t = p->b - b;
            d += t * t;
            if (d >= mindist)
                continue;
            if (d == 0)
                return i;
            if (d < mindist) {
                besti = i;
                mindist = d;
            }
        }
    return besti;
}

/*
 * Allocate a color in the X color map and return the pixel.
 * If the "expected pixel" is non-negative then we will only
 * accept the allocation if we get exactly that pixel value.
 * This prevents us from seeing a bunch of ReadWrite pixels
 * allocated by another imaging application and duplicating
 * that set of inaccessible pixels in our precious remaining
 * ReadOnly colormap cells.
 */
static int
alloc_col(Display *dpy, Colormap cm, int r, int g, int b, int pixel,
          AwtGraphicsConfigDataPtr awt_data)
{
    XColor col;

    r = CLIP(r, 0, 255);
    g = CLIP(g, 0, 255);
    b = CLIP(b, 0, 255);

    col.flags = DoRed | DoGreen | DoBlue;
    col.red   = (r << 8) | r;
    col.green = (g << 8) | g;
    col.blue  = (b << 8) | b;
    if (XAllocColor(dpy, cm, &col)) {
#ifdef DEBUG
        if (debug_colormap)
            jio_fprintf(stdout, "allocated %d (%d,%d, %d)\n", col.pixel, r, g, b);
#endif
        if (pixel >= 0 && col.pixel != (unsigned long)pixel) {
            /*
             * If we were trying to allocate a shareable "ReadOnly"
             * color then we would have gotten back the expected
             * pixel.  If the returned pixel was different, then
             * the source color that we were attempting to gain
             * access to must be some other application's ReadWrite
             * private color.  We free the returned pixel so that
             * we won't waste precious colormap entries by duplicating
             * that color in the as yet unallocated entries.  We
             * return -1 here to indicate the failure to get the
             * expected pixel.
             */
#ifdef DEBUG
            if (debug_colormap)
                jio_fprintf(stdout, "   used by other app, freeing\n");
#endif
            awt_data->color_data->awt_Colors[pixel].flags = UNAVAILABLE_COLOR;
            XFreeColors(dpy, cm, &col.pixel, 1, 0);
            return -1;
        }
        /*
         * Our current implementation doesn't support pixels which
         * don't fit in 8 bit (even for 12-bit visuals)
         */
        if (col.pixel > 255) {
#ifdef DEBUG
            if (debug_colormap)
                jio_fprintf(stdout, "pixel %d for (%d,%d, %d) is > 8 bit, releasing.\n",
                            col.pixel, r, g, b);
#endif
            XFreeColors(dpy, cm, &col.pixel, 1, 0);
            return awt_color_match(r, g, b, awt_data);
        }

        awt_data->color_data->awt_Colors[col.pixel].flags = ALLOCATED_COLOR;
        awt_data->color_data->awt_Colors[col.pixel].r = col.red   >> 8;
        awt_data->color_data->awt_Colors[col.pixel].g = col.green >> 8;
        awt_data->color_data->awt_Colors[col.pixel].b = col.blue  >> 8;
        if (awt_data->color_data->awt_icmLUT != 0) {
            awt_data->color_data->awt_icmLUT2Colors[col.pixel] = col.pixel;
            awt_data->color_data->awt_icmLUT[col.pixel] =
                0xff000000 |
                (awt_data->color_data->awt_Colors[col.pixel].r<<16) |
                (awt_data->color_data->awt_Colors[col.pixel].g<<8) |
                (awt_data->color_data->awt_Colors[col.pixel].b);
        }
        return col.pixel;
#ifdef DEBUG
    } else if (debug_colormap) {
        jio_fprintf(stdout, "can't allocate (%d,%d, %d)\n", r, g, b);
#endif
    }

    return awt_color_match(r, g, b, awt_data);
}
#endif /* !HEADLESS */

void
awt_fill_imgcv(ImgConvertFcn **array, int mask, int value, ImgConvertFcn fcn)
{
    int i;

    for (i = 0; i < NUM_IMGCV; i++) {
        if ((i & mask) == value) {
            array[i] = fcn;
        }
    }
}

#ifndef HEADLESS
/*
 * called from X11Server_create() in xlib.c
 */
int
awt_allocate_colors(AwtGraphicsConfigDataPtr awt_data)
{
    Display *dpy;
    unsigned long freecolors[MAX_PALETTE_SIZE], plane_masks[1];
    int paletteSize;
    XColor cols[MAX_PALETTE_SIZE];
    unsigned char reds[256], greens[256], blues[256];
    int indices[256];
    Colormap cm;
    int i, j, k, cmapsize, nfree, depth, bpp;
    int allocatedColorsNum, unavailableColorsNum;
    XPixmapFormatValues *pPFV;
    int numpfv;
    XVisualInfo *pVI;
    char *forcemono;
    char *forcegray;

    make_uns_ordered_dither_array(img_oda_alpha, 256);


    forcemono = getenv("FORCEMONO");
    forcegray = getenv("FORCEGRAY");
    if (forcemono && !forcegray)
        forcegray = forcemono;

    /*
     * Get the colormap and make sure we have the right visual
     */
    dpy = awt_display;
    cm = awt_data->awt_cmap;
    depth = awt_data->awt_depth;
    pVI = &awt_data->awt_visInfo;
    awt_data->awt_num_colors = awt_data->awt_visInfo.colormap_size;
    awt_data->awtImage = (awtImageData *) calloc (1, sizeof (awtImageData));
    if (awt_data->awtImage == NULL) {
        return 0;
    }

    pPFV = XListPixmapFormats(dpy, &numpfv);
    if (pPFV) {
        for (i = 0; i < numpfv; i++) {
            if (pPFV[i].depth == depth) {
                awt_data->awtImage->wsImageFormat = pPFV[i];
                break;
            }
        }
        XFree(pPFV);
    }
    bpp = awt_data->awtImage->wsImageFormat.bits_per_pixel;
    if (bpp == 24) {
        bpp = 32;
    }
    awt_data->awtImage->clrdata.bitsperpixel = bpp;
    awt_data->awtImage->Depth = depth;

    if ((bpp == 32 || bpp == 16) && pVI->class == TrueColor && depth >= 15) {
        awt_data->AwtColorMatch = awt_color_matchTC;
        awt_data->awtImage->clrdata.rOff = 0;
        for (i = pVI->red_mask; (i & 1) == 0; i >>= 1) {
            awt_data->awtImage->clrdata.rOff++;
        }
        awt_data->awtImage->clrdata.rScale = 0;
        while (i < 0x80) {
            awt_data->awtImage->clrdata.rScale++;
            i <<= 1;
        }
        awt_data->awtImage->clrdata.gOff = 0;
        for (i = pVI->green_mask; (i & 1) == 0; i >>= 1) {
            awt_data->awtImage->clrdata.gOff++;
        }
        awt_data->awtImage->clrdata.gScale = 0;
        while (i < 0x80) {
            awt_data->awtImage->clrdata.gScale++;
            i <<= 1;
        }
        awt_data->awtImage->clrdata.bOff = 0;
        for (i = pVI->blue_mask; (i & 1) == 0; i >>= 1) {
            awt_data->awtImage->clrdata.bOff++;
        }
        awt_data->awtImage->clrdata.bScale = 0;
        while (i < 0x80) {
            awt_data->awtImage->clrdata.bScale++;
            i <<= 1;
        }
#ifdef NEED_IMAGE_CONVERT
        awt_fill_imgcv(awt_data->awtImage->convert, 0, 0, DirectImageConvert);
        awt_fill_imgcv(awt_data->awtImage->convert,
                       (IMGCV_SCALEBITS | IMGCV_INSIZEBITS
                        | IMGCV_ALPHABITS | IMGCV_CMBITS),
                       (IMGCV_UNSCALED | IMGCV_BYTEIN
                        | IMGCV_OPAQUE | IMGCV_ICM),
                       (bpp == 32
                        ? Dir32IcmOpqUnsImageConvert
                        : Dir16IcmOpqUnsImageConvert));
        awt_fill_imgcv(awt_data->awtImage->convert,
                       (IMGCV_SCALEBITS | IMGCV_INSIZEBITS
                        | IMGCV_ALPHABITS | IMGCV_CMBITS),
                       (IMGCV_UNSCALED | IMGCV_BYTEIN
                        | IMGCV_ALPHA | IMGCV_ICM),
                       (bpp == 32
                        ? Dir32IcmTrnUnsImageConvert
                        : Dir16IcmTrnUnsImageConvert));
        awt_fill_imgcv(awt_data->awtImage->convert,
                       (IMGCV_SCALEBITS | IMGCV_INSIZEBITS
                        | IMGCV_ALPHABITS | IMGCV_CMBITS),
                       (IMGCV_SCALED | IMGCV_BYTEIN
                        | IMGCV_OPAQUE | IMGCV_ICM),
                       (bpp == 32
                        ? Dir32IcmOpqSclImageConvert
                        : Dir16IcmOpqSclImageConvert));
        awt_fill_imgcv(awt_data->awtImage->convert,
                       (IMGCV_SCALEBITS | IMGCV_INSIZEBITS
                        | IMGCV_ALPHABITS | IMGCV_CMBITS),
                       (IMGCV_UNSCALED | IMGCV_INTIN
                        | IMGCV_OPAQUE | IMGCV_DCM8),
                       (bpp == 32
                        ? Dir32DcmOpqUnsImageConvert
                        : Dir16DcmOpqUnsImageConvert));
        awt_fill_imgcv(awt_data->awtImage->convert,
                       (IMGCV_SCALEBITS | IMGCV_INSIZEBITS
                        | IMGCV_ALPHABITS | IMGCV_CMBITS),
                       (IMGCV_UNSCALED | IMGCV_INTIN
                        | IMGCV_ALPHA | IMGCV_DCM8),
                       (bpp == 32
                        ? Dir32DcmTrnUnsImageConvert
                        : Dir16DcmTrnUnsImageConvert));
        awt_fill_imgcv(awt_data->awtImage->convert,
                       (IMGCV_SCALEBITS | IMGCV_INSIZEBITS
                        | IMGCV_ALPHABITS | IMGCV_CMBITS),
                       (IMGCV_SCALED | IMGCV_INTIN
                        | IMGCV_OPAQUE | IMGCV_DCM8),
                       (bpp == 32
                        ? Dir32DcmOpqSclImageConvert
                        : Dir16DcmOpqSclImageConvert));
#endif /* NEED_IMAGE_CONVERT */
    } else if (bpp <= 16 && (pVI->class == StaticGray
                            || pVI->class == GrayScale
                            || (pVI->class == PseudoColor && forcegray))) {
        awt_data->AwtColorMatch = awt_color_matchGS;
        awt_data->awtImage->clrdata.grayscale = 1;
        awt_data->awtImage->clrdata.bitsperpixel = MAX(bpp, 8);
#ifdef NEED_IMAGE_CONVERT
        awt_fill_imgcv(awt_data->awtImage->convert, 0, 0, PseudoImageConvert);
        if (getenv("NOFSDITHER") == NULL) {
            awt_fill_imgcv(awt_data->awtImage->convert,
                           IMGCV_ORDERBITS, IMGCV_TDLRORDER,
                           PseudoFSImageConvert);
        }
#endif /* NEED_IMAGE_CONVERT */
    } else if (depth <= 12 && (pVI->class == PseudoColor
                             || pVI->class == TrueColor
                             || pVI->class == StaticColor)) {
        if (pVI->class == TrueColor)
           awt_data->awt_num_colors = (1 << pVI->depth);
        awt_data->AwtColorMatch = awt_color_match;
        awt_data->awtImage->clrdata.bitsperpixel = MAX(bpp, 8);
#ifdef NEED_IMAGE_CONVERT
        awt_fill_imgcv(awt_data->awtImage->convert, 0, 0, PseudoImageConvert);
        if (getenv("NOFSDITHER") == NULL) {
            awt_fill_imgcv(awt_data->awtImage->convert, IMGCV_ORDERBITS,
                           IMGCV_TDLRORDER, PseudoFSImageConvert);
            awt_fill_imgcv(awt_data->awtImage->convert,
                           (IMGCV_SCALEBITS | IMGCV_INSIZEBITS
                            | IMGCV_ALPHABITS | IMGCV_ORDERBITS
                            | IMGCV_CMBITS),
                           (IMGCV_UNSCALED | IMGCV_BYTEIN
                            | IMGCV_OPAQUE | IMGCV_TDLRORDER
                            | IMGCV_ICM),
                           FSColorIcmOpqUnsImageConvert);
            awt_fill_imgcv(awt_data->awtImage->convert,
                           (IMGCV_SCALEBITS | IMGCV_INSIZEBITS
                            | IMGCV_ALPHABITS | IMGCV_ORDERBITS
                            | IMGCV_CMBITS),
                           (IMGCV_UNSCALED | IMGCV_INTIN
                            | IMGCV_OPAQUE | IMGCV_TDLRORDER
                            | IMGCV_DCM8),
                           FSColorDcmOpqUnsImageConvert);
        }
        awt_fill_imgcv(awt_data->awtImage->convert,
                       (IMGCV_SCALEBITS | IMGCV_INSIZEBITS | IMGCV_ALPHABITS
                        | IMGCV_ORDERBITS | IMGCV_CMBITS),
                       (IMGCV_UNSCALED | IMGCV_BYTEIN | IMGCV_OPAQUE
                        | IMGCV_RANDORDER | IMGCV_ICM),
                       OrdColorIcmOpqUnsImageConvert);
        awt_fill_imgcv(awt_data->awtImage->convert,
                       (IMGCV_SCALEBITS | IMGCV_INSIZEBITS | IMGCV_ALPHABITS
                        | IMGCV_ORDERBITS | IMGCV_CMBITS),
                       (IMGCV_UNSCALED | IMGCV_INTIN | IMGCV_OPAQUE
                        | IMGCV_RANDORDER | IMGCV_DCM8),
                       OrdColorDcmOpqUnsImageConvert);
#endif /* NEED_IMAGE_CONVERT */
    } else {
        free (awt_data->awtImage);
        return 0;
    }

    if (depth > 12) {
        return 1;
    }

    if (depth == 12) {
        paletteSize = MAX_PALETTE12_SIZE;
    } else {
        paletteSize = MAX_PALETTE8_SIZE;
    }

    if (awt_data->awt_num_colors > paletteSize) {
        free(awt_data->awtImage);
        return 0;
    }

    /* Allocate ColorData structure */
    awt_data->color_data = ZALLOC (_ColorData);
    if (awt_data->color_data == NULL) {
        free(awt_data->awtImage);
        return 0;
    }

    awt_data->color_data->screendata = 1; /* This ColorData struct corresponds
                                             to some AWT screen/visual, so when
                                             any IndexColorModel using this
                                             struct is finalized, don't free
                                             the struct in freeICMColorData.
                                           */

    /*
     * Initialize colors array
     */
    for (i = 0; i < awt_data->awt_num_colors; i++) {
        cols[i].pixel = i;
    }

    awt_data->color_data->awt_Colors =
        (ColorEntry *)calloc(paletteSize, sizeof (ColorEntry));
    if (awt_data->color_data->awt_Colors == NULL) {
        free(awt_data->awtImage);
        free(awt_data->color_data);
        return 0;
    }

    XQueryColors(dpy, cm, cols, awt_data->awt_num_colors);
    for (i = 0; i < awt_data->awt_num_colors; i++) {
        awt_data->color_data->awt_Colors[i].r = cols[i].red >> 8;
        awt_data->color_data->awt_Colors[i].g = cols[i].green >> 8;
        awt_data->color_data->awt_Colors[i].b = cols[i].blue >> 8;
        awt_data->color_data->awt_Colors[i].flags = LIKELY_COLOR;
    }

    /*
     * Determine which colors in the colormap can be allocated and mark
     * them in the colors array
     */
    nfree = 0;
    for (i = (paletteSize / 2); i > 0; i >>= 1) {
        if (XAllocColorCells(dpy, cm, False, plane_masks, 0,
                             freecolors + nfree, i)) {
            nfree += i;
        }
    }

    for (i = 0; i < nfree; i++) {
        awt_data->color_data->awt_Colors[freecolors[i]].flags = FREE_COLOR;
    }

#ifdef DEBUG
    if (debug_colormap) {
        jio_fprintf(stdout, "%d free.\n", nfree);
    }
#endif

    XFreeColors(dpy, cm, freecolors, nfree, 0);

    /*
     * Allocate the colors that are already allocated by other
     * applications
     */
    for (i = 0; i < awt_data->awt_num_colors; i++) {
        if (awt_data->color_data->awt_Colors[i].flags == LIKELY_COLOR) {
            awt_data->color_data->awt_Colors[i].flags = FREE_COLOR;
            alloc_col(dpy, cm,
                      awt_data->color_data->awt_Colors[i].r,
                      awt_data->color_data->awt_Colors[i].g,
                      awt_data->color_data->awt_Colors[i].b, i, awt_data);
        }
    }
#ifdef DEBUG
    if (debug_colormap) {
        jio_fprintf(stdout, "got the already allocated ones\n");
    }
#endif

    /*
     * Allocate more colors, filling the color space evenly.
     */

    alloc_col(dpy, cm, 255, 255, 255, -1, awt_data);
    alloc_col(dpy, cm, 0, 0, 0, -1, awt_data);

    if (awt_data->awtImage->clrdata.grayscale) {
        int g;
        ColorEntry *p;

        if (!forcemono) {
            for (i = 128; i > 0; i >>= 1) {
                for (g = i; g < 256; g += (i * 2)) {
                    alloc_col(dpy, cm, g, g, g, -1, awt_data);
                }
            }
        }

        awt_data->color_data->img_grays =
            (unsigned char *)calloc(256, sizeof(unsigned char));
        if ( awt_data->color_data->img_grays == NULL) {
            free(awt_data->awtImage);
            free(awt_data->color_data);
            return 0;
        }
        for (g = 0; g < 256; g++) {
            int mindist, besti;
            int d;

            p = awt_data->color_data->awt_Colors;
            mindist = 256;
            besti = 0;
            for (i = 0 ; i < awt_data->awt_num_colors ; i++, p++) {
                if (forcegray && (p->r != p->g || p->g != p->b))
                    continue;
                if (forcemono && p->g != 0 && p->g != 255)
                    continue;
                if (p->flags == ALLOCATED_COLOR) {
                    d = p->g - g;
                    if (d < 0) d = -d;
                    if (d < mindist) {
                        besti = i;
                        if (d == 0) {
                            break;
                        }
                        mindist = d;
                    }
                }
            }

            awt_data->color_data->img_grays[g] = besti;
        }


        if (forcemono || (depth == 1)) {
            char *gammastr = getenv("HJGAMMA");
            double gamma = atof(gammastr ? gammastr : "1.6");
            if (gamma < 0.01) gamma = 1.0;
#ifdef DEBUG
            if (debug_colormap) {
                jio_fprintf(stderr, "gamma = %f\n", gamma);
            }
#endif
            for (i = 0; i < 256; i++) {
                img_bwgamma[i] = (int) (pow(i/255.0, gamma) * 255);
#ifdef DEBUG
                if (debug_colormap) {
                    jio_fprintf(stderr, "%3d ", img_bwgamma[i]);
                    if ((i & 7) == 7)
                        jio_fprintf(stderr, "\n");
                }
#endif
            }
        } else {
            for (i = 0; i < 256; i++) {
                img_bwgamma[i] = i;
            }
        }

#ifdef DEBUG
        if (debug_colormap) {
            jio_fprintf(stderr, "GrayScale initialized\n");
            jio_fprintf(stderr, "color table:\n");
            for (i = 0; i < awt_data->awt_num_colors; i++) {
                jio_fprintf(stderr, "%3d: %3d %3d %3d\n",
                        i, awt_data->color_data->awt_Colors[i].r,
                        awt_data->color_data->awt_Colors[i].g,
                        awt_data->color_data->awt_Colors[i].b);
            }
            jio_fprintf(stderr, "gray table:\n");
            for (i = 0; i < 256; i++) {
                jio_fprintf(stderr, "%3d ", awt_data->color_data->img_grays[i]);
                if ((i & 7) == 7)
                    jio_fprintf(stderr, "\n");
            }
        }
#endif

    } else {

        alloc_col(dpy, cm, 255, 0, 0, -1, awt_data);
        alloc_col(dpy, cm, 0, 255, 0, -1,awt_data);
        alloc_col(dpy, cm, 0, 0, 255, -1,awt_data);
        alloc_col(dpy, cm, 255, 255, 0, -1,awt_data);
        alloc_col(dpy, cm, 255, 0, 255, -1,awt_data);
        alloc_col(dpy, cm, 0, 255, 255, -1,awt_data);
        alloc_col(dpy, cm, 192, 192, 192, -1,awt_data);
        alloc_col(dpy, cm, 255, 128, 128, -1,awt_data);
        alloc_col(dpy, cm, 128, 255, 128, -1,awt_data);
        alloc_col(dpy, cm, 128, 128, 255, -1,awt_data);
        alloc_col(dpy, cm, 255, 255, 128, -1,awt_data);
        alloc_col(dpy, cm, 255, 128, 255, -1,awt_data);
        alloc_col(dpy, cm, 128, 255, 255, -1,awt_data);
    }

    allocatedColorsNum = 0;
    unavailableColorsNum = 0;
    /* we do not support more than 256 entries in the colormap
       even for 12-bit PseudoColor visuals */
    for (i = 0; i < MAX_PALETTE8_SIZE; i++) {
        if (awt_data->color_data->awt_Colors[i].flags == ALLOCATED_COLOR)
        {
            reds[allocatedColorsNum] = awt_data->color_data->awt_Colors[i].r;
            greens[allocatedColorsNum] = awt_data->color_data->awt_Colors[i].g;
            blues[allocatedColorsNum] = awt_data->color_data->awt_Colors[i].b;
            allocatedColorsNum++;
        } else if (awt_data->color_data->awt_Colors[i].flags ==
                                                        UNAVAILABLE_COLOR) {
            unavailableColorsNum++;
        }
    }

    if (depth > 8) {
        cmapsize = MAX_PALETTE8_SIZE - unavailableColorsNum;
    } else {
        cmapsize = 0;
        if (getenv("CMAPSIZE") != 0) {
            cmapsize = atoi(getenv("CMAPSIZE"));
        }

        if (cmapsize <= 0) {
            cmapsize = CMAP_ALLOC_DEFAULT;
        }

        if (cmapsize < allocatedColorsNum + unavailableColorsNum + CMAP_ALLOC_MIN) {
            cmapsize = allocatedColorsNum + unavailableColorsNum + CMAP_ALLOC_MIN;
        }

        if (cmapsize > CMAP_ALLOC_MAX) {
            cmapsize = CMAP_ALLOC_MAX;
        }

        if (cmapsize < allocatedColorsNum) {
            cmapsize = allocatedColorsNum;
        }
        cmapsize -= unavailableColorsNum;
    }

    k = 0;
    if (getenv("VIRTCUBESIZE") != 0) {
        k = atoi(getenv("VIRTCUBESIZE"));
    }
    if (k == 0 || (k & (k - 1)) != 0 || k > 32) {
        k = getVirtCubeSize();
    }
    awt_data->color_data->img_clr_tbl =
        (unsigned char *)calloc(LOOKUPSIZE * LOOKUPSIZE * LOOKUPSIZE,
                                sizeof(unsigned char));
    if (awt_data->color_data->img_clr_tbl == NULL) {
        free(awt_data->awtImage);
        free(awt_data->color_data);
        return 0;
    }
    img_makePalette(cmapsize, k, LOOKUPSIZE, 50, 250,
                    allocatedColorsNum, TRUE, reds, greens, blues,
                    awt_data->color_data->img_clr_tbl);
                    /*img_clr_tbl);*/

    for (i = 0; i < cmapsize; i++) {
        indices[i] = alloc_col(dpy, cm, reds[i], greens[i], blues[i], -1,
                               awt_data);
    }
    for (i = 0; i < LOOKUPSIZE * LOOKUPSIZE * LOOKUPSIZE  ; i++) {
        awt_data->color_data->img_clr_tbl[i] =
            indices[awt_data->color_data->img_clr_tbl[i]];
    }

    awt_data->color_data->img_oda_red   = &(std_img_oda_red[0][0]);
    awt_data->color_data->img_oda_green = &(std_img_oda_green[0][0]);
    awt_data->color_data->img_oda_blue  = &(std_img_oda_blue[0][0]);
    make_dither_arrays(cmapsize, awt_data->color_data);
    std_odas_computed = 1;

#ifdef DEBUG
    if (debug_colormap) {
        int alloc_count = 0;
        int reuse_count = 0;
        int free_count = 0;
        for (i = 0; i < awt_data->awt_num_colors; i++) {
            switch (awt_data->color_data->awt_Colors[i].flags) {
              case ALLOCATED_COLOR:
                alloc_count++;
                break;
              case LIKELY_COLOR:
                reuse_count++;
                break;
              case FREE_COLOR:
                free_count++;
                break;
            }
        }
        jio_fprintf(stdout, "%d total, %d allocated, %d reused, %d still free.\n",
                    awt_data->awt_num_colors, alloc_count, reuse_count, free_count);
    }
#endif

    /* Fill in the ICM lut and lut2cmap mapping */
    awt_data->color_data->awt_numICMcolors = 0;
    awt_data->color_data->awt_icmLUT2Colors =
        (unsigned char *)calloc(paletteSize, sizeof (unsigned char));
    awt_data->color_data->awt_icmLUT = (int *)calloc(paletteSize, sizeof(int));
    if (awt_data->color_data->awt_icmLUT2Colors == NULL || awt_data->color_data->awt_icmLUT == NULL) {
        free(awt_data->awtImage);
        free(awt_data->color_data);
        return 0;
    }

    for (i=0; i < paletteSize; i++) {
        /* Keep the mapping between this lut and the actual cmap */
        awt_data->color_data->awt_icmLUT2Colors
            [awt_data->color_data->awt_numICMcolors] = i;

        if (awt_data->color_data->awt_Colors[i].flags == ALLOCATED_COLOR) {
            /* Screen IndexColorModel LUTS are always xRGB */
            awt_data->color_data->awt_icmLUT
                    [awt_data->color_data->awt_numICMcolors++] = 0xff000000 |
                (awt_data->color_data->awt_Colors[i].r<<16) |
                (awt_data->color_data->awt_Colors[i].g<<8) |
                (awt_data->color_data->awt_Colors[i].b);
        } else {
            /* Screen IndexColorModel LUTS are always xRGB */
            awt_data->color_data->awt_icmLUT
                        [awt_data->color_data->awt_numICMcolors++] = 0;
        }
    }
    return 1;
}
#endif /* !HEADLESS */

#define red(v)          (((v) >> 16) & 0xFF)
#define green(v)        (((v) >>  8) & 0xFF)
#define blue(v)         (((v) >>  0) & 0xFF)

#ifndef HEADLESS

jobject getColorSpace(JNIEnv* env, jint csID) {
    jclass clazz;
    jobject cspaceL;
    jmethodID mid;

    clazz = (*env)->FindClass(env,"java/awt/color/ColorSpace");
    CHECK_NULL_RETURN(clazz, NULL);
    mid = (*env)->GetStaticMethodID(env, clazz, "getInstance",
                                    "(I)Ljava/awt/color/ColorSpace;");
    CHECK_NULL_RETURN(mid, NULL);

    /* SECURITY: This is safe, because static methods cannot
     *           be overridden, and this method does not invoke
     *           client code
     */

    return (*env)->CallStaticObjectMethod(env, clazz, mid, csID);
}

jobject awtJNI_GetColorModel(JNIEnv *env, AwtGraphicsConfigDataPtr aData)
{
    jobject awt_colormodel = NULL;
    jclass clazz;
    jmethodID mid;

    if ((*env)->PushLocalFrame(env, 16) < 0)
        return NULL;

    if ((aData->awt_visInfo.class == TrueColor) &&
        (aData->awt_depth >= 15))
    {
        clazz = (*env)->FindClass(env,"java/awt/image/DirectColorModel");
        if (clazz == NULL) {
            (*env)->PopLocalFrame(env, 0);
            return NULL;
        }

        if (!aData->isTranslucencySupported) {

            mid = (*env)->GetMethodID(env,clazz,"<init>","(IIIII)V");

            if (mid == NULL) {
                (*env)->PopLocalFrame(env, 0);
                return NULL;
            }
            awt_colormodel = (*env)->NewObject(env,clazz, mid,
                    aData->awt_visInfo.depth,
                    aData->awt_visInfo.red_mask,
                    aData->awt_visInfo.green_mask,
                    aData->awt_visInfo.blue_mask,
                    0);
        } else {
            clazz = (*env)->FindClass(env,"sun/awt/X11GraphicsConfig");
            if (clazz == NULL) {
                (*env)->PopLocalFrame(env, 0);
                return NULL;
            }

            if (aData->renderPictFormat.direct.red == 16) {
                mid = (*env)->GetStaticMethodID( env,clazz,"createDCM32",
                        "(IIIIZ)Ljava/awt/image/DirectColorModel;");

                if (mid == NULL) {
                    (*env)->PopLocalFrame(env, 0);
                    return NULL;
                }

                awt_colormodel = (*env)->CallStaticObjectMethod(
                        env,clazz, mid,
                        aData->renderPictFormat.direct.redMask
                            << aData->renderPictFormat.direct.red,
                        aData->renderPictFormat.direct.greenMask
                            << aData->renderPictFormat.direct.green,
                        aData->renderPictFormat.direct.blueMask
                            << aData->renderPictFormat.direct.blue,
                        aData->renderPictFormat.direct.alphaMask
                            << aData->renderPictFormat.direct.alpha,
                        JNI_TRUE);
            } else {
                mid = (*env)->GetStaticMethodID( env,clazz,"createABGRCCM",
                        "()Ljava/awt/image/ComponentColorModel;");

                if (mid == NULL) {
                    (*env)->PopLocalFrame(env, 0);
                    return NULL;
                }

                awt_colormodel = (*env)->CallStaticObjectMethod(
                        env,clazz, mid);
            }
        }

        if(awt_colormodel == NULL)
        {
            (*env)->PopLocalFrame(env, 0);
            return NULL;
        }

    }
    else if (aData->awt_visInfo.class == StaticGray &&
             aData->awt_num_colors == 256) {
        jobject cspace = NULL;
        jint bits[1];
        jintArray bitsArray;
        jboolean falseboolean = JNI_FALSE;

        cspace = getColorSpace(env, java_awt_color_ColorSpace_CS_GRAY);

        if (cspace == NULL) {
            (*env)->PopLocalFrame(env, 0);
            return NULL;
        }

        bits[0] = 8;
        bitsArray = (*env)->NewIntArray(env, 1);
        if (bitsArray == NULL) {
            (*env)->PopLocalFrame(env, 0);
            return NULL;
        } else {
            (*env)->SetIntArrayRegion(env, bitsArray, 0, 1, bits);
        }

        clazz = (*env)->FindClass(env,"java/awt/image/ComponentColorModel");
        if (clazz == NULL) {
            (*env)->PopLocalFrame(env, 0);
            return NULL;
        }

        mid = (*env)->GetMethodID(env,clazz,"<init>",
            "(Ljava/awt/color/ColorSpace;[IZZII)V");

        if (mid == NULL) {
            (*env)->PopLocalFrame(env, 0);
            return NULL;
        }

        awt_colormodel = (*env)->NewObject(env,clazz, mid,
                                           cspace,
                                           bitsArray,
                                           falseboolean,
                                           falseboolean,
                                           java_awt_Transparency_OPAQUE,
                                           java_awt_image_DataBuffer_TYPE_BYTE);

        if(awt_colormodel == NULL)
        {
            (*env)->PopLocalFrame(env, 0);
            return NULL;
        }

    } else {
        jint rgb[MAX_PALETTE_SIZE];
        jbyte valid[MAX_PALETTE_SIZE / 8], *pValid;
        jintArray hArray;
        jobject validBits = NULL;
        ColorEntry *c;
        int i, allocAllGray, b, allvalid, paletteSize;
        jlong pData;

        if (aData->awt_visInfo.depth == 12) {
            paletteSize = MAX_PALETTE12_SIZE;
        } else {
            paletteSize = MAX_PALETTE8_SIZE;
        }

        c = aData->color_data->awt_Colors;
        pValid = &valid[sizeof(valid)];
        allocAllGray = 1;
        b = 0;
        allvalid = 1;

        for (i = 0; i < paletteSize; i++, c++) {
            if (c->flags == ALLOCATED_COLOR) {
                rgb[i] = (0xff000000 |
                          (c->r << 16) |
                          (c->g <<  8) |
                          (c->b <<  0));
                if (c->r != c->g || c->g != c->b) {
                    allocAllGray = 0;
                }
                b |= (1 << (i % 8));
            } else {
                rgb[i] = 0;
                b &= ~(1 << (i % 8));
                allvalid = 0;
            }
            if ((i % 8) == 7) {
                *--pValid = b;
                /* b = 0; not needed as each bit is explicitly set */
            }
        }

        if (allocAllGray && (aData->awtImage->clrdata.grayscale == 0)) {
            /*
              Fix for 4351638 - Gray scale HW mode on Dome frame buffer
                                crashes VM on Solaris.
              It is possible for an X11 frame buffer to advertise a
              PseudoColor visual, but to force all allocated colormap
              entries to be gray colors.  The Dome card does this when the
              HW is jumpered for a grayscale monitor, but the default
              visual is set to PseudoColor.  In that case awtJNI_GetColorModel
              will be called with aData->awtImage->clrdata.grayscale == 0,
              but the IndexColorModel created below will detect that only
              gray colors exist and expect the inverse gray LUT to exist.
              So above when filling the hR, hG, and hB arrays we detect
              whether all allocated colors are gray.  If so, but
              aData->awtImage->clrdata.grayscale == 0, we fall into this
              code to set aData->awtImage->clrdata.grayscale = 1 and do
              other things needed for the grayscale case.
             */

            int i;
            int g;
            ColorEntry *p;

            aData->awtImage->clrdata.grayscale = 1;

            aData->color_data->img_grays =
                (unsigned char *)calloc(256, sizeof(unsigned char));

            if (aData->color_data->img_grays == NULL) {
                (*env)->PopLocalFrame(env, 0);
                return NULL;
            }

            for (g = 0; g < 256; g++) {
                int mindist, besti;
                int d;

                p = aData->color_data->awt_Colors;
                mindist = 256;
                besti = 0;
                for (i = 0 ; i < paletteSize; i++, p++) {
                    if (p->flags == ALLOCATED_COLOR) {
                        d = p->g - g;
                        if (d < 0) d = -d;
                        if (d < mindist) {
                            besti = i;
                            if (d == 0) {
                                break;
                            }
                            mindist = d;
                        }
                    }
                }

                aData->color_data->img_grays[g] = besti;
            }

            for (i = 0; i < 256; i++) {
                img_bwgamma[i] = i;    /* REMIND: what is img_bwgamma?
                                        *         is it still used anywhere?
                                        */
            }
        }

        if (aData->awtImage->clrdata.grayscale) {
            int i;
            ColorEntry *p;

            /* For purposes of creating an IndexColorModel, use
               transparent black for non-allocated or non-gray colors.
             */
            p = aData->color_data->awt_Colors;
            b = 0;
            pValid = &valid[sizeof(valid)];
            for (i = 0; i < paletteSize; i++, p++) {
                if ((p->flags != ALLOCATED_COLOR) ||
                    (p->r != p->g || p->g != p->b))
                {
                    rgb[i] = 0;
                    b &= ~(1 << (i % 8));
                    allvalid = 0;
                } else {
                    b |= (1 << (i % 8));
                }
                if ((i % 8) == 7) {
                    *--pValid = b;
                    /* b = 0; not needed as each bit is explicitly set */
                }
            }

            if (aData->color_data->pGrayInverseLutData == NULL) {
                /* Compute the inverse gray LUT for this aData->color_data
                   struct, if not already computed.
                 */
                initInverseGrayLut(rgb, aData->awt_num_colors,
                                   aData->color_data);
            }
        }

        if (!allvalid) {
            jobject bArray = (*env)->NewByteArray(env, sizeof(valid));
            if (bArray == NULL)
            {
                (*env)->PopLocalFrame(env, 0);
                return NULL;
            }
            else
            {
                (*env)->SetByteArrayRegion(env, bArray, 0, sizeof(valid),
                                           valid);
            }
            validBits = JNU_NewObjectByName(env,
                                            "java/math/BigInteger",
                                            "([B)V", bArray);
            if (validBits == NULL)
            {
                (*env)->PopLocalFrame(env, 0);
                return NULL;
            }
        }

        hArray = (*env)->NewIntArray(env, paletteSize);
        if (hArray == NULL)
        {
            (*env)->PopLocalFrame(env, 0);
            return NULL;
        }
        else
        {
            (*env)->SetIntArrayRegion(env, hArray, 0, paletteSize, rgb);
        }

        if (aData->awt_visInfo.depth == 8) {
            awt_colormodel =
                JNU_NewObjectByName(env,
                                    "java/awt/image/IndexColorModel",
                                    "(II[IIILjava/math/BigInteger;)V",
                                    8, 256, hArray, 0,
                                    java_awt_image_DataBuffer_TYPE_BYTE,
                                    validBits);
        } else {
            awt_colormodel =
                JNU_NewObjectByName(env,
                                    "java/awt/image/IndexColorModel",
                                    "(II[IIILjava/math/BigInteger;)V",
                                    12, 4096, hArray, 0,
                                    java_awt_image_DataBuffer_TYPE_USHORT,
                                    validBits);
        }

        if (awt_colormodel == NULL)
        {
            (*env)->PopLocalFrame(env, 0);
            return NULL;
        }

        /* Set pData field of ColorModel to point to ColorData */
        JNU_SetLongFieldFromPtr(env, awt_colormodel, g_CMpDataID,
                                aData->color_data);

    }

    return (*env)->PopLocalFrame(env, awt_colormodel);
}
#endif /* !HEADLESS */

extern jfieldID colorValueID;

#ifndef HEADLESS
void
awt_allocate_systemrgbcolors (jint *rgbColors, int num_colors,
                              AwtGraphicsConfigDataPtr awtData) {
    int i, pixel;
    for (i = 0; i < num_colors; i++)
        pixel = alloc_col (awt_display, awtData->awt_cmap, red (rgbColors [i]),
                           green (rgbColors [i]), blue (rgbColors [i]), -1,
                           awtData);
}

int
awtCreateX11Colormap(AwtGraphicsConfigDataPtr adata) {
    int screen = adata->awt_visInfo.screen;
    Colormap cmap = (Colormap)NULL;

    if (adata->awt_visInfo.visual == DefaultVisual(awt_display, screen)) {
        cmap = DefaultColormap(awt_display, screen);
    } else {
        Window root = RootWindow(awt_display, screen);

        if (adata->awt_visInfo.visual->class % 2) {
            Atom actual_type;
            int actual_format;
            unsigned long nitems, bytes_after;
            XStandardColormap *scm;

            XGetWindowProperty (awt_display, root, XA_RGB_DEFAULT_MAP,
                                0L, 1L, False, AnyPropertyType, &actual_type,
                                &actual_format, &nitems, &bytes_after,
                                (unsigned char **) &scm);

            XGetWindowProperty (awt_display, root, XA_RGB_DEFAULT_MAP, 0L,
                                bytes_after/4 + 1, False, AnyPropertyType,
                                &actual_type, &actual_format, &nitems,
                                &bytes_after, (unsigned char **) &scm);

            nitems /= (sizeof (XStandardColormap)/4);
            for (; nitems > 0; ++scm, --nitems)
                if (scm->visualid == adata->awt_visInfo.visualid) {
                    cmap = scm->colormap;
                    break;
                }
        }
        if (!cmap) {
            cmap = XCreateColormap (awt_display, root,
                                    adata->awt_visInfo.visual,
                                    AllocNone);
        }
    }

    adata->awt_cmap = cmap;
    if (!awt_allocate_colors(adata)) {
        XFreeColormap(awt_display, adata->awt_cmap);
        adata->awt_cmap = (Colormap)NULL;
        return 0;
    }
    return 1;
}

void
awtJNI_CreateColorData(JNIEnv *env, AwtGraphicsConfigDataPtr adata,
                       int lock) {

    /* Create Colormap */
    if (lock) {
        AWT_LOCK ();
    }

    awtCreateX11Colormap(adata);

    /* If depth is 8, allocate system colors also...  Here
     * we just get the array of System Colors and allocate
     * it which may be a bit wasteful (if only some were
     * changed). But we don't know which ones were changed
     * and alloc-ing a pixel that is already allocated won't
     * hurt. */

    if (adata->awt_depth == 8 ||
        (adata->awt_depth == 12 && adata->awt_visInfo.class == PseudoColor))
    {
        jint colorVals [java_awt_SystemColor_NUM_COLORS];
        jclass sysColors;
        jfieldID colorID;
        jintArray colors;

        /* Unlock now to initialize the SystemColor class */
        if (lock) {
            AWT_UNLOCK_CHECK_EXCEPTION(env);
        }
        sysColors = (*env)->FindClass (env, "java/awt/SystemColor");
        CHECK_NULL(sysColors);

        if (lock) {
            AWT_LOCK ();
        }
        colorID = (*env)->GetStaticFieldID (env, sysColors,
                                                   "systemColors",
                                                   "[I");

        if (colorID == NULL) {
            if (lock) {
                AWT_UNLOCK();
            }
            return;
        }

        colors = (jintArray) (*env)->GetStaticObjectField
                                                (env, sysColors, colorID);

        (*env)->GetIntArrayRegion (env, colors, 0,
                                     java_awt_SystemColor_NUM_COLORS,
                                     (jint *) colorVals);

        awt_allocate_systemrgbcolors (colorVals,
                        (java_awt_SystemColor_NUM_COLORS - 1), adata);

    }

    if (lock) {
        AWT_UNLOCK ();
    }
}

#endif /* !HEADLESS */
