/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <float.h>
#include <limits.h>
#include <math_defines.h>
#include <sys/cdefs.h>

#if __cplusplus >= 201103L
#    define NOEXCEPT noexcept
#else
#    define NOEXCEPT
#endif

__BEGIN_DECLS

#define MATH_ERRNO 1
#define MATH_ERREXCEPT 2
#define math_errhandling MATH_ERREXCEPT

#if FLT_EVAL_METHOD == 0
typedef float float_t;
typedef double double_t;
#elif FLT_EVAL_METHOD == 1
typedef double float_t;
typedef double double_t;
#elif FLT_EVAL_METHOD == 2
typedef long double float_t;
typedef long double double_t;
#else
typedef float float_t;
typedef double double_t;
#endif

/* Basic floating point operations */
long double fabsl(long double) NOEXCEPT;
double fabs(double) NOEXCEPT;
float fabsf(float) NOEXCEPT;
long double fmodl(long double, long double) NOEXCEPT;
double fmod(double, double) NOEXCEPT;
float fmodf(float, float) NOEXCEPT;
long double fmaxl(long double, long double) NOEXCEPT;
double fmax(double, double) NOEXCEPT;
float fmaxf(float, float) NOEXCEPT;
long double fminl(long double, long double) NOEXCEPT;
double fmin(double, double) NOEXCEPT;
float fminf(float, float) NOEXCEPT;
long double remainderl(long double, long double) NOEXCEPT;
double remainder(double, double) NOEXCEPT;
float remainderf(float, float) NOEXCEPT;
long double nanl(const char*) NOEXCEPT;
double nan(const char*) NOEXCEPT;
float nanf(const char*) NOEXCEPT;

/* Exponential functions */
long double expl(long double) NOEXCEPT;
double exp(double) NOEXCEPT;
float expf(float) NOEXCEPT;
long double exp2l(long double) NOEXCEPT;
double exp2(double) NOEXCEPT;
float exp2f(float) NOEXCEPT;
long double expm1l(long double) NOEXCEPT;
double expm1(double) NOEXCEPT;
float expm1f(float) NOEXCEPT;
long double logl(long double) NOEXCEPT;
double log(double) NOEXCEPT;
float logf(float) NOEXCEPT;
double log2(double) NOEXCEPT;
float log2f(float) NOEXCEPT;
long double log2l(long double) NOEXCEPT;
long double log10l(long double) NOEXCEPT;
double log10(double) NOEXCEPT;
float log10f(float) NOEXCEPT;
long double log1pl(long double) NOEXCEPT;
double log1p(double) NOEXCEPT;
float log1pf(float) NOEXCEPT;

/* Power functions */
long double powl(long double x, long double y) NOEXCEPT;
double pow(double x, double y) NOEXCEPT;
float powf(float x, float y) NOEXCEPT;
long double sqrtl(long double) NOEXCEPT;
double sqrt(double) NOEXCEPT;
float sqrtf(float) NOEXCEPT;
long double cbrtl(long double) NOEXCEPT;
double cbrt(double) NOEXCEPT;
float cbrtf(float) NOEXCEPT;
long double hypotl(long double, long double) NOEXCEPT;
double hypot(double, double) NOEXCEPT;
float hypotf(float, float) NOEXCEPT;

/* Trigonometric functions */
long double sinl(long double) NOEXCEPT;
double sin(double) NOEXCEPT;
float sinf(float) NOEXCEPT;
long double cosl(long double) NOEXCEPT;
double cos(double) NOEXCEPT;
float cosf(float) NOEXCEPT;
long double tanl(long double) NOEXCEPT;
double tan(double) NOEXCEPT;
float tanf(float) NOEXCEPT;
long double asinl(long double) NOEXCEPT;
double asin(double) NOEXCEPT;
float asinf(float) NOEXCEPT;
long double acosl(long double) NOEXCEPT;
double acos(double) NOEXCEPT;
float acosf(float) NOEXCEPT;
long double atanl(long double) NOEXCEPT;
double atan(double) NOEXCEPT;
float atanf(float) NOEXCEPT;
long double atan2l(long double, long double) NOEXCEPT;
double atan2(double, double) NOEXCEPT;
float atan2f(float, float) NOEXCEPT;

/* Hyperbolic functions*/
long double sinhl(long double) NOEXCEPT;
double sinh(double) NOEXCEPT;
float sinhf(float) NOEXCEPT;
long double coshl(long double) NOEXCEPT;
double cosh(double) NOEXCEPT;
float coshf(float) NOEXCEPT;
long double tanhl(long double) NOEXCEPT;
double tanh(double) NOEXCEPT;
float tanhf(float) NOEXCEPT;
long double asinhl(long double) NOEXCEPT;
double asinh(double) NOEXCEPT;
float asinhf(float) NOEXCEPT;
long double acoshl(long double) NOEXCEPT;
double acosh(double) NOEXCEPT;
float acoshf(float) NOEXCEPT;
long double atanhl(long double) NOEXCEPT;
double atanh(double) NOEXCEPT;
float atanhf(float) NOEXCEPT;

