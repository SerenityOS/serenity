/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/CSSRuleList.h>

namespace Web::CSS {

CSSRuleList::CSSRuleList(NonnullRefPtrVector<CSSRule>&& rules)
    : m_rules(rules)
{
}

CSSRuleList::~CSSRuleList()
{
}

}
