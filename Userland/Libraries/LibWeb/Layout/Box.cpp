/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Painter.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLBodyElement.h>
#include <LibWeb/HTML/HTMLHtmlElement.h>
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

    if (phase == PaintPhase::Background) {
        // If the body's background properties were propagated to the root element, do no re-paint the body's background.
        if (is_body() && document().html_element()->should_use_body_background_properties())
            return;

        Gfx::IntRect background_rect;

        Color background_color = computed_values().background_color();
        const Gfx::Bitmap* background_image = this->background_image() ? this->background_image()->bitmap() : nullptr;
        CSS::Repeat background_repeat_x = computed_values().background_repeat_x();
        CSS::Repeat background_repeat_y = computed_values().background_repeat_y();

        if (is_root_element()) {
            // CSS 2.1 Appendix E.2: If the element is a root element, paint the background over the entire canvas.
            background_rect = context.viewport_rect();

            // Section 2.11.2: If the computed value of background-image on the root element is none and its background-color is transparent,
            // user agents must instead propagate the computed values of the background properties from that element’s first HTML BODY child element.
            if (document().html_element()->should_use_body_background_properties()) {
                background_color = document().background_color(context.palette());
                background_image = document().background_image();
                background_repeat_x = document().background_repeat_x();
                background_repeat_y = document().background_repeat_y();
            }
        } else {
            background_rect = enclosing_int_rect(padded_rect);
        }

        context.painter().fill_rect(background_rect, move(background_color));
        if (background_image)
            paint_background_image(context, *background_image, background_repeat_x, background_repeat_y, move(background_rect));
    }

    if (phase == PaintPhase::Border) {
        auto bordered_rect = this->bordered_rect();
        Painting::paint_border(context, Painting::BorderEdge::Left, bordered_rect, computed_values());
        Painting::paint_border(context, Painting::BorderEdge::Right, bordered_rect, computed_values());
        Painting::paint_border(context, Painting::BorderEdge::Top, bordered_rect, computed_values());
        Painting::paint_border(context, Painting::BorderEdge::Bottom, bordered_rect, computed_values());
    }

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
    CSS::Repeat background_repeat_x,
    CSS::Repeat background_repeat_y,
    Gfx::IntRect background_rect)
{
    switch (background_repeat_x) {
    case CSS::Repeat::Round:
    case CSS::Repeat::Space:
        // FIXME: Support 'round' and 'space'. Fall through to 'repeat' since that most closely resembles these.
    case CSS::Repeat::Repeat:
        // The background rect is already sized to align with 'repeat'.
        break;
    case CSS::Repeat::NoRepeat:
        background_rect.set_width(background_image.width());
        break;
    }

    switch (background_repeat_y) {
    case CSS::Repeat::Round:
    case CSS::Repeat::Space:
        // FIXME: Support 'round' and 'space'. Fall through to 'repeat' since that most closely resembles these.
    case CSS::Repeat::Repeat:
        // The background rect is already sized to align with 'repeat'.
        break;
    case CSS::Repeat::NoRepeat:
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
        rect.translate_by(block->effective_offset());
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
