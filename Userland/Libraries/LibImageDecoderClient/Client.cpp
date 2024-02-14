/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibCore/AnonymousBuffer.h>
#include <LibImageDecoderClient/Client.h>

namespace ImageDecoderClient {

Client::Client(NonnullOwnPtr<Core::LocalSocket> socket)
    : IPC::ConnectionToServer<ImageDecoderClientEndpoint, ImageDecoderServerEndpoint>(*this, move(socket))
{
}

void Client::die()
{
    if (on_death)
        on_death();
}

Optional<DecodedImage> Client::decode_image(ReadonlyBytes encoded_data, Optional<Gfx::IntSize> ideal_size, Optional<ByteString> mime_type)
{
    if (encoded_data.is_empty())
        return {};

    auto encoded_buffer_or_error = Core::AnonymousBuffer::create_with_size(encoded_data.size());
    if (encoded_buffer_or_error.is_error()) {
        dbgln("Could not allocate encoded buffer");
        return {};
    }
    auto encoded_buffer = encoded_buffer_or_error.release_value();

    memcpy(encoded_buffer.data<void>(), encoded_data.data(), encoded_data.size());
    auto response_or_error = try_decode_image(move(encoded_buffer), ideal_size, mime_type);

    if (response_or_error.is_error()) {
        dbgln("ImageDecoder died heroically");
        return {};
    }

    auto& response = response_or_error.value();

    if (response.bitmaps().is_empty())
        return {};

    DecodedImage image;
    image.is_animated = response.is_animated();
    image.loop_count = response.loop_count();
    image.scale = response.scale();
    image.frames.ensure_capacity(response.bitmaps().size());
    auto bitmaps = response.take_bitmaps();
    for (size_t i = 0; i < bitmaps.size(); ++i) {
        if (!bitmaps[i].is_valid())
            return {};

        image.frames.empend(*bitmaps[i].bitmap(), response.durations()[i]);
    }
    return image;
}

}
