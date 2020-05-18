/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "Parser.h"
#include <AK/StringBuilder.h>
#include <AK/Utf32View.h>
#include <ctype.h>
#include <stdio.h>
#include <unistd.h>

void Parser::commit_token(Token::Type type, AllowEmptyToken allow_empty)
{
    if (allow_empty == AllowEmptyToken::No && m_token.is_empty())
        return;
    StringBuilder builder;
    builder.append(Utf32View { m_token.data(), m_token.size() });
    auto token = builder.build();
    if (state() == InRedirectionPath) {
        m_redirections.last().path = token;
        m_token.clear_with_capacity();
        return;
    }
    m_tokens.append({ token, m_position, m_token.size(), type });
    m_token.clear_with_capacity();
};

void Parser::commit_subcommand()
{
    if (m_tokens.is_empty())
        return;
    m_subcommands.append({ move(m_tokens), move(m_redirections), {} });
}

void Parser::commit_command()
{
    if (m_subcommands.is_empty())
        return;
    m_commands.append({ move(m_subcommands) });
}

void Parser::do_pipe()
{
    m_redirections.append({ Redirection::Pipe, STDOUT_FILENO });
    commit_subcommand();
}

void Parser::begin_redirect_read(int fd)
{
    m_redirections.append({ Redirection::FileRead, fd });
}

void Parser::begin_redirect_write(int fd)
{
    m_redirections.append({ Redirection::FileWrite, fd });
}

bool Parser::in_state(State state) const
{
    for (auto s : m_state_stack)
        if (s == state)
            return true;
    return false;
}

