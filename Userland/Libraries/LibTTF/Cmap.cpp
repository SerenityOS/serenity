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

#include <AK/Optional.h>
#include <LibTTF/Cmap.h>

namespace TTF {

extern u16 be_u16(const u8* ptr);
extern u32 be_u32(const u8* ptr);
extern i16 be_i16(const u8* ptr);

Cmap::Subtable::Platform Cmap::Subtable::platform_id() const
{
    switch (m_raw_platform_id) {
    case 0:
        return Platform::Unicode;
    case 1:
        return Platform::Macintosh;
    case 3:
        return Platform::Windows;
    case 4:
        return Platform::Custom;
    default:
        VERIFY_NOT_REACHED();
    }
}

Cmap::Subtable::Format Cmap::Subtable::format() const
{
    switch (be_u16(m_slice.offset_pointer(0))) {
    case 0:
        return Format::ByteEncoding;
    case 2:
        return Format::HighByte;
    case 4:
        return Format::SegmentToDelta;
    case 6:
        return Format::TrimmedTable;
    case 8:
        return Format::Mixed16And32;
    case 10:
        return Format::TrimmedArray;
    case 12:
        return Format::SegmentedCoverage;
    case 13:
        return Format::ManyToOneRange;
    case 14:
        return Format::UnicodeVariationSequences;
    default:
        VERIFY_NOT_REACHED();
    }
}

u32 Cmap::num_subtables() const
{
    return be_u16(m_slice.offset_pointer((u32)Offsets::NumTables));
}

Optional<Cmap::Subtable> Cmap::subtable(u32 index) const
{
    if (index >= num_subtables()) {
        return {};
    }
    u32 record_offset = (u32)Sizes::TableHeader + index * (u32)Sizes::EncodingRecord;
    u16 platform_id = be_u16(m_slice.offset_pointer(record_offset));
    u16 encoding_id = be_u16(m_slice.offset_pointer(record_offset + (u32)Offsets::EncodingRecord_EncodingID));
    u32 subtable_offset = be_u32(m_slice.offset_pointer(record_offset + (u32)Offsets::EncodingRecord_Offset));
    VERIFY(subtable_offset < m_slice.size());
    auto subtable_slice = ReadonlyBytes(m_slice.offset_pointer(subtable_offset), m_slice.size() - subtable_offset);
    return Subtable(subtable_slice, platform_id, encoding_id);
}

// FIXME: This only handles formats 4 (SegmentToDelta) and 12 (SegmentedCoverage) for now.
u32 Cmap::Subtable::glyph_id_for_codepoint(u32 codepoint) const
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

u32 Cmap::Subtable::glyph_id_for_codepoint_table_4(u32 codepoint) const
{
    u32 segcount_x2 = be_u16(m_slice.offset_pointer((u32)Table4Offsets::SegCountX2));
    if (m_slice.size() < segcount_x2 * (u32)Table4Sizes::NonConstMultiplier + (u32)Table4Sizes::Constant) {
        return 0;
    }
    for (u32 offset = 0; offset < segcount_x2; offset += 2) {
        u32 end_codepoint = be_u16(m_slice.offset_pointer((u32)Table4Offsets::EndConstBase + offset));
        if (codepoint > end_codepoint) {
            continue;
        }
        u32 start_codepoint = be_u16(m_slice.offset_pointer((u32)Table4Offsets::StartConstBase + segcount_x2 + offset));
        if (codepoint < start_codepoint) {
            break;
        }
        u32 delta = be_u16(m_slice.offset_pointer((u32)Table4Offsets::DeltaConstBase + segcount_x2 * 2 + offset));
        u32 range = be_u16(m_slice.offset_pointer((u32)Table4Offsets::RangeConstBase + segcount_x2 * 3 + offset));
        if (range == 0) {
            return (codepoint + delta) & 0xffff;
        }
        u32 glyph_offset = (u32)Table4Offsets::GlyphOffsetConstBase + segcount_x2 * 3 + offset + range + (codepoint - start_codepoint) * 2;
        VERIFY(glyph_offset + 2 <= m_slice.size());
        return (be_u16(m_slice.offset_pointer(glyph_offset)) + delta) & 0xffff;
    }
    return 0;
}

u32 Cmap::Subtable::glyph_id_for_codepoint_table_12(u32 codepoint) const
{
    u32 num_groups = be_u32(m_slice.offset_pointer((u32)Table12Offsets::NumGroups));
    VERIFY(m_slice.size() >= (u32)Table12Sizes::Header + (u32)Table12Sizes::Record * num_groups);
    for (u32 offset = 0; offset < num_groups * (u32)Table12Sizes::Record; offset += (u32)Table12Sizes::Record) {
        u32 start_codepoint = be_u32(m_slice.offset_pointer((u32)Table12Offsets::Record_StartCode + offset));
        if (codepoint < start_codepoint) {
            break;
        }
        u32 end_codepoint = be_u32(m_slice.offset_pointer((u32)Table12Offsets::Record_EndCode + offset));
        if (codepoint > end_codepoint) {
            continue;
        }
        u32 glyph_offset = be_u32(m_slice.offset_pointer((u32)Table12Offsets::Record_StartGlyph + offset));
        return codepoint - start_codepoint + glyph_offset;
    }
    return 0;
}

u32 Cmap::glyph_id_for_codepoint(u32 codepoint) const
{
    auto opt_subtable = subtable(m_active_index);
    if (!opt_subtable.has_value()) {
        return 0;
    }
    auto subtable = opt_subtable.value();
    return subtable.glyph_id_for_codepoint(codepoint);
}

Optional<Cmap> Cmap::from_slice(const ReadonlyBytes& slice)
{
    if (slice.size() < (size_t)Sizes::TableHeader) {
        return {};
    }
    return Cmap(slice);
}

}
