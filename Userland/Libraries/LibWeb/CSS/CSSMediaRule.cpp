/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibJS/Runtime/Realm.h>
#include <LibWeb/Bindings/CSSMediaRulePrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/CSS/CSSMediaRule.h>

namespace Web::CSS {

JS::NonnullGCPtr<CSSMediaRule> CSSMediaRule::create(JS::Realm& realm, MediaList& media_queries, CSSRuleList& rules)
{
    return realm.heap().allocate<CSSMediaRule>(realm, realm, media_queries, rules);
}

CSSMediaRule::CSSMediaRule(JS::Realm& realm, MediaList& media, CSSRuleList& rules)
    : CSSConditionRule(realm, rules)
    , m_media(media)
{
}

void CSSMediaRule::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::CSSMediaRulePrototype>(realm, "CSSMediaRule"));
}

void CSSMediaRule::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_media);
}

String CSSMediaRule::condition_text() const
{
    return String::from_deprecated_string(m_media->media_text().to_deprecated_string()).release_value();
}

void CSSMediaRule::set_condition_text(String const& text)
{
    m_media->set_media_text(text);
}

// https://www.w3.org/TR/cssom-1/#serialize-a-css-rule
DeprecatedString CSSMediaRule::serialized() const
{
    // The result of concatenating the following:
    StringBuilder builder;

    // 1. The string "@media", followed by a single SPACE (U+0020).
    builder.append("@media "sv);
    // 2. The result of performing serialize a media query list on rule’s media query list.
    builder.append(condition_text());
    // 3. A single SPACE (U+0020), followed by the string "{", i.e., LEFT CURLY BRACKET (U+007B), followed by a newline.
    builder.append(" {\n"sv);
    // 4. The result of performing serialize a CSS rule on each rule in the rule’s cssRules list, separated by a newline and indented by two spaces.
    for (size_t i = 0; i < css_rules().length(); i++) {
        auto rule = css_rules().item(i);
        if (i != 0)
            builder.append("\n"sv);
        builder.append("  "sv);
        builder.append(rule->css_text());
    }
    // 5. A newline, followed by the string "}", i.e., RIGHT CURLY BRACKET (U+007D)
    builder.append("\n}"sv);

    return builder.to_deprecated_string();
}

}
