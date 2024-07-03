/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Point.h>

namespace Gfx {

class AnimationWriter {
public:
    virtual ~AnimationWriter();

    enum class BlendMode {
        // The new frame replaces the data below it.
        Replace,

        // The new frame is blended on top of the data below it.
        // Use only when the new frame has completely opaque and completely transparent pixels.
        // The opaque pixels will replace the pixels below them, the transparent pixels will leave pixels below them unchanged.
        // Use only with AnimationWriter subclasses that return true from can_blend_frames().
        Blend,
    };

    // Flushes the frame to disk.
    // IntRect { at, at + bitmap.size() } must fit in the dimensions
    // passed to `start_writing_animation()`.
    virtual ErrorOr<void> add_frame(Bitmap&, int duration_ms, IntPoint at = {}, BlendMode disposal_method = BlendMode::Replace) = 0;

    // If this is set to Yes and can_blend_frames() returns true, add_frame_relative_to_last_frame() may
    // call add_frame() with BlendMode::Blend and a frame that has transparent pixels.
    enum class AllowInterFrameCompression {
        No,
        Yes,
    };
    ErrorOr<void> add_frame_relative_to_last_frame(Bitmap&, int duration_ms, RefPtr<Bitmap> last_frame, AllowInterFrameCompression = AllowInterFrameCompression::Yes);

    virtual bool can_blend_frames() const { return false; }

private:
    bool can_zero_out_unchanging_pixels(Bitmap& new_frame, Gfx::IntRect new_frame_rect, Bitmap& last_frame, AllowInterFrameCompression) const;
};

}
