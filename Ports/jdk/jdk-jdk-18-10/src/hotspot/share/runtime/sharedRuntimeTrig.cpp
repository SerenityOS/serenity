/*
 * Copyright (c) 2001, 2017, Oracle and/or its affiliates. All rights reserved.
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
#include "jni.h"
#include "runtime/interfaceSupport.inline.hpp"
#include "runtime/sharedRuntime.hpp"
#include "runtime/sharedRuntimeMath.hpp"

// This file contains copies of the fdlibm routines used by
// StrictMath. It turns out that it is almost always required to use
// these runtime routines; the Intel CPU doesn't meet the Java
// specification for sin/cos outside a certain limited argument range,
// and the SPARC CPU doesn't appear to have sin/cos instructions. It
// also turns out that avoiding the indirect call through function
// pointer out to libjava.so in SharedRuntime speeds these routines up
// by roughly 15% on both Win32/x86 and Solaris/SPARC.

/*
 * __kernel_rem_pio2(x,y,e0,nx,prec,ipio2)
 * double x[],y[]; int e0,nx,prec; int ipio2[];
 *
 * __kernel_rem_pio2 return the last three digits of N with
 *              y = x - N*pi/2
 * so that |y| < pi/2.
 *
 * The method is to compute the integer (mod 8) and fraction parts of
 * (2/pi)*x without doing the full multiplication. In general we
 * skip the part of the product that are known to be a huge integer (
 * more accurately, = 0 mod 8 ). Thus the number of operations are
 * independent of the exponent of the input.
 *
 * (2/pi) is represented by an array of 24-bit integers in ipio2[].
 *
 * Input parameters:
 *      x[]     The input value (must be positive) is broken into nx
 *              pieces of 24-bit integers in double precision format.
 *              x[i] will be the i-th 24 bit of x. The scaled exponent
 *              of x[0] is given in input parameter e0 (i.e., x[0]*2^e0
 *              match x's up to 24 bits.
 *
 *              Example of breaking a double positive z into x[0]+x[1]+x[2]:
 *                      e0 = ilogb(z)-23
 *                      z  = scalbn(z,-e0)
 *              for i = 0,1,2
 *                      x[i] = floor(z)
 *                      z    = (z-x[i])*2**24
 *
 *
 *      y[]     ouput result in an array of double precision numbers.
 *              The dimension of y[] is:
 *                      24-bit  precision       1
 *                      53-bit  precision       2
 *                      64-bit  precision       2
 *                      113-bit precision       3
 *              The actual value is the sum of them. Thus for 113-bit
 *              precsion, one may have to do something like:
 *
 *              long double t,w,r_head, r_tail;
 *              t = (long double)y[2] + (long double)y[1];
 *              w = (long double)y[0];
 *              r_head = t+w;
 *              r_tail = w - (r_head - t);
 *
 *      e0      The exponent of x[0]
 *
 *      nx      dimension of x[]
 *
 *      prec    an interger indicating the precision:
 *                      0       24  bits (single)
 *                      1       53  bits (double)
 *                      2       64  bits (extended)
 *                      3       113 bits (quad)
 *
 *      ipio2[]
 *              integer array, contains the (24*i)-th to (24*i+23)-th
 *              bit of 2/pi after binary point. The corresponding
 *              floating value is
 *
 *                      ipio2[i] * 2^(-24(i+1)).
 *
 * External function:
 *      double scalbn(), floor();
 *
 *
 * Here is the description of some local variables:
 *
 *      jk      jk+1 is the initial number of terms of ipio2[] needed
 *              in the computation. The recommended value is 2,3,4,
 *              6 for single, double, extended,and quad.
 *
 *      jz      local integer variable indicating the number of
 *              terms of ipio2[] used.
 *
 *      jx      nx - 1
 *
 *      jv      index for pointing to the suitable ipio2[] for the
 *              computation. In general, we want
 *                      ( 2^e0*x[0] * ipio2[jv-1]*2^(-24jv) )/8
 *              is an integer. Thus
 *                      e0-3-24*jv >= 0 or (e0-3)/24 >= jv
 *              Hence jv = max(0,(e0-3)/24).
 *
 *      jp      jp+1 is the number of terms in PIo2[] needed, jp = jk.
 *
 *      q[]     double array with integral value, representing the
 *              24-bits chunk of the product of x and 2/pi.
 *
 *      q0      the corresponding exponent of q[0]. Note that the
 *              exponent for q[i] would be q0-24*i.
 *
 *      PIo2[]  double precision array, obtained by cutting pi/2
 *              into 24 bits chunks.
 *
 *      f[]     ipio2[] in floating point
 *
 *      iq[]    integer array by breaking up q[] in 24-bits chunk.
 *
 *      fq[]    final product of x*(2/pi) in fq[0],..,fq[jk]
 *
 *      ih      integer. If >0 it indicates q[] is >= 0.5, hence
 *              it also indicates the *sign* of the result.
 *
 */


