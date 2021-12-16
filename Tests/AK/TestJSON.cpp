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

    auto name = form_json.as_object().get("name").to_string();

    EXPECT_EQ(name, "Form1");

    auto widgets = form_json.as_object().get("widgets").as_array();

    widgets.for_each([&](const JsonValue& widget_value) {
        auto& widget_object = widget_value.as_object();
        auto widget_class = widget_object.get("class").as_string();
        widget_object.for_each_member([&]([[maybe_unused]] auto& property_name, [[maybe_unused]] const JsonValue& property_value) {
        });
    });
}

TEST_CASE(json_empty_string)
{
    auto json = JsonValue::from_string("\"\"").value();
    EXPECT_EQ(json.type(), JsonValue::Type::String);
    EXPECT_EQ(json.as_string().is_null(), false);
    EXPECT_EQ(json.as_string().is_empty(), true);
}

TEST_CASE(json_string)
{
    auto json = JsonValue::from_string("\"A\"").value();
    EXPECT_EQ(json.type(), JsonValue::Type::String);
    EXPECT_EQ(json.as_string().is_null(), false);
    EXPECT_EQ(json.as_string().length(), size_t { 1 });
    EXPECT_EQ(json.as_string() == "A", true);
}

TEST_CASE(json_utf8_character)
{
    auto json = JsonValue::from_string("\"\\u0041\"").value();
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
    auto value = JsonValue::from_string("");
    EXPECT_EQ(value.value().is_null(), true);
}
