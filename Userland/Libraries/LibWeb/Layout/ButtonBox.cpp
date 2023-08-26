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
        set_natural_width(CSSPixels(font().width(static_cast<HTML::HTMLInputElement&>(dom_node()).value())));
        set_natural_height(font().pixel_size_rounded_up());
    }
}

JS::GCPtr<Painting::Paintable> ButtonBox::create_paintable() const
{
    return Painting::ButtonPaintable::create(*this);
}

}
