/*
 * Copyright (c) 2019-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/CSSStyleSheet.h>
#include <LibWeb/DOM/ExceptionOr.h>

namespace Web::CSS {

CSSStyleSheet::CSSStyleSheet(NonnullRefPtrVector<CSSRule> rules)
    : m_rules(CSSRuleList::create(move(rules)))
{
}

CSSStyleSheet::~CSSStyleSheet()
{
}

// https://drafts.csswg.org/cssom/#dom-cssstylesheet-insertrule
DOM::ExceptionOr<unsigned> CSSStyleSheet::insert_rule(StringView rule, unsigned index)
{
    // FIXME: 1. If the origin-clean flag is unset, throw a SecurityError exception.

    // FIXME: 2. If the disallow modification flag is set, throw a NotAllowedError DOMException.

    // Let parsed rule be the return value of invoking parse a rule with rule.

    // If parsed rule is a syntax error, return parsed rule.

    // If parsed rule is an @import rule, and the constructed flag is set, throw a SyntaxError DOMException.

    // Return the result of invoking insert a CSS rule rule in the CSS rules at index.

    (void)index;
    (void)rule;
    TODO();
}

// https://drafts.csswg.org/cssom/#dom-cssstylesheet-deleterule
DOM::ExceptionOr<void> CSSStyleSheet::delete_rule(unsigned index)
{
    // FIXME: 1. If the origin-clean flag is unset, throw a SecurityError exception.

    // FIXME: 2. If the disallow modification flag is set, throw a NotAllowedError DOMException.

    // 3. Remove a CSS rule in the CSS rules at index.
    return m_rules->remove_a_css_rule(index);
}

// https://drafts.csswg.org/cssom/#dom-cssstylesheet-removerule
DOM::ExceptionOr<void> CSSStyleSheet::remove_rule(unsigned index)
{
    // The removeRule(index) method must run the same steps as deleteRule().
    return delete_rule(index);
}

}
