/*
 * Copyright (c) 2021-2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/CSSSupportsRulePrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/CSS/CSSSupportsRule.h>
#include <LibWeb/CSS/Parser/Parser.h>

namespace Web::CSS {

WebIDL::ExceptionOr<JS::NonnullGCPtr<CSSSupportsRule>> CSSSupportsRule::create(JS::Realm& realm, NonnullRefPtr<Supports>&& supports, CSSRuleList& rules)
{
    return MUST_OR_THROW_OOM(realm.heap().allocate<CSSSupportsRule>(realm, realm, move(supports), rules));
}

CSSSupportsRule::CSSSupportsRule(JS::Realm& realm, NonnullRefPtr<Supports>&& supports, CSSRuleList& rules)
    : CSSConditionRule(realm, rules)
    , m_supports(move(supports))
{
}

void CSSSupportsRule::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::CSSSupportsRulePrototype>(realm, "CSSSupportsRule"));
}

DeprecatedString CSSSupportsRule::condition_text() const
{
    return m_supports->to_string().release_value_but_fixme_should_propagate_errors().to_deprecated_string();
}

void CSSSupportsRule::set_condition_text(DeprecatedString text)
{
    if (auto new_supports = parse_css_supports(Parser::ParsingContext { realm() }, text))
        m_supports = new_supports.release_nonnull();
}

// https://www.w3.org/TR/cssom-1/#serialize-a-css-rule
DeprecatedString CSSSupportsRule::serialized() const
{
    // Note: The spec doesn't cover this yet, so I'm roughly following the spec for the @media rule.
    // It should be pretty close!

    StringBuilder builder;

    builder.append("@supports "sv);
    builder.append(condition_text());
    builder.append(" {\n"sv);
    for (size_t i = 0; i < css_rules().length(); i++) {
        auto rule = css_rules().item(i);
        if (i != 0)
            builder.append("\n"sv);
        builder.append("  "sv);
        builder.append(rule->css_text());
    }
    builder.append("\n}"sv);

    return builder.to_deprecated_string();
}

}
