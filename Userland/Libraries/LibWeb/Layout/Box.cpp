/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibGfx/Painter.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLBodyElement.h>
#include <LibWeb/Layout/BlockBox.h>
#include <LibWeb/Layout/Box.h>
#include <LibWeb/Page/Frame.h>
#include <LibWeb/Painting/BorderPainting.h>

namespace Web::Layout {

void Box::paint(PaintContext& context, PaintPhase phase)
{
    if (!is_visible())
        return;

    Gfx::PainterStateSaver saver(context.painter());
    if (is_fixed_position())
        context.painter().translate(context.scroll_offset());

    auto padded_rect = this->padded_rect();

    if (phase == PaintPhase::Background && !is_body()) {
        auto background_rect = enclosing_int_rect(padded_rect);
        context.painter().fill_rect(background_rect, computed_values().background_color());

        if (background_image() && background_image()->bitmap()) {
            paint_background_image(context, *background_image()->bitmap(), computed_values().background_repeat(), move(background_rect));
        }
    }

    if (phase == PaintPhase::Border) {
        auto bordered_rect = this->bordered_rect();
        Painting::paint_border(context, Painting::BorderEdge::Left, bordered_rect, computed_values());
        Painting::paint_border(context, Painting::BorderEdge::Right, bordered_rect, computed_values());
        Painting::paint_border(context, Painting::BorderEdge::Top, bordered_rect, computed_values());
        Painting::paint_border(context, Painting::BorderEdge::Bottom, bordered_rect, computed_values());
    }

    Layout::NodeWithStyleAndBoxModelMetrics::paint(context, phase);

    if (phase == PaintPhase::Overlay && dom_node() && document().inspected_node() == dom_node()) {
        auto content_rect = absolute_rect();

        auto margin_box = box_model().margin_box();
        Gfx::FloatRect margin_rect;
        margin_rect.set_x(absolute_x() - margin_box.left);
        margin_rect.set_width(width() + margin_box.left + margin_box.right);
        margin_rect.set_y(absolute_y() - margin_box.top);
        margin_rect.set_height(height() + margin_box.top + margin_box.bottom);

        context.painter().draw_rect(enclosing_int_rect(margin_rect), Color::Yellow);
        context.painter().draw_rect(enclosing_int_rect(padded_rect), Color::Cyan);
        context.painter().draw_rect(enclosing_int_rect(content_rect), Color::Magenta);
    }

    if (phase == PaintPhase::FocusOutline && dom_node() && dom_node()->is_element() && downcast<DOM::Element>(*dom_node()).is_focused()) {
        context.painter().draw_rect(enclosing_int_rect(absolute_rect()), context.palette().focus_outline());
    }
}

void Box::paint_background_image(
    PaintContext& context,
    const Gfx::Bitmap& background_image,
    CSS::Repeat background_repeat,
    Gfx::IntRect background_rect)
{
    switch (background_repeat) {
    case CSS::Repeat::Repeat:
        // The background rect is already sized to align with 'repeat'.
        break;
    case CSS::Repeat::RepeatX:
        background_rect.set_height(background_image.height());
        break;
    case CSS::Repeat::RepeatY:
        background_rect.set_width(background_image.width());
        break;
    case CSS::Repeat::NoRepeat:
    default: // FIXME: Support 'round' and 'square'
        background_rect.set_width(background_image.width());
        background_rect.set_height(background_image.height());
        break;
    }

    context.painter().blit_tiled(background_rect, background_image, background_image.rect());
}

HitTestResult Box::hit_test(const Gfx::IntPoint& position, HitTestType type) const
{
    // FIXME: It would be nice if we could confidently skip over hit testing
    //        parts of the layout tree, but currently we can't just check
    //        m_rect.contains() since inline text rects can't be trusted..
    HitTestResult result { absolute_rect().contains(position.x(), position.y()) ? this : nullptr };
    for_each_child_in_paint_order([&](auto& child) {
        auto child_result = child.hit_test(position, type);
        if (child_result.layout_node)
            result = child_result;
    });
    return result;
}

void Box::set_needs_display()
{
    if (!is_inline()) {
        frame().set_needs_display(enclosing_int_rect(absolute_rect()));
        return;
    }

    Node::set_needs_display();
}

bool Box::is_body() const
{
    return dom_node() && dom_node() == document().body();
}

void Box::set_offset(const Gfx::FloatPoint& offset)
{
    if (m_offset == offset)
        return;
    m_offset = offset;
    did_set_rect();
}

void Box::set_size(const Gfx::FloatSize& size)
{
    if (m_size == size)
        return;
    m_size = size;
    did_set_rect();
}

Gfx::FloatPoint Box::effective_offset() const
{
    if (m_containing_line_box_fragment)
        return m_containing_line_box_fragment->offset();
    return m_offset;
}

const Gfx::FloatRect Box::absolute_rect() const
{
    Gfx::FloatRect rect { effective_offset(), size() };
    for (auto* block = containing_block(); block; block = block->containing_block()) {
        rect.move_by(block->effective_offset());
    }
    return rect;
}

void Box::set_containing_line_box_fragment(LineBoxFragment& fragment)
{
    m_containing_line_box_fragment = fragment.make_weak_ptr();
}

StackingContext* Box::enclosing_stacking_context()
{
    for (auto* ancestor = parent(); ancestor; ancestor = ancestor->parent()) {
        if (!is<Box>(ancestor))
            continue;
        auto& ancestor_box = downcast<Box>(*ancestor);
        if (!ancestor_box.establishes_stacking_context())
            continue;
        VERIFY(ancestor_box.stacking_context());
        return ancestor_box.stacking_context();
    }
    // We should always reach the Layout::InitialContainingBlockBox stacking context.
    VERIFY_NOT_REACHED();
}

bool Box::establishes_stacking_context() const
{
    if (!has_style())
        return false;
    if (dom_node() == document().root())
        return true;
    auto position = computed_values().position();
    auto z_index = computed_values().z_index();
    if (position == CSS::Position::Absolute || position == CSS::Position::Relative) {
        if (z_index.has_value())
            return true;
    }
    if (position == CSS::Position::Fixed || position == CSS::Position::Sticky)
        return true;
    return false;
}

LineBox& Box::ensure_last_line_box()
{
    if (m_line_boxes.is_empty())
        return add_line_box();
    return m_line_boxes.last();
}

LineBox& Box::add_line_box()
{
    m_line_boxes.append(LineBox());
    return m_line_boxes.last();
}

float Box::width_of_logical_containing_block() const
{
    auto* containing_block = this->containing_block();
    VERIFY(containing_block);
    return containing_block->width();
}

}
