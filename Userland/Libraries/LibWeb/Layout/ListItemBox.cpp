/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobi@tobyase.de>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/ListItemBox.h>
#include <LibWeb/Layout/ListItemMarkerBox.h>

namespace Web::Layout {

ListItemBox::ListItemBox(DOM::Document& document, DOM::Element& element, NonnullRefPtr<CSS::StyleProperties> style)
    : Layout::BlockBox(document, &element, move(style))
{
}

ListItemBox::~ListItemBox()
{
}

void ListItemBox::layout_marker()
{
    if (m_marker) {
        remove_child(*m_marker);
        m_marker = nullptr;
    }

    if (computed_values().list_style_type() == CSS::ListStyleType::None)
        return;

    if (!m_marker) {
        int child_index = parent()->index_of_child<ListItemBox>(*this).value();
        m_marker = adopt_ref(*new ListItemMarkerBox(document(), computed_values().list_style_type(), child_index + 1));
        if (first_child())
            m_marker->set_inline(first_child()->is_inline());
        append_child(*m_marker);
    }

    m_marker->set_offset(-(m_marker->width() + 4), 0);
    m_marker->set_height(line_height());
}

}
