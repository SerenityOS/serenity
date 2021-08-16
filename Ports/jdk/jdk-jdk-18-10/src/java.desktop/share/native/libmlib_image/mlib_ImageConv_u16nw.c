/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *   Internal functions for mlib_ImageConv* on U8/S16/U16 types and
 *   MLIB_EDGE_DST_NO_WRITE mask
 */

#include "mlib_image.h"
#include "mlib_c_ImageConv.h"

/*
  This define switches between functions of different data types
*/
#define IMG_TYPE 3

/***************************************************************/
#if IMG_TYPE == 1

#define DTYPE             mlib_u8
#define CONV_FUNC(KERN)   mlib_c_conv##KERN##nw_u8
#define CONV_FUNC_I(KERN) mlib_i_conv##KERN##nw_u8
#define DSCALE            (1 << 24)
#define FROM_S32(x)       (((x) >> 24) ^ 128)
#define S64TOS32(x)       (x)
#define SAT_OFF           -(1u << 31)

#elif IMG_TYPE == 2

#define DTYPE             mlib_s16
#define CONV_FUNC(KERN)   mlib_conv##KERN##nw_s16
#define CONV_FUNC_I(KERN) mlib_i_conv##KERN##nw_s16
#define DSCALE            65536.0
#define FROM_S32(x)       ((x) >> 16)
#define S64TOS32(x)       ((x) & 0xffffffff)
#define SAT_OFF

#elif IMG_TYPE == 3

#define DTYPE             mlib_u16
#define CONV_FUNC(KERN)   mlib_conv##KERN##nw_u16
#define CONV_FUNC_I(KERN) mlib_i_conv##KERN##nw_u16
#define DSCALE            65536.0
#define FROM_S32(x)       (((x) >> 16) ^ 0x8000)
#define S64TOS32(x)       (x)
#define SAT_OFF           -(1u << 31)

#endif /* IMG_TYPE == 1 */

/***************************************************************/
#define BUFF_SIZE   1600

#define CACHE_SIZE  (64*1024)

/***************************************************************/
#define FTYPE mlib_d64

#ifndef MLIB_USE_FTOI_CLAMPING

#define CLAMP_S32(x)                                            \
  (((x) <= MLIB_S32_MIN) ? MLIB_S32_MIN : (((x) >= MLIB_S32_MAX) ? MLIB_S32_MAX : (mlib_s32)(x)))

#else

#define CLAMP_S32(x) ((mlib_s32)(x))

#endif /* MLIB_USE_FTOI_CLAMPING */

/***************************************************************/
#define D2I(x) CLAMP_S32((x) SAT_OFF)

/***************************************************************/
#ifdef _LITTLE_ENDIAN

#define STORE2(res0, res1)                                      \
  dp[0    ] = res1;                                             \
  dp[chan1] = res0

#else

#define STORE2(res0, res1)                                      \
  dp[0    ] = res0;                                             \
  dp[chan1] = res1

#endif /* _LITTLE_ENDIAN */

/***************************************************************/
#ifdef _NO_LONGLONG

#define LOAD_BUFF(buff)                                         \
  buff[i    ] = sp[0];                                          \
  buff[i + 1] = sp[chan1]

#else /* _NO_LONGLONG */

#ifdef _LITTLE_ENDIAN

#define LOAD_BUFF(buff)                                         \
  *(mlib_s64*)(buff + i) = (((mlib_s64)sp[chan1]) << 32) | S64TOS32((mlib_s64)sp[0])

#else /* _LITTLE_ENDIAN */

#define LOAD_BUFF(buff)                                         \
  *(mlib_s64*)(buff + i) = (((mlib_s64)sp[0]) << 32) | S64TOS32((mlib_s64)sp[chan1])

#endif /* _LITTLE_ENDIAN */
#endif /* _NO_LONGLONG */

/***************************************************************/
typedef union {
  mlib_d64 d64;
  struct {
    mlib_s32 i0;
    mlib_s32 i1;
  } i32s;
  struct {
    mlib_s32 f0;
    mlib_s32 f1;
  } f32s;
} d64_2x32;

/***************************************************************/
#define DEF_VARS(type)                                          \
  type     *adr_src, *sl, *sp = NULL;                           \
  type     *adr_dst, *dl, *dp = NULL;                           \
  FTYPE    *pbuff = buff;                                       \
  mlib_s32 wid, hgt, sll, dll;                                  \
  mlib_s32 nchannel, chan1;                                     \
  mlib_s32 i, j, c

