/*
 * Copyright (c) 1996, 2018, Oracle and/or its affiliates. All rights reserved.
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

/* Iterative color palette generation */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#ifdef TIMES
#include <time.h>
#endif /* TIMES */

#ifndef MAKECUBE_EXE
#include "jvm.h"
#include "jni_util.h"

extern JavaVM *jvm;
#endif
#include "img_colors.h"

#define jio_fprintf fprintf

#define TRUE 1
#define FALSE 0
static float monitor_gamma[3] = {2.6f, 2.6f, 2.4f}; /* r,g,b */
static float mat[3][3] = {
    {0.3811f, 0.2073f, 0.0213f},
    {0.3203f, 0.6805f, 0.1430f},
    {0.2483f, 0.1122f, 1.2417f}
};
static float whiteXYZ[3] = { 0.9497f, 1.0000f, 1.4060f };
#define whitex (0.9497f / (0.9497f + 1.0000f + 1.4060f))
#define whitey (1.0000f / (0.9497f + 1.0000f + 1.4060f))
static float uwht = 4*whitex/(-2*whitex + 12*whitey + 3);
static float vwht = 9*whitey/(-2*whitex + 12*whitey + 3);

static float Rmat[3][256];
static float Gmat[3][256];
static float Bmat[3][256];
static float Ltab[256], Utab[256], Vtab[256];

typedef struct {
    unsigned char red;
    unsigned char green;
    unsigned char blue;
    unsigned char bestidx;
    int nextidx;
    float L, U, V;
    float dist;
    float dE;
    float dL;
} CmapEntry;

static int num_virt_cmap_entries;
static CmapEntry *virt_cmap;
static int prevtest[256];
static int nexttest[256];

static float Lscale = 10.0f;
/* this is a multiplier--it should not be zero */
static float Weight = 250.0f;

#define WEIGHT_DIST(d,l)   (Weight*(d)/(Weight+(l)))
#define UNWEIGHT_DIST(d,l) ((Weight+(l))*(d)/Weight)

#if 0
#define WEIGHT_DIST(d,l) (d)
#define UNWEIGHT_DIST(d,l) (d)
#endif

static void
init_matrices()
{
    static int done = 0;
    int i;

    if (done) {
        return;
    }
    for (i = 0; i < 256; ++i)
    {
        float iG = (float) pow(i/255.0, monitor_gamma[0]);
        Rmat[0][i] = mat[0][0] * iG;
        Rmat[1][i] = mat[0][1] * iG;
        Rmat[2][i] = mat[0][2] * iG;

        iG = (float) pow(i/255.0, monitor_gamma[1]);
        Gmat[0][i] = mat[1][0] * iG;
        Gmat[1][i] = mat[1][1] * iG;
        Gmat[2][i] = mat[1][2] * iG;

        iG = (float) pow(i/255.0, monitor_gamma[2]);
        Bmat[0][i] = mat[2][0] * iG;
        Bmat[1][i] = mat[2][1] * iG;
        Bmat[2][i] = mat[2][2] * iG;
    }
    done = 1;
}

static void
LUV_convert(int red, int grn, int blu, float *L, float *u, float *v)
{
    float X = Rmat[0][red] + Gmat[0][grn] + Bmat[0][blu];
    float Y = Rmat[1][red] + Gmat[1][grn] + Bmat[1][blu];
    float Z = Rmat[2][red] + Gmat[2][grn] + Bmat[2][blu];
    float sum = X+Y+Z;

    if (sum != 0.0f) {
        float x    = X/sum;
        float y    = Y/sum;
        float dnm  = -2*x + 12*y + 3;
        float ytmp = (float) pow(Y/whiteXYZ[1], 1.0/3.0);

        if (ytmp < .206893f) {
            *L = 903.3f*Y/whiteXYZ[1];
        } else {
            *L = 116*(ytmp) - 16;
        }
        if (dnm != 0.0f) {
            float uprm = 4*x/dnm;
            float vprm = 9*y/dnm;

            *u = 13*(*L)*(uprm-uwht);
            *v = 13*(*L)*(vprm-vwht);
        } else {
            *u = 0.0f;
            *v = 0.0f;
        }
    } else {
        *L = 0.0f;
        *u = 0.0f;
        *v = 0.0f;
    }
}

static int cmapmax;
static int total;
static unsigned char cmap_r[256], cmap_g[256], cmap_b[256];

