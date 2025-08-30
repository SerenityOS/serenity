/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

// Spec: ITU-T_T_88__08_2018.pdf in the zip file here:
// https://www.itu.int/rec/T-REC-T.88-201808-I
// JBIG2Loader.cpp has many spec notes.

#include <AK/NonnullOwnPtr.h>
#include <AK/StdLibExtras.h>
#include <AK/Stream.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/BilevelImage.h>
#include <LibGfx/ImageFormats/JBIG2Writer.h>
#include <LibGfx/ImageFormats/QMArithmeticCoder.h>

namespace Gfx {

namespace {

// Similar to 6.2.2 Input parameters, but with an input image.
struct GenericRegionEncodingInputParameters {
    bool is_modified_modified_read { false }; // "MMR" in spec.
    BilevelImage const& image;                // Of dimensions "GBW" x "GBH" in spec terms.
    u8 gb_template { 0 };
    bool is_typical_prediction_used { false };          // "TPGDON" in spec.
    bool is_extended_reference_template_used { false }; // "EXTTEMPLATE" in spec.
    Optional<BilevelImage const&> skip_pattern {};      // "USESKIP", "SKIP" in spec.

    Array<JBIG2::AdaptiveTemplatePixel, 12> adaptive_template_pixels {}; // "GBATX" / "GBATY" in spec.
    // FIXME: GBCOLS, GBCOMBOP, COLEXTFLAG

