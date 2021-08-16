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


/*
 * FUNCTION
 *      mlib_ImageCreateStruct   - create image data structure
 *      mlib_ImageCreate         - create image data structure and allocate
 *                                 memory for image data
 *      mlib_ImageDelete         - delete image
 *      mlib_ImageCreateSubimage - create sub-image
 *
 *      mlib_ImageCreateRowTable - create row starts pointer table
 *      mlib_ImageDeleteRowTable - delete row starts pointer table
 *
 *      mlib_ImageSetPaddings    - set paddings for clipping box borders
 *
 *      mlib_ImageSetFormat      - set image format
 *
 * SYNOPSIS
 *        mlib_image *mlib_ImageCreateStruct(mlib_type  type,
 *                                           mlib_s32   channels,
 *                                           mlib_s32   width,
 *                                           mlib_s32   height,
 *                                           mlib_s32   stride,
 *                                           const void *data)
 *
 *        mlib_image *mlib_ImageCreate(mlib_type type,
 *                                     mlib_s32  channels,
 *                                     mlib_s32  width,
 *                                     mlib_s32  height)
 *
 *        void mlib_ImageDelete(mlib_image *img)
 *
 *        mlib_image *mlib_ImageCreateSubimage(mlib_image *img,
 *                                             mlib_s32   x,
 *                                             mlib_s32   y,
 *                                             mlib_s32   w,
 *                                             mlib_s32   h)
 *
 *        void *mlib_ImageCreateRowTable(mlib_image *img)
 *
 *        void mlib_ImageDeleteRowTable(mlib_image *img)
 *
 *        mlib_status mlib_ImageSetPaddings(mlib_image *img,
 *                                          mlib_u8    left,
 *                                          mlib_u8    top,
 *                                          mlib_u8    right,
 *                                          mlib_u8    bottom)
 *
 *        mlib_status mlib_ImageSetFormat(mlib_image  *img,
 *                                        mlib_format format)
 * ARGUMENTS
 *      img       pointer to image data structure
 *      type      image data type, one of MLIB_BIT, MLIB_BYTE, MLIB_SHORT,
 *                MLIB_USHORT, MLIB_INT, MLIB_FLOAT or MLIB_DOUBLE
 *      channels  number of image channels
 *      width     image width in pixels
 *      height    image height in pixels
 *      stride    linebytes( bytes to next row) of the image
 *      data      pointer to image data allocated by user
 *      x         x coordinate of the left border in the source image
 *      y         y coordinate of the top border in the source image
 *      w         width of the sub-image
 *      h         height of the sub-image
 *      left      clipping box left padding
 *      top       clipping box top padding
 *      right     clipping box right padding
 *      bottom    clipping box bottom padding
 *      format    image format
 *
 * DESCRIPTION
 *      mlib_ImageCreateStruct() creates a mediaLib image data structure
 *      using parameter supplied by user.
 *
 *      mlib_ImageCreate() creates a mediaLib image data structure and
 *      allocates memory space for image data.
 *
 *      mlib_ImageDelete() deletes the mediaLib image data structure
 *      and frees the memory space of the image data if it is allocated
 *      through mlib_ImageCreate().
 *
 *      mlib_ImageCreateSubimage() creates a mediaLib image structure
 *      for a sub-image based on a source image.
 *
 *      mlib_ImageCreateRowTable() creates row starts pointer table and
 *      puts it into mlib_image->state field.
 *
 *      mlib_ImageDeleteRowTable() deletes row starts pointer table from
 *      image and puts NULL into mlib_image->state field.
 *
 *      mlib_ImageSetPaddings() sets new values for the clipping box paddings
 *
 *      mlib_ImageSetFormat() sets new value for the image format
 */

#include <stdlib.h>
#include "mlib_image.h"
#include "mlib_ImageRowTable.h"
#include "mlib_ImageCreate.h"
#include "safe_math.h"

