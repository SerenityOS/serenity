/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Demangle.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/Painter.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Dump.h>
#include <LibWeb/HTML/HTMLHtmlElement.h>
#include <LibWeb/Layout/BlockBox.h>
#include <LibWeb/Layout/FormattingContext.h>
#include <LibWeb/Layout/InitialContainingBlockBox.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Layout/TextNode.h>
#include <LibWeb/Page/BrowsingContext.h>

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
    return computed_values().position() != CSS::Position::Static || is<InitialContainingBlockBox>(*this);
}

const BlockBox* Node::containing_block() const
{
    auto nearest_block_ancestor = [this] {
        auto* ancestor = parent();
        while (ancestor && !is<BlockBox>(*ancestor))
            ancestor = ancestor->parent();
        return static_cast<const BlockBox*>(ancestor);
    };

    if (is<TextNode>(*this))
        return nearest_block_ancestor();

    auto position = computed_values().position();

    if (position == CSS::Position::Absolute) {
        auto* ancestor = parent();
        while (ancestor && !ancestor->can_contain_boxes_with_position_absolute())
            ancestor = ancestor->parent();
        while (ancestor && (!is<BlockBox>(*ancestor) || ancestor->is_anonymous()))
            ancestor = ancestor->containing_block();
        return static_cast<const BlockBox*>(ancestor);
    }

    if (position == CSS::Position::Fixed)
        return &root();

    return nearest_block_ancestor();
}

bool Node::establishes_stacking_context() const
{
    if (!has_style())
        return false;
    if (dom_node() == document().root())
        return true;
    auto position = computed_values().position();
    if (position == CSS::Position::Absolute || position == CSS::Position::Relative || position == CSS::Position::Fixed || position == CSS::Position::Sticky)
        return true;
    return false;
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

const BrowsingContext& Node::browsing_context() const
{
    VERIFY(document().browsing_context());
    return *document().browsing_context();
}

BrowsingContext& Node::browsing_context()
{
    VERIFY(document().browsing_context());
    return *document().browsing_context();
}

const InitialContainingBlockBox& Node::root() const
{
    VERIFY(document().layout_node());
    return *document().layout_node();
}

InitialContainingBlockBox& Node::root()
{
    VERIFY(document().layout_node());
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
                browsing_context().set_needs_display(enclosing_int_rect(fragment.absolute_rect()));
            }
            return IterationDecision::Continue;
        });
    }
}

Gfx::FloatPoint Node::box_type_agnostic_position() const
{
    if (is<Box>(*this))
        return downcast<Box>(*this).absolute_position();
    VERIFY(is_inline());
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
    // flex-items don't float.
    if (is_flex_item())
        return false;
    return computed_values().float_() != CSS::Float::None;
}

bool Node::is_positioned() const
{
    return has_style() && computed_values().position() != CSS::Position::Static;
}

bool Node::is_absolutely_positioned() const
{
    if (!has_style())
        return false;
    auto position = computed_values().position();
    return position == CSS::Position::Absolute || position == CSS::Position::Fixed;
}

bool Node::is_fixed_position() const
{
    if (!has_style())
        return false;
    auto position = computed_values().position();
    return position == CSS::Position::Fixed;
}

NodeWithStyle::NodeWithStyle(DOM::Document& document, DOM::Node* node, NonnullRefPtr<CSS::StyleProperties> specified_style)
    : Node(document, node)
{
    m_has_style = true;
    apply_style(*specified_style);
}

NodeWithStyle::NodeWithStyle(DOM::Document& document, DOM::Node* node, CSS::ComputedValues computed_values)
    : Node(document, node)
    , m_computed_values(move(computed_values))
{
    m_has_style = true;
    m_font = Gfx::FontDatabase::default_font();
}

