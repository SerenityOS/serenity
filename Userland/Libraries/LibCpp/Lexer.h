/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "LibCpp/Token.h"
#include <AK/StringView.h>
#include <AK/Vector.h>

namespace Cpp {

class Lexer {
public:
    explicit Lexer(StringView const&, size_t start_line = 0);

    Vector<Token> lex();

    void set_ignore_whitespace(bool value) { m_options.ignore_whitespace = value; }

private:
    char peek(size_t offset = 0) const;
    char consume();

    StringView m_input;
    size_t m_index { 0 };
    Position m_previous_position { 0, 0 };
    Position m_position { 0, 0 };

    struct Options {
        bool ignore_whitespace { false };
    } m_options;
};

}