/***************************************************************/
mlib_image* mlib_ImageSet(mlib_image *image,
                          mlib_type  type,
                          mlib_s32   channels,
                          mlib_s32   width,
                          mlib_s32   height,
                          mlib_s32   stride,
                          const void *data)
{
  mlib_s32        wb;                /* width in bytes */
  mlib_s32        mask;              /* mask for check of stride */

  if (image == NULL) return NULL;

/* for some ugly functions calling with incorrect parameters */
  image -> type     = type;
  image -> channels = channels;
  image -> width    = width;
  image -> height   = height;
  image -> stride   = stride;
  image -> data     = (void *)data;
  image -> state    = NULL;
  image -> format   = MLIB_FORMAT_UNKNOWN;

  image -> paddings[0] = 0;
  image -> paddings[1] = 0;
  image -> paddings[2] = 0;
  image -> paddings[3] = 0;

  image -> bitoffset = 0;

  if (width <= 0 || height <= 0 || channels < 1 || channels > 4) {
    return NULL;
  }

/* Check if stride == width
   * If it is then image can be treated as a 1-D vector
 */

  if (!SAFE_TO_MULT(width, channels)) {
    return NULL;
  }

  wb = width * channels;

  switch (type) {
    case MLIB_DOUBLE:
      if (!SAFE_TO_MULT(wb, 8)) {
        return NULL;
      }
      wb *= 8;
      mask = 7;
      break;
    case MLIB_FLOAT:
    case MLIB_INT:
      if (!SAFE_TO_MULT(wb, 4)) {
        return NULL;
      }
      wb *= 4;
      mask = 3;
      break;
    case MLIB_USHORT:
    case MLIB_SHORT:
      if (!SAFE_TO_MULT(wb, 2)) {
        return NULL;
      }
      wb *= 2;
      mask = 1;
      break;
    case MLIB_BYTE:
      // wb is ready
      mask = 0;
      break;
    case MLIB_BIT:
      if (!SAFE_TO_ADD(7, wb)) {
        return NULL;
      }
      wb = (wb + 7) / 8;
      mask = 0;
      break;
    default:
      return NULL;
  }

  if (stride & mask) {
    return NULL;
  }

  image -> flags    = ((width & 0xf) << 8);          /* set width field */
  image -> flags   |= ((stride & 0xf) << 16);        /* set stride field */
  image -> flags   |= ((height & 0xf) << 12);        /* set height field */
  image -> flags   |= (mlib_addr)data & 0xff;
  image -> flags   |= MLIB_IMAGE_USERALLOCATED;      /* user allocated data */

  if ((stride != wb) ||
      ((type == MLIB_BIT) && (stride * 8 != width * channels))) {
    image -> flags |= MLIB_IMAGE_ONEDVECTOR;
  }

  image -> flags &= MLIB_IMAGE_ATTRIBUTESET;

  return image;
}

/***************************************************************/
JNIEXPORT
mlib_image* mlib_ImageCreateStruct(mlib_type  type,
                                   mlib_s32   channels,
                                   mlib_s32   width,
                                   mlib_s32   height,
                                   mlib_s32   stride,
                                   const void *data)
{
  mlib_image *image;
  if (stride <= 0) {
    return NULL;
  }

  image = (mlib_image *)mlib_malloc(sizeof(mlib_image));
  if (image == NULL) {
    return NULL;
  }

  if (mlib_ImageSet(image, type, channels, width, height, stride, data) == NULL) {
    mlib_free(image);
    image = NULL;
  }

  return image;
}

