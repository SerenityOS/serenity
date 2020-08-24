/*
 * Copyright (c) 2020-2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
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

#include "MonitorWidget.h"
#include <LibGUI/Painter.h>

MonitorWidget::MonitorWidget()
{
    m_monitor_bitmap = Gfx::Bitmap::load_from_file("/res/graphics/monitor.png");
    m_monitor_rect = { 8, 9, 320, 180 };
}

bool MonitorWidget::set_wallpaper(String path)
{
    m_desktop_wallpaper_path = path;
    auto bitmap_ptr = Gfx::Bitmap::load_from_file(path);
    if (!bitmap_ptr)
        return false;
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
    Gfx::IntRect screen_rect = { 0, 0, m_desktop_resolution.width(), m_desktop_resolution.height() };
    auto screen_bitmap = Gfx::Bitmap::create(m_monitor_bitmap->format(), m_desktop_resolution);
    GUI::Painter screen_painter(*screen_bitmap);
    screen_painter.fill_rect(screen_rect, m_desktop_color);

    if (!m_desktop_wallpaper_bitmap.is_null()) {
        if (m_desktop_wallpaper_mode == "simple") {
            screen_painter.blit({ 0, 0 }, *m_desktop_wallpaper_bitmap, m_desktop_wallpaper_bitmap->rect());
        } else if (m_desktop_wallpaper_mode == "center") {
            Gfx::IntPoint offset { screen_rect.width() / 2 - m_desktop_wallpaper_bitmap->size().width() / 2, screen_rect.height() / 2 - m_desktop_wallpaper_bitmap->size().height() / 2 };
            screen_painter.blit_offset(screen_rect.location(), *m_desktop_wallpaper_bitmap, screen_rect, offset);
        } else if (m_desktop_wallpaper_mode == "tile") {
            screen_painter.draw_tiled_bitmap(screen_bitmap->rect(), *m_desktop_wallpaper_bitmap);
        } else if (m_desktop_wallpaper_mode == "scaled") {
            screen_painter.draw_scaled_bitmap(screen_bitmap->rect(), *m_desktop_wallpaper_bitmap, m_desktop_wallpaper_bitmap->rect());
        } else {
            ASSERT_NOT_REACHED();
        }
    }

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    painter.blit({ 0, 0 }, *m_monitor_bitmap, m_monitor_bitmap->rect());
    painter.draw_scaled_bitmap(m_monitor_rect, *screen_bitmap, screen_bitmap->rect());

    if (!m_desktop_resolution.is_null())
        painter.draw_text(m_monitor_rect, m_desktop_resolution.to_string(), Gfx::TextAlignment::Center);
}
