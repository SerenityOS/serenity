/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/TypeCasts.h>
#include <LibCore/DirIterator.h>
#include <LibGfx/FontDatabase.h>
#include <LibWeb/CSS/StyleProperties.h>
#include <LibWeb/FontCache.h>
#include <ctype.h>

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
    m_property_values.set((unsigned)id, move(value));
}

void StyleProperties::set_property(CSS::PropertyID id, const StringView& value)
{
    m_property_values.set((unsigned)id, StringStyleValue::create(value));
}

Optional<NonnullRefPtr<StyleValue>> StyleProperties::property(CSS::PropertyID id) const
{
    auto it = m_property_values.find((unsigned)id);
    if (it == m_property_values.end())
        return {};
    return it->value;
}

Length StyleProperties::length_or_fallback(CSS::PropertyID id, const Length& fallback) const
{
    auto value = property(id);
    if (!value.has_value())
        return fallback;
    return value.value()->to_length();
}

LengthBox StyleProperties::length_box(CSS::PropertyID left_id, CSS::PropertyID top_id, CSS::PropertyID right_id, CSS::PropertyID bottom_id, const CSS::Length& default_value) const
{
    LengthBox box;
    box.left = length_or_fallback(left_id, default_value);
    box.top = length_or_fallback(top_id, default_value);
    box.right = length_or_fallback(right_id, default_value);
    box.bottom = length_or_fallback(bottom_id, default_value);
    return box;
}

String StyleProperties::string_or_fallback(CSS::PropertyID id, const StringView& fallback) const
{
    auto value = property(id);
    if (!value.has_value())
        return fallback;
    return value.value()->to_string();
}

Color StyleProperties::color_or_fallback(CSS::PropertyID id, const DOM::Document& document, Color fallback) const
{
    auto value = property(id);
    if (!value.has_value())
        return fallback;
    return value.value()->to_color(document);
}

void StyleProperties::load_font() const
{
    auto family_value = string_or_fallback(CSS::PropertyID::FontFamily, "Katica");
    auto font_size = property(CSS::PropertyID::FontSize).value_or(IdentifierStyleValue::create(CSS::ValueID::Medium));
    auto font_weight = property(CSS::PropertyID::FontWeight).value_or(IdentifierStyleValue::create(CSS::ValueID::Normal));

    auto family_parts = family_value.split(',');
    auto family = family_parts[0];

    auto monospace = false;
    auto bold = false;

    if (family.is_one_of("monospace", "ui-monospace")) {
        monospace = true;
        family = "Csilla";
    } else if (family.is_one_of("serif", "sans-serif", "cursive", "fantasy", "ui-serif", "ui-sans-serif", "ui-rounded")) {
        family = "Katica";
    }

    int weight = 400;
    if (font_weight->is_identifier()) {
        switch (static_cast<const IdentifierStyleValue&>(*font_weight).id()) {
        case CSS::ValueID::Normal:
            weight = 400;
            break;
        case CSS::ValueID::Bold:
            weight = 700;
            break;
        case CSS::ValueID::Lighter:
            // FIXME: This should be relative to the parent.
            weight = 400;
            break;
        case CSS::ValueID::Bolder:
            // FIXME: This should be relative to the parent.
            weight = 700;
            break;
        default:
            break;
        }
    } else if (font_weight->is_length()) {
        // FIXME: This isn't really a length, it's a numeric value..
        int font_weight_integer = font_weight->to_length().raw_value();
        if (font_weight_integer <= 400)
            weight = 400;
        if (font_weight_integer <= 700)
            weight = 700;
        weight = 900;
    }

    bold = weight > 400;

    int size = 10;
    if (font_size->is_identifier()) {
        switch (static_cast<const IdentifierStyleValue&>(*font_size).id()) {
        case CSS::ValueID::XxSmall:
        case CSS::ValueID::XSmall:
        case CSS::ValueID::Small:
        case CSS::ValueID::Medium:
            // FIXME: Should be based on "user's default font size"
            size = 10;
            break;
        case CSS::ValueID::Large:
        case CSS::ValueID::XLarge:
        case CSS::ValueID::XxLarge:
        case CSS::ValueID::XxxLarge:
            // FIXME: Should be based on "user's default font size"
            size = 12;
            break;
        case CSS::ValueID::Smaller:
            // FIXME: This should be relative to the parent.
            size = 10;
            break;
        case CSS::ValueID::Larger:
            // FIXME: This should be relative to the parent.
            size = 12;
            break;

        default:
            break;
        }
    } else if (font_size->is_length()) {
        // FIXME: This isn't really a length, it's a numeric value..
        int font_size_integer = font_size->to_length().raw_value();
        size = font_size_integer;
    }

    FontSelector font_selector { family, size, weight };

    auto found_font = FontCache::the().get(font_selector);
    if (found_font) {
        m_font = found_font;
        return;
    }

    Gfx::FontDatabase::the().for_each_font([&](auto& font) {
        if (font.family() == family && font.weight() == weight && font.presentation_size() == size)
            found_font = font;
    });

    if (!found_font) {
        dbgln("Font not found: '{}' {} {}", family, size, weight);
        found_font = font_fallback(monospace, bold);
    }

    m_font = found_font;
    FontCache::the().set(font_selector, *m_font);
}

