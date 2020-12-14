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
    return adopt(*new StyleProperties(*this));
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

LengthBox StyleProperties::length_box(CSS::PropertyID left_id, CSS::PropertyID top_id, CSS::PropertyID right_id, CSS::PropertyID bottom_id) const
{
    LengthBox box;
    box.left = length_or_fallback(left_id, CSS::Length::make_auto());
    box.top = length_or_fallback(top_id, CSS::Length::make_auto());
    box.right = length_or_fallback(right_id, CSS::Length::make_auto());
    box.bottom = length_or_fallback(bottom_id, CSS::Length::make_auto());
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

    if (family.is_one_of("monospace", "ui-monospace"))
        family = "Csilla";
    else if (family.is_one_of("serif", "sans-serif", "cursive", "fantasy", "ui-serif", "ui-sans-serif", "ui-rounded"))
        family = "Katica";

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
        if (font_size_integer <= 10)
            size = 10;
        else if (font_size_integer <= 12)
            size = 12;
        else
            size = 14;
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
        found_font = Gfx::Font::default_font();
    }

    m_font = found_font;
    FontCache::the().set(font_selector, *m_font);
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

CSS::Position StyleProperties::position() const
{
    if (property(CSS::PropertyID::Position).has_value()) {
        String position_string = string_or_fallback(CSS::PropertyID::Position, "static");
        if (position_string == "relative")
            return CSS::Position::Relative;
        if (position_string == "absolute")
            return CSS::Position::Absolute;
        if (position_string == "sticky")
            return CSS::Position::Sticky;
        if (position_string == "fixed")
            return CSS::Position::Fixed;
    }
    return CSS::Position::Static;
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

CSS::TextAlign StyleProperties::text_align() const
{
    auto string = string_or_fallback(CSS::PropertyID::TextAlign, "left");
    if (string == "center")
        return CSS::TextAlign::Center;
    if (string == "right")
        return CSS::TextAlign::Right;
    if (string == "justify")
        return CSS::TextAlign::Justify;
    if (string == "-libweb-center")
        return CSS::TextAlign::VendorSpecificCenter;
    // Otherwise, just assume "left"..
    return CSS::TextAlign::Left;
}

Optional<CSS::WhiteSpace> StyleProperties::white_space() const
{
    auto value = property(CSS::PropertyID::WhiteSpace);
    if (!value.has_value() || !value.value()->is_string())
        return {};
    auto string = value.value()->to_string();
    if (string == "normal")
        return CSS::WhiteSpace::Normal;
    if (string == "nowrap")
        return CSS::WhiteSpace::Nowrap;
    if (string == "pre")
        return CSS::WhiteSpace::Pre;
    if (string == "pre-line")
        return CSS::WhiteSpace::PreLine;
    if (string == "pre-wrap")
        return CSS::WhiteSpace::PreWrap;
    return {};
}

Optional<CSS::LineStyle> StyleProperties::line_style(CSS::PropertyID property_id) const
{
    auto value = property(property_id);
    if (!value.has_value() || !value.value()->is_string())
        return {};
    auto string = value.value()->to_string();
    if (string == "none")
        return CSS::LineStyle::None;
    if (string == "hidden")
        return CSS::LineStyle::Hidden;
    if (string == "dotted")
        return CSS::LineStyle::Dotted;
    if (string == "dashed")
        return CSS::LineStyle::Dashed;
    if (string == "solid")
        return CSS::LineStyle::Solid;
    if (string == "double")
        return CSS::LineStyle::Double;
    if (string == "groove")
        return CSS::LineStyle::Groove;
    if (string == "ridge")
        return CSS::LineStyle::Ridge;
    if (string == "inset")
        return CSS::LineStyle::Inset;
    if (string == "outset")
        return CSS::LineStyle::Outset;
    return {};
}

Optional<CSS::Float> StyleProperties::float_() const
{
    auto value = property(CSS::PropertyID::Float);
    if (!value.has_value() || !value.value()->is_string())
        return {};
    auto string = value.value()->to_string();
    if (string == "none")
        return CSS::Float::None;
    if (string == "left")
        return CSS::Float::Left;
    if (string == "right")
        return CSS::Float::Right;
    return {};
}

Optional<CSS::Clear> StyleProperties::clear() const
{
    auto value = property(CSS::PropertyID::Clear);
    if (!value.has_value() || !value.value()->is_string())
        return {};
    auto string = value.value()->to_string();
    if (string == "none")
        return CSS::Clear::None;
    if (string == "left")
        return CSS::Clear::Left;
    if (string == "right")
        return CSS::Clear::Right;
    if (string == "both")
        return CSS::Clear::Both;
    return {};
}

CSS::Display StyleProperties::display() const
{
    auto display = string_or_fallback(CSS::PropertyID::Display, "inline");
    if (display == "none")
        return CSS::Display::None;
    if (display == "block")
        return CSS::Display::Block;
    if (display == "inline")
        return CSS::Display::Inline;
    if (display == "inline-block")
        return CSS::Display::InlineBlock;
    if (display == "list-item")
        return CSS::Display::ListItem;
    if (display == "table")
        return CSS::Display::Table;
    if (display == "table-row")
        return CSS::Display::TableRow;
    if (display == "table-cell")
        return CSS::Display::TableCell;
    if (display == "table-row-group")
        return CSS::Display::TableRowGroup;
    if (display == "table-header-group")
        return CSS::Display::TableHeaderGroup;
    if (display == "table-footer-group")
        return CSS::Display::TableFooterGroup;
    dbg() << "Unknown display type: _" << display << "_";
    return CSS::Display::Block;
}

}
