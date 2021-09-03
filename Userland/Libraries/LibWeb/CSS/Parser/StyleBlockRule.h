/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <YAK/RefCounted.h>
#include <YAK/Vector.h>
#include <LibWeb/CSS/Parser/StyleComponentValueRule.h>
#include <LibWeb/CSS/Parser/Token.h>

namespace Web::CSS {

class StyleBlockRule : public RefCounted<StyleBlockRule> {
    friend class Parser;

public:
    StyleBlockRule();
    ~StyleBlockRule();

    bool is_curly() const { return m_token.is(Token::Type::OpenCurly); }
    bool is_paren() const { return m_token.is(Token::Type::OpenParen); }
    bool is_square() const { return m_token.is(Token::Type::OpenSquare); }

    Vector<StyleComponentValueRule> const& values() const { return m_values; }

    String to_string() const;

private:
    Token m_token;
    Vector<StyleComponentValueRule> m_values;
};
}
