/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/CSSConditionRulePrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/CSS/CSSConditionRule.h>

namespace Web::CSS {

CSSConditionRule::CSSConditionRule(JS::Realm& realm, CSSRuleList& rules)
    : CSSGroupingRule(realm, rules)
{
}

void CSSConditionRule::for_each_effective_style_rule(Function<void(CSSStyleRule const&)> const& callback) const
{
    if (condition_matches())
        CSSGroupingRule::for_each_effective_style_rule(callback);
}

JS::ThrowCompletionOr<void> CSSConditionRule::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::CSSConditionRulePrototype>(realm, "CSSConditionRule"));

    return {};
}

}
