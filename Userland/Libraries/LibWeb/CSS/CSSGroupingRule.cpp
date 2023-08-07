/*
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/CSSGroupingRulePrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Bindings/MainThreadVM.h>
#include <LibWeb/CSS/CSSGroupingRule.h>
#include <LibWeb/CSS/CSSRuleList.h>
#include <LibWeb/HTML/Window.h>

namespace Web::CSS {

CSSGroupingRule::CSSGroupingRule(JS::Realm& realm, CSSRuleList& rules)
    : CSSRule(realm)
    , m_rules(rules)
{
    for (auto& rule : *m_rules)
        rule->set_parent_rule(this);
}

void CSSGroupingRule::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::CSSGroupingRulePrototype>(realm, "CSSGroupingRule"));
}

void CSSGroupingRule::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_rules);
}

WebIDL::ExceptionOr<u32> CSSGroupingRule::insert_rule(StringView rule, u32 index)
{
    TRY(m_rules->insert_a_css_rule(rule, index));
    // NOTE: The spec doesn't say where to set the parent rule, so we'll do it here.
    m_rules->item(index)->set_parent_rule(this);
    return index;
}

WebIDL::ExceptionOr<void> CSSGroupingRule::delete_rule(u32 index)
{
    return m_rules->remove_a_css_rule(index);
}

void CSSGroupingRule::for_each_effective_style_rule(Function<void(CSSStyleRule const&)> const& callback) const
{
    m_rules->for_each_effective_style_rule(callback);
}

void CSSGroupingRule::for_each_effective_keyframes_at_rule(Function<void(CSSKeyframesRule const&)> const& callback) const
{
    m_rules->for_each_effective_keyframes_at_rule(callback);
}

void CSSGroupingRule::set_parent_style_sheet(CSSStyleSheet* parent_style_sheet)
{
    CSSRule::set_parent_style_sheet(parent_style_sheet);
    for (auto& rule : *m_rules)
        rule->set_parent_style_sheet(parent_style_sheet);
}

}
