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
#include "ClientConnection.h"
#include "Event.h"
#include "EventLoop.h"
#include "Screen.h"
#include "Window.h"
#include "WindowManager.h"
#include <AK/Debug.h>
#include <AK/Memory.h>
#include <AK/ScopeGuard.h>
#include <LibCore/Timer.h>
#include <LibGfx/Font.h>
#include <LibGfx/Painter.h>
#include <LibGfx/StylePainter.h>
#include <LibThread/BackgroundAction.h>

namespace WindowServer {

Compositor& Compositor::the()
{
    static Compositor s_the;
    return s_the;
}

static WallpaperMode mode_to_enum(const String& name)
{
    if (name == "simple")
        return WallpaperMode::Simple;
    if (name == "tile")
        return WallpaperMode::Tile;
    if (name == "center")
        return WallpaperMode::Center;
    if (name == "stretch")
        return WallpaperMode::Stretch;
    return WallpaperMode::Simple;
}

Compositor::Compositor()
{
    m_display_link_notify_timer = add<Core::Timer>(
        1000 / 60, [this] {
            notify_display_links();
        });
    m_display_link_notify_timer->stop();

    m_compose_timer = Core::Timer::create_single_shot(
        1000 / 60,
        [this] {
            compose();
        },
        this);

    m_immediate_compose_timer = Core::Timer::create_single_shot(
        0,
        [this] {
            compose();
        },
        this);

    m_screen_can_set_buffer = Screen::the().can_set_buffer();
    init_bitmaps();
}

void Compositor::init_bitmaps()
{
    auto& screen = Screen::the();
    auto size = screen.size();

    m_front_bitmap = Gfx::Bitmap::create_wrapper(Gfx::BitmapFormat::RGB32, size, screen.scale_factor(), screen.pitch(), screen.scanline(0));
    m_front_painter = make<Gfx::Painter>(*m_front_bitmap);

    if (m_screen_can_set_buffer)
        m_back_bitmap = Gfx::Bitmap::create_wrapper(Gfx::BitmapFormat::RGB32, size, screen.scale_factor(), screen.pitch(), screen.scanline(screen.physical_height()));
    else
        m_back_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::RGB32, size, screen.scale_factor());
    m_back_painter = make<Gfx::Painter>(*m_back_bitmap);

    m_temp_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::RGB32, size, screen.scale_factor());
    m_temp_painter = make<Gfx::Painter>(*m_temp_bitmap);

    m_buffers_are_flipped = false;

    invalidate_screen();
}

void Compositor::did_construct_window_manager(Badge<WindowManager>)
{
    auto& wm = WindowManager::the();
    m_wallpaper_mode = mode_to_enum(wm.config()->read_entry("Background", "Mode", "simple"));
    m_custom_background_color = Color::from_string(wm.config()->read_entry("Background", "Color", ""));

    invalidate_screen();
    invalidate_occlusions();
    compose();
}

