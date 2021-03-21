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
#include <AK/Queue.h>
#include <LibGUI/GMLLexer.h>
#include <LibGUI/GMLParser.h>
#include <ctype.h>

namespace GUI {

static Optional<JsonValue> parse_core_object(Queue<GMLToken>& tokens)
{
    JsonObject object;
    JsonArray children;

    auto peek = [&] {
        if (tokens.is_empty())
            return GMLToken::Type::Unknown;
        return tokens.head().m_type;
    };

    while (peek() == GMLToken::Type::Comment)
        tokens.dequeue();

    if (peek() != GMLToken::Type::ClassMarker) {
        dbgln("Expected class marker");
        return {};
    }

    tokens.dequeue();

    if (peek() != GMLToken::Type::ClassName) {
        dbgln("Expected class name");
        return {};
    }

    auto class_name = tokens.dequeue();
    object.set("class", JsonValue(class_name.m_view));

    if (peek() != GMLToken::Type::LeftCurly) {
        // Empty object
        return object;
    }
    tokens.dequeue();

    for (;;) {
        if (peek() == GMLToken::Type::RightCurly) {
            // End of object
            break;
        }

        if (peek() == GMLToken::Type::ClassMarker) {
            // It's a child object.
            auto value = parse_core_object(tokens);
            if (!value.has_value()) {
                dbgln("Parsing child object failed");
                return {};
            }
            if (!value.value().is_object()) {
                dbgln("Expected child to be Core::Object");
                return {};
            }
            children.append(value.release_value());
        } else if (peek() == GMLToken::Type::Identifier) {
            // It's a property.
            auto property_name = tokens.dequeue();

            if (property_name.m_view.is_empty()) {
                dbgln("Expected non-empty property name");
                return {};
            }

            if (peek() != GMLToken::Type::Colon) {
                dbgln("Expected ':'");
                return {};
            }
            tokens.dequeue();

            JsonValue value;
            if (peek() == GMLToken::Type::ClassMarker) {
                auto parsed_value = parse_core_object(tokens);
                if (!parsed_value.has_value())
                    return {};
                if (!parsed_value.value().is_object()) {
                    dbgln("Expected property to be Core::Object");
                    return {};
                }
                value = parsed_value.release_value();
            } else if (peek() == GMLToken::Type::JsonValue) {
                auto value_string = tokens.dequeue();
                auto parsed_value = JsonValue::from_string(value_string.m_view);
                if (!parsed_value.has_value()) {
                    dbgln("Expected property to be JSON value");
                    return {};
                }
                value = parsed_value.release_value();
            }
            object.set(property_name.m_view, move(value));
        } else if (peek() == GMLToken::Type::Comment) {
            tokens.dequeue();
        } else {
            dbgln("Expected child, property, comment, or }}");
            return {};
        }
    }

    if (peek() != GMLToken::Type::RightCurly) {
        dbgln("Expected }}");
        return {};
    }
    tokens.dequeue();

    if (!children.is_empty())
        object.set("children", move(children));

    return object;
}

JsonValue parse_gml(const StringView& string)
{
    auto lexer = GMLLexer(string);

    Queue<GMLToken> tokens;
    for (auto& token : lexer.lex())
        tokens.enqueue(token);

    auto root = parse_core_object(tokens);

    if (!root.has_value())
        return JsonValue();

    return root.release_value();
}

}
