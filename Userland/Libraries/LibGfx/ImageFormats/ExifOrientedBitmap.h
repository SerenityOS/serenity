/*
 * Copyright (c) 2023, Lucas Chollet <lucas.chollet@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtr.h>
#include <LibGfx/Bitmap.h>

namespace Gfx {
class ExifOrientedBitmap {
public:
    // In the EXIF 3.0 specification, 4.6.5.1.6. Orientation
    enum class Orientation {
        None = 1,
        FlipHorizontally = 2,
        Rotate180 = 3,
        FlipVertically = 4,
        Rotate90ClockwiseThenFlipHorizontally = 5,
        Rotate90Clockwise = 6,
        FlipHorizontallyThenRotate90Clockwise = 7,
        Rotate90CounterClockwise = 8,
    };

    template<typename... Args>
    static ErrorOr<ExifOrientedBitmap> create(BitmapFormat format, IntSize size, Orientation orientation)
    {
        auto bitmap = TRY(Bitmap::create(format, oriented_size(size, orientation)));
        return ExifOrientedBitmap(move(bitmap), size, orientation);
    }

    void set_pixel(u32 x, u32 y, Color color)
    {
        auto const new_position = oriented_position(IntPoint(x, y));
        m_bitmap->set_pixel(new_position, color);
    }

    NonnullRefPtr<Bitmap>& bitmap()
    {
        return m_bitmap;
    }

private:
    ExifOrientedBitmap(NonnullRefPtr<Bitmap> bitmap, IntSize size, Orientation orientation)
        : m_bitmap(move(bitmap))
        , m_orientation(orientation)
        , m_width(size.width())
        , m_height(size.height())
    {
    }

    static IntSize oriented_size(IntSize size, Orientation orientation)
    {
        switch (orientation) {

        case Orientation::None:
        case Orientation::FlipHorizontally:
        case Orientation::Rotate180:
        case Orientation::FlipVertically:
            return size;
        case Orientation::Rotate90ClockwiseThenFlipHorizontally:
        case Orientation::Rotate90Clockwise:
        case Orientation::FlipHorizontallyThenRotate90Clockwise:
        case Orientation::Rotate90CounterClockwise:
            return { size.height(), size.width() };
        }
        VERIFY_NOT_REACHED();
    }

    IntPoint oriented_position(IntPoint point)
    {
        auto const flip_horizontally = [this](IntPoint point) {
            return IntPoint(m_width - point.x() - 1, point.y());
        };

        auto const rotate_90_clockwise = [this](IntPoint point) {
            return IntPoint(m_height - point.y() - 1, point.x());
        };

        switch (m_orientation) {
        case Orientation::None:
            return point;
        case Orientation::FlipHorizontally:
            return flip_horizontally(point);
        case Orientation::Rotate180:
            return IntPoint(m_width - point.x() - 1, m_height - point.y() - 1);
        case Orientation::FlipVertically:
            return IntPoint(point.x(), m_height - point.y() - 1);
        case Orientation::Rotate90ClockwiseThenFlipHorizontally:
            return flip_horizontally(rotate_90_clockwise(point));
        case Orientation::Rotate90Clockwise:
            return rotate_90_clockwise(point);
        case Orientation::FlipHorizontallyThenRotate90Clockwise:
            return rotate_90_clockwise(flip_horizontally(point));
        case Orientation::Rotate90CounterClockwise:
            return IntPoint(point.y(), m_width - point.x() - 1);
        }
        VERIFY_NOT_REACHED();
    }

    NonnullRefPtr<Bitmap> m_bitmap;
    Orientation m_orientation;

    u32 m_width {};
    u32 m_height {};
};
}
