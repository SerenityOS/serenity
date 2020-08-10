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

#include <LibC/assert.h>
#include <math.h>
#include <stdint.h>
#include <stdlib.h>

template<size_t>
constexpr double e_to_power();
template<>
constexpr double e_to_power<0>() { return 1; }
template<size_t exponent>
constexpr double e_to_power() { return M_E * e_to_power<exponent - 1>(); }

template<size_t>
constexpr size_t factorial();
template<>
constexpr size_t factorial<0>() { return 1; }
template<size_t value>
constexpr size_t factorial() { return value * factorial<value - 1>(); }

template<size_t>
constexpr size_t product_even();
template<>
constexpr size_t product_even<2>() { return 2; }
template<size_t value>
constexpr size_t product_even() { return value * product_even<value - 2>(); }

template<size_t>
constexpr size_t product_odd();
template<>
constexpr size_t product_odd<1>() { return 1; }
template<size_t value>
constexpr size_t product_odd() { return value * product_odd<value - 2>(); }

extern "C" {

double trunc(double x) NOEXCEPT
{
    return (int64_t)x;
}

double cos(double angle) NOEXCEPT
{
    return sin(angle + M_PI_2);
}

float cosf(float angle) NOEXCEPT
{
    return sinf(angle + M_PI_2);
}

// This can also be done with a taylor expansion, but for
// now this works pretty well (and doesn't mess anything up
// in quake in particular, which is very Floating-Point precision
// heavy)
double sin(double angle) NOEXCEPT
{
    double ret = 0.0;
    __asm__(
        "fsin"
        : "=t"(ret)
        : "0"(angle));

    return ret;
}

float sinf(float angle) NOEXCEPT
{
    float ret = 0.0f;
    __asm__(
        "fsin"
        : "=t"(ret)
        : "0"(angle));
    return ret;
}

double pow(double x, double y) NOEXCEPT
{
    // FIXME: Please fix me. I am naive.
    if (y == 0)
        return 1;
    if (y == 1)
        return x;
    int y_as_int = (int)y;
    if (y == (double)y_as_int) {
        double result = x;
        for (int i = 0; i < fabs(y) - 1; ++i)
            result *= x;
        if (y < 0)
            result = 1.0 / result;
        return result;
    }
    return exp(y * log(x));
}

float powf(float x, float y) NOEXCEPT
{
    // FIXME: Please fix me. I am naive.
    if (y == 0)
        return 1;
    if (y == 1)
        return x;
    int y_as_int = (int)y;
    if (y == (float)y_as_int) {
        float result = x;
        for (int i = 0; i < fabs(y) - 1; ++i)
            result *= x;
        if (y < 0)
            result = 1.0 / result;
        return result;
    }
    return (float)exp((double)y * log((double)x));
}

double ldexp(double x, int exp) NOEXCEPT
{
    // FIXME: Please fix me. I am naive.
    double val = pow(2, exp);
    return x * val;
}

double tanh(double x) NOEXCEPT
{
    if (x > 0) {
        double exponentiated = exp(2 * x);
        return (exponentiated - 1) / (exponentiated + 1);
    }
    double plusX = exp(x);
    double minusX = 1 / plusX;
    return (plusX - minusX) / (plusX + minusX);
}

static double ampsin(double angle) NOEXCEPT
{
    double looped_angle = fmod(M_PI + angle, M_TAU) - M_PI;
    double looped_angle_squared = looped_angle * looped_angle;

    double quadratic_term;
    if (looped_angle > 0) {
        quadratic_term = -looped_angle_squared;
    } else {
        quadratic_term = looped_angle_squared;
    }

    double linear_term = M_PI * looped_angle;

    return quadratic_term + linear_term;
}

double tan(double angle) NOEXCEPT
{
    return ampsin(angle) / ampsin(M_PI_2 + angle);
}

double sqrt(double x) NOEXCEPT
{
    double res;
    __asm__("fsqrt"
            : "=t"(res)
            : "0"(x));
    return res;
}

float sqrtf(float x) NOEXCEPT
{
    float res;
    __asm__("fsqrt"
            : "=t"(res)
            : "0"(x));
    return res;
}

double sinh(double x) NOEXCEPT
{
    double exponentiated = exp(x);
    if (x > 0)
        return (exponentiated * exponentiated - 1) / 2 / exponentiated;
    return (exponentiated - 1 / exponentiated) / 2;
}

double log10(double x) NOEXCEPT
{
    return log(x) / M_LN10;
}

double log(double x) NOEXCEPT
{
    if (x < 0)
        return NAN;
    if (x == 0)
        return -INFINITY;
    double y = 1 + 2 * (x - 1) / (x + 1);
    double exponentiated = exp(y);
    y = y + 2 * (x - exponentiated) / (x + exponentiated);
    exponentiated = exp(y);
    y = y + 2 * (x - exponentiated) / (x + exponentiated);
    exponentiated = exp(y);
    return y + 2 * (x - exponentiated) / (x + exponentiated);
}

float logf(float x) NOEXCEPT
{
    return (float)log(x);
}

double fmod(double index, double period) NOEXCEPT
{
    return index - trunc(index / period) * period;
}

float fmodf(float index, float period) NOEXCEPT
{
    return index - trunc(index / period) * period;
}

double exp(double exponent) NOEXCEPT
{
    double result = 1;
    if (exponent >= 1) {
        size_t integer_part = (size_t)exponent;
        if (integer_part & 1)
            result *= e_to_power<1>();
        if (integer_part & 2)
            result *= e_to_power<2>();
        if (integer_part > 3) {
            if (integer_part & 4)
                result *= e_to_power<4>();
            if (integer_part & 8)
                result *= e_to_power<8>();
            if (integer_part & 16)
                result *= e_to_power<16>();
            if (integer_part & 32)
                result *= e_to_power<32>();
            if (integer_part >= 64)
                return INFINITY;
        }
        exponent -= integer_part;
    } else if (exponent < 0)
        return 1 / exp(-exponent);
    double taylor_series_result = 1 + exponent;
    double taylor_series_numerator = exponent * exponent;
    taylor_series_result += taylor_series_numerator / factorial<2>();
    taylor_series_numerator *= exponent;
    taylor_series_result += taylor_series_numerator / factorial<3>();
    taylor_series_numerator *= exponent;
    taylor_series_result += taylor_series_numerator / factorial<4>();
    taylor_series_numerator *= exponent;
    taylor_series_result += taylor_series_numerator / factorial<5>();
    return result * taylor_series_result;
}

float expf(float exponent) NOEXCEPT
{
    return (float)exp(exponent);
}

double exp2(double exponent) NOEXCEPT
{
    return pow(2.0, exponent);
}

float exp2f(float exponent) NOEXCEPT
{
    return pow(2.0f, exponent);
}

double cosh(double x) NOEXCEPT
{
    double exponentiated = exp(-x);
    if (x < 0)
        return (1 + exponentiated * exponentiated) / 2 / exponentiated;
    return (1 / exponentiated + exponentiated) / 2;
}

double atan2(double y, double x) NOEXCEPT
{
    if (x > 0)
        return atan(y / x);
    if (x == 0) {
        if (y > 0)
            return M_PI_2;
        if (y < 0)
            return -M_PI_2;
        return 0;
    }
    if (y >= 0)
        return atan(y / x) + M_PI;
    return atan(y / x) - M_PI;
}

float atan2f(float y, float x) NOEXCEPT
{
    return (float)atan2(y, x);
}

double atan(double x) NOEXCEPT
{
    if (x < 0)
        return -atan(-x);
    if (x > 1)
        return M_PI_2 - atan(1 / x);
    double squared = x * x;
    return x / (1 + 1 * 1 * squared / (3 + 2 * 2 * squared / (5 + 3 * 3 * squared / (7 + 4 * 4 * squared / (9 + 5 * 5 * squared / (11 + 6 * 6 * squared / (13 + 7 * 7 * squared)))))));
}

double asin(double x) NOEXCEPT
{
    if (x > 1 || x < -1)
        return NAN;
    if (x > 0.5 || x < -0.5)
        return 2 * atan(x / (1 + sqrt(1 - x * x)));
    double squared = x * x;
    double value = x;
    double i = x * squared;
    value += i * product_odd<1>() / product_even<2>() / 3;
    i *= squared;
    value += i * product_odd<3>() / product_even<4>() / 5;
    i *= squared;
    value += i * product_odd<5>() / product_even<6>() / 7;
    i *= squared;
    value += i * product_odd<7>() / product_even<8>() / 9;
    i *= squared;
    value += i * product_odd<9>() / product_even<10>() / 11;
    i *= squared;
    value += i * product_odd<11>() / product_even<12>() / 13;
    return value;
}

float asinf(float x) NOEXCEPT
{
    return (float)asin(x);
}

double acos(double x) NOEXCEPT
{
    return M_PI_2 - asin(x);
}

float acosf(float x) NOEXCEPT
{
    return M_PI_2 - asinf(x);
}

double fabs(double value) NOEXCEPT
{
    return value < 0 ? -value : value;
}

double log2(double x) NOEXCEPT
{
    return log(x) / M_LN2;
}

float log2f(float x) NOEXCEPT
{
    return log2(x);
}

long double log2l(long double x) NOEXCEPT
{
    return log2(x);
}

double frexp(double, int*) NOEXCEPT
{
    ASSERT_NOT_REACHED();
    return 0;
}

float frexpf(float, int*) NOEXCEPT
{
    ASSERT_NOT_REACHED();
    return 0;
}

long double frexpl(long double, int*) NOEXCEPT
{
    ASSERT_NOT_REACHED();
    return 0;
}

double round(double value) NOEXCEPT
{
    // FIXME: Please fix me. I am naive.
    if (value >= 0.0)
        return (double)(int)(value + 0.5);
    return (double)(int)(value - 0.5);
}

float roundf(float value) NOEXCEPT
{
    // FIXME: Please fix me. I am naive.
    if (value >= 0.0f)
        return (float)(int)(value + 0.5f);
    return (float)(int)(value - 0.5f);
}

float floorf(float value) NOEXCEPT
{
    if (value >= 0)
        return (int)value;
    int intvalue = (int)value;
    return ((float)intvalue == value) ? intvalue : intvalue - 1;
}

double floor(double value) NOEXCEPT
{
    if (value >= 0)
        return (int)value;
    int intvalue = (int)value;
    return ((double)intvalue == value) ? intvalue : intvalue - 1;
}

double rint(double value) NOEXCEPT
{
    return (int)roundf(value);
}

float ceilf(float value) NOEXCEPT
{
    // FIXME: Please fix me. I am naive.
    int as_int = (int)value;
    if (value == (float)as_int)
        return as_int;
    if (value < 0) {
        if (as_int == 0)
            return -0;
        return as_int;
    }
    return as_int + 1;
}

double ceil(double value) NOEXCEPT
{
    // FIXME: Please fix me. I am naive.
    int as_int = (int)value;
    if (value == (double)as_int)
        return as_int;
    if (value < 0) {
        if (as_int == 0)
            return -0;
        return as_int;
    }
    return as_int + 1;
}

double modf(double x, double* intpart) NOEXCEPT
{
    *intpart = (double)((int)(x));
    return x - (int)x;
}

double gamma(double x) NOEXCEPT
{
    // Stirling approximation
    return sqrt(2.0 * M_PI / x) * pow(x / M_E, x);
}

double expm1(double x) NOEXCEPT
{
    return pow(M_E, x) - 1;
}

double cbrt(double x) NOEXCEPT
{
    if (x > 0) {
        return pow(x, 1.0 / 3.0);
    }

    return -pow(-x, 1.0 / 3.0);
}

double log1p(double x) NOEXCEPT
{
    return log(1 + x);
}

double acosh(double x) NOEXCEPT
{
    return log(x + sqrt(x * x - 1));
}

double asinh(double x) NOEXCEPT
{
    return log(x + sqrt(x * x + 1));
}

double atanh(double x) NOEXCEPT
{
    return log((1 + x) / (1 - x)) / 2.0;
}

double hypot(double x, double y) NOEXCEPT
{
    return sqrt(x * x + y * y);
}

double erf(double x) NOEXCEPT
{
    // algorithm taken from Abramowitz and Stegun (no. 26.2.17)
    double t = 1 / (1 + 0.47047 * fabs(x));
    double poly = t * (0.3480242 + t * (-0.958798 + t * 0.7478556));
    double answer = 1 - poly * exp(-x * x);
    if (x < 0)
        return -answer;

    return answer;
}

double erfc(double x) NOEXCEPT
{
    return 1 - erf(x);
}
}
