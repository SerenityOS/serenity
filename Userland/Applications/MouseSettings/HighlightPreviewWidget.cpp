/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HighlightPreviewWidget.h"
#include <AK/String.h>
#include <LibCore/ConfigFile.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/Painter.h>
#include <LibGfx/AntiAliasingPainter.h>

namespace MouseSettings {

HighlightPreviewWidget::HighlightPreviewWidget(Gfx::Palette const& palette)
    : GUI::AbstractThemePreview(palette)
{
    (void)reload_cursor();
}

ErrorOr<void> HighlightPreviewWidget::reload_cursor()
{
    auto cursor_theme = GUI::ConnectionToWindowServer::the().get_cursor_theme();
    auto theme_path = String::formatted("/res/cursor-themes/{}/{}", cursor_theme, "Config.ini");
    auto cursor_theme_config = TRY(Core::ConfigFile::open(theme_path));
    auto load_bitmap = [](StringView path, StringView default_path) {
        auto maybe_bitmap = Gfx::Bitmap::try_load_from_file(path);
        if (!maybe_bitmap.is_error())
            return maybe_bitmap;
        return Gfx::Bitmap::try_load_from_file(default_path);
    };
    constexpr auto default_cursor_path = "/res/cursor-themes/Default/arrow.x2y2.png";
    auto cursor_path = String::formatted("/res/cursor-themes/{}/{}",
        cursor_theme, cursor_theme_config->read_entry("Cursor", "Arrow"));
    m_cursor_bitmap = TRY(load_bitmap(cursor_path, default_cursor_path));
    return {};
}

void HighlightPreviewWidget::paint_preview(GUI::PaintEvent&)
{
    GUI::Painter painter(*this);
    if (m_radius > 0 && m_color.alpha() > 0) {
        Gfx::AntiAliasingPainter aa_painter { painter };
        Gfx::IntRect highlight_rect { 0, 0, m_radius * 2, m_radius * 2 };
        highlight_rect.center_within(frame_inner_rect());
        aa_painter.fill_ellipse(highlight_rect, m_color);
    }
    if (m_cursor_bitmap)
        painter.blit(m_cursor_bitmap->rect().centered_within(frame_inner_rect()).location(), *m_cursor_bitmap, m_cursor_bitmap->rect());
}

}
