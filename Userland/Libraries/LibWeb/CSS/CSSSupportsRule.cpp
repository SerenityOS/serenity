/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/CSSSupportsRulePrototype.h>
#include <LibWeb/Bindings/WindowObject.h>
#include <LibWeb/CSS/CSSSupportsRule.h>
#include <LibWeb/CSS/Parser/Parser.h>

namespace Web::CSS {

CSSSupportsRule* CSSSupportsRule::create(Bindings::WindowObject& window_object, NonnullRefPtr<Supports>&& supports, CSSRuleList& rules)
{
    return window_object.heap().allocate<CSSSupportsRule>(window_object.realm(), window_object, move(supports), rules);
}

CSSSupportsRule::CSSSupportsRule(Bindings::WindowObject& window_object, NonnullRefPtr<Supports>&& supports, CSSRuleList& rules)
    : CSSConditionRule(window_object, rules)
    , m_supports(move(supports))
{
    set_prototype(&window_object.ensure_web_prototype<Bindings::CSSSupportsRulePrototype>("CSSSupportsRule"));
}

String CSSSupportsRule::condition_text() const
{
    return m_supports->to_string();
}

void CSSSupportsRule::set_condition_text(String text)
{
    if (auto new_supports = parse_css_supports({}, text))
        m_supports = new_supports.release_nonnull();
}

// https://www.w3.org/TR/cssom-1/#serialize-a-css-rule
String CSSSupportsRule::serialized() const
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

    return builder.to_string();
}

}
