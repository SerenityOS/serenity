/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#ifdef __clang__
#    pragma clang optimize off
#else
#    pragma GCC optimize("O0")
#endif

#include <LibTest/TestCase.h>

#include <float.h>
#include <math.h>

TEST_CASE(atan2)
{
    EXPECT_APPROXIMATE(atan2(-1, -0.0e0), -M_PI_2);
    EXPECT_APPROXIMATE(atan2(-0.0e0, -1), -M_PI);
    EXPECT_APPROXIMATE(atan2(0.0e0, -1), M_PI);
    EXPECT_APPROXIMATE(atan2(-0.0e0, 1), -0.0e0);
    EXPECT_APPROXIMATE(atan2(0.0e0, 1), 0.0e0);
}

TEST_CASE(trig)
{
    EXPECT_APPROXIMATE(sin(1234), 0.601928);
    EXPECT_APPROXIMATE(cos(1234), -0.798551);
    EXPECT_APPROXIMATE(tan(1234), -0.753775);
    EXPECT_APPROXIMATE(sqrt(1234), 35.128336);
    EXPECT_APPROXIMATE(sin(-1), -0.8414709848078965);
    EXPECT_APPROXIMATE(cos(-1), 0.5403023058681398);
    EXPECT_APPROXIMATE(tan(-1), -1.5574077246549023);
    EXPECT(isnan(sqrt(-1)));
    EXPECT(isnan(asin(1.1)));
    EXPECT(isnan(asin(-1.1)));
    EXPECT_APPROXIMATE(asin(0), 0.0);
    EXPECT_APPROXIMATE(asin(0.01), 0.01);
    EXPECT_APPROXIMATE(asin(0.1), 0.100167);
    EXPECT_APPROXIMATE(asin(0.3), 0.304693);
    EXPECT_APPROXIMATE(asin(0.499), 0.522444);
    EXPECT_APPROXIMATE(asin(0.5), 0.523599);
    EXPECT_APPROXIMATE(asin(0.501), 0.524754);
    EXPECT_APPROXIMATE(asin(0.9), 1.119770);
    EXPECT_APPROXIMATE(asin(0.99), 1.429257);
    EXPECT_APPROXIMATE(asin(1.0), 1.570796);
    EXPECT_APPROXIMATE(atan(0), 0.0);
    EXPECT_APPROXIMATE(atan(0.5), 0.463648);
    EXPECT_APPROXIMATE(atan(-0.5), -0.463648);
    EXPECT_APPROXIMATE(atan(5.5), 1.390943);
    EXPECT_APPROXIMATE(atan(-5.5), -1.390943);
    EXPECT_APPROXIMATE(atan(555.5), 1.568996);
}

TEST_CASE(other)
{
    EXPECT_EQ(trunc(9999999999999.5), 9999999999999.0);
    EXPECT_EQ(trunc(-9999999999999.5), -9999999999999.0);
}

TEST_CASE(exponents)
{
    struct values {
        double x;
        double exp;
        double sinh;
        double cosh;
        double tanh;
    };

    values values[8] {
        { 1.500000, 4.481689, 2.129279, 2.352410, 0.905148 },
        { 20.990000, 1305693298.670892, 652846649.335446, 652846649.335446, 1.000000 },
        { 20.010000, 490041186.687082, 245020593.343541, 245020593.343541, 1.000000 },
        { 0.000000, 1.000000, 0.000000, 1.000000, 0.000000 },
        { 0.010000, 1.010050, 0.010000, 1.000050, 0.010000 },
        { -0.010000, 0.990050, -0.010000, 1.000050, -0.010000 },
        { -1.000000, 0.367879, -1.175201, 1.543081, -0.761594 },
        { -17.000000, 0.000000, -12077476.376788, 12077476.376788, -1.000000 },
    };
    for (auto& v : values) {
        EXPECT_APPROXIMATE(exp(v.x), v.exp);
        EXPECT_APPROXIMATE(sinh(v.x), v.sinh);
        EXPECT_APPROXIMATE(cosh(v.x), v.cosh);
        EXPECT_APPROXIMATE(tanh(v.x), v.tanh);
    }
    EXPECT_EQ(exp(1000), __builtin_huge_val());
}

