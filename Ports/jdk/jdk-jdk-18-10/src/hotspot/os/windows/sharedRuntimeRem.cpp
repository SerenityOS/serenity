/*
* Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
* DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
*
* This code is free software; you can redistribute it and/or modify it
* under the terms of the GNU General Public License version 2 only, as
* published by the Free Software Foundation.
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
*
*/

#include "precompiled.hpp"
#include "runtime/sharedRuntime.hpp"

#ifdef _WIN64
// These are copied defines from fdlibm.h, this allows us to keep the code
// the same as in the JDK, for easier maintenance.

#define __HI(x) *(1+(int*)&x)
#define __LO(x) *(int*)&x

// This code is a copy of __ieee754_fmod() from the JDK's libfdlibm and is
// used as a workaround for issues with the Windows x64 CRT implementation
// of fmod. Microsoft has acknowledged that this is an issue in Visual Studio
// 2012 and forward, but has not provided a time frame for a fix other than that
// it'll not be fixed in Visual Studio 2013 or 2015.

static const double one = 1.0, Zero[] = { 0.0, -0.0, };

double SharedRuntime::fmod_winx64(double x, double y)
{
  int n, hx, hy, hz, ix, iy, sx, i;
  unsigned lx, ly, lz;

  hx = __HI(x);           /* high word of x */
  lx = __LO(x);           /* low  word of x */
  hy = __HI(y);           /* high word of y */
  ly = __LO(y);           /* low  word of y */
  sx = hx & 0x80000000;             /* sign of x */
  hx ^= sx;                /* |x| */
  hy &= 0x7fffffff;       /* |y| */

#pragma warning( disable : 4146 )
  /* purge off exception values */
  if ((hy | ly) == 0 || (hx >= 0x7ff00000) ||       /* y=0,or x not finite */
    ((hy | ((ly | -ly) >> 31))>0x7ff00000))     /* or y is NaN */
#pragma warning( default : 4146 )
    return (x*y) / (x*y);
  if (hx <= hy) {
    if ((hx<hy) || (lx<ly)) return x;      /* |x|<|y| return x */
    if (lx == ly)
      return Zero[(unsigned)sx >> 31];  /* |x|=|y| return x*0*/
  }

  /* determine ix = ilogb(x) */
  if (hx<0x00100000) {     /* subnormal x */
    if (hx == 0) {
      for (ix = -1043, i = lx; i>0; i <<= 1) ix -= 1;
    }
    else {
      for (ix = -1022, i = (hx << 11); i>0; i <<= 1) ix -= 1;
    }
  }
  else ix = (hx >> 20) - 1023;

  /* determine iy = ilogb(y) */
  if (hy<0x00100000) {     /* subnormal y */
    if (hy == 0) {
      for (iy = -1043, i = ly; i>0; i <<= 1) iy -= 1;
    }
    else {
      for (iy = -1022, i = (hy << 11); i>0; i <<= 1) iy -= 1;
    }
  }
  else iy = (hy >> 20) - 1023;

  /* set up {hx,lx}, {hy,ly} and align y to x */
  if (ix >= -1022)
    hx = 0x00100000 | (0x000fffff & hx);
  else {          /* subnormal x, shift x to normal */
    n = -1022 - ix;
    if (n <= 31) {
      hx = (hx << n) | (lx >> (32 - n));
      lx <<= n;
    }
    else {
      hx = lx << (n - 32);
      lx = 0;
    }
  }
  if (iy >= -1022)
    hy = 0x00100000 | (0x000fffff & hy);
  else {          /* subnormal y, shift y to normal */
    n = -1022 - iy;
    if (n <= 31) {
      hy = (hy << n) | (ly >> (32 - n));
      ly <<= n;
    }
    else {
      hy = ly << (n - 32);
      ly = 0;
    }
  }

  /* fix point fmod */
  n = ix - iy;
  while (n--) {
    hz = hx - hy; lz = lx - ly; if (lx<ly) hz -= 1;
    if (hz<0){ hx = hx + hx + (lx >> 31); lx = lx + lx; }
    else {
      if ((hz | lz) == 0)          /* return sign(x)*0 */
        return Zero[(unsigned)sx >> 31];
      hx = hz + hz + (lz >> 31); lx = lz + lz;
    }
  }
  hz = hx - hy; lz = lx - ly; if (lx<ly) hz -= 1;
  if (hz >= 0) { hx = hz; lx = lz; }

  /* convert back to floating value and restore the sign */
  if ((hx | lx) == 0)                  /* return sign(x)*0 */
    return Zero[(unsigned)sx >> 31];
  while (hx<0x00100000) {          /* normalize x */
    hx = hx + hx + (lx >> 31); lx = lx + lx;
    iy -= 1;
  }
  if (iy >= -1022) {        /* normalize output */
    hx = ((hx - 0x00100000) | ((iy + 1023) << 20));
    __HI(x) = hx | sx;
    __LO(x) = lx;
  }
  else {                /* subnormal output */
    n = -1022 - iy;
    if (n <= 20) {
      lx = (lx >> n) | ((unsigned)hx << (32 - n));
      hx >>= n;
    }
    else if (n <= 31) {
      lx = (hx << (32 - n)) | (lx >> n); hx = sx;
    }
    else {
      lx = hx >> (n - 32); hx = sx;
    }
    __HI(x) = hx | sx;
    __LO(x) = lx;
    x *= one;           /* create necessary signal */
  }
  return x;               /* exact output */
}

#endif
