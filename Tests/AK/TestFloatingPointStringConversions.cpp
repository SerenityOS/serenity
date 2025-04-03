/*
 * Copyright (c) 2025, Tomás Simões <tomasprsimoes@tecnico.ulisboa.pt>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FloatingPointStringConversions.h>
#include <AK/UFixedBigInt.h>
#include <LibTest/TestCase.h>

using namespace AK;

enum class ParserType {
    Regular,
    Hexfloat,
    Complete
};

struct FloatingPointTestCase {
    StringView input;
    double expected_value;
    FloatingPointError expected_error;
    int expected_end_offset;
    ParserType parser_type;
};

static void floating_point_parsing_helper(FloatingPointTestCase const& test_case)
{
    auto const* input = test_case.input.characters_without_null_termination();
    size_t const input_length = test_case.input.length();

    switch (test_case.parser_type) {
    case ParserType::Regular: {
        auto result = parse_first_floating_point<double>(input, input + input_length);
        EXPECT_EQ(result.error, test_case.expected_error);
        // If the error is NoOrInvalidInput, parse_first_floating_point might return NaN/null
        if (test_case.expected_error != FloatingPointError::NoOrInvalidInput) {
            EXPECT_EQ(result.value, test_case.expected_value);
            EXPECT_EQ(result.end_ptr, input + test_case.expected_end_offset);
        }
        break;
    }
    case ParserType::Hexfloat: {
        auto result = parse_first_hexfloat_until_zero_character<double>(input);
        EXPECT_EQ(result.error, test_case.expected_error);
        EXPECT_EQ(result.value, test_case.expected_value);
        EXPECT_EQ(result.end_ptr, input + test_case.expected_end_offset);
        break;
    }
    case ParserType::Complete: {
        auto result = parse_floating_point_completely<double>(input, input + input_length);
        if (test_case.expected_error == FloatingPointError::None) {
            EXPECT(result.has_value());
            EXPECT_EQ(result.value(), test_case.expected_value);
        } else {
            EXPECT(!result.has_value());
        }
        break;
    }
    }
}

TEST_CASE(basic_integer)
{
    floating_point_parsing_helper({ "123"sv, 123.0, FloatingPointError::None, 3, ParserType::Regular });
}

TEST_CASE(decimal_number)
{
    floating_point_parsing_helper({ "123.45"sv, 123.45, FloatingPointError::None, 6, ParserType::Regular });
}

TEST_CASE(exponent_notation)
{
    floating_point_parsing_helper({ "1.5e3"sv, 1500.0, FloatingPointError::None, 5, ParserType::Regular });
}

TEST_CASE(negative_number)
{
    floating_point_parsing_helper({ "-67.89"sv, -67.89, FloatingPointError::None, 6, ParserType::Regular });
}

TEST_CASE(zero)
{
    floating_point_parsing_helper({ "0"sv, 0.0, FloatingPointError::None, 1, ParserType::Regular });
}

TEST_CASE(negative_zero)
{
    floating_point_parsing_helper({ "-0.0"sv, -0.0, FloatingPointError::None, 4, ParserType::Regular });
}

TEST_CASE(invalid_input)
{
    floating_point_parsing_helper({ "abc"sv, 0.0, FloatingPointError::NoOrInvalidInput, 0, ParserType::Regular });
}

TEST_CASE(partial_parse)
{
    floating_point_parsing_helper({ "123.45abc"sv, 123.45, FloatingPointError::None, 6, ParserType::Regular });
}

TEST_CASE(hex_float)
{
    floating_point_parsing_helper({ "0x1.8p1"sv, 3.0, FloatingPointError::None, 7, ParserType::Hexfloat });
}

TEST_CASE(out_of_range)
{
    floating_point_parsing_helper({ "1e309"sv, __builtin_huge_val(), FloatingPointError::OutOfRange, 5, ParserType::Regular });
}

TEST_CASE(rounded_down_to_zero)
{
    floating_point_parsing_helper({ "1e-400"sv, 0.0, FloatingPointError::RoundedDownToZero, 6, ParserType::Regular });
}

TEST_CASE(parse_completely_valid)
{
    floating_point_parsing_helper({ "123.45"sv, 123.45, FloatingPointError::None, 0, ParserType::Complete });
}

TEST_CASE(parse_completely_invalid)
{
    floating_point_parsing_helper({ "123.45a"sv, 0.0, FloatingPointError::NoOrInvalidInput, 0, ParserType::Complete });
}
