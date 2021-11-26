/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Mițca Dumitru <dumitru0mitca@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ExtraMathConstants.h>
#include <AK/Math.h>
#include <AK/Platform.h>
#include <AK/StdLibExtras.h>
#include <LibC/assert.h>
#include <fenv.h>
#include <math.h>
#include <stdint.h>
#include <stdlib.h>

#ifdef __clang__
#    pragma clang diagnostic push
#    pragma clang diagnostic ignored "-Wdouble-promotion"
#endif

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
    if ((int)x == x && x <= max_integer_whose_factorial_fits + 1) {
        long long result = 1;
        for (long long cursor = 2; cursor < (long long)x; cursor++)
            result *= cursor;
        return (FloatT)result;
    }

    // Stirling approximation
    return sqrtl(2.0 * M_PIl / static_cast<long double>(x)) * powl(static_cast<long double>(x) / M_El, static_cast<long double>(x));
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

#define MAKE_AK_BACKED1(name)                     \
    long double name##l(long double arg) NOEXCEPT \
    {                                             \
        return AK::name<long double>(arg);        \
    }                                             \
    double name(double arg) NOEXCEPT              \
    {                                             \
        return AK::name<double>(arg);             \
    }                                             \
    float name##f(float arg) NOEXCEPT             \
    {                                             \
        return AK::name<float>(arg);              \
    }
#define MAKE_AK_BACKED2(name)                                        \
    long double name##l(long double arg1, long double arg2) NOEXCEPT \
    {                                                                \
        return AK::name<long double>(arg1, arg2);                    \
    }                                                                \
    double name(double arg1, double arg2) NOEXCEPT                   \
    {                                                                \
        return AK::name<double>(arg1, arg2);                         \
    }                                                                \
    float name##f(float arg1, float arg2) NOEXCEPT                   \
    {                                                                \
        return AK::name<float>(arg1, arg2);                          \
    }

MAKE_AK_BACKED1(sin);
MAKE_AK_BACKED1(cos);
MAKE_AK_BACKED1(tan);
MAKE_AK_BACKED1(asin);
MAKE_AK_BACKED1(acos);
MAKE_AK_BACKED1(atan);
MAKE_AK_BACKED1(sinh);
MAKE_AK_BACKED1(cosh);
MAKE_AK_BACKED1(tanh);
MAKE_AK_BACKED1(asinh);
MAKE_AK_BACKED1(acosh);
MAKE_AK_BACKED1(atanh);
MAKE_AK_BACKED1(sqrt);
MAKE_AK_BACKED1(cbrt);
MAKE_AK_BACKED1(log);
MAKE_AK_BACKED1(log2);
MAKE_AK_BACKED1(log10);
MAKE_AK_BACKED1(exp);
MAKE_AK_BACKED1(exp2);
MAKE_AK_BACKED1(fabs);

MAKE_AK_BACKED2(atan2);
MAKE_AK_BACKED2(hypot);
MAKE_AK_BACKED2(fmod);
MAKE_AK_BACKED2(pow);
MAKE_AK_BACKED2(remainder);

long double truncl(long double x) NOEXCEPT
{
    if (fabsl(x) < LONG_LONG_MAX) {
        // This is 1.6 times faster than the implementation using the "internal_to_integer"
        // helper (on x86_64)
        // https://quick-bench.com/q/xBmxuY8am9qibSYVna90Y6PIvqA
        u64 temp;
        asm(
            "fisttpq %[temp]\n"
            "fildq %[temp]"
            : "+t"(x)
            : [temp] "m"(temp));
        return x;
    }

    return internal_to_integer(x, RoundingMode::ToZero);
}

double trunc(double x) NOEXCEPT
{
    if (fabs(x) < LONG_LONG_MAX) {
        u64 temp;
        asm(
            "fisttpq %[temp]\n"
            "fildq %[temp]"
            : "+t"(x)
            : [temp] "m"(temp));
        return x;
    }

    return internal_to_integer(x, RoundingMode::ToZero);
}

float truncf(float x) NOEXCEPT
{
    if (fabsf(x) < LONG_LONG_MAX) {
        u64 temp;
        asm(
            "fisttpq %[temp]\n"
            "fildq %[temp]"
            : "+t"(x)
            : [temp] "m"(temp));
        return x;
    }

    return internal_to_integer(x, RoundingMode::ToZero);
}

long double rintl(long double value)
{
    double res;
    asm(
        "frndint\n"
        : "=t"(res)
        : "0"(value));
    return res;
}
double rint(double value)
{
    double res;
    asm(
        "frndint\n"
        : "=t"(res)
        : "0"(value));
    return res;
}
float rintf(float value)
{
    double res;
    asm(
        "frndint\n"
        : "=t"(res)
        : "0"(value));
    return res;
}

long lrintl(long double value)
{
    long res;
    asm(
        "fistpl %0\n"
        : "+m"(res)
        : "t"(value)
        : "st");
    return res;
}
long lrint(double value)
{
    long res;
    asm(
        "fistpl %0\n"
        : "+m"(res)
        : "t"(value)
        : "st");
    return res;
}
long lrintf(float value)
{
    long res;
    asm(
        "fistpl %0\n"
        : "+m"(res)
        : "t"(value)
        : "st");
    return res;
}

long long llrintl(long double value)
{
    long long res;
    asm(
        "fistpq %0\n"
        : "+m"(res)
        : "t"(value)
        : "st");
    return res;
}
long long llrint(double value)
{
    long long res;
    asm(
        "fistpq %0\n"
        : "+m"(res)
        : "t"(value)
        : "st");
    return res;
}
long long llrintf(float value)
{
    long long res;
    asm(
        "fistpq %0\n"
        : "+m"(res)
        : "t"(value)
        : "st");
    return res;
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

#ifdef __clang__
#    pragma clang diagnostic pop
#endif
