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
 *      mlib_ImageLookUp - table lookup
 *
 * SYNOPSIS
 *      mlib_status mlib_ImageLookUp(mlib_image       *dst,
 *                                   const mlib_image *src,
 *                                   const void       **table)
 *
 * ARGUMENT
 *      dst      Pointer to destination image.
 *      src      Pointer to source image.
 *      table    Lookup table.
 *
 * DESCRIPTION
 *      The mlib_ImageLookUp function performs general table lookup on an
 *      image. The destination image is obtained by passing a source image
 *      through a lookup table.
 *
 *      The source image may be 1-, 2-, 3-, or 4-channeled of data types
 *      MLIB_BIT, MLIB_BYTE, MLIB_SHORT, MLIB_USHORT, or MLIB_INT. The lookup
 *      table may be 1-, 2-, 3-, or 4-channeled of data types MLIB_BYTE,
 *      MLIB_SHORT, MLIB_USHORT, MLIB_INT, MLIB_FLOAT, or MLIB_DOUBLE.
 *      The destination image must have the same
 *      number of channels as either source image or the lookup table,
 *      whichever is greater, and the same data type as the lookup table.
 *
 *      It is the user's responsibility to make sure that the lookup table
 *      supplied is suitable for the source image. Specifically, the table
 *      entries cover the entire range of source data. Otherwise, the result
 *      of this function is undefined.
 *
 *      The pixel values of the destination image are defined as the following:
 *
 *      If the source image is single-channeled and the destination image is
 *      multi-channeled, then the lookup table has the same number of channels
 *      as the destination image:
 *
 *          dst[x][y][c] = table[c][src[x][y][0]]
 *
 *      If the source image is multi-channeled and the destination image is
 *      multi-channeled, with the same number of channels as the source image,
 *      then the lookup table will have the same number of channels as
 *      the source image:
 *
 *          dst[x][y][c] = table[c][src[x][y][c]]
 */

#include "mlib_image.h"
#include "mlib_ImageCheck.h"
#include "mlib_ImageLookUp.h"
#include "mlib_c_ImageLookUp.h"

