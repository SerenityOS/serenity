/*
 * Copyright (c) 2019-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/NonnullRefPtrVector.h>
#include <LibWeb/CSS/CSSRule.h>
#include <LibWeb/CSS/CSSRuleList.h>
#include <LibWeb/CSS/CSSStyleRule.h>
#include <LibWeb/CSS/StyleSheet.h>
#include <LibWeb/Loader/Resource.h>

namespace Web::CSS {

class CSSImportRule;

class CSSStyleSheet final
    : public StyleSheet
    , public Weakable<CSSStyleSheet> {
    JS_OBJECT(CSSStyleSheet, StyleSheet);

public:
    static CSSStyleSheet* create(Bindings::WindowObject&, NonnullRefPtrVector<CSSRule> rules, Optional<AK::URL> location);

    explicit CSSStyleSheet(Bindings::WindowObject&, NonnullRefPtrVector<CSSRule>, Optional<AK::URL> location);
    virtual ~CSSStyleSheet() override = default;

    CSSStyleSheet& impl() { return *this; }

    void set_owner_css_rule(CSSRule* rule) { m_owner_css_rule = rule; }

    virtual String type() const override { return "text/css"; }

    CSSRuleList const& rules() const { return m_rules; }
    CSSRuleList& rules() { return m_rules; }
    void set_rules(NonnullRefPtr<CSSRuleList> rules) { m_rules = move(rules); }

    CSSRuleList* css_rules() { return m_rules; }
    CSSRuleList const* css_rules() const { return m_rules; }

    DOM::ExceptionOr<unsigned> insert_rule(StringView rule, unsigned index);
    DOM::ExceptionOr<void> remove_rule(unsigned index);
    DOM::ExceptionOr<void> delete_rule(unsigned index);

    void for_each_effective_style_rule(Function<void(CSSStyleRule const&)> const& callback) const;
    // Returns whether the match state of any media queries changed after evaluation.
    bool evaluate_media_queries(HTML::Window const&);

    void set_style_sheet_list(Badge<StyleSheetList>, StyleSheetList*);

private:
    virtual void visit_edges(Cell::Visitor&) override;

    NonnullRefPtr<CSSRuleList> m_rules;

    WeakPtr<CSSRule> m_owner_css_rule;

    JS::GCPtr<StyleSheetList> m_style_sheet_list;
};

}

namespace Web::Bindings {
inline JS::Object* wrap(JS::Realm&, Web::CSS::CSSStyleSheet& object) { return &object; }
using CSSStyleSheetWrapper = Web::CSS::CSSStyleSheet;
}