/*
 * Constants:
 * The hexadecimal values are the intended ones for the following
 * constants. The decimal values may be used, provided that the
 * compiler will convert from decimal to binary accurately enough
 * to produce the hexadecimal values shown.
 */


static const int init_jk[] = {2,3,4,6}; /* initial value for jk */

static const double PIo2[] = {
  1.57079625129699707031e+00, /* 0x3FF921FB, 0x40000000 */
  7.54978941586159635335e-08, /* 0x3E74442D, 0x00000000 */
  5.39030252995776476554e-15, /* 0x3CF84698, 0x80000000 */
  3.28200341580791294123e-22, /* 0x3B78CC51, 0x60000000 */
  1.27065575308067607349e-29, /* 0x39F01B83, 0x80000000 */
  1.22933308981111328932e-36, /* 0x387A2520, 0x40000000 */
  2.73370053816464559624e-44, /* 0x36E38222, 0x80000000 */
  2.16741683877804819444e-51, /* 0x3569F31D, 0x00000000 */
};

static const double
zeroB   = 0.0,
one     = 1.0,
two24B  = 1.67772160000000000000e+07, /* 0x41700000, 0x00000000 */
twon24  = 5.96046447753906250000e-08; /* 0x3E700000, 0x00000000 */

static int __kernel_rem_pio2(double *x, double *y, int e0, int nx, int prec, const int *ipio2) {
  int jz,jx,jv,jp,jk,carry,n,iq[20],i,j,k,m,q0,ih;
  double z,fw,f[20],fq[20],q[20];

  /* initialize jk*/
  jk = init_jk[prec];
  jp = jk;

  /* determine jx,jv,q0, note that 3>q0 */
  jx =  nx-1;
  jv = (e0-3)/24; if(jv<0) jv=0;
  q0 =  e0-24*(jv+1);

  /* set up f[0] to f[jx+jk] where f[jx+jk] = ipio2[jv+jk] */
  j = jv-jx; m = jx+jk;
  for(i=0;i<=m;i++,j++) f[i] = (j<0)? zeroB : (double) ipio2[j];

  /* compute q[0],q[1],...q[jk] */
  for (i=0;i<=jk;i++) {
    for(j=0,fw=0.0;j<=jx;j++) fw += x[j]*f[jx+i-j]; q[i] = fw;
  }

  jz = jk;
recompute:
  /* distill q[] into iq[] reversingly */
  for(i=0,j=jz,z=q[jz];j>0;i++,j--) {
    fw    =  (double)((int)(twon24* z));
    iq[i] =  (int)(z-two24B*fw);
    z     =  q[j-1]+fw;
  }

  /* compute n */
  z  = scalbnA(z,q0);           /* actual value of z */
  z -= 8.0*floor(z*0.125);              /* trim off integer >= 8 */
  n  = (int) z;
  z -= (double)n;
  ih = 0;
  if(q0>0) {    /* need iq[jz-1] to determine n */
    i  = (iq[jz-1]>>(24-q0)); n += i;
    iq[jz-1] -= i<<(24-q0);
    ih = iq[jz-1]>>(23-q0);
  }
  else if(q0==0) ih = iq[jz-1]>>23;
  else if(z>=0.5) ih=2;

  if(ih>0) {    /* q > 0.5 */
    n += 1; carry = 0;
    for(i=0;i<jz ;i++) {        /* compute 1-q */
      j = iq[i];
      if(carry==0) {
        if(j!=0) {
          carry = 1; iq[i] = 0x1000000- j;
        }
      } else  iq[i] = 0xffffff - j;
    }
    if(q0>0) {          /* rare case: chance is 1 in 12 */
      switch(q0) {
      case 1:
        iq[jz-1] &= 0x7fffff; break;
      case 2:
        iq[jz-1] &= 0x3fffff; break;
      }
    }
    if(ih==2) {
      z = one - z;
      if(carry!=0) z -= scalbnA(one,q0);
    }
  }

  /* check if recomputation is needed */
  if(z==zeroB) {
    j = 0;
    for (i=jz-1;i>=jk;i--) j |= iq[i];
    if(j==0) { /* need recomputation */
      for(k=1;iq[jk-k]==0;k++);   /* k = no. of terms needed */

      for(i=jz+1;i<=jz+k;i++) {   /* add q[jz+1] to q[jz+k] */
        f[jx+i] = (double) ipio2[jv+i];
        for(j=0,fw=0.0;j<=jx;j++) fw += x[j]*f[jx+i-j];
        q[i] = fw;
      }
      jz += k;
      goto recompute;
    }
  }

  /* chop off zero terms */
  if(z==0.0) {
    jz -= 1; q0 -= 24;
    while(iq[jz]==0) { jz--; q0-=24;}
  } else { /* break z into 24-bit if necessary */
    z = scalbnA(z,-q0);
    if(z>=two24B) {
      fw = (double)((int)(twon24*z));
      iq[jz] = (int)(z-two24B*fw);
      jz += 1; q0 += 24;
      iq[jz] = (int) fw;
    } else iq[jz] = (int) z ;
  }

  /* convert integer "bit" chunk to floating-point value */
  fw = scalbnA(one,q0);
  for(i=jz;i>=0;i--) {
    q[i] = fw*(double)iq[i]; fw*=twon24;
  }

  /* compute PIo2[0,...,jp]*q[jz,...,0] */
  for(i=jz;i>=0;i--) {
    for(fw=0.0,k=0;k<=jp&&k<=jz-i;k++) fw += PIo2[k]*q[i+k];
    fq[jz-i] = fw;
  }

  /* compress fq[] into y[] */
  switch(prec) {
  case 0:
    fw = 0.0;
    for (i=jz;i>=0;i--) fw += fq[i];
    y[0] = (ih==0)? fw: -fw;
    break;
  case 1:
  case 2:
    fw = 0.0;
    for (i=jz;i>=0;i--) fw += fq[i];
    y[0] = (ih==0)? fw: -fw;
    fw = fq[0]-fw;
    for (i=1;i<=jz;i++) fw += fq[i];
    y[1] = (ih==0)? fw: -fw;
    break;
  case 3:       /* painful */
    for (i=jz;i>0;i--) {
      fw      = fq[i-1]+fq[i];
      fq[i]  += fq[i-1]-fw;
      fq[i-1] = fw;
    }
    for (i=jz;i>1;i--) {
      fw      = fq[i-1]+fq[i];
      fq[i]  += fq[i-1]-fw;
      fq[i-1] = fw;
    }
    for (fw=0.0,i=jz;i>=2;i--) fw += fq[i];
    if(ih==0) {
      y[0] =  fq[0]; y[1] =  fq[1]; y[2] =  fw;
    } else {
      y[0] = -fq[0]; y[1] = -fq[1]; y[2] = -fw;
    }
  }
  return n&7;
}


