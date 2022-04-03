/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Forward.h>
#include <LibJS/Heap/Handle.h>
#include <LibWeb/Bindings/Wrappable.h>

namespace Web::HTML {

class ImageData
    : public RefCounted<ImageData>
    , public Bindings::Wrappable {
public:
    using WrapperType = Bindings::ImageDataWrapper;

    static RefPtr<ImageData> create_with_size(JS::GlobalObject&, int width, int height);

    ~ImageData();

    unsigned width() const;
    unsigned height() const;

    Gfx::Bitmap& bitmap() { return m_bitmap; }
    Gfx::Bitmap const& bitmap() const { return m_bitmap; }

    JS::Uint8ClampedArray* data();
    const JS::Uint8ClampedArray* data() const;

private:
    explicit ImageData(NonnullRefPtr<Gfx::Bitmap>, JS::Handle<JS::Uint8ClampedArray>);

    NonnullRefPtr<Gfx::Bitmap> m_bitmap;
    JS::Handle<JS::Uint8ClampedArray> m_data;
};

}
