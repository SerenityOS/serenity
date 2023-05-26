/*
 * Copyright (c) 2023, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CSSKeyframeRule.h"
#include <LibWeb/CSS/CSSRuleList.h>

namespace Web::CSS {

void CSSKeyframeRule::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_declarations);
}

JS::ThrowCompletionOr<void> CSSKeyframeRule::initialize(JS::Realm&)
{
    return {};
}

DeprecatedString CSSKeyframeRule::serialized() const
{
    StringBuilder builder;
    builder.appendff("{}% {{ {} }}", key().value(), style()->serialized());
    return builder.to_deprecated_string();
}

}
