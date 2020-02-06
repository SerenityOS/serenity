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

#include <AK/QuickSort.h>
#include <LibCore/CDirIterator.h>
#include <LibDraw/Font.h>
#include <LibGUI/GFontDatabase.h>
#include <dirent.h>
#include <stdio.h>
#include <stdlib.h>

static GFontDatabase* s_the;

GFontDatabase& GFontDatabase::the()
{
    if (!s_the)
        s_the = new GFontDatabase;
    return *s_the;
}

GFontDatabase::GFontDatabase()
{
    Core::DirIterator di("/res/fonts", Core::DirIterator::SkipDots);
    if (di.has_error()) {
        fprintf(stderr, "CDirIterator: %s\n", di.error_string());
        exit(1);
    }
    while (di.has_next()) {
        String name = di.next_path();
        auto path = String::format("/res/fonts/%s", name.characters());
        if (auto font = Gfx::Font::load_from_file(path)) {
            Metadata metadata;
            metadata.path = path;
            metadata.glyph_height = font->glyph_height();
            metadata.is_fixed_width = font->is_fixed_width();
            m_name_to_metadata.set(font->name(), move(metadata));
        }
    }
}

GFontDatabase::~GFontDatabase()
{
}

void GFontDatabase::for_each_font(Function<void(const StringView&)> callback)
{
    Vector<String> names;
    names.ensure_capacity(m_name_to_metadata.size());
    for (auto& it : m_name_to_metadata)
        names.append(it.key);
    quick_sort(names.begin(), names.end(), AK::is_less_than<String>);
    for (auto& name : names)
        callback(name);
}

void GFontDatabase::for_each_fixed_width_font(Function<void(const StringView&)> callback)
{
    Vector<String> names;
    names.ensure_capacity(m_name_to_metadata.size());
    for (auto& it : m_name_to_metadata) {
        if (it.value.is_fixed_width)
            names.append(it.key);
    }
    quick_sort(names.begin(), names.end(), AK::is_less_than<String>);
    for (auto& name : names)
        callback(name);
}

RefPtr<Gfx::Font> GFontDatabase::get_by_name(const StringView& name)
{
    auto it = m_name_to_metadata.find(name);
    if (it == m_name_to_metadata.end())
        return nullptr;
    return Gfx::Font::load_from_file((*it).value.path);
}
