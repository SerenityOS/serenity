/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include <AK/Forward.h>
#include <LibGfx/Orientation.h>
#include <LibIPC/Forward.h>

namespace Gfx {

template<typename T>
class Size {
public:
    Size() { }

    Size(T w, T h)
        : m_width(w)
        , m_height(h)
    {
    }

    template<typename U>
    Size(U width, U height)
        : m_width(width)
        , m_height(height)
    {
    }

    template<typename U>
    explicit Size(const Size<U>& other)
        : m_width(other.width())
        , m_height(other.height())
    {
    }

    bool is_null() const { return !m_width && !m_height; }
    bool is_empty() const { return m_width <= 0 || m_height <= 0; }

    T width() const { return m_width; }
    T height() const { return m_height; }

    T area() const { return width() * height(); }

    void set_width(T w) { m_width = w; }
    void set_height(T h) { m_height = h; }

    bool operator==(const Size<T>& other) const
    {
        return m_width == other.m_width && m_height == other.m_height;
    }

    bool operator!=(const Size<T>& other) const
    {
        return !(*this == other);
    }

    Size<T>& operator-=(const Size<T>& other)
    {
        m_width -= other.m_width;
        m_height -= other.m_height;
        return *this;
    }

    Size<T>& operator+=(const Size<T>& other)
    {
        m_width += other.m_width;
        m_height += other.m_height;
        return *this;
    }

    T primary_size_for_orientation(Orientation orientation) const
    {
        return orientation == Orientation::Vertical ? height() : width();
    }

    void set_primary_size_for_orientation(Orientation orientation, T value)
    {
        if (orientation == Orientation::Vertical) {
            set_height(value);
        } else {
            set_width(value);
        }
    }

    T secondary_size_for_orientation(Orientation orientation) const
    {
        return orientation == Orientation::Vertical ? width() : height();
    }

    void set_secondary_size_for_orientation(Orientation orientation, T value)
    {
        if (orientation == Orientation::Vertical) {
            set_width(value);
        } else {
            set_height(value);
        }
    }

    template<typename U>
    Size<U> to_type() const
    {
        return Size<U>(*this);
    }

    String to_string() const;

private:
    T m_width { 0 };
    T m_height { 0 };
};

template<typename T>
const LogStream& operator<<(const LogStream& stream, const Gfx::Size<T>& size)
{
    return stream << size.to_string();
}

using IntSize = Size<int>;
using FloatSize = Size<float>;

}

namespace IPC {

bool encode(Encoder&, const Gfx::IntSize&);
bool decode(Decoder&, Gfx::IntSize&);

}