/***************************************************************/
#define GET_SRC_DST_PARAMETERS(type)                            \
  hgt = mlib_ImageGetHeight(src);                               \
  wid = mlib_ImageGetWidth(src);                                \
  nchannel = mlib_ImageGetChannels(src);                        \
  sll = mlib_ImageGetStride(src) / sizeof(type);                \
  dll = mlib_ImageGetStride(dst) / sizeof(type);                \
  adr_src = (type *)mlib_ImageGetData(src);                     \
  adr_dst = (type *)mlib_ImageGetData(dst)

/***************************************************************/
#if IMG_TYPE == 1

/* Test for the presence of any "1" bit in bits
   8 to 31 of val. If present, then val is either
   negative or >255. If over/underflows of 8 bits
   are uncommon, then this technique can be a win,
   since only a single test, rather than two, is
   necessary to determine if clamping is needed.
   On the other hand, if over/underflows are common,
   it adds an extra test.
*/
#define CLAMP_STORE(dst, val)                                   \
  if (val & 0xffffff00) {                                       \
    if (val < MLIB_U8_MIN)                                      \
      dst = MLIB_U8_MIN;                                        \
    else                                                        \
      dst = MLIB_U8_MAX;                                        \
  } else {                                                      \
    dst = (mlib_u8)val;                                         \
  }

#elif IMG_TYPE == 2

#define CLAMP_STORE(dst, val)                                   \
  if (val >= MLIB_S16_MAX)                                      \
    dst = MLIB_S16_MAX;                                         \
  else if (val <= MLIB_S16_MIN)                                 \
    dst = MLIB_S16_MIN;                                         \
  else                                                          \
    dst = (mlib_s16)val

#elif IMG_TYPE == 3

#define CLAMP_STORE(dst, val)                                   \
  if (val >= MLIB_U16_MAX)                                      \
    dst = MLIB_U16_MAX;                                         \
  else if (val <= MLIB_U16_MIN)                                 \
    dst = MLIB_U16_MIN;                                         \
  else                                                          \
    dst = (mlib_u16)val

#endif /* IMG_TYPE == 1 */

/***************************************************************/
#define MAX_KER   7
#define MAX_N    15

