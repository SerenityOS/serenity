/*
 * Copyright (c) 2022, Maciej <sppmacd@pm.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Parser.h"
#include "Utils.h"

ParserErrorOr<AST::ConfigFile> Parser::parse()
{
    if (!is_valid_cpp_identifier(m_domain_name))
        return ParserError { .message = String::formatted("Domain name {} is not a valid C++ identifier", m_domain_name) };

    AST::ConfigFile config_file(m_domain_name);

    while (!is_eof()) {
        consume_while(is_ascii_space);
        auto next = peek();

        if (next == '#') {
            // TODO: Group annotations
            consume_line();
        } else if (next == '[') {
            consume(); // [
            auto group_name = consume_until(']');
            if (group_name.contains('\n'))
                return ParserError { .message = "Group name cannot span multiple lines" };
            consume(); // ]
            consume_line();
            auto group = TRY(parse_group(group_name));
            config_file.add_group(group);
        }
    }
    return config_file;
}

ParserErrorOr<AST::Group> Parser::parse_group(String const& group_name)
{
    AST::Group group(group_name);
    while (true) {
        consume_while(is_ascii_space);
        auto maybe_comment_with_annotation = TRY(parse_comment_with_annotation());
        consume_while(is_ascii_space);
        if (peek() == '[' || is_eof())
            break;

        AST::Annotation annotation = maybe_comment_with_annotation.has_value() ? maybe_comment_with_annotation.value() : AST::Annotation(AST::Annotation::Type::String);

        auto option_name = consume_until('=');
        if (option_name.contains('\n'))
            return ParserError { .message = "Option name cannot span multiple lines" };
        consume(); // '='
        auto option_value = consume_line();

        AST::Option option(move(annotation), move(option_name), move(option_value));
        group.add_option(move(option));
    }
    return group;
}

ParserErrorOr<Optional<AST::Annotation>> Parser::parse_comment_with_annotation()
{
    consume_while(is_ascii_space);
    if (peek() != '#')
        return Optional<AST::Annotation> {};

    // Try to consume normal comments first
    while (true) {
        consume_while(is_ascii_space);
        if (!consume_specific('#'))
            return Optional<AST::Annotation> {};
        consume_while(is_ascii_space);
        if (consume_specific('@'))
            break;
        consume_line();
    }

    String type_name = consume_while(is_ascii_alphanumeric);
    AST::Annotation::Type type = TRY([&]() -> ParserErrorOr<AST::Annotation::Type> {
        if (type_name == "string")
            return AST::Annotation::Type::String;
        if (type_name == "bool")
            return AST::Annotation::Type::Bool;
        if (type_name == "i32")
            return AST::Annotation::Type::I32;
        return ParserError { .message = String::formatted("Invalid option type: {}", type_name) };
    }());

    // Everything after type name is ignored.
    // TODO: Support annotation parameters (e.g. range, enum values,...)
    consume_line();

    // Make comments after annotations invalidate the annotation.
    if (consume_comments())
        return Optional<AST::Annotation> {};
    return AST::Annotation(type);
}

bool Parser::consume_comments()
{
    bool comment_encountered = false;
    while (true) {
        consume_while(is_ascii_space);
        if (!consume_specific('#'))
            return comment_encountered;
        comment_encountered = true;
        consume_line();
    }
}
