/*
 * Copyright (c) 2018-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Demangle.h>
#include <LibWeb/DOM/Document.h>
#include <LibWeb/Dump.h>
#include <LibWeb/HTML/BrowsingContext.h>
#include <LibWeb/HTML/HTMLHtmlElement.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/FormattingContext.h>
#include <LibWeb/Layout/InitialContainingBlock.h>
#include <LibWeb/Layout/Node.h>
#include <LibWeb/Layout/TextNode.h>
#include <LibWeb/Platform/FontPlugin.h>
#include <typeinfo>

namespace Web::Layout {

Node::Node(DOM::Document& document, DOM::Node* node)
    : m_document(document)
    , m_dom_node(node)
{
    m_serial_id = m_document->next_layout_node_serial_id({});

    if (m_dom_node)
        m_dom_node->set_layout_node({}, this);
}

Node::~Node()
{
    if (m_dom_node && m_dom_node->layout_node() == this)
        m_dom_node->set_layout_node({}, nullptr);
}

// https://www.w3.org/TR/css-display-3/#out-of-flow
bool Node::is_out_of_flow(FormattingContext const& formatting_context) const
{
    // A layout node is out of flow if either:

    // 1. It is floated (which requires that floating is not inhibited).
    if (!formatting_context.inhibits_floating() && computed_values().float_() != CSS::Float::None)
        return true;

    // 2. It is "absolutely positioned".
    if (is_absolutely_positioned())
        return true;

    return false;
}

bool Node::can_contain_boxes_with_position_absolute() const
{
    return computed_values().position() != CSS::Position::Static || is<InitialContainingBlock>(*this);
}

BlockContainer const* Node::containing_block() const
{
    if (is<TextNode>(*this))
        return first_ancestor_of_type<BlockContainer>();

    auto position = computed_values().position();

    if (position == CSS::Position::Absolute) {
        auto* ancestor = parent();
        while (ancestor && !ancestor->can_contain_boxes_with_position_absolute())
            ancestor = ancestor->parent();
        while (ancestor && (!is<BlockContainer>(*ancestor) || ancestor->is_anonymous()))
            ancestor = ancestor->containing_block();
        return static_cast<BlockContainer const*>(ancestor);
    }

    if (position == CSS::Position::Fixed)
        return &root();

    return first_ancestor_of_type<BlockContainer>();
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
    if (!computed_values().transformations().is_empty())
        return true;

    // Element that is a child of a flex container, with z-index value other than auto.
    if (parent() && parent()->computed_values().display().is_flex_inside() && computed_values().z_index().has_value())
        return true;

    // Element that is a child of a grid container, with z-index value other than auto.
    if (parent() && parent()->computed_values().display().is_grid_inside() && computed_values().z_index().has_value())
        return true;

    return computed_values().opacity() < 1.0f;
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

InitialContainingBlock const& Node::root() const
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
        block->paint_box()->for_each_fragment([&](auto& fragment) {
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
        return verify_cast<Box>(*this).paint_box()->absolute_position();
    VERIFY(is_inline());
    Gfx::FloatPoint position;
    if (auto* block = containing_block()) {
        block->paint_box()->for_each_fragment([&](auto& fragment) {
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

NodeWithStyle::NodeWithStyle(DOM::Document& document, DOM::Node* node, NonnullRefPtr<CSS::StyleProperties> computed_style)
    : Node(document, node)
{
    m_has_style = true;
    apply_style(*computed_style);
}

NodeWithStyle::NodeWithStyle(DOM::Document& document, DOM::Node* node, CSS::ComputedValues computed_values)
    : Node(document, node)
    , m_computed_values(move(computed_values))
{
    m_has_style = true;
    m_font = Platform::FontPlugin::the().default_font();
}

void NodeWithStyle::apply_style(const CSS::StyleProperties& computed_style)
{
    auto& computed_values = static_cast<CSS::MutableComputedValues&>(m_computed_values);

    // NOTE: We have to be careful that font-related properties get set in the right order.
    //       m_font is used by Length::to_px() when resolving sizes against this layout node.
    //       That's why it has to be set before everything else.
    m_font = computed_style.computed_font();
    computed_values.set_font_size(computed_style.property(CSS::PropertyID::FontSize)->to_length().to_px(*this));
    computed_values.set_font_weight(computed_style.property(CSS::PropertyID::FontWeight)->to_integer());
    m_line_height = computed_style.line_height(*this);

    computed_values.set_vertical_align(computed_style.vertical_align());

    {
        auto attachments = computed_style.property(CSS::PropertyID::BackgroundAttachment);
        auto clips = computed_style.property(CSS::PropertyID::BackgroundClip);
        auto images = computed_style.property(CSS::PropertyID::BackgroundImage);
        auto origins = computed_style.property(CSS::PropertyID::BackgroundOrigin);
        auto positions = computed_style.property(CSS::PropertyID::BackgroundPosition);
        auto repeats = computed_style.property(CSS::PropertyID::BackgroundRepeat);
        auto sizes = computed_style.property(CSS::PropertyID::BackgroundSize);

        auto count_layers = [](auto maybe_style_value) -> size_t {
            if (maybe_style_value->is_value_list())
                return maybe_style_value->as_value_list().size();
            else
                return 1;
        };

        auto value_for_layer = [](auto& style_value, size_t layer_index) -> RefPtr<CSS::StyleValue> {
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

            if (auto image_value = value_for_layer(images, layer_index); image_value) {
                if (image_value->is_abstract_image()) {
                    layer.background_image = image_value->as_abstract_image();
                    layer.background_image->load_any_resources(document());
                }
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
    computed_values.set_background_color(computed_style.color_or_fallback(CSS::PropertyID::BackgroundColor, *this, CSS::InitialValues::background_color()));

    if (auto box_sizing = computed_style.box_sizing(); box_sizing.has_value())
        computed_values.set_box_sizing(box_sizing.release_value());

    if (auto maybe_font_variant = computed_style.font_variant(); maybe_font_variant.has_value())
        computed_values.set_font_variant(maybe_font_variant.release_value());

    // FIXME: BorderXRadius properties are now BorderRadiusStyleValues, so make use of that.
    auto border_bottom_left_radius = computed_style.property(CSS::PropertyID::BorderBottomLeftRadius);
    if (border_bottom_left_radius->is_border_radius()) {
        computed_values.set_border_bottom_left_radius(
            CSS::BorderRadiusData {
                border_bottom_left_radius->as_border_radius().horizontal_radius(),
                border_bottom_left_radius->as_border_radius().vertical_radius() });
    }
    auto border_bottom_right_radius = computed_style.property(CSS::PropertyID::BorderBottomRightRadius);
    if (border_bottom_right_radius->is_border_radius()) {
        computed_values.set_border_bottom_right_radius(
            CSS::BorderRadiusData {
                border_bottom_right_radius->as_border_radius().horizontal_radius(),
                border_bottom_right_radius->as_border_radius().vertical_radius() });
    }
    auto border_top_left_radius = computed_style.property(CSS::PropertyID::BorderTopLeftRadius);
    if (border_top_left_radius->is_border_radius()) {
        computed_values.set_border_top_left_radius(
            CSS::BorderRadiusData {
                border_top_left_radius->as_border_radius().horizontal_radius(),
                border_top_left_radius->as_border_radius().vertical_radius() });
    }
    auto border_top_right_radius = computed_style.property(CSS::PropertyID::BorderTopRightRadius);
    if (border_top_right_radius->is_border_radius()) {
        computed_values.set_border_top_right_radius(
            CSS::BorderRadiusData {
                border_top_right_radius->as_border_radius().horizontal_radius(),
                border_top_right_radius->as_border_radius().vertical_radius() });
    }
    computed_values.set_display(computed_style.display());

    auto flex_direction = computed_style.flex_direction();
    if (flex_direction.has_value())
        computed_values.set_flex_direction(flex_direction.value());

    auto flex_wrap = computed_style.flex_wrap();
    if (flex_wrap.has_value())
        computed_values.set_flex_wrap(flex_wrap.value());

    auto flex_basis = computed_style.flex_basis();
    if (flex_basis.has_value())
        computed_values.set_flex_basis(flex_basis.value());

    computed_values.set_flex_grow(computed_style.flex_grow());
    computed_values.set_flex_shrink(computed_style.flex_shrink());
    computed_values.set_order(computed_style.order());
    computed_values.set_clip(computed_style.clip());
    computed_values.set_backdrop_filter(computed_style.backdrop_filter());

    auto justify_content = computed_style.justify_content();
    if (justify_content.has_value())
        computed_values.set_justify_content(justify_content.value());

    auto align_items = computed_style.align_items();
    if (align_items.has_value())
        computed_values.set_align_items(align_items.value());

    auto align_self = computed_style.align_self();
    if (align_self.has_value())
        computed_values.set_align_self(align_self.value());

    auto appearance = computed_style.appearance();
    if (appearance.has_value())
        computed_values.set_appearance(appearance.value());

    auto position = computed_style.position();
    if (position.has_value())
        computed_values.set_position(position.value());

    auto text_align = computed_style.text_align();
    if (text_align.has_value())
        computed_values.set_text_align(text_align.value());

    auto text_justify = computed_style.text_justify();
    if (text_align.has_value())
        computed_values.set_text_justify(text_justify.value());

    auto white_space = computed_style.white_space();
    if (white_space.has_value())
        computed_values.set_white_space(white_space.value());

    auto float_ = computed_style.float_();
    if (float_.has_value())
        computed_values.set_float(float_.value());

    auto clear = computed_style.clear();
    if (clear.has_value())
        computed_values.set_clear(clear.value());

    auto overflow_x = computed_style.overflow_x();
    if (overflow_x.has_value())
        computed_values.set_overflow_x(overflow_x.value());

    auto overflow_y = computed_style.overflow_y();
    if (overflow_y.has_value())
        computed_values.set_overflow_y(overflow_y.value());

    auto cursor = computed_style.cursor();
    if (cursor.has_value())
        computed_values.set_cursor(cursor.value());

    auto image_rendering = computed_style.image_rendering();
    if (image_rendering.has_value())
        computed_values.set_image_rendering(image_rendering.value());

    auto pointer_events = computed_style.pointer_events();
    if (pointer_events.has_value())
        computed_values.set_pointer_events(pointer_events.value());

    computed_values.set_text_decoration_line(computed_style.text_decoration_line());

    auto text_decoration_style = computed_style.text_decoration_style();
    if (text_decoration_style.has_value())
        computed_values.set_text_decoration_style(text_decoration_style.value());

    auto text_transform = computed_style.text_transform();
    if (text_transform.has_value())
        computed_values.set_text_transform(text_transform.value());

    if (auto list_style_type = computed_style.list_style_type(); list_style_type.has_value())
        computed_values.set_list_style_type(list_style_type.value());

    auto list_style_image = computed_style.property(CSS::PropertyID::ListStyleImage);
    if (list_style_image->is_abstract_image()) {
        m_list_style_image = list_style_image->as_abstract_image();
        m_list_style_image->load_any_resources(document());
    }

    computed_values.set_color(computed_style.color_or_fallback(CSS::PropertyID::Color, *this, CSS::InitialValues::color()));

    // FIXME: The default text decoration color value is `currentcolor`, but since we can't resolve that easily,
    //        we just manually grab the value from `color`. This makes it dependent on `color` being
    //        specified first, so it's far from ideal.
    computed_values.set_text_decoration_color(computed_style.color_or_fallback(CSS::PropertyID::TextDecorationColor, *this, computed_values.color()));
    if (auto maybe_text_decoration_thickness = computed_style.length_percentage(CSS::PropertyID::TextDecorationThickness); maybe_text_decoration_thickness.has_value())
        computed_values.set_text_decoration_thickness(maybe_text_decoration_thickness.release_value());

    computed_values.set_text_shadow(computed_style.text_shadow());

    computed_values.set_z_index(computed_style.z_index());
    computed_values.set_opacity(computed_style.opacity());

    if (auto maybe_visibility = computed_style.visibility(); maybe_visibility.has_value())
        computed_values.set_visibility(maybe_visibility.release_value());

    m_visible = computed_values.opacity() != 0 && computed_values.visibility() == CSS::Visibility::Visible;

    if (auto maybe_length_percentage = computed_style.length_percentage(CSS::PropertyID::Width); maybe_length_percentage.has_value())
        computed_values.set_width(maybe_length_percentage.release_value());
    if (auto maybe_length_percentage = computed_style.length_percentage(CSS::PropertyID::MinWidth); maybe_length_percentage.has_value())
        computed_values.set_min_width(maybe_length_percentage.release_value());
    if (auto maybe_length_percentage = computed_style.length_percentage(CSS::PropertyID::MaxWidth); maybe_length_percentage.has_value())
        computed_values.set_max_width(maybe_length_percentage.release_value());

    if (auto maybe_length_percentage = computed_style.length_percentage(CSS::PropertyID::Height); maybe_length_percentage.has_value())
        computed_values.set_height(maybe_length_percentage.release_value());
    if (auto maybe_length_percentage = computed_style.length_percentage(CSS::PropertyID::MinHeight); maybe_length_percentage.has_value())
        computed_values.set_min_height(maybe_length_percentage.release_value());
    if (auto maybe_length_percentage = computed_style.length_percentage(CSS::PropertyID::MaxHeight); maybe_length_percentage.has_value())
        computed_values.set_max_height(maybe_length_percentage.release_value());

    computed_values.set_inset(computed_style.length_box(CSS::PropertyID::Left, CSS::PropertyID::Top, CSS::PropertyID::Right, CSS::PropertyID::Bottom, CSS::Length::make_auto()));
    computed_values.set_margin(computed_style.length_box(CSS::PropertyID::MarginLeft, CSS::PropertyID::MarginTop, CSS::PropertyID::MarginRight, CSS::PropertyID::MarginBottom, CSS::Length::make_px(0)));
    computed_values.set_padding(computed_style.length_box(CSS::PropertyID::PaddingLeft, CSS::PropertyID::PaddingTop, CSS::PropertyID::PaddingRight, CSS::PropertyID::PaddingBottom, CSS::Length::make_px(0)));

    computed_values.set_box_shadow(computed_style.box_shadow());

    computed_values.set_transformations(computed_style.transformations());
    computed_values.set_transform_origin(computed_style.transform_origin());

    auto do_border_style = [&](CSS::BorderData& border, CSS::PropertyID width_property, CSS::PropertyID color_property, CSS::PropertyID style_property) {
        // FIXME: The default border color value is `currentcolor`, but since we can't resolve that easily,
        //        we just manually grab the value from `color`. This makes it dependent on `color` being
        //        specified first, so it's far from ideal.
        border.color = computed_style.color_or_fallback(color_property, *this, computed_values.color());
        border.line_style = computed_style.line_style(style_property).value_or(CSS::LineStyle::None);
        if (border.line_style == CSS::LineStyle::None)
            border.width = 0;
        else
            border.width = computed_style.length_or_fallback(width_property, CSS::Length::make_px(0)).to_px(*this);
    };

    do_border_style(computed_values.border_left(), CSS::PropertyID::BorderLeftWidth, CSS::PropertyID::BorderLeftColor, CSS::PropertyID::BorderLeftStyle);
    do_border_style(computed_values.border_top(), CSS::PropertyID::BorderTopWidth, CSS::PropertyID::BorderTopColor, CSS::PropertyID::BorderTopStyle);
    do_border_style(computed_values.border_right(), CSS::PropertyID::BorderRightWidth, CSS::PropertyID::BorderRightColor, CSS::PropertyID::BorderRightStyle);
    do_border_style(computed_values.border_bottom(), CSS::PropertyID::BorderBottomWidth, CSS::PropertyID::BorderBottomColor, CSS::PropertyID::BorderBottomStyle);

    computed_values.set_content(computed_style.content());
    computed_values.set_grid_template_columns(computed_style.grid_template_columns());
    computed_values.set_grid_template_rows(computed_style.grid_template_rows());
    computed_values.set_grid_column_end(computed_style.grid_column_end());
    computed_values.set_grid_column_start(computed_style.grid_column_start());
    computed_values.set_grid_row_end(computed_style.grid_row_end());
    computed_values.set_grid_row_start(computed_style.grid_row_start());

    if (auto fill = computed_style.property(CSS::PropertyID::Fill); fill->has_color())
        computed_values.set_fill(fill->to_color(*this));
    if (auto stroke = computed_style.property(CSS::PropertyID::Stroke); stroke->has_color())
        computed_values.set_stroke(stroke->to_color(*this));
    auto stroke_width = computed_style.property(CSS::PropertyID::StrokeWidth);
    // FIXME: Converting to pixels isn't really correct - values should be in "user units"
    //        https://svgwg.org/svg2-draft/coords.html#TermUserUnits
    if (stroke_width->is_numeric())
        computed_values.set_stroke_width(CSS::Length::make_px(stroke_width->to_number()));
    else
        computed_values.set_stroke_width(stroke_width->to_length());
}

bool Node::is_root_element() const
{
    if (is_anonymous())
        return false;
    return is<HTML::HTMLHtmlElement>(*dom_node());
}

String Node::class_name() const
{
    auto const* mangled_name = typeid(*this).name();
    return demangle({ mangled_name, strlen(mangled_name) });
}

String Node::debug_description() const
{
    StringBuilder builder;
    builder.append(class_name().substring_view(13));
    if (dom_node()) {
        builder.appendff("<{}>", dom_node()->node_name());
        if (dom_node()->is_element()) {
            auto& element = static_cast<DOM::Element const&>(*dom_node());
            if (auto id = element.get_attribute(HTML::AttributeNames::id); !id.is_null())
                builder.appendff("#{}", id);
            for (auto const& class_name : element.class_names())
                builder.appendff(".{}", class_name);
        }
    } else {
        builder.append("(anonymous)"sv);
    }
    return builder.to_string();
}

bool Node::is_inline_block() const
{
    return is_inline() && is<BlockContainer>(*this);
}

NonnullRefPtr<NodeWithStyle> NodeWithStyle::create_anonymous_wrapper() const
{
    auto wrapper = adopt_ref(*new BlockContainer(const_cast<DOM::Document&>(document()), nullptr, m_computed_values.clone_inherited_values()));
    static_cast<CSS::MutableComputedValues&>(wrapper->m_computed_values).set_display(CSS::Display(CSS::Display::Outside::Block, CSS::Display::Inside::Flow));
    wrapper->m_font = m_font;
    wrapper->m_line_height = m_line_height;
    return wrapper;
}

void Node::set_paintable(RefPtr<Painting::Paintable> paintable)
{
    m_paintable = move(paintable);
}

RefPtr<Painting::Paintable> Node::create_paintable() const
{
    return nullptr;
}

bool Node::is_anonymous() const
{
    return !m_dom_node.ptr();
}

DOM::Node const* Node::dom_node() const
{
    return m_dom_node.ptr();
}

DOM::Node* Node::dom_node()
{
    return m_dom_node.ptr();
}

DOM::Document& Node::document()
{
    return *m_document;
}

DOM::Document const& Node::document() const
{
    return *m_document;
}

}
