/*
 * Copyright (c) 2022, Luke Wilde <lukew@serenityos.org>
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2023, Sam Atkins <atkinssj@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/BitStream.h>
#include <AK/MemoryStream.h>
#include <LibCompress/Brotli.h>
#include <LibCore/Resource.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Font/OpenType/Font.h>
#include <LibGfx/Font/WOFF2/Font.h>

// The following is an implementation of the WOFF2 specification.
// https://www.w3.org/TR/WOFF2/

namespace WOFF2 {

// https://www.w3.org/TR/WOFF2/#woff20Header
struct [[gnu::packed]] Header {
    BigEndian<u32> signature;             // 0x774F4632 'wOF2'
    BigEndian<u32> flavor;                // The "sfnt version" of the input font.
    BigEndian<u32> length;                // Total size of the WOFF file.
    BigEndian<u16> num_tables;            // Number of entries in directory of font tables.
    BigEndian<u16> reserved;              // Reserved; set to 0.
    BigEndian<u32> total_sfnt_size;       // Total size needed for the uncompressed font data, including the sfnt header,
                                          // directory, and font tables (including padding).
    BigEndian<u32> total_compressed_size; // Total length of the compressed data block.
    BigEndian<u16> major_version;         // Major version of the WOFF file.
    BigEndian<u16> minor_version;         // Minor version of the WOFF file.
    BigEndian<u32> meta_offset;           // Offset to metadata block, from beginning of WOFF file.
    BigEndian<u32> meta_length;           // Length of compressed metadata block.
    BigEndian<u32> meta_orig_length;      // Uncompressed size of metadata block.
    BigEndian<u32> priv_offset;           // Offset to private data block, from beginning of WOFF file.
    BigEndian<u32> priv_length;           // Length of private data block.
};
static_assert(AssertSize<Header, 48>());

}

template<>
class AK::Traits<WOFF2::Header> : public DefaultTraits<WOFF2::Header> {
public:
    static constexpr bool is_trivially_serializable() { return true; }
};

namespace WOFF2 {

static constexpr u32 WOFF2_SIGNATURE = 0x774F4632;
static constexpr u32 TTCF_SIGNAURE = 0x74746366;
static constexpr size_t SFNT_HEADER_SIZE = 12;
static constexpr size_t SFNT_TABLE_SIZE = 16;

[[maybe_unused]] static ErrorOr<u16> read_255_u_short(FixedMemoryStream& stream)
{
    constexpr u8 one_more_byte_code_1 = 255;
    constexpr u8 one_more_byte_code_2 = 254;
    constexpr u8 word_code = 253;
    constexpr u8 lowest_u_code = 253;
    constexpr u16 lowest_u_code_multiplied_by_2 = lowest_u_code * 2;

    auto code = TRY(stream.read_value<u8>());

    if (code == word_code) {
        return TRY(stream.read_value<BigEndian<u16>>());
    }

    if (code == one_more_byte_code_1) {
        u16 final_value = TRY(stream.read_value<u8>());
        final_value += lowest_u_code;
        return final_value;
    }

    if (code == one_more_byte_code_2) {
        u16 final_value = TRY(stream.read_value<u8>());
        final_value += lowest_u_code_multiplied_by_2;
        return final_value;
    }
    return code;
}

static ErrorOr<u32> read_uint_base_128(SeekableStream& stream)
{
    u32 accumulator = 0;

    for (u8 i = 0; i < 5; ++i) {
        u8 const next_byte = TRY(stream.read_value<u8>());

        if (i == 0 && next_byte == 0x80)
            return Error::from_string_literal("UIntBase128 type contains a leading zero");

        if (accumulator & 0xfe000000)
            return Error::from_string_literal("UIntBase128 type exceeds the length of a u32");

        accumulator = (accumulator << 7) | (next_byte & 0x7F);

        if ((next_byte & 0x80) == 0)
            return accumulator;
    }

    return Error::from_string_literal("UIntBase128 type is larger than 5 bytes");
}

static i16 be_i16(u8 const* ptr)
{
    return (((i16)ptr[0]) << 8) | ((i16)ptr[1]);
}

static u16 pow_2_less_than_or_equal(u16 x)
{
    VERIFY(x > 0);
    VERIFY(x < 32769);
    return 1 << (sizeof(u16) * 8 - count_leading_zeroes_safe<u16>(x - 1));
}

enum class TransformationVersion {
    Version0,
    Version1,
    Version2,
    Version3,
};

struct TableDirectoryEntry {
    TransformationVersion transformation_version { TransformationVersion::Version0 };
    OpenType::Tag tag;
    u32 original_length { 0 };
    Optional<u32> transform_length;

    bool has_transformation() const
    {
        return transform_length.has_value();
    }
};

// NOTE: Any tags less than 4 characters long are padded with spaces at the end.
static constexpr Array<OpenType::Tag, 63> known_tag_names = {
    OpenType::Tag("cmap"),
    OpenType::Tag("head"),
    OpenType::Tag("hhea"),
    OpenType::Tag("hmtx"),
    OpenType::Tag("maxp"),
    OpenType::Tag("name"),
    OpenType::Tag("OS/2"),
    OpenType::Tag("post"),
    OpenType::Tag("cvt "),
    OpenType::Tag("fpgm"),
    OpenType::Tag("glyf"),
    OpenType::Tag("loca"),
    OpenType::Tag("prep"),
    OpenType::Tag("CFF "),
    OpenType::Tag("VORG"),
    OpenType::Tag("EBDT"),
    OpenType::Tag("EBLC"),
    OpenType::Tag("gasp"),
    OpenType::Tag("hdmx"),
    OpenType::Tag("kern"),
    OpenType::Tag("LTSH"),
    OpenType::Tag("PCLT"),
    OpenType::Tag("VDMX"),
    OpenType::Tag("vhea"),
    OpenType::Tag("vmtx"),
    OpenType::Tag("BASE"),
    OpenType::Tag("GDEF"),
    OpenType::Tag("GPOS"),
    OpenType::Tag("GSUB"),
    OpenType::Tag("EBSC"),
    OpenType::Tag("JSTF"),
    OpenType::Tag("MATH"),
    OpenType::Tag("CBDT"),
    OpenType::Tag("CBLC"),
    OpenType::Tag("COLR"),
    OpenType::Tag("CPAL"),
    OpenType::Tag("SVG "),
    OpenType::Tag("sbix"),
    OpenType::Tag("acnt"),
    OpenType::Tag("avar"),
    OpenType::Tag("bdat"),
    OpenType::Tag("bloc"),
    OpenType::Tag("bsln"),
    OpenType::Tag("cvar"),
    OpenType::Tag("fdsc"),
    OpenType::Tag("feat"),
    OpenType::Tag("fmtx"),
    OpenType::Tag("fvar"),
    OpenType::Tag("gvar"),
    OpenType::Tag("hsty"),
    OpenType::Tag("just"),
    OpenType::Tag("lcar"),
    OpenType::Tag("mort"),
    OpenType::Tag("morx"),
    OpenType::Tag("opbd"),
    OpenType::Tag("prop"),
    OpenType::Tag("trak"),
    OpenType::Tag("Zapf"),
    OpenType::Tag("Silf"),
    OpenType::Tag("Glat"),
    OpenType::Tag("Gloc"),
    OpenType::Tag("Feat"),
    OpenType::Tag("Sill"),
};

struct CoordinateTripletEncoding {
    u8 byte_count { 0 };
    u8 x_bits { 0 };
    u8 y_bits { 0 };
    Optional<u16> delta_x;
    Optional<u16> delta_y;
    Optional<bool> positive_x;
    Optional<bool> positive_y;
};

// https://www.w3.org/TR/WOFF2/#triplet_decoding
// 5.2. Decoding of variable-length X and Y coordinates
static CoordinateTripletEncoding const coordinate_triplet_encodings[128] = {
    { 2, 0, 8, {}, 0, {}, false },       // 0
    { 2, 0, 8, {}, 0, {}, true },        // 1
    { 2, 0, 8, {}, 256, {}, false },     // 2
    { 2, 0, 8, {}, 256, {}, true },      // 3
    { 2, 0, 8, {}, 512, {}, false },     // 4
    { 2, 0, 8, {}, 512, {}, true },      // 5
    { 2, 0, 8, {}, 768, {}, false },     // 6
    { 2, 0, 8, {}, 768, {}, true },      // 7
    { 2, 0, 8, {}, 1024, {}, false },    // 8
    { 2, 0, 8, {}, 1024, {}, true },     // 9
    { 2, 8, 0, 0, {}, false, {} },       // 10
    { 2, 8, 0, 0, {}, true, {} },        // 11
    { 2, 8, 0, 256, {}, false, {} },     // 12
    { 2, 8, 0, 256, {}, true, {} },      // 13
    { 2, 8, 0, 512, {}, false, {} },     // 14
    { 2, 8, 0, 512, {}, true, {} },      // 15
    { 2, 8, 0, 768, {}, false, {} },     // 16
    { 2, 8, 0, 768, {}, true, {} },      // 17
    { 2, 8, 0, 1024, {}, false, {} },    // 18
    { 2, 8, 0, 1024, {}, true, {} },     // 19
    { 2, 4, 4, 1, 1, false, false },     // 20
    { 2, 4, 4, 1, 1, true, false },      // 21
    { 2, 4, 4, 1, 1, false, true },      // 22
    { 2, 4, 4, 1, 1, true, true },       // 23
    { 2, 4, 4, 1, 17, false, false },    // 24
    { 2, 4, 4, 1, 17, true, false },     // 25
    { 2, 4, 4, 1, 17, false, true },     // 26
    { 2, 4, 4, 1, 17, true, true },      // 27
    { 2, 4, 4, 1, 33, false, false },    // 28
    { 2, 4, 4, 1, 33, true, false },     // 29
    { 2, 4, 4, 1, 33, false, true },     // 30
    { 2, 4, 4, 1, 33, true, true },      // 31
    { 2, 4, 4, 1, 49, false, false },    // 32
    { 2, 4, 4, 1, 49, true, false },     // 33
    { 2, 4, 4, 1, 49, false, true },     // 34
    { 2, 4, 4, 1, 49, true, true },      // 35
    { 2, 4, 4, 17, 1, false, false },    // 36
    { 2, 4, 4, 17, 1, true, false },     // 37
    { 2, 4, 4, 17, 1, false, true },     // 38
    { 2, 4, 4, 17, 1, true, true },      // 39
    { 2, 4, 4, 17, 17, false, false },   // 40
    { 2, 4, 4, 17, 17, true, false },    // 41
    { 2, 4, 4, 17, 17, false, true },    // 42
    { 2, 4, 4, 17, 17, true, true },     // 43
    { 2, 4, 4, 17, 33, false, false },   // 44
    { 2, 4, 4, 17, 33, true, false },    // 45
    { 2, 4, 4, 17, 33, false, true },    // 46
    { 2, 4, 4, 17, 33, true, true },     // 47
    { 2, 4, 4, 17, 49, false, false },   // 48
    { 2, 4, 4, 17, 49, true, false },    // 49
    { 2, 4, 4, 17, 49, false, true },    // 50
    { 2, 4, 4, 17, 49, true, true },     // 51
    { 2, 4, 4, 33, 1, false, false },    // 52
    { 2, 4, 4, 33, 1, true, false },     // 53
    { 2, 4, 4, 33, 1, false, true },     // 54
    { 2, 4, 4, 33, 1, true, true },      // 55
    { 2, 4, 4, 33, 17, false, false },   // 56
    { 2, 4, 4, 33, 17, true, false },    // 57
    { 2, 4, 4, 33, 17, false, true },    // 58
    { 2, 4, 4, 33, 17, true, true },     // 59
    { 2, 4, 4, 33, 33, false, false },   // 60
    { 2, 4, 4, 33, 33, true, false },    // 61
    { 2, 4, 4, 33, 33, false, true },    // 62
    { 2, 4, 4, 33, 33, true, true },     // 63
    { 2, 4, 4, 33, 49, false, false },   // 64
    { 2, 4, 4, 33, 49, true, false },    // 65
    { 2, 4, 4, 33, 49, false, true },    // 66
    { 2, 4, 4, 33, 49, true, true },     // 67
    { 2, 4, 4, 49, 1, false, false },    // 68
    { 2, 4, 4, 49, 1, true, false },     // 69
    { 2, 4, 4, 49, 1, false, true },     // 70
    { 2, 4, 4, 49, 1, true, true },      // 71
    { 2, 4, 4, 49, 17, false, false },   // 72
    { 2, 4, 4, 49, 17, true, false },    // 73
    { 2, 4, 4, 49, 17, false, true },    // 74
    { 2, 4, 4, 49, 17, true, true },     // 75
    { 2, 4, 4, 49, 33, false, false },   // 76
    { 2, 4, 4, 49, 33, true, false },    // 77
    { 2, 4, 4, 49, 33, false, true },    // 78
    { 2, 4, 4, 49, 33, true, true },     // 79
    { 2, 4, 4, 49, 49, false, false },   // 80
    { 2, 4, 4, 49, 49, true, false },    // 81
    { 2, 4, 4, 49, 49, false, true },    // 82
    { 2, 4, 4, 49, 49, true, true },     // 83
    { 3, 8, 8, 1, 1, false, false },     // 84
    { 3, 8, 8, 1, 1, true, false },      // 85
    { 3, 8, 8, 1, 1, false, true },      // 86
    { 3, 8, 8, 1, 1, true, true },       // 87
    { 3, 8, 8, 1, 257, false, false },   // 88
    { 3, 8, 8, 1, 257, true, false },    // 89
    { 3, 8, 8, 1, 257, false, true },    // 90
    { 3, 8, 8, 1, 257, true, true },     // 91
    { 3, 8, 8, 1, 513, false, false },   // 92
    { 3, 8, 8, 1, 513, true, false },    // 93
    { 3, 8, 8, 1, 513, false, true },    // 94
    { 3, 8, 8, 1, 513, true, true },     // 95
    { 3, 8, 8, 257, 1, false, false },   // 96
    { 3, 8, 8, 257, 1, true, false },    // 97
    { 3, 8, 8, 257, 1, false, true },    // 98
    { 3, 8, 8, 257, 1, true, true },     // 99
    { 3, 8, 8, 257, 257, false, false }, // 100
    { 3, 8, 8, 257, 257, true, false },  // 101
    { 3, 8, 8, 257, 257, false, true },  // 102
    { 3, 8, 8, 257, 257, true, true },   // 103
    { 3, 8, 8, 257, 513, false, false }, // 104
    { 3, 8, 8, 257, 513, true, false },  // 105
    { 3, 8, 8, 257, 513, false, true },  // 106
    { 3, 8, 8, 257, 513, true, true },   // 107
    { 3, 8, 8, 513, 1, false, false },   // 108
    { 3, 8, 8, 513, 1, true, false },    // 109
    { 3, 8, 8, 513, 1, false, true },    // 110
    { 3, 8, 8, 513, 1, true, true },     // 111
    { 3, 8, 8, 513, 257, false, false }, // 112
    { 3, 8, 8, 513, 257, true, false },  // 113
    { 3, 8, 8, 513, 257, false, true },  // 114
    { 3, 8, 8, 513, 257, true, true },   // 115
    { 3, 8, 8, 513, 513, false, false }, // 116
    { 3, 8, 8, 513, 513, true, false },  // 117
    { 3, 8, 8, 513, 513, false, true },  // 118
    { 3, 8, 8, 513, 513, true, true },   // 119
    { 4, 12, 12, 0, 0, false, false },   // 120
    { 4, 12, 12, 0, 0, true, false },    // 121
    { 4, 12, 12, 0, 0, false, true },    // 122
    { 4, 12, 12, 0, 0, true, true },     // 123
    { 5, 16, 16, 0, 0, false, false },   // 124
    { 5, 16, 16, 0, 0, true, false },    // 125
    { 5, 16, 16, 0, 0, false, true },    // 126
    { 5, 16, 16, 0, 0, true, true },     // 127
};

struct FontPoint {
    i16 x { 0 };
    i16 y { 0 };
    bool on_curve { false };
};

static ErrorOr<Vector<FontPoint>> retrieve_points_of_simple_glyph(FixedMemoryStream& flags_stream, FixedMemoryStream& glyph_stream, u16 number_of_points)
{
    Vector<FontPoint> points;
    TRY(points.try_ensure_capacity(number_of_points));

    i16 x = 0;
    i16 y = 0;

    for (u32 point = 0; point < number_of_points; ++point) {
        u8 flags = TRY(flags_stream.read_value<u8>());
        bool on_curve = (flags & 0x80) == 0;
        u8 coordinate_triplet_index = flags & 0x7F;

        auto const& coordinate_triplet_encoding = coordinate_triplet_encodings[coordinate_triplet_index];

        // The byte_count in the array accounts for the flags, but we already read them in from a different stream.
        u8 const byte_count_not_including_flags = coordinate_triplet_encoding.byte_count - 1;

        u8 point_coordinates_buffer[4];
        Bytes point_coordinates { point_coordinates_buffer, byte_count_not_including_flags };
        TRY(glyph_stream.read_until_filled(point_coordinates));

        int delta_x = 0;
        int delta_y = 0;

        switch (coordinate_triplet_encoding.x_bits) {
        case 0:
            break;
        case 4:
            delta_x = static_cast<i16>(point_coordinates[0] >> 4);
            break;
        case 8:
            delta_x = static_cast<i16>(point_coordinates[0]);
            break;
        case 12:
            delta_x = (static_cast<i16>(point_coordinates[0]) << 4) | (static_cast<i16>(point_coordinates[1]) >> 4);
            break;
        case 16:
            delta_x = be_i16(point_coordinates.data());
            break;
        default:
            VERIFY_NOT_REACHED();
        }

        switch (coordinate_triplet_encoding.y_bits) {
        case 0:
            break;
        case 4:
            delta_y = static_cast<i16>(point_coordinates[0] & 0x0f);
            break;
        case 8:
            delta_y = byte_count_not_including_flags == 2 ? static_cast<i16>(point_coordinates[1]) : static_cast<i16>(point_coordinates[0]);
            break;
        case 12:
            delta_y = (static_cast<i16>(point_coordinates[1] & 0x0f) << 8) | static_cast<i16>(point_coordinates[2]);
            break;
        case 16:
            delta_y = be_i16(point_coordinates.offset(2));
            break;
        default:
            VERIFY_NOT_REACHED();
        }

        if (coordinate_triplet_encoding.delta_x.has_value()) {
            if (Checked<i16>::addition_would_overflow(delta_x, coordinate_triplet_encoding.delta_x.value()))
                return Error::from_string_literal("EOVERFLOW 3");

            delta_x += coordinate_triplet_encoding.delta_x.value();
        }

        if (coordinate_triplet_encoding.delta_y.has_value()) {
            if (Checked<i16>::addition_would_overflow(delta_y, coordinate_triplet_encoding.delta_y.value()))
                return Error::from_string_literal("EOVERFLOW 4");

            delta_y += coordinate_triplet_encoding.delta_y.value();
        }

        if (coordinate_triplet_encoding.positive_x.has_value() && !coordinate_triplet_encoding.positive_x.value())
            delta_x = -delta_x;

        if (coordinate_triplet_encoding.positive_y.has_value() && !coordinate_triplet_encoding.positive_y.value())
            delta_y = -delta_y;

        if (Checked<i16>::addition_would_overflow(x, delta_x))
            return Error::from_string_literal("EOVERFLOW 5");

        if (Checked<i16>::addition_would_overflow(y, delta_y))
            return Error::from_string_literal("EOVERFLOW 6");

        x += delta_x;
        y += delta_y;

        points.unchecked_append(FontPoint { .x = x, .y = y, .on_curve = on_curve });
    }

    return points;
}

// https://www.w3.org/TR/WOFF2/#glyf_table_format
struct [[gnu::packed]] TransformedGlyfTable {
    BigEndian<u16> reserved;                // = 0x0000
    BigEndian<u16> option_flags;            // Bit 0: if set, indicates the presence of the overlapSimpleBitmap[] bit array.
                                            // Bits 1-15: Reserved.
    BigEndian<u16> num_glyphs;              // Number of glyphs
    BigEndian<u16> index_format;            // Offset format for loca table, should be consistent with indexToLocFormat of the
                                            // original head table (see [OFF] specification)
    BigEndian<u32> n_contour_stream_size;   // Size of nContour stream in bytes
    BigEndian<u32> n_points_stream_size;    // Size of nPoints stream in bytes
    BigEndian<u32> flag_stream_size;        // Size of flag stream in bytes
    BigEndian<u32> glyph_stream_size;       // Size of glyph stream in bytes (a stream of variable-length encoded values, see
                                            // description below)
    BigEndian<u32> composite_stream_size;   // Size of composite stream in bytes (a stream of variable-length encoded values,
                                            // see description below)
    BigEndian<u32> bbox_stream_size;        // Size of bbox data in bytes representing combined length of bboxBitmap
                                            // (a packed bit array) and bboxStream (a stream of Int16 values)
    BigEndian<u32> instruction_stream_size; // Size of instruction stream (a stream of UInt8 values)

    // Other fields are variable-length, and so are not represented in this struct:
    // Int16      nContourStream[]       Stream of Int16 values representing number of contours for each glyph record
    // 255UInt16  nPointsStream[]        Stream of values representing number of outline points for each contour in glyph records
    // UInt8      flagStream[]           Stream of UInt8 values representing flag values for each outline point.
    // Vary       glyphStream[]          Stream of bytes representing point coordinate values using variable length encoding
    //                                   format (defined in subclause 5.2)
    // Vary       compositeStream[]      Stream of bytes representing component flag values and associated composite glyph data
    // UInt8      bboxBitmap[]           Bitmap (a numGlyphs-long bit array) indicating explicit bounding boxes
    // Int16      bboxStream[]           Stream of Int16 values representing glyph bounding box data
    // UInt8      instructionStream[]    Stream of UInt8 values representing a set of instructions for each corresponding glyph
    // UInt8      overlapSimpleBitmap[]  A numGlyphs-long bit array that provides values for the overlap flag [bit 6] for each
    //                                   simple glyph. (Flag values for composite glyphs are already encoded as part of the
    //                                   compositeStream[]).
};
static_assert(AssertSize<TransformedGlyfTable, 36>());

}

template<>
class AK::Traits<WOFF2::TransformedGlyfTable> : public DefaultTraits<WOFF2::TransformedGlyfTable> {
public:
    static constexpr bool is_trivially_serializable() { return true; }
};

namespace WOFF2 {

enum class LocaElementSize {
    TwoBytes,
    FourBytes,
};

struct GlyfAndLocaTableBuffers {
    ByteBuffer glyf_table;
    ByteBuffer loca_table;
};

enum SimpleGlyphFlags : u8 {
    OnCurve = 0x01,
    XShortVector = 0x02,
    YShortVector = 0x04,
    RepeatFlag = 0x08,
    XIsSameOrPositiveXShortVector = 0x10,
    YIsSameOrPositiveYShortVector = 0x20,
};

static ErrorOr<GlyfAndLocaTableBuffers> create_glyf_and_loca_tables_from_transformed_glyf_table(FixedMemoryStream& table_stream)
{
    auto header = TRY(table_stream.read_value<TransformedGlyfTable>());

    auto loca_element_size = header.index_format == 0 ? LocaElementSize::TwoBytes : LocaElementSize::FourBytes;

    size_t table_size = TRY(table_stream.size());
    u64 total_size_of_streams = header.n_contour_stream_size;
    total_size_of_streams += header.n_points_stream_size;
    total_size_of_streams += header.flag_stream_size;
    total_size_of_streams += header.glyph_stream_size;
    total_size_of_streams += header.composite_stream_size;
    total_size_of_streams += header.bbox_stream_size;
    total_size_of_streams += header.instruction_stream_size;

    if (table_size < total_size_of_streams)
        return Error::from_string_literal("Not enough data to read in streams of transformed glyf table");

    auto number_of_contours_stream = FixedMemoryStream(TRY(table_stream.read_in_place<u8>(header.n_contour_stream_size)));
    auto number_of_points_stream = FixedMemoryStream(TRY(table_stream.read_in_place<u8>(header.n_points_stream_size)));
    auto flag_stream = FixedMemoryStream(TRY(table_stream.read_in_place<u8>(header.flag_stream_size)));
    auto glyph_stream = FixedMemoryStream(TRY(table_stream.read_in_place<u8>(header.glyph_stream_size)));
    auto composite_stream = FixedMemoryStream(TRY(table_stream.read_in_place<u8>(header.composite_stream_size)));

    size_t bounding_box_bitmap_length = ((header.num_glyphs + 31) >> 5) << 2;
    auto bounding_box_bitmap_memory_stream = FixedMemoryStream(TRY(table_stream.read_in_place<u8>(bounding_box_bitmap_length)));
    auto bounding_box_bitmap_bit_stream = BigEndianInputBitStream { MaybeOwned<Stream>(bounding_box_bitmap_memory_stream) };

    if (header.bbox_stream_size < bounding_box_bitmap_length)
        return Error::from_string_literal("Not enough data to read bounding box stream of transformed glyf table");
    auto bounding_box_stream = FixedMemoryStream(TRY(table_stream.read_in_place<u8>(header.bbox_stream_size - bounding_box_bitmap_length)));

    auto instruction_stream = FixedMemoryStream(TRY(table_stream.read_in_place<u8>(header.instruction_stream_size)));

    ByteBuffer reconstructed_glyf_table;
    Vector<u32> loca_indexes;

    auto append_u16 = [&](BigEndian<u16> value) -> ErrorOr<void> {
        return reconstructed_glyf_table.try_append(&value, sizeof(value));
    };

    auto append_i16 = [&](BigEndian<i16> value) -> ErrorOr<void> {
        return reconstructed_glyf_table.try_append(&value, sizeof(value));
    };

    auto append_bytes = [&](ReadonlyBytes bytes) -> ErrorOr<void> {
        return reconstructed_glyf_table.try_append(bytes);
    };

    for (size_t glyph_index = 0; glyph_index < header.num_glyphs; ++glyph_index) {
        size_t starting_glyf_table_size = reconstructed_glyf_table.size();

        bool has_bounding_box = TRY(bounding_box_bitmap_bit_stream.read_bit());

        auto number_of_contours = TRY(number_of_contours_stream.read_value<BigEndian<i16>>());

        if (number_of_contours == 0) {
            // Empty glyph

            // Reconstruction of an empty glyph (when nContour = 0) is a simple step
            // that involves incrementing the glyph record count and creating a new entry in the loca table
            // where loca[n] = loca[n-1].

            // If the bboxBitmap flag indicates that the bounding box values are explicitly encoded in the bboxStream
            // the decoder MUST reject WOFF2 file as invalid.
            if (has_bounding_box)
                return Error::from_string_literal("Empty glyphs cannot have an explicit bounding box");
        } else if (number_of_contours < 0) {
            // Decoding of Composite Glyphs

            [[maybe_unused]] i16 bounding_box_x_min = 0;
            [[maybe_unused]] i16 bounding_box_y_min = 0;
            [[maybe_unused]] i16 bounding_box_x_max = 0;
            [[maybe_unused]] i16 bounding_box_y_max = 0;

            if (has_bounding_box) {
                bounding_box_x_min = TRY(bounding_box_stream.read_value<BigEndian<i16>>());
                bounding_box_y_min = TRY(bounding_box_stream.read_value<BigEndian<i16>>());
                bounding_box_x_max = TRY(bounding_box_stream.read_value<BigEndian<i16>>());
                bounding_box_y_max = TRY(bounding_box_stream.read_value<BigEndian<i16>>());
            }

            TRY(append_i16(number_of_contours));
            TRY(append_i16(bounding_box_x_min));
            TRY(append_i16(bounding_box_y_min));
            TRY(append_i16(bounding_box_x_max));
            TRY(append_i16(bounding_box_y_max));

            bool have_instructions = false;
            u16 flags = to_underlying(OpenType::Glyf::CompositeFlags::MoreComponents);
            while (flags & to_underlying(OpenType::Glyf::CompositeFlags::MoreComponents)) {
                // 1a. Read a UInt16 from compositeStream. This is interpreted as a component flag word as in the TrueType spec.
                //     Based on the flag values, there are between 4 and 14 additional argument bytes,
                //     interpreted as glyph index, arg1, arg2, and optional scale or affine matrix.

                flags = TRY(composite_stream.read_value<BigEndian<u16>>());

                if (flags & to_underlying(OpenType::Glyf::CompositeFlags::WeHaveInstructions)) {
                    have_instructions = true;
                }

                // 2a. Read the number of argument bytes as determined in step 1a from the composite stream,
                //     and store these in the reconstructed glyph.
                //     If the flag word read in step 1a has the FLAG_MORE_COMPONENTS bit (bit 5) set, go back to step 1a.

                size_t argument_byte_count = 2;

                if (flags & to_underlying(OpenType::Glyf::CompositeFlags::Arg1AndArg2AreWords)) {
                    argument_byte_count += 4;
                } else {
                    argument_byte_count += 2;
                }

                if (flags & to_underlying(OpenType::Glyf::CompositeFlags::WeHaveAScale)) {
                    argument_byte_count += 2;
                } else if (flags & to_underlying(OpenType::Glyf::CompositeFlags::WeHaveAnXAndYScale)) {
                    argument_byte_count += 4;
                } else if (flags & to_underlying(OpenType::Glyf::CompositeFlags::WeHaveATwoByTwo)) {
                    argument_byte_count += 8;
                }

                TRY(append_u16(flags));
                TRY(reconstructed_glyf_table.try_append(TRY(composite_stream.read_in_place<u8>(argument_byte_count))));
            }

            if (have_instructions) {
                auto number_of_instructions = TRY(read_255_u_short(glyph_stream));
                TRY(append_u16(number_of_instructions));

                if (number_of_instructions)
                    TRY(reconstructed_glyf_table.try_append(TRY(instruction_stream.read_in_place<u8>(number_of_instructions))));
            }
        } else if (number_of_contours > 0) {
            // Decoding of Simple Glyphs

            // For a simple glyph (when nContour > 0), the process continues as follows:
            // Each of these is the number of points of that contour.
            // Convert this into the endPtsOfContours[] array by computing the cumulative sum, then subtracting one.

            Vector<size_t> end_points_of_contours;
            size_t number_of_points = 0;

            for (size_t contour_index = 0; contour_index < static_cast<size_t>(number_of_contours); ++contour_index) {
                size_t number_of_points_for_this_contour = TRY(read_255_u_short(number_of_points_stream));
                if (Checked<size_t>::addition_would_overflow(number_of_points, number_of_points_for_this_contour))
                    return Error::from_string_literal("EOVERFLOW 1");

                number_of_points += number_of_points_for_this_contour;
                if (number_of_points == 0)
                    return Error::from_string_literal("EOVERFLOW 2");

                TRY(end_points_of_contours.try_append(number_of_points - 1));
            }

            auto points = TRY(retrieve_points_of_simple_glyph(flag_stream, glyph_stream, number_of_points));

            auto instruction_size = TRY(read_255_u_short(glyph_stream));
            auto instructions_buffer = TRY(ByteBuffer::create_zeroed(instruction_size));
            if (instruction_size != 0)
                TRY(instruction_stream.read_until_filled(instructions_buffer));

            i16 bounding_box_x_min = 0;
            i16 bounding_box_y_min = 0;
            i16 bounding_box_x_max = 0;
            i16 bounding_box_y_max = 0;

            if (has_bounding_box) {
                bounding_box_x_min = TRY(bounding_box_stream.read_value<BigEndian<i16>>());
                bounding_box_y_min = TRY(bounding_box_stream.read_value<BigEndian<i16>>());
                bounding_box_x_max = TRY(bounding_box_stream.read_value<BigEndian<i16>>());
                bounding_box_y_max = TRY(bounding_box_stream.read_value<BigEndian<i16>>());
            } else {
                for (size_t point_index = 0; point_index < points.size(); ++point_index) {
                    auto& point = points.at(point_index);

                    if (point_index == 0) {
                        bounding_box_x_min = bounding_box_x_max = point.x;
                        bounding_box_y_min = bounding_box_y_max = point.y;
                        continue;
                    }

                    bounding_box_x_min = min(bounding_box_x_min, point.x);
                    bounding_box_x_max = max(bounding_box_x_max, point.x);
                    bounding_box_y_min = min(bounding_box_y_min, point.y);
                    bounding_box_y_max = max(bounding_box_y_max, point.y);
                }
            }

            TRY(append_i16(number_of_contours));
            TRY(append_i16(bounding_box_x_min));
            TRY(append_i16(bounding_box_y_min));
            TRY(append_i16(bounding_box_x_max));
            TRY(append_i16(bounding_box_y_max));

            for (auto end_point : end_points_of_contours)
                TRY(append_u16(end_point));

            TRY(append_u16(instruction_size));
            if (instruction_size != 0)
                TRY(append_bytes(instructions_buffer));

            Vector<FontPoint> relative_points;
            TRY(relative_points.try_ensure_capacity(points.size()));

            {
                i16 previous_point_x = 0;
                i16 previous_point_y = 0;
                for (auto& point : points) {
                    i16 x = point.x - previous_point_x;
                    i16 y = point.y - previous_point_y;
                    relative_points.unchecked_append({ x, y, point.on_curve });
                    previous_point_x = point.x;
                    previous_point_y = point.y;
                }
            }

            Optional<u8> last_flags;
            u8 repeat_count = 0;

            for (auto& point : relative_points) {
                u8 flags = 0;

                if (point.on_curve)
                    flags |= SimpleGlyphFlags::OnCurve;

                if (point.x == 0) {
                    flags |= SimpleGlyphFlags::XIsSameOrPositiveXShortVector;
                } else if (point.x > -256 && point.x < 256) {
                    flags |= SimpleGlyphFlags::XShortVector;

                    if (point.x > 0)
                        flags |= SimpleGlyphFlags::XIsSameOrPositiveXShortVector;
                }

                if (point.y == 0) {
                    flags |= SimpleGlyphFlags::YIsSameOrPositiveYShortVector;
                } else if (point.y > -256 && point.y < 256) {
                    flags |= SimpleGlyphFlags::YShortVector;

                    if (point.y > 0)
                        flags |= SimpleGlyphFlags::YIsSameOrPositiveYShortVector;
                }

                if (last_flags.has_value() && flags == last_flags.value() && repeat_count != 0xff) {
                    // NOTE: Update the previous entry to say it's repeating.
                    reconstructed_glyf_table[reconstructed_glyf_table.size() - 1] |= SimpleGlyphFlags::RepeatFlag;
                    ++repeat_count;
                } else {
                    if (repeat_count != 0) {
                        TRY(reconstructed_glyf_table.try_append(repeat_count));
                        repeat_count = 0;
                    }
                    TRY(reconstructed_glyf_table.try_append(flags));
                }
                last_flags = flags;
            }
            if (repeat_count != 0) {
                TRY(reconstructed_glyf_table.try_append(repeat_count));
            }

            for (auto& point : relative_points) {
                if (point.x == 0) {
                    // No need to write to the table.
                } else if (point.x > -256 && point.x < 256) {
                    TRY(reconstructed_glyf_table.try_append(abs(point.x)));
                } else {
                    TRY(append_i16(point.x));
                }
            }

            for (auto& point : relative_points) {
                if (point.y == 0) {
                    // No need to write to the table.
                } else if (point.y > -256 && point.y < 256) {
                    TRY(reconstructed_glyf_table.try_append(abs(point.y)));
                } else {
                    TRY(append_i16(point.y));
                }
            }
        }

        // NOTE: Make sure each glyph starts on a 4-byte boundary.
        //       I haven't found the spec text for this, but it matches other implementations.
        while (reconstructed_glyf_table.size() % 4 != 0) {
            TRY(reconstructed_glyf_table.try_append(0));
        }

        TRY(loca_indexes.try_append(starting_glyf_table_size));
    }

    TRY(loca_indexes.try_append(reconstructed_glyf_table.size()));

    size_t loca_element_size_in_bytes = loca_element_size == LocaElementSize::TwoBytes ? sizeof(u16) : sizeof(u32);
    size_t loca_table_buffer_size = loca_indexes.size() * loca_element_size_in_bytes;
    ByteBuffer loca_table_buffer;
    TRY(loca_table_buffer.try_ensure_capacity(loca_table_buffer_size));
    for (auto loca_index : loca_indexes) {
        if (loca_element_size == LocaElementSize::TwoBytes) {
            auto value = BigEndian<u16>(loca_index >> 1);
            loca_table_buffer.append({ &value, sizeof(value) });
        } else {
            auto value = BigEndian<u32>(loca_index);
            loca_table_buffer.append({ &value, sizeof(value) });
        }
    }

    return GlyfAndLocaTableBuffers { .glyf_table = move(reconstructed_glyf_table), .loca_table = move(loca_table_buffer) };
}

ErrorOr<NonnullRefPtr<Font>> Font::try_load_from_resource(Core::Resource const& resource)
{
    return try_load_from_externally_owned_memory(resource.data());
}

ErrorOr<NonnullRefPtr<Font>> Font::try_load_from_externally_owned_memory(ReadonlyBytes bytes)
{
    FixedMemoryStream stream(bytes);
    return try_load_from_externally_owned_memory(stream);
}

ErrorOr<NonnullRefPtr<Font>> Font::try_load_from_externally_owned_memory(SeekableStream& stream)
{
    auto header = TRY(stream.read_value<Header>());

    // The signature field in the WOFF2 header MUST contain the value of 0x774F4632 ('wOF2'), which distinguishes it from WOFF 1.0 files.
    // If the field does not contain this value, user agents MUST reject the file as invalid.
    if (header.signature != WOFF2_SIGNATURE)
        return Error::from_string_literal("Invalid WOFF2 signature");

    // The interpretation of the WOFF2 Header is the same as the WOFF Header in [WOFF1], with the addition of one new totalCompressedSize field.
    // NOTE: See WOFF/Font.cpp for more comments about this.

    static constexpr size_t MAX_BUFFER_SIZE = 10 * MiB;
    if (header.length > TRY(stream.size()))
        return Error::from_string_literal("Invalid WOFF length");
    if (header.num_tables == 0 || header.num_tables > NumericLimits<u16>::max() / 16)
        return Error::from_string_literal("Invalid WOFF numTables");
    if (header.total_compressed_size > MAX_BUFFER_SIZE)
        return Error::from_string_literal("Compressed font is more than 10 MiB");
    if (header.meta_length == 0 && header.meta_offset != 0)
        return Error::from_string_literal("Invalid WOFF meta block offset");
    if (header.priv_length == 0 && header.priv_offset != 0)
        return Error::from_string_literal("Invalid WOFF private block offset");
    if (header.flavor == TTCF_SIGNAURE)
        return Error::from_string_literal("Font collections not yet supported");

    // NOTE: "The "totalSfntSize" value in the WOFF2 Header is intended to be used for reference purposes only. It may represent the size of the uncompressed input font file,
    //        but if the transformed 'glyf' and 'loca' tables are present, the uncompressed size of the reconstructed tables and the total decompressed font size may differ
    //        substantially from the original total size specified in the WOFF2 Header."
    //        We use it as an initial size of the font buffer and extend it as necessary.
    auto font_buffer_size = clamp(header.total_sfnt_size, sizeof(OpenType::TableDirectory) + header.num_tables * sizeof(TableDirectoryEntry), MAX_BUFFER_SIZE);
    auto font_buffer = TRY(ByteBuffer::create_zeroed(font_buffer_size));

    u16 search_range = pow_2_less_than_or_equal(header.num_tables);
    OpenType::TableDirectory table_directory {
        .sfnt_version = header.flavor,
        .num_tables = header.num_tables,
        .search_range = search_range * 16,
        .entry_selector = log2(search_range),
        .range_shift = header.num_tables * 16 - search_range * 16,
    };
    font_buffer.overwrite(0, &table_directory, sizeof(table_directory));

    Vector<TableDirectoryEntry> table_entries;
    TRY(table_entries.try_ensure_capacity(header.num_tables));

    u64 total_length_of_all_tables = 0;

    for (size_t table_entry_index = 0; table_entry_index < header.num_tables; ++table_entry_index) {
        TableDirectoryEntry table_directory_entry;
        u8 const flags_byte = TRY(stream.read_value<u8>());

        switch ((flags_byte & 0xC0) >> 6) {
        case 0:
            table_directory_entry.transformation_version = TransformationVersion::Version0;
            break;
        case 1:
            table_directory_entry.transformation_version = TransformationVersion::Version1;
            break;
        case 2:
            table_directory_entry.transformation_version = TransformationVersion::Version2;
            break;
        case 3:
            table_directory_entry.transformation_version = TransformationVersion::Version3;
            break;
        default:
            VERIFY_NOT_REACHED();
        }

        u8 tag_number = flags_byte & 0x3F;

        if (tag_number != 0x3F) {
            table_directory_entry.tag = known_tag_names[tag_number];
        } else {
            table_directory_entry.tag = TRY(stream.read_value<OpenType::Tag>());
        }

        table_directory_entry.original_length = TRY(read_uint_base_128(stream));

        bool needs_to_read_transform_length = false;
        if (table_directory_entry.tag == OpenType::Tag("glyf") || table_directory_entry.tag == OpenType::Tag("loca"))
            needs_to_read_transform_length = table_directory_entry.transformation_version == TransformationVersion::Version0;
        else
            needs_to_read_transform_length = table_directory_entry.transformation_version != TransformationVersion::Version0;

        if (needs_to_read_transform_length) {
            u32 transform_length = TRY(read_uint_base_128(stream));
            table_directory_entry.transform_length = transform_length;
            total_length_of_all_tables += transform_length;
        } else {
            total_length_of_all_tables += table_directory_entry.original_length;
        }

        table_entries.unchecked_append(move(table_directory_entry));
    }

    // FIXME: Read in collection header and entries.

    auto glyf_table = table_entries.find_if([](TableDirectoryEntry const& entry) {
        return entry.tag == OpenType::Tag("glyf");
    });

    auto loca_table = table_entries.find_if([](TableDirectoryEntry const& entry) {
        return entry.tag == OpenType::Tag("loca");
    });

    // "In other words, both glyf and loca tables must either be present in their transformed format or with null transform applied to both tables."
    if (glyf_table.is_end() != loca_table.is_end())
        return Error::from_string_literal("Must have both 'loca' and 'glyf' tables if one of them is present");

    if (!glyf_table.is_end() && !loca_table.is_end()) {
        if (glyf_table->transformation_version != loca_table->transformation_version)
            return Error::from_string_literal("The 'loca' and 'glyf' tables must have the same transformation version");
    }

    if (!loca_table.is_end()) {
        if (loca_table->has_transformation() && loca_table->transform_length.value() != 0)
            return Error::from_string_literal("Transformed 'loca' table must have a transform length of 0");
    }

    auto compressed_bytes_read_buffer = TRY(ByteBuffer::create_zeroed(header.total_compressed_size));
    auto compressed_bytes = TRY(stream.read_some(compressed_bytes_read_buffer));
    if (compressed_bytes.size() != header.total_compressed_size)
        return Error::from_string_literal("Not enough data to read in the reported size of the compressed data");

    auto compressed_stream = FixedMemoryStream(compressed_bytes);
    auto brotli_stream = Compress::BrotliDecompressionStream { MaybeOwned<Stream>(compressed_stream) };
    auto decompressed_table_data = TRY(brotli_stream.read_until_eof());
    if (decompressed_table_data.size() != total_length_of_all_tables)
        return Error::from_string_literal("Size of the decompressed data is not equal to the total of the reported lengths of each table");

    auto decompressed_data_stream = FixedMemoryStream(decompressed_table_data.bytes());
    size_t font_buffer_offset = SFNT_HEADER_SIZE + header.num_tables * SFNT_TABLE_SIZE;
    Optional<GlyfAndLocaTableBuffers> glyf_and_loca_buffer;
    for (size_t table_entry_index = 0; table_entry_index < header.num_tables; ++table_entry_index) {
        auto& table_entry = table_entries.at(table_entry_index);
        u32 length_to_read = table_entry.has_transformation() ? table_entry.transform_length.value() : table_entry.original_length;

        auto table_buffer = TRY(ByteBuffer::create_zeroed(length_to_read));
        auto table_bytes = TRY(decompressed_data_stream.read_some(table_buffer));
        if (table_bytes.size() != length_to_read)
            return Error::from_string_literal("Not enough data to read decompressed table");

        size_t table_directory_offset = SFNT_HEADER_SIZE + table_entry_index * SFNT_TABLE_SIZE;

        if (table_entry.has_transformation()) {
            if (table_entry.tag == OpenType::Tag("glyf")) {
                auto table_stream = FixedMemoryStream(table_bytes);
                glyf_and_loca_buffer = TRY(create_glyf_and_loca_tables_from_transformed_glyf_table(table_stream));

                if (font_buffer.size() < (font_buffer_offset + glyf_and_loca_buffer->glyf_table.size()))
                    TRY(font_buffer.try_resize(font_buffer_offset + glyf_and_loca_buffer->glyf_table.size()));

                OpenType::TableRecord table_record {
                    .table_tag = table_entry.tag,
                    .checksum = 0, // FIXME: WOFF2 does not give us the original checksum.
                    .offset = font_buffer_offset,
                    .length = glyf_and_loca_buffer->glyf_table.size(),
                };
                font_buffer.overwrite(table_directory_offset, &table_record, sizeof(table_record));

                font_buffer.overwrite(font_buffer_offset, glyf_and_loca_buffer->glyf_table.data(), glyf_and_loca_buffer->glyf_table.size());
                font_buffer_offset += glyf_and_loca_buffer->glyf_table.size();
            } else if (table_entry.tag == OpenType::Tag("loca")) {
                // FIXME: Handle loca table coming before glyf table in input?
                VERIFY(glyf_and_loca_buffer.has_value());
                if (font_buffer.size() < (font_buffer_offset + glyf_and_loca_buffer->loca_table.size()))
                    TRY(font_buffer.try_resize(font_buffer_offset + glyf_and_loca_buffer->loca_table.size()));

                OpenType::TableRecord table_record {
                    .table_tag = table_entry.tag,
                    .checksum = 0, // FIXME: WOFF2 does not give us the original checksum.
                    .offset = font_buffer_offset,
                    .length = glyf_and_loca_buffer->loca_table.size(),
                };
                font_buffer.overwrite(table_directory_offset, &table_record, sizeof(table_record));

                font_buffer.overwrite(font_buffer_offset, glyf_and_loca_buffer->loca_table.data(), glyf_and_loca_buffer->loca_table.size());
                font_buffer_offset += glyf_and_loca_buffer->loca_table.size();
            } else if (table_entry.tag == OpenType::Tag("hmtx")) {
                return Error::from_string_literal("Decoding transformed hmtx table not yet supported");
            } else {
                return Error::from_string_literal("Unknown transformation");
            }
        } else {
            OpenType::TableRecord table_record {
                .table_tag = table_entry.tag,
                .checksum = 0, // FIXME: WOFF2 does not give us the original checksum.
                .offset = font_buffer_offset,
                .length = length_to_read,
            };
            font_buffer.overwrite(table_directory_offset, &table_record, sizeof(table_record));

            if (font_buffer.size() < (font_buffer_offset + length_to_read))
                TRY(font_buffer.try_resize(font_buffer_offset + length_to_read));
            font_buffer.overwrite(font_buffer_offset, table_buffer.data(), length_to_read);

            font_buffer_offset += length_to_read;
        }
    }

    auto input_font = TRY(OpenType::Font::try_load_from_externally_owned_memory(font_buffer.bytes()));
    return adopt_ref(*new Font(input_font, move(font_buffer)));
}

}
