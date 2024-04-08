/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/MemoryStream.h>
#include <LibGfx/ImageFormats/ISOBMFF/JPEG2000Boxes.h>
#include <LibGfx/ImageFormats/ISOBMFF/Reader.h>
#include <LibGfx/ImageFormats/JPEG2000Loader.h>

// Core coding system spec (.jp2 format): T-REC-T.800-201511-S!!PDF-E.pdf available here:
// https://www.itu.int/rec/dologin_pub.asp?lang=e&id=T-REC-T.800-201511-S!!PDF-E&type=items

// Extensions (.jpx format): T-REC-T.801-202106-S!!PDF-E.pdf available here:
// https://handle.itu.int/11.1002/1000/14666-en?locatt=format:pdf&auth

// rfc3745 lists the MIME type. It only mentions the jp2_id_string as magic number.

namespace Gfx {

// A JPEG2000 image can be stored in a codestream with markers, similar to a JPEG image,
// or in a JP2 file, which is a container format based on boxes similar to ISOBMFF.

// This is the marker for the codestream version. We don't support this yet.
// If we add support, add a second `"image/jp2"` line to MimeData.cpp for this magic number.
// T.800 Annex A, Codestream syntax, A.2 Information in the marker segments and A.3 Construction of the codestream
[[maybe_unused]] static constexpr u8 marker_id_string[] = { 0xFF, 0x4F, 0xFF, 0x51 };

// This is the marker for the box version.
// T.800 Annex I, JP2 file format syntax, I.5.1 JPEG 2000 Signature box
static constexpr u8 jp2_id_string[] = { 0x00, 0x00, 0x00, 0x0C, 0x6A, 0x50, 0x20, 0x20, 0x0D, 0x0A, 0x87, 0x0A };

// Table A.2 – List of markers and marker segments
// "Delimiting markers and marker segments"
#define J2K_SOC 0xFF4F // "Start of codestream"
#define J2K_SOT 0xFF90 // "Start of tile-part"
#define J2K_SOD 0xFF93 // "Start of data"
#define J2K_EOC 0xFFD9 // "End of codestream"
// "Fixed information marker segments"
#define J2K_SIZ 0xFF51 // "Image and tile size"
// "Functional marker segments"
#define J2K_COD 0xFF52 // "Coding style default"
#define J2K_COC 0xFF53 // "Coding style component"
#define J2K_RGN 0xFF5E // "Region-of-interest"
#define J2K_QCD 0xFF5C // "Quantization default"
#define J2K_QCC 0xFF5D // "Quantization component"
#define J2K_POC 0xFF5F // "Progression order change"
// "Pointer marker segments"
#define J2K_TLM 0xFF55 // "Tile-part lengths"
#define J2K_PLM 0xFF57 // "Packet length, main header"
#define J2K_PLT 0xFF58 // "Packet length, tile-part header"
#define J2K_PPM 0xFF60 // "Packed packet headers, main header"
#define J2K_PPT 0xFF61 // "Packed packet headers, tile-part header"
// "In-bit-stream markers and marker segments"
#define J2K_SOP 0xFF91 // "Start of packet"
#define J2K_EPH 0xFF92 // "End of packet header"
// "Informational marker segments"
#define J2K_CRG 0xFF63 // "Component registration"
#define J2K_COM 0xFF64 // "Comment"

// A.4.2 Start of tile-part (SOT)
struct StartOfTilePart {
    // "Tile index. This number refers to the tiles in raster order starting at the number 0."
    u16 tile_index { 0 }; // "Isot" in spec.

    // "Length, in bytes, from the beginning of the first byte of this SOT marker segment of the tile-part to
    //  the end of the data of that tile-part. Figure A.16 shows this alignment. Only the last tile-part in the
    //  codestream may contain a 0 for Psot. If the Psot is 0, this tile-part is assumed to contain all data until
    //  the EOC marker."
    u32 tile_part_length { 0 }; // "Psot" in spec.

    // "Tile-part index. There is a specific order required for decoding tile-parts; this index denotes the order
    //  from 0. If there is only one tile-part for a tile, then this value is zero. The tile-parts of this tile shall
    //  appear in the codestream in this order, although not necessarily consecutively."
    u8 tile_part_index { 0 }; // "TPsot" in spec.

