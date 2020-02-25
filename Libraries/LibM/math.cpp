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
#include <LibM/math.h>
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

double trunc(double x)
{
    return (int64_t)x;
}

double cos(double angle)
{
    return sin(angle + M_PI_2);
}

float cosf(float angle)
{
    return sinf(angle + M_PI_2);
}

// This can also be done with a taylor expansion, but for
// now this works pretty well (and doesn't mess anything up
// in quake in particular, which is very Floating-Point precision
// heavy)
double sin(double angle)
{
    double ret = 0.0;
    __asm__(
        "fsin"
        : "=t"(ret)
        : "0"(angle));

    return ret;
}

float sinf(float angle)
{
    float ret = 0.0f;
    __asm__(
        "fsin"
        : "=t"(ret)
        : "0"(angle));
    return ret;
}

double pow(double x, double y)
{
    //FIXME: Extremely unlikely to be standards compliant.
    return exp(y * log(x));
}

float powf(float x, float y)
{
    return (float)exp((double)y * log((double)x));
}

double ldexp(double x, int exp)
{
    // FIXME: Please fix me. I am naive.
    double val = pow(2, exp);
    return x * val;
}

double tanh(double x)
{
    if (x > 0) {
        double exponentiated = exp(2 * x);
        return (exponentiated - 1) / (exponentiated + 1);
    }
    double plusX = exp(x);
    double minusX = 1 / plusX;
    return (plusX - minusX) / (plusX + minusX);
}

double ampsin(double angle)
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

double tan(double angle)
{
    return ampsin(angle) / ampsin(M_PI_2 + angle);
}

double sqrt(double x)
{
    double res;
    __asm__("fsqrt"
            : "=t"(res)
            : "0"(x));
    return res;
}

float sqrtf(float x)
{
    float res;
    __asm__("fsqrt"
            : "=t"(res)
            : "0"(x));
    return res;
}

double sinh(double x)
{
    double exponentiated = exp(x);
    if (x > 0)
        return (exponentiated * exponentiated - 1) / 2 / exponentiated;
    return (exponentiated - 1 / exponentiated) / 2;
}

double log10(double x)
{
    return log(x) / M_LN10;
}

double log(double x)
{
    if (x < 0)
        return __builtin_nan("");
    if (x == 0)
        return -__builtin_huge_val();
    double y = 1 + 2 * (x - 1) / (x + 1);
    double exponentiated = exp(y);
    y = y + 2 * (x - exponentiated) / (x + exponentiated);
    exponentiated = exp(y);
    y = y + 2 * (x - exponentiated) / (x + exponentiated);
    exponentiated = exp(y);
    return y + 2 * (x - exponentiated) / (x + exponentiated);
}

float logf(float x)
{
    return (float)log(x);
}

double fmod(double index, double period)
{
    return index - trunc(index / period) * period;
}

double exp(double exponent)
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
                return __builtin_huge_val();
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

float expf(float exponent)
{
    return (float)exp(exponent);
}

double cosh(double x)
{
    double exponentiated = exp(-x);
    if (x < 0)
        return (1 + exponentiated * exponentiated) / 2 / exponentiated;
    return (1 / exponentiated + exponentiated) / 2;
}

double atan2(double y, double x)
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

float atan2f(float y, float x)
{
    return (float)atan2(y, x);
}

double atan(double x)
{
    if (x < 0)
        return -atan(-x);
    if (x > 1)
        return M_PI_2 - atan(1 / x);
    double squared = x * x;
    return x / (1 + 1 * 1 * squared / (3 + 2 * 2 * squared / (5 + 3 * 3 * squared / (7 + 4 * 4 * squared / (9 + 5 * 5 * squared / (11 + 6 * 6 * squared / (13 + 7 * 7 * squared)))))));
}

double asin(double x)
{
    if (x > 1 || x < -1)
        return __builtin_nan("");
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

float asinf(float x)
{
    return (float)asin(x);
}

double acos(double x)
{
    return M_PI_2 - asin(x);
}

float acosf(float x)
{
    return M_PI_2 - asinf(x);
}

double fabs(double value)
{
    return value < 0 ? -value : value;
}

double log2(double x)
{
    return log(x) / M_LN2;
}

float log2f(float x)
{
    return log2(x);
}

long double log2l(long double x)
{
    return log2(x);
}

double frexp(double, int*)
{
    ASSERT_NOT_REACHED();
    return 0;
}

float frexpf(float, int*)
{
    ASSERT_NOT_REACHED();
    return 0;
}

long double frexpl(long double, int*)
{
    ASSERT_NOT_REACHED();
    return 0;
}

float roundf(float value)
{
    // FIXME: Please fix me. I am naive.
    if (value >= 0.0f)
        return (float)(int)(value + 0.5f);
    return (float)(int)(value - 0.5f);
}

double floor(double value)
{
    return (int)value;
}

double rint(double value)
{
    return (int)roundf(value);
}

float ceilf(float value)
{
    // FIXME: Please fix me. I am naive.
    int as_int = (int)value;
    if (value == (float)as_int) {
        return (float)as_int;
    }
    return as_int + 1;
}

double ceil(double value)
{
    // FIXME: Please fix me. I am naive.
    int as_int = (int)value;
    if (value == (double)as_int) {
        return (double)as_int;
    }
    return as_int + 1;
}

double modf(double x, double* intpart)
{
    *intpart = (double)((int)(x));
    return x - (int)x;
}
}
