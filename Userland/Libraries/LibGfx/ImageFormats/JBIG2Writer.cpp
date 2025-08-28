/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

// Spec: ITU-T_T_88__08_2018.pdf in the zip file here:
// https://www.itu.int/rec/T-REC-T.88-201808-I
// JBIG2Loader.cpp has many spec notes.

#include <AK/StdLibExtras.h>
#include <AK/Stream.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/JBIG2Writer.h>

namespace Gfx {

namespace {

struct JBIG2WritingContext {
    JBIG2Writer::Options options;
    Gfx::Bitmap const& bitmap;
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
    page_information.bitmap_width = context.bitmap.width();
    page_information.bitmap_height = context.bitmap.height();
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
    JBIG2WritingContext context { .options = options, .bitmap = bitmap };
    TRY(encode_jbig2_header(stream));

    JBIG2::PageInformationSegment page_info;
    TRY(fill_page_information(context, page_info));
    TRY(encode_page_information(context, stream, page_info));

    TRY(encode_end_of_page(context, stream));

    return {};
}

}
