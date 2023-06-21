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

struct VP8LHeader {
    u16 width;
    u16 height;
    bool is_alpha_used;
    ReadonlyBytes lossless_data;
};

// Parses the header data in a VP8L chunk. Pass the payload of a `VP8L` chunk, after the tag and after the tag's data size.
ErrorOr<VP8LHeader> decode_webp_chunk_VP8L_header(ReadonlyBytes vp8l_data);

ErrorOr<NonnullRefPtr<Bitmap>> decode_webp_chunk_VP8L_contents(VP8LHeader const&);

}
