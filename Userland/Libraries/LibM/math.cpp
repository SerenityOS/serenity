/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mițca Dumitru <dumitru0mitca@gmail.com>
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

#include <AK/Platform.h>
#include <AK/StdLibExtras.h>
#include <LibC/assert.h>
#include <fenv.h>
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

enum class RoundingMode {
    ToZero = FE_TOWARDZERO,
    Up = FE_UPWARD,
    Down = FE_DOWNWARD,
    ToEven = FE_TONEAREST
};

template<typename T>
union FloatExtractor;

#if ARCH(I386) || ARCH(X86_64)
// This assumes long double is 80 bits, which is true with GCC on Intel platforms
template<>
union FloatExtractor<long double> {
    static const int mantissa_bits = 64;
    static const unsigned long long mantissa_max = ~0u;
    static const int exponent_bias = 16383;
    static const int exponent_bits = 15;
    static const unsigned exponent_max = 32767;
    struct {
        unsigned long long mantissa;
        unsigned exponent : 15;
        unsigned sign : 1;
    };
    long double d;
};
#endif

template<>
union FloatExtractor<double> {
    static const int mantissa_bits = 52;
    static const unsigned long long mantissa_max = (1ull << 52) - 1;
    static const int exponent_bias = 1023;
    static const int exponent_bits = 11;
    static const unsigned exponent_max = 2047;
    struct {
        unsigned long long mantissa : 52;
        unsigned exponent : 11;
        unsigned sign : 1;
    };
    double d;
};

template<>
union FloatExtractor<float> {
    static const int mantissa_bits = 23;
    static const unsigned mantissa_max = (1 << 23) - 1;
    static const int exponent_bias = 127;
    static const int exponent_bits = 8;
    static const unsigned exponent_max = 255;
    struct {
        unsigned long long mantissa : 23;
        unsigned exponent : 8;
        unsigned sign : 1;
    };
    float d;
};

// This is much branchier than it really needs to be
template<typename FloatType>
static FloatType internal_to_integer(FloatType x, RoundingMode rounding_mode)
{
    if (!isfinite(x))
        return x;
    using Extractor = FloatExtractor<decltype(x)>;
    Extractor extractor;
    extractor.d = x;
    auto unbiased_exponent = extractor.exponent - Extractor::exponent_bias;
    bool round = false;
    bool guard = false;
    if (unbiased_exponent < 0) {
        // it was easier to special case [0..1) as it saves us from
        // handling subnormals, underflows, etc
        if (unbiased_exponent == -1) {
            round = true;
        }
        guard = extractor.mantissa != 0;
        extractor.mantissa = 0;
        extractor.exponent = 0;
    } else {
        if (unbiased_exponent >= Extractor::mantissa_bits)
            return x;
        auto dead_bitcount = Extractor::mantissa_bits - unbiased_exponent;
        auto dead_mask = (1ull << dead_bitcount) - 1;
        auto dead_bits = extractor.mantissa & dead_mask;
        extractor.mantissa &= ~dead_mask;

        auto guard_mask = dead_mask >> 1;
        guard = (dead_bits & guard_mask) != 0;
        round = (dead_bits & ~guard_mask) != 0;
    }
    bool should_round = false;
    switch (rounding_mode) {
    case RoundingMode::ToEven:
        should_round = round;
        break;
    case RoundingMode::Up:
        if (!extractor.sign)
            should_round = guard || round;
        break;
    case RoundingMode::Down:
        if (extractor.sign)
            should_round = guard || round;
        break;
    case RoundingMode::ToZero:
        break;
    }
    if (should_round) {
        // We could do this ourselves, but this saves us from manually
        // handling overflow.
        if (extractor.sign)
            extractor.d -= 1.0;
        else
            extractor.d += 1.0;
    }

    return extractor.d;
}

