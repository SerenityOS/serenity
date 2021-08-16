/*
 * Copyright (c) 2000, 2018, Oracle and/or its affiliates. All rights reserved.
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
#include "AlphaMath.h"

JNIEXPORT unsigned char mul8table[256][256];
JNIEXPORT unsigned char div8table[256][256];

void initAlphaTables()
{
    unsigned int i;
    unsigned int j;

    for (i = 1; i < 256; i++) {                 /* SCALE == (1 << 24) */
        unsigned int inc = (i << 16) + (i<<8) + i;       /* approx. SCALE * (i/255.0) */
        unsigned int val = inc + (1 << 23);              /* inc + SCALE*0.5 */
        for (j = 1; j < 256; j++) {
            mul8table[i][j] = (val >> 24);      /* val / SCALE */
            val += inc;
        }
    }

    for (i = 1; i < 256; i++) {
        unsigned int inc;
        unsigned int val;
        inc = 0xff;
        inc = ((inc << 24) + i/2) / i;
        val = (1 << 23);
        for (j = 0; j < i; j++) {
            div8table[i][j] = (val >> 24);
            val += inc;
        }
        for (j = i; j < 256; j++) {
            div8table[i][j] = 255;
        }
    }
}
