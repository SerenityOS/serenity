/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "Compositor.h"
#include "Animation.h"
#include "ClientConnection.h"
#include "Event.h"
#include "EventLoop.h"
#include "MultiScaleBitmaps.h"
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
#include <LibThreading/BackgroundAction.h>

namespace WindowServer {

Compositor& Compositor::the()
{
    static Compositor s_the;
    return s_the;
}

static WallpaperMode mode_to_enum(const String& name)
{
    if (name == "tile")
        return WallpaperMode::Tile;
    if (name == "stretch")
        return WallpaperMode::Stretch;
    if (name == "center")
        return WallpaperMode::Center;
    return WallpaperMode::Center;
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

    init_bitmaps();
}

const Gfx::Bitmap* Compositor::cursor_bitmap_for_screenshot(Badge<ClientConnection>, Screen& screen) const
{
    if (!m_current_cursor)
        return nullptr;
    return &m_current_cursor->bitmap(screen.scale_factor());
}

const Gfx::Bitmap& Compositor::front_bitmap_for_screenshot(Badge<ClientConnection>, Screen& screen) const
{
    return *m_screen_data[screen.index()].m_front_bitmap;
}

void Compositor::ScreenData::init_bitmaps(Compositor& compositor, Screen& screen)
{
    auto size = screen.size();

    m_front_bitmap = Gfx::Bitmap::create_wrapper(Gfx::BitmapFormat::BGRx8888, size, screen.scale_factor(), screen.pitch(), screen.scanline(0));
    m_front_painter = make<Gfx::Painter>(*m_front_bitmap);
    m_front_painter->translate(-screen.rect().location());

    if (screen.can_set_buffer())
        m_back_bitmap = Gfx::Bitmap::create_wrapper(Gfx::BitmapFormat::BGRx8888, size, screen.scale_factor(), screen.pitch(), screen.scanline(screen.physical_height()));
    else
        m_back_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, size, screen.scale_factor());
    m_back_painter = make<Gfx::Painter>(*m_back_bitmap);
    m_back_painter->translate(-screen.rect().location());

    m_temp_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, size, screen.scale_factor());
    m_temp_painter = make<Gfx::Painter>(*m_temp_bitmap);
    m_temp_painter->translate(-screen.rect().location());

    m_buffers_are_flipped = false;
    m_screen_can_set_buffer = screen.can_set_buffer();

    // Recreate the screen-number overlay as the Screen instances may have changed, or get rid of it if we no longer need it
    if (compositor.showing_screen_numbers()) {
        m_screen_number_overlay = compositor.create_overlay<ScreenNumberOverlay>(screen);
        m_screen_number_overlay->set_enabled(true);
    } else {
        m_screen_number_overlay = nullptr;
    }
}

void Compositor::init_bitmaps()
{
    m_screen_data.resize(Screen::count());
    Screen::for_each([&](auto& screen) {
        m_screen_data[screen.index()].init_bitmaps(*this, screen);
        return IterationDecision::Continue;
    });

    invalidate_screen();
}

void Compositor::did_construct_window_manager(Badge<WindowManager>)
{
    auto& wm = WindowManager::the();

    m_current_window_stack = &wm.current_window_stack();

    m_wallpaper_mode = mode_to_enum(wm.config()->read_entry("Background", "Mode", "center"));
    m_custom_background_color = Color::from_string(wm.config()->read_entry("Background", "Color", ""));

    invalidate_screen();
    invalidate_occlusions();
    compose();
}

Gfx::IntPoint Compositor::window_transition_offset(Window& window)
{
    if (WindowManager::is_stationary_window_type(window.type()))
        return {};

    if (window.is_moving_to_another_stack())
        return {};

    return window.outer_stack()->transition_offset();
}

