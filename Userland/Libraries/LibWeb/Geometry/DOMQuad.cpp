/*
 * Copyright (c) 2023, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/Geometry/DOMQuad.h>

namespace Web::Geometry {

JS::NonnullGCPtr<DOMQuad> DOMQuad::construct_impl(JS::Realm& realm, DOMPointInit const& p1, DOMPointInit const& p2, DOMPointInit const& p3, DOMPointInit const& p4)
{
    return realm.heap().allocate<DOMQuad>(realm, realm, p1, p2, p3, p4);
}

DOMQuad::DOMQuad(JS::Realm& realm, DOMPointInit const& p1, DOMPointInit const& p2, DOMPointInit const& p3, DOMPointInit const& p4)
    : PlatformObject(realm)
    , m_p1(DOMPoint::from_point(realm.vm(), p1))
    , m_p2(DOMPoint::from_point(realm.vm(), p2))
    , m_p3(DOMPoint::from_point(realm.vm(), p3))
    , m_p4(DOMPoint::from_point(realm.vm(), p4))
{
}

DOMQuad::~DOMQuad() = default;

// https://drafts.fxtf.org/geometry/#dom-domquad-fromrect
JS::NonnullGCPtr<DOMQuad> DOMQuad::from_rect(JS::VM& vm, DOMRectInit const& other)
{
    // The fromRect(other) static method on DOMQuad must create a DOMQuad from the DOMRectInit dictionary other.
    return construct_impl(*vm.current_realm(), { other.x, other.y },
        { other.x + other.width, other.y },
        { other.x + other.width, other.y + other.height },
        { other.x, other.y + other.height });
}

// https://drafts.fxtf.org/geometry/#dom-domquad-fromquad
JS::NonnullGCPtr<DOMQuad> DOMQuad::from_quad(JS::VM& vm, DOMQuadInit const& other)
{
    // The fromQuad(other) static method on DOMQuad must create a DOMQuad from the DOMQuadInit dictionary other.
    return construct_impl(*vm.current_realm(), other.p1, other.p2, other.p3, other.p4);
}

// https://drafts.fxtf.org/geometry/#dom-domquad-getbounds
JS::NonnullGCPtr<DOMRect> DOMQuad::get_bounds() const
{
    // 1. Let bounds be a DOMRect object.
    auto bounds = DOMRect::create(realm(), {});

    // 2. Let left be the NaN-safe minimum of point 1’s x coordinate, point 2’s x coordinate, point 3’s x coordinate and point 4’s x coordinate.
    auto left = min(m_p1->x(), min(m_p2->x(), min(m_p3->x(), m_p4->x())));

    // 3. Let top be the NaN-safe minimum of point 1’s y coordinate, point 2’s y coordinate, point 3’s y coordinate and point 4’s y coordinate.
    auto top = min(m_p1->y(), min(m_p2->y(), min(m_p3->y(), m_p4->y())));

    // 4. Let right be the NaN-safe maximum of point 1’s x coordinate, point 2’s x coordinate, point 3’s x coordinate and point 4’s x coordinate.
    auto right = max(m_p1->x(), max(m_p2->x(), max(m_p3->x(), m_p4->x())));

    // 5. Let bottom be the NaN-safe maximum of point 1’s y coordinate, point 2’s y coordinate, point 3’s y coordinate and point 4’s y coordinate.
    auto bottom = max(m_p1->y(), max(m_p2->y(), max(m_p3->y(), m_p4->y())));

    // 6. Set x coordinate of bounds to left, y coordinate of bounds to top, width dimension of bounds to right - left and height dimension of bounds to bottom - top.
    bounds->set_x(left);
    bounds->set_y(top);
    bounds->set_width(right - left);
    bounds->set_height(bottom - top);

    // 7. Return bounds.
    return bounds;
}

void DOMQuad::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::DOMQuadPrototype>(realm, "DOMQuad"));
}

void DOMQuad::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_p1.ptr());
    visitor.visit(m_p2.ptr());
    visitor.visit(m_p3.ptr());
    visitor.visit(m_p4.ptr());
}

}
