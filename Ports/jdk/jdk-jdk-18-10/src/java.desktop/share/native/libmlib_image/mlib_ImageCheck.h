/*
 * Copyright (c) 1999, 2003, Oracle and/or its affiliates. All rights reserved.
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


#ifndef MLIB_IMAGECHECK_H
#define MLIB_IMAGECHECK_H

#include <stdlib.h>
#include <mlib_image.h>

#ifdef __cplusplus
extern "C" {
#endif

/***************************************************************/

#define MLIB_IMAGE_CHECK(image)                                 \
  if (image == NULL) return MLIB_NULLPOINTER

#define MLIB_IMAGE_SIZE_EQUAL(image1,image2)                       \
  if (mlib_ImageGetWidth(image1)  != mlib_ImageGetWidth(image2) || \
      mlib_ImageGetHeight(image1) != mlib_ImageGetHeight(image2))  \
    return MLIB_FAILURE

#define MLIB_IMAGE_TYPE_EQUAL(image1,image2)                    \
  if (mlib_ImageGetType(image1) != mlib_ImageGetType(image2))   \
    return MLIB_FAILURE

#define MLIB_IMAGE_CHAN_EQUAL(image1,image2)                          \
  if (mlib_ImageGetChannels(image1) != mlib_ImageGetChannels(image2)) \
    return MLIB_FAILURE

#define MLIB_IMAGE_FULL_EQUAL(image1,image2)                    \
  MLIB_IMAGE_SIZE_EQUAL(image1,image2);                         \
  MLIB_IMAGE_TYPE_EQUAL(image1,image2);                         \
  MLIB_IMAGE_CHAN_EQUAL(image1,image2)

#define MLIB_IMAGE_HAVE_TYPE(image, type)                       \
  if (mlib_ImageGetType(image) != type)                         \
    return MLIB_FAILURE

#define MLIB_IMAGE_HAVE_CHAN(image, channels)                   \
  if (mlib_ImageGetChannels(image) != channels)                 \
    return MLIB_FAILURE

#define MLIB_IMAGE_HAVE_3_OR_4_CHAN(image)                      \
  if (mlib_ImageGetChannels(image) != 3 &&                      \
      mlib_ImageGetChannels(image) != 4)                        \
    return MLIB_FAILURE

#define MLIB_IMAGE_CHAN_SRC1_OR_EQ(src, dst)                      \
  if (mlib_ImageGetChannels(src) != 1) {                          \
    if (mlib_ImageGetChannels(src) != mlib_ImageGetChannels(dst)) \
      return MLIB_FAILURE;                                        \
  }

#define MLIB_IMAGE_TYPE_DSTBIT_OR_EQ(src, dst)                  \
  if ((mlib_ImageGetType(src) != mlib_ImageGetType(dst)) &&     \
      (mlib_ImageGetType(dst) != MLIB_BIT)) {                   \
    return MLIB_FAILURE;                                        \
  }

#define MLIB_IMAGE_GET_ALL_PARAMS(image, type, nchan, width, height, stride, pdata) \
  type   = mlib_ImageGetType(image);                                                \
  nchan  = mlib_ImageGetChannels(image);                                            \
  width  = mlib_ImageGetWidth(image);                                               \
  height = mlib_ImageGetHeight(image);                                              \
  stride = mlib_ImageGetStride(image);                                              \
  pdata  = (void*)mlib_ImageGetData(image)

/***************************************************************/

#ifdef __cplusplus
}
#endif
#endif  /* MLIB_IMAGECHECK_H */
