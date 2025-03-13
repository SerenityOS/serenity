/*
 * Copyright (c) 2025, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Error.h>
#include <AK/Vector.h>

namespace Gfx {

class Channel {
public:
    static ErrorOr<Channel> create(u32 width, u32 height)
    {
        Channel channel;

        channel.m_width = width;
        channel.m_height = height;

        TRY(channel.m_pixels.try_resize(channel.m_width * channel.m_height));

        return channel;
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

    u32 hshift() const
    {
        return m_hshift;
    }

    u32 vshift() const
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

private:
    u32 m_width {};
    u32 m_height {};

    u32 m_hshift {};
    u32 m_vshift {};

    bool m_decoded { false };

    Vector<i32> m_pixels {};
};

}
