/*
 * Copyright (c) 2023, Preston Taylor <PrestonLeeTaylor@proton.me>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Format.h>

namespace Gfx {

template<typename T>
class Range {
public:
    constexpr Range() = default;

    constexpr Range(T min, T max)
        : m_min(min)
        , m_max(max)
    {
    }

    template<typename U>
    constexpr Range(U min, U max)
        : m_min(min)
        , m_max(max)
    {
    }

    template<typename U>
    explicit constexpr Range(Range<U> const& other)
        : m_min(other.min())
        , m_max(other.max())
    {
    }

    [[nodiscard]] ALWAYS_INLINE constexpr T min() const { return m_min; }
    [[nodiscard]] ALWAYS_INLINE constexpr T max() const { return m_max; }

    [[nodiscard]] ALWAYS_INLINE constexpr T dist() const { return m_max - m_min; }

    ALWAYS_INLINE void set_min(T min) { m_min = min; }
    ALWAYS_INLINE void set_max(T max) { m_max = max; }

    ALWAYS_INLINE void translate_by(T const& delta)
    {
        m_min += delta;
        m_max += delta;
    }

    void intersect(Range<T> const& other)
    {
        T intersected_min = AK::max(this->min(), other.min());
        T intersected_max = AK::min(this->max(), other.max());

        if (intersected_min > intersected_max) {
            m_min = 0;
            m_max = 0;
            return;
        }

        set_min(intersected_min);
        set_max(intersected_max);
    }

    [[nodiscard]] Range<T> translated(T const& delta) const
    {
        Range<T> range = *this;
        range.translate_by(delta);
        return range;
    }

    template<typename U>
    requires(!IsSame<T, U>)
    [[nodiscard]] ALWAYS_INLINE Range<U> to_type() const
    {
        return Range<U>(*this);
    }

private:
    T m_min { 0 };
    T m_max { 0 };
};

using IntRange = Range<int>;

}

namespace AK {

template<typename T>
struct Formatter<Gfx::Range<T>> : Formatter<FormatString> {
    ErrorOr<void> format(FormatBuilder& builder, Gfx::Range<T> const& value)
    {
        return Formatter<FormatString>::format(builder, "[{}-{}]"sv, value.min(), value.max());
    }
};

}
