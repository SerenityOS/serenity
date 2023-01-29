/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Element.h>
#include <LibWeb/Layout/TableRowBox.h>

namespace Web::Layout {

TableRowBox::TableRowBox(DOM::Document& document, DOM::Element* element, NonnullRefPtr<CSS::StyleProperties> style)
    : Box(document, element, move(style))
{
}

TableRowBox::TableRowBox(DOM::Document& document, DOM::Element* element, CSS::ComputedValues computed_values)
    : Box(document, element, move(computed_values))
{
}

TableRowBox::~TableRowBox() = default;

}
