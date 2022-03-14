/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Painter.h>
#include <LibWeb/Layout/CanvasBox.h>
#include <LibWeb/Painting/CanvasPaintable.h>

namespace Web::Layout {

CanvasBox::CanvasBox(DOM::Document& document, HTML::HTMLCanvasElement& element, NonnullRefPtr<CSS::StyleProperties> style)
    : ReplacedBox(document, element, move(style))
{
}

CanvasBox::~CanvasBox() = default;

void CanvasBox::prepare_for_replaced_layout()
{
    set_intrinsic_width(dom_node().width());
    set_intrinsic_height(dom_node().height());
}

RefPtr<Painting::Paintable> CanvasBox::create_paintable() const
{
    return Painting::CanvasPaintable::create(*this);
}

}