/*
 * ====================================================
 * Copyright (c) 1993 Oracle and/or its affiliates. All rights reserved.
 *
 * Developed at SunPro, a Sun Microsystems, Inc. business.
 * Permission to use, copy, modify, and distribute this
 * software is freely granted, provided that this notice
 * is preserved.
 * ====================================================
 *
 */

/* __ieee754_rem_pio2(x,y)
 *
 * return the remainder of x rem pi/2 in y[0]+y[1]
 * use __kernel_rem_pio2()
 */

/*
 * Table of constants for 2/pi, 396 Hex digits (476 decimal) of 2/pi
 */
static const int two_over_pi[] = {
  0xA2F983, 0x6E4E44, 0x1529FC, 0x2757D1, 0xF534DD, 0xC0DB62,
  0x95993C, 0x439041, 0xFE5163, 0xABDEBB, 0xC561B7, 0x246E3A,
  0x424DD2, 0xE00649, 0x2EEA09, 0xD1921C, 0xFE1DEB, 0x1CB129,
  0xA73EE8, 0x8235F5, 0x2EBB44, 0x84E99C, 0x7026B4, 0x5F7E41,
  0x3991D6, 0x398353, 0x39F49C, 0x845F8B, 0xBDF928, 0x3B1FF8,
  0x97FFDE, 0x05980F, 0xEF2F11, 0x8B5A0A, 0x6D1F6D, 0x367ECF,
  0x27CB09, 0xB74F46, 0x3F669E, 0x5FEA2D, 0x7527BA, 0xC7EBE5,
  0xF17B3D, 0x0739F7, 0x8A5292, 0xEA6BFB, 0x5FB11F, 0x8D5D08,
  0x560330, 0x46FC7B, 0x6BABF0, 0xCFBC20, 0x9AF436, 0x1DA9E3,
  0x91615E, 0xE61B08, 0x659985, 0x5F14A0, 0x68408D, 0xFFD880,
  0x4D7327, 0x310606, 0x1556CA, 0x73A8C9, 0x60E27B, 0xC08C6B,
};

