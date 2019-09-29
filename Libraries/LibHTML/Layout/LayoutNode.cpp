#include <LibGUI/GPainter.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/Layout/LayoutBlock.h>
#include <LibHTML/Layout/LayoutNode.h>

//#define DRAW_BOXES_AROUND_LAYOUT_NODES

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
    // TODO: render our background and border
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