RefPtr<Gfx::Font> StyleProperties::font_fallback(bool monospace, bool bold) const
{
    if (monospace && bold)
        return Gfx::FontDatabase::default_fixed_width_font().bold_variant();

    if (monospace)
        return Gfx::FontDatabase::default_fixed_width_font();

    if (bold)
        return Gfx::FontDatabase::default_font().bold_variant();

    return Gfx::FontDatabase::default_font();
}

float StyleProperties::line_height(const Layout::Node& layout_node) const
{
    auto line_height_length = length_or_fallback(CSS::PropertyID::LineHeight, Length::make_auto());
    if (line_height_length.is_absolute())
        return (float)line_height_length.to_px(layout_node);
    return (float)font().glyph_height() * 1.4f;
}

Optional<int> StyleProperties::z_index() const
{
    auto value = property(CSS::PropertyID::ZIndex);
    if (!value.has_value())
        return {};
    return static_cast<int>(value.value()->to_length().raw_value());
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
    auto value = property(CSS::PropertyID::FlexBasis);
    if (!value.has_value())
        return {};

    if (value.value()->is_identifier() && value.value()->to_identifier() == CSS::ValueID::Content)
        return { { CSS::FlexBasis::Content, {} } };

    if (value.value()->is_length())
        return { { CSS::FlexBasis::Length, value.value()->to_length() } };

    return {};
}

Optional<float> StyleProperties::flex_grow_factor() const
{
    auto value = property(CSS::PropertyID::FlexGrow);
    if (!value.has_value())
        return {};
    if (value.value()->is_length() && downcast<CSS::LengthStyleValue>(value.value().ptr())->to_length().raw_value() == 0)
        return { 0 };
    if (!value.value()->is_numeric())
        return {};
    auto numeric = downcast<CSS::NumericStyleValue>(value.value().ptr());
    return numeric->value();
}

Optional<float> StyleProperties::flex_shrink_factor() const
{
    auto value = property(CSS::PropertyID::FlexShrink);
    if (!value.has_value())
        return {};
    if (value.value()->is_length() && downcast<CSS::LengthStyleValue>(value.value().ptr())->to_length().raw_value() == 0)
        return { 0 };
    if (!value.value()->is_numeric())
        return {};
    auto numeric = downcast<CSS::NumericStyleValue>(value.value().ptr());
    return numeric->value();
}

