#include <AK/TestSuite.h>

#include <AK/AKString.h>
#include <AK/HashMap.h>
#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/StringBuilder.h>

TEST_CASE(load_form)
{
    FILE* fp = fopen("../../Base/home/anon/test.frm", "r");
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

TEST_MAIN(JSON)
