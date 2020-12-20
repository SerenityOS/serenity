/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/GenericLexer.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <LibGUI/GMLParser.h>
#include <ctype.h>

namespace GUI {

static bool is_valid_class_name_character(char ch)
{
    return isalpha(ch) || ch == ':';
}

static bool is_valid_property_name_character(char ch)
{
    return isalpha(ch) || ch == '_';
}

static void swallow_whitespace(GenericLexer& scanner)
{
    scanner.consume_while([](auto ch) { return isspace(ch); });
}

static JsonValue parse_core_object(GenericLexer& scanner)
{
    JsonObject object;
    JsonArray children;

    // '@Foo' means new Core::Object of class Foo
    if (!scanner.consume_specific('@'))
        ASSERT_NOT_REACHED();

    auto class_name = scanner.consume_while([](auto ch) { return is_valid_class_name_character(ch); });
    object.set("class", JsonValue(class_name));

    swallow_whitespace(scanner);

    if (!scanner.consume_specific('{'))
        ASSERT_NOT_REACHED();

    swallow_whitespace(scanner);

    for (;;) {
        swallow_whitespace(scanner);

        if (scanner.peek() == '}') {
            // End of object
            break;
        }

        if (scanner.peek() == '@') {
            // It's a child object.
            auto value = parse_core_object(scanner);
            ASSERT(value.is_object());
            children.append(move(value));
        } else {
            // It's a property.
            auto property_name = scanner.consume_while([](auto ch) { return is_valid_property_name_character(ch); });
            swallow_whitespace(scanner);

            ASSERT(!property_name.is_empty());

            if (!scanner.consume_specific(':'))
                ASSERT_NOT_REACHED();

            swallow_whitespace(scanner);

            JsonValue value;
            if (scanner.peek() == '@') {
                value = parse_core_object(scanner);
                ASSERT(value.is_object());
            } else {
                auto value_string = scanner.consume_line();
                value = JsonValue::from_string(value_string).release_value();
            }
            object.set(property_name, move(value));
        }
    }

    if (!scanner.consume_specific('}'))
        ASSERT_NOT_REACHED();

    if (!children.is_empty())
        object.set("children", move(children));

    return object;
}

JsonValue parse_gml(const StringView& string)
{
    GenericLexer scanner(string);
    auto root = parse_core_object(scanner);

    if (root.is_null())
        return JsonValue();

    return root;
}

}