void Compositor::compose()
{
    auto& wm = WindowManager::the();
    auto& ws = Screen::the();

    {
        auto& current_cursor = wm.active_cursor();
        if (m_current_cursor != &current_cursor)
            change_cursor(&current_cursor);
    }

    if (!m_invalidated_any) {
        // nothing dirtied since the last compose pass.
        return;
    }

    if (m_occlusions_dirty) {
        m_occlusions_dirty = false;
        recompute_occlusions();
    }

    auto dirty_screen_rects = move(m_dirty_screen_rects);
    dirty_screen_rects.add(m_last_geometry_label_damage_rect.intersected(ws.rect()));
    dirty_screen_rects.add(m_last_dnd_rect.intersected(ws.rect()));
    if (m_invalidated_cursor) {
        if (wm.dnd_client())
            dirty_screen_rects.add(wm.dnd_rect().intersected(ws.rect()));
    }

    // Mark window regions as dirty that need to be re-rendered
    wm.for_each_visible_window_from_back_to_front([&](Window& window) {
        auto frame_rect = window.frame().rect();
        for (auto& dirty_rect : dirty_screen_rects.rects()) {
            auto invalidate_rect = dirty_rect.intersected(frame_rect);
            if (!invalidate_rect.is_empty()) {
                auto inner_rect_offset = window.rect().location() - frame_rect.location();
                invalidate_rect.move_by(-(frame_rect.location() + inner_rect_offset));
                window.invalidate_no_notify(invalidate_rect);
                m_invalidated_window = true;
            }
        }
        window.prepare_dirty_rects();
        return IterationDecision::Continue;
    });

    // Any windows above or below a given window that need to be re-rendered
    // also require us to re-render that window's intersecting area, regardless
    // of whether that window has any dirty rectangles
    wm.for_each_visible_window_from_back_to_front([&](Window& window) {
        auto& transparency_rects = window.transparency_rects();
        if (transparency_rects.is_empty())
            return IterationDecision::Continue;

        auto frame_rect = window.frame().rect();
        auto& dirty_rects = window.dirty_rects();
        wm.for_each_visible_window_from_back_to_front([&](Window& w) {
            if (&w == &window)
                return IterationDecision::Continue;
            auto frame_rect2 = w.frame().rect();
            if (!frame_rect2.intersects(frame_rect))
                return IterationDecision::Continue;
            transparency_rects.for_each_intersected(w.dirty_rects(), [&](const Gfx::IntRect& intersected_dirty) {
                dirty_rects.add(intersected_dirty);
                return IterationDecision::Continue;
            });
            return IterationDecision::Continue;
        });
        return IterationDecision::Continue;
    });

    Color background_color = wm.palette().desktop_background();
    if (m_custom_background_color.has_value())
        background_color = m_custom_background_color.value();

    if constexpr (COMPOSE_DEBUG) {
        dbgln("COMPOSE: invalidated: window: {} cursor: {}, any: {}", m_invalidated_window, m_invalidated_cursor, m_invalidated_any);
        for (auto& r : dirty_screen_rects.rects())
            dbgln("dirty screen: {}", r);
    }

    Gfx::DisjointRectSet flush_rects;
    Gfx::DisjointRectSet flush_transparent_rects;
    Gfx::DisjointRectSet flush_special_rects;
    auto cursor_rect = current_cursor_rect();
    bool need_to_draw_cursor = false;

    auto check_restore_cursor_back = [&](const Gfx::IntRect& rect) {
        if (!need_to_draw_cursor && rect.intersects(cursor_rect)) {
            // Restore what's behind the cursor if anything touches the area of the cursor
            need_to_draw_cursor = true;
            restore_cursor_back();
        }
    };

    auto prepare_rect = [&](const Gfx::IntRect& rect) {
        dbgln<COMPOSE_DEBUG>("    -> flush opaque: {}", rect);
        ASSERT(!flush_rects.intersects(rect));
        ASSERT(!flush_transparent_rects.intersects(rect));
        flush_rects.add(rect);
        check_restore_cursor_back(rect);
    };

    auto prepare_transparency_rect = [&](const Gfx::IntRect& rect) {
        dbgln<COMPOSE_DEBUG>("   -> flush transparent: {}", rect);
        ASSERT(!flush_rects.intersects(rect));
        bool have_rect = false;
        for (auto& r : flush_transparent_rects.rects()) {
            if (r == rect) {
                have_rect = true;
                break;
            }
        }

        if (!have_rect) {
            flush_transparent_rects.add(rect);
            check_restore_cursor_back(rect);
        }
    };

    if (!m_cursor_back_bitmap || m_invalidated_cursor)
        check_restore_cursor_back(cursor_rect);

    auto back_painter = *m_back_painter;
    auto temp_painter = *m_temp_painter;

    auto paint_wallpaper = [&](Gfx::Painter& painter, const Gfx::IntRect& rect) {
        // FIXME: If the wallpaper is opaque and covers the whole rect, no need to fill with color!
        painter.fill_rect(rect, background_color);
        if (m_wallpaper) {
            if (m_wallpaper_mode == WallpaperMode::Simple) {
                painter.blit(rect.location(), *m_wallpaper, rect);
            } else if (m_wallpaper_mode == WallpaperMode::Center) {
                Gfx::IntPoint offset { (ws.width() - m_wallpaper->width()) / 2, (ws.height() - m_wallpaper->height()) / 2 };
                painter.blit_offset(rect.location(), *m_wallpaper, rect, offset);
            } else if (m_wallpaper_mode == WallpaperMode::Tile) {
                painter.draw_tiled_bitmap(rect, *m_wallpaper);
            } else if (m_wallpaper_mode == WallpaperMode::Stretch) {
                float hscale = (float)m_wallpaper->width() / (float)ws.width();
                float vscale = (float)m_wallpaper->height() / (float)ws.height();

                // TODO: this may look ugly, we should scale to a backing bitmap and then blit
                auto src_rect = Gfx::FloatRect { rect.x() * hscale, rect.y() * vscale, rect.width() * hscale, rect.height() * vscale };
                painter.draw_scaled_bitmap(rect, *m_wallpaper, src_rect);
            } else {
                ASSERT_NOT_REACHED();
            }
        }
    };

    m_opaque_wallpaper_rects.for_each_intersected(dirty_screen_rects, [&](const Gfx::IntRect& render_rect) {
        dbgln<COMPOSE_DEBUG>("  render wallpaper opaque: {}", render_rect);
        prepare_rect(render_rect);
        paint_wallpaper(back_painter, render_rect);
        return IterationDecision::Continue;
    });

    auto compose_window = [&](Window& window) -> IterationDecision {
        auto frame_rect = window.frame().rect();
        if (!frame_rect.intersects(ws.rect()))
            return IterationDecision::Continue;
        auto frame_rects = frame_rect.shatter(window.rect());

        dbgln<COMPOSE_DEBUG>("  window {} frame rect: {}", window.title(), frame_rect);

        RefPtr<Gfx::Bitmap> backing_store = window.backing_store();
        auto compose_window_rect = [&](Gfx::Painter& painter, const Gfx::IntRect& rect) {
            if (!window.is_fullscreen()) {
                rect.for_each_intersected(frame_rects, [&](const Gfx::IntRect& intersected_rect) {
                    // TODO: Should optimize this to use a backing buffer
                    Gfx::PainterStateSaver saver(painter);
                    painter.add_clip_rect(intersected_rect);
                    dbgln<COMPOSE_DEBUG>("    render frame: {}", intersected_rect);
                    window.frame().paint(painter);
                    return IterationDecision::Continue;
                });
            }

            if (!backing_store) {
                if (window.is_opaque())
                    painter.fill_rect(window.rect().intersected(rect), wm.palette().window());
                return;
            }

            // Decide where we would paint this window's backing store.
            // This is subtly different from widow.rect(), because window
            // size may be different from its backing store size. This
            // happens when the window has been resized and the client
            // has not yet attached a new backing store. In this case,
            // we want to try to blit the backing store at the same place
            // it was previously, and fill the rest of the window with its
            // background color.
            Gfx::IntRect backing_rect;
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

            Gfx::IntRect dirty_rect_in_backing_coordinates = rect.intersected(window.rect())
                                                                 .intersected(backing_rect)
                                                                 .translated(-backing_rect.location());

            if (dirty_rect_in_backing_coordinates.is_empty())
                return;
            auto dst = backing_rect.location().translated(dirty_rect_in_backing_coordinates.location());

            if (window.client() && window.client()->is_unresponsive()) {
                painter.blit_filtered(dst, *backing_store, dirty_rect_in_backing_coordinates, [](Color src) {
                    return src.to_grayscale().darkened(0.75f);
                });
            } else {
                painter.blit(dst, *backing_store, dirty_rect_in_backing_coordinates, window.opacity());
            }

            if (window.is_opaque()) {
                for (auto background_rect : window.rect().shatter(backing_rect))
                    painter.fill_rect(background_rect, wm.palette().window());
            }
        };

        auto& dirty_rects = window.dirty_rects();

        if constexpr (COMPOSE_DEBUG) {
            for (auto& dirty_rect : dirty_rects.rects())
                dbgln("    dirty: {}", dirty_rect);
            for (auto& r : window.opaque_rects().rects())
                dbgln("    opaque: {}", r);
            for (auto& r : window.transparency_rects().rects())
                dbgln("    transparent: {}", r);
        }

        // Render opaque portions directly to the back buffer
        auto& opaque_rects = window.opaque_rects();
        if (!opaque_rects.is_empty()) {
            opaque_rects.for_each_intersected(dirty_rects, [&](const Gfx::IntRect& render_rect) {
                dbgln<COMPOSE_DEBUG>("    render opaque: {}", render_rect);

                prepare_rect(render_rect);
                Gfx::PainterStateSaver saver(back_painter);
                back_painter.add_clip_rect(render_rect);
                compose_window_rect(back_painter, render_rect);
                return IterationDecision::Continue;
            });
        }

        // Render the wallpaper for any transparency directly covering
        // the wallpaper
        auto& transparency_wallpaper_rects = window.transparency_wallpaper_rects();
        if (!transparency_wallpaper_rects.is_empty()) {
            transparency_wallpaper_rects.for_each_intersected(dirty_rects, [&](const Gfx::IntRect& render_rect) {
                dbgln<COMPOSE_DEBUG>("    render wallpaper: {}", render_rect);

                prepare_transparency_rect(render_rect);
                paint_wallpaper(temp_painter, render_rect);
                return IterationDecision::Continue;
            });
        }
        auto& transparency_rects = window.transparency_rects();
        if (!transparency_rects.is_empty()) {
            transparency_rects.for_each_intersected(dirty_rects, [&](const Gfx::IntRect& render_rect) {
                dbgln<COMPOSE_DEBUG>("    render transparent: {}", render_rect);

                prepare_transparency_rect(render_rect);
                Gfx::PainterStateSaver saver(temp_painter);
                temp_painter.add_clip_rect(render_rect);
                compose_window_rect(temp_painter, render_rect);
                return IterationDecision::Continue;
            });
        }
        return IterationDecision::Continue;
    };

    // Paint the window stack.
    if (m_invalidated_window) {
        if (auto* fullscreen_window = wm.active_fullscreen_window()) {
            compose_window(*fullscreen_window);
        } else {
            wm.for_each_visible_window_from_back_to_front([&](Window& window) {
                compose_window(window);
                window.clear_dirty_rects();
                return IterationDecision::Continue;
            });
        }

        // Check that there are no overlapping transparent and opaque flush rectangles
        ASSERT(![&]() {
            for (auto& rect_transparent : flush_transparent_rects.rects()) {
                for (auto& rect_opaque : flush_rects.rects()) {
                    if (rect_opaque.intersects(rect_transparent)) {
                        dbgln("Transparent rect {} overlaps opaque rect: {}: {}", rect_transparent, rect_opaque, rect_opaque.intersected(rect_transparent));
                        return true;
                    }
                }
            }
            return false;
        }());

        // Copy anything rendered to the temporary buffer to the back buffer
        for (auto& rect : flush_transparent_rects.rects())
            back_painter.blit(rect.location(), *m_temp_bitmap, rect);

        Gfx::IntRect geometry_label_damage_rect;
        if (draw_geometry_label(geometry_label_damage_rect))
            flush_special_rects.add(geometry_label_damage_rect);
    }

    m_invalidated_any = false;
    m_invalidated_window = false;
    m_invalidated_cursor = false;

    if (wm.dnd_client()) {
        auto dnd_rect = wm.dnd_rect();

        // TODO: render once into a backing bitmap, then just blit...
        auto render_dnd = [&]() {
            back_painter.fill_rect(dnd_rect, wm.palette().selection().with_alpha(200));
            back_painter.draw_rect(dnd_rect, wm.palette().selection());
            if (!wm.dnd_text().is_empty()) {
                auto text_rect = dnd_rect;
                if (wm.dnd_bitmap())
                    text_rect.move_by(wm.dnd_bitmap()->width() + 8, 0);
                back_painter.draw_text(text_rect, wm.dnd_text(), Gfx::TextAlignment::CenterLeft, wm.palette().selection_text());
            }
            if (wm.dnd_bitmap()) {
                back_painter.blit(dnd_rect.top_left().translated(4, 4), *wm.dnd_bitmap(), wm.dnd_bitmap()->rect());
            }
        };

        dirty_screen_rects.for_each_intersected(dnd_rect, [&](const Gfx::IntRect& render_rect) {
            Gfx::PainterStateSaver saver(back_painter);
            back_painter.add_clip_rect(render_rect);
            render_dnd();
            return IterationDecision::Continue;
        });
        flush_transparent_rects.for_each_intersected(dnd_rect, [&](const Gfx::IntRect& render_rect) {
            Gfx::PainterStateSaver saver(back_painter);
            back_painter.add_clip_rect(render_rect);
            render_dnd();
            return IterationDecision::Continue;
        });
        m_last_dnd_rect = dnd_rect;
    } else {
        if (!m_last_dnd_rect.is_empty()) {
            invalidate_screen(m_last_dnd_rect);
            m_last_dnd_rect = {};
        }
    }

    run_animations(flush_special_rects);

    if (need_to_draw_cursor) {
        flush_rects.add(cursor_rect);
        if (cursor_rect != m_last_cursor_rect)
            flush_rects.add(m_last_cursor_rect);
        draw_cursor(cursor_rect);
    }

    if (m_flash_flush) {
        for (auto& rect : flush_rects.rects())
            m_front_painter->fill_rect(rect, Color::Yellow);
    }

    if (m_screen_can_set_buffer)
        flip_buffers();

    for (auto& rect : flush_rects.rects())
        flush(rect);
    for (auto& rect : flush_transparent_rects.rects())
        flush(rect);
    for (auto& rect : flush_special_rects.rects())
        flush(rect);
}

