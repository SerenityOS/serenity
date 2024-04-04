/*
 * Copyright (c) 2021, Itamar S. <itamar8910@gmail.com>
 * Copyright (c) 2023, Volodymyr V. <vvmposeydon@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/FlyString.h>
#include <AK/Function.h>
#include <AK/HashMap.h>
#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibGLSL/Token.h>

namespace GLSL {

class Preprocessor {

public:
    explicit Preprocessor(String filename, String program);
    ErrorOr<Vector<Token>> process_and_lex();
    Vector<StringView> included_paths() const { return m_included_paths; }

    struct Definition {
        StringView key;
        Vector<StringView> parameters;
        String value;
        FlyString filename;
        size_t line { 0 };
        size_t column { 0 };
    };
    using Definitions = HashMap<StringView, Definition>;

    struct Substitution {
        Vector<Token> original_tokens;
        Definition defined_value;
        String processed_value;
    };

    Definitions const& definitions() const { return m_definitions; }
    Vector<Substitution> const& substitutions() const { return m_substitutions; }

    void set_ignore_unsupported_keywords(bool ignore) { m_options.ignore_unsupported_keywords = ignore; }
    void set_ignore_invalid_statements(bool ignore) { m_options.ignore_invalid_statements = ignore; }
    void set_keep_include_statements(bool keep) { m_options.keep_include_statements = keep; }

    Function<Definitions(StringView)> definitions_in_header_callback { nullptr };

    Vector<Token> const& unprocessed_tokens() const { return m_unprocessed_tokens; }

private:
    ErrorOr<void> handle_preprocessor_statement(StringView);
    void handle_include_statement(StringView);
    ErrorOr<void> handle_preprocessor_keyword(StringView keyword, GenericLexer& line_lexer);
    ErrorOr<String> remove_escaped_newlines(StringView value);

    ErrorOr<size_t> do_substitution(Vector<Token> const& tokens, size_t token_index, Definition const&);
    ErrorOr<Optional<Definition>> create_definition(StringView line);

    struct MacroCall {
        Token name;
        struct Argument {
            Vector<Token> tokens;
        };
        Vector<Argument> arguments;
        size_t end_token_index { 0 };
    };
    Optional<MacroCall> parse_macro_call(Vector<Token> const& tokens, size_t token_index);
    ErrorOr<String> evaluate_macro_call(MacroCall const&, Definition const&);

    String m_filename;
    String m_program;

    Vector<Token> m_unprocessed_tokens;
    Vector<Token> m_processed_tokens;
    Definitions m_definitions;
    Vector<Substitution> m_substitutions;

    size_t m_current_line { 0 };
    size_t m_current_depth { 0 };
    Vector<size_t> m_depths_of_taken_branches;
    Vector<size_t> m_depths_of_not_taken_branches;

    enum class State {
        Normal,
        SkipIfBranch,
        SkipElseBranch
    };
    State m_state { State::Normal };

    Vector<StringView> m_included_paths;

    struct Options {
        bool ignore_unsupported_keywords { false };
        bool ignore_invalid_statements { false };
        bool keep_include_statements { false };
    } m_options;
};
}