/***************************************************************/
JNIEXPORT
mlib_image* mlib_ImageCreate(mlib_type type,
                             mlib_s32  channels,
                             mlib_s32  width,
                             mlib_s32  height)
{
  mlib_image *image;
  mlib_s32        wb;                /* width in bytes */
  void       *data;

/* sanity check */
  if (width <= 0 || height <= 0 || channels < 1 || channels > 4) {
    return NULL;
  };

  if (!SAFE_TO_MULT(width, channels)) {
    return NULL;
  }

  wb = width * channels;

  switch (type) {
    case MLIB_DOUBLE:
      if (!SAFE_TO_MULT(wb, 8)) {
        return NULL;
      }
      wb *= 8;
      break;
    case MLIB_FLOAT:
    case MLIB_INT:
      if (!SAFE_TO_MULT(wb, 4)) {
        return NULL;
      }
      wb *= 4;
      break;
    case MLIB_USHORT:
    case MLIB_SHORT:
      if (!SAFE_TO_MULT(wb, 2)) {
        return NULL;
      }
      wb *= 2;
      break;
    case MLIB_BYTE:
      // wb is ready
      break;
    case MLIB_BIT:
      if (!SAFE_TO_ADD(7, wb)) {
        return NULL;
      }
      wb = (wb + 7) / 8;
      break;
    default:
      return NULL;
  }

  if (!SAFE_TO_MULT(wb, height)) {
      return NULL;
  }

  data = mlib_malloc(wb * height);
  if (data == NULL) {
    return NULL;
  }

  image = (mlib_image *)mlib_malloc(sizeof(mlib_image));
  if (image == NULL) {
    mlib_free(data);
    return NULL;
  };

  image -> type     = type;
  image -> channels = channels;
  image -> width    = width;
  image -> height   = height;
  image -> stride   = wb;
  image -> data     = data;
  image -> flags    = ((width & 0xf) << 8);        /* set width field */
  image -> flags   |= ((height & 0xf) << 12);      /* set height field */
  image -> flags   |= ((wb & 0xf) << 16);          /* set stride field */
  image -> flags   |= (mlib_addr)data & 0xff;
  image -> format   = MLIB_FORMAT_UNKNOWN;

  image -> paddings[0] = 0;
  image -> paddings[1] = 0;
  image -> paddings[2] = 0;
  image -> paddings[3] = 0;

  image -> bitoffset = 0;

  if ((type == MLIB_BIT) && (wb * 8 != width * channels)) {
    image -> flags |= MLIB_IMAGE_ONEDVECTOR;       /* not 1-d vector */
  }

  image -> flags &= MLIB_IMAGE_ATTRIBUTESET;
  image -> state  = NULL;

  return image;
}

/***************************************************************/
JNIEXPORT
void mlib_ImageDelete(mlib_image *img)
{
  if (img == NULL) return;
  if ((img -> flags & MLIB_IMAGE_USERALLOCATED) == 0) {
    mlib_free(img -> data);
  }

  mlib_ImageDeleteRowTable(img);
  mlib_free(img);
}

/***************************************************************/
mlib_image *mlib_ImageCreateSubimage(mlib_image *img,
                                     mlib_s32   x,
                                     mlib_s32   y,
                                     mlib_s32   w,
                                     mlib_s32   h)
{
  mlib_image     *subimage;
  mlib_type      type;
  mlib_s32       channels;
  mlib_s32       width;                 /* for parent image */
  mlib_s32       height;                /* for parent image */
  mlib_s32       stride;
  mlib_s32       bitoffset = 0;
  void           *data;

/* sanity check */
  if (w <= 0 || h <= 0 || img == NULL) return NULL;

  type     = img -> type;
  channels = img -> channels;
  width    = img -> width;
  height   = img -> height;
  stride   = img -> stride;

/* clip the sub-image with respect to the parent image */
  if (((x + w) <= 0) || ((y + h) <= 0) ||
      (x >= width) || (y >= height)) {
    return NULL;
  }
  else {
    if (x < 0) {
      w += x;                                        /* x is negative */
      x = 0;
    }

    if (y < 0) {
      h += y;                                        /* y is negative */
      y = 0;
    }

    if ((x + w) > width) {
      w = width - x;
    }

    if ((y + h) > height) {
      h = height - y;
    }
  }

/* compute sub-image origin */
  data = (mlib_u8 *)(img -> data) + y * stride;

  switch (type) {
    case MLIB_DOUBLE:
      data = (mlib_u8 *)data + x * channels * 8;
      break;
    case MLIB_FLOAT:
    case MLIB_INT:
      data = (mlib_u8 *)data + x * channels * 4;
      break;
    case MLIB_USHORT:
    case MLIB_SHORT:
      data = (mlib_u8 *)data + x * channels * 2;
      break;
    case MLIB_BYTE:
      data = (mlib_u8 *)data + x * channels;
      break;
    case MLIB_BIT:
      bitoffset = img -> bitoffset;
      data = (mlib_u8 *)data + (x * channels + bitoffset) / 8;
      bitoffset = (x * channels + bitoffset) & 7;
      break;
    default:
      return NULL;
  }

  subimage = mlib_ImageCreateStruct(type,
                                    channels,
                                    w,
                                    h,
                                    stride,
                                    data);

  if (subimage != NULL && type == MLIB_BIT)
    subimage -> bitoffset = bitoffset;

  return subimage;
}