#define DIST_THRESHOLD 7
static int
no_close_color(float l, float u, float v, int c_tot, int exact) {
    int i;
    for (i = 0; i < c_tot; ++i) {
        float t, dist = 0.0f;
        t = Ltab[i] - l; dist += t*t*Lscale;
        t = Utab[i] - u; dist += t*t;
        t = Vtab[i] - v; dist += t*t;

        if (dist < (exact ? 0.1 : DIST_THRESHOLD))
            return 0;
    }

    return 1;
}

static int
add_color(int r, int g, int b, int f) {
    if (total >= cmapmax)
        return 0;
    cmap_r[total] = r;
    cmap_g[total] = g;
    cmap_b[total] = b;
    LUV_convert(cmap_r[total],cmap_g[total],cmap_b[total],
                Ltab + total, Utab + total, Vtab + total);
    if (no_close_color(Ltab[total], Utab[total], Vtab[total], total-1, f)) {
        ++total;
        return 1;
    } else {
        return 0;
    }
}

static void
init_primaries() {
    int r, g, b;

    for (r = 0; r < 256; r += (r?128:127)) {
        for (g = 0; g < 256; g += (g?128:127)) {
            for (b = 0; b < 256; b += (b?128:127)) {
                if ((r == g) && (g == b)) continue; /* black or white */
                add_color(r, g, b, TRUE);
            }
        }
    }
}

static void
init_pastels() {
    int i;
    /* very light colors */
    for (i = 6; i >= 0; --i)
        add_color((i&4) ? 0xff : 0xf0,
                  (i&2) ? 0xff : 0xf0,
                  (i&1) ? 0xff : 0xf0, TRUE);
}

static void
init_grays() {
    int i;
    for (i = 15; i < 255; i += 16)
        add_color(i, i, i, TRUE);
}

static void
init_mac_palette() {
    add_color(255, 255, 204, TRUE);
    add_color(255, 255, 0,   TRUE);
    add_color(255, 204, 153, TRUE);
    add_color(255, 102, 204, TRUE);
    add_color(255, 102, 51,  TRUE);
    add_color(221, 0, 0,     TRUE);
    add_color(204, 204, 255, TRUE);
    add_color(204, 153, 102, TRUE);
    add_color(153, 255, 255, TRUE);
    add_color(153, 153, 255, TRUE);
    add_color(153, 102, 153, TRUE);
    add_color(153, 0, 102,   TRUE);
    add_color(102, 102, 204, TRUE);
    add_color(51, 255, 153,  TRUE);
    add_color(51, 153, 102,  TRUE);
    add_color(51, 102, 102,  TRUE);
    add_color(51, 51, 102,   TRUE);
    add_color(51, 0, 153,    TRUE);
    add_color(0, 187, 0,     TRUE);
    add_color(0, 153, 255,   TRUE);
    add_color(0, 0, 221,     TRUE);
}

