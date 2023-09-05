/*
 * Copyright (c) 2020, Srimanta Barua <srimanta.barua1@gmail.com>
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 * Copyright (c) 2023, Lukas Affolter <git@lukasach.dev>
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
using Offset32 = BigEndian<u32>;

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

    struct Version1 {
        Version0 version0;
        BigEndian<u32> ul_code_page_range1;
        BigEndian<u32> ul_code_page_range2;
    };

    struct Version2 {
        Version1 version1;
        BigEndian<i16> sx_height;
        BigEndian<i16> s_cap_height;
        BigEndian<u16> us_default_char;
        BigEndian<u16> us_break_char;
        BigEndian<u16> us_max_context;
    };

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
    static Optional<Name> from_slice(ReadonlyBytes);

    String family_name() const { return string_for_id(NameId::FamilyName); }
    String subfamily_name() const { return string_for_id(NameId::SubfamilyName); }
    String typographic_family_name() const { return string_for_id(NameId::TypographicFamilyName); }
    String typographic_subfamily_name() const { return string_for_id(NameId::TypographicSubfamilyName); }

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

// https://learn.microsoft.com/en-us/typography/opentype/spec/eblc
// EBLC — Embedded Bitmap Location Table
class EBLC {
public:
    // https://learn.microsoft.com/en-us/typography/opentype/spec/eblc#sbitlinemetrics-record
    struct SbitLineMetrics {
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

    // https://learn.microsoft.com/en-us/typography/opentype/spec/eblc#indexsubtablearray
    struct IndexSubTableArray {
        BigEndian<u16> first_glyph_index;
        BigEndian<u16> last_glyph_index;
        Offset32 additional_offset_to_index_subtable;
    };

    // https://learn.microsoft.com/en-us/typography/opentype/spec/eblc#indexsubheader
    struct IndexSubHeader {
        BigEndian<u16> index_format;
        BigEndian<u16> image_format;
        Offset32 image_data_offset;
    };

    // https://learn.microsoft.com/en-us/typography/opentype/spec/eblc#indexsubtable1-variable-metrics-glyphs-with-4-byte-offsets
    // IndexSubTable1: variable-metrics glyphs with 4-byte offsets
    struct IndexSubTable1 {
        IndexSubHeader header;
        Offset32 sbit_offsets[];
    };
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/cblc
// CBLC — Color Bitmap Location Table
class CBLC {
public:
    // https://learn.microsoft.com/en-us/typography/opentype/spec/cblc#bitmapsize-record
    struct BitmapSize {
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

    // https://learn.microsoft.com/en-us/typography/opentype/spec/cblc#cblcheader
    struct CblcHeader {
        BigEndian<u16> major_version;
        BigEndian<u16> minor_version;
        BigEndian<u32> num_sizes;
        BitmapSize bitmap_sizes[];
    };

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
    struct SmallGlyphMetrics {
        u8 height {};
        u8 width {};
        i8 bearing_x {};
        i8 bearing_y {};
        u8 advance {};
    };
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/cbdt
// CBDT — Color Bitmap Data Table
class CBDT {
public:
    // https://learn.microsoft.com/en-us/typography/opentype/spec/cbdt#table-structure
    struct CbdtHeader {
        BigEndian<u16> major_version;
        BigEndian<u16> minor_version;
    };

    // https://learn.microsoft.com/en-us/typography/opentype/spec/cbdt#format-17-small-metrics-png-image-data
    struct Format17 {
        EBDT::SmallGlyphMetrics glyph_metrics;
        BigEndian<u32> data_len;
        u8 data[];
    };

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
struct FeatureRecord {
    Tag feature_tag;
    Offset16 feature_offset;
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/chapter2#feature-list-table
struct FeatureList {
    BigEndian<u16> feature_count;
    FeatureRecord feature_records[];
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/chapter2#feature-table
struct Feature {
    Offset16 feature_params_offset;
    BigEndian<u16> lookup_index_count;
    BigEndian<u16> lookup_list_indices[];
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/chapter2#lookup-table
struct Lookup {
    BigEndian<u16> lookup_type;
    BigEndian<u16> lookup_flag;
    BigEndian<u16> subtable_count;
    Offset16 subtable_offsets[];
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/chapter2#lookup-list-table
struct LookupList {
    BigEndian<u16> lookup_count;
    Offset16 lookup_offsets[];
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/chapter2#coverage-format-1
struct CoverageFormat1 {
    BigEndian<u16> coverage_format;
    BigEndian<u16> glyph_count;
    BigEndian<u16> glyph_array[];
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/chapter2#coverage-format-2
struct RangeRecord {
    BigEndian<u16> start_glyph_id;
    BigEndian<u16> end_glyph_id;
    BigEndian<u16> start_coverage_index;
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/chapter2#coverage-format-2
struct CoverageFormat2 {
    BigEndian<u16> coverage_format;
    BigEndian<u16> range_count;
    RangeRecord range_records[];
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/chapter2#class-definition-table-format-2
struct ClassRangeRecord {
    BigEndian<u16> start_glyph_id;
    BigEndian<u16> end_glyph_id;
    BigEndian<u16> class_;
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/chapter2#class-definition-table-format-2
struct ClassDefFormat2 {
    BigEndian<u16> class_format;
    BigEndian<u16> class_range_count;
    ClassRangeRecord class_range_records[];
};

// https://learn.microsoft.com/en-us/typography/opentype/spec/gpos#gpos-header
class GPOS {
public:
    // https://learn.microsoft.com/en-us/typography/opentype/spec/gpos#gpos-header
    struct GPOSHeader {
        BigEndian<u16> major_version;
        BigEndian<u16> minor_version;
        Offset16 script_list_offset;
        Offset16 feature_list_offset;
        Offset16 lookup_list_offset;
    };

    // https://learn.microsoft.com/en-us/typography/opentype/spec/gpos#pair-adjustment-positioning-format-1-adjustments-for-glyph-pairs
    struct PairPosFormat1 {
        BigEndian<u16> pos_format;
        Offset16 coverage_offset;
        BigEndian<u16> value_format1;
        BigEndian<u16> value_format2;
        BigEndian<u16> pair_set_count;
        Offset16 pair_set_offsets[];
    };

    // https://learn.microsoft.com/en-us/typography/opentype/spec/gpos#value-record
    struct ValueRecord {
        BigEndian<i16> x_placement;
        BigEndian<i16> y_placement;
        BigEndian<i16> x_advance;
        BigEndian<i16> y_advance;
        Offset16 x_placement_device_offset;
        Offset16 y_placement_device_offset;
        Offset16 x_advance_device_offset;
        Offset16 y_advance_device_offset;
    };

    // https://learn.microsoft.com/en-us/typography/opentype/spec/gpos#pair-adjustment-positioning-format-2-class-pair-adjustment
    struct PairPosFormat2 {
        BigEndian<u16> pos_format;
        Offset16 coverage_offset;
        BigEndian<u16> value_format1;
        BigEndian<u16> value_format2;
        Offset16 class_def1_offset;
        Offset16 class_def2_offset;
        BigEndian<u16> class1_count;
        BigEndian<u16> class2_count;
    };

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
