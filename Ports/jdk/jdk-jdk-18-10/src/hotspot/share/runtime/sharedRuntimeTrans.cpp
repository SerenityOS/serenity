/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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

// This file contains copies of the fdlibm routines used by
// StrictMath. It turns out that it is almost always required to use
// these runtime routines; the Intel CPU doesn't meet the Java
// specification for sin/cos outside a certain limited argument range,
// and the SPARC CPU doesn't appear to have sin/cos instructions. It
// also turns out that avoiding the indirect call through function
// pointer out to libjava.so in SharedRuntime speeds these routines up
// by roughly 15% on both Win32/x86 and Solaris/SPARC.

// Enabling optimizations in this file causes incorrect code to be
// generated; can not figure out how to turn down optimization for one
// file in the IDE on Windows
#ifdef WIN32
# pragma warning( disable: 4748 ) // /GS can not protect parameters and local variables from local buffer overrun because optimizations are disabled in function
# pragma optimize ( "", off )
#endif

#include "runtime/sharedRuntimeMath.hpp"

/* __ieee754_log(x)
 * Return the logarithm of x
 *
 * Method :
 *   1. Argument Reduction: find k and f such that
 *                    x = 2^k * (1+f),
 *       where  sqrt(2)/2 < 1+f < sqrt(2) .
 *
 *   2. Approximation of log(1+f).
 *    Let s = f/(2+f) ; based on log(1+f) = log(1+s) - log(1-s)
 *             = 2s + 2/3 s**3 + 2/5 s**5 + .....,
 *             = 2s + s*R
 *      We use a special Reme algorithm on [0,0.1716] to generate
 *    a polynomial of degree 14 to approximate R The maximum error
 *    of this polynomial approximation is bounded by 2**-58.45. In
 *    other words,
 *                    2      4      6      8      10      12      14
 *        R(z) ~ Lg1*s +Lg2*s +Lg3*s +Lg4*s +Lg5*s  +Lg6*s  +Lg7*s
 *    (the values of Lg1 to Lg7 are listed in the program)
 *    and
 *        |      2          14          |     -58.45
 *        | Lg1*s +...+Lg7*s    -  R(z) | <= 2
 *        |                             |
 *    Note that 2s = f - s*f = f - hfsq + s*hfsq, where hfsq = f*f/2.
 *    In order to guarantee error in log below 1ulp, we compute log
 *    by
 *            log(1+f) = f - s*(f - R)        (if f is not too large)
 *            log(1+f) = f - (hfsq - s*(hfsq+R)).     (better accuracy)
 *
 *    3. Finally,  log(x) = k*ln2 + log(1+f).
 *                        = k*ln2_hi+(f-(hfsq-(s*(hfsq+R)+k*ln2_lo)))
 *       Here ln2 is split into two floating point number:
 *                    ln2_hi + ln2_lo,
 *       where n*ln2_hi is always exact for |n| < 2000.
 *
 * Special cases:
 *    log(x) is NaN with signal if x < 0 (including -INF) ;
 *    log(+INF) is +INF; log(0) is -INF with signal;
 *    log(NaN) is that NaN with no signal.
 *
 * Accuracy:
 *    according to an error analysis, the error is always less than
 *    1 ulp (unit in the last place).
 *
 * Constants:
 * The hexadecimal values are the intended ones for the following
 * constants. The decimal values may be used, provided that the
 * compiler will convert from decimal to binary accurately enough
 * to produce the hexadecimal values shown.
 */

static const double
ln2_hi  =  6.93147180369123816490e-01,        /* 3fe62e42 fee00000 */
  ln2_lo  =  1.90821492927058770002e-10,        /* 3dea39ef 35793c76 */
  Lg1 = 6.666666666666735130e-01,  /* 3FE55555 55555593 */
  Lg2 = 3.999999999940941908e-01,  /* 3FD99999 9997FA04 */
  Lg3 = 2.857142874366239149e-01,  /* 3FD24924 94229359 */
  Lg4 = 2.222219843214978396e-01,  /* 3FCC71C5 1D8E78AF */
  Lg5 = 1.818357216161805012e-01,  /* 3FC74664 96CB03DE */
  Lg6 = 1.531383769920937332e-01,  /* 3FC39A09 D078C69F */
  Lg7 = 1.479819860511658591e-01;  /* 3FC2F112 DF3E5244 */

