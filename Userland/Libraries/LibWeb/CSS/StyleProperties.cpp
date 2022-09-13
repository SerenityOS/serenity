/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibCore/DirIterator.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibWeb/CSS/Clip.h>
#include <LibWeb/CSS/StyleProperties.h>
#include <LibWeb/FontCache.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/Node.h>

namespace Web::CSS {

StyleProperties::StyleProperties(StyleProperties const& other)
    : m_property_values(other.m_property_values)
{
    if (other.m_font) {
        m_font = other.m_font->clone();
    } else {
        m_font = nullptr;
    }
}

NonnullRefPtr<StyleProperties> StyleProperties::clone() const
{
    return adopt_ref(*new StyleProperties(*this));
}

void StyleProperties::set_property(CSS::PropertyID id, NonnullRefPtr<StyleValue> value)
{
    m_property_values[to_underlying(id)] = move(value);
}

NonnullRefPtr<StyleValue> StyleProperties::property(CSS::PropertyID property_id) const
{
    auto value = m_property_values[to_underlying(property_id)];
    // By the time we call this method, all properties have values assigned.
    VERIFY(!value.is_null());
    return value.release_nonnull();
}

Length StyleProperties::length_or_fallback(CSS::PropertyID id, Length const& fallback) const
{
    auto value = property(id);

    if (value->is_calculated())
        return Length::make_calculated(value->as_calculated());

    if (value->has_length())
        return value->to_length();

    return fallback;
}

LengthPercentage StyleProperties::length_percentage_or_fallback(CSS::PropertyID id, LengthPercentage const& fallback) const
{
    return length_percentage(id).value_or(fallback);
}

Optional<LengthPercentage> StyleProperties::length_percentage(CSS::PropertyID id) const
{
    auto value = property(id);

    if (value->is_calculated())
        return LengthPercentage { value->as_calculated() };

    if (value->is_percentage())
        return value->as_percentage().percentage();

    if (value->has_length())
        return value->to_length();

    return {};
}

LengthBox StyleProperties::length_box(CSS::PropertyID left_id, CSS::PropertyID top_id, CSS::PropertyID right_id, CSS::PropertyID bottom_id, const CSS::Length& default_value) const
{
    LengthBox box;
    box.left() = length_percentage_or_fallback(left_id, default_value);
    box.top() = length_percentage_or_fallback(top_id, default_value);
    box.right() = length_percentage_or_fallback(right_id, default_value);
    box.bottom() = length_percentage_or_fallback(bottom_id, default_value);
    return box;
}

Color StyleProperties::color_or_fallback(CSS::PropertyID id, Layout::NodeWithStyle const& node, Color fallback) const
{
    auto value = property(id);
    if (!value->has_color())
        return fallback;
    return value->to_color(node);
}

NonnullRefPtr<Gfx::Font> StyleProperties::font_fallback(bool monospace, bool bold)
{
    if (monospace && bold)
        return Gfx::FontDatabase::default_fixed_width_font().bold_variant();

    if (monospace)
        return Gfx::FontDatabase::default_fixed_width_font();

    if (bold)
        return Gfx::FontDatabase::default_font().bold_variant();

    return Gfx::FontDatabase::default_font();
}

float StyleProperties::line_height(Layout::Node const& layout_node) const
{
    auto line_height = property(CSS::PropertyID::LineHeight);

    if (line_height->is_identifier() && line_height->to_identifier() == ValueID::Normal)
        return layout_node.font().pixel_metrics().line_spacing();

    if (line_height->is_length()) {
        auto line_height_length = line_height->to_length();
        if (!line_height_length.is_auto())
            return line_height_length.to_px(layout_node);
    }

    if (line_height->is_numeric())
        return Length(line_height->to_number(), Length::Type::Em).to_px(layout_node);

    if (line_height->is_percentage()) {
        // Percentages are relative to 1em. https://www.w3.org/TR/css-inline-3/#valdef-line-height-percentage
        auto& percentage = line_height->as_percentage().percentage();
        return Length(percentage.as_fraction(), Length::Type::Em).to_px(layout_node);
    }

    return layout_node.font().pixel_metrics().line_spacing();
}

Optional<int> StyleProperties::z_index() const
{
    auto value = property(CSS::PropertyID::ZIndex);
    if (value->has_auto())
        return {};
    if (value->has_integer())
        return value->to_integer();
    return {};
}

float StyleProperties::opacity() const
{
    auto value = property(CSS::PropertyID::Opacity);

    float unclamped_opacity = 1.0f;

    if (value->has_number()) {
        unclamped_opacity = value->to_number();
    } else if (value->is_calculated()) {
        auto& calculated = value->as_calculated();
        if (calculated.resolved_type() == CalculatedStyleValue::ResolvedType::Percentage) {
            auto maybe_percentage = value->as_calculated().resolve_percentage();
            if (maybe_percentage.has_value())
                unclamped_opacity = maybe_percentage->as_fraction();
            else
                dbgln("Unable to resolve calc() as opacity (percentage): {}", value->to_string());
        } else {
            auto maybe_number = value->as_calculated().resolve_number();
            if (maybe_number.has_value())
                unclamped_opacity = maybe_number.value();
            else
                dbgln("Unable to resolve calc() as opacity (number): {}", value->to_string());
        }
    } else if (value->is_percentage()) {
        unclamped_opacity = value->as_percentage().percentage().as_fraction();
    }

    return clamp(unclamped_opacity, 0.0f, 1.0f);
}

Optional<CSS::FlexDirection> StyleProperties::flex_direction() const
{
    auto value = property(CSS::PropertyID::FlexDirection);
    return value_id_to_flex_direction(value->to_identifier());
}

Optional<CSS::FlexWrap> StyleProperties::flex_wrap() const
{
    auto value = property(CSS::PropertyID::FlexWrap);
    return value_id_to_flex_wrap(value->to_identifier());
}

Optional<CSS::FlexBasisData> StyleProperties::flex_basis() const
{
    auto value = property(CSS::PropertyID::FlexBasis);

    if (value->is_identifier() && value->to_identifier() == CSS::ValueID::Content)
        return { { CSS::FlexBasis::Content, {} } };

    if (value->has_auto())
        return { { CSS::FlexBasis::Auto, {} } };

    if (value->is_percentage())
        return { { CSS::FlexBasis::LengthPercentage, value->as_percentage().percentage() } };

    if (value->has_length())
        return { { CSS::FlexBasis::LengthPercentage, value->to_length() } };

    return {};
}

float StyleProperties::flex_grow() const
{
    auto value = property(CSS::PropertyID::FlexGrow);
    if (!value->has_number())
        return 0;
    return value->to_number();
}

float StyleProperties::flex_shrink() const
{
    auto value = property(CSS::PropertyID::FlexShrink);
    if (!value->has_number())
        return 1;
    return value->to_number();
}

int StyleProperties::order() const
{
    auto value = property(CSS::PropertyID::Order);
    if (!value->has_integer())
        return 0;
    return value->to_integer();
}

Optional<CSS::ImageRendering> StyleProperties::image_rendering() const
{
    auto value = property(CSS::PropertyID::ImageRendering);
    return value_id_to_image_rendering(value->to_identifier());
}

CSS::Clip StyleProperties::clip() const
{
    auto value = property(CSS::PropertyID::Clip);
    if (!value->has_rect())
        return CSS::Clip::make_auto();
    return CSS::Clip(value->as_rect().rect());
}

Optional<CSS::JustifyContent> StyleProperties::justify_content() const
{
    auto value = property(CSS::PropertyID::JustifyContent);
    return value_id_to_justify_content(value->to_identifier());
}

Vector<CSS::Transformation> StyleProperties::transformations() const
{
    auto value = property(CSS::PropertyID::Transform);

    if (value->is_identifier() && value->to_identifier() == CSS::ValueID::None)
        return {};

    if (!value->is_value_list())
        return {};

    auto& list = value->as_value_list();

    Vector<CSS::Transformation> transformations;

    for (auto& it : list.values()) {
        if (!it.is_transformation())
            return {};
        auto& transformation_style_value = it.as_transformation();
        CSS::Transformation transformation;
        transformation.function = transformation_style_value.transform_function();
        Vector<TransformValue> values;
        for (auto& transformation_value : transformation_style_value.values()) {
            if (transformation_value.is_length()) {
                values.append({ transformation_value.to_length() });
            } else if (transformation_value.is_percentage()) {
                values.append({ transformation_value.as_percentage().percentage() });
            } else if (transformation_value.is_numeric()) {
                values.append({ transformation_value.to_number() });
            } else if (transformation_value.is_angle()) {
                values.append({ transformation_value.as_angle().angle() });
            } else {
                dbgln("FIXME: Unsupported value in transform!");
            }
        }
        transformation.values = move(values);
        transformations.append(move(transformation));
    }
    return transformations;
}

static Optional<LengthPercentage> length_percentage_for_style_value(StyleValue const& value)
{
    if (value.is_length())
        return value.to_length();
    if (value.is_percentage())
        return value.as_percentage().percentage();
    return {};
}

CSS::TransformOrigin StyleProperties::transform_origin() const
{
    auto value = property(CSS::PropertyID::TransformOrigin);
    if (!value->is_value_list() || value->as_value_list().size() != 2)
        return {};
    auto const& list = value->as_value_list();
    auto x_value = length_percentage_for_style_value(list.values()[0]);
    auto y_value = length_percentage_for_style_value(list.values()[1]);
    if (!x_value.has_value() || !y_value.has_value()) {
        return {};
    }
    return { x_value.value(), y_value.value() };
}

Optional<CSS::AlignItems> StyleProperties::align_items() const
{
    auto value = property(CSS::PropertyID::AlignItems);
    return value_id_to_align_items(value->to_identifier());
}

Optional<CSS::AlignSelf> StyleProperties::align_self() const
{
    auto value = property(CSS::PropertyID::AlignSelf);
    return value_id_to_align_self(value->to_identifier());
}

Optional<CSS::Appearance> StyleProperties::appearance() const
{
    auto value = property(CSS::PropertyID::Appearance);
    auto appearance = value_id_to_appearance(value->to_identifier());
    if (appearance.has_value()) {
        switch (*appearance) {
        // Note: All these compatibility values can be treated as 'auto'
        case CSS::Appearance::Textfield:
        case CSS::Appearance::MenulistButton:
        case CSS::Appearance::Searchfield:
        case CSS::Appearance::Textarea:
        case CSS::Appearance::PushButton:
        case CSS::Appearance::SliderHorizontal:
        case CSS::Appearance::Checkbox:
        case CSS::Appearance::Radio:
        case CSS::Appearance::SquareButton:
        case CSS::Appearance::Menulist:
        case CSS::Appearance::Listbox:
        case CSS::Appearance::Meter:
        case CSS::Appearance::ProgressBar:
        case CSS::Appearance::Button:
            appearance = CSS::Appearance::Auto;
            break;
        default:
            break;
        }
    }
    return appearance;
}

Optional<CSS::Position> StyleProperties::position() const
{
    auto value = property(CSS::PropertyID::Position);
    return value_id_to_position(value->to_identifier());
}

bool StyleProperties::operator==(StyleProperties const& other) const
{
    if (m_property_values.size() != other.m_property_values.size())
        return false;

    for (size_t i = 0; i < m_property_values.size(); ++i) {
        auto const& my_ptr = m_property_values[i];
        auto const& other_ptr = other.m_property_values[i];
        if (!my_ptr) {
            if (other_ptr)
                return false;
            continue;
        }
        if (!other_ptr)
            return false;
        auto const& my_value = *my_ptr;
        auto const& other_value = *other_ptr;
        if (my_value.type() != other_value.type())
            return false;
        if (my_value != other_value)
            return false;
    }

    return true;
}

Optional<CSS::TextAlign> StyleProperties::text_align() const
{
    auto value = property(CSS::PropertyID::TextAlign);
    return value_id_to_text_align(value->to_identifier());
}

Optional<CSS::TextJustify> StyleProperties::text_justify() const
{
    auto value = property(CSS::PropertyID::TextJustify);
    return value_id_to_text_justify(value->to_identifier());
}

Optional<CSS::PointerEvents> StyleProperties::pointer_events() const
{
    auto value = property(CSS::PropertyID::PointerEvents);
    return value_id_to_pointer_events(value->to_identifier());
}

Optional<CSS::WhiteSpace> StyleProperties::white_space() const
{
    auto value = property(CSS::PropertyID::WhiteSpace);
    return value_id_to_white_space(value->to_identifier());
}

Optional<CSS::LineStyle> StyleProperties::line_style(CSS::PropertyID property_id) const
{
    auto value = property(property_id);
    return value_id_to_line_style(value->to_identifier());
}

Optional<CSS::Float> StyleProperties::float_() const
{
    auto value = property(CSS::PropertyID::Float);
    return value_id_to_float(value->to_identifier());
}

Optional<CSS::Clear> StyleProperties::clear() const
{
    auto value = property(CSS::PropertyID::Clear);
    return value_id_to_clear(value->to_identifier());
}

CSS::ContentData StyleProperties::content() const
{
    auto value = property(CSS::PropertyID::Content);
    if (value->is_content()) {
        auto& content_style_value = value->as_content();

        CSS::ContentData content_data;

        // FIXME: The content is a list of things: strings, identifiers or functions that return strings, and images.
        //        So it can't always be represented as a single String, but may have to be multiple boxes.
        //        For now, we'll just assume strings since that is easiest.
        StringBuilder builder;
        for (auto const& item : content_style_value.content().values()) {
            if (item.is_string()) {
                builder.append(item.to_string());
            } else {
                // TODO: Implement quotes, counters, images, and other things.
            }
        }
        content_data.type = ContentData::Type::String;
        content_data.data = builder.to_string();

        if (content_style_value.has_alt_text()) {
            StringBuilder alt_text_builder;
            for (auto const& item : content_style_value.alt_text()->values()) {
                if (item.is_string()) {
                    alt_text_builder.append(item.to_string());
                } else {
                    // TODO: Implement counters
                }
            }
            content_data.alt_text = alt_text_builder.to_string();
        }

        return content_data;
    }

    switch (value->to_identifier()) {
    case ValueID::None:
        return { ContentData::Type::None };
    case ValueID::Normal:
        return { ContentData::Type::Normal };
    default:
        break;
    }

    return CSS::ContentData {};
}

Optional<CSS::Cursor> StyleProperties::cursor() const
{
    auto value = property(CSS::PropertyID::Cursor);
    return value_id_to_cursor(value->to_identifier());
}

Optional<CSS::Visibility> StyleProperties::visibility() const
{
    auto value = property(CSS::PropertyID::Visibility);
    if (!value->is_identifier())
        return {};
    return value_id_to_visibility(value->to_identifier());
}

CSS::Display StyleProperties::display() const
{
    auto value = property(CSS::PropertyID::Display);
    if (!value->is_identifier())
        return CSS::Display::from_short(CSS::Display::Short::Inline);
    switch (value->to_identifier()) {
    case CSS::ValueID::None:
        return CSS::Display::from_short(CSS::Display::Short::None);
    case CSS::ValueID::Block:
        return CSS::Display::from_short(CSS::Display::Short::Block);
    case CSS::ValueID::Inline:
        return CSS::Display::from_short(CSS::Display::Short::Inline);
    case CSS::ValueID::InlineBlock:
        return CSS::Display::from_short(CSS::Display::Short::InlineBlock);
    case CSS::ValueID::ListItem:
        return CSS::Display::from_short(CSS::Display::Short::ListItem);
    case CSS::ValueID::Table:
        return CSS::Display::from_short(CSS::Display::Short::Table);
    case CSS::ValueID::TableRow:
        return CSS::Display { CSS::Display::Internal::TableRow };
    case CSS::ValueID::TableCell:
        return CSS::Display { CSS::Display::Internal::TableCell };
    case CSS::ValueID::TableColumn:
        return CSS::Display { CSS::Display::Internal::TableColumn };
    case CSS::ValueID::TableColumnGroup:
        return CSS::Display { CSS::Display::Internal::TableColumnGroup };
    case CSS::ValueID::TableCaption:
        return CSS::Display { CSS::Display::Internal::TableCaption };
    case CSS::ValueID::TableRowGroup:
        return CSS::Display { CSS::Display::Internal::TableRowGroup };
    case CSS::ValueID::TableHeaderGroup:
        return CSS::Display { CSS::Display::Internal::TableHeaderGroup };
    case CSS::ValueID::TableFooterGroup:
        return CSS::Display { CSS::Display::Internal::TableFooterGroup };
    case CSS::ValueID::Flex:
        return CSS::Display::from_short(CSS::Display::Short::Flex);
    case CSS::ValueID::InlineFlex:
        return CSS::Display::from_short(CSS::Display::Short::InlineFlex);
    case CSS::ValueID::Grid:
        return CSS::Display::from_short(CSS::Display::Short::Grid);
    default:
        return CSS::Display::from_short(CSS::Display::Short::Block);
    }
}

Vector<CSS::TextDecorationLine> StyleProperties::text_decoration_line() const
{
    auto value = property(CSS::PropertyID::TextDecorationLine);

    if (value->is_value_list()) {
        Vector<CSS::TextDecorationLine> lines;
        auto& values = value->as_value_list().values();
        for (auto const& item : values) {
            lines.append(value_id_to_text_decoration_line(item.to_identifier()).value());
        }
        return lines;
    }

    if (value->is_identifier() && value->to_identifier() == ValueID::None)
        return {};

    VERIFY_NOT_REACHED();
}

Optional<CSS::TextDecorationStyle> StyleProperties::text_decoration_style() const
{
    auto value = property(CSS::PropertyID::TextDecorationStyle);
    return value_id_to_text_decoration_style(value->to_identifier());
}

Optional<CSS::TextTransform> StyleProperties::text_transform() const
{
    auto value = property(CSS::PropertyID::TextTransform);
    return value_id_to_text_transform(value->to_identifier());
}

Optional<CSS::ListStyleType> StyleProperties::list_style_type() const
{
    auto value = property(CSS::PropertyID::ListStyleType);
    return value_id_to_list_style_type(value->to_identifier());
}

Optional<CSS::Overflow> StyleProperties::overflow_x() const
{
    return overflow(CSS::PropertyID::OverflowX);
}

Optional<CSS::Overflow> StyleProperties::overflow_y() const
{
    return overflow(CSS::PropertyID::OverflowY);
}

Optional<CSS::Overflow> StyleProperties::overflow(CSS::PropertyID property_id) const
{
    auto value = property(property_id);
    return value_id_to_overflow(value->to_identifier());
}

Vector<ShadowData> StyleProperties::shadow(PropertyID property_id) const
{
    auto value = property(property_id);

    auto make_shadow_data = [](ShadowStyleValue const& value) {
        return ShadowData { value.color(), value.offset_x(), value.offset_y(), value.blur_radius(), value.spread_distance(), value.placement() };
    };

    if (value->is_value_list()) {
        auto& value_list = value->as_value_list();

        Vector<ShadowData> shadow_data;
        shadow_data.ensure_capacity(value_list.size());
        for (auto const& layer_value : value_list.values())
            shadow_data.append(make_shadow_data(layer_value.as_shadow()));

        return shadow_data;
    }

    if (value->is_shadow()) {
        auto& box = value->as_shadow();
        return { make_shadow_data(box) };
    }

    return {};
}

Vector<ShadowData> StyleProperties::box_shadow() const
{
    return shadow(PropertyID::BoxShadow);
}

Vector<ShadowData> StyleProperties::text_shadow() const
{
    return shadow(PropertyID::TextShadow);
}

Optional<CSS::BoxSizing> StyleProperties::box_sizing() const
{
    auto value = property(CSS::PropertyID::BoxSizing);
    return value_id_to_box_sizing(value->to_identifier());
}

Variant<CSS::VerticalAlign, CSS::LengthPercentage> StyleProperties::vertical_align() const
{
    auto value = property(CSS::PropertyID::VerticalAlign);

    if (value->is_identifier())
        return value_id_to_vertical_align(value->to_identifier()).release_value();

    if (value->is_length())
        return CSS::LengthPercentage(value->to_length());

    if (value->is_percentage())
        return CSS::LengthPercentage(value->as_percentage().percentage());

    VERIFY_NOT_REACHED();
}

Optional<CSS::FontVariant> StyleProperties::font_variant() const
{
    auto value = property(CSS::PropertyID::FontVariant);
    return value_id_to_font_variant(value->to_identifier());
}

Vector<CSS::GridTrackSize> StyleProperties::grid_template_columns() const
{
    auto value = property(CSS::PropertyID::GridTemplateColumns);
    return value->as_grid_track_size().grid_track_size();
}

Vector<CSS::GridTrackSize> StyleProperties::grid_template_rows() const
{
    auto value = property(CSS::PropertyID::GridTemplateRows);
    return value->as_grid_track_size().grid_track_size();
}

CSS::GridTrackPlacement StyleProperties::grid_column_end() const
{
    auto value = property(CSS::PropertyID::GridColumnEnd);
    return value->as_grid_track_placement().grid_track_placement();
}

CSS::GridTrackPlacement StyleProperties::grid_column_start() const
{
    auto value = property(CSS::PropertyID::GridColumnStart);
    return value->as_grid_track_placement().grid_track_placement();
}

CSS::GridTrackPlacement StyleProperties::grid_row_end() const
{
    auto value = property(CSS::PropertyID::GridRowEnd);
    return value->as_grid_track_placement().grid_track_placement();
}

CSS::GridTrackPlacement StyleProperties::grid_row_start() const
{
    auto value = property(CSS::PropertyID::GridRowStart);
    return value->as_grid_track_placement().grid_track_placement();
}

}
