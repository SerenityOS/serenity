/*
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Shell/AST.h>
#include <Shell/PosixLexer.h>

namespace Shell::Posix {

class Parser {
public:
    Parser(StringView input, bool interactive = false, Optional<Reduction> starting_reduction = {})
        : m_lexer(input)
        , m_in_interactive_mode(interactive)
        , m_eof_token(Token::eof())
    {
        fill_token_buffer(starting_reduction);
    }

    RefPtr<AST::Node> parse();
    RefPtr<AST::Node> parse_word_list();

    struct Error {
        DeprecatedString message;
        Optional<AST::Position> position;
    };
    auto& errors() const { return m_errors; }

private:
    Optional<Token> next_expanded_token(Optional<Reduction> starting_reduction = {});
    Vector<Token> perform_expansions(Vector<Token> tokens);
    void fill_token_buffer(Optional<Reduction> starting_reduction = {});
    void handle_heredoc_contents();

    Token const& peek()
    {
        if (eof())
            return m_eof_token;
        handle_heredoc_contents();
        return m_token_buffer[m_token_index];
    }
    Token const& consume()
    {
        if (eof())
            return m_eof_token;
        handle_heredoc_contents();
        return m_token_buffer[m_token_index++];
    }
    void skip()
    {
        if (eof())
            return;
        m_token_index++;
    }
    bool eof() const
    {
        return m_token_index == m_token_buffer.size() || m_token_buffer[m_token_index].type == Token::Type::Eof;
    }

    struct CaseItemsResult {
        Vector<AST::Position> pipe_positions;
        NonnullRefPtrVector<AST::Node> nodes;
    };

    RefPtr<AST::Node> parse_complete_command();
    RefPtr<AST::Node> parse_list();
    RefPtr<AST::Node> parse_and_or();
    RefPtr<AST::Node> parse_pipeline();
    RefPtr<AST::Node> parse_pipe_sequence();
    RefPtr<AST::Node> parse_command();
    RefPtr<AST::Node> parse_compound_command();
    RefPtr<AST::Node> parse_subshell();
    RefPtr<AST::Node> parse_compound_list();
    RefPtr<AST::Node> parse_term();
    RefPtr<AST::Node> parse_for_clause();
    RefPtr<AST::Node> parse_case_clause();
    CaseItemsResult parse_case_list();
    RefPtr<AST::Node> parse_if_clause();
    RefPtr<AST::Node> parse_while_clause();
    RefPtr<AST::Node> parse_until_clause();
    RefPtr<AST::Node> parse_function_definition();
    RefPtr<AST::Node> parse_function_body();
    RefPtr<AST::Node> parse_brace_group();
    RefPtr<AST::Node> parse_do_group();
    RefPtr<AST::Node> parse_simple_command();
    RefPtr<AST::Node> parse_prefix();
    RefPtr<AST::Node> parse_suffix();
    RefPtr<AST::Node> parse_io_redirect();
    RefPtr<AST::Node> parse_redirect_list();
    RefPtr<AST::Node> parse_io_file(AST::Position, Optional<int> fd);
    RefPtr<AST::Node> parse_io_here(AST::Position, Optional<int> fd);
    RefPtr<AST::Node> parse_word();

    template<typename... Ts>
    void error(Token const& token, CheckedFormatString<Ts...> fmt, Ts&&... args)
    {
        m_errors.append(Error {
            DeprecatedString::formatted(fmt.view(), forward<Ts>(args)...),
            token.position,
        });
    }

    Lexer m_lexer;
    bool m_in_interactive_mode { false };
    Vector<Token, 2> m_token_buffer;
    size_t m_token_index { 0 };
    Vector<Token> m_previous_token_buffer;

    Vector<Error> m_errors;
    HashMap<DeprecatedString, NonnullRefPtr<AST::Heredoc>> m_unprocessed_heredoc_entries;

    Token m_eof_token;

    bool m_disallow_command_prefix { true };
};

}
