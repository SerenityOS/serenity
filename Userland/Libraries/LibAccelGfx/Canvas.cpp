/*
 * Copyright (c) 2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Canvas.h"
#include <GL/gl.h>
#include <LibGfx/Bitmap.h>

namespace AccelGfx {

Canvas Canvas::create(Context& context, NonnullRefPtr<Gfx::Bitmap> bitmap)
{
    VERIFY(bitmap->format() == Gfx::BitmapFormat::BGRA8888);
    Canvas canvas { move(bitmap), context };
    canvas.initialize();
    return canvas;
}

Canvas::Canvas(NonnullRefPtr<Gfx::Bitmap> bitmap, Context& context)
    : m_bitmap(move(bitmap))
    , m_context(context)
{
}

void Canvas::initialize()
{
    m_surface = m_context.create_surface(width(), height());
    m_context.set_active_surface(m_surface);
    glViewport(0, 0, width(), height());
}

void Canvas::flush()
{
    glPixelStorei(GL_PACK_ALIGNMENT, 1);
    glReadPixels(0, 0, width(), height(), GL_BGRA, GL_UNSIGNED_BYTE, m_bitmap->scanline(0));
}

Canvas::~Canvas()
{
    m_context.destroy_surface(m_surface);
}

}
