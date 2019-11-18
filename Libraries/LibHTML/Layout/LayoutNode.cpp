#include <LibGUI/GPainter.h>
#include <LibHTML/DOM/Document.h>
#include <LibHTML/DOM/Element.h>
#include <LibHTML/Frame.h>
#include <LibHTML/Layout/LayoutBlock.h>
#include <LibHTML/Layout/LayoutNode.h>

LayoutNode::LayoutNode(const Node* node)
    : m_node(node)
{
    if (m_node)
        m_node->set_layout_node({}, this);
}

LayoutNode::~LayoutNode()
{
    if (m_node && m_node->layout_node() == this)
        m_node->set_layout_node({}, nullptr);
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
        if (is<LayoutBlock>(*ancestor))
            return to<LayoutBlock>(ancestor);
    }
    return nullptr;
}

void LayoutNode::render(RenderingContext& context)
{
    if (!is_visible())
        return;

    // TODO: render our border
    for_each_child([&](auto& child) {
        child.render(context);
    });
}

HitTestResult LayoutNode::hit_test(const Point& position) const
{
    HitTestResult result;
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

Document& LayoutNode::document()
{
    if (is_anonymous())
        return parent()->document();
    // FIXME: Remove this const_cast once we give up on the idea of a const link from layout tree to DOM tree.
    return const_cast<Node*>(node())->document();
}

const LayoutDocument& LayoutNode::root() const
{
    ASSERT(document().layout_node());
    return *document().layout_node();
}

LayoutDocument& LayoutNode::root()
{
    ASSERT(document().layout_node());
    return *document().layout_node();
}

void LayoutNode::split_into_lines(LayoutBlock& container)
{
    for_each_child([&](auto& child) {
        if (child.is_inline()) {
            child.split_into_lines(container);
        } else {
            // FIXME: Support block children of inlines.
        }
    });
}

void LayoutNode::set_needs_display()
{
    auto* frame = document().frame();
    ASSERT(frame);

    if (auto* block = containing_block()) {
        block->for_each_fragment([&](auto& fragment) {
            if (&fragment.layout_node() == this || is_ancestor_of(fragment.layout_node())) {
                const_cast<Frame*>(frame)->set_needs_display(enclosing_int_rect(fragment.rect()));
            }
            return IterationDecision::Continue;
        });
    }
}

FloatPoint LayoutNode::box_type_agnostic_position() const
{
    if (is_box())
        return to<LayoutBox>(*this).position();
    ASSERT(is_inline());
    FloatPoint position;
    if (auto* block = containing_block()) {
        block->for_each_fragment([&](auto& fragment) {
            if (&fragment.layout_node() == this || is_ancestor_of(fragment.layout_node())) {
                position = fragment.rect().location();
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });
    }
    return position;
}
