/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <limits.h>
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

#define HUGE_VAL 1e10000
#define INFINITY __builtin_huge_val()
#define NAN __builtin_nan("")
#define M_E 2.718281828459045
#define M_PI 3.141592653589793
#define M_PI_2 1.570796326794896
#define M_TAU 6.283185307179586
#define M_DEG2RAD 0.017453292519943
#define M_RAD2DEG 57.29577951308232
#define M_LN2 0.69314718055995
#define M_LN10 2.30258509299405
#define M_SQRT2 1.4142135623730951
#define M_SQRT1_2 0.7071067811865475

#define FP_NAN 0
#define FP_INFINITE 1
#define FP_ZERO 2
#define FP_SUBNORMAL 3
#define FP_NORMAL 4
#define fpclassify(x) __builtin_fpclassify(FP_NAN, FP_INFINITE, FP_ZERO, FP_SUBNORMAL, FP_ZERO, x)

#define signbit(x) __builtin_signbit(x)
#define isnan(x) __builtin_isnan(x)
#define isinf(x) __builtin_isinf_sign(x)
#define isfinite(x) __builtin_isfinite(x)
#define isnormal(x) __builtin_isnormal(x)
#define isgreater(x, y) __builtin_isgreater((x), (y))
#define isgreaterequal(x, y) __builtin_isgreaterequal((x), (y))
#define isless(x, y) __builtin_isless((x), (y))
#define islessequal(x, y) __builtin_islessequal((x), (y))
#define islessgreater(x, y) __builtin_islessgreater((x), (y))
#define isunordered(x, y) __builtin_isunoredered((x), (y))

#define DOUBLE_MAX ((double)0b0111111111101111111111111111111111111111111111111111111111111111)
#define DOUBLE_MIN ((double)0b0000000000010000000000000000000000000000000000000000000000000000)

long double nanl(const char*) NOEXCEPT;
double nan(const char*) NOEXCEPT;
float nanf(const char*) NOEXCEPT;

long double acosl(long double) NOEXCEPT;
double acos(double) NOEXCEPT;
float acosf(float) NOEXCEPT;
long double asinl(long double) NOEXCEPT;
double asin(double) NOEXCEPT;
float asinf(float) NOEXCEPT;
long double atanl(long double) NOEXCEPT;
double atan(double) NOEXCEPT;
float atanf(float) NOEXCEPT;
long double atan2l(long double, long double) NOEXCEPT;
double atan2(double, double) NOEXCEPT;
float atan2f(float, float) NOEXCEPT;
long double cosl(long double) NOEXCEPT;
double cos(double) NOEXCEPT;
float cosf(float) NOEXCEPT;
long double coshl(long double) NOEXCEPT;
double cosh(double) NOEXCEPT;
float coshf(float) NOEXCEPT;
long double sinl(long double) NOEXCEPT;
double sin(double) NOEXCEPT;
float sinf(float) NOEXCEPT;
long double sinhl(long double) NOEXCEPT;
double sinh(double) NOEXCEPT;
float sinhf(float) NOEXCEPT;
long double tanl(long double) NOEXCEPT;
double tan(double) NOEXCEPT;
float tanf(float) NOEXCEPT;
long double tanhl(long double) NOEXCEPT;
double tanh(double) NOEXCEPT;
float tanhf(float) NOEXCEPT;
double ceil(double) NOEXCEPT;
float ceilf(float) NOEXCEPT;
long double ceill(long double) NOEXCEPT;
double floor(double) NOEXCEPT;
float floorf(float) NOEXCEPT;
long double floorl(long double) NOEXCEPT;
double trunc(double) NOEXCEPT;
float truncf(float) NOEXCEPT;
long double truncl(long double) NOEXCEPT;
double round(double) NOEXCEPT;
float roundf(float) NOEXCEPT;
long double roundl(long double) NOEXCEPT;
double rint(double) NOEXCEPT;
float rintf(float) NOEXCEPT;
long lrintl(long double) NOEXCEPT;
long lrint(double) NOEXCEPT;
long lrintf(float) NOEXCEPT;
long long llrintl(long double) NOEXCEPT;
long long llrint(double) NOEXCEPT;
long long llrintf(float) NOEXCEPT;
long double fabsl(long double) NOEXCEPT;
double fabs(double) NOEXCEPT;
float fabsf(float) NOEXCEPT;
long double fmodl(long double, long double) NOEXCEPT;
double fmod(double, double) NOEXCEPT;
float fmodf(float, float) NOEXCEPT;
long double expl(long double) NOEXCEPT;
double exp(double) NOEXCEPT;
float expf(float) NOEXCEPT;
long double exp2l(long double) NOEXCEPT;
double exp2(double) NOEXCEPT;
float exp2f(float) NOEXCEPT;
long double frexpl(long double, int* exp) NOEXCEPT;
double frexp(double, int* exp) NOEXCEPT;
float frexpf(float, int* exp) NOEXCEPT;
long double logl(long double) NOEXCEPT;
double log(double) NOEXCEPT;
float logf(float) NOEXCEPT;
long double log10l(long double) NOEXCEPT;
double log10(double) NOEXCEPT;
float log10f(float) NOEXCEPT;
long double sqrtl(long double) NOEXCEPT;
double sqrt(double) NOEXCEPT;
float sqrtf(float) NOEXCEPT;

