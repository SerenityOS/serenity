/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>
#include <LibGfx/Orientation.h>
#include <LibGfx/Point.h>
#include <LibIPC/Forward.h>

namespace Gfx {

template<typename T>
class Size {
public:
    constexpr Size() = default;

    constexpr Size(T w, T h)
        : m_width(w)
        , m_height(h)
    {
    }

    template<typename U>
    constexpr Size(U width, U height)
        : m_width(width)
        , m_height(height)
    {
    }

    template<typename U>
    explicit constexpr Size(Size<U> const& other)
        : m_width(other.width())
        , m_height(other.height())
    {
    }

    [[nodiscard]] ALWAYS_INLINE constexpr T width() const { return m_width; }
    [[nodiscard]] ALWAYS_INLINE constexpr T height() const { return m_height; }
    [[nodiscard]] ALWAYS_INLINE constexpr T area() const { return width() * height(); }

    ALWAYS_INLINE constexpr void set_width(T w) { m_width = w; }
    ALWAYS_INLINE constexpr void set_height(T h) { m_height = h; }

    [[nodiscard]] ALWAYS_INLINE constexpr bool is_null() const { return !m_width && !m_height; }
    [[nodiscard]] ALWAYS_INLINE constexpr bool is_empty() const { return m_width <= 0 || m_height <= 0; }

    constexpr void scale_by(T dx, T dy)
    {
        m_width *= dx;
        m_height *= dy;
    }

    constexpr void transform_by(AffineTransform const& transform) { *this = transform.map(*this); }

    ALWAYS_INLINE constexpr void scale_by(T dboth) { scale_by(dboth, dboth); }
    ALWAYS_INLINE constexpr void scale_by(Point<T> const& s) { scale_by(s.x(), s.y()); }

    [[nodiscard]] constexpr Size scaled_by(T dx, T dy) const
    {
        Size<T> size = *this;
        size.scale_by(dx, dy);
        return size;
    }

    [[nodiscard]] constexpr Size scaled_by(T dboth) const
    {
        Size<T> size = *this;
        size.scale_by(dboth);
        return size;
    }

    [[nodiscard]] constexpr Size scaled_by(Point<T> const& s) const
    {
        Size<T> size = *this;
        size.scale_by(s);
        return size;
    }

    [[nodiscard]] constexpr Size transformed_by(AffineTransform const& transform) const
    {
        Size<T> size = *this;
        size.transform_by(transform);
        return size;
    }

    template<typename U>
    [[nodiscard]] constexpr bool contains(Size<U> const& other) const
    {
        return other.m_width <= m_width && other.m_height <= m_height;
    }

    template<class U>
    [[nodiscard]] constexpr bool operator==(Size<U> const& other) const
    {
        return width() == other.width() && height() == other.height();
    }

    template<class U>
    [[nodiscard]] constexpr bool operator!=(Size<U> const& other) const
    {
        return !(*this == other);
    }

    constexpr Size<T>& operator-=(Size<T> const& other)
    {
        m_width -= other.m_width;
        m_height -= other.m_height;
        return *this;
    }

    Size<T>& operator+=(Size<T> const& other)
    {
        m_width += other.m_width;
        m_height += other.m_height;
        return *this;
    }

    [[nodiscard]] constexpr Size<T> operator*(T factor) const { return { m_width * factor, m_height * factor }; }

    constexpr Size<T>& operator*=(T factor)
    {
        m_width *= factor;
        m_height *= factor;
        return *this;
    }

    [[nodiscard]] constexpr T primary_size_for_orientation(Orientation orientation) const
    {
        return orientation == Orientation::Vertical ? height() : width();
    }

    constexpr void set_primary_size_for_orientation(Orientation orientation, T value)
    {
        if (orientation == Orientation::Vertical) {
            set_height(value);
        } else {
            set_width(value);
        }
    }

    [[nodiscard]] constexpr T secondary_size_for_orientation(Orientation orientation) const
    {
        return orientation == Orientation::Vertical ? width() : height();
    }

    constexpr void set_secondary_size_for_orientation(Orientation orientation, T value)
    {
        if (orientation == Orientation::Vertical) {
            set_width(value);
        } else {
            set_height(value);
        }
    }

    template<typename U>
    [[nodiscard]] ALWAYS_INLINE constexpr Size<U> to_type() const
    {
        return Size<U>(*this);
    }

    [[nodiscard]] String to_string() const;

private:
    T m_width { 0 };
    T m_height { 0 };
};

using IntSize = Size<int>;
using FloatSize = Size<float>;

}

namespace AK {

template<typename T>
struct Formatter<Gfx::Size<T>> : Formatter<StringView> {
    ErrorOr<void> format(FormatBuilder& builder, Gfx::Size<T> const& value)
    {
        return Formatter<StringView>::format(builder, value.to_string());
    }
};

}

namespace IPC {

bool encode(Encoder&, Gfx::IntSize const&);
ErrorOr<void> decode(Decoder&, Gfx::IntSize&);

}
