/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
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
    StyleComponentValueRule(Token);
    explicit StyleComponentValueRule(NonnullRefPtr<StyleFunctionRule>);
    explicit StyleComponentValueRule(NonnullRefPtr<StyleBlockRule>);
    ~StyleComponentValueRule();

    bool is_block() const { return m_value.has<NonnullRefPtr<StyleBlockRule>>(); }
    StyleBlockRule const& block() const { return m_value.get<NonnullRefPtr<StyleBlockRule>>(); }

    bool is_function() const { return m_value.has<NonnullRefPtr<StyleFunctionRule>>(); }
    StyleFunctionRule const& function() const { return m_value.get<NonnullRefPtr<StyleFunctionRule>>(); }

    bool is_token() const { return m_value.has<Token>(); }
    bool is(Token::Type type) const { return is_token() && token().is(type); }
    Token const& token() const { return m_value.get<Token>(); }
    operator Token() const { return m_value.get<Token>(); }

    String to_string() const;
    String to_debug_string() const;

private:
    Variant<Token, NonnullRefPtr<StyleFunctionRule>, NonnullRefPtr<StyleBlockRule>> m_value;
};
}
