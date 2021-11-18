/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/CSSGroupingRule.h>
#include <LibWeb/CSS/CSSRuleList.h>

namespace Web::CSS {

CSSGroupingRule::CSSGroupingRule(NonnullRefPtrVector<CSSRule>&& rules)
    : m_rules(CSSRuleList::create(move(rules)))
{
}

CSSGroupingRule::~CSSGroupingRule()
{
}

size_t CSSGroupingRule::insert_rule(StringView, size_t)
{
    // https://www.w3.org/TR/cssom-1/#insert-a-css-rule
    TODO();
}

void CSSGroupingRule::delete_rule(size_t)
{
    // https://www.w3.org/TR/cssom-1/#remove-a-css-rule
    TODO();
}

void CSSGroupingRule::for_each_effective_style_rule(Function<void(CSSStyleRule const&)> const& callback) const
{
    m_rules->for_each_effective_style_rule(callback);
}

}
