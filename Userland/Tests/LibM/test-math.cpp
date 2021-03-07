/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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

#include <AK/TestSuite.h>

#include <float.h>
#include <math.h>

TEST_CASE(trig)
{
    EXPECT_APPROXIMATE(sin(1234), 0.601927);
    EXPECT_APPROXIMATE(cos(1234), -0.798550);
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
    EXPECT_APPROXIMATE(asin(0.99), 1.429245);
    EXPECT_APPROXIMATE(asin(1.0), 1.570750);
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
        { 1.500000, 4.481626, 2.129246, 2.352379, 0.905148 },
        { 20.990000, 1304956710.432035, 652478355.216017, 652478355.216017, 1.000000 },
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
    EXPECT_APPROXIMATE(log(0.5), -0.693233);
    EXPECT_APPROXIMATE(log(1.1), 0.095310);
    EXPECT_APPROXIMATE(log(5), 1.609480);
    EXPECT_APPROXIMATE(log(5.5), 1.704842);
    EXPECT_APPROXIMATE(log(500), 6.214104);
    EXPECT_APPROXIMATE(log2(5), 2.321989);
    EXPECT_APPROXIMATE(log10(5), 0.698988);
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
    void format(FormatBuilder& builder, const Extractor& value)
    {
        builder.put_literal("{");
        builder.put_u64(value.sign);
        builder.put_literal(", ");
        builder.put_u64(value.exponent, 16, true);
        builder.put_literal(", ");
        builder.put_u64(value.mantissa, 16, true);
        builder.put_literal("}");
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

    EXPECT_EQ(scalbn(0x0.0000000000008p-1022, 16), 0x0.0000000000008p-1006);
    static constexpr auto biggest_subnormal = DBL_MIN - DBL_TRUE_MIN;
    auto smallest_normal = scalbn(biggest_subnormal, 1);
    Extractor ex(smallest_normal);
    EXPECT(ex.exponent != 0);

    EXPECT_EQ(scalbn(2.0, 4), 32.0);
}

TEST_MAIN(Math)
