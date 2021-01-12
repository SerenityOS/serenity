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

#pragma once

#include <LibWeb/Loader/Resource.h>

namespace Web {

class ImageResource final : public Resource {
    friend class Resource;

public:
    virtual ~ImageResource() override;
    Gfx::ImageDecoder& ensure_decoder();
    const Gfx::Bitmap* bitmap(size_t frame_index = 0) const;

    bool should_decode_in_process() const;

    void update_volatility();

private:
    explicit ImageResource(const LoadRequest&);
    RefPtr<Gfx::ImageDecoder> m_decoder;
    mutable RefPtr<Gfx::Bitmap> m_decoded_image;
    mutable bool m_has_attempted_decode { false };
};

class ImageResourceClient : public ResourceClient {
public:
    virtual ~ImageResourceClient();

    virtual bool is_visible_in_viewport() const { return false; }

protected:
    ImageResource* resource() { return static_cast<ImageResource*>(ResourceClient::resource()); }
    const ImageResource* resource() const { return static_cast<const ImageResource*>(ResourceClient::resource()); }

private:
    virtual Resource::Type client_type() const override { return Resource::Type::Image; }
};

}
