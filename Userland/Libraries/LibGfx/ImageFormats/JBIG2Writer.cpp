/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

// Spec: ITU-T_T_88__08_2018.pdf in the zip file here:
// https://www.itu.int/rec/T-REC-T.88-201808-I
// JBIG2Loader.cpp has many spec notes.

#include <AK/BitStream.h>
#include <AK/MemoryStream.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/StdLibExtras.h>
#include <AK/Stream.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/BilevelImage.h>
#include <LibGfx/ImageFormats/CCITTEncoder.h>
#include <LibGfx/ImageFormats/JBIG2Writer.h>
#include <LibGfx/ImageFormats/MQArithmeticCoder.h>

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

    if (inputs.is_modified_modified_read) {
        // FIXME: It's a bit wasteful to re-convert the BilevelImage to a Bitmap here.
        AllocatingMemoryStream output_stream;
        TRY(Gfx::CCITT::Group4Encoder::encode(output_stream, TRY(inputs.image.to_gfx_bitmap())));
        return output_stream.read_until_eof();
    }

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
        // 6.2.5.2 Coding order and edge conventions
        // "• All pixels lying outside the bounds of the actual bitmap have the value 0."
        // We don't have to check y >= buffer->height() because check_valid_adaptive_template_pixel() rejects y > 0.
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
    MQArithmeticEncoder encoder = TRY(MQArithmeticEncoder::initialize(0));

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
            // "i) If the current row of GBREG is identical to the row immediately above, then SLTP = 1; otherwise SLTP = 0."
            bool is_line_identical_to_previous_line = true;
            for (size_t x = 0; x < width; ++x) {
                if (inputs.image.get_bit(x, y) != get_pixel(inputs.image, (int)x, (int)y - 1)) {
                    is_line_identical_to_previous_line = false;
                    break;
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

static ErrorOr<void> encode_jbig2_header(Stream& stream, JBIG2::FileHeaderData const& header)
{
    TRY(stream.write_until_depleted(JBIG2::id_string));

    // D.4.2 File header flags
    u8 header_flags = 0;

    JBIG2::Organization organization = header.organization;
    if (organization == JBIG2::Organization::Sequential)
        header_flags |= 1;

    // FIXME: Add an option for this.
    bool uses_templates_with_12_AT_pixels = false;

    // FIXME: Maybe add support for colors one day.
    bool contains_colored_region_segments = false;

    if (!header.number_of_pages.has_value())
        header_flags |= 2;

    if (uses_templates_with_12_AT_pixels)
        header_flags |= 4;

    if (contains_colored_region_segments)
        header_flags |= 8;

    TRY(stream.write_value<u8>(header_flags));

    // D.4.3 Number of pages
    if (header.number_of_pages.has_value())
        TRY(stream.write_value<BigEndian<u32>>(header.number_of_pages.value()));

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
    VERIFY(header.referred_to_segment_numbers.size() == header.referred_to_segment_retention_flags.size());
    if (header.referred_to_segment_numbers.size() <= 4) {
        u8 count_and_retention_flags = 0;
        count_and_retention_flags |= header.referred_to_segment_numbers.size() << 5;
        if (header.retention_flag)
            count_and_retention_flags |= 1;
        for (size_t i = 0; i < header.referred_to_segment_numbers.size(); ++i) {
            if (header.referred_to_segment_retention_flags[i])
                count_and_retention_flags |= 1 << (i + 1);
        }
        TRY(stream.write_value<u8>(count_and_retention_flags));
    } else {
        if (header.referred_to_segment_numbers.size() >= (1 << 28))
            return Error::from_string_literal("JBIG2Writer: Too many referred-to segments");
        u32 count_of_referred_to_segments = header.referred_to_segment_numbers.size();
        TRY(stream.write_value<BigEndian<u32>>(count_of_referred_to_segments | 7 << 28));

        LittleEndianOutputBitStream bit_stream { MaybeOwned { stream } };
        TRY(bit_stream.write_bits(header.retention_flag, 1));
        for (size_t i = 0; i < header.referred_to_segment_numbers.size(); ++i)
            TRY(bit_stream.write_bits(header.referred_to_segment_retention_flags[i], 1));
        TRY(bit_stream.flush_buffer_to_stream());
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

static ErrorOr<void> encode_page_information_data(Stream& stream, JBIG2::PageInformationSegment const& page_information)
{
    TRY(stream.write_value<BigEndian<u32>>(page_information.bitmap_width));
    TRY(stream.write_value<BigEndian<u32>>(page_information.bitmap_height));
    TRY(stream.write_value<BigEndian<u32>>(page_information.page_x_resolution));
    TRY(stream.write_value<BigEndian<u32>>(page_information.page_y_resolution));
    TRY(stream.write_value<u8>(page_information.flags));
    TRY(stream.write_value<BigEndian<u16>>(page_information.striping_information));
    return {};
}

ErrorOr<void> JBIG2Writer::encode(Stream& stream, Bitmap const& bitmap, Options const&)
{
    auto bilevel_image = TRY(BilevelImage::create_from_bitmap(bitmap, DitheringAlgorithm::FloydSteinberg));

    JBIG2::FileData jbig2;
    jbig2.header.number_of_pages = 1;
    jbig2.header.organization = JBIG2::Organization::Sequential;

    u32 next_segment_number { 0 };
    auto next_segment_header = [&next_segment_number]() {
        JBIG2::SegmentHeaderData header;
        header.segment_number = next_segment_number++;
        header.page_association = 1;
        return header;
    };

    Gfx::JBIG2::PageInformationSegment page_info {};
    page_info.bitmap_width = bilevel_image->width();
    page_info.bitmap_height = bilevel_image->height();
    page_info.flags = 1; // "eventually lossless" bit set, default pixel value white, default combination operator OR.
    jbig2.segments.append(JBIG2::SegmentData {
        .header = next_segment_header(),
        .data = page_info,
    });

    JBIG2::GenericRegionSegmentData generic_region { .image = move(bilevel_image) };
    generic_region.region_segment_information.width = generic_region.image->width();
    generic_region.region_segment_information.height = generic_region.image->height();
    generic_region.region_segment_information.flags = 0;
    generic_region.adaptive_template_pixels[0] = { 3, -1 };
    generic_region.adaptive_template_pixels[1] = { -3, -1 };
    generic_region.adaptive_template_pixels[2] = { 2, -2 };
    generic_region.adaptive_template_pixels[3] = { -2, -2 };
    generic_region.flags = 1u << 3; // TPGDON, gb_template 0.
    jbig2.segments.append(JBIG2::SegmentData {
        .header = next_segment_header(),
        .data = JBIG2::ImmediateGenericRegionSegmentData { .generic_region = move(generic_region) },
    });

    jbig2.segments.append(JBIG2::SegmentData {
        .header = next_segment_header(),
        .data = JBIG2::EndOfPageSegmentData {},
    });

    return encode_with_explicit_data(stream, jbig2);
}

static ErrorOr<void> encode_generic_region(JBIG2::GenericRegionSegmentData const& generic_region, Vector<u8>& scratch_buffer)
{
    GenericRegionEncodingInputParameters inputs { .image = *generic_region.image };
    inputs.is_modified_modified_read = generic_region.flags & 1;
    inputs.gb_template = (generic_region.flags >> 1) & 3;
    inputs.is_typical_prediction_used = (generic_region.flags >> 3) & 1;
    inputs.is_extended_reference_template_used = (generic_region.flags >> 4) & 1;
    inputs.adaptive_template_pixels = generic_region.adaptive_template_pixels;
    inputs.require_eof_after_mmr = GenericRegionEncodingInputParameters::RequireEOFBAfterMMR::No;
    Optional<JBIG2::GenericContexts> contexts;
    if (!inputs.is_modified_modified_read)
        contexts = JBIG2::GenericContexts { inputs.gb_template };
    auto data = TRY(generic_region_encoding_procedure(inputs, contexts));

    int number_of_adaptive_template_pixels = 0;
    if (!inputs.is_modified_modified_read)
        number_of_adaptive_template_pixels = inputs.gb_template == 0 ? 4 : 1;

    if (inputs.gb_template == 0 && inputs.is_extended_reference_template_used) {
        // This was added in T.88 Amendment 2 (https://www.itu.int/rec/T-REC-T.88-200306-S!Amd2/en) mid-2003.
        // I haven't seen it being used in the wild, and the spec says "32-byte field as shown below" and then shows 24 bytes,
        // so it's not clear how much data to write.
        return Error::from_string_literal("JBIG2Writer: GBTEMPLATE=0 EXTTEMPLATE=1 not yet implemented");
    }

    TRY(scratch_buffer.try_resize(sizeof(JBIG2::RegionSegmentInformationField) + 1 + 2 * number_of_adaptive_template_pixels + data.size()));
    FixedMemoryStream stream { scratch_buffer, FixedMemoryStream::Mode::ReadWrite };

    TRY(encode_region_segment_information_field(stream, generic_region.region_segment_information));
    TRY(stream.write_value<u8>(generic_region.flags));
    for (int i = 0; i < number_of_adaptive_template_pixels; ++i) {
        TRY(stream.write_value<i8>(generic_region.adaptive_template_pixels[i].x));
        TRY(stream.write_value<i8>(generic_region.adaptive_template_pixels[i].y));
    }
    TRY(stream.write_until_depleted(data));
    return {};
}

static ErrorOr<void> encode_segment(Stream& stream, JBIG2::SegmentData const& segment_data)
{
    Vector<u8> scratch_buffer;

    auto encoded_data = TRY(segment_data.data.visit(
        [&scratch_buffer](JBIG2::ImmediateGenericRegionSegmentData const& generic_region_wrapper) -> ErrorOr<ReadonlyBytes> {
            TRY(encode_generic_region(generic_region_wrapper.generic_region, scratch_buffer));
            return scratch_buffer;
        },
        [&scratch_buffer](JBIG2::ImmediateLosslessGenericRegionSegmentData const& generic_region_wrapper) -> ErrorOr<ReadonlyBytes> {
            TRY(encode_generic_region(generic_region_wrapper.generic_region, scratch_buffer));
            return scratch_buffer;
        },
        [&scratch_buffer](JBIG2::PageInformationSegment const& page_information) -> ErrorOr<ReadonlyBytes> {
            TRY(scratch_buffer.try_resize(sizeof(JBIG2::PageInformationSegment)));
            FixedMemoryStream stream { scratch_buffer, FixedMemoryStream::Mode::ReadWrite };
            TRY(encode_page_information_data(stream, page_information));
            return scratch_buffer;
        },
        [](JBIG2::EndOfFileSegmentData const&) -> ErrorOr<ReadonlyBytes> {
            return ReadonlyBytes {};
        },
        [](JBIG2::EndOfPageSegmentData const&) -> ErrorOr<ReadonlyBytes> {
            return ReadonlyBytes {};
        }));

    JBIG2::SegmentHeader header;
    header.segment_number = segment_data.header.segment_number;
    header.type = segment_data.data.visit(
        [](JBIG2::ImmediateGenericRegionSegmentData const&) { return JBIG2::SegmentType::ImmediateGenericRegion; },
        [](JBIG2::ImmediateLosslessGenericRegionSegmentData const&) { return JBIG2::SegmentType::ImmediateLosslessGenericRegion; },
        [](JBIG2::PageInformationSegment const&) { return JBIG2::SegmentType::PageInformation; },
        [](JBIG2::EndOfFileSegmentData const&) { return JBIG2::SegmentType::EndOfFile; },
        [](JBIG2::EndOfPageSegmentData const&) { return JBIG2::SegmentType::EndOfPage; });
    header.retention_flag = segment_data.header.retention_flag;
    for (auto const& reference : segment_data.header.referred_to_segments) {
        header.referred_to_segment_numbers.append(reference.segment_number);
        header.referred_to_segment_retention_flags.append(reference.retention_flag);
    }
    header.page_association = segment_data.header.page_association;
    header.data_length = encoded_data.size(); // FIXME: Make optional for immediate generic regions.

    TRY(encode_segment_header(stream, header));
    TRY(stream.write_until_depleted(encoded_data));

    return {};
}

ErrorOr<void> JBIG2Writer::encode_with_explicit_data(Stream& stream, JBIG2::FileData const& file_data)
{
    if (file_data.header.organization != JBIG2::Organization::Sequential)
        return Error::from_string_literal("can only encode sequential files yet");

    TRY(encode_jbig2_header(stream, file_data.header));

    for (auto const& segment : file_data.segments) {
        TRY(encode_segment(stream, segment));
    }

    return {};
}

}
