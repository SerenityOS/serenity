/*
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/NonnullRefPtr.h>
#include <LibWeb/CSS/CSSRule.h>
#include <LibWeb/CSS/CSSRuleList.h>
#include <LibWeb/Forward.h>

namespace Web::CSS {

class CSSGroupingRule : public CSSRule {
    AK_MAKE_NONCOPYABLE(CSSGroupingRule);
    AK_MAKE_NONMOVABLE(CSSGroupingRule);

public:
    using WrapperType = Bindings::CSSGroupingRuleWrapper;

    virtual ~CSSGroupingRule() = default;

    CSSRuleList const& css_rules() const { return *m_rules; }
    CSSRuleList& css_rules() { return *m_rules; }
    CSSRuleList* css_rules_for_bindings() { return m_rules.cell(); }
    DOM::ExceptionOr<u32> insert_rule(StringView rule, u32 index = 0);
    DOM::ExceptionOr<void> delete_rule(u32 index);

    virtual void for_each_effective_style_rule(Function<void(CSSStyleRule const&)> const& callback) const;

    virtual void set_parent_style_sheet(CSSStyleSheet*) override;

protected:
    explicit CSSGroupingRule(NonnullRefPtrVector<CSSRule>&&);

private:
    JS::Handle<CSSRuleList> m_rules;
};

}
