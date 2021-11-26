/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Element.h>
#include <LibWeb/Layout/TableCellBox.h>
#include <LibWeb/Layout/TableRowBox.h>

namespace Web::Layout {

TableCellBox::TableCellBox(DOM::Document& document, DOM::Element* element, NonnullRefPtr<CSS::StyleProperties> style)
    : Layout::BlockContainer(document, element, move(style))
{
}

TableCellBox::TableCellBox(DOM::Document& document, DOM::Element* element, CSS::ComputedValues computed_values)
    : Layout::BlockContainer(document, element, move(computed_values))
{
}

TableCellBox::~TableCellBox()
{
}

size_t TableCellBox::colspan() const
{
    if (!dom_node())
        return 1;
    return verify_cast<DOM::Element>(*dom_node()).attribute(HTML::AttributeNames::colspan).to_uint().value_or(1);
}

}
