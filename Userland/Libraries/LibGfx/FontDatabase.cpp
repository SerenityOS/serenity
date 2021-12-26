/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullRefPtrVector.h>
#include <AK/QuickSort.h>
#include <LibCore/DirIterator.h>
#include <LibGfx/Font.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/Typeface.h>
#include <LibTTF/Font.h>
#include <stdlib.h>

namespace Gfx {

static FontDatabase* s_the;

FontDatabase& FontDatabase::the()
{
    if (!s_the)
        s_the = new FontDatabase;
    return *s_the;
}

Font& FontDatabase::default_font()
{
    static Font* font;
    if (!font) {
        font = FontDatabase::the().get_by_name("Katica 10 400");
        VERIFY(font);
    }
    return *font;
}

Font& FontDatabase::default_fixed_width_font()
{
    static Font* font;
    if (!font) {
        font = FontDatabase::the().get_by_name("Csilla 10 400");
        VERIFY(font);
    }
    return *font;
}

Font& FontDatabase::default_bold_fixed_width_font()
{
    static Font* font;
    if (!font) {
        font = FontDatabase::the().get_by_name("Csilla 10 700");
        VERIFY(font);
    }
    return *font;
}

struct FontDatabase::Private {
    HashMap<String, RefPtr<Gfx::Font>> full_name_to_font_map;
    Vector<RefPtr<Typeface>> typefaces;
};

FontDatabase::FontDatabase()
    : m_private(make<Private>())
{
    Core::DirIterator dir_iterator("/res/fonts", Core::DirIterator::SkipDots);
    if (dir_iterator.has_error()) {
        warnln("DirIterator: {}", dir_iterator.error_string());
        exit(1);
    }
    while (dir_iterator.has_next()) {
        auto path = dir_iterator.next_full_path();

        if (path.ends_with(".font"sv)) {
            if (auto font = Gfx::BitmapFont::load_from_file(path)) {
                m_private->full_name_to_font_map.set(font->qualified_name(), font);
                auto typeface = get_or_create_typeface(font->family(), font->variant());
                typeface->add_bitmap_font(font);
            }
        } else if (path.ends_with(".ttf"sv)) {
            // FIXME: What about .otf and .woff
            if (auto font = TTF::Font::load_from_file(path)) {
                auto typeface = get_or_create_typeface(font->family(), font->variant());
                typeface->set_ttf_font(font);
            }
        }
    }
}

FontDatabase::~FontDatabase()
{
}

void FontDatabase::for_each_font(Function<void(const Gfx::Font&)> callback)
{
    Vector<RefPtr<Gfx::Font>> fonts;
    fonts.ensure_capacity(m_private->full_name_to_font_map.size());
    for (auto& it : m_private->full_name_to_font_map)
        fonts.append(it.value);
    quick_sort(fonts, [](auto& a, auto& b) { return a->qualified_name() < b->qualified_name(); });
    for (auto& font : fonts)
        callback(*font);
}

void FontDatabase::for_each_fixed_width_font(Function<void(const Gfx::Font&)> callback)
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

RefPtr<Gfx::Font> FontDatabase::get_by_name(const StringView& name)
{
    auto it = m_private->full_name_to_font_map.find(name);
    if (it == m_private->full_name_to_font_map.end()) {
        dbgln("Font lookup failed: '{}'", name);
        return nullptr;
    }
    return it->value;
}

RefPtr<Gfx::Font> FontDatabase::get(const String& family, unsigned size, unsigned weight)
{
    for (auto typeface : m_private->typefaces) {
        if (typeface->family() == family && typeface->weight() == weight)
            return typeface->get_font(size);
    }
    return nullptr;
}

RefPtr<Gfx::Font> FontDatabase::get(const String& family, const String& variant, unsigned size)
{
    for (auto typeface : m_private->typefaces) {
        if (typeface->family() == family && typeface->variant() == variant)
            return typeface->get_font(size);
    }
    return nullptr;
}

RefPtr<Typeface> FontDatabase::get_or_create_typeface(const String& family, const String& variant)
{
    for (auto typeface : m_private->typefaces) {
        if (typeface->family() == family && typeface->variant() == variant)
            return typeface;
    }
    auto typeface = adopt_ref(*new Typeface(family, variant));
    m_private->typefaces.append(typeface);
    return typeface;
}

void FontDatabase::for_each_typeface(Function<void(const Typeface&)> callback)
{
    for (auto typeface : m_private->typefaces) {
        callback(*typeface);
    }
}

}
