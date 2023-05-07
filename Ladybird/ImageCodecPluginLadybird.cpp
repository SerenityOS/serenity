/*
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ImageCodecPluginLadybird.h"
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/ImageDecoder.h>

namespace Ladybird {

ImageCodecPluginLadybird::~ImageCodecPluginLadybird() = default;

Optional<Web::Platform::DecodedImage> ImageCodecPluginLadybird::decode_image(ReadonlyBytes data)
{
    auto decoder = Gfx::ImageDecoder::try_create_for_raw_bytes(data);

    if (!decoder || !decoder->frame_count()) {
        return {};
    }

    bool had_errors = false;
    Vector<Web::Platform::Frame> frames;
    for (size_t i = 0; i < decoder->frame_count(); ++i) {
        auto frame_or_error = decoder->frame(i);
        if (frame_or_error.is_error()) {
            frames.append({ {}, 0 });
            had_errors = true;
        } else {
            auto frame = frame_or_error.release_value();
            frames.append({ move(frame.image), static_cast<size_t>(frame.duration) });
        }
    }

    if (had_errors)
        return {};

    return Web::Platform::DecodedImage {
        decoder->is_animated(),
        static_cast<u32>(decoder->loop_count()),
        move(frames),
    };
}

}
