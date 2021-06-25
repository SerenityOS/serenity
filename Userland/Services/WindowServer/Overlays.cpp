/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Overlays.h"
#include "Compositor.h"
#include "WindowManager.h"

namespace WindowServer {

Overlay::~Overlay()
{
    Compositor::the().remove_overlay(*this);
}

bool Overlay::invalidate()
{
    if (m_invalidated)
        return false;
    m_invalidated = true;
    // m_current_rect should only get updated by recompute_overlay_rects()
    if (!m_current_rect.is_empty())
        Compositor::the().invalidate_screen(m_current_rect);
    return true;
}

void Overlay::set_enabled(bool enable)
{
    if (is_enabled() == enable)
        return;

    if (enable)
        Compositor::the().add_overlay(*this);
    else
        Compositor::the().remove_overlay(*this);
}

void Overlay::set_rect(Gfx::IntRect const& rect)
{
    if (m_rect == rect)
        return;
    m_rect = rect;
    invalidate();
    if (is_enabled())
        Compositor::the().overlay_rects_changed();
    rect_changed();
}

BitmapOverlay::BitmapOverlay()
{
    clear_bitmaps();
}

void BitmapOverlay::rect_changed()
{
    clear_bitmaps();
    Overlay::rect_changed();
}

void BitmapOverlay::clear_bitmaps()
{
    m_bitmaps = MultiScaleBitmaps::create_empty();
}

void BitmapOverlay::render(Gfx::Painter& painter, Screen const& screen)
{
    auto scale_factor = screen.scale_factor();
    auto* bitmap = m_bitmaps->find_bitmap(scale_factor);
    if (!bitmap) {
        auto new_bitmap = create_bitmap(scale_factor);
        if (!new_bitmap)
            return;
        bitmap = new_bitmap.ptr();
        m_bitmaps->add_bitmap(scale_factor, new_bitmap.release_nonnull());
    }

    painter.blit({}, *bitmap, bitmap->rect());
}

RectangularOverlay::RectangularOverlay()
{
    clear_bitmaps();
}

void RectangularOverlay::rect_changed()
{
    clear_bitmaps();
}

void RectangularOverlay::clear_bitmaps()
{
    m_rendered_bitmaps = MultiScaleBitmaps::create_empty();
}

void RectangularOverlay::render(Gfx::Painter& painter, Screen const& screen)
{
    auto scale_factor = screen.scale_factor();
    auto* bitmap = m_rendered_bitmaps->find_bitmap(scale_factor);
    if (!bitmap) {
        auto new_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, rect().size(), scale_factor);
        if (!new_bitmap)
            return;
        bitmap = new_bitmap.ptr();

        Gfx::Painter bitmap_painter(*new_bitmap);
        if (auto* shadow_bitmap = WindowManager::the().overlay_rect_shadow()) {
            WindowFrame::paint_simple_rect_shadow(bitmap_painter, new_bitmap->rect(), shadow_bitmap->bitmap(scale_factor), true, true);
        } else {
            bitmap_painter.fill_rect(new_bitmap->rect(), Color(Color::Black).with_alpha(0xcc));
        }
        render_overlay_bitmap(bitmap_painter);
        m_rendered_bitmaps->add_bitmap(scale_factor, new_bitmap.release_nonnull());
    }

    painter.blit({}, *bitmap, bitmap->rect());
}

Gfx::IntRect RectangularOverlay::calculate_frame_rect(Gfx::IntRect const& rect)
{
    if (auto* shadow_bitmap = WindowManager::the().overlay_rect_shadow()) {
        Gfx::IntSize size;
        int base_size = shadow_bitmap->default_bitmap().height() / 2;
        size = { base_size, base_size };
        return rect.inflated(2 * base_size, 2 * base_size);
    }
    return rect.inflated(2 * default_frame_thickness, 2 * default_frame_thickness);
}

void RectangularOverlay::set_content_rect(Gfx::IntRect const& rect)
{
    set_rect(calculate_frame_rect(rect));
}