static void
init_virt_cmap(int tablesize, int testsize)
{
    int r, g, b;
    int gray = -1;
    CmapEntry *pCmap;
    unsigned int dotest[256];

    if (virt_cmap) {
        free(virt_cmap);
        virt_cmap = 0;
    }

    num_virt_cmap_entries = tablesize * tablesize * tablesize;
    virt_cmap = malloc(sizeof(CmapEntry) * num_virt_cmap_entries);
    /*
     * Fix for bug 4070647 malloc return value not check in img_colors.c
     * We have to handle the malloc failure differently under
     * Win32 and Solaris since under Solaris this file is linked with
     * libawt.so and under Win32 it's a separate awt_makecube.exe
     * application.
     */
    if (virt_cmap == NULL) {
#ifndef MAKECUBE_EXE
        JNIEnv *env = (JNIEnv *)JNU_GetEnv(jvm, JNI_VERSION_1_2);
        JNU_ThrowOutOfMemoryError(env, "init_virt_cmap: OutOfMemoryError");
        return;
#else
        fprintf(stderr,"init_virt_cmap: OutOfMemoryError\n");
        exit(-1);
#endif
    }
    pCmap = virt_cmap;
    for (r = 0; r < total; r++) {
        if (cmap_r[r] == cmap_g[r] && cmap_g[r] == cmap_b[r]) {
            if (gray < 0 || cmap_r[gray] < cmap_r[r]) {
                gray = r;
            }
        }
    }
    if (gray < 0) {
#ifdef DEBUG
        jio_fprintf(stderr, "Didn't find any grays in color table!\n");
#endif /* DEBUG */
        gray = 0;
    }
    g = 0;
    b = 0;
    for (r = 0; r < tablesize - 1; ++r) {
        if (g >= 0) {
            b = r;
            dotest[r] = 1;
            g -= tablesize;
        } else {
            dotest[r] = 0;
        }
        prevtest[r] = b;
        g += testsize;
    }
    b = r;
    prevtest[r] = b;
    dotest[r] = 1;
    for (r = tablesize - 1; r >= 0; --r) {
        if (prevtest[r] == r) {
            b = r;
        }
        nexttest[r] = b;
    }
#ifdef DEBUG
    for (r = 0; r < tablesize; ++r) {
        if (dotest[r]) {
            if (prevtest[r] != r || nexttest[r] != r) {
                jio_fprintf(stderr, "prev/next != r!\n");
            }
        }
    }
#endif /* DEBUG */
    for (r = 0; r < tablesize; ++r)
    {
        int red = (int)(floor(r*255.0/(tablesize - 1)));
        for (g = 0; g < tablesize; ++g)
        {
            int green = (int)(floor(g*255.0/(tablesize - 1)));
            for (b = 0; b < tablesize; ++b)
            {
                int blue = (int)(floor(b*255.0/(tablesize - 1)));
                float t, d;
                if (pCmap >= virt_cmap + num_virt_cmap_entries) {
#ifdef DEBUG
                    jio_fprintf(stderr, "OUT OF pCmap CONVERSION SPACE!\n");
#endif /* DEBUG */
                    continue;           /* Shouldn't happen */
                }
                pCmap->red = red;
                pCmap->green = green;
                pCmap->blue = blue;
                LUV_convert(red, green, blue, &pCmap->L, &pCmap->U, &pCmap->V);
                if ((red != green || green != blue) &&
                    (!dotest[r] || !dotest[g] || !dotest[b]))
                {
                    pCmap->nextidx = -1;
                    pCmap++;
                    continue;
                }
                pCmap->bestidx = gray;
                pCmap->nextidx = 0;
                t = Ltab[gray] - pCmap->L;
                d = t * t;
                if (red == green && green == blue) {
                    pCmap->dist = d;
                    d *= Lscale;
                } else {
                    d *= Lscale;
                    t = Utab[gray] - pCmap->U;
                    d += t * t;
                    t = Vtab[gray] - pCmap->V;
                    d += t * t;
                    pCmap->dist = d;
                }
                pCmap->dE = WEIGHT_DIST(d, pCmap->L);
                pCmap++;
            }
        }
    }
#ifdef DEBUG
    if (pCmap < virt_cmap + num_virt_cmap_entries) {
        jio_fprintf(stderr, "Didn't fill pCmap conversion table!\n");
    }
#endif /* DEBUG */
}

static int
find_nearest(CmapEntry *pCmap) {
    int red = pCmap->red;
    int grn = pCmap->green;
    int blu = pCmap->blue;
    float L = pCmap->L;
    float dist;
    int i;

    if ((red == grn) && (grn == blu)) {
        dist = pCmap->dist;

        for (i = pCmap->nextidx; i < total; ++i) {
            float dL;

            if (cmap_r[i] != cmap_g[i] || cmap_g[i] != cmap_b[i]) {
                continue;
            }

            dL = Ltab[i] - L; dL *= dL;

            if (dL < dist) {
                dist = dL;
                pCmap->dist = dist;
                pCmap->dL = dist;
                pCmap->dE = WEIGHT_DIST(dist*Lscale,L);
                pCmap->bestidx = i;
            }
        }
        pCmap->nextidx = total;
    } else {
        float U = pCmap->U;
        float V = pCmap->V;
        dist = pCmap->dist;

        for (i = pCmap->nextidx; i < total; ++i) {
            float dL, dU, dV, dE;
            dL = Ltab[i] - L; dL *= (dL*Lscale);
            dU = Utab[i] - U; dU *= dU;
            dV = Vtab[i] - V; dV *= dV;

            dE = dL + dU + dV;
            if (dE < dist)
            {
                dist = dE;
                /* *delta = (dL/4) + dU + dV; */
                /* *delta = dist */
                /* *delta = dL + 100*(dU+dV)/(100+L); */
                pCmap->dist = dist;
                pCmap->dE = WEIGHT_DIST(dE, L);
                pCmap->dL = dL/Lscale;
                pCmap->bestidx = i;
            }
        }
        pCmap->nextidx = total;
    }

    return pCmap->bestidx;
}

