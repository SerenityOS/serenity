/*
 * Copyright (c) 2020, Stephan Unverwerth <s.unverwerth@gmx.de>
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
#include <LibGfx/Bitmap.h>
#include <LibGfx/Size.h>

namespace Gfx {

// FIXME: Make a MutableGlyphBitmap buddy class for FontEditor instead?
class GlyphBitmap {
public:
    GlyphBitmap() = default;
    GlyphBitmap(const unsigned* rows, IntSize size)
        : m_rows(rows)
        , m_size(size)
    {
    }

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

    IntSize size() const { return m_size; }
    int width() const { return m_size.width(); }
    int height() const { return m_size.height(); }

private:
    const unsigned* m_rows { nullptr };
    IntSize m_size { 0, 0 };
};

class Glyph {
public:
    Glyph(const GlyphBitmap& glyph_bitmap, int left_bearing, int advance, int ascent)
        : m_glyph_bitmap(glyph_bitmap)
        , m_left_bearing(left_bearing)
        , m_advance(advance)
        , m_ascent(ascent)
    {
    }

    Glyph(RefPtr<Bitmap> bitmap, int left_bearing, int advance, int ascent)
        : m_bitmap(bitmap)
        , m_left_bearing(left_bearing)
        , m_advance(advance)
        , m_ascent(ascent)
    {
    }

    bool is_glyph_bitmap() const { return !m_bitmap; }
    GlyphBitmap glyph_bitmap() const { return m_glyph_bitmap; }
    RefPtr<Bitmap> bitmap() const { return m_bitmap; }
    int left_bearing() const { return m_left_bearing; }
    int advance() const { return m_advance; }
    int ascent() const { return m_ascent; }

private:
    GlyphBitmap m_glyph_bitmap;
    RefPtr<Bitmap> m_bitmap;
    int m_left_bearing;
    int m_advance;
    int m_ascent;
};

class Font : public RefCounted<Font> {
public:
    virtual NonnullRefPtr<Font> clone() const = 0;
    virtual ~Font() {};

    virtual u8 presentation_size() const = 0;

    virtual u16 weight() const = 0;
    virtual Glyph glyph(u32 code_point) const = 0;

    virtual u8 glyph_width(size_t ch) const = 0;
    virtual int glyph_or_emoji_width(u32 code_point) const = 0;
    virtual u8 glyph_height() const = 0;
    virtual int x_height() const = 0;

    virtual u8 min_glyph_width() const = 0;
    virtual u8 max_glyph_width() const = 0;
    virtual u8 glyph_fixed_width() const = 0;

    virtual u8 baseline() const = 0;
    virtual u8 mean_line() const = 0;

    virtual int width(const StringView&) const = 0;
    virtual int width(const Utf8View&) const = 0;
    virtual int width(const Utf32View&) const = 0;

    virtual String name() const = 0;

    virtual bool is_fixed_width() const = 0;

    virtual u8 glyph_spacing() const = 0;

    virtual int glyph_count() const = 0;

    virtual String family() const = 0;
    virtual String variant() const = 0;

    virtual String qualified_name() const = 0;

    virtual const Font& bold_variant() const = 0;
};

}
