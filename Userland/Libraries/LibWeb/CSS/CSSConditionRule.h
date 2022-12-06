/*
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullRefPtr.h>
#include <LibWeb/CSS/CSSGroupingRule.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

class CSSConditionRule : public CSSGroupingRule {
    WEB_PLATFORM_OBJECT(CSSConditionRule, CSSGroupingRule);

public:
    virtual ~CSSConditionRule() = default;

    virtual DeprecatedString condition_text() const = 0;
    virtual void set_condition_text(DeprecatedString) = 0;
    virtual bool condition_matches() const = 0;

    virtual void for_each_effective_style_rule(Function<void(CSSStyleRule const&)> const& callback) const override;

protected:
    CSSConditionRule(JS::Realm&, CSSRuleList&);
};

}