/***************************************************************/
JNIEXPORT
mlib_status mlib_ImageLookUp(mlib_image       *dst,
                             const mlib_image *src,
                             const void       **table)
{
  mlib_s32   slb, dlb, xsize, ysize, nchan, ichan, bitoff_src;
  mlib_type  stype, dtype;
  void       *sa, *da;

  MLIB_IMAGE_CHECK(src);
  MLIB_IMAGE_CHECK(dst);
  MLIB_IMAGE_SIZE_EQUAL(src, dst);
  MLIB_IMAGE_CHAN_SRC1_OR_EQ(src, dst);

  stype = mlib_ImageGetType(src);
  dtype = mlib_ImageGetType(dst);
  ichan = mlib_ImageGetChannels(src);
  nchan = mlib_ImageGetChannels(dst);
  xsize = mlib_ImageGetWidth(src);
  ysize = mlib_ImageGetHeight(src);
  slb   = mlib_ImageGetStride(src);
  dlb   = mlib_ImageGetStride(dst);
  sa    = mlib_ImageGetData(src);
  da    = mlib_ImageGetData(dst);

  if (ichan == nchan) {
    if (dtype == MLIB_BYTE) {
      if (stype == MLIB_BYTE) {

        mlib_c_ImageLookUp_U8_U8(sa, slb,
                                 da, dlb,
                                 xsize, ysize, nchan,
                                 (const mlib_u8 **) table);

        return MLIB_SUCCESS;

      } else if (stype == MLIB_SHORT) {

        mlib_c_ImageLookUp_S16_U8(sa, slb/2,
                                  da, dlb,
                                  xsize, ysize, nchan,
                                  (const mlib_u8 **) table);
        return MLIB_SUCCESS;

      } else if (stype == MLIB_USHORT) {

        mlib_c_ImageLookUp_U16_U8(sa, slb/2,
                                  da, dlb,
                                  xsize, ysize, nchan,
                                  (const mlib_u8 **) table);
        return MLIB_SUCCESS;

      } else if (stype == MLIB_INT) {

        mlib_c_ImageLookUp_S32_U8(sa, slb/4,
                                  da, dlb,
                                  xsize, ysize, nchan,
                                  (const mlib_u8 **) table);
        return MLIB_SUCCESS;

      } else if (stype == MLIB_BIT) {

        if (nchan != 1) return MLIB_FAILURE;

        bitoff_src = mlib_ImageGetBitOffset(src);   /* bits to first byte */
        return mlib_ImageLookUp_Bit_U8_1(sa, slb,
                                da, dlb,
                                xsize, ysize, nchan, bitoff_src,
                                (const mlib_u8 **) table);
      }

    } else if (dtype == MLIB_SHORT) {

      if (stype == MLIB_BYTE) {

        mlib_c_ImageLookUp_U8_S16(sa, slb,
                                  da, dlb/2,
                                  xsize, ysize, nchan,
                                  (const mlib_s16 **) table);

        return MLIB_SUCCESS;

      } else if (stype == MLIB_SHORT) {

        mlib_c_ImageLookUp_S16_S16(sa, slb/2,
                                   da, dlb/2,
                                   xsize, ysize, nchan,
                                   (const mlib_s16 **) table);
        return MLIB_SUCCESS;

      } else if (stype == MLIB_USHORT) {

        mlib_c_ImageLookUp_U16_S16(sa, slb/2,
                                   da, dlb/2,
                                   xsize, ysize, nchan,
                                   (const mlib_s16 **) table);
        return MLIB_SUCCESS;

      } else if (stype == MLIB_INT) {

        mlib_c_ImageLookUp_S32_S16(sa, slb/4,
                                   da, dlb/2,
                                   xsize, ysize, nchan,
                                   (const mlib_s16 **) table);
        return MLIB_SUCCESS;
      }

    } else if (dtype == MLIB_USHORT) {

      if (stype == MLIB_BYTE) {

        mlib_c_ImageLookUp_U8_U16(sa, slb,
                                  da, dlb/2,
                                  xsize, ysize, nchan,
                                  (const mlib_s16 **) table);

        return MLIB_SUCCESS;

      } else if (stype == MLIB_SHORT) {

        mlib_c_ImageLookUp_S16_U16(sa, slb/2,
                                   da, dlb/2,
                                   xsize, ysize, nchan,
                                   (const mlib_s16 **) table);
        return MLIB_SUCCESS;

      } else if (stype == MLIB_USHORT) {

        mlib_c_ImageLookUp_U16_U16(sa, slb/2,
                                   da, dlb/2,
                                   xsize, ysize, nchan,
                                   (const mlib_s16 **) table);
        return MLIB_SUCCESS;

      } else if (stype == MLIB_INT) {

        mlib_c_ImageLookUp_S32_U16(sa, slb/4,
                                   da, dlb/2,
                                   xsize, ysize, nchan,
                                   (const mlib_s16 **) table);
        return MLIB_SUCCESS;
      }

    } else if (dtype == MLIB_INT) {

      if (stype == MLIB_BYTE) {

        mlib_c_ImageLookUp_U8_S32(sa, slb,
                                  da, dlb/4,
                                  xsize, ysize, nchan,
                                  (const mlib_s32 **) table);

        return MLIB_SUCCESS;

      } else if (stype == MLIB_SHORT) {

        mlib_c_ImageLookUp_S16_S32(sa, slb/2,
                                   da, dlb/4,
                                   xsize, ysize, nchan,
                                   (const mlib_s32 **) table);
        return MLIB_SUCCESS;

      } else if (stype == MLIB_USHORT) {

        mlib_c_ImageLookUp_U16_S32(sa, slb/2,
                                   da, dlb/4,
                                   xsize, ysize, nchan,
                                   (const mlib_s32 **) table);
        return MLIB_SUCCESS;

      } else if (stype == MLIB_INT) {

        mlib_c_ImageLookUp_S32_S32(sa, slb/4,
                                   da, dlb/4,
                                   xsize, ysize, nchan,
                                   (const mlib_s32 **) table);
        return MLIB_SUCCESS;
      }

    } else if (dtype == MLIB_FLOAT) {

      if (stype == MLIB_BYTE) {

        mlib_c_ImageLookUp_U8_S32(sa, slb,
                                  da, dlb/4,
                                  xsize, ysize, nchan,
                                  (const mlib_s32 **) table);

        return MLIB_SUCCESS;

      } else if (stype == MLIB_SHORT) {

        mlib_c_ImageLookUp_S16_S32(sa, slb/2,
                                   da, dlb/4,
                                   xsize, ysize, nchan,
                                   (const mlib_s32 **) table);
        return MLIB_SUCCESS;

      } else if (stype == MLIB_USHORT) {

        mlib_c_ImageLookUp_U16_S32(sa, slb/2,
                                   da, dlb/4,
                                   xsize, ysize, nchan,
                                   (const mlib_s32 **) table);
        return MLIB_SUCCESS;

      } else if (stype == MLIB_INT) {

        mlib_c_ImageLookUp_S32_S32(sa, slb/4,
                                   da, dlb/4,
                                   xsize, ysize, nchan,
                                   (const mlib_s32 **) table);
        return MLIB_SUCCESS;
      }

    } else if (dtype == MLIB_DOUBLE) {

      if (stype == MLIB_BYTE) {

        mlib_ImageLookUp_U8_D64(sa, slb,
                                da, dlb/8,
                                xsize, ysize, nchan,
                                (const mlib_d64 **) table);

        return MLIB_SUCCESS;

      } else if (stype == MLIB_SHORT) {

        mlib_ImageLookUp_S16_D64(sa, slb/2,
                                 da, dlb/8,
                                 xsize, ysize, nchan,
                                 (const mlib_d64 **) table);
        return MLIB_SUCCESS;

      } else if (stype == MLIB_USHORT) {

        mlib_ImageLookUp_U16_D64(sa, slb/2,
                                 da, dlb/8,
                                 xsize, ysize, nchan,
                                 (const mlib_d64 **) table);
        return MLIB_SUCCESS;

      } else if (stype == MLIB_INT) {

        mlib_ImageLookUp_S32_D64(sa, slb/4,
                                 da, dlb/8,
                                 xsize, ysize, nchan,
                                 (const mlib_d64 **) table);
        return MLIB_SUCCESS;
      }
    }

  } else if (ichan == 1) {

    if (dtype == MLIB_BYTE) {

      if (stype == MLIB_BYTE) {

        mlib_c_ImageLookUpSI_U8_U8(sa, slb,
                                   da, dlb,
                                   xsize, ysize, nchan,
                                   (const mlib_u8 **) table);

        return MLIB_SUCCESS;

      } else if (stype == MLIB_SHORT) {

        mlib_c_ImageLookUpSI_S16_U8(sa, slb/2,
                                    da, dlb,
                                    xsize, ysize, nchan,
                                    (const mlib_u8 **) table);
        return MLIB_SUCCESS;

      } else if (stype == MLIB_USHORT) {

        mlib_c_ImageLookUpSI_U16_U8(sa, slb/2,
                                    da, dlb,
                                    xsize, ysize, nchan,
                                    (const mlib_u8 **) table);
        return MLIB_SUCCESS;

      } else if (stype == MLIB_INT) {

        mlib_c_ImageLookUpSI_S32_U8(sa, slb/4,
                                    da, dlb,
                                    xsize, ysize, nchan,
                                    (const mlib_u8 **) table);
        return MLIB_SUCCESS;

      } else if (stype == MLIB_BIT) {

        bitoff_src = mlib_ImageGetBitOffset(src);

        if (nchan == 2) {

        return mlib_ImageLookUp_Bit_U8_2(sa, slb,
                                da, dlb,
                                xsize, ysize, nchan, bitoff_src,
                                (const mlib_u8 **) table);
        } else  if (nchan == 3) {

        return mlib_ImageLookUp_Bit_U8_3(sa, slb,
                                da, dlb,
                                xsize, ysize, nchan, bitoff_src,
                                (const mlib_u8 **) table);

        } else /* (nchan == 4) */ {

        return mlib_ImageLookUp_Bit_U8_4(sa, slb,
                                da, dlb,
                                xsize, ysize, nchan, bitoff_src,
                                (const mlib_u8 **) table);
        }
      }

    } else if (dtype == MLIB_SHORT) {

      if (stype == MLIB_BYTE) {

        mlib_c_ImageLookUpSI_U8_S16(sa, slb,
                                    da, dlb/2,
                                    xsize, ysize, nchan,
                                    (const mlib_s16 **) table);

        return MLIB_SUCCESS;

      } else if (stype == MLIB_SHORT) {

        mlib_c_ImageLookUpSI_S16_S16(sa, slb/2,
                                     da, dlb/2,
                                     xsize, ysize, nchan,
                                     (const mlib_s16 **) table);
        return MLIB_SUCCESS;

      } else if (stype == MLIB_USHORT) {

        mlib_c_ImageLookUpSI_U16_S16(sa, slb/2,
                                     da, dlb/2,
                                     xsize, ysize, nchan,
                                     (const mlib_s16 **) table);
        return MLIB_SUCCESS;

      } else if (stype == MLIB_INT) {

        mlib_c_ImageLookUpSI_S32_S16(sa, slb/4,
                                     da, dlb/2,
                                     xsize, ysize, nchan,
                                     (const mlib_s16 **) table);
        return MLIB_SUCCESS;
      }

    } else if (dtype == MLIB_USHORT) {

      if (stype == MLIB_BYTE) {

        mlib_c_ImageLookUpSI_U8_U16(sa, slb,
                                    da, dlb/2,
                                    xsize, ysize, nchan,
                                    (const mlib_s16 **) table);

        return MLIB_SUCCESS;

      } else if (stype == MLIB_SHORT) {

        mlib_c_ImageLookUpSI_S16_U16(sa, slb/2,
                                     da, dlb/2,
                                     xsize, ysize, nchan,
                                     (const mlib_u16 **) table);
        return MLIB_SUCCESS;

      } else if (stype == MLIB_USHORT) {

        mlib_c_ImageLookUpSI_U16_U16(sa, slb/2,
                                     da, dlb/2,
                                     xsize, ysize, nchan,
                                     (const mlib_u16 **) table);
        return MLIB_SUCCESS;

      } else if (stype == MLIB_INT) {

        mlib_c_ImageLookUpSI_S32_U16(sa, slb/4,
                                     da, dlb/2,
                                     xsize, ysize, nchan,
                                     (const mlib_u16 **) table);
        return MLIB_SUCCESS;
      }

    } else if (dtype == MLIB_INT) {

      if (stype == MLIB_BYTE) {

        mlib_c_ImageLookUpSI_U8_S32(sa, slb,
                                    da, dlb/4,
                                    xsize, ysize, nchan,
                                    (const mlib_s32 **) table);

        return MLIB_SUCCESS;

      } else if (stype == MLIB_SHORT) {

        mlib_c_ImageLookUpSI_S16_S32(sa, slb/2,
                                     da, dlb/4,
                                     xsize, ysize, nchan,
                                     (const mlib_s32 **) table);
        return MLIB_SUCCESS;

      } else if (stype == MLIB_USHORT) {

        mlib_c_ImageLookUpSI_U16_S32(sa, slb/2,
                                     da, dlb/4,
                                     xsize, ysize, nchan,
                                     (const mlib_s32 **) table);
        return MLIB_SUCCESS;

      } else if (stype == MLIB_INT) {

        mlib_c_ImageLookUpSI_S32_S32(sa, slb/4,
                                     da, dlb/4,
                                     xsize, ysize, nchan,
                                     (const mlib_s32 **) table);
        return MLIB_SUCCESS;
      }

    } else if (dtype == MLIB_FLOAT) {

      if (stype == MLIB_BYTE) {

        mlib_c_ImageLookUpSI_U8_S32(sa, slb,
                                    da, dlb/4,
                                    xsize, ysize, nchan,
                                    (const mlib_s32 **) table);

        return MLIB_SUCCESS;

      } else if (stype == MLIB_SHORT) {

        mlib_c_ImageLookUpSI_S16_S32(sa, slb/2,
                                     da, dlb/4,
                                     xsize, ysize, nchan,
                                     (const mlib_s32 **) table);
        return MLIB_SUCCESS;

      } else if (stype == MLIB_USHORT) {

        mlib_c_ImageLookUpSI_U16_S32(sa, slb/2,
                                     da, dlb/4,
                                     xsize, ysize, nchan,
                                     (const mlib_s32 **) table);
        return MLIB_SUCCESS;

      } else if (stype == MLIB_INT) {

        mlib_c_ImageLookUpSI_S32_S32(sa, slb/4,
                                     da, dlb/4,
                                     xsize, ysize, nchan,
                                     (const mlib_s32 **) table);
        return MLIB_SUCCESS;
      }

    } else if (dtype == MLIB_DOUBLE) {

      if (stype == MLIB_BYTE) {

        mlib_ImageLookUpSI_U8_D64(sa, slb,
                                  da, dlb/8,
                                  xsize, ysize, nchan,
                                  (const mlib_d64 **) table);

        return MLIB_SUCCESS;

      } else if (stype == MLIB_SHORT) {

        mlib_ImageLookUpSI_S16_D64(sa, slb/2,
                                   da, dlb/8,
                                   xsize, ysize, nchan,
                                   (const mlib_d64 **) table);
        return MLIB_SUCCESS;

      } else if (stype == MLIB_USHORT) {

        mlib_ImageLookUpSI_U16_D64(sa, slb/2,
                                   da, dlb/8,
                                   xsize, ysize, nchan,
                                   (const mlib_d64 **) table);
        return MLIB_SUCCESS;

      } else if (stype == MLIB_INT) {

        mlib_ImageLookUpSI_S32_D64(sa, slb/4,
                                   da, dlb/8,
                                   xsize, ysize, nchan,
                                   (const mlib_d64 **) table);
        return MLIB_SUCCESS;
      }
    }
  }

  return MLIB_FAILURE;
}

/***************************************************************/
