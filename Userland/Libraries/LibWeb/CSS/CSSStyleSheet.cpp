/*
 * Copyright (c) 2019-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/CSSStyleSheet.h>
#include <LibWeb/CSS/Parser/Parser.h>
#include <LibWeb/DOM/ExceptionOr.h>

namespace Web::CSS {

CSSStyleSheet::CSSStyleSheet(NonnullRefPtrVector<CSSRule> rules)
    : m_rules(CSSRuleList::create(move(rules)))
{
}

CSSStyleSheet::~CSSStyleSheet()
{
}

// https://www.w3.org/TR/cssom/#dom-cssstylesheet-insertrule
DOM::ExceptionOr<unsigned> CSSStyleSheet::insert_rule(StringView rule, unsigned index)
{
    // FIXME: 1. If the origin-clean flag is unset, throw a SecurityError exception.

    // FIXME: 2. If the disallow modification flag is set, throw a NotAllowedError DOMException.

    // 3. Let parsed rule be the return value of invoking parse a rule with rule.
    auto parsed_rule = parse_css_rule(CSS::ParsingContext {}, rule);

    // 4. If parsed rule is a syntax error, return parsed rule.
    if (!parsed_rule)
        return DOM::SyntaxError::create("Unable to parse CSS rule.");

    // FIXME: 5. If parsed rule is an @import rule, and the constructed flag is set, throw a SyntaxError DOMException.

    // 6. Return the result of invoking insert a CSS rule rule in the CSS rules at index.
    return m_rules->insert_a_css_rule(parsed_rule.release_nonnull(), index);
}

// https://www.w3.org/TR/cssom/#dom-cssstylesheet-deleterule
DOM::ExceptionOr<void> CSSStyleSheet::delete_rule(unsigned index)
{
    // FIXME: 1. If the origin-clean flag is unset, throw a SecurityError exception.

    // FIXME: 2. If the disallow modification flag is set, throw a NotAllowedError DOMException.

    // 3. Remove a CSS rule in the CSS rules at index.
    return m_rules->remove_a_css_rule(index);
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

void CSSStyleSheet::evaluate_media_queries(DOM::Window const& window)
{
    m_rules->evaluate_media_queries(window);
}

}