static const int npio2_hw[] = {
  0x3FF921FB, 0x400921FB, 0x4012D97C, 0x401921FB, 0x401F6A7A, 0x4022D97C,
  0x4025FDBB, 0x402921FB, 0x402C463A, 0x402F6A7A, 0x4031475C, 0x4032D97C,
  0x40346B9C, 0x4035FDBB, 0x40378FDB, 0x403921FB, 0x403AB41B, 0x403C463A,
  0x403DD85A, 0x403F6A7A, 0x40407E4C, 0x4041475C, 0x4042106C, 0x4042D97C,
  0x4043A28C, 0x40446B9C, 0x404534AC, 0x4045FDBB, 0x4046C6CB, 0x40478FDB,
  0x404858EB, 0x404921FB,
};

/*
 * invpio2:  53 bits of 2/pi
 * pio2_1:   first  33 bit of pi/2
 * pio2_1t:  pi/2 - pio2_1
 * pio2_2:   second 33 bit of pi/2
 * pio2_2t:  pi/2 - (pio2_1+pio2_2)
 * pio2_3:   third  33 bit of pi/2
 * pio2_3t:  pi/2 - (pio2_1+pio2_2+pio2_3)
 */

static const double
zeroA =  0.00000000000000000000e+00, /* 0x00000000, 0x00000000 */
half =  5.00000000000000000000e-01, /* 0x3FE00000, 0x00000000 */
two24A =  1.67772160000000000000e+07, /* 0x41700000, 0x00000000 */
invpio2 =  6.36619772367581382433e-01, /* 0x3FE45F30, 0x6DC9C883 */
pio2_1  =  1.57079632673412561417e+00, /* 0x3FF921FB, 0x54400000 */
pio2_1t =  6.07710050650619224932e-11, /* 0x3DD0B461, 0x1A626331 */
pio2_2  =  6.07710050630396597660e-11, /* 0x3DD0B461, 0x1A600000 */
pio2_2t =  2.02226624879595063154e-21, /* 0x3BA3198A, 0x2E037073 */
pio2_3  =  2.02226624871116645580e-21, /* 0x3BA3198A, 0x2E000000 */
pio2_3t =  8.47842766036889956997e-32; /* 0x397B839A, 0x252049C1 */

static int __ieee754_rem_pio2(double x, double *y) {
  double z,w,t,r,fn;
  double tx[3];
  int e0,i,j,nx,n,ix,hx,i0;

  i0 = ((*(int*)&two24A)>>30)^1;        /* high word index */
  hx = *(i0+(int*)&x);          /* high word of x */
  ix = hx&0x7fffffff;
  if(ix<=0x3fe921fb)   /* |x| ~<= pi/4 , no need for reduction */
    {y[0] = x; y[1] = 0; return 0;}
  if(ix<0x4002d97c) {  /* |x| < 3pi/4, special case with n=+-1 */
    if(hx>0) {
      z = x - pio2_1;
      if(ix!=0x3ff921fb) {    /* 33+53 bit pi is good enough */
        y[0] = z - pio2_1t;
        y[1] = (z-y[0])-pio2_1t;
      } else {                /* near pi/2, use 33+33+53 bit pi */
        z -= pio2_2;
        y[0] = z - pio2_2t;
        y[1] = (z-y[0])-pio2_2t;
      }
      return 1;
    } else {    /* negative x */
      z = x + pio2_1;
      if(ix!=0x3ff921fb) {    /* 33+53 bit pi is good enough */
        y[0] = z + pio2_1t;
        y[1] = (z-y[0])+pio2_1t;
      } else {                /* near pi/2, use 33+33+53 bit pi */
        z += pio2_2;
        y[0] = z + pio2_2t;
        y[1] = (z-y[0])+pio2_2t;
      }
      return -1;
    }
  }
  if(ix<=0x413921fb) { /* |x| ~<= 2^19*(pi/2), medium size */
    t  = fabsd(x);
    n  = (int) (t*invpio2+half);
    fn = (double)n;
    r  = t-fn*pio2_1;
    w  = fn*pio2_1t;    /* 1st round good to 85 bit */
    if(n<32&&ix!=npio2_hw[n-1]) {
      y[0] = r-w;       /* quick check no cancellation */
    } else {
      j  = ix>>20;
      y[0] = r-w;
      i = j-(((*(i0+(int*)&y[0]))>>20)&0x7ff);
      if(i>16) {  /* 2nd iteration needed, good to 118 */
        t  = r;
        w  = fn*pio2_2;
        r  = t-w;
        w  = fn*pio2_2t-((t-r)-w);
        y[0] = r-w;
        i = j-(((*(i0+(int*)&y[0]))>>20)&0x7ff);
        if(i>49)  {     /* 3rd iteration need, 151 bits acc */
          t  = r;       /* will cover all possible cases */
          w  = fn*pio2_3;
          r  = t-w;
          w  = fn*pio2_3t-((t-r)-w);
          y[0] = r-w;
        }
      }
    }
    y[1] = (r-y[0])-w;
    if(hx<0)    {y[0] = -y[0]; y[1] = -y[1]; return -n;}
    else         return n;
  }
  /*
   * all other (large) arguments
   */
  if(ix>=0x7ff00000) {          /* x is inf or NaN */
    y[0]=y[1]=x-x; return 0;
  }
  /* set z = scalbn(|x|,ilogb(x)-23) */
  *(1-i0+(int*)&z) = *(1-i0+(int*)&x);
  e0    = (ix>>20)-1046;        /* e0 = ilogb(z)-23; */
  *(i0+(int*)&z) = ix - (e0<<20);
  for(i=0;i<2;i++) {
    tx[i] = (double)((int)(z));
    z     = (z-tx[i])*two24A;
  }
  tx[2] = z;
  nx = 3;
  while(tx[nx-1]==zeroA) nx--;  /* skip zero term */
  n  =  __kernel_rem_pio2(tx,y,e0,nx,2,two_over_pi);
  if(hx<0) {y[0] = -y[0]; y[1] = -y[1]; return -n;}
  return n;
}


