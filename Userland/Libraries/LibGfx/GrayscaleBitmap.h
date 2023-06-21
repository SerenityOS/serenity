/*
 * Copyright (c) 2023, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Size.h"
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/StringView.h>

namespace Gfx {

class GrayscaleBitmap {
public:
    GrayscaleBitmap() = delete;
    constexpr GrayscaleBitmap(ReadonlyBytes data, unsigned width, unsigned height)
        : m_data(data)
        , m_size(width, height)
    {
        VERIFY(width * height == data.size());
    }

    constexpr u8 pixel_at(unsigned x, unsigned y) const { return m_data[y * width() + x]; }
    constexpr ReadonlyBytes data() const { return m_data; }

    constexpr IntSize size() const { return m_size; }
    constexpr unsigned width() const { return m_size.width(); }
    constexpr unsigned height() const { return m_size.height(); }

private:
    ReadonlyBytes m_data {};
    IntSize m_size {};
};

}
