/*
 * Copyright (c) 2019-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/CSSStyleSheetPrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/CSS/CSSStyleSheet.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/StyleComputer.h>
#include <LibWeb/CSS/StyleSheetList.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::CSS {

JS::NonnullGCPtr<CSSStyleSheet> CSSStyleSheet::create(JS::Realm& realm, CSSRuleList& rules, MediaList& media, Optional<AK::URL> location)
{
    return realm.heap().allocate<CSSStyleSheet>(realm, realm, rules, media, move(location));
}

CSSStyleSheet::CSSStyleSheet(JS::Realm& realm, CSSRuleList& rules, MediaList& media, Optional<AK::URL> location)
    : StyleSheet(realm, media)
    , m_rules(&rules)
{
    if (location.has_value())
        set_location(MUST(location->to_string()));

    for (auto& rule : *m_rules)
        rule->set_parent_style_sheet(this);

    recalculate_namespaces();

    m_rules->on_change = [this]() {
        recalculate_namespaces();
    };
}

void CSSStyleSheet::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::CSSStyleSheetPrototype>(realm, "CSSStyleSheet"));
}

void CSSStyleSheet::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_style_sheet_list.ptr());
    visitor.visit(m_rules);
    visitor.visit(m_owner_css_rule);
    visitor.visit(m_default_namespace_rule);
    for (auto& [key, namespace_rule] : m_namespace_rules)
        visitor.visit(namespace_rule);
}

// https://www.w3.org/TR/cssom/#dom-cssstylesheet-insertrule
WebIDL::ExceptionOr<unsigned> CSSStyleSheet::insert_rule(StringView rule, unsigned index)
{
    // FIXME: 1. If the origin-clean flag is unset, throw a SecurityError exception.

    // FIXME: 2. If the disallow modification flag is set, throw a NotAllowedError DOMException.

    // 3. Let parsed rule be the return value of invoking parse a rule with rule.
    auto context = m_style_sheet_list ? CSS::Parser::ParsingContext { m_style_sheet_list->document() } : CSS::Parser::ParsingContext { realm() };
    auto parsed_rule = parse_css_rule(context, rule);

    // 4. If parsed rule is a syntax error, return parsed rule.
    if (!parsed_rule)
        return WebIDL::SyntaxError::create(realm(), "Unable to parse CSS rule.");

    // FIXME: 5. If parsed rule is an @import rule, and the constructed flag is set, throw a SyntaxError DOMException.

    // 6. Return the result of invoking insert a CSS rule rule in the CSS rules at index.
    auto result = m_rules->insert_a_css_rule(parsed_rule, index);

    if (!result.is_exception()) {
        // NOTE: The spec doesn't say where to set the parent style sheet, so we'll do it here.
        parsed_rule->set_parent_style_sheet(this);

        if (m_style_sheet_list) {
            m_style_sheet_list->document().style_computer().invalidate_rule_cache();
            m_style_sheet_list->document().invalidate_style();
        }
    }

    return result;
}

// https://www.w3.org/TR/cssom/#dom-cssstylesheet-deleterule
WebIDL::ExceptionOr<void> CSSStyleSheet::delete_rule(unsigned index)
{
    // FIXME: 1. If the origin-clean flag is unset, throw a SecurityError exception.

    // FIXME: 2. If the disallow modification flag is set, throw a NotAllowedError DOMException.

    // 3. Remove a CSS rule in the CSS rules at index.
    auto result = m_rules->remove_a_css_rule(index);
    if (!result.is_exception()) {
        if (m_style_sheet_list) {
            m_style_sheet_list->document().style_computer().invalidate_rule_cache();
            m_style_sheet_list->document().invalidate_style();
        }
    }
    return result;
}

// https://www.w3.org/TR/cssom/#dom-cssstylesheet-removerule
WebIDL::ExceptionOr<void> CSSStyleSheet::remove_rule(unsigned index)
{
    // The removeRule(index) method must run the same steps as deleteRule().
    return delete_rule(index);
}

void CSSStyleSheet::for_each_effective_style_rule(Function<void(CSSStyleRule const&)> const& callback) const
{
    if (m_media->matches()) {
        m_rules->for_each_effective_style_rule(callback);
    }
}

void CSSStyleSheet::for_each_effective_keyframes_at_rule(Function<void(CSSKeyframesRule const&)> const& callback) const
{
    if (m_media->matches())
        m_rules->for_each_effective_keyframes_at_rule(callback);
}

bool CSSStyleSheet::evaluate_media_queries(HTML::Window const& window)
{
    bool any_media_queries_changed_match_state = false;

    bool did_match = m_media->matches();
    bool now_matches = m_media->evaluate(window);
    if (did_match != now_matches)
        any_media_queries_changed_match_state = true;
    if (now_matches && m_rules->evaluate_media_queries(window))
        any_media_queries_changed_match_state = true;

    return any_media_queries_changed_match_state;
}

void CSSStyleSheet::set_style_sheet_list(Badge<StyleSheetList>, StyleSheetList* list)
{
    m_style_sheet_list = list;
}

Optional<StringView> CSSStyleSheet::default_namespace() const
{
    if (m_default_namespace_rule)
        return m_default_namespace_rule->namespace_uri().view();

    return {};
}

Optional<StringView> CSSStyleSheet::namespace_uri(StringView namespace_prefix) const
{
    return m_namespace_rules.get(namespace_prefix)
        .map([](JS::GCPtr<CSSNamespaceRule> namespace_) {
            return namespace_->namespace_uri().view();
        });
}

void CSSStyleSheet::recalculate_namespaces()
{
    m_default_namespace_rule = nullptr;
    m_namespace_rules.clear();

    for (JS::NonnullGCPtr<CSSRule> rule : *m_rules) {
        // "Any @namespace rules must follow all @charset and @import rules and precede all other
        // non-ignored at-rules and style rules in a style sheet.
        // ...
        // A syntactically invalid @namespace rule (whether malformed or misplaced) must be ignored."
        // https://drafts.csswg.org/css-namespaces/#syntax
        switch (rule->type()) {
        case CSSRule::Type::Import:
            continue;

        case CSSRule::Type::Namespace:
            break;

        default:
            // Any other types mean that further @namespace rules are invalid, so we can stop here.
            return;
        }

        auto& namespace_rule = verify_cast<CSSNamespaceRule>(*rule);
        if (!namespace_rule.namespace_uri().is_empty() && namespace_rule.prefix().is_empty())
            m_default_namespace_rule = namespace_rule;

        m_namespace_rules.set(FlyString::from_deprecated_fly_string(namespace_rule.prefix()).release_value_but_fixme_should_propagate_errors(), namespace_rule);
    }
}

}
