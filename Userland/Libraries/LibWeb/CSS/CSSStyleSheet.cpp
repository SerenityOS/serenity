/*
 * Copyright (c) 2019-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/CSSStyleSheetPrototype.h>
#include <LibWeb/CSS/CSSStyleSheet.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/CSS/StyleSheetList.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/DOM/ExceptionOr.h>

namespace Web::CSS {

CSSStyleSheet* CSSStyleSheet::create(Bindings::WindowObject& window_object, NonnullRefPtrVector<CSSRule> rules, Optional<AK::URL> location)
{
    return window_object.heap().allocate<CSSStyleSheet>(window_object.realm(), window_object, move(rules), move(location));
}

CSSStyleSheet::CSSStyleSheet(Bindings::WindowObject& window_object, NonnullRefPtrVector<CSSRule> rules, Optional<AK::URL> location)
    : StyleSheet(window_object)
    , m_rules(CSSRuleList::create(move(rules)))
{
    set_prototype(&window_object.ensure_web_prototype<Bindings::CSSStyleSheetPrototype>("CSSStyleSheet"));

    if (location.has_value())
        set_location(location->to_string());

    for (auto& rule : *m_rules)
        rule.set_parent_style_sheet(this);
}

void CSSStyleSheet::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_style_sheet_list.ptr());
}

// https://www.w3.org/TR/cssom/#dom-cssstylesheet-insertrule
DOM::ExceptionOr<unsigned> CSSStyleSheet::insert_rule(StringView rule, unsigned index)
{
    // FIXME: 1. If the origin-clean flag is unset, throw a SecurityError exception.

    // FIXME: 2. If the disallow modification flag is set, throw a NotAllowedError DOMException.

    // 3. Let parsed rule be the return value of invoking parse a rule with rule.
    auto parsed_rule = parse_css_rule(CSS::Parser::ParsingContext {}, rule);

    // 4. If parsed rule is a syntax error, return parsed rule.
    if (!parsed_rule)
        return DOM::SyntaxError::create("Unable to parse CSS rule.");

    // FIXME: 5. If parsed rule is an @import rule, and the constructed flag is set, throw a SyntaxError DOMException.

    // 6. Return the result of invoking insert a CSS rule rule in the CSS rules at index.
    auto parsed_rule_nonnull = parsed_rule.release_nonnull();
    auto result = m_rules->insert_a_css_rule(parsed_rule_nonnull, index);

    if (!result.is_exception()) {
        // NOTE: The spec doesn't say where to set the parent style sheet, so we'll do it here.
        parsed_rule_nonnull->set_parent_style_sheet(this);

        if (m_style_sheet_list) {
            m_style_sheet_list->document().style_computer().invalidate_rule_cache();
            m_style_sheet_list->document().invalidate_style();
        }
    }

    return result;
}

// https://www.w3.org/TR/cssom/#dom-cssstylesheet-deleterule
DOM::ExceptionOr<void> CSSStyleSheet::delete_rule(unsigned index)
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
DOM::ExceptionOr<void> CSSStyleSheet::remove_rule(unsigned index)
{
    // The removeRule(index) method must run the same steps as deleteRule().
    return delete_rule(index);
}

void CSSStyleSheet::for_each_effective_style_rule(Function<void(CSSStyleRule const&)> const& callback) const
{
    m_rules->for_each_effective_style_rule(callback);
}

bool CSSStyleSheet::evaluate_media_queries(HTML::Window const& window)
{
    return m_rules->evaluate_media_queries(window);
}

void CSSStyleSheet::set_style_sheet_list(Badge<StyleSheetList>, StyleSheetList* list)
{
    m_style_sheet_list = list;
}

}
