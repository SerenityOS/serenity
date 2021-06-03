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
    Lexer(StringView const&);

    Vector<Token> lex();

private:
    char peek(size_t offset = 0) const;
    char consume();

    StringView m_input;
    size_t m_index { 0 };
    Position m_previous_position { 0, 0 };
    Position m_position { 0, 0 };
};

}