void NodeWithStyle::apply_style(const CSS::StyleProperties& specified_style)
{
    auto& computed_values = static_cast<CSS::MutableComputedValues&>(m_computed_values);

    m_font = specified_style.font();
    m_line_height = specified_style.line_height(*this);

    {
        // FIXME: This doesn't work right for relative font-sizes
        auto length = specified_style.length_or_fallback(CSS::PropertyID::FontSize, CSS::Length(10, CSS::Length::Type::Px));
        m_font_size = length.raw_value();
    }

    auto bgimage = specified_style.property(CSS::PropertyID::BackgroundImage);
    if (bgimage.has_value() && bgimage.value()->is_image()) {
        m_background_image = static_ptr_cast<CSS::ImageStyleValue>(bgimage.value());
    }

    auto border_bottom_left_radius = specified_style.property(CSS::PropertyID::BorderBottomLeftRadius);
    if (border_bottom_left_radius.has_value())
        computed_values.set_border_bottom_left_radius(border_bottom_left_radius.value()->to_length());

    auto border_bottom_right_radius = specified_style.property(CSS::PropertyID::BorderBottomRightRadius);
    if (border_bottom_right_radius.has_value())
        computed_values.set_border_bottom_right_radius(border_bottom_right_radius.value()->to_length());

    auto border_top_left_radius = specified_style.property(CSS::PropertyID::BorderTopLeftRadius);
    if (border_top_left_radius.has_value())
        computed_values.set_border_top_left_radius(border_top_left_radius.value()->to_length());

    auto border_top_right_radius = specified_style.property(CSS::PropertyID::BorderTopRightRadius);
    if (border_top_right_radius.has_value())
        computed_values.set_border_top_right_radius(border_top_right_radius.value()->to_length());

    auto background_repeat_x = specified_style.background_repeat_x();
    if (background_repeat_x.has_value())
        computed_values.set_background_repeat_x(background_repeat_x.value());

    auto background_repeat_y = specified_style.background_repeat_y();
    if (background_repeat_y.has_value())
        computed_values.set_background_repeat_y(background_repeat_y.value());

    computed_values.set_display(specified_style.display());

    auto flex_direction = specified_style.flex_direction();
    if (flex_direction.has_value())
        computed_values.set_flex_direction(flex_direction.value());

    auto flex_wrap = specified_style.flex_wrap();
    if (flex_wrap.has_value())
        computed_values.set_flex_wrap(flex_wrap.value());

    auto flex_basis = specified_style.flex_basis();
    if (flex_basis.has_value())
        computed_values.set_flex_basis(flex_basis.value());

    computed_values.set_flex_grow_factor(specified_style.flex_grow_factor());
    computed_values.set_flex_shrink_factor(specified_style.flex_shrink_factor());

    auto position = specified_style.position();
    if (position.has_value()) {
        computed_values.set_position(position.value());
        if (position.value() == CSS::Position::Absolute) {
            m_has_definite_width = true;
            m_has_definite_height = true;
        }
    }

    auto text_align = specified_style.text_align();
    if (text_align.has_value())
        computed_values.set_text_align(text_align.value());

    auto white_space = specified_style.white_space();
    if (white_space.has_value())
        computed_values.set_white_space(white_space.value());

    auto float_ = specified_style.float_();
    if (float_.has_value())
        computed_values.set_float(float_.value());

    auto clear = specified_style.clear();
    if (clear.has_value())
        computed_values.set_clear(clear.value());

    auto overflow_x = specified_style.overflow_x();
    if (overflow_x.has_value())
        computed_values.set_overflow_x(overflow_x.value());

    auto overflow_y = specified_style.overflow_y();
    if (overflow_y.has_value())
        computed_values.set_overflow_y(overflow_y.value());

    auto cursor = specified_style.cursor();
    if (cursor.has_value())
        computed_values.set_cursor(cursor.value());

    auto text_decoration_line = specified_style.text_decoration_line();
    if (text_decoration_line.has_value())
        computed_values.set_text_decoration_line(text_decoration_line.value());

    auto text_transform = specified_style.text_transform();
    if (text_transform.has_value())
        computed_values.set_text_transform(text_transform.value());

    if (auto list_style_type = specified_style.list_style_type(); list_style_type.has_value())
        computed_values.set_list_style_type(list_style_type.value());

    computed_values.set_color(specified_style.color_or_fallback(CSS::PropertyID::Color, document(), Color::Black));
    computed_values.set_background_color(specified_style.color_or_fallback(CSS::PropertyID::BackgroundColor, document(), Color::Transparent));

    computed_values.set_z_index(specified_style.z_index());

    if (auto width = specified_style.property(CSS::PropertyID::Width); width.has_value())
        m_has_definite_width = true;
    computed_values.set_width(specified_style.length_or_fallback(CSS::PropertyID::Width, {}));
    computed_values.set_min_width(specified_style.length_or_fallback(CSS::PropertyID::MinWidth, {}));
    computed_values.set_max_width(specified_style.length_or_fallback(CSS::PropertyID::MaxWidth, {}));

    if (auto height = specified_style.property(CSS::PropertyID::Height); height.has_value())
        m_has_definite_height = true;
    computed_values.set_height(specified_style.length_or_fallback(CSS::PropertyID::Height, {}));
    computed_values.set_min_height(specified_style.length_or_fallback(CSS::PropertyID::MinHeight, {}));
    computed_values.set_max_height(specified_style.length_or_fallback(CSS::PropertyID::MaxHeight, {}));

    computed_values.set_offset(specified_style.length_box(CSS::PropertyID::Left, CSS::PropertyID::Top, CSS::PropertyID::Right, CSS::PropertyID::Bottom, CSS::Length::make_auto()));
    computed_values.set_margin(specified_style.length_box(CSS::PropertyID::MarginLeft, CSS::PropertyID::MarginTop, CSS::PropertyID::MarginRight, CSS::PropertyID::MarginBottom, CSS::Length::make_px(0)));
    computed_values.set_padding(specified_style.length_box(CSS::PropertyID::PaddingLeft, CSS::PropertyID::PaddingTop, CSS::PropertyID::PaddingRight, CSS::PropertyID::PaddingBottom, CSS::Length::make_px(0)));

    auto do_border_style = [&](CSS::BorderData& border, CSS::PropertyID width_property, CSS::PropertyID color_property, CSS::PropertyID style_property) {
        border.color = specified_style.color_or_fallback(color_property, document(), Color::Transparent);
        border.line_style = specified_style.line_style(style_property).value_or(CSS::LineStyle::None);
        if (border.line_style == CSS::LineStyle::None)
            border.width = 0;
        else
            border.width = specified_style.length_or_fallback(width_property, {}).resolved_or_zero(*this, 0).to_px(*this);
    };

    do_border_style(computed_values.border_left(), CSS::PropertyID::BorderLeftWidth, CSS::PropertyID::BorderLeftColor, CSS::PropertyID::BorderLeftStyle);
    do_border_style(computed_values.border_top(), CSS::PropertyID::BorderTopWidth, CSS::PropertyID::BorderTopColor, CSS::PropertyID::BorderTopStyle);
    do_border_style(computed_values.border_right(), CSS::PropertyID::BorderRightWidth, CSS::PropertyID::BorderRightColor, CSS::PropertyID::BorderRightStyle);
    do_border_style(computed_values.border_bottom(), CSS::PropertyID::BorderBottomWidth, CSS::PropertyID::BorderBottomColor, CSS::PropertyID::BorderBottomStyle);
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

bool Node::handle_mousewheel(Badge<EventHandler>, const Gfx::IntPoint&, unsigned, unsigned, int wheel_delta)
{
    if (auto* containing_block = this->containing_block()) {
        if (!containing_block->is_scrollable())
            return false;
        auto new_offset = containing_block->scroll_offset();
        new_offset.translate_by(0, wheel_delta);
        containing_block->set_scroll_offset(new_offset);
        return true;
    }

    return false;
}

bool Node::is_root_element() const
{
    if (is_anonymous())
        return false;
    return is<HTML::HTMLHtmlElement>(*dom_node());
}

String Node::class_name() const
{
    return demangle(typeid(*this).name());
}

bool Node::is_inline_block() const
{
    return is_inline() && is<BlockBox>(*this);
}

NonnullRefPtr<NodeWithStyle> NodeWithStyle::create_anonymous_wrapper() const
{
    auto wrapper = adopt_ref(*new BlockBox(const_cast<DOM::Document&>(document()), nullptr, m_computed_values.clone_inherited_values()));
    wrapper->m_font = m_font;
    wrapper->m_font_size = m_font_size;
    wrapper->m_line_height = m_line_height;
    wrapper->m_background_image = m_background_image;
    return wrapper;
}

}
