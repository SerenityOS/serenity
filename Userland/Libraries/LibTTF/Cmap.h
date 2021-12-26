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

#include <AK/Span.h>

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

        Subtable(const ReadonlyBytes& slice, u16 platform_id, u16 encoding_id)
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

        ReadonlyBytes m_slice;
        u16 m_raw_platform_id { 0 };
        u16 m_encoding_id { 0 };
    };

    static Optional<Cmap> from_slice(const ReadonlyBytes&);
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

    Cmap(const ReadonlyBytes& slice)
        : m_slice(slice)
    {
    }

    ReadonlyBytes m_slice;
    u32 m_active_index { UINT32_MAX };
};

}
