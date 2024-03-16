/*
 * Copyright (c) 2023, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CSSKeyframesRule.h"
#include <LibWeb/Bindings/CSSKeyframesRulePrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>

namespace Web::CSS {

JS_DEFINE_ALLOCATOR(CSSKeyframesRule);

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
    WEB_SET_PROTOTYPE_FOR_INTERFACE(CSSKeyframesRule);
}

String CSSKeyframesRule::serialized() const
{
    StringBuilder builder;
    builder.appendff("@keyframes \"{}\"", name());
    builder.append(" { "sv);
    for (auto const& keyframe : keyframes()) {
        builder.append(keyframe->css_text());
        builder.append(' ');
    }
    builder.append('}');
    return MUST(builder.to_string());
}

}
