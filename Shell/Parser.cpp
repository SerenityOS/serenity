#include "Parser.h"
#include <stdio.h>
#include <unistd.h>
#include <ctype.h>

void Parser::commit_token()
{
    if (m_token.is_empty())
        return;
    if (m_state == InRedirectionPath) {
        m_redirections.last().path = String::copy(m_token);
        m_token.clear_with_capacity();
        return;
    }
    m_tokens.append(String::copy(m_token));
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

Vector<Command> Parser::parse()
{
    for (int i = 0; i < m_input.length(); ++i) {
        char ch = m_input.characters()[i];
        switch (m_state) {
        case State::Free:
            if (ch == ' ') {
                commit_token();
                break;
            }
            if (ch == ';') {
                commit_token();
                commit_subcommand();
                commit_command();
                break;
            }
            if (ch == '|') {
                commit_token();
                if (m_tokens.is_empty()) {
                    fprintf(stderr, "Syntax error: Nothing before pipe (|)\n");
                    return {};
                }
                do_pipe();
                break;
            }
            if (ch == '>') {
                commit_token();
                begin_redirect_write(STDOUT_FILENO);

                // Search for another > for append.
                m_state = State::InWriteAppendOrRedirectionPath;
                break;
            }
            if (ch == '<') {
                commit_token();
                begin_redirect_read(STDIN_FILENO);
                m_state = State::InRedirectionPath;
                break;
            }
            if (ch == '\\') {
                if (i == m_input.length() - 1) {
                    fprintf(stderr, "Syntax error: Nothing to escape (\\)\n");
                    return {};
                }
                char next_ch = m_input.characters()[i + 1];
                m_token.append(next_ch);
                ++i;
                break;
            }
            if (ch == '\'') {
                m_state = State::InSingleQuotes;
                break;
            }
            if (ch == '\"') {
                m_state = State::InDoubleQuotes;
                break;
            }
            if (isdigit(ch)) {
                if (i != m_input.length() - 1) {
                    char next_ch = m_input.characters()[i + 1];
                    if (next_ch == '>') {
                        commit_token();
                        begin_redirect_write(ch - '0');
                        ++i;

                        // Search for another > for append.
                        m_state = State::InWriteAppendOrRedirectionPath;
                        break;
                    }
                    if (next_ch == '<') {
                        commit_token();
                        begin_redirect_read(ch - '0');
                        ++i;

                        m_state = State::InRedirectionPath;
                        break;
                    }
                }
            }
            m_token.append(ch);
            break;
        case State::InWriteAppendOrRedirectionPath:
            if (ch == '>') {
                commit_token();
                m_state = State::InRedirectionPath;
                ASSERT(m_redirections.size());
                m_redirections[m_redirections.size() - 1].type = Redirection::FileWriteAppend;
                break;
            }

            // Not another > means that it's probably a path.
            m_state = InRedirectionPath;
            [[fallthrough]];
        case State::InRedirectionPath:
            if (ch == '<') {
                commit_token();
                begin_redirect_read(STDIN_FILENO);
                m_state = State::InRedirectionPath;
                break;
            }
            if (ch == '>') {
                commit_token();
                begin_redirect_read(STDOUT_FILENO);
                m_state = State::InRedirectionPath;
                break;
            }
            if (ch == '|') {
                commit_token();
                if (m_tokens.is_empty()) {
                    fprintf(stderr, "Syntax error: Nothing before pipe (|)\n");
                    return {};
                }
                do_pipe();
                m_state = State::Free;
                break;
            }
            if (ch == ' ')
                break;
            m_token.append(ch);
            break;
        case State::InSingleQuotes:
            if (ch == '\'') {
                commit_token();
                m_state = State::Free;
                break;
            }
            m_token.append(ch);
            break;
        case State::InDoubleQuotes:
            if (ch == '\"') {
                commit_token();
                m_state = State::Free;
                break;
            }
            if (ch == '\\') {
                if (i == m_input.length() - 1) {
                    fprintf(stderr, "Syntax error: Nothing to escape (\\)\n");
                    return {};
                }
                char next_ch = m_input.characters()[i + 1];
                if (next_ch == '$' || next_ch == '`'
                    || next_ch == '"' || next_ch == '\\') {
                    m_token.append(next_ch);
                    ++i;
                    continue;
                }
                m_token.append('\\');
                break;
            }
            m_token.append(ch);
            break;
        };
    }
    commit_token();
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