    // "Number of tile-parts of a tile in the codestream. Two values are allowed: the correct number of tile-
    //  parts for that tile and zero. A zero value indicates that the number of tile-parts of this tile is not
    //  specified in this tile-part.
    u8 number_of_tile_parts { 0 }; // "TNsot" in spec.
};

static ErrorOr<StartOfTilePart> read_start_of_tile_part(ReadonlyBytes data)
{
    if (data.size() < 8)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Not enough data for SOT marker segment");

    StartOfTilePart sot;
    sot.tile_index = *reinterpret_cast<AK::BigEndian<u16> const*>(data.data());
    sot.tile_part_length = *reinterpret_cast<AK::BigEndian<u32> const*>(data.data() + 2);
    sot.tile_part_index = data[6];
    sot.number_of_tile_parts = data[7];

    dbgln_if(JPEG2000_DEBUG, "JPEG2000ImageDecoderPlugin: SOT marker segment: tile_index={}, tile_part_length={}, tile_part_index={}, number_of_tile_parts={}", sot.tile_index, sot.tile_part_length, sot.tile_part_index, sot.number_of_tile_parts);

    return sot;
}

struct JPEG2000LoadingContext {
    enum class State {
        NotDecoded = 0,
        DecodedTileHeaders,
        Error,
    };
    State state { State::NotDecoded };
    ReadonlyBytes codestream_data;
    size_t codestream_cursor { 0 };
    Optional<ReadonlyBytes> icc_data;

    IntSize size;

    ISOBMFF::BoxList boxes;
};

struct MarkerSegment {
    u16 marker;

