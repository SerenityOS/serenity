/*
 * Copyright (c) 2020, Matthew Olsson <mattco@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Bindings/ExceptionOrUtils.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/DOMStringMap.h>
#include <LibWeb/SVG/SVGElement.h>

namespace Web::SVG {

SVGElement::SVGElement(DOM::Document& document, DOM::QualifiedName qualified_name)
    : Element(document, move(qualified_name))
{
}

JS::ThrowCompletionOr<void> SVGElement::initialize(JS::Realm& realm)
{
    MUST_OR_THROW_OOM(Base::initialize(realm));
    set_prototype(&Bindings::ensure_web_prototype<Bindings::SVGElementPrototype>(realm, "SVGElement"));

    m_dataset = TRY(Bindings::throw_dom_exception_if_needed(realm.vm(), [&]() {
        return HTML::DOMStringMap::create(*this);
    }));

    return {};
}

void SVGElement::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_dataset);
}

}
