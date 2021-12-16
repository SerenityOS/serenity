/*
 * Copyright (c) 2020, Srimanta Barua <srimanta.barua1@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Span.h>
#include <stdint.h>

namespace TTF {

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

        Subtable(ReadonlyBytes slice, u16 platform_id, u16 encoding_id)
            : m_slice(slice)
            , m_raw_platform_id(platform_id)
            , m_encoding_id(encoding_id)
        {
        }
        // Returns 0 if glyph not found. This corresponds to the "missing glyph"
        u32 glyph_id_for_code_point(u32 code_point) const;
        Optional<Platform> platform_id() const;
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

        u32 glyph_id_for_code_point_table_4(u32 code_point) const;
        u32 glyph_id_for_code_point_table_12(u32 code_point) const;

        ReadonlyBytes m_slice;
        u16 m_raw_platform_id { 0 };
        u16 m_encoding_id { 0 };
    };

    static Optional<Cmap> from_slice(ReadonlyBytes);
    u32 num_subtables() const;
    Optional<Subtable> subtable(u32 index) const;
    void set_active_index(u32 index) { m_active_index = index; }
    // Returns 0 if glyph not found. This corresponds to the "missing glyph"
    u32 glyph_id_for_code_point(u32 code_point) const;

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

    Cmap(ReadonlyBytes slice)
        : m_slice(slice)
    {
    }

    ReadonlyBytes m_slice;
    u32 m_active_index { UINT32_MAX };
};

}