long double modfl(long double, long double*) NOEXCEPT;
double modf(double, double*) NOEXCEPT;
float modff(float, float*) NOEXCEPT;
double ldexp(double, int exp) NOEXCEPT;
float ldexpf(float, int exp) NOEXCEPT;

long double powl(long double x, long double y) NOEXCEPT;
double pow(double x, double y) NOEXCEPT;
float powf(float x, float y) NOEXCEPT;

#define FP_ILOGB0 INT_MIN
#define FP_ILOGNAN INT_MAX

int ilogbl(long double) NOEXCEPT;
int ilogb(double) NOEXCEPT;
int ilogbf(float) NOEXCEPT;
long double logbl(long double) NOEXCEPT;
double logb(double) NOEXCEPT;
float logbf(float) NOEXCEPT;
double log2(double) NOEXCEPT;
float log2f(float) NOEXCEPT;
long double log2l(long double) NOEXCEPT;
double frexp(double, int*) NOEXCEPT;
float frexpf(float, int*) NOEXCEPT;
long double frexpl(long double, int*) NOEXCEPT;

double gamma(double) NOEXCEPT;
long double expm1l(long double) NOEXCEPT;
double expm1(double) NOEXCEPT;
float expm1f(float) NOEXCEPT;
long double cbrtl(long double) NOEXCEPT;
double cbrt(double) NOEXCEPT;
float cbrtf(float) NOEXCEPT;
long double log1pl(long double) NOEXCEPT;
double log1p(double) NOEXCEPT;
float log1pf(float) NOEXCEPT;
long double acoshl(long double) NOEXCEPT;
double acosh(double) NOEXCEPT;
float acoshf(float) NOEXCEPT;
long double asinhl(long double) NOEXCEPT;
double asinh(double) NOEXCEPT;
float asinhf(float) NOEXCEPT;
long double atanhl(long double) NOEXCEPT;
double atanh(double) NOEXCEPT;
float atanhf(float) NOEXCEPT;
long double hypotl(long double, long double) NOEXCEPT;
double hypot(double, double) NOEXCEPT;
float hypotf(float, float) NOEXCEPT;
long double erfl(long double) NOEXCEPT;
double erf(double) NOEXCEPT;
float erff(float) NOEXCEPT;
long double erfcl(long double) NOEXCEPT;
double erfc(double) NOEXCEPT;
float erfcf(float) NOEXCEPT;

double nextafter(double, double) NOEXCEPT;
float nextafterf(float, float) NOEXCEPT;
long double nextafterl(long double, long double) NOEXCEPT;
double nexttoward(double, long double) NOEXCEPT;
float nexttowardf(float, long double) NOEXCEPT;
long double nexttowardl(long double, long double) NOEXCEPT;

float scalbnf(float, int) NOEXCEPT;
double scalbn(double, int) NOEXCEPT;
long double scalbnl(long double, int) NOEXCEPT;
float scalbnlf(float, long) NOEXCEPT;
double scalbln(double, long) NOEXCEPT;
long double scalblnl(long double, long) NOEXCEPT;

float copysignf(float x, float y) NOEXCEPT;
double copysign(double x, double y) NOEXCEPT;
long double copysignl(long double x, long double y) NOEXCEPT;

__END_DECLS
