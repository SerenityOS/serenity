/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ThemePreviewWidget.h"
#include <AK/Array.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Painter.h>
#include <LibGfx/StylePainter.h>

namespace DisplaySettings {

ThemePreviewWidget::ThemePreviewWidget(Gfx::Palette const& palette)
    : GUI::AbstractThemePreview(palette)
{
    set_fixed_size(304, 201);
}

ErrorOr<void> ThemePreviewWidget::set_theme(String path)
{
    auto config_file = TRY(Core::File::open(path.to_byte_string(), Core::File::OpenMode::Read));
    TRY(set_theme_from_file(path.to_byte_string(), move(config_file)));
    return {};
}

void ThemePreviewWidget::paint_preview(GUI::PaintEvent&)
{
    GUI::Painter painter(*this);

    auto active_window_rect = frame_inner_rect().shrunken(48, 100);
    auto inactive_window_rect = active_window_rect.translated(-8, -32);
    auto message_box = active_window_rect.shrunken(100, 60);

    Array window_group {
        Window { active_window_rect },
        Window { inactive_window_rect },
        Window { message_box }
    };
    center_window_group_within(window_group, frame_inner_rect());

    paint_window("Inactive Window"sv, inactive_window_rect, Gfx::WindowTheme::WindowState::Inactive, inactive_window_icon());
    paint_window("Active Window"sv, active_window_rect, Gfx::WindowTheme::WindowState::Active, active_window_icon());
    paint_window("Alert"sv, message_box, Gfx::WindowTheme::WindowState::Highlighted, active_window_icon(), 1);

    auto draw_centered_button = [&](auto window_rect, auto text, int button_width, int button_height) {
        Gfx::IntRect button_rect { 0, 0, button_width, button_height };
        button_rect.center_within(window_rect);
        Gfx::StylePainter::paint_button(painter, button_rect, preview_palette(), Gfx::ButtonStyle::Normal, false, false, false, true, false, false);
        painter.draw_text(button_rect, text, Gfx::TextAlignment::Center, preview_palette().color(foreground_role()), Gfx::TextElision::Right, Gfx::TextWrapping::DontWrap);
    };

    draw_centered_button(message_box, "Ok"sv, 32, 16);
}

}
