/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/Parser/StyleBlockRule.h>
#include <LibWeb/CSS/Parser/StyleFunctionRule.h>
#include <LibWeb/CSS/Parser/Token.h>

namespace Web::CSS {

class StyleComponentValueRule {
    friend class Parser;

public:
    enum class ComponentType {
        Token,
        Function,
        Block
    };

    explicit StyleComponentValueRule(ComponentType);
    ~StyleComponentValueRule();

    bool is_block() const { return m_type == ComponentType::Block; }
    StyleBlockRule const& block() const { return m_block; }

    bool is_function() const { return m_type == ComponentType::Function; }
    StyleFunctionRule const& function() const { return m_function; }

    bool is(Token::TokenType type) const
    {
        return m_type == ComponentType::Token && m_token.is(type);
    }
    Token const& token() const { return m_token; }
    operator Token() const { return m_token; }

    String to_string() const;

private:
    ComponentType m_type;
    Token m_token;
    StyleFunctionRule m_function;
    StyleBlockRule m_block;
};
}
