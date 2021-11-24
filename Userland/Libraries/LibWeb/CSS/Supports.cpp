/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/Supports.h>

namespace Web::CSS {

Supports::Supports(NonnullOwnPtr<Condition>&& condition)
    : m_condition(move(condition))
{
    auto result = m_condition->evaluate();
    if (result == MatchResult::Unknown) {
        dbgln("!!! Evaluation of CSS Supports returned 'Unknown'!");
    }
    m_matches = result == MatchResult::True;
}

MatchResult Supports::Condition::evaluate() const
{
    switch (type) {
    case Type::Not:
        return negate(children.first().evaluate());
    case Type::And:
        return evaluate_and(children, [](auto& child) { return child.evaluate(); });
    case Type::Or:
        return evaluate_or(children, [](auto& child) { return child.evaluate(); });
    }
    VERIFY_NOT_REACHED();
}

MatchResult Supports::InParens::evaluate() const
{
    return value.visit(
        [&](NonnullOwnPtr<Condition>& condition) {
            return condition->evaluate();
        },
        [&](Feature& feature) {
            return feature.evaluate();
        },
        [&](GeneralEnclosed& general_enclosed) {
            return general_enclosed.evaluate();
        });
}

MatchResult Supports::Feature::evaluate() const
{
    auto style_property = Parser({}, declaration).parse_as_declaration();
    if (style_property.has_value())
        return MatchResult::True;
    return MatchResult::False;
}

}
