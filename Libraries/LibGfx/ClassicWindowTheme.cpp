/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <LibGfx/Bitmap.h>
#include <LibGfx/ClassicWindowTheme.h>
#include <LibGfx/Font.h>
#include <LibGfx/Painter.h>
#include <LibGfx/Palette.h>
#include <LibGfx/StylePainter.h>

namespace Gfx {

ClassicWindowTheme::ClassicWindowTheme()
{
}

ClassicWindowTheme::~ClassicWindowTheme()
{
}

Gfx::IntRect ClassicWindowTheme::title_bar_icon_rect(WindowType window_type, const IntRect& window_rect, const Palette& palette) const
{
    auto titlebar_rect = title_bar_rect(window_type, window_rect, palette);
    Gfx::IntRect icon_rect {
        titlebar_rect.x() + 2,
        titlebar_rect.y(),
        16,
        16,
    };
    icon_rect.center_vertically_within(titlebar_rect);
    icon_rect.move_by(0, 1);
    return icon_rect;
}

Gfx::IntRect ClassicWindowTheme::title_bar_text_rect(WindowType window_type, const IntRect& window_rect, const Palette& palette) const
{
    auto titlebar_rect = title_bar_rect(window_type, window_rect, palette);
    auto titlebar_icon_rect = title_bar_icon_rect(window_type, window_rect, palette);
    return {
        titlebar_rect.x() + 3 + titlebar_icon_rect.width() + 2,
        titlebar_rect.y(),
        titlebar_rect.width() - 5 - titlebar_icon_rect.width() - 2,
        titlebar_rect.height()
    };
}

void ClassicWindowTheme::paint_normal_frame(Painter& painter, WindowState window_state, const IntRect& outer_rect, const IntRect& window_rect, const StringView& title_text, const Bitmap& icon, const Palette& palette, const IntRect& leftmost_button_rect) const
{
    Gfx::StylePainter::paint_window_frame(painter, outer_rect, palette);

    auto& title_font = Font::default_bold_font();

    auto titlebar_rect = title_bar_rect(WindowType::Normal, window_rect, palette);
    auto titlebar_icon_rect = title_bar_icon_rect(WindowType::Normal, window_rect, palette);
    auto titlebar_inner_rect = title_bar_text_rect(WindowType::Normal, window_rect, palette);
    auto titlebar_title_rect = titlebar_inner_rect;
    titlebar_title_rect.set_width(Font::default_bold_font().width(title_text));

    auto [title_color, border_color, border_color2, stripes_color, shadow_color] = compute_frame_colors(window_state, palette);

    painter.draw_line(titlebar_rect.bottom_left().translated(0, 1), titlebar_rect.bottom_right().translated(0, 1), palette.button());
    painter.draw_line(titlebar_rect.bottom_left().translated(0, 2), titlebar_rect.bottom_right().translated(0, 2), palette.button());

    painter.fill_rect_with_gradient(titlebar_rect, border_color, border_color2);

    int stripe_left = titlebar_title_rect.right() + 4;
    int stripe_right = leftmost_button_rect.left() - 3;
    if (stripe_left && stripe_right && stripe_left < stripe_right) {
        for (int i = 2; i <= titlebar_inner_rect.height() - 2; i += 2) {
            painter.draw_line({ stripe_left, titlebar_inner_rect.y() + i }, { stripe_right, titlebar_inner_rect.y() + i }, stripes_color);
        }
    }

    auto clipped_title_rect = titlebar_title_rect;
    clipped_title_rect.set_width(stripe_right - clipped_title_rect.x());
    if (!clipped_title_rect.is_empty()) {
        painter.draw_text(clipped_title_rect.translated(1, 2), title_text, title_font, Gfx::TextAlignment::CenterLeft, shadow_color, Gfx::TextElision::Right);
        // FIXME: The translated(0, 1) wouldn't be necessary if we could center text based on its baseline.
        painter.draw_text(clipped_title_rect.translated(0, 1), title_text, title_font, Gfx::TextAlignment::CenterLeft, title_color, Gfx::TextElision::Right);
    }

    painter.blit(titlebar_icon_rect.location(), icon, icon.rect());
}

IntRect ClassicWindowTheme::title_bar_rect(WindowType window_type, const IntRect& window_rect, const Palette& palette) const
{
    auto& title_font = Font::default_bold_font();
    auto window_titlebar_height = palette.window_title_height();
    // FIXME: The top of the titlebar doesn't get redrawn properly if this padding is different
    int total_vertical_padding = title_font.glyph_height() - 1;

    if (window_type == WindowType::Notification)
        return { window_rect.width() + 3, total_vertical_padding / 2 - 1, window_titlebar_height, window_rect.height() };
    return { 4, total_vertical_padding / 2, window_rect.width(), window_titlebar_height };
}

ClassicWindowTheme::FrameColors ClassicWindowTheme::compute_frame_colors(WindowState state, const Palette& palette) const
{
    switch (state) {
    case WindowState::Highlighted:
        return { palette.highlight_window_title(), palette.highlight_window_border1(), palette.highlight_window_border2(), palette.highlight_window_title_stripes(), palette.highlight_window_title_shadow() };
    case WindowState::Moving:
        return { palette.moving_window_title(), palette.moving_window_border1(), palette.moving_window_border2(), palette.moving_window_title_stripes(), palette.moving_window_title_shadow() };
    case WindowState::Active:
        return { palette.active_window_title(), palette.active_window_border1(), palette.active_window_border2(), palette.active_window_title_stripes(), palette.active_window_title_shadow() };
    case WindowState::Inactive:
        return { palette.inactive_window_title(), palette.inactive_window_border1(), palette.inactive_window_border2(), palette.inactive_window_title_stripes(), palette.inactive_window_title_shadow() };
    default:
        ASSERT_NOT_REACHED();
    }
}

void ClassicWindowTheme::paint_notification_frame(Painter& painter, const IntRect& outer_rect, const IntRect& window_rect, const Palette& palette, const IntRect& close_button_rect) const
{
    Gfx::StylePainter::paint_window_frame(painter, outer_rect, palette);

    auto titlebar_rect = title_bar_rect(WindowType::Notification, window_rect, palette);
    painter.fill_rect_with_gradient(Gfx::Orientation::Vertical, titlebar_rect, palette.active_window_border1(), palette.active_window_border2());

    int stripe_top = close_button_rect.bottom() + 4;
    int stripe_bottom = window_rect.height() - 3;
    if (stripe_top && stripe_bottom && stripe_top < stripe_bottom) {
        for (int i = 2; i <= palette.window_title_height() - 2; i += 2) {
            painter.draw_line({ titlebar_rect.x() + i, stripe_top }, { titlebar_rect.x() + i, stripe_bottom }, palette.active_window_title_stripes());
        }
    }

}

}
