/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

#include "jni.h"
#include "dither.h"

JNIEXPORT sgn_ordered_dither_array std_img_oda_red;
JNIEXPORT sgn_ordered_dither_array std_img_oda_green;
JNIEXPORT sgn_ordered_dither_array std_img_oda_blue;
JNIEXPORT int std_odas_computed = 0;

JNIEXPORT void JNICALL
initInverseGrayLut(int* prgb, int rgbsize, ColorData *cData) {
    int *inverse;
    int lastindex, lastgray, missing, i;

    if (!cData) {
        return;
    }

    inverse = calloc(256, sizeof(int));
    if (!inverse) {
        return;
    }
    cData->pGrayInverseLutData = inverse;

    for (i = 0; i < 256; i++) {
        inverse[i] = -1;
    }

    /* First, fill the gray values */
    for (i = 0; i < rgbsize; i++) {
        int r, g, b, rgb = prgb[i];
        if (rgb == 0x0) {
            /* ignore transparent black */
            continue;
        }
        r = (rgb >> 16) & 0xff;
        g = (rgb >> 8 ) & 0xff;
        b = rgb & 0xff;
        if (b == r && b == g) {
            inverse[b] = i;
        }
    }

    /* fill the missing gaps by taking the valid values
     * on either side and filling them halfway into the gap
     */
    lastindex = -1;
    lastgray = -1;
    missing = 0;
    for (i = 0; i < 256; i++) {
        if (inverse[i] < 0) {
            inverse[i] = lastgray;
            missing = 1;
        } else {
            lastgray = inverse[i];
            if (missing) {
                lastindex = lastindex < 0 ? 0 : (i+lastindex)/2;
                while (lastindex < i) {
                    inverse[lastindex++] = lastgray;
                }
            }
            lastindex = i;
            missing = 0;
        }
    }
}

void freeICMColorData(ColorData *pData) {
    if (CANFREE(pData)) {
        if (pData->img_clr_tbl) {
            free(pData->img_clr_tbl);
        }
        if (pData->pGrayInverseLutData) {
            free(pData->pGrayInverseLutData);
        }
        free(pData);
    }
}

/* REMIND: does not deal well with bifurcation which happens when two
 * palette entries map to the same cube vertex
 */

static int
recurseLevel(CubeStateInfo *priorState) {
    int i;
    CubeStateInfo currentState;
    memcpy(&currentState, priorState, sizeof(CubeStateInfo));


    currentState.rgb = (unsigned short *)malloc(6
                                                * sizeof(unsigned short)
                                                * priorState->activeEntries);
    if (currentState.rgb == NULL) {
        return 0;
    }

    currentState.indices = (unsigned char *)malloc(6
                                                * sizeof(unsigned char)
                                                * priorState->activeEntries);

    if (currentState.indices == NULL) {
        free(currentState.rgb);
        return 0;
    }

    currentState.depth++;
    if (currentState.depth > priorState->maxDepth) {
        priorState->maxDepth = currentState.depth;
    }
    currentState.activeEntries = 0;
    for (i=priorState->activeEntries - 1; i >= 0; i--) {
        unsigned short rgb = priorState->rgb[i];
        unsigned char  index = priorState->indices[i];
        ACTIVATE(rgb, 0x7c00, 0x0400, currentState, index);
        ACTIVATE(rgb, 0x03e0, 0x0020, currentState, index);
        ACTIVATE(rgb, 0x001f, 0x0001, currentState, index);
    }
    if (currentState.activeEntries) {
        if (!recurseLevel(&currentState)) {
            free(currentState.rgb);
            free(currentState.indices);
            return 0;
        }
    }
    if (currentState.maxDepth > priorState->maxDepth) {
        priorState->maxDepth = currentState.maxDepth;
    }

    free(currentState.rgb);
    free(currentState.indices);
    return  1;
}

/*
 * REMIND: take core inversedLUT calculation to the shared tree and
 * recode the functions (Win32)awt_Image:initCubemap(),
 * (Win32)awt_Image:make_cubemap(), (Win32)AwtToolkit::GenerateInverseLUT(),
 * (Solaris)color:initCubemap() to call the shared codes.
 */
