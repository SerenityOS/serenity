/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Badge.h>
#include <ImageDecoder/ClientConnection.h>
#include <ImageDecoder/ImageDecoderClientEndpoint.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageDecoder.h>
#include <LibGfx/SystemTheme.h>

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

OwnPtr<Messages::ImageDecoderServer::GreetResponse> ClientConnection::handle(const Messages::ImageDecoderServer::Greet&)
{
    return make<Messages::ImageDecoderServer::GreetResponse>();
}

OwnPtr<Messages::ImageDecoderServer::DecodeImageResponse> ClientConnection::handle(const Messages::ImageDecoderServer::DecodeImage& message)
{
    auto encoded_buffer = message.data();
    if (!encoded_buffer.is_valid()) {
#if IMAGE_DECODER_DEBUG
        dbgln("Encoded data is invalid");
#endif
        return {};
    }

    auto decoder = Gfx::ImageDecoder::create(encoded_buffer.data<u8>(), encoded_buffer.size());

    if (!decoder->frame_count()) {
#if IMAGE_DECODER_DEBUG
        dbgln("Could not decode image from encoded data");
#endif
        return make<Messages::ImageDecoderServer::DecodeImageResponse>(false, 0, Vector<Gfx::ShareableBitmap> {}, Vector<u32> {});
    }

    Vector<Gfx::ShareableBitmap> bitmaps;
    Vector<u32> durations;
    for (size_t i = 0; i < decoder->frame_count(); ++i) {
        // FIXME: All image decoder plugins should be rewritten to return frame() instead of bitmap().
        //        Non-animated images can simply return 1 frame.
        Gfx::ImageFrameDescriptor frame;
        if (decoder->is_animated()) {
            frame = decoder->frame(i);
        } else {
            frame.image = decoder->bitmap();
        }
        if (frame.image)
            bitmaps.append(frame.image->to_shareable_bitmap());
        else
            bitmaps.append(Gfx::ShareableBitmap {});
        durations.append(frame.duration);
    }

    return make<Messages::ImageDecoderServer::DecodeImageResponse>(decoder->is_animated(), decoder->loop_count(), bitmaps, durations);
}

}
