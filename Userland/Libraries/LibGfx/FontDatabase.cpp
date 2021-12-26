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

#include <AK/NonnullRefPtrVector.h>
#include <AK/QuickSort.h>
#include <LibCore/DirIterator.h>
#include <LibGfx/Font.h>
#include <LibGfx/FontDatabase.h>
#include <dirent.h>
#include <stdio.h>
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
        ASSERT(font);
    }
    return *font;
}

Font& FontDatabase::default_fixed_width_font()
{
    static Font* font;
    if (!font) {
        font = FontDatabase::the().get_by_name("Csilla 10 400");
        ASSERT(font);
    }
    return *font;
}

Font& FontDatabase::default_bold_fixed_width_font()
{
    static Font* font;
    if (!font) {
        font = FontDatabase::the().get_by_name("Csilla 10 700");
        ASSERT(font);
    }
    return *font;
}

Font& FontDatabase::default_bold_font()
{
    static Font* font;
    if (!font) {
        font = FontDatabase::the().get_by_name("Katica 10 700");
        ASSERT(font);
    }
    return *font;
}

struct FontDatabase::Private {
    HashMap<String, RefPtr<Gfx::Font>> full_name_to_font_map;
};

FontDatabase::FontDatabase()
    : m_private(make<Private>())
{
    Core::DirIterator di("/res/fonts", Core::DirIterator::SkipDots);
    if (di.has_error()) {
        fprintf(stderr, "DirIterator: %s\n", di.error_string());
        exit(1);
    }
    while (di.has_next()) {
        String name = di.next_path();
        if (!name.ends_with(".font"))
            continue;

        auto path = String::format("/res/fonts/%s", name.characters());
        if (auto font = Gfx::Font::load_from_file(path)) {
            m_private->full_name_to_font_map.set(font->qualified_name(), font);
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
    for (auto& it : m_private->full_name_to_font_map) {
        auto& font = *it.value;
        if (font.family() == family && font.presentation_size() == size && font.weight() == weight)
            return font;
    }
    return nullptr;
}

}
