/*
 * Copyright (c) 2022, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibShell/AST.h>
#include <LibShell/PosixLexer.h>

namespace Shell::Posix {

class Parser {
public:
    Parser(StringView input, bool interactive = false, Optional<Reduction> starting_reduction = {})
        : m_lexer(input)
        , m_in_interactive_mode(interactive)
        , m_eof_token(Token::eof())
    {
        (void)fill_token_buffer(starting_reduction);
    }

    enum class AllowNewlines {
        No,
        Yes,
    };

    RefPtr<AST::Node> parse();
    RefPtr<AST::Node> parse_word_list(AllowNewlines = AllowNewlines::No);

    struct Error {
        ByteString message;
        Optional<AST::Position> position;
    };
    auto& errors() const { return m_errors; }

private:
    ErrorOr<Optional<Token>> next_expanded_token(Optional<Reduction> starting_reduction = {});
    Vector<Token> perform_expansions(Vector<Token> tokens);
    ErrorOr<void> fill_token_buffer(Optional<Reduction> starting_reduction = {});
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
        handle_heredoc_contents();
        m_token_index++;
    }
    bool eof() const
    {
        return m_token_index == m_token_buffer.size() || m_token_buffer[m_token_index].type == Token::Type::Eof;
    }

    struct CaseItemsResult {
        Vector<AST::Position> pipe_positions;
        Vector<NonnullRefPtr<AST::Node>> nodes;
    };

    ErrorOr<RefPtr<AST::Node>> parse_complete_command();
    ErrorOr<RefPtr<AST::Node>> parse_list();
    ErrorOr<RefPtr<AST::Node>> parse_and_or();
    ErrorOr<RefPtr<AST::Node>> parse_pipeline();
    ErrorOr<RefPtr<AST::Node>> parse_pipe_sequence(bool is_negated);
    ErrorOr<RefPtr<AST::Node>> parse_command();
    ErrorOr<RefPtr<AST::Node>> parse_compound_command();
    ErrorOr<RefPtr<AST::Node>> parse_subshell();
    ErrorOr<RefPtr<AST::Node>> parse_compound_list();
    ErrorOr<RefPtr<AST::Node>> parse_term();
    ErrorOr<RefPtr<AST::Node>> parse_for_clause();
    ErrorOr<RefPtr<AST::Node>> parse_case_clause();
    ErrorOr<RefPtr<AST::Node>> parse_if_clause();
    ErrorOr<RefPtr<AST::Node>> parse_while_clause();
    ErrorOr<RefPtr<AST::Node>> parse_until_clause();
    ErrorOr<RefPtr<AST::Node>> parse_function_definition();
    ErrorOr<RefPtr<AST::Node>> parse_function_body();
    ErrorOr<RefPtr<AST::Node>> parse_brace_group();
    ErrorOr<RefPtr<AST::Node>> parse_do_group();
    ErrorOr<RefPtr<AST::Node>> parse_simple_command();
    ErrorOr<RefPtr<AST::Node>> parse_prefix();
    ErrorOr<RefPtr<AST::Node>> parse_suffix();
    ErrorOr<RefPtr<AST::Node>> parse_io_redirect();
    ErrorOr<RefPtr<AST::Node>> parse_redirect_list();
    ErrorOr<RefPtr<AST::Node>> parse_io_file(AST::Position, Optional<int> fd);
    ErrorOr<RefPtr<AST::Node>> parse_io_here(AST::Position, Optional<int> fd);
    ErrorOr<RefPtr<AST::Node>> parse_word();
    ErrorOr<RefPtr<AST::Node>> parse_bash_like_list();
    ErrorOr<CaseItemsResult> parse_case_list();

    template<typename... Ts>
    void error(Token const& token, CheckedFormatString<Ts...> fmt, Ts&&... args)
    {
        m_errors.append(Error {
            ByteString::formatted(fmt.view(), forward<Ts>(args)...),
            token.position,
        });
    }

    Lexer m_lexer;
    bool m_in_interactive_mode { false };
    Vector<Token, 2> m_token_buffer;
    size_t m_token_index { 0 };
    Vector<Token> m_previous_token_buffer;

    Vector<Error> m_errors;
    HashMap<String, NonnullRefPtr<AST::Heredoc>> m_unprocessed_heredoc_entries;

    Token m_eof_token;

    bool m_disallow_command_prefix { true };
};

}
