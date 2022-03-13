/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Font.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Layout/ButtonBox.h>
#include <LibWeb/Layout/Label.h>
#include <LibWeb/Painting/ButtonPaintable.h>

namespace Web::Layout {

ButtonBox::ButtonBox(DOM::Document& document, HTML::HTMLInputElement& element, NonnullRefPtr<CSS::StyleProperties> style)
    : LabelableNode(document, element, move(style))
{
}

ButtonBox::~ButtonBox()
{
}

void ButtonBox::prepare_for_replaced_layout()
{
    set_intrinsic_width(font().width(dom_node().value()));
    set_intrinsic_height(font().glyph_height());
}

RefPtr<Painting::Paintable> ButtonBox::create_paintable() const
{
    return Painting::ButtonPaintable::create(*this);
}

}
