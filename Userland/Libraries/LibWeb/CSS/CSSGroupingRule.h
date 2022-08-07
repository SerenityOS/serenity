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
    JS_OBJECT(CSSGroupingRule, CSSRule);

public:
    CSSGroupingRule& impl() { return *this; }

    virtual ~CSSGroupingRule() = default;

    CSSRuleList const& css_rules() const { return m_rules; }
    CSSRuleList& css_rules() { return m_rules; }
    CSSRuleList* css_rules_for_bindings() { return &m_rules; }
    DOM::ExceptionOr<u32> insert_rule(StringView rule, u32 index = 0);
    DOM::ExceptionOr<void> delete_rule(u32 index);

    virtual void for_each_effective_style_rule(Function<void(CSSStyleRule const&)> const& callback) const;

    virtual void set_parent_style_sheet(CSSStyleSheet*) override;

protected:
    explicit CSSGroupingRule(Bindings::WindowObject&, CSSRuleList&);

private:
    virtual void visit_edges(Cell::Visitor&) override;

    CSSRuleList& m_rules;
};

}

namespace Web::Bindings {
inline JS::Object* wrap(JS::Realm&, Web::CSS::CSSGroupingRule& object) { return &object; }
using CSSGroupingRuleWrapper = Web::CSS::CSSGroupingRule;
}
