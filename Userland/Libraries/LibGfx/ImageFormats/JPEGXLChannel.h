/*
 * Copyright (c) 2025, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Vector.h>

namespace Gfx {

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

    i32 get(u32 x, u32 y) const
    {
        return m_pixels[y * m_width + x];
    }

    void set(u32 x, u32 y, i32 value)
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
    Channel() = default;

    u32 m_width {};
    u32 m_height {};

    i8 m_hshift {};
    i8 m_vshift {};

    bool m_decoded { false };

    Vector<i32> m_pixels {};
};

}
