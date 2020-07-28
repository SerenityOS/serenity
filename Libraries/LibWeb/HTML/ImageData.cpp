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
#include <LibJS/Runtime/Uint8ClampedArray.h>
#include <LibWeb/HTML/ImageData.h>

namespace Web::HTML {

RefPtr<ImageData> ImageData::create_with_size(JS::GlobalObject& global_object, int width, int height)
{
    if (width <= 0 || height <= 0)
        return nullptr;

    if (width > 16384 || height > 16384)
        return nullptr;

    dbg() << "Creating ImageData with " << width << "x" << height;

    auto* data = JS::Uint8ClampedArray::create(global_object, width * height * 4);
    if (!data)
        return nullptr;

    auto data_handle = JS::make_handle(data);

    auto bitmap = Gfx::Bitmap::create_wrapper(Gfx::BitmapFormat::RGBA32, Gfx::IntSize(width, height), width * sizeof(u32), (u32*)data->data());
    if (!bitmap)
        return nullptr;
    return adopt(*new ImageData(bitmap.release_nonnull(), move(data_handle)));
}

ImageData::ImageData(NonnullRefPtr<Gfx::Bitmap> bitmap, JS::Handle<JS::Uint8ClampedArray> data)
    : m_bitmap(move(bitmap))
    , m_data(move(data))
{
}

ImageData::~ImageData()
{
}

unsigned ImageData::width() const
{
    return m_bitmap->width();
}

unsigned ImageData::height() const
{
    return m_bitmap->height();
}

JS::Uint8ClampedArray* ImageData::data()
{
    return m_data.cell();
}

const JS::Uint8ClampedArray* ImageData::data() const
{
    return m_data.cell();
}

}
