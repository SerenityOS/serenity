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
#include <LibWeb/DOM/Element.h>
#include <LibWeb/Frame/Frame.h>
#include <LibWeb/Layout/LayoutBlock.h>
#include <LibWeb/Layout/LayoutDocument.h>
#include <LibWeb/Layout/LayoutNode.h>
#include <LibWeb/Layout/LayoutReplaced.h>

namespace Web {

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

void LayoutNode::layout(LayoutMode layout_mode)
{
    for_each_child([&](auto& child) {
        child.layout(layout_mode);
    });
}

bool LayoutNode::can_contain_boxes_with_position_absolute() const
{
    return style().position() != CSS::Position::Static || is_root();
}

const LayoutBlock* LayoutNode::containing_block() const
{
    auto nearest_block_ancestor = [this] {
        auto* ancestor = parent();
        while (ancestor && !is<LayoutBlock>(*ancestor))
            ancestor = ancestor->parent();
        return to<LayoutBlock>(ancestor);
    };

    if (is_text())
        return nearest_block_ancestor();

    auto position = style().position();

    if (position == CSS::Position::Absolute) {
        auto* ancestor = parent();
        while (ancestor && !ancestor->can_contain_boxes_with_position_absolute())
            ancestor = ancestor->parent();
        while (ancestor && (!is<LayoutBlock>(ancestor) || ancestor->is_anonymous()))
            ancestor = ancestor->containing_block();
        return to<LayoutBlock>(ancestor);
    }

    if (position == CSS::Position::Fixed)
        return &root();

    return nearest_block_ancestor();
}

void LayoutNode::render(RenderingContext& context)
{
    if (!is_visible())
        return;

    for_each_child([&](auto& child) {
        if (child.is_box() && to<LayoutBox>(child).stacking_context())
            return;
        child.render(context);
    });
}

HitTestResult LayoutNode::hit_test(const Gfx::IntPoint& position) const
{
    HitTestResult result;
    for_each_child([&](auto& child) {
        auto child_result = child.hit_test(position);
        if (child_result.layout_node)
            result = child_result;
    });
    return result;
}

const Frame& LayoutNode::frame() const
{
    ASSERT(document().frame());
    return *document().frame();
}

Frame& LayoutNode::frame()
{
    ASSERT(document().frame());
    return *document().frame();
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

void LayoutNode::split_into_lines(LayoutBlock& container, LayoutMode layout_mode)
{
    for_each_child([&](auto& child) {
        child.split_into_lines(container, layout_mode);
    });
}

void LayoutNode::set_needs_display()
{
    if (auto* block = containing_block()) {
        block->for_each_fragment([&](auto& fragment) {
            if (&fragment.layout_node() == this || is_ancestor_of(fragment.layout_node())) {
                frame().set_needs_display(enclosing_int_rect(fragment.absolute_rect()));
            }
            return IterationDecision::Continue;
        });
    }
}

float LayoutNode::font_size() const
{
    // FIXME: This doesn't work right for relative font-sizes
    auto length = style().length_or_fallback(CSS::PropertyID::FontSize, Length(10, Length::Type::Px));
    return length.raw_value();
}

Gfx::FloatPoint LayoutNode::box_type_agnostic_position() const
{
    if (is_box())
        return to<LayoutBox>(*this).absolute_position();
    ASSERT(is_inline());
    Gfx::FloatPoint position;
    if (auto* block = containing_block()) {
        block->for_each_fragment([&](auto& fragment) {
            if (&fragment.layout_node() == this || is_ancestor_of(fragment.layout_node())) {
                position = fragment.absolute_rect().location();
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });
    }
    return position;
}

bool LayoutNode::is_absolutely_positioned() const
{
    if (!has_style())
        return false;
    auto position = style().position();
    return position == CSS::Position::Absolute || position == CSS::Position::Fixed;
}

bool LayoutNode::is_fixed_position() const
{
    if (!has_style())
        return false;
    return style().position() == CSS::Position::Fixed;
}

}
