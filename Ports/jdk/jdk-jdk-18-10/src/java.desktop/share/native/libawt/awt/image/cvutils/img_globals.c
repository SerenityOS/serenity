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

/*
 * This file implements some of the standard utility procedures used
 * by the image conversion package.
 */

#include "jni.h"
#include "img_globals.h"

#include "java_awt_image_IndexColorModel.h"
#include "java_awt_Transparency.h"

/*
 * This function constructs an 8x8 ordered dither array which can be
 * used to dither data into an output range with discreet values that
 * differ by the value specified as quantum.  A monochrome screen would
 * use a dither array constructed with the quantum 256.
 * The array values produced are unsigned and intended to be used with
 * a lookup table which returns the next color darker than the error
 * adjusted color used as the index.
 */
JNIEXPORT void JNICALL
make_uns_ordered_dither_array(uns_ordered_dither_array oda,
                              int quantum)
{
    int i, j, k;

    oda[0][0] = 0;
    for (k = 1; k < 8; k *= 2) {
        for (i = 0; i < k; i++) {
            for (j = 0; j < k; j++) {
                oda[ i ][ j ] = oda[i][j] * 4;
                oda[i+k][j+k] = oda[i][j] + 1;
                oda[ i ][j+k] = oda[i][j] + 2;
                oda[i+k][ j ] = oda[i][j] + 3;
            }
        }
    }
    for (i = 0; i < 8; i++) {
        for (j = 0; j < 8; j++) {
            oda[i][j] = oda[i][j] * quantum / 64;
        }
    }
}

/*
 * This function constructs an 8x8 ordered dither array which can be
 * used to dither data into an output range with discreet values that
 * are distributed over the range from minerr to maxerr around a given
 * target color value.
 * The array values produced are signed and intended to be used with
 * a lookup table which returns the closest color to the error adjusted
 * color used as an index.
 */
void
make_sgn_ordered_dither_array(char* oda, int minerr, int maxerr)
{
    int i, j, k;

    oda[0] = 0;
    for (k = 1; k < 8; k *= 2) {
        for (i = 0; i < k; i++) {
            for (j = 0; j < k; j++) {
                oda[(i<<3) + j] = oda[(i<<3)+j] * 4;
                oda[((i+k)<<3) + j+k] = oda[(i<<3)+j] + 1;
                oda[(i<<3) + j+k] = oda[(i<<3)+j] + 2;
                oda[((i+k)<<3) + j] = oda[(i<<3)+j] + 3;
            }
        }
    }
    k = 0;
    for (i = 0; i < 8; i++) {
        for (j = 0; j < 8; j++) {
            oda[k] = oda[k] * (maxerr - minerr) / 64 + minerr;
            k++;
        }
    }
}

#ifdef TESTING
#include <stdio.h>

/* Function to test the ordered dither error matrix initialization function. */
main(int argc, char **argv)
{
    int i, j;
    int quantum;
    int max, val;
    uns_ordered_dither_array oda;

    if (argc > 1) {
        quantum = atoi(argv[1]);
    } else {
        quantum = 64;
    }
    make_uns_ordered_dither_array(oda, quantum);
    for (i = 0; i < 8; i++) {
        for (j = 0; j < 8; j++) {
            val = oda[i][j];
            printf("%4d", val);
            if (max < val) {
                max = val;
            }
        }
        printf("\n");
    }
    printf("\nmax = %d\n", max);
}
#endif /* TESTING */
