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

#import "sun_java2d_metal_MTLGraphicsConfig.h"

#import "MTLGraphicsConfig.h"
#import "ThreadUtilities.h"
#import "awt.h"

/**
 * Disposes all memory and resources associated with the given
 * MTLGraphicsConfigInfo (including its native MTLContext data).
 */
void
MTLGC_DestroyMTLGraphicsConfig(jlong pConfigInfo)
{
    J2dTraceLn(J2D_TRACE_INFO, "MTLGC_DestroyMTLGraphicsConfig");

    MTLGraphicsConfigInfo *mtlinfo =
        (MTLGraphicsConfigInfo *)jlong_to_ptr(pConfigInfo);
    if (mtlinfo == NULL) {
        J2dRlsTraceLn(J2D_TRACE_ERROR,
                      "MTLGC_DestroyMTLGraphicsConfig: info is null");
        return;
    }

    MTLContext *mtlc = (MTLContext*)mtlinfo->context;
    if (mtlc != NULL) {
        [mtlinfo->context release];
        mtlinfo->context = nil;
    }
    free(mtlinfo);
}

JNIEXPORT jboolean JNICALL
Java_sun_java2d_metal_MTLGraphicsConfig_isMetalFrameworkAvailable
    (JNIEnv *env, jclass mtlgc)
{
    jboolean metalSupported = JNI_FALSE;

    // It is guranteed that metal supported GPU is available macOS 10.14 onwards
    if (@available(macOS 10.14, *)) {
        metalSupported = JNI_TRUE;
    }

    J2dRlsTraceLn1(J2D_TRACE_INFO, "MTLGraphicsConfig_isMetalFrameworkAvailable : %d", metalSupported);

    return metalSupported;
}

JNIEXPORT jboolean JNICALL
Java_sun_java2d_metal_MTLGraphicsConfig_tryLoadMetalLibrary
    (JNIEnv *env, jclass mtlgc, jint displayID, jstring shadersLibName)
{
    __block jboolean ret = JNI_FALSE;

JNI_COCOA_ENTER(env);

    __block NSString* path = NormalizedPathNSStringFromJavaString(env, shadersLibName);

    [ThreadUtilities performOnMainThreadWaiting:YES block:^() {

        id<MTLDevice> device = CGDirectDisplayCopyCurrentMetalDevice(displayID);
        if (device != nil) {
            NSError* error = nil;
            id<MTLLibrary> lib = [device newLibraryWithFile:path error:&error];
            if (lib != nil) {
                ret = JNI_TRUE;
            } else {
                J2dRlsTraceLn(J2D_TRACE_ERROR, "MTLGraphicsConfig_tryLoadMetalLibrary - Failed to load Metal shader library.");
            }
        } else {
            J2dRlsTraceLn(J2D_TRACE_ERROR, "MTLGraphicsConfig_tryLoadMetalLibrary - Failed to create MTLDevice.");
        }
    }];

JNI_COCOA_EXIT(env);
    return ret;
}

/**
 * Determines whether the Metal pipeline can be used for a given screen number and
 * shader library path. A MTLContext is created and the native MTLGraphicsConfigInfo
 * structure is initialized for this context. A pointer to this structure is
 * returned as a jlong.
 * If initialization fails at any point, zero is returned, indicating that Metal pipeline
 * cannot be used for this GraphicsConfig (we should fallback on an existing 2D pipeline).
 */
JNIEXPORT jlong JNICALL
Java_sun_java2d_metal_MTLGraphicsConfig_getMTLConfigInfo
    (JNIEnv *env, jclass mtlgc, jint displayID, jstring mtlShadersLib)
{
    __block MTLContext* mtlc = nil;
    __block MTLGraphicsConfigInfo* mtlinfo = nil;

JNI_COCOA_ENTER(env);

    __block NSString* path = NormalizedPathNSStringFromJavaString(env, mtlShadersLib);

    [ThreadUtilities performOnMainThreadWaiting:YES block:^() {

        mtlc = [[MTLContext alloc] initWithDevice:CGDirectDisplayCopyCurrentMetalDevice(displayID)
                                       shadersLib:path];
        if (mtlc != 0L) {
            // create the MTLGraphicsConfigInfo record for this context
            mtlinfo = (MTLGraphicsConfigInfo *)malloc(sizeof(MTLGraphicsConfigInfo));
            if (mtlinfo != NULL) {
                memset(mtlinfo, 0, sizeof(MTLGraphicsConfigInfo));
                mtlinfo->context = mtlc;
            } else {
                J2dRlsTraceLn(J2D_TRACE_ERROR, "MTLGraphicsConfig_getMTLConfigInfo: could not allocate memory for mtlinfo");
                [mtlc release];
                mtlc = nil;
            }
        } else {
            J2dRlsTraceLn(J2D_TRACE_ERROR, "MTLGraphicsConfig_getMTLConfigInfo: could not initialze MTLContext.");
        }
    }];

JNI_COCOA_EXIT(env);

    return ptr_to_jlong(mtlinfo);
}

JNIEXPORT jint JNICALL
Java_sun_java2d_metal_MTLGraphicsConfig_nativeGetMaxTextureSize
    (JNIEnv *env, jclass mtlgc)
{
    J2dTraceLn(J2D_TRACE_INFO, "MTLGraphicsConfig_nativeGetMaxTextureSize");

    return (jint)MTL_GPU_FAMILY_MAC_TXT_SIZE;
}