TEST_CASE(logarithms)
{
    EXPECT(isnan(log(-1)));
    EXPECT(log(0) < -1000000);
    EXPECT_APPROXIMATE(log(0.5), -0.693147);
    EXPECT_APPROXIMATE(log(1.1), 0.095310);
    EXPECT_APPROXIMATE(log(5), 1.609438);
    EXPECT_APPROXIMATE(log(5.5), 1.704748);
    EXPECT_APPROXIMATE(log(500), 6.214608);
    EXPECT_APPROXIMATE(log2(5), 2.321928);
    EXPECT_APPROXIMATE(log10(5), 0.698970);
}

union Extractor {
    explicit Extractor(double d)
        : d(d)
    {
    }
    Extractor(unsigned sign, unsigned exponent, unsigned long long mantissa)
        : mantissa(mantissa)
        , exponent(exponent)
        , sign(sign)
    {
    }
    struct {
        unsigned long long mantissa : 52;
        unsigned exponent : 11;
        unsigned sign : 1;
    };
    double d;

    bool operator==(const Extractor& other) const
    {
        return other.sign == sign && other.exponent == exponent && other.mantissa == mantissa;
    }
};
namespace AK {
template<>
struct Formatter<Extractor> : StandardFormatter {
    ErrorOr<void> format(FormatBuilder& builder, Extractor const& value)
    {
        TRY(builder.put_literal("{"));
        TRY(builder.put_u64(value.sign));
        TRY(builder.put_literal(", "));
        TRY(builder.put_u64(value.exponent, 16, true));
        TRY(builder.put_literal(", "));
        TRY(builder.put_u64(value.mantissa, 16, true));
        TRY(builder.put_literal("}"));
        return {};
    }
};
}

static Extractor nextafter_translator(Extractor x, Extractor target)
{
    return Extractor(nextafter(x.d, target.d));
}

