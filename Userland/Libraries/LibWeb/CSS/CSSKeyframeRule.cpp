/*
 * Copyright (c) 2023, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CSSKeyframeRule.h"
#include <LibWeb/Bindings/CSSKeyframeRulePrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/CSS/CSSRuleList.h>

namespace Web::CSS {

WebIDL::ExceptionOr<JS::NonnullGCPtr<CSSKeyframeRule>> CSSKeyframeRule::create(JS::Realm& realm, CSS::Percentage key, Web::CSS::CSSStyleDeclaration& declarations)
{
    return MUST_OR_THROW_OOM(realm.heap().allocate<CSSKeyframeRule>(realm, realm, key, declarations));
}

void CSSKeyframeRule::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_declarations);
}

void CSSKeyframeRule::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::CSSKeyframeRulePrototype>(realm, "CSSKeyframeRule"));
}

DeprecatedString CSSKeyframeRule::serialized() const
{
    StringBuilder builder;
    builder.appendff("{}% {{ {} }}", key().value(), style()->serialized());
    return builder.to_deprecated_string();
}

}
