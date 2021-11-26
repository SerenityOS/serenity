/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/CSSSupportsRule.h>
#include <LibWeb/CSS/Parser/Parser.h>

namespace Web::CSS {

CSSSupportsRule::CSSSupportsRule(NonnullRefPtr<Supports>&& supports, NonnullRefPtrVector<CSSRule>&& rules)
    : CSSConditionRule(move(rules))
    , m_supports(move(supports))
{
}

CSSSupportsRule::~CSSSupportsRule()
{
}

String CSSSupportsRule::condition_text() const
{
    // FIXME: Serializing supports rules!
    return "<supports-condition>";
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

    builder.append("@supports ");
    builder.append(condition_text());
    builder.append(" {\n");
    for (size_t i = 0; i < css_rules().length(); i++) {
        auto rule = css_rules().item(i);
        if (i != 0)
            builder.append("\n");
        builder.append("  ");
        builder.append(rule->css_text());
    }
    builder.append("\n}");

    return builder.to_string();
}

}
