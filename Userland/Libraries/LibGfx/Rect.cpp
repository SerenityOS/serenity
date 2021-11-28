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

template<typename T>

Rect<T>::RelativeLocation::RelativeLocation(Rect<T> const& base_rect, Rect<T> const& other_rect)
{
    if (base_rect.is_empty() || other_rect.is_empty())
        return;
    auto parts = base_rect.shatter(other_rect);
    for (auto& part : parts) {
        if (part.x() < other_rect.x()) {
            if (part.y() < other_rect.y())
                m_top_left = true;
            if ((part.y() >= other_rect.y() && part.y() < other_rect.bottom()) || (part.y() <= other_rect.bottom() && part.bottom() > other_rect.y()))
                m_left = true;
            if (part.y() >= other_rect.bottom() || part.bottom() > other_rect.y())
                m_bottom_left = true;
        }
        if (part.x() >= other_rect.x() || part.right() > other_rect.x()) {
            if (part.y() < other_rect.y())
                m_top = true;
            if (part.y() >= other_rect.bottom() || part.bottom() > other_rect.bottom())
                m_bottom = true;
        }
        if (part.x() >= other_rect.right() || part.right() > other_rect.right()) {
            if (part.y() < other_rect.y())
                m_top_right = true;
            if ((part.y() >= other_rect.y() && part.y() < other_rect.bottom()) || (part.y() <= other_rect.bottom() && part.bottom() > other_rect.y()))
                m_right = true;
            if (part.y() >= other_rect.bottom() || part.bottom() > other_rect.y())
                m_bottom_right = true;
        }
    }
}

template<typename T>
Vector<Point<T>, 2> Rect<T>::intersected(Line<T> const& line) const
{
    if (is_empty())
        return {};
    Vector<Point<T>, 2> points;
    if (auto point = line.intersected({ top_left(), top_right() }); point.has_value())
        points.append({ point.value().x(), y() });
    if (auto point = line.intersected({ bottom_left(), bottom_right() }); point.has_value()) {
        points.append({ point.value().x(), bottom() });
        if (points.size() == 2)
            return points;
    }
    if (height() > 2) {
        if (auto point = line.intersected({ { x(), y() + 1 }, { x(), bottom() - 1 } }); point.has_value()) {
            points.append({ x(), point.value().y() });
            if (points.size() == 2)
                return points;
        }
        if (auto point = line.intersected({ { right(), y() + 1 }, { right(), bottom() - 1 } }); point.has_value())
            points.append({ right(), point.value().y() });
    }
    return points;
}

template<typename T>
float Rect<T>::center_point_distance_to(Rect<T> const& other) const
{
    return Line { center(), other.center() }.length();
}

template<typename T>
Vector<Point<T>, 2> Rect<T>::closest_outside_center_points(Rect<T> const& other) const
{
    if (intersects(other))
        return {};
    Line centers_line { center(), other.center() };
    auto points_this = intersected(centers_line);
    VERIFY(points_this.size() == 1);
    auto points_other = other.intersected(centers_line);
    VERIFY(points_other.size() == 1);
    return { points_this[0], points_other[0] };
}

template<typename T>
float Rect<T>::outside_center_point_distance_to(Rect<T> const& other) const
{
    auto points = closest_outside_center_points(other);
    if (points.is_empty())
        return 0.0;
    return Line { points[0], points[0] }.length();
}

template<typename T>
Rect<T> Rect<T>::constrained_to(Rect<T> const& constrain_rect) const
{
    if (constrain_rect.contains(*this))
        return *this;
    T move_x = 0, move_y = 0;
    if (right() > constrain_rect.right())
        move_x = constrain_rect.right() - right();
    if (bottom() > constrain_rect.bottom())
        move_y = constrain_rect.bottom() - bottom();
    if (x() < constrain_rect.x())
        move_x = x() - constrain_rect.x();
    if (y() < constrain_rect.y())
        move_y = y() - constrain_rect.y();
    auto rect = *this;
    if (move_x != 0 || move_y != 0)
        rect.translate_by(move_x, move_y);
    return rect;
}

template<typename T>
Rect<T> Rect<T>::aligned_within(Size<T> const& rect_size, Point<T> const& align_at, TextAlignment alignment) const
{
    if (rect_size.is_empty())
        return {};
    if (!size().contains(rect_size))
        return {};
    if (!contains(align_at))
        return {};

    Rect<T> rect;
    switch (alignment) {
    case TextAlignment::TopLeft:
        rect = { align_at, rect_size };
        break;
    case TextAlignment::CenterLeft:
        rect = { { align_at.x(), align_at.y() - rect_size.height() / 2 }, rect_size };
        break;
    case TextAlignment::Center:
        rect = { { align_at.x() - rect_size.width() / 2, align_at.y() - rect_size.height() / 2 }, rect_size };
        break;
    case TextAlignment::CenterRight:
        rect = { { align_at.x() - rect_size.width() / 2, align_at.y() }, rect_size };
        break;
    case TextAlignment::TopRight:
        rect = { { align_at.x() - rect_size.width(), align_at.y() }, rect_size };
        break;
    case TextAlignment::BottomLeft:
        rect = { { align_at.x(), align_at.y() - rect_size.width() }, rect_size };
        break;
    case TextAlignment::BottomRight:
        rect = { { align_at.x() - rect_size.width(), align_at.y() - rect_size.width() }, rect_size };
        break;
    }
    return rect.constrained_to(*this);
}

