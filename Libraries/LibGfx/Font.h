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
#include <AK/Utf8View.h>
#include <LibGfx/Rect.h>

namespace Gfx {

// FIXME: Make a MutableGlyphBitmap buddy class for FontEditor instead?
class GlyphBitmap {
    friend class Font;

public:
    const unsigned* rows() const { return m_rows; }
    unsigned row(unsigned index) const { return m_rows[index]; }

    bool bit_at(int x, int y) const { return row(y) & (1 << x); }
    void set_bit_at(int x, int y, bool b)
    {
        auto& mutable_row = const_cast<unsigned*>(m_rows)[y];
        if (b)
            mutable_row |= 1 << x;
        else
            mutable_row &= ~(1 << x);
    }

    Size size() const { return m_size; }
    int width() const { return m_size.width(); }
    int height() const { return m_size.height(); }

private:
    GlyphBitmap(const unsigned* rows, Size size)
        : m_rows(rows)
        , m_size(size)
    {
    }

    const unsigned* m_rows { nullptr };
    Size m_size;
};

class Font : public RefCounted<Font> {
public:
    static Font& default_font();
    static Font& default_bold_font();

    static Font& default_fixed_width_font();
    static Font& default_bold_fixed_width_font();

    RefPtr<Font> clone() const;

    static RefPtr<Font> load_from_file(const StringView& path);
    bool write_to_file(const StringView& path);

    ~Font();

    GlyphBitmap glyph_bitmap(char ch) const { return GlyphBitmap(&m_rows[(u8)ch * m_glyph_height], { glyph_width(ch), m_glyph_height }); }

    u8 glyph_width(char ch) const { return m_fixed_width ? m_glyph_width : m_glyph_widths[(u8)ch]; }
    int glyph_or_emoji_width(u32 codepoint) const;
    u8 glyph_height() const { return m_glyph_height; }
    u8 min_glyph_width() const { return m_min_glyph_width; }
    u8 max_glyph_width() const { return m_max_glyph_width; }
    int width(const StringView&) const;
    int width(const Utf8View&) const;

    String name() const { return m_name; }
    void set_name(const StringView& name) { m_name = name; }

    bool is_fixed_width() const { return m_fixed_width; }
    void set_fixed_width(bool b) { m_fixed_width = b; }

    u8 glyph_spacing() const { return m_glyph_spacing; }
    void set_glyph_spacing(u8 spacing) { m_glyph_spacing = spacing; }

    void set_glyph_width(char ch, u8 width)
    {
        ASSERT(m_glyph_widths);
        m_glyph_widths[(u8)ch] = width;
    }

private:
    Font(const StringView& name, unsigned* rows, u8* widths, bool is_fixed_width, u8 glyph_width, u8 glyph_height, u8 glyph_spacing);

    static RefPtr<Font> load_from_memory(const u8*);

    String m_name;

    unsigned* m_rows { nullptr };
    u8* m_glyph_widths { nullptr };
    MappedFile m_mapped_file;

    u8 m_glyph_width { 0 };
    u8 m_glyph_height { 0 };
    u8 m_min_glyph_width { 0 };
    u8 m_max_glyph_width { 0 };
    u8 m_glyph_spacing { 0 };

    bool m_fixed_width { false };
};

}
