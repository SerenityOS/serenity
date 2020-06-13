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

#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageDecoder.h>
#include <LibWeb/Loader/ImageLoader.h>
#include <LibWeb/Loader/ResourceLoader.h>

namespace Web {

ImageLoader::ImageLoader()
{
}

void ImageLoader::load(const URL& url)
{
    LoadRequest request;
    request.set_url(url);
    set_resource(ResourceLoader::the().load_resource(Resource::Type::Image, request));
}

void ImageLoader::set_visible_in_viewport(bool visible_in_viewport)
{
    if (m_visible_in_viewport == visible_in_viewport)
        return;
    m_visible_in_viewport = visible_in_viewport;

    // FIXME: Don't update volatility every time. If we're here, we're probably scanning through
    //        the whole document, updating "is visible in viewport" flags, and this could lead
    //        to the same bitmap being marked volatile back and forth unnecessarily.
    if (resource())
        resource()->update_volatility();
}

void ImageLoader::resource_did_load()
{
    ASSERT(resource());

    if (!resource()->mime_type().starts_with("image/")) {
        if (on_fail)
            on_fail();
        return;
    }

    if (!resource()->has_encoded_data()) {
        dbg() << "ImageLoader: Resource did load, no encoded data. URL: " << resource()->url();
    } else {
        dbg() << "ImageLoader: Resource did load, has encoded data. URL: " << resource()->url();
    }
    m_decoder = resource()->ensure_decoder();

    if (on_load)
        on_load();
}

void ImageLoader::resource_did_fail()
{
    dbg() << "ImageLoader: Resource did fail. URL: " << resource()->url();
    if (on_fail)
        on_fail();
}

void ImageLoader::resource_did_replace_decoder()
{
    m_decoder = resource()->ensure_decoder();
}

const Gfx::Bitmap* ImageLoader::bitmap() const
{
    return m_decoder ? m_decoder->bitmap() : nullptr;
}

const Gfx::ImageDecoder* ImageLoader::image_decoder() const
{
    return m_decoder;
}

}
