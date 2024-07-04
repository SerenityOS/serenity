/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <LibGfx/Forward.h>

namespace Gfx {

struct VP8LEncoderOptions {
    // For each TransformType, set bit `1 << transform_type` if that transform type is allowed.
    unsigned allowed_transforms { 0xf };

    // If set, must be in [1, 11].
    // Even if this set, if the encoder decides that a color cache would not be useful, it may not use one
    // (e.g. for images that use a color indexing transform already).
    Optional<unsigned> color_cache_bits { 6 };
};

ErrorOr<ByteBuffer> compress_VP8L_image_data(Bitmap const&, VP8LEncoderOptions const&, bool& is_fully_opaque);

}
