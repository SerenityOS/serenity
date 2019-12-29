#include <AK/TestSuite.h>

#include <AK/String.h>
#include <AK/HashMap.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
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
    EXPECT_EQ(json.as_string().length(), 2);
}

TEST_MAIN(JSON)
