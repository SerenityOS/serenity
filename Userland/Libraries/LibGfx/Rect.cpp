/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/DeprecatedString.h>
#include <AK/StdLibExtras.h>
#include <AK/Vector.h>
#include <LibGfx/Line.h>
#include <LibGfx/Rect.h>
#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>

namespace Gfx {

template<>
DeprecatedString IntRect::to_deprecated_string() const
{
    return DeprecatedString::formatted("[{},{} {}x{}]", x(), y(), width(), height());
}

template<>
DeprecatedString FloatRect::to_deprecated_string() const
{
    return DeprecatedString::formatted("[{},{} {}x{}]", x(), y(), width(), height());
}

}

namespace IPC {

template<>
bool encode(Encoder& encoder, Gfx::IntRect const& rect)
{
    encoder << rect.location() << rect.size();
    return true;
}

template<>
ErrorOr<Gfx::IntRect> decode(Decoder& decoder)
{
    auto point = TRY(decoder.decode<Gfx::IntPoint>());
    auto size = TRY(decoder.decode<Gfx::IntSize>());
    return Gfx::IntRect { point, size };
}

}

template class Gfx::Rect<int>;
template class Gfx::Rect<float>;
