#include <AK/TestSuite.h>

#include <AK/FloatToDigits.h>
#include <math.h>

[[nodiscard]] static bool check_digits_are_equal(Vector<int> a, Vector<int> b)
{
    if (a.size() != b.size())
        return false;
    for (size_t i = 0; i < a.size(); ++i) {
        if (a[i] != b[i])
            return false;
    }
    return true;
}

TEST_CASE(digits_one)
{
    auto res = AK::double_to_digits(0x1p+0, 10, AK::FloatToDigitPrecisionMode::None, 0);
    EXPECT(res.is_positive);
    EXPECT_EQ(res.exponent, 0);
    EXPECT(check_digits_are_equal(res.digits, { 1 }));
}

TEST_CASE(digits_negative_one)
{
    auto res = AK::double_to_digits(-0x1p+0, 10, AK::FloatToDigitPrecisionMode::None, 0);
    EXPECT(!res.is_positive);
    EXPECT_EQ(res.exponent, 0);
    EXPECT(check_digits_are_equal(res.digits, { 1 }));
}

TEST_CASE(digits_subnormal)
{
    // 0x0.00000000009c3p-1022 = 1.2347e-320
    auto res = AK::double_to_digits(0x0.00000000009c3p-1022, 10, AK::FloatToDigitPrecisionMode::None, 0);
    EXPECT(res.is_positive);
    EXPECT_EQ(res.exponent, -324);
    EXPECT(check_digits_are_equal(res.digits, { 1, 2, 3, 4, 7 }));
}

TEST_CASE(digits_negative_subnormal)
{
    auto res = AK::double_to_digits(-0x0.00000000009c3p-1022, 10, AK::FloatToDigitPrecisionMode::None, 0);
    EXPECT(!res.is_positive);
    EXPECT_EQ(res.exponent, -324);
    EXPECT(check_digits_are_equal(res.digits, { 1, 2, 3, 4, 7 }));
}

TEST_CASE(digits_zero)
{
    auto res = AK::double_to_digits(0x0p+0, 10, AK::FloatToDigitPrecisionMode::None, 0);
    EXPECT(res.is_positive);
    EXPECT_EQ(res.exponent, 0);
    EXPECT(check_digits_are_equal(res.digits, { 0 }));
}

TEST_CASE(digits_negative_zero)
{
    auto res = AK::double_to_digits(-0x0p+0, 10, AK::FloatToDigitPrecisionMode::None, 0);
    EXPECT(!res.is_positive);
    EXPECT_EQ(res.exponent, 0);
    EXPECT(check_digits_are_equal(res.digits, { 0 }));
}

TEST_CASE(str_infinity)
{
    EXPECT_EQ("inf", AK::double_to_string(INFINITY, 10, false, AK::FloatToStringMode::Shortest, AK::FloatToDigitPrecisionMode::None));
    EXPECT_EQ("INF", AK::double_to_string(INFINITY, 10, true, AK::FloatToStringMode::Shortest, AK::FloatToDigitPrecisionMode::None));
    EXPECT_EQ("-inf", AK::double_to_string(-INFINITY, 10, false, AK::FloatToStringMode::Shortest, AK::FloatToDigitPrecisionMode::None));
    EXPECT_EQ("-INF", AK::double_to_string(-INFINITY, 10, true, AK::FloatToStringMode::Shortest, AK::FloatToDigitPrecisionMode::None));
}

TEST_CASE(str_nan)
{
    EXPECT_EQ("nan", AK::double_to_string(NAN, 10, false, AK::FloatToStringMode::Shortest, AK::FloatToDigitPrecisionMode::None));
    EXPECT_EQ("NAN", AK::double_to_string(NAN, 10, true, AK::FloatToStringMode::Shortest, AK::FloatToDigitPrecisionMode::None));
}

TEST_CASE(str_one)
{
    EXPECT_EQ("1", AK::double_to_string(0x1p+0, 10, false, AK::FloatToStringMode::Shortest, AK::FloatToDigitPrecisionMode::None));
    EXPECT_EQ("-1", AK::double_to_string(-0x1p+0, 10, false, AK::FloatToStringMode::Shortest, AK::FloatToDigitPrecisionMode::None));
}

TEST_CASE(str_sign_mode)
{
    EXPECT_EQ("123.45", AK::double_to_string(0x1.edccccccccccdp+6, 10, false, AK::FloatToStringMode::Shortest, AK::FloatToDigitPrecisionMode::None));
    EXPECT_EQ("-123.45", AK::double_to_string(-0x1.edccccccccccdp+6, 10, false, AK::FloatToStringMode::Shortest, AK::FloatToDigitPrecisionMode::None));
    EXPECT_EQ("+123.45", AK::double_to_string(0x1.edccccccccccdp+6, 10, false, AK::FloatToStringMode::Shortest, AK::FloatToDigitPrecisionMode::None, 23, AK::FormatBuilder::SignMode::Always));
    EXPECT_EQ(" 123.45", AK::double_to_string(0x1.edccccccccccdp+6, 10, false, AK::FloatToStringMode::Shortest, AK::FloatToDigitPrecisionMode::None, 23, AK::FormatBuilder::SignMode::Reserved));
}

