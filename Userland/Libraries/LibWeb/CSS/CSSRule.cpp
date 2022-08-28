/*
 * Copyright (c) 2021, the SerenityOS developers.
 * Copyright (c) 2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/CSSRulePrototype.h>
#include <LibWeb/CSS/CSSRule.h>
#include <LibWeb/CSS/CSSStyleSheet.h>
#include <LibWeb/HTML/Window.h>

namespace Web::CSS {

CSSRule::CSSRule(HTML::Window& window_object)
    : PlatformObject(window_object.ensure_web_prototype<Bindings::CSSRulePrototype>("CSSRule"))
{
}

void CSSRule::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_parent_style_sheet.ptr());
    visitor.visit(m_parent_rule.ptr());
}

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
    m_parent_rule = parent_rule;
}

void CSSRule::set_parent_style_sheet(CSSStyleSheet* parent_style_sheet)
{
    m_parent_style_sheet = parent_style_sheet;
}

}
