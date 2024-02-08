/*
 * Copyright (c) 2020, Srimanta Barua <srimanta.barua1@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Error.h>
#include <AK/Format.h>
#include <AK/Optional.h>
#include <LibGfx/Font/OpenType/Cmap.h>

namespace OpenType {

extern u16 be_u16(u8 const*);
extern u32 be_u32(u8 const*);
extern i16 be_i16(u8 const*);

Optional<Cmap::Subtable::Platform> Cmap::Subtable::platform_id() const
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
        return {};
    }
}

Cmap::Subtable::Format Cmap::Subtable::format() const
{
    switch (be_u16(m_slice.offset(0))) {
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
    return be_u16(m_slice.offset((u32)Offsets::NumTables));
}

Optional<Cmap::Subtable> Cmap::subtable(u32 index) const
{
    if (index >= num_subtables()) {
        return {};
    }
    u32 record_offset = (u32)Sizes::TableHeader + index * (u32)Sizes::EncodingRecord;
    if (record_offset + (u32)Offsets::EncodingRecord_Offset + sizeof(u32) > m_slice.size())
        return {};
    u16 platform_id = be_u16(m_slice.offset(record_offset));
    u16 encoding_id = be_u16(m_slice.offset(record_offset + (u32)Offsets::EncodingRecord_EncodingID));
    u32 subtable_offset = be_u32(m_slice.offset(record_offset + (u32)Offsets::EncodingRecord_Offset));
    if (subtable_offset >= m_slice.size())
        return {};
    auto subtable_slice = ReadonlyBytes(m_slice.offset(subtable_offset), m_slice.size() - subtable_offset);
    return Subtable(subtable_slice, platform_id, encoding_id);
}

ErrorOr<void> Cmap::validate_active_cmap_format() const
{
    auto opt_subtable = subtable(m_active_index);
    VERIFY(opt_subtable.has_value());
    return opt_subtable.value().validate_format_can_be_read();
}

ErrorOr<void> Cmap::Subtable::validate_format_can_be_read() const
{
    // Keep in sync with switch in glyph_id_for_code_point().
    switch (format()) {
    case Format::ByteEncoding:
    case Format::SegmentToDelta:
    case Format::TrimmedTable:
    case Format::SegmentedCoverage:
        return {};
    default:
        return Error::from_string_view("Unimplemented cmap format"sv);
    }
}

// FIXME: Implement the missing formats.
u32 Cmap::Subtable::glyph_id_for_code_point(u32 code_point) const
{
    // Keep in sync with switch in validate_format_can_be_read().
    switch (format()) {
    case Format::ByteEncoding:
        return glyph_id_for_code_point_table_0(code_point);
    case Format::SegmentToDelta:
        return glyph_id_for_code_point_table_4(code_point);
    case Format::TrimmedTable:
        return glyph_id_for_code_point_table_6(code_point);
    case Format::SegmentedCoverage:
        return glyph_id_for_code_point_table_12(code_point);
    default:
        dbgln("OpenType Cmap: Unimplemented format {}", (int)format());
        return 0;
    }
}

u32 Cmap::Subtable::glyph_id_for_code_point_table_0(u32 code_point) const
{
    // https://learn.microsoft.com/en-us/typography/opentype/spec/cmap#format-0-byte-encoding-table
    if (code_point > 255)
        return 0;

    return m_slice.at((u32)Table0Offsets::GlyphIdArray + code_point);
}

u32 Cmap::Subtable::glyph_id_for_code_point_table_4(u32 code_point) const
{
    // https://learn.microsoft.com/en-us/typography/opentype/spec/cmap#format-4-segment-mapping-to-delta-values
    u32 segcount_x2 = be_u16(m_slice.offset((u32)Table4Offsets::SegCountX2));
    if (m_slice.size() < segcount_x2 * (u32)Table4Sizes::NonConstMultiplier + (u32)Table4Sizes::Constant)
        return 0;

    u32 segcount = segcount_x2 / 2;
    u32 l = 0, r = segcount - 1;
    while (l < r) {
        u32 mid = l + (r - l) / 2;
        u32 end_code_point_at_mid = be_u16(m_slice.offset((u32)Table4Offsets::EndConstBase + (mid * 2)));
        if (code_point <= end_code_point_at_mid)
            r = mid;
        else
            l = mid + 1;
    }

    u32 offset = l * 2;
    u32 start_code_point = be_u16(m_slice.offset((u32)Table4Offsets::StartConstBase + segcount_x2 + offset));
    if (start_code_point > code_point)
        return 0;

    u32 delta = be_u16(m_slice.offset((u32)Table4Offsets::DeltaConstBase + segcount_x2 * 2 + offset));
    u32 range = be_u16(m_slice.offset((u32)Table4Offsets::RangeConstBase + segcount_x2 * 3 + offset));
    if (range == 0)
        return (code_point + delta) & 0xffff;
    u32 glyph_offset = (u32)Table4Offsets::GlyphOffsetConstBase + segcount_x2 * 3 + offset + range + (code_point - start_code_point) * 2;
    VERIFY(glyph_offset + 2 <= m_slice.size());
    return (be_u16(m_slice.offset(glyph_offset)) + delta) & 0xffff;
}

u32 Cmap::Subtable::glyph_id_for_code_point_table_6(u32 code_point) const
{
    // https://learn.microsoft.com/en-us/typography/opentype/spec/cmap#format-6-trimmed-table-mapping
    u32 first_code = be_u16(m_slice.offset((u32)Table6Offsets::FirstCode));
    if (code_point < first_code)
        return 0;

    u32 entry_count = be_u16(m_slice.offset((u32)Table6Offsets::EntryCount));
    u32 code_offset = code_point - first_code;
    if (code_offset >= entry_count)
        return 0;

    return be_u16(m_slice.offset((u32)Table6Offsets::GlyphIdArray + code_offset * 2));
}

u32 Cmap::Subtable::glyph_id_for_code_point_table_12(u32 code_point) const
{
    // https://learn.microsoft.com/en-us/typography/opentype/spec/cmap#format-12-segmented-coverage
    u32 num_groups = be_u32(m_slice.offset((u32)Table12Offsets::NumGroups));
    VERIFY(m_slice.size() >= (u32)Table12Sizes::Header + (u32)Table12Sizes::Record * num_groups);
    for (u32 offset = 0; offset < num_groups * (u32)Table12Sizes::Record; offset += (u32)Table12Sizes::Record) {
        u32 start_code_point = be_u32(m_slice.offset((u32)Table12Offsets::Record_StartCode + offset));
        if (code_point < start_code_point)
            break;

        u32 end_code_point = be_u32(m_slice.offset((u32)Table12Offsets::Record_EndCode + offset));
        if (code_point > end_code_point)
            continue;

        u32 glyph_offset = be_u32(m_slice.offset((u32)Table12Offsets::Record_StartGlyph + offset));
        return code_point - start_code_point + glyph_offset;
    }
    return 0;
}

u32 Cmap::glyph_id_for_code_point(u32 code_point) const
{
    auto opt_subtable = subtable(m_active_index);
    if (!opt_subtable.has_value())
        return 0;

    auto subtable = opt_subtable.value();
    return subtable.glyph_id_for_code_point(code_point);
}

ErrorOr<Cmap> Cmap::from_slice(ReadonlyBytes slice)
{
    if (slice.size() < (size_t)Sizes::TableHeader)
        return Error::from_string_literal("Could not load Cmap: Not enough data");

    return Cmap(slice);
}

}
