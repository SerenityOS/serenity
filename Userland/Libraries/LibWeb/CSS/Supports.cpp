/*
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/Supports.h>

namespace Web::CSS {

Supports::Supports(NonnullOwnPtr<Condition>&& condition)
    : m_condition(move(condition))
{
    m_matches = m_condition->evaluate();
}

bool Supports::Condition::evaluate() const
{
    switch (type) {
    case Type::Not:
        return !children.first().evaluate();
    case Type::And:
        for (auto& child : children) {
            if (!child.evaluate())
                return false;
        }
        return true;
    case Type::Or:
        for (auto& child : children) {
            if (child.evaluate())
                return true;
        }
        return false;
    }
    VERIFY_NOT_REACHED();
}

bool Supports::InParens::evaluate() const
{
    return value.visit(
        [&](NonnullOwnPtr<Condition> const& condition) {
            return condition->evaluate();
        },
        [&](Feature const& feature) {
            return feature.evaluate();
        },
        [&](GeneralEnclosed const&) {
            return false;
        });
}

bool Supports::Declaration::evaluate() const
{
    auto style_property = Parser({}, declaration).parse_as_declaration();
    return style_property.has_value();
}

bool Supports::Selector::evaluate() const
{
    auto style_property = Parser({}, selector).parse_as_selector();
    return style_property.has_value();
}

bool Supports::Feature::evaluate() const
{
    return value.visit(
        [&](Declaration const& declaration) {
            return declaration.evaluate();
        },
        [&](Selector const& selector) {
            return selector.evaluate();
        });
}

}