    // OptionalNone for markers that don't have data.
    // For markers that do have data, this does not include the marker length data. (`data.size() + 2` is the value of the marker length field.)
    Optional<ReadonlyBytes> data;
};

static ErrorOr<u16> peek_marker(JPEG2000LoadingContext& context)
{
    if (context.codestream_cursor + 2 > context.codestream_data.size())
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Not enough data for marker");
    return *reinterpret_cast<AK::BigEndian<u16> const*>(context.codestream_data.data() + context.codestream_cursor);
}

static ErrorOr<MarkerSegment> read_marker_at_cursor(JPEG2000LoadingContext& context)
{
    u16 marker = TRY(peek_marker(context));
    // "All markers with the marker code between 0xFF30 and 0xFF3F have no marker segment parameters. They shall be skipped by the decoder."
    // "The SOC, SOD and EOC are delimiting markers not marker segments, and have no explicit length information or other parameters."
    bool is_marker_segment = !(marker >= 0xFF30 && marker <= 0xFF3F) && marker != J2K_SOC && marker != J2K_SOD && marker != J2K_EOC;

    MarkerSegment marker_segment;
    marker_segment.marker = marker;

    if (is_marker_segment) {
        if (context.codestream_cursor + 4 > context.codestream_data.size())
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Not enough data for marker segment length");
        u16 marker_length = *reinterpret_cast<AK::BigEndian<u16> const*>(context.codestream_data.data() + context.codestream_cursor + 2);
        if (marker_length < 2)
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Marker segment length too small");
        if (context.codestream_cursor + 2 + marker_length > context.codestream_data.size())
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Not enough data for marker segment data");
        marker_segment.data = ReadonlyBytes { context.codestream_data.data() + context.codestream_cursor + 4, marker_length - 2u };
    }

    context.codestream_cursor += 2;
    if (is_marker_segment)
        context.codestream_cursor += 2 + marker_segment.data->size();

    return marker_segment;
}

static ErrorOr<void> parse_codestream_main_header(JPEG2000LoadingContext& context)
{
    // Figure A.3 – Construction of the main header
    // "Required as the first marker"
    auto marker = TRY(read_marker_at_cursor(context));
    if (marker.marker != J2K_SOC)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Expected SOC marker");

    // "Required as the second marker segment"
    marker = TRY(read_marker_at_cursor(context));
    if (marker.marker != J2K_SIZ)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Expected SIZ marker");
    // FIXME: Parse SIZ marker.

    while (true) {
        u16 marker = TRY(peek_marker(context));
        switch (marker) {
        case J2K_COD:
        case J2K_COC:
        case J2K_QCD:
        case J2K_QCC:
        case J2K_RGN:
        case J2K_POC:
        case J2K_PPM:
        case J2K_TLM:
        case J2K_PLM:
        case J2K_CRG:
        case J2K_COM: {
            // FIXME: These are valid main header markers. Parse contents.
            auto marker = TRY(read_marker_at_cursor(context));
            dbgln("JPEG2000ImageDecoderPlugin: marker {:#04x} not yet implemented", marker.marker);
            break;
        }
        case J2K_SOT:
            // SOT terminates the main header.
            return {};
        default:
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Unexpected marker in main header");
        }
    }
}

static ErrorOr<void> parse_codestream_tile_header(JPEG2000LoadingContext& context)
{
    // Figure A.4 – Construction of the first tile-part header of a given tile
    // Figure A.5 – Construction of a non-first tile-part header

    // "Required as the first marker segment of every tile-part header"
    auto tile_start = context.codestream_cursor;
    auto marker = TRY(read_marker_at_cursor(context));
    if (marker.marker != J2K_SOT)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Expected SOT marker");
    auto start_of_tile = TRY(read_start_of_tile_part(marker.data.value()));
    // FIXME: Store start_of_tile on context somewhere.

    bool found_start_of_data = false;
    while (!found_start_of_data) {
        u16 marker = TRY(peek_marker(context));
        switch (marker) {
        case J2K_SOD:
            // "Required as the last marker segment of every tile-part header"
            context.codestream_cursor += 2;
            found_start_of_data = true;
            break;
        // FIXME: COD, COC, QCD, QCC are only valid on the first tile part header, reject them in non-first tile part headers.
        case J2K_COD:
        case J2K_COC:
        case J2K_QCD:
        case J2K_QCC:
        case J2K_RGN:
        case J2K_POC:
        case J2K_PPT:
        case J2K_PLT:
        case J2K_COM: {
            // FIXME: These are valid tile part header markers. Parse contents.
            auto marker = TRY(read_marker_at_cursor(context));
            dbgln("JPEG2000ImageDecoderPlugin: marker {:#04x} not yet implemented in tile header", marker.marker);
            break;
        }
        default:
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Unexpected marker in tile header");
        }
    }

    u32 tile_bitstream_length;
    if (start_of_tile.tile_part_length == 0) {
        // Leave room for EOC marker.
        if (context.codestream_data.size() - context.codestream_cursor < 2)
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Not enough data for EOC marker");
        tile_bitstream_length = context.codestream_data.size() - context.codestream_cursor - 2;
    } else {
        u32 tile_header_length = context.codestream_cursor - tile_start;
        if (start_of_tile.tile_part_length < tile_header_length)
            return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Invalid tile part length");
        tile_bitstream_length = start_of_tile.tile_part_length - tile_header_length;
    }

    if (context.codestream_cursor + tile_bitstream_length > context.codestream_data.size())
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Not enough data for tile bitstream");
    // FIXME: Store context.codestream_data.slice(context.codestream_cursor, tile_bitstream_length) somewhere on the context.

    context.codestream_cursor += tile_bitstream_length;
    dbgln_if(JPEG2000_DEBUG, "JPEG2000ImageDecoderPlugin: Tile bitstream length: {}", tile_bitstream_length);

    return {};
}

static ErrorOr<void> parse_codestream_tile_headers(JPEG2000LoadingContext& context)
{
    while (true) {
        auto marker = TRY(peek_marker(context));
        if (marker == J2K_EOC) {
            context.codestream_cursor += 2;
            break;
        }
        TRY(parse_codestream_tile_header(context));
    }

    if (context.codestream_cursor < context.codestream_data.size())
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Unexpected data after EOC marker");
    return {};
}

static ErrorOr<void> decode_jpeg2000_header(JPEG2000LoadingContext& context, ReadonlyBytes data)
{
    if (!JPEG2000ImageDecoderPlugin::sniff(data))
        return Error::from_string_literal("JPEG2000LoadingContext: Invalid JPEG2000 header");

    auto reader = TRY(Gfx::ISOBMFF::Reader::create(TRY(try_make<FixedMemoryStream>(data))));
    context.boxes = TRY(reader.read_entire_file());

    // I.2.2 File organization
    // "A particular order of those boxes in the file is not generally implied. However, the JPEG 2000 Signature box
    //  shall be the first box in a JP2 file, the File Type box shall immediately follow the JPEG 2000 Signature box
    //  and the JP2 Header box shall fall before the Contiguous Codestream box."
    if (context.boxes.size() < 4)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Expected at least four boxes");

    // Required toplevel boxes: signature box, file type box, jp2 header box, contiguous codestream box.

    if (context.boxes[0]->box_type() != ISOBMFF::BoxType::JPEG2000SignatureBox)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Expected JPEG2000SignatureBox as first box");
    if (context.boxes[1]->box_type() != ISOBMFF::BoxType::FileTypeBox)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Expected FileTypeBox as second box");

    Optional<size_t> jp2_header_box_index;
    Optional<size_t> contiguous_codestream_box_index;
    for (size_t i = 2; i < context.boxes.size(); ++i) {
        if (context.boxes[i]->box_type() == ISOBMFF::BoxType::JPEG2000HeaderBox) {
            // "Within a JP2 file, there shall be one and only one JP2 Header box."
            if (jp2_header_box_index.has_value())
                return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Multiple JP2 Header boxes");
            jp2_header_box_index = i;
        }
        if (context.boxes[i]->box_type() == ISOBMFF::BoxType::JPEG2000ContiguousCodestreamBox && !contiguous_codestream_box_index.has_value()) {
            // "a conforming reader shall ignore all codestreams after the first codestream found in the file.
            //  Contiguous Codestream boxes may be found anywhere in the file except before the JP2 Header box."
            contiguous_codestream_box_index = i;
            if (!jp2_header_box_index.has_value() || contiguous_codestream_box_index.value() < jp2_header_box_index.value())
                return Error::from_string_literal("JPEG2000ImageDecoderPlugin: JP2 Header box must come before Contiguous Codestream box");
        }
    }

    if (!jp2_header_box_index.has_value())
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Expected JP2 Header box");
    if (!contiguous_codestream_box_index.has_value())
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Expected Contiguous Codestream box");

    // FIXME: JPEG2000ContiguousCodestreamBox makes a copy of the codestream data. That's too heavy for header scanning.
    // Add a mode to ISOBMFF::Reader where it only stores offsets for the codestream data and the ICC profile.
    auto const& codestream_box = static_cast<ISOBMFF::JPEG2000ContiguousCodestreamBox const&>(*context.boxes[contiguous_codestream_box_index.value()]);
    context.codestream_data = codestream_box.codestream.bytes();

    // Required child boxes of the jp2 header box: image header box, color box.

    Optional<size_t> image_header_box_index;
    Optional<size_t> color_header_box_index;
    auto const& header_box = static_cast<ISOBMFF::JPEG2000HeaderBox const&>(*context.boxes[jp2_header_box_index.value()]);
    for (size_t i = 0; i < header_box.child_boxes().size(); ++i) {
        auto const& subbox = header_box.child_boxes()[i];
        if (subbox->box_type() == ISOBMFF::BoxType::JPEG2000ImageHeaderBox) {
            if (image_header_box_index.has_value())
                return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Multiple Image Header boxes");
            image_header_box_index = i;
        }
        if (subbox->box_type() == ISOBMFF::BoxType::JPEG2000ColorSpecificationBox) {
            // T.800 says there should be just one 'colr' box, but T.801 allows several and says to pick the one with highest precedence.
            bool use_this_color_box;
            if (!color_header_box_index.has_value()) {
                use_this_color_box = true;
            } else {
                auto const& new_header_box = static_cast<ISOBMFF::JPEG2000ColorSpecificationBox const&>(*header_box.child_boxes()[i]);
                auto const& current_color_box = static_cast<ISOBMFF::JPEG2000ColorSpecificationBox const&>(*header_box.child_boxes()[color_header_box_index.value()]);
                use_this_color_box = new_header_box.precedence > current_color_box.precedence;
            }

            if (use_this_color_box)
                color_header_box_index = i;
        }
    }

    if (!image_header_box_index.has_value())
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Expected Image Header box");
    if (!color_header_box_index.has_value())
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Expected Color Specification box");

    auto const& image_header_box = static_cast<ISOBMFF::JPEG2000ImageHeaderBox const&>(*header_box.child_boxes()[image_header_box_index.value()]);
    context.size = { image_header_box.width, image_header_box.height };

    auto const& color_header_box = static_cast<ISOBMFF::JPEG2000ColorSpecificationBox const&>(*header_box.child_boxes()[color_header_box_index.value()]);
    if (color_header_box.method == 2 || color_header_box.method == 3)
        context.icc_data = color_header_box.icc_data.bytes();

    TRY(parse_codestream_main_header(context));

    return {};
}

bool JPEG2000ImageDecoderPlugin::sniff(ReadonlyBytes data)
{
    return data.starts_with(jp2_id_string);
}

JPEG2000ImageDecoderPlugin::JPEG2000ImageDecoderPlugin()
{
    m_context = make<JPEG2000LoadingContext>();
}

IntSize JPEG2000ImageDecoderPlugin::size()
{
    return m_context->size;
}

ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> JPEG2000ImageDecoderPlugin::create(ReadonlyBytes data)
{
    auto plugin = TRY(adopt_nonnull_own_or_enomem(new (nothrow) JPEG2000ImageDecoderPlugin()));
    TRY(decode_jpeg2000_header(*plugin->m_context, data));
    return plugin;
}

ErrorOr<ImageFrameDescriptor> JPEG2000ImageDecoderPlugin::frame(size_t index, Optional<IntSize>)
{
    if (index != 0)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Invalid frame index");

    if (m_context->state == JPEG2000LoadingContext::State::Error)
        return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Decoding failed");

    if (m_context->state < JPEG2000LoadingContext::State::DecodedTileHeaders) {
        TRY(parse_codestream_tile_headers(*m_context));
        m_context->state = JPEG2000LoadingContext::State::DecodedTileHeaders;
    }

    return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Draw the rest of the owl");
}

ErrorOr<Optional<ReadonlyBytes>> JPEG2000ImageDecoderPlugin::icc_data()
{
    return m_context->icc_data;
}

}