static double zero = 0.0;

static double __ieee754_log(double x) {
  double hfsq,f,s,z,R,w,t1,t2,dk;
  int k,hx,i,j;
  unsigned lx;

  hx = high(x);               /* high word of x */
  lx = low(x);                /* low  word of x */

  k=0;
  if (hx < 0x00100000) {                   /* x < 2**-1022  */
    if (((hx&0x7fffffff)|lx)==0)
      return -two54/zero;             /* log(+-0)=-inf */
    if (hx<0) return (x-x)/zero;   /* log(-#) = NaN */
    k -= 54; x *= two54; /* subnormal number, scale up x */
    hx = high(x);             /* high word of x */
  }
  if (hx >= 0x7ff00000) return x+x;
  k += (hx>>20)-1023;
  hx &= 0x000fffff;
  i = (hx+0x95f64)&0x100000;
  set_high(&x, hx|(i^0x3ff00000)); /* normalize x or x/2 */
  k += (i>>20);
  f = x-1.0;
  if((0x000fffff&(2+hx))<3) {  /* |f| < 2**-20 */
    if(f==zero) {
      if (k==0) return zero;
      else {dk=(double)k; return dk*ln2_hi+dk*ln2_lo;}
    }
    R = f*f*(0.5-0.33333333333333333*f);
    if(k==0) return f-R; else {dk=(double)k;
    return dk*ln2_hi-((R-dk*ln2_lo)-f);}
  }
  s = f/(2.0+f);
  dk = (double)k;
  z = s*s;
  i = hx-0x6147a;
  w = z*z;
  j = 0x6b851-hx;
  t1= w*(Lg2+w*(Lg4+w*Lg6));
  t2= z*(Lg1+w*(Lg3+w*(Lg5+w*Lg7)));
  i |= j;
  R = t2+t1;
  if(i>0) {
    hfsq=0.5*f*f;
    if(k==0) return f-(hfsq-s*(hfsq+R)); else
      return dk*ln2_hi-((hfsq-(s*(hfsq+R)+dk*ln2_lo))-f);
  } else {
    if(k==0) return f-s*(f-R); else
      return dk*ln2_hi-((s*(f-R)-dk*ln2_lo)-f);
  }
}

JRT_LEAF(jdouble, SharedRuntime::dlog(jdouble x))
  return __ieee754_log(x);
JRT_END

/* __ieee754_log10(x)
 * Return the base 10 logarithm of x
 *
 * Method :
 *    Let log10_2hi = leading 40 bits of log10(2) and
 *        log10_2lo = log10(2) - log10_2hi,
 *        ivln10   = 1/log(10) rounded.
 *    Then
 *            n = ilogb(x),
 *            if(n<0)  n = n+1;
 *            x = scalbn(x,-n);
 *            log10(x) := n*log10_2hi + (n*log10_2lo + ivln10*log(x))
 *
 * Note 1:
 *    To guarantee log10(10**n)=n, where 10**n is normal, the rounding
 *    mode must set to Round-to-Nearest.
 * Note 2:
 *    [1/log(10)] rounded to 53 bits has error  .198   ulps;
 *    log10 is monotonic at all binary break points.
 *
 * Special cases:
 *    log10(x) is NaN with signal if x < 0;
 *    log10(+INF) is +INF with no signal; log10(0) is -INF with signal;
 *    log10(NaN) is that NaN with no signal;
 *    log10(10**N) = N  for N=0,1,...,22.
 *
 * Constants:
 * The hexadecimal values are the intended ones for the following constants.
 * The decimal values may be used, provided that the compiler will convert
 * from decimal to binary accurately enough to produce the hexadecimal values
 * shown.
 */

static const double
ivln10     =  4.34294481903251816668e-01, /* 0x3FDBCB7B, 0x1526E50E */
  log10_2hi  =  3.01029995663611771306e-01, /* 0x3FD34413, 0x509F6000 */
  log10_2lo  =  3.69423907715893078616e-13; /* 0x3D59FEF3, 0x11F12B36 */

