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
Preprocessor::Preprocessor(const String& filename, const StringView& program)
    : m_filename(filename)
    , m_program(program)
{
    GenericLexer program_lexer { m_program };
    for (;;) {
        if (program_lexer.is_eof())
            break;
        auto line = program_lexer.consume_until('\n');
        bool has_multiline = false;
        while (line.ends_with('\\') && !program_lexer.is_eof()) {
            auto continuation = program_lexer.consume_until('\n');
            line = StringView { line.characters_without_null_termination(), line.length() + continuation.length() + 1 };
            // Append an empty line to keep the line count correct.
            m_lines.append({});
            has_multiline = true;
        }

        if (has_multiline)
            m_lines.last() = line;
        else
            m_lines.append(line);
    }
}

Vector<Token> Preprocessor::process_and_lex()
{
    for (; m_line_index < m_lines.size(); ++m_line_index) {
        auto& line = m_lines[m_line_index];

        bool include_in_processed_text = false;
        if (line.starts_with("#")) {
            auto keyword = handle_preprocessor_line(line);
            if (m_options.keep_include_statements && keyword == "include")
                include_in_processed_text = true;
        } else if (m_state == State::Normal) {
            include_in_processed_text = true;
        }

        if (include_in_processed_text) {
            process_line(line);
        }
    }

    return m_tokens;
}

static void consume_whitespace(GenericLexer& lexer)
{
    auto ignore_line = [&] {
        for (;;) {
            if (lexer.consume_specific("\\\n"sv)) {
                lexer.ignore(2);
            } else {
                lexer.ignore_until('\n');
                break;
            }
        }
    };
    for (;;) {
        if (lexer.consume_specific("//"sv))
            ignore_line();
        else if (lexer.consume_specific("/*"sv))
            lexer.ignore_until("*/");
        else if (lexer.next_is("\\\n"sv))
            lexer.ignore(2);
        else if (lexer.is_eof() || !lexer.next_is(isspace))
            break;
        else
            lexer.ignore();
    }
}

Preprocessor::PreprocessorKeyword Preprocessor::handle_preprocessor_line(const StringView& line)
{
    GenericLexer lexer(line);

    consume_whitespace(lexer);
    lexer.consume_specific('#');
    consume_whitespace(lexer);
    auto keyword = lexer.consume_until(' ');
    if (keyword.is_empty() || keyword.is_null() || keyword.is_whitespace())
        return {};

    handle_preprocessor_keyword(keyword, lexer);
    return keyword;
}

void Preprocessor::handle_preprocessor_keyword(const StringView& keyword, GenericLexer& line_lexer)
{
    if (keyword == "include") {
        consume_whitespace(line_lexer);
        auto include_path = line_lexer.consume_all();
        m_included_paths.append(include_path);
        if (definitions_in_header_callback) {
            for (auto& def : definitions_in_header_callback(include_path))
                m_definitions.set(def.key, def.value);
        }
        return;
    }

    if (keyword == "else") {
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

    if (!m_options.ignore_unsupported_keywords) {
        dbgln("Unsupported preprocessor keyword: {}", keyword);
        VERIFY_NOT_REACHED();
    }
}

void Preprocessor::process_line(StringView const& line)
{
    Lexer line_lexer { line, m_line_index };
    line_lexer.set_ignore_whitespace(true);
    auto tokens = line_lexer.lex();

    for (size_t i = 0; i < tokens.size(); ++i) {
        auto& token = tokens[i];
        if (token.type() == Token::Type::Identifier) {
            if (auto defined_value = m_definitions.find(token.text()); defined_value != m_definitions.end()) {
                auto last_substituted_token_index = do_substitution(tokens, i, defined_value->value);
                i = last_substituted_token_index;
                continue;
            }
        }
        m_tokens.append(token);
    }
}

size_t Preprocessor::do_substitution(Vector<Token> const& tokens, size_t token_index, Definition const& defined_value)
{
    if (defined_value.value.is_null())
        return token_index;

    Substitution sub;
    sub.defined_value = defined_value;

    auto macro_call = parse_macro_call(tokens, token_index);

    if (!macro_call.has_value())
        return token_index;

    // TODO: Evaluate macro call
    auto processed_value = defined_value.value;
    Vector<Token> original_tokens;
    for (size_t i = token_index; i <= macro_call->end_token_index; ++i) {
        original_tokens.append(tokens[i]);
    }
    VERIFY(!original_tokens.is_empty());

    m_substitutions.append({ original_tokens, defined_value, processed_value });

    Lexer lexer(processed_value);
    for (auto& token : lexer.lex()) {
        if (token.type() == Token::Type::Whitespace)
            continue;
        token.set_start(original_tokens.first().start());
        token.set_end(original_tokens.first().end());
        m_tokens.append(token);
    }
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
    MacroCall::Argument current_argument;

    size_t paren_depth = 1;
    for (; token_index < tokens.size(); ++token_index) {
        auto& token = tokens[token_index];
        if (token.type() == Token::Type::LeftParen)
            ++paren_depth;
        if (token.type() == Token::Type::RightParen)
            --paren_depth;

        if (paren_depth == 0) {
            arguments.append(move(current_argument));
            break;
        }

        if (paren_depth == 1 && token.type() == Token::Type::Comma) {
            arguments.append(move(current_argument));
            current_argument = {};
        } else {
            current_argument.tokens.append(token);
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
    definition.line = m_line_index;

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

    definition.value = line.substring_view(tokens[token_index].start().column);

    return definition;
}

};
