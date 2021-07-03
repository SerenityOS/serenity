/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mițca Dumitru <dumitru0mitca@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ExtraMathConstants.h>
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
            extractor.d -= static_cast<FloatType>(1.0);
        else
            extractor.d += static_cast<FloatType>(1.0);
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
static FloatT internal_modf(FloatT x, FloatT* intpart) NOEXCEPT
{
    FloatT integer_part = internal_to_integer(x, RoundingMode::ToZero);
    *intpart = integer_part;
    auto fraction = x - integer_part;
    if (signbit(fraction) != signbit(x))
        fraction = -fraction;
    return fraction;
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

template<typename FloatT>
static FloatT internal_gamma(FloatT x) NOEXCEPT
{
    if (isnan(x))
        return (FloatT)NAN;

    if (x == (FloatT)0.0)
        return signbit(x) ? (FloatT)-INFINITY : (FloatT)INFINITY;

    if (x < (FloatT)0 && (rintl(x) == x || isinf(x)))
        return (FloatT)NAN;

    if (isinf(x))
        return (FloatT)INFINITY;

    using Extractor = FloatExtractor<FloatT>;
    // These constants were obtained through use of WolframAlpha
    constexpr long long max_integer_whose_factorial_fits = (Extractor::mantissa_bits == FloatExtractor<long double>::mantissa_bits ? 20 : (Extractor::mantissa_bits == FloatExtractor<double>::mantissa_bits ? 18 : (Extractor::mantissa_bits == FloatExtractor<float>::mantissa_bits ? 10 : 0)));
    static_assert(max_integer_whose_factorial_fits != 0, "internal_gamma needs to be aware of the integer factorial that fits in this floating point type.");
    if (rintl(x) == (long double)x && x <= max_integer_whose_factorial_fits) {
        long long result = 1;
        for (long long cursor = 1; cursor <= min(max_integer_whose_factorial_fits, (long long)x); cursor++)
            result *= cursor;
        return (FloatT)result;
    }

    // Stirling approximation
    return sqrtl(2.0 * M_PI / static_cast<long double>(x)) * powl(static_cast<long double>(x) / M_E, static_cast<long double>(x));
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

long double cosl(long double angle) NOEXCEPT
{
    return sinl(angle + M_PI_2);
}

double cos(double angle) NOEXCEPT
{
    return sin(angle + M_PI_2);
}

float cosf(float angle) NOEXCEPT
{
    return sinf(angle + static_cast<float>(M_PI_2));
}

long double sinl(long double angle) NOEXCEPT
{
    long double ret = 0.0;
    __asm__(
        "fsin"
        : "=t"(ret)
        : "0"(angle));

    return ret;
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

long double powl(long double x, long double y) NOEXCEPT
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
    if (y == (long double)y_as_int) {
        long double result = x;
        for (int i = 0; i < fabsl(y) - 1; ++i)
            result *= x;
        if (y < 0)
            result = 1.0l / result;
        return result;
    }
    return exp2l(y * log2l(x));
}

double pow(double x, double y) NOEXCEPT
{
    return (double)powl(x, y);
}

float powf(float x, float y) NOEXCEPT
{
    return (float)powl(x, y);
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

long double tanhl(long double x) NOEXCEPT
{
    if (x > 0) {
        long double exponentiated = expl(2 * x);
        return (exponentiated - 1) / (exponentiated + 1);
    }
    long double plusX = expl(x);
    long double minusX = 1 / plusX;
    return (plusX - minusX) / (plusX + minusX);
}

double tanh(double x) NOEXCEPT
{
    return (double)tanhl(x);
}

float tanhf(float x) NOEXCEPT
{
    return (float)tanhl(x);
}

[[maybe_unused]] static long double ampsin(long double angle) NOEXCEPT
{
    long double looped_angle = fmodl(M_PI + angle, M_TAU) - M_PI;
    long double looped_angle_squared = looped_angle * looped_angle;

    long double quadratic_term;
    if (looped_angle > 0) {
        quadratic_term = -looped_angle_squared;
    } else {
        quadratic_term = looped_angle_squared;
    }

    long double linear_term = M_PI * looped_angle;

    return quadratic_term + linear_term;
}

long double tanl(long double angle) NOEXCEPT
{
    long double ret = 0.0, one;
    __asm__(
        "fptan"
        : "=t"(one), "=u"(ret)
        : "0"(angle));

    return ret;
}

double tan(double angle) NOEXCEPT
{
    return (double)tanl(angle);
}

float tanf(float angle) NOEXCEPT
{
    return (float)tanl(angle);
}

long double sqrtl(long double x) NOEXCEPT
{
    long double res;
    asm("fsqrt"
        : "=t"(res)
        : "0"(x));
    return res;
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

long double sinhl(long double x) NOEXCEPT
{
    long double exponentiated = expl(x);
    if (x > 0)
        return (exponentiated * exponentiated - 1) / 2 / exponentiated;
    return (exponentiated - 1 / exponentiated) / 2;
}

double sinh(double x) NOEXCEPT
{
    return (double)sinhl(x);
}

float sinhf(float x) NOEXCEPT
{
    return (float)sinhl(x);
}

long double log10l(long double x) NOEXCEPT
{
    long double ret = 0.0l;
    __asm__(
        "fldlg2\n"
        "fld %%st(1)\n"
        "fyl2x\n"
        "fstp %%st(1)"
        : "=t"(ret)
        : "0"(x));
    return ret;
}

double log10(double x) NOEXCEPT
{
    return (double)log10l(x);
}

float log10f(float x) NOEXCEPT
{
    return (float)log10l(x);
}

long double logl(long double x) NOEXCEPT
{
    long double ret = 0.0l;
    asm(
        "fldln2\n"
        "fld %%st(1)\n"
        "fyl2x\n"
        "fstp %%st(1)"
        : "=t"(ret)
        : "0"(x));
    return ret;
}

double log(double x) NOEXCEPT
{
    return (double)logl(x);
}

float logf(float x) NOEXCEPT
{
    return (float)logl(x);
}

long double fmodl(long double index, long double period) NOEXCEPT
{
    return index - truncl(index / period) * period;
}

double fmod(double index, double period) NOEXCEPT
{
    return index - trunc(index / period) * period;
}

float fmodf(float index, float period) NOEXCEPT
{
    return index - truncf(index / period) * period;
}

// FIXME: These aren't exactly like fmod, but these definitions are probably good enough for now
long double remainderl(long double x, long double y) NOEXCEPT
{
    return fmodl(x, y);
}

double remainder(double x, double y) NOEXCEPT
{
    return fmod(x, y);
}

float remainderf(float x, float y) NOEXCEPT
{
    return fmodf(x, y);
}

long double expl(long double exponent) NOEXCEPT
{
    long double res = 0;
    asm("fldl2e\n"
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

double exp(double exponent) NOEXCEPT
{
    return (double)expl(exponent);
}

float expf(float exponent) NOEXCEPT
{
    return (float)expl(exponent);
}

long double exp2l(long double exponent) NOEXCEPT
{
    long double res = 0;
    asm("fld1\n"
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

double exp2(double exponent) NOEXCEPT
{
    return (double)exp2l(exponent);
}

float exp2f(float exponent) NOEXCEPT
{
    return (float)exp2l(exponent);
}

long double coshl(long double x) NOEXCEPT
{
    long double exponentiated = expl(-x);
    if (x < 0)
        return (1 + exponentiated * exponentiated) / 2 / exponentiated;
    return (1 / exponentiated + exponentiated) / 2;
}

double cosh(double x) NOEXCEPT
{
    return (double)coshl(x);
}

float coshf(float x) NOEXCEPT
{
    return (float)coshl(x);
}

long double atan2l(long double y, long double x) NOEXCEPT
{
    if (x == 0) {
        if (y > 0)
            return M_PI_2;
        if (y < 0)
            return -M_PI_2;
        return 0;
    }

    long double result = 0; //atanl(y / x);
    __asm__("fpatan"
            : "=t"(result)
            : "0"(x), "u"(y)
            : "st(1)");
    return result;
}

double atan2(double y, double x) NOEXCEPT
{
    return (double)atan2l(y, x);
}

float atan2f(float y, float x) NOEXCEPT
{
    return (float)atan2l(y, x);
}

long double atanl(long double x) NOEXCEPT
{
    if (x < 0)
        return -atanl(-x);
    if (x > 1)
        return M_PI_2 - atanl(1 / x);
    long double squared = x * x;
    return x / (1 + 1 * 1 * squared / (3 + 2 * 2 * squared / (5 + 3 * 3 * squared / (7 + 4 * 4 * squared / (9 + 5 * 5 * squared / (11 + 6 * 6 * squared / (13 + 7 * 7 * squared)))))));
}

double atan(double x) NOEXCEPT
{
    return (double)atanl(x);
}

float atanf(float x) NOEXCEPT
{
    return (float)atanl(x);
}

long double asinl(long double x) NOEXCEPT
{
    if (x > 1 || x < -1)
        return NAN;
    if (x > 0.5 || x < -0.5)
        return 2 * atanl(x / (1 + sqrtl(1 - x * x)));
    long double squared = x * x;
    long double value = x;
    long double i = x * squared;
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

double asin(double x) NOEXCEPT
{
    return (double)asinl(x);
}

float asinf(float x) NOEXCEPT
{
    return (float)asinl(x);
}

long double acosl(long double x) NOEXCEPT
{
    return M_PI_2 - asinl(x);
}

double acos(double x) NOEXCEPT
{
    return M_PI_2 - asin(x);
}

float acosf(float x) NOEXCEPT
{
    return static_cast<float>(M_PI_2) - asinf(x);
}

long double fabsl(long double value) NOEXCEPT
{
    return value < 0 ? -value : value;
}

double fabs(double value) NOEXCEPT
{
    return value < 0 ? -value : value;
}

float fabsf(float value) NOEXCEPT
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

long double log2l(long double x) NOEXCEPT
{
    long double ret = 0.0;
    asm(
        "fld1\n"
        "fld %%st(1)\n"
        "fyl2x\n"
        "fstp %%st(1)"
        : "=t"(ret)
        : "0"(x));
    return ret;
}

double log2(double x) NOEXCEPT
{
    return (double)log2l(x);
}

float log2f(float x) NOEXCEPT
{
    return (float)log2l(x);
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

long lroundf(float value) NOEXCEPT
{
    return internal_to_integer(value, RoundingMode::ToEven);
}

long lround(double value) NOEXCEPT
{
    return internal_to_integer(value, RoundingMode::ToEven);
}

long lroundl(long double value) NOEXCEPT
{
    return internal_to_integer(value, RoundingMode::ToEven);
}

long long llroundf(float value) NOEXCEPT
{
    return internal_to_integer(value, RoundingMode::ToEven);
}

long long llround(double value) NOEXCEPT
{
    return internal_to_integer(value, RoundingMode::ToEven);
}

long long llroundd(long double value) NOEXCEPT
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

long double modfl(long double x, long double* intpart) NOEXCEPT
{
    return internal_modf(x, intpart);
}

double modf(double x, double* intpart) NOEXCEPT
{
    return internal_modf(x, intpart);
}

float modff(float x, float* intpart) NOEXCEPT
{
    return internal_modf(x, intpart);
}

double gamma(double x) NOEXCEPT
{
    // Stirling approximation
    return sqrt(2.0 * M_PI / x) * pow(x / M_E, x);
}

long double tgammal(long double value) NOEXCEPT
{
    return internal_gamma(value);
}

double tgamma(double value) NOEXCEPT
{
    return internal_gamma(value);
}

float tgammaf(float value) NOEXCEPT
{
    return internal_gamma(value);
}

int signgam = 0;

long double lgammal(long double value) NOEXCEPT
{
    return lgammal_r(value, &signgam);
}

double lgamma(double value) NOEXCEPT
{
    return lgamma_r(value, &signgam);
}

float lgammaf(float value) NOEXCEPT
{
    return lgammaf_r(value, &signgam);
}

long double lgammal_r(long double value, int* sign) NOEXCEPT
{
    if (value == 1.0 || value == 2.0)
        return 0.0;
    if (isinf(value) || value == 0.0)
        return INFINITY;
    long double result = logl(internal_gamma(value));
    *sign = signbit(result) ? -1 : 1;
    return result;
}

double lgamma_r(double value, int* sign) NOEXCEPT
{
    if (value == 1.0 || value == 2.0)
        return 0.0;
    if (isinf(value) || value == 0.0)
        return INFINITY;
    double result = log(internal_gamma(value));
    *sign = signbit(result) ? -1 : 1;
    return result;
}

float lgammaf_r(float value, int* sign) NOEXCEPT
{
    if (value == 1.0f || value == 2.0f)
        return 0.0;
    if (isinf(value) || value == 0.0f)
        return INFINITY;
    float result = logf(internal_gamma(value));
    *sign = signbit(result) ? -1 : 1;
    return result;
}

long double expm1l(long double x) NOEXCEPT
{
    return expl(x) - 1;
}

double expm1(double x) NOEXCEPT
{
    return exp(x) - 1;
}

float expm1f(float x) NOEXCEPT
{
    return expf(x) - 1;
}

long double cbrtl(long double x) NOEXCEPT
{
    if (isinf(x) || x == 0)
        return x;
    if (x < 0)
        return -cbrtl(-x);

    long double r = x;
    long double ex = 0;

    while (r < 0.125l) {
        r *= 8;
        ex--;
    }
    while (r > 1.0l) {
        r *= 0.125l;
        ex++;
    }

    r = (-0.46946116l * r + 1.072302l) * r + 0.3812513l;

    while (ex < 0) {
        r *= 0.5l;
        ex++;
    }
    while (ex > 0) {
        r *= 2.0l;
        ex--;
    }

    r = (2.0l / 3.0l) * r + (1.0l / 3.0l) * x / (r * r);
    r = (2.0l / 3.0l) * r + (1.0l / 3.0l) * x / (r * r);
    r = (2.0l / 3.0l) * r + (1.0l / 3.0l) * x / (r * r);
    r = (2.0l / 3.0l) * r + (1.0l / 3.0l) * x / (r * r);

    return r;
}

double cbrt(double x) NOEXCEPT
{
    return (double)cbrtl(x);
}

float cbrtf(float x) NOEXCEPT
{
    return (float)cbrtl(x);
}

long double log1pl(long double x) NOEXCEPT
{
    return logl(1 + x);
}

double log1p(double x) NOEXCEPT
{
    return log(1 + x);
}

float log1pf(float x) NOEXCEPT
{
    return logf(1 + x);
}

long double acoshl(long double x) NOEXCEPT
{
    return logl(x + sqrtl(x * x - 1));
}

double acosh(double x) NOEXCEPT
{
    return log(x + sqrt(x * x - 1));
}

float acoshf(float x) NOEXCEPT
{
    return logf(x + sqrtf(x * x - 1));
}

long double asinhl(long double x) NOEXCEPT
{
    return logl(x + sqrtl(x * x + 1));
}

double asinh(double x) NOEXCEPT
{
    return log(x + sqrt(x * x + 1));
}

float asinhf(float x) NOEXCEPT
{
    return logf(x + sqrtf(x * x + 1));
}

long double atanhl(long double x) NOEXCEPT
{
    return logl((1 + x) / (1 - x)) / 2.0l;
}

double atanh(double x) NOEXCEPT
{
    return log((1 + x) / (1 - x)) / 2.0;
}

float atanhf(float x) NOEXCEPT
{
    return logf((1 + x) / (1 - x)) / 2.0f;
}

long double hypotl(long double x, long double y) NOEXCEPT
{
    return sqrtl(x * x + y * y);
}

double hypot(double x, double y) NOEXCEPT
{
    return sqrt(x * x + y * y);
}

float hypotf(float x, float y) NOEXCEPT
{
    return sqrtf(x * x + y * y);
}

long double erfl(long double x) NOEXCEPT
{
    // algorithm taken from Abramowitz and Stegun (no. 26.2.17)
    long double t = 1 / (1 + 0.47047l * fabsl(x));
    long double poly = t * (0.3480242l + t * (-0.958798l + t * 0.7478556l));
    long double answer = 1 - poly * expl(-x * x);
    if (x < 0)
        return -answer;

    return answer;
}

double erf(double x) NOEXCEPT
{
    return (double)erfl(x);
}

float erff(float x) NOEXCEPT
{
    return (float)erf(x);
}

long double erfcl(long double x) NOEXCEPT
{
    return 1 - erfl(x);
}

double erfc(double x) NOEXCEPT
{
    return 1 - erf(x);
}

float erfcf(float x) NOEXCEPT
{
    return 1 - erff(x);
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

long double nextafterl(long double x, long double target) NOEXCEPT
{
    return internal_nextafter(x, target >= x);
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

long double nexttowardl(long double x, long double target) NOEXCEPT
{
    if (x == target)
        return target;
    return internal_nextafter(x, target >= x);
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

long double fmaxl(long double x, long double y) NOEXCEPT
{
    if (isnan(x))
        return y;
    if (isnan(y))
        return x;

    return x > y ? x : y;
}

double fmax(double x, double y) NOEXCEPT
{
    if (isnan(x))
        return y;
    if (isnan(y))
        return x;

    return x > y ? x : y;
}

float fmaxf(float x, float y) NOEXCEPT
{
    if (isnan(x))
        return y;
    if (isnan(y))
        return x;

    return x > y ? x : y;
}

long double fminl(long double x, long double y) NOEXCEPT
{
    if (isnan(x))
        return y;
    if (isnan(y))
        return x;

    return x < y ? x : y;
}

double fmin(double x, double y) NOEXCEPT
{
    if (isnan(x))
        return y;
    if (isnan(y))
        return x;

    return x < y ? x : y;
}

float fminf(float x, float y) NOEXCEPT
{
    if (isnan(x))
        return y;
    if (isnan(y))
        return x;

    return x < y ? x : y;
}

long double nearbyintl(long double value) NOEXCEPT
{
    return internal_to_integer(value, RoundingMode { fegetround() });
}

double nearbyint(double value) NOEXCEPT
{
    return internal_to_integer(value, RoundingMode { fegetround() });
}

float nearbyintf(float value) NOEXCEPT
{
    return internal_to_integer(value, RoundingMode { fegetround() });
}
}
