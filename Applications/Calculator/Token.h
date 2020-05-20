//
// Created by zac on 17/5/20.
//

#pragma once

#include <AK/String.h>

class Token final {
public:
    enum class Kind {
        INVALID,
        NUMBER,
        PLUS,
        MINUS,
        MULTIPLY,
        DIVIDE,
        OPEN_PARENTHESIS,
        CLOSE_PARENTHESIS
    };

    Token(Kind);
    Token(Kind, double);
    Kind kind() const { return m_kind; }
    double value() const { return m_value; }

    void print_token(const DebugLogStream&);

private:
    Kind m_kind;
    double m_value;
};
