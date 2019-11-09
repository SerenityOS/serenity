#include <LibGUI/GPainter.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/HTMLBodyElement.h>
#include <LibHTML/Frame.h>
#include <LibHTML/Layout/LayoutBlock.h>
#include <LibHTML/Layout/LayoutBox.h>

//#define DRAW_BOXES_AROUND_LAYOUT_NODES
//#define DRAW_BOXES_AROUND_HOVERED_NODES

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
        context.painter().draw_rect(m_rect, Color::Magenta);

    Rect padded_rect;
    padded_rect.set_x(x() - box_model().padding().left.to_px());
    padded_rect.set_width(width() + box_model().padding().left.to_px() + box_model().padding().right.to_px());
    padded_rect.set_y(y() - box_model().padding().top.to_px());
    padded_rect.set_height(height() + box_model().padding().top.to_px() + box_model().padding().bottom.to_px());

    if (!is_body()) {
        auto bgcolor = style().property(CSS::PropertyID::BackgroundColor);
        if (bgcolor.has_value() && bgcolor.value()->is_color()) {
            context.painter().fill_rect(padded_rect, bgcolor.value()->to_color(document()));
        }

        auto bgimage = style().property(CSS::PropertyID::BackgroundImage);
        if (bgimage.has_value() && bgimage.value()->is_image()) {
            auto& image_value = static_cast<const ImageStyleValue&>(*bgimage.value());
            if (image_value.bitmap()) {
                context.painter().draw_tiled_bitmap(padded_rect, *image_value.bitmap());
            }
        }
    }

    // FIXME: Respect all individual border sides
    auto border_width_value = style().property(CSS::PropertyID::BorderTopWidth);
    auto border_color_value = style().property(CSS::PropertyID::BorderTopColor);
    auto border_style_value = style().property(CSS::PropertyID::BorderTopStyle);

    if (border_width_value.has_value()) {
        int border_width = border_width_value.value()->to_length().to_px();

        Color border_color;
        if (border_color_value.has_value())
            border_color = border_color_value.value()->to_color(document());
        else {
            // FIXME: This is basically CSS "currentColor" which should be handled elsewhere
            //        in a much more reusable way.
            auto color_value = style().property(CSS::PropertyID::Color);
            if (color_value.has_value())
                border_color = color_value.value()->to_color(document());
            else
                border_color = Color::Black;
        }

        if (border_style_value.has_value() && border_style_value.value()->to_string() == "inset") {
            // border-style: inset
            auto shadow_color = Color::from_rgb(0x888888);
            auto highlight_color = Color::from_rgb(0x5a5a5a);
            context.painter().draw_line(padded_rect.top_left(), padded_rect.top_right(), highlight_color, border_width);
            context.painter().draw_line(padded_rect.top_right(), padded_rect.bottom_right(), shadow_color, border_width);
            context.painter().draw_line(padded_rect.bottom_right(), padded_rect.bottom_left(), shadow_color, border_width);
            context.painter().draw_line(padded_rect.bottom_left(), padded_rect.top_left(), highlight_color, border_width);
        } else if (border_style_value.has_value() && border_style_value.value()->to_string() == "outset") {
            // border-style: outset
            auto highlight_color = Color::from_rgb(0x888888);
            auto shadow_color = Color::from_rgb(0x5a5a5a);
            context.painter().draw_line(padded_rect.top_left(), padded_rect.top_right(), highlight_color, border_width);
            context.painter().draw_line(padded_rect.top_right(), padded_rect.bottom_right(), shadow_color, border_width);
            context.painter().draw_line(padded_rect.bottom_right(), padded_rect.bottom_left(), shadow_color, border_width);
            context.painter().draw_line(padded_rect.bottom_left(), padded_rect.top_left(), highlight_color, border_width);
        } else {
            // border-style: solid
            context.painter().draw_line(padded_rect.top_left(), padded_rect.top_right(), border_color, border_width);
            context.painter().draw_line(padded_rect.top_right(), padded_rect.bottom_right(), border_color, border_width);
            context.painter().draw_line(padded_rect.bottom_right(), padded_rect.bottom_left(), border_color, border_width);
            context.painter().draw_line(padded_rect.bottom_left(), padded_rect.top_left(), border_color, border_width);
        }
    }

    LayoutNodeWithStyleAndBoxModelMetrics::render(context);
}

HitTestResult LayoutBox::hit_test(const Point& position) const
{
    // FIXME: It would be nice if we could confidently skip over hit testing
    //        parts of the layout tree, but currently we can't just check
    //        m_rect.contains() since inline text rects can't be trusted..
    HitTestResult result { m_rect.contains(position) ? this : nullptr };
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
        const_cast<Frame*>(frame)->set_needs_display(rect());
        return;
    }

    LayoutNode::set_needs_display();
}

bool LayoutBox::is_body() const
{
    return node() && node() == document().body();
}
