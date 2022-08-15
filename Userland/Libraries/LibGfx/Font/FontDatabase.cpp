/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/FlyString.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/QuickSort.h>
#include <LibCore/DirIterator.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Font/TrueType/Font.h>
#include <LibGfx/Font/Typeface.h>
#include <LibGfx/Font/WOFF/Font.h>
#include <stdlib.h>

namespace Gfx {

FontDatabase& FontDatabase::the()
{
    static FontDatabase s_the;
    return s_the;
}

static RefPtr<Font> s_default_font;
static String s_default_font_query;

static RefPtr<Font> s_window_title_font;
static String s_window_title_font_query;

static RefPtr<Font> s_fixed_width_font;
static String s_fixed_width_font_query;

static String s_default_fonts_lookup_path = "/res/fonts";

void FontDatabase::set_default_font_query(String query)
{
    if (s_default_font_query == query)
        return;
    s_default_font_query = move(query);
    s_default_font = nullptr;
}

String FontDatabase::default_font_query()
{
    return s_default_font_query;
}

void FontDatabase::set_window_title_font_query(String query)
{
    if (s_window_title_font_query == query)
        return;
    s_window_title_font_query = move(query);
    s_window_title_font = nullptr;
}

String FontDatabase::window_title_font_query()
{
    return s_window_title_font_query;
}

void FontDatabase::set_default_fonts_lookup_path(String path)
{
    if (s_default_fonts_lookup_path == path)
        return;
    s_default_fonts_lookup_path = move(path);
}

String FontDatabase::default_fonts_lookup_path()
{
    return s_default_fonts_lookup_path;
}

Font& FontDatabase::default_font()
{
    if (!s_default_font) {
        VERIFY(!s_default_font_query.is_empty());
        s_default_font = FontDatabase::the().get_by_name(s_default_font_query);
        VERIFY(s_default_font);
    }
    return *s_default_font;
}

Font& FontDatabase::window_title_font()
{
    if (!s_window_title_font) {
        VERIFY(!s_window_title_font_query.is_empty());
        s_window_title_font = FontDatabase::the().get_by_name(s_window_title_font_query);
        VERIFY(s_window_title_font);
    }
    return *s_window_title_font;
}

void FontDatabase::set_fixed_width_font_query(String query)
{
    if (s_fixed_width_font_query == query)
        return;
    s_fixed_width_font_query = move(query);
    s_fixed_width_font = nullptr;
}

String FontDatabase::fixed_width_font_query()
{
    return s_fixed_width_font_query;
}

Font& FontDatabase::default_fixed_width_font()
{
    if (!s_fixed_width_font) {
        VERIFY(!s_fixed_width_font_query.is_empty());
        s_fixed_width_font = FontDatabase::the().get_by_name(s_fixed_width_font_query);
        VERIFY(s_fixed_width_font);
    }
    return *s_fixed_width_font;
}

struct FontDatabase::Private {
    HashMap<String, NonnullRefPtr<Gfx::Font>> full_name_to_font_map;
    Vector<RefPtr<Typeface>> typefaces;
};

FontDatabase::FontDatabase()
    : m_private(make<Private>())
{
    Core::DirIterator dir_iterator(s_default_fonts_lookup_path, Core::DirIterator::SkipDots);
    if (dir_iterator.has_error()) {
        warnln("DirIterator: {}", dir_iterator.error_string());
        exit(1);
    }
    while (dir_iterator.has_next()) {
        auto path = dir_iterator.next_full_path();

        if (path.ends_with(".font"sv)) {
            if (auto font_or_error = Gfx::BitmapFont::try_load_from_file(path); !font_or_error.is_error()) {
                auto font = font_or_error.release_value();
                m_private->full_name_to_font_map.set(font->qualified_name(), *font);
                auto typeface = get_or_create_typeface(font->family(), font->variant());
                typeface->add_bitmap_font(font);
            }
        } else if (path.ends_with(".ttf"sv)) {
            // FIXME: What about .otf
            if (auto font_or_error = TTF::Font::try_load_from_file(path); !font_or_error.is_error()) {
                auto font = font_or_error.release_value();
                auto typeface = get_or_create_typeface(font->family(), font->variant());
                typeface->set_vector_font(move(font));
            }
        } else if (path.ends_with(".woff"sv)) {
            if (auto font_or_error = WOFF::Font::try_load_from_file(path); !font_or_error.is_error()) {
                auto font = font_or_error.release_value();
                auto typeface = get_or_create_typeface(font->family(), font->variant());
                typeface->set_vector_font(move(font));
            }
        }
    }
}

void FontDatabase::for_each_font(Function<void(Gfx::Font const&)> callback)
{
    Vector<RefPtr<Gfx::Font>> fonts;
    fonts.ensure_capacity(m_private->full_name_to_font_map.size());
    for (auto& it : m_private->full_name_to_font_map)
        fonts.append(it.value);
    quick_sort(fonts, [](auto& a, auto& b) { return a->qualified_name() < b->qualified_name(); });
    for (auto& font : fonts)
        callback(*font);
}

void FontDatabase::for_each_fixed_width_font(Function<void(Gfx::Font const&)> callback)
{
    Vector<RefPtr<Gfx::Font>> fonts;
    fonts.ensure_capacity(m_private->full_name_to_font_map.size());
    for (auto& it : m_private->full_name_to_font_map) {
        if (it.value->is_fixed_width())
            fonts.append(it.value);
    }
    quick_sort(fonts, [](auto& a, auto& b) { return a->qualified_name() < b->qualified_name(); });
    for (auto& font : fonts)
        callback(*font);
}

RefPtr<Gfx::Font> FontDatabase::get_by_name(StringView name)
{
    auto it = m_private->full_name_to_font_map.find(name);
    if (it == m_private->full_name_to_font_map.end()) {
        auto parts = name.split_view(" "sv);
        if (parts.size() >= 4) {
            auto slope = parts.take_last().to_int().value_or(0);
            auto weight = parts.take_last().to_int().value_or(0);
            auto size = parts.take_last().to_int().value_or(0);
            auto family = String::join(' ', parts);
            return get(family, size, weight, slope);
        }
        dbgln("Font lookup failed: '{}'", name);
        return nullptr;
    }
    return it->value;
}

RefPtr<Gfx::Font> FontDatabase::get(FlyString const& family, float point_size, unsigned weight, unsigned slope, Font::AllowInexactSizeMatch allow_inexact_size_match)
{
    for (auto typeface : m_private->typefaces) {
        if (typeface->family() == family && typeface->weight() == weight && typeface->slope() == slope)
            return typeface->get_font(point_size, allow_inexact_size_match);
    }
    return nullptr;
}

RefPtr<Gfx::Font> FontDatabase::get(FlyString const& family, FlyString const& variant, float point_size, Font::AllowInexactSizeMatch allow_inexact_size_match)
{
    for (auto typeface : m_private->typefaces) {
        if (typeface->family() == family && typeface->variant() == variant)
            return typeface->get_font(point_size, allow_inexact_size_match);
    }
    return nullptr;
}

RefPtr<Typeface> FontDatabase::get_or_create_typeface(String const& family, String const& variant)
{
    for (auto typeface : m_private->typefaces) {
        if (typeface->family() == family && typeface->variant() == variant)
            return typeface;
    }
    auto typeface = adopt_ref(*new Typeface(family, variant));
    m_private->typefaces.append(typeface);
    return typeface;
}

void FontDatabase::for_each_typeface(Function<void(Typeface const&)> callback)
{
    for (auto typeface : m_private->typefaces) {
        callback(*typeface);
    }
}

}
