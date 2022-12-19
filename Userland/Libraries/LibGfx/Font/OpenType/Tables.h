/*
 * Copyright (c) 2020, Srimanta Barua <srimanta.barua1@gmail.com>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Error.h>
#include <AK/FixedArray.h>
#include <AK/Span.h>

namespace OpenType {

enum class IndexToLocFormat {
    Offset16,
    Offset32,
};

struct Fixed {
    BigEndian<u16> integer;
    BigEndian<u16> fraction;
};

struct LongDateTime {
    BigEndian<u64> value;
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/head
// head: Font Header Table
class Head {
public:
    static Optional<Head> from_slice(ReadonlyBytes);
    u16 units_per_em() const;
    i16 xmin() const;
    i16 ymin() const;
    i16 xmax() const;
    i16 ymax() const;
    u16 style() const;
    u16 lowest_recommended_ppem() const;
    IndexToLocFormat index_to_loc_format() const;

private:
    struct FontHeaderTable {
        BigEndian<u16> major_version;
        BigEndian<u16> minor_version;
        Fixed font_revision;
        BigEndian<u32> checksum_adjustment;
        BigEndian<u32> magic_number;
        BigEndian<u16> flags;
        BigEndian<u16> units_per_em;
        LongDateTime created;
        LongDateTime modified;
        BigEndian<i16> x_min;
        BigEndian<i16> y_min;
        BigEndian<i16> x_max;
        BigEndian<i16> y_max;
        BigEndian<u16> mac_style;
        BigEndian<u16> lowest_rec_ppem;
        BigEndian<i16> font_direction_hint;
        BigEndian<i16> index_to_loc_format;
        BigEndian<i16> glyph_data_format;
    };

    FontHeaderTable const& header() const { return *bit_cast<FontHeaderTable const*>(m_slice.data()); }

    Head(ReadonlyBytes slice)
        : m_slice(slice)
    {
    }

    ReadonlyBytes m_slice;
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/hhea
// hhea - Horizontal Header Table
class Hhea {
public:
    static Optional<Hhea> from_slice(ReadonlyBytes);
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

    Hhea(ReadonlyBytes slice)
        : m_slice(slice)
    {
    }

    ReadonlyBytes m_slice;
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/maxp
// Maxp: Maximum Profile
class Maxp {
public:
    static Optional<Maxp> from_slice(ReadonlyBytes);
    u16 num_glyphs() const;

private:
    enum class Offsets {
        NumGlyphs = 4
    };
    enum class Sizes {
        TableV0p5 = 6,
    };

    Maxp(ReadonlyBytes slice)
        : m_slice(slice)
    {
    }

    ReadonlyBytes m_slice;
};

struct GlyphHorizontalMetrics {
    u16 advance_width;
    i16 left_side_bearing;
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/hmtx
// hmtx: Horizontal Metrics Table
class Hmtx {
public:
    static Optional<Hmtx> from_slice(ReadonlyBytes, u32 num_glyphs, u32 number_of_h_metrics);
    GlyphHorizontalMetrics get_glyph_horizontal_metrics(u32 glyph_id) const;

private:
    enum class Sizes {
        LongHorMetric = 4,
        LeftSideBearing = 2
    };

    Hmtx(ReadonlyBytes slice, u32 num_glyphs, u32 number_of_h_metrics)
        : m_slice(slice)
        , m_num_glyphs(num_glyphs)
        , m_number_of_h_metrics(number_of_h_metrics)
    {
    }

    ReadonlyBytes m_slice;
    u32 m_num_glyphs { 0 };
    u32 m_number_of_h_metrics { 0 };
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/os2
// OS/2: OS/2 and Windows Metrics Table
class OS2 {
public:
    enum class Offsets {
        WeightClass = 4,
        Selection = 62,
        TypographicAscender = 68,
        TypographicDescender = 70,
        TypographicLineGap = 72,
        End = 78,
    };

    u16 weight_class() const;
    u16 selection() const;
    i16 typographic_ascender() const;
    i16 typographic_descender() const;
    i16 typographic_line_gap() const;

    explicit OS2(ReadonlyBytes slice)
        : m_slice(slice)
    {
    }

private:
    ReadonlyBytes m_slice;
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/name
// name: Naming Table
class Name {
public:
    enum class Platform {
        Unicode = 0,
        Macintosh = 1,
        Windows = 3,
    };
    enum class MacintoshLanguage {
        English = 0,
    };
    enum class WindowsLanguage {
        EnglishUnitedStates = 0x0409,
    };
    static Optional<Name> from_slice(ReadonlyBytes);

    DeprecatedString family_name() const { return string_for_id(NameId::FamilyName); }
    DeprecatedString subfamily_name() const { return string_for_id(NameId::SubfamilyName); }
    DeprecatedString typographic_family_name() const { return string_for_id(NameId::TypographicFamilyName); }
    DeprecatedString typographic_subfamily_name() const { return string_for_id(NameId::TypographicSubfamilyName); }

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

    Name(ReadonlyBytes slice)
        : m_slice(slice)
    {
    }

    DeprecatedString string_for_id(NameId id) const;

    ReadonlyBytes m_slice;
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/kern
// kern - Kerning
class Kern {
public:
    static ErrorOr<Kern> from_slice(ReadonlyBytes);
    i16 get_glyph_kerning(u16 left_glyph_id, u16 right_glyph_id) const;

private:
    enum Sizes : size_t {
        SubtableHeader = 6,
        Format0Entry = 6,
    };

    Kern(ReadonlyBytes slice, FixedArray<size_t> subtable_offsets)
        : m_slice(slice)
        , m_subtable_offsets(move(subtable_offsets))
    {
    }

    static Optional<i16> read_glyph_kerning_format0(ReadonlyBytes slice, u16 left_glyph_id, u16 right_glyph_id);

    ReadonlyBytes m_slice;
    FixedArray<size_t> m_subtable_offsets;
};

}
