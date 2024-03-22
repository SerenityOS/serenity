/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

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

bool JPEG2000ImageDecoderPlugin::sniff(ReadonlyBytes data)
{
    return data.starts_with(jp2_id_string);
}

ErrorOr<NonnullOwnPtr<ImageDecoderPlugin>> JPEG2000ImageDecoderPlugin::create(ReadonlyBytes)
{
    return Error::from_string_view("FIXME: Draw the rest of the owl"sv);
}

}