static double __ieee754_log10(double x) {
  double y,z;
  int i,k,hx;
  unsigned lx;

  hx = high(x);       /* high word of x */
  lx = low(x);        /* low word of x */

  k=0;
  if (hx < 0x00100000) {                  /* x < 2**-1022  */
    if (((hx&0x7fffffff)|lx)==0)
      return -two54/zero;             /* log(+-0)=-inf */
    if (hx<0) return (x-x)/zero;        /* log(-#) = NaN */
    k -= 54; x *= two54; /* subnormal number, scale up x */
    hx = high(x);                /* high word of x */
  }
  if (hx >= 0x7ff00000) return x+x;
  k += (hx>>20)-1023;
  i  = ((unsigned)k&0x80000000)>>31;
  hx = (hx&0x000fffff)|((0x3ff-i)<<20);
  y  = (double)(k+i);
  set_high(&x, hx);
  z  = y*log10_2lo + ivln10*__ieee754_log(x);
  return  z+y*log10_2hi;
}

JRT_LEAF(jdouble, SharedRuntime::dlog10(jdouble x))
  return __ieee754_log10(x);
JRT_END


/* __ieee754_exp(x)
 * Returns the exponential of x.
 *
 * Method
 *   1. Argument reduction:
 *      Reduce x to an r so that |r| <= 0.5*ln2 ~ 0.34658.
 *      Given x, find r and integer k such that
 *
 *               x = k*ln2 + r,  |r| <= 0.5*ln2.
 *
 *      Here r will be represented as r = hi-lo for better
 *      accuracy.
 *
 *   2. Approximation of exp(r) by a special rational function on
 *      the interval [0,0.34658]:
 *      Write
 *          R(r**2) = r*(exp(r)+1)/(exp(r)-1) = 2 + r*r/6 - r**4/360 + ...
 *      We use a special Reme algorithm on [0,0.34658] to generate
 *      a polynomial of degree 5 to approximate R. The maximum error
 *      of this polynomial approximation is bounded by 2**-59. In
 *      other words,
 *          R(z) ~ 2.0 + P1*z + P2*z**2 + P3*z**3 + P4*z**4 + P5*z**5
 *      (where z=r*r, and the values of P1 to P5 are listed below)
 *      and
 *          |                  5          |     -59
 *          | 2.0+P1*z+...+P5*z   -  R(z) | <= 2
 *          |                             |
 *      The computation of exp(r) thus becomes
 *                             2*r
 *              exp(r) = 1 + -------
 *                            R - r
 *                                 r*R1(r)
 *                     = 1 + r + ----------- (for better accuracy)
 *                                2 - R1(r)
 *      where
 *                               2       4             10
 *              R1(r) = r - (P1*r  + P2*r  + ... + P5*r   ).
 *
 *   3. Scale back to obtain exp(x):
 *      From step 1, we have
 *         exp(x) = 2^k * exp(r)
 *
 * Special cases:
 *      exp(INF) is INF, exp(NaN) is NaN;
 *      exp(-INF) is 0, and
 *      for finite argument, only exp(0)=1 is exact.
 *
 * Accuracy:
 *      according to an error analysis, the error is always less than
 *      1 ulp (unit in the last place).
 *
 * Misc. info.
 *      For IEEE double
 *          if x >  7.09782712893383973096e+02 then exp(x) overflow
 *          if x < -7.45133219101941108420e+02 then exp(x) underflow
 *
 * Constants:
 * The hexadecimal values are the intended ones for the following
 * constants. The decimal values may be used, provided that the
 * compiler will convert from decimal to binary accurately enough
 * to produce the hexadecimal values shown.
 */

static const double
one     = 1.0,
  halF[2]       = {0.5,-0.5,},
  twom1000= 9.33263618503218878990e-302,     /* 2**-1000=0x01700000,0*/
    o_threshold=  7.09782712893383973096e+02,  /* 0x40862E42, 0xFEFA39EF */
    u_threshold= -7.45133219101941108420e+02,  /* 0xc0874910, 0xD52D3051 */
    ln2HI[2]   ={ 6.93147180369123816490e-01,  /* 0x3fe62e42, 0xfee00000 */
                  -6.93147180369123816490e-01,},/* 0xbfe62e42, 0xfee00000 */
    ln2LO[2]   ={ 1.90821492927058770002e-10,  /* 0x3dea39ef, 0x35793c76 */
                  -1.90821492927058770002e-10,},/* 0xbdea39ef, 0x35793c76 */
      invln2 =  1.44269504088896338700e+00, /* 0x3ff71547, 0x652b82fe */
        P1   =  1.66666666666666019037e-01, /* 0x3FC55555, 0x5555553E */
        P2   = -2.77777777770155933842e-03, /* 0xBF66C16C, 0x16BEBD93 */
        P3   =  6.61375632143793436117e-05, /* 0x3F11566A, 0xAF25DE2C */
        P4   = -1.65339022054652515390e-06, /* 0xBEBBBD41, 0xC5D26BF1 */
        P5   =  4.13813679705723846039e-08; /* 0x3E663769, 0x72BEA4D0 */

