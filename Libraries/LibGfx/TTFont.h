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

#include <AK/ByteBuffer.h>
#include <AK/OwnPtr.h>
#include <AK/Result.h>
#include <AK/StringView.h>

namespace Gfx {
namespace TTF {

class Font;

enum class IndexToLocFormat {
    Offset16,
    Offset32,
};

class Head {
private:
    Head() {}
    Head(ByteBuffer&& slice)
        : m_slice(move(slice))
    {
        ASSERT(m_slice.size() >= 54);
    }
    u16 units_per_em() const;
    i16 xmin() const;
    i16 ymin() const;
    i16 xmax() const;
    i16 ymax() const;
    u16 lowest_recommended_ppem() const;
    Result<IndexToLocFormat, i16> index_to_loc_format() const;

    ByteBuffer m_slice;

    friend Font;
};

class Hhea {
private:
    Hhea() {}
    Hhea(ByteBuffer&& slice)
        : m_slice(move(slice))
    {
        ASSERT(m_slice.size() >= 36);
    }
    u16 number_of_h_metrics() const;

    ByteBuffer m_slice;

    friend Font;
};

class Maxp {
private:
    Maxp() {}
    Maxp(ByteBuffer&& slice)
        : m_slice(move(slice))
    {
        ASSERT(m_slice.size() >= 6);
    }
    u16 num_glyphs() const;

    ByteBuffer m_slice;

    friend Font;
};

struct GlyphHorizontalMetrics {
    u16 advance_width;
    i16 left_side_bearing;
};

class Hmtx {
private:
    Hmtx() {}
    Hmtx(ByteBuffer&& slice, u32 num_glyphs, u32 number_of_h_metrics)
        : m_slice(move(slice))
        , m_num_glyphs(num_glyphs)
        , m_number_of_h_metrics(number_of_h_metrics)
    {
        ASSERT(m_slice.size() >= number_of_h_metrics * 2 + num_glyphs * 2);
    }
    GlyphHorizontalMetrics get_glyph_horizontal_metrics(u32 glyph_id) const;

    ByteBuffer m_slice;
    u32 m_num_glyphs;
    u32 m_number_of_h_metrics;

    friend Font;
};

enum class CmapSubtablePlatform {
    Unicode,
    Macintosh,
    Windows,
    Custom,
};

enum class CmapSubtableFormat {
    ByteEncoding,
    HighByte,
    SegmentToDelta,
    TrimmedTable,
    Mixed16And32,
    TrimmedArray,
    SegmentedCoverage,
    ManyToOneRange,
    UnicodeVariationSequences,
};

class Cmap;

class CmapSubtable {
public:
    CmapSubtablePlatform platform_id() const;
    u16 encoding_id() const { return m_encoding_id; }
    CmapSubtableFormat format() const;

private:
    CmapSubtable(ByteBuffer&& slice, u16 platform_id, u16 encoding_id)
        : m_slice(move(slice))
        , m_raw_platform_id(platform_id)
        , m_encoding_id(encoding_id)
    {
    }
    // Returns 0 if glyph not found. This corresponds to the "missing glyph"
    u32 glyph_id_for_codepoint(u32 codepoint) const;
    u32 glyph_id_for_codepoint_table_4(u32 codepoint) const;
    u32 glyph_id_for_codepoint_table_12(u32 codepoint) const;

    ByteBuffer m_slice;
    u16 m_raw_platform_id;
    u16 m_encoding_id;

    friend Cmap;
};

class Cmap {
private:
    Cmap() {}
    Cmap(ByteBuffer&& slice)
        : m_slice(move(slice))
    {
        ASSERT(m_slice.size() > 4);
    }
    u32 num_subtables() const;
    Optional<CmapSubtable> subtable(u32 index) const;
    void set_active_index(u32 index) { m_active_index = index; }
    // Returns 0 if glyph not found. This corresponds to the "missing glyph"
    u32 glyph_id_for_codepoint(u32 codepoint) const;

    ByteBuffer m_slice;
    u32 m_active_index { UINT32_MAX };

    friend Font;
};

class Font {
public:
    static OwnPtr<Font> load_from_file(const StringView& path, unsigned index);

private:
    Font(AK::ByteBuffer&& buffer, u32 offset);

    AK::ByteBuffer m_buffer;
    Head m_head;
    Hhea m_hhea;
    Maxp m_maxp;
    Hmtx m_hmtx;
    Cmap m_cmap;
};

}
}
