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

#include <AK/HashMap.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/String.h>
#include <AK/StringBuilder.h>

TEST_CASE(load_form)
{
    FILE* fp = fopen("../../Base/home/anon/little/test.frm", "r");
    ASSERT(fp);

    StringBuilder builder;
    for (;;) {
        char buffer[1024];
        if (!fgets(buffer, sizeof(buffer), fp))
            break;
        builder.append(buffer);
    }

    fclose(fp);

    JsonValue form_json = JsonValue::from_string(builder.to_string());

    EXPECT(form_json.is_object());

    auto name = form_json.as_object().get("name").to_string();

    EXPECT_EQ(name, "Form1");

    auto widgets = form_json.as_object().get("widgets").as_array();

    widgets.for_each([&](const JsonValue& widget_value) {
        auto& widget_object = widget_value.as_object();
        auto widget_class = widget_object.get("class").as_string();
        widget_object.for_each_member([&](auto& property_name, const JsonValue& property_value) {
            (void)property_name;
            (void)property_value;
            //dbgprintf("Set property %s.%s to '%s'\n", widget_class.characters(), property_name.characters(), property_value.serialized().characters());
        });
    });
}

BENCHMARK_CASE(load_4chan_catalog)
{
    FILE* fp = fopen("4chan_catalog.json", "r");
    ASSERT(fp);

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
        JsonValue form_json = JsonValue::from_string(json_string);
        EXPECT(form_json.is_array());
    }
}

TEST_CASE(json_empty_string)
{
    auto json = JsonValue::from_string("\"\"");
    EXPECT_EQ(json.type(), JsonValue::Type::String);
    EXPECT_EQ(json.as_string().is_null(), false);
    EXPECT_EQ(json.as_string().is_empty(), true);
}

TEST_CASE(json_utf8_character)
{
    auto json = JsonValue::from_string("\"\xc3\x84\"");
    EXPECT_EQ(json.type(), JsonValue::Type::String);
    EXPECT_EQ(json.as_string().is_null(), false);
    EXPECT_EQ(json.as_string().length(), size_t { 2 });
}

TEST_MAIN(JSON)
