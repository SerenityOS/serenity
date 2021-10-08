/*
 * Copyright (c) 2021, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/CSS/CSSSupportsRule.h>
#include <LibWeb/CSS/Parser/Parser.h>

namespace Web::CSS {

CSSSupportsRule::CSSSupportsRule(NonnullRefPtr<Supports>&& supports, NonnullRefPtrVector<CSSRule>&& rules)
    : CSSConditionRule(move(rules))
    , m_supports(move(supports))
{
}

CSSSupportsRule::~CSSSupportsRule()
{
}

String CSSSupportsRule::condition_text() const
{
    // FIXME: Serializing supports rules!
    return "<supports-condition>";
}

void CSSSupportsRule::set_condition_text(String text)
{
    if (auto new_supports = parse_css_supports({}, text))
        m_supports = new_supports.release_nonnull();
}

}