static double __ieee754_exp(double x) {
  double y,hi=0,lo=0,c,t;
  int k=0,xsb;
  unsigned hx;

  hx  = high(x);                /* high word of x */
  xsb = (hx>>31)&1;             /* sign bit of x */
  hx &= 0x7fffffff;             /* high word of |x| */

  /* filter out non-finite argument */
  if(hx >= 0x40862E42) {                        /* if |x|>=709.78... */
    if(hx>=0x7ff00000) {
      if(((hx&0xfffff)|low(x))!=0)
        return x+x;             /* NaN */
      else return (xsb==0)? x:0.0;      /* exp(+-inf)={inf,0} */
    }
    if(x > o_threshold) return hugeX*hugeX; /* overflow */
    if(x < u_threshold) return twom1000*twom1000; /* underflow */
  }

  /* argument reduction */
  if(hx > 0x3fd62e42) {         /* if  |x| > 0.5 ln2 */
    if(hx < 0x3FF0A2B2) {       /* and |x| < 1.5 ln2 */
      hi = x-ln2HI[xsb]; lo=ln2LO[xsb]; k = 1-xsb-xsb;
    } else {
      k  = (int)(invln2*x+halF[xsb]);
      t  = k;
      hi = x - t*ln2HI[0];      /* t*ln2HI is exact here */
      lo = t*ln2LO[0];
    }
    x  = hi - lo;
  }
  else if(hx < 0x3e300000)  {   /* when |x|<2**-28 */
    if(hugeX+x>one) return one+x;/* trigger inexact */
  }
  else k = 0;

  /* x is now in primary range */
  t  = x*x;
  c  = x - t*(P1+t*(P2+t*(P3+t*(P4+t*P5))));
  if(k==0)      return one-((x*c)/(c-2.0)-x);
  else          y = one-((lo-(x*c)/(2.0-c))-hi);
  if(k >= -1021) {
    set_high(&y, high(y) + (k<<20)); /* add k to y's exponent */
    return y;
  } else {
    set_high(&y, high(y) + ((k+1000)<<20)); /* add k to y's exponent */
    return y*twom1000;
  }
}

JRT_LEAF(jdouble, SharedRuntime::dexp(jdouble x))
  return __ieee754_exp(x);
JRT_END

/* __ieee754_pow(x,y) return x**y
 *
 *                    n
 * Method:  Let x =  2   * (1+f)
 *      1. Compute and return log2(x) in two pieces:
 *              log2(x) = w1 + w2,
 *         where w1 has 53-24 = 29 bit trailing zeros.
 *      2. Perform y*log2(x) = n+y' by simulating muti-precision
 *         arithmetic, where |y'|<=0.5.
 *      3. Return x**y = 2**n*exp(y'*log2)
 *
 * Special cases:
 *      1.  (anything) ** 0  is 1
 *      2.  (anything) ** 1  is itself
 *      3.  (anything) ** NAN is NAN
 *      4.  NAN ** (anything except 0) is NAN
 *      5.  +-(|x| > 1) **  +INF is +INF
 *      6.  +-(|x| > 1) **  -INF is +0
 *      7.  +-(|x| < 1) **  +INF is +0
 *      8.  +-(|x| < 1) **  -INF is +INF
 *      9.  +-1         ** +-INF is NAN
 *      10. +0 ** (+anything except 0, NAN)               is +0
 *      11. -0 ** (+anything except 0, NAN, odd integer)  is +0
 *      12. +0 ** (-anything except 0, NAN)               is +INF
 *      13. -0 ** (-anything except 0, NAN, odd integer)  is +INF
 *      14. -0 ** (odd integer) = -( +0 ** (odd integer) )
 *      15. +INF ** (+anything except 0,NAN) is +INF
 *      16. +INF ** (-anything except 0,NAN) is +0
 *      17. -INF ** (anything)  = -0 ** (-anything)
 *      18. (-anything) ** (integer) is (-1)**(integer)*(+anything**integer)
 *      19. (-anything except 0 and inf) ** (non-integer) is NAN
 *
 * Accuracy:
 *      pow(x,y) returns x**y nearly rounded. In particular
 *                      pow(integer,integer)
 *      always returns the correct integer provided it is
 *      representable.
 *
 * Constants :
 * The hexadecimal values are the intended ones for the following
 * constants. The decimal values may be used, provided that the
 * compiler will convert from decimal to binary accurately enough
 * to produce the hexadecimal values shown.
 */