void Compositor::flush(const Gfx::IntRect& a_rect)
{
    auto rect = Gfx::IntRect::intersection(a_rect, Screen::the().rect());

    // Almost everything in Compositor is in logical coordinates, with the painters having
    // a scale applied. But this routine accesses the backbuffer pixels directly, so it
    // must work in physical coordinates.
    rect = rect * Screen::the().scale_factor();
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

void Compositor::invalidate_screen()
{
    invalidate_screen(Screen::the().rect());
}

void Compositor::invalidate_screen(const Gfx::IntRect& screen_rect)
{
    m_dirty_screen_rects.add(screen_rect.intersected(Screen::the().rect()));

    if (m_invalidated_any)
        return;

    m_invalidated_any = true;
    m_invalidated_window = true;
    start_compose_async_timer();
}

void Compositor::invalidate_window()
{
    if (m_invalidated_window)
        return;
    m_invalidated_window = true;
    m_invalidated_any = true;

    start_compose_async_timer();
}

void Compositor::start_compose_async_timer()
{
    // We delay composition by a timer interval, but to not affect latency too
    // much, if a pending compose is not already scheduled, we also schedule an
    // immediate compose the next spin of the event loop.
    if (!m_compose_timer->is_active()) {
        m_compose_timer->start();
        m_immediate_compose_timer->start();
    }
}

bool Compositor::set_background_color(const String& background_color)
{
    auto color = Color::from_string(background_color);
    if (!color.has_value())
        return false;

    m_custom_background_color = color;

    auto& wm = WindowManager::the();
    wm.config()->write_entry("Background", "Color", background_color);
    bool ret_val = wm.config()->sync();

    if (ret_val)
        Compositor::invalidate_screen();

    return ret_val;
}

bool Compositor::set_wallpaper_mode(const String& mode)
{
    auto& wm = WindowManager::the();
    wm.config()->write_entry("Background", "Mode", mode);
    bool ret_val = wm.config()->sync();

    if (ret_val) {
        m_wallpaper_mode = mode_to_enum(mode);
        Compositor::invalidate_screen();
    }

    return ret_val;
}

bool Compositor::set_wallpaper(const String& path, Function<void(bool)>&& callback)
{
    LibThread::BackgroundAction<RefPtr<Gfx::Bitmap>>::create(
        [path] {
            return Gfx::Bitmap::load_from_file(path);
        },

        [this, path, callback = move(callback)](RefPtr<Gfx::Bitmap> bitmap) {
            m_wallpaper_path = path;
            m_wallpaper = move(bitmap);
            invalidate_screen();
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

void Compositor::run_animations(Gfx::DisjointRectSet& flush_rects)
{
    static const int minimize_animation_steps = 10;
    auto& painter = *m_back_painter;
    Gfx::PainterStateSaver saver(painter);
    painter.set_draw_op(Gfx::Painter::DrawOp::Invert);

    WindowManager::the().for_each_window([&](Window& window) {
        if (window.in_minimize_animation()) {
            int animation_index = window.minimize_animation_index();

            auto from_rect = window.is_minimized() ? window.frame().rect() : window.taskbar_rect();
            auto to_rect = window.is_minimized() ? window.taskbar_rect() : window.frame().rect();

            float x_delta_per_step = (float)(from_rect.x() - to_rect.x()) / minimize_animation_steps;
            float y_delta_per_step = (float)(from_rect.y() - to_rect.y()) / minimize_animation_steps;
            float width_delta_per_step = (float)(from_rect.width() - to_rect.width()) / minimize_animation_steps;
            float height_delta_per_step = (float)(from_rect.height() - to_rect.height()) / minimize_animation_steps;

            Gfx::IntRect rect {
                from_rect.x() - (int)(x_delta_per_step * animation_index),
                from_rect.y() - (int)(y_delta_per_step * animation_index),
                from_rect.width() - (int)(width_delta_per_step * animation_index),
                from_rect.height() - (int)(height_delta_per_step * animation_index)
            };

            dbgln<MINIMIZE_ANIMATION_DEBUG>("Minimize animation from {} to {} frame# {} {}", from_rect, to_rect, animation_index, rect);

            painter.draw_rect(rect, Color::Transparent); // Color doesn't matter, we draw inverted
            flush_rects.add(rect);
            invalidate_screen(rect);

            window.step_minimize_animation();
            if (window.minimize_animation_index() >= minimize_animation_steps)
                window.end_minimize_animation();
        }
        return IterationDecision::Continue;
    });
}

bool Compositor::set_resolution(int desired_width, int desired_height, int scale_factor)
{
    auto screen_rect = Screen::the().rect();
    if (screen_rect.width() == desired_width && screen_rect.height() == desired_height && Screen::the().scale_factor() == scale_factor)
        return true;

    // Make sure it's impossible to set an invalid resolution
    if (!(desired_width >= 640 && desired_height >= 480 && scale_factor >= 1)) {
        dbgln("Compositor: Tried to set invalid resolution: {}x{}", desired_width, desired_height);
        return false;
    }

    int old_scale_factor = Screen::the().scale_factor();
    bool success = Screen::the().set_resolution(desired_width, desired_height, scale_factor);
    if (success && old_scale_factor != scale_factor)
        WindowManager::the().reload_icon_bitmaps_after_scale_change();
    init_bitmaps();
    invalidate_occlusions();
    compose();
    return success;
}

Gfx::IntRect Compositor::current_cursor_rect() const
{
    auto& wm = WindowManager::the();
    auto& current_cursor = m_current_cursor ? *m_current_cursor : wm.active_cursor();
    return { Screen::the().cursor_location().translated(-current_cursor.params().hotspot()), current_cursor.size() };
}

void Compositor::invalidate_cursor(bool compose_immediately)
{
    if (m_invalidated_cursor)
        return;
    m_invalidated_cursor = true;
    m_invalidated_any = true;

    if (compose_immediately)
        compose();
    else
        start_compose_async_timer();
}

bool Compositor::draw_geometry_label(Gfx::IntRect& geometry_label_damage_rect)
{
    auto& wm = WindowManager::the();
    auto* window_being_moved_or_resized = wm.m_move_window ? wm.m_move_window.ptr() : (wm.m_resize_window ? wm.m_resize_window.ptr() : nullptr);
    if (!window_being_moved_or_resized) {
        m_last_geometry_label_damage_rect = {};
        return false;
    }
    auto geometry_string = window_being_moved_or_resized->rect().to_string();
    if (!window_being_moved_or_resized->size_increment().is_null()) {
        int width_steps = (window_being_moved_or_resized->width() - window_being_moved_or_resized->base_size().width()) / window_being_moved_or_resized->size_increment().width();
        int height_steps = (window_being_moved_or_resized->height() - window_being_moved_or_resized->base_size().height()) / window_being_moved_or_resized->size_increment().height();
        geometry_string = String::formatted("{} ({}x{})", geometry_string, width_steps, height_steps);
    }

    auto geometry_label_rect = Gfx::IntRect { 0, 0, wm.font().width(geometry_string) + 16, wm.font().glyph_height() + 10 };
    geometry_label_rect.center_within(window_being_moved_or_resized->rect());
    auto desktop_rect = wm.desktop_rect();
    if (geometry_label_rect.left() < desktop_rect.left())
        geometry_label_rect.set_left(desktop_rect.left());
    if (geometry_label_rect.top() < desktop_rect.top())
        geometry_label_rect.set_top(desktop_rect.top());
    if (geometry_label_rect.right() > desktop_rect.right())
        geometry_label_rect.set_right_without_resize(desktop_rect.right());
    if (geometry_label_rect.bottom() > desktop_rect.bottom())
        geometry_label_rect.set_bottom_without_resize(desktop_rect.bottom());

    auto& back_painter = *m_back_painter;
    back_painter.fill_rect(geometry_label_rect.translated(1, 1), Color(Color::Black).with_alpha(80));
    Gfx::StylePainter::paint_button(back_painter, geometry_label_rect.translated(-1, -1), wm.palette(), Gfx::ButtonStyle::Normal, false);
    back_painter.draw_text(geometry_label_rect.translated(-1, -1), geometry_string, Gfx::TextAlignment::Center, wm.palette().window_text());

    geometry_label_damage_rect = geometry_label_rect.inflated(2, 2);
    m_last_geometry_label_damage_rect = geometry_label_damage_rect;
    return true;
}

void Compositor::change_cursor(const Cursor* cursor)
{
    if (m_current_cursor == cursor)
        return;
    m_current_cursor = cursor;
    m_current_cursor_frame = 0;
    if (m_cursor_timer) {
        m_cursor_timer->stop();
        m_cursor_timer = nullptr;
    }
    if (cursor && cursor->params().frames() > 1 && cursor->params().frame_ms() != 0) {
        m_cursor_timer = add<Core::Timer>(
            cursor->params().frame_ms(), [this, cursor] {
                if (m_current_cursor != cursor)
                    return;
                auto frames = cursor->params().frames();
                if (++m_current_cursor_frame >= frames)
                    m_current_cursor_frame = 0;
                invalidate_cursor(true);
            });
    }
}

void Compositor::draw_cursor(const Gfx::IntRect& cursor_rect)
{
    auto& wm = WindowManager::the();

    if (!m_cursor_back_bitmap || m_cursor_back_bitmap->size() != cursor_rect.size() || m_cursor_back_bitmap->scale() != Screen::the().scale_factor()) {
        m_cursor_back_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::RGB32, cursor_rect.size(), Screen::the().scale_factor());
        m_cursor_back_painter = make<Gfx::Painter>(*m_cursor_back_bitmap);
    }

    auto& current_cursor = m_current_cursor ? *m_current_cursor : wm.active_cursor();
    m_cursor_back_painter->blit({ 0, 0 }, *m_back_bitmap, current_cursor.rect().translated(cursor_rect.location()).intersected(Screen::the().rect()));
    m_back_painter->blit(cursor_rect.location(), current_cursor.bitmap(), current_cursor.source_rect(m_current_cursor_frame));
    m_last_cursor_rect = cursor_rect;
}

void Compositor::restore_cursor_back()
{
    if (!m_cursor_back_bitmap || m_cursor_back_bitmap->scale() != m_back_bitmap->scale())
        return;

    auto last_cursor_rect = m_last_cursor_rect.intersected(Screen::the().rect());
    m_back_painter->blit(last_cursor_rect.location(), *m_cursor_back_bitmap, { { 0, 0 }, last_cursor_rect.size() });
}

void Compositor::notify_display_links()
{
    ClientConnection::for_each_client([](auto& client) {
        client.notify_display_link({});
    });
}

void Compositor::increment_display_link_count(Badge<ClientConnection>)
{
    ++m_display_link_count;
    if (m_display_link_count == 1)
        m_display_link_notify_timer->start();
}

void Compositor::decrement_display_link_count(Badge<ClientConnection>)
{
    ASSERT(m_display_link_count);
    --m_display_link_count;
    if (!m_display_link_count)
        m_display_link_notify_timer->stop();
}

bool Compositor::any_opaque_window_above_this_one_contains_rect(const Window& a_window, const Gfx::IntRect& rect)
{
    bool found_containing_window = false;
    bool checking = false;
    WindowManager::the().for_each_visible_window_from_back_to_front([&](Window& window) {
        if (&window == &a_window) {
            checking = true;
            return IterationDecision::Continue;
        }
        if (!checking)
            return IterationDecision::Continue;
        if (!window.is_visible())
            return IterationDecision::Continue;
        if (window.is_minimized())
            return IterationDecision::Continue;
        if (!window.is_opaque())
            return IterationDecision::Continue;
        if (window.frame().rect().contains(rect)) {
            found_containing_window = true;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return found_containing_window;
};

void Compositor::recompute_occlusions()
{
    auto& wm = WindowManager::the();
    wm.for_each_visible_window_from_back_to_front([&](Window& window) {
        if (wm.m_switcher.is_visible()) {
            window.set_occluded(false);
        } else {
            if (any_opaque_window_above_this_one_contains_rect(window, window.frame().rect()))
                window.set_occluded(true);
            else
                window.set_occluded(false);
        }
        return IterationDecision::Continue;
    });

#if OCCLUSIONS_DEBUG
    dbgln("OCCLUSIONS:");
#endif

    auto screen_rect = Screen::the().rect();

    if (auto* fullscreen_window = wm.active_fullscreen_window()) {
        WindowManager::the().for_each_visible_window_from_front_to_back([&](Window& w) {
            auto& visible_opaque = w.opaque_rects();
            auto& transparency_rects = w.transparency_rects();
            auto& transparency_wallpaper_rects = w.transparency_wallpaper_rects();
            if (&w == fullscreen_window) {
                if (w.is_opaque()) {
                    visible_opaque = screen_rect;
                    transparency_rects.clear();
                    transparency_wallpaper_rects.clear();
                } else {
                    visible_opaque.clear();
                    transparency_rects = screen_rect;
                    transparency_wallpaper_rects = screen_rect;
                }
            } else {
                visible_opaque.clear();
                transparency_rects.clear();
                transparency_wallpaper_rects.clear();
            }
            return IterationDecision::Continue;
        });

        m_opaque_wallpaper_rects.clear();
    } else {
        Gfx::DisjointRectSet visible_rects(screen_rect);
        bool have_transparent = false;
        WindowManager::the().for_each_visible_window_from_front_to_back([&](Window& w) {
            auto window_frame_rect = w.frame().rect().intersected(screen_rect);
            w.transparency_wallpaper_rects().clear();
            auto& visible_opaque = w.opaque_rects();
            auto& transparency_rects = w.transparency_rects();
            if (w.is_minimized() || window_frame_rect.is_empty()) {
                visible_opaque.clear();
                transparency_rects.clear();
                return IterationDecision::Continue;
            }

            Gfx::DisjointRectSet opaque_covering;
            if (w.is_opaque()) {
                visible_opaque = visible_rects.intersected(window_frame_rect);
                transparency_rects.clear();
            } else {
                visible_opaque.clear();
                transparency_rects = visible_rects.intersected(window_frame_rect);
            }

            bool found_this_window = false;
            WindowManager::the().for_each_visible_window_from_back_to_front([&](Window& w2) {
                if (!found_this_window) {
                    if (&w == &w2)
                        found_this_window = true;
                    return IterationDecision::Continue;
                }

                if (w2.is_minimized())
                    return IterationDecision::Continue;
                auto window_frame_rect2 = w2.frame().rect().intersected(screen_rect);
                auto covering_rect = window_frame_rect2.intersected(window_frame_rect);
                if (covering_rect.is_empty())
                    return IterationDecision::Continue;

                if (w2.is_opaque()) {
                    opaque_covering.add(covering_rect);
                    if (opaque_covering.contains(window_frame_rect)) {
                        // This window is entirely covered by another opaque window
                        visible_opaque.clear();
                        transparency_rects.clear();
                        return IterationDecision::Break;
                    }

                    if (!visible_opaque.is_empty()) {
                        auto uncovered_opaque = visible_opaque.shatter(covering_rect);
                        visible_opaque = move(uncovered_opaque);
                    }

                    if (!transparency_rects.is_empty()) {
                        auto uncovered_transparency = transparency_rects.shatter(covering_rect);
                        transparency_rects = move(uncovered_transparency);
                    }
                } else {
                    visible_rects.for_each_intersected(covering_rect, [&](const Gfx::IntRect& intersected) {
                        transparency_rects.add(intersected);
                        if (!visible_opaque.is_empty()) {
                            auto uncovered_opaque = visible_opaque.shatter(intersected);
                            visible_opaque = move(uncovered_opaque);
                        }
                        return IterationDecision::Continue;
                    });
                }

                return IterationDecision::Continue;
            });

            if (!transparency_rects.is_empty())
                have_transparent = true;

            ASSERT(!visible_opaque.intersects(transparency_rects));

            if (w.is_opaque()) {
                // Determine visible area for the window below
                auto visible_rects_below_window = visible_rects.shatter(window_frame_rect);
                visible_rects = move(visible_rects_below_window);
            }
            return IterationDecision::Continue;
        });

        if (have_transparent) {
            // Determine what transparent window areas need to render the wallpaper first
            WindowManager::the().for_each_visible_window_from_back_to_front([&](Window& w) {
                auto& transparency_wallpaper_rects = w.transparency_wallpaper_rects();
                if (w.is_opaque() || w.is_minimized()) {
                    transparency_wallpaper_rects.clear();
                    return IterationDecision::Continue;
                }
                Gfx::DisjointRectSet& transparency_rects = w.transparency_rects();
                if (transparency_rects.is_empty()) {
                    transparency_wallpaper_rects.clear();
                    return IterationDecision::Continue;
                }

                transparency_wallpaper_rects = visible_rects.intersected(transparency_rects);

                auto remaining_visible = visible_rects.shatter(transparency_wallpaper_rects);
                visible_rects = move(remaining_visible);
                return IterationDecision::Continue;
            });
        }

        m_opaque_wallpaper_rects = move(visible_rects);
    }

    if constexpr (OCCLUSIONS_DEBUG) {
        for (auto& r : m_opaque_wallpaper_rects.rects())
            dbgln("  wallpaper opaque: {}", r);
    }

    wm.for_each_visible_window_from_back_to_front([&](Window& w) {
        auto window_frame_rect = w.frame().rect().intersected(screen_rect);
        if (w.is_minimized() || window_frame_rect.is_empty())
            return IterationDecision::Continue;

        if constexpr (OCCLUSIONS_DEBUG) {
            dbgln("  Window {} frame rect: {}", w.title(), window_frame_rect);
            for (auto& r : w.opaque_rects().rects())
                dbgln("    opaque: {}", r);
            for (auto& r : w.transparency_wallpaper_rects().rects())
                dbgln("    transparent wallpaper: {}", r);
            for (auto& r : w.transparency_rects().rects())
                dbgln("    transparent: {}", r);
        }

        ASSERT(!w.opaque_rects().intersects(m_opaque_wallpaper_rects));
        ASSERT(!w.transparency_rects().intersects(m_opaque_wallpaper_rects));
        ASSERT(!w.transparency_wallpaper_rects().intersects(m_opaque_wallpaper_rects));
        return IterationDecision::Continue;
    });
}

}
