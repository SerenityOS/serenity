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
    auto style_property = Parser::Parser({}, declaration).parse_as_supports_condition();
    return style_property.has_value();
}

bool Supports::Selector::evaluate() const
{
    auto style_property = Parser::Parser({}, selector).parse_as_selector();
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

DeprecatedString Supports::Declaration::to_deprecated_string() const
{
    return DeprecatedString::formatted("({})", declaration);
}

DeprecatedString Supports::Selector::to_deprecated_string() const
{
    return DeprecatedString::formatted("selector({})", selector);
}

DeprecatedString Supports::Feature::to_deprecated_string() const
{
    return value.visit([](auto& it) { return it.to_deprecated_string(); });
}

DeprecatedString Supports::InParens::to_deprecated_string() const
{
    return value.visit(
        [](NonnullOwnPtr<Condition> const& condition) -> DeprecatedString { return DeprecatedString::formatted("({})", condition->to_deprecated_string()); },
        [](Supports::Feature const& it) -> DeprecatedString { return it.to_deprecated_string(); },
        [](GeneralEnclosed const& it) -> DeprecatedString { return it.to_string(); });
}

DeprecatedString Supports::Condition::to_deprecated_string() const
{
    switch (type) {
    case Type::Not:
        return DeprecatedString::formatted("not {}", children.first().to_deprecated_string());
    case Type::And:
        return DeprecatedString::join(" and "sv, children);
    case Type::Or:
        return DeprecatedString::join(" or "sv, children);
    }
    VERIFY_NOT_REACHED();
}

DeprecatedString Supports::to_deprecated_string() const
{
    return m_condition->to_deprecated_string();
}

}
