/*
 * Copyright (c) 2024, Matthew Olsson <mattco@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Animations/KeyframeEffect.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/CSS/CSSAnimation.h>
#include <LibWeb/DOM/Element.h>

namespace Web::CSS {

JS::NonnullGCPtr<CSSAnimation> CSSAnimation::create(JS::Realm& realm)
{
    return realm.heap().allocate<CSSAnimation>(realm, realm);
}

CSSAnimation::CSSAnimation(JS::Realm& realm)
    : Animations::Animation(realm)
{
}

void CSSAnimation::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::CSSAnimationPrototype>(realm, "CSSAnimation"_fly_string));
}

void CSSAnimation::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_owning_element);
}

}