unsigned char*
initCubemap(int* cmap,
            int  cmap_len,
            int  cube_dim) {
    int i;
    CubeStateInfo currentState;
    int cubesize = cube_dim * cube_dim * cube_dim;
    unsigned char *useFlags;
    unsigned char *newILut = (unsigned char*)malloc(cubesize);
    int cmap_mid = (cmap_len >> 1) + (cmap_len & 0x1);
    if (newILut) {

      useFlags = (unsigned char *)calloc(cubesize, 1);

      if (useFlags == 0) {
          free(newILut);
#ifdef DEBUG
        fprintf(stderr, "Out of memory in color:initCubemap()1\n");
#endif
          return NULL;
      }

        currentState.depth          = 0;
        currentState.maxDepth       = 0;
        currentState.usedFlags      = useFlags;
        currentState.activeEntries  = 0;
        currentState.iLUT           = newILut;

        currentState.rgb = (unsigned short *)
                                malloc(cmap_len * sizeof(unsigned short));
        if (currentState.rgb == NULL) {
            free(newILut);
            free(useFlags);
#ifdef DEBUG
        fprintf(stderr, "Out of memory in color:initCubemap()2\n");
#endif
            return NULL;
        }

        currentState.indices = (unsigned char *)
                                malloc(cmap_len * sizeof(unsigned char));
        if (currentState.indices == NULL) {
            free(currentState.rgb);
            free(newILut);
            free(useFlags);
#ifdef DEBUG
        fprintf(stderr, "Out of memory in color:initCubemap()3\n");
#endif
            return NULL;
        }

        for (i = 0; i < cmap_mid; i++) {
            unsigned short rgb;
            int pixel = cmap[i];
            rgb = (pixel & 0x00f80000) >> 9;
            rgb |= (pixel & 0x0000f800) >> 6;
            rgb |=  (pixel & 0xf8) >> 3;
            INSERTNEW(currentState, rgb, i);
            pixel = cmap[cmap_len - i - 1];
            rgb = (pixel & 0x00f80000) >> 9;
            rgb |= (pixel & 0x0000f800) >> 6;
            rgb |=  (pixel & 0xf8) >> 3;
            INSERTNEW(currentState, rgb, cmap_len - i - 1);
        }

        if (!recurseLevel(&currentState)) {
            free(newILut);
            free(useFlags);
            free(currentState.rgb);
            free(currentState.indices);
#ifdef DEBUG
        fprintf(stderr, "Out of memory in color:initCubemap()4\n");
#endif
            return NULL;
        }

        free(useFlags);
        free(currentState.rgb);
        free(currentState.indices);

        return newILut;
    }

#ifdef DEBUG
        fprintf(stderr, "Out of memory in color:initCubemap()5\n");
#endif
    return NULL;
}

void
initDitherTables(ColorData* cData) {


    if(std_odas_computed) {
        cData->img_oda_red   = &(std_img_oda_red[0][0]);
        cData->img_oda_green = &(std_img_oda_green[0][0]);
        cData->img_oda_blue  = &(std_img_oda_blue[0][0]);
    } else {
        cData->img_oda_red   = &(std_img_oda_red[0][0]);
        cData->img_oda_green = &(std_img_oda_green[0][0]);
        cData->img_oda_blue  = &(std_img_oda_blue[0][0]);
        make_dither_arrays(256, cData);
        std_odas_computed = 1;
    }

}

JNIEXPORT void JNICALL
make_dither_arrays(int cmapsize, ColorData *cData) {
    int i, j, k;

    /*
     * Initialize the per-component ordered dithering arrays
     * Choose a size based on how far between elements in the
     * virtual cube.  Assume the cube has cuberoot(cmapsize)
     * elements per axis and those elements are distributed
     * over 256 colors.
     * The calculation should really divide by (#comp/axis - 1)
     * since the first and last elements are at the extremes of
     * the 256 levels, but in a practical sense this formula
     * produces a smaller error array which results in smoother
     * images that have slightly less color fidelity but much
     * less dithering noise, especially for grayscale images.
     */
    i = (int) (256 / pow(cmapsize, 1.0/3.0));
    make_sgn_ordered_dither_array(cData->img_oda_red, -i / 2, i / 2);
    make_sgn_ordered_dither_array(cData->img_oda_green, -i / 2, i / 2);
    make_sgn_ordered_dither_array(cData->img_oda_blue, -i / 2, i / 2);

    /*
     * Flip green horizontally and blue vertically so that
     * the errors don't line up in the 3 primary components.
     */
    for (i = 0; i < 8; i++) {
        for (j = 0; j < 4; j++) {
            k = cData->img_oda_green[(i<<3)+j];
            cData->img_oda_green[(i<<3)+j] = cData->img_oda_green[(i<<3)+7 - j];
            cData->img_oda_green[(i<<3) + 7 - j] = k;
            k = cData->img_oda_blue[(j<<3)+i];
            cData->img_oda_blue[(j<<3)+i] = cData->img_oda_blue[((7 - j)<<3)+i];
            cData->img_oda_blue[((7 - j)<<3) + i] = k;
        }
    }
}
