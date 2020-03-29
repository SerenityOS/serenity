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

//#define DEBUG_MODE

MonitorWidget::MonitorWidget()
{
    m_monitor_bitmap = Gfx::Bitmap::load_from_file("/res/monitor.png");
    m_monitor_rect = { 8, 9, 320, 180 };
}

void MonitorWidget::set_wallpaper(String path)
{
    m_desktop_wallpaper_path = path;
    m_desktop_wallpaper_bitmap = Gfx::Bitmap::load_from_file(path);
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

void MonitorWidget::set_desktop_resolution(Gfx::Size resolution)
{
    m_desktop_resolution = resolution;
}

Gfx::Size MonitorWidget::desktop_resolution()
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
#ifdef DEBUG_MODE
    dbg() << "Paint event fired."
          << " Color:" << m_desktop_color.to_string() << "."
          << " Resolution:" << m_desktop_resolution.to_string() << "."
          << " Wallpaper:" << m_desktop_wallpaper_path << ".";
#endif

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    painter.blit({ 0, 0 }, *m_monitor_bitmap, m_monitor_bitmap->rect());

    painter.fill_rect(m_monitor_rect, m_desktop_color);

    if (!m_desktop_wallpaper_bitmap.is_null()) {
        if (m_desktop_wallpaper_mode == "simple") {
            painter.blit({ 8, 9 }, *m_desktop_wallpaper_bitmap, { 88, 51, 200, 150 });
        } else if (m_desktop_wallpaper_mode == "center") {
            painter.draw_scaled_bitmap({ 88, 51, 160, 90 }, *m_desktop_wallpaper_bitmap, m_desktop_wallpaper_bitmap->rect());
        } else if (m_desktop_wallpaper_mode == "tile") {
            painter.draw_tiled_bitmap(m_monitor_rect, *m_desktop_wallpaper_bitmap);
        } else if (m_desktop_wallpaper_mode == "scaled") {
            painter.draw_scaled_bitmap(m_monitor_rect, *m_desktop_wallpaper_bitmap, m_desktop_wallpaper_bitmap->rect());
        } else {
            ASSERT_NOT_REACHED();
        }
    }

    if (!m_desktop_resolution.is_null())
        painter.draw_text(m_monitor_rect, m_desktop_resolution.to_string(), Gfx::TextAlignment::Center);
}
