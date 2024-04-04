/*
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Realm.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/Supports.h>

namespace Web::CSS {

Supports::Supports(JS::Realm& realm, NonnullOwnPtr<Condition>&& condition)
    : m_condition(move(condition))
{
    m_matches = m_condition->evaluate(realm);
}

bool Supports::Condition::evaluate(JS::Realm& realm) const
{
    switch (type) {
    case Type::Not:
        return !children.first().evaluate(realm);
    case Type::And:
        for (auto& child : children) {
            if (!child.evaluate(realm))
                return false;
        }
        return true;
    case Type::Or:
        for (auto& child : children) {
            if (child.evaluate(realm))
                return true;
        }
        return false;
    }
    VERIFY_NOT_REACHED();
}

bool Supports::InParens::evaluate(JS::Realm& realm) const
{
    return value.visit(
        [&](NonnullOwnPtr<Condition> const& condition) {
            return condition->evaluate(realm);
        },
        [&](Feature const& feature) {
            return feature.evaluate(realm);
        },
        [&](GeneralEnclosed const&) {
            return false;
        });
}

bool Supports::Declaration::evaluate(JS::Realm& realm) const
{
    auto style_property = parse_css_supports_condition(Parser::ParsingContext { realm }, declaration);
    return style_property.has_value();
}

bool Supports::Selector::evaluate(JS::Realm& realm) const
{
    auto style_property = parse_selector(Parser::ParsingContext { realm }, selector);
    return style_property.has_value();
}

bool Supports::Feature::evaluate(JS::Realm& realm) const
{
    return value.visit(
        [&](Declaration const& declaration) {
            return declaration.evaluate(realm);
        },
        [&](Selector const& selector) {
            return selector.evaluate(realm);
        });
}

String Supports::Declaration::to_string() const
{
    return MUST(String::formatted("({})", declaration));
}

String Supports::Selector::to_string() const
{
    return MUST(String::formatted("selector({})", selector));
}

String Supports::Feature::to_string() const
{
    return value.visit([](auto& it) { return it.to_string(); });
}

String Supports::InParens::to_string() const
{
    return value.visit(
        [](NonnullOwnPtr<Condition> const& condition) { return MUST(String::formatted("({})", condition->to_string())); },
        [](Supports::Feature const& it) { return it.to_string(); },
        [](GeneralEnclosed const& it) { return it.to_string(); });
}

String Supports::Condition::to_string() const
{
    switch (type) {
    case Type::Not:
        return MUST(String::formatted("not {}", children.first().to_string()));
    case Type::And:
        return MUST(String::join(" and "sv, children));
    case Type::Or:
        return MUST(String::join(" or "sv, children));
    }
    VERIFY_NOT_REACHED();
}

String Supports::to_string() const
{
    return m_condition->to_string();
}

}
