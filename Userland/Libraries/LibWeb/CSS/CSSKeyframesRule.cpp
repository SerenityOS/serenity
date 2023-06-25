/*
 * Copyright (c) 2023, Ali Mohammad Pur <mpfard@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "CSSKeyframesRule.h"
#include <LibWeb/Bindings/CSSKeyframesRulePrototype.h>
#include <LibWeb/Bindings/Intrinsics.h>

namespace Web::CSS {

void CSSKeyframesRule::visit_edges(Visitor& visitor)
{
    Base::visit_edges(visitor);
    for (auto& keyframe : m_keyframes)
        visitor.visit(keyframe);
}

JS::ThrowCompletionOr<void> CSSKeyframesRule::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::CSSKeyframesRulePrototype>(realm, "CSSKeyframesRule"));

    return {};
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
