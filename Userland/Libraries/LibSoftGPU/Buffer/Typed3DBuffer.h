/*
 * Copyright (c) 2022, Jelle Raaijmakers <jelle@gmta.nl>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/FixedArray.h>
#include <AK/NonnullRefPtr.h>
#include <AK/RefCounted.h>
#include <AK/Try.h>

namespace SoftGPU {

/**
 * TypedBuffer<T> is a generic 3D buffer that can be used to store
 * values of a specific type at X, Y and Z coordinates. It is used as
 * storage for images, and frame, depth and stencil buffers.
 */
template<typename T>
class Typed3DBuffer final : public RefCounted<Typed3DBuffer<T>> {
public:
    static ErrorOr<NonnullRefPtr<Typed3DBuffer<T>>> try_create(int width, int height, int depth)
    {
        VERIFY(width > 0 && height > 0 && depth > 0);
        auto data = TRY(FixedArray<T>::create(width * height * depth));
        return adopt_ref(*new Typed3DBuffer(width, height, depth, move(data)));
    }

    ALWAYS_INLINE T* buffer_pointer(int x, int y, int z)
    {
        return &m_data[z * m_width * m_height + y * m_width + x];
    }

    ALWAYS_INLINE T const* buffer_pointer(int x, int y, int z) const
    {
        return &m_data[z * m_width * m_height + y * m_width + x];
    }

    void fill(T value, int x1, int x2, int y1, int y2, int z1, int z2)
    {
        for (auto z = z1; z < z2; ++z) {
            for (auto y = y1; y < y2; ++y) {
                auto* xline = buffer_pointer(0, y, z);
                for (auto x = x1; x < x2; ++x)
                    xline[x] = value;
            }
        }
    }

    int depth() const { return m_depth; }
    int height() const { return m_height; }
    int width() const { return m_width; }

private:
    Typed3DBuffer(int width, int height, int depth, FixedArray<T> data)
        : m_data(move(data))
        , m_depth(depth)
        , m_height(height)
        , m_width(width)
    {
    }

    FixedArray<T> m_data;
    int m_depth;
    int m_height;
    int m_width;
};

}
