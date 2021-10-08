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

void CSSConditionRule::for_each_effective_style_rule(Function<void(CSSStyleRule const&)> const& callback) const
{
    if (condition_matches())
        CSSGroupingRule::for_each_effective_style_rule(callback);
}

bool CSSConditionRule::for_first_not_loaded_import_rule(Function<void(CSSImportRule&)> const& callback)
{
    if (condition_matches())
        return CSSGroupingRule::for_first_not_loaded_import_rule(callback);

    return false;
}

}
