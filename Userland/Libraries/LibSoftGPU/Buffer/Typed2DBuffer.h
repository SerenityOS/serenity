/*
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/Try.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Rect.h>
#include <LibGfx/Size.h>
#include <LibSoftGPU/Buffer/Typed3DBuffer.h>

namespace SoftGPU {

/**
 * Typed2DBuffer<T> wraps Typed3DBuffer<T> and only interacts on the 2D plane with z = 0.
 */
template<typename T>
class Typed2DBuffer final : public RefCounted<Typed2DBuffer<T>> {
public:
    static ErrorOr<NonnullRefPtr<Typed2DBuffer>> try_create(Gfx::IntSize size)
    {
        auto buffer = TRY(Typed3DBuffer<T>::try_create(size.width(), size.height(), 1));
        return adopt_ref(*new Typed2DBuffer(buffer));
    }

    void fill(T value, Gfx::IntRect const& rect) { m_buffer->fill(value, rect.left(), rect.right(), rect.top(), rect.bottom(), 0, 1); }
    ALWAYS_INLINE T* scanline(int y) { return m_buffer->buffer_pointer(0, y, 0); }
    ALWAYS_INLINE T const* scanline(int y) const { return m_buffer->buffer_pointer(0, y, 0); }

    void blit_flipped_to_bitmap(Gfx::Bitmap& bitmap, Gfx::IntRect const& target) const
    requires IsSame<T, u32>
    {
        VERIFY(bitmap.format() == Gfx::BitmapFormat::BGRA8888 || bitmap.format() == Gfx::BitmapFormat::BGRx8888);
        int source_y = 0;

        // NOTE: we are flipping the Y-coordinate here, which is OpenGL-specific: (0, 0) is considered the lower-left corner of the window
        for (int y = target.bottom() - 1; y >= target.top(); --y) {
            auto const* buffer_scanline = scanline(source_y++);
            auto* bitmap_scanline = bitmap.scanline(y);
            memcpy(bitmap_scanline + target.left(), buffer_scanline, sizeof(u32) * target.width());
        }
    }

private:
    Typed2DBuffer(NonnullRefPtr<Typed3DBuffer<T>> buffer)
        : m_buffer(buffer)
    {
    }

    NonnullRefPtr<Typed3DBuffer<T>> m_buffer;
};

}
