/*
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/CSSGroupingRule.h>
#include <LibWeb/CSS/CSSRuleList.h>

namespace Web::CSS {

CSSGroupingRule::CSSGroupingRule(NonnullRefPtrVector<CSSRule>&& rules)
    : m_rules(CSSRuleList::create(move(rules)))
{
    for (auto& rule : *m_rules)
        rule.set_parent_rule(this);
}

DOM::ExceptionOr<u32> CSSGroupingRule::insert_rule(StringView rule, u32 index)
{
    TRY(m_rules->insert_a_css_rule(rule, index));
    // NOTE: The spec doesn't say where to set the parent rule, so we'll do it here.
    m_rules->item(index)->set_parent_rule(this);
    return index;
}

DOM::ExceptionOr<void> CSSGroupingRule::delete_rule(u32 index)
{
    return m_rules->remove_a_css_rule(index);
}

void CSSGroupingRule::for_each_effective_style_rule(Function<void(CSSStyleRule const&)> const& callback) const
{
    m_rules->for_each_effective_style_rule(callback);
}

void CSSGroupingRule::set_parent_style_sheet(CSSStyleSheet* parent_style_sheet)
{
    CSSRule::set_parent_style_sheet(parent_style_sheet);
    for (auto& rule : *m_rules)
        rule.set_parent_style_sheet(parent_style_sheet);
}

}
