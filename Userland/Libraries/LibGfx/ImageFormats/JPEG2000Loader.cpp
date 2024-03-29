/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

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

struct JPEG2000LoadingContext {
    enum class State {
        NotDecoded = 0,
        Error,
    };
    State state { State::NotDecoded };
    ReadonlyBytes codestream_data;
    Optional<ReadonlyBytes> icc_data;

    IntSize size;

    ISOBMFF::BoxList boxes;
};

static ErrorOr<void> decode_jpeg2000_header(JPEG2000LoadingContext& context, ReadonlyBytes data)
{
    if (!JPEG2000ImageDecoderPlugin::sniff(data))
        return Error::from_string_literal("JBIG2LoadingContext: Invalid JBIG2 header");

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

    return Error::from_string_literal("JPEG2000ImageDecoderPlugin: Draw the rest of the owl");
}

ErrorOr<Optional<ReadonlyBytes>> JPEG2000ImageDecoderPlugin::icc_data()
{
    return m_context->icc_data;
}

}
