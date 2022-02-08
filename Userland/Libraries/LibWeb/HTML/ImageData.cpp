/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Bitmap.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibWeb/HTML/ImageData.h>

namespace Web::HTML {

RefPtr<ImageData> ImageData::create_with_size(JS::GlobalObject& global_object, int width, int height)
{
    if (width <= 0 || height <= 0)
        return nullptr;

    if (width > 16384 || height > 16384)
        return nullptr;

    dbgln("Creating ImageData with {}x{}", width, height);

    auto data_or_error = JS::Uint8ClampedArray::create(global_object, width * height * 4);
    if (data_or_error.is_error())
        return nullptr;
    auto* data = data_or_error.release_value();

    auto data_handle = JS::make_handle(data);

    auto bitmap_or_error = Gfx::Bitmap::try_create_wrapper(Gfx::BitmapFormat::RGBA8888, Gfx::IntSize(width, height), 1, width * sizeof(u32), data->data().data());
    if (bitmap_or_error.is_error())
        return nullptr;
    return adopt_ref(*new ImageData(bitmap_or_error.release_value(), move(data_handle)));
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
