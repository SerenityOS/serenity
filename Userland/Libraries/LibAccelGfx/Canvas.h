/*
 * Copyright (c) 2023, Aliaksandr Kalenik <kalenik.aliaksandr@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/RefCounted.h>
#include <LibAccelGfx/GL.h>
#include <LibGfx/Rect.h>

namespace AccelGfx {

class Canvas : public RefCounted<Canvas> {
public:
    static NonnullRefPtr<Canvas> create(Gfx::IntSize const& size)
    {
        return adopt_ref(*new Canvas(size));
    }

    Gfx::IntSize const& size() const { return m_size; }
    GL::Framebuffer const& framebuffer() const { return m_framebuffer; }

    void bind()
    {
        GL::bind_framebuffer(m_framebuffer);
    }

    virtual ~Canvas()
    {
        GL::delete_framebuffer(m_framebuffer);
    }

private:
    Canvas(Gfx::IntSize const& size)
        : m_size(size)
        , m_framebuffer(GL::create_framebuffer(size))
    {
    }

    Gfx::IntSize m_size;
    GL::Framebuffer m_framebuffer;
};

}
