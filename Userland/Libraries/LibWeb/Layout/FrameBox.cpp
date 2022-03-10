/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <LibGfx/Painter.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/Layout/FrameBox.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Painting/NestedBrowsingContextPaintable.h>

namespace Web::Layout {

FrameBox::FrameBox(DOM::Document& document, DOM::Element& element, NonnullRefPtr<CSS::StyleProperties> style)
    : ReplacedBox(document, element, move(style))
{
}

FrameBox::~FrameBox()
{
}

void FrameBox::prepare_for_replaced_layout()
{
    VERIFY(dom_node().nested_browsing_context());

    // FIXME: Do proper error checking, etc.
    set_intrinsic_width(dom_node().attribute(HTML::AttributeNames::width).to_int().value_or(300));
    set_intrinsic_height(dom_node().attribute(HTML::AttributeNames::height).to_int().value_or(150));
}

void FrameBox::did_set_rect()
{
    ReplacedBox::did_set_rect();

    VERIFY(dom_node().nested_browsing_context());
    dom_node().nested_browsing_context()->set_size(m_paint_box->content_size().to_type<int>());
}

OwnPtr<Painting::Paintable> FrameBox::create_paintable() const
{
    return Painting::NestedBrowsingContextPaintable::create(*this);
}

}
