/*
 * Copyright (c) 1997, 2018, Oracle and/or its affiliates. All rights reserved.
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


#ifndef __ORIG_MLIB_IMAGE_PROTO_H
#define __ORIG_MLIB_IMAGE_PROTO_H

#include <mlib_types.h>
#include <mlib_status.h>
#include <mlib_image_types.h>
#include "jni.h"
#if defined ( __MEDIALIB_OLD_NAMES_ADDED )
#include <../include/mlib_image_proto.h>
#endif /* defined ( __MEDIALIB_OLD_NAMES_ADDED ) */

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

#if defined ( __USE_J2D_NAMES )
#include "j2d_names.h"
#endif // __USE_J2D_NAMES

#if defined ( _MSC_VER )
#if ! defined ( __MEDIALIB_OLD_NAMES )
#define __MEDIALIB_OLD_NAMES
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
#endif /* defined ( _MSC_VER ) */

/* Arithmetic Operations ( arith ) */


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageAbs mlib_ImageAbs
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageAbs(mlib_image *dst,
                             const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageAbs_Fp mlib_ImageAbs_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageAbs_Fp(mlib_image *dst,
                                const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageAbs_Fp_Inp mlib_ImageAbs_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageAbs_Fp_Inp(mlib_image *srcdst);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageAbs_Inp mlib_ImageAbs_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageAbs_Inp(mlib_image *srcdst);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageAdd mlib_ImageAdd
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageAdd(mlib_image *dst,
                             const mlib_image *src1,
                             const mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageAdd_Fp mlib_ImageAdd_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageAdd_Fp(mlib_image *dst,
                                const mlib_image *src1,
                                const mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageAdd_Fp_Inp mlib_ImageAdd_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageAdd_Fp_Inp(mlib_image *src1dst,
                                    const mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageAdd_Inp mlib_ImageAdd_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageAdd_Inp(mlib_image *src1dst,
                                 const mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageAve mlib_ImageAve
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageAve(mlib_image *dst,
                             const mlib_image *src1,
                             const mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageAve_Fp mlib_ImageAve_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageAve_Fp(mlib_image *dst,
                                const mlib_image *src1,
                                const mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageAve_Fp_Inp mlib_ImageAve_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageAve_Fp_Inp(mlib_image *src1dst,
                                    const mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageAve_Inp mlib_ImageAve_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageAve_Inp(mlib_image *src1dst,
                                 const mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageBlend mlib_ImageBlend
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageBlend(mlib_image *dst,
                               const mlib_image *src1,
                               const mlib_image *src2,
                               const mlib_image *alpha);

/* src1dst = src1dst * alpha + src2 * (1 - alpha) */

#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageBlend1_Fp_Inp mlib_ImageBlend1_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageBlend1_Fp_Inp(mlib_image *src1dst,
                                       const mlib_image *src2,
                                       const mlib_image *alpha);

/* src1dst = src1dst * alpha + src2 * (1 - alpha) */

#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageBlend1_Inp mlib_ImageBlend1_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageBlend1_Inp(mlib_image *src1dst,
                                    const mlib_image *src2,
                                    const mlib_image *alpha);

/* src2dst = src1 * alpha + src2dst * (1 - alpha) */

#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageBlend2_Fp_Inp mlib_ImageBlend2_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageBlend2_Fp_Inp(mlib_image *src2dst,
                                       const mlib_image *src1,
                                       const mlib_image *alpha);

/* src2dst = src1 * alpha + src2dst * (1 - alpha) */

#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageBlend2_Inp mlib_ImageBlend2_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageBlend2_Inp(mlib_image *src2dst,
                                    const mlib_image *src1,
                                    const mlib_image *alpha);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageBlend_Fp mlib_ImageBlend_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageBlend_Fp(mlib_image *dst,
                                  const mlib_image *src1,
                                  const mlib_image *src2,
                                  const mlib_image *alpha);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageBlendMulti mlib_ImageBlendMulti
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageBlendMulti(mlib_image *dst,
                                    const mlib_image **srcs,
                                    const mlib_image **alphas,
                                    const mlib_s32 *c,
                                    mlib_s32 n);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageBlendMulti_Fp mlib_ImageBlendMulti_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageBlendMulti_Fp(mlib_image *dst,
                                       const mlib_image **srcs,
                                       const mlib_image **alphas,
                                       const mlib_d64 *c,
                                       mlib_s32 n);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageBlendRGBA2ARGB mlib_ImageBlendRGBA2ARGB
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageBlendRGBA2ARGB(mlib_image *dst,
                                        const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageBlendRGBA2BGRA mlib_ImageBlendRGBA2BGRA
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageBlendRGBA2BGRA(mlib_image *dst,
                                        const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorBlend mlib_ImageColorBlend
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorBlend(mlib_image *dst,
                                    const mlib_image *src,
                                    const mlib_s32 *color,
                                    mlib_s32 cmask);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorBlend_Fp mlib_ImageColorBlend_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorBlend_Fp(mlib_image *dst,
                                       const mlib_image *src,
                                       const mlib_d64 *color,
                                       mlib_s32 cmask);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorBlend_Fp_Inp mlib_ImageColorBlend_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorBlend_Fp_Inp(mlib_image *srcdst,
                                           const mlib_d64 *color,
                                           mlib_s32 cmask);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorBlend_Inp mlib_ImageColorBlend_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorBlend_Inp(mlib_image *srcdst,
                                        const mlib_s32 *color,
                                        mlib_s32 cmask);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstAdd mlib_ImageConstAdd
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstAdd(mlib_image *dst,
                                  const mlib_image *src,
                                  const mlib_s32 *c);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstAdd_Fp mlib_ImageConstAdd_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstAdd_Fp(mlib_image *dst,
                                     const mlib_image *src,
                                     const mlib_d64 *c);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstAdd_Fp_Inp mlib_ImageConstAdd_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstAdd_Fp_Inp(mlib_image *srcdst,
                                         const mlib_d64 *c);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstAdd_Inp mlib_ImageConstAdd_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstAdd_Inp(mlib_image *srcdst,
                                      const mlib_s32 *c);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstDiv mlib_ImageConstDiv
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstDiv(mlib_image *dst,
                                  const mlib_image *src,
                                  const mlib_d64 *consts);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstDivShift mlib_ImageConstDivShift
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstDivShift(mlib_image *dst,
                                       const mlib_image *src,
                                       const mlib_s32 *consts,
                                       mlib_s32 shift);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstDivShift_Inp mlib_ImageConstDivShift_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstDivShift_Inp(mlib_image *srcdst,
                                           const mlib_s32 *consts,
                                           mlib_s32 shift);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstDiv_Fp mlib_ImageConstDiv_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstDiv_Fp(mlib_image *dst,
                                     const mlib_image *src,
                                     const mlib_d64 *consts);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstDiv_Fp_Inp mlib_ImageConstDiv_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstDiv_Fp_Inp(mlib_image *srcdst,
                                         const mlib_d64 *consts);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstDiv_Inp mlib_ImageConstDiv_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstDiv_Inp(mlib_image *srcdst,
                                      const mlib_d64 *consts);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstMul mlib_ImageConstMul
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstMul(mlib_image *dst,
                                  const mlib_image *src,
                                  const mlib_d64 *consts);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstMulShift mlib_ImageConstMulShift
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstMulShift(mlib_image *dst,
                                       const mlib_image *src,
                                       const mlib_s32 *consts,
                                       mlib_s32 shift);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstMulShift_Inp mlib_ImageConstMulShift_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstMulShift_Inp(mlib_image *srcdst,
                                           const mlib_s32 *consts,
                                           mlib_s32 shift);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstMul_Fp mlib_ImageConstMul_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstMul_Fp(mlib_image *dst,
                                     const mlib_image *src,
                                     const mlib_d64 *consts);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstMul_Fp_Inp mlib_ImageConstMul_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstMul_Fp_Inp(mlib_image *srcdst,
                                         const mlib_d64 *consts);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstMul_Inp mlib_ImageConstMul_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstMul_Inp(mlib_image *srcdst,
                                      const mlib_d64 *consts);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstSub mlib_ImageConstSub
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstSub(mlib_image *dst,
                                  const mlib_image *src,
                                  const mlib_s32 *c);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstSub_Fp mlib_ImageConstSub_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstSub_Fp(mlib_image *dst,
                                     const mlib_image *src,
                                     const mlib_d64 *c);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstSub_Fp_Inp mlib_ImageConstSub_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstSub_Fp_Inp(mlib_image *srcdst,
                                         const mlib_d64 *c);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstSub_Inp mlib_ImageConstSub_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstSub_Inp(mlib_image *srcdst,
                                      const mlib_s32 *c);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageDiv1_Fp_Inp mlib_ImageDiv1_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageDiv1_Fp_Inp(mlib_image *src1dst,
                                     const mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageDiv2_Fp_Inp mlib_ImageDiv2_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageDiv2_Fp_Inp(mlib_image *src2dst,
                                     const mlib_image *src1);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageDivAlpha mlib_ImageDivAlpha
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageDivAlpha(mlib_image *dst,
                                  const mlib_image *src,
                                  mlib_s32 cmask);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageDivAlpha_Fp mlib_ImageDivAlpha_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageDivAlpha_Fp(mlib_image *dst,
                                     const mlib_image *src,
                                     mlib_s32 cmask);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageDivAlpha_Fp_Inp mlib_ImageDivAlpha_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageDivAlpha_Fp_Inp(mlib_image *img,
                                         mlib_s32 cmask);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageDivAlpha_Inp mlib_ImageDivAlpha_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageDivAlpha_Inp(mlib_image *img,
                                      mlib_s32 cmask);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageDivConstShift mlib_ImageDivConstShift
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageDivConstShift(mlib_image *dst,
                                       const mlib_image *src,
                                       const mlib_s32 *consts,
                                       mlib_s32 shift);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageDivConstShift_Inp mlib_ImageDivConstShift_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageDivConstShift_Inp(mlib_image *srcdst,
                                           const mlib_s32 *consts,
                                           mlib_s32 shift);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageDivShift mlib_ImageDivShift
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageDivShift(mlib_image *dst,
                                  const mlib_image *src1,
                                  const mlib_image *src2,
                                  mlib_s32 shift);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageDivShift1_Inp mlib_ImageDivShift1_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageDivShift1_Inp(mlib_image *src1dst,
                                       const mlib_image *src2,
                                       mlib_s32 shift);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageDivShift2_Inp mlib_ImageDivShift2_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageDivShift2_Inp(mlib_image *src2dst,
                                       const mlib_image *src1,
                                       mlib_s32 shift);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageDiv_Fp mlib_ImageDiv_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageDiv_Fp(mlib_image *dst,
                                const mlib_image *src1,
                                const mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageExp mlib_ImageExp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageExp(mlib_image *dst,
                             const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageExp_Fp mlib_ImageExp_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageExp_Fp(mlib_image *dst,
                                const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageExp_Fp_Inp mlib_ImageExp_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageExp_Fp_Inp(mlib_image *srcdst);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageExp_Inp mlib_ImageExp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageExp_Inp(mlib_image *srcdst);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageInvert mlib_ImageInvert
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageInvert(mlib_image *dst,
                                const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageInvert_Fp mlib_ImageInvert_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageInvert_Fp(mlib_image *dst,
                                   const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageInvert_Fp_Inp mlib_ImageInvert_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageInvert_Fp_Inp(mlib_image *srcdst);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageInvert_Inp mlib_ImageInvert_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageInvert_Inp(mlib_image *srcdst);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageLog mlib_ImageLog
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageLog(mlib_image *dst,
                             const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageLog_Fp mlib_ImageLog_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageLog_Fp(mlib_image *dst,
                                const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageLog_Fp_Inp mlib_ImageLog_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageLog_Fp_Inp(mlib_image *srcdst);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageLog_Inp mlib_ImageLog_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageLog_Inp(mlib_image *srcdst);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMax mlib_ImageMax
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMax(mlib_image *dst,
                             const mlib_image *src1,
                             const mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMax_Fp mlib_ImageMax_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMax_Fp(mlib_image *dst,
                                const mlib_image *src1,
                                const mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMax_Fp_Inp mlib_ImageMax_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMax_Fp_Inp(mlib_image *src1dst,
                                    const mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMax_Inp mlib_ImageMax_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMax_Inp(mlib_image *src1dst,
                                 const mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMin mlib_ImageMin
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMin(mlib_image *dst,
                             const mlib_image *src1,
                             const mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMin_Fp mlib_ImageMin_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMin_Fp(mlib_image *dst,
                                const mlib_image *src1,
                                const mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMin_Fp_Inp mlib_ImageMin_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMin_Fp_Inp(mlib_image *src1dst,
                                    const mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMin_Inp mlib_ImageMin_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMin_Inp(mlib_image *src1dst,
                                 const mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMulAlpha mlib_ImageMulAlpha
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMulAlpha(mlib_image *dst,
                                  const mlib_image *src,
                                  mlib_s32 cmask);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMulAlpha_Fp mlib_ImageMulAlpha_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMulAlpha_Fp(mlib_image *dst,
                                     const mlib_image *src,
                                     mlib_s32 cmask);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMulAlpha_Fp_Inp mlib_ImageMulAlpha_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMulAlpha_Fp_Inp(mlib_image *img,
                                         mlib_s32 cmask);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMulAlpha_Inp mlib_ImageMulAlpha_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMulAlpha_Inp(mlib_image *img,
                                      mlib_s32 cmask);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMulShift mlib_ImageMulShift
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMulShift(mlib_image *dst,
                                  const mlib_image *src1,
                                  const mlib_image *src2,
                                  mlib_s32 shift);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMulShift_Inp mlib_ImageMulShift_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMulShift_Inp(mlib_image *src1dst,
                                      const mlib_image *src2,
                                      mlib_s32 shift);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMul_Fp mlib_ImageMul_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMul_Fp(mlib_image *dst,
                                const mlib_image *src1,
                                const mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMul_Fp_Inp mlib_ImageMul_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMul_Fp_Inp(mlib_image *src1dst,
                                    const mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageScalarBlend mlib_ImageScalarBlend
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageScalarBlend(mlib_image *dst,
                                     const mlib_image *src1,
                                     const mlib_image *src2,
                                     const mlib_s32 *alpha);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageScalarBlend_Fp mlib_ImageScalarBlend_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageScalarBlend_Fp(mlib_image *dst,
                                        const mlib_image *src1,
                                        const mlib_image *src2,
                                        const mlib_d64 *alpha);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageScalarBlend_Fp_Inp mlib_ImageScalarBlend_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageScalarBlend_Fp_Inp(mlib_image *src1dst,
                                            const mlib_image *src2,
                                            const mlib_d64 *alpha);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageScalarBlend_Inp mlib_ImageScalarBlend_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageScalarBlend_Inp(mlib_image *src1dst,
                                         const mlib_image *src2,
                                         const mlib_s32 *alpha);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageScale mlib_ImageScale
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageScale(mlib_image *dst,
                               const mlib_image *src,
                               const mlib_s32 *alpha,
                               const mlib_s32 *beta,
                               mlib_s32 shift);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageScale2 mlib_ImageScale2
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageScale2(mlib_image *dst,
                                const mlib_image *src,
                                const mlib_d64 *alpha,
                                const mlib_d64 *beta);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageScale2_Inp mlib_ImageScale2_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageScale2_Inp(mlib_image *srcdst,
                                    const mlib_d64 *alpha,
                                    const mlib_d64 *beta);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageScale_Fp mlib_ImageScale_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageScale_Fp(mlib_image *dst,
                                  const mlib_image *src,
                                  const mlib_d64 *alpha,
                                  const mlib_d64 *beta);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageScale_Fp_Inp mlib_ImageScale_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageScale_Fp_Inp(mlib_image *srcdst,
                                      const mlib_d64 *alpha,
                                      const mlib_d64 *beta);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageScale_Inp mlib_ImageScale_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageScale_Inp(mlib_image *srcdst,
                                   const mlib_s32 *alpha,
                                   const mlib_s32 *beta,
                                   mlib_s32 shift);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageSqrShift mlib_ImageSqrShift
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageSqrShift(mlib_image *dst,
                                  const mlib_image *src,
                                  mlib_s32 shift);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageSqrShift_Inp mlib_ImageSqrShift_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageSqrShift_Inp(mlib_image *srcdst,
                                      mlib_s32 shift);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageSqr_Fp mlib_ImageSqr_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageSqr_Fp(mlib_image *dst,
                                const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageSqr_Fp_Inp mlib_ImageSqr_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageSqr_Fp_Inp(mlib_image *srcdst);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageSub mlib_ImageSub
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageSub(mlib_image *dst,
                             const mlib_image *src1,
                             const mlib_image *src2);

/* src1dst = src1dst - src2 */

#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageSub1_Fp_Inp mlib_ImageSub1_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageSub1_Fp_Inp(mlib_image *src1dst,
                                     const mlib_image *src2);

/* src1dst = src1dst - src2 */

#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageSub1_Inp mlib_ImageSub1_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageSub1_Inp(mlib_image *src1dst,
                                  const mlib_image *src2);

/* src2dst = src1 - src2dst */

#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageSub2_Fp_Inp mlib_ImageSub2_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageSub2_Fp_Inp(mlib_image *src2dst,
                                     const mlib_image *src1);

/* src2dst = src1 - src2dst */

#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageSub2_Inp mlib_ImageSub2_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageSub2_Inp(mlib_image *src2dst,
                                  const mlib_image *src1);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageSub_Fp mlib_ImageSub_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageSub_Fp(mlib_image *dst,
                                const mlib_image *src1,
                                const mlib_image *src2);

/* Color Space Conversion ( color ) */


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorConvert1 mlib_ImageColorConvert1
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorConvert1(mlib_image *dst,
                                       const mlib_image *src,
                                       const mlib_d64 *cmat);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorConvert1_Fp mlib_ImageColorConvert1_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorConvert1_Fp(mlib_image *dst,
                                          const mlib_image *src,
                                          const mlib_d64 *cmat);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorConvert2 mlib_ImageColorConvert2
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorConvert2(mlib_image *dst,
                                       const mlib_image *src,
                                       const mlib_d64 *cmat,
                                       const mlib_d64 *offset);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorConvert2_Fp mlib_ImageColorConvert2_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorConvert2_Fp(mlib_image *dst,
                                          const mlib_image *src,
                                          const mlib_d64 *cmat,
                                          const mlib_d64 *offset);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorHSL2RGB mlib_ImageColorHSL2RGB
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorHSL2RGB(mlib_image *dst,
                                      const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorHSL2RGB_Fp mlib_ImageColorHSL2RGB_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorHSL2RGB_Fp(mlib_image *dst,
                                         const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorHSV2RGB mlib_ImageColorHSV2RGB
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorHSV2RGB(mlib_image *dst,
                                      const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorHSV2RGB_Fp mlib_ImageColorHSV2RGB_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorHSV2RGB_Fp(mlib_image *dst,
                                         const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorRGB2CIEMono mlib_ImageColorRGB2CIEMono
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorRGB2CIEMono(mlib_image *dst,
                                          const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorRGB2CIEMono_Fp mlib_ImageColorRGB2CIEMono_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorRGB2CIEMono_Fp(mlib_image *dst,
                                             const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorRGB2HSL mlib_ImageColorRGB2HSL
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorRGB2HSL(mlib_image *dst,
                                      const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorRGB2HSL_Fp mlib_ImageColorRGB2HSL_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorRGB2HSL_Fp(mlib_image *dst,
                                         const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorRGB2HSV mlib_ImageColorRGB2HSV
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorRGB2HSV(mlib_image *dst,
                                      const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorRGB2HSV_Fp mlib_ImageColorRGB2HSV_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorRGB2HSV_Fp(mlib_image *dst,
                                         const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorRGB2Mono mlib_ImageColorRGB2Mono
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorRGB2Mono(mlib_image *dst,
                                       const mlib_image *src,
                                       const mlib_d64 *weight);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorRGB2Mono_Fp mlib_ImageColorRGB2Mono_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorRGB2Mono_Fp(mlib_image *dst,
                                          const mlib_image *src,
                                          const mlib_d64 *weight);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorRGB2XYZ mlib_ImageColorRGB2XYZ
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorRGB2XYZ(mlib_image *dst,
                                      const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorRGB2XYZ_Fp mlib_ImageColorRGB2XYZ_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorRGB2XYZ_Fp(mlib_image *dst,
                                         const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorRGB2YCC mlib_ImageColorRGB2YCC
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorRGB2YCC(mlib_image *dst,
                                      const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorRGB2YCC_Fp mlib_ImageColorRGB2YCC_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorRGB2YCC_Fp(mlib_image *dst,
                                         const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorXYZ2RGB mlib_ImageColorXYZ2RGB
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorXYZ2RGB(mlib_image *dst,
                                      const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorXYZ2RGB_Fp mlib_ImageColorXYZ2RGB_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorXYZ2RGB_Fp(mlib_image *dst,
                                         const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorYCC2RGB mlib_ImageColorYCC2RGB
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorYCC2RGB(mlib_image *dst,
                                      const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorYCC2RGB_Fp mlib_ImageColorYCC2RGB_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorYCC2RGB_Fp(mlib_image *dst,
                                         const mlib_image *src);

/* Image Creation, Deletion and Query ( common ) */


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageCreate mlib_ImageCreate
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
JNIEXPORT mlib_image *
__mlib_ImageCreate(mlib_type type,
                   mlib_s32 channels,
                   mlib_s32 width,
                   mlib_s32 height);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageCreateStruct mlib_ImageCreateStruct
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
JNIEXPORT mlib_image *
__mlib_ImageCreateStruct(mlib_type type,
                         mlib_s32 channels,
                         mlib_s32 width,
                         mlib_s32 height,
                         mlib_s32 stride,
                         const void *data);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageCreateSubimage mlib_ImageCreateSubimage
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_image * __mlib_ImageCreateSubimage(mlib_image *img,
                                        mlib_s32 x,
                                        mlib_s32 y,
                                        mlib_s32 w,
                                        mlib_s32 h);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageDelete mlib_ImageDelete
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
JNIEXPORT void
__mlib_ImageDelete(mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageSetPaddings mlib_ImageSetPaddings
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageSetPaddings(mlib_image *img,
                                     mlib_u8 left,
                                     mlib_u8 top,
                                     mlib_u8 right,
                                     mlib_u8 bottom);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageSetFormat mlib_ImageSetFormat
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageSetFormat(mlib_image *img,
                                   mlib_format format);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageGetType mlib_ImageGetType
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
static mlib_type  __mlib_ImageGetType(const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageGetChannels mlib_ImageGetChannels
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
static mlib_s32  __mlib_ImageGetChannels(const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageGetWidth mlib_ImageGetWidth
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
static mlib_s32  __mlib_ImageGetWidth(const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageGetHeight mlib_ImageGetHeight
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
static mlib_s32  __mlib_ImageGetHeight(const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageGetStride mlib_ImageGetStride
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
static mlib_s32  __mlib_ImageGetStride(const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageGetData mlib_ImageGetData
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
static void * __mlib_ImageGetData(const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageGetFlags mlib_ImageGetFlags
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
static mlib_s32  __mlib_ImageGetFlags(const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageGetPaddings mlib_ImageGetPaddings
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
static mlib_u8 * __mlib_ImageGetPaddings(const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageGetBitOffset mlib_ImageGetBitOffset
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
static mlib_s32  __mlib_ImageGetBitOffset(const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageGetFormat mlib_ImageGetFormat
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
static mlib_format  __mlib_ImageGetFormat(const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageIsNotAligned2 mlib_ImageIsNotAligned2
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
static mlib_s32  __mlib_ImageIsNotAligned2(const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageIsNotAligned4 mlib_ImageIsNotAligned4
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
static mlib_s32  __mlib_ImageIsNotAligned4(const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageIsNotAligned64 mlib_ImageIsNotAligned64
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
static mlib_s32  __mlib_ImageIsNotAligned64(const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageIsNotAligned8 mlib_ImageIsNotAligned8
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
static mlib_s32  __mlib_ImageIsNotAligned8(const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageIsNotHeight2X mlib_ImageIsNotHeight2X
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
static mlib_s32  __mlib_ImageIsNotHeight2X(const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageIsNotHeight4X mlib_ImageIsNotHeight4X
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
static mlib_s32  __mlib_ImageIsNotHeight4X(const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageIsNotHeight8X mlib_ImageIsNotHeight8X
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
static mlib_s32  __mlib_ImageIsNotHeight8X(const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageIsNotOneDvector mlib_ImageIsNotOneDvector
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
static mlib_s32  __mlib_ImageIsNotOneDvector(const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageIsNotStride8X mlib_ImageIsNotStride8X
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
static mlib_s32  __mlib_ImageIsNotStride8X(const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageIsNotWidth2X mlib_ImageIsNotWidth2X
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
static mlib_s32  __mlib_ImageIsNotWidth2X(const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageIsNotWidth4X mlib_ImageIsNotWidth4X
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
static mlib_s32  __mlib_ImageIsNotWidth4X(const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageIsNotWidth8X mlib_ImageIsNotWidth8X
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
static mlib_s32  __mlib_ImageIsNotWidth8X(const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageIsUserAllocated mlib_ImageIsUserAllocated
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
static mlib_s32  __mlib_ImageIsUserAllocated(const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageTestFlags mlib_ImageTestFlags
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
static mlib_s32  __mlib_ImageTestFlags(const mlib_image *img,
                                       mlib_s32 flags);

/* Image Copying and Clearing ( copy ) */


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageClear mlib_ImageClear
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageClear(mlib_image *img,
                               const mlib_s32 *color);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageClearEdge mlib_ImageClearEdge
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageClearEdge(mlib_image *img,
                                   mlib_s32 dx,
                                   mlib_s32 dy,
                                   const mlib_s32 *color);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageClearEdge_Fp mlib_ImageClearEdge_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageClearEdge_Fp(mlib_image *img,
                                      mlib_s32 dx,
                                      mlib_s32 dy,
                                      const mlib_d64 *color);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageClear_Fp mlib_ImageClear_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageClear_Fp(mlib_image *img,
                                  const mlib_d64 *color);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageCopy mlib_ImageCopy
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageCopy(mlib_image *dst,
                              const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageCopyArea mlib_ImageCopyArea
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageCopyArea(mlib_image *img,
                                  mlib_s32 x,
                                  mlib_s32 y,
                                  mlib_s32 w,
                                  mlib_s32 h,
                                  mlib_s32 dx,
                                  mlib_s32 dy);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageCopyMask mlib_ImageCopyMask
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageCopyMask(mlib_image *dst,
                                  const mlib_image *src,
                                  const mlib_image *mask,
                                  const mlib_s32 *thresh);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageCopyMask_Fp mlib_ImageCopyMask_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageCopyMask_Fp(mlib_image *dst,
                                     const mlib_image *src,
                                     const mlib_image *mask,
                                     const mlib_d64 *thresh);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageCopySubimage mlib_ImageCopySubimage
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageCopySubimage(mlib_image *dst,
                                      const mlib_image *src,
                                      mlib_s32 xd,
                                      mlib_s32 yd,
                                      mlib_s32 xs,
                                      mlib_s32 ys,
                                      mlib_s32 w,
                                      mlib_s32 h);

/* Data Fomat Conversion ( format ) */


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageChannelCopy mlib_ImageChannelCopy
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageChannelCopy(mlib_image *dst,
                                     const mlib_image *src,
                                     mlib_s32 cmask);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageChannelExtract mlib_ImageChannelExtract
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageChannelExtract(mlib_image *dst,
                                        const mlib_image *src,
                                        mlib_s32 cmask);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageChannelInsert mlib_ImageChannelInsert
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageChannelInsert(mlib_image *dst,
                                       const mlib_image *src,
                                       mlib_s32 cmask);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageChannelMerge mlib_ImageChannelMerge
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageChannelMerge(mlib_image *dst,
                                      const mlib_image ** srcs);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageChannelSplit mlib_ImageChannelSplit
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageChannelSplit(mlib_image ** dsts,
                                      const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageDataTypeConvert mlib_ImageDataTypeConvert
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageDataTypeConvert(mlib_image *dst,
                                         const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageReformat mlib_ImageReformat
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageReformat(void **dstData,
                                  const void **srcData,
                                  mlib_s32 numChannels,
                                  mlib_s32 xSize,
                                  mlib_s32 ySize,
                                  mlib_type dstDataType,
                                  const mlib_s32 *dstBandoffsets,
                                  mlib_s32 dstScanlinestride,
                                  mlib_s32 dstPixelstride,
                                  mlib_type srcDataType,
                                  const mlib_s32 *srcBandoffsets,
                                  mlib_s32 srcScanlinestride,
                                  mlib_s32 srcPixelstride);

/* Fourier Transformation ( fourier ) */


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageFourierTransform mlib_ImageFourierTransform
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageFourierTransform(mlib_image *dst,
                                          const mlib_image *src,
                                          mlib_fourier_mode mode);

/* Geometric Operations ( geom ) */


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageAffine mlib_ImageAffine
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
JNIEXPORT mlib_status
__mlib_ImageAffine(mlib_image *dst,
                   const mlib_image *src,
                   const mlib_d64 *mtx,
                   mlib_filter filter,
                   mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageAffineTable mlib_ImageAffineTable
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageAffineTable(mlib_image *dst,
                                     const mlib_image *src,
                                     const mlib_d64 *mtx,
                                     const void *interp_table,
                                     mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageAffineTable_Fp mlib_ImageAffineTable_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageAffineTable_Fp(mlib_image *dst,
                                        const mlib_image *src,
                                        const mlib_d64 *mtx,
                                        const void *interp_table,
                                        mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageAffineTransform mlib_ImageAffineTransform
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageAffineTransform(mlib_image *dst,
                                         const mlib_image *src,
                                         const mlib_d64 *mtx,
                                         mlib_filter filter,
                                         mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageAffineTransform_Fp mlib_ImageAffineTransform_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageAffineTransform_Fp(mlib_image *dst,
                                            const mlib_image *src,
                                            const mlib_d64 *mtx,
                                            mlib_filter filter,
                                            mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageAffine_Fp mlib_ImageAffine_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageAffine_Fp(mlib_image *dst,
                                   const mlib_image *src,
                                   const mlib_d64 *mtx,
                                   mlib_filter filter,
                                   mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageFilteredSubsample mlib_ImageFilteredSubsample
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageFilteredSubsample(mlib_image *dst,
                                           const mlib_image *src,
                                           mlib_s32 scaleX,
                                           mlib_s32 scaleY,
                                           mlib_s32 transX,
                                           mlib_s32 transY,
                                           const mlib_d64 *hKernel,
                                           const mlib_d64 *vKernel,
                                           mlib_s32 hSize,
                                           mlib_s32 vSize,
                                           mlib_s32 hParity,
                                           mlib_s32 vParity,
                                           mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageFilteredSubsample_Fp mlib_ImageFilteredSubsample_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageFilteredSubsample_Fp(mlib_image *dst,
                                              const mlib_image *src,
                                              mlib_s32 scaleX,
                                              mlib_s32 scaleY,
                                              mlib_s32 transX,
                                              mlib_s32 transY,
                                              const mlib_d64 *hKernel,
                                              const mlib_d64 *vKernel,
                                              mlib_s32 hSize,
                                              mlib_s32 vSize,
                                              mlib_s32 hParity,
                                              mlib_s32 vParity,
                                              mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageFlipAntiDiag mlib_ImageFlipAntiDiag
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageFlipAntiDiag(mlib_image *dst,
                                      const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageFlipAntiDiag_Fp mlib_ImageFlipAntiDiag_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageFlipAntiDiag_Fp(mlib_image *dst,
                                         const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageFlipMainDiag mlib_ImageFlipMainDiag
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageFlipMainDiag(mlib_image *dst,
                                      const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageFlipMainDiag_Fp mlib_ImageFlipMainDiag_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageFlipMainDiag_Fp(mlib_image *dst,
                                         const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageFlipX mlib_ImageFlipX
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageFlipX(mlib_image *dst,
                               const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageFlipX_Fp mlib_ImageFlipX_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageFlipX_Fp(mlib_image *dst,
                                  const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageFlipY mlib_ImageFlipY
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageFlipY(mlib_image *dst,
                               const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageFlipY_Fp mlib_ImageFlipY_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageFlipY_Fp(mlib_image *dst,
                                  const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageGridWarp mlib_ImageGridWarp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageGridWarp(mlib_image *dst,
                                  const mlib_image *src,
                                  const mlib_f32 *xWarpPos,
                                  const mlib_f32 *yWarpPos,
                                  mlib_d64 postShiftX,
                                  mlib_d64 postShiftY,
                                  mlib_s32 xStart,
                                  mlib_s32 xStep,
                                  mlib_s32 xNumCells,
                                  mlib_s32 yStart,
                                  mlib_s32 yStep,
                                  mlib_s32 yNumCells,
                                  mlib_filter filter,
                                  mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageGridWarpTable mlib_ImageGridWarpTable
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageGridWarpTable(mlib_image *dst,
                                       const mlib_image *src,
                                       const mlib_f32 *xWarpPos,
                                       const mlib_f32 *yWarpPos,
                                       mlib_d64 postShiftX,
                                       mlib_d64 postShiftY,
                                       mlib_s32 xStart,
                                       mlib_s32 xStep,
                                       mlib_s32 xNumCells,
                                       mlib_s32 yStart,
                                       mlib_s32 yStep,
                                       mlib_s32 yNumCells,
                                       const void *table,
                                       mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageGridWarpTable_Fp mlib_ImageGridWarpTable_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageGridWarpTable_Fp(mlib_image *dst,
                                          const mlib_image *src,
                                          const mlib_f32 *xWarpPos,
                                          const mlib_f32 *yWarpPos,
                                          mlib_d64 postShiftX,
                                          mlib_d64 postShiftY,
                                          mlib_s32 xStart,
                                          mlib_s32 xStep,
                                          mlib_s32 xNumCells,
                                          mlib_s32 yStart,
                                          mlib_s32 yStep,
                                          mlib_s32 yNumCells,
                                          const void *table,
                                          mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageGridWarp_Fp mlib_ImageGridWarp_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageGridWarp_Fp(mlib_image *dst,
                                     const mlib_image *src,
                                     const mlib_f32 *xWarpPos,
                                     const mlib_f32 *yWarpPos,
                                     mlib_d64 postShiftX,
                                     mlib_d64 postShiftY,
                                     mlib_s32 xStart,
                                     mlib_s32 xStep,
                                     mlib_s32 xNumCells,
                                     mlib_s32 yStart,
                                     mlib_s32 yStep,
                                     mlib_s32 yNumCells,
                                     mlib_filter filter,
                                     mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageInterpTableCreate mlib_ImageInterpTableCreate
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
void * __mlib_ImageInterpTableCreate(mlib_type type,
                                     mlib_s32 width,
                                     mlib_s32 height,
                                     mlib_s32 leftPadding,
                                     mlib_s32 topPadding,
                                     mlib_s32 subsampleBitsH,
                                     mlib_s32 subsampleBitsV,
                                     mlib_s32 precisionBits,
                                     const void *dataH,
                                     const void *dataV);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageInterpTableDelete mlib_ImageInterpTableDelete
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
void  __mlib_ImageInterpTableDelete(void *interp_table);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImagePolynomialWarp mlib_ImagePolynomialWarp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImagePolynomialWarp(mlib_image *dst,
                                        const mlib_image *src,
                                        const mlib_d64 *xCoeffs,
                                        const mlib_d64 *yCoeffs,
                                        mlib_s32 n,
                                        mlib_d64 preShiftX,
                                        mlib_d64 preShiftY,
                                        mlib_d64 postShiftX,
                                        mlib_d64 postShiftY,
                                        mlib_d64 preScaleX,
                                        mlib_d64 preScaleY,
                                        mlib_d64 postScaleX,
                                        mlib_d64 postScaleY,
                                        mlib_filter filter,
                                        mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImagePolynomialWarpTable mlib_ImagePolynomialWarpTable
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImagePolynomialWarpTable(mlib_image *dst,
                                             const mlib_image *src,
                                             const mlib_d64 *xCoeffs,
                                             const mlib_d64 *yCoeffs,
                                             mlib_s32 n,
                                             mlib_d64 preShiftX,
                                             mlib_d64 preShiftY,
                                             mlib_d64 postShiftX,
                                             mlib_d64 postShiftY,
                                             mlib_d64 preScaleX,
                                             mlib_d64 preScaleY,
                                             mlib_d64 postScaleX,
                                             mlib_d64 postScaleY,
                                             const void *interp_table,
                                             mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImagePolynomialWarpTable_Fp mlib_ImagePolynomialWarpTable_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImagePolynomialWarpTable_Fp(mlib_image *dst,
                                                const mlib_image *src,
                                                const mlib_d64 *xCoeffs,
                                                const mlib_d64 *yCoeffs,
                                                mlib_s32 n,
                                                mlib_d64 preShiftX,
                                                mlib_d64 preShiftY,
                                                mlib_d64 postShiftX,
                                                mlib_d64 postShiftY,
                                                mlib_d64 preScaleX,
                                                mlib_d64 preScaleY,
                                                mlib_d64 postScaleX,
                                                mlib_d64 postScaleY,
                                                const void *interp_table,
                                                mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImagePolynomialWarp_Fp mlib_ImagePolynomialWarp_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImagePolynomialWarp_Fp(mlib_image *dst,
                                           const mlib_image *src,
                                           const mlib_d64 *xCoeffs,
                                           const mlib_d64 *yCoeffs,
                                           mlib_s32 n,
                                           mlib_d64 preShiftX,
                                           mlib_d64 preShiftY,
                                           mlib_d64 postShiftX,
                                           mlib_d64 postShiftY,
                                           mlib_d64 preScaleX,
                                           mlib_d64 preScaleY,
                                           mlib_d64 postScaleX,
                                           mlib_d64 postScaleY,
                                           mlib_filter filter,
                                           mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageRotate mlib_ImageRotate
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageRotate(mlib_image *dst,
                                const mlib_image *src,
                                mlib_d64 angle,
                                mlib_d64 xcenter,
                                mlib_d64 ycenter,
                                mlib_filter filter,
                                mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageRotate180 mlib_ImageRotate180
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageRotate180(mlib_image *dst,
                                   const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageRotate180_Fp mlib_ImageRotate180_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageRotate180_Fp(mlib_image *dst,
                                      const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageRotate270 mlib_ImageRotate270
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageRotate270(mlib_image *dst,
                                   const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageRotate270_Fp mlib_ImageRotate270_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageRotate270_Fp(mlib_image *dst,
                                      const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageRotate90 mlib_ImageRotate90
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageRotate90(mlib_image *dst,
                                  const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageRotate90_Fp mlib_ImageRotate90_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageRotate90_Fp(mlib_image *dst,
                                     const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageRotate_Fp mlib_ImageRotate_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageRotate_Fp(mlib_image *dst,
                                   const mlib_image *src,
                                   mlib_d64 angle,
                                   mlib_d64 xcenter,
                                   mlib_d64 ycenter,
                                   mlib_filter filter,
                                   mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageSubsampleAverage mlib_ImageSubsampleAverage
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageSubsampleAverage(mlib_image *dst,
                                          const mlib_image *src,
                                          mlib_d64 scalex,
                                          mlib_d64 scaley);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageSubsampleAverage_Fp mlib_ImageSubsampleAverage_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageSubsampleAverage_Fp(mlib_image *dst,
                                             const mlib_image *src,
                                             mlib_d64 scalex,
                                             mlib_d64 scaley);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageSubsampleBinaryToGray mlib_ImageSubsampleBinaryToGray
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageSubsampleBinaryToGray(mlib_image *dst,
                                               const mlib_image *src,
                                               mlib_d64 xscale,
                                               mlib_d64 yscale,
                                               const mlib_u8 *lutGray);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageZoomIn2X mlib_ImageZoomIn2X
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageZoomIn2X(mlib_image *dst,
                                  const mlib_image *src,
                                  mlib_filter filter,
                                  mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageZoomIn2X_Fp mlib_ImageZoomIn2X_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageZoomIn2X_Fp(mlib_image *dst,
                                     const mlib_image *src,
                                     mlib_filter filter,
                                     mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageZoomOut2X mlib_ImageZoomOut2X
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageZoomOut2X(mlib_image *dst,
                                   const mlib_image *src,
                                   mlib_filter filter,
                                   mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageZoomOut2X_Fp mlib_ImageZoomOut2X_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageZoomOut2X_Fp(mlib_image *dst,
                                      const mlib_image *src,
                                      mlib_filter filter,
                                      mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageZoomTranslate mlib_ImageZoomTranslate
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageZoomTranslate(mlib_image *dst,
                                       const mlib_image *src,
                                       mlib_d64 zoomx,
                                       mlib_d64 zoomy,
                                       mlib_d64 tx,
                                       mlib_d64 ty,
                                       mlib_filter filter,
                                       mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageZoomTranslateTable mlib_ImageZoomTranslateTable
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageZoomTranslateTable(mlib_image *dst,
                                            const mlib_image *src,
                                            mlib_d64 zoomx,
                                            mlib_d64 zoomy,
                                            mlib_d64 tx,
                                            mlib_d64 ty,
                                            const void *interp_table,
                                            mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageZoomTranslateTable_Fp mlib_ImageZoomTranslateTable_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageZoomTranslateTable_Fp(mlib_image *dst,
                                               const mlib_image *src,
                                               mlib_d64 zoomx,
                                               mlib_d64 zoomy,
                                               mlib_d64 tx,
                                               mlib_d64 ty,
                                               const void *interp_table,
                                               mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageZoomTranslateToGray mlib_ImageZoomTranslateToGray
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageZoomTranslateToGray(mlib_image *dst,
                                             const mlib_image *src,
                                             mlib_d64 zoomx,
                                             mlib_d64 zoomy,
                                             mlib_d64 tx,
                                             mlib_d64 ty,
                                             mlib_filter filter,
                                             mlib_edge edge,
                                             const mlib_s32 *ghigh,
                                             const mlib_s32 *glow);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageZoomTranslate_Fp mlib_ImageZoomTranslate_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageZoomTranslate_Fp(mlib_image *dst,
                                          const mlib_image *src,
                                          mlib_d64 zoomx,
                                          mlib_d64 zoomy,
                                          mlib_d64 tx,
                                          mlib_d64 ty,
                                          mlib_filter filter,
                                          mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageZoom mlib_ImageZoom
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageZoom(mlib_image *dst,
                              const mlib_image *src,
                              mlib_d64 zoomx,
                              mlib_d64 zoomy,
                              mlib_filter filter,
                              mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageZoom_Fp mlib_ImageZoom_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageZoom_Fp(mlib_image *dst,
                                 const mlib_image *src,
                                 mlib_d64 zoomx,
                                 mlib_d64 zoomy,
                                 mlib_filter filter,
                                 mlib_edge edge);

/* Logical Operations ( logic ) */


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageAnd mlib_ImageAnd
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageAnd(mlib_image *dst,
                             const mlib_image *src1,
                             const mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageAndNot mlib_ImageAndNot
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageAndNot(mlib_image *dst,
                                const mlib_image *src1,
                                const mlib_image *src2);

/* src1dst = src1dst & (~src2) */

#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageAndNot1_Inp mlib_ImageAndNot1_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageAndNot1_Inp(mlib_image *src1dst,
                                     const mlib_image *src2);

/* src2dst = src1 & (~src2dst) */

#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageAndNot2_Inp mlib_ImageAndNot2_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageAndNot2_Inp(mlib_image *src2dst,
                                     const mlib_image *src1);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageAnd_Inp mlib_ImageAnd_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageAnd_Inp(mlib_image *src1dst,
                                 const mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstAnd mlib_ImageConstAnd
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstAnd(mlib_image *dst,
                                  const mlib_image *src,
                                  const mlib_s32 *c);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstAndNot mlib_ImageConstAndNot
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstAndNot(mlib_image *dst,
                                     const mlib_image *src,
                                     const mlib_s32 *c);

/* srcdst = (~srcdst) & c */

#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstAndNot_Inp mlib_ImageConstAndNot_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstAndNot_Inp(mlib_image *srcdst,
                                         const mlib_s32 *c);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstAnd_Inp mlib_ImageConstAnd_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstAnd_Inp(mlib_image *srcdst,
                                      const mlib_s32 *c);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstNotAnd mlib_ImageConstNotAnd
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstNotAnd(mlib_image *dst,
                                     const mlib_image *src,
                                     const mlib_s32 *c);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstNotAnd_Inp mlib_ImageConstNotAnd_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstNotAnd_Inp(mlib_image *srcdst,
                                         const mlib_s32 *c);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstNotOr mlib_ImageConstNotOr
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstNotOr(mlib_image *dst,
                                    const mlib_image *src,
                                    const mlib_s32 *c);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstNotOr_Inp mlib_ImageConstNotOr_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstNotOr_Inp(mlib_image *srcdst,
                                        const mlib_s32 *c);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstNotXor mlib_ImageConstNotXor
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstNotXor(mlib_image *dst,
                                     const mlib_image *src,
                                     const mlib_s32 *c);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstNotXor_Inp mlib_ImageConstNotXor_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstNotXor_Inp(mlib_image *srcdst,
                                         const mlib_s32 *c);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstOr mlib_ImageConstOr
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstOr(mlib_image *dst,
                                 const mlib_image *src,
                                 const mlib_s32 *c);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstOrNot mlib_ImageConstOrNot
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstOrNot(mlib_image *dst,
                                    const mlib_image *src,
                                    const mlib_s32 *c);

/* srcdst = (~srcdst) | c */

#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstOrNot_Inp mlib_ImageConstOrNot_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstOrNot_Inp(mlib_image *srcdst,
                                        const mlib_s32 *c);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstOr_Inp mlib_ImageConstOr_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstOr_Inp(mlib_image *srcdst,
                                     const mlib_s32 *c);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstXor mlib_ImageConstXor
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstXor(mlib_image *dst,
                                  mlib_image *src,
                                  mlib_s32 *c);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConstXor_Inp mlib_ImageConstXor_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConstXor_Inp(mlib_image *srcdst,
                                      const mlib_s32 *c);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageNot mlib_ImageNot
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageNot(mlib_image *dst,
                             const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageNotAnd mlib_ImageNotAnd
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageNotAnd(mlib_image *dst,
                                const mlib_image *src1,
                                const mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageNotAnd_Inp mlib_ImageNotAnd_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageNotAnd_Inp(mlib_image *src1dst,
                                    const mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageNotOr mlib_ImageNotOr
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageNotOr(mlib_image *dst,
                               const mlib_image *src1,
                               const mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageNotOr_Inp mlib_ImageNotOr_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageNotOr_Inp(mlib_image *src1dst,
                                   const mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageNotXor mlib_ImageNotXor
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageNotXor(mlib_image *dst,
                                const mlib_image *src1,
                                const mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageNotXor_Inp mlib_ImageNotXor_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageNotXor_Inp(mlib_image *src1dst,
                                    const mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageNot_Inp mlib_ImageNot_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageNot_Inp(mlib_image *srcdst);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageOr mlib_ImageOr
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageOr(mlib_image *dst,
                            const mlib_image *src1,
                            const mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageOrNot mlib_ImageOrNot
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageOrNot(mlib_image *dst,
                               const mlib_image *src1,
                               const mlib_image *src2);

/* src1dst = src1dst | (~src2) */

#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageOrNot1_Inp mlib_ImageOrNot1_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageOrNot1_Inp(mlib_image *src1dst,
                                    const mlib_image *src2);

/* src2dst = src1 | (~src2dst) */

#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageOrNot2_Inp mlib_ImageOrNot2_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageOrNot2_Inp(mlib_image *src2dst,
                                    const mlib_image *src1);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageOr_Inp mlib_ImageOr_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageOr_Inp(mlib_image *src1dst,
                                const mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageXor mlib_ImageXor
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageXor(mlib_image *dst,
                             mlib_image *src1,
                             mlib_image *src2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageXor_Inp mlib_ImageXor_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageXor_Inp(mlib_image *src1dst,
                                 const mlib_image *src2);

/* Radiometric Operations ( radio ) */


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorReplace mlib_ImageColorReplace
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorReplace(mlib_image *dst,
                                      const mlib_image *src,
                                      const mlib_s32 *color1,
                                      const mlib_s32 *color2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorReplace_Fp mlib_ImageColorReplace_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorReplace_Fp(mlib_image *dst,
                                         const mlib_image *src,
                                         const mlib_d64 *color1,
                                         const mlib_d64 *color2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorReplace_Fp_Inp mlib_ImageColorReplace_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorReplace_Fp_Inp(mlib_image *srcdst,
                                             const mlib_d64 *color1,
                                             const mlib_d64 *color2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageColorReplace_Inp mlib_ImageColorReplace_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageColorReplace_Inp(mlib_image *srcdst,
                                          const mlib_s32 *color1,
                                          const mlib_s32 *color2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageHistogram mlib_ImageHistogram
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageHistogram(mlib_s32 ** histo,
                                   const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageHistogram2 mlib_ImageHistogram2
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageHistogram2(mlib_s32 ** histo,
                                    const mlib_image *img,
                                    const mlib_s32 *numBins,
                                    const mlib_s32 *lowValue,
                                    const mlib_s32 *highValue,
                                    mlib_s32 xStart,
                                    mlib_s32 yStart,
                                    mlib_s32 xPeriod,
                                    mlib_s32 yPeriod);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageLookUp mlib_ImageLookUp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
JNIEXPORT mlib_status
__mlib_ImageLookUp(mlib_image *dst,
                   const mlib_image *src,
                   const void **table);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageLookUp2 mlib_ImageLookUp2
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageLookUp2(mlib_image *dst,
                                 const mlib_image *src,
                                 const void **table,
                                 const mlib_s32 *offsets,
                                 mlib_s32 channels);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageLookUpMask mlib_ImageLookUpMask
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageLookUpMask(mlib_image *dst,
                                    const mlib_image *src,
                                    const void **table,
                                    mlib_s32 channels,
                                    mlib_s32 cmask);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageLookUp_Inp mlib_ImageLookUp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageLookUp_Inp(mlib_image *srcdst,
                                    const void **table);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageThresh1 mlib_ImageThresh1
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageThresh1(mlib_image *dst,
                                 const mlib_image *src,
                                 const mlib_s32 *thresh,
                                 const mlib_s32 *ghigh,
                                 const mlib_s32 *glow);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageThresh1_Fp mlib_ImageThresh1_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageThresh1_Fp(mlib_image *dst,
                                    const mlib_image *src,
                                    const mlib_d64 *thresh,
                                    const mlib_d64 *ghigh,
                                    const mlib_d64 *glow);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageThresh1_Fp_Inp mlib_ImageThresh1_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageThresh1_Fp_Inp(mlib_image *srcdst,
                                        const mlib_d64 *thresh,
                                        const mlib_d64 *ghigh,
                                        const mlib_d64 *glow);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageThresh1_Inp mlib_ImageThresh1_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageThresh1_Inp(mlib_image *srcdst,
                                     const mlib_s32 *thresh,
                                     const mlib_s32 *ghigh,
                                     const mlib_s32 *glow);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageThresh2 mlib_ImageThresh2
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageThresh2(mlib_image *dst,
                                 const mlib_image *src,
                                 const mlib_s32 *thresh,
                                 const mlib_s32 *glow);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageThresh2_Fp mlib_ImageThresh2_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageThresh2_Fp(mlib_image *dst,
                                    const mlib_image *src,
                                    const mlib_d64 *thresh,
                                    const mlib_d64 *glow);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageThresh2_Fp_Inp mlib_ImageThresh2_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageThresh2_Fp_Inp(mlib_image *srcdst,
                                        const mlib_d64 *thresh,
                                        const mlib_d64 *glow);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageThresh2_Inp mlib_ImageThresh2_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageThresh2_Inp(mlib_image *srcdst,
                                     const mlib_s32 *thresh,
                                     const mlib_s32 *glow);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageThresh3 mlib_ImageThresh3
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageThresh3(mlib_image *dst,
                                 const mlib_image *src,
                                 const mlib_s32 *thresh,
                                 const mlib_s32 *ghigh);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageThresh3_Fp mlib_ImageThresh3_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageThresh3_Fp(mlib_image *dst,
                                    const mlib_image *src,
                                    const mlib_d64 *thresh,
                                    const mlib_d64 *ghigh);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageThresh3_Fp_Inp mlib_ImageThresh3_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageThresh3_Fp_Inp(mlib_image *srcdst,
                                        const mlib_d64 *thresh,
                                        const mlib_d64 *ghigh);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageThresh3_Inp mlib_ImageThresh3_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageThresh3_Inp(mlib_image *srcdst,
                                     const mlib_s32 *thresh,
                                     const mlib_s32 *ghigh);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageThresh4 mlib_ImageThresh4
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageThresh4(mlib_image *dst,
                                 const mlib_image *src,
                                 const mlib_s32 *thigh,
                                 const mlib_s32 *tlow,
                                 const mlib_s32 *ghigh,
                                 const mlib_s32 *glow);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageThresh4_Fp mlib_ImageThresh4_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageThresh4_Fp(mlib_image *dst,
                                    const mlib_image *src,
                                    const mlib_d64 *thigh,
                                    const mlib_d64 *tlow,
                                    const mlib_d64 *ghigh,
                                    const mlib_d64 *glow);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageThresh4_Fp_Inp mlib_ImageThresh4_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageThresh4_Fp_Inp(mlib_image *srcdst,
                                        const mlib_d64 *thigh,
                                        const mlib_d64 *tlow,
                                        const mlib_d64 *ghigh,
                                        const mlib_d64 *glow);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageThresh4_Inp mlib_ImageThresh4_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageThresh4_Inp(mlib_image *srcdst,
                                     const mlib_s32 *thigh,
                                     const mlib_s32 *tlow,
                                     const mlib_s32 *ghigh,
                                     const mlib_s32 *glow);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageThresh5 mlib_ImageThresh5
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageThresh5(mlib_image *dst,
                                 const mlib_image *src,
                                 const mlib_s32 *thigh,
                                 const mlib_s32 *tlow,
                                 const mlib_s32 *gmid);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageThresh5_Fp mlib_ImageThresh5_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageThresh5_Fp(mlib_image *dst,
                                    const mlib_image *src,
                                    const mlib_d64 *thigh,
                                    const mlib_d64 *tlow,
                                    const mlib_d64 *gmid);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageThresh5_Fp_Inp mlib_ImageThresh5_Fp_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageThresh5_Fp_Inp(mlib_image *srcdst,
                                        const mlib_d64 *thigh,
                                        const mlib_d64 *tlow,
                                        const mlib_d64 *gmid);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageThresh5_Inp mlib_ImageThresh5_Inp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageThresh5_Inp(mlib_image *srcdst,
                                     const mlib_s32 *thigh,
                                     const mlib_s32 *tlow,
                                     const mlib_s32 *gmid);

/* Linear and Not-Linear Spatial Operations, Morphological Operations ( spatial ) */


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConv2x2 mlib_ImageConv2x2
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConv2x2(mlib_image *dst,
                                 const mlib_image *src,
                                 const mlib_s32 *kernel,
                                 mlib_s32 scale,
                                 mlib_s32 cmask,
                                 mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConv2x2_Fp mlib_ImageConv2x2_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConv2x2_Fp(mlib_image *dst,
                                    const mlib_image *src,
                                    const mlib_d64 *kernel,
                                    mlib_s32 cmask,
                                    mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConv3x3 mlib_ImageConv3x3
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConv3x3(mlib_image *dst,
                                 const mlib_image *src,
                                 const mlib_s32 *kernel,
                                 mlib_s32 scale,
                                 mlib_s32 cmask,
                                 mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConv3x3_Fp mlib_ImageConv3x3_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConv3x3_Fp(mlib_image *dst,
                                    const mlib_image *src,
                                    const mlib_d64 *kernel,
                                    mlib_s32 cmask,
                                    mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConv4x4 mlib_ImageConv4x4
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConv4x4(mlib_image *dst,
                                 const mlib_image *src,
                                 const mlib_s32 *kernel,
                                 mlib_s32 scale,
                                 mlib_s32 cmask,
                                 mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConv4x4_Fp mlib_ImageConv4x4_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConv4x4_Fp(mlib_image *dst,
                                    const mlib_image *src,
                                    const mlib_d64 *kernel,
                                    mlib_s32 cmask,
                                    mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConv5x5 mlib_ImageConv5x5
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConv5x5(mlib_image *dst,
                                 const mlib_image *src,
                                 const mlib_s32 *kernel,
                                 mlib_s32 scale,
                                 mlib_s32 cmask,
                                 mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConv5x5_Fp mlib_ImageConv5x5_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConv5x5_Fp(mlib_image *dst,
                                    const mlib_image *src,
                                    const mlib_d64 *kernel,
                                    mlib_s32 cmask,
                                    mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConv7x7 mlib_ImageConv7x7
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConv7x7(mlib_image *dst,
                                 const mlib_image *src,
                                 const mlib_s32 *kernel,
                                 mlib_s32 scale,
                                 mlib_s32 cmask,
                                 mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConv7x7_Fp mlib_ImageConv7x7_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConv7x7_Fp(mlib_image *dst,
                                    const mlib_image *src,
                                    const mlib_d64 *kernel,
                                    mlib_s32 cmask,
                                    mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConvKernelConvert mlib_ImageConvKernelConvert
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
JNIEXPORT mlib_status
__mlib_ImageConvKernelConvert(mlib_s32 *ikernel,
                              mlib_s32 *iscale,
                              const mlib_d64 *fkernel,
                              mlib_s32 m,
                              mlib_s32 n,
                              mlib_type type);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConvMxN mlib_ImageConvMxN
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
JNIEXPORT mlib_status
__mlib_ImageConvMxN(mlib_image *dst,
                    const mlib_image *src,
                    const mlib_s32 *kernel,
                    mlib_s32 m,
                    mlib_s32 n,
                    mlib_s32 dm,
                    mlib_s32 dn,
                    mlib_s32 scale,
                    mlib_s32 cmask,
                    mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConvMxN_Fp mlib_ImageConvMxN_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConvMxN_Fp(mlib_image *dst,
                                    const mlib_image *src,
                                    const mlib_d64 *kernel,
                                    mlib_s32 m,
                                    mlib_s32 n,
                                    mlib_s32 dm,
                                    mlib_s32 dn,
                                    mlib_s32 cmask,
                                    mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConvolveMxN mlib_ImageConvolveMxN
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConvolveMxN(mlib_image *dst,
                                     const mlib_image *src,
                                     const mlib_d64 *kernel,
                                     mlib_s32 m,
                                     mlib_s32 n,
                                     mlib_s32 dm,
                                     mlib_s32 dn,
                                     mlib_s32 cmask,
                                     mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageConvolveMxN_Fp mlib_ImageConvolveMxN_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageConvolveMxN_Fp(mlib_image *dst,
                                        const mlib_image *src,
                                        const mlib_d64 *kernel,
                                        mlib_s32 m,
                                        mlib_s32 n,
                                        mlib_s32 dm,
                                        mlib_s32 dn,
                                        mlib_s32 cmask,
                                        mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageDilate4 mlib_ImageDilate4
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageDilate4(mlib_image *dst,
                                 const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageDilate4_Fp mlib_ImageDilate4_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageDilate4_Fp(mlib_image *dst,
                                    const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageDilate8 mlib_ImageDilate8
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageDilate8(mlib_image *dst,
                                 const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageDilate8_Fp mlib_ImageDilate8_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageDilate8_Fp(mlib_image *dst,
                                    const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageErode4 mlib_ImageErode4
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageErode4(mlib_image *dst,
                                const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageErode4_Fp mlib_ImageErode4_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageErode4_Fp(mlib_image *dst,
                                   const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageErode8 mlib_ImageErode8
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageErode8(mlib_image *dst,
                                const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageErode8_Fp mlib_ImageErode8_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageErode8_Fp(mlib_image *dst,
                                   const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageGradient3x3 mlib_ImageGradient3x3
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageGradient3x3(mlib_image *dst,
                                     const mlib_image *src,
                                     const mlib_d64 *hmask,
                                     const mlib_d64 *vmask,
                                     mlib_s32 cmask,
                                     mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageGradient3x3_Fp mlib_ImageGradient3x3_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageGradient3x3_Fp(mlib_image *dst,
                                        const mlib_image *src,
                                        const mlib_d64 *hmask,
                                        const mlib_d64 *vmask,
                                        mlib_s32 cmask,
                                        mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageGradientMxN mlib_ImageGradientMxN
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageGradientMxN(mlib_image *dst,
                                     const mlib_image *src,
                                     const mlib_d64 *hmask,
                                     const mlib_d64 *vmask,
                                     mlib_s32 m,
                                     mlib_s32 n,
                                     mlib_s32 dm,
                                     mlib_s32 dn,
                                     mlib_s32 cmask,
                                     mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageGradientMxN_Fp mlib_ImageGradientMxN_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageGradientMxN_Fp(mlib_image *dst,
                                        const mlib_image *src,
                                        const mlib_d64 *hmask,
                                        const mlib_d64 *vmask,
                                        mlib_s32 m,
                                        mlib_s32 n,
                                        mlib_s32 dm,
                                        mlib_s32 dn,
                                        mlib_s32 cmask,
                                        mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMaxFilter3x3 mlib_ImageMaxFilter3x3
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMaxFilter3x3(mlib_image *dst,
                                      const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMaxFilter3x3_Fp mlib_ImageMaxFilter3x3_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMaxFilter3x3_Fp(mlib_image *dst,
                                         const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMaxFilter5x5 mlib_ImageMaxFilter5x5
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMaxFilter5x5(mlib_image *dst,
                                      const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMaxFilter5x5_Fp mlib_ImageMaxFilter5x5_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMaxFilter5x5_Fp(mlib_image *dst,
                                         const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMaxFilter7x7 mlib_ImageMaxFilter7x7
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMaxFilter7x7(mlib_image *dst,
                                      const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMaxFilter7x7_Fp mlib_ImageMaxFilter7x7_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMaxFilter7x7_Fp(mlib_image *dst,
                                         const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMedianFilter3x3 mlib_ImageMedianFilter3x3
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMedianFilter3x3(mlib_image *dst,
                                         const mlib_image *src,
                                         mlib_median_mask mmask,
                                         mlib_s32 cmask,
                                         mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMedianFilter3x3_Fp mlib_ImageMedianFilter3x3_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMedianFilter3x3_Fp(mlib_image *dst,
                                            const mlib_image *src,
                                            mlib_median_mask mmask,
                                            mlib_s32 cmask,
                                            mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMedianFilter3x3_US mlib_ImageMedianFilter3x3_US
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMedianFilter3x3_US(mlib_image *dst,
                                            const mlib_image *src,
                                            mlib_median_mask mmask,
                                            mlib_s32 cmask,
                                            mlib_edge edge,
                                            mlib_s32 bits);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMedianFilter5x5 mlib_ImageMedianFilter5x5
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMedianFilter5x5(mlib_image *dst,
                                         const mlib_image *src,
                                         mlib_median_mask mmask,
                                         mlib_s32 cmask,
                                         mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMedianFilter5x5_Fp mlib_ImageMedianFilter5x5_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMedianFilter5x5_Fp(mlib_image *dst,
                                            const mlib_image *src,
                                            mlib_median_mask mmask,
                                            mlib_s32 cmask,
                                            mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMedianFilter5x5_US mlib_ImageMedianFilter5x5_US
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMedianFilter5x5_US(mlib_image *dst,
                                            const mlib_image *src,
                                            mlib_median_mask mmask,
                                            mlib_s32 cmask,
                                            mlib_edge edge,
                                            mlib_s32 bits);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMedianFilter7x7 mlib_ImageMedianFilter7x7
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMedianFilter7x7(mlib_image *dst,
                                         const mlib_image *src,
                                         mlib_median_mask mmask,
                                         mlib_s32 cmask,
                                         mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMedianFilter7x7_Fp mlib_ImageMedianFilter7x7_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMedianFilter7x7_Fp(mlib_image *dst,
                                            const mlib_image *src,
                                            mlib_median_mask mmask,
                                            mlib_s32 cmask,
                                            mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMedianFilter7x7_US mlib_ImageMedianFilter7x7_US
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMedianFilter7x7_US(mlib_image *dst,
                                            const mlib_image *src,
                                            mlib_median_mask mmask,
                                            mlib_s32 cmask,
                                            mlib_edge edge,
                                            mlib_s32 bits);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMedianFilterMxN mlib_ImageMedianFilterMxN
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMedianFilterMxN(mlib_image *dst,
                                         const mlib_image *src,
                                         mlib_s32 m,
                                         mlib_s32 n,
                                         mlib_median_mask mmask,
                                         mlib_s32 cmask,
                                         mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMedianFilterMxN_Fp mlib_ImageMedianFilterMxN_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMedianFilterMxN_Fp(mlib_image *dst,
                                            const mlib_image *src,
                                            mlib_s32 m,
                                            mlib_s32 n,
                                            mlib_median_mask mmask,
                                            mlib_s32 cmask,
                                            mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMedianFilterMxN_US mlib_ImageMedianFilterMxN_US
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMedianFilterMxN_US(mlib_image *dst,
                                            const mlib_image *src,
                                            mlib_s32 m,
                                            mlib_s32 n,
                                            mlib_median_mask mmask,
                                            mlib_s32 cmask,
                                            mlib_edge edge,
                                            mlib_s32 bits);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMinFilter3x3 mlib_ImageMinFilter3x3
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMinFilter3x3(mlib_image *dst,
                                      const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMinFilter3x3_Fp mlib_ImageMinFilter3x3_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMinFilter3x3_Fp(mlib_image *dst,
                                         const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMinFilter5x5 mlib_ImageMinFilter5x5
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMinFilter5x5(mlib_image *dst,
                                      const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMinFilter5x5_Fp mlib_ImageMinFilter5x5_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMinFilter5x5_Fp(mlib_image *dst,
                                         const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMinFilter7x7 mlib_ImageMinFilter7x7
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMinFilter7x7(mlib_image *dst,
                                      const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMinFilter7x7_Fp mlib_ImageMinFilter7x7_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMinFilter7x7_Fp(mlib_image *dst,
                                         const mlib_image *src);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageRankFilter3x3 mlib_ImageRankFilter3x3
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageRankFilter3x3(mlib_image *dst,
                                       const mlib_image *src,
                                       mlib_s32 rank);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageRankFilter3x3_Fp mlib_ImageRankFilter3x3_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageRankFilter3x3_Fp(mlib_image *dst,
                                          const mlib_image *src,
                                          mlib_s32 rank);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageRankFilter3x3_US mlib_ImageRankFilter3x3_US
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageRankFilter3x3_US(mlib_image *dst,
                                          const mlib_image *src,
                                          mlib_s32 rank,
                                          mlib_s32 bits);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageRankFilter5x5 mlib_ImageRankFilter5x5
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageRankFilter5x5(mlib_image *dst,
                                       const mlib_image *src,
                                       mlib_s32 rank);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageRankFilter5x5_Fp mlib_ImageRankFilter5x5_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageRankFilter5x5_Fp(mlib_image *dst,
                                          const mlib_image *src,
                                          mlib_s32 rank);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageRankFilter5x5_US mlib_ImageRankFilter5x5_US
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageRankFilter5x5_US(mlib_image *dst,
                                          const mlib_image *src,
                                          mlib_s32 rank,
                                          mlib_s32 bits);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageRankFilter7x7 mlib_ImageRankFilter7x7
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageRankFilter7x7(mlib_image *dst,
                                       const mlib_image *src,
                                       mlib_s32 rank);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageRankFilter7x7_Fp mlib_ImageRankFilter7x7_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageRankFilter7x7_Fp(mlib_image *dst,
                                          const mlib_image *src,
                                          mlib_s32 rank);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageRankFilter7x7_US mlib_ImageRankFilter7x7_US
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageRankFilter7x7_US(mlib_image *dst,
                                          const mlib_image *src,
                                          mlib_s32 rank,
                                          mlib_s32 bits);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageRankFilterMxN mlib_ImageRankFilterMxN
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageRankFilterMxN(mlib_image *dst,
                                       const mlib_image *src,
                                       mlib_s32 m,
                                       mlib_s32 n,
                                       mlib_s32 rank);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageRankFilterMxN_Fp mlib_ImageRankFilterMxN_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageRankFilterMxN_Fp(mlib_image *dst,
                                          const mlib_image *src,
                                          mlib_s32 m,
                                          mlib_s32 n,
                                          mlib_s32 rank);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageRankFilterMxN_US mlib_ImageRankFilterMxN_US
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageRankFilterMxN_US(mlib_image *dst,
                                          const mlib_image *src,
                                          mlib_s32 m,
                                          mlib_s32 n,
                                          mlib_s32 rank,
                                          mlib_s32 bits);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageSConv3x3 mlib_ImageSConv3x3
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageSConv3x3(mlib_image *dst,
                                  const mlib_image *src,
                                  const mlib_s32 *hkernel,
                                  const mlib_s32 *vkernel,
                                  mlib_s32 scale,
                                  mlib_s32 cmask,
                                  mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageSConv3x3_Fp mlib_ImageSConv3x3_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageSConv3x3_Fp(mlib_image *dst,
                                     const mlib_image *src,
                                     const mlib_d64 *hkernel,
                                     const mlib_d64 *vkernel,
                                     mlib_s32 cmask,
                                     mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageSConv5x5 mlib_ImageSConv5x5
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageSConv5x5(mlib_image *dst,
                                  const mlib_image *src,
                                  const mlib_s32 *hkernel,
                                  const mlib_s32 *vkernel,
                                  mlib_s32 scale,
                                  mlib_s32 cmask,
                                  mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageSConv5x5_Fp mlib_ImageSConv5x5_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageSConv5x5_Fp(mlib_image *dst,
                                     const mlib_image *src,
                                     const mlib_d64 *hkernel,
                                     const mlib_d64 *vkernel,
                                     mlib_s32 cmask,
                                     mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageSConv7x7 mlib_ImageSConv7x7
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageSConv7x7(mlib_image *dst,
                                  const mlib_image *src,
                                  const mlib_s32 *hkernel,
                                  const mlib_s32 *vkernel,
                                  mlib_s32 scale,
                                  mlib_s32 cmask,
                                  mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageSConv7x7_Fp mlib_ImageSConv7x7_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageSConv7x7_Fp(mlib_image *dst,
                                     const mlib_image *src,
                                     const mlib_d64 *hkernel,
                                     const mlib_d64 *vkernel,
                                     mlib_s32 cmask,
                                     mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageSConvKernelConvert mlib_ImageSConvKernelConvert
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageSConvKernelConvert(mlib_s32 *ihkernel,
                                            mlib_s32 *ivkernel,
                                            mlib_s32 *iscale,
                                            const mlib_d64 *fhkernel,
                                            const mlib_d64 *fvkernel,
                                            mlib_s32 m,
                                            mlib_s32 n,
                                            mlib_type type);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageSobel mlib_ImageSobel
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageSobel(mlib_image *dst,
                               const mlib_image *src,
                               mlib_s32 cmask,
                               mlib_edge edge);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageSobel_Fp mlib_ImageSobel_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageSobel_Fp(mlib_image *dst,
                                  const mlib_image *src,
                                  mlib_s32 cmask,
                                  mlib_edge edge);

/* Image Staistics ( stat ) */


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageAutoCorrel mlib_ImageAutoCorrel
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageAutoCorrel(mlib_d64 *correl,
                                    const mlib_image *img,
                                    mlib_s32 dx,
                                    mlib_s32 dy);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageAutoCorrel_Fp mlib_ImageAutoCorrel_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageAutoCorrel_Fp(mlib_d64 *correl,
                                       const mlib_image *img,
                                       mlib_s32 dx,
                                       mlib_s32 dy);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageCrossCorrel mlib_ImageCrossCorrel
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageCrossCorrel(mlib_d64 *correl,
                                     const mlib_image *img1,
                                     const mlib_image *img2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageCrossCorrel_Fp mlib_ImageCrossCorrel_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageCrossCorrel_Fp(mlib_d64 *correl,
                                        const mlib_image *img1,
                                        const mlib_image *img2);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageExtrema2 mlib_ImageExtrema2
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageExtrema2(mlib_s32 *min,
                                  mlib_s32 *max,
                                  const mlib_image *img,
                                  mlib_s32 xStart,
                                  mlib_s32 yStart,
                                  mlib_s32 xPeriod,
                                  mlib_s32 yPeriod);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageExtrema2_Fp mlib_ImageExtrema2_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageExtrema2_Fp(mlib_d64 *min,
                                     mlib_d64 *max,
                                     const mlib_image *img,
                                     mlib_s32 xStart,
                                     mlib_s32 yStart,
                                     mlib_s32 xPeriod,
                                     mlib_s32 yPeriod);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageExtremaLocations mlib_ImageExtremaLocations
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageExtremaLocations(mlib_s32 *min,
                                          mlib_s32 *max,
                                          const mlib_image *img,
                                          mlib_s32 xStart,
                                          mlib_s32 yStart,
                                          mlib_s32 xPeriod,
                                          mlib_s32 yPeriod,
                                          mlib_s32 saveLocations,
                                          mlib_s32 maxRuns,
                                          mlib_s32 *minCounts,
                                          mlib_s32 *maxCounts,
                                          mlib_s32 **minLocations,
                                          mlib_s32 **maxLocations,
                                          mlib_s32 len);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageExtremaLocations_Fp mlib_ImageExtremaLocations_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageExtremaLocations_Fp(mlib_d64 *min,
                                             mlib_d64 *max,
                                             const mlib_image *img,
                                             mlib_s32 xStart,
                                             mlib_s32 yStart,
                                             mlib_s32 xPeriod,
                                             mlib_s32 yPeriod,
                                             mlib_s32 saveLocations,
                                             mlib_s32 maxRuns,
                                             mlib_s32 *minCounts,
                                             mlib_s32 *maxCounts,
                                             mlib_s32 **minLocations,
                                             mlib_s32 **maxLocations,
                                             mlib_s32 len);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMaximum mlib_ImageMaximum
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMaximum(mlib_s32 *max,
                                 const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMaximum_Fp mlib_ImageMaximum_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMaximum_Fp(mlib_d64 *max,
                                    const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMean mlib_ImageMean
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMean(mlib_d64 *mean,
                              const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMean_Fp mlib_ImageMean_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMean_Fp(mlib_d64 *mean,
                                 const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMinimum mlib_ImageMinimum
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMinimum(mlib_s32 *min,
                                 const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMinimum_Fp mlib_ImageMinimum_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMinimum_Fp(mlib_d64 *min,
                                    const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMoment2 mlib_ImageMoment2
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMoment2(mlib_d64 *moment,
                                 const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageMoment2_Fp mlib_ImageMoment2_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageMoment2_Fp(mlib_d64 *moment,
                                    const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageStdDev mlib_ImageStdDev
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageStdDev(mlib_d64 *sdev,
                                const mlib_image *img,
                                const mlib_d64 *mean);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageStdDev_Fp mlib_ImageStdDev_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageStdDev_Fp(mlib_d64 *sdev,
                                   const mlib_image *img,
                                   const mlib_d64 *mean);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageXProj mlib_ImageXProj
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageXProj(mlib_d64 *xproj,
                               const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageXProj_Fp mlib_ImageXProj_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageXProj_Fp(mlib_d64 *xproj,
                                  const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageYProj mlib_ImageYProj
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageYProj(mlib_d64 *yproj,
                               const mlib_image *img);


#if defined ( __MEDIALIB_OLD_NAMES )
#define __mlib_ImageYProj_Fp mlib_ImageYProj_Fp
#endif /* ! defined ( __MEDIALIB_OLD_NAMES ) */
mlib_status  __mlib_ImageYProj_Fp(mlib_d64 *yproj,
                                  const mlib_image *img);

#ifdef __cplusplus
}
#endif /* __cplusplus */
#endif /* __ORIG_MLIB_IMAGE_PROTO_H */