/***************************************************************/
mlib_image *mlib_ImageSetSubimage(mlib_image       *dst,
                                  const mlib_image *src,
                                  mlib_s32         x,
                                  mlib_s32         y,
                                  mlib_s32         w,
                                  mlib_s32         h)
{
  mlib_type  type     = src -> type;
  mlib_s32   channels = src -> channels;
  mlib_s32   stride   = src -> stride;
  mlib_u8    *data    = src -> data;
  mlib_s32   bitoffset = 0;

  data += y * stride;

  switch (type) {
    case MLIB_DOUBLE:
      data += channels * x * 8;
      break;
    case MLIB_FLOAT:
    case MLIB_INT:
      data += channels * x * 4;
      break;
    case MLIB_USHORT:
    case MLIB_SHORT:
      data += channels * x * 2;
      break;
    case MLIB_BYTE:
      data += channels * x;
      break;
    case MLIB_BIT:
      bitoffset = src -> bitoffset + channels * x;
      data += (bitoffset >= 0) ? bitoffset/8 : (bitoffset - 7)/8; /* with rounding toward -Inf */
      bitoffset &= 7;
      break;
    default:
      return NULL;
  }

  if (h > 0) {
    dst = mlib_ImageSet(dst, type, channels, w, h, stride, data);
  } else {
    h = - h;
    dst = mlib_ImageSet(dst, type, channels, w, h, - stride, data + (h - 1)*stride);
  }

  if (dst != NULL && type == MLIB_BIT) {
    dst -> bitoffset = bitoffset;
  }

  return dst;
}

/***************************************************************/
void *mlib_ImageCreateRowTable(mlib_image *img)
{
  mlib_u8  **rtable, *tline;
  mlib_s32 i, im_height, im_stride;

  if (img == NULL) return NULL;
  if (img -> state)  return img -> state;

  im_height = mlib_ImageGetHeight(img);
  im_stride = mlib_ImageGetStride(img);
  tline     = mlib_ImageGetData(img);
  if (tline == NULL) return NULL;
  rtable    = mlib_malloc((3 + im_height)*sizeof(mlib_u8 *));
  if (rtable == NULL) return NULL;

  rtable[0] = 0;
  rtable[1] = (mlib_u8*)((void **)rtable + 1);
  rtable[2 + im_height] = (mlib_u8*)((void **)rtable + 1);
  for (i = 0; i < im_height; i++) {
    rtable[i+2] = tline;
    tline    += im_stride;
  }

  img -> state = ((void **)rtable + 2);
  return img -> state;
}

/***************************************************************/
void mlib_ImageDeleteRowTable(mlib_image *img)
{
  void **state;

  if (img == NULL) return;

  state = img -> state;
  if (!state) return;

  mlib_free(state - 2);
  img -> state = 0;
}

/***************************************************************/
mlib_status mlib_ImageSetPaddings(mlib_image *img,
                                  mlib_u8    left,
                                  mlib_u8    top,
                                  mlib_u8    right,
                                  mlib_u8    bottom)
{
  if (img == NULL) return MLIB_FAILURE;

  if ((left + right) >= img -> width ||
      (top + bottom) >= img -> height) return MLIB_OUTOFRANGE;

  img -> paddings[0] = left;
  img -> paddings[1] = top;
  img -> paddings[2] = right;
  img -> paddings[3] = bottom;

  return MLIB_SUCCESS;
}

/***************************************************************/
mlib_status mlib_ImageSetFormat(mlib_image  *img,
                                mlib_format format)
{
  if (img == NULL) return MLIB_FAILURE;

  img -> format = format;

  return MLIB_SUCCESS;
}

/***************************************************************/
