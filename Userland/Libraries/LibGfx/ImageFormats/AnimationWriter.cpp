/*
 * Copyright (c) 2024, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGfx/Bitmap.h>
#include <LibGfx/ImageFormats/AnimationWriter.h>
#include <LibGfx/Rect.h>

namespace Gfx {

AnimationWriter::~AnimationWriter() = default;

static bool are_scanlines_equal(Bitmap const& a, Bitmap const& b, int y)
{
    for (int x = 0; x < a.width(); ++x) {
        if (a.get_pixel(x, y) != b.get_pixel(x, y))
            return false;
    }
    return true;
}

static bool are_columns_equal(Bitmap const& a, Bitmap const& b, int x, int y1, int y2)
{
    for (int y = y1; y < y2; ++y) {
        if (a.get_pixel(x, y) != b.get_pixel(x, y))
            return false;
    }
    return true;
}

static Gfx::IntRect rect_where_pixels_are_different(Bitmap const& a, Bitmap const& b)
{
    VERIFY(a.size() == b.size());

    // FIXME: This works on physical pixels.
    VERIFY(a.scale() == 1);
    VERIFY(b.scale() == 1);

    int number_of_equal_pixels_at_top = 0;
    while (number_of_equal_pixels_at_top < a.height() && are_scanlines_equal(a, b, number_of_equal_pixels_at_top))
        ++number_of_equal_pixels_at_top;

    int number_of_equal_pixels_at_bottom = 0;
    while (number_of_equal_pixels_at_bottom < a.height() - number_of_equal_pixels_at_top && are_scanlines_equal(a, b, a.height() - number_of_equal_pixels_at_bottom - 1))
        ++number_of_equal_pixels_at_bottom;

    int const y1 = number_of_equal_pixels_at_top;
    int const y2 = a.height() - number_of_equal_pixels_at_bottom;

    int number_of_equal_pixels_at_left = 0;
    while (number_of_equal_pixels_at_left < a.width() && are_columns_equal(a, b, number_of_equal_pixels_at_left, y1, y2))
        ++number_of_equal_pixels_at_left;

    int number_of_equal_pixels_at_right = 0;
    while (number_of_equal_pixels_at_right < a.width() - number_of_equal_pixels_at_left && are_columns_equal(a, b, a.width() - number_of_equal_pixels_at_right - 1, y1, y2))
        ++number_of_equal_pixels_at_right;

    // WebP can only encode even-sized animation frame positions.
    // FIXME: Change API shape in some way so that the AnimationWriter base class doesn't have to know about this detail of a subclass.
    if (number_of_equal_pixels_at_left % 2 != 0)
        --number_of_equal_pixels_at_left;
    if (number_of_equal_pixels_at_top % 2 != 0)
        --number_of_equal_pixels_at_top;

    Gfx::IntRect rect;
    rect.set_x(number_of_equal_pixels_at_left);
    rect.set_y(number_of_equal_pixels_at_top);
    rect.set_width(a.width() - number_of_equal_pixels_at_left - number_of_equal_pixels_at_right);
    rect.set_height(a.height() - number_of_equal_pixels_at_top - number_of_equal_pixels_at_bottom);

    return rect;
}

bool AnimationWriter::can_zero_out_unchanging_pixels(Bitmap& new_frame, Gfx::IntRect new_frame_rect, Bitmap& last_frame, AllowInterFrameCompression allow_inter_frame_compression) const
{
    if (!can_blend_frames() || allow_inter_frame_compression == AllowInterFrameCompression::No)
        return false;

    VERIFY(new_frame.width() == new_frame_rect.width());
    VERIFY(new_frame.height() == new_frame_rect.height());
    for (int y = 0; y < new_frame.height(); ++y) {
        for (int x = 0; x < new_frame.width(); ++x) {
            if (new_frame.get_pixel(x, y).alpha() != 255 && new_frame.get_pixel(x, y) != last_frame.get_pixel(x + new_frame_rect.x(), y + new_frame_rect.y()))
                return false;
        }
    }
    return true;
}

ErrorOr<void> AnimationWriter::add_frame_relative_to_last_frame(Bitmap& frame, int duration_ms, RefPtr<Bitmap> last_frame, AllowInterFrameCompression allow_inter_frame_compression)
{
    if (!last_frame)
        return add_frame(frame, duration_ms);

    auto rect = rect_where_pixels_are_different(*last_frame, frame);

    if (rect.is_empty()) {
        // The frame is identical to the last frame. Don't store an empty bitmap.
        // FIXME: We could delay writing the last frame until we know that the next frame is different,
        //        and just keep increasing that frame's duration instead.
        rect = { 0, 0, 1, 1 };
    }

    // FIXME: It would be nice to have a way to crop a bitmap without copying the data.
    auto differences = TRY(frame.cropped(rect));

    BlendMode blend_mode = BlendMode::Replace;

    // If all frames of the animation have no alpha, set color values of pixels that are in the changed rect that are
    // equal to the last frame to transparent black and set the frame to be blended. This is almost smaller after compression.
    if (can_zero_out_unchanging_pixels(*differences, rect, *last_frame, allow_inter_frame_compression)) {
        for (int y = 0; y < differences->height(); ++y) {
            for (int x = 0; x < differences->width(); ++x) {
                if (differences->get_pixel(x, y) == last_frame->get_pixel(x + rect.x(), y + rect.y()) || differences->get_pixel(x, y).alpha() == 0)
                    differences->set_pixel(x, y, Color(0, 0, 0, 0));
            }
        }
        blend_mode = BlendMode::Blend;
    }

    // This assumes a replacement disposal method.
    return add_frame(differences, duration_ms, rect.location(), blend_mode);
}

}
