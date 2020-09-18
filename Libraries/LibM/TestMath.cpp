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

#include <AK/TestSuite.h>

#include <math.h>

#define EXPECT_CLOSE(a, b)              \
    {                                   \
        EXPECT(fabs(a - b) < 0.000001); \
    }

TEST_CASE(trig)
{
    EXPECT_CLOSE(sin(1234), 0.653316);
    EXPECT_CLOSE(cos(1234), -0.830914);
    EXPECT_CLOSE(tan(1234), -0.786262);
    EXPECT_CLOSE(sqrt(1234), 35.128336)
    EXPECT_CLOSE(sin(-1), -0.867955);
    EXPECT_CLOSE(cos(-1), 0.594715);
    EXPECT_CLOSE(tan(-1), -1.459446);
    EXPECT(isnan(sqrt(-1)));
    EXPECT(isnan(asin(1.1)));
    EXPECT(isnan(asin(-1.1)));
    EXPECT_CLOSE(asin(0), 0.0);
    EXPECT_CLOSE(asin(0.01), 0.01);
    EXPECT_CLOSE(asin(0.1), 0.100167);
    EXPECT_CLOSE(asin(0.3), 0.304693);
    EXPECT_CLOSE(asin(0.499), 0.522444);
    EXPECT_CLOSE(asin(0.5), 0.523599);
    EXPECT_CLOSE(asin(0.501), 0.524754);
    EXPECT_CLOSE(asin(0.9), 1.119770);
    EXPECT_CLOSE(asin(0.99), 1.429246);
    EXPECT_CLOSE(asin(1.0), 1.570750);
    EXPECT_CLOSE(atan(0), 0.0)
    EXPECT_CLOSE(atan(0.5), 0.463648)
    EXPECT_CLOSE(atan(-0.5), -0.463648)
    EXPECT_CLOSE(atan(5.5), 1.390943)
    EXPECT_CLOSE(atan(-5.5), -1.390943)
    EXPECT_CLOSE(atan(555.5), 1.568996)
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
        EXPECT_CLOSE(exp(v.x), v.exp);
        EXPECT_CLOSE(sinh(v.x), v.sinh);
        EXPECT_CLOSE(cosh(v.x), v.cosh);
        EXPECT_CLOSE(tanh(v.x), v.tanh);
    }
    EXPECT_EQ(exp(1000), std::numeric_limits<double>::infinity());
}

TEST_CASE(logarithms)
{
    EXPECT(isnan(log(-1)));
    EXPECT(log(0) < -1000000);
    EXPECT_CLOSE(log(0.5), -0.693233)
    EXPECT_CLOSE(log(1.1), 0.095310)
    EXPECT_CLOSE(log(5), 1.609480)
    EXPECT_CLOSE(log(5.5), 1.704842)
    EXPECT_CLOSE(log(500), 6.214104)
    EXPECT_CLOSE(log2(5), 2.321989)
    EXPECT_CLOSE(log10(5), 0.698988)
}

TEST_MAIN(Math)
