/*
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Size.h>
#include <LibSoftGPU/Buffer/Typed2DBuffer.h>

namespace SoftGPU {

/*
 * The frame buffer is a 2D buffer that consists of:
 * - color buffer(s); (FIXME: implement multiple color buffers)
 * - depth buffer;
 * - stencil buffer;
 * - accumulation buffer. (FIXME: implement accumulation buffer)
 */
template<typename C, typename D, typename S>
class FrameBuffer final : public RefCounted<FrameBuffer<C, D, S>> {
public:
    static ErrorOr<NonnullRefPtr<FrameBuffer<C, D, S>>> try_create(Gfx::IntSize size)
    {
        Gfx::IntRect rect = { 0, 0, size.width(), size.height() };
        auto color_buffer = TRY(Typed2DBuffer<C>::try_create(size));
        auto depth_buffer = TRY(Typed2DBuffer<D>::try_create(size));
        auto stencil_buffer = TRY(Typed2DBuffer<S>::try_create(size));
        return adopt_ref(*new FrameBuffer(rect, color_buffer, depth_buffer, stencil_buffer));
    }

    NonnullRefPtr<Typed2DBuffer<C>> color_buffer() { return m_color_buffer; }
    NonnullRefPtr<Typed2DBuffer<D>> depth_buffer() { return m_depth_buffer; }
    NonnullRefPtr<Typed2DBuffer<S>> stencil_buffer() { return m_stencil_buffer; }
    Gfx::IntRect rect() const { return m_rect; }

private:
    FrameBuffer(Gfx::IntRect rect, NonnullRefPtr<Typed2DBuffer<C>> color_buffer, NonnullRefPtr<Typed2DBuffer<D>> depth_buffer, NonnullRefPtr<Typed2DBuffer<S>> stencil_buffer)
        : m_color_buffer(color_buffer)
        , m_depth_buffer(depth_buffer)
        , m_stencil_buffer(stencil_buffer)
        , m_rect(rect)
    {
    }

    NonnullRefPtr<Typed2DBuffer<C>> m_color_buffer;
    NonnullRefPtr<Typed2DBuffer<D>> m_depth_buffer;
    NonnullRefPtr<Typed2DBuffer<S>> m_stencil_buffer;
    Gfx::IntRect m_rect;
};

}
