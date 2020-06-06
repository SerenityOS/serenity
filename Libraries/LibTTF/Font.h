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
#include <AK/Optional.h>
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
    static RefPtr<Font> load_from_file(const StringView& path, unsigned index);

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
    u32 glyph_count() const { return m_maxp.num_glyphs(); }

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
            ASSERT(m_slice.size() >= (size_t) Sizes::Table);
        }
        u16 units_per_em() const;
        i16 xmin() const;
        i16 ymin() const;
        i16 xmax() const;
        i16 ymax() const;
        u16 lowest_recommended_ppem() const;
        IndexToLocFormat index_to_loc_format() const;

    private:
        enum class Offsets {
            UnitsPerEM = 18,
            XMin = 36,
            YMin = 38,
            XMax = 40,
            YMax = 42,
            LowestRecPPEM = 46,
            IndexToLocFormat = 50,
        };
        enum class Sizes {
            Table = 54,
        };

        ByteBuffer m_slice;
    };

    class Hhea {
    public:
        Hhea() {}
        Hhea(const ByteBuffer& slice)
            : m_slice(slice)
        {
            ASSERT(m_slice.size() >= (size_t) Sizes::Table);
        }
        i16 ascender() const;
        i16 descender() const;
        i16 line_gap() const;
        u16 advance_width_max() const;
        u16 number_of_h_metrics() const;

    private:
        enum class Offsets {
            Ascender = 4,
            Descender = 6,
            LineGap = 8,
            AdvanceWidthMax = 10,
            NumberOfHMetrics = 34,
        };
        enum class Sizes {
            Table = 36,
        };

        ByteBuffer m_slice;
    };

    class Maxp {
    public:
        Maxp() {}
        Maxp(const ByteBuffer& slice)
            : m_slice(slice)
        {
            ASSERT(m_slice.size() >= (size_t) Sizes::TableV0p5);
        }
        u16 num_glyphs() const;

    private:
        enum class Offsets {
            NumGlyphs = 4
        };
        enum class Sizes {
            TableV0p5 = 6,
        };

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
            ASSERT(m_slice.size() >= number_of_h_metrics * (u32) Sizes::LongHorMetric + (num_glyphs - number_of_h_metrics) * (u32) Sizes::LeftSideBearing);
        }
        GlyphHorizontalMetrics get_glyph_horizontal_metrics(u32 glyph_id) const;

    private:
        enum class Sizes {
            LongHorMetric = 4,
            LeftSideBearing = 2
        };

        ByteBuffer m_slice;
        u32 m_num_glyphs;
        u32 m_number_of_h_metrics;
    };

    class Cmap {
    public:
        class Subtable {
        public:
            enum class Platform {
                Unicode = 0,
                Macintosh = 1,
                Windows = 3,
                Custom = 4,
            };
            enum class Format {
                ByteEncoding = 0,
                HighByte = 2,
                SegmentToDelta = 4,
                TrimmedTable = 6,
                Mixed16And32 = 8,
                TrimmedArray = 10,
                SegmentedCoverage = 12,
                ManyToOneRange = 13,
                UnicodeVariationSequences = 14,
            };
            enum class WindowsEncoding {
                UnicodeBMP = 1,
                UnicodeFullRepertoire = 10,
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
            enum class Table4Offsets {
                SegCountX2 = 6,
                EndConstBase = 14,
                StartConstBase = 16,
                DeltaConstBase = 16,
                RangeConstBase = 16,
                GlyphOffsetConstBase = 16,
            };
            enum class Table4Sizes {
                Constant = 16,
                NonConstMultiplier = 4,
            };
            enum class Table12Offsets {
                NumGroups = 12,
                Record_StartCode = 16,
                Record_EndCode = 20,
                Record_StartGlyph = 24,
            };
            enum class Table12Sizes {
                Header = 16,
                Record = 12,
            };

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
            ASSERT(m_slice.size() > (size_t) Sizes::TableHeader);
        }

        u32 num_subtables() const;
        Optional<Subtable> subtable(u32 index) const;
        void set_active_index(u32 index) { m_active_index = index; }
        // Returns 0 if glyph not found. This corresponds to the "missing glyph"
        u32 glyph_id_for_codepoint(u32 codepoint) const;

    private:
        enum class Offsets {
            NumTables = 2,
            EncodingRecord_EncodingID = 2,
            EncodingRecord_Offset = 4,
        };
        enum class Sizes {
            TableHeader = 4,
            EncodingRecord = 8,
        };

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
            RefPtr<Gfx::Bitmap> raster(float x_scale, float y_scale) const;
            int ascender() const
            {
                if (m_type == Type::Simple) {
                    return m_meta.simple.ymax;
                }
                // FIXME: Support composite outlines.
                TODO();
            }
            int descender() const
            {
                if (m_type == Type::Simple) {
                    return m_meta.simple.ymin;
                }
                // FIXME: Support composite outlines.
                TODO();
            }

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
            RefPtr<Gfx::Bitmap> raster_simple(float x_scale, float y_scale) const;

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
        enum class Offsets {
            XMin = 2,
            YMin = 4,
            XMax = 6,
            YMax = 8,
        };
        enum class Sizes {
            GlyphHeader = 10,
        };

        ByteBuffer m_slice;
    };

    ByteBuffer m_buffer;
    Head m_head;
    Hhea m_hhea;
    Maxp m_maxp;
    Hmtx m_hmtx;
    Cmap m_cmap;
    Loca m_loca;
    Glyf m_glyf;

    friend ScaledFont;
};

class ScaledFont {
public:
    ScaledFont(RefPtr<Font> font, float point_width, float point_height, unsigned dpi_x = DEFAULT_DPI, unsigned dpi_y = DEFAULT_DPI)
        : m_font(font)
    {
        float units_per_em = m_font->m_head.units_per_em();
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
