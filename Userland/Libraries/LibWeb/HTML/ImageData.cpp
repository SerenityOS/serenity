/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Bitmap.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/ImageData.h>

namespace Web::HTML {

JS::GCPtr<ImageData> ImageData::create_with_size(JS::Realm& realm, int width, int height)
{

    if (width <= 0 || height <= 0)
        return nullptr;

    if (width > 16384 || height > 16384)
        return nullptr;

    auto data_or_error = JS::Uint8ClampedArray::create(realm, width * height * 4);
    if (data_or_error.is_error())
        return nullptr;
    auto data = JS::NonnullGCPtr<JS::Uint8ClampedArray>(*data_or_error.release_value());

    auto bitmap_or_error = Gfx::Bitmap::create_wrapper(Gfx::BitmapFormat::RGBA8888, Gfx::IntSize(width, height), 1, width * sizeof(u32), data->data().data());
    if (bitmap_or_error.is_error())
        return nullptr;
    return realm.heap().allocate<ImageData>(realm, realm, bitmap_or_error.release_value(), move(data));
}

ImageData::ImageData(JS::Realm& realm, NonnullRefPtr<Gfx::Bitmap> bitmap, JS::NonnullGCPtr<JS::Uint8ClampedArray> data)
    : PlatformObject(realm)
    , m_bitmap(move(bitmap))
    , m_data(move(data))
{
}

ImageData::~ImageData() = default;

void ImageData::initialize(JS::Realm& realm)
{
    Base::initialize(realm);
    set_prototype(&Bindings::ensure_web_prototype<Bindings::ImageDataPrototype>(realm, "ImageData"));
}

void ImageData::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_data.ptr());
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
    return m_data;
}

const JS::Uint8ClampedArray* ImageData::data() const
{
    return m_data;
}

}
