/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *  DESCRIPTION
 *    Calculates cliping boundary for Affine functions.
 *
 */

#include "mlib_image.h"
#include "mlib_SysMath.h"
#include "mlib_ImageAffine.h"
#include "safe_math.h"


/***************************************************************/
mlib_status mlib_AffineEdges(mlib_affine_param *param,
                             const mlib_image  *dst,
                             const mlib_image  *src,
                             void              *buff_lcl,
                             mlib_s32          buff_size,
                             mlib_s32          kw,
                             mlib_s32          kh,
                             mlib_s32          kw1,
                             mlib_s32          kh1,
                             mlib_edge         edge,
                             const mlib_d64    *mtx,
                             mlib_s32          shiftx,
                             mlib_s32          shifty)
{
  mlib_u8 *buff = buff_lcl;
  mlib_u8 **lineAddr = param->lineAddr;
  mlib_s32 srcWidth, dstWidth, srcHeight, dstHeight, srcYStride, dstYStride;
  mlib_s32 *leftEdges, *rightEdges, *xStarts, *yStarts, bsize0, bsize1 = 0;
  mlib_u8 *srcData, *dstData;
  mlib_u8 *paddings;
  void *warp_tbl = NULL;
  mlib_s32 yStart = 0, yFinish = -1, dX, dY;

  mlib_d64 xClip, yClip, wClip, hClip;
  mlib_d64 delta = 0.;
  mlib_d64 minX, minY, maxX, maxY;

  mlib_d64 coords[4][2];
  mlib_d64 a = mtx[0], b = mtx[1], tx = mtx[2], c = mtx[3], d = mtx[4], ty = mtx[5];
  mlib_d64 a2, b2, tx2, c2, d2, ty2;
  mlib_d64 dx, dy, div;
  mlib_s32 sdx, sdy;
  mlib_d64 dTop;
  mlib_d64 val0;
  mlib_s32 top, bot;
  mlib_s32 topIdx, max_xsize = 0;
  mlib_s32 i, j, t;

  srcData = mlib_ImageGetData(src);
  dstData = mlib_ImageGetData(dst);
  srcWidth = mlib_ImageGetWidth(src);
  srcHeight = mlib_ImageGetHeight(src);
  dstWidth = mlib_ImageGetWidth(dst);
  dstHeight = mlib_ImageGetHeight(dst);
  srcYStride = mlib_ImageGetStride(src);
  dstYStride = mlib_ImageGetStride(dst);
  paddings = mlib_ImageGetPaddings(src);

  /* All the transformation matrix parameters should be finite. if not, return failure */
  if (!(IS_FINITE(a) && IS_FINITE(b) && IS_FINITE(c) && IS_FINITE(d) &&
        IS_FINITE(tx) && IS_FINITE(ty))) {
    return MLIB_FAILURE;
  }

  if (srcWidth >= (1 << 15) || srcHeight >= (1 << 15)) {
    return MLIB_FAILURE;
  }

  div = a * d - b * c;

  if (div == 0.0) {
    return MLIB_FAILURE;
  }

  bsize0 = (dstHeight * sizeof(mlib_s32) + 7) & ~7;

  if (lineAddr == NULL) {
    bsize1 = ((srcHeight + 4 * kh) * sizeof(mlib_u8 *) + 7) & ~7;
  }

  param->buff_malloc = NULL;

  if ((4 * bsize0 + bsize1) > buff_size) {
    buff = param->buff_malloc = mlib_malloc(4 * bsize0 + bsize1);

    if (buff == NULL)
      return MLIB_FAILURE;
  }

  leftEdges = (mlib_s32 *) (buff);
  rightEdges = (mlib_s32 *) (buff += bsize0);
  xStarts = (mlib_s32 *) (buff += bsize0);
  yStarts = (mlib_s32 *) (buff += bsize0);

  if (lineAddr == NULL) {
    mlib_u8 *srcLinePtr = srcData;
    lineAddr = (mlib_u8 **) (buff += bsize0);
    for (i = 0; i < 2 * kh; i++)
      lineAddr[i] = srcLinePtr;
    lineAddr += 2 * kh;
    for (i = 0; i < srcHeight - 1; i++) {
      lineAddr[i] = srcLinePtr;
      srcLinePtr += srcYStride;
    }

    for (i = srcHeight - 1; i < srcHeight + 2 * kh; i++)
      lineAddr[i] = srcLinePtr;
  }

  if ((mlib_s32) edge < 0) {                               /* process edges */
    minX = 0;
    minY = 0;
    maxX = srcWidth;
    maxY = srcHeight;
  }
  else {

    if (kw > 1)
      delta = -0.5;                                        /* for MLIB_NEAREST filter delta = 0. */

    minX = (kw1 - delta);
    minY = (kh1 - delta);
    maxX = srcWidth - ((kw - 1) - (kw1 - delta));
    maxY = srcHeight - ((kh - 1) - (kh1 - delta));

    if (edge == MLIB_EDGE_SRC_PADDED) {
      if (minX < paddings[0])
        minX = paddings[0];

      if (minY < paddings[1])
        minY = paddings[1];

      if (maxX > (srcWidth - paddings[2]))
        maxX = srcWidth - paddings[2];

      if (maxY > (srcHeight - paddings[3]))
        maxY = srcHeight - paddings[3];
    }
  }

  xClip = minX;
  yClip = minY;
  wClip = maxX;
  hClip = maxY;

/*
 *   STORE_PARAM(param, src);
 *   STORE_PARAM(param, dst);
 */
  param->src = (void *)src;
  param->dst = (void *)dst;
  STORE_PARAM(param, lineAddr);
  STORE_PARAM(param, dstData);
  STORE_PARAM(param, srcYStride);
  STORE_PARAM(param, dstYStride);
  STORE_PARAM(param, leftEdges);
  STORE_PARAM(param, rightEdges);
  STORE_PARAM(param, xStarts);
  STORE_PARAM(param, yStarts);
  STORE_PARAM(param, max_xsize);
  STORE_PARAM(param, yStart);
  STORE_PARAM(param, yFinish);
  STORE_PARAM(param, warp_tbl);

  if ((xClip >= wClip) || (yClip >= hClip)) {
    return MLIB_SUCCESS;
  }

  a2 = d;
  b2 = -b;
  tx2 = (-d * tx + b * ty);
  c2 = -c;
  d2 = a;
  ty2 = (c * tx - a * ty);

  dx = a2;
  dy = c2;

  tx -= 0.5;
  ty -= 0.5;

  coords[0][0] = xClip * a + yClip * b + tx;
  coords[0][1] = xClip * c + yClip * d + ty;

  coords[2][0] = wClip * a + hClip * b + tx;
  coords[2][1] = wClip * c + hClip * d + ty;

  if (div > 0) {
    coords[1][0] = wClip * a + yClip * b + tx;
    coords[1][1] = wClip * c + yClip * d + ty;

    coords[3][0] = xClip * a + hClip * b + tx;
    coords[3][1] = xClip * c + hClip * d + ty;
  }
  else {
    coords[3][0] = wClip * a + yClip * b + tx;
    coords[3][1] = wClip * c + yClip * d + ty;

    coords[1][0] = xClip * a + hClip * b + tx;
    coords[1][1] = xClip * c + hClip * d + ty;
  }

  topIdx = 0;
  for (i = 1; i < 4; i++) {

    if (coords[i][1] < coords[topIdx][1])
      topIdx = i;
  }

  dTop = coords[topIdx][1];
  val0 = dTop;
  SAT32(top);
  bot = -1;

  if (top >= dstHeight) {
    return MLIB_SUCCESS;
  }

  if (dTop >= 0.0) {
    mlib_d64 xLeft, xRight, x;
    mlib_s32 nextIdx;

    if (dTop == top) {
      xLeft = coords[topIdx][0];
      xRight = coords[topIdx][0];
      nextIdx = (topIdx + 1) & 0x3;

      if (dTop == coords[nextIdx][1]) {
        x = coords[nextIdx][0];
        xLeft = (xLeft <= x) ? xLeft : x;
        xRight = (xRight >= x) ? xRight : x;
      }

      nextIdx = (topIdx - 1) & 0x3;

      if (dTop == coords[nextIdx][1]) {
        x = coords[nextIdx][0];
        xLeft = (xLeft <= x) ? xLeft : x;
        xRight = (xRight >= x) ? xRight : x;
      }

      val0 = xLeft;
      SAT32(t);
      leftEdges[top] = (t >= xLeft) ? t : ++t;

      if (xLeft >= MLIB_S32_MAX)
        leftEdges[top] = MLIB_S32_MAX;

      val0 = xRight;
      SAT32(rightEdges[top]);
    }
    else
      top++;
  }
  else
    top = 0;

  for (i = 0; i < 2; i++) {
    mlib_d64 dY1 = coords[(topIdx - i) & 0x3][1];
    mlib_d64 dX1 = coords[(topIdx - i) & 0x3][0];
    mlib_d64 dY2 = coords[(topIdx - i - 1) & 0x3][1];
    mlib_d64 dX2 = coords[(topIdx - i - 1) & 0x3][0];
    mlib_d64 x = dX1, slope = (dX2 - dX1) / (dY2 - dY1);
    mlib_s32 y1;
    mlib_s32 y2;

    if (dY1 == dY2)
      continue;

    if (!(IS_FINITE(slope))) {
      continue;
    }

    if (dY1 < 0.0)
      y1 = 0;
    else {
      val0 = dY1 + 1;
      SAT32(y1);
    }

    val0 = dY2;
    SAT32(y2);

    if (y2 >= dstHeight)
      y2 = (mlib_s32) (dstHeight - 1);

    x += slope * (y1 - dY1);
    for (j = y1; j <= y2; j++) {
      val0 = x;
      SAT32(t);
      leftEdges[j] = (t >= x) ? t : ++t;

      if (x >= MLIB_S32_MAX)
        leftEdges[j] = MLIB_S32_MAX;
      x += slope;
    }
  }

  for (i = 0; i < 2; i++) {
    mlib_d64 dY1 = coords[(topIdx + i) & 0x3][1];
    mlib_d64 dX1 = coords[(topIdx + i) & 0x3][0];
    mlib_d64 dY2 = coords[(topIdx + i + 1) & 0x3][1];
    mlib_d64 dX2 = coords[(topIdx + i + 1) & 0x3][0];
    mlib_d64 x = dX1, slope = (dX2 - dX1) / (dY2 - dY1);
    mlib_s32 y1;
    mlib_s32 y2;

    if (dY1 == dY2)
      continue;

    if (!(IS_FINITE(slope))) {
      continue;
    }

    if (dY1 < 0.0)
      y1 = 0;
    else {
      val0 = dY1 + 1;
      SAT32(y1);
    }

    val0 = dY2;
    SAT32(y2);

    if (y2 >= dstHeight)
      y2 = (mlib_s32) (dstHeight - 1);

    x += slope * (y1 - dY1);
    for (j = y1; j <= y2; j++) {
      val0 = x;
      SAT32(rightEdges[j]);
      x += slope;
    }

    bot = y2;
  }

  {
    mlib_d64 dxCl = xClip * div;
    mlib_d64 dyCl = yClip * div;
    mlib_d64 dwCl = wClip * div;
    mlib_d64 dhCl = hClip * div;

    mlib_s32 xCl = (mlib_s32) (xClip + delta);
    mlib_s32 yCl = (mlib_s32) (yClip + delta);
    mlib_s32 wCl = (mlib_s32) (wClip + delta);
    mlib_s32 hCl = (mlib_s32) (hClip + delta);

    /*
     * mlib_s32 xCl = (mlib_s32)(xClip + delta);
     * mlib_s32 yCl = (mlib_s32)(yClip + delta);
     * mlib_s32 wCl = (mlib_s32)(wClip);
     * mlib_s32 hCl = (mlib_s32)(hClip);
     */

    if (edge == MLIB_EDGE_SRC_PADDED) {
      xCl = kw1;
      yCl = kh1;
      wCl = (mlib_s32) (srcWidth - ((kw - 1) - kw1));
      hCl = (mlib_s32) (srcHeight - ((kh - 1) - kh1));
    }

    div = 1.0 / div;

    sdx = (mlib_s32) (a2 * div * (1 << shiftx));
    sdy = (mlib_s32) (c2 * div * (1 << shifty));

    if (div > 0) {

      for (i = top; i <= bot; i++) {
        mlib_s32 xLeft = leftEdges[i];
        mlib_s32 xRight = rightEdges[i];
        mlib_s32 xs, ys, x_e, y_e, x_s, y_s;
        mlib_d64 dxs, dys, dxe, dye;
        mlib_d64 xl, ii, xr;

        xLeft = (xLeft < 0) ? 0 : xLeft;
        xRight = (xRight >= dstWidth) ? (mlib_s32) (dstWidth - 1) : xRight;

        xl = xLeft + 0.5;
        ii = i + 0.5;
        xr = xRight + 0.5;
        dxs = xl * a2 + ii * b2 + tx2;
        dys = xl * c2 + ii * d2 + ty2;

        if ((dxs < dxCl) || (dxs >= dwCl) || (dys < dyCl) || (dys >= dhCl)) {
          dxs += dx;
          dys += dy;
          if (xLeft < MLIB_S32_MAX) {
              xLeft++;
          }

          if ((dxs < dxCl) || (dxs >= dwCl) || (dys < dyCl) || (dys >= dhCl))
            xRight = -1;
        }

        dxe = xr * a2 + ii * b2 + tx2;
        dye = xr * c2 + ii * d2 + ty2;

        if ((dxe < dxCl) || (dxe >= dwCl) || (dye < dyCl) || (dye >= dhCl)) {
          dxe -= dx;
          dye -= dy;
          if (xRight > MLIB_S32_MIN) {
              xRight--;
          }

          if ((dxe < dxCl) || (dxe >= dwCl) || (dye < dyCl) || (dye >= dhCl))
            xRight = -1;
        }

        xs = (mlib_s32) ((dxs * div + delta) * (1 << shiftx));
        x_s = xs >> shiftx;

        ys = (mlib_s32) ((dys * div + delta) * (1 << shifty));
        y_s = ys >> shifty;

        if (x_s < xCl)
          xs = (xCl << shiftx);
        else if (x_s >= wCl)
          xs = ((wCl << shiftx) - 1);

        if (y_s < yCl)
          ys = (yCl << shifty);
        else if (y_s >= hCl)
          ys = ((hCl << shifty) - 1);

        if (xRight >= xLeft) {
          x_e = ((xRight - xLeft) * sdx + xs) >> shiftx;
          y_e = ((xRight - xLeft) * sdy + ys) >> shifty;

          if ((x_e < xCl) || (x_e >= wCl)) {
            if (sdx > 0)
              sdx -= 1;
            else
              sdx += 1;
          }

          if ((y_e < yCl) || (y_e >= hCl)) {
            if (sdy > 0)
              sdy -= 1;
            else
              sdy += 1;
          }
        }

        leftEdges[i] = xLeft;
        rightEdges[i] = xRight;
        xStarts[i] = xs;
        yStarts[i] = ys;

        if ((xRight - xLeft + 1) > max_xsize)
          max_xsize = (xRight - xLeft + 1);
      }
    }
    else {

      for (i = top; i <= bot; i++) {
        mlib_s32 xLeft = leftEdges[i];
        mlib_s32 xRight = rightEdges[i];
        mlib_s32 xs, ys, x_e, y_e, x_s, y_s;
        mlib_d64 dxs, dys, dxe, dye;
        mlib_d64 xl, ii, xr;

        xLeft = (xLeft < 0) ? 0 : xLeft;
        xRight = (xRight >= dstWidth) ? (mlib_s32) (dstWidth - 1) : xRight;

        xl = xLeft + 0.5;
        ii = i + 0.5;
        xr = xRight + 0.5;
        dxs = xl * a2 + ii * b2 + tx2;
        dys = xl * c2 + ii * d2 + ty2;

        if ((dxs > dxCl) || (dxs <= dwCl) || (dys > dyCl) || (dys <= dhCl)) {
          dxs += dx;
          dys += dy;
          if (xLeft < MLIB_S32_MAX) {
              xLeft++;
          }

          if ((dxs > dxCl) || (dxs <= dwCl) || (dys > dyCl) || (dys <= dhCl))
            xRight = -1;
        }

        dxe = xr * a2 + ii * b2 + tx2;
        dye = xr * c2 + ii * d2 + ty2;

        if ((dxe > dxCl) || (dxe <= dwCl) || (dye > dyCl) || (dye <= dhCl)) {
          dxe -= dx;
          dye -= dy;
          if (xRight > MLIB_S32_MIN) {
              xRight--;
          }

          if ((dxe > dxCl) || (dxe <= dwCl) || (dye > dyCl) || (dye <= dhCl))
            xRight = -1;
        }

        xs = (mlib_s32) ((dxs * div + delta) * (1 << shiftx));
        x_s = xs >> shiftx;

        if (x_s < xCl)
          xs = (xCl << shiftx);
        else if (x_s >= wCl)
          xs = ((wCl << shiftx) - 1);

        ys = (mlib_s32) ((dys * div + delta) * (1 << shifty));
        y_s = ys >> shifty;

        if (y_s < yCl)
          ys = (yCl << shifty);
        else if (y_s >= hCl)
          ys = ((hCl << shifty) - 1);

        if (xRight >= xLeft) {
          x_e = ((xRight - xLeft) * sdx + xs) >> shiftx;
          y_e = ((xRight - xLeft) * sdy + ys) >> shifty;

          if ((x_e < xCl) || (x_e >= wCl)) {
            if (sdx > 0)
              sdx -= 1;
            else
              sdx += 1;
          }

          if ((y_e < yCl) || (y_e >= hCl)) {
            if (sdy > 0)
              sdy -= 1;
            else
              sdy += 1;
          }
        }

        leftEdges[i] = xLeft;
        rightEdges[i] = xRight;
        xStarts[i] = xs;
        yStarts[i] = ys;

        if ((xRight - xLeft + 1) > max_xsize)
          max_xsize = (xRight - xLeft + 1);
      }
    }
  }

  while (leftEdges[top] > rightEdges[top] && top <= bot)
    top++;

  if (top < bot)
    while (leftEdges[bot] > rightEdges[bot])
      bot--;

  yStart = top;
  yFinish = bot;
  dX = sdx;
  dY = sdy;

  dstData += (yStart - 1) * dstYStride;

  STORE_PARAM(param, dstData);
  STORE_PARAM(param, yStart);
  STORE_PARAM(param, yFinish);
  STORE_PARAM(param, max_xsize);
  STORE_PARAM(param, dX);
  STORE_PARAM(param, dY);

  return MLIB_SUCCESS;
}

/***************************************************************/
