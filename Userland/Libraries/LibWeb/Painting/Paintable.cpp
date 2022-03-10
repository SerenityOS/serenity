/*
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Painting/Paintable.h>

namespace Web::Painting {

Paintable::Paintable(Layout::Box const& layout_box)
    : m_layout_box(layout_box)
{
}

Paintable::~Paintable()
{
}

PaintableWithLines::PaintableWithLines(Layout::BlockContainer const& layout_box)
    : Paintable(layout_box)
{
}

PaintableWithLines::~PaintableWithLines()
{
}

void Paintable::set_offset(const Gfx::FloatPoint& offset)
{
    if (m_offset == offset)
        return;
    m_offset = offset;
    // FIXME: This const_cast is gross.
    const_cast<Layout::Box&>(m_layout_box).did_set_rect();
}

void Paintable::set_content_size(Gfx::FloatSize const& size)
{
    if (m_content_size == size)
        return;
    m_content_size = size;
    // FIXME: This const_cast is gross.
    const_cast<Layout::Box&>(m_layout_box).did_set_rect();
}

Gfx::FloatPoint Paintable::effective_offset() const
{
    if (m_containing_line_box_fragment.has_value()) {
        auto const& fragment = m_layout_box.containing_block()->paint_box()->line_boxes()[m_containing_line_box_fragment->line_box_index].fragments()[m_containing_line_box_fragment->fragment_index];
        return fragment.offset();
    }
    return m_offset;
}

Gfx::FloatRect Paintable::absolute_rect() const
{
    Gfx::FloatRect rect { effective_offset(), content_size() };
    for (auto* block = m_layout_box.containing_block(); block; block = block->containing_block())
        rect.translate_by(block->m_paint_box->effective_offset());
    return rect;
}

void Paintable::set_containing_line_box_fragment(Optional<Layout::LineBoxFragmentCoordinate> fragment_coordinate)
{
    m_containing_line_box_fragment = fragment_coordinate;
}

Painting::StackingContext* Paintable::enclosing_stacking_context()
{
    for (auto* ancestor = m_layout_box.parent(); ancestor; ancestor = ancestor->parent()) {
        if (!is<Layout::Box>(ancestor))
            continue;
        auto& ancestor_box = static_cast<Layout::Box&>(const_cast<Layout::NodeWithStyle&>(*ancestor));
        if (!ancestor_box.establishes_stacking_context())
            continue;
        VERIFY(ancestor_box.m_paint_box->stacking_context());
        return ancestor_box.m_paint_box->stacking_context();
    }
    // We should always reach the Layout::InitialContainingBlock stacking context.
    VERIFY_NOT_REACHED();
}

}
