/*
 * Copyright (c) 2000, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *   Internal functions for mlib_ImageConv* on S32 type and
 *   MLIB_EDGE_DST_NO_WRITE mask
 *
 */

#include "mlib_image.h"
#include "mlib_ImageConv.h"

/***************************************************************/
#define CACHE_SIZE (64*1024)

/***************************************************************/
#define CONV_FUNC(KERN) mlib_conv##KERN##nw_s32

/***************************************************************/
#ifndef MLIB_USE_FTOI_CLAMPING

#define CLAMP_S32(dst, src)                                       \
  if (src > (mlib_d64)MLIB_S32_MAX) src = (mlib_d64)MLIB_S32_MAX; \
  if (src < (mlib_d64)MLIB_S32_MIN) src = (mlib_d64)MLIB_S32_MIN; \
  dst = (mlib_s32)src

#else

#define CLAMP_S32(dst, src) dst = (mlib_s32)(src)

#endif /* MLIB_USE_FTOI_CLAMPING */

/***************************************************************/
#define GET_SRC_DST_PARAMETERS(type)                            \
  mlib_s32 hgt = mlib_ImageGetHeight(src);                      \
  mlib_s32 wid = mlib_ImageGetWidth(src);                       \
  mlib_s32 sll = mlib_ImageGetStride(src) / sizeof(type);       \
  mlib_s32 dll = mlib_ImageGetStride(dst) / sizeof(type);       \
  type*    adr_src = mlib_ImageGetData(src);                    \
  type*    adr_dst = mlib_ImageGetData(dst);                    \
  mlib_s32 chan1 = mlib_ImageGetChannels(src)
/*  mlib_s32 chan2 = chan1 + chan1 */

/***************************************************************/
#define DEF_VARS(type)                                          \
  GET_SRC_DST_PARAMETERS(type);                                 \
  type     *sl, *sp, *sl1, *dl, *dp;                            \
  mlib_d64 *pbuff = buff, *buff0, *buff1, *buff2, *buffT;       \
  mlib_s32 i, j, c;                                             \
  mlib_d64 scalef, d0, d1

/***************************************************************/
#define DEF_VARS_MxN(type)                                      \
  GET_SRC_DST_PARAMETERS(type);                                 \
  type     *sl, *sp = NULL, *dl, *dp = NULL;                    \
  mlib_d64 *pbuff = buff;                                       \
  mlib_s32 i, j, c

/***************************************************************/
#define FTYPE  mlib_d64
#define DTYPE  mlib_s32

#define BUFF_SIZE  1600

