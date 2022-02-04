/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Parser.h"
#include "AST.h"
#include "Lexer.h"
#include <AK/Error.h>
#include <AK/GenericLexer.h>
#include <AK/JsonObject.h>
#include <AK/JsonValue.h>
#include <AK/Queue.h>
#include <AK/RefPtr.h>

namespace GUI::GML {

static ErrorOr<NonnullRefPtr<Object>> parse_gml_object(Queue<Token>& tokens)
{
    auto object = TRY(try_make_ref_counted<Object>());

    auto peek = [&] {
        if (tokens.is_empty())
            return Token::Type::Unknown;
        return tokens.head().m_type;
    };

    while (peek() == Token::Type::Comment) {
        dbgln("found comment {}", tokens.head().m_view);
        TRY(object->add_child(TRY(Node::from_token<Comment>(tokens.dequeue()))));
    }

    if (peek() != Token::Type::ClassMarker)
        return Error::from_string_literal("Expected class marker"sv);

    tokens.dequeue();

    if (peek() != Token::Type::ClassName)
        return Error::from_string_literal("Expected class name"sv);

    auto class_name = tokens.dequeue();
    object->set_name(class_name.m_view);

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
            TRY(object->add_child(TRY(parse_gml_object(tokens))));
        } else if (peek() == Token::Type::Identifier) {
            // It's a property.
            auto property_name = tokens.dequeue();

            if (property_name.m_view.is_empty())
                return Error::from_string_literal("Expected non-empty property name"sv);

            if (peek() != Token::Type::Colon)
                return Error::from_string_literal("Expected ':'"sv);

            tokens.dequeue();

            RefPtr<ValueNode> value;
            if (peek() == Token::Type::ClassMarker)
                value = TRY(parse_gml_object(tokens));
            else if (peek() == Token::Type::JsonValue)
                value = TRY(try_make_ref_counted<JsonValueNode>(TRY(JsonValueNode::from_string(tokens.dequeue().m_view))));

            auto property = TRY(try_make_ref_counted<KeyValuePair>(property_name.m_view, value.release_nonnull()));
            TRY(object->add_child(property));

        } else if (peek() == Token::Type::Comment) {
            TRY(object->add_child(TRY(Node::from_token<Comment>(tokens.dequeue()))));
        } else {
            return Error::from_string_literal("Expected child, property, comment, or }}"sv);
        }
    }

    if (peek() != Token::Type::RightCurly)
        return Error::from_string_literal("Expected }}"sv);

    tokens.dequeue();

    return object;
}

ErrorOr<NonnullRefPtr<GMLFile>> parse_gml(StringView string)
{
    auto lexer = Lexer(string);

    Queue<Token> tokens;
    for (auto& token : lexer.lex())
        tokens.enqueue(token);

    auto file = TRY(try_make_ref_counted<GMLFile>());

    auto peek = [&] {
        if (tokens.is_empty())
            return Token::Type::Unknown;
        return tokens.head().m_type;
    };

    while (peek() == Token::Type::Comment)
        TRY(file->add_child(TRY(Node::from_token<Comment>(tokens.dequeue()))));

    TRY(file->add_child(TRY(parse_gml_object(tokens))));

    while (!tokens.is_empty())
        TRY(file->add_child(TRY(Node::from_token<Comment>(tokens.dequeue()))));

    return file;
}

}
