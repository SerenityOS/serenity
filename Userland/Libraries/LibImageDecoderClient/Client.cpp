/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
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

#include <LibCore/AnonymousBuffer.h>
#include <LibImageDecoderClient/Client.h>

namespace ImageDecoderClient {

Client::Client()
    : IPC::ServerConnection<ImageDecoderClientEndpoint, ImageDecoderServerEndpoint>(*this, "/tmp/portal/image")
{
    handshake();
}

void Client::die()
{
    if (on_death)
        on_death();
}

void Client::handshake()
{
    send_sync<Messages::ImageDecoderServer::Greet>();
}

void Client::handle(const Messages::ImageDecoderClient::Dummy&)
{
}

Optional<DecodedImage> Client::decode_image(const ByteBuffer& encoded_data)
{
    if (encoded_data.is_empty())
        return {};

    auto encoded_buffer = Core::AnonymousBuffer::create_with_size(encoded_data.size());
    if (!encoded_buffer.is_valid()) {
        dbgln("Could not allocate encoded buffer");
        return {};
    }

    memcpy(encoded_buffer.data<void>(), encoded_data.data(), encoded_data.size());
    auto response = send_sync_but_allow_failure<Messages::ImageDecoderServer::DecodeImage>(move(encoded_buffer));

    if (!response) {
        dbgln("ImageDecoder died heroically");
        return {};
    }

    DecodedImage image;
    image.is_animated = response->is_animated();
    image.loop_count = response->loop_count();
    image.frames.resize(response->bitmaps().size());
    for (size_t i = 0; i < image.frames.size(); ++i) {
        auto& frame = image.frames[i];
        frame.bitmap = response->bitmaps()[i].bitmap();
        frame.duration = response->durations()[i];
    }
    return move(image);
}

}
