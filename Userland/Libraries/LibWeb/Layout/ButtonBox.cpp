/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Font/Font.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Layout/ButtonBox.h>
#include <LibWeb/Painting/ButtonPaintable.h>

namespace Web::Layout {

ButtonBox::ButtonBox(DOM::Document& document, HTML::HTMLInputElement& element, NonnullRefPtr<CSS::StyleProperties> style)
    : FormAssociatedLabelableNode(document, element, move(style))
{
}

ButtonBox::~ButtonBox() = default;

void ButtonBox::prepare_for_replaced_layout()
{
    // For <input type="submit" /> and <input type="button" />, the contents of
    // the button does not appear as the contents of the element but as the
    // value attribute. This is not the case with <button />, which contains
    // its contents normally.
    if (is<HTML::HTMLInputElement>(dom_node())) {
        set_intrinsic_width(font().width(static_cast<HTML::HTMLInputElement&>(dom_node()).value()));
        set_intrinsic_height(font().glyph_height());
    }
}

RefPtr<Painting::Paintable> ButtonBox::create_paintable() const
{
    return Painting::ButtonPaintable::create(*this);
}

}