TEST_CASE(str_test_float_to_str_mode)
{
    EXPECT_EQ("3.14e-5", AK::double_to_string(0x1.0766fc8e5b77fp-15, 10, false, AK::FloatToStringMode::Shortest, AK::FloatToDigitPrecisionMode::None));
    EXPECT_EQ("0.000314", AK::double_to_string(0x1.4940bbb1f255fp-12, 10, false, AK::FloatToStringMode::Shortest, AK::FloatToDigitPrecisionMode::None));
    EXPECT_EQ("3140000", AK::double_to_string(0x1.7f4dp+21, 10, false, AK::FloatToStringMode::Shortest, AK::FloatToDigitPrecisionMode::None));
    EXPECT_EQ("3.14e+8", AK::double_to_string(0x1.2b7428p+28, 10, false, AK::FloatToStringMode::Shortest, AK::FloatToDigitPrecisionMode::None));
    EXPECT_EQ("7e+0", AK::double_to_string(0x1.cp+2, 10, false, AK::FloatToStringMode::Exponential, AK::FloatToDigitPrecisionMode::None));
    EXPECT_EQ("700000000", AK::double_to_string(0x1.4dc938p+29, 10, false, AK::FloatToStringMode::Fixed, AK::FloatToDigitPrecisionMode::None));
}

TEST_CASE(str_base)
{
    EXPECT_EQ("1111011.011101001111000000001001000100011111011101111", AK::double_to_string(0x1.edd3c02447ddep+6, 2, false, AK::FloatToStringMode::Fixed, AK::FloatToDigitPrecisionMode::None));
    EXPECT_EQ("3f.gfzvuftmj", AK::double_to_string(0x1.edd3c02447ddep+6, 36, false, AK::FloatToStringMode::Fixed, AK::FloatToDigitPrecisionMode::None));
}

TEST_CASE(str_precision_relative)
{
    EXPECT_EQ("3000", AK::double_to_string(0x1.89ep+11, 10, false, AK::FloatToStringMode::Shortest, AK::FloatToDigitPrecisionMode::Relative, 1));
    EXPECT_EQ("3200", AK::double_to_string(0x1.89ep+11, 10, false, AK::FloatToStringMode::Shortest, AK::FloatToDigitPrecisionMode::Relative, 2));
    EXPECT_EQ("3150", AK::double_to_string(0x1.89ep+11, 10, false, AK::FloatToStringMode::Shortest, AK::FloatToDigitPrecisionMode::Relative, 3));
    EXPECT_EQ("3151", AK::double_to_string(0x1.89ep+11, 10, false, AK::FloatToStringMode::Shortest, AK::FloatToDigitPrecisionMode::Relative, 4));
    EXPECT_EQ("3151", AK::double_to_string(0x1.89ep+11, 10, false, AK::FloatToStringMode::Shortest, AK::FloatToDigitPrecisionMode::Relative, 100));
}

TEST_CASE(str_precision_absolute)
{
    EXPECT_EQ("3150", AK::double_to_string(0x1.89e4d4fdf3b64p+11, 10, false, AK::FloatToStringMode::Shortest, AK::FloatToDigitPrecisionMode::Absolute, -1));
    EXPECT_EQ("3151", AK::double_to_string(0x1.89e4d4fdf3b64p+11, 10, false, AK::FloatToStringMode::Shortest, AK::FloatToDigitPrecisionMode::Absolute, 0));
    EXPECT_EQ("3151.2", AK::double_to_string(0x1.89e4d4fdf3b64p+11, 10, false, AK::FloatToStringMode::Shortest, AK::FloatToDigitPrecisionMode::Absolute, 1));
    EXPECT_EQ("3151.15", AK::double_to_string(0x1.89e4d4fdf3b64p+11, 10, false, AK::FloatToStringMode::Shortest, AK::FloatToDigitPrecisionMode::Absolute, 2));
    EXPECT_EQ("3151.151", AK::double_to_string(0x1.89e4d4fdf3b64p+11, 10, false, AK::FloatToStringMode::Shortest, AK::FloatToDigitPrecisionMode::Absolute, 3));
    EXPECT_EQ("3151.151", AK::double_to_string(0x1.89e4d4fdf3b64p+11, 10, false, AK::FloatToStringMode::Shortest, AK::FloatToDigitPrecisionMode::Absolute, 100));
}

TEST_MAIN(FloatToDigits)