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

#include <LibGUI/GPainter.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/HTMLBodyElement.h>
#include <LibHTML/Frame.h>
#include <LibHTML/Layout/LayoutBlock.h>
#include <LibHTML/Layout/LayoutBox.h>

//#define DRAW_BOXES_AROUND_LAYOUT_NODES
//#define DRAW_BOXES_AROUND_HOVERED_NODES

void LayoutBox::paint_border(RenderingContext& context, Edge edge, const Gfx::FloatRect& rect, CSS::PropertyID style_property_id, CSS::PropertyID color_property_id, CSS::PropertyID width_property_id)
{
    auto border_width = style().property(width_property_id);
    if (!border_width.has_value())
        return;

    auto border_style = style().property(style_property_id);
    float width = border_width.value()->to_length().to_px();

    int int_width = max((int)width, 1);

    Color color;
    auto border_color = style().property(color_property_id);
    if (border_color.has_value()) {
        color = border_color.value()->to_color(document());
    } else {
        // FIXME: This is basically CSS "currentColor" which should be handled elsewhere
        //        in a much more reusable way.
        auto current_color = style().property(CSS::PropertyID::Color);
        if (current_color.has_value())
            color = current_color.value()->to_color(document());
        else
            color = Color::Black;
    }

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

    bool dotted = border_style.has_value() && border_style.value()->to_string() == "dotted";

    auto draw_line = [&](auto& p1, auto& p2) {
        context.painter().draw_line({ (int)p1.x(), (int)p1.y() }, { (int)p2.x(), (int)p2.y() }, color, 1, dotted);
    };

    auto width_for = [&](CSS::PropertyID property_id) -> float {
        auto width = style().property(property_id);
        if (!width.has_value())
            return 0;
        return width.value()->to_length().to_px();
    };

    float p1_step = 0;
    float p2_step = 0;

    switch (edge) {
    case Edge::Top:
        p1_step = width_for(CSS::PropertyID::BorderLeftWidth) / (float)int_width;
        p2_step = width_for(CSS::PropertyID::BorderRightWidth) / (float)int_width;
        for (int i = 0; i < int_width; ++i) {
            draw_line(p1, p2);
            p1.move_by(p1_step, 1);
            p2.move_by(-p2_step, 1);
        }
        break;
    case Edge::Right:
        p1_step = width_for(CSS::PropertyID::BorderTopWidth) / (float)int_width;
        p2_step = width_for(CSS::PropertyID::BorderBottomWidth) / (float)int_width;
        for (int i = int_width - 1; i >= 0; --i) {
            draw_line(p1, p2);
            p1.move_by(-1, p1_step);
            p2.move_by(-1, -p2_step);
        }
        break;
    case Edge::Bottom:
        p1_step = width_for(CSS::PropertyID::BorderLeftWidth) / (float)int_width;
        p2_step = width_for(CSS::PropertyID::BorderRightWidth) / (float)int_width;
        for (int i = int_width - 1; i >= 0; --i) {
            draw_line(p1, p2);
            p1.move_by(p1_step, -1);
            p2.move_by(-p2_step, -1);
        }
        break;
    case Edge::Left:
        p1_step = width_for(CSS::PropertyID::BorderTopWidth) / (float)int_width;
        p2_step = width_for(CSS::PropertyID::BorderBottomWidth) / (float)int_width;
        for (int i = 0; i < int_width; ++i) {
            draw_line(p1, p2);
            p1.move_by(1, p1_step);
            p2.move_by(1, -p2_step);
        }
        break;
    }
}

void LayoutBox::render(RenderingContext& context)
{
    if (!is_visible())
        return;

#ifdef DRAW_BOXES_AROUND_LAYOUT_NODES
    context.painter().draw_rect(m_rect, Color::Blue);
#endif
#ifdef DRAW_BOXES_AROUND_HOVERED_NODES
    if (!is_anonymous() && node() == document().hovered_node())
        context.painter().draw_rect(m_rect, Color::Red);
#endif

    if (node() && document().inspected_node() == node())
        context.painter().draw_rect(enclosing_int_rect(m_rect), Color::Magenta);

    Gfx::FloatRect padded_rect;
    padded_rect.set_x(x() - box_model().padding().left.to_px());
    padded_rect.set_width(width() + box_model().padding().left.to_px() + box_model().padding().right.to_px());
    padded_rect.set_y(y() - box_model().padding().top.to_px());
    padded_rect.set_height(height() + box_model().padding().top.to_px() + box_model().padding().bottom.to_px());

    if (!is_body()) {
        auto bgcolor = style().property(CSS::PropertyID::BackgroundColor);
        if (bgcolor.has_value() && bgcolor.value()->is_color()) {
            context.painter().fill_rect(enclosing_int_rect(padded_rect), bgcolor.value()->to_color(document()));
        }

        auto bgimage = style().property(CSS::PropertyID::BackgroundImage);
        if (bgimage.has_value() && bgimage.value()->is_image()) {
            auto& image_value = static_cast<const ImageStyleValue&>(*bgimage.value());
            if (image_value.bitmap()) {
                context.painter().draw_tiled_bitmap(enclosing_int_rect(padded_rect), *image_value.bitmap());
            }
        }
    }

    Gfx::FloatRect bordered_rect;
    bordered_rect.set_x(padded_rect.x() - box_model().border().left.to_px());
    bordered_rect.set_width(padded_rect.width() + box_model().border().left.to_px() + box_model().border().right.to_px());
    bordered_rect.set_y(padded_rect.y() - box_model().border().top.to_px());
    bordered_rect.set_height(padded_rect.height() + box_model().border().top.to_px() + box_model().border().bottom.to_px());

    paint_border(context, Edge::Left, bordered_rect, CSS::PropertyID::BorderLeftStyle, CSS::PropertyID::BorderLeftColor, CSS::PropertyID::BorderLeftWidth);
    paint_border(context, Edge::Right, bordered_rect, CSS::PropertyID::BorderRightStyle, CSS::PropertyID::BorderRightColor, CSS::PropertyID::BorderRightWidth);
    paint_border(context, Edge::Top, bordered_rect, CSS::PropertyID::BorderTopStyle, CSS::PropertyID::BorderTopColor, CSS::PropertyID::BorderTopWidth);
    paint_border(context, Edge::Bottom, bordered_rect, CSS::PropertyID::BorderBottomStyle, CSS::PropertyID::BorderBottomColor, CSS::PropertyID::BorderBottomWidth);

    LayoutNodeWithStyleAndBoxModelMetrics::render(context);
}

HitTestResult LayoutBox::hit_test(const Gfx::Point& position) const
{
    // FIXME: It would be nice if we could confidently skip over hit testing
    //        parts of the layout tree, but currently we can't just check
    //        m_rect.contains() since inline text rects can't be trusted..
    HitTestResult result { m_rect.contains(FloatPoint(position.x(), position.y())) ? this : nullptr };
    for_each_child([&](auto& child) {
        auto child_result = child.hit_test(position);
        if (child_result.layout_node)
            result = child_result;
    });
    return result;
}

void LayoutBox::set_needs_display()
{
    auto* frame = document().frame();
    ASSERT(frame);

    if (!is_inline()) {
        const_cast<Frame*>(frame)->set_needs_display(enclosing_int_rect(rect()));
        return;
    }

    LayoutNode::set_needs_display();
}

bool LayoutBox::is_body() const
{
    return node() && node() == document().body();
}