#define MAX_OFFENDERS 32
static CmapEntry *offenders[MAX_OFFENDERS + 1];
static int num_offenders;

static void
insert_in_list(CmapEntry *pCmap)
{
    int i;
    float dE = pCmap->dE;

    for (i = num_offenders; i > 0; --i) {
        if (dE < offenders[i-1]->dE) break;
        offenders[i] = offenders[i-1];
    }

    offenders[i] = pCmap;
    if (num_offenders < MAX_OFFENDERS) ++num_offenders;
}

static void
handle_biggest_offenders(int testtblsize, int maxcolors) {
    int i, j;
    float dEthresh = 0;
    CmapEntry *pCmap;

    num_offenders = 0;

    for (pCmap = virt_cmap, i = 0; i < num_virt_cmap_entries; i++, pCmap++) {
        if (pCmap->nextidx < 0) {
            continue;
        }
        if (num_offenders == MAX_OFFENDERS
            && pCmap->dE < offenders[MAX_OFFENDERS-1]->dE)
        {
            continue;
        }
        find_nearest(pCmap);
        insert_in_list(pCmap);
    }

    if (num_offenders > 0) {
        dEthresh = offenders[num_offenders-1]->dE;
    }

    for (i = 0; (total < maxcolors) && (i < num_offenders); ++i) {
        pCmap = offenders[i];

        if (!pCmap) continue;

        j = add_color(pCmap->red, pCmap->green, pCmap->blue, FALSE);

        if (j) {
            for (j = i+1; j < num_offenders; ++j) {
                float dE;

                pCmap = offenders[j];
                if (!pCmap) {
                    continue;
                }

                find_nearest(pCmap);

                dE = pCmap->dE;
                if (dE < dEthresh) {
                    offenders[j] = 0;
                } else {
                    if (offenders[i+1] == 0 || dE > offenders[i+1]->dE) {
                        offenders[j] = offenders[i+1];
                        offenders[i+1] = pCmap;
                    }
                }
            }
        }
    }
}

