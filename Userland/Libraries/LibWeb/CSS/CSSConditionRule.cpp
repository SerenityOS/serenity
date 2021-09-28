/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/CSSConditionRule.h>

namespace Web::CSS {

CSSConditionRule::CSSConditionRule(NonnullRefPtrVector<CSSRule>&& rules)
    : CSSGroupingRule(move(rules))
{
}

CSSConditionRule::~CSSConditionRule()
{
}

}
