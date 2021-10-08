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

Supports::MatchResult Supports::Condition::evaluate() const
{
    switch (type) {
    case Type::Not:
        return negate(children.first().evaluate());
    case Type::And: {
        size_t true_results = 0;
        for (auto& child : children) {
            auto child_match = child.evaluate();
            if (child_match == MatchResult::False)
                return MatchResult::False;
            if (child_match == MatchResult::True)
                true_results++;
        }
        if (true_results == children.size())
            return MatchResult::True;
        return MatchResult::Unknown;
    }
    case Type::Or: {
        size_t false_results = 0;
        for (auto& child : children) {
            auto child_match = child.evaluate();
            if (child_match == MatchResult::True)
                return MatchResult::True;
            if (child_match == MatchResult::False)
                false_results++;
        }
        if (false_results == children.size())
            return MatchResult::False;
        return MatchResult::Unknown;
    }
    }
    VERIFY_NOT_REACHED();
}

Supports::MatchResult Supports::InParens::evaluate() const
{
    return value.visit(
        [&](NonnullOwnPtr<Condition>& condition) {
            return condition->evaluate();
        },
        [&](Feature& feature) {
            return feature.evaluate();
        },
        [&](GeneralEnclosed&) {
            return MatchResult::Unknown;
        });
}

Supports::MatchResult Supports::Feature::evaluate() const
{
    auto style_property = Parser({}, "").convert_to_style_property(declaration);
    if (style_property.has_value())
        return MatchResult::True;
    return MatchResult::False;
}

}