/* __kernel_sin( x, y, iy)
 * kernel sin function on [-pi/4, pi/4], pi/4 ~ 0.7854
 * Input x is assumed to be bounded by ~pi/4 in magnitude.
 * Input y is the tail of x.
 * Input iy indicates whether y is 0. (if iy=0, y assume to be 0).
 *
 * Algorithm
 *      1. Since sin(-x) = -sin(x), we need only to consider positive x.
 *      2. if x < 2^-27 (hx<0x3e400000 0), return x with inexact if x!=0.
 *      3. sin(x) is approximated by a polynomial of degree 13 on
 *         [0,pi/4]
 *                               3            13
 *              sin(x) ~ x + S1*x + ... + S6*x
 *         where
 *
 *      |sin(x)         2     4     6     8     10     12  |     -58
 *      |----- - (1+S1*x +S2*x +S3*x +S4*x +S5*x  +S6*x   )| <= 2
 *      |  x                                               |
 *
 *      4. sin(x+y) = sin(x) + sin'(x')*y
 *                  ~ sin(x) + (1-x*x/2)*y
 *         For better accuracy, let
 *                   3      2      2      2      2
 *              r = x *(S2+x *(S3+x *(S4+x *(S5+x *S6))))
 *         then                   3    2
 *              sin(x) = x + (S1*x + (x *(r-y/2)+y))
 */

static const double
S1  = -1.66666666666666324348e-01, /* 0xBFC55555, 0x55555549 */
S2  =  8.33333333332248946124e-03, /* 0x3F811111, 0x1110F8A6 */
S3  = -1.98412698298579493134e-04, /* 0xBF2A01A0, 0x19C161D5 */
S4  =  2.75573137070700676789e-06, /* 0x3EC71DE3, 0x57B1FE7D */
S5  = -2.50507602534068634195e-08, /* 0xBE5AE5E6, 0x8A2B9CEB */
S6  =  1.58969099521155010221e-10; /* 0x3DE5D93A, 0x5ACFD57C */

static double __kernel_sin(double x, double y, int iy)
{
        double z,r,v;
        int ix;
        ix = high(x)&0x7fffffff;                /* high word of x */
        if(ix<0x3e400000)                       /* |x| < 2**-27 */
           {if((int)x==0) return x;}            /* generate inexact */
        z       =  x*x;
        v       =  z*x;
        r       =  S2+z*(S3+z*(S4+z*(S5+z*S6)));
        if(iy==0) return x+v*(S1+z*r);
        else      return x-((z*(half*y-v*r)-y)-v*S1);
}

