/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedFlyString.h>
#include <AK/FlyString.h>
#include <AK/LexicalPath.h>
#include <AK/Queue.h>
#include <AK/QuickSort.h>
#include <LibCore/Resource.h>
#include <LibFileSystem/FileSystem.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Font/OpenType/Font.h>
#include <LibGfx/Font/Typeface.h>
#include <LibGfx/Font/WOFF/Font.h>

namespace Gfx {

FontDatabase& FontDatabase::the()
{
    static FontDatabase s_the;
    return s_the;
}

static RefPtr<Font> s_default_font;
static ByteString s_default_font_query;

static RefPtr<Font> s_window_title_font;
static ByteString s_window_title_font_query;

static RefPtr<Font> s_fixed_width_font;
static ByteString s_fixed_width_font_query;

void FontDatabase::set_default_font_query(ByteString query)
{
    if (s_default_font_query == query)
        return;
    s_default_font_query = move(query);
    s_default_font = nullptr;
}

ByteString FontDatabase::default_font_query()
{
    return s_default_font_query;
}

void FontDatabase::set_window_title_font_query(ByteString query)
{
    if (s_window_title_font_query == query)
        return;
    s_window_title_font_query = move(query);
    s_window_title_font = nullptr;
}

ByteString FontDatabase::window_title_font_query()
{
    return s_window_title_font_query;
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

void FontDatabase::set_fixed_width_font_query(ByteString query)
{
    if (s_fixed_width_font_query == query)
        return;
    s_fixed_width_font_query = move(query);
    s_fixed_width_font = nullptr;
}

ByteString FontDatabase::fixed_width_font_query()
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
    HashMap<ByteString, NonnullRefPtr<Gfx::Font>, CaseInsensitiveStringTraits> full_name_to_font_map;
    HashMap<FlyString, Vector<NonnullRefPtr<Typeface>>, AK::ASCIICaseInsensitiveFlyStringTraits> typefaces;
};

void FontDatabase::load_all_fonts_from_uri(StringView uri)
{
    auto root_or_error = Core::Resource::load_from_uri(uri);
    if (root_or_error.is_error()) {
        dbgln("FontDatabase::load_all_fonts_from_uri('{}'): {}", uri, root_or_error.error());
        return;
    }
    auto root = root_or_error.release_value();

    root->for_each_descendant_file([this](Core::Resource const& resource) -> IterationDecision {
        auto uri = resource.uri();
        auto path = LexicalPath(uri.bytes_as_string_view());
        if (path.has_extension(".font"sv)) {
            if (auto font_or_error = Gfx::BitmapFont::try_load_from_resource(resource); !font_or_error.is_error()) {
                auto font = font_or_error.release_value();
                m_private->full_name_to_font_map.set(font->qualified_name().to_byte_string(), *font);
                auto typeface = get_or_create_typeface(font->family(), font->variant());
                typeface->add_bitmap_font(font);
            }
        } else if (path.has_extension(".ttf"sv)) {
            // FIXME: What about .otf
            if (auto font_or_error = OpenType::Font::try_load_from_resource(resource); !font_or_error.is_error()) {
                auto font = font_or_error.release_value();
                auto typeface = get_or_create_typeface(font->family(), font->variant());
                typeface->set_vector_font(move(font));
            }
        } else if (path.has_extension(".woff"sv)) {
            if (auto font_or_error = WOFF::Font::try_load_from_resource(resource); !font_or_error.is_error()) {
                auto font = font_or_error.release_value();
                auto typeface = get_or_create_typeface(font->family(), font->variant());
                typeface->set_vector_font(move(font));
            }
        }
        return IterationDecision::Continue;
    });
}

FontDatabase::FontDatabase()
    : m_private(make<Private>())
{
    load_all_fonts_from_uri("resource://fonts"sv);
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
            auto slope = parts.take_last().to_number<int>().value_or(0);
            auto weight = parts.take_last().to_number<int>().value_or(0);
            auto size = parts.take_last().to_number<int>().value_or(0);
            auto family = MUST(String::join(' ', parts));
            return get(family, size, weight, Gfx::FontWidth::Normal, slope);
        }
        dbgln("Font lookup failed: '{}'", name);
        return nullptr;
    }
    return it->value;
}

RefPtr<Gfx::Font> FontDatabase::get(FlyString const& family, float point_size, unsigned weight, unsigned width, unsigned slope, Font::AllowInexactSizeMatch allow_inexact_size_match)
{
    auto it = m_private->typefaces.find(family);
    if (it == m_private->typefaces.end())
        return nullptr;
    for (auto const& typeface : it->value) {
        if (typeface->weight() == weight && typeface->width() == width && typeface->slope() == slope)
            return typeface->get_font(point_size, allow_inexact_size_match);
    }
    return nullptr;
}

RefPtr<Gfx::Font> FontDatabase::get(FlyString const& family, FlyString const& variant, float point_size, Font::AllowInexactSizeMatch allow_inexact_size_match)
{
    auto it = m_private->typefaces.find(family);
    if (it == m_private->typefaces.end())
        return nullptr;
    for (auto const& typeface : it->value) {
        if (typeface->variant() == variant)
            return typeface->get_font(point_size, allow_inexact_size_match);
    }
    return nullptr;
}

RefPtr<Typeface> FontDatabase::get_or_create_typeface(FlyString const& family, FlyString const& variant)
{
    auto it = m_private->typefaces.find(family);
    if (it != m_private->typefaces.end()) {
        for (auto const& typeface : it->value) {
            if (typeface->variant() == variant)
                return typeface;
        }
    }
    auto typeface = adopt_ref(*new Typeface(family, variant));
    m_private->typefaces.ensure(family).append(typeface);
    return typeface;
}

void FontDatabase::for_each_typeface(Function<void(Typeface const&)> callback)
{
    for (auto const& it : m_private->typefaces) {
        for (auto const& jt : it.value) {
            callback(*jt);
        }
    }
}

void FontDatabase::for_each_typeface_with_family_name(FlyString const& family_name, Function<void(Typeface const&)> callback)
{
    auto it = m_private->typefaces.find(family_name);
    if (it == m_private->typefaces.end())
        return;
    for (auto const& typeface : it->value)
        callback(*typeface);
}

}
