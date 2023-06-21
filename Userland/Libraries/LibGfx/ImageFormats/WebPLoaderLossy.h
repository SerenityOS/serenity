/*
 * Copyright (c) 2023, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Span.h>
#include <AK/Types.h>
#include <LibGfx/Bitmap.h>

namespace Gfx {

struct VP8Header {
    u8 version;
    bool show_frame;
    u32 size_of_first_partition;
    u32 width;
    u8 horizontal_scale;
    u32 height;
    u8 vertical_scale;
    ReadonlyBytes first_partition;
    ReadonlyBytes second_partition;
};

// Parses the header data in a VP8 chunk. Pass the payload of a `VP8 ` chunk, after the tag and after the tag's data size.
ErrorOr<VP8Header> decode_webp_chunk_VP8_header(ReadonlyBytes vp8_data);

ErrorOr<NonnullRefPtr<Bitmap>> decode_webp_chunk_VP8_contents(VP8Header const&, bool include_alpha_channel);

}