TEST_CASE(nextafter)
{
    EXPECT_EQ(nextafter_translator(Extractor(0x0, 0x7fe, 0xfffffffffffff), Extractor(0x0, 0x7fe, 0xfffffffffffff)), Extractor(0x0, 0x7fe, 0xfffffffffffff));
    EXPECT_EQ(nextafter_translator(Extractor(0x0, 0x1, 0x0), Extractor(0x0, 0x412, 0xe848000000000)), Extractor(0x0, 0x1, 0x1));
    EXPECT_EQ(nextafter_translator(Extractor(0x0, 0x3ff, 0x0), Extractor(0x0, 0x412, 0xe848200000000)), Extractor(0x0, 0x3ff, 0x1));
    EXPECT_EQ(nextafter_translator(Extractor(0x1, 0x0, 0x0), Extractor(0x0, 0x412, 0xe848000000000)), Extractor(0x0, 0x0, 0x1));
    EXPECT_EQ(nextafter_translator(Extractor(0x0, 0x0, 0x0), Extractor(0x0, 0x412, 0xe848000000000)), Extractor(0x0, 0x0, 0x1));
    EXPECT_EQ(nextafter_translator(Extractor(0x1, 0x3ff, 0x0), Extractor(0x0, 0x412, 0xe847e00000000)), Extractor(0x1, 0x3fe, 0xfffffffffffff));
    EXPECT_EQ(nextafter_translator(Extractor(0x0, 0x0, 0x1), Extractor(0x0, 0x412, 0xe848000000000)), Extractor(0x0, 0x0, 0x2));
    EXPECT_EQ(nextafter_translator(Extractor(0x0, 0x7fe, 0xfffffffffffff), Extractor(0x0, 0x7fe, 0xfffffffffffff)), Extractor(0x0, 0x7fe, 0xfffffffffffff));
    EXPECT_EQ(nextafter_translator(Extractor(0x0, 0x412, 0xe848000000000), Extractor(0x0, 0x1, 0x0)), Extractor(0x0, 0x412, 0xe847fffffffff));
    EXPECT_EQ(nextafter_translator(Extractor(0x0, 0x412, 0xe848200000000), Extractor(0x0, 0x3ff, 0x0)), Extractor(0x0, 0x412, 0xe8481ffffffff));
    EXPECT_EQ(nextafter_translator(Extractor(0x0, 0x412, 0xe848000000000), Extractor(0x1, 0x0, 0x0)), Extractor(0x0, 0x412, 0xe847fffffffff));
    EXPECT_EQ(nextafter_translator(Extractor(0x0, 0x412, 0xe848000000000), Extractor(0x0, 0x0, 0x0)), Extractor(0x0, 0x412, 0xe847fffffffff));
    EXPECT_EQ(nextafter_translator(Extractor(0x0, 0x412, 0xe847e00000000), Extractor(0x1, 0x3ff, 0x0)), Extractor(0x0, 0x412, 0xe847dffffffff));
    EXPECT_EQ(nextafter_translator(Extractor(0x0, 0x412, 0xe848000000000), Extractor(0x0, 0x0, 0x1)), Extractor(0x0, 0x412, 0xe847fffffffff));
    EXPECT_EQ(nextafter_translator(Extractor(0x0, 0x7fe, 0xfffffffffffff), Extractor(0x0, 0x7fe, 0xfffffffffffff)), Extractor(0x0, 0x7fe, 0xfffffffffffff));
    EXPECT_EQ(nextafter_translator(Extractor(0x0, 0x1, 0x0), Extractor(0x0, 0x1, 0x0)), Extractor(0x0, 0x1, 0x0));
    EXPECT_EQ(nextafter_translator(Extractor(0x0, 0x3ff, 0x0), Extractor(0x0, 0x3ff, 0x0)), Extractor(0x0, 0x3ff, 0x0));
    EXPECT_EQ(nextafter_translator(Extractor(0x1, 0x0, 0x0), Extractor(0x1, 0x0, 0x0)), Extractor(0x1, 0x0, 0x0));
    EXPECT_EQ(nextafter_translator(Extractor(0x0, 0x0, 0x0), Extractor(0x0, 0x0, 0x0)), Extractor(0x0, 0x0, 0x0));
    EXPECT_EQ(nextafter_translator(Extractor(0x1, 0x3ff, 0x0), Extractor(0x1, 0x3ff, 0x0)), Extractor(0x1, 0x3ff, 0x0));
    EXPECT_EQ(nextafter_translator(Extractor(0x0, 0x0, 0x1), Extractor(0x0, 0x0, 0x1)), Extractor(0x0, 0x0, 0x1));
    EXPECT_EQ(nextafter_translator(Extractor(0x1, 0x7fe, 0xfffffffffffff), Extractor(0x0, 0x7fe, 0xfffffffffffff)), Extractor(0x1, 0x7fe, 0xffffffffffffe));
    EXPECT_EQ(nextafter_translator(Extractor(0x1, 0x1, 0x0), Extractor(0x0, 0x1, 0x0)), Extractor(0x1, 0x0, 0xfffffffffffff));
    EXPECT_EQ(nextafter_translator(Extractor(0x1, 0x3ff, 0x0), Extractor(0x0, 0x3ff, 0x0)), Extractor(0x1, 0x3fe, 0xfffffffffffff));
    EXPECT_EQ(nextafter_translator(Extractor(0x0, 0x0, 0x0), Extractor(0x1, 0x0, 0x0)), Extractor(0x1, 0x0, 0x0));
    EXPECT_EQ(nextafter_translator(Extractor(0x1, 0x0, 0x0), Extractor(0x0, 0x0, 0x0)), Extractor(0x0, 0x0, 0x0));
    EXPECT_EQ(nextafter_translator(Extractor(0x0, 0x3ff, 0x0), Extractor(0x1, 0x3ff, 0x0)), Extractor(0x0, 0x3fe, 0xfffffffffffff));
    EXPECT_EQ(nextafter_translator(Extractor(0x1, 0x0, 0x1), Extractor(0x0, 0x0, 0x1)), Extractor(0x1, 0x0, 0x0));
    EXPECT_EQ(nextafter_translator(Extractor(0x0, 0x7fe, 0xfffffffffffff), Extractor(0x1, 0x7fe, 0xfffffffffffff)), Extractor(0x0, 0x7fe, 0xffffffffffffe));
    EXPECT_EQ(nextafter_translator(Extractor(0x0, 0x1, 0x0), Extractor(0x1, 0x1, 0x0)), Extractor(0x0, 0x0, 0xfffffffffffff));
    EXPECT_EQ(nextafter_translator(Extractor(0x0, 0x3ff, 0x0), Extractor(0x1, 0x3ff, 0x0)), Extractor(0x0, 0x3fe, 0xfffffffffffff));
    EXPECT_EQ(nextafter_translator(Extractor(0x1, 0x0, 0x0), Extractor(0x0, 0x0, 0x0)), Extractor(0x0, 0x0, 0x0));
    EXPECT_EQ(nextafter_translator(Extractor(0x0, 0x0, 0x0), Extractor(0x1, 0x0, 0x0)), Extractor(0x1, 0x0, 0x0));
    EXPECT_EQ(nextafter_translator(Extractor(0x1, 0x3ff, 0x0), Extractor(0x0, 0x3ff, 0x0)), Extractor(0x1, 0x3fe, 0xfffffffffffff));
    EXPECT_EQ(nextafter_translator(Extractor(0x0, 0x0, 0x1), Extractor(0x1, 0x0, 0x1)), Extractor(0x0, 0x0, 0x0));
    EXPECT_EQ(nextafter_translator(Extractor(0x0, 0x7fe, 0xfffffffffffff), Extractor(0x0, 0x7fe, 0xfffffffffffff)), Extractor(0x0, 0x7fe, 0xfffffffffffff));
    EXPECT_EQ(nextafter_translator(Extractor(0x0, 0x1, 0x0), Extractor(0x1, 0x419, 0x7d78400000000)), Extractor(0x0, 0x0, 0xfffffffffffff));
    EXPECT_EQ(nextafter_translator(Extractor(0x0, 0x3ff, 0x0), Extractor(0x1, 0x419, 0x7d783fc000000)), Extractor(0x0, 0x3fe, 0xfffffffffffff));
    EXPECT_EQ(nextafter_translator(Extractor(0x1, 0x0, 0x0), Extractor(0x1, 0x419, 0x7d78400000000)), Extractor(0x1, 0x0, 0x1));
    EXPECT_EQ(nextafter_translator(Extractor(0x0, 0x0, 0x0), Extractor(0x1, 0x419, 0x7d78400000000)), Extractor(0x1, 0x0, 0x1));
    EXPECT_EQ(nextafter_translator(Extractor(0x1, 0x3ff, 0x0), Extractor(0x1, 0x419, 0x7d78404000000)), Extractor(0x1, 0x3ff, 0x1));
    EXPECT_EQ(nextafter_translator(Extractor(0x0, 0x0, 0x1), Extractor(0x1, 0x419, 0x7d78400000000)), Extractor(0x0, 0x0, 0x0));
    EXPECT_EQ(nextafter_translator(Extractor(0x0, 0x7fe, 0xfffffffffffff), Extractor(0x0, 0x7fe, 0xfffffffffffff)), Extractor(0x0, 0x7fe, 0xfffffffffffff));
    EXPECT_EQ(nextafter_translator(Extractor(0x1, 0x419, 0x7d78400000000), Extractor(0x0, 0x1, 0x0)), Extractor(0x1, 0x419, 0x7d783ffffffff));
    EXPECT_EQ(nextafter_translator(Extractor(0x1, 0x419, 0x7d783fc000000), Extractor(0x0, 0x3ff, 0x0)), Extractor(0x1, 0x419, 0x7d783fbffffff));
    EXPECT_EQ(nextafter_translator(Extractor(0x1, 0x419, 0x7d78400000000), Extractor(0x1, 0x0, 0x0)), Extractor(0x1, 0x419, 0x7d783ffffffff));
    EXPECT_EQ(nextafter_translator(Extractor(0x1, 0x419, 0x7d78400000000), Extractor(0x0, 0x0, 0x0)), Extractor(0x1, 0x419, 0x7d783ffffffff));
    EXPECT_EQ(nextafter_translator(Extractor(0x1, 0x419, 0x7d78404000000), Extractor(0x1, 0x3ff, 0x0)), Extractor(0x1, 0x419, 0x7d78403ffffff));
    EXPECT_EQ(nextafter_translator(Extractor(0x1, 0x419, 0x7d78400000000), Extractor(0x0, 0x0, 0x1)), Extractor(0x1, 0x419, 0x7d783ffffffff));
}

