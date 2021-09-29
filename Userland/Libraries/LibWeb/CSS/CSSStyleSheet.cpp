/*
 * Copyright (c) 2019-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/CSSStyleSheet.h>

namespace Web::CSS {

CSSStyleSheet::CSSStyleSheet(NonnullRefPtrVector<CSSRule> rules)
    : m_rules(CSSRuleList::create(move(rules)))
{
}

CSSStyleSheet::~CSSStyleSheet()
{
}

}
