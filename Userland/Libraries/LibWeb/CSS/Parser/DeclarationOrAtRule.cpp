/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/DeclarationOrAtRule.h>
#include <LibWeb/CSS/Parser/Function.h>

namespace Web::CSS::Parser {

DeclarationOrAtRule::DeclarationOrAtRule(RefPtr<Rule> at)
    : m_type(DeclarationType::At)
    , m_at(move(at))
{
}

DeclarationOrAtRule::DeclarationOrAtRule(Declaration declaration)
    : m_type(DeclarationType::Declaration)
    , m_declaration(move(declaration))
{
}

DeclarationOrAtRule::~DeclarationOrAtRule() = default;

DeprecatedString DeclarationOrAtRule::to_deprecated_string() const
{
    StringBuilder builder;
    switch (m_type) {
    default:
    case DeclarationType::At:
        builder.append(m_at->to_deprecated_string());
        break;
    case DeclarationType::Declaration:
        builder.append(m_declaration->to_deprecated_string());
        break;
    }

    return builder.to_deprecated_string();
}

}
