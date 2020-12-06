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
#include <LibWeb/Dump.h>
#include <LibWeb/HTML/HTMLHtmlElement.h>
#include <LibWeb/Layout/BlockBox.h>
#include <LibWeb/Layout/FormattingContext.h>
#include <LibWeb/Layout/InitialContainingBlockBox.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Layout/ReplacedBox.h>
#include <LibWeb/Page/Frame.h>

namespace Web::Layout {

Node::Node(DOM::Document& document, DOM::Node* node)
    : m_document(document)
    , m_dom_node(node)
{
    if (m_dom_node)
        m_dom_node->set_layout_node({}, this);
}

Node::~Node()
{
    if (m_dom_node && m_dom_node->layout_node() == this)
        m_dom_node->set_layout_node({}, nullptr);
}

bool Node::can_contain_boxes_with_position_absolute() const
{
    return style().position() != CSS::Position::Static || is_initial_containing_block();
}

const BlockBox* Node::containing_block() const
{
    auto nearest_block_ancestor = [this] {
        auto* ancestor = parent();
        while (ancestor && !is<BlockBox>(*ancestor))
            ancestor = ancestor->parent();
        return downcast<BlockBox>(ancestor);
    };

    if (is_text())
        return nearest_block_ancestor();

    auto position = style().position();

    if (position == CSS::Position::Absolute) {
        auto* ancestor = parent();
        while (ancestor && !ancestor->can_contain_boxes_with_position_absolute())
            ancestor = ancestor->parent();
        while (ancestor && (!is<BlockBox>(ancestor) || ancestor->is_anonymous()))
            ancestor = ancestor->containing_block();
        return downcast<BlockBox>(ancestor);
    }

    if (position == CSS::Position::Fixed)
        return &root();

    return nearest_block_ancestor();
}

void Node::paint(PaintContext& context, PaintPhase phase)
{
    if (!is_visible())
        return;

    before_children_paint(context, phase);

    for_each_child_in_paint_order([&](auto& child) {
        child.paint(context, phase);
    });

    after_children_paint(context, phase);
}

HitTestResult Node::hit_test(const Gfx::IntPoint& position, HitTestType type) const
{
    HitTestResult result;
    for_each_child_in_paint_order([&](auto& child) {
        auto child_result = child.hit_test(position, type);
        if (child_result.layout_node)
            result = child_result;
    });
    return result;
}

const Frame& Node::frame() const
{
    ASSERT(document().frame());
    return *document().frame();
}

Frame& Node::frame()
{
    ASSERT(document().frame());
    return *document().frame();
}

const InitialContainingBlockBox& Node::root() const
{
    ASSERT(document().layout_node());
    return *document().layout_node();
}

InitialContainingBlockBox& Node::root()
{
    ASSERT(document().layout_node());
    return *document().layout_node();
}

void Node::split_into_lines(InlineFormattingContext& context, LayoutMode layout_mode)
{
    for_each_child([&](auto& child) {
        child.split_into_lines(context, layout_mode);
    });
}

void Node::set_needs_display()
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

float Node::font_size() const
{
    // FIXME: This doesn't work right for relative font-sizes
    auto length = specified_style().length_or_fallback(CSS::PropertyID::FontSize, CSS::Length(10, CSS::Length::Type::Px));
    return length.raw_value();
}

Gfx::FloatPoint Node::box_type_agnostic_position() const
{
    if (is_box())
        return downcast<Box>(*this).absolute_position();
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

bool Node::is_floating() const
{
    if (!has_style())
        return false;
    return style().float_() != CSS::Float::None;
}

bool Node::is_positioned() const
{
    return has_style() && style().position() != CSS::Position::Static;
}

bool Node::is_absolutely_positioned() const
{
    if (!has_style())
        return false;
    auto position = style().position();
    return position == CSS::Position::Absolute || position == CSS::Position::Fixed;
}

bool Node::is_fixed_position() const
{
    if (!has_style())
        return false;
    auto position = style().position();
    return position == CSS::Position::Fixed;
}

NodeWithStyle::NodeWithStyle(DOM::Document& document, DOM::Node* node, NonnullRefPtr<CSS::StyleProperties> specified_style)
    : Node(document, node)
    , m_specified_style(move(specified_style))
{
    m_has_style = true;
    apply_style(*m_specified_style);
}

void NodeWithStyle::apply_style(const CSS::StyleProperties& specified_style)
{
    auto& style = static_cast<MutableLayoutStyle&>(m_style);

    style.set_position(specified_style.position());
    style.set_text_align(specified_style.text_align());

    auto white_space = specified_style.white_space();
    if (white_space.has_value())
        style.set_white_space(white_space.value());

    auto float_ = specified_style.float_();
    if (float_.has_value())
        style.set_float(float_.value());

    auto clear = specified_style.clear();
    if (clear.has_value())
        style.set_clear(clear.value());

    style.set_z_index(specified_style.z_index());
    style.set_width(specified_style.length_or_fallback(CSS::PropertyID::Width, {}));
    style.set_min_width(specified_style.length_or_fallback(CSS::PropertyID::MinWidth, {}));
    style.set_max_width(specified_style.length_or_fallback(CSS::PropertyID::MaxWidth, {}));
    style.set_height(specified_style.length_or_fallback(CSS::PropertyID::Height, {}));
    style.set_min_height(specified_style.length_or_fallback(CSS::PropertyID::MinHeight, {}));
    style.set_max_height(specified_style.length_or_fallback(CSS::PropertyID::MaxHeight, {}));

    style.set_offset(specified_style.length_box(CSS::PropertyID::Left, CSS::PropertyID::Top, CSS::PropertyID::Right, CSS::PropertyID::Bottom));
    style.set_margin(specified_style.length_box(CSS::PropertyID::MarginLeft, CSS::PropertyID::MarginTop, CSS::PropertyID::MarginRight, CSS::PropertyID::MarginBottom));
    style.set_padding(specified_style.length_box(CSS::PropertyID::PaddingLeft, CSS::PropertyID::PaddingTop, CSS::PropertyID::PaddingRight, CSS::PropertyID::PaddingBottom));

    auto do_border_style = [&](BorderData& border, CSS::PropertyID width_property, CSS::PropertyID color_property, CSS::PropertyID style_property) {
        border.width = specified_style.length_or_fallback(width_property, {}).resolved_or_zero(*this, 0).to_px(*this);
        border.color = specified_style.color_or_fallback(color_property, document(), Color::Transparent);
        border.line_style = specified_style.line_style(style_property).value_or(CSS::LineStyle::None);
    };

    do_border_style(style.border_left(), CSS::PropertyID::BorderLeftWidth, CSS::PropertyID::BorderLeftColor, CSS::PropertyID::BorderLeftStyle);
    do_border_style(style.border_top(), CSS::PropertyID::BorderTopWidth, CSS::PropertyID::BorderTopColor, CSS::PropertyID::BorderTopStyle);
    do_border_style(style.border_right(), CSS::PropertyID::BorderRightWidth, CSS::PropertyID::BorderRightColor, CSS::PropertyID::BorderRightStyle);
    do_border_style(style.border_bottom(), CSS::PropertyID::BorderBottomWidth, CSS::PropertyID::BorderBottomColor, CSS::PropertyID::BorderBottomStyle);
}

void Node::handle_mousedown(Badge<EventHandler>, const Gfx::IntPoint&, unsigned, unsigned)
{
}

void Node::handle_mouseup(Badge<EventHandler>, const Gfx::IntPoint&, unsigned, unsigned)
{
}

void Node::handle_mousemove(Badge<EventHandler>, const Gfx::IntPoint&, unsigned, unsigned)
{
}

bool Node::is_root_element() const
{
    if (is_anonymous())
        return false;
    return is<HTML::HTMLHtmlElement>(*dom_node());
}

}
