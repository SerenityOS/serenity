/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

#include "MTLUtils.h"

#include <jni.h>
#include <simd/simd.h>
#import <ThreadUtilities.h>
#import <PropertiesUtilities.h>
#include "common.h"
#include "Trace.h"

extern void J2dTraceImpl(int level, jboolean cr, const char *string, ...);
void J2dTraceTraceVector(simd_float4 pt) {
    J2dTraceImpl(J2D_TRACE_VERBOSE, JNI_FALSE, "[%lf %lf %lf %lf]", pt.x, pt.y, pt.z, pt.w);
}

void checkTransform(float * position, simd_float4x4 transform4x4) {
    J2dTraceImpl(J2D_TRACE_VERBOSE, JNI_FALSE, "check transform: ");

    simd_float4 fpt = simd_make_float4(position[0], position[1], position[2], 1.f);
    simd_float4 fpt_trans = simd_mul(transform4x4, fpt);
    J2dTraceTraceVector(fpt);
    J2dTraceImpl(J2D_TRACE_VERBOSE, JNI_FALSE, "  ===>>>  ");
    J2dTraceTraceVector(fpt_trans);
    J2dTraceImpl(J2D_TRACE_VERBOSE, JNI_TRUE, " ");
}

static void traceMatrix(simd_float4x4 * mtx) {
    for (int row = 0; row < 4; ++row) {
        J2dTraceImpl(J2D_TRACE_VERBOSE, JNI_FALSE, "  [%lf %lf %lf %lf]",
                    mtx->columns[0][row], mtx->columns[1][row], mtx->columns[2][row], mtx->columns[3][row]);
    }
}

void traceRaster(char * p, int width, int height, int stride) {
    for (int y = 0; y < height; ++y) {
        for (int x = 0; x < width; ++x) {
            unsigned char pix0 = p[y*stride + x*4];
            unsigned char pix1 = p[y*stride + x*4 + 1];
            unsigned char pix2 = p[y*stride + x*4 + 2];
            unsigned char pix3 = p[y*stride + x*4 + 3];
            J2dTraceImpl(J2D_TRACE_INFO, JNI_FALSE,"[%u,%u,%u,%u], ", pix0, pix1, pix2, pix3);
        }
        J2dTraceImpl(J2D_TRACE_INFO, JNI_TRUE, "");
    }
}

void tracePoints(jint nPoints, jint *xPoints, jint *yPoints) {
    for (int i = 0; i < nPoints; i++)
        J2dTraceImpl(J2D_TRACE_INFO, JNI_TRUE, "\t(%d, %d)", *(xPoints++), *(yPoints++));
}


jboolean isOptionEnabled(const char * option) {
    JNIEnv *env = [ThreadUtilities getJNIEnvUncached];

    NSString * optionProp = [PropertiesUtilities
            javaSystemPropertyForKey:[NSString stringWithUTF8String:option] withEnv:env];
    NSString * lowerCaseProp = [optionProp localizedLowercaseString];
    return [@"true" isEqual:lowerCaseProp];
}
