#pragma once

#include <AK/AKString.h>
#include <AK/Vector.h>

struct Redirection {
    enum Type { Pipe, FileWrite, FileRead, Rewire };
    Type type;
    int fd { -1 };
    int rewire_fd { -1 };
    String path { };
};

struct Subcommand {
    Vector<String> args;
    Vector<Redirection> redirections;
};

class Parser {
public:
    explicit Parser(const String& input) : m_input(input) { }

    Vector<Subcommand> parse();

private:
    void commit_token();
    void commit_subcommand();
    void do_pipe();
    void begin_redirect_read(int fd);
    void begin_redirect_write(int fd);

    enum State {
        Free,
        InSingleQuotes,
        InDoubleQuotes,
        InRedirectionPath,
    };
    State m_state { Free };
    String m_input;

    Vector<Subcommand> m_subcommands;
    Vector<String> m_tokens;
    Vector<Redirection> m_redirections;
    Vector<char> m_token;
};