static mlib_status mlib_ImageConv1xN(mlib_image       *dst,
                                     const mlib_image *src,
                                     const mlib_d64   *k,
                                     mlib_s32         n,
                                     mlib_s32         dn,
                                     mlib_s32         cmask)
{
  FTYPE    buff[BUFF_SIZE];
  mlib_s32 off, kh;
  const FTYPE    *pk;
  FTYPE    k0, k1, k2, k3, d0, d1;
  FTYPE    p0, p1, p2, p3, p4;
  DTYPE    *sl_c, *dl_c, *sl0;
  mlib_s32 l, hsize, max_hsize;
  DEF_VARS_MxN(DTYPE);

  hgt -= (n - 1);
  adr_dst += dn*dll;

  max_hsize = (CACHE_SIZE/sizeof(DTYPE))/sll;

  if (!max_hsize) max_hsize = 1;

  if (max_hsize > BUFF_SIZE) {
    pbuff = mlib_malloc(sizeof(FTYPE)*max_hsize);
  }

  sl_c = adr_src;
  dl_c = adr_dst;

  for (l = 0; l < hgt; l += hsize) {
    hsize = hgt - l;

    if (hsize > max_hsize) hsize = max_hsize;

    for (c = 0; c < chan1; c++) {
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

            d0 = p0*k0 + p1*k1 + p2*k2 + p3*k3 + pbuff[j];
            d1 = p1*k0 + p2*k1 + p3*k2 + p4*k3 + pbuff[j + 1];
            CLAMP_S32(dp[0  ], d0);
            CLAMP_S32(dp[dll], d1);

            pbuff[j] = 0;
            pbuff[j + 1] = 0;

            sp += 2*sll;
            dp += 2*dll;
          }

          if (j < hsize) {
            p0 = p2; p1 = p3; p2 = p4;
            p3 = sp[0];

            d0 = p0*k0 + p1*k1 + p2*k2 + p3*k3 + pbuff[j];
            CLAMP_S32(dp[0], d0);

            pbuff[j] = 0;
          }

        } else if (kh == 3) {
          sp += 2*sll;

          for (j = 0; j <= (hsize - 2); j += 2) {
            p0 = p2; p1 = p3;
            p2 = sp[0];
            p3 = sp[sll];

            d0 = p0*k0 + p1*k1 + p2*k2 + pbuff[j];
            d1 = p1*k0 + p2*k1 + p3*k2 + pbuff[j + 1];
            CLAMP_S32(dp[0  ], d0);
            CLAMP_S32(dp[dll], d1);

            pbuff[j] = 0;
            pbuff[j + 1] = 0;

            sp += 2*sll;
            dp += 2*dll;
          }

          if (j < hsize) {
            p0 = p2; p1 = p3;
            p2 = sp[0];

            d0 = p0*k0 + p1*k1 + p2*k2 + pbuff[j];
            CLAMP_S32(dp[0], d0);

            pbuff[j] = 0;
          }

        } else if (kh == 2) {
          sp += sll;

          for (j = 0; j <= (hsize - 2); j += 2) {
            p0 = p2;
            p1 = sp[0];
            p2 = sp[sll];

            d0 = p0*k0 + p1*k1 + pbuff[j];
            d1 = p1*k0 + p2*k1 + pbuff[j + 1];
            CLAMP_S32(dp[0  ], d0);
            CLAMP_S32(dp[dll], d1);

            pbuff[j] = 0;
            pbuff[j + 1] = 0;

            sp += 2*sll;
            dp += 2*dll;
          }

          if (j < hsize) {
            p0 = p2;
            p1 = sp[0];

            d0 = p0*k0 + p1*k1 + pbuff[j];
            CLAMP_S32(dp[0], d0);

            pbuff[j] = 0;
          }

        } else /* if (kh == 1) */ {
          for (j = 0; j < hsize; j++) {
            p0 = sp[0];

            d0 = p0*k0 + pbuff[j];
            CLAMP_S32(dp[0], d0);

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
#define MAX_KER 7

#define MAX_N     15

#undef  BUFF_SIZE
#define BUFF_SIZE 1500

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
  mlib_d64  buff[BUFF_SIZE], *buffs_arr[2*(MAX_N + 1)];
  mlib_d64  **buffs = buffs_arr, *buffd;
  mlib_d64  akernel[256], *k = akernel, fscale = 1.0;
  mlib_s32  l, off, kw, bsize, buff_ind, mn;
  mlib_d64  d0, d1;
  mlib_d64  k0, k1, k2, k3, k4, k5, k6;
  mlib_d64  p0, p1, p2, p3, p4, p5, p6, p7;
  DEF_VARS_MxN(mlib_s32);
  mlib_s32 chan2 = chan1 + chan1;

  mlib_status status = MLIB_SUCCESS;

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

  bsize = (n + 2)*wid;

  if ((bsize > BUFF_SIZE) || (n > MAX_N)) {
    pbuff = mlib_malloc(sizeof(mlib_d64)*bsize + sizeof(mlib_d64*)*2*(n + 1));

    if (pbuff == NULL) {
      status = MLIB_FAILURE;
      FREE_AND_RETURN_STATUS;
    }
    buffs = (mlib_d64**)(pbuff + bsize);
  }

  for (l = 0; l < (n + 1); l++) buffs[l] = pbuff + l*wid;
  for (l = 0; l < (n + 1); l++) buffs[l + (n + 1)] = buffs[l];
  buffd = buffs[n] + wid;

  wid -= (m - 1);
  hgt -= (n - 1);
  adr_dst += dn*dll + dm*chan1;

  for (c = 0; c < chan1; c++) {
    if (!(cmask & (1 << (chan1 - 1 - c)))) continue;

    sl = adr_src + c;
    dl = adr_dst + c;

    for (l = 0; l < n; l++) {
      mlib_d64 *buff = buffs[l];

      for (i = 0; i < wid + (m - 1); i++) {
        buff[i] = (mlib_d64)sl[i*chan1];
      }

      sl += sll;
    }

    buff_ind = 0;

    for (i = 0; i < wid; i++) buffd[i] = 0.0;

    for (j = 0; j < hgt; j++) {
      mlib_d64 **buffc = buffs + buff_ind;
      mlib_d64 *buffn = buffc[n];
      mlib_d64 *pk = k;

      for (l = 0; l < n; l++) {
        mlib_d64 *buff_l = buffc[l];

        for (off = 0; off < m;) {
          mlib_d64 *buff = buff_l + off;

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

                buffn[i    ] = (mlib_d64)sp[0];
                buffn[i + 1] = (mlib_d64)sp[chan1];

                d0 = p0*k0 + p1*k1 + p2*k2 + p3*k3 + p4*k4 + p5*k5 + p6*k6 + buffd[i    ];
                d1 = p1*k0 + p2*k1 + p3*k2 + p4*k3 + p5*k4 + p6*k5 + p7*k6 + buffd[i + 1];

                CLAMP_S32(dp[0],     d0);
                CLAMP_S32(dp[chan1], d1);

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

                buffn[i    ] = (mlib_d64)sp[0];
                buffn[i + 1] = (mlib_d64)sp[chan1];

                d0 = p0*k0 + p1*k1 + p2*k2 + p3*k3 + p4*k4 + p5*k5 + buffd[i    ];
                d1 = p1*k0 + p2*k1 + p3*k2 + p4*k3 + p5*k4 + p6*k5 + buffd[i + 1];

                CLAMP_S32(dp[0],     d0);
                CLAMP_S32(dp[chan1], d1);

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

                buffn[i    ] = (mlib_d64)sp[0];
                buffn[i + 1] = (mlib_d64)sp[chan1];

                d0 = p0*k0 + p1*k1 + p2*k2 + p3*k3 + p4*k4 + buffd[i    ];
                d1 = p1*k0 + p2*k1 + p3*k2 + p4*k3 + p5*k4 + buffd[i + 1];

                CLAMP_S32(dp[0],     d0);
                CLAMP_S32(dp[chan1], d1);

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

                buffn[i    ] = (mlib_d64)sp[0];
                buffn[i + 1] = (mlib_d64)sp[chan1];

                d0 = p0*k0 + p1*k1 + p2*k2 + p3*k3 + buffd[i    ];
                d1 = p1*k0 + p2*k1 + p3*k2 + p4*k3 + buffd[i + 1];

                CLAMP_S32(dp[0],     d0);
                CLAMP_S32(dp[chan1], d1);

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

                buffn[i    ] = (mlib_d64)sp[0];
                buffn[i + 1] = (mlib_d64)sp[chan1];

                d0 = p0*k0 + p1*k1 + p2*k2 + buffd[i    ];
                d1 = p1*k0 + p2*k1 + p3*k2 + buffd[i + 1];

                CLAMP_S32(dp[0],     d0);
                CLAMP_S32(dp[chan1], d1);

                buffd[i    ] = 0.0;
                buffd[i + 1] = 0.0;

                sp += chan2;
                dp += chan2;
              }
            }

          } else { /* kw == 2 */

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

                buffn[i    ] = (mlib_d64)sp[0];
                buffn[i + 1] = (mlib_d64)sp[chan1];

                d0 = p0*k0 + p1*k1 + buffd[i    ];
                d1 = p1*k0 + p2*k1 + buffd[i + 1];

                CLAMP_S32(dp[0],     d0);
                CLAMP_S32(dp[chan1], d1);

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
        mlib_d64 *pk = k, s = 0;
        mlib_s32 x;

        for (l = 0; l < n; l++) {
          mlib_d64 *buff = buffc[l] + i;

          for (x = 0; x < m; x++) s += buff[x] * (*pk++);
        }

        CLAMP_S32(dp[0], s);

        buffn[i] = (mlib_d64)sp[0];

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
