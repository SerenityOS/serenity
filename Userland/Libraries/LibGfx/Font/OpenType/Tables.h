/*
 * Copyright (c) 2020, Srimanta Barua <srimanta.barua1@gmail.com>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 * Copyright (c) 2023, Lukas Affolter <git@lukasach.dev>
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/DeprecatedString.h>
#include <AK/Endian.h>
#include <AK/Error.h>
#include <AK/FixedArray.h>
#include <AK/Forward.h>
#include <AK/Span.h>
#include <AK/String.h>

namespace OpenType {

enum class IndexToLocFormat {
    Offset16,
    Offset32,
};

struct [[gnu::packed]] Fixed {
    BigEndian<u16> integer;
    BigEndian<u16> fraction;
};
static_assert(AssertSize<Fixed, 4>());

struct [[gnu::packed]] LongDateTime {
    BigEndian<u64> value;
};
static_assert(AssertSize<LongDateTime, 8>());

struct [[gnu::packed]] Version16Dot16 {
    BigEndian<u16> major;
    BigEndian<u16> minor;
};
static_assert(AssertSize<Version16Dot16, 4>());

using FWord = BigEndian<i16>;
using UFWord = BigEndian<u16>;
using Tag = BigEndian<u32>;
using Offset16 = BigEndian<u16>;
using Offset32 = BigEndian<u32>;

// https://learn.microsoft.com/en-us/typography/opentype/spec/otff#table-directory
// Table Directory (known as the Offset Table in ISO-IEC 14496-22:2019)
struct [[gnu::packed]] TableDirectory {
    BigEndian<u32> sfnt_version;
    BigEndian<u16> num_tables;     // Number of tables.
    BigEndian<u16> search_range;   // (Maximum power of 2 <= numTables) x 16.
    BigEndian<u16> entry_selector; // Log2(maximum power of 2 <= numTables).
    BigEndian<u16> range_shift;    // NumTables x 16 - searchRange.
};
static_assert(AssertSize<TableDirectory, 12>());

// https://learn.microsoft.com/en-us/typography/opentype/spec/otff#table-directory
struct [[gnu::packed]] TableRecord {
    Tag table_tag;           // Table identifier.
    BigEndian<u32> checksum; // CheckSum for this table.
    Offset32 offset;         // Offset from beginning of TrueType font file.
    BigEndian<u32> length;   // Length of this table.
};
static_assert(AssertSize<TableRecord, 16>());

}

template<>
class AK::Traits<OpenType::TableDirectory const> : public GenericTraits<OpenType::TableDirectory const> {
public:
    static constexpr bool is_trivially_serializable() { return true; }
};

template<>
class AK::Traits<OpenType::TableRecord const> : public GenericTraits<OpenType::TableRecord const> {
public:
    static constexpr bool is_trivially_serializable() { return true; }
};

namespace OpenType {

// https://learn.microsoft.com/en-us/typography/opentype/spec/head
// head: Font Header Table
class Head {
public:
    static ErrorOr<Head> from_slice(ReadonlyBytes);
    u16 units_per_em() const;
    i16 xmin() const;
    i16 ymin() const;
    i16 xmax() const;
    i16 ymax() const;
    u16 style() const;
    u16 lowest_recommended_ppem() const;
    IndexToLocFormat index_to_loc_format() const;

private:
    struct [[gnu::packed]] FontHeaderTable {
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
    static_assert(AssertSize<FontHeaderTable, 54>());

    Head(FontHeaderTable const& font_header_table)
        : m_data(font_header_table)
    {
    }

    FontHeaderTable const& m_data;
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/hhea
// hhea - Horizontal Header Table
class Hhea {
public:
    static ErrorOr<Hhea> from_slice(ReadonlyBytes);
    i16 ascender() const;
    i16 descender() const;
    i16 line_gap() const;
    u16 advance_width_max() const;
    u16 number_of_h_metrics() const;

private:
    struct [[gnu::packed]] HorizontalHeaderTable {
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
    static_assert(AssertSize<HorizontalHeaderTable, 36>());

    Hhea(HorizontalHeaderTable const& horizontal_header_table)
        : m_data(horizontal_header_table)
    {
    }

    HorizontalHeaderTable const& m_data;
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/maxp
// Maxp: Maximum Profile
class Maxp {
public:
    static ErrorOr<Maxp> from_slice(ReadonlyBytes);

    u16 num_glyphs() const;

private:
    struct [[gnu::packed]] MaximumProfileVersion0_5 {
        Version16Dot16 version;
        BigEndian<u16> num_glyphs;
    };
    static_assert(AssertSize<MaximumProfileVersion0_5, 6>());

    struct [[gnu::packed]] MaximumProfileVersion1_0 {
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
    static_assert(AssertSize<MaximumProfileVersion1_0, 32>());

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
    static ErrorOr<Hmtx> from_slice(ReadonlyBytes, u32 num_glyphs, u32 number_of_h_metrics);
    GlyphHorizontalMetrics get_glyph_horizontal_metrics(u32 glyph_id) const;

private:
    struct [[gnu::packed]] LongHorMetric {
        BigEndian<u16> advance_width;
        BigEndian<i16> lsb;
    };
    static_assert(AssertSize<LongHorMetric, 4>());

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
    u16 width_class() const;
    u16 selection() const;
    i16 typographic_ascender() const;
    i16 typographic_descender() const;
    i16 typographic_line_gap() const;

    bool use_typographic_metrics() const;

    [[nodiscard]] Optional<i16> x_height() const;

    explicit OS2(ReadonlyBytes slice)
        : m_slice(slice)
    {
    }

private:
    struct [[gnu::packed]] Version0 {
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
    static_assert(AssertSize<Version0, 78>());

    struct [[gnu::packed]] Version1 {
        Version0 version0;
        BigEndian<u32> ul_code_page_range1;
        BigEndian<u32> ul_code_page_range2;
    };
    static_assert(AssertSize<Version1, 86>());

    struct [[gnu::packed]] Version2 {
        Version1 version1;
        BigEndian<i16> sx_height;
        BigEndian<i16> s_cap_height;
        BigEndian<u16> us_default_char;
        BigEndian<u16> us_break_char;
        BigEndian<u16> us_max_context;
    };
    static_assert(AssertSize<Version2, 96>());

    Version0 const& header() const { return *bit_cast<Version0 const*>(m_slice.data()); }
    Version2 const& header_v2() const
    {
        VERIFY(header().version >= 2);
        return *bit_cast<Version2 const*>(m_slice.data());
    }

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
    static ErrorOr<Name> from_slice(ReadonlyBytes);

    String family_name() const { return string_for_id(NameId::FamilyName); }
    String subfamily_name() const { return string_for_id(NameId::SubfamilyName); }
    String typographic_family_name() const { return string_for_id(NameId::TypographicFamilyName); }
    String typographic_subfamily_name() const { return string_for_id(NameId::TypographicSubfamilyName); }

private:
    // https://learn.microsoft.com/en-us/typography/opentype/spec/name#name-records
    struct [[gnu::packed]] NameRecord {
        BigEndian<u16> platform_id;
        BigEndian<u16> encoding_id;
        BigEndian<u16> language_id;
        BigEndian<u16> name_id;
        BigEndian<u16> length;
        Offset16 string_offset;
    };
    static_assert(AssertSize<NameRecord, 12>());

    // https://learn.microsoft.com/en-us/typography/opentype/spec/name#naming-table-version-0
    struct [[gnu::packed]] NamingTableVersion0 {
        BigEndian<u16> version;
        BigEndian<u16> count;
        Offset16 storage_offset;
        NameRecord name_record[0];
    };
    static_assert(AssertSize<NamingTableVersion0, 6>());

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

    [[nodiscard]] String string_for_id(NameId) const;

    ReadonlyBytes m_slice;
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/kern
// kern - Kerning
class Kern {
public:
    static ErrorOr<Kern> from_slice(ReadonlyBytes);
    i16 get_glyph_kerning(u16 left_glyph_id, u16 right_glyph_id) const;

private:
    struct [[gnu::packed]] Header {
        BigEndian<u16> version;
        BigEndian<u16> n_tables;
    };
    static_assert(AssertSize<Header, 4>());

    struct [[gnu::packed]] SubtableHeader {
        BigEndian<u16> version;
        BigEndian<u16> length;
        BigEndian<u16> coverage;
    };
    static_assert(AssertSize<SubtableHeader, 6>());

    // https://learn.microsoft.com/en-us/typography/opentype/spec/kern#format-0
    struct [[gnu::packed]] Format0 {
        BigEndian<u16> n_pairs;
        BigEndian<u16> search_range;
        BigEndian<u16> entry_selector;
        BigEndian<u16> range_shift;
    };
    static_assert(AssertSize<Format0, 8>());

    struct [[gnu::packed]] Format0Pair {
        BigEndian<u16> left;
        BigEndian<u16> right;
        FWord value;
    };
    static_assert(AssertSize<Format0Pair, 6>());

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

// https://learn.microsoft.com/en-us/typography/opentype/spec/eblc
// EBLC — Embedded Bitmap Location Table
class EBLC {
public:
    // https://learn.microsoft.com/en-us/typography/opentype/spec/eblc#sbitlinemetrics-record
    struct [[gnu::packed]] SbitLineMetrics {
        i8 ascender {};
        i8 descender {};
        u8 width_max {};
        i8 caret_slope_numerator {};
        i8 caret_slope_denominator {};
        i8 caret_offset {};
        i8 min_origin_sb {};
        i8 min_advance_sb {};
        i8 max_before_bl {};
        i8 min_after_bl {};
        i8 pad1 {};
        i8 pad2 {};
    };
    static_assert(AssertSize<SbitLineMetrics, 12>());

    // https://learn.microsoft.com/en-us/typography/opentype/spec/eblc#indexsubtablearray
    struct [[gnu::packed]] IndexSubTableArray {
        BigEndian<u16> first_glyph_index;
        BigEndian<u16> last_glyph_index;
        Offset32 additional_offset_to_index_subtable;
    };
    static_assert(AssertSize<IndexSubTableArray, 8>());

    // https://learn.microsoft.com/en-us/typography/opentype/spec/eblc#indexsubheader
    struct [[gnu::packed]] IndexSubHeader {
        BigEndian<u16> index_format;
        BigEndian<u16> image_format;
        Offset32 image_data_offset;
    };
    static_assert(AssertSize<IndexSubHeader, 8>());

    // https://learn.microsoft.com/en-us/typography/opentype/spec/eblc#indexsubtable1-variable-metrics-glyphs-with-4-byte-offsets
    // IndexSubTable1: variable-metrics glyphs with 4-byte offsets
    struct [[gnu::packed]] IndexSubTable1 {
        IndexSubHeader header;
        Offset32 sbit_offsets[];
    };
    static_assert(AssertSize<IndexSubTable1, 8>());
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/cblc
// CBLC — Color Bitmap Location Table
class CBLC {
public:
    // https://learn.microsoft.com/en-us/typography/opentype/spec/cblc#bitmapsize-record
    struct [[gnu::packed]] BitmapSize {
        Offset32 index_subtable_array_offset;
        BigEndian<u32> index_tables_size;
        BigEndian<u32> number_of_index_subtables;
        BigEndian<u32> color_ref;
        EBLC::SbitLineMetrics hori;
        EBLC::SbitLineMetrics vert;
        BigEndian<u16> start_glyph_index;
        BigEndian<u16> end_glyph_index;
        u8 ppem_x {};
        u8 ppem_y {};
        u8 bit_depth {};
        i8 flags {};
    };
    static_assert(AssertSize<BitmapSize, 48>());

    // https://learn.microsoft.com/en-us/typography/opentype/spec/cblc#cblcheader
    struct [[gnu::packed]] CblcHeader {
        BigEndian<u16> major_version;
        BigEndian<u16> minor_version;
        BigEndian<u32> num_sizes;
        BitmapSize bitmap_sizes[];
    };
    static_assert(AssertSize<CblcHeader, 8>());

    CblcHeader const& header() const { return *bit_cast<CblcHeader const*>(m_slice.data()); }
    ReadonlySpan<BitmapSize> bitmap_sizes() const { return { header().bitmap_sizes, header().num_sizes }; }
    Optional<BitmapSize const&> bitmap_size_for_glyph_id(u32 glyph_id) const;

    static ErrorOr<CBLC> from_slice(ReadonlyBytes);
    ReadonlyBytes bytes() const { return m_slice; }

    Optional<EBLC::IndexSubHeader const&> index_subtable_for_glyph_id(u32 glyph_id, u16& first_glyph_index, u16& last_glyph_index) const;

private:
    explicit CBLC(ReadonlyBytes slice)
        : m_slice(slice)
    {
    }

    ReadonlyBytes m_slice;
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/ebdt
// EBDT — Embedded Bitmap Data Table
class EBDT {
public:
    // https://learn.microsoft.com/en-us/typography/opentype/spec/ebdt#smallglyphmetrics
    struct [[gnu::packed]] SmallGlyphMetrics {
        u8 height {};
        u8 width {};
        i8 bearing_x {};
        i8 bearing_y {};
        u8 advance {};
    };
    static_assert(AssertSize<SmallGlyphMetrics, 5>());
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/cbdt
// CBDT — Color Bitmap Data Table
class CBDT {
public:
    // https://learn.microsoft.com/en-us/typography/opentype/spec/cbdt#table-structure
    struct [[gnu::packed]] CbdtHeader {
        BigEndian<u16> major_version;
        BigEndian<u16> minor_version;
    };
    static_assert(AssertSize<CbdtHeader, 4>());

    // https://learn.microsoft.com/en-us/typography/opentype/spec/cbdt#format-17-small-metrics-png-image-data
    struct [[gnu::packed]] Format17 {
        EBDT::SmallGlyphMetrics glyph_metrics;
        BigEndian<u32> data_len;
        u8 data[];
    };
    static_assert(AssertSize<Format17, 9>());

    static ErrorOr<CBDT> from_slice(ReadonlyBytes);
    ReadonlyBytes bytes() const { return m_slice; }

    CbdtHeader const& header() const { return *bit_cast<CbdtHeader const*>(m_slice.data()); }

private:
    explicit CBDT(ReadonlyBytes slice)
        : m_slice(slice)
    {
    }

    ReadonlyBytes m_slice;
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/chapter2#feature-list-table
struct [[gnu::packed]] FeatureRecord {
    Tag feature_tag;
    Offset16 feature_offset;
};
static_assert(AssertSize<FeatureRecord, 6>());

// https://learn.microsoft.com/en-us/typography/opentype/spec/chapter2#feature-list-table
struct [[gnu::packed]] FeatureList {
    BigEndian<u16> feature_count;
    FeatureRecord feature_records[];
};
static_assert(AssertSize<FeatureList, 2>());

// https://learn.microsoft.com/en-us/typography/opentype/spec/chapter2#feature-table
struct [[gnu::packed]] Feature {
    Offset16 feature_params_offset;
    BigEndian<u16> lookup_index_count;
    BigEndian<u16> lookup_list_indices[];
};
static_assert(AssertSize<Feature, 4>());

// https://learn.microsoft.com/en-us/typography/opentype/spec/chapter2#lookup-table
struct [[gnu::packed]] Lookup {
    BigEndian<u16> lookup_type;
    BigEndian<u16> lookup_flag;
    BigEndian<u16> subtable_count;
    Offset16 subtable_offsets[];
};
static_assert(AssertSize<Lookup, 6>());

// https://learn.microsoft.com/en-us/typography/opentype/spec/chapter2#lookup-list-table
struct [[gnu::packed]] LookupList {
    BigEndian<u16> lookup_count;
    Offset16 lookup_offsets[];
};
static_assert(AssertSize<LookupList, 2>());

// https://learn.microsoft.com/en-us/typography/opentype/spec/chapter2#coverage-format-1
struct [[gnu::packed]] CoverageFormat1 {
    BigEndian<u16> coverage_format;
    BigEndian<u16> glyph_count;
    BigEndian<u16> glyph_array[];
};
static_assert(AssertSize<CoverageFormat1, 4>());

// https://learn.microsoft.com/en-us/typography/opentype/spec/chapter2#coverage-format-2
struct [[gnu::packed]] RangeRecord {
    BigEndian<u16> start_glyph_id;
    BigEndian<u16> end_glyph_id;
    BigEndian<u16> start_coverage_index;
};
static_assert(AssertSize<RangeRecord, 6>());

// https://learn.microsoft.com/en-us/typography/opentype/spec/chapter2#coverage-format-2
struct [[gnu::packed]] CoverageFormat2 {
    BigEndian<u16> coverage_format;
    BigEndian<u16> range_count;
    RangeRecord range_records[];
};
static_assert(AssertSize<CoverageFormat2, 4>());

// https://learn.microsoft.com/en-us/typography/opentype/spec/chapter2#class-definition-table-format-2
struct [[gnu::packed]] ClassRangeRecord {
    BigEndian<u16> start_glyph_id;
    BigEndian<u16> end_glyph_id;
    BigEndian<u16> class_;
};
static_assert(AssertSize<ClassRangeRecord, 6>());

// https://learn.microsoft.com/en-us/typography/opentype/spec/chapter2#class-definition-table-format-2
struct [[gnu::packed]] ClassDefFormat2 {
    BigEndian<u16> class_format;
    BigEndian<u16> class_range_count;
    ClassRangeRecord class_range_records[];
};
static_assert(AssertSize<ClassDefFormat2, 4>());

// https://learn.microsoft.com/en-us/typography/opentype/spec/gpos#gpos-header
class GPOS {
public:
    // https://learn.microsoft.com/en-us/typography/opentype/spec/gpos#gpos-header
    struct [[gnu::packed]] GPOSHeader {
        BigEndian<u16> major_version;
        BigEndian<u16> minor_version;
        Offset16 script_list_offset;
        Offset16 feature_list_offset;
        Offset16 lookup_list_offset;
    };
    static_assert(AssertSize<GPOSHeader, 10>());

    // https://learn.microsoft.com/en-us/typography/opentype/spec/gpos#pair-adjustment-positioning-format-1-adjustments-for-glyph-pairs
    struct [[gnu::packed]] PairPosFormat1 {
        BigEndian<u16> pos_format;
        Offset16 coverage_offset;
        BigEndian<u16> value_format1;
        BigEndian<u16> value_format2;
        BigEndian<u16> pair_set_count;
        Offset16 pair_set_offsets[];
    };
    static_assert(AssertSize<PairPosFormat1, 10>());

    // https://learn.microsoft.com/en-us/typography/opentype/spec/gpos#value-record
    struct [[gnu::packed]] ValueRecord {
        BigEndian<i16> x_placement;
        BigEndian<i16> y_placement;
        BigEndian<i16> x_advance;
        BigEndian<i16> y_advance;
        Offset16 x_placement_device_offset;
        Offset16 y_placement_device_offset;
        Offset16 x_advance_device_offset;
        Offset16 y_advance_device_offset;
    };
    static_assert(AssertSize<ValueRecord, 16>());

    // https://learn.microsoft.com/en-us/typography/opentype/spec/gpos#pair-adjustment-positioning-format-2-class-pair-adjustment
    struct [[gnu::packed]] PairPosFormat2 {
        BigEndian<u16> pos_format;
        Offset16 coverage_offset;
        BigEndian<u16> value_format1;
        BigEndian<u16> value_format2;
        Offset16 class_def1_offset;
        Offset16 class_def2_offset;
        BigEndian<u16> class1_count;
        BigEndian<u16> class2_count;
    };
    static_assert(AssertSize<PairPosFormat2, 16>());

    enum class ValueFormat : u16 {
        X_PLACEMENT = 0x0001,
        Y_PLACEMENT = 0x0002,
        X_ADVANCE = 0x0004,
        Y_ADVANCE = 0x0008,
        X_PLACEMENT_DEVICE = 0x0010,
        Y_PLACEMENT_DEVICE = 0x0020,
        X_ADVANCE_DEVICE = 0x0040,
        Y_ADVANCE_DEVICE = 0x0080,
    };

    GPOSHeader const& header() const { return *bit_cast<GPOSHeader const*>(m_slice.data()); }

    Optional<i16> glyph_kerning(u16 left_glyph_id, u16 right_glyph_id) const;

    static ErrorOr<GPOS> from_slice(ReadonlyBytes slice) { return GPOS { slice }; }

private:
    GPOS(ReadonlyBytes slice)
        : m_slice(slice)
    {
    }

    ReadonlyBytes m_slice;
};
}