Optional<CSS::Position> StyleProperties::position() const
{
    auto value = property(CSS::PropertyID::Position);
    if (!value.has_value() || !value.value()->is_identifier())
        return {};
    switch (static_cast<const IdentifierStyleValue&>(*value.value()).id()) {
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
    if (!value.has_value() || !value.value()->is_identifier())
        return {};

    switch (static_cast<const IdentifierStyleValue&>(*value.value()).id()) {
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

Optional<CSS::WhiteSpace> StyleProperties::white_space() const
{
    auto value = property(CSS::PropertyID::WhiteSpace);
    if (!value.has_value() || !value.value()->is_identifier())
        return {};
    switch (static_cast<const IdentifierStyleValue&>(*value.value()).id()) {
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
    if (!value.has_value() || !value.value()->is_identifier())
        return {};
    switch (static_cast<const IdentifierStyleValue&>(*value.value()).id()) {
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
    if (!value.has_value() || !value.value()->is_identifier())
        return {};
    switch (static_cast<const IdentifierStyleValue&>(*value.value()).id()) {
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
    if (!value.has_value() || !value.value()->is_identifier())
        return {};
    switch (static_cast<const IdentifierStyleValue&>(*value.value()).id()) {
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
    if (!value.has_value() || !value.value()->is_identifier())
        return {};
    switch (static_cast<const IdentifierStyleValue&>(*value.value()).id()) {
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
        return CSS::Display::Inline;
    switch (static_cast<const IdentifierStyleValue&>(*value.value()).id()) {
    case CSS::ValueID::None:
        return CSS::Display::None;
    case CSS::ValueID::Block:
        return CSS::Display::Block;
    case CSS::ValueID::Inline:
        return CSS::Display::Inline;
    case CSS::ValueID::InlineBlock:
        return CSS::Display::InlineBlock;
    case CSS::ValueID::ListItem:
        return CSS::Display::ListItem;
    case CSS::ValueID::Table:
        return CSS::Display::Table;
    case CSS::ValueID::TableRow:
        return CSS::Display::TableRow;
    case CSS::ValueID::TableCell:
        return CSS::Display::TableCell;
    case CSS::ValueID::TableColumn:
        return CSS::Display::TableColumn;
    case CSS::ValueID::TableColumnGroup:
        return CSS::Display::TableColumnGroup;
    case CSS::ValueID::TableCaption:
        return CSS::Display::TableCaption;
    case CSS::ValueID::TableRowGroup:
        return CSS::Display::TableRowGroup;
    case CSS::ValueID::TableHeaderGroup:
        return CSS::Display::TableHeaderGroup;
    case CSS::ValueID::TableFooterGroup:
        return CSS::Display::TableFooterGroup;
    case CSS::ValueID::Flex:
        return CSS::Display::Flex;
    default:
        return CSS::Display::Block;
    }
}

Optional<CSS::TextDecorationLine> StyleProperties::text_decoration_line() const
{
    auto value = property(CSS::PropertyID::TextDecorationLine);
    if (!value.has_value() || !value.value()->is_identifier())
        return {};
    switch (static_cast<const IdentifierStyleValue&>(*value.value()).id()) {
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

Optional<CSS::Repeat> StyleProperties::background_repeat_x() const
{
    auto value = property(CSS::PropertyID::BackgroundRepeatX);
    if (!value.has_value())
        return {};

    switch (value.value()->to_identifier()) {
    case CSS::ValueID::NoRepeat:
        return CSS::Repeat::NoRepeat;
    case CSS::ValueID::Repeat:
        return CSS::Repeat::Repeat;
    case CSS::ValueID::Round:
        return CSS::Repeat::Round;
    case CSS::ValueID::Space:
        return CSS::Repeat::Space;
    default:
        return {};
    }
}

Optional<CSS::Repeat> StyleProperties::background_repeat_y() const
{
    auto value = property(CSS::PropertyID::BackgroundRepeatY);
    if (!value.has_value())
        return {};

    switch (value.value()->to_identifier()) {
    case CSS::ValueID::NoRepeat:
        return CSS::Repeat::NoRepeat;
    case CSS::ValueID::Repeat:
        return CSS::Repeat::Repeat;
    case CSS::ValueID::Round:
        return CSS::Repeat::Round;
    case CSS::ValueID::Space:
        return CSS::Repeat::Space;
    default:
        return {};
    }
}
}
