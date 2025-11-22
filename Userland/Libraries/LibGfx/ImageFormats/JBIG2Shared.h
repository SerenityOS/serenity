/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Endian.h>
#include <AK/Optional.h>
#include <AK/Types.h>
#include <AK/Vector.h>
#include <LibGfx/ImageFormats/MQArithmeticCoder.h>
#include <LibGfx/Rect.h>

namespace Gfx::JBIG2 {

class HuffmanTable;

struct TextRegionHuffmanTables {
    JBIG2::HuffmanTable const* first_s_table { nullptr };                 // "SBHUFFFS" in spec.
    JBIG2::HuffmanTable const* subsequent_s_table { nullptr };            // "SBHUFFDS" in spec.
    JBIG2::HuffmanTable const* delta_t_table { nullptr };                 // "SBHUFFDT" in spec.
    JBIG2::HuffmanTable const* refinement_delta_width_table { nullptr };  // "SBHUFFRDW" in spec.
    JBIG2::HuffmanTable const* refinement_delta_height_table { nullptr }; // "SBHUFFRDH" in spec.
    JBIG2::HuffmanTable const* refinement_x_offset_table { nullptr };     // "SBHUFFRDX" in spec.
    JBIG2::HuffmanTable const* refinement_y_offset_table { nullptr };     // "SBHUFFRDY" in spec.
    JBIG2::HuffmanTable const* refinement_size_table { nullptr };         // "SBHUFFRSIZE" in spec.
};
ErrorOr<TextRegionHuffmanTables> text_region_huffman_tables_from_flags(u16 huffman_flags, Vector<JBIG2::HuffmanTable const*> custom_tables);

struct SymbolDictionaryHuffmanTables {
    JBIG2::HuffmanTable const* delta_height_table { nullptr };               // "SDHUFFDH" in spec.
    JBIG2::HuffmanTable const* delta_width_table { nullptr };                // "SDHUFFDW" in spec.
    JBIG2::HuffmanTable const* bitmap_size_table { nullptr };                // "SDHUFFBMSIZE" in spec.
    JBIG2::HuffmanTable const* number_of_symbol_instances_table { nullptr }; // "SDHUFFAGGINST" in spec.
};
ErrorOr<SymbolDictionaryHuffmanTables> symbol_dictionary_huffman_tables_from_flags(u16 flags, Vector<JBIG2::HuffmanTable const*> custom_tables);

// JBIG2 spec, Annex D, D.4.1 ID string
inline constexpr u8 id_string[] = { 0x97, 0x4A, 0x42, 0x32, 0x0D, 0x0A, 0x1A, 0x0A };

// 7.3 Segment types
enum SegmentType {
    SymbolDictionary = 0,
    IntermediateTextRegion = 4,
    ImmediateTextRegion = 6,
    ImmediateLosslessTextRegion = 7,
    PatternDictionary = 16,
    IntermediateHalftoneRegion = 20,
    ImmediateHalftoneRegion = 22,
    ImmediateLosslessHalftoneRegion = 23,
    IntermediateGenericRegion = 36,
    ImmediateGenericRegion = 38,
    ImmediateLosslessGenericRegion = 39,
    IntermediateGenericRefinementRegion = 40,
    ImmediateGenericRefinementRegion = 42,
    ImmediateLosslessGenericRefinementRegion = 43,
    PageInformation = 48,
    EndOfPage = 49,
    EndOfStripe = 50,
    EndOfFile = 51,
    Profiles = 52,
    Tables = 53,
    ColorPalette = 54,
    Extension = 62,
};

// Annex D
enum class Organization {
    // D.1 Sequential organization
    Sequential,

    // D.2 Random-access organization
    RandomAccess,

    // D.3 Embedded organization
    Embedded,
};

struct SegmentHeader {
    u32 segment_number { 0 };
    SegmentType type { SegmentType::Extension };
    bool retention_flag { false };

    // These two have the same size.
    Vector<u32> referred_to_segment_numbers;
    Vector<bool> referred_to_segment_retention_flags;

    // 7.2.6 Segment page association
    // "The first page must be numbered "1". This field may contain a value of zero; this value indicates that this segment is not associated with any page."
    u32 page_association { 0 };

    Optional<u32> data_length;
};

// 7.4.3.1.1 Text region segment flags
enum class ReferenceCorner {
    BottomLeft = 0,
    TopLeft = 1,
    BottomRight = 2,
    TopRight = 3,
};

// 7.4.8.5 Page segment flags
enum class CombinationOperator {
    Or = 0,
    And = 1,
    Xor = 2,
    XNor = 3,
    Replace = 4,
};

// 7.4.1 Region segment information field
struct [[gnu::packed]] RegionSegmentInformationField {
    BigEndian<u32> width;
    BigEndian<u32> height;
    BigEndian<u32> x_location;
    BigEndian<u32> y_location;
    u8 flags { 0 };

    IntRect rect() const
    {
        return { x_location, y_location, width, height };
    }

    CombinationOperator external_combination_operator() const
    {
        VERIFY((flags & 0x7) <= 4);
        return static_cast<CombinationOperator>(flags & 0x7);
    }

