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

struct Version16Dot16 {
    BigEndian<u16> major;
    BigEndian<u16> minor;
};

using FWord = BigEndian<i16>;
using UFWord = BigEndian<u16>;
using Tag = BigEndian<u32>;
using Offset16 = BigEndian<u16>;

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
    struct HorizontalHeaderTable {
        BigEndian<u16> major_version;
        BigEndian<u16> minor_version;
        FWord ascender;
        FWord descender;
        FWord line_gap;
        UFWord advance_width_max;
        FWord min_left_side_bearing;
        FWord min_right_side_bearing;
        FWord x_max_extent;
        BigEndian<i16> caret_slope_rise;
        BigEndian<i16> caret_slope_run;
        BigEndian<i16> caret_offset;
        BigEndian<i16> reserved[4];
        BigEndian<i16> metric_data_format;
        BigEndian<u16> number_of_h_metrics;
    };

    HorizontalHeaderTable const& header() const { return *bit_cast<HorizontalHeaderTable const*>(m_slice.data()); }

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
    struct MaximumProfileVersion0_5 {
        Version16Dot16 version;
        BigEndian<u16> num_glyphs;
    };

    struct MaximumProfileVersion1_0 {
        Version16Dot16 version;
        BigEndian<u16> num_glyphs;
        BigEndian<u16> max_points;
        BigEndian<u16> max_contours;
        BigEndian<u16> max_composite_points;
        BigEndian<u16> max_composite_contours;
        BigEndian<u16> max_zones;
        BigEndian<u16> max_twilight_points;
        BigEndian<u16> max_storage;
        BigEndian<u16> max_function_defs;
        BigEndian<u16> max_instruction_defs;
        BigEndian<u16> max_stack_elements;
        BigEndian<u16> max_size_of_instructions;
        BigEndian<u16> max_component_elements;
        BigEndian<u16> max_component_depths;
    };

    MaximumProfileVersion0_5 const& header() const { return *bit_cast<MaximumProfileVersion0_5 const*>(m_slice.data()); }

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

// https://learn.microsoft.com/en-us/typography/opentype/spec/fpgm
// fpgm: Font Program
struct Fpgm {
public:
    explicit Fpgm(ReadonlyBytes slice)
        : m_slice(slice)
    {
    }

    ReadonlyBytes program_data() const { return m_slice; }

private:
    ReadonlyBytes m_slice;
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/prep
// prep: Control Value Program
struct Prep {
public:
    explicit Prep(ReadonlyBytes slice)
        : m_slice(slice)
    {
    }

    ReadonlyBytes program_data() const { return m_slice; }

private:
    ReadonlyBytes m_slice;
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/hmtx
// hmtx: Horizontal Metrics Table
class Hmtx {
public:
    static Optional<Hmtx> from_slice(ReadonlyBytes, u32 num_glyphs, u32 number_of_h_metrics);
    GlyphHorizontalMetrics get_glyph_horizontal_metrics(u32 glyph_id) const;

private:
    struct LongHorMetric {
        BigEndian<u16> advance_width;
        BigEndian<i16> lsb;
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
    u16 weight_class() const;
    u16 selection() const;
    i16 typographic_ascender() const;
    i16 typographic_descender() const;
    i16 typographic_line_gap() const;

    bool use_typographic_metrics() const;

    explicit OS2(ReadonlyBytes slice)
        : m_slice(slice)
    {
    }

private:
    struct Version0 {
        BigEndian<u16> version;
        BigEndian<i16> avg_char_width;
        BigEndian<u16> us_weight_class;
        BigEndian<u16> us_width_class;
        BigEndian<u16> fs_type;
        BigEndian<i16> y_subscript_x_size;
        BigEndian<i16> y_subscript_y_size;
        BigEndian<i16> y_subscript_x_offset;
        BigEndian<i16> y_subscript_y_offset;
        BigEndian<i16> y_superscript_x_size;
        BigEndian<i16> y_superscript_y_size;
        BigEndian<i16> y_superscript_x_offset;
        BigEndian<i16> y_superscript_y_offset;
        BigEndian<i16> y_strikeout_size;
        BigEndian<i16> y_strikeout_position;
        BigEndian<i16> s_family_class;
        u8 panose[10];
        BigEndian<u32> ul_unicode_range1;
        BigEndian<u32> ul_unicode_range2;
        BigEndian<u32> ul_unicode_range3;
        BigEndian<u32> ul_unicode_range4;
        Tag ach_vend_id;
        BigEndian<u16> fs_selection;
        BigEndian<u16> fs_first_char_index;
        BigEndian<u16> us_last_char_index;
        BigEndian<i16> s_typo_ascender;
        BigEndian<i16> s_typo_descender;
        BigEndian<i16> s_typo_line_gap;
        BigEndian<u16> us_win_ascent;
        BigEndian<u16> us_win_descent;
    };

    Version0 const& header() const { return *bit_cast<Version0 const*>(m_slice.data()); }

    ReadonlyBytes m_slice;
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/name
// name: Naming Table
class Name {
public:
    enum class Platform : u16 {
        Unicode = 0,
        Macintosh = 1,
        Windows = 3,
    };
    enum class MacintoshLanguage : u16 {
        English = 0,
    };
    enum class WindowsLanguage : u16 {
        EnglishUnitedStates = 0x0409,
    };
    static Optional<Name> from_slice(ReadonlyBytes);

    DeprecatedString family_name() const { return string_for_id(NameId::FamilyName); }
    DeprecatedString subfamily_name() const { return string_for_id(NameId::SubfamilyName); }
    DeprecatedString typographic_family_name() const { return string_for_id(NameId::TypographicFamilyName); }
    DeprecatedString typographic_subfamily_name() const { return string_for_id(NameId::TypographicSubfamilyName); }

private:
    // https://learn.microsoft.com/en-us/typography/opentype/spec/name#name-records
    struct NameRecord {
        BigEndian<u16> platform_id;
        BigEndian<u16> encoding_id;
        BigEndian<u16> language_id;
        BigEndian<u16> name_id;
        BigEndian<u16> length;
        Offset16 string_offset;
    };

    // https://learn.microsoft.com/en-us/typography/opentype/spec/name#naming-table-version-0
    struct NamingTableVersion0 {
        BigEndian<u16> version;
        BigEndian<u16> count;
        Offset16 storage_offset;
        NameRecord name_record[0];
    };

    NamingTableVersion0 const& header() const { return *bit_cast<NamingTableVersion0 const*>(m_slice.data()); }

    enum class NameId : u16 {
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

    DeprecatedString string_for_id(NameId) const;

    ReadonlyBytes m_slice;
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/kern
// kern - Kerning
class Kern {
public:
    static ErrorOr<Kern> from_slice(ReadonlyBytes);
    i16 get_glyph_kerning(u16 left_glyph_id, u16 right_glyph_id) const;

private:
    struct Header {
        BigEndian<u16> version;
        BigEndian<u16> n_tables;
    };

    struct SubtableHeader {
        BigEndian<u16> version;
        BigEndian<u16> length;
        BigEndian<u16> coverage;
    };

    // https://learn.microsoft.com/en-us/typography/opentype/spec/kern#format-0
    struct Format0 {
        BigEndian<u16> n_pairs;
        BigEndian<u16> search_range;
        BigEndian<u16> entry_selector;
        BigEndian<u16> range_shift;
    };

    struct Format0Pair {
        BigEndian<u16> left;
        BigEndian<u16> right;
        FWord value;
    };

    Header const& header() const { return *bit_cast<Header const*>(m_slice.data()); }

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
