/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/Layout/FrameBox.h>
#include <LibWeb/Layout/Viewport.h>
#include <LibWeb/Painting/NestedBrowsingContextPaintable.h>

namespace Web::Layout {

FrameBox::FrameBox(DOM::Document& document, DOM::Element& element, NonnullRefPtr<CSS::StyleProperties> style)
    : ReplacedBox(document, element, move(style))
{
}

FrameBox::~FrameBox() = default;

void FrameBox::prepare_for_replaced_layout()
{
    VERIFY(dom_node().nested_browsing_context());

    // FIXME: Do proper error checking, etc.
    set_natural_width(dom_node().deprecated_attribute(HTML::AttributeNames::width).to_int().value_or(300));
    set_natural_height(dom_node().deprecated_attribute(HTML::AttributeNames::height).to_int().value_or(150));
}

void FrameBox::did_set_content_size()
{
    ReplacedBox::did_set_content_size();

    VERIFY(dom_node().content_navigable());
    dom_node().content_navigable()->set_size(paintable_box()->content_size());
}

JS::GCPtr<Painting::Paintable> FrameBox::create_paintable() const
{
    return Painting::NestedBrowsingContextPaintable::create(*this);
}

}
