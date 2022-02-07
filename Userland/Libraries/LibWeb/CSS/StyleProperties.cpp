/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibCore/DirIterator.h>
#include <LibGfx/FontDatabase.h>
#include <LibWeb/CSS/StyleProperties.h>
#include <LibWeb/FontCache.h>
#include <LibWeb/Layout/BlockContainer.h>
#include <LibWeb/Layout/Node.h>

namespace Web::CSS {

StyleProperties::StyleProperties()
{
}

StyleProperties::StyleProperties(const StyleProperties& other)
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
    m_property_values.set(id, move(value));
}

Optional<NonnullRefPtr<StyleValue>> StyleProperties::property(CSS::PropertyID property_id) const
{
    auto it = m_property_values.find(property_id);
    if (it == m_property_values.end())
        return {};
    return it->value;
}

Length StyleProperties::length_or_fallback(CSS::PropertyID id, Length const& fallback) const
{
    auto maybe_value = property(id);
    if (!maybe_value.has_value())
        return fallback;
    auto& value = maybe_value.value();

    if (value->is_calculated())
        return Length::make_calculated(value->as_calculated());

    if (value->has_length())
        return value->to_length();

    return fallback;
}

LengthPercentage StyleProperties::length_percentage_or_fallback(CSS::PropertyID id, LengthPercentage const& fallback) const
{
    auto maybe_value = property(id);
    if (!maybe_value.has_value())
        return fallback;
    auto& value = maybe_value.value();

    if (value->is_calculated())
        return LengthPercentage { value->as_calculated() };

    if (value->is_percentage())
        return value->as_percentage().percentage();

    if (value->has_length())
        return value->to_length();

    return fallback;
}

LengthBox StyleProperties::length_box(CSS::PropertyID left_id, CSS::PropertyID top_id, CSS::PropertyID right_id, CSS::PropertyID bottom_id, const CSS::Length& default_value) const
{
    LengthBox box;
    box.left = length_percentage_or_fallback(left_id, default_value);
    box.top = length_percentage_or_fallback(top_id, default_value);
    box.right = length_percentage_or_fallback(right_id, default_value);
    box.bottom = length_percentage_or_fallback(bottom_id, default_value);
    return box;
}

Color StyleProperties::color_or_fallback(CSS::PropertyID id, Layout::NodeWithStyle const& node, Color fallback) const
{
    auto value = property(id);
    if (!value.has_value() || !value.value()->has_color())
        return fallback;
    return value.value()->to_color(node);
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
    constexpr float font_height_to_line_height_multiplier = 1.4f;

    if (auto maybe_line_height = property(CSS::PropertyID::LineHeight); maybe_line_height.has_value()) {
        auto line_height = maybe_line_height.release_value();

        if (line_height->is_identifier() && line_height->to_identifier() == ValueID::Normal)
            return Length(1, Length::Type::Em).to_px(layout_node) * font_height_to_line_height_multiplier;

        if (line_height->is_length()) {
            auto line_height_length = line_height->to_length();
            if (!line_height_length.is_undefined_or_auto())
                return line_height_length.to_px(layout_node);
        }

        if (line_height->is_numeric())
            return Length(line_height->to_number(), Length::Type::Em).to_px(layout_node);

        if (line_height->is_percentage()) {
            // Percentages are relative to 1em. https://www.w3.org/TR/css-inline-3/#valdef-line-height-percentage
            auto& percentage = line_height->as_percentage().percentage();
            return Length(percentage.as_fraction(), Length::Type::Em).to_px(layout_node);
        }
    }

    return Length(font_height_to_line_height_multiplier, Length::Type::Em).to_px(layout_node);
}

Optional<int> StyleProperties::z_index() const
{
    auto maybe_value = property(CSS::PropertyID::ZIndex);
    if (!maybe_value.has_value())
        return {};
    auto& value = maybe_value.value();

    if (value->has_auto())
        return 0;
    if (value->has_integer())
        return value->to_integer();
    return {};
}

float StyleProperties::opacity() const
{
    auto maybe_value = property(CSS::PropertyID::Opacity);
    if (!maybe_value.has_value())
        return 1.0f;
    auto& value = maybe_value.value();

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
    if (!value.has_value())
        return {};
    switch (value.value()->to_identifier()) {
    case CSS::ValueID::Row:
        return CSS::FlexDirection::Row;
    case CSS::ValueID::RowReverse:
        return CSS::FlexDirection::RowReverse;
    case CSS::ValueID::Column:
        return CSS::FlexDirection::Column;
    case CSS::ValueID::ColumnReverse:
        return CSS::FlexDirection::ColumnReverse;
    default:
        return {};
    }
}

