/*
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ImageCodecPlugin.h"
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/ImageDecoder.h>

namespace Ladybird {

ImageCodecPlugin::~ImageCodecPlugin() = default;

Optional<Web::Platform::DecodedImage> ImageCodecPlugin::decode_image(ReadonlyBytes data)
{
    auto decoder = Gfx::ImageDecoder::try_create_for_raw_bytes(data);

    if (!decoder || !decoder->frame_count()) {
        return {};
    }

    Vector<Web::Platform::Frame> frames;
    for (size_t i = 0; i < decoder->frame_count(); ++i) {
        auto frame_or_error = decoder->frame(i);
        if (frame_or_error.is_error())
            return {};
        auto frame = frame_or_error.release_value();
        frames.append({ move(frame.image), static_cast<size_t>(frame.duration) });
    }

    return Web::Platform::DecodedImage {
        decoder->is_animated(),
        static_cast<u32>(decoder->loop_count()),
        move(frames),
    };
}

}
