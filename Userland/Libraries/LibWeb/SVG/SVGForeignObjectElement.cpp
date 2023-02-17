/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/Parser/HTMLParser.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/SVG/AttributeNames.h>
#include <LibWeb/SVG/SVGAnimatedLength.h>
#include <LibWeb/SVG/SVGForeignObjectElement.h>
#include <LibWeb/SVG/SVGLength.h>

namespace Web::SVG {

SVGForeignObjectElement::SVGForeignObjectElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : SVGGraphicsElement(document, move(qualified_name))
{
}

SVGForeignObjectElement::~SVGForeignObjectElement() = default;

JS::ThrowCompletionOr<void> SVGForeignObjectElement::initialize(JS::Realm& realm)
{
    auto& vm = realm.vm();

    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::SVGForeignObjectElementPrototype>(realm, "SVGForeignObjectElement"));

    // FIXME: These never actually get updated!
    m_x = TRY(Bindings::throw_dom_exception_if_needed(vm, [&]() {
        return SVGAnimatedLength::create(realm, SVGLength::create(realm, 0, 0), SVGLength::create(realm, 0, 0));
    }));
    m_y = TRY(Bindings::throw_dom_exception_if_needed(vm, [&]() {
        return SVGAnimatedLength::create(realm, SVGLength::create(realm, 0, 0), SVGLength::create(realm, 0, 0));
    }));
    m_width = TRY(Bindings::throw_dom_exception_if_needed(vm, [&]() {
        return SVGAnimatedLength::create(realm, SVGLength::create(realm, 0, 0), SVGLength::create(realm, 0, 0));
    }));
    m_height = TRY(Bindings::throw_dom_exception_if_needed(vm, [&]() {
        return SVGAnimatedLength::create(realm, SVGLength::create(realm, 0, 0), SVGLength::create(realm, 0, 0));
    }));

    return {};
}

void SVGForeignObjectElement::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_x);
    visitor.visit(m_y);
    visitor.visit(m_width);
    visitor.visit(m_height);
}

JS::GCPtr<Layout::Node> SVGForeignObjectElement::create_layout_node(NonnullRefPtr<CSS::StyleProperties> style)
{
    return heap().allocate_without_realm<Layout::BlockContainer>(document(), this, move(style));
}

void SVGForeignObjectElement::apply_presentational_hints(CSS::StyleProperties& style) const
{
    Base::apply_presentational_hints(style);

    if (auto width_value = HTML::parse_dimension_value(attribute(SVG::AttributeNames::width)))
        style.set_property(CSS::PropertyID::Width, width_value.release_nonnull());

    if (auto height_value = HTML::parse_dimension_value(attribute(SVG::AttributeNames::height)))
        style.set_property(CSS::PropertyID::Height, height_value.release_nonnull());
}

JS::NonnullGCPtr<SVG::SVGAnimatedLength> SVGForeignObjectElement::x()
{
    return *m_x;
}

JS::NonnullGCPtr<SVG::SVGAnimatedLength> SVGForeignObjectElement::y()
{
    return *m_y;
}

JS::NonnullGCPtr<SVG::SVGAnimatedLength> SVGForeignObjectElement::width()
{
    return *m_width;
}

JS::NonnullGCPtr<SVG::SVGAnimatedLength> SVGForeignObjectElement::height()
{
    return *m_height;
}

}
