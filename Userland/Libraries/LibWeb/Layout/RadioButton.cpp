/*
 * Copyright (c) 2021, Tim Flynn <trflynn89@serenityos.org>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Event.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Layout/Label.h>
#include <LibWeb/Layout/RadioButton.h>
#include <LibWeb/Painting/RadioButtonPaintable.h>

namespace Web::Layout {

RadioButton::RadioButton(DOM::Document& document, HTML::HTMLInputElement& element, NonnullRefPtr<CSS::StyleProperties> style)
    : LabelableNode(document, element, move(style))
{
    set_intrinsic_width(12);
    set_intrinsic_height(12);
}

RadioButton::~RadioButton()
{
}

RefPtr<Painting::Paintable> RadioButton::create_paintable() const
{
    return Painting::RadioButtonPaintable::create(*this);
}

}