Vector<Command> Parser::parse()
{
    auto it = m_input.begin();
    auto length = m_input.length_in_codepoints();
    for (size_t i = 0; i < length; ++i, ++it, m_position = i) {
        auto ch = *it;
        switch (state()) {
        case State::Free:
            if (ch == '#') {
                commit_token(Token::Bare);

                while (i < length) {
                    ch = *++it;
                    ++i;
                    ++m_position;
                    if (ch == '\n')
                        break;
                    m_token.append(ch);
                }
                commit_token(Token::Comment);
                break;
            }
            if (ch == ' ') {
                commit_token(Token::Bare);
                break;
            }
            if (ch == ';') {
                commit_token(Token::Special);
                commit_subcommand();
                commit_command();
                break;
            }
            if (ch == '|') {
                commit_token(Token::Special);
                if (m_tokens.is_empty()) {
                    fprintf(stderr, "Syntax error: Nothing before pipe (|)\n");
                    return {};
                }
                do_pipe();
                break;
            }
            if (ch == '>') {
                commit_token(Token::Special);
                begin_redirect_write(STDOUT_FILENO);

                // Search for another > for append.
                push_state(State::InWriteAppendOrRedirectionPath);
                break;
            }
            if (ch == '<') {
                commit_token(Token::Special);
                begin_redirect_read(STDIN_FILENO);
                push_state(State::InRedirectionPath);
                break;
            }
            if (ch == '\\') {
                if (i == length - 1) {
                    fprintf(stderr, "Syntax error: Nothing to escape (\\)\n");
                    return {};
                }
                char next_ch = *++it;
                m_token.append(next_ch);
                ++i;
                break;
            }
            if (ch == '\'') {
                push_state(State::InSingleQuotes);
                break;
            }
            if (ch == '\"') {
                push_state(State::InDoubleQuotes);
                break;
            }

            // redirection from zsh-style multi-digit fd, such as {10}>file
            if (ch == '{') {
                bool is_multi_fd_redirection = false;
                size_t redir_end = i + 1;
                auto lookahead_it = it;
                ++lookahead_it;

                while (redir_end < length) {
                    char lookahead_ch = *lookahead_it;
                    if (isdigit(lookahead_ch)) {
                        ++redir_end;
                        ++lookahead_it;
                        continue;
                    }
                    if (lookahead_ch == '}' && redir_end + 1 != length) {
                        // Disallow {}> and {}<
                        if (redir_end == i + 1)
                            break;

                        ++redir_end;
                        auto ch = *++lookahead_it;
                        if (ch == '>' || ch == '<')
                            is_multi_fd_redirection = true;
                        break;
                    }
                    break;
                }

                if (is_multi_fd_redirection) {
                    commit_token(Token::Special);

                    int fd = atoi((const char*)m_input.bytes() + m_input.byte_offset_of(++it));

                    if (*it == '>') {
                        begin_redirect_write(fd);
                        // Search for another > for append.
                        push_state(State::InWriteAppendOrRedirectionPath);
                    }
                    if (*it == '<') {
                        begin_redirect_read(fd);
                        push_state(State::InRedirectionPath);
                    }

                    i = redir_end;

                    break;
                }
            }
            if (isdigit(ch)) {
                if (i != length - 1) {
                    auto next_it = it;
                    char next_ch = *next_it;
                    if (next_ch == '>') {
                        commit_token(Token::Special);
                        begin_redirect_write(ch - '0');
                        ++i;
                        ++it;

                        // Search for another > for append.
                        push_state(State::InWriteAppendOrRedirectionPath);
                        break;
                    }
                    if (next_ch == '<') {
                        commit_token(Token::Special);
                        begin_redirect_read(ch - '0');
                        ++i;
                        ++it;

                        push_state(State::InRedirectionPath);
                        break;
                    }
                }
            }
            m_token.append(ch);
            break;
        case State::InWriteAppendOrRedirectionPath:
            if (ch == '>') {
                commit_token(Token::Special);
                pop_state();
                push_state(State::InRedirectionPath);
                ASSERT(m_redirections.size());
                m_redirections[m_redirections.size() - 1].type = Redirection::FileWriteAppend;
                break;
            }

            // Not another > means that it's probably a path.
            pop_state();
            push_state(InRedirectionPath);
            [[fallthrough]];
        case State::InRedirectionPath:
            if (ch == '<') {
                commit_token(Token::Special);
                begin_redirect_read(STDIN_FILENO);
                pop_state();
                push_state(State::InRedirectionPath);
                break;
            }
            if (ch == '>') {
                commit_token(Token::Special);
                begin_redirect_read(STDOUT_FILENO);
                pop_state();
                push_state(State::InRedirectionPath);
                break;
            }
            if (ch == '|') {
                commit_token(Token::Special);
                if (m_tokens.is_empty()) {
                    fprintf(stderr, "Syntax error: Nothing before pipe (|)\n");
                    return {};
                }
                do_pipe();
                pop_state();
                break;
            }
            if (ch == '"') {
                push_state(State::InDoubleQuotes);
                break;
            }
            if (ch == '\'') {
                push_state(State::InSingleQuotes);
                break;
            }
            if (ch == ' ')
                break;
            m_token.append(ch);
            break;
        case State::InSingleQuotes:
            if (ch == '\'') {
                if (!in_state(State::InRedirectionPath))
                    commit_token(Token::SingleQuoted, AllowEmptyToken::Yes);
                pop_state();
                break;
            }
            m_token.append(ch);
            break;
        case State::InDoubleQuotes:
            if (ch == '\"') {
                if (!in_state(State::InRedirectionPath))
                    commit_token(Token::DoubleQuoted, AllowEmptyToken::Yes);
                pop_state();
                break;
            }
            if (ch == '\\') {
                if (i == length - 1) {
                    fprintf(stderr, "Syntax error: Nothing to escape (\\)\n");
                    return {};
                }
                auto next_it = it;
                ++next_it;
                char next_ch = *next_it;
                if (next_ch == '$' || next_ch == '`'
                    || next_ch == '"' || next_ch == '\\') {
                    m_token.append(next_ch);
                    ++i;
                    ++it;
                    continue;
                }
                m_token.append('\\');
                break;
            }
            m_token.append(ch);
            break;
        };
    }

    while (m_state_stack.size() > 1) {
        if (state() == State::InDoubleQuotes) {
            commit_token(Token::UnterminatedDoubleQuoted, AllowEmptyToken::Yes);
        } else if (state() == State::InSingleQuotes) {
            commit_token(Token::UnterminatedSingleQuoted, AllowEmptyToken::Yes);
        } else {
            commit_token(Token::Bare, AllowEmptyToken::No);
        }
        pop_state();
    }
    ASSERT(state() == State::Free);

    commit_token(Token::Bare);
    commit_subcommand();
    commit_command();

    if (!m_subcommands.is_empty()) {
        for (auto& redirection : m_subcommands.last().redirections) {
            if (redirection.type == Redirection::Pipe) {
                fprintf(stderr, "Syntax error: Nothing after last pipe (|)\n");
                return {};
            }
        }
    }

    return move(m_commands);
}
