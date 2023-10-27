/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibAccelGfx/Context.h>
#include <LibAccelGfx/Forward.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Rect.h>

namespace AccelGfx {

class Canvas {
public:
    static Canvas create(Context& context, NonnullRefPtr<Gfx::Bitmap> bitmap);

    [[nodiscard]] Gfx::IntSize size() const { return m_bitmap->size(); }
    [[nodiscard]] int width() const { return m_bitmap->width(); }
    [[nodiscard]] int height() const { return m_bitmap->height(); }

    void flush();

    [[nodiscard]] Gfx::Bitmap const& bitmap() const { return *m_bitmap; }

    ~Canvas();

private:
    explicit Canvas(NonnullRefPtr<Gfx::Bitmap>, Context&);

    void initialize();

    NonnullRefPtr<Gfx::Bitmap> m_bitmap;

    Context& m_context;
    Context::Surface m_surface;
};

}
