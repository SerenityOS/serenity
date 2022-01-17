/*
 * Copyright (c) 2021, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Overlays.h"
#include "Compositor.h"
#include "WindowManager.h"
#include <LibGfx/StylePainter.h>

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
    auto previous_rect = m_rect;
    m_rect = rect;
    invalidate();
    if (is_enabled())
        Compositor::the().overlay_rects_changed();
    rect_changed(previous_rect);
}

BitmapOverlay::BitmapOverlay()
{
    clear_bitmaps();
}

void BitmapOverlay::rect_changed(Gfx::IntRect const& previous_rect)
{
    if (rect().size() != previous_rect.size())
        clear_bitmaps();
    Overlay::rect_changed(previous_rect);
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

void RectangularOverlay::rect_changed(Gfx::IntRect const& previous_rect)
{
    if (rect().size() != previous_rect.size())
        clear_bitmaps();
}

void RectangularOverlay::clear_bitmaps()
{
    m_rendered_bitmaps = MultiScaleBitmaps::create_empty();
}

void RectangularOverlay::render(Gfx::Painter& painter, Screen const& screen)
{
    if (m_content_invalidated) {
        clear_bitmaps();
        m_content_invalidated = false;
    }
    auto scale_factor = screen.scale_factor();
    auto* bitmap = m_rendered_bitmaps->find_bitmap(scale_factor);
    if (!bitmap) {
        auto bitmap_or_error = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, rect().size(), scale_factor);
        if (bitmap_or_error.is_error())
            return;
        auto new_bitmap = bitmap_or_error.release_value_but_fixme_should_propagate_errors();
        bitmap = new_bitmap.ptr();

        Gfx::Painter bitmap_painter(*new_bitmap);
        if (auto* shadow_bitmap = WindowManager::the().overlay_rect_shadow()) {
            Gfx::StylePainter::paint_simple_rect_shadow(bitmap_painter, new_bitmap->rect(), shadow_bitmap->bitmap(scale_factor), true, true);
        } else {
            bitmap_painter.fill_rect(new_bitmap->rect(), Color(Color::Black).with_alpha(0xcc));
        }
        render_overlay_bitmap(bitmap_painter);
        m_rendered_bitmaps->add_bitmap(scale_factor, move(new_bitmap));
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

void RectangularOverlay::invalidate_content()
{
    m_content_invalidated = true;
    invalidate();
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
            for (unsigned ch = '0'; ch <= '9'; ch++) {
                if (!font.contains_glyph(ch)) {
                    // Skip this font, it doesn't have glyphs for digits
                    return;
                }
            }
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

        auto rect = calculate_frame_rect(m_label_rect).centered_within(window->frame().rect());
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
    invalidate_content();
}

DndOverlay::DndOverlay(String const& text, Gfx::Bitmap const* bitmap)
    : m_bitmap(bitmap)
    , m_text(text)
{
    update_rect();
}

Gfx::Font const& DndOverlay::font()
{
    return WindowManager::the().font();
}

void DndOverlay::update_rect()
{
    int bitmap_width = m_bitmap ? m_bitmap->width() : 0;
    int bitmap_height = m_bitmap ? m_bitmap->height() : 0;
    auto& font = this->font();
    int width = font.width(m_text) + bitmap_width;
    int height = max((int)font.glyph_height(), bitmap_height);
    auto location = Compositor::the().current_cursor_rect().center().translated(8, 8);
    set_rect(Gfx::IntRect(location, { width, height }).inflated(16, 8));
}

RefPtr<Gfx::Bitmap> DndOverlay::create_bitmap(int scale_factor)
{
    auto bitmap_or_error = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, rect().size(), scale_factor);
    if (bitmap_or_error.is_error())
        return {};
    auto new_bitmap = bitmap_or_error.release_value_but_fixme_should_propagate_errors();

    auto& wm = WindowManager::the();
    Gfx::Painter bitmap_painter(*new_bitmap);
    auto bitmap_rect = new_bitmap->rect();
    bitmap_painter.fill_rect(bitmap_rect, wm.palette().selection().with_alpha(200));
    bitmap_painter.draw_rect(bitmap_rect, wm.palette().selection());
    if (!m_text.is_empty()) {
        auto text_rect = bitmap_rect;
        if (m_bitmap)
            text_rect.translate_by(m_bitmap->width() + 8, 0);
        bitmap_painter.draw_text(text_rect, m_text, Gfx::TextAlignment::CenterLeft, wm.palette().selection_text());
    }
    if (m_bitmap)
        bitmap_painter.blit(bitmap_rect.top_left().translated(4, 4), *m_bitmap, m_bitmap->rect());
    return new_bitmap;
}

void WindowStackSwitchOverlay::render_overlay_bitmap(Gfx::Painter& painter)
{
    // We should come up with a more elegant way to get the content rectangle
    auto content_rect = Gfx::IntRect({}, m_content_size).centered_within({ {}, rect().size() });
    auto active_color = WindowManager::the().palette().active_window_border1();
    auto inactive_color = WindowManager::the().palette().inactive_window_border1();
    for (int y = 0; y < m_rows; y++) {
        for (int x = 0; x < m_columns; x++) {
            Gfx::IntRect rect {
                content_rect.left() + x * (default_screen_rect_width + default_screen_rect_padding),
                content_rect.top() + y * (default_screen_rect_height + default_screen_rect_padding),
                default_screen_rect_width,
                default_screen_rect_height
            };
            bool is_target = y == m_target_row && x == m_target_column;
            painter.fill_rect(rect, is_target ? active_color : inactive_color);
        }
    }
}

WindowStackSwitchOverlay::WindowStackSwitchOverlay(Screen& screen, WindowStack& target_window_stack)
    : m_rows((int)WindowManager::the().window_stack_rows())
    , m_columns((int)WindowManager::the().window_stack_columns())
    , m_target_row((int)target_window_stack.row())
    , m_target_column((int)target_window_stack.column())
{
    m_content_size = {
        m_columns * (default_screen_rect_width + default_screen_rect_padding) - default_screen_rect_padding,
        m_rows * (default_screen_rect_height + default_screen_rect_padding) - default_screen_rect_padding,
    };
    set_rect(calculate_frame_rect(Gfx::IntRect({}, m_content_size).inflated(2 * default_screen_rect_margin, 2 * default_screen_rect_margin)).centered_within(screen.rect()));
}

}
