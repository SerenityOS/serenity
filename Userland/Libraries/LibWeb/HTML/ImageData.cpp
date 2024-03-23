/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2024, Kenneth Myhra <kennethmyhra@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Bitmap.h>
#include <LibJS/Runtime/TypedArray.h>
#include <LibWeb/Bindings/Intrinsics.h>
#include <LibWeb/HTML/ImageData.h>
#include <LibWeb/WebIDL/DOMException.h>
#include <LibWeb/WebIDL/ExceptionOr.h>

namespace Web::HTML {

JS_DEFINE_ALLOCATOR(ImageData);

WebIDL::ExceptionOr<JS::NonnullGCPtr<ImageData>> ImageData::create(JS::Realm& realm, u32 sw, u32 sh, Optional<ImageDataSettings> const&)
{
    auto& vm = realm.vm();

    // 1. If one or both of sw and sh are zero, then throw an "IndexSizeError" DOMException.
    if (sw == 0 || sh == 0)
        return WebIDL::IndexSizeError::create(realm, "The source width and height must be greater than zero."_fly_string);

    // 2. Initialize this given sw, sh, and settings set to settings.
    // 3. Initialize the image data of this to transparent black.
    auto data = TRY(JS::Uint8ClampedArray::create(realm, sw * sh * 4));
    auto bitmap = TRY_OR_THROW_OOM(vm, Gfx::Bitmap::create_wrapper(Gfx::BitmapFormat::RGBA8888, Gfx::IntSize(sw, sw), 1, sw * sizeof(u32), data->data().data()));

    return realm.heap().allocate<ImageData>(realm, realm, bitmap, data);
}

WebIDL::ExceptionOr<JS::NonnullGCPtr<ImageData>> ImageData::construct_impl(JS::Realm& realm, u32 sw, u32 sh, Optional<ImageDataSettings> const& settings)
{
    return ImageData::create(realm, sw, sh, settings);
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
    WEB_SET_PROTOTYPE_FOR_INTERFACE(ImageData);
}

void ImageData::visit_edges(Cell::Visitor& visitor)
{
    Base::visit_edges(visitor);
    visitor.visit(m_data);
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
