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

enum class IndexToLocFormat {
    Offset16,
    Offset32,
};

class Head {
public:
    static Optional<Head> from_slice(const ReadonlyBytes&);
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

    Head(const ReadonlyBytes& slice)
        : m_slice(slice)
    {
    }

    ReadonlyBytes m_slice;
};

class Hhea {
public:
    static Optional<Hhea> from_slice(const ReadonlyBytes&);
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

    Hhea(const ReadonlyBytes& slice)
        : m_slice(slice)
    {
    }

    ReadonlyBytes m_slice;
};

class Maxp {
public:
    static Optional<Maxp> from_slice(const ReadonlyBytes&);
    u16 num_glyphs() const;

private:
    enum class Offsets {
        NumGlyphs = 4
    };
    enum class Sizes {
        TableV0p5 = 6,
    };

    Maxp(const ReadonlyBytes& slice)
        : m_slice(slice)
    {
    }

    ReadonlyBytes m_slice;
};

struct GlyphHorizontalMetrics {
    u16 advance_width;
    i16 left_side_bearing;
};

class Hmtx {
public:
    static Optional<Hmtx> from_slice(const ReadonlyBytes&, u32 num_glyphs, u32 number_of_h_metrics);
    GlyphHorizontalMetrics get_glyph_horizontal_metrics(u32 glyph_id) const;

private:
    enum class Sizes {
        LongHorMetric = 4,
        LeftSideBearing = 2
    };

    Hmtx(const ReadonlyBytes& slice, u32 num_glyphs, u32 number_of_h_metrics)
        : m_slice(slice)
        , m_num_glyphs(num_glyphs)
        , m_number_of_h_metrics(number_of_h_metrics)
    {
    }

    ReadonlyBytes m_slice;
    u32 m_num_glyphs { 0 };
    u32 m_number_of_h_metrics { 0 };
};

class Name {
public:
    static Optional<Name> from_slice(const ReadonlyBytes&);

    String family_name() const { return string_for_id(NameId::FamilyName); }
    String subfamily_name() const { return string_for_id(NameId::SubfamilyName); }
    String typographic_family_name() const { return string_for_id(NameId::TypographicFamilyName); }
    String typographic_subfamily_name() const { return string_for_id(NameId::TypographicSubfamilyName); }

private:
    enum class NameId {
        Copyright = 0,
        FamilyName = 1,
        SubfamilyName = 2,
        UniqueIdentifier = 3,
        FullName = 4,
        VersionString = 5,
        PostscriptName = 6,
        Trademark = 7,
        Manufacturer = 8,
        Designer = 9,
        Description = 10,
        TypographicFamilyName = 16,
        TypographicSubfamilyName = 17,
    };

    Name(const ReadonlyBytes& slice)
        : m_slice(slice)
    {
    }

    String string_for_id(NameId id) const;

    ReadonlyBytes m_slice;
};

}
