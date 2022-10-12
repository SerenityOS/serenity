/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/HashMap.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>

TEST_CASE(load_form)
{
    String raw_form_json = R"(
    {
        "name": "Form1",
        "widgets": [
            {
                "enabled": true,
                "forecolor": "#000000ff",
                "ruler_visible": false,
                "autofill": false,
                "x": 155,
                "tooltip": null,
                "height": 121,
                "width": 126,
                "y": 10,
                "class": "GTextEditor",
                "text": "Hi",
                "backcolor": "#c0c0c0ff",
                "visible":true
            }
        ]
    })";

    JsonValue form_json = JsonValue::from_string(raw_form_json).value();

    EXPECT(form_json.is_object());

    auto name = form_json.as_object().get("name"sv).to_string();

    EXPECT_EQ(name, "Form1");

    auto widgets = form_json.as_object().get("widgets"sv).as_array();

    widgets.for_each([&](JsonValue const& widget_value) {
        auto& widget_object = widget_value.as_object();
        auto widget_class = widget_object.get("class"sv).as_string();
        widget_object.for_each_member([&]([[maybe_unused]] auto& property_name, [[maybe_unused]] const JsonValue& property_value) {
        });
    });
}

TEST_CASE(json_empty_string)
{
    auto json = JsonValue::from_string("\"\""sv).value();
    EXPECT_EQ(json.type(), JsonValue::Type::String);
    EXPECT_EQ(json.as_string().is_null(), false);
    EXPECT_EQ(json.as_string().is_empty(), true);
}

TEST_CASE(json_string)
{
    auto json = JsonValue::from_string("\"A\""sv).value();
    EXPECT_EQ(json.type(), JsonValue::Type::String);
    EXPECT_EQ(json.as_string().is_null(), false);
    EXPECT_EQ(json.as_string().length(), size_t { 1 });
    EXPECT_EQ(json.as_string() == "A", true);
}

TEST_CASE(json_utf8_character)
{
    auto json = JsonValue::from_string("\"\\u0041\""sv).value();
    EXPECT_EQ(json.type(), JsonValue::Type::String);
    EXPECT_EQ(json.as_string().is_null(), false);
    EXPECT_EQ(json.as_string().length(), size_t { 1 });
    EXPECT_EQ(json.as_string() == "A", true);
}

/*
FIXME: Parse JSON from a Utf8View

TEST_CASE(json_utf8_multibyte)
{
    auto json_or_error = JsonValue::from_string("\"š\"");
    EXPECT_EQ(json_or_error.is_error(), false);

    auto& json = json_or_error.value();
    EXPECT_EQ(json.type(), JsonValue::Type::String);
    EXPECT_EQ(json.as_string().is_null(), false);
    EXPECT_EQ(json.as_string().length(), size_t { 2 });
    EXPECT_EQ(json.as_string() == "š", true);
    EXPECT_EQ(json.as_string() == "\xc5\xa1", true);
}
*/

TEST_CASE(json_64_bit_value)
{
    auto big_value = 0x12345678aabbccddull;
    JsonValue big_json_value(big_value);
    JsonValue big_json_value_copy = big_json_value;
    EXPECT_EQ(big_json_value.as_u64(), big_json_value_copy.as_u64());
}

TEST_CASE(json_duplicate_keys)
{
    JsonObject json;
    json.set("test", "foo");
    json.set("test", "bar");
    json.set("test", "baz");
    EXPECT_EQ(json.to_string(), "{\"test\":\"baz\"}");
}

TEST_CASE(json_u64_roundtrip)
{
    auto big_value = 0xffffffffffffffffull;
    auto json = JsonValue(big_value).to_string();
    auto value = JsonValue::from_string(json);
    EXPECT_EQ_FORCE(value.is_error(), false);
    EXPECT_EQ(value.value().as_u64(), big_value);
}

TEST_CASE(json_parse_empty_string)
{
    auto value = JsonValue::from_string(""sv);
    EXPECT_EQ(value.is_error(), true);
}

TEST_CASE(json_parse_long_decimals)
{
    auto value = JsonValue::from_string("1644452550.6489999294281"sv);
    EXPECT_EQ(value.value().as_double(), 1644452550.6489999294281);
}

