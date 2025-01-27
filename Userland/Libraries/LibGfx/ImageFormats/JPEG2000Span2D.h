/*
 * Copyright (c) 2025, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Span.h>
#include <LibGfx/Size.h>

namespace Gfx::JPEG2000 {

template<class T>
struct Span2D {
    Span<T> data;
    IntSize size;
    int pitch { 0 };

    Span2D() = default;

    Span2D(Span<T> data, IntSize size, int pitch)
        : data(data)
        , size(size)
        , pitch(pitch)
    {
        VERIFY(size.is_empty() || static_cast<int>(data.size()) >= (size.height() - 1) * pitch + size.width());
    }

    [[nodiscard]] ALWAYS_INLINE constexpr Span<T> scanline(int y) const
    {
        return data.slice(y * pitch, size.width());
    }

    [[nodiscard]] ALWAYS_INLINE constexpr T const& operator[](size_t index) const
    {
        return data[index];
    }

    [[nodiscard]] ALWAYS_INLINE constexpr int width() const
    {
        return size.width();
    }

    [[nodiscard]] ALWAYS_INLINE constexpr int height() const
    {
        return size.height();
    }
};

}