template<typename T>
Point<T> Rect<T>::closest_to(Point<T> const& point) const
{
    if (is_empty())
        return {};
    Optional<Point<T>> closest_point;
    float closest_distance = 0.0;
    auto check_distance = [&](const Line<T>& line) {
        auto point_on_line = line.closest_to(point);
        auto distance = Line { point_on_line, point }.length();
        if (!closest_point.has_value() || distance < closest_distance) {
            closest_point = point_on_line;
            closest_distance = distance;
        }
    };

    check_distance({ top_left(), top_right() });
    check_distance({ bottom_left(), bottom_right() });
    if (height() > 2) {
        check_distance({ { x(), y() + 1 }, { x(), bottom() - 1 } });
        check_distance({ { right(), y() + 1 }, { right(), bottom() - 1 } });
    }
    VERIFY(closest_point.has_value());
    VERIFY(side(closest_point.value()) != Side::None);
    return closest_point.value();
}

template<typename T>
void Rect<T>::intersect(Rect<T> const& other)
{
    T l = max(left(), other.left());
    T r = min(right(), other.right());
    T t = max(top(), other.top());
    T b = min(bottom(), other.bottom());

    if (l > r || t > b) {
        m_location = {};
        m_size = {};
        return;
    }

    m_location.set_x(l);
    m_location.set_y(t);
    m_size.set_width((r - l) + 1);
    m_size.set_height((b - t) + 1);
}

template<typename T>
Rect<T> Rect<T>::united(Rect<T> const& other) const
{
    if (is_null())
        return other;
    if (other.is_null())
        return *this;
    Rect<T> rect;
    rect.set_left(min(left(), other.left()));
    rect.set_top(min(top(), other.top()));
    rect.set_right(max(right(), other.right()));
    rect.set_bottom(max(bottom(), other.bottom()));
    return rect;
}

template<typename T>
Vector<Rect<T>, 4> Rect<T>::shatter(Rect<T> const& hammer) const
{
    Vector<Rect<T>, 4> pieces;
    if (!intersects(hammer)) {
        pieces.unchecked_append(*this);
        return pieces;
    }
    Rect<T> top_shard {
        x(),
        y(),
        width(),
        hammer.y() - y()
    };
    Rect<T> bottom_shard {
        x(),
        hammer.y() + hammer.height(),
        width(),
        (y() + height()) - (hammer.y() + hammer.height())
    };
    Rect<T> left_shard {
        x(),
        max(hammer.y(), y()),
        hammer.x() - x(),
        min((hammer.y() + hammer.height()), (y() + height())) - max(hammer.y(), y())
    };
    Rect<T> right_shard {
        hammer.x() + hammer.width(),
        max(hammer.y(), y()),
        right() - hammer.right(),
        min((hammer.y() + hammer.height()), (y() + height())) - max(hammer.y(), y())
    };
    if (!top_shard.is_empty())
        pieces.unchecked_append(top_shard);
    if (!bottom_shard.is_empty())
        pieces.unchecked_append(bottom_shard);
    if (!left_shard.is_empty())
        pieces.unchecked_append(left_shard);
    if (!right_shard.is_empty())
        pieces.unchecked_append(right_shard);

    return pieces;
}

template<typename T>
void Rect<T>::align_within(Rect<T> const& other, TextAlignment alignment)
{
    switch (alignment) {
    case TextAlignment::Center:
        center_within(other);
        return;
    case TextAlignment::TopLeft:
        set_location(other.location());
        return;
    case TextAlignment::TopRight:
        set_x(other.x() + other.width() - width());
        set_y(other.y());
        return;
    case TextAlignment::CenterLeft:
        set_x(other.x());
        center_vertically_within(other);
        return;
    case TextAlignment::CenterRight:
        set_x(other.x() + other.width() - width());
        center_vertically_within(other);
        return;
    case TextAlignment::BottomLeft:
        set_x(other.x());
        set_y(other.y() + other.height() - height());
        return;
    case TextAlignment::BottomRight:
        set_x(other.x() + other.width() - width());
        set_y(other.y() + other.height() - height());
        return;
    }
}

template<typename T>
void Rect<T>::set_size_around(Size<T> const& new_size, Point<T> const& fixed_point)
{
    const T new_x = fixed_point.x() - (T)(new_size.width() * ((float)(fixed_point.x() - x()) / width()));
    const T new_y = fixed_point.y() - (T)(new_size.height() * ((float)(fixed_point.y() - y()) / height()));

    set_location({ new_x, new_y });
    set_size(new_size);
}

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