    enum RequireEOFBAfterMMR {
        No,
        Yes,
    } require_eof_after_mmr { RequireEOFBAfterMMR::No };
};

}

// 6.2 Generic region decoding procedure, but in backwards.
static ErrorOr<ByteBuffer> generic_region_encoding_procedure(GenericRegionEncodingInputParameters const& inputs, Optional<JBIG2::GenericContexts>& maybe_contexts)
{
    // FIXME: Try to come up with a way to share more code with generic_region_decoding_procedure().
    auto width = inputs.image.width();
    auto height = inputs.image.height();

    if (inputs.is_modified_modified_read)
        return Error::from_string_literal("JBIG2Writer: Cannot encode MMR yet");

    auto& contexts = maybe_contexts.value();

    // 6.2.5 Decoding using a template and arithmetic coding
    if (inputs.is_extended_reference_template_used)
        return Error::from_string_literal("JBIG2Writer: Cannot encode EXTTEMPLATE yet");

    int number_of_adaptive_template_pixels = inputs.gb_template == 0 ? 4 : 1;
    for (int i = 0; i < number_of_adaptive_template_pixels; ++i)
        TRY(check_valid_adaptive_template_pixel(inputs.adaptive_template_pixels[i]));

    if (inputs.skip_pattern.has_value() && (inputs.skip_pattern->width() != width || inputs.skip_pattern->height() != height))
        return Error::from_string_literal("JBIG2Writer: Invalid USESKIP dimensions");

    if (inputs.skip_pattern.has_value())
        return Error::from_string_literal("JBIG2Writer: Cannot encode USESKIP yet");

    static constexpr auto get_pixel = [](BilevelImage const& buffer, int x, int y) -> bool {
        if (x < 0 || x >= (int)buffer.width() || y < 0)
            return false;
        return buffer.get_bit(x, y);
    };

    // Figure 3(a) – Template when GBTEMPLATE = 0 and EXTTEMPLATE = 0,
    constexpr auto compute_context_0 = [](BilevelImage const& buffer, ReadonlySpan<JBIG2::AdaptiveTemplatePixel> adaptive_pixels, int x, int y) -> u16 {
        u16 result = 0;
        for (int i = 0; i < 4; ++i)
            result = (result << 1) | (u16)get_pixel(buffer, x + adaptive_pixels[i].x, y + adaptive_pixels[i].y);
        for (int i = 0; i < 3; ++i)
            result = (result << 1) | (u16)get_pixel(buffer, x - 1 + i, y - 2);
        for (int i = 0; i < 5; ++i)
            result = (result << 1) | (u16)get_pixel(buffer, x - 2 + i, y - 1);
        for (int i = 0; i < 4; ++i)
            result = (result << 1) | (u16)get_pixel(buffer, x - 4 + i, y);
        return result;
    };

    // Figure 4 – Template when GBTEMPLATE = 1
    auto compute_context_1 = [](BilevelImage const& buffer, ReadonlySpan<JBIG2::AdaptiveTemplatePixel> adaptive_pixels, int x, int y) -> u16 {
        u16 result = 0;
        result = (result << 1) | (u16)get_pixel(buffer, x + adaptive_pixels[0].x, y + adaptive_pixels[0].y);
        for (int i = 0; i < 4; ++i)
            result = (result << 1) | (u16)get_pixel(buffer, x - 1 + i, y - 2);
        for (int i = 0; i < 5; ++i)
            result = (result << 1) | (u16)get_pixel(buffer, x - 2 + i, y - 1);
        for (int i = 0; i < 3; ++i)
            result = (result << 1) | (u16)get_pixel(buffer, x - 3 + i, y);
        return result;
    };

    // Figure 5 – Template when GBTEMPLATE = 2
    auto compute_context_2 = [](BilevelImage const& buffer, ReadonlySpan<JBIG2::AdaptiveTemplatePixel> adaptive_pixels, int x, int y) -> u16 {
        u16 result = 0;
        result = (result << 1) | (u16)get_pixel(buffer, x + adaptive_pixels[0].x, y + adaptive_pixels[0].y);
        for (int i = 0; i < 3; ++i)
            result = (result << 1) | (u16)get_pixel(buffer, x - 1 + i, y - 2);
        for (int i = 0; i < 4; ++i)
            result = (result << 1) | (u16)get_pixel(buffer, x - 2 + i, y - 1);
        for (int i = 0; i < 2; ++i)
            result = (result << 1) | (u16)get_pixel(buffer, x - 2 + i, y);
        return result;
    };

    // Figure 6 – Template when GBTEMPLATE = 3
    auto compute_context_3 = [](BilevelImage const& buffer, ReadonlySpan<JBIG2::AdaptiveTemplatePixel> adaptive_pixels, int x, int y) -> u16 {
        u16 result = 0;
        result = (result << 1) | (u16)get_pixel(buffer, x + adaptive_pixels[0].x, y + adaptive_pixels[0].y);
        for (int i = 0; i < 5; ++i)
            result = (result << 1) | (u16)get_pixel(buffer, x - 3 + i, y - 1);
        for (int i = 0; i < 4; ++i)
            result = (result << 1) | (u16)get_pixel(buffer, x - 4 + i, y);
        return result;
    };

    u16 (*compute_context)(BilevelImage const&, ReadonlySpan<JBIG2::AdaptiveTemplatePixel>, int, int);
    if (inputs.gb_template == 0)
        compute_context = compute_context_0;
    else if (inputs.gb_template == 1)
        compute_context = compute_context_1;
    else if (inputs.gb_template == 2)
        compute_context = compute_context_2;
    else {
        VERIFY(inputs.gb_template == 3);
        compute_context = compute_context_3;
    }

    // "The values of the pixels in this neighbourhood define a context. Each context has its own adaptive probability estimate
    //  used by the arithmetic coder (see Annex E)."
    // "* Decode the current pixel by invoking the arithmetic entropy decoding procedure, with CX set to the value formed by
    //    concatenating the label "GB" and the 10-16 pixel values gathered in CONTEXT."
    // Implementor's note: What this is supposed to mean is that we have a bunch of independent contexts, and we pick the
    // context for the current pixel based on pixel values in the neighborhood. The "GB" part just means this context is
    // independent from other contexts in the spec. They are passed in to this function.

    // Figure 8 – Reused context for coding the SLTP value when GBTEMPLATE is 0
    constexpr u16 sltp_context_for_template_0 = 0b10011'0110010'0101;

    // Figure 9 – Reused context for coding the SLTP value when GBTEMPLATE is 1
    constexpr u16 sltp_context_for_template_1 = 0b0011'110010'101;

    // Figure 10 – Reused context for coding the SLTP value when GBTEMPLATE is 2
    constexpr u16 sltp_context_for_template_2 = 0b001'11001'01;

    // Figure 11 – Reused context for coding the SLTP value when GBTEMPLATE is 3
    constexpr u16 sltp_context_for_template_3 = 0b011001'0101;

    u16 sltp_context = [](u8 gb_template) {
        if (gb_template == 0)
            return sltp_context_for_template_0;
        if (gb_template == 1)
            return sltp_context_for_template_1;
        if (gb_template == 2)
            return sltp_context_for_template_2;
        VERIFY(gb_template == 3);
        return sltp_context_for_template_3;
    }(inputs.gb_template);

    // 6.2.5.7 Decoding the bitmap
    QMArithmeticEncoder encoder = TRY(QMArithmeticEncoder::initialize(0));

    // "1) Set:
    //         LTP = 0"
    bool ltp = false; // "Line (uses) Typical Prediction" maybe?

    // " 2) Create a bitmap GBREG of width GBW and height GBH pixels."
    // auto result = TRY(BilevelImage::create(inputs.region_width, inputs.region_height));

    // "3) Decode each row as follows:"
    for (size_t y = 0; y < height; ++y) {
        // "a) If all GBH rows have been decoded then the decoding is complete; proceed to step 4)."
        // "b) If TPGDON is 1, then decode a bit using the arithmetic entropy coder..."
        if (inputs.is_typical_prediction_used) {
            bool is_line_identical_to_previous_line = false;
            if (y > 0) {
                // "i) If the current row of GBREG is identical to the row immediately above, then SLTP = 1; otherwise SLTP = 0."
                is_line_identical_to_previous_line = true;
                for (size_t x = 0; x < width; ++x) {
                    if (inputs.image.get_bit(x, y) != inputs.image.get_bit(x, y - 1)) {
                        is_line_identical_to_previous_line = false;
                        break;
                    }
                }
            }

            // "SLTP" in spec. "Swap LTP" or "Switch LTP" maybe?
            bool sltp = ltp ^ is_line_identical_to_previous_line;
            encoder.encode_bit(sltp, contexts.contexts[sltp_context]);
            ltp = is_line_identical_to_previous_line;
            if (ltp)
                continue;
        }

        // "d) If LTP = 0 then, from left to right, decode each pixel of the current row of GBREG. The procedure for each
        //     pixel is as follows:"
        for (size_t x = 0; x < width; ++x) {
            // "i) If USESKIP is 1 and the pixel in the bitmap SKIP at the location corresponding to the current pixel is 1,
            //     then set the current pixel to 0."
            if (inputs.skip_pattern.has_value() && inputs.skip_pattern->get_bit(x, y))
                continue;

            // "ii) Otherwise:"
            u16 context = compute_context(inputs.image, inputs.adaptive_template_pixels, x, y);
            encoder.encode_bit(inputs.image.get_bit(x, y), contexts.contexts[context]);
        }
    }

    // "4) After all the rows have been decoded, the current contents of the bitmap GBREG are the results that shall be
    //     obtained by every decoder, whether it performs this exact sequence of steps or not."
    // In the encoding case, this means the compressed data is complete.
    return encoder.finalize();
}

namespace {

struct JBIG2WritingContext {
    JBIG2Writer::Options options;
    Gfx::BilevelImage const& bilevel_image;
    u32 next_segment_number { 0 };
};

}

static ErrorOr<void> encode_jbig2_header(Stream& stream)
{
    TRY(stream.write_until_depleted(JBIG2::id_string));

    // D.4.2 File header flags
    u8 header_flags = 0;

    // FIXME: Make an option for this.
    JBIG2::Organization organization = JBIG2::Organization::Sequential;
    if (organization == JBIG2::Organization::Sequential)
        header_flags |= 1;

    bool has_known_number_of_pages = true;

    // FIXME: Add an option for this.
    bool uses_templates_with_12_AT_pixels = false;

    // FIXME: Maybe add support for colors one day.
    bool contains_colored_region_segments = false;

    if (!has_known_number_of_pages)
        header_flags |= 2;

    if (uses_templates_with_12_AT_pixels)
        header_flags |= 4;

    if (contains_colored_region_segments)
        header_flags |= 8;

    TRY(stream.write_value<u8>(header_flags));

    // D.4.3 Number of pages
    if (has_known_number_of_pages)
        TRY(stream.write_value<BigEndian<u32>>(1));

    return {};
}

static ErrorOr<void> encode_segment_header(Stream& stream, JBIG2::SegmentHeader const& header)
{
    // 7.2.2 Segment number
    TRY(stream.write_value<BigEndian<u32>>(header.segment_number));

    // 7.2.3 Segment header flags

    // FIXME: Add an option to force this.
    bool segment_page_association_size_is_32_bits = header.page_association >= 256;

    bool segment_retained_only_by_itself_and_extension_segments = false; // FIXME: Compute?

    u8 flags = static_cast<u8>(header.type);
    if (segment_page_association_size_is_32_bits)
        flags |= 0x40;
    if (segment_retained_only_by_itself_and_extension_segments)
        flags |= 0x80;

    TRY(stream.write_value<u8>(flags));

    // 7.2.4 Referred-to segment count and retention flags
    if (header.referred_to_segment_numbers.size() <= 4) {
        u8 count_and_retention_flags = 0;
        count_and_retention_flags |= header.referred_to_segment_numbers.size() << 5;
        // FIXME: Set retention flags properly?
        TRY(stream.write_value<u8>(count_and_retention_flags));
    } else {
        if (header.referred_to_segment_numbers.size() >= (1 << 28))
            return Error::from_string_literal("JBIG2Writer: Too many referred-to segments");
        u32 count_of_referred_to_segments = header.referred_to_segment_numbers.size();
        TRY(stream.write_value<BigEndian<u32>>(count_of_referred_to_segments | 7 << 28));
        // FIXME: Set retention flags properly?
        for (u32 i = 0; i < ceil_div(count_of_referred_to_segments + 1, 8u); i++)
            TRY(stream.write_value<u8>(0));
    }

    // 7.2.5 Referred-to segment numbers
    for (u32 referred_to_segment_number : header.referred_to_segment_numbers) {
        VERIFY(referred_to_segment_number < header.segment_number);
        if (header.segment_number <= 256)
            TRY(stream.write_value<u8>(referred_to_segment_number));
        else if (header.segment_number <= 65536)
            TRY(stream.write_value<BigEndian<u16>>(referred_to_segment_number));
        else
            TRY(stream.write_value<BigEndian<u32>>(referred_to_segment_number));
    }

    // 7.2.6 Segment page association
    if (segment_page_association_size_is_32_bits)
        TRY(stream.write_value<BigEndian<u32>>(header.page_association));
    else
        TRY(stream.write_value<u8>(header.page_association));

    // 7.2.7 Segment data length
    VERIFY(header.data_length.has_value() || header.type == JBIG2::SegmentType::ImmediateGenericRegion);
    if (header.data_length.has_value())
        TRY(stream.write_value<BigEndian<u32>>(*header.data_length));
    else
        TRY(stream.write_value<BigEndian<u32>>(0xffff'ffff));

    return {};
}

static ErrorOr<void> encode_region_segment_information_field(Stream& stream, JBIG2::RegionSegmentInformationField const& region_information)
{
    // 7.4.8 Page information segment syntax
    TRY(stream.write_value<BigEndian<u32>>(region_information.width));
    TRY(stream.write_value<BigEndian<u32>>(region_information.height));
    TRY(stream.write_value<BigEndian<u32>>(region_information.x_location));
    TRY(stream.write_value<BigEndian<u32>>(region_information.y_location));
    TRY(stream.write_value<u8>(region_information.flags));

    return {};
}

static ErrorOr<void> encode_immediate_lossless_generic_region(JBIG2WritingContext& context, Stream& stream)
{
    // FIXME: Add options for MMR, GBTEMPLATE, TPGDON, EXTTEMPLATE, USESKIP, GBATX/GBATY.
    GenericRegionEncodingInputParameters inputs { .image = context.bilevel_image };
    inputs.is_modified_modified_read = false;
    inputs.gb_template = 0;
    inputs.is_typical_prediction_used = true;
    inputs.is_extended_reference_template_used = false;
    inputs.adaptive_template_pixels[0] = { -1, -1 };
    inputs.adaptive_template_pixels[1] = { 0, -1 };
    inputs.adaptive_template_pixels[2] = { -1, 0 };
    inputs.adaptive_template_pixels[3] = { 1, -1 };
    inputs.require_eof_after_mmr = GenericRegionEncodingInputParameters::RequireEOFBAfterMMR::No;

    int number_of_adaptive_template_pixels = inputs.gb_template == 0 ? 4 : 1;

    Optional<JBIG2::GenericContexts> contexts = JBIG2::GenericContexts { inputs.gb_template };
    auto data = TRY(generic_region_encoding_procedure(inputs, contexts));

    // 7.4.6 Generic region segment syntax
    JBIG2::SegmentHeader generic_region_segment_header;
    generic_region_segment_header.segment_number = context.next_segment_number++;
    generic_region_segment_header.type = JBIG2::SegmentType::ImmediateLosslessGenericRegion;
    generic_region_segment_header.page_association = 1;

    // FIXME: Add option to set size to OptionalNone for immediate generic regions (7.2.7).
    // FIXME: Add option to write too-high height in region information field if data_length is OptionalNone
    //        (the actual height is written after the data then).
    generic_region_segment_header.data_length = sizeof(JBIG2::RegionSegmentInformationField) + 1 + 2 * number_of_adaptive_template_pixels + data.size();
    TRY(encode_segment_header(stream, generic_region_segment_header));

    JBIG2::RegionSegmentInformationField region_information;
    region_information.width = context.bilevel_image.width();
    region_information.height = context.bilevel_image.height();
    region_information.x_location = 0;
    region_information.y_location = 0;
    region_information.flags = 0;

    JBIG2::CombinationOperator combination_operator = context.options.default_combination_operator;
    region_information.flags |= static_cast<u8>(combination_operator);
    bool is_color_bitmap = false; // FIXME: Add support for colors one day.
    if (is_color_bitmap)
        region_information.flags |= 0x8;

    TRY(encode_region_segment_information_field(stream, region_information));

    u8 flags = 0;
    bool uses_mmr = false; // FIXME: Add an option for this.
    if (uses_mmr)
        flags |= 1;
    flags |= (inputs.gb_template & 3) << 1;
    if (inputs.is_typical_prediction_used)
        flags |= 0x8;
    if (inputs.is_extended_reference_template_used)
        flags |= 0x10;
    TRY(stream.write_value<u8>(flags));

    for (int i = 0; i < number_of_adaptive_template_pixels; ++i) {
        TRY(stream.write_value<i8>(inputs.adaptive_template_pixels[i].x));
        TRY(stream.write_value<i8>(inputs.adaptive_template_pixels[i].y));
    }
    TRY(stream.write_until_depleted(data));

    if (!generic_region_segment_header.data_length.has_value())
        TRY(stream.write_value<BigEndian<u32>>(region_information.height));

    return {};
}

static ErrorOr<void> encode_page_information(JBIG2WritingContext& context, Stream& stream, JBIG2::PageInformationSegment const& page_information)
{
    // 7.4.8 Page information segment syntax
    JBIG2::SegmentHeader page_information_segment_header;
    page_information_segment_header.segment_number = context.next_segment_number++;
    page_information_segment_header.type = JBIG2::SegmentType::PageInformation;
    page_information_segment_header.page_association = 1;
    page_information_segment_header.data_length = sizeof(JBIG2::PageInformationSegment);
    TRY(encode_segment_header(stream, page_information_segment_header));

    TRY(stream.write_value<BigEndian<u32>>(page_information.bitmap_width));
    TRY(stream.write_value<BigEndian<u32>>(page_information.bitmap_height));
    TRY(stream.write_value<BigEndian<u32>>(page_information.page_x_resolution));
    TRY(stream.write_value<BigEndian<u32>>(page_information.page_y_resolution));
    TRY(stream.write_value<u8>(page_information.flags));
    TRY(stream.write_value<BigEndian<u16>>(page_information.striping_information));

    return {};
}

static ErrorOr<void> encode_end_of_page(JBIG2WritingContext& context, Stream& stream)
{
    // 7.4.9 End of page segment syntax
    JBIG2::SegmentHeader end_of_page;
    end_of_page.segment_number = context.next_segment_number++;
    end_of_page.type = JBIG2::SegmentType::EndOfPage;
    end_of_page.page_association = 1;
    end_of_page.data_length = 0;
    return encode_segment_header(stream, end_of_page);
}

static ErrorOr<void> fill_page_information(JBIG2WritingContext& context, JBIG2::PageInformationSegment& page_information)
{
    page_information.bitmap_width = context.bilevel_image.width();
    page_information.bitmap_height = context.bilevel_image.height();
    page_information.page_x_resolution = 0;
    page_information.page_y_resolution = 0;

    page_information.flags = 0;

    bool is_eventually_lossless = true;
    bool might_contain_refinements = false;

    // FIXME: Make options for this.
    u8 default_color = 0; // 0 = white, 1 = black

    bool requires_auxiliary_buffers = false;                                   // FIXME: Compute?
    bool direct_region_segments_override_default_combination_operator = false; // FIXME: Compute?
    bool might_contain_coloured_segments = false;                              // FIXME: Add support for colors one day.

    if (is_eventually_lossless)
        page_information.flags |= 1;
    if (might_contain_refinements)
        page_information.flags |= 2;
    page_information.flags |= (default_color & 1) << 2;
    if (context.options.default_combination_operator == JBIG2::CombinationOperator::Replace)
        return Error::from_string_literal("JBIG2Writer: 'Replace' is invalid operator at page level");
    page_information.flags |= static_cast<u8>(context.options.default_combination_operator) << 3;

    if (requires_auxiliary_buffers)
        page_information.flags |= 0x20;
    if (direct_region_segments_override_default_combination_operator)
        page_information.flags |= 0x40;
    if (might_contain_coloured_segments)
        page_information.flags |= 0x80;

    page_information.striping_information = 0; // FIXME: Optionally stripe eventually.
    return {};
}

ErrorOr<void> JBIG2Writer::encode(Stream& stream, Bitmap const& bitmap, Options const& options)
{
    auto bilevel_image = TRY(BilevelImage::create_from_bitmap(bitmap, DitheringAlgorithm::FloydSteinberg));

    JBIG2WritingContext context { .options = options, .bilevel_image = *bilevel_image };
    TRY(encode_jbig2_header(stream));

    JBIG2::PageInformationSegment page_info;
    TRY(fill_page_information(context, page_info));
    TRY(encode_page_information(context, stream, page_info));

    TRY(encode_immediate_lossless_generic_region(context, stream));

    TRY(encode_end_of_page(context, stream));

    return {};
}

}
