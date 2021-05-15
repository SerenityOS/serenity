/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MonitorWidget.h"
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font.h>

namespace DisplaySettings {

MonitorWidget::MonitorWidget()
{
    m_monitor_bitmap = Gfx::Bitmap::load_from_file("/res/graphics/monitor.png");
    m_monitor_rect = { 8, 9, 320, 180 };
}

bool MonitorWidget::set_wallpaper(String path)
{
    auto bitmap_ptr = Gfx::Bitmap::load_from_file(path);
    if (!bitmap_ptr && !path.is_empty())
        return false;
    m_desktop_wallpaper_path = path;
    m_desktop_wallpaper_bitmap = bitmap_ptr;
    return true;
}

String MonitorWidget::wallpaper()
{
    return m_desktop_wallpaper_path;
}

void MonitorWidget::set_wallpaper_mode(String mode)
{
    m_desktop_wallpaper_mode = mode;
}

String MonitorWidget::wallpaper_mode()
{
    return m_desktop_wallpaper_mode;
}

void MonitorWidget::set_desktop_resolution(Gfx::IntSize resolution)
{
    m_desktop_resolution = resolution;
}

Gfx::IntSize MonitorWidget::desktop_resolution()
{
    return m_desktop_resolution;
}

void MonitorWidget::set_background_color(Gfx::Color color)
{
    m_desktop_color = color;
}

Gfx::Color MonitorWidget::background_color()
{
    return m_desktop_color;
}

void MonitorWidget::paint_event(GUI::PaintEvent& event)
{
    Gfx::IntRect screen_rect = { { 0, 0 }, m_desktop_resolution };
    auto screen_bitmap = Gfx::Bitmap::create(m_monitor_bitmap->format(), m_desktop_resolution);
    GUI::Painter screen_painter(*screen_bitmap);
    screen_painter.fill_rect(screen_rect, m_desktop_color);

    if (!m_desktop_wallpaper_bitmap.is_null()) {
        if (m_desktop_wallpaper_mode == "simple") {
            screen_painter.blit({ 0, 0 }, *m_desktop_wallpaper_bitmap, m_desktop_wallpaper_bitmap->rect());
        } else if (m_desktop_wallpaper_mode == "center") {
            Gfx::IntPoint offset { (screen_rect.width() - m_desktop_wallpaper_bitmap->width()) / 2, (screen_rect.height() - m_desktop_wallpaper_bitmap->height()) / 2 };
            screen_painter.blit_offset(screen_rect.location(), *m_desktop_wallpaper_bitmap, screen_rect, offset);
        } else if (m_desktop_wallpaper_mode == "tile") {
            screen_painter.draw_tiled_bitmap(screen_bitmap->rect(), *m_desktop_wallpaper_bitmap);
        } else if (m_desktop_wallpaper_mode == "stretch") {
            screen_painter.draw_scaled_bitmap(screen_bitmap->rect(), *m_desktop_wallpaper_bitmap, m_desktop_wallpaper_bitmap->rect());
        } else {
            VERIFY_NOT_REACHED();
        }
    }

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    painter.blit({ 0, 0 }, *m_monitor_bitmap, m_monitor_bitmap->rect());
    painter.draw_scaled_bitmap(m_monitor_rect, *screen_bitmap, screen_bitmap->rect());

    if (!m_desktop_resolution.is_null()) {
        auto displayed_resolution_string = Gfx::IntSize { m_desktop_resolution.width(), m_desktop_resolution.height() }.to_string();

        // Render text label scaled with scale factor to hint at its effect.
        // FIXME: Once bitmaps have intrinsic scale factors, we could create a bitmap with an intrinsic scale factor of m_desktop_scale_factor
        // and that should give us the same effect with less code.
        auto text_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, Gfx::IntSize { painter.font().width(displayed_resolution_string) + 1, painter.font().glyph_height() + 1 });
        GUI::Painter text_painter(*text_bitmap);
        text_painter.set_font(painter.font());

        text_painter.draw_text({}, displayed_resolution_string, Gfx::TextAlignment::BottomRight, Color::Black);
        text_painter.draw_text({}, displayed_resolution_string, Gfx::TextAlignment::TopLeft, Color::White);

        Gfx::IntRect text_rect = text_bitmap->rect();
        text_rect.set_width(text_rect.width() * m_desktop_scale_factor);
        text_rect.set_height(text_rect.height() * m_desktop_scale_factor);
        text_rect.center_within(m_monitor_rect);
        painter.draw_scaled_bitmap(text_rect, *text_bitmap, text_bitmap->rect());
    }
}

}