void Compositor::compose()
{
    auto& wm = WindowManager::the();

    {
        auto& current_cursor = wm.active_cursor();
        if (m_current_cursor != &current_cursor) {
            change_cursor(&current_cursor);
            m_invalidated_cursor = m_invalidated_any = true;
        }
    }

    if (!m_invalidated_any) {
        // nothing dirtied since the last compose pass.
        return;
    }

    if (m_occlusions_dirty) {
        m_occlusions_dirty = false;
        recompute_occlusions();
    }

    // We should have recomputed occlusions if any overlay rects were changed
    VERIFY(!m_overlay_rects_changed);

    auto dirty_screen_rects = move(m_dirty_screen_rects);

    bool window_stack_transition_in_progress = m_transitioning_to_window_stack != nullptr;

    // Mark window regions as dirty that need to be re-rendered
    wm.for_each_visible_window_from_back_to_front([&](Window& window) {
        auto transition_offset = window_transition_offset(window);
        auto frame_rect = window.frame().render_rect();
        auto frame_rect_on_screen = frame_rect.translated(transition_offset);
        for (auto& dirty_rect : dirty_screen_rects.rects()) {
            auto invalidate_rect = dirty_rect.intersected(frame_rect_on_screen);
            if (!invalidate_rect.is_empty()) {
                auto inner_rect_offset = window.rect().location() - frame_rect.location();
                invalidate_rect.translate_by(-(frame_rect.location() + inner_rect_offset + transition_offset));
                window.invalidate_no_notify(invalidate_rect);
                m_invalidated_window = true;
            }
        }
        window.prepare_dirty_rects();
        if (window_stack_transition_in_progress)
            window.dirty_rects().translate_by(transition_offset);
        return IterationDecision::Continue;
    });

    // Any windows above or below a given window that need to be re-rendered
    // also require us to re-render that window's intersecting area, regardless
    // of whether that window has any dirty rectangles
    wm.for_each_visible_window_from_back_to_front([&](Window& window) {
        auto& transparency_rects = window.transparency_rects();
        if (transparency_rects.is_empty())
            return IterationDecision::Continue;

        auto frame_rect = window.frame().render_rect();
        auto& dirty_rects = window.dirty_rects();
        wm.for_each_visible_window_from_back_to_front([&](Window& w) {
            if (&w == &window)
                return IterationDecision::Continue;
            auto frame_rect2 = w.frame().render_rect();
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

    auto& cursor_screen = ScreenInput::the().cursor_location_screen();

    for (auto& screen_data : m_screen_data) {
        screen_data.m_flush_rects.clear_with_capacity();
        screen_data.m_flush_transparent_rects.clear_with_capacity();
        screen_data.m_flush_special_rects.clear_with_capacity();
    }

    auto cursor_rect = current_cursor_rect();

    bool need_to_draw_cursor = false;
    Gfx::IntRect previous_cursor_rect;
    Screen* previous_cursor_screen = nullptr;
    auto check_restore_cursor_back = [&](Screen& screen, const Gfx::IntRect& rect) {
        if (&screen == &cursor_screen && !previous_cursor_screen && !need_to_draw_cursor && rect.intersects(cursor_rect)) {
            // Restore what's behind the cursor if anything touches the area of the cursor
            need_to_draw_cursor = true;
            auto& screen_data = m_screen_data[cursor_screen.index()];
            if (screen_data.restore_cursor_back(cursor_screen, previous_cursor_rect))
                previous_cursor_screen = &screen;
        }
    };

    if (&cursor_screen != m_current_cursor_screen) {
        // Cursor moved to another screen, restore on the cursor's background on the previous screen
        need_to_draw_cursor = true;
        if (m_current_cursor_screen) {
            auto& screen_data = m_screen_data[m_current_cursor_screen->index()];
            if (screen_data.restore_cursor_back(*m_current_cursor_screen, previous_cursor_rect))
                previous_cursor_screen = m_current_cursor_screen;
        }
        m_current_cursor_screen = &cursor_screen;
    }

    auto prepare_rect = [&](Screen& screen, const Gfx::IntRect& rect) {
        auto& screen_data = m_screen_data[screen.index()];
        dbgln_if(COMPOSE_DEBUG, "    -> flush opaque: {}", rect);
        VERIFY(!screen_data.m_flush_rects.intersects(rect));
        VERIFY(!screen_data.m_flush_transparent_rects.intersects(rect));
        screen_data.m_flush_rects.add(rect);
        check_restore_cursor_back(screen, rect);
    };

    auto prepare_transparency_rect = [&](Screen& screen, const Gfx::IntRect& rect) {
        auto& screen_data = m_screen_data[screen.index()];
        dbgln_if(COMPOSE_DEBUG, "   -> flush transparent: {}", rect);
        VERIFY(!screen_data.m_flush_rects.intersects(rect));
        for (auto& r : screen_data.m_flush_transparent_rects.rects()) {
            if (r == rect)
                return;
        }

        screen_data.m_flush_transparent_rects.add(rect);
        check_restore_cursor_back(screen, rect);
    };

    if (!m_screen_data[cursor_screen.index()].m_cursor_back_bitmap || m_invalidated_cursor)
        check_restore_cursor_back(cursor_screen, cursor_rect);

    auto paint_wallpaper = [&](Screen& screen, Gfx::Painter& painter, const Gfx::IntRect& rect, const Gfx::IntRect& screen_rect) {
        // FIXME: If the wallpaper is opaque and covers the whole rect, no need to fill with color!
        painter.fill_rect(rect, background_color);
        if (m_wallpaper) {
            if (m_wallpaper_mode == WallpaperMode::Center) {
                Gfx::IntPoint offset { (screen.width() - m_wallpaper->width()) / 2, (screen.height() - m_wallpaper->height()) / 2 };
                painter.blit_offset(rect.location(), *m_wallpaper, rect.translated(-screen_rect.location()), offset);
            } else if (m_wallpaper_mode == WallpaperMode::Tile) {
                painter.draw_tiled_bitmap(rect, *m_wallpaper);
            } else if (m_wallpaper_mode == WallpaperMode::Stretch) {
                float hscale = (float)m_wallpaper->width() / (float)screen.width();
                float vscale = (float)m_wallpaper->height() / (float)screen.height();

                // TODO: this may look ugly, we should scale to a backing bitmap and then blit
                auto relative_rect = rect.translated(-screen_rect.location());
                auto src_rect = Gfx::FloatRect { relative_rect.x() * hscale, relative_rect.y() * vscale, relative_rect.width() * hscale, relative_rect.height() * vscale };
                painter.draw_scaled_bitmap(rect, *m_wallpaper, src_rect);
            } else {
                VERIFY_NOT_REACHED();
            }
        }
    };

    m_opaque_wallpaper_rects.for_each_intersected(dirty_screen_rects, [&](const Gfx::IntRect& render_rect) {
        Screen::for_each([&](auto& screen) {
            auto screen_rect = screen.rect();
            auto screen_render_rect = screen_rect.intersected(render_rect);
            if (!screen_render_rect.is_empty()) {
                auto& back_painter = *m_screen_data[screen.index()].m_back_painter;
                dbgln_if(COMPOSE_DEBUG, "  render wallpaper opaque: {} on screen #{}", screen_render_rect, screen.index());
                prepare_rect(screen, render_rect);
                paint_wallpaper(screen, back_painter, render_rect, screen_rect);
            }
            return IterationDecision::Continue;
        });
        return IterationDecision::Continue;
    });

    auto compose_window = [&](Window& window) -> IterationDecision {
        if (window.screens().is_empty()) {
            // This window doesn't intersect with any screens, so there's nothing to render
            return IterationDecision::Continue;
        }
        auto transition_offset = window_transition_offset(window);
        auto frame_rect = window.frame().render_rect().translated(transition_offset);
        auto window_rect = window.rect().translated(transition_offset);
        auto frame_rects = frame_rect.shatter(window_rect);

        dbgln_if(COMPOSE_DEBUG, "  window {} frame rect: {}", window.title(), frame_rect);

        RefPtr<Gfx::Bitmap> backing_store = window.backing_store();
        auto compose_window_rect = [&](Screen& screen, Gfx::Painter& painter, const Gfx::IntRect& rect) {
            if (!window.is_fullscreen()) {
                rect.for_each_intersected(frame_rects, [&](const Gfx::IntRect& intersected_rect) {
                    Gfx::PainterStateSaver saver(painter);
                    painter.add_clip_rect(intersected_rect);
                    painter.translate(transition_offset);
                    dbgln_if(COMPOSE_DEBUG, "    render frame: {}", intersected_rect);
                    window.frame().paint(screen, painter, intersected_rect.translated(-transition_offset));
                    return IterationDecision::Continue;
                });
            }

            auto clear_window_rect = [&](const Gfx::IntRect& clear_rect) {
                auto fill_color = wm.palette().window();
                if (!window.is_opaque())
                    fill_color.set_alpha(255 * window.opacity());
                painter.fill_rect(clear_rect, fill_color);
            };

            if (!backing_store) {
                clear_window_rect(window_rect.intersected(rect));
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
                backing_rect.set_location(window_rect.location());
                break;
            case ResizeDirection::Left:
            case ResizeDirection::Up:
            case ResizeDirection::UpLeft:
                backing_rect.set_right_without_resize(window_rect.right());
                backing_rect.set_bottom_without_resize(window_rect.bottom());
                break;
            case ResizeDirection::UpRight:
                backing_rect.set_left(window.rect().left());
                backing_rect.set_bottom_without_resize(window_rect.bottom());
                break;
            case ResizeDirection::DownLeft:
                backing_rect.set_right_without_resize(window_rect.right());
                backing_rect.set_top(window_rect.top());
                break;
            }

            Gfx::IntRect dirty_rect_in_backing_coordinates = rect.intersected(window_rect)
                                                                 .intersected(backing_rect)
                                                                 .translated(-backing_rect.location());

            if (!dirty_rect_in_backing_coordinates.is_empty()) {
                auto dst = backing_rect.location().translated(dirty_rect_in_backing_coordinates.location());

                if (window.client() && window.client()->is_unresponsive()) {
                    if (window.is_opaque()) {
                        painter.blit_filtered(dst, *backing_store, dirty_rect_in_backing_coordinates, [](Color src) {
                            return src.to_grayscale().darkened(0.75f);
                        });
                    } else {
                        u8 alpha = 255 * window.opacity();
                        painter.blit_filtered(dst, *backing_store, dirty_rect_in_backing_coordinates, [&](Color src) {
                            auto color = src.to_grayscale().darkened(0.75f);
                            color.set_alpha(alpha);
                            return color;
                        });
                    }
                } else {
                    painter.blit(dst, *backing_store, dirty_rect_in_backing_coordinates, window.opacity());
                }
            }

            for (auto background_rect : window_rect.shatter(backing_rect))
                clear_window_rect(background_rect);
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
                for (auto* screen : window.screens()) {
                    auto screen_render_rect = render_rect.intersected(screen->rect());
                    if (screen_render_rect.is_empty())
                        continue;
                    dbgln_if(COMPOSE_DEBUG, "    render opaque: {} on screen #{}", screen_render_rect, screen->index());

                    prepare_rect(*screen, screen_render_rect);
                    auto& back_painter = *m_screen_data[screen->index()].m_back_painter;
                    Gfx::PainterStateSaver saver(back_painter);
                    back_painter.add_clip_rect(screen_render_rect);
                    compose_window_rect(*screen, back_painter, screen_render_rect);
                }
                return IterationDecision::Continue;
            });
        }

        // Render the wallpaper for any transparency directly covering
        // the wallpaper
        auto& transparency_wallpaper_rects = window.transparency_wallpaper_rects();
        if (!transparency_wallpaper_rects.is_empty()) {
            transparency_wallpaper_rects.for_each_intersected(dirty_rects, [&](const Gfx::IntRect& render_rect) {
                for (auto* screen : window.screens()) {
                    auto screen_rect = screen->rect();
                    auto screen_render_rect = render_rect.intersected(screen_rect);
                    if (screen_render_rect.is_empty())
                        continue;
                    dbgln_if(COMPOSE_DEBUG, "    render wallpaper: {} on screen #{}", screen_render_rect, screen->index());

                    auto& temp_painter = *m_screen_data[screen->index()].m_temp_painter;
                    prepare_transparency_rect(*screen, screen_render_rect);
                    paint_wallpaper(*screen, temp_painter, screen_render_rect, screen_rect);
                }
                return IterationDecision::Continue;
            });
        }
        auto& transparency_rects = window.transparency_rects();
        if (!transparency_rects.is_empty()) {
            transparency_rects.for_each_intersected(dirty_rects, [&](const Gfx::IntRect& render_rect) {
                for (auto* screen : window.screens()) {
                    auto screen_rect = screen->rect();
                    auto screen_render_rect = render_rect.intersected(screen_rect);
                    if (screen_render_rect.is_empty())
                        continue;
                    dbgln_if(COMPOSE_DEBUG, "    render transparent: {} on screen #{}", screen_render_rect, screen->index());

                    prepare_transparency_rect(*screen, screen_render_rect);
                    auto& temp_painter = *m_screen_data[screen->index()].m_temp_painter;
                    Gfx::PainterStateSaver saver(temp_painter);
                    temp_painter.add_clip_rect(screen_render_rect);
                    compose_window_rect(*screen, temp_painter, screen_render_rect);
                }
                return IterationDecision::Continue;
            });
        }
        return IterationDecision::Continue;
    };

    // Paint the window stack.
    if (m_invalidated_window) {
        if (auto* fullscreen_window = wm.active_fullscreen_window()) {
            compose_window(*fullscreen_window);
            fullscreen_window->clear_dirty_rects();
        } else {
            wm.for_each_visible_window_from_back_to_front([&](Window& window) {
                compose_window(window);
                window.clear_dirty_rects();
                return IterationDecision::Continue;
            });
        }

        // Check that there are no overlapping transparent and opaque flush rectangles
        VERIFY(![&]() {
            bool is_overlapping = false;
            Screen::for_each([&](auto& screen) {
                auto& screen_data = m_screen_data[screen.index()];
                auto& flush_transparent_rects = screen_data.m_flush_transparent_rects;
                auto& flush_rects = screen_data.m_flush_rects;
                for (auto& rect_transparent : flush_transparent_rects.rects()) {
                    for (auto& rect_opaque : flush_rects.rects()) {
                        if (rect_opaque.intersects(rect_transparent)) {
                            dbgln("Transparent rect {} overlaps opaque rect: {}: {}", rect_transparent, rect_opaque, rect_opaque.intersected(rect_transparent));
                            is_overlapping = true;
                            return IterationDecision::Break;
                        }
                    }
                }
                return IterationDecision::Continue;
            });
            return is_overlapping;
        }());

        if (!m_overlay_list.is_empty()) {
            // Render everything to the temporary buffer before we copy it back
            render_overlays();
        }

        // Copy anything rendered to the temporary buffer to the back buffer
        Screen::for_each([&](auto& screen) {
            auto screen_rect = screen.rect();
            auto& screen_data = m_screen_data[screen.index()];
            for (auto& rect : screen_data.m_flush_transparent_rects.rects())
                screen_data.m_back_painter->blit(rect.location(), *screen_data.m_temp_bitmap, rect.translated(-screen_rect.location()));
            return IterationDecision::Continue;
        });
    }

    m_invalidated_any = false;
    m_invalidated_window = false;
    m_invalidated_cursor = false;

    if (!m_animations.is_empty()) {
        Screen::for_each([&](auto& screen) {
            auto& screen_data = m_screen_data[screen.index()];
            update_animations(screen, screen_data.m_flush_special_rects);
            return IterationDecision::Continue;
        });
        // As long as animations are running make sure we keep rendering frames
        m_invalidated_any = true;
        start_compose_async_timer();
    }

    if (need_to_draw_cursor) {
        auto& screen_data = m_screen_data[cursor_screen.index()];
        screen_data.draw_cursor(cursor_screen, cursor_rect);
        screen_data.m_flush_rects.add(cursor_rect.intersected(cursor_screen.rect()));
        if (previous_cursor_screen && cursor_rect != previous_cursor_rect)
            m_screen_data[previous_cursor_screen->index()].m_flush_rects.add(previous_cursor_rect);
    }

    Screen::for_each([&](auto& screen) {
        flush(screen);
        return IterationDecision::Continue;
    });
}

void Compositor::flush(Screen& screen)
{
    auto& screen_data = m_screen_data[screen.index()];
    if (m_flash_flush) {
        for (auto& rect : screen_data.m_flush_rects.rects())
            screen_data.m_front_painter->fill_rect(rect, Color::Yellow);
    }

    if (screen_data.m_screen_can_set_buffer)
        screen_data.flip_buffers(screen);

    auto screen_rect = screen.rect();
    bool device_can_flush_buffers = screen.can_device_flush_buffers();
    auto do_flush = [&](Gfx::IntRect rect) {
        VERIFY(screen_rect.contains(rect));
        rect.translate_by(-screen_rect.location());

        // Almost everything in Compositor is in logical coordinates, with the painters having
        // a scale applied. But this routine accesses the backbuffer pixels directly, so it
        // must work in physical coordinates.
        auto scaled_rect = rect * screen.scale_factor();
        Gfx::RGBA32* front_ptr = screen_data.m_front_bitmap->scanline(scaled_rect.y()) + scaled_rect.x();
        Gfx::RGBA32* back_ptr = screen_data.m_back_bitmap->scanline(scaled_rect.y()) + scaled_rect.x();
        size_t pitch = screen_data.m_back_bitmap->pitch();

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

        if (screen_data.m_screen_can_set_buffer) {
            to_ptr = back_ptr;
            from_ptr = front_ptr;
        } else {
            to_ptr = front_ptr;
            from_ptr = back_ptr;
        }

        for (int y = 0; y < scaled_rect.height(); ++y) {
            fast_u32_copy(to_ptr, from_ptr, scaled_rect.width());
            from_ptr = (const Gfx::RGBA32*)((const u8*)from_ptr + pitch);
            to_ptr = (Gfx::RGBA32*)((u8*)to_ptr + pitch);
        }
        if (device_can_flush_buffers)
            screen.queue_flush_display_rect(rect);
    };
    for (auto& rect : screen_data.m_flush_rects.rects())
        do_flush(rect);
    for (auto& rect : screen_data.m_flush_transparent_rects.rects())
        do_flush(rect);
    for (auto& rect : screen_data.m_flush_special_rects.rects())
        do_flush(rect);
    if (device_can_flush_buffers)
        screen.flush_display();
}

void Compositor::invalidate_screen()
{
    invalidate_screen(Screen::bounding_rect());
}

void Compositor::invalidate_screen(const Gfx::IntRect& screen_rect)
{
    m_dirty_screen_rects.add(screen_rect.intersected(Screen::bounding_rect()));

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
    Threading::BackgroundAction<RefPtr<Gfx::Bitmap>>::create(
        [path](auto&) {
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

void Compositor::ScreenData::flip_buffers(Screen& screen)
{
    VERIFY(m_screen_can_set_buffer);
    swap(m_front_bitmap, m_back_bitmap);
    swap(m_front_painter, m_back_painter);
    screen.set_buffer(m_buffers_are_flipped ? 0 : 1);
    m_buffers_are_flipped = !m_buffers_are_flipped;
}

void Compositor::screen_resolution_changed()
{
    // Screens may be gone now, invalidate any references to them
    m_current_cursor_screen = nullptr;

    init_bitmaps();
    invalidate_occlusions();
    overlay_rects_changed();
    compose();
}

Gfx::IntRect Compositor::current_cursor_rect() const
{
    auto& wm = WindowManager::the();
    auto& current_cursor = m_current_cursor ? *m_current_cursor : wm.active_cursor();
    return { ScreenInput::the().cursor_location().translated(-current_cursor.params().hotspot()), current_cursor.size() };
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

void Compositor::render_overlays()
{
    // NOTE: overlays should always be rendered to the temporary buffer!
    for (auto& overlay : m_overlay_list) {
        for (auto* screen : overlay.m_screens) {
            auto& screen_data = m_screen_data[screen->index()];
            auto& painter = screen_data.overlay_painter();
            screen_data.for_each_intersected_flushing_rect(overlay.current_render_rect(), [&](auto& intersected_overlay_rect) {
                Gfx::PainterStateSaver saver(painter);
                painter.add_clip_rect(intersected_overlay_rect);
                painter.translate(overlay.m_current_rect.location());
                overlay.render(painter, *screen);
                return IterationDecision::Continue;
            });
        }
    }
}

void Compositor::add_overlay(Overlay& overlay)
{
    VERIFY(!overlay.m_list_node.is_in_list());
    auto zorder = overlay.zorder();
    bool did_insert = false;
    for (auto& other_overlay : m_overlay_list) {
        if (other_overlay.zorder() > zorder) {
            m_overlay_list.insert_before(other_overlay, overlay);
            did_insert = true;
            break;
        }
    }
    if (!did_insert)
        m_overlay_list.append(overlay);

    overlay.clear_invalidated();
    overlay_rects_changed();
    auto& rect = overlay.rect();
    if (!rect.is_empty())
        invalidate_screen(rect);
}

void Compositor::remove_overlay(Overlay& overlay)
{
    auto& current_render_rect = overlay.current_render_rect();
    if (!current_render_rect.is_empty())
        invalidate_screen(current_render_rect);
    m_overlay_list.remove(overlay);
}

void Compositor::ScreenData::draw_cursor(Screen& screen, const Gfx::IntRect& cursor_rect)
{
    auto& wm = WindowManager::the();

    if (!m_cursor_back_bitmap || m_cursor_back_bitmap->size() != cursor_rect.size() || m_cursor_back_bitmap->scale() != screen.scale_factor()) {
        m_cursor_back_bitmap = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRx8888, cursor_rect.size(), screen.scale_factor());
        m_cursor_back_painter = make<Gfx::Painter>(*m_cursor_back_bitmap);
    }

    auto& compositor = Compositor::the();
    auto& current_cursor = compositor.m_current_cursor ? *compositor.m_current_cursor : wm.active_cursor();
    auto screen_rect = screen.rect();
    m_cursor_back_painter->blit({ 0, 0 }, *m_back_bitmap, current_cursor.rect().translated(cursor_rect.location()).intersected(screen_rect).translated(-screen_rect.location()));
    m_back_painter->blit(cursor_rect.location(), current_cursor.bitmap(screen.scale_factor()), current_cursor.source_rect(compositor.m_current_cursor_frame));
    m_last_cursor_rect = cursor_rect;
    VERIFY(compositor.m_current_cursor_screen == &screen);
    m_cursor_back_is_valid = true;
}

bool Compositor::ScreenData::restore_cursor_back(Screen& screen, Gfx::IntRect& last_cursor_rect)
{
    if (!m_cursor_back_is_valid || !m_cursor_back_bitmap || m_cursor_back_bitmap->scale() != m_back_bitmap->scale())
        return false;

    last_cursor_rect = m_last_cursor_rect.intersected(screen.rect());
    m_back_painter->blit(last_cursor_rect.location(), *m_cursor_back_bitmap, { { 0, 0 }, last_cursor_rect.size() });
    m_cursor_back_is_valid = false;
    return true;
}

void Compositor::update_fonts()
{
    ScreenNumberOverlay::pick_font();
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
    VERIFY(m_display_link_count);
    --m_display_link_count;
    if (!m_display_link_count)
        m_display_link_notify_timer->stop();
}

void Compositor::invalidate_current_screen_number_rects()
{
    for (auto& screen_data : m_screen_data) {
        if (screen_data.m_screen_number_overlay)
            screen_data.m_screen_number_overlay->invalidate();
    }
}

void Compositor::increment_show_screen_number(Badge<ClientConnection>)
{
    if (m_show_screen_number_count++ == 0) {
        Screen::for_each([&](auto& screen) {
            auto& screen_data = m_screen_data[screen.index()];
            VERIFY(!screen_data.m_screen_number_overlay);
            screen_data.m_screen_number_overlay = create_overlay<ScreenNumberOverlay>(screen);
            screen_data.m_screen_number_overlay->set_enabled(true);
            return IterationDecision::Continue;
        });
    }
}
void Compositor::decrement_show_screen_number(Badge<ClientConnection>)
{
    if (--m_show_screen_number_count == 0) {
        invalidate_current_screen_number_rects();
        for (auto& screen_data : m_screen_data)
            screen_data.m_screen_number_overlay = nullptr;
    }
}

void Compositor::overlays_theme_changed()
{
    for (auto& overlay : m_overlay_list)
        overlay.theme_changed();
    overlay_rects_changed();
}

void Compositor::overlay_rects_changed()
{
    if (m_overlay_rects_changed)
        return;

    m_overlay_rects_changed = true;
    m_invalidated_any = true;
    invalidate_occlusions();
    for (auto& rect : m_overlay_rects.rects())
        invalidate_screen(rect);
    start_compose_async_timer();
}

void Compositor::recompute_overlay_rects()
{
    // The purpose of this is to gather all areas that we will render over
    // regular window contents. This effectively just forces those areas to
    // be rendered as transparency areas, which allows us to render these
    // flicker-free.
    m_overlay_rects.clear_with_capacity();
    for (auto& overlay : m_overlay_list) {
        auto& render_rect = overlay.rect();
        m_overlay_rects.add(render_rect);

        // Save the rectangle we are using for rendering from now on
        overlay.did_recompute_occlusions();

        // Cache which screens this overlay are rendered on
        overlay.m_screens.clear_with_capacity();
        Screen::for_each([&](auto& screen) {
            if (render_rect.intersects(screen.rect()))
                overlay.m_screens.append(&screen);
            return IterationDecision::Continue;
        });

        invalidate_screen(render_rect);
    }
}

void Compositor::recompute_occlusions()
{
    auto& wm = WindowManager::the();
    bool is_switcher_visible = wm.m_switcher.is_visible();
    auto never_occlude = [&](WindowStack& window_stack) {
        if (is_switcher_visible) {
            switch (wm.m_switcher.mode()) {
            case WindowSwitcher::Mode::ShowCurrentDesktop:
                // Any window on the currently rendered desktop should not be occluded, even if it's behind
                // another window entirely.
                return &window_stack == m_current_window_stack || &window_stack == m_transitioning_to_window_stack;
            case WindowSwitcher::Mode::ShowAllWindows:
                // The window switcher wants to know about all windows, even those on other desktops
                return true;
            }
        }
        return false;
    };

    wm.for_each_window_stack([&](WindowStack& window_stack) {
        if (&window_stack == m_current_window_stack || &window_stack == m_transitioning_to_window_stack) {
            // We'll calculate precise occlusions for these further down. Changing occlusions right now
            // may trigger an additional unnecessary notification
        } else {
            window_stack.set_all_occluded(!never_occlude(window_stack));
        }
        return IterationDecision::Continue;
    });

    if (m_overlay_rects_changed) {
        m_overlay_rects_changed = false;
        recompute_overlay_rects();
    }

    if constexpr (OCCLUSIONS_DEBUG) {
        dbgln("OCCLUSIONS:");
        for (auto& rect : m_overlay_rects.rects())
            dbgln("  overlay: {}", rect);
    }

    bool window_stack_transition_in_progress = m_transitioning_to_window_stack != nullptr;
    auto& main_screen = Screen::main();
    if (auto* fullscreen_window = wm.active_fullscreen_window()) {
        // TODO: support fullscreen windows on all screens
        auto screen_rect = main_screen.rect();
        wm.for_each_visible_window_from_front_to_back([&](Window& w) {
            auto& visible_opaque = w.opaque_rects();
            auto& transparency_rects = w.transparency_rects();
            auto& transparency_wallpaper_rects = w.transparency_wallpaper_rects();
            w.screens().clear_with_capacity();
            if (&w == fullscreen_window) {
                w.screens().append(&main_screen);
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
        Gfx::DisjointRectSet visible_rects;
        visible_rects.add_many(Screen::rects());
        bool have_transparent = false;
        wm.for_each_visible_window_from_front_to_back([&](Window& w) {
            w.transparency_wallpaper_rects().clear();
            auto& visible_opaque = w.opaque_rects();
            visible_opaque.clear();
            auto& transparency_rects = w.transparency_rects();
            transparency_rects.clear();
            w.screens().clear_with_capacity();
            if (w.is_minimized())
                return IterationDecision::Continue;

            auto transition_offset = window_transition_offset(w);
            auto transparent_frame_render_rects = w.frame().transparent_render_rects();
            auto opaque_frame_render_rects = w.frame().opaque_render_rects();
            if (window_stack_transition_in_progress) {
                transparent_frame_render_rects.translate_by(transition_offset);
                opaque_frame_render_rects.translate_by(transition_offset);
            }
            Gfx::DisjointRectSet visible_opaque_rects;
            Screen::for_each([&](auto& screen) {
                auto screen_rect = screen.rect();
                if (auto transparent_render_rects = transparent_frame_render_rects.intersected(screen_rect); !transparent_render_rects.is_empty()) {
                    if (transparency_rects.is_empty())
                        transparency_rects = move(transparent_render_rects);
                    else
                        transparency_rects.add(transparent_render_rects);
                }
                if (auto opaque_render_rects = opaque_frame_render_rects.intersected(screen_rect); !opaque_render_rects.is_empty()) {
                    if (visible_opaque_rects.is_empty())
                        visible_opaque_rects = move(opaque_render_rects);
                    else
                        visible_opaque_rects.add(opaque_render_rects);
                }
                return IterationDecision::Continue;
            });
            visible_opaque = visible_rects.intersected(visible_opaque_rects);

            auto render_rect = w.frame().render_rect();
            auto render_rect_on_screen = render_rect;
            auto visible_window_rects = visible_rects.intersected(w.rect().translated(transition_offset));
            if (window_stack_transition_in_progress)
                render_rect_on_screen.translate_by(transition_offset);
            Gfx::DisjointRectSet opaque_covering;
            bool found_this_window = false;
            wm.for_each_visible_window_from_back_to_front([&](Window& w2) {
                if (!found_this_window) {
                    if (&w == &w2)
                        found_this_window = true;
                    return IterationDecision::Continue;
                }

                if (w2.is_minimized())
                    return IterationDecision::Continue;

                auto w2_render_rect = w2.frame().render_rect();
                auto w2_render_rect_on_screen = w2_render_rect;
                auto w2_transition_offset = window_transition_offset(w2);
                if (window_stack_transition_in_progress)
                    w2_render_rect_on_screen.translate_by(w2_transition_offset);
                if (!render_rect_on_screen.intersects(w2_render_rect_on_screen))
                    return IterationDecision::Continue;

                auto opaque_rects = w2.frame().opaque_render_rects();
                auto transparent_rects = w2.frame().transparent_render_rects();
                if (window_stack_transition_in_progress) {
                    auto transition_offset_2 = window_transition_offset(w2);
                    opaque_rects.translate_by(transition_offset_2);
                    transparent_rects.translate_by(transition_offset_2);
                }
                opaque_rects = opaque_rects.intersected(render_rect_on_screen);
                transparent_rects = transparent_rects.intersected(render_rect_on_screen);
                if (opaque_rects.is_empty() && transparent_rects.is_empty())
                    return IterationDecision::Continue;
                for (auto& covering : opaque_rects.rects()) {
                    opaque_covering.add(covering);
                    if (!visible_window_rects.is_empty())
                        visible_window_rects = visible_window_rects.shatter(covering);
                    if (opaque_covering.contains(render_rect_on_screen)) {
                        // This entire window (including frame) is entirely covered by other opaque window areas
                        visible_window_rects.clear();
                        visible_opaque.clear();
                        transparency_rects.clear();
                        return IterationDecision::Break;
                    }
                    if (!visible_opaque.is_empty()) {
                        auto uncovered_opaque = visible_opaque.shatter(covering);
                        visible_opaque = move(uncovered_opaque);
                    }

                    if (!transparency_rects.is_empty()) {
                        auto uncovered_transparency = transparency_rects.shatter(covering);
                        transparency_rects = move(uncovered_transparency);
                    }
                }

                for (auto& covering : transparent_rects.rects()) {
                    visible_rects.for_each_intersected(covering, [&](const Gfx::IntRect& intersected) {
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

            // This window should not be occluded while the window switcher is interested in it (depending
            // on the mode it's in). If it isn't then determine occlusions based on whether the window
            // rect has any visible areas at all.
            w.set_occluded(never_occlude(*w.outer_stack()) ? false : visible_window_rects.is_empty());

            if (!m_overlay_rects.is_empty() && m_overlay_rects.intersects(visible_opaque)) {
                // In order to render overlays flicker-free we need to force these area into the
                // temporary transparency rendering buffer
                transparency_rects.add(m_overlay_rects.intersected(visible_opaque));
                visible_opaque = visible_opaque.shatter(m_overlay_rects);
            }

            bool have_opaque = !visible_opaque.is_empty();
            if (!transparency_rects.is_empty())
                have_transparent = true;
            if (have_transparent || have_opaque) {
                // Figure out what screens this window is rendered on
                // We gather this information so we can more quickly
                // render the window on each of the screens that it
                // needs to be rendered on.
                Screen::for_each([&](auto& screen) {
                    auto screen_rect = screen.rect();
                    for (auto& r : visible_opaque.rects()) {
                        if (r.intersects(screen_rect)) {
                            w.screens().append(&screen);
                            return IterationDecision::Continue;
                        }
                    }
                    for (auto& r : transparency_rects.rects()) {
                        if (r.intersects(screen_rect)) {
                            w.screens().append(&screen);
                            return IterationDecision::Continue;
                        }
                    }
                    return IterationDecision::Continue;
                });
            }

            VERIFY(!visible_opaque.intersects(transparency_rects));

            // Determine visible area for the window below
            visible_rects = visible_rects.shatter(visible_opaque);
            return IterationDecision::Continue;
        });

        if (have_transparent) {
            // Determine what transparent window areas need to render the wallpaper first
            wm.for_each_visible_window_from_back_to_front([&](Window& w) {
                auto& transparency_wallpaper_rects = w.transparency_wallpaper_rects();
                if (w.is_minimized()) {
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
        auto window_frame_rect = w.frame().render_rect();
        if (w.is_minimized() || window_frame_rect.is_empty() || w.screens().is_empty())
            return IterationDecision::Continue;

        if constexpr (OCCLUSIONS_DEBUG) {
            dbgln("  Window {} frame rect: {} rendered on screens: {}", w.title(), window_frame_rect, w.screens().size());
            for (auto& s : w.screens())
                dbgln("    screen: #{}", s->index());
            for (auto& r : w.opaque_rects().rects())
                dbgln("    opaque: {}", r);
            for (auto& r : w.transparency_wallpaper_rects().rects())
                dbgln("    transparent wallpaper: {}", r);
            for (auto& r : w.transparency_rects().rects())
                dbgln("    transparent: {}", r);
        }

        VERIFY(!w.opaque_rects().intersects(m_opaque_wallpaper_rects));
        VERIFY(!w.transparency_rects().intersects(m_opaque_wallpaper_rects));
        VERIFY(!w.transparency_wallpaper_rects().intersects(m_opaque_wallpaper_rects));
        return IterationDecision::Continue;
    });
}

void Compositor::register_animation(Badge<Animation>, Animation& animation)
{
    bool was_empty = m_animations.is_empty();
    auto result = m_animations.set(&animation);
    VERIFY(result == AK::HashSetResult::InsertedNewEntry);
    if (was_empty)
        start_compose_async_timer();
}

void Compositor::animation_started(Badge<Animation>)
{
    m_invalidated_any = true;
    start_compose_async_timer();
}

void Compositor::unregister_animation(Badge<Animation>, Animation& animation)
{
    bool was_removed = m_animations.remove(&animation);
    VERIFY(was_removed);
}

void Compositor::update_animations(Screen& screen, Gfx::DisjointRectSet& flush_rects)
{
    auto& painter = *m_screen_data[screen.index()].m_back_painter;
    for (RefPtr<Animation> animation : m_animations) {
        animation->update({}, painter, screen, flush_rects);
    }
}

void Compositor::create_window_stack_switch_overlay(WindowStack& target_stack)
{
    stop_window_stack_switch_overlay_timer();
    Screen::for_each([&](auto& screen) {
        auto& screen_data = m_screen_data[screen.index()];
        screen_data.m_window_stack_switch_overlay = nullptr; // delete it first
        screen_data.m_window_stack_switch_overlay = create_overlay<WindowStackSwitchOverlay>(screen, target_stack);
        screen_data.m_window_stack_switch_overlay->set_enabled(true);
        return IterationDecision::Continue;
    });
}

void Compositor::remove_window_stack_switch_overlays()
{
    Screen::for_each([&](auto& screen) {
        auto& screen_data = m_screen_data[screen.index()];
        screen_data.m_window_stack_switch_overlay = nullptr;
        return IterationDecision::Continue;
    });
}

void Compositor::stop_window_stack_switch_overlay_timer()
{
    if (m_stack_switch_overlay_timer) {
        // Cancel any timer, we're going to delete the overlay
        m_stack_switch_overlay_timer->stop();
        m_stack_switch_overlay_timer = nullptr;
    }
}

void Compositor::start_window_stack_switch_overlay_timer()
{
    if (m_stack_switch_overlay_timer) {
        m_stack_switch_overlay_timer->stop();
        m_stack_switch_overlay_timer = nullptr;
    }
    bool have_overlay = false;
    Screen::for_each([&](auto& screen) {
        auto& screen_data = m_screen_data[screen.index()];
        if (screen_data.m_window_stack_switch_overlay) {
            have_overlay = true;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    if (!have_overlay)
        return;
    m_stack_switch_overlay_timer = Core::Timer::create_single_shot(
        500,
        [this] {
            remove_window_stack_switch_overlays();
        },
        this);
    m_stack_switch_overlay_timer->start();
}

void Compositor::finish_window_stack_switch()
{
    VERIFY(m_transitioning_to_window_stack);
    VERIFY(m_current_window_stack);
    VERIFY(m_transitioning_to_window_stack != m_current_window_stack);

    m_current_window_stack->set_transition_offset({}, {});
    m_transitioning_to_window_stack->set_transition_offset({}, {});

    auto* previous_window_stack = m_current_window_stack;
    m_current_window_stack = m_transitioning_to_window_stack;
    m_transitioning_to_window_stack = nullptr;

    m_window_stack_transition_animation = nullptr;

    auto& wm = WindowManager::the();
    if (!wm.m_switcher.is_visible())
        previous_window_stack->set_all_occluded(true);
    wm.did_switch_window_stack({}, *previous_window_stack, *m_current_window_stack);

    invalidate_occlusions();

    // Rather than invalidating the entire we could invalidate all render rectangles
    // that are affected by the transition offset before and after changing it.
    invalidate_screen();

    start_window_stack_switch_overlay_timer();
}

void Compositor::set_current_window_stack_no_transition(WindowStack& new_window_stack)
{
    if (m_transitioning_to_window_stack) {
        finish_window_stack_switch();
        VERIFY(!m_window_stack_transition_animation);
        VERIFY(!m_transitioning_to_window_stack);
    }
    if (m_current_window_stack == &new_window_stack)
        return;
    m_current_window_stack = &new_window_stack;
    invalidate_for_window_stack_merge_or_change();
}

void Compositor::invalidate_for_window_stack_merge_or_change()
{
    invalidate_occlusions();
    invalidate_screen();
}

void Compositor::switch_to_window_stack(WindowStack& new_window_stack, bool show_overlay)
{
    if (m_transitioning_to_window_stack) {
        if (m_transitioning_to_window_stack == &new_window_stack)
            return;
        // A switch is in progress, but the user is impatient. Finish the transition instantly
        finish_window_stack_switch();
        VERIFY(!m_window_stack_transition_animation);
        // Now switch to the next target as usual
    }
    VERIFY(m_current_window_stack);

    if (&new_window_stack == m_current_window_stack) {
        // So that the user knows which stack they're on, show the overlay briefly
        if (show_overlay) {
            create_window_stack_switch_overlay(*m_current_window_stack);
            start_window_stack_switch_overlay_timer();
        } else {
            stop_window_stack_switch_overlay_timer();
            remove_window_stack_switch_overlays();
        }
        return;
    }
    VERIFY(!m_transitioning_to_window_stack);
    m_transitioning_to_window_stack = &new_window_stack;

    auto window_stack_size = Screen::bounding_rect().size();

    int delta_x = 0;
    if (new_window_stack.column() < m_current_window_stack->column())
        delta_x = window_stack_size.width();
    else if (new_window_stack.column() > m_current_window_stack->column())
        delta_x = -window_stack_size.width();
    int delta_y = 0;
    if (new_window_stack.row() < m_current_window_stack->row())
        delta_y = window_stack_size.height();
    else if (new_window_stack.row() > m_current_window_stack->row()) {
        delta_y = -window_stack_size.height();
    }

    m_transitioning_to_window_stack->set_transition_offset({}, { -delta_x, -delta_y });
    m_current_window_stack->set_transition_offset({}, {});

    if (show_overlay) {
        // We start the timer when the animation ends!
        create_window_stack_switch_overlay(*m_transitioning_to_window_stack);
    } else {
        stop_window_stack_switch_overlay_timer();
        remove_window_stack_switch_overlays();
    }

    VERIFY(!m_window_stack_transition_animation);
    m_window_stack_transition_animation = Animation::create();
    m_window_stack_transition_animation->set_duration(250);
    m_window_stack_transition_animation->on_update = [this, delta_x, delta_y](float progress, Gfx::Painter&, Screen&, Gfx::DisjointRectSet&) {
        VERIFY(m_transitioning_to_window_stack);
        VERIFY(m_current_window_stack);

        // Set transition offset for the window stack we're transitioning out of
        auto previous_transition_offset_from = m_current_window_stack->transition_offset();
        Gfx::IntPoint transition_offset_from { (float)delta_x * progress, (float)delta_y * progress };
        if (previous_transition_offset_from == transition_offset_from)
            return;

        {
            // we need to render both, the existing dirty rectangles as well as where we're shifting to
            auto translated_dirty_rects = m_dirty_screen_rects.clone();
            auto transition_delta = transition_offset_from - previous_transition_offset_from;
            translated_dirty_rects.translate_by(transition_delta);
            m_dirty_screen_rects.add(translated_dirty_rects.intersected(Screen::bounding_rect()));
        }
        m_current_window_stack->set_transition_offset({}, transition_offset_from);

        // Set transition offset for the window stack we're transitioning to
        Gfx::IntPoint transition_offset_to { (float)-delta_x * (1.0f - progress), (float)-delta_y * (1.0f - progress) };
        m_transitioning_to_window_stack->set_transition_offset({}, transition_offset_to);

        invalidate_occlusions();

        // Rather than invalidating the entire we could invalidate all render rectangles
        // that are affected by the transition offset before and after changing it.
        invalidate_screen();
    };

    m_window_stack_transition_animation->on_stop = [this] {
        finish_window_stack_switch();
    };
    m_window_stack_transition_animation->start();
}

}
