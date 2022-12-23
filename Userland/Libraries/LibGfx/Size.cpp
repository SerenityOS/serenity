/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <LibGfx/Size.h>
#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>

namespace Gfx {

template<>
DeprecatedString IntSize::to_deprecated_string() const
{
    return DeprecatedString::formatted("[{}x{}]", m_width, m_height);
}

template<>
DeprecatedString FloatSize::to_deprecated_string() const
{
    return DeprecatedString::formatted("[{}x{}]", m_width, m_height);
}

}

namespace IPC {

template<>
bool encode(Encoder& encoder, Gfx::IntSize const& size)
{
    encoder << size.width() << size.height();
    return true;
}

template<>
ErrorOr<Gfx::IntSize> decode(Decoder& decoder)
{
    auto width = TRY(decoder.decode<int>());
    auto height = TRY(decoder.decode<int>());
    return Gfx::IntSize { width, height };
}

}

template class Gfx::Size<int>;
template class Gfx::Size<float>;