TEST_CASE(json_parse_number_with_exponent)
{
    auto value_without_fraction = JsonValue::from_string("10e5"sv);
    EXPECT_EQ(value_without_fraction.value().as_double(), 1000000.0);

    auto value_with_fraction = JsonValue::from_string("10.5e5"sv);
    EXPECT_EQ(value_with_fraction.value().as_double(), 1050000.0);
}

TEST_CASE(json_parse_special_numbers)
{
#define EXPECT_TO_MATCH_NUMBER_BIT_WISE(string_input, double_input)                                                           \
    do {                                                                                                                      \
        auto value_or_error = JsonValue::from_string(string_input##sv);                                                       \
        VERIFY(!value_or_error.is_error());                                                                                   \
        if (value_or_error.is_error())                                                                                        \
            dbgln("got {}", value_or_error.error());                                                                          \
        EXPECT(value_or_error.value().is_number());                                                                           \
        EXPECT_EQ(bit_cast<u64>(value_or_error.value().to_double(4321.0)), bit_cast<u64>(static_cast<double>(double_input))); \
    } while (false)

    EXPECT_TO_MATCH_NUMBER_BIT_WISE("-0", -0.);
    EXPECT_TO_MATCH_NUMBER_BIT_WISE("-0.0", -0.0);
    EXPECT_TO_MATCH_NUMBER_BIT_WISE("-0.00", -0.00);
    EXPECT_TO_MATCH_NUMBER_BIT_WISE("-0e0", -0e0);
    EXPECT_TO_MATCH_NUMBER_BIT_WISE("-0e1", -0e1);
    EXPECT_TO_MATCH_NUMBER_BIT_WISE("-0e2", -0e2);
    EXPECT_TO_MATCH_NUMBER_BIT_WISE("-0e1000", -0e1000);
    EXPECT_TO_MATCH_NUMBER_BIT_WISE("-0e-1000", -0e-1000);

    EXPECT_TO_MATCH_NUMBER_BIT_WISE("0", 0.);
    EXPECT_TO_MATCH_NUMBER_BIT_WISE("0.0", 0.0);
    EXPECT_TO_MATCH_NUMBER_BIT_WISE("0.00", 0.00);
    EXPECT_TO_MATCH_NUMBER_BIT_WISE("0e0", 0e0);
    EXPECT_TO_MATCH_NUMBER_BIT_WISE("0e1", 0e1);
    EXPECT_TO_MATCH_NUMBER_BIT_WISE("0e2", 0e2);
    EXPECT_TO_MATCH_NUMBER_BIT_WISE("0e1000", 0e1000);
    EXPECT_TO_MATCH_NUMBER_BIT_WISE("0e-1000", 0e-1000);

    // These technically can be non zero, but not in doubles
    EXPECT_TO_MATCH_NUMBER_BIT_WISE("-1e-2000", -0.);
    EXPECT_TO_MATCH_NUMBER_BIT_WISE("1e-2000", 0.);

#undef EXPECT_TO_MATCH_NUMBER_BIT_WISE
}

TEST_CASE(json_parse_fails_on_invalid_number)
{
#define EXPECT_JSON_PARSE_TO_FAIL(value) \
    EXPECT(JsonValue::from_string(value##sv).is_error());

    EXPECT_JSON_PARSE_TO_FAIL("-");
    EXPECT_JSON_PARSE_TO_FAIL("00");
    EXPECT_JSON_PARSE_TO_FAIL("01");
    EXPECT_JSON_PARSE_TO_FAIL("-01");
    EXPECT_JSON_PARSE_TO_FAIL(".1");
    EXPECT_JSON_PARSE_TO_FAIL("-.1");
    EXPECT_JSON_PARSE_TO_FAIL("-,1");
    EXPECT_JSON_PARSE_TO_FAIL(".1e1");
    EXPECT_JSON_PARSE_TO_FAIL(".1e-1");
    EXPECT_JSON_PARSE_TO_FAIL("-.1e1");
    EXPECT_JSON_PARSE_TO_FAIL("-.1e-1");
    EXPECT_JSON_PARSE_TO_FAIL("1.e1");
    EXPECT_JSON_PARSE_TO_FAIL("1.e-1");
    EXPECT_JSON_PARSE_TO_FAIL("-1.e1");
    EXPECT_JSON_PARSE_TO_FAIL("-1.e-1");
    EXPECT_JSON_PARSE_TO_FAIL("1e");
    EXPECT_JSON_PARSE_TO_FAIL("1e+");
    EXPECT_JSON_PARSE_TO_FAIL("1e-");
    EXPECT_JSON_PARSE_TO_FAIL("1e-f");
    EXPECT_JSON_PARSE_TO_FAIL("1.e");
    EXPECT_JSON_PARSE_TO_FAIL("1.e+");
    EXPECT_JSON_PARSE_TO_FAIL("1.e-");
    EXPECT_JSON_PARSE_TO_FAIL("1.e-f");
    EXPECT_JSON_PARSE_TO_FAIL("1p2");
    EXPECT_JSON_PARSE_TO_FAIL("1.p2");
    EXPECT_JSON_PARSE_TO_FAIL("0x1.0p2");
    EXPECT_JSON_PARSE_TO_FAIL("0x1");
    EXPECT_JSON_PARSE_TO_FAIL("0x7");
    EXPECT_JSON_PARSE_TO_FAIL("0xA");
    EXPECT_JSON_PARSE_TO_FAIL("0x");
    EXPECT_JSON_PARSE_TO_FAIL("-0x");
    EXPECT_JSON_PARSE_TO_FAIL("0x");
    EXPECT_JSON_PARSE_TO_FAIL("1x");
    EXPECT_JSON_PARSE_TO_FAIL("100x");
    EXPECT_JSON_PARSE_TO_FAIL("1000000000000000000000x");
    EXPECT_JSON_PARSE_TO_FAIL("0e2x");
    EXPECT_JSON_PARSE_TO_FAIL("0.1e2x");
    EXPECT_JSON_PARSE_TO_FAIL("0.1x");
    EXPECT_JSON_PARSE_TO_FAIL("1e2x");
    EXPECT_JSON_PARSE_TO_FAIL("1.2x");
    EXPECT_JSON_PARSE_TO_FAIL("1.2e2x");
    EXPECT_JSON_PARSE_TO_FAIL(".0");
    EXPECT_JSON_PARSE_TO_FAIL(".e1");
    EXPECT_JSON_PARSE_TO_FAIL("-.0");
    EXPECT_JSON_PARSE_TO_FAIL("-.e1");
    EXPECT_JSON_PARSE_TO_FAIL("+0");
    EXPECT_JSON_PARSE_TO_FAIL("+0.0");
    EXPECT_JSON_PARSE_TO_FAIL("+0.00");
    EXPECT_JSON_PARSE_TO_FAIL("+0e0");
    EXPECT_JSON_PARSE_TO_FAIL("+0e1");
    EXPECT_JSON_PARSE_TO_FAIL("+0e2");
    EXPECT_JSON_PARSE_TO_FAIL("+0e1000");
    EXPECT_JSON_PARSE_TO_FAIL("+0e-1000");

    EXPECT_JSON_PARSE_TO_FAIL("+10");
    EXPECT_JSON_PARSE_TO_FAIL("+10e1");
    EXPECT_JSON_PARSE_TO_FAIL("+10.3");
    EXPECT_JSON_PARSE_TO_FAIL("+10.3e1");

    EXPECT_JSON_PARSE_TO_FAIL("0x1");
    EXPECT_JSON_PARSE_TO_FAIL("0x2");
    EXPECT_JSON_PARSE_TO_FAIL("0xA");
    EXPECT_JSON_PARSE_TO_FAIL("0xB");
    EXPECT_JSON_PARSE_TO_FAIL("0xF");
    EXPECT_JSON_PARSE_TO_FAIL("0Xf");
    EXPECT_JSON_PARSE_TO_FAIL("0X3");

    EXPECT_JSON_PARSE_TO_FAIL("10ee1");
    EXPECT_JSON_PARSE_TO_FAIL("1e1e1");

    // These could be valid within an array but not as the top level value
    EXPECT_JSON_PARSE_TO_FAIL("0,0");
    EXPECT_JSON_PARSE_TO_FAIL(",1");
    EXPECT_JSON_PARSE_TO_FAIL("10e1,");
    EXPECT_JSON_PARSE_TO_FAIL("10e,1");
    EXPECT_JSON_PARSE_TO_FAIL("10,e1");
    EXPECT_JSON_PARSE_TO_FAIL("1,0e1");
    EXPECT_JSON_PARSE_TO_FAIL(",10e1");

#undef EXPECT_JSON_PARSE_TO_FAIL
}
