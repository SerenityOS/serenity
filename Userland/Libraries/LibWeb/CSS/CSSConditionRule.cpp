/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/CSSConditionRulePrototype.h>
#include <LibWeb/CSS/CSSConditionRule.h>
#include <LibWeb/HTML/Window.h>

namespace Web::CSS {

CSSConditionRule::CSSConditionRule(HTML::Window& window_object, CSSRuleList& rules)
    : CSSGroupingRule(window_object, rules)
{
    set_prototype(&window_object.ensure_web_prototype<Bindings::CSSConditionRulePrototype>("CSSConditionRule"));
}

void CSSConditionRule::for_each_effective_style_rule(Function<void(CSSStyleRule const&)> const& callback) const
{
    if (condition_matches())
        CSSGroupingRule::for_each_effective_style_rule(callback);
}

}
