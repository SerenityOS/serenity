/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/ListItemBox.h>
#include <LibWeb/Layout/ListItemMarkerBox.h>

namespace Web::Layout {

ListItemBox::ListItemBox(DOM::Document& document, DOM::Element* element, NonnullRefPtr<CSS::StyleProperties> style)
    : Layout::BlockContainer(document, element, move(style))
{
}

ListItemBox::~ListItemBox() = default;

void ListItemBox::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_marker);
}

void ListItemBox::set_marker(JS::GCPtr<ListItemMarkerBox> marker)
{
    m_marker = move(marker);
}

}