Optional<CSS::FlexWrap> StyleProperties::flex_wrap() const
{
    auto value = property(CSS::PropertyID::FlexWrap);
    if (!value.has_value())
        return {};
    switch (value.value()->to_identifier()) {
    case CSS::ValueID::Wrap:
        return CSS::FlexWrap::Wrap;
    case CSS::ValueID::Nowrap:
        return CSS::FlexWrap::Nowrap;
    case CSS::ValueID::WrapReverse:
        return CSS::FlexWrap::WrapReverse;
    default:
        return {};
    }
}

Optional<CSS::FlexBasisData> StyleProperties::flex_basis() const
{
    auto maybe_value = property(CSS::PropertyID::FlexBasis);
    if (!maybe_value.has_value())
        return {};
    auto& value = maybe_value.value();

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
    if (!value.has_value() || !value.value()->has_number())
        return 0;
    return value.value()->to_number();
}

float StyleProperties::flex_shrink() const
{
    auto value = property(CSS::PropertyID::FlexShrink);
    if (!value.has_value() || !value.value()->has_number())
        return 1;
    return value.value()->to_number();
}

Optional<CSS::JustifyContent> StyleProperties::justify_content() const
{
    auto value = property(CSS::PropertyID::JustifyContent);
    if (!value.has_value())
        return {};
    switch (value.value()->to_identifier()) {
    case CSS::ValueID::FlexStart:
        return CSS::JustifyContent::FlexStart;
    case CSS::ValueID::FlexEnd:
        return CSS::JustifyContent::FlexEnd;
    case CSS::ValueID::Center:
        return CSS::JustifyContent::Center;
    case CSS::ValueID::SpaceBetween:
        return CSS::JustifyContent::SpaceBetween;
    case CSS::ValueID::SpaceAround:
        return CSS::JustifyContent::SpaceAround;
    default:
        return {};
    }
}

Vector<CSS::Transformation> StyleProperties::transformations() const
{
    auto value = property(CSS::PropertyID::Transform);
    if (!value.has_value())
        return {};

    if (value.value()->is_identifier() && value.value()->to_identifier() == CSS::ValueID::None)
        return {};

    if (!value.value()->is_value_list())
        return {};

    auto& list = value.value()->as_value_list();

    Vector<CSS::Transformation> transformations;

    for (auto& it : list.values()) {
        if (!it.is_transformation())
            return {};
        auto& transformation_style_value = it.as_transformation();
        CSS::Transformation transformation;
        transformation.function = transformation_style_value.transform_function();
        Vector<Variant<CSS::Length, float>> values;
        for (auto& transformation_value : transformation_style_value.values()) {
            if (transformation_value.is_length()) {
                values.append({ transformation_value.to_length() });
            } else if (transformation_value.is_numeric()) {
                values.append({ transformation_value.to_number() });
            } else {
                dbgln("FIXME: Unsupported value in transform!");
            }
        }
        transformation.values = move(values);
        transformations.append(move(transformation));
    }
    return transformations;
}

Optional<CSS::AlignItems> StyleProperties::align_items() const
{
    auto value = property(CSS::PropertyID::AlignItems);
    if (!value.has_value())
        return {};
    switch (value.value()->to_identifier()) {
    case CSS::ValueID::FlexStart:
        return CSS::AlignItems::FlexStart;
    case CSS::ValueID::FlexEnd:
        return CSS::AlignItems::FlexEnd;
    case CSS::ValueID::Center:
        return CSS::AlignItems::Center;
    case CSS::ValueID::Baseline:
        return CSS::AlignItems::Baseline;
    case CSS::ValueID::Stretch:
        return CSS::AlignItems::Stretch;
    default:
        return {};
    }
}

Optional<CSS::Position> StyleProperties::position() const
{
    auto value = property(CSS::PropertyID::Position);
    if (!value.has_value())
        return {};
    switch (value.value()->to_identifier()) {
    case CSS::ValueID::Static:
        return CSS::Position::Static;
    case CSS::ValueID::Relative:
        return CSS::Position::Relative;
    case CSS::ValueID::Absolute:
        return CSS::Position::Absolute;
    case CSS::ValueID::Fixed:
        return CSS::Position::Fixed;
    case CSS::ValueID::Sticky:
        return CSS::Position::Sticky;
    default:
        return {};
    }
}

