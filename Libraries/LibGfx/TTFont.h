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
#include <AK/Noncopyable.h>
#include <AK/OwnPtr.h>
#include <AK/StringView.h>
#include <LibGfx/Size.h>

namespace Gfx {
namespace TTF {

class AABitmap {
public:
    AABitmap(Size size)
        : m_size(size)
    {
        m_data = OwnPtr(new u8[size.width() * size.height()]);
    }
    Size size() const { return m_size; }
    u8 byte_at(int x, int y) const { return m_data[y * m_size.width() + x]; }
    void set_byte_at(int x, int y, u8 value)
    {
        m_data[y * m_size.width() + x] = value;
    }

private:
    Size m_size;
    OwnPtr<u8> m_data;
};

class Font {
    AK_MAKE_NONCOPYABLE(Font);

public:
    static OwnPtr<Font> load_from_file(const StringView& path, unsigned index);
    AABitmap raster_codepoint(u32 codepoint, float x_scale, float y_scale) const;

private:
    enum class IndexToLocFormat {
        Offset16,
        Offset32,
    };

    class Head {
    public:
        Head() {}
        Head(const ByteBuffer& slice)
            : m_slice(slice)
        {
            ASSERT(m_slice.size() >= 54);
        }
        u16 units_per_em() const;
        i16 xmin() const;
        i16 ymin() const;
        i16 xmax() const;
        i16 ymax() const;
        u16 lowest_recommended_ppem() const;
        IndexToLocFormat index_to_loc_format() const;

    private:
        ByteBuffer m_slice;
    };

    class Hhea {
    public:
        Hhea() {}
        Hhea(const ByteBuffer& slice)
            : m_slice(slice)
        {
            ASSERT(m_slice.size() >= 36);
        }
        u16 number_of_h_metrics() const;

    private:
        ByteBuffer m_slice;
    };

    class Maxp {
    public:
        Maxp() {}
        Maxp(const ByteBuffer& slice)
            : m_slice(slice)
        {
            ASSERT(m_slice.size() >= 6);
        }
        u16 num_glyphs() const;

    private:
        ByteBuffer m_slice;
    };

    struct GlyphHorizontalMetrics {
        u16 advance_width;
        i16 left_side_bearing;
    };

    class Hmtx {
    public:
        Hmtx() {}
        Hmtx(const ByteBuffer& slice, u32 num_glyphs, u32 number_of_h_metrics)
            : m_slice(slice)
            , m_num_glyphs(num_glyphs)
            , m_number_of_h_metrics(number_of_h_metrics)
        {
            ASSERT(m_slice.size() >= number_of_h_metrics * 2 + num_glyphs * 2);
        }
        GlyphHorizontalMetrics get_glyph_horizontal_metrics(u32 glyph_id) const;

    private:
        ByteBuffer m_slice;
        u32 m_num_glyphs;
        u32 m_number_of_h_metrics;
    };

    class Cmap {
    public:
        class Subtable {
        public:
            enum class Platform {
                Unicode,
                Macintosh,
                Windows,
                Custom,
            };

            enum class Format {
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

            Subtable(const ByteBuffer& slice, u16 platform_id, u16 encoding_id)
                : m_slice(slice)
                , m_raw_platform_id(platform_id)
                , m_encoding_id(encoding_id)
            {
            }
            // Returns 0 if glyph not found. This corresponds to the "missing glyph"
            u32 glyph_id_for_codepoint(u32 codepoint) const;
            Platform platform_id() const;
            u16 encoding_id() const { return m_encoding_id; }
            Format format() const;

        private:
            u32 glyph_id_for_codepoint_table_4(u32 codepoint) const;
            u32 glyph_id_for_codepoint_table_12(u32 codepoint) const;

            ByteBuffer m_slice;
            u16 m_raw_platform_id;
            u16 m_encoding_id;
        };

        Cmap() {}
        Cmap(const ByteBuffer& slice)
            : m_slice(slice)
        {
            ASSERT(m_slice.size() > 4);
        }

        u32 num_subtables() const;
        Optional<Subtable> subtable(u32 index) const;
        void set_active_index(u32 index) { m_active_index = index; }
        // Returns 0 if glyph not found. This corresponds to the "missing glyph"
        u32 glyph_id_for_codepoint(u32 codepoint) const;

    private:
        ByteBuffer m_slice;
        u32 m_active_index { UINT32_MAX };
    };

    class Loca {
    public:
        Loca() {}
        Loca(const ByteBuffer& slice, u32 num_glyphs, IndexToLocFormat index_to_loc_format)
            : m_slice(slice)
            , m_num_glyphs(num_glyphs)
            , m_index_to_loc_format(index_to_loc_format)
        {
            switch (m_index_to_loc_format) {
            case IndexToLocFormat::Offset16:
                ASSERT(m_slice.size() >= m_num_glyphs * 2);
                break;
            case IndexToLocFormat::Offset32:
                ASSERT(m_slice.size() >= m_num_glyphs * 4);
                break;
            }
        }
        u32 get_glyph_offset(u32 glyph_id) const;

    private:
        ByteBuffer m_slice;
        u32 m_num_glyphs;
        IndexToLocFormat m_index_to_loc_format;
    };

    class Glyf {
    public:
        class Glyph {
        public:
            static Glyph simple(const ByteBuffer& slice, u16 num_contours, i16 xmin, i16 ymin, i16 xmax, i16 ymax);
            static Glyph composite(const ByteBuffer& slice); // FIXME: This is currently just a dummy. Need to add support for composite glyphs.
            AABitmap raster(float x_scale, float y_scale) const;

        private:
            enum class Type {
                Simple,
                Composite,
            };
            struct Simple {
                u16 num_contours;
                i16 xmin;
                i16 ymin;
                i16 xmax;
                i16 ymax;
            };
            struct Composite {
            };

            Glyph(const ByteBuffer& slice, Type type)
                : m_type(type)
                , m_slice(move(slice))
            {
            }
            AABitmap raster_simple(float x_scale, float y_scale) const;

            Type m_type;
            ByteBuffer m_slice;
            union {
                Simple simple;
                Composite composite;
            } m_meta;
        };

        Glyf() {}
        Glyf(const ByteBuffer& slice)
            : m_slice(move(slice))
        {
        }
        Glyph glyph(u32 offset) const;

    private:
        ByteBuffer m_slice;
    };

    Font(ByteBuffer&& buffer, u32 offset);

    ByteBuffer m_buffer;
    Head m_head;
    Hhea m_hhea;
    Maxp m_maxp;
    Hmtx m_hmtx;
    Cmap m_cmap;
    Loca m_loca;
    Glyf m_glyf;
};

}
}
