/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/CSSRuleList.h>

namespace Web::CSS {

CSSRuleList::CSSRuleList(NonnullRefPtrVector<CSSRule>&& rules)
    : m_rules(move(rules))
{
}

CSSRuleList::~CSSRuleList()
{
}

bool CSSRuleList::is_supported_property_index(u32 index) const
{
    // The object’s supported property indices are the numbers in the range zero to one less than the number of CSSRule objects represented by the collection.
    // If there are no such CSSRule objects, then there are no supported property indices.
    return index < m_rules.size();
}

}