static mlib_status mlib_ImageConv1xN(mlib_image       *dst,
                                     const mlib_image *src,
                                     const mlib_d64   *k,
                                     mlib_s32         n,
                                     mlib_s32         dn,
                                     mlib_s32         cmask)
{
  FTYPE    buff[BUFF_SIZE];
  mlib_s32 off, kh;
  mlib_s32 d0, d1;
  const FTYPE    *pk;
  FTYPE    k0, k1, k2, k3;
  FTYPE    p0, p1, p2, p3, p4;
  DEF_VARS(DTYPE);
  DTYPE    *sl_c, *dl_c, *sl0;
  mlib_s32 l, hsize, max_hsize;
  GET_SRC_DST_PARAMETERS(DTYPE);

  hgt -= (n - 1);
  adr_dst += dn*dll;

  max_hsize = (CACHE_SIZE/sizeof(DTYPE))/sll;

  if (!max_hsize) max_hsize = 1;

  if (max_hsize > BUFF_SIZE) {
    pbuff = mlib_malloc(sizeof(FTYPE)*max_hsize);
  }

  chan1 = nchannel;

  sl_c = adr_src;
  dl_c = adr_dst;

  for (l = 0; l < hgt; l += hsize) {
    hsize = hgt - l;

    if (hsize > max_hsize) hsize = max_hsize;

    for (c = 0; c < nchannel; c++) {
      if (!(cmask & (1 << (chan1 - 1 - c)))) continue;

      sl = sl_c + c;
      dl = dl_c + c;

      for (j = 0; j < hsize; j++) pbuff[j] = 0.0;

      for (i = 0; i < wid; i++) {
        sl0 = sl;

        for (off = 0; off < (n - 4); off += 4) {
          pk = k + off;
          sp = sl0;

          k0 = pk[0]; k1 = pk[1]; k2 = pk[2]; k3 = pk[3];
          p2 = sp[0]; p3 = sp[sll]; p4 = sp[2*sll];
          sp += 3*sll;

          for (j = 0; j < hsize; j += 2) {
            p0 = p2; p1 = p3; p2 = p4;
            p3 = sp[0];
            p4 = sp[sll];

            pbuff[j    ] += p0*k0 + p1*k1 + p2*k2 + p3*k3;
            pbuff[j + 1] += p1*k0 + p2*k1 + p3*k2 + p4*k3;

            sp += 2*sll;
          }

          sl0 += 4*sll;
        }

        pk = k + off;
        sp = sl0;

        k0 = pk[0]; k1 = pk[1]; k2 = pk[2]; k3 = pk[3];
        p2 = sp[0]; p3 = sp[sll]; p4 = sp[2*sll];

        dp = dl;
        kh = n - off;

        if (kh == 4) {
          sp += 3*sll;

          for (j = 0; j <= (hsize - 2); j += 2) {
            p0 = p2; p1 = p3; p2 = p4;
            p3 = sp[0];
            p4 = sp[sll];

            d0 = D2I(p0*k0 + p1*k1 + p2*k2 + p3*k3 + pbuff[j]);
            d1 = D2I(p1*k0 + p2*k1 + p3*k2 + p4*k3 + pbuff[j + 1]);

            dp[0  ] = FROM_S32(d0);
            dp[dll] = FROM_S32(d1);

            pbuff[j] = 0;
            pbuff[j + 1] = 0;

            sp += 2*sll;
            dp += 2*dll;
          }

          if (j < hsize) {
            p0 = p2; p1 = p3; p2 = p4;
            p3 = sp[0];

            d0 = D2I(p0*k0 + p1*k1 + p2*k2 + p3*k3 + pbuff[j]);

            pbuff[j] = 0;

            dp[0] = FROM_S32(d0);
          }

        } else if (kh == 3) {
          sp += 2*sll;

          for (j = 0; j <= (hsize - 2); j += 2) {
            p0 = p2; p1 = p3;
            p2 = sp[0];
            p3 = sp[sll];

            d0 = D2I(p0*k0 + p1*k1 + p2*k2 + pbuff[j]);
            d1 = D2I(p1*k0 + p2*k1 + p3*k2 + pbuff[j + 1]);

            dp[0  ] = FROM_S32(d0);
            dp[dll] = FROM_S32(d1);

            pbuff[j] = 0;
            pbuff[j + 1] = 0;

            sp += 2*sll;
            dp += 2*dll;
          }

          if (j < hsize) {
            p0 = p2; p1 = p3;
            p2 = sp[0];

            d0 = D2I(p0*k0 + p1*k1 + p2*k2 + pbuff[j]);

            pbuff[j] = 0;

            dp[0] = FROM_S32(d0);
          }

        } else if (kh == 2) {
          sp += sll;

          for (j = 0; j <= (hsize - 2); j += 2) {
            p0 = p2;
            p1 = sp[0];
            p2 = sp[sll];

            d0 = D2I(p0*k0 + p1*k1 + pbuff[j]);
            d1 = D2I(p1*k0 + p2*k1 + pbuff[j + 1]);

            dp[0  ] = FROM_S32(d0);
            dp[dll] = FROM_S32(d1);

            pbuff[j] = 0;
            pbuff[j + 1] = 0;

            sp += 2*sll;
            dp += 2*dll;
          }

          if (j < hsize) {
            p0 = p2;
            p1 = sp[0];

            d0 = D2I(p0*k0 + p1*k1 + pbuff[j]);

            pbuff[j] = 0;

            dp[0] = FROM_S32(d0);
          }

        } else /* if (kh == 1) */ {
          for (j = 0; j < hsize; j++) {
            p0 = sp[0];

            d0 = D2I(p0*k0 + pbuff[j]);

            dp[0] = FROM_S32(d0);

            pbuff[j] = 0;

            sp += sll;
            dp += dll;
          }
        }

        sl += chan1;
        dl += chan1;
      }
    }

    sl_c += max_hsize*sll;
    dl_c += max_hsize*dll;
  }

  if (pbuff != buff) mlib_free(pbuff);

  return MLIB_SUCCESS;
}