    bool is_color_bitmap() const
    {
        return (flags & 0x8) != 0;
    }
};
static_assert(AssertSize<RegionSegmentInformationField, 17>());

struct AdaptiveTemplatePixel {
    i8 x { 0 };
    i8 y { 0 };
};

// Figure 7 – Field to which AT pixel locations are restricted
inline ErrorOr<void> check_valid_adaptive_template_pixel(AdaptiveTemplatePixel const& adaptive_template_pixel)
{
    // Don't have to check < -127 or > 127: The offsets are stored in an i8, so they can't be out of those bounds.
    if (adaptive_template_pixel.y > 0)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Adaptive pixel y too big");
    if (adaptive_template_pixel.y == 0 && adaptive_template_pixel.x > -1)
        return Error::from_string_literal("JBIG2ImageDecoderPlugin: Adaptive pixel x too big");
    return {};
}

struct GenericContexts {
    GenericContexts(u8 template_)
    {
        contexts.resize(1 << number_of_context_bits_for_template(template_));
    }

    Vector<MQArithmeticCoderContext> contexts; // "GB" (+ binary suffix) in spec.

private:
    static u8 number_of_context_bits_for_template(u8 template_)
    {
        if (template_ == 0)
            return 16;
        if (template_ == 1)
            return 13;
        VERIFY(template_ == 2 || template_ == 3);
        return 10;
    }
};

struct RefinementContexts {
    explicit RefinementContexts(u8 refinement_template)
    {
        contexts.resize(1 << (refinement_template == 0 ? 13 : 10));
    }

    Vector<MQArithmeticCoderContext> contexts; // "GR" (+ binary suffix) in spec.
};

// 7.4.8 Page information segment syntax
struct [[gnu::packed]] PageInformationSegment {
    BigEndian<u32> bitmap_width;
    BigEndian<u32> bitmap_height;
    BigEndian<u32> page_x_resolution; // In pixels/meter.
    BigEndian<u32> page_y_resolution; // In pixels/meter.
    u8 flags { 0 };
    BigEndian<u16> striping_information;

    bool is_eventually_lossless() const { return flags & 1; }
    bool might_contain_refinements() const { return (flags >> 1) & 1; }
    u8 default_color() const { return (flags >> 2) & 1; }

    CombinationOperator default_combination_operator() const
    {
        return static_cast<CombinationOperator>((flags >> 3) & 3);
    }

    bool requires_auxiliary_buffers() const { return (flags >> 5) & 1; }

    bool direct_region_segments_override_default_combination_operator() const
    {
        return (flags >> 6) & 1;
    }

    bool might_contain_coloured_segments() const { return (flags >> 7) & 1; }

    bool page_is_striped() const { return (striping_information & 0x8000) != 0; }
    u16 maximum_stripe_size() const { return striping_information & 0x7FFF; }
};
static_assert(AssertSize<PageInformationSegment, 19>());

// 7.4.10 End of stripe segment syntax
struct [[gnu::packed]] EndOfStripeSegment {
    // "The segment data of an end of stripe segment consists of one four-byte value, specifying the Y coordinate of the end row."
    BigEndian<u32> y_coordinate;
};
static_assert(AssertSize<EndOfStripeSegment, 4>());

enum class ExtensionType {
    SingleByteCodedComment = 0x20000000,
    MultiByteCodedComment = 0x20000002,
};

struct Code {
    u16 prefix_length {};         // "PREFLEN" in spec. High bit set for lower range table line.
    u8 range_length {};           // "RANGELEN" in spec.
    Optional<i32> first_value {}; // First number in "VAL" in spec.
    u32 code {};                  // "Encoding" in spec.

    constexpr static int LowerRangeBit = 0x8000;
};

ErrorOr<Vector<u32>> assign_huffman_codes(ReadonlyBytes code_lengths);
ErrorOr<Vector<JBIG2::Code>> uniform_huffman_codes(u32 number_of_symbols, u32 code_length);

class HuffmanTable {
public:
    enum class StandardTable {
        B_1,  // Standard Huffman table A
        B_2,  // Standard Huffman table B
        B_3,  // Standard Huffman table C
        B_4,  // Standard Huffman table D
        B_5,  // Standard Huffman table E
        B_6,  // Standard Huffman table F
        B_7,  // Standard Huffman table G
        B_8,  // Standard Huffman table H
        B_9,  // Standard Huffman table I
        B_10, // Standard Huffman table J
        B_11, // Standard Huffman table K
        B_12, // Standard Huffman table L
        B_13, // Standard Huffman table M
        B_14, // Standard Huffman table N
        B_15, // Standard Huffman table O
    };
    static ErrorOr<HuffmanTable*> standard_huffman_table(StandardTable);

    bool has_oob_symbol() const { return m_has_oob_symbol; }

    // Returns OptionalNone for OOB.
    ErrorOr<Optional<i32>> read_symbol(BigEndianInputBitStream&) const;

    // Will never return OOB.
    ErrorOr<i32> read_symbol_non_oob(BigEndianInputBitStream&) const;

    // Takes OptionalNone for OOB.
    ErrorOr<void> write_symbol(BigEndianOutputBitStream&, Optional<i32>) const;
    ErrorOr<void> write_symbol_non_oob(BigEndianOutputBitStream&, i32) const;

    HuffmanTable(ReadonlySpan<Code> codes, bool has_oob_symbol = false);

private:
    ErrorOr<Optional<i32>> read_symbol_internal(BigEndianInputBitStream&) const;
    ErrorOr<void> write_symbol_internal(BigEndianOutputBitStream&, Optional<i32>) const;

    ReadonlySpan<Code> m_codes;
    bool m_has_oob_symbol { false };
};

}
