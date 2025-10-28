/*
 * Copyright (c) 2025, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Vector.h>

namespace Gfx {

/// 5.2 - Mirroring
static u32 mirror_1d(i32 coord, u32 size)
{
    if (coord < 0)
        return mirror_1d(-coord - 1, size);
    else if (static_cast<u32>(coord) >= size)
        return mirror_1d(2 * size - 1 - coord, size);
    else
        return coord;
}
///

struct ChannelInfo {
    static ChannelInfo from_size(IntSize size)
    {
        return {
            .width = static_cast<u32>(size.width()),
            .height = static_cast<u32>(size.height()),
        };
    }
    u32 width {};
    u32 height {};
    i8 hshift {};
    i8 vshift {};
};

namespace Detail {

template<OneOf<i32, f32> T>
class Channel {
    AK_MAKE_NONCOPYABLE(Channel);
    AK_MAKE_DEFAULT_MOVABLE(Channel);

public:
    static ErrorOr<Channel> create(ChannelInfo const& info)
    {
        Channel channel;

        channel.m_width = info.width;
        channel.m_height = info.height;
        channel.m_hshift = info.hshift;
        channel.m_vshift = info.vshift;

        TRY(channel.m_pixels.try_resize(channel.m_width * channel.m_height));

        return channel;
    }

    ErrorOr<Channel> copy(Optional<IntSize> destination_size = {}) const
    {
        Channel other;

        if (destination_size.has_value()) {
            VERIFY(static_cast<u32>(destination_size->width()) >= m_width);
            VERIFY(static_cast<u32>(destination_size->height()) >= m_height);
            other.m_width = destination_size->width();
            other.m_height = destination_size->height();
        } else {
            other.m_width = m_width;
            other.m_height = m_height;
        }
        other.m_hshift = m_hshift;
        other.m_vshift = m_vshift;
        other.m_decoded = m_decoded;

        TRY(other.m_pixels.try_resize(other.m_width * other.m_height));
        for (u32 y {}; y < m_height; ++y) {
            for (u32 x {}; x < m_width; ++x)
                other.set(x, y, get(x, y));
        }

        return other;
    }

    template<OneOf<i32, f32> OtherT>
    requires(!SameAs<T, OtherT>)
    ErrorOr<Channel<OtherT>> as(u8 bits_per_sample) const
    {
        Channel<OtherT> other {};

        other.m_width = m_width;
        other.m_height = m_height;
        other.m_hshift = m_hshift;
        other.m_vshift = m_vshift;
        other.m_decoded = m_decoded;

        TRY(other.m_pixels.try_resize(other.m_width * other.m_height));
        for (u32 y {}; y < m_height; ++y) {
            for (u32 x {}; x < m_width; ++x) {
                if constexpr (IsSame<OtherT, f32>)
                    other.set(x, y, static_cast<f32>(get(x, y)) / ((1 << bits_per_sample) - 1));
                else
                    other.set(x, y, round(get(x, y) * ((1 << bits_per_sample) - 1)));
            }
        }

        return other;
    }

    T& get(u32 x, u32 y)
    {
        return m_pixels[y * m_width + x];
    }

    T get(u32 x, u32 y) const
    {
        return m_pixels[y * m_width + x];
    }

    T get_mirrored(i64 x, i64 y) const
    {
        x = mirror_1d(x, width());
        y = mirror_1d(y, height());
        return m_pixels[y * m_width + x];
    }

    void set(u32 x, u32 y, T value)
    {
        m_pixels[y * m_width + x] = value;
    }

    u32 width() const
    {
        return m_width;
    }

    u32 height() const
    {
        return m_height;
    }

    i8 hshift() const
    {
        return m_hshift;
    }

    i8 vshift() const
    {
        return m_vshift;
    }

    bool decoded() const
    {
        return m_decoded;
    }

    void set_decoded(bool decoded)
    {
        m_decoded = decoded;
    }

    void copy_from(IntRect destination, Channel const& other)
    {
        VERIFY(destination.left() >= 0);
        VERIFY(destination.top() >= 0);
        VERIFY(static_cast<u32>(destination.right()) <= m_width);
        VERIFY(static_cast<u32>(destination.bottom()) <= m_height);

        VERIFY(static_cast<u32>(destination.width()) == other.width());
        VERIFY(static_cast<u32>(destination.height()) == other.height());

        for (i32 y = 0; y < destination.height(); ++y) {
            for (i32 x = 0; x < destination.width(); ++x) {
                set(x + destination.left(), y + destination.top(), other.get(x, y));
            }
        }
    }

private:
    template<OneOf<i32, f32>>
    friend class Channel;

    Channel() = default;

    u32 m_width {};
    u32 m_height {};

    i8 m_hshift {};
    i8 m_vshift {};

    bool m_decoded { false };

    Vector<T> m_pixels {};
};

}

using Channel = Detail::Channel<i32>;
using FloatChannel = Detail::Channel<f32>;

}