/***************************************************************/
mlib_status CONV_FUNC(MxN)(mlib_image       *dst,
                           const mlib_image *src,
                           const mlib_s32   *kernel,
                           mlib_s32         m,
                           mlib_s32         n,
                           mlib_s32         dm,
                           mlib_s32         dn,
                           mlib_s32         scale,
                           mlib_s32         cmask)
{
  FTYPE    buff[BUFF_SIZE], *buffs_arr[2*(MAX_N + 1)];
  FTYPE    **buffs = buffs_arr, *buffd;
  FTYPE    akernel[256], *k = akernel, fscale = DSCALE;
  mlib_s32 mn, l, off, kw, bsize, buff_ind;
  mlib_s32 d0, d1;
  FTYPE    k0, k1, k2, k3, k4, k5, k6;
  FTYPE    p0, p1, p2, p3, p4, p5, p6, p7;
  d64_2x32 dd;
  DEF_VARS(DTYPE);
  mlib_s32 chan2;
  mlib_s32 *buffo, *buffi;
  mlib_status status = MLIB_SUCCESS;

  GET_SRC_DST_PARAMETERS(DTYPE);

  if (scale > 30) {
    fscale *= 1.0/(1 << 30);
    scale -= 30;
  }

  fscale /= (1 << scale);

  mn = m*n;

  if (mn > 256) {
    k = mlib_malloc(mn*sizeof(mlib_d64));

    if (k == NULL) return MLIB_FAILURE;
  }

  for (i = 0; i < mn; i++) {
    k[i] = kernel[i]*fscale;
  }

  if (m == 1) {
    status = mlib_ImageConv1xN(dst, src, k, n, dn, cmask);
    FREE_AND_RETURN_STATUS;
  }

  bsize = (n + 3)*wid;

  if ((bsize > BUFF_SIZE) || (n > MAX_N)) {
    pbuff = mlib_malloc(sizeof(FTYPE)*bsize + sizeof(FTYPE *)*2*(n + 1));

    if (pbuff == NULL) {
      status = MLIB_FAILURE;
      FREE_AND_RETURN_STATUS;
    }
    buffs = (FTYPE   **)(pbuff + bsize);
  }

  for (l = 0; l < (n + 1); l++) buffs[l] = pbuff + l*wid;
  for (l = 0; l < (n + 1); l++) buffs[l + (n + 1)] = buffs[l];
  buffd = buffs[n] + wid;
  buffo = (mlib_s32*)(buffd + wid);
  buffi = buffo + (wid &~ 1);

  chan1 = nchannel;
  chan2 = chan1 + chan1;

  wid -= (m - 1);
  hgt -= (n - 1);
  adr_dst += dn*dll + dm*nchannel;

  for (c = 0; c < nchannel; c++) {
    if (!(cmask & (1 << (chan1 - 1 - c)))) continue;

    sl = adr_src + c;
    dl = adr_dst + c;

    for (l = 0; l < n; l++) {
      FTYPE    *buff = buffs[l];

      for (i = 0; i < wid + (m - 1); i++) {
        buff[i] = (FTYPE)sl[i*chan1];
      }

      sl += sll;
    }

    buff_ind = 0;

    for (i = 0; i < wid; i++) buffd[i] = 0.0;

    for (j = 0; j < hgt; j++) {
      FTYPE    **buffc = buffs + buff_ind;
      FTYPE    *buffn = buffc[n];
      FTYPE    *pk = k;

      for (l = 0; l < n; l++) {
        FTYPE    *buff_l = buffc[l];

        for (off = 0; off < m;) {
          FTYPE    *buff = buff_l + off;

          kw = m - off;

          if (kw > 2*MAX_KER) kw = MAX_KER; else
            if (kw > MAX_KER) kw = kw/2;
          off += kw;

          sp = sl;
          dp = dl;

          p2 = buff[0]; p3 = buff[1]; p4 = buff[2];
          p5 = buff[3]; p6 = buff[4]; p7 = buff[5];

          k0 = pk[0]; k1 = pk[1]; k2 = pk[2]; k3 = pk[3];
          k4 = pk[4]; k5 = pk[5]; k6 = pk[6];
          pk += kw;

          if (kw == 7) {

            if (l < (n - 1) || off < m) {
              for (i = 0; i <= (wid - 2); i += 2) {
                p0 = p2; p1 = p3; p2 = p4; p3 = p5; p4 = p6; p5 = p7;

                p6 = buff[i + 6]; p7 = buff[i + 7];

                buffd[i    ] += p0*k0 + p1*k1 + p2*k2 + p3*k3 + p4*k4 + p5*k5 + p6*k6;
                buffd[i + 1] += p1*k0 + p2*k1 + p3*k2 + p4*k3 + p5*k4 + p6*k5 + p7*k6;
              }

            } else {
              for (i = 0; i <= (wid - 2); i += 2) {
                p0 = p2; p1 = p3; p2 = p4; p3 = p5; p4 = p6; p5 = p7;

                p6 = buff[i + 6]; p7 = buff[i + 7];

                LOAD_BUFF(buffi);

                dd.d64 = *(FTYPE   *)(buffi + i);
                buffn[i    ] = (FTYPE)dd.i32s.i0;
                buffn[i + 1] = (FTYPE)dd.i32s.i1;

                d0 = D2I(p0*k0 + p1*k1 + p2*k2 + p3*k3 + p4*k4 + p5*k5 + p6*k6 + buffd[i    ]);
                d1 = D2I(p1*k0 + p2*k1 + p3*k2 + p4*k3 + p5*k4 + p6*k5 + p7*k6 + buffd[i + 1]);

                dp[0    ] = FROM_S32(d0);
                dp[chan1] = FROM_S32(d1);

                buffd[i    ] = 0.0;
                buffd[i + 1] = 0.0;

                sp += chan2;
                dp += chan2;
              }
            }

          } else if (kw == 6) {

            if (l < (n - 1) || off < m) {
              for (i = 0; i <= (wid - 2); i += 2) {
                p0 = p2; p1 = p3; p2 = p4; p3 = p5; p4 = p6;

                p5 = buff[i + 5]; p6 = buff[i + 6];

                buffd[i    ] += p0*k0 + p1*k1 + p2*k2 + p3*k3 + p4*k4 + p5*k5;
                buffd[i + 1] += p1*k0 + p2*k1 + p3*k2 + p4*k3 + p5*k4 + p6*k5;
              }

            } else {
              for (i = 0; i <= (wid - 2); i += 2) {
                p0 = p2; p1 = p3; p2 = p4; p3 = p5; p4 = p6;

                p5 = buff[i + 5]; p6 = buff[i + 6];

                buffn[i    ] = (FTYPE)sp[0];
                buffn[i + 1] = (FTYPE)sp[chan1];

                d0 = D2I(p0*k0 + p1*k1 + p2*k2 + p3*k3 + p4*k4 + p5*k5 + buffd[i    ]);
                d1 = D2I(p1*k0 + p2*k1 + p3*k2 + p4*k3 + p5*k4 + p6*k5 + buffd[i + 1]);

                dp[0    ] = FROM_S32(d0);
                dp[chan1] = FROM_S32(d1);

                buffd[i    ] = 0.0;
                buffd[i + 1] = 0.0;

                sp += chan2;
                dp += chan2;
              }
            }

          } else if (kw == 5) {

            if (l < (n - 1) || off < m) {
              for (i = 0; i <= (wid - 2); i += 2) {
                p0 = p2; p1 = p3; p2 = p4; p3 = p5;

                p4 = buff[i + 4]; p5 = buff[i + 5];

                buffd[i    ] += p0*k0 + p1*k1 + p2*k2 + p3*k3 + p4*k4;
                buffd[i + 1] += p1*k0 + p2*k1 + p3*k2 + p4*k3 + p5*k4;
              }

            } else {
              for (i = 0; i <= (wid - 2); i += 2) {
                p0 = p2; p1 = p3; p2 = p4; p3 = p5;

                p4 = buff[i + 4]; p5 = buff[i + 5];

                buffn[i    ] = (FTYPE)sp[0];
                buffn[i + 1] = (FTYPE)sp[chan1];

                d0 = D2I(p0*k0 + p1*k1 + p2*k2 + p3*k3 + p4*k4 + buffd[i    ]);
                d1 = D2I(p1*k0 + p2*k1 + p3*k2 + p4*k3 + p5*k4 + buffd[i + 1]);

                dp[0    ] = FROM_S32(d0);
                dp[chan1] = FROM_S32(d1);

                buffd[i    ] = 0.0;
                buffd[i + 1] = 0.0;

                sp += chan2;
                dp += chan2;
              }
            }

          } else if (kw == 4) {

            if (l < (n - 1) || off < m) {
              for (i = 0; i <= (wid - 2); i += 2) {
                p0 = p2; p1 = p3; p2 = p4;

                p3 = buff[i + 3]; p4 = buff[i + 4];

                buffd[i    ] += p0*k0 + p1*k1 + p2*k2 + p3*k3;
                buffd[i + 1] += p1*k0 + p2*k1 + p3*k2 + p4*k3;
              }

            } else {
              for (i = 0; i <= (wid - 2); i += 2) {
                p0 = p2; p1 = p3; p2 = p4;

                p3 = buff[i + 3]; p4 = buff[i + 4];

                buffn[i    ] = (FTYPE)sp[0];
                buffn[i + 1] = (FTYPE)sp[chan1];

                d0 = D2I(p0*k0 + p1*k1 + p2*k2 + p3*k3 + buffd[i    ]);
                d1 = D2I(p1*k0 + p2*k1 + p3*k2 + p4*k3 + buffd[i + 1]);

                dp[0    ] = FROM_S32(d0);
                dp[chan1] = FROM_S32(d1);

                buffd[i    ] = 0.0;
                buffd[i + 1] = 0.0;

                sp += chan2;
                dp += chan2;
              }
            }

          } else if (kw == 3) {

            if (l < (n - 1) || off < m) {
              for (i = 0; i <= (wid - 2); i += 2) {
                p0 = p2; p1 = p3;

                p2 = buff[i + 2]; p3 = buff[i + 3];

                buffd[i    ] += p0*k0 + p1*k1 + p2*k2;
                buffd[i + 1] += p1*k0 + p2*k1 + p3*k2;
              }

            } else {
              for (i = 0; i <= (wid - 2); i += 2) {
                p0 = p2; p1 = p3;

                p2 = buff[i + 2]; p3 = buff[i + 3];

                buffn[i    ] = (FTYPE)sp[0];
                buffn[i + 1] = (FTYPE)sp[chan1];

                d0 = D2I(p0*k0 + p1*k1 + p2*k2 + buffd[i    ]);
                d1 = D2I(p1*k0 + p2*k1 + p3*k2 + buffd[i + 1]);

                dp[0    ] = FROM_S32(d0);
                dp[chan1] = FROM_S32(d1);

                buffd[i    ] = 0.0;
                buffd[i + 1] = 0.0;

                sp += chan2;
                dp += chan2;
              }
            }

          } else /*if (kw == 2)*/ {

            if (l < (n - 1) || off < m) {
              for (i = 0; i <= (wid - 2); i += 2) {
                p0 = p2;

                p1 = buff[i + 1]; p2 = buff[i + 2];

                buffd[i    ] += p0*k0 + p1*k1;
                buffd[i + 1] += p1*k0 + p2*k1;
              }

            } else {
              for (i = 0; i <= (wid - 2); i += 2) {
                p0 = p2;

                p1 = buff[i + 1]; p2 = buff[i + 2];

                buffn[i    ] = (FTYPE)sp[0];
                buffn[i + 1] = (FTYPE)sp[chan1];

                d0 = D2I(p0*k0 + p1*k1 + buffd[i    ]);
                d1 = D2I(p1*k0 + p2*k1 + buffd[i + 1]);

                dp[0    ] = FROM_S32(d0);
                dp[chan1] = FROM_S32(d1);

                buffd[i    ] = 0.0;
                buffd[i + 1] = 0.0;

                sp += chan2;
                dp += chan2;
              }
            }
          }
        }
      }

      /* last pixels */
      for (; i < wid; i++) {
        FTYPE    *pk = k, s = 0;
        mlib_s32 x, d0;

        for (l = 0; l < n; l++) {
          FTYPE    *buff = buffc[l] + i;

          for (x = 0; x < m; x++) s += buff[x] * (*pk++);
        }

        d0 = D2I(s);
        dp[0] = FROM_S32(d0);

        buffn[i] = (FTYPE)sp[0];

        sp += chan1;
        dp += chan1;
      }

      for (l = 0; l < (m - 1); l++) buffn[wid + l] = sp[l*chan1];

      /* next line */
      sl += sll;
      dl += dll;

      buff_ind++;

      if (buff_ind >= n + 1) buff_ind = 0;
    }
  }

  FREE_AND_RETURN_STATUS;
}

