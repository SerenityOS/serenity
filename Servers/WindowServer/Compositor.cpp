/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
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

#include "Compositor.h"
#include "Event.h"
#include "EventLoop.h"
#include "Screen.h"
#include "Window.h"
#include "WindowManager.h"
#include <LibCore/Timer.h>
#include <LibGfx/Font.h>
#include <LibGfx/Painter.h>
#include <LibThread/BackgroundAction.h>

// #define COMPOSITOR_DEBUG

namespace WindowServer {

Compositor& Compositor::the()
{
    static Compositor s_the;
    return s_the;
}

WallpaperMode mode_to_enum(const String& name)
{
    if (name == "simple")
        return WallpaperMode::Simple;
    if (name == "tile")
        return WallpaperMode::Tile;
    if (name == "center")
        return WallpaperMode::Center;
    if (name == "scaled")
        return WallpaperMode::Scaled;
    return WallpaperMode::Simple;
}

Compositor::Compositor()
{
    m_compose_timer = Core::Timer::construct(this);
    m_immediate_compose_timer = Core::Timer::construct(this);

    m_screen_can_set_buffer = Screen::the().can_set_buffer();

    init_bitmaps();

    m_compose_timer->on_timeout = [&]() {
#if defined(COMPOSITOR_DEBUG)
        dbgprintf("Compositor: delayed frame callback: %d rects\n", m_dirty_rects.size());
#endif
        compose();
    };
    m_compose_timer->set_single_shot(true);
    m_compose_timer->set_interval(1000 / 60);
    m_immediate_compose_timer->on_timeout = [this]() {
#if defined(COMPOSITOR_DEBUG)
        dbgprintf("Compositor: immediate frame callback: %d rects\n", m_dirty_rects.size());
#endif
        compose();
    };
    m_immediate_compose_timer->set_single_shot(true);
    m_immediate_compose_timer->set_interval(0);
}

void Compositor::init_bitmaps()
{
    auto& screen = Screen::the();
    auto size = screen.size();

    m_front_bitmap = Gfx::Bitmap::create_wrapper(Gfx::BitmapFormat::RGB32, size, screen.pitch(), screen.scanline(0));

    if (m_screen_can_set_buffer)
        m_back_bitmap = Gfx::Bitmap::create_wrapper(Gfx::BitmapFormat::RGB32, size, screen.pitch(), screen.scanline(size.height()));
    else
        m_back_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::RGB32, size);

    m_front_painter = make<Gfx::Painter>(*m_front_bitmap);
    m_back_painter = make<Gfx::Painter>(*m_back_bitmap);

    m_buffers_are_flipped = false;

    invalidate();
}