/*
 * __kernel_cos( x,  y )
 * kernel cos function on [-pi/4, pi/4], pi/4 ~ 0.785398164
 * Input x is assumed to be bounded by ~pi/4 in magnitude.
 * Input y is the tail of x.
 *
 * Algorithm
 *      1. Since cos(-x) = cos(x), we need only to consider positive x.
 *      2. if x < 2^-27 (hx<0x3e400000 0), return 1 with inexact if x!=0.
 *      3. cos(x) is approximated by a polynomial of degree 14 on
 *         [0,pi/4]
 *                                       4            14
 *              cos(x) ~ 1 - x*x/2 + C1*x + ... + C6*x
 *         where the remez error is
 *
 *      |              2     4     6     8     10    12     14 |     -58
 *      |cos(x)-(1-.5*x +C1*x +C2*x +C3*x +C4*x +C5*x  +C6*x  )| <= 2
 *      |                                                      |
 *
 *                     4     6     8     10    12     14
 *      4. let r = C1*x +C2*x +C3*x +C4*x +C5*x  +C6*x  , then
 *             cos(x) = 1 - x*x/2 + r
 *         since cos(x+y) ~ cos(x) - sin(x)*y
 *                        ~ cos(x) - x*y,
 *         a correction term is necessary in cos(x) and hence
 *              cos(x+y) = 1 - (x*x/2 - (r - x*y))
 *         For better accuracy when x > 0.3, let qx = |x|/4 with
 *         the last 32 bits mask off, and if x > 0.78125, let qx = 0.28125.
 *         Then
 *              cos(x+y) = (1-qx) - ((x*x/2-qx) - (r-x*y)).
 *         Note that 1-qx and (x*x/2-qx) is EXACT here, and the
 *         magnitude of the latter is at least a quarter of x*x/2,
 *         thus, reducing the rounding error in the subtraction.
 */

static const double
C1  =  4.16666666666666019037e-02, /* 0x3FA55555, 0x5555554C */
C2  = -1.38888888888741095749e-03, /* 0xBF56C16C, 0x16C15177 */
C3  =  2.48015872894767294178e-05, /* 0x3EFA01A0, 0x19CB1590 */
C4  = -2.75573143513906633035e-07, /* 0xBE927E4F, 0x809C52AD */
C5  =  2.08757232129817482790e-09, /* 0x3E21EE9E, 0xBDB4B1C4 */
C6  = -1.13596475577881948265e-11; /* 0xBDA8FAE9, 0xBE8838D4 */

static double __kernel_cos(double x, double y)
{
  double a,h,z,r,qx=0;
  int ix;
  ix = high(x)&0x7fffffff;              /* ix = |x|'s high word*/
  if(ix<0x3e400000) {                   /* if x < 2**27 */
    if(((int)x)==0) return one;         /* generate inexact */
  }
  z  = x*x;
  r  = z*(C1+z*(C2+z*(C3+z*(C4+z*(C5+z*C6)))));
  if(ix < 0x3FD33333)                   /* if |x| < 0.3 */
    return one - (0.5*z - (z*r - x*y));
  else {
    if(ix > 0x3fe90000) {               /* x > 0.78125 */
      qx = 0.28125;
    } else {
      set_high(&qx, ix-0x00200000); /* x/4 */
      set_low(&qx, 0);
    }
    h = 0.5*z-qx;
    a = one-qx;
    return a - (h - (z*r-x*y));
  }
}

/* __kernel_tan( x, y, k )
 * kernel tan function on [-pi/4, pi/4], pi/4 ~ 0.7854
 * Input x is assumed to be bounded by ~pi/4 in magnitude.
 * Input y is the tail of x.
 * Input k indicates whether tan (if k=1) or
 * -1/tan (if k= -1) is returned.
 *
 * Algorithm
 *      1. Since tan(-x) = -tan(x), we need only to consider positive x.
 *      2. if x < 2^-28 (hx<0x3e300000 0), return x with inexact if x!=0.
 *      3. tan(x) is approximated by a odd polynomial of degree 27 on
 *         [0,0.67434]
 *                               3             27
 *              tan(x) ~ x + T1*x + ... + T13*x
 *         where
 *
 *              |tan(x)         2     4            26   |     -59.2
 *              |----- - (1+T1*x +T2*x +.... +T13*x    )| <= 2
 *              |  x                                    |
 *
 *         Note: tan(x+y) = tan(x) + tan'(x)*y
 *                        ~ tan(x) + (1+x*x)*y
 *         Therefore, for better accuracy in computing tan(x+y), let
 *                   3      2      2       2       2
 *              r = x *(T2+x *(T3+x *(...+x *(T12+x *T13))))
 *         then
 *                                  3    2
 *              tan(x+y) = x + (T1*x + (x *(r+y)+y))
 *
 *      4. For x in [0.67434,pi/4],  let y = pi/4 - x, then
 *              tan(x) = tan(pi/4-y) = (1-tan(y))/(1+tan(y))
 *                     = 1 - 2*(tan(y) - (tan(y)^2)/(1+tan(y)))
 */

