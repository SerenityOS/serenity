/*
 * Copyright (c) 2021, Linus Groh <mail@linusgroh.de>
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

#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/StringBuilder.h>
#include <LibGUI/GMLFormatter.h>
#include <LibGUI/GMLParser.h>

namespace GUI {

static String format_gml_object(const JsonObject& node, size_t indentation = 0, bool is_inline = false)
{
    StringBuilder builder;

    auto indent = [&builder](size_t indentation) {
        for (size_t i = 0; i < indentation; ++i)
            builder.append("    ");
    };

    struct Property {
        String key;
        JsonValue value;
    };
    Vector<Property> properties;
    node.for_each_member([&](auto& key, auto& value) {
        if (key != "class" && key != "layout" && key != "children")
            properties.append({ key, value });
        return IterationDecision::Continue;
    });

    if (!is_inline)
        indent(indentation);
    builder.append('@');
    builder.append(node.get("class").as_string());
    builder.append(" {\n");

    for (auto& property : properties) {
        indent(indentation + 1);
        builder.append(property.key);
        builder.append(": ");
        if (property.value.is_array()) {
            // custom array serialization as AK's doesn't pretty-print
            // objects and arrays (we only care about arrays (for now))
            builder.append("[");
            auto first = true;
            property.value.as_array().for_each([&](auto& value) {
                if (!first)
                    builder.append(", ");
                first = false;
                value.serialize(builder);
            });
            builder.append("]");
        } else {
            property.value.serialize(builder);
        }
        builder.append("\n");
    }

    if (node.has("layout")) {
        auto layout = node.get("layout").as_object();
        if (!properties.is_empty())
            builder.append("\n");
        indent(indentation + 1);
        builder.append("layout: ");
        builder.append(format_gml_object(move(layout), indentation + 1, true));
    }

    if (node.has("children")) {
        auto children = node.get("children").as_array();
        auto first = properties.is_empty() && !node.has("layout");
        children.for_each([&](auto& value) {
            if (!first)
                builder.append("\n");
            first = false;
            builder.append(format_gml_object(value.as_object(), indentation + 1));
        });
    }

    indent(indentation);
    builder.append("}\n");

    return builder.to_string();
}

String format_gml(const StringView& string)
{
    // FIXME: Preserve comments somehow, they're not contained
    // in the JSON object returned by parse_gml()
    auto ast = parse_gml(string);
    if (ast.is_null())
        return {};
    VERIFY(ast.is_object());
    return format_gml_object(ast.as_object());
}

}
