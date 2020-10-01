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

#include <AK/SharedBuffer.h>
#include <LibImageDecoderClient/Client.h>

namespace ImageDecoderClient {

Client::Client()
    : IPC::ServerConnection<ImageDecoderClientEndpoint, ImageDecoderServerEndpoint>(*this, "/tmp/portal/image")
{
    handshake();
}

void Client::handshake()
{
    auto response = send_sync<Messages::ImageDecoderServer::Greet>(getpid());
    set_my_client_id(response->client_id());
    set_server_pid(response->server_pid());
}

void Client::handle(const Messages::ImageDecoderClient::Dummy&)
{
}

RefPtr<Gfx::Bitmap> Client::decode_image(const ByteBuffer& encoded_data)
{
    if (encoded_data.is_empty())
        return nullptr;

    auto encoded_buffer = SharedBuffer::create_with_size(encoded_data.size());
    if (!encoded_buffer) {
        dbg() << "Could not allocate encoded shbuf";
        return nullptr;
    }

    memcpy(encoded_buffer->data<void>(), encoded_data.data(), encoded_data.size());

    encoded_buffer->seal();
    encoded_buffer->share_with(server_pid());

    auto response = send_sync<Messages::ImageDecoderServer::DecodeImage>(encoded_buffer->shbuf_id(), encoded_data.size());
    auto bitmap_format = (Gfx::BitmapFormat)response->bitmap_format();
    if (bitmap_format == Gfx::BitmapFormat::Invalid) {
#ifdef IMAGE_DECODER_CLIENT_DEBUG
        dbg() << "Response image was invalid";
#endif
        return nullptr;
    }

    if (response->size().is_empty()) {
        dbg() << "Response image was empty";
        return nullptr;
    }

    auto decoded_buffer = SharedBuffer::create_from_shbuf_id(response->decoded_shbuf_id());
    if (!decoded_buffer) {
        dbg() << "Could not map decoded image shbuf_id=" << response->decoded_shbuf_id();
        return nullptr;
    }

    return Gfx::Bitmap::create_with_shared_buffer(bitmap_format, decoded_buffer.release_nonnull(), response->size(), response->palette());
}

}
