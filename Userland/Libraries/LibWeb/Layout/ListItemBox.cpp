/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, Tobias Christiansen <tobyase@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/ListItemBox.h>
#include <LibWeb/Layout/ListItemMarkerBox.h>

namespace Web::Layout {

ListItemBox::ListItemBox(DOM::Document& document, DOM::Element& element, NonnullRefPtr<CSS::StyleProperties> style)
    : Layout::BlockContainer(document, &element, move(style))
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
        auto* marker_style = dom_node().specified_css_values();
        VERIFY(marker_style);
        int child_index = parent()->index_of_child<ListItemBox>(*this).value();
        m_marker = adopt_ref(*new ListItemMarkerBox(document(), computed_values().list_style_type(), child_index + 1, *marker_style));
        if (first_child())
            m_marker->set_inline(first_child()->is_inline());
        append_child(*m_marker);
    }

    m_marker->set_offset(-(m_marker->content_width() + 4), 0);

    if (m_marker->content_height() > content_height())
        set_content_height(m_marker->content_height());
}

}
