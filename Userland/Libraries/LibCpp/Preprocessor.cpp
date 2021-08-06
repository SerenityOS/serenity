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
    Vector<Token> all_tokens;
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
            for (auto& token : process_line(line)) {
                if (token.type() != Token::Type::Whitespace)
                    all_tokens.append(token);
            }
        }
    }

    return all_tokens;
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
            auto key = line_lexer.consume_until(' ');
            consume_whitespace(line_lexer);

            DefinedValue value;
            value.filename = m_filename;
            value.line = m_line_index;

            auto string_value = line_lexer.consume_all();
            if (!string_value.is_empty())
                value.value = string_value;

            m_definitions.set(key, value);
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

Vector<Token> Preprocessor::process_line(const StringView& line)
{
    Lexer line_lexer { line, m_line_index };
    auto tokens = line_lexer.lex();

    // TODO: Go over tokens of line, do token substitution

    return tokens;
}

};