bool StyleProperties::operator==(const StyleProperties& other) const
{
    if (m_property_values.size() != other.m_property_values.size())
        return false;

    for (auto& it : m_property_values) {
        auto jt = other.m_property_values.find(it.key);
        if (jt == other.m_property_values.end())
            return false;
        auto& my_value = *it.value;
        auto& other_value = *jt->value;
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
    if (!value.has_value())
        return {};
    switch (value.value()->to_identifier()) {
    case CSS::ValueID::Left:
        return CSS::TextAlign::Left;
    case CSS::ValueID::Center:
        return CSS::TextAlign::Center;
    case CSS::ValueID::Right:
        return CSS::TextAlign::Right;
    case CSS::ValueID::Justify:
        return CSS::TextAlign::Justify;
    case CSS::ValueID::LibwebCenter:
        return CSS::TextAlign::LibwebCenter;
    default:
        return {};
    }
}

Optional<CSS::PointerEvents> StyleProperties::pointer_events() const
{
    auto value = property(CSS::PropertyID::PointerEvents);
    if (!value.has_value())
        return {};

    switch (value.value()->to_identifier()) {
    case CSS::ValueID::Auto:
        return CSS::PointerEvents::Auto;
    case CSS::ValueID::All:
        return CSS::PointerEvents::All;
    case CSS::ValueID::None:
        return CSS::PointerEvents::None;
    default:
        return {};
    }
}

Optional<CSS::WhiteSpace> StyleProperties::white_space() const
{
    auto value = property(CSS::PropertyID::WhiteSpace);
    if (!value.has_value())
        return {};
    switch (value.value()->to_identifier()) {
    case CSS::ValueID::Normal:
        return CSS::WhiteSpace::Normal;
    case CSS::ValueID::Nowrap:
        return CSS::WhiteSpace::Nowrap;
    case CSS::ValueID::Pre:
        return CSS::WhiteSpace::Pre;
    case CSS::ValueID::PreLine:
        return CSS::WhiteSpace::PreLine;
    case CSS::ValueID::PreWrap:
        return CSS::WhiteSpace::PreWrap;
    default:
        return {};
    }
}

Optional<CSS::LineStyle> StyleProperties::line_style(CSS::PropertyID property_id) const
{
    auto value = property(property_id);
    if (!value.has_value())
        return {};
    switch (value.value()->to_identifier()) {
    case CSS::ValueID::None:
        return CSS::LineStyle::None;
    case CSS::ValueID::Hidden:
        return CSS::LineStyle::Hidden;
    case CSS::ValueID::Dotted:
        return CSS::LineStyle::Dotted;
    case CSS::ValueID::Dashed:
        return CSS::LineStyle::Dashed;
    case CSS::ValueID::Solid:
        return CSS::LineStyle::Solid;
    case CSS::ValueID::Double:
        return CSS::LineStyle::Double;
    case CSS::ValueID::Groove:
        return CSS::LineStyle::Groove;
    case CSS::ValueID::Ridge:
        return CSS::LineStyle::Ridge;
    case CSS::ValueID::Inset:
        return CSS::LineStyle::Inset;
    case CSS::ValueID::Outset:
        return CSS::LineStyle::Outset;
    default:
        return {};
    }
}

Optional<CSS::Float> StyleProperties::float_() const
{
    auto value = property(CSS::PropertyID::Float);
    if (!value.has_value())
        return {};
    switch (value.value()->to_identifier()) {
    case CSS::ValueID::None:
        return CSS::Float::None;
    case CSS::ValueID::Left:
        return CSS::Float::Left;
    case CSS::ValueID::Right:
        return CSS::Float::Right;
    default:
        return {};
    }
}

Optional<CSS::Clear> StyleProperties::clear() const
{
    auto value = property(CSS::PropertyID::Clear);
    if (!value.has_value())
        return {};
    switch (value.value()->to_identifier()) {
    case CSS::ValueID::None:
        return CSS::Clear::None;
    case CSS::ValueID::Left:
        return CSS::Clear::Left;
    case CSS::ValueID::Right:
        return CSS::Clear::Right;
    case CSS::ValueID::Both:
        return CSS::Clear::Both;
    default:
        return {};
    }
}

Optional<CSS::Cursor> StyleProperties::cursor() const
{
    auto value = property(CSS::PropertyID::Cursor);
    if (!value.has_value())
        return {};
    switch (value.value()->to_identifier()) {
    case CSS::ValueID::Auto:
        return CSS::Cursor::Auto;
    case CSS::ValueID::Default:
        return CSS::Cursor::Default;
    case CSS::ValueID::None:
        return CSS::Cursor::None;
    case CSS::ValueID::ContextMenu:
        return CSS::Cursor::ContextMenu;
    case CSS::ValueID::Help:
        return CSS::Cursor::Help;
    case CSS::ValueID::Pointer:
        return CSS::Cursor::Pointer;
    case CSS::ValueID::Progress:
        return CSS::Cursor::Progress;
    case CSS::ValueID::Wait:
        return CSS::Cursor::Wait;
    case CSS::ValueID::Cell:
        return CSS::Cursor::Cell;
    case CSS::ValueID::Crosshair:
        return CSS::Cursor::Crosshair;
    case CSS::ValueID::Text:
        return CSS::Cursor::Text;
    case CSS::ValueID::VerticalText:
        return CSS::Cursor::VerticalText;
    case CSS::ValueID::Alias:
        return CSS::Cursor::Alias;
    case CSS::ValueID::Copy:
        return CSS::Cursor::Copy;
    case CSS::ValueID::Move:
        return CSS::Cursor::Move;
    case CSS::ValueID::NoDrop:
        return CSS::Cursor::NoDrop;
    case CSS::ValueID::NotAllowed:
        return CSS::Cursor::NotAllowed;
    case CSS::ValueID::Grab:
        return CSS::Cursor::Grab;
    case CSS::ValueID::Grabbing:
        return CSS::Cursor::Grabbing;
    case CSS::ValueID::EResize:
        return CSS::Cursor::EResize;
    case CSS::ValueID::NResize:
        return CSS::Cursor::NResize;
    case CSS::ValueID::NeResize:
        return CSS::Cursor::NeResize;
    case CSS::ValueID::NwResize:
        return CSS::Cursor::NwResize;
    case CSS::ValueID::SResize:
        return CSS::Cursor::SResize;
    case CSS::ValueID::SeResize:
        return CSS::Cursor::SeResize;
    case CSS::ValueID::SwResize:
        return CSS::Cursor::SwResize;
    case CSS::ValueID::WResize:
        return CSS::Cursor::WResize;
    case CSS::ValueID::EwResize:
        return CSS::Cursor::EwResize;
    case CSS::ValueID::NsResize:
        return CSS::Cursor::NsResize;
    case CSS::ValueID::NeswResize:
        return CSS::Cursor::NeswResize;
    case CSS::ValueID::NwseResize:
        return CSS::Cursor::NwseResize;
    case CSS::ValueID::ColResize:
        return CSS::Cursor::ColResize;
    case CSS::ValueID::RowResize:
        return CSS::Cursor::RowResize;
    case CSS::ValueID::AllScroll:
        return CSS::Cursor::AllScroll;
    case CSS::ValueID::ZoomIn:
        return CSS::Cursor::ZoomIn;
    case CSS::ValueID::ZoomOut:
        return CSS::Cursor::ZoomOut;
    default:
        return {};
    }
}

CSS::Display StyleProperties::display() const
{
    auto value = property(CSS::PropertyID::Display);
    if (!value.has_value() || !value.value()->is_identifier())
        return CSS::Display::from_short(CSS::Display::Short::Inline);
    switch (value.value()->to_identifier()) {
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
    default:
        return CSS::Display::from_short(CSS::Display::Short::Block);
    }
}

Optional<CSS::TextDecorationLine> StyleProperties::text_decoration_line() const
{
    auto value = property(CSS::PropertyID::TextDecorationLine);
    if (!value.has_value())
        return {};
    switch (value.value()->to_identifier()) {
    case CSS::ValueID::None:
        return CSS::TextDecorationLine::None;
    case CSS::ValueID::Underline:
        return CSS::TextDecorationLine::Underline;
    case CSS::ValueID::Overline:
        return CSS::TextDecorationLine::Overline;
    case CSS::ValueID::LineThrough:
        return CSS::TextDecorationLine::LineThrough;
    case CSS::ValueID::Blink:
        return CSS::TextDecorationLine::Blink;
    default:
        return {};
    }
}

Optional<CSS::TextDecorationStyle> StyleProperties::text_decoration_style() const
{
    auto value = property(CSS::PropertyID::TextDecorationStyle);
    if (!value.has_value())
        return {};
    switch (value.value()->to_identifier()) {
    case CSS::ValueID::Solid:
        return CSS::TextDecorationStyle::Solid;
    case CSS::ValueID::Double:
        return CSS::TextDecorationStyle::Double;
    case CSS::ValueID::Dotted:
        return CSS::TextDecorationStyle::Dotted;
    case CSS::ValueID::Dashed:
        return CSS::TextDecorationStyle::Dashed;
    case CSS::ValueID::Wavy:
        return CSS::TextDecorationStyle::Wavy;
    default:
        return {};
    }
}

Optional<CSS::TextTransform> StyleProperties::text_transform() const
{
    auto value = property(CSS::PropertyID::TextTransform);
    if (!value.has_value())
        return {};
    switch (value.value()->to_identifier()) {
    case CSS::ValueID::None:
        return CSS::TextTransform::None;
    case CSS::ValueID::Lowercase:
        return CSS::TextTransform::Lowercase;
    case CSS::ValueID::Uppercase:
        return CSS::TextTransform::Uppercase;
    case CSS::ValueID::Capitalize:
        return CSS::TextTransform::Capitalize;
    case CSS::ValueID::FullWidth:
        return CSS::TextTransform::FullWidth;
    case CSS::ValueID::FullSizeKana:
        return CSS::TextTransform::FullSizeKana;
    default:
        return {};
    }
}

Optional<CSS::ListStyleType> StyleProperties::list_style_type() const
{
    auto value = property(CSS::PropertyID::ListStyleType);
    if (!value.has_value())
        return {};
    switch (value.value()->to_identifier()) {
    case CSS::ValueID::None:
        return CSS::ListStyleType::None;
    case CSS::ValueID::Disc:
        return CSS::ListStyleType::Disc;
    case CSS::ValueID::Circle:
        return CSS::ListStyleType::Circle;
    case CSS::ValueID::Square:
        return CSS::ListStyleType::Square;
    case CSS::ValueID::Decimal:
        return CSS::ListStyleType::Decimal;
    case CSS::ValueID::DecimalLeadingZero:
        return CSS::ListStyleType::DecimalLeadingZero;
    case CSS::ValueID::LowerAlpha:
        return CSS::ListStyleType::LowerAlpha;
    case CSS::ValueID::LowerLatin:
        return CSS::ListStyleType::LowerLatin;
    case CSS::ValueID::UpperAlpha:
        return CSS::ListStyleType::UpperAlpha;
    case CSS::ValueID::UpperLatin:
        return CSS::ListStyleType::UpperLatin;
    case CSS::ValueID::UpperRoman:
        return CSS::ListStyleType::UpperRoman;
    case CSS::ValueID::LowerRoman:
        return CSS::ListStyleType::LowerRoman;
    default:
        return {};
    }
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
    if (!value.has_value())
        return {};
    switch (value.value()->to_identifier()) {
    case CSS::ValueID::Auto:
        return CSS::Overflow::Auto;
    case CSS::ValueID::Visible:
        return CSS::Overflow::Visible;
    case CSS::ValueID::Hidden:
        return CSS::Overflow::Hidden;
    case CSS::ValueID::Clip:
        return CSS::Overflow::Clip;
    case CSS::ValueID::Scroll:
        return CSS::Overflow::Scroll;
    default:
        return {};
    }
}

Optional<CSS::BoxShadowData> StyleProperties::box_shadow() const
{
    auto value_or_error = property(CSS::PropertyID::BoxShadow);
    if (!value_or_error.has_value())
        return {};

    auto value = value_or_error.value();
    if (!value->is_box_shadow())
        return {};

    auto& box = value->as_box_shadow();
    return { { box.offset_x(), box.offset_y(), box.blur_radius(), box.color() } };
}

CSS::BoxSizing StyleProperties::box_sizing() const
{
    auto value = property(CSS::PropertyID::BoxSizing);
    if (!value.has_value())
        return {};

    switch (value.value()->to_identifier()) {
    case CSS::ValueID::BorderBox:
        return CSS::BoxSizing::BorderBox;
    case CSS::ValueID::ContentBox:
        return CSS::BoxSizing::ContentBox;
    default:
        return {};
    }
}
}