Gfx::Font const* ScreenNumberOverlay::s_font { nullptr };

ScreenNumberOverlay::ScreenNumberOverlay(Screen& screen)
    : m_screen(screen)
{
    if (!s_font)
        pick_font();

    Gfx::IntRect rect {
        default_offset,
        default_offset,
        default_size,
        default_size
    };
    rect.translate_by(screen.rect().location());
    set_rect(rect);
}

void ScreenNumberOverlay::pick_font()
{
    auto screen_number_content_rect_size = calculate_content_rect_for_screen(Screen::main()).size();
    auto& font_database = Gfx::FontDatabase::the();
    auto& default_font = WindowManager::the().font();
    String best_font_name;
    int best_font_size = -1;
    font_database.for_each_font([&](Gfx::Font const& font) {
        // TODO: instead of picking *any* font we should probably compare font.name()
        // with default_font.name(). But the default font currently does not provide larger sizes
        auto size = font.glyph_height();
        if (size * 2 <= screen_number_content_rect_size.height() && size > best_font_size) {
            best_font_name = font.qualified_name();
            best_font_size = size;
        }
    });

    if (auto best_font = font_database.get_by_name(best_font_name)) {
        s_font = best_font.ptr();
    } else {
        s_font = &default_font;
    }

    Compositor::the().for_each_overlay([&](auto& overlay) {
        if (overlay.zorder() == ZOrder::ScreenNumber)
            overlay.invalidate();
        return IterationDecision::Continue;
    });
}

Gfx::Font const& ScreenNumberOverlay::font()
{
    if (!s_font) {
        pick_font();
        VERIFY(s_font);
    }
    return *s_font;
}

void ScreenNumberOverlay::render_overlay_bitmap(Gfx::Painter& painter)
{
    painter.draw_text({ {}, rect().size() }, String::formatted("{}", m_screen.index() + 1), font(), Gfx::TextAlignment::Center, Color::White);
}

Gfx::IntRect ScreenNumberOverlay::calculate_content_rect_for_screen(Screen& screen)
{
    Gfx::IntRect content_rect {
        screen.rect().location().translated(default_offset, default_offset),
        { default_size, default_size }
    };

    return calculate_frame_rect(content_rect);
}

WindowGeometryOverlay::WindowGeometryOverlay(Window& window)
    : m_window(window)
{
    update_rect();
}

void WindowGeometryOverlay::update_rect()
{
    if (auto* window = m_window.ptr()) {
        auto& wm = WindowManager::the();
        if (!window->size_increment().is_null()) {
            int width_steps = (window->width() - window->base_size().width()) / window->size_increment().width();
            int height_steps = (window->height() - window->base_size().height()) / window->size_increment().height();
            m_label = String::formatted("{} ({}x{})", window->rect(), width_steps, height_steps);
        } else {
            m_label = window->rect().to_string();
        }
        m_label_rect = Gfx::IntRect { 0, 0, wm.font().width(m_label) + 16, wm.font().glyph_height() + 10 };

        auto rect = calculate_frame_rect(m_label_rect);
        rect.center_within(window->frame().rect());
        auto desktop_rect = wm.desktop_rect(ScreenInput::the().cursor_location_screen());
        if (rect.left() < desktop_rect.left())
            rect.set_left(desktop_rect.left());
        if (rect.top() < desktop_rect.top())
            rect.set_top(desktop_rect.top());
        if (rect.right() > desktop_rect.right())
            rect.set_right_without_resize(desktop_rect.right());
        if (rect.bottom() > desktop_rect.bottom())
            rect.set_bottom_without_resize(desktop_rect.bottom());

        set_rect(rect);
    } else {
        set_enabled(false);
    }
}

void WindowGeometryOverlay::render_overlay_bitmap(Gfx::Painter& painter)
{
    painter.draw_text({ {}, rect().size() }, m_label, WindowManager::the().font(), Gfx::TextAlignment::Center, Color::White);
}

void WindowGeometryOverlay::window_rect_changed()
{
    update_rect();
    invalidate();
}

}
