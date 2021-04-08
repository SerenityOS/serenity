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

#pragma once

#include <AK/MappedFile.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/String.h>
#include <AK/Types.h>
#include <LibGfx/Font.h>
#include <LibGfx/Size.h>

namespace Gfx {

enum FontTypes {
    Default = 0,
    LatinExtendedA = 1,
    // There are many blocks between LatinExtendedA and Cyrrilic that has to be added later.
    // Cyrrilic has to be switched to another number
    Cyrillic = 2
};

class BitmapFont : public Font {
public:
    NonnullRefPtr<Font> clone() const;
    static NonnullRefPtr<BitmapFont> create(u8 glyph_height, u8 glyph_width, bool fixed, FontTypes type);

    static RefPtr<BitmapFont> load_from_file(const StringView& path);
    bool write_to_file(const StringView& path);

    ~BitmapFont();

    u8 presentation_size() const { return m_presentation_size; }
    void set_presentation_size(u8 size) { m_presentation_size = size; }

    u16 weight() const { return m_weight; }
    void set_weight(u16 weight) { m_weight = weight; }

    Glyph glyph(u32 code_point) const;
    bool contains_glyph(u32 code_point) const { return code_point < (u32)glyph_count(); }

    u8 glyph_width(size_t ch) const { return m_fixed_width ? m_glyph_width : m_glyph_widths[ch]; }
    int glyph_or_emoji_width(u32 code_point) const;
    u8 glyph_height() const { return m_glyph_height; }
    int x_height() const { return m_x_height; }

    u8 min_glyph_width() const { return m_min_glyph_width; }
    u8 max_glyph_width() const { return m_max_glyph_width; }
    u8 glyph_fixed_width() const { return m_glyph_width; }

    u8 baseline() const { return m_baseline; }
    void set_baseline(u8 baseline)
    {
        m_baseline = baseline;
        update_x_height();
    }

    u8 mean_line() const { return m_mean_line; }
    void set_mean_line(u8 mean_line)
    {
        m_mean_line = mean_line;
        update_x_height();
    }

    int width(const StringView&) const;
    int width(const Utf8View&) const;
    int width(const Utf32View&) const;

    String name() const { return m_name; }
    void set_name(String name) { m_name = move(name); }

    bool is_fixed_width() const { return m_fixed_width; }
    void set_fixed_width(bool b) { m_fixed_width = b; }

    u8 glyph_spacing() const { return m_glyph_spacing; }
    void set_glyph_spacing(u8 spacing) { m_glyph_spacing = spacing; }

    void set_glyph_width(size_t ch, u8 width)
    {
        VERIFY(m_glyph_widths);
        m_glyph_widths[ch] = width;
    }

    int glyph_count() const { return m_glyph_count; }

    FontTypes type() { return m_type; }
    void set_type(FontTypes type);

    String family() const { return m_family; }
    void set_family(String family) { m_family = move(family); }
    String variant() const { return String::formatted("{}", weight()); }

    String qualified_name() const;

    const Font& bold_variant() const;

private:
    BitmapFont(String name, String family, unsigned* rows, u8* widths, bool is_fixed_width, u8 glyph_width, u8 glyph_height, u8 glyph_spacing, FontTypes type, u8 baseline, u8 mean_line, u8 presentation_size, u16 weight, bool owns_arrays = false);

    static RefPtr<BitmapFont> load_from_memory(const u8*);
    static size_t glyph_count_by_type(FontTypes type);

    void update_x_height() { m_x_height = m_baseline - m_mean_line; };

    String m_name;
    String m_family;
    FontTypes m_type;
    size_t m_glyph_count { 256 };

    unsigned* m_rows { nullptr };
    u8* m_glyph_widths { nullptr };
    RefPtr<MappedFile> m_mapped_file;

    u8 m_glyph_width { 0 };
    u8 m_glyph_height { 0 };
    u8 m_x_height { 0 };
    u8 m_min_glyph_width { 0 };
    u8 m_max_glyph_width { 0 };
    u8 m_glyph_spacing { 0 };
    u8 m_baseline { 0 };
    u8 m_mean_line { 0 };
    u8 m_presentation_size { 0 };
    u16 m_weight { 0 };

    bool m_fixed_width { false };
    bool m_owns_arrays { false };

    mutable RefPtr<Gfx::Font> m_bold_variant;
};

}
