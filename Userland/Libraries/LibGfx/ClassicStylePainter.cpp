/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2020, Sarah Taube <metalflakecobaltpaint@gmail.com>
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

static void paint_button_new(Painter& painter, IntRect const& a_rect, Palette const& palette, ButtonStyle style, bool pressed, bool checked, bool hovered, bool enabled, bool focused)
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
    if (focused) {
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

void ClassicStylePainter::paint_button(Painter& painter, IntRect const& rect, Palette const& palette, ButtonStyle button_style, bool pressed, bool hovered, bool checked, bool enabled, bool focused)
{
    if (button_style == ButtonStyle::Normal || button_style == ButtonStyle::ThickCap)
        return paint_button_new(painter, rect, palette, button_style, pressed, checked, hovered, enabled, focused);

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

    painter.draw_line(rect.top_left(), rect.top_right(), base_color);
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

void ClassicStylePainter::paint_progressbar(Painter& painter, IntRect const& rect, Palette const& palette, int min, int max, int value, StringView const& text, Orientation orientation)
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
        s_unfilled_circle_bitmap = Bitmap::try_load_from_file("/res/icons/serenity/unfilled-radio-circle.png");
        s_filled_circle_bitmap = Bitmap::try_load_from_file("/res/icons/serenity/filled-radio-circle.png");
        s_changing_filled_circle_bitmap = Bitmap::try_load_from_file("/res/icons/serenity/changing-filled-radio-circle.png");
        s_changing_unfilled_circle_bitmap = Bitmap::try_load_from_file("/res/icons/serenity/changing-unfilled-radio-circle.png");
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

}
