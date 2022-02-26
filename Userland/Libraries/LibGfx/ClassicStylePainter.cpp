/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Sarah Taube <metalflakecobaltpaint@gmail.com>
 * Copyright (c) 2021, Filiph Sandström <filiph.sandstrom@filfatstudios.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/StringView.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/CharacterBitmap.h>
#include <LibGfx/ClassicStylePainter.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Palette.h>

namespace Gfx {

void ClassicStylePainter::paint_tab_button(Painter& painter, IntRect const& rect, Palette const& palette, bool active, bool hovered, bool enabled, bool top, bool in_active_window)
{
    Color base_color = palette.button();
    Color highlight_color2 = palette.threed_highlight();
    Color shadow_color1 = palette.threed_shadow1();
    Color shadow_color2 = palette.threed_shadow2();

    if (hovered && enabled && !active)
        base_color = palette.hover_highlight();

    PainterStateSaver saver(painter);
    painter.translate(rect.location());

    if (top) {
        // Base
        painter.fill_rect({ 1, 1, rect.width() - 2, rect.height() - 1 }, base_color);

        // Top line
        if (active) {
            auto accent = palette.accent();
            if (!in_active_window)
                accent = accent.to_grayscale();
            painter.draw_line({ 3, 0 }, { rect.width() - 3, 0 }, accent.darkened());
            Gfx::IntRect accent_rect { 1, 1, rect.width() - 2, 2 };
            painter.fill_rect_with_gradient(accent_rect, accent, accent.lightened(1.5f));
            painter.set_pixel({ 2, 0 }, highlight_color2);
        } else {
            painter.draw_line({ 2, 0 }, { rect.width() - 3, 0 }, highlight_color2);
        }

        // Left side
        painter.draw_line({ 0, 2 }, { 0, rect.height() - 1 }, highlight_color2);
        painter.set_pixel({ 1, 1 }, highlight_color2);

        // Right side

        IntPoint top_right_outer { rect.width() - 1, 2 };
        IntPoint bottom_right_outer { rect.width() - 1, rect.height() - 1 };
        painter.draw_line(top_right_outer, bottom_right_outer, shadow_color2);

        IntPoint top_right_inner { rect.width() - 2, 2 };
        IntPoint bottom_right_inner { rect.width() - 2, rect.height() - 1 };
        painter.draw_line(top_right_inner, bottom_right_inner, shadow_color1);

        painter.set_pixel(rect.width() - 2, 1, shadow_color2);
    } else {
        // Base
        painter.fill_rect({ 0, 0, rect.width() - 1, rect.height() }, base_color);

        // Bottom line
        if (active) {
            auto accent = palette.accent();
            if (!in_active_window)
                accent = accent.to_grayscale();
            Gfx::IntRect accent_rect { 1, rect.height() - 3, rect.width() - 2, 2 };
            painter.fill_rect_with_gradient(accent_rect, accent, accent.lightened(1.5f));
            painter.draw_line({ 2, rect.height() - 1 }, { rect.width() - 3, rect.height() - 1 }, accent.darkened());
        } else {
            painter.draw_line({ 2, rect.height() - 1 }, { rect.width() - 3, rect.height() - 1 }, shadow_color2);
        }

        // Left side
        painter.draw_line({ 0, 0 }, { 0, rect.height() - 3 }, highlight_color2);
        painter.set_pixel({ 1, rect.height() - 2 }, highlight_color2);

        // Right side
        IntPoint top_right_outer { rect.width() - 1, 0 };
        IntPoint bottom_right_outer { rect.width() - 1, rect.height() - 3 };
        painter.draw_line(top_right_outer, bottom_right_outer, shadow_color2);

        IntPoint top_right_inner { rect.width() - 2, 0 };
        IntPoint bottom_right_inner { rect.width() - 2, rect.height() - 3 };
        painter.draw_line(top_right_inner, bottom_right_inner, shadow_color1);

        painter.set_pixel(rect.width() - 2, rect.height() - 2, shadow_color2);
    }
}

static void paint_button_new(Painter& painter, IntRect const& a_rect, Palette const& palette, ButtonStyle style, bool pressed, bool checked, bool hovered, bool enabled, bool focused, bool default_button)
{
    Color button_color = palette.button();
    Color highlight_color = palette.threed_highlight();
    Color shadow_color1 = palette.threed_shadow1();
    Color shadow_color2 = palette.threed_shadow2();

    if (checked && enabled) {
        if (hovered)
            button_color = palette.hover_highlight();
        else
            button_color = palette.button();
    } else if (hovered && enabled)
        button_color = palette.hover_highlight();

    PainterStateSaver saver(painter);

    auto rect = a_rect;
    if (focused || default_button) {
        painter.draw_rect(a_rect, palette.threed_shadow2());
        rect.shrink(2, 2);
    }

    painter.translate(rect.location());

    if (pressed || checked) {
        // Base
        Gfx::IntRect base_rect { 1, 1, rect.width() - 2, rect.height() - 2 };

        if (checked && !pressed)
            painter.fill_rect_with_dither_pattern(base_rect, palette.button().lightened(1.3f), palette.button());
        else
            painter.fill_rect(base_rect, button_color);

        // Top shadow
        painter.draw_line({ 0, 0 }, { rect.width() - 2, 0 }, shadow_color2);
        painter.draw_line({ 0, 0 }, { 0, rect.height() - 2 }, shadow_color2);

        // Sunken shadow
        painter.draw_line({ 1, 1 }, { rect.width() - 3, 1 }, shadow_color1);
        painter.draw_line({ 1, 2 }, { 1, rect.height() - 3 }, shadow_color1);

        // Outer highlight
        painter.draw_line({ 0, rect.height() - 1 }, { rect.width() - 1, rect.height() - 1 }, highlight_color);
        painter.draw_line({ rect.width() - 1, 0 }, { rect.width() - 1, rect.height() - 2 }, highlight_color);

        // Inner highlight
        painter.draw_line({ 1, rect.height() - 2 }, { rect.width() - 2, rect.height() - 2 }, palette.button());
        painter.draw_line({ rect.width() - 2, 1 }, { rect.width() - 2, rect.height() - 3 }, palette.button());
    } else {
        // Base
        painter.fill_rect({ 0, 0, rect.width(), rect.height() }, button_color);

        // Top highlight
        if (style == ButtonStyle::Normal) {
            painter.draw_line({ 0, 0 }, { rect.width() - 2, 0 }, highlight_color);
            painter.draw_line({ 0, 0 }, { 0, rect.height() - 2 }, highlight_color);
        } else if (style == ButtonStyle::ThickCap) {
            painter.draw_line({ 1, 1 }, { rect.width() - 2, 1 }, highlight_color);
            painter.draw_line({ 1, 1 }, { 1, rect.height() - 2 }, highlight_color);
        }

        // Outer shadow
        painter.draw_line({ 0, rect.height() - 1 }, { rect.width() - 1, rect.height() - 1 }, shadow_color2);
        painter.draw_line({ rect.width() - 1, 0 }, { rect.width() - 1, rect.height() - 2 }, shadow_color2);

        // Inner shadow
        painter.draw_line({ 1, rect.height() - 2 }, { rect.width() - 2, rect.height() - 2 }, shadow_color1);
        painter.draw_line({ rect.width() - 2, 1 }, { rect.width() - 2, rect.height() - 3 }, shadow_color1);
    }
}

void ClassicStylePainter::paint_button(Painter& painter, IntRect const& rect, Palette const& palette, ButtonStyle button_style, bool pressed, bool hovered, bool checked, bool enabled, bool focused, bool default_button)
{
    if (button_style == ButtonStyle::Normal || button_style == ButtonStyle::ThickCap)
        return paint_button_new(painter, rect, palette, button_style, pressed, checked, hovered, enabled, focused, default_button);

    if (button_style == ButtonStyle::Coolbar && !enabled)
        return;

    Color button_color = palette.button();
    Color highlight_color = palette.threed_highlight();
    Color shadow_color = button_style == ButtonStyle::Coolbar ? palette.threed_shadow1() : palette.threed_shadow2();

    PainterStateSaver saver(painter);
    painter.translate(rect.location());

    if (pressed || checked) {
        // Base
        IntRect base_rect { 1, 1, rect.width() - 2, rect.height() - 2 };
        if (button_style == ButtonStyle::Coolbar) {
            if (checked && !pressed) {
                painter.fill_rect_with_dither_pattern(base_rect, palette.button().lightened(1.3f), Color());
            } else {
                painter.fill_rect(base_rect, button_color);
            }
        }

        // Sunken shadow
        painter.draw_line({ 1, 1 }, { rect.width() - 2, 1 }, shadow_color);
        painter.draw_line({ 1, 2 }, { 1, rect.height() - 2 }, shadow_color);

        // Bottom highlight
        painter.draw_line({ rect.width() - 2, 1 }, { rect.width() - 2, rect.height() - 3 }, highlight_color);
        painter.draw_line({ 1, rect.height() - 2 }, { rect.width() - 2, rect.height() - 2 }, highlight_color);
    } else if (hovered) {
        if (button_style == ButtonStyle::Coolbar) {
            // Base
            painter.fill_rect({ 1, 1, rect.width() - 2, rect.height() - 2 }, button_color);
        }

        // Top highlight
        painter.draw_line({ 1, 1 }, { rect.width() - 2, 1 }, highlight_color);
        painter.draw_line({ 1, 2 }, { 1, rect.height() - 2 }, highlight_color);

        // Bottom shadow
        painter.draw_line({ rect.width() - 2, 1 }, { rect.width() - 2, rect.height() - 3 }, shadow_color);
        painter.draw_line({ 1, rect.height() - 2 }, { rect.width() - 2, rect.height() - 2 }, shadow_color);
    }
}

void ClassicStylePainter::paint_frame(Painter& painter, IntRect const& rect, Palette const& palette, FrameShape shape, FrameShadow shadow, int thickness, bool skip_vertical_lines)
{
    if (shape == Gfx::FrameShape::NoFrame)
        return;

    if (shape == FrameShape::Window) {
        StylePainter::paint_window_frame(painter, rect, palette);
        return;
    }

    Color top_left_color;
    Color bottom_right_color;
    Color dark_shade = palette.threed_shadow1();
    Color light_shade = palette.threed_highlight();

    if (shape == FrameShape::Container && thickness >= 2) {
        if (shadow == FrameShadow::Raised) {
            dark_shade = palette.threed_shadow2();
        }
    }

    if (shadow == FrameShadow::Raised) {
        top_left_color = light_shade;
        bottom_right_color = dark_shade;
    } else if (shadow == FrameShadow::Sunken) {
        top_left_color = dark_shade;
        bottom_right_color = light_shade;
    } else if (shadow == FrameShadow::Plain) {
        top_left_color = dark_shade;
        bottom_right_color = dark_shade;
    }

    if (thickness >= 1) {
        painter.draw_line(rect.top_left(), rect.top_right(), top_left_color);
        painter.draw_line(rect.bottom_left(), rect.bottom_right(), bottom_right_color);

        if (shape != FrameShape::Panel || !skip_vertical_lines) {
            painter.draw_line(rect.top_left().translated(0, 1), rect.bottom_left().translated(0, -1), top_left_color);
            painter.draw_line(rect.top_right(), rect.bottom_right().translated(0, -1), bottom_right_color);
        }
    }

    if (shape == FrameShape::Container && thickness >= 2) {
        Color top_left_color;
        Color bottom_right_color;
        Color dark_shade = palette.threed_shadow2();
        Color light_shade = palette.button();
        if (shadow == FrameShadow::Raised) {
            dark_shade = palette.threed_shadow1();
            top_left_color = light_shade;
            bottom_right_color = dark_shade;
        } else if (shadow == FrameShadow::Sunken) {
            top_left_color = dark_shade;
            bottom_right_color = light_shade;
        } else if (shadow == FrameShadow::Plain) {
            top_left_color = dark_shade;
            bottom_right_color = dark_shade;
        }
        IntRect inner_container_frame_rect = rect.shrunken(2, 2);
        painter.draw_line(inner_container_frame_rect.top_left(), inner_container_frame_rect.top_right(), top_left_color);
        painter.draw_line(inner_container_frame_rect.bottom_left(), inner_container_frame_rect.bottom_right(), bottom_right_color);
        painter.draw_line(inner_container_frame_rect.top_left().translated(0, 1), inner_container_frame_rect.bottom_left().translated(0, -1), top_left_color);
        painter.draw_line(inner_container_frame_rect.top_right(), inner_container_frame_rect.bottom_right().translated(0, -1), bottom_right_color);
    }

    if (shape == FrameShape::Box && thickness >= 2) {
        swap(top_left_color, bottom_right_color);
        IntRect inner_rect = rect.shrunken(2, 2);
        painter.draw_line(inner_rect.top_left(), inner_rect.top_right(), top_left_color);
        painter.draw_line(inner_rect.bottom_left(), inner_rect.bottom_right(), bottom_right_color);
        painter.draw_line(inner_rect.top_left().translated(0, 1), inner_rect.bottom_left().translated(0, -1), top_left_color);
        painter.draw_line(inner_rect.top_right(), inner_rect.bottom_right().translated(0, -1), bottom_right_color);
    }
}

void ClassicStylePainter::paint_window_frame(Painter& painter, IntRect const& rect, Palette const& palette)
{
    Color base_color = palette.button();
    Color dark_shade = palette.threed_shadow2();
    Color mid_shade = palette.threed_shadow1();
    Color light_shade = palette.threed_highlight();
    auto border_thickness = palette.window_border_thickness();
    auto border_radius = palette.window_border_radius();

    if (border_radius > 0) {
        // FIXME: This will draw "useless" pixels that'll get drawn over by the window contents.
        // preferrably we should just remove the corner pixels from the completely drawn window
        // but I don't know how to do that yet. :^)
        painter.fill_rect_with_rounded_corners({ rect.x() - border_radius / 2,
                                                   rect.y() - border_radius / 2,
                                                   rect.width() + border_radius,
                                                   rect.height() + border_radius },
            base_color, border_radius);
        return;
    }

    painter.draw_rect_with_thickness({ rect.x() + border_thickness / 2,
                                         rect.y() + border_thickness / 2,
                                         rect.width() - border_thickness,
                                         rect.height() - border_thickness },
        base_color, border_thickness);

    painter.draw_line(rect.top_left().translated(0, 1), rect.bottom_left(), base_color);
    painter.draw_line(rect.top_left().translated(1, 1), rect.top_right().translated(-1, 1), light_shade);
    painter.draw_line(rect.top_left().translated(1, 1), rect.bottom_left().translated(1, -1), light_shade);
    painter.draw_line(rect.top_left().translated(2, 2), rect.top_right().translated(-2, 2), base_color);
    painter.draw_line(rect.top_left().translated(2, 2), rect.bottom_left().translated(2, -2), base_color);
    painter.draw_line(rect.top_left().translated(3, 3), rect.top_right().translated(-3, 3), base_color);
    painter.draw_line(rect.top_left().translated(3, 3), rect.bottom_left().translated(3, -3), base_color);

    painter.draw_line(rect.top_right(), rect.bottom_right(), dark_shade);
    painter.draw_line(rect.top_right().translated(-1, 1), rect.bottom_right().translated(-1, -1), mid_shade);
    painter.draw_line(rect.top_right().translated(-2, 2), rect.bottom_right().translated(-2, -2), base_color);
    painter.draw_line(rect.top_right().translated(-3, 3), rect.bottom_right().translated(-3, -3), base_color);
    painter.draw_line(rect.bottom_left(), rect.bottom_right(), dark_shade);
    painter.draw_line(rect.bottom_left().translated(1, -1), rect.bottom_right().translated(-1, -1), mid_shade);
    painter.draw_line(rect.bottom_left().translated(2, -2), rect.bottom_right().translated(-2, -2), base_color);
    painter.draw_line(rect.bottom_left().translated(3, -3), rect.bottom_right().translated(-3, -3), base_color);
}

void ClassicStylePainter::paint_progressbar(Painter& painter, IntRect const& rect, Palette const& palette, int min, int max, int value, StringView text, Orientation orientation)
{
    // First we fill the entire widget with the gradient. This incurs a bit of
    // overdraw but ensures a consistent look throughout the progression.
    Color start_color = palette.active_window_border1();
    Color end_color = palette.active_window_border2();
    painter.fill_rect_with_gradient(orientation, rect, start_color, end_color);

    if (!text.is_null()) {
        painter.draw_text(rect.translated(1, 1), text, TextAlignment::Center, palette.base_text());
        painter.draw_text(rect, text, TextAlignment::Center, palette.base_text().inverted());
    }

    float range_size = max - min;
    float progress = (value - min) / range_size;

    // Then we carve out a hole in the remaining part of the widget.
    // We draw the text a third time, clipped and inverse, for sharp contrast.
    IntRect hole_rect;
    if (orientation == Orientation::Horizontal) {
        float progress_width = progress * rect.width();
        hole_rect = { (int)progress_width, 0, (int)(rect.width() - progress_width), rect.height() };
    } else {
        float progress_height = progress * rect.height();
        hole_rect = { 0, 0, rect.width(), (int)(rect.height() - progress_height) };
    }
    hole_rect.translate_by(rect.location());
    hole_rect.set_right_without_resize(rect.right());
    PainterStateSaver saver(painter);
    painter.fill_rect(hole_rect, palette.base());

    painter.add_clip_rect(hole_rect);
    if (!text.is_null())
        painter.draw_text(rect.translated(0, 0), text, TextAlignment::Center, palette.base_text());
}

static RefPtr<Gfx::Bitmap> s_unfilled_circle_bitmap;
static RefPtr<Gfx::Bitmap> s_filled_circle_bitmap;
static RefPtr<Gfx::Bitmap> s_changing_filled_circle_bitmap;
static RefPtr<Gfx::Bitmap> s_changing_unfilled_circle_bitmap;

static Gfx::Bitmap const& circle_bitmap(bool checked, bool changing)
{
    if (changing)
        return checked ? *s_changing_filled_circle_bitmap : *s_changing_unfilled_circle_bitmap;
    return checked ? *s_filled_circle_bitmap : *s_unfilled_circle_bitmap;
}

void ClassicStylePainter::paint_radio_button(Painter& painter, IntRect const& rect, Palette const&, bool is_checked, bool is_being_pressed)
{
    if (!s_unfilled_circle_bitmap) {
        s_unfilled_circle_bitmap = Bitmap::try_load_from_file("/res/icons/serenity/unfilled-radio-circle.png").release_value_but_fixme_should_propagate_errors();
        s_filled_circle_bitmap = Bitmap::try_load_from_file("/res/icons/serenity/filled-radio-circle.png").release_value_but_fixme_should_propagate_errors();
        s_changing_filled_circle_bitmap = Bitmap::try_load_from_file("/res/icons/serenity/changing-filled-radio-circle.png").release_value_but_fixme_should_propagate_errors();
        s_changing_unfilled_circle_bitmap = Bitmap::try_load_from_file("/res/icons/serenity/changing-unfilled-radio-circle.png").release_value_but_fixme_should_propagate_errors();
    }

    auto& bitmap = circle_bitmap(is_checked, is_being_pressed);
    painter.blit(rect.location(), bitmap, bitmap.rect());
}

static char const* s_checked_bitmap_data = {
    "         "
    "       # "
    "      ## "
    "     ### "
    " ## ###  "
    " #####   "
    "  ###    "
    "   #     "
    "         "
};

static Gfx::CharacterBitmap* s_checked_bitmap;
static int const s_checked_bitmap_width = 9;
static int const s_checked_bitmap_height = 9;

void ClassicStylePainter::paint_check_box(Painter& painter, IntRect const& rect, Palette const& palette, bool is_enabled, bool is_checked, bool is_being_pressed)
{
    painter.fill_rect(rect, is_enabled ? palette.base() : palette.window());
    paint_frame(painter, rect, palette, Gfx::FrameShape::Container, Gfx::FrameShadow::Sunken, 2);

    if (is_being_pressed) {
        // FIXME: This color should not be hard-coded.
        painter.draw_rect(rect.shrunken(4, 4), Color::MidGray);
    }

    if (is_checked) {
        if (!s_checked_bitmap)
            s_checked_bitmap = &Gfx::CharacterBitmap::create_from_ascii(s_checked_bitmap_data, s_checked_bitmap_width, s_checked_bitmap_height).leak_ref();
        painter.draw_bitmap(rect.shrunken(4, 4).location(), *s_checked_bitmap, is_enabled ? palette.base_text() : palette.threed_shadow1());
    }
}

void ClassicStylePainter::paint_transparency_grid(Painter& painter, IntRect const& rect, Palette const& palette)
{
    painter.fill_rect_with_checkerboard(rect, { 8, 8 }, palette.base().darkened(0.9), palette.base());
}

void ClassicStylePainter::paint_simple_rect_shadow(Painter& painter, IntRect const& containing_rect, Bitmap const& shadow_bitmap, bool shadow_includes_frame, bool fill_content)
{
    // The layout of the shadow_bitmap is defined like this:
    // +---------+----+---------+----+----+----+
    // |   TL    | T  |   TR    | LT | L  | LB |
    // +---------+----+---------+----+----+----+
    // |   BL    | B  |   BR    | RT | R  | RB |
    // +---------+----+---------+----+----+----+
    // Located strictly on the top or bottom of the rectangle, above or below of the content:
    //   TL = top-left     T = top     TR = top-right
    //   BL = bottom-left  B = bottom  BR = bottom-right
    // Located on the left or right of the rectangle, but not above or below of the content:
    //   LT = left-top     L = left    LB = left-bottom
    //   RT = right-top    R = right   RB = right-bottom
    // So, the bitmap has two rows and 6 column, two of which are twice as wide.
    // The height divided by two defines a cell size, and width of each
    // column must be the same as the height of the cell, except for the
    // first and third column, which are twice as wide.
    // If fill_content is true, it will use the RGBA color of right-bottom pixel of TL to fill the rectangle enclosed
    if (shadow_bitmap.height() % 2 != 0) {
        dbgln("Can't paint simple rect shadow, shadow bitmap height {} is not even", shadow_bitmap.height());
        return;
    }
    auto base_size = shadow_bitmap.height() / 2;
    if (shadow_bitmap.width() != base_size * (6 + 2)) {
        if (shadow_bitmap.width() % base_size != 0)
            dbgln("Can't paint simple rect shadow, shadow bitmap width {} is not a multiple of {}", shadow_bitmap.width(), base_size);
        else
            dbgln("Can't paint simple rect shadow, shadow bitmap width {} but expected {}", shadow_bitmap.width(), base_size * (6 + 2));
        return;
    }

    // The containing_rect should have been inflated appropriately
    VERIFY(containing_rect.size().contains(Gfx::IntSize { base_size, base_size }));

    auto sides_height = containing_rect.height() - 2 * base_size;
    auto half_height = sides_height / 2;
    auto containing_horizontal_rect = containing_rect;

    int horizontal_shift = 0;
    if (half_height < base_size && !shadow_includes_frame) {
        // If the height is too small we need to shift the left/right accordingly, unless the shadow includes portions of the frame
        horizontal_shift = base_size - half_height;
        containing_horizontal_rect.set_left(containing_horizontal_rect.left() + horizontal_shift);
        containing_horizontal_rect.set_right(containing_horizontal_rect.right() - 2 * horizontal_shift);
    }
    auto half_width = containing_horizontal_rect.width() / 2;
    int corner_piece_width = min(containing_horizontal_rect.width() / 2, base_size * 2);
    int left_corners_right = containing_horizontal_rect.left() + corner_piece_width;
    int right_corners_left = max(containing_horizontal_rect.right() - corner_piece_width + 1, left_corners_right + 1);
    auto paint_horizontal = [&](int y, int src_row) {
        if (half_width <= 0)
            return;
        Gfx::PainterStateSaver save(painter);
        painter.add_clip_rect({ containing_horizontal_rect.left(), y, containing_horizontal_rect.width(), base_size });
        painter.blit({ containing_horizontal_rect.left(), y }, shadow_bitmap, { 0, src_row * base_size, corner_piece_width, base_size });
        painter.blit({ right_corners_left, y }, shadow_bitmap, { 5 * base_size - corner_piece_width, src_row * base_size, corner_piece_width, base_size });
        for (int x = left_corners_right; x < right_corners_left; x += base_size) {
            auto width = min(right_corners_left - x, base_size);
            painter.blit({ x, y }, shadow_bitmap, { corner_piece_width, src_row * base_size, width, base_size });
        }
    };

    paint_horizontal(containing_rect.top(), 0);
    paint_horizontal(containing_rect.bottom() - base_size + 1, 1);

    int corner_piece_height = min(half_height, base_size);
    int top_corners_bottom = base_size + corner_piece_height;
    int bottom_corners_top = base_size + max(half_height, sides_height - corner_piece_height);
    auto paint_vertical = [&](int x, int src_row, int hshift, int hsrcshift) {
        Gfx::PainterStateSaver save(painter);
        painter.add_clip_rect({ x, containing_rect.y() + base_size, base_size, containing_rect.height() - 2 * base_size });
        painter.blit({ x + hshift, containing_rect.top() + top_corners_bottom - corner_piece_height }, shadow_bitmap, { base_size * 5 + hsrcshift, src_row * base_size, base_size - hsrcshift, corner_piece_height });
        painter.blit({ x + hshift, containing_rect.top() + bottom_corners_top }, shadow_bitmap, { base_size * 7 + hsrcshift, src_row * base_size + base_size - corner_piece_height, base_size - hsrcshift, corner_piece_height });
        for (int y = top_corners_bottom; y < bottom_corners_top; y += base_size) {
            auto height = min(bottom_corners_top - y, base_size);
            painter.blit({ x, containing_rect.top() + y }, shadow_bitmap, { base_size * 6, src_row * base_size, base_size, height });
        }
    };

    paint_vertical(containing_rect.left(), 0, horizontal_shift, 0);
    if (shadow_includes_frame)
        horizontal_shift = 0; // TODO: fix off-by-one on rectangles barely wide enough
    paint_vertical(containing_rect.right() - base_size + 1, 1, 0, horizontal_shift);

    if (fill_content) {
        // Fill the enclosed rectangle with the RGBA color of the right-bottom pixel of the TL tile
        auto inner_rect = containing_rect.shrunken(2 * base_size, 2 * base_size);
        if (!inner_rect.is_empty())
            painter.fill_rect(inner_rect, shadow_bitmap.get_pixel(2 * base_size - 1, base_size - 1));
    }
}

}