static const double
bp[] = {1.0, 1.5,},
  dp_h[] = { 0.0, 5.84962487220764160156e-01,}, /* 0x3FE2B803, 0x40000000 */
    dp_l[] = { 0.0, 1.35003920212974897128e-08,}, /* 0x3E4CFDEB, 0x43CFD006 */
      zeroX    =  0.0,
        two     =  2.0,
        two53   =  9007199254740992.0,  /* 0x43400000, 0x00000000 */
        /* poly coefs for (3/2)*(log(x)-2s-2/3*s**3 */
        L1X  =  5.99999999999994648725e-01, /* 0x3FE33333, 0x33333303 */
        L2X  =  4.28571428578550184252e-01, /* 0x3FDB6DB6, 0xDB6FABFF */
        L3X  =  3.33333329818377432918e-01, /* 0x3FD55555, 0x518F264D */
        L4X  =  2.72728123808534006489e-01, /* 0x3FD17460, 0xA91D4101 */
        L5X  =  2.30660745775561754067e-01, /* 0x3FCD864A, 0x93C9DB65 */
        L6X  =  2.06975017800338417784e-01, /* 0x3FCA7E28, 0x4A454EEF */
        lg2  =  6.93147180559945286227e-01, /* 0x3FE62E42, 0xFEFA39EF */
        lg2_h  =  6.93147182464599609375e-01, /* 0x3FE62E43, 0x00000000 */
        lg2_l  = -1.90465429995776804525e-09, /* 0xBE205C61, 0x0CA86C39 */
        ovt =  8.0085662595372944372e-0017, /* -(1024-log2(ovfl+.5ulp)) */
        cp    =  9.61796693925975554329e-01, /* 0x3FEEC709, 0xDC3A03FD =2/(3ln2) */
        cp_h  =  9.61796700954437255859e-01, /* 0x3FEEC709, 0xE0000000 =(float)cp */
        cp_l  = -7.02846165095275826516e-09, /* 0xBE3E2FE0, 0x145B01F5 =tail of cp_h*/
        ivln2    =  1.44269504088896338700e+00, /* 0x3FF71547, 0x652B82FE =1/ln2 */
        ivln2_h  =  1.44269502162933349609e+00, /* 0x3FF71547, 0x60000000 =24b 1/ln2*/
        ivln2_l  =  1.92596299112661746887e-08; /* 0x3E54AE0B, 0xF85DDF44 =1/ln2 tail*/

