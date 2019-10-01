#include <LibGUI/GPainter.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutBlock.h>
#include <LibHTML/Layout/LayoutNode.h>

//#define DRAW_BOXES_AROUND_LAYOUT_NODES
//#define DRAW_BOXES_AROUND_HOVERED_NODES

LayoutNode::LayoutNode(const Node* node, StyleProperties&& style_properties)
    : m_node(node)
    , m_style_properties(style_properties)
{
}

LayoutNode::~LayoutNode()
{
}

void LayoutNode::layout()
{
    for_each_child([](auto& child) {
        child.layout();
    });
}

const LayoutBlock* LayoutNode::containing_block() const
{
    for (auto* ancestor = parent(); ancestor; ancestor = ancestor->parent()) {
        if (ancestor->is_block())
            return static_cast<const LayoutBlock*>(ancestor);
    }
    return nullptr;
}

void LayoutNode::render(RenderingContext& context)
{
#ifdef DRAW_BOXES_AROUND_LAYOUT_NODES
    context.painter().draw_rect(m_rect, Color::Blue);
#endif
#ifdef DRAW_BOXES_AROUND_HOVERED_NODES
    if (!is_anonymous() && node() == document().hovered_node())
        context.painter().draw_rect(m_rect, Color::Red);
#endif

    Rect padded_rect;
    padded_rect.set_x(rect().x() - style().padding().left.to_px());
    padded_rect.set_width(rect().width() + style().padding().left.to_px() + style().padding().right.to_px());
    padded_rect.set_y(rect().y() - style().padding().top.to_px());
    padded_rect.set_height(rect().height() + style().padding().top.to_px() + style().padding().bottom.to_px());

    auto bgcolor = style_properties().property("background-color");
    if (bgcolor.has_value() && bgcolor.value()->is_color()) {
        context.painter().fill_rect(padded_rect, bgcolor.value()->to_color());
    }

    auto border_width_value = style_properties().property("border-width");
    auto border_color_value = style_properties().property("border-color");
    auto border_style_value = style_properties().property("border-style");
    if (border_width_value.has_value() && border_color_value.has_value()) {
        int border_width = border_width_value.value()->to_length().to_px();
        Color border_color = border_color_value.value()->to_color();

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

    // TODO: render our border
    for_each_child([&](auto& child) {
        child.render(context);
    });
}

HitTestResult LayoutNode::hit_test(const Point& position) const
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

const Document& LayoutNode::document() const
{
    if (is_anonymous())
        return parent()->document();
    return node()->document();
}
