/*
 * Copyright (c) 2022, Tim Flynn <trflynn89@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/SVGAnimatedLengthPrototype.h>
#include <LibWeb/HTML/Window.h>
#include <LibWeb/SVG/SVGAnimatedLength.h>

namespace Web::SVG {

JS::NonnullGCPtr<SVGAnimatedLength> SVGAnimatedLength::create(HTML::Window& window, JS::NonnullGCPtr<SVGLength> base_val, JS::NonnullGCPtr<SVGLength> anim_val)
{
    return *window.heap().allocate<SVGAnimatedLength>(window.realm(), window, move(base_val), move(anim_val));
}

SVGAnimatedLength::SVGAnimatedLength(HTML::Window& window, JS::NonnullGCPtr<SVGLength> base_val, JS::NonnullGCPtr<SVGLength> anim_val)
    : PlatformObject(window.realm())
    , m_base_val(move(base_val))
    , m_anim_val(move(anim_val))
{
    set_prototype(&window.ensure_web_prototype<Bindings::SVGAnimatedLengthPrototype>("SVGAnimatedLength"));

    // The object referenced by animVal will always be distinct from the one referenced by baseVal, even when the attribute is not animated.
    VERIFY(m_base_val.ptr() != m_anim_val.ptr());
}

SVGAnimatedLength::~SVGAnimatedLength() = default;

void SVGAnimatedLength::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_base_val.ptr());
    visitor.visit(m_anim_val.ptr());
}

}