static const double
pio4  =  7.85398163397448278999e-01, /* 0x3FE921FB, 0x54442D18 */
pio4lo=  3.06161699786838301793e-17, /* 0x3C81A626, 0x33145C07 */
T[] =  {
  3.33333333333334091986e-01, /* 0x3FD55555, 0x55555563 */
  1.33333333333201242699e-01, /* 0x3FC11111, 0x1110FE7A */
  5.39682539762260521377e-02, /* 0x3FABA1BA, 0x1BB341FE */
  2.18694882948595424599e-02, /* 0x3F9664F4, 0x8406D637 */
  8.86323982359930005737e-03, /* 0x3F8226E3, 0xE96E8493 */
  3.59207910759131235356e-03, /* 0x3F6D6D22, 0xC9560328 */
  1.45620945432529025516e-03, /* 0x3F57DBC8, 0xFEE08315 */
  5.88041240820264096874e-04, /* 0x3F4344D8, 0xF2F26501 */
  2.46463134818469906812e-04, /* 0x3F3026F7, 0x1A8D1068 */
  7.81794442939557092300e-05, /* 0x3F147E88, 0xA03792A6 */
  7.14072491382608190305e-05, /* 0x3F12B80F, 0x32F0A7E9 */
 -1.85586374855275456654e-05, /* 0xBEF375CB, 0xDB605373 */
  2.59073051863633712884e-05, /* 0x3EFB2A70, 0x74BF7AD4 */
};

static double __kernel_tan(double x, double y, int iy)
{
  double z,r,v,w,s;
  int ix,hx;
  hx = high(x);           /* high word of x */
  ix = hx&0x7fffffff;     /* high word of |x| */
  if(ix<0x3e300000) {                     /* x < 2**-28 */
    if((int)x==0) {                       /* generate inexact */
      if (((ix | low(x)) | (iy + 1)) == 0)
        return one / fabsd(x);
      else {
        if (iy == 1)
          return x;
        else {    /* compute -1 / (x+y) carefully */
          double a, t;

          z = w = x + y;
          set_low(&z, 0);
          v = y - (z - x);
          t = a = -one / w;
          set_low(&t, 0);
          s = one + t * z;
          return t + a * (s + t * v);
        }
      }
    }
  }
  if(ix>=0x3FE59428) {                    /* |x|>=0.6744 */
    if(hx<0) {x = -x; y = -y;}
    z = pio4-x;
    w = pio4lo-y;
    x = z+w; y = 0.0;
  }
  z       =  x*x;
  w       =  z*z;
  /* Break x^5*(T[1]+x^2*T[2]+...) into
   *    x^5(T[1]+x^4*T[3]+...+x^20*T[11]) +
   *    x^5(x^2*(T[2]+x^4*T[4]+...+x^22*[T12]))
   */
  r = T[1]+w*(T[3]+w*(T[5]+w*(T[7]+w*(T[9]+w*T[11]))));
  v = z*(T[2]+w*(T[4]+w*(T[6]+w*(T[8]+w*(T[10]+w*T[12])))));
  s = z*x;
  r = y + z*(s*(r+v)+y);
  r += T[0]*s;
  w = x+r;
  if(ix>=0x3FE59428) {
    v = (double)iy;
    return (double)(1-((hx>>30)&2))*(v-2.0*(x-(w*w/(w+v)-r)));
  }
  if(iy==1) return w;
  else {          /* if allow error up to 2 ulp,
                     simply return -1.0/(x+r) here */
    /*  compute -1.0/(x+r) accurately */
    double a,t;
    z  = w;
    set_low(&z, 0);
    v  = r-(z - x);     /* z+v = r+x */
    t = a  = -1.0/w;    /* a = -1.0/w */
    set_low(&t, 0);
    s  = 1.0+t*z;
    return t+a*(s+t*v);
  }
}


//----------------------------------------------------------------------
//
// Routines for new sin/cos implementation
//
//----------------------------------------------------------------------

/* sin(x)
 * Return sine function of x.
 *
 * kernel function:
 *      __kernel_sin            ... sine function on [-pi/4,pi/4]
 *      __kernel_cos            ... cose function on [-pi/4,pi/4]
 *      __ieee754_rem_pio2      ... argument reduction routine
 *
 * Method.
 *      Let S,C and T denote the sin, cos and tan respectively on
 *      [-PI/4, +PI/4]. Reduce the argument x to y1+y2 = x-k*pi/2
 *      in [-pi/4 , +pi/4], and let n = k mod 4.
 *      We have
 *
 *          n        sin(x)      cos(x)        tan(x)
 *     ----------------------------------------------------------
 *          0          S           C             T
 *          1          C          -S            -1/T
 *          2         -S          -C             T
 *          3         -C           S            -1/T
 *     ----------------------------------------------------------
 *
 * Special cases:
 *      Let trig be any of sin, cos, or tan.
 *      trig(+-INF)  is NaN, with signals;
 *      trig(NaN)    is that NaN;
 *
 * Accuracy:
 *      TRIG(x) returns trig(x) nearly rounded
 */

