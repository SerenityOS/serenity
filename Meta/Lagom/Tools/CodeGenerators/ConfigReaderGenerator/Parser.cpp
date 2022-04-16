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

    auto is_ascii_space_but_not_newline = [](auto c) {
        return is_ascii_space(c) && c != '\n';
    };

    // Parameters
    AST::Annotation annotation { type };
    while (true) {
        consume_while(is_ascii_space_but_not_newline);
        String parameter_name = consume_while([](auto c) { return is_ascii_alphanumeric(c) || c == '_'; });
        if (parameter_name.is_empty())
            break;
        if (parameter_name == "allowed_values") {
            if (!consume_specific('('))
                return ParserError { .message = "Expected '('" };
            Vector<String> allowed_values;
            while (true) {
                String value = consume_while([](auto c) { return is_ascii_alphanumeric(c) || c == '_'; });
                allowed_values.append(move(value));
                if (!consume_specific(',')) {
                    consume_while(is_ascii_space_but_not_newline);
                    break;
                }
                consume_while(is_ascii_space_but_not_newline);
            }
            if (!consume_specific(')'))
                return ParserError { .message = "Expected ')'" };
            annotation.set_allowed_values(allowed_values);
        } else {
            return ParserError { .message = String::formatted("Invalid annotation parameter: {}", parameter_name) };
        }
    }

    // Ignore rest of line
    consume_line();

    // Make comments after annotations invalidate the annotation.
    if (consume_comments())
        return Optional<AST::Annotation> {};
    return annotation;
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
