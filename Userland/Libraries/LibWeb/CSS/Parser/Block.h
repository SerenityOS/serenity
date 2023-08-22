/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <AK/Vector.h>
#include <LibWeb/CSS/Parser/ComponentValue.h>
#include <LibWeb/CSS/Parser/Token.h>
#include <LibWeb/Forward.h>

namespace Web::CSS::Parser {

class Block : public RefCounted<Block> {
public:
    static NonnullRefPtr<Block> create(Token token, Vector<ComponentValue>&& values)
    {
        return adopt_ref(*new Block(move(token), move(values)));
    }

    ~Block();

    bool is_curly() const { return m_token.is(Token::Type::OpenCurly); }
    bool is_paren() const { return m_token.is(Token::Type::OpenParen); }
    bool is_square() const { return m_token.is(Token::Type::OpenSquare); }

    Token const& token() const { return m_token; }

    Vector<ComponentValue> const& values() const { return m_values; }

    String to_string() const;

private:
    Block(Token, Vector<ComponentValue>&&);
    Token m_token;
    Vector<ComponentValue> m_values;
};
}
