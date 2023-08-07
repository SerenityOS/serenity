/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/SVG/SVGAnimatedLength.h>

namespace Web::SVG {

WebIDL::ExceptionOr<JS::NonnullGCPtr<SVGAnimatedLength>> SVGAnimatedLength::create(JS::Realm& realm, JS::NonnullGCPtr<SVGLength> base_val, JS::NonnullGCPtr<SVGLength> anim_val)
{
    return MUST_OR_THROW_OOM(realm.heap().allocate<SVGAnimatedLength>(realm, realm, move(base_val), move(anim_val)));
}

SVGAnimatedLength::SVGAnimatedLength(JS::Realm& realm, JS::NonnullGCPtr<SVGLength> base_val, JS::NonnullGCPtr<SVGLength> anim_val)
    : PlatformObject(realm)
    , m_base_val(move(base_val))
    , m_anim_val(move(anim_val))
{
    // The object referenced by animVal will always be distinct from the one referenced by baseVal, even when the attribute is not animated.
    VERIFY(m_base_val.ptr() != m_anim_val.ptr());
}

SVGAnimatedLength::~SVGAnimatedLength() = default;

void SVGAnimatedLength::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::SVGAnimatedLengthPrototype>(realm, "SVGAnimatedLength"));
}

void SVGAnimatedLength::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_base_val.ptr());
    visitor.visit(m_anim_val.ptr());
}

}
