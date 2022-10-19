/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StdLibExtras.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibGfx/Line.h>
#include <LibGfx/Rect.h>
#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>

namespace Gfx {

template<>
String IntRect::to_string() const
{
    return String::formatted("[{},{} {}x{}]", x(), y(), width(), height());
}

template<>
String FloatRect::to_string() const
{
    return String::formatted("[{},{} {}x{}]", x(), y(), width(), height());
}

}

namespace IPC {

bool encode(Encoder& encoder, Gfx::IntRect const& rect)
{
    encoder << rect.location() << rect.size();
    return true;
}

ErrorOr<void> decode(Decoder& decoder, Gfx::IntRect& rect)
{
    Gfx::IntPoint point;
    Gfx::IntSize size;
    TRY(decoder.decode(point));
    TRY(decoder.decode(size));
    rect = { point, size };
    return {};
}

}

template class Gfx::Rect<int>;
template class Gfx::Rect<float>;
