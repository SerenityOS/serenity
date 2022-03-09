/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Painting/Box.h>

namespace Web::Painting {

void Box::set_offset(const Gfx::FloatPoint& offset)
{
    if (m_offset == offset)
        return;
    m_offset = offset;
    // FIXME: This const_cast is gross.
    const_cast<Layout::Box&>(m_layout_box).did_set_rect();
}

void Box::set_content_size(Gfx::FloatSize const& size)
{
    if (m_content_size == size)
        return;
    m_content_size = size;
    // FIXME: This const_cast is gross.
    const_cast<Layout::Box&>(m_layout_box).did_set_rect();
}

Gfx::FloatPoint Box::effective_offset() const
{
    if (m_containing_line_box_fragment.has_value()) {
        auto const& fragment = m_layout_box.containing_block()->m_paint_box->line_boxes()[m_containing_line_box_fragment->line_box_index].fragments()[m_containing_line_box_fragment->fragment_index];
        return fragment.offset();
    }
    return m_offset;
}

Gfx::FloatRect Box::absolute_rect() const
{
    Gfx::FloatRect rect { effective_offset(), content_size() };
    for (auto* block = m_layout_box.containing_block(); block; block = block->containing_block())
        rect.translate_by(block->m_paint_box->effective_offset());
    return rect;
}

void Box::set_containing_line_box_fragment(Optional<Layout::LineBoxFragmentCoordinate> fragment_coordinate)
{
    m_containing_line_box_fragment = fragment_coordinate;
}

}
