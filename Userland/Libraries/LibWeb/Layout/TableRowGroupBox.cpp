/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/DOM/Element.h>
#include <LibWeb/Layout/TableCellBox.h>
#include <LibWeb/Layout/TableRowBox.h>
#include <LibWeb/Layout/TableRowGroupBox.h>

namespace Web::Layout {

TableRowGroupBox::TableRowGroupBox(DOM::Document& document, DOM::Element* element, NonnullRefPtr<CSS::StyleProperties> style)
    : Layout::BlockContainer(document, element, move(style))
{
}

TableRowGroupBox::~TableRowGroupBox() = default;

}
