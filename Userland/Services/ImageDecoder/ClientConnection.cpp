/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <YAK/Debug.h>
#include <ImageDecoder/ClientConnection.h>
#include <ImageDecoder/ImageDecoderClientEndpoint.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageDecoder.h>

namespace ImageDecoder {

static HashMap<int, RefPtr<ClientConnection>> s_connections;

ClientConnection::ClientConnection(NonnullRefPtr<Core::LocalSocket> socket, int client_id)
    : IPC::ClientConnection<ImageDecoderClientEndpoint, ImageDecoderServerEndpoint>(*this, move(socket), client_id)
{
    s_connections.set(client_id, *this);
}

ClientConnection::~ClientConnection()
{
}

void ClientConnection::die()
{
    s_connections.remove(client_id());
    exit(0);
}

Messages::ImageDecoderServer::DecodeImageResponse ClientConnection::decode_image(Core::AnonymousBuffer const& encoded_buffer)
{
    if (!encoded_buffer.is_valid()) {
        dbgln_if(IMAGE_DECODER_DEBUG, "Encoded data is invalid");
        return nullptr;
    }

    auto decoder = Gfx::ImageDecoder::try_create(ReadonlyBytes { encoded_buffer.data<u8>(), encoded_buffer.size() });

    if (!decoder) {
        dbgln_if(IMAGE_DECODER_DEBUG, "Could not find suitable image decoder plugin for data");
        return { false, 0, Vector<Gfx::ShareableBitmap> {}, Vector<u32> {} };
    }

    if (!decoder->frame_count()) {
        dbgln_if(IMAGE_DECODER_DEBUG, "Could not decode image from encoded data");
        return { false, 0, Vector<Gfx::ShareableBitmap> {}, Vector<u32> {} };
    }

    Vector<Gfx::ShareableBitmap> bitmaps;
    Vector<u32> durations;
    for (size_t i = 0; i < decoder->frame_count(); ++i) {
        auto frame = decoder->frame(i);
        if (frame.image)
            bitmaps.append(frame.image->to_shareable_bitmap());
        else
            bitmaps.append(Gfx::ShareableBitmap {});
        durations.append(frame.duration);
    }

    return { decoder->is_animated(), static_cast<u32>(decoder->loop_count()), bitmaps, durations };
}

}
