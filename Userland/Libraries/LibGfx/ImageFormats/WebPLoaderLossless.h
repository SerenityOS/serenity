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

ErrorOr<VP8LHeader> decode_webp_chunk_VP8L_header(ReadonlyBytes vp8l_data);
ErrorOr<NonnullRefPtr<Bitmap>> decode_webp_chunk_VP8L_contents(VP8LHeader const& vp8l_header);

}
