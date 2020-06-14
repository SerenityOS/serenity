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

#include "Font.h"
#include <AK/LogStream.h>
#include <AK/Utf8View.h>
#include <AK/Utf32View.h>
#include <bits/stdint.h>
#include <LibCore/File.h>
#include <math.h>

namespace TTF {

u16 be_u16(const u8* ptr)
{
    return (((u16) ptr[0]) << 8) | ((u16) ptr[1]);
}

u32 be_u32(const u8* ptr)
{
    return (((u32) ptr[0]) << 24) | (((u32) ptr[1]) << 16) | (((u32) ptr[2]) << 8) | ((u32) ptr[3]);
}

i16 be_i16(const u8* ptr)
{
    return (((i16) ptr[0]) << 8) | ((i16) ptr[1]);
}

float be_fword(const u8* ptr)
{
    return (float) be_i16(ptr) / (float) (1 << 14);
}

u32 tag_from_str(const char *str)
{
    return be_u32((const u8*) str);
}

u16 Font::Head::units_per_em() const
{
    return be_u16(m_slice.offset_pointer((u32) Offsets::UnitsPerEM));
}

i16 Font::Head::xmin() const
{
    return be_i16(m_slice.offset_pointer((u32) Offsets::XMin));
}

i16 Font::Head::ymin() const
{
    return be_i16(m_slice.offset_pointer((u32) Offsets::YMin));
}

i16 Font::Head::xmax() const
{
    return be_i16(m_slice.offset_pointer((u32) Offsets::XMax));
}

i16 Font::Head::ymax() const
{
    return be_i16(m_slice.offset_pointer((u32) Offsets::YMax));
}

u16 Font::Head::lowest_recommended_ppem() const
{
    return be_u16(m_slice.offset_pointer((u32) Offsets::LowestRecPPEM));
}

Font::IndexToLocFormat Font::Head::index_to_loc_format() const
{
    i16 raw = be_i16(m_slice.offset_pointer((u32) Offsets::IndexToLocFormat));
    switch (raw) {
    case 0:
        return IndexToLocFormat::Offset16;
    case 1:
        return IndexToLocFormat::Offset32;
    default:
        ASSERT_NOT_REACHED();
    }
}

i16 Font::Hhea::ascender() const
{
    return be_i16(m_slice.offset_pointer((u32) Offsets::Ascender));
}

i16 Font::Hhea::descender() const
{
    return be_i16(m_slice.offset_pointer((u32) Offsets::Descender));
}

i16 Font::Hhea::line_gap() const
{
    return be_i16(m_slice.offset_pointer((u32) Offsets::LineGap));
}

u16 Font::Hhea::advance_width_max() const
{
    return be_u16(m_slice.offset_pointer((u32) Offsets::AdvanceWidthMax));
}

u16 Font::Hhea::number_of_h_metrics() const
{
    return be_u16(m_slice.offset_pointer((u32) Offsets::NumberOfHMetrics));
}

Font::GlyphHorizontalMetrics Font::Hmtx::get_glyph_horizontal_metrics(u32 glyph_id) const
{
    ASSERT(glyph_id < m_num_glyphs);
    if (glyph_id < m_number_of_h_metrics) {
        auto offset = glyph_id * (u32) Sizes::LongHorMetric;
        u16 advance_width = be_u16(m_slice.offset_pointer(offset));
        i16 left_side_bearing = be_i16(m_slice.offset_pointer(offset + 2));
        return GlyphHorizontalMetrics {
            .advance_width = advance_width,
            .left_side_bearing = left_side_bearing,
        };
    }
    auto offset = m_number_of_h_metrics * (u32) Sizes::LongHorMetric + (glyph_id - m_number_of_h_metrics) * (u32) Sizes::LeftSideBearing;
    u16 advance_width = be_u16(m_slice.offset_pointer((m_number_of_h_metrics - 1) * (u32) Sizes::LongHorMetric));
    i16 left_side_bearing = be_i16(m_slice.offset_pointer(offset));
    return GlyphHorizontalMetrics {
        .advance_width = advance_width,
        .left_side_bearing = left_side_bearing,
    };
}

u16 Font::Maxp::num_glyphs() const
{
    return be_u16(m_slice.offset_pointer((u32) Offsets::NumGlyphs));
}

RefPtr<Font> Font::load_from_file(const StringView& path, unsigned index)
{
    auto file_or_error = Core::File::open(String(path), Core::IODevice::ReadOnly);
    if (file_or_error.is_error()) {
        dbg() << "Could not open file: " << file_or_error.error();
        return nullptr;
    }
    auto file = file_or_error.value();
    if (!file->open(Core::IODevice::ReadOnly)) {
        dbg() << "Could not open file";
        return nullptr;
    }
    auto buffer = file->read_all();
    if (buffer.size() < 4) {
        dbg() << "Font file too small";
        return nullptr;
    }
    u32 tag = be_u32(buffer.data());
    if (tag == tag_from_str("ttcf")) {
        // It's a font collection
        if (buffer.size() < (u32) Sizes::TTCHeaderV1 + sizeof(u32) * (index + 1)) {
            dbg() << "Font file too small";
            return nullptr;
        }
        u32 offset = be_u32(buffer.offset_pointer((u32) Sizes::TTCHeaderV1 + sizeof(u32) * index));
        return adopt(*new Font(move(buffer), offset));
    }
    if (tag == tag_from_str("OTTO")) {
        dbg() << "CFF fonts not supported yet";
        return nullptr;
    }
    if (tag != 0x00010000) {
        dbg() << "Not a valid  font";
        return nullptr;
    }
    return adopt(*new Font(move(buffer), 0));
}

// FIXME: "loca" and "glyf" are not available for CFF fonts.
Font::Font(ByteBuffer&& buffer, u32 offset)
    : m_buffer(move(buffer))
{
    ASSERT(m_buffer.size() >= offset + (u32) Sizes::OffsetTable);
    Optional<ByteBuffer> head_slice = {};
    Optional<ByteBuffer> hhea_slice = {};
    Optional<ByteBuffer> maxp_slice = {};
    Optional<ByteBuffer> hmtx_slice = {};
    Optional<ByteBuffer> cmap_slice = {};
    Optional<ByteBuffer> loca_slice = {};
    Optional<ByteBuffer> glyf_slice = {};

    //auto sfnt_version = be_u32(data + offset);
    auto num_tables = be_u16(m_buffer.offset_pointer(offset + (u32) Offsets::NumTables));
    ASSERT(m_buffer.size() >= offset + (u32) Sizes::OffsetTable + num_tables * (u32) Sizes::TableRecord);

    for (auto i = 0; i < num_tables; i++) {
        u32 record_offset = offset + (u32) Sizes::OffsetTable + i * (u32) Sizes::TableRecord;
        u32 tag = be_u32(m_buffer.offset_pointer(record_offset));
        u32 table_offset = be_u32(m_buffer.offset_pointer(record_offset + (u32) Offsets::TableRecord_Offset));
        u32 table_length = be_u32(m_buffer.offset_pointer(record_offset + (u32) Offsets::TableRecord_Length));
        ASSERT(m_buffer.size() >= table_offset + table_length);
        auto buffer = ByteBuffer::wrap(m_buffer.offset_pointer(table_offset), table_length);

        // Get the table offsets we need.
        if (tag == tag_from_str("head")) {
            head_slice = buffer;
        } else if (tag == tag_from_str("hhea")) {
            hhea_slice = buffer;
        } else if (tag == tag_from_str("maxp")) {
            maxp_slice = buffer;
        } else if (tag == tag_from_str("hmtx")) {
            hmtx_slice = buffer;
        } else if (tag == tag_from_str("cmap")) {
            cmap_slice = buffer;
        } else if (tag == tag_from_str("loca")) {
            loca_slice = buffer;
        } else if (tag == tag_from_str("glyf")) {
            glyf_slice = buffer;
        }
    }

    // Check that we've got everything we need.
    ASSERT(head_slice.has_value());
    ASSERT(hhea_slice.has_value());
    ASSERT(maxp_slice.has_value());
    ASSERT(hmtx_slice.has_value());
    ASSERT(cmap_slice.has_value());
    ASSERT(loca_slice.has_value());
    ASSERT(glyf_slice.has_value());

    // Load the tables.
    m_head = Head(head_slice.value());
    m_hhea = Hhea(hhea_slice.value());
    m_maxp = Maxp(maxp_slice.value());
    m_hmtx = Hmtx(hmtx_slice.value(), m_maxp.num_glyphs(), m_hhea.number_of_h_metrics());
    m_cmap = Cmap(cmap_slice.value());
    m_loca = Loca(loca_slice.value(), m_maxp.num_glyphs(), m_head.index_to_loc_format());
    m_glyf = Glyf(glyf_slice.value());

    // Select cmap table. FIXME: Do this better. Right now, just looks for platform "Windows"
    // and corresponding encoding "Unicode full repertoire", or failing that, "Unicode BMP"
    for (u32 i = 0; i < m_cmap.num_subtables(); i++) {
        auto opt_subtable = m_cmap.subtable(i);
        if (!opt_subtable.has_value()) {
            continue;
        }
        auto subtable = opt_subtable.value();
        if (subtable.platform_id() == Cmap::Subtable::Platform::Windows) {
            if (subtable.encoding_id() == (u16) Cmap::Subtable::WindowsEncoding::UnicodeFullRepertoire) {
                m_cmap.set_active_index(i);
                break;
            }
            if (subtable.encoding_id() == (u16) Cmap::Subtable::WindowsEncoding::UnicodeBMP) {
                m_cmap.set_active_index(i);
                break;
            }
        }
    }
}

ScaledFontMetrics Font::metrics(float x_scale, float y_scale) const
{
    auto ascender = m_hhea.ascender() * y_scale;
    auto descender = m_hhea.descender() * y_scale;
    auto line_gap = m_hhea.line_gap() * y_scale;
    auto advance_width_max = m_hhea.advance_width_max() * x_scale;
    return ScaledFontMetrics {
        .ascender = (int) roundf(ascender),
        .descender = (int) roundf(descender),
        .line_gap = (int) roundf(line_gap),
        .advance_width_max = (int) roundf(advance_width_max),
    };
}

ScaledGlyphMetrics Font::glyph_metrics(u32 glyph_id, float x_scale, float y_scale) const
{
    if (glyph_id >= m_maxp.num_glyphs()) {
        glyph_id = 0;
    }
    auto horizontal_metrics = m_hmtx.get_glyph_horizontal_metrics(glyph_id);
    auto glyph_offset = m_loca.get_glyph_offset(glyph_id);
    auto glyph = m_glyf.glyph(glyph_offset);
    int ascender = glyph.ascender();
    int descender = glyph.descender();
    return ScaledGlyphMetrics {
        .ascender = (int) roundf(ascender * y_scale),
        .descender = (int) roundf(descender * y_scale),
        .advance_width = (int) roundf(horizontal_metrics.advance_width * x_scale),
        .left_side_bearing = (int) roundf(horizontal_metrics.left_side_bearing * x_scale),
    };
}

// FIXME: "loca" and "glyf" are not available for CFF fonts.
RefPtr<Gfx::Bitmap> Font::raster_glyph(u32 glyph_id, float x_scale, float y_scale) const
{
    if (glyph_id >= m_maxp.num_glyphs()) {
        glyph_id = 0;
    }
    auto glyph_offset = m_loca.get_glyph_offset(glyph_id);
    auto glyph = m_glyf.glyph(glyph_offset);
    return glyph.raster(x_scale, y_scale, [&](u16 glyph_id) {
        if (glyph_id >= m_maxp.num_glyphs()) {
            glyph_id = 0;
        }
        auto glyph_offset = m_loca.get_glyph_offset(glyph_id);
        return m_glyf.glyph(glyph_offset);
    });
}

int ScaledFont::width(const StringView& string) const
{
    Utf8View utf8 { string };
    return width(utf8);
}

int ScaledFont::width(const Utf8View& utf8) const
{
    int width = 0;
    for (u32 codepoint : utf8) {
        u32 glyph_id = glyph_id_for_codepoint(codepoint);
        auto metrics = glyph_metrics(glyph_id);
        width += metrics.advance_width;
    }
    return width;
}

int ScaledFont::width(const Utf32View& utf32) const
{
    int width = 0;
    for (size_t i = 0; i < utf32.length(); i++) {
        u32 glyph_id = glyph_id_for_codepoint(utf32.codepoints()[i]);
        auto metrics = glyph_metrics(glyph_id);
        width += metrics.advance_width;
    }
    return width;
}

}
