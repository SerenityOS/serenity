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

#include <sys/cdefs.h>

#if __cplusplus >= 201103L
#    define NOEXCEPT noexcept
#else
#    define NOEXCEPT
#endif

__BEGIN_DECLS

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

#define DOUBLE_MAX ((double)0b0111111111101111111111111111111111111111111111111111111111111111)
#define DOUBLE_MIN ((double)0b0000000000010000000000000000000000000000000000000000000000000000)

double acos(double) NOEXCEPT;
float acosf(float) NOEXCEPT;
double asin(double) NOEXCEPT;
float asinf(float) NOEXCEPT;
double atan(double) NOEXCEPT;
float atanf(float) NOEXCEPT;
double atan2(double, double) NOEXCEPT;
float atan2f(float, float) NOEXCEPT;
double cos(double) NOEXCEPT;
float cosf(float) NOEXCEPT;
double cosh(double) NOEXCEPT;
float coshf(float) NOEXCEPT;
double sin(double) NOEXCEPT;
float sinf(float) NOEXCEPT;
double sinh(double) NOEXCEPT;
float sinhf(float) NOEXCEPT;
double tan(double) NOEXCEPT;
float tanf(float) NOEXCEPT;
double tanh(double) NOEXCEPT;
float tanhf(float) NOEXCEPT;
double ceil(double) NOEXCEPT;
float ceilf(float) NOEXCEPT;
double floor(double) NOEXCEPT;
float floorf(float) NOEXCEPT;
double round(double) NOEXCEPT;
float roundf(float) NOEXCEPT;
double fabs(double) NOEXCEPT;
float fabsf(float) NOEXCEPT;
double fmod(double, double) NOEXCEPT;
float fmodf(float, float) NOEXCEPT;
double exp(double) NOEXCEPT;
float expf(float) NOEXCEPT;
double exp2(double) NOEXCEPT;
float exp2f(float) NOEXCEPT;
double frexp(double, int* exp) NOEXCEPT;
float frexpf(float, int* exp) NOEXCEPT;
double log(double) NOEXCEPT;
float logf(float) NOEXCEPT;
double log10(double) NOEXCEPT;
float log10f(float) NOEXCEPT;
double sqrt(double) NOEXCEPT;
float sqrtf(float) NOEXCEPT;
double modf(double, double*) NOEXCEPT;
float modff(float, float*) NOEXCEPT;
double ldexp(double, int exp) NOEXCEPT;
float ldexpf(float, int exp) NOEXCEPT;

double pow(double x, double y) NOEXCEPT;
float powf(float x, float y) NOEXCEPT;

double log2(double) NOEXCEPT;
float log2f(float) NOEXCEPT;
long double log2l(long double) NOEXCEPT;
double frexp(double, int*) NOEXCEPT;
float frexpf(float, int*) NOEXCEPT;
long double frexpl(long double, int*) NOEXCEPT;

double gamma(double) NOEXCEPT;
double expm1(double) NOEXCEPT;
double cbrt(double) NOEXCEPT;
double log1p(double) NOEXCEPT;
double acosh(double) NOEXCEPT;
double asinh(double) NOEXCEPT;
double atanh(double) NOEXCEPT;
double hypot(double, double) NOEXCEPT;
double erf(double) NOEXCEPT;
double erfc(double) NOEXCEPT;

__END_DECLS
