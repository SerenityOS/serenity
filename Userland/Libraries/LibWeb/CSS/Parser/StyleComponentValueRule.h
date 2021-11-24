/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <AK/RefPtr.h>
#include <LibWeb/CSS/Parser/Token.h>

namespace Web::CSS {

class StyleBlockRule;
class StyleFunctionRule;

class StyleComponentValueRule {
    friend class Parser;

public:
    enum class ComponentType {
        Token,
        Function,
        Block
    };

    StyleComponentValueRule(Token);
    explicit StyleComponentValueRule(NonnullRefPtr<StyleFunctionRule>);
    explicit StyleComponentValueRule(NonnullRefPtr<StyleBlockRule>);
    ~StyleComponentValueRule();

    bool is_block() const { return m_type == ComponentType::Block; }
    StyleBlockRule const& block() const
    {
        VERIFY(is_block());
        return *m_block;
    }

    bool is_function() const { return m_type == ComponentType::Function; }
    StyleFunctionRule const& function() const
    {
        VERIFY(is_function());
        return *m_function;
    }

    bool is(Token::Type type) const
    {
        return m_type == ComponentType::Token && m_token.is(type);
    }
    Token const& token() const { return m_token; }
    operator Token() const { return m_token; }

    String to_string() const;
    String to_debug_string() const;

private:
    ComponentType m_type;
    Token m_token;
    RefPtr<StyleFunctionRule> m_function;
    RefPtr<StyleBlockRule> m_block;
};
}
