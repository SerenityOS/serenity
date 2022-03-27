/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "ThemePreviewWidget.h"
#include <AK/Array.h>
#include <LibCore/File.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Painter.h>
#include <LibGfx/StylePainter.h>

namespace DisplaySettings {

ThemePreviewWidget::ThemePreviewWidget(Gfx::Palette const& palette)
    : GUI::AbstractThemePreview(palette)
{
    set_fixed_size(304, 201);
}

void ThemePreviewWidget::set_theme(String path)
{
    set_theme_from_file(*Core::File::open(path, Core::OpenMode::ReadOnly).release_value_but_fixme_should_propagate_errors());
}

void ThemePreviewWidget::paint_preview(GUI::PaintEvent&)
{
    GUI::Painter painter(*this);

    auto active_window_rect = rect().shrunken(48, 100).translated(4, 26);
    auto inactive_window_rect = active_window_rect.translated(-8, -32);
    auto message_box = active_window_rect.shrunken(100, 60);

    paint_window("Inactive Window", inactive_window_rect, Gfx::WindowTheme::WindowState::Inactive, inactive_window_icon());
    paint_window("Active Window", active_window_rect, Gfx::WindowTheme::WindowState::Active, active_window_icon());
    paint_window("Alert", message_box, Gfx::WindowTheme::WindowState::Highlighted, active_window_icon(), 1);

    auto draw_centered_button = [&](auto window_rect, auto text, int button_width, int button_height) {
        auto box_center = window_rect.center();
        Gfx::IntRect button_rect { box_center.x() - button_width / 2, box_center.y() - button_height / 2, button_width, button_height };
        Gfx::StylePainter::paint_button(
            painter, button_rect, preview_palette(), Gfx::ButtonStyle::Normal, false, false, false, true, false, false);
        painter.draw_text(button_rect, text, Gfx::TextAlignment::Center, preview_palette().color(foreground_role()), Gfx::TextElision::Right, Gfx::TextWrapping::DontWrap);
    };

    draw_centered_button(message_box, "Ok", 32, 16);
}

}