void Compositor::compose()
{
    auto& wm = WindowManager::the();
    if (m_wallpaper_mode == WallpaperMode::Unchecked)
        m_wallpaper_mode = mode_to_enum(wm.wm_config()->read_entry("Background", "Mode", "simple"));
    auto& ws = Screen::the();

    auto dirty_rects = move(m_dirty_rects);

    if (dirty_rects.size() == 0) {
        // nothing dirtied since the last compose pass.
        return;
    }

    dirty_rects.add(Gfx::Rect::intersection(m_last_geometry_label_rect, Screen::the().rect()));
    dirty_rects.add(Gfx::Rect::intersection(m_last_cursor_rect, Screen::the().rect()));
    dirty_rects.add(Gfx::Rect::intersection(m_last_dnd_rect, Screen::the().rect()));
    dirty_rects.add(Gfx::Rect::intersection(current_cursor_rect(), Screen::the().rect()));
#ifdef DEBUG_COUNTERS
    dbgprintf("[WM] compose #%u (%u rects)\n", ++m_compose_count, dirty_rects.rects().size());
#endif

    auto any_dirty_rect_intersects_window = [&dirty_rects](const Window& window) {
        auto window_frame_rect = window.frame().rect();
        for (auto& dirty_rect : dirty_rects.rects()) {
            if (dirty_rect.intersects(window_frame_rect))
                return true;
        }
        return false;
    };

    // Paint the wallpaper.
    for (auto& dirty_rect : dirty_rects.rects()) {
        if (wm.any_opaque_window_contains_rect(dirty_rect))
            continue;
        // FIXME: If the wallpaper is opaque, no need to fill with color!
        m_back_painter->fill_rect(dirty_rect, wm.palette().desktop_background());
        if (m_wallpaper) {
            if (m_wallpaper_mode == WallpaperMode::Simple) {
                m_back_painter->blit(dirty_rect.location(), *m_wallpaper, dirty_rect);
            } else if (m_wallpaper_mode == WallpaperMode::Center) {
                Gfx::Point offset { ws.size().width() / 2 - m_wallpaper->size().width() / 2,
                    ws.size().height() / 2 - m_wallpaper->size().height() / 2 };
                m_back_painter->blit_offset(dirty_rect.location(), *m_wallpaper,
                    dirty_rect, offset);
            } else if (m_wallpaper_mode == WallpaperMode::Tile) {
                m_back_painter->draw_tiled_bitmap(dirty_rect, *m_wallpaper);
            } else if (m_wallpaper_mode == WallpaperMode::Scaled) {
                float hscale = (float)m_wallpaper->size().width() / (float)ws.size().width();
                float vscale = (float)m_wallpaper->size().height() / (float)ws.size().height();

                m_back_painter->blit_scaled(dirty_rect, *m_wallpaper, dirty_rect, hscale, vscale);
            } else {
                ASSERT_NOT_REACHED();
            }
        }
    }

    auto compose_window = [&](Window& window) -> IterationDecision {
        if (!any_dirty_rect_intersects_window(window))
            return IterationDecision::Continue;
        Gfx::PainterStateSaver saver(*m_back_painter);
        m_back_painter->add_clip_rect(window.frame().rect());
        RefPtr<Gfx::Bitmap> backing_store = window.backing_store();
        for (auto& dirty_rect : dirty_rects.rects()) {
            if (wm.any_opaque_window_above_this_one_contains_rect(window, dirty_rect))
                continue;
            Gfx::PainterStateSaver saver(*m_back_painter);
            m_back_painter->add_clip_rect(dirty_rect);
            if (!backing_store)
                m_back_painter->fill_rect(dirty_rect, wm.palette().window());
            if (!window.is_fullscreen())
                window.frame().paint(*m_back_painter);
            if (!backing_store)
                continue;

            // Decide where we would paint this window's backing store.
            // This is subtly different from widow.rect(), because window
            // size may be different from its backing store size. This
            // happens when the window has been resized and the client
            // has not yet attached a new backing store. In this case,
            // we want to try to blit the backing store at the same place
            // it was previously, and fill the rest of the window with its
            // background color.
            Gfx::Rect backing_rect;
            backing_rect.set_size(backing_store->size());
            switch (WindowManager::the().resize_direction_of_window(window)) {
            case ResizeDirection::None:
            case ResizeDirection::Right:
            case ResizeDirection::Down:
            case ResizeDirection::DownRight:
                backing_rect.set_location(window.rect().location());
                break;
            case ResizeDirection::Left:
            case ResizeDirection::Up:
            case ResizeDirection::UpLeft:
                backing_rect.set_right_without_resize(window.rect().right());
                backing_rect.set_bottom_without_resize(window.rect().bottom());
                break;
            case ResizeDirection::UpRight:
                backing_rect.set_left(window.rect().left());
                backing_rect.set_bottom_without_resize(window.rect().bottom());
                break;
            case ResizeDirection::DownLeft:
                backing_rect.set_right_without_resize(window.rect().right());
                backing_rect.set_top(window.rect().top());
                break;
            }

            Gfx::Rect dirty_rect_in_backing_coordinates = dirty_rect
                                                              .intersected(window.rect())
                                                              .intersected(backing_rect)
                                                              .translated(-backing_rect.location());

            if (dirty_rect_in_backing_coordinates.is_empty())
                continue;
            auto dst = backing_rect.location().translated(dirty_rect_in_backing_coordinates.location());

            m_back_painter->blit(dst, *backing_store, dirty_rect_in_backing_coordinates, window.opacity());
            for (auto background_rect : window.rect().shatter(backing_rect))
                m_back_painter->fill_rect(background_rect, wm.palette().window());
        }
        return IterationDecision::Continue;
    };

    // Paint the window stack.
    if (auto* fullscreen_window = wm.active_fullscreen_window()) {
        compose_window(*fullscreen_window);
    } else {
        wm.for_each_visible_window_from_back_to_front([&](Window& window) {
            return compose_window(window);
        });

        draw_geometry_label();
    }

    run_animations();

    draw_cursor();

    if (m_flash_flush) {
        for (auto& rect : dirty_rects.rects())
            m_front_painter->fill_rect(rect, Color::Yellow);
    }

    if (m_screen_can_set_buffer)
        flip_buffers();

    for (auto& r : dirty_rects.rects())
        flush(r);
}

