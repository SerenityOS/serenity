/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Preprocessor.h"
#include <AK/Assertions.h>
#include <AK/GenericLexer.h>
#include <AK/StringBuilder.h>
#include <LibCpp/Lexer.h>
#include <ctype.h>

namespace Cpp {
Preprocessor::Preprocessor(ByteString const& filename, StringView program)
    : m_filename(filename)
    , m_program(program)
{
}

Vector<Token> Preprocessor::process_and_lex()
{
    Lexer lexer { m_program };
    lexer.set_ignore_whitespace(true);
    auto tokens = lexer.lex();

    m_unprocessed_tokens = tokens;

    for (size_t token_index = 0; token_index < tokens.size(); ++token_index) {
        auto& token = tokens[token_index];
        m_current_line = token.start().line;
        if (token.type() == Token::Type::PreprocessorStatement) {
            handle_preprocessor_statement(token.text());
            m_processed_tokens.append(tokens[token_index]);
            continue;
        }

        if (m_state != State::Normal)
            continue;

        if (token.type() == Token::Type::IncludeStatement) {
            if (token_index >= tokens.size() - 1 || tokens[token_index + 1].type() != Token::Type::IncludePath)
                continue;
            handle_include_statement(tokens[token_index + 1].text());
            if (m_options.keep_include_statements) {
                m_processed_tokens.append(tokens[token_index]);
                m_processed_tokens.append(tokens[token_index + 1]);
            }
            ++token_index; // Also skip IncludePath token
            continue;
        }

        if (token.type() == Token::Type::Identifier) {
            if (auto defined_value = m_definitions.find(token.text()); defined_value != m_definitions.end()) {
                auto last_substituted_token_index = do_substitution(tokens, token_index, defined_value->value);
                token_index = last_substituted_token_index;
                continue;
            }
        }

        m_processed_tokens.append(token);
    }

    return m_processed_tokens;
}

static void consume_whitespace(GenericLexer& lexer)
{
    auto ignore_line = [&] {
        for (;;) {
            if (lexer.consume_specific("\\\n"sv)) {
                lexer.ignore(2);
            } else {
                lexer.ignore_until('\n');
                lexer.ignore();
                break;
            }
        }
    };
    for (;;) {
        if (lexer.consume_specific("//"sv)) {
            ignore_line();
        } else if (lexer.consume_specific("/*"sv)) {
            lexer.ignore_until("*/");
            lexer.ignore(2);
        } else if (lexer.next_is("\\\n"sv)) {
            lexer.ignore(2);
        } else if (lexer.is_eof() || !lexer.next_is(isspace)) {
            break;
        } else {
            lexer.ignore();
        }
    }
}

void Preprocessor::handle_preprocessor_statement(StringView line)
{
    GenericLexer lexer(line);

    consume_whitespace(lexer);
    lexer.consume_specific('#');
    consume_whitespace(lexer);
    auto keyword = lexer.consume_until(' ');
    lexer.ignore();
    if (keyword.is_empty() || keyword.is_whitespace())
        return;

    handle_preprocessor_keyword(keyword, lexer);
}

void Preprocessor::handle_include_statement(StringView include_path)
{
    m_included_paths.append(include_path);
    if (definitions_in_header_callback) {
        for (auto& def : definitions_in_header_callback(include_path))
            m_definitions.set(def.key, def.value);
    }
}

void Preprocessor::handle_preprocessor_keyword(StringView keyword, GenericLexer& line_lexer)
{
    if (keyword == "include") {
        // Should have called 'handle_include_statement'.
        VERIFY_NOT_REACHED();
    }

    if (keyword == "else") {
        if (m_options.ignore_invalid_statements && m_current_depth == 0)
            return;
        VERIFY(m_current_depth > 0);
        if (m_depths_of_not_taken_branches.contains_slow(m_current_depth - 1)) {
            m_depths_of_not_taken_branches.remove_all_matching([this](auto x) { return x == m_current_depth - 1; });
            m_state = State::Normal;
        }
        if (m_depths_of_taken_branches.contains_slow(m_current_depth - 1)) {
            m_state = State::SkipElseBranch;
        }
        return;
    }

    if (keyword == "endif") {
        if (m_options.ignore_invalid_statements && m_current_depth == 0)
            return;
        VERIFY(m_current_depth > 0);
        --m_current_depth;
        if (m_depths_of_not_taken_branches.contains_slow(m_current_depth)) {
            m_depths_of_not_taken_branches.remove_all_matching([this](auto x) { return x == m_current_depth; });
        }
        if (m_depths_of_taken_branches.contains_slow(m_current_depth)) {
            m_depths_of_taken_branches.remove_all_matching([this](auto x) { return x == m_current_depth; });
        }
        m_state = State::Normal;
        return;
    }

    if (keyword == "define") {
        if (m_state == State::Normal) {
            auto definition = create_definition(line_lexer.consume_all());
            if (definition.has_value())
                m_definitions.set(definition->key, *definition);
        }
        return;
    }
    if (keyword == "undef") {
        if (m_state == State::Normal) {
            auto key = line_lexer.consume_until(' ');
            line_lexer.consume_all();
            m_definitions.remove(key);
        }
        return;
    }
    if (keyword == "ifdef") {
        ++m_current_depth;
        if (m_state == State::Normal) {
            auto key = line_lexer.consume_until(' ');
            line_lexer.ignore();
            if (m_definitions.contains(key)) {
                m_depths_of_taken_branches.append(m_current_depth - 1);
                return;
            } else {
                m_depths_of_not_taken_branches.append(m_current_depth - 1);
                m_state = State::SkipIfBranch;
                return;
            }
        }
        return;
    }
    if (keyword == "ifndef") {
        ++m_current_depth;
        if (m_state == State::Normal) {
            auto key = line_lexer.consume_until(' ');
            line_lexer.ignore();
            if (!m_definitions.contains(key)) {
                m_depths_of_taken_branches.append(m_current_depth - 1);
                return;
            } else {
                m_depths_of_not_taken_branches.append(m_current_depth - 1);
                m_state = State::SkipIfBranch;
                return;
            }
        }
        return;
    }
    if (keyword == "if") {
        ++m_current_depth;
        if (m_state == State::Normal) {
            // FIXME: Implement #if logic
            // We currently always take #if branches.
            m_depths_of_taken_branches.append(m_current_depth - 1);
        }
        return;
    }

    if (keyword == "elif") {
        if (m_options.ignore_invalid_statements && m_current_depth == 0)
            return;
        VERIFY(m_current_depth > 0);
        // FIXME: Evaluate the elif expression
        // We currently always treat the expression in #elif as true.
        if (m_depths_of_not_taken_branches.contains_slow(m_current_depth - 1) /* && should_take*/) {
            m_depths_of_not_taken_branches.remove_all_matching([this](auto x) { return x == m_current_depth - 1; });
            m_state = State::Normal;
        }
        if (m_depths_of_taken_branches.contains_slow(m_current_depth - 1)) {
            m_state = State::SkipElseBranch;
        }
        return;
    }
    if (keyword == "pragma") {
        line_lexer.consume_all();
        return;
    }
    if (keyword == "error") {
        line_lexer.consume_all();
        return;
    }

    if (!m_options.ignore_unsupported_keywords) {
        dbgln("Unsupported preprocessor keyword: {}", keyword);
        VERIFY_NOT_REACHED();
    }
}

size_t Preprocessor::do_substitution(Vector<Token> const& tokens, size_t token_index, Definition const& defined_value)
{
    if (defined_value.value.is_empty())
        return token_index;

    Substitution sub;
    sub.defined_value = defined_value;

    auto macro_call = parse_macro_call(tokens, token_index);

    if (!macro_call.has_value())
        return token_index;

    Vector<Token> original_tokens;
    for (size_t i = token_index; i <= macro_call->end_token_index; ++i) {
        original_tokens.append(tokens[i]);
    }
    VERIFY(!original_tokens.is_empty());

    auto processed_value = evaluate_macro_call(*macro_call, defined_value);
    m_substitutions.append({ original_tokens, defined_value, processed_value });

    Lexer lexer(processed_value);
    lexer.lex_iterable([&](auto token) {
        if (token.type() == Token::Type::Whitespace)
            return;
        token.set_start(original_tokens.first().start());
        token.set_end(original_tokens.first().end());
        m_processed_tokens.append(token);
    });
    return macro_call->end_token_index;
}

Optional<Preprocessor::MacroCall> Preprocessor::parse_macro_call(Vector<Token> const& tokens, size_t token_index)
{
    auto name = tokens[token_index];
    ++token_index;

    if (token_index >= tokens.size() || tokens[token_index].type() != Token::Type::LeftParen)
        return MacroCall { name, {}, token_index - 1 };
    ++token_index;

    Vector<MacroCall::Argument> arguments;
    Optional<MacroCall::Argument> current_argument;

    size_t paren_depth = 1;
    for (; token_index < tokens.size(); ++token_index) {
        auto& token = tokens[token_index];
        if (token.type() == Token::Type::LeftParen)
            ++paren_depth;
        if (token.type() == Token::Type::RightParen)
            --paren_depth;

        if (paren_depth == 0) {
            if (current_argument.has_value())
                arguments.append(*current_argument);
            break;
        }

        if (paren_depth == 1 && token.type() == Token::Type::Comma) {
            if (current_argument.has_value())
                arguments.append(*current_argument);
            current_argument = {};
        } else {
            if (!current_argument.has_value())
                current_argument = MacroCall::Argument {};
            current_argument->tokens.append(token);
        }
    }

    if (token_index >= tokens.size())
        return {};

    return MacroCall { name, move(arguments), token_index };
}

Optional<Preprocessor::Definition> Preprocessor::create_definition(StringView line)
{
    Lexer lexer { line };
    lexer.set_ignore_whitespace(true);
    auto tokens = lexer.lex();
    if (tokens.is_empty())
        return {};

    if (tokens.first().type() != Token::Type::Identifier)
        return {};

    Definition definition;
    definition.filename = m_filename;
    definition.line = m_current_line;

    definition.key = tokens.first().text();

    if (tokens.size() == 1)
        return definition;

    size_t token_index = 1;
    // Parse macro parameters (if any)
    if (tokens[token_index].type() == Token::Type::LeftParen) {
        ++token_index;
        while (token_index < tokens.size() && tokens[token_index].type() != Token::Type::RightParen) {
            auto param = tokens[token_index];
            if (param.type() != Token::Type::Identifier)
                return {};

            if (token_index + 1 >= tokens.size())
                return {};

            ++token_index;

            if (tokens[token_index].type() == Token::Type::Comma)
                ++token_index;
            else if (tokens[token_index].type() != Token::Type::RightParen)
                return {};

            definition.parameters.empend(param.text());
        }
        if (token_index >= tokens.size())
            return {};
        ++token_index;
    }

    if (token_index < tokens.size())
        definition.value = remove_escaped_newlines(line.substring_view(tokens[token_index].start().column));

    return definition;
}

ByteString Preprocessor::remove_escaped_newlines(StringView value)
{
    static constexpr auto escaped_newline = "\\\n"sv;
    AK::StringBuilder processed_value;
    GenericLexer lexer { value };
    while (!lexer.is_eof()) {
        processed_value.append(lexer.consume_until(escaped_newline));
        lexer.ignore(escaped_newline.length());
    }
    return processed_value.to_byte_string();
}

ByteString Preprocessor::evaluate_macro_call(MacroCall const& macro_call, Definition const& definition)
{
    if (macro_call.arguments.size() != definition.parameters.size()) {
        dbgln("mismatch in # of arguments for macro call: {}", macro_call.name.text());
        return {};
    }

    Lexer lexer { definition.value };
    StringBuilder processed_value;
    lexer.lex_iterable([&](auto token) {
        if (token.type() != Token::Type::Identifier) {
            processed_value.append(token.text());
            return;
        }

        auto param_index = definition.parameters.find_first_index(token.text());
        if (!param_index.has_value()) {
            processed_value.append(token.text());
            return;
        }

        auto& argument = macro_call.arguments[*param_index];
        for (auto& arg_token : argument.tokens) {
            processed_value.append(arg_token.text());
        }
    });

    return processed_value.to_byte_string();
}

};