/* Error and gamma functions */
long double erfl(long double) NOEXCEPT;
double erf(double) NOEXCEPT;
float erff(float) NOEXCEPT;
long double erfcl(long double) NOEXCEPT;
double erfc(double) NOEXCEPT;
float erfcf(float) NOEXCEPT;
double gamma(double) NOEXCEPT;
long double tgammal(long double) NOEXCEPT;
double tgamma(double) NOEXCEPT;
float tgammaf(float) NOEXCEPT;
long double lgammal(long double) NOEXCEPT;
double lgamma(double) NOEXCEPT;
float lgammaf(float) NOEXCEPT;
long double lgammal_r(long double, int*) NOEXCEPT;
double lgamma_r(double, int*) NOEXCEPT;
float lgammaf_r(float, int*) NOEXCEPT;
extern int signgam;

/* Nearest integer floating point operations */
long double ceill(long double) NOEXCEPT;
double ceil(double) NOEXCEPT;
float ceilf(float) NOEXCEPT;
long double floorl(long double) NOEXCEPT;
double floor(double) NOEXCEPT;
float floorf(float) NOEXCEPT;
long double truncl(long double) NOEXCEPT;
double trunc(double) NOEXCEPT;
float truncf(float) NOEXCEPT;
float roundf(float) NOEXCEPT;
double round(double) NOEXCEPT;
long double roundl(long double) NOEXCEPT;
long lroundf(float) NOEXCEPT;
long lround(double) NOEXCEPT;
long lroundl(long double) NOEXCEPT;
long long llroundf(float) NOEXCEPT;
long long llround(double) NOEXCEPT;
long long llroundd(long double) NOEXCEPT;
long long llroundl(long double x) NOEXCEPT;
double nearbyint(double x) NOEXCEPT;
float nearbyintf(float x) NOEXCEPT;
long double nearbyintl(long double x) NOEXCEPT;

float rintf(float) NOEXCEPT;
double rint(double) NOEXCEPT;
long double rintl(long double) NOEXCEPT;
long lrintl(long double) NOEXCEPT;
long lrint(double) NOEXCEPT;
long lrintf(float) NOEXCEPT;
long long llrintl(long double) NOEXCEPT;
long long llrint(double) NOEXCEPT;
long long llrintf(float) NOEXCEPT;

/* Floating point manipulation functions */
long double frexpl(long double, int* exp) NOEXCEPT;
double frexp(double, int* exp) NOEXCEPT;
float frexpf(float, int* exp) NOEXCEPT;
long double ldexpl(long double, int exp) NOEXCEPT;
double ldexp(double, int exp) NOEXCEPT;
float ldexpf(float, int exp) NOEXCEPT;
long double modfl(long double, long double*) NOEXCEPT;
double modf(double, double*) NOEXCEPT;
float modff(float, float*) NOEXCEPT;
float scalbnf(float, int) NOEXCEPT;
double scalbn(double, int) NOEXCEPT;
long double scalbnl(long double, int) NOEXCEPT;
float scalbnlf(float, long) NOEXCEPT;
double scalbln(double, long) NOEXCEPT;
float scalblnf(float, long) NOEXCEPT;
long double scalblnl(long double, long) NOEXCEPT;
int ilogbl(long double) NOEXCEPT;
int ilogb(double) NOEXCEPT;
int ilogbf(float) NOEXCEPT;
long double logbl(long double) NOEXCEPT;
double logb(double) NOEXCEPT;
float logbf(float) NOEXCEPT;
double nextafter(double, double) NOEXCEPT;
float nextafterf(float, float) NOEXCEPT;
long double nextafterl(long double, long double) NOEXCEPT;
double nexttoward(double, long double) NOEXCEPT;
float nexttowardf(float, long double) NOEXCEPT;
long double nexttowardl(long double, long double) NOEXCEPT;
float copysignf(float x, float y) NOEXCEPT;
double copysign(double x, double y) NOEXCEPT;
long double copysignl(long double x, long double y) NOEXCEPT;

/* positive difference */
double fdim(double x, double y) NOEXCEPT;
float fdimf(float x, float y) NOEXCEPT;
long double fdiml(long double x, long double y) NOEXCEPT;

/* floating-point multiply and add */
double fma(double x, double y, double z) NOEXCEPT;
float fmaf(float x, float y, float z) NOEXCEPT;
long double fmal(long double x, long double y, long double z) NOEXCEPT;

/* remainder and part of quotient */
double remquo(double x, double y, int* quo) NOEXCEPT;
float remquof(float x, float y, int* quo) NOEXCEPT;
long double remquol(long double x, long double y, int* quo) NOEXCEPT;

__END_DECLS
