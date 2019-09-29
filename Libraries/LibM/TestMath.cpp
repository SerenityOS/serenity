#include <AK/TestSuite.h>
 
#include <math.h>

#define EXPECT_CLOSE(a, b) { EXPECT(fabs(a - b) < 0.000001); }

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
        { 1.500000, 4.481626, 2.129246, 2.352379, 0.905148},
        { 20.990000, 1304956710.432035, 652478355.216017, 652478355.216017, 1.000000},
        { 20.010000, 490041186.687082, 245020593.343541, 245020593.343541, 1.000000},
        { 0.000000, 1.000000, 0.000000, 1.000000, 0.000000},
        { 0.010000, 1.010050, 0.010000, 1.000050, 0.010000},
        { -0.010000, 0.990050, -0.010000, 1.000050, -0.010000},
        { -1.000000, 0.367879, -1.175201, 1.543081, -0.761594},
        { -17.000000, 0.000000, -12077476.376788, 12077476.376788, -1.000000},
    };
    for (auto& v : values) {
        EXPECT_CLOSE(exp(v.x), v.exp);
        EXPECT_CLOSE(sinh(v.x), v.sinh);
        EXPECT_CLOSE(cosh(v.x), v.cosh);
        EXPECT_CLOSE(tanh(v.x), v.tanh);
    }
    EXPECT_EQ(exp(1000), std::numeric_limits<double>::infinity());
}

TEST_MAIN(Math)
