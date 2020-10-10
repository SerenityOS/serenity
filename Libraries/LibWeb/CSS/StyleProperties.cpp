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
    box.left = length_or_fallback(left_id, {});
    box.top = length_or_fallback(top_id, {});
    box.right = length_or_fallback(right_id, {});
    box.bottom = length_or_fallback(bottom_id, {});
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
    auto font_family = string_or_fallback(CSS::PropertyID::FontFamily, "Katica");
    auto font_weight = string_or_fallback(CSS::PropertyID::FontWeight, "normal");

    if (auto cached_font = FontCache::the().get({ font_family, font_weight })) {
        m_font = cached_font;
        return;
    }

    String weight;
    if (font_weight == "lighter")
        weight = "Thin";
    else if (font_weight == "normal")
        weight = "";
    else if (font_weight == "bold")
        weight = "Bold";
    else {
        dbg() << "Unknown font-weight: " << font_weight;
        weight = "";
    }

    auto look_for_file = [](const StringView& expected_name) -> String {
        // TODO: handle font sizes properly?
        Core::DirIterator it { "/res/fonts/", Core::DirIterator::Flags::SkipDots };
        while (it.has_next()) {
            String name = it.next_path();

            if (!name.ends_with(".font"))
                continue;
            if (!name.starts_with(expected_name))
                continue;

            // Check that a numeric size immediately
            // follows the font name. This prevents,
            // for example, matching KaticaBold when
            // the regular Katica is requested.
            if (!isdigit(name[expected_name.length()]))
                continue;

            return name;
        }
        return {};
    };

    // FIXME: Do this properly, with quote handling etc.
    for (auto& font_name : font_family.split(',')) {
        font_name = font_name.trim_whitespace();
        if (font_name == "monospace")
            font_name = "Csilla";

        auto file_name = look_for_file(String::format("%s%s", font_name.characters(), weight.characters()));
        if (file_name.is_null() && weight == "")
            file_name = look_for_file(String::format("%sRegular", font_name.characters()));
        if (file_name.is_null())
            continue;

#ifdef HTML_DEBUG
        dbg() << "Found font " << file_name << " for family " << font_family << " weight " << font_weight;
#endif
        m_font = Gfx::Font::load_from_file(String::format("/res/fonts/%s", file_name.characters()));
        FontCache::the().set({ font_name, font_weight }, *m_font);
        return;
    }

#ifdef HTML_DEBUG
    dbg() << "Failed to find a font for family " << font_family << " weight " << font_weight;
#endif
    if (font_weight == "bold")
        m_font = Gfx::Font::default_bold_font();
    else
        m_font = Gfx::Font::default_font();
    // FIXME: This is a hack to stop chewing CPU on sites that use a font we don't have and have a lot of text
    //        or changes text often. Examples are the Serenity 2nd birthday page and the JS specification.
    FontCache::the().set({ font_family, font_weight }, *m_font);
}

float StyleProperties::line_height(const LayoutNode& layout_node) const
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
        if (my_value.to_string() != other_value.to_string())
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