void Compositor::flush(const Gfx::Rect& a_rect)
{
    auto rect = Gfx::Rect::intersection(a_rect, Screen::the().rect());

#ifdef DEBUG_COUNTERS
    dbgprintf("[WM] flush #%u (%d,%d %dx%d)\n", ++m_flush_count, rect.x(), rect.y(), rect.width(), rect.height());
#endif

    Gfx::RGBA32* front_ptr = m_front_bitmap->scanline(rect.y()) + rect.x();
    Gfx::RGBA32* back_ptr = m_back_bitmap->scanline(rect.y()) + rect.x();
    size_t pitch = m_back_bitmap->pitch();

    // NOTE: The meaning of a flush depends on whether we can flip buffers or not.
    //
    //       If flipping is supported, flushing means that we've flipped, and now we
    //       copy the changed bits from the front buffer to the back buffer, to keep
    //       them in sync.
    //
    //       If flipping is not supported, flushing means that we copy the changed
    //       rects from the backing bitmap to the display framebuffer.

    Gfx::RGBA32* to_ptr;
    const Gfx::RGBA32* from_ptr;

    if (m_screen_can_set_buffer) {
        to_ptr = back_ptr;
        from_ptr = front_ptr;
    } else {
        to_ptr = front_ptr;
        from_ptr = back_ptr;
    }

    for (int y = 0; y < rect.height(); ++y) {
        fast_u32_copy(to_ptr, from_ptr, rect.width());
        from_ptr = (const Gfx::RGBA32*)((const u8*)from_ptr + pitch);
        to_ptr = (Gfx::RGBA32*)((u8*)to_ptr + pitch);
    }
}

void Compositor::invalidate()
{
    m_dirty_rects.clear_with_capacity();
    invalidate(Screen::the().rect());
}

void Compositor::invalidate(const Gfx::Rect& a_rect)
{
    auto rect = Gfx::Rect::intersection(a_rect, Screen::the().rect());
    if (rect.is_empty())
        return;

    m_dirty_rects.add(rect);

    // We delay composition by a timer interval, but to not affect latency too
    // much, if a pending compose is not already scheduled, we also schedule an
    // immediate compose the next spin of the event loop.
    if (!m_compose_timer->is_active()) {
#if defined(COMPOSITOR_DEBUG)
        dbgprintf("Invalidated (starting immediate frame): %dx%d %dx%d\n", a_rect.x(), a_rect.y(), a_rect.width(), a_rect.height());
#endif
        m_compose_timer->start();
        m_immediate_compose_timer->start();
    } else {
#if defined(COMPOSITOR_DEBUG)
        dbgprintf("Invalidated (frame callback pending): %dx%d %dx%d\n", a_rect.x(), a_rect.y(), a_rect.width(), a_rect.height());
#endif
    }
}

bool Compositor::set_wallpaper(const String& path, Function<void(bool)>&& callback)
{
    LibThread::BackgroundAction<RefPtr<Gfx::Bitmap>>::create(
        [path] {
            return Gfx::Bitmap::load_from_file(path);
        },

        [this, path, callback = move(callback)](RefPtr<Gfx::Bitmap> bitmap) {
            if (!bitmap) {
                callback(false);
                return;
            }
            m_wallpaper_path = path;
            m_wallpaper = move(bitmap);
            invalidate();
            callback(true);
        });
    return true;
}

void Compositor::flip_buffers()
{
    ASSERT(m_screen_can_set_buffer);
    swap(m_front_bitmap, m_back_bitmap);
    swap(m_front_painter, m_back_painter);
    Screen::the().set_buffer(m_buffers_are_flipped ? 0 : 1);
    m_buffers_are_flipped = !m_buffers_are_flipped;
}

