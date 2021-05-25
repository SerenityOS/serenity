/*
 * Copyright (c) 2020, Hüseyin Aslıtürk <asliturk@hotmail.com>
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "MonitorWidget.h"
#include <LibGUI/Desktop.h>
#include <LibGUI/Painter.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Font.h>

REGISTER_WIDGET(DisplaySettings, MonitorWidget)

namespace DisplaySettings {

MonitorWidget::MonitorWidget()
{
    m_desktop_resolution = GUI::Desktop::the().rect().size();
    m_monitor_bitmap = Gfx::Bitmap::load_from_file("/res/graphics/monitor.png");
    m_desktop_bitmap = Gfx::Bitmap::create(m_monitor_bitmap->format(), { 280, 158 });
    m_monitor_rect = { { 12, 13 }, m_desktop_bitmap->size() };
    set_fixed_size(304, 201);
}

bool MonitorWidget::set_wallpaper(String path)
{
    if (path.is_empty())
        return false;
    auto bitmap = Gfx::Bitmap::load_from_file(path);
    if (!bitmap)
        return false;
    m_desktop_wallpaper_path = move(path);
    m_wallpaper_bitmap = move(bitmap);
    m_desktop_dirty = true;
    update();
    return true;
}

String MonitorWidget::wallpaper()
{
    return m_desktop_wallpaper_path;
}

void MonitorWidget::set_wallpaper_mode(String mode)
{
    if (m_desktop_wallpaper_mode == mode)
        return;
    m_desktop_wallpaper_mode = move(mode);
    m_desktop_dirty = true;
    update();
}

String MonitorWidget::wallpaper_mode()
{
    return m_desktop_wallpaper_mode;
}

void MonitorWidget::set_desktop_resolution(Gfx::IntSize resolution)
{
    if (m_desktop_resolution == resolution)
        return;
    m_desktop_resolution = resolution;
    m_desktop_dirty = true;
    update();
}

Gfx::IntSize MonitorWidget::desktop_resolution()
{
    return m_desktop_resolution;
}

void MonitorWidget::set_background_color(Gfx::Color color)
{
    if (m_desktop_color == color)
        return;
    m_desktop_color = color;
    m_desktop_dirty = true;
    update();
}

Gfx::Color MonitorWidget::background_color()
{
    return m_desktop_color;
}

void MonitorWidget::redraw_desktop_if_needed()
{
    if (!m_desktop_dirty)
        return;

    m_desktop_dirty = false;

    GUI::Painter painter(*m_desktop_bitmap);
    painter.fill_rect(m_desktop_bitmap->rect(), m_desktop_color);

    if (!m_wallpaper_bitmap)
        return;

    float sw = (float)m_desktop_bitmap->width() / (float)m_desktop_resolution.width();
    float sh = (float)m_desktop_bitmap->height() / (float)m_desktop_resolution.height();

    auto scaled_size = m_wallpaper_bitmap->size().to_type<float>().scaled_by(sw, sh).to_type<int>();
    auto scaled_bitmap = m_wallpaper_bitmap->scaled(sw, sh);

    if (m_desktop_wallpaper_mode == "center") {
        Gfx::IntRect centered_rect { {}, scaled_size };
        centered_rect.center_within(m_desktop_bitmap->rect());
        painter.blit(centered_rect.location(), *scaled_bitmap, scaled_bitmap->rect());
    } else if (m_desktop_wallpaper_mode == "tile") {
        painter.draw_tiled_bitmap(m_desktop_bitmap->rect(), *scaled_bitmap);
    } else if (m_desktop_wallpaper_mode == "stretch") {
        painter.draw_scaled_bitmap(m_desktop_bitmap->rect(), *m_wallpaper_bitmap, m_wallpaper_bitmap->rect());
    } else {
        VERIFY_NOT_REACHED();
    }
}

void MonitorWidget::paint_event(GUI::PaintEvent& event)
{
    redraw_desktop_if_needed();

    GUI::Painter painter(*this);
    painter.add_clip_rect(event.rect());

    painter.blit({ 0, 0 }, *m_monitor_bitmap, m_monitor_bitmap->rect());
    painter.blit(m_monitor_rect.location(), *m_desktop_bitmap, m_desktop_bitmap->rect());

#if 0
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
#endif
}

}
