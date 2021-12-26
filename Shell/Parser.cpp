#include "Parser.h"
#include <stdio.h>
#include <unistd.h>

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
    m_subcommands.append({ move(m_tokens), move(m_redirections) });
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

Vector<Subcommand> Parser::parse()
{
    for (int i = 0; i < m_input.length(); ++i) {
        char ch = m_input.characters()[i];
        switch (m_state) {
        case State::Free:
            if (ch == ' ') {
                commit_token();
                break;
            }
            if (ch == '|') {
                commit_token();
                if (m_tokens.is_empty()) {
                    fprintf(stderr, "Syntax error: Nothing before pipe (|)\n");
                    return { };
                }
                do_pipe();
                break;
            }
            if (ch == '>') {
                commit_token();
                begin_redirect_write(STDOUT_FILENO);
                m_state = State::InRedirectionPath;
                break;
            }
            if (ch == '<') {
                commit_token();
                begin_redirect_read(STDIN_FILENO);
                m_state = State::InRedirectionPath;
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
            m_token.append(ch);
            break;
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
                    return { };
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
            m_token.append(ch);
            break;
        };
    }
    commit_token();
    commit_subcommand();

    if (!m_subcommands.is_empty()) {
        for (auto& redirection : m_subcommands.last().redirections) {
            if (redirection.type == Redirection::Pipe) {
                fprintf(stderr, "Syntax error: Nothing after last pipe (|)\n");
                return { };
            }
        }
    }

    return move(m_subcommands);
}
