/*
 * Copyright (c) 2020-2021, the SerenityOS developers.
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
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

}
