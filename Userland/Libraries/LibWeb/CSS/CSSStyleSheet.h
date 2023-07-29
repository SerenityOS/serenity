/*
 * Copyright (c) 2019-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibWeb/CSS/CSSRule.h>
#include <LibWeb/CSS/CSSRuleList.h>
#include <LibWeb/CSS/CSSStyleRule.h>
#include <LibWeb/CSS/StyleSheet.h>

namespace Web::CSS {

class CSSImportRule;

class CSSStyleSheet final
    : public StyleSheet
    , public Weakable<CSSStyleSheet> {
    WEB_PLATFORM_OBJECT(CSSStyleSheet, StyleSheet);

public:
    static WebIDL::ExceptionOr<JS::NonnullGCPtr<CSSStyleSheet>> create(JS::Realm&, CSSRuleList& rules, MediaList& media, Optional<AK::URL> location);

    virtual ~CSSStyleSheet() override = default;

    void set_owner_css_rule(CSSRule* rule) { m_owner_css_rule = rule; }

    virtual DeprecatedString type() const override { return "text/css"; }

    CSSRuleList const& rules() const { return *m_rules; }
    CSSRuleList& rules() { return *m_rules; }
    void set_rules(CSSRuleList& rules) { m_rules = &rules; }

    CSSRuleList* css_rules() { return m_rules; }
    CSSRuleList const* css_rules() const { return m_rules; }

    WebIDL::ExceptionOr<unsigned> insert_rule(StringView rule, unsigned index);
    WebIDL::ExceptionOr<void> remove_rule(unsigned index);
    WebIDL::ExceptionOr<void> delete_rule(unsigned index);

    void for_each_effective_style_rule(Function<void(CSSStyleRule const&)> const& callback) const;
    // Returns whether the match state of any media queries changed after evaluation.
    bool evaluate_media_queries(HTML::Window const&);
    void for_each_effective_keyframes_at_rule(Function<void(CSSKeyframesRule const&)> const& callback) const;

    void set_style_sheet_list(Badge<StyleSheetList>, StyleSheetList*);

    Optional<StringView> namespace_filter() const;

private:
    CSSStyleSheet(JS::Realm&, CSSRuleList&, MediaList&, Optional<AK::URL> location);

    virtual JS::ThrowCompletionOr<void> initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    JS::GCPtr<CSSRuleList> m_rules;

    JS::GCPtr<StyleSheetList> m_style_sheet_list;
    JS::GCPtr<CSSRule> m_owner_css_rule;
};

}