// This is much branchier than it really needs to be
template<typename FloatType>
static FloatType internal_nextafter(FloatType x, bool up)
{
    if (!isfinite(x))
        return x;
    using Extractor = FloatExtractor<decltype(x)>;
    Extractor extractor;
    extractor.d = x;
    if (x == 0) {
        if (!extractor.sign) {
            extractor.mantissa = 1;
            extractor.sign = !up;
            return extractor.d;
        }
        if (up) {
            extractor.sign = false;
            extractor.mantissa = 1;
            return extractor.d;
        }
        extractor.mantissa = 1;
        extractor.sign = up != extractor.sign;
        return extractor.d;
    }
    if (up != extractor.sign) {
        extractor.mantissa++;
        if (!extractor.mantissa) {
            // no need to normalize the mantissa as we just hit a power
            // of two.
            extractor.exponent++;
            if (extractor.exponent == Extractor::exponent_max) {
                extractor.exponent = Extractor::exponent_max - 1;
                extractor.mantissa = Extractor::mantissa_max;
            }
        }
        return extractor.d;
    }

    if (!extractor.mantissa) {
        if (extractor.exponent) {
            extractor.exponent--;
            extractor.mantissa = Extractor::mantissa_max;
        } else {
            extractor.d = 0;
        }
        return extractor.d;
    }

    extractor.mantissa--;
    if (extractor.mantissa != Extractor::mantissa_max)
        return extractor.d;
    if (extractor.exponent) {
        extractor.exponent--;
        // normalize
        extractor.mantissa <<= 1;
    } else {
        if (extractor.sign) {
            // Negative infinity
            extractor.mantissa = 0;
            extractor.exponent = Extractor::exponent_max;
        }
    }
    return extractor.d;
}

template<typename FloatT>
static int internal_ilogb(FloatT x) NOEXCEPT
{
    if (x == 0)
        return FP_ILOGB0;

    if (isnan(x))
        return FP_ILOGNAN;

    if (!isfinite(x))
        return INT_MAX;

    using Extractor = FloatExtractor<FloatT>;

    Extractor extractor;
    extractor.d = x;

    return (int)extractor.exponent - Extractor::exponent_bias;
}

template<typename FloatT>
static FloatT internal_scalbn(FloatT x, int exponent) NOEXCEPT
{
    if (x == 0 || !isfinite(x) || isnan(x) || exponent == 0)
        return x;

    using Extractor = FloatExtractor<FloatT>;
    Extractor extractor;
    extractor.d = x;

    if (extractor.exponent != 0) {
        extractor.exponent = clamp((int)extractor.exponent + exponent, 0, (int)Extractor::exponent_max);
        return extractor.d;
    }

    unsigned leading_mantissa_zeroes = extractor.mantissa == 0 ? 32 : __builtin_clz(extractor.mantissa);
    int shift = min((int)leading_mantissa_zeroes, exponent);
    exponent = max(exponent - shift, 0);

    extractor.exponent <<= shift;
    extractor.exponent = exponent + 1;

    return extractor.d;
}

template<typename FloatT>
static FloatT internal_copysign(FloatT x, FloatT y) NOEXCEPT
{
    using Extractor = FloatExtractor<FloatT>;
    Extractor ex, ey;
    ex.d = x;
    ey.d = y;
    ex.sign = ey.sign;
    return ex.d;
}

