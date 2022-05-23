/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/CSSRule.h>
#include <LibWeb/CSS/CSSStyleSheet.h>

namespace Web::CSS {

// https://www.w3.org/TR/cssom/#dom-cssrule-csstext
String CSSRule::css_text() const
{
    // The cssText attribute must return a serialization of the CSS rule.
    return serialized();
}

// https://www.w3.org/TR/cssom/#dom-cssrule-csstext
void CSSRule::set_css_text(StringView)
{
    // On setting the cssText attribute must do nothing.
}

void CSSRule::set_parent_rule(CSSRule* parent_rule)
{
    if (parent_rule)
        m_parent_rule = parent_rule->make_weak_ptr();
    else
        m_parent_rule = nullptr;
}

void CSSRule::set_parent_style_sheet(CSSStyleSheet* parent_style_sheet)
{
    if (parent_style_sheet)
        m_parent_style_sheet = parent_style_sheet->make_weak_ptr();
    else
        m_parent_style_sheet = nullptr;
}

}
