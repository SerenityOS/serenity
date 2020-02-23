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

#include <AK/JsonArray.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/LogStream.h>
#include <AK/StringBuilder.h>
#include <LibCore/File.h>
#include <stdio.h>

int main(int argc, char** argv)
{
    if (argc != 2) {
        printf("usage: %s <form-file>\n", argv[0]);
        return 0;
    }

    auto file = Core::File::construct(argv[1]);
    if (!file->open(Core::IODevice::ReadOnly)) {
        fprintf(stderr, "Error: Cannot open %s: %s\n", argv[1], file->error_string());
        return 1;
    }

    auto file_contents = file->read_all();
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
        StringBuilder builder;
        auto parts = class_name.split(':');
        builder.append(parts.last());
        dbg() << "#include <LibGUI/" << builder.to_string() << ".h>";
    });

    dbg() << "struct UI_" << name << " {";
    dbg() << "    RefPtr<GUI::Widget> main_widget;";

    widgets.as_array().for_each([&](auto& value) {
        ASSERT(value.is_object());
        const JsonObject& widget_object = value.as_object();
        auto name = widget_object.get("name").to_string();
        auto class_name = widget_object.get("class").to_string();
        dbg() << "    RefPtr<" << class_name << "> " << name << ";";
    });

    dbg() << "    UI_" << name << "();";

    dbg() << "};";

    dbg() << "UI_" << name << "::UI_" << name << "()";
    dbg() << "{";

    dbg() << "    main_widget = GUI::Widget::construct();";
    dbg() << "    main_widget->set_fill_with_background_color(true);";

    widgets.as_array().for_each([&](auto& value) {
        ASSERT(value.is_object());
        const JsonObject& widget_object = value.as_object();
        auto name = widget_object.get("name").to_string();
        auto class_name = widget_object.get("class").to_string();
        dbg() << "    " << name << " = main_widget->add<" << class_name << ">();";

        widget_object.for_each_member([&](auto& property_name, const JsonValue& property_value) {
            if (property_name == "class")
                return;

            String value;

            if (property_value.is_null())
                value = "{}";
            else
                value = property_value.serialized<StringBuilder>();

            dbg() << "    " << name << "->set_" << property_name << "(" << value << ");";
        });

        dbg() << "";
    });
    dbg() << "}";

    return 0;
}