extern "C" {

float nanf(const char* s) NOEXCEPT
{
    return __builtin_nanf(s);
}

double nan(const char* s) NOEXCEPT
{
    return __builtin_nan(s);
}

long double nanl(const char* s) NOEXCEPT
{
    return __builtin_nanl(s);
}

double trunc(double x) NOEXCEPT
{
    return internal_to_integer(x, RoundingMode::ToZero);
}

float truncf(float x) NOEXCEPT
{
    return internal_to_integer(x, RoundingMode::ToZero);
}

long double truncl(long double x) NOEXCEPT
{
    return internal_to_integer(x, RoundingMode::ToZero);
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
    if (isnan(y))
        return y;
    if (y == 0)
        return 1;
    if (x == 0)
        return 0;
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
    return exp2(y * log2(x));
}

float powf(float x, float y) NOEXCEPT
{
    return (float)pow(x, y);
}

// On systems where FLT_RADIX == 2, ldexp is equivalent to scalbn
long double ldexpl(long double x, int exp) NOEXCEPT
{
    return internal_scalbn(x, exp);
}

double ldexp(double x, int exp) NOEXCEPT
{
    return internal_scalbn(x, exp);
}

float ldexpf(float x, int exp) NOEXCEPT
{
    return internal_scalbn(x, exp);
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

float tanf(float angle) NOEXCEPT
{
    return (float)tan((double)angle);
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
    double ret = 0.0;
    __asm__(
        "fldlg2\n"
        "fld %%st(1)\n"
        "fyl2x\n"
        "fstp %%st(1)"
        : "=t"(ret)
        : "0"(x));
    return ret;
}

double log(double x) NOEXCEPT
{
    double ret = 0.0;
    __asm__(
        "fldln2\n"
        "fld %%st(1)\n"
        "fyl2x\n"
        "fstp %%st(1)"
        : "=t"(ret)
        : "0"(x));
    return ret;
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
    double res = 0;
    __asm__("fldl2e\n"
            "fmulp\n"
            "fld1\n"
            "fld %%st(1)\n"
            "fprem\n"
            "f2xm1\n"
            "faddp\n"
            "fscale\n"
            "fstp %%st(1)"
            : "=t"(res)
            : "0"(exponent));
    return res;
}

float expf(float exponent) NOEXCEPT
{
    return (float)exp(exponent);
}

double exp2(double exponent) NOEXCEPT
{
    double res = 0;
    __asm__("fld1\n"
            "fld %%st(1)\n"
            "fprem\n"
            "f2xm1\n"
            "faddp\n"
            "fscale\n"
            "fstp %%st(1)"
            : "=t"(res)
            : "0"(exponent));
    return res;
}

float exp2f(float exponent) NOEXCEPT
{
    return (float)exp2(exponent);
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

int ilogbl(long double x) NOEXCEPT
{
    return internal_ilogb(x);
}

int ilogb(double x) NOEXCEPT
{
    return internal_ilogb(x);
}

int ilogbf(float x) NOEXCEPT
{
    return internal_ilogb(x);
}

long double logbl(long double x) NOEXCEPT
{
    return ilogbl(x);
}

double logb(double x) NOEXCEPT
{
    return ilogb(x);
}

float logbf(float x) NOEXCEPT
{
    return ilogbf(x);
}

double log2(double x) NOEXCEPT
{
    double ret = 0.0;
    __asm__(
        "fld1\n"
        "fld %%st(1)\n"
        "fyl2x\n"
        "fstp %%st(1)"
        : "=t"(ret)
        : "0"(x));
    return ret;
}

float log2f(float x) NOEXCEPT
{
    return log2(x);
}

long double log2l(long double x) NOEXCEPT
{
    return log2(x);
}

double frexp(double x, int* exp) NOEXCEPT
{
    *exp = (x == 0) ? 0 : (1 + ilogb(x));
    return scalbn(x, -(*exp));
}

float frexpf(float x, int* exp) NOEXCEPT
{
    *exp = (x == 0) ? 0 : (1 + ilogbf(x));
    return scalbnf(x, -(*exp));
}

long double frexpl(long double x, int* exp) NOEXCEPT
{
    *exp = (x == 0) ? 0 : (1 + ilogbl(x));
    return scalbnl(x, -(*exp));
}

double round(double value) NOEXCEPT
{
    return internal_to_integer(value, RoundingMode::ToEven);
}

float roundf(float value) NOEXCEPT
{
    return internal_to_integer(value, RoundingMode::ToEven);
}

long double roundl(long double value) NOEXCEPT
{
    return internal_to_integer(value, RoundingMode::ToEven);
}

float floorf(float value) NOEXCEPT
{
    return internal_to_integer(value, RoundingMode::Down);
}

double floor(double value) NOEXCEPT
{
    return internal_to_integer(value, RoundingMode::Down);
}

long double floorl(long double value) NOEXCEPT
{
    return internal_to_integer(value, RoundingMode::Down);
}

long double rintl(long double value) NOEXCEPT
{
    return internal_to_integer(value, RoundingMode { fegetround() });
}

double rint(double value) NOEXCEPT
{
    return internal_to_integer(value, RoundingMode { fegetround() });
}

float rintf(float value) NOEXCEPT
{
    return internal_to_integer(value, RoundingMode { fegetround() });
}

long lrintl(long double value) NOEXCEPT
{
    return (long)internal_to_integer(value, RoundingMode { fegetround() });
}

long lrint(double value) NOEXCEPT
{
    return (long)internal_to_integer(value, RoundingMode { fegetround() });
}

long lrintf(float value) NOEXCEPT
{
    return (long)internal_to_integer(value, RoundingMode { fegetround() });
}

long long llrintl(long double value) NOEXCEPT
{
    return (long long)internal_to_integer(value, RoundingMode { fegetround() });
}

long long llrint(double value) NOEXCEPT
{
    return (long long)internal_to_integer(value, RoundingMode { fegetround() });
}

long long llrintf(float value) NOEXCEPT
{
    return (long long)internal_to_integer(value, RoundingMode { fegetround() });
}

float ceilf(float value) NOEXCEPT
{
    return internal_to_integer(value, RoundingMode::Up);
}

double ceil(double value) NOEXCEPT
{
    return internal_to_integer(value, RoundingMode::Up);
}

long double ceill(long double value) NOEXCEPT
{
    return internal_to_integer(value, RoundingMode::Up);
}

double modf(double x, double* intpart) NOEXCEPT
{
    double integer_part = internal_to_integer(x, RoundingMode::ToZero);
    *intpart = integer_part;
    auto fraction = x - integer_part;
    if (signbit(fraction) != signbit(x))
        fraction = -fraction;
    return fraction;
}

double gamma(double x) NOEXCEPT
{
    // Stirling approximation
    return sqrt(2.0 * M_PI / x) * pow(x / M_E, x);
}

double expm1(double x) NOEXCEPT
{
    return exp(x) - 1;
}

double cbrt(double x) NOEXCEPT
{
    if (isinf(x) || x == 0)
        return x;
    if (x < 0)
        return -cbrt(-x);

    double r = x;
    double ex = 0;

    while (r < 0.125) {
        r *= 8;
        ex--;
    }
    while (r > 1.0) {
        r *= 0.125;
        ex++;
    }

    r = (-0.46946116 * r + 1.072302) * r + 0.3812513;

    while (ex < 0) {
        r *= 0.5;
        ex++;
    }
    while (ex > 0) {
        r *= 2;
        ex--;
    }

    r = (2.0 / 3.0) * r + (1.0 / 3.0) * x / (r * r);
    r = (2.0 / 3.0) * r + (1.0 / 3.0) * x / (r * r);
    r = (2.0 / 3.0) * r + (1.0 / 3.0) * x / (r * r);
    r = (2.0 / 3.0) * r + (1.0 / 3.0) * x / (r * r);

    return r;
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

double nextafter(double x, double target) NOEXCEPT
{
    if (x == target)
        return target;
    return internal_nextafter(x, target >= x);
}

float nextafterf(float x, float target) NOEXCEPT
{
    if (x == target)
        return target;
    return internal_nextafter(x, target >= x);
}

long double nextafterl(long double, long double) NOEXCEPT
{
    TODO();
}

double nexttoward(double x, long double target) NOEXCEPT
{
    if (x == target)
        return target;
    return internal_nextafter(x, target >= x);
}

float nexttowardf(float x, long double target) NOEXCEPT
{
    if (x == target)
        return target;
    return internal_nextafter(x, target >= x);
}

long double nexttowardl(long double, long double) NOEXCEPT
{
    TODO();
}

float copysignf(float x, float y) NOEXCEPT
{
    return internal_copysign(x, y);
}

double copysign(double x, double y) NOEXCEPT
{
    return internal_copysign(x, y);
}

long double copysignl(long double x, long double y) NOEXCEPT
{
    return internal_copysign(x, y);
}

float scalbnf(float x, int exponent) NOEXCEPT
{
    return internal_scalbn(x, exponent);
}

double scalbn(double x, int exponent) NOEXCEPT
{
    return internal_scalbn(x, exponent);
}

long double scalbnl(long double x, int exponent) NOEXCEPT
{
    return internal_scalbn(x, exponent);
}

float scalbnlf(float x, long exponent) NOEXCEPT
{
    return internal_scalbn(x, exponent);
}

double scalbln(double x, long exponent) NOEXCEPT
{
    return internal_scalbn(x, exponent);
}

long double scalblnl(long double x, long exponent) NOEXCEPT
{
    return internal_scalbn(x, exponent);
}
}
