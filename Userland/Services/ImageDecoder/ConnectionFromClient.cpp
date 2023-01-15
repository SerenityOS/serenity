/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <ImageDecoder/ConnectionFromClient.h>
#include <ImageDecoder/ImageDecoderClientEndpoint.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageDecoder.h>

namespace ImageDecoder {

ConnectionFromClient::ConnectionFromClient(NonnullOwnPtr<Core::Stream::LocalSocket> socket)
    : IPC::ConnectionFromClient<ImageDecoderClientEndpoint, ImageDecoderServerEndpoint>(*this, move(socket), 1)
{
}

void ConnectionFromClient::die()
{
    Core::EventLoop::current().quit(0);
}

static void decode_image_to_bitmaps_and_durations_with_decoder(Gfx::ImageDecoder const& decoder, Vector<Gfx::ShareableBitmap>& bitmaps, Vector<u32>& durations)
{
    for (size_t i = 0; i < decoder.frame_count(); ++i) {
        auto frame_or_error = decoder.frame(i);
        if (frame_or_error.is_error()) {
            bitmaps.append(Gfx::ShareableBitmap {});
            durations.append(0);
        } else {
            auto frame = frame_or_error.release_value();
            bitmaps.append(frame.image->to_shareable_bitmap());
            durations.append(frame.duration);
        }
    }
}

static void decode_image_to_details(Core::AnonymousBuffer const& encoded_buffer, Optional<StringView> known_path, bool& is_animated, u32& loop_count, Vector<Gfx::ShareableBitmap>& bitmaps, Vector<u32>& durations)
{
    VERIFY(bitmaps.size() == 0);
    VERIFY(durations.size() == 0);
    VERIFY(!is_animated);
    VERIFY(loop_count == 0);

    RefPtr<Gfx::ImageDecoder> decoder;
    if (known_path.has_value())
        decoder = Gfx::ImageDecoder::try_create_for_raw_bytes_with_known_path(known_path.value(), ReadonlyBytes { encoded_buffer.data<u8>(), encoded_buffer.size() });
    else
        decoder = Gfx::ImageDecoder::try_create_for_raw_bytes(ReadonlyBytes { encoded_buffer.data<u8>(), encoded_buffer.size() });

    if (!decoder) {
        dbgln_if(IMAGE_DECODER_DEBUG, "Could not find suitable image decoder plugin for data");
        return;
    }

    if (!decoder->frame_count()) {
        dbgln_if(IMAGE_DECODER_DEBUG, "Could not decode image from encoded data");
        return;
    }
    decode_image_to_bitmaps_and_durations_with_decoder(*decoder, bitmaps, durations);
}

Messages::ImageDecoderServer::DecodeImageWithKnownPathResponse ConnectionFromClient::decode_image_with_known_path(DeprecatedString const& path, Core::AnonymousBuffer const& encoded_buffer)
{
    if (!encoded_buffer.is_valid()) {
        dbgln_if(IMAGE_DECODER_DEBUG, "Encoded data is invalid");
        return nullptr;
    }

    bool is_animated = false;
    u32 loop_count = 0;
    Vector<Gfx::ShareableBitmap> bitmaps;
    Vector<u32> durations;
    decode_image_to_details(encoded_buffer, path.substring_view(0), is_animated, loop_count, bitmaps, durations);
    return { is_animated, loop_count, bitmaps, durations };
}

Messages::ImageDecoderServer::DecodeImageResponse ConnectionFromClient::decode_image(Core::AnonymousBuffer const& encoded_buffer)
{
    if (!encoded_buffer.is_valid()) {
        dbgln_if(IMAGE_DECODER_DEBUG, "Encoded data is invalid");
        return nullptr;
    }

    bool is_animated = false;
    u32 loop_count = 0;
    Vector<Gfx::ShareableBitmap> bitmaps;
    Vector<u32> durations;
    decode_image_to_details(encoded_buffer, {}, is_animated, loop_count, bitmaps, durations);
    return { is_animated, loop_count, bitmaps, durations };
}

}
