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

#ifndef MTLBlitLoops_h_Included
#define MTLBlitLoops_h_Included

#include "sun_java2d_metal_MTLBlitLoops.h"
#include "MTLSurfaceDataBase.h"
#include "MTLContext.h"

#define OFFSET_SRCTYPE sun_java2d_metal_MTLBlitLoops_OFFSET_SRCTYPE
#define OFFSET_HINT    sun_java2d_metal_MTLBlitLoops_OFFSET_HINT
#define OFFSET_TEXTURE sun_java2d_metal_MTLBlitLoops_OFFSET_TEXTURE
#define OFFSET_RTT     sun_java2d_metal_MTLBlitLoops_OFFSET_RTT
#define OFFSET_XFORM   sun_java2d_metal_MTLBlitLoops_OFFSET_XFORM
#define OFFSET_ISOBLIT sun_java2d_metal_MTLBlitLoops_OFFSET_ISOBLIT

void MTLBlitLoops_IsoBlit(JNIEnv *env,
                          MTLContext *mtlc, jlong pSrcOps, jlong pDstOps,
                          jboolean xform, jint hint,
                          jboolean texture,
                          jint sx1, jint sy1,
                          jint sx2, jint sy2,
                          jdouble dx1, jdouble dy1,
                          jdouble dx2, jdouble dy2);

void MTLBlitLoops_Blit(JNIEnv *env,
                       MTLContext *mtlc, jlong pSrcOps, jlong pDstOps,
                       jboolean xform, jint hint,
                       jint srctype, jboolean texture,
                       jint sx1, jint sy1,
                       jint sx2, jint sy2,
                       jdouble dx1, jdouble dy1,
                       jdouble dx2, jdouble dy2);

void MTLBlitLoops_SurfaceToSwBlit(JNIEnv *env, MTLContext *mtlc,
                                  jlong pSrcOps, jlong pDstOps, jint dsttype,
                                  jint srcx, jint srcy,
                                  jint dstx, jint dsty,
                                  jint width, jint height);

void MTLBlitLoops_CopyArea(JNIEnv *env,
                           MTLContext *mtlc, BMTLSDOps *dstOps,
                           jint x, jint y,
                           jint width, jint height,
                           jint dx, jint dy);

void MTLBlitTex2Tex(MTLContext *mtlc, id<MTLTexture> src, id<MTLTexture> dest);

void drawTex2Tex(MTLContext *mtlc,
                        id<MTLTexture> src, id<MTLTexture> dst,
                        jboolean isSrcOpaque, jboolean isDstOpaque, jint hint,
                        jint sx1, jint sy1, jint sx2, jint sy2,
                        jdouble dx1, jdouble dy1, jdouble dx2, jdouble dy2);

#endif /* MTLBlitLoops_h_Included */
