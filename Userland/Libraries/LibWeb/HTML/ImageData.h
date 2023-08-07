/*
 * Copyright (c) 2020-2022, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGfx/Forward.h>
#include <LibWeb/Bindings/PlatformObject.h>

namespace Web::HTML {

class ImageData final : public Bindings::PlatformObject {
    WEB_PLATFORM_OBJECT(ImageData, Bindings::PlatformObject);

public:
    static JS::GCPtr<ImageData> create_with_size(JS::Realm&, int width, int height);

    virtual ~ImageData() override;

    unsigned width() const;
    unsigned height() const;

    Gfx::Bitmap& bitmap() { return m_bitmap; }
    Gfx::Bitmap const& bitmap() const { return m_bitmap; }

    JS::Uint8ClampedArray* data();
    const JS::Uint8ClampedArray* data() const;

private:
    ImageData(JS::Realm&, NonnullRefPtr<Gfx::Bitmap>, JS::NonnullGCPtr<JS::Uint8ClampedArray>);

    virtual void initialize(JS::Realm&) override;
    virtual void visit_edges(Cell::Visitor&) override;

    NonnullRefPtr<Gfx::Bitmap> m_bitmap;
    JS::NonnullGCPtr<JS::Uint8ClampedArray> m_data;
};

}