/***************************************************************/
/* for x86, using integer multiplies is faster */

#define STORE_RES(res, x)                                       \
  x >>= shift2;                                                 \
  CLAMP_STORE(res, x)

mlib_status CONV_FUNC_I(MxN)(mlib_image       *dst,
                             const mlib_image *src,
                             const mlib_s32   *kernel,
                             mlib_s32         m,
                             mlib_s32         n,
                             mlib_s32         dm,
                             mlib_s32         dn,
                             mlib_s32         scale,
                             mlib_s32         cmask)
{
  mlib_s32 buff[BUFF_SIZE], *buffd = buff;
  mlib_s32 l, off, kw;
  mlib_s32 d0, d1, shift1, shift2;
  mlib_s32 k0, k1, k2, k3, k4, k5, k6;
  mlib_s32 p0, p1, p2, p3, p4, p5, p6, p7;
  DTYPE    *adr_src, *sl, *sp = NULL;
  DTYPE    *adr_dst, *dl, *dp = NULL;
  mlib_s32 wid, hgt, sll, dll;
  mlib_s32 nchannel, chan1;
  mlib_s32 i, j, c;
  mlib_s32 chan2;
  mlib_s32 k_locl[MAX_N*MAX_N], *k = k_locl;
  GET_SRC_DST_PARAMETERS(DTYPE);

#if IMG_TYPE != 1
  shift1 = 16;
#else
  shift1 = 8;
#endif /* IMG_TYPE != 1 */
  shift2 = scale - shift1;

  chan1 = nchannel;
  chan2 = chan1 + chan1;

  wid -= (m - 1);
  hgt -= (n - 1);
  adr_dst += dn*dll + dm*nchannel;

  if (wid > BUFF_SIZE) {
    buffd = mlib_malloc(sizeof(mlib_s32)*wid);

    if (buffd == NULL) return MLIB_FAILURE;
  }

  if (m*n > MAX_N*MAX_N) {
    k = mlib_malloc(sizeof(mlib_s32)*(m*n));

    if (k == NULL) {
      if (buffd != buff) mlib_free(buffd);
      return MLIB_FAILURE;
    }
  }

  for (i = 0; i < m*n; i++) {
    k[i] = kernel[i] >> shift1;
  }

  for (c = 0; c < nchannel; c++) {
    if (!(cmask & (1 << (nchannel - 1 - c)))) continue;

    sl = adr_src + c;
    dl = adr_dst + c;

    for (i = 0; i < wid; i++) buffd[i] = 0;

    for (j = 0; j < hgt; j++) {
      mlib_s32 *pk = k;

      for (l = 0; l < n; l++) {
        DTYPE *sp0 = sl + l*sll;

        for (off = 0; off < m;) {
          sp = sp0 + off*chan1;
          dp = dl;

          kw = m - off;

          if (kw > 2*MAX_KER) kw = MAX_KER; else
            if (kw > MAX_KER) kw = kw/2;
          off += kw;

          p2 = sp[0]; p3 = sp[chan1]; p4 = sp[chan2];
          p5 = sp[chan2 + chan1]; p6 = sp[chan2 + chan2]; p7 = sp[5*chan1];

          k0 = pk[0]; k1 = pk[1]; k2 = pk[2]; k3 = pk[3];
          k4 = pk[4]; k5 = pk[5]; k6 = pk[6];
          pk += kw;

          sp += (kw - 1)*chan1;

          if (kw == 7) {

            if (l < (n - 1) || off < m) {
              for (i = 0; i <= (wid - 2); i += 2) {
                p0 = p2; p1 = p3; p2 = p4; p3 = p5; p4 = p6; p5 = p7;
                p6 = sp[0];
                p7 = sp[chan1];

                buffd[i    ] += p0*k0 + p1*k1 + p2*k2 + p3*k3 + p4*k4 + p5*k5 + p6*k6;
                buffd[i + 1] += p1*k0 + p2*k1 + p3*k2 + p4*k3 + p5*k4 + p6*k5 + p7*k6;

                sp += chan2;
              }

            } else {
              for (i = 0; i <= (wid - 2); i += 2) {
                p0 = p2; p1 = p3; p2 = p4; p3 = p5; p4 = p6; p5 = p7;
                p6 = sp[0];
                p7 = sp[chan1];

                d0 = (p0*k0 + p1*k1 + p2*k2 + p3*k3 + p4*k4 + p5*k5 + p6*k6 + buffd[i    ]);
                d1 = (p1*k0 + p2*k1 + p3*k2 + p4*k3 + p5*k4 + p6*k5 + p7*k6 + buffd[i + 1]);

                STORE_RES(dp[0    ], d0);
                STORE_RES(dp[chan1], d1);

                buffd[i    ] = 0;
                buffd[i + 1] = 0;

                sp += chan2;
                dp += chan2;
              }
            }

          } else if (kw == 6) {

            if (l < (n - 1) || off < m) {
              for (i = 0; i <= (wid - 2); i += 2) {
                p0 = p2; p1 = p3; p2 = p4; p3 = p5; p4 = p6;
                p5 = sp[0];
                p6 = sp[chan1];

                buffd[i    ] += p0*k0 + p1*k1 + p2*k2 + p3*k3 + p4*k4 + p5*k5;
                buffd[i + 1] += p1*k0 + p2*k1 + p3*k2 + p4*k3 + p5*k4 + p6*k5;

                sp += chan2;
              }

            } else {
              for (i = 0; i <= (wid - 2); i += 2) {
                p0 = p2; p1 = p3; p2 = p4; p3 = p5; p4 = p6;
                p5 = sp[0];
                p6 = sp[chan1];

                d0 = (p0*k0 + p1*k1 + p2*k2 + p3*k3 + p4*k4 + p5*k5 + buffd[i    ]);
                d1 = (p1*k0 + p2*k1 + p3*k2 + p4*k3 + p5*k4 + p6*k5 + buffd[i + 1]);

                STORE_RES(dp[0    ], d0);
                STORE_RES(dp[chan1], d1);

                buffd[i    ] = 0;
                buffd[i + 1] = 0;

                sp += chan2;
                dp += chan2;
              }
            }

          } else if (kw == 5) {

            if (l < (n - 1) || off < m) {
              for (i = 0; i <= (wid - 2); i += 2) {
                p0 = p2; p1 = p3; p2 = p4; p3 = p5;
                p4 = sp[0];
                p5 = sp[chan1];

                buffd[i    ] += p0*k0 + p1*k1 + p2*k2 + p3*k3 + p4*k4;
                buffd[i + 1] += p1*k0 + p2*k1 + p3*k2 + p4*k3 + p5*k4;

                sp += chan2;
              }

            } else {
              for (i = 0; i <= (wid - 2); i += 2) {
                p0 = p2; p1 = p3; p2 = p4; p3 = p5;
                p4 = sp[0];
                p5 = sp[chan1];

                d0 = (p0*k0 + p1*k1 + p2*k2 + p3*k3 + p4*k4 + buffd[i    ]);
                d1 = (p1*k0 + p2*k1 + p3*k2 + p4*k3 + p5*k4 + buffd[i + 1]);

                STORE_RES(dp[0    ], d0);
                STORE_RES(dp[chan1], d1);

                buffd[i    ] = 0;
                buffd[i + 1] = 0;

                sp += chan2;
                dp += chan2;
              }
            }

          } else if (kw == 4) {

            if (l < (n - 1) || off < m) {
              for (i = 0; i <= (wid - 2); i += 2) {
                p0 = p2; p1 = p3; p2 = p4;
                p3 = sp[0];
                p4 = sp[chan1];

                buffd[i    ] += p0*k0 + p1*k1 + p2*k2 + p3*k3;
                buffd[i + 1] += p1*k0 + p2*k1 + p3*k2 + p4*k3;

                sp += chan2;
              }

            } else {
              for (i = 0; i <= (wid - 2); i += 2) {
                p0 = p2; p1 = p3; p2 = p4;
                p3 = sp[0];
                p4 = sp[chan1];

                d0 = (p0*k0 + p1*k1 + p2*k2 + p3*k3 + buffd[i    ]);
                d1 = (p1*k0 + p2*k1 + p3*k2 + p4*k3 + buffd[i + 1]);

                STORE_RES(dp[0    ], d0);
                STORE_RES(dp[chan1], d1);

                buffd[i    ] = 0;
                buffd[i + 1] = 0;

                sp += chan2;
                dp += chan2;
              }
            }

          } else if (kw == 3) {

            if (l < (n - 1) || off < m) {
              for (i = 0; i <= (wid - 2); i += 2) {
                p0 = p2; p1 = p3;
                p2 = sp[0];
                p3 = sp[chan1];

                buffd[i    ] += p0*k0 + p1*k1 + p2*k2;
                buffd[i + 1] += p1*k0 + p2*k1 + p3*k2;

                sp += chan2;
              }

            } else {
              for (i = 0; i <= (wid - 2); i += 2) {
                p0 = p2; p1 = p3;
                p2 = sp[0];
                p3 = sp[chan1];

                d0 = (p0*k0 + p1*k1 + p2*k2 + buffd[i    ]);
                d1 = (p1*k0 + p2*k1 + p3*k2 + buffd[i + 1]);

                STORE_RES(dp[0    ], d0);
                STORE_RES(dp[chan1], d1);

                buffd[i    ] = 0;
                buffd[i + 1] = 0;

                sp += chan2;
                dp += chan2;
              }
            }

          } else if (kw == 2) {

            if (l < (n - 1) || off < m) {
              for (i = 0; i <= (wid - 2); i += 2) {
                p0 = p2;
                p1 = sp[0];
                p2 = sp[chan1];

                buffd[i    ] += p0*k0 + p1*k1;
                buffd[i + 1] += p1*k0 + p2*k1;

                sp += chan2;
              }

            } else {
              for (i = 0; i <= (wid - 2); i += 2) {
                p0 = p2;
                p1 = sp[0];
                p2 = sp[chan1];

                d0 = (p0*k0 + p1*k1 + buffd[i    ]);
                d1 = (p1*k0 + p2*k1 + buffd[i + 1]);

                STORE_RES(dp[0    ], d0);
                STORE_RES(dp[chan1], d1);

                buffd[i    ] = 0;
                buffd[i + 1] = 0;

                sp += chan2;
                dp += chan2;
              }
            }

          } else /*if (kw == 1)*/ {

            if (l < (n - 1) || off < m) {
              for (i = 0; i <= (wid - 2); i += 2) {
                p0 = sp[0];
                p1 = sp[chan1];

                buffd[i    ] += p0*k0;
                buffd[i + 1] += p1*k0;

                sp += chan2;
              }

            } else {
              for (i = 0; i <= (wid - 2); i += 2) {
                p0 = sp[0];
                p1 = sp[chan1];

                d0 = (p0*k0 + buffd[i    ]);
                d1 = (p1*k0 + buffd[i + 1]);

                STORE_RES(dp[0    ], d0);
                STORE_RES(dp[chan1], d1);

                buffd[i    ] = 0;
                buffd[i + 1] = 0;

                sp += chan2;
                dp += chan2;
              }
            }
          }
        }
      }

      /* last pixels */
      for (; i < wid; i++) {
        mlib_s32 *pk = k, s = 0;
        mlib_s32 x;

        for (l = 0; l < n; l++) {
          sp = sl + l*sll + i*chan1;

          for (x = 0; x < m; x++) {
            s += sp[0] * pk[0];
            sp += chan1;
            pk ++;
          }
        }

        STORE_RES(dp[0], s);

        sp += chan1;
        dp += chan1;
      }

      sl += sll;
      dl += dll;
    }
  }

  if (buffd != buff) mlib_free(buffd);
  if (k != k_locl) mlib_free(k);

  return MLIB_SUCCESS;
}

/***************************************************************/
