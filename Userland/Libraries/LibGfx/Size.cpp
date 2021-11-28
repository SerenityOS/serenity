/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibGfx/Size.h>
#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>

namespace Gfx {

template<>
String IntSize::to_string() const
{
    return String::formatted("[{}x{}]", m_width, m_height);
}

template<>
String FloatSize::to_string() const
{
    return String::formatted("[{}x{}]", m_width, m_height);
}

}

namespace IPC {

bool encode(Encoder& encoder, Gfx::IntSize const& size)
{
    encoder << size.width() << size.height();
    return true;
}

ErrorOr<void> decode(Decoder& decoder, Gfx::IntSize& size)
{
    int width = 0;
    int height = 0;
    TRY(decoder.decode(width));
    TRY(decoder.decode(height));
    size = { width, height };
    return {};
}

}

template class Gfx::Size<int>;
template class Gfx::Size<float>;
