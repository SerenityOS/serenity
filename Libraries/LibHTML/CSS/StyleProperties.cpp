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

#include <LibCore/CDirIterator.h>
#include <LibHTML/CSS/StyleProperties.h>
#include <LibHTML/FontCache.h>
#include <ctype.h>

StyleProperties::StyleProperties()
{
}

StyleProperties::StyleProperties(const StyleProperties& other)
    : m_property_values(*new HashMap(other.m_property_values))
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

String StyleProperties::string_or_fallback(CSS::PropertyID id, const StringView& fallback) const
{
    auto value = property(id);
    if (!value.has_value())
        return fallback;
    return value.value()->to_string();
}

Color StyleProperties::color_or_fallback(CSS::PropertyID id, const Document& document, Color fallback) const
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
            ASSERT(name.ends_with(".font"));
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

    String file_name = look_for_file(String::format("%s%s", font_family.characters(), weight.characters()));
    if (file_name.is_null() && weight == "")
        file_name = look_for_file(String::format("%sRegular", font_family.characters()));

    if (file_name.is_null()) {
        dbg() << "Failed to find a font for family " << font_family << " weight " << font_weight;

        if (font_weight == "bold")
            m_font = Font::default_bold_font();
        else
            m_font = Font::default_font();
        return;
    }

#ifdef HTML_DEBUG
    dbg() << "Found font " << file_name << " for family " << font_family << " weight " << font_weight;
#endif

    m_font = Font::load_from_file(String::format("/res/fonts/%s", file_name.characters()));
    FontCache::the().set({ font_family, font_weight }, *m_font);
}

float StyleProperties::line_height() const
{
    auto line_height_length = length_or_fallback(CSS::PropertyID::LineHeight, {});
    if (line_height_length.is_absolute())
        return (float)font().glyph_height() * line_height_length.to_px();
    return (float)font().glyph_height() * 1.4f;
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
