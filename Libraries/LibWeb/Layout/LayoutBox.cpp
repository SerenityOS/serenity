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

#include <LibGUI/Painter.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/HTML/HTMLBodyElement.h>
#include <LibWeb/Layout/LayoutBlock.h>
#include <LibWeb/Layout/LayoutBox.h>
#include <LibWeb/Page/Frame.h>

namespace Web {

void LayoutBox::paint_border(PaintContext& context, Edge edge, const Gfx::FloatRect& rect, CSS::PropertyID style_property_id, const BorderData& border_data)
{
    float width = border_data.width;
    if (width <= 0)
        return;

    auto color = border_data.color;
    auto border_style = specified_style().property(style_property_id);
    int int_width = max((int)width, 1);

    auto first_point_for_edge = [](Edge edge, const Gfx::FloatRect& rect) {
        switch (edge) {
        case Edge::Top:
            return rect.top_left();
        case Edge::Right:
            return rect.top_right();
        case Edge::Bottom:
            return rect.bottom_left();
        case Edge::Left:
        default:
            return rect.top_left();
        }
    };

    auto second_point_for_edge = [](Edge edge, const Gfx::FloatRect& rect) {
        switch (edge) {
        case Edge::Top:
            return rect.top_right();
        case Edge::Right:
            return rect.bottom_right();
        case Edge::Bottom:
            return rect.bottom_right();
        case Edge::Left:
        default:
            return rect.bottom_left();
        }
    };

    auto p1 = first_point_for_edge(edge, rect);
    auto p2 = second_point_for_edge(edge, rect);

    if (border_style.has_value() && border_style.value()->to_string() == "inset") {
        auto top_left_color = Color::from_rgb(0x5a5a5a);
        auto bottom_right_color = Color::from_rgb(0x888888);
        color = (edge == Edge::Left || edge == Edge::Top) ? top_left_color : bottom_right_color;
    } else if (border_style.has_value() && border_style.value()->to_string() == "outset") {
        auto top_left_color = Color::from_rgb(0x888888);
        auto bottom_right_color = Color::from_rgb(0x5a5a5a);
        color = (edge == Edge::Left || edge == Edge::Top) ? top_left_color : bottom_right_color;
    }

    auto line_style = Gfx::Painter::LineStyle::Solid;
    if (border_style.has_value()) {
        if (border_style.value()->to_string() == "dotted")
            line_style = Gfx::Painter::LineStyle::Dotted;
        if (border_style.value()->to_string() == "dashed")
            line_style = Gfx::Painter::LineStyle::Dashed;
    }

    if (line_style != Gfx::Painter::LineStyle::Solid) {
        switch (edge) {
        case Edge::Top:
            p1.move_by(int_width / 2, int_width / 2);
            p2.move_by(-int_width / 2, int_width / 2);
            break;
        case Edge::Right:
            p1.move_by(-int_width / 2, int_width / 2);
            p2.move_by(-int_width / 2, -int_width / 2);
            break;
        case Edge::Bottom:
            p1.move_by(int_width / 2, -int_width / 2);
            p2.move_by(-int_width / 2, -int_width / 2);
            break;
        case Edge::Left:
            p1.move_by(int_width / 2, int_width / 2);
            p2.move_by(int_width / 2, -int_width / 2);
            break;
        }
        context.painter().draw_line({ (int)p1.x(), (int)p1.y() }, { (int)p2.x(), (int)p2.y() }, color, int_width, line_style);
        return;
    }

    auto draw_line = [&](auto& p1, auto& p2) {
        context.painter().draw_line({ (int)p1.x(), (int)p1.y() }, { (int)p2.x(), (int)p2.y() }, color, 1, line_style);
    };

    float p1_step = 0;
    float p2_step = 0;

    switch (edge) {
    case Edge::Top:
        p1_step = style().border_left().width / (float)int_width;
        p2_step = style().border_right().width / (float)int_width;
        for (int i = 0; i < int_width; ++i) {
            draw_line(p1, p2);
            p1.move_by(p1_step, 1);
            p2.move_by(-p2_step, 1);
        }
        break;
    case Edge::Right:
        p1_step = style().border_top().width / (float)int_width;
        p2_step = style().border_bottom().width / (float)int_width;
        for (int i = int_width - 1; i >= 0; --i) {
            draw_line(p1, p2);
            p1.move_by(-1, p1_step);
            p2.move_by(-1, -p2_step);
        }
        break;
    case Edge::Bottom:
        p1_step = style().border_left().width / (float)int_width;
        p2_step = style().border_right().width / (float)int_width;
        for (int i = int_width - 1; i >= 0; --i) {
            draw_line(p1, p2);
            p1.move_by(p1_step, -1);
            p2.move_by(-p2_step, -1);
        }
        break;
    case Edge::Left:
        p1_step = style().border_top().width / (float)int_width;
        p2_step = style().border_bottom().width / (float)int_width;
        for (int i = 0; i < int_width; ++i) {
            draw_line(p1, p2);
            p1.move_by(1, p1_step);
            p2.move_by(1, -p2_step);
        }
        break;
    }
}

void LayoutBox::paint(PaintContext& context, PaintPhase phase)
{
    if (!is_visible())
        return;

    Gfx::PainterStateSaver saver(context.painter());
    if (is_fixed_position())
        context.painter().translate(context.scroll_offset());

    Gfx::FloatRect padded_rect;
    padded_rect.set_x(absolute_x() - box_model().padding.left.to_px(*this));
    padded_rect.set_width(width() + box_model().padding.left.to_px(*this) + box_model().padding.right.to_px(*this));
    padded_rect.set_y(absolute_y() - box_model().padding.top.to_px(*this));
    padded_rect.set_height(height() + box_model().padding.top.to_px(*this) + box_model().padding.bottom.to_px(*this));

    if (phase == PaintPhase::Background && !is_body()) {
        // FIXME: We should paint the body here too, but that currently happens at the view layer.
        auto bgcolor = specified_style().property(CSS::PropertyID::BackgroundColor);
        if (bgcolor.has_value() && bgcolor.value()->is_color()) {
            context.painter().fill_rect(enclosing_int_rect(padded_rect), bgcolor.value()->to_color(document()));
        }

        auto bgimage = specified_style().property(CSS::PropertyID::BackgroundImage);
        if (bgimage.has_value() && bgimage.value()->is_image()) {
            auto& image_value = static_cast<const CSS::ImageStyleValue&>(*bgimage.value());
            if (image_value.bitmap()) {
                context.painter().draw_tiled_bitmap(enclosing_int_rect(padded_rect), *image_value.bitmap());
            }
        }
    }

    if (phase == PaintPhase::Border) {
        Gfx::FloatRect bordered_rect;
        bordered_rect.set_x(padded_rect.x() - box_model().border.left.to_px(*this));
        bordered_rect.set_width(padded_rect.width() + box_model().border.left.to_px(*this) + box_model().border.right.to_px(*this));
        bordered_rect.set_y(padded_rect.y() - box_model().border.top.to_px(*this));
        bordered_rect.set_height(padded_rect.height() + box_model().border.top.to_px(*this) + box_model().border.bottom.to_px(*this));

        paint_border(context, Edge::Left, bordered_rect, CSS::PropertyID::BorderLeftStyle, style().border_left());
        paint_border(context, Edge::Right, bordered_rect, CSS::PropertyID::BorderRightStyle, style().border_right());
        paint_border(context, Edge::Top, bordered_rect, CSS::PropertyID::BorderTopStyle, style().border_top());
        paint_border(context, Edge::Bottom, bordered_rect, CSS::PropertyID::BorderBottomStyle, style().border_bottom());
    }

    LayoutNodeWithStyleAndBoxModelMetrics::paint(context, phase);

    if (phase == PaintPhase::Overlay && node() && document().inspected_node() == node()) {
        auto content_rect = absolute_rect();

        auto margin_box = box_model().margin_box(*this);
        Gfx::FloatRect margin_rect;
        margin_rect.set_x(absolute_x() - margin_box.left);
        margin_rect.set_width(width() + margin_box.left + margin_box.right);
        margin_rect.set_y(absolute_y() - margin_box.top);
        margin_rect.set_height(height() + margin_box.top + margin_box.bottom);

        context.painter().draw_rect(enclosing_int_rect(margin_rect), Color::Yellow);
        context.painter().draw_rect(enclosing_int_rect(padded_rect), Color::Cyan);
        context.painter().draw_rect(enclosing_int_rect(content_rect), Color::Magenta);
    }

    if (phase == PaintPhase::FocusOutline && node() && node()->is_element() && downcast<DOM::Element>(*node()).is_focused()) {
        context.painter().draw_rect(enclosing_int_rect(absolute_rect()), context.palette().focus_outline());
    }
}

HitTestResult LayoutBox::hit_test(const Gfx::IntPoint& position, HitTestType type) const
{
    // FIXME: It would be nice if we could confidently skip over hit testing
    //        parts of the layout tree, but currently we can't just check
    //        m_rect.contains() since inline text rects can't be trusted..
    HitTestResult result { absolute_rect().contains(position.x(), position.y()) ? this : nullptr };
    for_each_child([&](auto& child) {
        if (is<LayoutBox>(child) && downcast<LayoutBox>(child).stacking_context())
            return;
        auto child_result = child.hit_test(position, type);
        if (child_result.layout_node)
            result = child_result;
    });
    return result;
}

void LayoutBox::set_needs_display()
{
    if (!is_inline()) {
        frame().set_needs_display(enclosing_int_rect(absolute_rect()));
        return;
    }

    LayoutNode::set_needs_display();
}

bool LayoutBox::is_body() const
{
    return node() && node() == document().body();
}

void LayoutBox::set_offset(const Gfx::FloatPoint& offset)
{
    if (m_offset == offset)
        return;
    m_offset = offset;
    did_set_rect();
}

void LayoutBox::set_size(const Gfx::FloatSize& size)
{
    if (m_size == size)
        return;
    m_size = size;
    did_set_rect();
}

Gfx::FloatPoint LayoutBox::effective_offset() const
{
    if (m_containing_line_box_fragment)
        return m_containing_line_box_fragment->offset();
    return m_offset;
}

const Gfx::FloatRect LayoutBox::absolute_rect() const
{
    Gfx::FloatRect rect { effective_offset(), size() };
    for (auto* block = containing_block(); block; block = block->containing_block()) {
        rect.move_by(block->effective_offset());
    }
    return rect;
}

void LayoutBox::set_containing_line_box_fragment(LineBoxFragment& fragment)
{
    m_containing_line_box_fragment = fragment.make_weak_ptr();
}

StackingContext* LayoutBox::enclosing_stacking_context()
{
    for (auto* ancestor = parent(); ancestor; ancestor = ancestor->parent()) {
        if (!ancestor->is_box())
            continue;
        auto& ancestor_box = downcast<LayoutBox>(*ancestor);
        if (!ancestor_box.establishes_stacking_context())
            continue;
        ASSERT(ancestor_box.stacking_context());
        return ancestor_box.stacking_context();
    }
    // We should always reach the LayoutDocument stacking context.
    ASSERT_NOT_REACHED();
}

bool LayoutBox::establishes_stacking_context() const
{
    if (!has_style())
        return false;
    if (node() == document().root())
        return true;
    auto position = style().position();
    auto z_index = style().z_index();
    if (position == CSS::Position::Absolute || position == CSS::Position::Relative) {
        if (z_index.has_value())
            return true;
    }
    if (position == CSS::Position::Fixed || position == CSS::Position::Sticky)
        return true;
    return false;
}

}