TEST_CASE(scalbn)
{
    EXPECT(isnan(scalbn(NAN, 3)));
    EXPECT(!isfinite(scalbn(INFINITY, 5)));
    EXPECT_EQ(scalbn(0, 3), 0);
    EXPECT_EQ(scalbn(15.3, 0), 15.3);

    // TODO: implement denormal handling in fallback scalbn
    //     EXPECT_EQ(scalbn(0x0.0000000000008p-1022, 16), 0x0.0000000000008p-1006);
    static constexpr auto biggest_subnormal = DBL_MIN - DBL_TRUE_MIN;
    auto smallest_normal = scalbn(biggest_subnormal, 1);
    Extractor ex(smallest_normal);
    EXPECT(ex.exponent != 0);

    EXPECT_EQ(scalbn(2.0, 4), 32.0);
}

TEST_CASE(gamma)
{
    EXPECT(isinf(tgamma(+0.0)) && !signbit(tgamma(+0.0)));
    EXPECT(isinf(tgamma(-0.0)) && signbit(tgamma(-0.0)));
    EXPECT(isinf(tgamma(INFINITY)) && !signbit(tgamma(INFINITY)));
    EXPECT(isnan(tgamma(NAN)));
    EXPECT(isnan(tgamma(-INFINITY)));
    EXPECT(isnan(tgamma(-5)));

    // TODO: investigate Stirling approximation implementation of gamma function
    //EXPECT_APPROXIMATE(tgamma(0.5), sqrt(M_PI));
    EXPECT_EQ(tgammal(21.0l), 2'432'902'008'176'640'000.0l);
    EXPECT_EQ(tgamma(19.0), 6'402'373'705'728'000.0);
    EXPECT_EQ(tgammaf(11.0f), 3628800.0f);
    EXPECT_EQ(tgamma(4.0), 6);

    EXPECT_EQ(lgamma(1.0), 0.0);
    EXPECT_EQ(lgamma(2.0), 0.0);
    EXPECT(isinf(lgamma(0.0)));
    EXPECT(!signbit(lgamma(-0.0)));
    EXPECT(isnan(lgamma(NAN)));
    EXPECT(isinf(lgamma(INFINITY)));
    EXPECT(isinf(lgamma(-INFINITY)));
    EXPECT_EQ(signgam, 1);
    lgamma(-2.5);
    EXPECT_EQ(signgam, -1);
}

TEST_CASE(fmax_and_fmin)
{
    EXPECT(fmax(-INFINITY, 0) == 0);
    EXPECT(fmax(NAN, 12) == 12);
    EXPECT(fmax(5, NAN) == 5);
    EXPECT(isnan(fmax(NAN, NAN)));
    EXPECT(isinf(fmax(1'000'000, INFINITY)));

    EXPECT(isinf(fmin(-INFINITY, 0)));
    EXPECT(fmin(0, INFINITY) == 0);
    EXPECT(fmin(NAN, 5) == 5);
    EXPECT(fmin(0, NAN) == 0);
    EXPECT(isnan(fmin(NAN, NAN)));
}

TEST_CASE(acos)
{
    EXPECT_APPROXIMATE(acos(-1), M_PI);
    EXPECT_APPROXIMATE(acos(0), 0.5 * M_PI);
    EXPECT_APPROXIMATE(acos(1), 0);
    EXPECT(isnan(acos(1.1)));
}

TEST_CASE(floor)
{
    EXPECT_EQ(floor(0.125), 0);
    EXPECT_EQ(floor(-0.125), -1.0);
    EXPECT_EQ(floor(0.5), 0);
    EXPECT_EQ(floor(-0.5), -1.0);
    EXPECT_EQ(floor(0.25), 0);
    EXPECT_EQ(floor(-0.25), -1.0);
    EXPECT_EQ(floor(-3.0 / 2.0), -2.0);
}

TEST_CASE(ceil)
{
    EXPECT_EQ(ceil(0.125), 1.0);
    EXPECT_EQ(ceil(-0.125), 0);
    EXPECT_EQ(ceil(0.5), 1.0);
    EXPECT_EQ(ceil(-0.5), 0);
    EXPECT_EQ(ceil(0.25), 1.0);
    EXPECT_EQ(ceil(-0.25), 0);
    EXPECT_EQ(ceil(-3.0 / 2.0), -1.0);
}
