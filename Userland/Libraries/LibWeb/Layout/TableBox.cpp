/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Element.h>
#include <LibWeb/Layout/TableBox.h>

namespace Web::Layout {

TableBox::TableBox(DOM::Document& document, DOM::Element* element, NonnullRefPtr<CSS::StyleProperties> style)
    : Layout::BlockContainer(document, element, move(style))
{
}

TableBox::TableBox(DOM::Document& document, DOM::Element* element, CSS::ComputedValues computed_values)
    : Layout::BlockContainer(document, element, move(computed_values))
{
}

TableBox::~TableBox()
{
}

}
