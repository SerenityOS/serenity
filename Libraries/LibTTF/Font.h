/*
 * Copyright (c) 2020, Srimanta Barua <srimanta.barua1@gmail.com>
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

#include "Cmap.h"
#include <AK/ByteBuffer.h>
#include <AK/Noncopyable.h>
#include <AK/RefCounted.h>
#include <AK/StringView.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Size.h>

#define POINTS_PER_INCH 72.0f
#define DEFAULT_DPI     96

namespace TTF {

class ScaledFont;

struct ScaledFontMetrics {
    int ascender;
    int descender;
    int line_gap;
    int advance_width_max;

    int height() const
    {
        return ascender - descender;
    }
};

struct ScaledGlyphMetrics {
    int ascender;
    int descender;
    int advance_width;
    int left_side_bearing;
};

class Font : public RefCounted<Font> {
    AK_MAKE_NONCOPYABLE(Font);

public:
    static RefPtr<Font> load_from_file(const StringView& path, unsigned index = 0);

private:
    enum class Offsets {
        NumTables = 4,
        TableRecord_Offset = 8,
        TableRecord_Length = 12,
    };
    enum class Sizes {
        TTCHeaderV1 = 12,
        OffsetTable = 12,
        TableRecord = 16,
    };

    Font(ByteBuffer&& buffer, u32 offset);
    ScaledFontMetrics metrics(float x_scale, float y_scale) const;
    ScaledGlyphMetrics glyph_metrics(u32 glyph_id, float x_scale, float y_scale) const;
    RefPtr<Gfx::Bitmap> raster_glyph(u32 glyph_id, float x_scale, float y_scale) const;
    u32 glyph_count() const;
    u16 units_per_em() const;

    // This owns the font data
    ByteBuffer m_buffer;
    // These are all non-owning slices
    ByteBuffer m_head_slice;
    ByteBuffer m_hhea_slice;
    ByteBuffer m_maxp_slice;
    ByteBuffer m_hmtx_slice;
    ByteBuffer m_loca_slice;
    ByteBuffer m_glyf_slice;
    // These are stateful wrappers around tables
    Cmap m_cmap;

    friend ScaledFont;
};

class ScaledFont {
public:
    ScaledFont(RefPtr<Font> font, float point_width, float point_height, unsigned dpi_x = DEFAULT_DPI, unsigned dpi_y = DEFAULT_DPI)
        : m_font(font)
    {
        float units_per_em = m_font->units_per_em();
        m_x_scale = (point_width * dpi_x) / (POINTS_PER_INCH * units_per_em);
        m_y_scale = (point_height * dpi_y) / (POINTS_PER_INCH * units_per_em);
    }
    u32 glyph_id_for_codepoint(u32 codepoint) const { return m_font->m_cmap.glyph_id_for_codepoint(codepoint); }
    ScaledFontMetrics metrics() const { return m_font->metrics(m_x_scale, m_y_scale); }
    ScaledGlyphMetrics glyph_metrics(u32 glyph_id) const { return m_font->glyph_metrics(glyph_id, m_x_scale, m_y_scale); }
    RefPtr<Gfx::Bitmap> raster_glyph(u32 glyph_id) const { return m_font->raster_glyph(glyph_id, m_x_scale, m_y_scale); }
    u32 glyph_count() const { return m_font->glyph_count(); }
    int width(const StringView&) const;
    int width(const Utf8View&) const;
    int width(const Utf32View&) const;

private:
    RefPtr<Font> m_font;
    float m_x_scale;
    float m_y_scale;

    friend Font;
};

}
