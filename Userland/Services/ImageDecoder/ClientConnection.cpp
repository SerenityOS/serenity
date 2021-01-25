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

OwnPtr<Messages::ImageDecoderServer::GreetResponse> ClientConnection::handle(const Messages::ImageDecoderServer::Greet& message)
{
    set_client_pid(message.client_pid());
    return make<Messages::ImageDecoderServer::GreetResponse>(client_id(), getpid());
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
    auto bitmap = decoder->bitmap();

    if (!bitmap) {
#if IMAGE_DECODER_DEBUG
        dbgln("Could not decode image from encoded data");
#endif
        return make<Messages::ImageDecoderServer::DecodeImageResponse>(Gfx::ShareableBitmap());
    }

    return make<Messages::ImageDecoderServer::DecodeImageResponse>(bitmap->to_shareable_bitmap());
}

}
