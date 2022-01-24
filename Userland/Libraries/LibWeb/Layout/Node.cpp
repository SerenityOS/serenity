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
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/HTMLHtmlElement.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/FormattingContext.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Layout/TextNode.h>
#include <typeinfo>

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
    return computed_values().position() != CSS::Position::Static || is<InitialContainingBlock>(*this);
}

const BlockContainer* Node::containing_block() const
{
    auto nearest_block_ancestor = [this] {
        auto* ancestor = parent();
        while (ancestor && !is<BlockContainer>(*ancestor))
            ancestor = ancestor->parent();
        return static_cast<const BlockContainer*>(ancestor);
    };

    if (is<TextNode>(*this))
        return nearest_block_ancestor();

    auto position = computed_values().position();

    if (position == CSS::Position::Absolute) {
        auto* ancestor = parent();
        while (ancestor && !ancestor->can_contain_boxes_with_position_absolute())
            ancestor = ancestor->parent();
        while (ancestor && (!is<BlockContainer>(*ancestor) || ancestor->is_anonymous()))
            ancestor = ancestor->containing_block();
        return static_cast<const BlockContainer*>(ancestor);
    }

    if (position == CSS::Position::Fixed)
        return &root();

    return nearest_block_ancestor();
}

bool Node::establishes_stacking_context() const
{
    if (!has_style())
        return false;
    if (dom_node() == &document().root())
        return true;
    auto position = computed_values().position();
    if (position == CSS::Position::Absolute || position == CSS::Position::Relative || position == CSS::Position::Fixed || position == CSS::Position::Sticky)
        return true;
    return computed_values().opacity() < 1.0f;
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

HTML::BrowsingContext const& Node::browsing_context() const
{
    VERIFY(document().browsing_context());
    return *document().browsing_context();
}

HTML::BrowsingContext& Node::browsing_context()
{
    VERIFY(document().browsing_context());
    return *document().browsing_context();
}

const InitialContainingBlock& Node::root() const
{
    VERIFY(document().layout_node());
    return *document().layout_node();
}

InitialContainingBlock& Node::root()
{
    VERIFY(document().layout_node());
    return *document().layout_node();
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
        return verify_cast<Box>(*this).absolute_position();
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

    m_font = specified_style.computed_font();
    m_line_height = specified_style.line_height(*this);

    {
        auto attachments = specified_style.property(CSS::PropertyID::BackgroundAttachment);
        auto clips = specified_style.property(CSS::PropertyID::BackgroundClip);
        auto images = specified_style.property(CSS::PropertyID::BackgroundImage);
        auto origins = specified_style.property(CSS::PropertyID::BackgroundOrigin);
        auto positions = specified_style.property(CSS::PropertyID::BackgroundPosition);
        auto repeats = specified_style.property(CSS::PropertyID::BackgroundRepeat);
        auto sizes = specified_style.property(CSS::PropertyID::BackgroundSize);

        auto count_layers = [](auto maybe_style_value) -> size_t {
            if (maybe_style_value.has_value() && maybe_style_value.value()->is_value_list())
                return maybe_style_value.value()->as_value_list().size();
            else
                return 1;
        };

        auto value_for_layer = [](auto maybe_style_value, size_t layer_index) -> RefPtr<CSS::StyleValue> {
            if (!maybe_style_value.has_value())
                return nullptr;
            auto& style_value = maybe_style_value.value();
            if (style_value->is_value_list())
                return style_value->as_value_list().value_at(layer_index, true);
            return style_value;
        };

        size_t layer_count = 1;
        layer_count = max(layer_count, count_layers(attachments));
        layer_count = max(layer_count, count_layers(clips));
        layer_count = max(layer_count, count_layers(images));
        layer_count = max(layer_count, count_layers(origins));
        layer_count = max(layer_count, count_layers(positions));
        layer_count = max(layer_count, count_layers(repeats));
        layer_count = max(layer_count, count_layers(sizes));

        Vector<CSS::BackgroundLayerData> layers;
        layers.ensure_capacity(layer_count);

        for (size_t layer_index = 0; layer_index < layer_count; layer_index++) {
            CSS::BackgroundLayerData layer;

            if (auto image_value = value_for_layer(images, layer_index); image_value && image_value->is_image()) {
                layer.image = image_value->as_image();
                layer.image->load_bitmap(document());
            }

            if (auto attachment_value = value_for_layer(attachments, layer_index); attachment_value && attachment_value->has_identifier()) {
                switch (attachment_value->to_identifier()) {
                case CSS::ValueID::Fixed:
                    layer.attachment = CSS::BackgroundAttachment::Fixed;
                    break;
                case CSS::ValueID::Local:
                    layer.attachment = CSS::BackgroundAttachment::Local;
                    break;
                case CSS::ValueID::Scroll:
                    layer.attachment = CSS::BackgroundAttachment::Scroll;
                    break;
                default:
                    break;
                }
            }

            auto as_box = [](auto value_id) {
                switch (value_id) {
                case CSS::ValueID::BorderBox:
                    return CSS::BackgroundBox::BorderBox;
                case CSS::ValueID::ContentBox:
                    return CSS::BackgroundBox::ContentBox;
                case CSS::ValueID::PaddingBox:
                    return CSS::BackgroundBox::PaddingBox;
                default:
                    VERIFY_NOT_REACHED();
                }
            };

            if (auto origin_value = value_for_layer(origins, layer_index); origin_value && origin_value->has_identifier()) {
                layer.origin = as_box(origin_value->to_identifier());
            }

            if (auto clip_value = value_for_layer(clips, layer_index); clip_value && clip_value->has_identifier()) {
                layer.clip = as_box(clip_value->to_identifier());
            }

            if (auto position_value = value_for_layer(positions, layer_index); position_value && position_value->is_position()) {
                auto& position = position_value->as_position();
                layer.position_edge_x = position.edge_x();
                layer.position_edge_y = position.edge_y();
                layer.position_offset_x = position.offset_x();
                layer.position_offset_y = position.offset_y();
            }

            if (auto size_value = value_for_layer(sizes, layer_index); size_value) {
                if (size_value->is_background_size()) {
                    auto& size = size_value->as_background_size();
                    layer.size_type = CSS::BackgroundSize::LengthPercentage;
                    layer.size_x = size.size_x();
                    layer.size_y = size.size_y();
                } else if (size_value->has_identifier()) {
                    switch (size_value->to_identifier()) {
                    case CSS::ValueID::Contain:
                        layer.size_type = CSS::BackgroundSize::Contain;
                        break;
                    case CSS::ValueID::Cover:
                        layer.size_type = CSS::BackgroundSize::Cover;
                        break;
                    default:
                        break;
                    }
                }
            }

            if (auto repeat_value = value_for_layer(repeats, layer_index); repeat_value && repeat_value->is_background_repeat()) {
                layer.repeat_x = repeat_value->as_background_repeat().repeat_x();
                layer.repeat_y = repeat_value->as_background_repeat().repeat_y();
            }

            layers.append(move(layer));
        }

        computed_values.set_background_layers(move(layers));
    }
    computed_values.set_background_color(specified_style.color_or_fallback(CSS::PropertyID::BackgroundColor, *this, CSS::InitialValues::background_color()));

    computed_values.set_box_sizing(specified_style.box_sizing());

    // FIXME: BorderXRadius properties are now BorderRadiusStyleValues, so make use of that.
    auto border_bottom_left_radius = specified_style.property(CSS::PropertyID::BorderBottomLeftRadius);
    if (border_bottom_left_radius.has_value() && border_bottom_left_radius.value()->is_border_radius())
        computed_values.set_border_bottom_left_radius(border_bottom_left_radius.value()->as_border_radius().horizontal_radius());

    auto border_bottom_right_radius = specified_style.property(CSS::PropertyID::BorderBottomRightRadius);
    if (border_bottom_right_radius.has_value() && border_bottom_right_radius.value()->is_border_radius())
        computed_values.set_border_bottom_right_radius(border_bottom_right_radius.value()->as_border_radius().horizontal_radius());

    auto border_top_left_radius = specified_style.property(CSS::PropertyID::BorderTopLeftRadius);
    if (border_top_left_radius.has_value() && border_top_left_radius.value()->is_border_radius())
        computed_values.set_border_top_left_radius(border_top_left_radius.value()->as_border_radius().horizontal_radius());

    auto border_top_right_radius = specified_style.property(CSS::PropertyID::BorderTopRightRadius);
    if (border_top_right_radius.has_value() && border_top_right_radius.value()->is_border_radius())
        computed_values.set_border_top_right_radius(border_top_right_radius.value()->as_border_radius().horizontal_radius());

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

    computed_values.set_flex_grow(specified_style.flex_grow());
    computed_values.set_flex_shrink(specified_style.flex_shrink());

    auto justify_content = specified_style.justify_content();
    if (justify_content.has_value())
        computed_values.set_justify_content(justify_content.value());

    auto align_items = specified_style.align_items();
    if (align_items.has_value())
        computed_values.set_align_items(align_items.value());

    auto position = specified_style.position();
    if (position.has_value())
        computed_values.set_position(position.value());

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

    auto pointer_events = specified_style.pointer_events();
    if (pointer_events.has_value())
        computed_values.set_pointer_events(pointer_events.value());

    auto text_decoration_line = specified_style.text_decoration_line();
    if (text_decoration_line.has_value())
        computed_values.set_text_decoration_line(text_decoration_line.value());

    auto text_decoration_style = specified_style.text_decoration_style();
    if (text_decoration_style.has_value())
        computed_values.set_text_decoration_style(text_decoration_style.value());

    auto text_transform = specified_style.text_transform();
    if (text_transform.has_value())
        computed_values.set_text_transform(text_transform.value());

    if (auto list_style_type = specified_style.list_style_type(); list_style_type.has_value())
        computed_values.set_list_style_type(list_style_type.value());

    auto list_style_image = specified_style.property(CSS::PropertyID::ListStyleImage);
    if (list_style_image.has_value() && list_style_image.value()->is_image()) {
        m_list_style_image = list_style_image.value()->as_image();
        m_list_style_image->load_bitmap(document());
    }

    computed_values.set_color(specified_style.color_or_fallback(CSS::PropertyID::Color, *this, CSS::InitialValues::color()));

    computed_values.set_z_index(specified_style.z_index());
    computed_values.set_opacity(specified_style.opacity());
    if (computed_values.opacity() == 0)
        m_visible = false;

    if (auto width = specified_style.property(CSS::PropertyID::Width); width.has_value() && !width.value()->has_auto())
        m_has_definite_width = true;
    computed_values.set_width(specified_style.length_percentage_or_fallback(CSS::PropertyID::Width, CSS::Length {}));
    computed_values.set_min_width(specified_style.length_percentage_or_fallback(CSS::PropertyID::MinWidth, CSS::Length {}));
    computed_values.set_max_width(specified_style.length_percentage_or_fallback(CSS::PropertyID::MaxWidth, CSS::Length {}));

    if (auto height = specified_style.property(CSS::PropertyID::Height); height.has_value() && !height.value()->has_auto())
        m_has_definite_height = true;
    computed_values.set_height(specified_style.length_percentage_or_fallback(CSS::PropertyID::Height, CSS::Length {}));
    computed_values.set_min_height(specified_style.length_percentage_or_fallback(CSS::PropertyID::MinHeight, CSS::Length {}));
    computed_values.set_max_height(specified_style.length_percentage_or_fallback(CSS::PropertyID::MaxHeight, CSS::Length {}));

    computed_values.set_offset(specified_style.length_box(CSS::PropertyID::Left, CSS::PropertyID::Top, CSS::PropertyID::Right, CSS::PropertyID::Bottom, CSS::Length::make_auto()));
    computed_values.set_margin(specified_style.length_box(CSS::PropertyID::MarginLeft, CSS::PropertyID::MarginTop, CSS::PropertyID::MarginRight, CSS::PropertyID::MarginBottom, CSS::Length::make_px(0)));
    computed_values.set_padding(specified_style.length_box(CSS::PropertyID::PaddingLeft, CSS::PropertyID::PaddingTop, CSS::PropertyID::PaddingRight, CSS::PropertyID::PaddingBottom, CSS::Length::make_px(0)));

    computed_values.set_box_shadow(specified_style.box_shadow());

    computed_values.set_transformations(specified_style.transformations());

    auto do_border_style = [&](CSS::BorderData& border, CSS::PropertyID width_property, CSS::PropertyID color_property, CSS::PropertyID style_property) {
        // FIXME: The default border color value is `currentcolor`, but since we can't resolve that easily,
        //        we just manually grab the value from `color`. This makes it dependent on `color` being
        //        specified first, so it's far from ideal.
        border.color = specified_style.color_or_fallback(color_property, *this, computed_values.color());
        border.line_style = specified_style.line_style(style_property).value_or(CSS::LineStyle::None);
        if (border.line_style == CSS::LineStyle::None)
            border.width = 0;
        else
            border.width = specified_style.length_or_fallback(width_property, {}).resolved_or_zero(*this).to_px(*this);
    };

    do_border_style(computed_values.border_left(), CSS::PropertyID::BorderLeftWidth, CSS::PropertyID::BorderLeftColor, CSS::PropertyID::BorderLeftStyle);
    do_border_style(computed_values.border_top(), CSS::PropertyID::BorderTopWidth, CSS::PropertyID::BorderTopColor, CSS::PropertyID::BorderTopStyle);
    do_border_style(computed_values.border_right(), CSS::PropertyID::BorderRightWidth, CSS::PropertyID::BorderRightColor, CSS::PropertyID::BorderRightStyle);
    do_border_style(computed_values.border_bottom(), CSS::PropertyID::BorderBottomWidth, CSS::PropertyID::BorderBottomColor, CSS::PropertyID::BorderBottomStyle);

    if (auto fill = specified_style.property(CSS::PropertyID::Fill); fill.has_value())
        computed_values.set_fill(fill.value()->to_color(*this));
    if (auto stroke = specified_style.property(CSS::PropertyID::Stroke); stroke.has_value())
        computed_values.set_stroke(stroke.value()->to_color(*this));
    if (auto stroke_width = specified_style.property(CSS::PropertyID::StrokeWidth); stroke_width.has_value()) {
        // FIXME: Converting to pixels isn't really correct - values should be in "user units"
        //        https://svgwg.org/svg2-draft/coords.html#TermUserUnits
        if (stroke_width.value()->is_numeric())
            computed_values.set_stroke_width(CSS::Length::make_px(stroke_width.value()->to_number()));
        else
            computed_values.set_stroke_width(stroke_width.value()->to_length());
    }
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

bool Node::handle_mousewheel(Badge<EventHandler>, const Gfx::IntPoint&, unsigned, unsigned, int wheel_delta_x, int wheel_delta_y)
{
    if (auto* containing_block = this->containing_block()) {
        if (!containing_block->is_scrollable())
            return false;
        auto new_offset = containing_block->scroll_offset();
        new_offset.translate_by(wheel_delta_x, wheel_delta_y);
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
    return is_inline() && is<BlockContainer>(*this);
}

NonnullRefPtr<NodeWithStyle> NodeWithStyle::create_anonymous_wrapper() const
{
    auto wrapper = adopt_ref(*new BlockContainer(const_cast<DOM::Document&>(document()), nullptr, m_computed_values.clone_inherited_values()));
    wrapper->m_font = m_font;
    wrapper->m_line_height = m_line_height;
    return wrapper;
}

}
