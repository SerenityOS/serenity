/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibWeb/CSS/Parser/Declaration.h>
#include <LibWeb/CSS/Parser/Rule.h>

namespace Web::CSS::Parser {

class DeclarationOrAtRule {
public:
    explicit DeclarationOrAtRule(RefPtr<Rule> at);
    explicit DeclarationOrAtRule(Declaration declaration);
    ~DeclarationOrAtRule();

    enum class DeclarationType {
        At,
        Declaration,
    };

    bool is_at_rule() const { return m_type == DeclarationType::At; }
    bool is_declaration() const { return m_type == DeclarationType::Declaration; }

    Rule const& at_rule() const
    {
        VERIFY(is_at_rule());
        return *m_at;
    }

    Declaration const& declaration() const
    {
        VERIFY(is_declaration());
        return *m_declaration;
    }

private:
    DeclarationType m_type;
    RefPtr<Rule> m_at;
    Optional<Declaration> m_declaration;
};

}
