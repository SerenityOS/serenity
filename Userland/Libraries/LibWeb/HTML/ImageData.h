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
    const Gfx::Bitmap& bitmap() const { return m_bitmap; }

    JS::Uint8ClampedArray* data();
    const JS::Uint8ClampedArray* data() const;

private:
    explicit ImageData(NonnullRefPtr<Gfx::Bitmap>, JS::Handle<JS::Uint8ClampedArray>);

    NonnullRefPtr<Gfx::Bitmap> m_bitmap;
    JS::Handle<JS::Uint8ClampedArray> m_data;
};

}
