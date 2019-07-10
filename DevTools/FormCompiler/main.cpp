#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/LogStream.h>
#include <LibCore/CFile.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (argc != 2) {
        printf("usage: %s <form-file>\n", argv[0]);
        return 0;
    }

    CFile file(argv[1]);
    if (!file.open(CIODevice::ReadOnly)) {
        fprintf(stderr, "Error: Cannot open %s: %s\n", argv[1], file.error_string());
        return 1;
    }

    auto file_contents = file.read_all();
    auto json = JsonValue::from_string(file_contents);

    if (!json.is_object()) {
        fprintf(stderr, "Malformed input\n");
        return 1;
    }

    auto name = json.as_object().get("name").to_string();
    auto widgets = json.as_object().get("widgets");

    if (!widgets.is_array()) {
        fprintf(stderr, "Malformed input\n");
        return 1;
    }

    dbg() << "#pragma once";

    widgets.as_array().for_each([&](auto& value) {
        const JsonObject& widget_object = value.as_object();
        auto class_name = widget_object.get("class").to_string();
        dbg() << "#include <LibGUI/" << class_name << ".h>";
    });

    dbg() << "struct UI_" << name << " {";
    dbg() << "    GWidget* main_widget;";

    widgets.as_array().for_each([&](auto& value) {
        ASSERT(value.is_object());
        const JsonObject& widget_object = value.as_object();
        auto name = widget_object.get("name").to_string();
        auto class_name = widget_object.get("class").to_string();
        dbg() << "    " << class_name << "* " << name << ";";
    });

    dbg() << "    UI_" << name << "();";

    dbg() << "};";

    dbg() << "UI_" << name << "::UI_" << name << "()";
    dbg() << "{";

    dbg() << "    main_widget = new GWidget(nullptr);";
    dbg() << "    main_widget->set_fill_with_background_color(true);";

    widgets.as_array().for_each([&](auto& value) {
        ASSERT(value.is_object());
        const JsonObject& widget_object = value.as_object();
        auto name = widget_object.get("name").to_string();
        auto class_name = widget_object.get("class").to_string();
        dbg() << "    " << name << " = new " << class_name << "(main_widget);";

        widget_object.for_each_member([&](auto& property_name, auto& property_value) {
            if (property_name == "class")
                return;

            String value;

            if (property_value.is_null())
                value = "{}";
            else
                value = property_value.serialized();

            dbg() << "    " << name << "->set_" << property_name << "(" << value << ");";
        });

        dbg() << "";
    });
    dbg() << "}";

    return 0;
}
