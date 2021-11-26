/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/Parser/StyleDeclarationRule.h>
#include <LibWeb/CSS/Parser/StyleRule.h>

namespace Web::CSS {

class DeclarationOrAtRule {
    friend class Parser;

public:
    explicit DeclarationOrAtRule(RefPtr<StyleRule> at);
    explicit DeclarationOrAtRule(StyleDeclarationRule declaration);
    ~DeclarationOrAtRule();

    enum class DeclarationType {
        At,
        Declaration,
    };

    bool is_at_rule() const { return m_type == DeclarationType::At; }
    bool is_declaration() const { return m_type == DeclarationType::Declaration; }

    StyleRule const& at_rule() const
    {
        VERIFY(is_at_rule());
        return *m_at;
    }

    StyleDeclarationRule const& declaration() const
    {
        VERIFY(is_declaration());
        return m_declaration;
    }

    String to_string() const;

private:
    DeclarationType m_type;
    RefPtr<StyleRule> m_at;
    StyleDeclarationRule m_declaration;
};

}
