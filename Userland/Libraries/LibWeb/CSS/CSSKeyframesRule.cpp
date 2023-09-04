/*
 * Copyright (c) 2023, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CSSKeyframesRule.h"
#include <LibWeb/Bindings/CSSKeyframesRulePrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>

namespace Web::CSS {

JS::NonnullGCPtr<CSSKeyframesRule> CSSKeyframesRule::create(JS::Realm& realm, FlyString name, JS::MarkedVector<JS::NonnullGCPtr<CSSKeyframeRule>> keyframes)
{
    return realm.heap().allocate<CSSKeyframesRule>(realm, realm, move(name), move(keyframes));
}

void CSSKeyframesRule::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    for (auto& keyframe : m_keyframes)
        visitor.visit(keyframe);
}

void CSSKeyframesRule::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::CSSKeyframesRulePrototype>(realm, "CSSKeyframesRule"));
}

DeprecatedString CSSKeyframesRule::serialized() const
{
    StringBuilder builder;
    builder.appendff("@keyframes \"{}\"", name());
    builder.append(" { "sv);
    for (auto& keyframe : keyframes()) {
        builder.append(keyframe->css_text());
        builder.append(' ');
    }
    builder.append('}');
    return builder.to_deprecated_string();
}

}
