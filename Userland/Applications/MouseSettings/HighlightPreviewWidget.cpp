/*
 * Copyright (c) 2022, MacDue <macdue@dueutil.tech>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "HighlightPreviewWidget.h"
#include <AK/ByteString.h>
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
    auto theme_path = ByteString::formatted("/res/cursor-themes/{}/{}", cursor_theme, "Config.ini");
    auto cursor_theme_config = TRY(Core::ConfigFile::open(theme_path));
    auto load_bitmap = [](StringView path, StringView default_path) {
        auto maybe_bitmap = Gfx::Bitmap::load_from_file(path);
        if (!maybe_bitmap.is_error())
            return maybe_bitmap;
        return Gfx::Bitmap::load_from_file(default_path);
    };
    constexpr auto default_cursor_path = "/res/cursor-themes/Default/arrow.x2y2.png"sv;
    auto cursor_path = ByteString::formatted("/res/cursor-themes/{}/{}",
        cursor_theme, cursor_theme_config->read_entry("Cursor", "Arrow"));
    m_cursor_bitmap = TRY(load_bitmap(cursor_path, default_cursor_path));
    m_cursor_params = Gfx::CursorParams::parse_from_filename(cursor_path, m_cursor_bitmap->rect().center()).constrained(*m_cursor_bitmap);
    // Setup cursor animation:
    if (m_cursor_params.frames() > 1 && m_cursor_params.frame_ms() > 0) {
        m_frame_timer = Core::Timer::create_repeating(m_cursor_params.frame_ms(), [&] {
            m_cursor_frame = (m_cursor_frame + 1) % m_cursor_params.frames();
            update();
        });
        m_frame_timer->start();
    } else {
        m_frame_timer = nullptr;
    }
    return {};
}

void HighlightPreviewWidget::paint_preview(GUI::PaintEvent&)
{
    GUI::Painter painter(*this);
    Gfx::AntiAliasingPainter aa_painter { painter };
    Gfx::IntRect highlight_rect { 0, 0, m_radius * 2, m_radius * 2 };
    highlight_rect.center_within(frame_inner_rect());
    aa_painter.fill_ellipse(highlight_rect, m_color);
    if (m_cursor_bitmap) {
        auto cursor_rect = m_cursor_bitmap->rect();
        if (m_cursor_params.frames() > 1)
            cursor_rect.set_width(cursor_rect.width() / m_cursor_params.frames());
        painter.blit(cursor_rect.centered_within(frame_inner_rect()).location(), *m_cursor_bitmap, cursor_rect.translated(m_cursor_frame * cursor_rect.width(), 0));
    }
}

}