double __ieee754_pow(double x, double y) {
  double z,ax,z_h,z_l,p_h,p_l;
  double y1,t1,t2,r,s,t,u,v,w;
  int i0,i1,i,j,k,yisint,n;
  int hx,hy,ix,iy;
  unsigned lx,ly;

  i0 = ((*(int*)&one)>>29)^1; i1=1-i0;
  hx = high(x); lx = low(x);
  hy = high(y); ly = low(y);
  ix = hx&0x7fffffff;  iy = hy&0x7fffffff;

  /* y==zero: x**0 = 1 */
  if((iy|ly)==0) return one;

  /* +-NaN return x+y */
  if(ix > 0x7ff00000 || ((ix==0x7ff00000)&&(lx!=0)) ||
     iy > 0x7ff00000 || ((iy==0x7ff00000)&&(ly!=0)))
    return x+y;

  /* determine if y is an odd int when x < 0
   * yisint = 0 ... y is not an integer
   * yisint = 1 ... y is an odd int
   * yisint = 2 ... y is an even int
   */
  yisint  = 0;
  if(hx<0) {
    if(iy>=0x43400000) yisint = 2; /* even integer y */
    else if(iy>=0x3ff00000) {
      k = (iy>>20)-0x3ff;          /* exponent */
      if(k>20) {
        j = ly>>(52-k);
        if((unsigned)(j<<(52-k))==ly) yisint = 2-(j&1);
      } else if(ly==0) {
        j = iy>>(20-k);
        if((j<<(20-k))==iy) yisint = 2-(j&1);
      }
    }
  }

  /* special value of y */
  if(ly==0) {
    if (iy==0x7ff00000) {       /* y is +-inf */
      if(((ix-0x3ff00000)|lx)==0)
        return  y - y;  /* inf**+-1 is NaN */
      else if (ix >= 0x3ff00000)/* (|x|>1)**+-inf = inf,0 */
        return (hy>=0)? y: zeroX;
      else                      /* (|x|<1)**-,+inf = inf,0 */
        return (hy<0)?-y: zeroX;
    }
    if(iy==0x3ff00000) {        /* y is  +-1 */
      if(hy<0) return one/x; else return x;
    }
    if(hy==0x40000000) return x*x; /* y is  2 */
    if(hy==0x3fe00000) {        /* y is  0.5 */
      if(hx>=0) /* x >= +0 */
        return sqrt(x);
    }
  }

  ax   = fabsd(x);
  /* special value of x */
  if(lx==0) {
    if(ix==0x7ff00000||ix==0||ix==0x3ff00000){
      z = ax;                   /*x is +-0,+-inf,+-1*/
      if(hy<0) z = one/z;       /* z = (1/|x|) */
      if(hx<0) {
        if(((ix-0x3ff00000)|yisint)==0) {
#ifdef CAN_USE_NAN_DEFINE
          z = NAN;
#else
          z = (z-z)/(z-z); /* (-1)**non-int is NaN */
#endif
        } else if(yisint==1)
          z = -1.0*z;           /* (x<0)**odd = -(|x|**odd) */
      }
      return z;
    }
  }

  n = (hx>>31)+1;

  /* (x<0)**(non-int) is NaN */
  if((n|yisint)==0)
#ifdef CAN_USE_NAN_DEFINE
    return NAN;
#else
    return (x-x)/(x-x);
#endif

  s = one; /* s (sign of result -ve**odd) = -1 else = 1 */
  if((n|(yisint-1))==0) s = -one;/* (-ve)**(odd int) */

  /* |y| is huge */
  if(iy>0x41e00000) { /* if |y| > 2**31 */
    if(iy>0x43f00000){  /* if |y| > 2**64, must o/uflow */
      if(ix<=0x3fefffff) return (hy<0)? hugeX*hugeX:tiny*tiny;
      if(ix>=0x3ff00000) return (hy>0)? hugeX*hugeX:tiny*tiny;
    }
    /* over/underflow if x is not close to one */
    if(ix<0x3fefffff) return (hy<0)? s*hugeX*hugeX:s*tiny*tiny;
    if(ix>0x3ff00000) return (hy>0)? s*hugeX*hugeX:s*tiny*tiny;
    /* now |1-x| is tiny <= 2**-20, suffice to compute
       log(x) by x-x^2/2+x^3/3-x^4/4 */
    t = ax-one;         /* t has 20 trailing zeros */
    w = (t*t)*(0.5-t*(0.3333333333333333333333-t*0.25));
    u = ivln2_h*t;      /* ivln2_h has 21 sig. bits */
    v = t*ivln2_l-w*ivln2;
    t1 = u+v;
    set_low(&t1, 0);
    t2 = v-(t1-u);
  } else {
    double ss,s2,s_h,s_l,t_h,t_l;
    n = 0;
    /* take care subnormal number */
    if(ix<0x00100000)
      {ax *= two53; n -= 53; ix = high(ax); }
    n  += ((ix)>>20)-0x3ff;
    j  = ix&0x000fffff;
    /* determine interval */
    ix = j|0x3ff00000;          /* normalize ix */
    if(j<=0x3988E) k=0;         /* |x|<sqrt(3/2) */
    else if(j<0xBB67A) k=1;     /* |x|<sqrt(3)   */
    else {k=0;n+=1;ix -= 0x00100000;}
    set_high(&ax, ix);

    /* compute ss = s_h+s_l = (x-1)/(x+1) or (x-1.5)/(x+1.5) */
    u = ax-bp[k];               /* bp[0]=1.0, bp[1]=1.5 */
    v = one/(ax+bp[k]);
    ss = u*v;
    s_h = ss;
    set_low(&s_h, 0);
    /* t_h=ax+bp[k] High */
    t_h = zeroX;
    set_high(&t_h, ((ix>>1)|0x20000000)+0x00080000+(k<<18));
    t_l = ax - (t_h-bp[k]);
    s_l = v*((u-s_h*t_h)-s_h*t_l);
    /* compute log(ax) */
    s2 = ss*ss;
    r = s2*s2*(L1X+s2*(L2X+s2*(L3X+s2*(L4X+s2*(L5X+s2*L6X)))));
    r += s_l*(s_h+ss);
    s2  = s_h*s_h;
    t_h = 3.0+s2+r;
    set_low(&t_h, 0);
    t_l = r-((t_h-3.0)-s2);
    /* u+v = ss*(1+...) */
    u = s_h*t_h;
    v = s_l*t_h+t_l*ss;
    /* 2/(3log2)*(ss+...) */
    p_h = u+v;
    set_low(&p_h, 0);
    p_l = v-(p_h-u);
    z_h = cp_h*p_h;             /* cp_h+cp_l = 2/(3*log2) */
    z_l = cp_l*p_h+p_l*cp+dp_l[k];
    /* log2(ax) = (ss+..)*2/(3*log2) = n + dp_h + z_h + z_l */
    t = (double)n;
    t1 = (((z_h+z_l)+dp_h[k])+t);
    set_low(&t1, 0);
    t2 = z_l-(((t1-t)-dp_h[k])-z_h);
  }

  /* split up y into y1+y2 and compute (y1+y2)*(t1+t2) */
  y1  = y;
  set_low(&y1, 0);
  p_l = (y-y1)*t1+y*t2;
  p_h = y1*t1;
  z = p_l+p_h;
  j = high(z);
  i = low(z);
  if (j>=0x40900000) {                          /* z >= 1024 */
    if(((j-0x40900000)|i)!=0)                   /* if z > 1024 */
      return s*hugeX*hugeX;                     /* overflow */
    else {
      if(p_l+ovt>z-p_h) return s*hugeX*hugeX;   /* overflow */
    }
  } else if((j&0x7fffffff)>=0x4090cc00 ) {      /* z <= -1075 */
    if(((j-0xc090cc00)|i)!=0)           /* z < -1075 */
      return s*tiny*tiny;               /* underflow */
    else {
      if(p_l<=z-p_h) return s*tiny*tiny;        /* underflow */
    }
  }
  /*
   * compute 2**(p_h+p_l)
   */
  i = j&0x7fffffff;
  k = (i>>20)-0x3ff;
  n = 0;
  if(i>0x3fe00000) {            /* if |z| > 0.5, set n = [z+0.5] */
    n = j+(0x00100000>>(k+1));
    k = ((n&0x7fffffff)>>20)-0x3ff;     /* new k for n */
    t = zeroX;
    set_high(&t, (n&~(0x000fffff>>k)));
    n = ((n&0x000fffff)|0x00100000)>>(20-k);
    if(j<0) n = -n;
    p_h -= t;
  }
  t = p_l+p_h;
  set_low(&t, 0);
  u = t*lg2_h;
  v = (p_l-(t-p_h))*lg2+t*lg2_l;
  z = u+v;
  w = v-(z-u);
  t  = z*z;
  t1  = z - t*(P1+t*(P2+t*(P3+t*(P4+t*P5))));
  r  = (z*t1)/(t1-two)-(w+z*w);
  z  = one-(r-z);
  j  = high(z);
  j += (n<<20);
  if((j>>20)<=0) z = scalbnA(z,n);       /* subnormal output */
  else set_high(&z, high(z) + (n<<20));
  return s*z;
}


JRT_LEAF(jdouble, SharedRuntime::dpow(jdouble x, jdouble y))
  return __ieee754_pow(x, y);
JRT_END

#ifdef WIN32
# pragma optimize ( "", on )
#endif
