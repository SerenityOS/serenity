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

#include <AK/Function.h>
#include <LibGfx/Bitmap.h>
#include <LibImageDecoderClient/Client.h>
#include <LibWeb/Loader/ImageResource.h>

namespace Web {

ImageResource::ImageResource(const LoadRequest& request)
    : Resource(Type::Image, request)
{
}

ImageResource::~ImageResource()
{
}

int ImageResource::frame_duration(size_t frame_index) const
{
    decode_if_needed();
    if (frame_index >= m_decoded_frames.size())
        return 0;
    return m_decoded_frames[frame_index].duration;
}

void ImageResource::decode_if_needed() const
{
    if (!has_encoded_data())
        return;

    if (m_has_attempted_decode)
        return;

    if (!m_decoded_frames.is_empty())
        return;

    auto image_decoder_client = ImageDecoderClient::Client::construct();
    auto image = image_decoder_client->decode_image(encoded_data());
    if (image.has_value()) {
        m_loop_count = image.value().loop_count;
        m_animated = image.value().is_animated;
        m_decoded_frames.resize(image.value().frames.size());
        for (size_t i = 0; i < m_decoded_frames.size(); ++i) {
            auto& frame = m_decoded_frames[i];
            frame.bitmap = image.value().frames[i].bitmap;
            frame.duration = image.value().frames[i].duration;
        }
    }

    m_has_attempted_decode = true;
}

const Gfx::Bitmap* ImageResource::bitmap(size_t frame_index) const
{
    decode_if_needed();
    if (frame_index >= m_decoded_frames.size())
        return nullptr;
    return m_decoded_frames[frame_index].bitmap;
}

void ImageResource::update_volatility()
{
    bool visible_in_viewport = false;
    for_each_client([&](auto& client) {
        if (static_cast<const ImageResourceClient&>(client).is_visible_in_viewport())
            visible_in_viewport = true;
    });

    if (!visible_in_viewport) {
        for (auto& frame : m_decoded_frames) {
            if (frame.bitmap)
                frame.bitmap->set_volatile();
        }
        return;
    }

    bool still_has_decoded_image = true;
    for (auto& frame : m_decoded_frames) {
        if (!frame.bitmap) {
            still_has_decoded_image = false;
        } else {
            bool still_has_frame = frame.bitmap->set_nonvolatile();
            if (!still_has_frame)
                still_has_decoded_image = false;
        }
    }
    if (still_has_decoded_image)
        return;

    m_decoded_frames.clear();
    m_has_attempted_decode = false;
}

ImageResourceClient::~ImageResourceClient()
{
}

}
