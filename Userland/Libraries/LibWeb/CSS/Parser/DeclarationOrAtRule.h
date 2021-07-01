/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/Parser/AtStyleRule.h>
#include <LibWeb/CSS/Parser/StyleDeclarationRule.h>

namespace Web::CSS {

class DeclarationOrAtRule {
    friend class Parser;

public:
    explicit DeclarationOrAtRule(RefPtr<AtStyleRule> at);
    explicit DeclarationOrAtRule(StyleDeclarationRule declaration);
    ~DeclarationOrAtRule();

    enum class DeclarationType {
        At,
        Declaration,
    };

    String to_string() const;

private:
    DeclarationType m_type;
    RefPtr<AtStyleRule> m_at;
    StyleDeclarationRule m_declaration;
};

}