JNIEXPORT void JNICALL
img_makePalette(int cmapsize, int tablesize, int lookupsize,
                float lscale, float weight,
                int prevclrs, int doMac,
                unsigned char *reds,
                unsigned char *greens,
                unsigned char *blues,
                unsigned char *lookup)
{
    CmapEntry *pCmap;
    int i, ix;
#ifdef STATS
    double ave_dL, ave_dE;
    double max_dL, max_dE;
#endif /* STATS */
#ifdef TIMES
    clock_t start, mid, tbl, end;

    start = clock();
#endif /* TIMES */

    init_matrices();
    Lscale = lscale;
    Weight = weight;

    cmapmax = cmapsize;
    total = 0;
    for (i = 0; i < prevclrs; i++) {
        add_color(reds[i], greens[i], blues[i], TRUE);
    }

    add_color(0, 0, 0, TRUE);
    add_color(255,255,255, TRUE);

    /* do grays next; otherwise find_nearest may break! */
    init_grays();
    if (doMac) {
        init_mac_palette();
    }
    init_pastels();

    init_primaries();

    /* special case some blues */
    add_color(0,0,192,TRUE);
    add_color(0x30,0x20,0x80,TRUE);
    add_color(0x20,0x60,0xc0,TRUE);

    init_virt_cmap(lookupsize, tablesize);

    while (total < cmapsize) {
        handle_biggest_offenders(tablesize, cmapsize);
    }

    memcpy(reds, cmap_r, cmapsize);
    memcpy(greens, cmap_g, cmapsize);
    memcpy(blues, cmap_b, cmapsize);

#ifdef TIMES
    mid = clock();
#endif /* TIMES */

    pCmap = virt_cmap;
    for (i = 0; i < num_virt_cmap_entries; i++, pCmap++) {
        if (pCmap->nextidx < 0) {
            continue;
        }
        if (pCmap->nextidx < total) {
            ix = find_nearest(pCmap);
        }
    }

#ifdef TIMES
    tbl = clock();
#endif /* TIMES */

    pCmap = virt_cmap;
    if (tablesize != lookupsize) {
        int r, g, b;
        for (r = 0; r < lookupsize; ++r)
        {
            for (g = 0; g < lookupsize; ++g)
            {
                for (b = 0; b < lookupsize; ++b, pCmap++)
                {
                    float L, U, V;
                    float bestd = 0;
                    CmapEntry *pTest;

                    if (pCmap->nextidx >= 0) {
                        continue;
                    }
#ifdef DEBUG
                    if (r == g && g == b) {
                        jio_fprintf(stderr, "GRAY VALUE!?\n");
                    }
#endif /* DEBUG */
                    L = pCmap->L;
                    U = pCmap->U;
                    V = pCmap->V;
                    for (i = 0; i < 8; i++) {
                        int ri, gi, bi;
                        float d, t;
                        ri = (i & 1) ? prevtest[r] : nexttest[r];
                        gi = (i & 2) ? prevtest[g] : nexttest[g];
                        bi = (i & 4) ? prevtest[b] : nexttest[b];
                        pTest = &virt_cmap[((ri * lookupsize)
                                            + gi) * lookupsize
                                           + bi];
#ifdef DEBUG
                        if (pTest->nextidx < 0) {
                            jio_fprintf(stderr, "OOPS!\n");
                        }
#endif /* DEBUG */
                        ix = pTest->bestidx;
                        t = Ltab[ix] - L; d  = t * t * Lscale;
                        if (i != 0 && d > bestd) continue;
                        t = Utab[ix] - U; d += t * t;
                        if (i != 0 && d > bestd) continue;
                        t = Vtab[ix] - V; d += t * t;
                        if (i != 0 && d > bestd) continue;
                        bestd = d;
                        pCmap->bestidx = ix;
                    }
                }
            }
        }
    }
    pCmap = virt_cmap;
    for (i = 0; i < num_virt_cmap_entries; i++) {
        *lookup++ = (pCmap++)->bestidx;
    }

#ifdef TIMES
    end = clock();
#endif /* TIMES */

#ifdef STATS
    max_dL = 0.0;
    max_dE = 0.0;
    ave_dL = 0.0;
    ave_dE = 0.0;

    pCmap = virt_cmap;
    for (i = 0; i < num_virt_cmap_entries; i++, pCmap++) {
        double t, dL, dU, dV, dE;
        if (pCmap->nextidx < 0) {
            int ix = pCmap->bestidx;
            dL = pCmap->L - Ltab[ix]; dL *= dL;
            dU = pCmap->U - Utab[ix]; dU *= dU;
            dV = pCmap->V - Vtab[ix]; dV *= dV;
            dE = dL * Lscale + dU + dV;
            dE = WEIGHT_DIST(dE, pCmap->L);
        } else {
            dL = pCmap->dL;
            dE = pCmap->dE;
        }

        if (dL > max_dL) max_dL = dL;
        t = UNWEIGHT_DIST(dE,dL) - dL*(Lscale-1);
        if (t > max_dE) max_dE = t;

        ave_dL += (dL > 0) ? sqrt(dL) : 0.0;
        ave_dE += (t > 0) ? sqrt(t) : 0.0;
    }

    jio_fprintf(stderr, "colors=%d, tablesize=%d, cubesize=%d, ",
            cmapsize, tablesize, lookupsize);
    jio_fprintf(stderr, "Lscale=%5.3f, Weight=%5.3f mac=%s\n",
            (double)lscale, (double)weight, doMac ? "true" : "false");
    jio_fprintf(stderr, "Worst case error dL = %5.3f, dE = %5.3f\n",
            sqrt(max_dL), sqrt(max_dE));
    jio_fprintf(stderr, "Average error dL = %5.3f, dE = %5.3f\n",
            ave_dL / num_virt_cmap_entries,  ave_dE / num_virt_cmap_entries);
#endif /* STATS */
#ifdef TIMES
    jio_fprintf(stderr, "%f seconds to find colors\n",
            (double)(mid - start) / CLOCKS_PER_SEC);
    jio_fprintf(stderr, "%f seconds to finish nearest colors\n",
            (double)(tbl - mid) / CLOCKS_PER_SEC);
    jio_fprintf(stderr, "%f seconds to make lookup table\n",
            (double)(end - tbl) / CLOCKS_PER_SEC);
    jio_fprintf(stderr, "%f seconds total\n",
            (double)(end - start) / CLOCKS_PER_SEC);
#endif /* TIMES */

    free(virt_cmap);
    virt_cmap = 0;
}