void Compositor::run_animations()
{
    static const int minimize_animation_steps = 10;

    WindowManager::the().for_each_window([&](Window& window) {
        if (window.in_minimize_animation()) {
            int animation_index = window.minimize_animation_index();

            auto from_rect = window.is_minimized() ? window.frame().rect() : window.taskbar_rect();
            auto to_rect = window.is_minimized() ? window.taskbar_rect() : window.frame().rect();

            float x_delta_per_step = (float)(from_rect.x() - to_rect.x()) / minimize_animation_steps;
            float y_delta_per_step = (float)(from_rect.y() - to_rect.y()) / minimize_animation_steps;
            float width_delta_per_step = (float)(from_rect.width() - to_rect.width()) / minimize_animation_steps;
            float height_delta_per_step = (float)(from_rect.height() - to_rect.height()) / minimize_animation_steps;

            Gfx::Rect rect {
                from_rect.x() - (int)(x_delta_per_step * animation_index),
                from_rect.y() - (int)(y_delta_per_step * animation_index),
                from_rect.width() - (int)(width_delta_per_step * animation_index),
                from_rect.height() - (int)(height_delta_per_step * animation_index)
            };

#ifdef MINIMIZE_ANIMATION_DEBUG
            dbg() << "Minimize animation from " << from_rect << " to " << to_rect << " frame# " << animation_index << " " << rect;
#endif

            m_back_painter->draw_rect(rect, Color::White);

            window.step_minimize_animation();
            if (window.minimize_animation_index() >= minimize_animation_steps)
                window.end_minimize_animation();

            invalidate(rect);
        }
        return IterationDecision::Continue;
    });
}

void Compositor::set_resolution(int desired_width, int desired_height)
{
    auto screen_rect = Screen::the().rect();
    if (screen_rect.width() == desired_width && screen_rect.height() == desired_height)
        return;

    // Make sure it's impossible to set an invalid resolution
    ASSERT(desired_width >= 640 && desired_height >= 480);
    Screen::the().set_resolution(desired_width, desired_height);
    init_bitmaps();
    compose();
}

Gfx::Rect Compositor::current_cursor_rect() const
{
    auto& wm = WindowManager::the();
    return { Screen::the().cursor_location().translated(-wm.active_cursor().hotspot()), wm.active_cursor().size() };
}

void Compositor::invalidate_cursor()
{
    auto& wm = WindowManager::the();
    if (wm.dnd_client())
        invalidate(wm.dnd_rect());
    invalidate(current_cursor_rect());
}

void Compositor::draw_geometry_label()
{
    auto& wm = WindowManager::the();
    auto* window_being_moved_or_resized = wm.m_move_window ? wm.m_move_window.ptr() : (wm.m_resize_window ? wm.m_resize_window.ptr() : nullptr);
    if (!window_being_moved_or_resized) {
        m_last_geometry_label_rect = {};
        return;
    }
    auto geometry_string = window_being_moved_or_resized->rect().to_string();
    if (!window_being_moved_or_resized->size_increment().is_null()) {
        int width_steps = (window_being_moved_or_resized->width() - window_being_moved_or_resized->base_size().width()) / window_being_moved_or_resized->size_increment().width();
        int height_steps = (window_being_moved_or_resized->height() - window_being_moved_or_resized->base_size().height()) / window_being_moved_or_resized->size_increment().height();
        geometry_string = String::format("%s (%dx%d)", geometry_string.characters(), width_steps, height_steps);
    }
    auto geometry_label_rect = Gfx::Rect { 0, 0, wm.font().width(geometry_string) + 16, wm.font().glyph_height() + 10 };
    geometry_label_rect.center_within(window_being_moved_or_resized->rect());
    m_back_painter->fill_rect(geometry_label_rect, Color::WarmGray);
    m_back_painter->draw_rect(geometry_label_rect, Color::DarkGray);
    m_back_painter->draw_text(geometry_label_rect, geometry_string, Gfx::TextAlignment::Center);
    m_last_geometry_label_rect = geometry_label_rect;
}

void Compositor::draw_cursor()
{
    auto& wm = WindowManager::the();
    Gfx::Rect cursor_rect = current_cursor_rect();
    m_back_painter->blit(cursor_rect.location(), wm.active_cursor().bitmap(), wm.active_cursor().rect());

    if (wm.dnd_client()) {
        auto dnd_rect = wm.dnd_rect();
        m_back_painter->fill_rect(dnd_rect, Color(110, 34, 9, 200));
        if (!wm.dnd_text().is_empty()) {
            auto text_rect = dnd_rect;
            if (wm.dnd_bitmap())
                text_rect.move_by(wm.dnd_bitmap()->width(), 0);
            m_back_painter->draw_text(text_rect, wm.dnd_text(), Gfx::TextAlignment::CenterLeft, Color::White);
        }
        if (wm.dnd_bitmap()) {
            m_back_painter->blit(dnd_rect.top_left(), *wm.dnd_bitmap(), wm.dnd_bitmap()->rect());
        }
        m_last_dnd_rect = dnd_rect;
    } else {
        m_last_dnd_rect = {};
    }
    m_last_cursor_rect = cursor_rect;
}

}
