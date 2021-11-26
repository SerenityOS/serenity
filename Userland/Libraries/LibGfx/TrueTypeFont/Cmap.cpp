/*
 * Copyright (c) 2020, Srimanta Barua <srimanta.barua1@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Optional.h>
#include <LibGfx/TrueTypeFont/Cmap.h>

namespace TTF {

extern u16 be_u16(u8 const*);
extern u32 be_u32(u8 const*);
extern i16 be_i16(u8 const*);

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
u32 Cmap::Subtable::glyph_id_for_code_point(u32 code_point) const
{
    switch (format()) {
    case Format::SegmentToDelta:
        return glyph_id_for_code_point_table_4(code_point);
    case Format::SegmentedCoverage:
        return glyph_id_for_code_point_table_12(code_point);
    default:
        return 0;
    }
}

u32 Cmap::Subtable::glyph_id_for_code_point_table_4(u32 code_point) const
{
    u32 segcount_x2 = be_u16(m_slice.offset_pointer((u32)Table4Offsets::SegCountX2));
    if (m_slice.size() < segcount_x2 * (u32)Table4Sizes::NonConstMultiplier + (u32)Table4Sizes::Constant) {
        return 0;
    }
    for (u32 offset = 0; offset < segcount_x2; offset += 2) {
        u32 end_code_point = be_u16(m_slice.offset_pointer((u32)Table4Offsets::EndConstBase + offset));
        if (code_point > end_code_point) {
            continue;
        }
        u32 start_code_point = be_u16(m_slice.offset_pointer((u32)Table4Offsets::StartConstBase + segcount_x2 + offset));
        if (code_point < start_code_point) {
            break;
        }
        u32 delta = be_u16(m_slice.offset_pointer((u32)Table4Offsets::DeltaConstBase + segcount_x2 * 2 + offset));
        u32 range = be_u16(m_slice.offset_pointer((u32)Table4Offsets::RangeConstBase + segcount_x2 * 3 + offset));
        if (range == 0) {
            return (code_point + delta) & 0xffff;
        }
        u32 glyph_offset = (u32)Table4Offsets::GlyphOffsetConstBase + segcount_x2 * 3 + offset + range + (code_point - start_code_point) * 2;
        VERIFY(glyph_offset + 2 <= m_slice.size());
        return (be_u16(m_slice.offset_pointer(glyph_offset)) + delta) & 0xffff;
    }
    return 0;
}

u32 Cmap::Subtable::glyph_id_for_code_point_table_12(u32 code_point) const
{
    u32 num_groups = be_u32(m_slice.offset_pointer((u32)Table12Offsets::NumGroups));
    VERIFY(m_slice.size() >= (u32)Table12Sizes::Header + (u32)Table12Sizes::Record * num_groups);
    for (u32 offset = 0; offset < num_groups * (u32)Table12Sizes::Record; offset += (u32)Table12Sizes::Record) {
        u32 start_code_point = be_u32(m_slice.offset_pointer((u32)Table12Offsets::Record_StartCode + offset));
        if (code_point < start_code_point) {
            break;
        }
        u32 end_code_point = be_u32(m_slice.offset_pointer((u32)Table12Offsets::Record_EndCode + offset));
        if (code_point > end_code_point) {
            continue;
        }
        u32 glyph_offset = be_u32(m_slice.offset_pointer((u32)Table12Offsets::Record_StartGlyph + offset));
        return code_point - start_code_point + glyph_offset;
    }
    return 0;
}

u32 Cmap::glyph_id_for_code_point(u32 code_point) const
{
    auto opt_subtable = subtable(m_active_index);
    if (!opt_subtable.has_value()) {
        return 0;
    }
    auto subtable = opt_subtable.value();
    return subtable.glyph_id_for_code_point(code_point);
}

Optional<Cmap> Cmap::from_slice(ReadonlyBytes slice)
{
    if (slice.size() < (size_t)Sizes::TableHeader) {
        return {};
    }
    return Cmap(slice);
}

}
