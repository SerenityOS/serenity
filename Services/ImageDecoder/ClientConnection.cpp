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
#include <AK/SharedBuffer.h>
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
    auto encoded_buffer = SharedBuffer::create_from_shbuf_id(message.encoded_shbuf_id());
    if (!encoded_buffer) {
#ifdef IMAGE_DECODER_DEBUG
        dbg() << "Could not map encoded data buffer";
#endif
        return nullptr;
    }

    if (message.encoded_size() > (size_t)encoded_buffer->size()) {
#ifdef IMAGE_DECODER_DEBUG
        dbg() << "Encoded buffer is smaller than encoded size";
#endif
        return nullptr;
    }

#ifdef IMAGE_DECODER_DEBUG
    dbg() << "Trying to decode " << message.encoded_size() << " bytes of image(?) data in shbuf_id=" << message.encoded_shbuf_id() << " (shbuf size: " << encoded_buffer->size() << ")";
#endif

    auto decoder = Gfx::ImageDecoder::create(encoded_buffer->data<u8>(), message.encoded_size());
    auto bitmap = decoder->bitmap();

    if (!bitmap) {
#ifdef IMAGE_DECODER_DEBUG
        dbg() << "Could not decode image from encoded data";
#endif
        return make<Messages::ImageDecoderServer::DecodeImageResponse>(-1, Gfx::IntSize(), (i32)Gfx::BitmapFormat::Invalid, Vector<u32>());
    }

    // FIXME: We should fix ShareableBitmap so you can send it in responses as well as requests..
    m_shareable_bitmap = bitmap->to_bitmap_backed_by_shared_buffer();
    m_shareable_bitmap->shared_buffer()->share_with(client_pid());
    Vector<u32> palette;
    if (m_shareable_bitmap->is_indexed()) {
        palette = m_shareable_bitmap->palette_to_vector();
    }
    return make<Messages::ImageDecoderServer::DecodeImageResponse>(m_shareable_bitmap->shbuf_id(), m_shareable_bitmap->size(), (i32)m_shareable_bitmap->format(), palette);
}

}
