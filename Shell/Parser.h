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

#pragma once

#include <AK/String.h>
#include <AK/Vector.h>

struct Token {
    enum Type {
        Bare,
        SingleQuoted,
        DoubleQuoted,
        UnterminatedSingleQuoted,
        UnterminatedDoubleQuoted,
        Comment,
        Special,
    };
    String text;
    size_t end;
    size_t length;
    Type type;
};

enum Attributes {
    None = 0x0,
    ShortCircuitOnFailure = 0x1,
    InBackground = 0x2,
};

struct Redirection {
    enum Type {
        Pipe,
        FileWrite,
        FileWriteAppend,
        FileRead,
    };
    Type type;
    int fd { -1 };
    int rewire_fd { -1 };
    size_t redirection_op_start { 0 };
    Token path {};
};

struct Rewiring {
    int fd { -1 };
    int rewire_fd { -1 };
};

struct Subcommand {
    Vector<Token> args;
    Vector<Redirection> redirections;
    Vector<Rewiring> rewirings;
};

struct Command {
    Vector<Subcommand> subcommands;
    Attributes attributes;
};

class Parser {
public:
    explicit Parser(const String& input)
        : m_input(input)
    {
    }

    Vector<Command> parse();

private:
    enum class AllowEmptyToken {
        No,
        Yes,
    };
    void commit_token(Token::Type, AllowEmptyToken = AllowEmptyToken::No);
    void commit_subcommand();
    void commit_command(Attributes = None);
    void do_pipe();
    void begin_redirect_read(int fd);
    void begin_redirect_write(int fd);

    enum State {
        Free,
        InSingleQuotes,
        InDoubleQuotes,
        InWriteAppendOrRedirectionPath,
        InRedirectionPath,
    };

    State state() const { return m_state_stack.last(); }

    void pop_state()
    {
        m_state_stack.take_last();
    }

    void push_state(State state)
    {
        m_state_stack.append(state);
    }

    bool in_state(State) const;

    Vector<State> m_state_stack { Free };
    String m_input;

    Vector<Command> m_commands;
    Vector<Subcommand> m_subcommands;
    Vector<Token> m_tokens;
    Vector<Redirection> m_redirections;
    Vector<char> m_token;
    size_t m_position { 0 };
};
