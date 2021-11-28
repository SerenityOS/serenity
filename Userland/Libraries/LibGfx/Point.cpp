/*
 * Copyright (c) 2020-2021, Andreas Kling <kling@serenityos.org>
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
void Point<T>::constrain(Rect<T> const& rect)
{
    m_x = AK::clamp<T>(x(), rect.left(), rect.right());
    m_y = AK::clamp<T>(y(), rect.top(), rect.bottom());
}

template<typename T>
[[nodiscard]] Point<T> Point<T>::end_point_for_aspect_ratio(Point<T> const& previous_end_point, float aspect_ratio) const
{
    VERIFY(aspect_ratio > 0);
    const T x_sign = previous_end_point.x() >= x() ? 1 : -1;
    const T y_sign = previous_end_point.y() >= y() ? 1 : -1;
    T dx = AK::abs(previous_end_point.x() - x());
    T dy = AK::abs(previous_end_point.y() - y());
    if (dx > dy) {
        dy = (T)((float)dx / aspect_ratio);
    } else {
        dx = (T)((float)dy * aspect_ratio);
    }
    return { x() + x_sign * dx, y() + y_sign * dy };
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

bool encode(Encoder& encoder, Gfx::IntPoint const& point)
{
    encoder << point.x() << point.y();
    return true;
}

ErrorOr<void> decode(Decoder& decoder, Gfx::IntPoint& point)
{
    int x = 0;
    int y = 0;
    TRY(decoder.decode(x));
    TRY(decoder.decode(y));
    point = { x, y };
    return {};
}

}

template class Gfx::Point<int>;
template class Gfx::Point<float>;
