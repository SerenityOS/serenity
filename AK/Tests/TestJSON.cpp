/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibTest/TestCase.h>

#include <AK/HashMap.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>

TEST_CASE(load_form)
{
    FILE* fp = fopen("test.frm", "r");
    VERIFY(fp);

    StringBuilder builder;
    for (;;) {
        char buffer[1024];
        if (!fgets(buffer, sizeof(buffer), fp))
            break;
        builder.append(buffer);
    }

    fclose(fp);

    JsonValue form_json = JsonValue::from_string(builder.to_string()).value();

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

BENCHMARK_CASE(load_4chan_catalog)
{
    FILE* fp = fopen("4chan_catalog.json", "r");
    VERIFY(fp);

    StringBuilder builder;
    for (;;) {
        char buffer[1024];
        if (!fgets(buffer, sizeof(buffer), fp))
            break;
        builder.append(buffer);
    }

    fclose(fp);

    auto json_string = builder.to_string();

    for (int i = 0; i < 10; ++i) {
        JsonValue form_json = JsonValue::from_string(json_string).value();
        EXPECT(form_json.is_array());
    }
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

TEST_CASE(json_utf8_multibyte)
{
    auto json = JsonValue::from_string("\"š\"").value();
    EXPECT_EQ(json.type(), JsonValue::Type::String);
    EXPECT_EQ(json.as_string().is_null(), false);
    EXPECT_EQ(json.as_string().length(), size_t { 2 });
    EXPECT_EQ(json.as_string() == "š", true);
    EXPECT_EQ(json.as_string() == "\xc5\xa1", true);
}

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
