/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/String.h>
#include <LibGfx/Point.h>
#include <LibGfx/Rect.h>
#include <LibIPC/Decoder.h>
#include <LibIPC/Encoder.h>

namespace Gfx {

template<typename T>
void Point<T>::constrain(const Rect<T>& rect)
{
    if (x() < rect.left()) {
        set_x(rect.left());
    } else if (x() > rect.right()) {
        set_x(rect.right());
    }

    if (y() < rect.top()) {
        set_y(rect.top());
    } else if (y() > rect.bottom()) {
        set_y(rect.bottom());
    }
}

template<>
String IntPoint::to_string() const
{
    return String::formatted("[{},{}]", x(), y());
}

template<>
String FloatPoint::to_string() const
{
    return String::formatted("[{},{}]", x(), y());
}

}

namespace IPC {

bool encode(Encoder& encoder, const Gfx::IntPoint& point)
{
    encoder << point.x() << point.y();
    return true;
}

bool decode(Decoder& decoder, Gfx::IntPoint& point)
{
    int x = 0;
    int y = 0;
    if (!decoder.decode(x))
        return false;
    if (!decoder.decode(y))
        return false;
    point = { x, y };
    return true;
}

}

template class Gfx::Point<int>;
template class Gfx::Point<float>;
