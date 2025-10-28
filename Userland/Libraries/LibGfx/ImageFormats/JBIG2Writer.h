/*
 * Copyright (c) 2024-2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/String.h>
#include <AK/Variant.h>
#include <LibGfx/Forward.h>
#include <LibGfx/ImageFormats/BilevelImage.h>
#include <LibGfx/ImageFormats/JBIG2Shared.h>

namespace Gfx {

struct JBIG2EncoderOptions {
};

// Structs in this namespace can be used to explicitly construct JBIG2 files with a certain structure.
// This is useful for making test inputs, and not useful else. Usually you want to use the usual encode() API instead.
namespace JBIG2 {

struct SegmentHeaderData {
    u32 segment_number { 0 };
    bool retention_flag { false };

    struct Reference {
        u32 segment_number { 0 };
        bool retention_flag { false };
    };
    Vector<Reference> referred_to_segments;

    // 7.2.6 Segment page association
    // "The first page must be numbered "1". This field may contain a value of zero; this value indicates that this segment is not associated with any page."
    u32 page_association { 0 };

    bool force_32_bit_page_association { false };
    bool is_immediate_generic_region_of_initially_unknown_size { false };
};

struct SymbolDictionarySegmentData {
    u16 flags { 0 };
    Array<AdaptiveTemplatePixel, 4> adaptive_template_pixels {};
    Array<AdaptiveTemplatePixel, 2> refinement_adaptive_template_pixels {};
    // FIXME: Add more fields.
    MQArithmeticEncoder::Trailing7FFFHandling trailing_7fff_handling { MQArithmeticEncoder::Trailing7FFFHandling::Keep };
};

struct TextRegionSegmentData {
    RegionSegmentInformationField region_segment_information {};
    u16 flags { 0 };
    u16 huffman_flags { 0 };
    Array<AdaptiveTemplatePixel, 2> refinement_adaptive_template_pixels {};
    // FIXME: Add more fields.
    MQArithmeticEncoder::Trailing7FFFHandling trailing_7fff_handling { MQArithmeticEncoder::Trailing7FFFHandling::Keep };
};

struct ImmediateTextRegionSegmentData {
    TextRegionSegmentData text_region;
};

struct ImmediateLosslessTextRegionSegmentData {
    TextRegionSegmentData text_region;
};

struct PatternDictionarySegmentData {
    u8 flags { 0 };
    u8 pattern_width { 0 };
    u8 pattern_height { 0 };
    u32 gray_max { 0 };
    NonnullRefPtr<BilevelImage> image;
    MQArithmeticEncoder::Trailing7FFFHandling trailing_7fff_handling { MQArithmeticEncoder::Trailing7FFFHandling::Keep };
};

struct HalftoneRegionSegmentData {
    RegionSegmentInformationField region_segment_information {};
    u8 flags { 0 };
    u32 grayscale_width { 0 };
    u32 grayscale_height { 0 };
    i32 grid_offset_x_times_256 { 0 };
    i32 grid_offset_y_times_256 { 0 };
    u16 grid_vector_x_times_256 { 0 };
    u16 grid_vector_y_times_256 { 0 };

    // Indices into pattern dictionary. At most 64 bits set per pixel.
    // grayscale_width * grayscale_height entries.
    Vector<u64> grayscale_image;

    MQArithmeticEncoder::Trailing7FFFHandling trailing_7fff_handling { MQArithmeticEncoder::Trailing7FFFHandling::Keep };
};

struct ImmediateHalftoneRegionSegmentData {
    HalftoneRegionSegmentData halftone_region;
};

struct ImmediateLosslessHalftoneRegionSegmentData {
    HalftoneRegionSegmentData halftone_region;
};

struct GenericRegionSegmentData {
    RegionSegmentInformationField region_segment_information {};
    u8 flags { 0 };
    Array<AdaptiveTemplatePixel, 12> adaptive_template_pixels {};
    NonnullRefPtr<BilevelImage> image;
    Optional<u32> real_height_for_generic_region_of_initially_unknown_size {};
    MQArithmeticEncoder::Trailing7FFFHandling trailing_7fff_handling { MQArithmeticEncoder::Trailing7FFFHandling::Keep };
};

struct ImmediateGenericRegionSegmentData {
    GenericRegionSegmentData generic_region;
};

struct ImmediateLosslessGenericRegionSegmentData {
    GenericRegionSegmentData generic_region;
};

struct IntermediateGenericRegionSegmentData {
    GenericRegionSegmentData generic_region;
};

struct GenericRefinementRegionSegmentData {
    RegionSegmentInformationField region_segment_information {};
    u8 flags { 0 };
    Array<AdaptiveTemplatePixel, 2> adaptive_template_pixels {};
    NonnullRefPtr<BilevelImage> image;
    MQArithmeticEncoder::Trailing7FFFHandling trailing_7fff_handling { MQArithmeticEncoder::Trailing7FFFHandling::Keep };
};

struct ImmediateGenericRefinementRegionSegmentData {
    GenericRefinementRegionSegmentData generic_refinement_region;
};

struct ImmediateLosslessGenericRefinementRegionSegmentData {
    GenericRefinementRegionSegmentData generic_refinement_region;
};

struct IntermediateGenericRefinementRegionSegmentData {
    GenericRefinementRegionSegmentData generic_refinement_region;
};

struct EndOfPageSegmentData { };
struct EndOfFileSegmentData { };

struct TablesData {
    u8 flags { 0 };
    i32 lowest_value { 0 };
    i32 highest_value { 0 };
    struct Entry {
        u8 prefix_length { 0 };
        u8 range_length { 0 };
    };
    Vector<Entry> entries;

    u8 lower_range_prefix_length { 0 };
    u8 upper_range_prefix_length { 0 };
    u8 out_of_band_prefix_length { 0 };
};

struct ExtensionData {
    JBIG2::ExtensionType type;

    struct Entry {
        String key;
        String value;
    };
    Vector<Entry> entries;
};

struct SegmentData {
    SegmentHeaderData header;
    Variant<
        SymbolDictionarySegmentData,
        ImmediateTextRegionSegmentData,
        ImmediateLosslessTextRegionSegmentData,
        PatternDictionarySegmentData,
        ImmediateHalftoneRegionSegmentData,
        ImmediateLosslessHalftoneRegionSegmentData,
        ImmediateGenericRegionSegmentData,
        ImmediateLosslessGenericRegionSegmentData,
        IntermediateGenericRegionSegmentData,
        ImmediateGenericRefinementRegionSegmentData,
        ImmediateLosslessGenericRefinementRegionSegmentData,
        IntermediateGenericRefinementRegionSegmentData,
        PageInformationSegment,
        EndOfPageSegmentData,
        EndOfStripeSegment,
        EndOfFileSegmentData,
        TablesData,
        ExtensionData>
        data;
};

struct FileHeaderData {
    Organization organization { Organization::Sequential };
    Optional<u32> number_of_pages { 0 };
    bool force_32bit_page_numbers { false };
};

struct FileData {
    FileHeaderData header;
    Vector<SegmentData> segments;
};

};

class JBIG2Writer {
public:
    using Options = JBIG2EncoderOptions;

    static ErrorOr<void> encode(Stream&, Bitmap const&, Options const& = {});

    static ErrorOr<void> encode_with_explicit_data(Stream&, JBIG2::FileData const&);

private:
    JBIG2Writer() = delete;
};

}
