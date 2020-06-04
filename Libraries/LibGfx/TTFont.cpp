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

#include "TTFont.h"
#include <AK/LogStream.h>
#include <bits/stdint.h>
#include <LibCore/File.h>

namespace Gfx {
namespace TTF {

static u16 be_u16(const u8* ptr)
{
    return (((u16) ptr[0]) << 8) | ((u16) ptr[1]);
}

static u32 be_u32(const u8* ptr)
{
    return (((u32) ptr[0]) << 24) | (((u32) ptr[1]) << 16) | (((u32) ptr[2]) << 8) | ((u32) ptr[3]);
}

static i16 be_i16(const u8* ptr)
{
    return (((i16) ptr[0]) << 8) | ((i16) ptr[1]);
}

static u32 tag_from_str(const char *str)
{
    return be_u32((const u8*) str);
}

u16 Font::Head::units_per_em() const
{
    return be_u16(m_slice.offset_pointer(18));
}

i16 Font::Head::xmin() const
{
    return be_i16(m_slice.offset_pointer(36));
}

i16 Font::Head::ymin() const
{
    return be_i16(m_slice.offset_pointer(38));
}

i16 Font::Head::xmax() const
{
    return be_i16(m_slice.offset_pointer(40));
}

i16 Font::Head::ymax() const
{
    return be_i16(m_slice.offset_pointer(42));
}

u16 Font::Head::lowest_recommended_ppem() const
{
    return be_u16(m_slice.offset_pointer(46));
}

Font::IndexToLocFormat Font::Head::index_to_loc_format() const
{
    i16 raw = be_i16(m_slice.offset_pointer(50));
    switch (raw) {
    case 0:
        return IndexToLocFormat::Offset16;
    case 1:
        return IndexToLocFormat::Offset32;
    default:
        ASSERT_NOT_REACHED();
    }
}

u16 Font::Hhea::number_of_h_metrics() const
{
    return be_u16(m_slice.offset_pointer(34));
}

u16 Font::Maxp::num_glyphs() const
{
    return be_u16(m_slice.offset_pointer(4));
}

Font::GlyphHorizontalMetrics Font::Hmtx::get_glyph_horizontal_metrics(u32 glyph_id) const
{
    ASSERT(glyph_id < m_num_glyphs);
    auto offset = glyph_id * 2;
    i16 left_side_bearing = be_i16(m_slice.offset_pointer(offset + 2));
    if (glyph_id < m_number_of_h_metrics) {
        u16 advance_width = be_u16(m_slice.offset_pointer(offset));
        return GlyphHorizontalMetrics {
            .advance_width = advance_width,
            .left_side_bearing = left_side_bearing,
        };
    } else {
        u16 advance_width = be_u16(m_slice.offset_pointer((m_number_of_h_metrics - 1) * 2));
        return GlyphHorizontalMetrics {
            .advance_width = advance_width,
            .left_side_bearing = left_side_bearing,
        };
    }
}

Font::Cmap::Subtable::Platform Font::Cmap::Subtable::platform_id() const
{
    switch (m_raw_platform_id) {
    case 0:  return Platform::Unicode;
    case 1:  return Platform::Macintosh;
    case 3:  return Platform::Windows;
    case 4:  return Platform::Custom;
    default: ASSERT_NOT_REACHED();
    }
}

Font::Cmap::Subtable::Format Font::Cmap::Subtable::format() const
{
    switch (be_u16(m_slice.offset_pointer(0))) {
        case 0:  return Format::ByteEncoding;
        case 2:  return Format::HighByte;
        case 4:  return Format::SegmentToDelta;
        case 6:  return Format::TrimmedTable;
        case 8:  return Format::Mixed16And32;
        case 10: return Format::TrimmedArray;
        case 12: return Format::SegmentedCoverage;
        case 13: return Format::ManyToOneRange;
        case 14: return Format::UnicodeVariationSequences;
        default: ASSERT_NOT_REACHED();
    }
}

u32 Font::Cmap::num_subtables() const
{
    return be_u16(m_slice.offset_pointer(2));
}

Optional<Font::Cmap::Subtable> Font::Cmap::subtable(u32 index) const
{
    if (index >= num_subtables()) {
        return {};
    }
    u32 record_offset = 4 + index * 8;
    u16 platform_id = be_u16(m_slice.offset_pointer(record_offset));
    u16 encoding_id = be_u16(m_slice.offset_pointer(record_offset + 2));
    u32 subtable_offset = be_u32(m_slice.offset_pointer(record_offset + 4));
    ASSERT(subtable_offset < m_slice.size());
    auto subtable_slice = ByteBuffer::wrap(m_slice.offset_pointer(subtable_offset), m_slice.size() - subtable_offset);
    return Subtable(move(subtable_slice), platform_id, encoding_id);
}

// FIXME: This only handles formats 4 (SegmentToDelta) and 12 (SegmentedCoverage) for now.
u32 Font::Cmap::Subtable::glyph_id_for_codepoint(u32 codepoint) const
{
    switch (format()) {
    case Format::SegmentToDelta:
        return glyph_id_for_codepoint_table_4(codepoint);
    case Format::SegmentedCoverage:
        return glyph_id_for_codepoint_table_12(codepoint);
    default:
        return 0;
    }
}

u32 Font::Cmap::Subtable::glyph_id_for_codepoint_table_4(u32 codepoint) const
{
    u32 segcount_x2 = be_u16(m_slice.offset_pointer(6));
    if (m_slice.size() < segcount_x2 * 4 + 16) {
        return 0;
    }
    for (u32 offset = 0; offset < segcount_x2; offset += 2) {
        u32 end_codepoint = be_u16(m_slice.offset_pointer(14 + offset));
        if (codepoint > end_codepoint) {
            continue;
        }
        u32 start_codepoint = be_u16(m_slice.offset_pointer(16 + segcount_x2 + offset));
        if (codepoint < start_codepoint) {
            break;
        }
        u32 delta = be_u16(m_slice.offset_pointer(16 + segcount_x2 * 2 + offset));
        u32 range = be_u16(m_slice.offset_pointer(16 + segcount_x2 * 3 + offset));
        if (range == 0) {
            return (codepoint + delta) & 0xffff;
        } else {
            u32 glyph_offset = 16 + segcount_x2 * 3 + offset + range + (codepoint - start_codepoint) * 2;
            ASSERT(glyph_offset + 2 <= m_slice.size());
            return (be_u16(m_slice.offset_pointer(glyph_offset)) + delta) & 0xffff;
        }
    }
    return 0;
}

u32 Font::Cmap::Subtable::glyph_id_for_codepoint_table_12(u32 codepoint) const
{
    u32 num_groups = be_u32(m_slice.offset_pointer(12));
    ASSERT(m_slice.size() >= 16 + 12 * num_groups);
    for (u32 offset = 0; offset < num_groups * 12; offset += 12) {
        u32 start_codepoint = be_u32(m_slice.offset_pointer(16 + offset));
        if (codepoint < start_codepoint) {
            break;
        }
        u32 end_codepoint = be_u32(m_slice.offset_pointer(20 + offset));
        if (codepoint > end_codepoint) {
            continue;
        }
        u32 glyph_offset = be_u32(m_slice.offset_pointer(24 + offset));
        return codepoint - start_codepoint + glyph_offset;
    }
    return 0;
}

u32 Font::Cmap::glyph_id_for_codepoint(u32 codepoint) const
{
    auto opt_subtable = subtable(m_active_index);
    if (!opt_subtable.has_value()) {
        return 0;
    }
    auto subtable = opt_subtable.value();
    return subtable.glyph_id_for_codepoint(codepoint);
}

u32 Font::Loca::get_glyph_offset(u32 glyph_id) const
{
    ASSERT(glyph_id < m_num_glyphs);
    switch (m_index_to_loc_format) {
    case IndexToLocFormat::Offset16:
        return ((u32) be_u16(m_slice.offset_pointer(glyph_id * 2))) * 2;
    case IndexToLocFormat::Offset32:
        return be_u32(m_slice.offset_pointer(glyph_id * 4));
    default:
        ASSERT_NOT_REACHED();
    }
}

OwnPtr<Font> Font::load_from_file(const StringView& path, unsigned index)
{
    dbg() << "path: " << path << " | index: " << index;
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
        if (buffer.size() < 12 + 4 * (index + 1)) {
            dbg() << "Font file too small";
            return nullptr;
        }
        u32 offset = be_u32(buffer.offset_pointer(12 + 4 * index));
        return OwnPtr(new Font(move(buffer), offset));
    } else if (tag == tag_from_str("OTTO")) {
        dbg() << "CFF fonts not supported yet";
        return nullptr;
    } else if (tag != 0x00010000) {
        dbg() << "Not a valid  font";
        return nullptr;
    } else {
        return OwnPtr(new Font(move(buffer), 0));
    }
}

Font::Glyf::Glyph Font::Glyf::Glyph::simple(ByteBuffer&& slice, u16 num_contours, i16 xmin, i16 ymin, i16 xmax, i16 ymax)
{
    auto ret = Glyph(move(slice), Type::Composite);
    ret.m_meta.simple = Simple {
        .num_contours = num_contours,
        .xmin = xmin,
        .ymin = ymin,
        .xmax = xmax,
        .ymax = ymax,
    };
    dbg() << "Loaded simple glyph:"
          << "\n  num_contours: " << num_contours
          << "\n  xmin: " << xmin
          << "\n  ymin: " << ymin
          << "\n  xmax: " << xmax
          << "\n  ymax: " << ymax;
    return ret;
}

// FIXME: This is currently just a dummy. Need to add support for composite glyphs.
Font::Glyf::Glyph Font::Glyf::Glyph::composite(ByteBuffer&& slice)
{
    auto ret = Glyph(move(slice), Type::Composite);
    ret.m_meta.composite = Composite();
    return ret;
}

Font::Glyf::Glyph Font::Glyf::glyph(u32 offset) const
{
    ASSERT(m_slice.size() >= offset + 10);
    i16 num_contours = be_i16(m_slice.offset_pointer(offset));
    i16 xmin = be_i16(m_slice.offset_pointer(offset + 2));
    i16 ymin = be_i16(m_slice.offset_pointer(offset + 4));
    i16 xmax = be_i16(m_slice.offset_pointer(offset + 6));
    i16 ymax = be_i16(m_slice.offset_pointer(offset + 8));
    auto slice = ByteBuffer::wrap(m_slice.offset_pointer(offset), m_slice.size() - offset);
    if (num_contours < 0) {
        return Glyph::composite(move(slice));
    } else {
        return Glyph::simple(move(slice), num_contours, xmin, ymin, xmax, ymax);
    }
}

// FIXME: "loca" and "glyf" are not available for CFF fonts.
Font::Font(ByteBuffer&& buffer, u32 offset)
    : m_buffer(move(buffer))
{
    ASSERT(m_buffer.size() >= offset + 12);
    Optional<ByteBuffer> head_slice = {};
    Optional<ByteBuffer> hhea_slice = {};
    Optional<ByteBuffer> maxp_slice = {};
    Optional<ByteBuffer> hmtx_slice = {};
    Optional<ByteBuffer> cmap_slice = {};
    Optional<ByteBuffer> loca_slice = {};
    Optional<ByteBuffer> glyf_slice = {};

    //auto sfnt_version = be_u32(data + offset);
    auto num_tables = be_u16(m_buffer.offset_pointer(offset + 4));
    ASSERT(m_buffer.size() >= offset + 12 + num_tables * 16);

    for (auto i = 0; i < num_tables; i++) {
        u32 record_offset = offset + 12 + i * 16;
        u32 tag = be_u32(m_buffer.offset_pointer(record_offset));
        u32 table_offset = be_u32(m_buffer.offset_pointer(record_offset + 8));
        u32 table_length = be_u32(m_buffer.offset_pointer(record_offset + 12));
        ASSERT(m_buffer.size() >= table_offset + table_length);
        auto buffer = ByteBuffer::wrap(m_buffer.offset_pointer(table_offset), table_length);

        // Get the table offsets we need.
        if (tag == tag_from_str("head")) {
            head_slice = move(buffer);
        } else if (tag == tag_from_str("hhea")) {
            hhea_slice = move(buffer);
        } else if (tag == tag_from_str("maxp")) {
            maxp_slice = move(buffer);
        } else if (tag == tag_from_str("hmtx")) {
            hmtx_slice = move(buffer);
        } else if (tag == tag_from_str("cmap")) {
            cmap_slice = move(buffer);
        } else if (tag == tag_from_str("loca")) {
            loca_slice = move(buffer);
        } else if (tag == tag_from_str("glyf")) {
            glyf_slice = move(buffer);
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
    m_head = Head(move(head_slice.value()));
    m_hhea = Hhea(move(hhea_slice.value()));
    m_maxp = Maxp(move(maxp_slice.value()));
    m_hmtx = Hmtx(move(hmtx_slice.value()), m_maxp.num_glyphs(), m_hhea.number_of_h_metrics());
    m_cmap = Cmap(move(cmap_slice.value()));
    m_loca = Loca(move(loca_slice.value()), m_maxp.num_glyphs(), m_head.index_to_loc_format());
    m_glyf = Glyf(move(glyf_slice.value()));

    // Select cmap table. FIXME: Do this better. Right now, just looks for platform "Windows"
    // and corresponding encoding "Unicode full repertoire", or failing that, "Unicode BMP"
    for (u32 i = 0; i < m_cmap.num_subtables(); i++) {
        auto opt_subtable = m_cmap.subtable(i);
        if (!opt_subtable.has_value()) {
            continue;
        }
        auto subtable = opt_subtable.value();
        if (subtable.platform_id() == Cmap::Subtable::Platform::Windows) {
            if (subtable.encoding_id() == 10) {
                m_cmap.set_active_index(i);
                break;
            }
            if (subtable.encoding_id() == 1) {
                m_cmap.set_active_index(i);
                break;
            }
        }
    }

    dbg() << "Glyph ID for 'A': " << m_cmap.glyph_id_for_codepoint('A');
    dbg() << "Glyph ID for 'B': " << m_cmap.glyph_id_for_codepoint('B');

    auto gid = m_cmap.glyph_id_for_codepoint('A');
    auto glyph_offset = m_loca.get_glyph_offset(gid);
    auto glyph = m_glyf.glyph(glyph_offset);
}

}
}
