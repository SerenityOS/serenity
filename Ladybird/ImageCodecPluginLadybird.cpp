/*
 * Copyright (c) 2022, Dex♪ <dexes.ttp@gmail.com>
 * Copyright (c) 2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ImageCodecPluginLadybird.h"
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/ImageDecoder.h>
#include <QImage>

namespace Ladybird {

ImageCodecPluginLadybird::~ImageCodecPluginLadybird() = default;

static Optional<Web::Platform::DecodedImage> decode_image_with_qt(ReadonlyBytes data)
{
    auto image = QImage::fromData(data.data(), static_cast<int>(data.size()));
    if (image.isNull())
        return {};
    image = image.convertToFormat(QImage::Format::Format_ARGB32);
    auto bitmap = MUST(Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, Gfx::IntSize(image.width(), image.height())));
    for (int y = 0; y < image.height(); ++y) {
        memcpy(bitmap->scanline_u8(y), image.scanLine(y), image.width() * 4);
    }
    Vector<Web::Platform::Frame> frames;

    frames.append(Web::Platform::Frame {
        bitmap,
    });
    return Web::Platform::DecodedImage {
        false,
        0,
        move(frames),
    };
}

static Optional<Web::Platform::DecodedImage> decode_image_with_libgfx(ReadonlyBytes data)
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

Optional<Web::Platform::DecodedImage> ImageCodecPluginLadybird::decode_image(ReadonlyBytes data)
{
    auto image = decode_image_with_libgfx(data);
    if (image.has_value())
        return image;
    return decode_image_with_qt(data);
}

}