JRT_LEAF(jdouble, SharedRuntime::dsin(jdouble x))
  double y[2],z=0.0;
  int n, ix;

  /* High word of x. */
  ix = high(x);

  /* |x| ~< pi/4 */
  ix &= 0x7fffffff;
  if(ix <= 0x3fe921fb) return __kernel_sin(x,z,0);

  /* sin(Inf or NaN) is NaN */
  else if (ix>=0x7ff00000) return x-x;

  /* argument reduction needed */
  else {
    n = __ieee754_rem_pio2(x,y);
    switch(n&3) {
    case 0: return  __kernel_sin(y[0],y[1],1);
    case 1: return  __kernel_cos(y[0],y[1]);
    case 2: return -__kernel_sin(y[0],y[1],1);
    default:
      return -__kernel_cos(y[0],y[1]);
    }
  }
JRT_END

/* cos(x)
 * Return cosine function of x.
 *
 * kernel function:
 *      __kernel_sin            ... sine function on [-pi/4,pi/4]
 *      __kernel_cos            ... cosine function on [-pi/4,pi/4]
 *      __ieee754_rem_pio2      ... argument reduction routine
 *
 * Method.
 *      Let S,C and T denote the sin, cos and tan respectively on
 *      [-PI/4, +PI/4]. Reduce the argument x to y1+y2 = x-k*pi/2
 *      in [-pi/4 , +pi/4], and let n = k mod 4.
 *      We have
 *
 *          n        sin(x)      cos(x)        tan(x)
 *     ----------------------------------------------------------
 *          0          S           C             T
 *          1          C          -S            -1/T
 *          2         -S          -C             T
 *          3         -C           S            -1/T
 *     ----------------------------------------------------------
 *
 * Special cases:
 *      Let trig be any of sin, cos, or tan.
 *      trig(+-INF)  is NaN, with signals;
 *      trig(NaN)    is that NaN;
 *
 * Accuracy:
 *      TRIG(x) returns trig(x) nearly rounded
 */

JRT_LEAF(jdouble, SharedRuntime::dcos(jdouble x))
  double y[2],z=0.0;
  int n, ix;

  /* High word of x. */
  ix = high(x);

  /* |x| ~< pi/4 */
  ix &= 0x7fffffff;
  if(ix <= 0x3fe921fb) return __kernel_cos(x,z);

  /* cos(Inf or NaN) is NaN */
  else if (ix>=0x7ff00000) return x-x;

  /* argument reduction needed */
  else {
    n = __ieee754_rem_pio2(x,y);
    switch(n&3) {
    case 0: return  __kernel_cos(y[0],y[1]);
    case 1: return -__kernel_sin(y[0],y[1],1);
    case 2: return -__kernel_cos(y[0],y[1]);
    default:
      return  __kernel_sin(y[0],y[1],1);
    }
  }
JRT_END

/* tan(x)
 * Return tangent function of x.
 *
 * kernel function:
 *      __kernel_tan            ... tangent function on [-pi/4,pi/4]
 *      __ieee754_rem_pio2      ... argument reduction routine
 *
 * Method.
 *      Let S,C and T denote the sin, cos and tan respectively on
 *      [-PI/4, +PI/4]. Reduce the argument x to y1+y2 = x-k*pi/2
 *      in [-pi/4 , +pi/4], and let n = k mod 4.
 *      We have
 *
 *          n        sin(x)      cos(x)        tan(x)
 *     ----------------------------------------------------------
 *          0          S           C             T
 *          1          C          -S            -1/T
 *          2         -S          -C             T
 *          3         -C           S            -1/T
 *     ----------------------------------------------------------
 *
 * Special cases:
 *      Let trig be any of sin, cos, or tan.
 *      trig(+-INF)  is NaN, with signals;
 *      trig(NaN)    is that NaN;
 *
 * Accuracy:
 *      TRIG(x) returns trig(x) nearly rounded
 */

JRT_LEAF(jdouble, SharedRuntime::dtan(jdouble x))
  double y[2],z=0.0;
  int n, ix;

  /* High word of x. */
  ix = high(x);

  /* |x| ~< pi/4 */
  ix &= 0x7fffffff;
  if(ix <= 0x3fe921fb) return __kernel_tan(x,z,1);

  /* tan(Inf or NaN) is NaN */
  else if (ix>=0x7ff00000) return x-x;            /* NaN */

  /* argument reduction needed */
  else {
    n = __ieee754_rem_pio2(x,y);
    return __kernel_tan(y[0],y[1],1-((n&1)<<1)); /*   1 -- n even
                                                     -1 -- n odd */
  }
JRT_END
