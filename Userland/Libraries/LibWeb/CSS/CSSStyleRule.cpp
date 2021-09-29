/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/CSSStyleRule.h>

namespace Web::CSS {

CSSStyleRule::CSSStyleRule(NonnullRefPtrVector<Selector>&& selectors, NonnullRefPtr<CSSStyleDeclaration>&& declaration)
    : m_selectors(move(selectors))
    , m_declaration(move(declaration))
{
}

CSSStyleRule::~CSSStyleRule()
{
}

// https://drafts.csswg.org/cssom/#dom-cssstylerule-selectortext
String CSSStyleRule::selector_text() const
{
    TODO();
}

// https://drafts.csswg.org/cssom/#dom-cssstylerule-selectortext
void CSSStyleRule::set_selector_text(StringView selector_text)
{
    // FIXME: 1. Run the parse a group of selectors algorithm on the given value.

    // FIXME: 2. If the algorithm returns a non-null value replace the associated group of selectors with the returned value.

    // FIXME: 3. Otherwise, if the algorithm returns a null value, do nothing.

    (void)selector_text;
    TODO();
}

// https://drafts.csswg.org/cssom/#dom-cssstylerule-style
CSSStyleDeclaration* CSSStyleRule::style()
{
    return m_declaration;
}

}
