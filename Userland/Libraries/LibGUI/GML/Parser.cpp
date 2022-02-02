/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Parser.h"
#include "Lexer.h"
#include <AK/GenericLexer.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/Queue.h>

namespace GUI::GML {

static Optional<JsonValue> parse_core_object(Queue<Token>& tokens)
{
    JsonObject object;
    JsonArray children;

    auto peek = [&] {
        if (tokens.is_empty())
            return Token::Type::Unknown;
        return tokens.head().m_type;
    };

    while (peek() == Token::Type::Comment)
        tokens.dequeue();

    if (peek() != Token::Type::ClassMarker) {
        dbgln("Expected class marker");
        return {};
    }

    tokens.dequeue();

    if (peek() != Token::Type::ClassName) {
        dbgln("Expected class name");
        return {};
    }

    auto class_name = tokens.dequeue();
    object.set("class", JsonValue(class_name.m_view));

    if (peek() != Token::Type::LeftCurly) {
        // Empty object
        return object;
    }
    tokens.dequeue();

    for (;;) {
        if (peek() == Token::Type::RightCurly) {
            // End of object
            break;
        }

        if (peek() == Token::Type::ClassMarker) {
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
        } else if (peek() == Token::Type::Identifier) {
            // It's a property.
            auto property_name = tokens.dequeue();

            if (property_name.m_view.is_empty()) {
                dbgln("Expected non-empty property name");
                return {};
            }

            if (peek() != Token::Type::Colon) {
                dbgln("Expected ':'");
                return {};
            }
            tokens.dequeue();

            JsonValue value;
            if (peek() == Token::Type::ClassMarker) {
                auto parsed_value = parse_core_object(tokens);
                if (!parsed_value.has_value())
                    return {};
                if (!parsed_value.value().is_object()) {
                    dbgln("Expected property to be Core::Object");
                    return {};
                }
                value = parsed_value.release_value();
            } else if (peek() == Token::Type::JsonValue) {
                auto value_string = tokens.dequeue();
                auto parsed_value = JsonValue::from_string(value_string.m_view);
                if (parsed_value.is_error()) {
                    dbgln("Expected property to be JSON value");
                    return {};
                }
                value = parsed_value.release_value();
            }
            object.set(property_name.m_view, move(value));
        } else if (peek() == Token::Type::Comment) {
            tokens.dequeue();
        } else {
            dbgln("Expected child, property, comment, or }}");
            return {};
        }
    }

    if (peek() != Token::Type::RightCurly) {
        dbgln("Expected }}");
        return {};
    }
    tokens.dequeue();

    if (!children.is_empty())
        object.set("children", move(children));

    return object;
}

JsonValue parse_gml(StringView string)
{
    auto lexer = Lexer(string);

    Queue<Token> tokens;
    for (auto& token : lexer.lex())
        tokens.enqueue(token);

    auto root = parse_core_object(tokens);

    if (!root.has_value())
        return JsonValue();

    return root.release_value();
}

}
