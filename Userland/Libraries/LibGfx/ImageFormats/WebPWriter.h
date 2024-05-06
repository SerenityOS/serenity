/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <LibGfx/Color.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Point.h>

namespace Gfx {

struct WebPEncoderOptions {
    Optional<ReadonlyBytes> icc_data;
};

class AnimationWriter {
public:
    virtual ~AnimationWriter() = default;

    // Flushes the frame to disk.
    // IntRect { at, at + bitmap.size() } must fit in the dimensions
    // passed to `start_writing_animation()`.
    // FIXME: Consider passing in disposal method and blend mode.
    virtual ErrorOr<void> add_frame(Bitmap&, int duration_ms, IntPoint at = {}) = 0;
};

class WebPWriter {
public:
    using Options = WebPEncoderOptions;

    // Always lossless at the moment.
    static ErrorOr<void> encode(Stream&, Bitmap const&, Options const& = {});

    // Always lossless at the moment.
    static ErrorOr<NonnullOwnPtr<AnimationWriter>> start_encoding_animation(SeekableStream&, IntSize dimensions, int loop_count = 0, Color background_color = Color::Black, Options const& = {});

private:
    WebPWriter() = delete;
};

}
