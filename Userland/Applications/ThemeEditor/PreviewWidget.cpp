/*
 * Copyright (c) 2020, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021-2022, Sam Atkins <atkinssj@serenityos.org>
 * Copyright (c) 2021, Antonio Di Stefano <tonio9681@gmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "PreviewWidget.h"
#include "Previews/WindowPreview.h"

#include <AK/LexicalPath.h>
#include <AK/StringView.h>
#include <LibCore/MimeData.h>
#include <LibFileSystemAccessClient/Client.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/WindowTheme.h>

namespace ThemeEditor {

PreviewWidget::PreviewWidget()
    : GUI::AbstractThemePreview(GUI::Application::the()->palette())
{
    set_greedy_for_hits(true);
}

void PreviewWidget::palette_changed()
{
    auto& gallery = *find_descendant_of_type_named<Previews::WindowPreview>("window_preview");
    gallery.set_preview_palette(preview_palette());
    update_preview_window_locations();
}

void PreviewWidget::set_color_filter(OwnPtr<Gfx::ColorBlindnessFilter> color_filter)
{
    m_color_filter = move(color_filter);
    repaint();
}

void PreviewWidget::update_preview_window_locations()
{
    auto& palette = preview_palette();
    int window_title_height = palette.metric(Gfx::MetricRole::TitleHeight)
        + palette.metric(Gfx::MetricRole::BorderThickness);

    constexpr int inactive_offset_x = -20;
    int inactive_offset_y = -(window_title_height + 4);
    constexpr int hightlight_offset_x = 140;
    int hightlight_offset_y = window_title_height + 40;

    m_active_window_rect = Gfx::IntRect(0, 0, 320, 220);
    m_inactive_window_rect = m_active_window_rect.translated(inactive_offset_x, inactive_offset_y);
    m_highlight_window_rect = Gfx::IntRect(m_active_window_rect.location(), { 160, 70 }).translated(hightlight_offset_x, hightlight_offset_y);

    auto window_group = Array {
        Window { m_active_window_rect },
        Window { m_inactive_window_rect },
        Window { m_highlight_window_rect },
    };

    center_window_group_within(window_group, frame_inner_rect());

    auto& gallery = *find_descendant_of_type_named<Previews::WindowPreview>("window_preview");
    gallery.set_relative_rect(m_active_window_rect);
}

void PreviewWidget::paint_preview(GUI::PaintEvent&)
{
    paint_window("Inactive window"sv, m_inactive_window_rect, Gfx::WindowTheme::WindowState::Inactive, active_window_icon());
    paint_window("Active window"sv, m_active_window_rect, Gfx::WindowTheme::WindowState::Active, inactive_window_icon());
}

void PreviewWidget::paint_hightlight_window()
{
    GUI::Painter painter(*this);
    paint_window("Highlight window"sv, m_highlight_window_rect, Gfx::WindowTheme::WindowState::Highlighted, inactive_window_icon(), 1);
    auto button_rect = Gfx::IntRect(0, 0, 80, 22).centered_within(m_highlight_window_rect);
    Gfx::StylePainter::paint_button(painter, button_rect, preview_palette(), Gfx::ButtonStyle::Normal, false, false, false, true, false, false);
    painter.draw_text(button_rect, ":^)"sv, Gfx::TextAlignment::Center, preview_palette().color(foreground_role()), Gfx::TextElision::Right, Gfx::TextWrapping::DontWrap);
}

void PreviewWidget::second_paint_event(GUI::PaintEvent&)
{
    GUI::Painter painter(*this);

    paint_hightlight_window();

    if (!m_color_filter)
        return;

    auto& target = painter.target();
    auto bitmap_clone_or_error = target.clone();
    if (bitmap_clone_or_error.is_error())
        return;

    auto clone = bitmap_clone_or_error.release_value();
    auto rect = target.rect();

    m_color_filter->apply(target, rect, *clone, rect);
}

void PreviewWidget::resize_event(GUI::ResizeEvent&)
{
    update_preview_window_locations();
}

}
