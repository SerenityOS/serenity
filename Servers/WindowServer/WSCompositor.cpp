#include "WSCompositor.h"
#include "WSEvent.h"
#include "WSEventLoop.h"
#include "WSScreen.h"
#include "WSWindow.h"
#include "WSWindowManager.h"
#include <LibDraw/Font.h>
#include <LibDraw/PNGLoader.h>
#include <LibDraw/Painter.h>
#include <LibThread/BackgroundAction.h>

// #define COMPOSITOR_DEBUG

WSCompositor& WSCompositor::the()
{
    static WSCompositor s_the;
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

WSCompositor::WSCompositor()
{
    m_compose_timer = CTimer::construct(this);
    m_immediate_compose_timer = CTimer::construct(this);

    m_screen_can_set_buffer = WSScreen::the().can_set_buffer();

    init_bitmaps();

    m_compose_timer->on_timeout = [&]() {
#if defined(COMPOSITOR_DEBUG)
        dbgprintf("WSCompositor: delayed frame callback: %d rects\n", m_dirty_rects.size());
#endif
        compose();
    };
    m_compose_timer->set_single_shot(true);
    m_compose_timer->set_interval(1000 / 60);
    m_immediate_compose_timer->on_timeout = [=]() {
#if defined(COMPOSITOR_DEBUG)
        dbgprintf("WSCompositor: immediate frame callback: %d rects\n", m_dirty_rects.size());
#endif
        compose();
    };
    m_immediate_compose_timer->set_single_shot(true);
    m_immediate_compose_timer->set_interval(0);
}

void WSCompositor::init_bitmaps()
{
    auto& screen = WSScreen::the();
    auto size = screen.size();

    m_front_bitmap = GraphicsBitmap::create_wrapper(GraphicsBitmap::Format::RGB32, size, screen.pitch(), screen.scanline(0));

    if (m_screen_can_set_buffer)
        m_back_bitmap = GraphicsBitmap::create_wrapper(GraphicsBitmap::Format::RGB32, size, screen.pitch(), screen.scanline(size.height()));
    else
        m_back_bitmap = GraphicsBitmap::create(GraphicsBitmap::Format::RGB32, size);

    m_front_painter = make<Painter>(*m_front_bitmap);
    m_back_painter = make<Painter>(*m_back_bitmap);

    m_buffers_are_flipped = false;

    invalidate();
}

void WSCompositor::compose()
{
    auto& wm = WSWindowManager::the();
    if (m_wallpaper_mode == WallpaperMode::Unchecked)
        m_wallpaper_mode = mode_to_enum(wm.wm_config()->read_entry("Background", "Mode", "simple"));
    auto& ws = WSScreen::the();

    auto dirty_rects = move(m_dirty_rects);

    if (dirty_rects.size() == 0) {
        // nothing dirtied since the last compose pass.
        return;
    }

    dirty_rects.add(Rect::intersection(m_last_geometry_label_rect, WSScreen::the().rect()));
    dirty_rects.add(Rect::intersection(m_last_cursor_rect, WSScreen::the().rect()));
    dirty_rects.add(Rect::intersection(current_cursor_rect(), WSScreen::the().rect()));
#ifdef DEBUG_COUNTERS
    dbgprintf("[WM] compose #%u (%u rects)\n", ++m_compose_count, dirty_rects.rects().size());
#endif

    auto any_dirty_rect_intersects_window = [&dirty_rects](const WSWindow& window) {
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
        m_back_painter->fill_rect(dirty_rect, wm.m_background_color);
        if (m_wallpaper) {
            if (m_wallpaper_mode == WallpaperMode::Simple) {
                m_back_painter->blit(dirty_rect.location(), *m_wallpaper, dirty_rect);
            } else if (m_wallpaper_mode == WallpaperMode::Center) {
                Point offset { ws.size().width() / 2 - m_wallpaper->size().width() / 2,
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

    auto compose_window = [&](WSWindow& window) -> IterationDecision {
        if (!any_dirty_rect_intersects_window(window))
            return IterationDecision::Continue;
        PainterStateSaver saver(*m_back_painter);
        m_back_painter->add_clip_rect(window.frame().rect());
        RefPtr<GraphicsBitmap> backing_store = window.backing_store();
        for (auto& dirty_rect : dirty_rects.rects()) {
            if (wm.any_opaque_window_above_this_one_contains_rect(window, dirty_rect))
                continue;
            PainterStateSaver saver(*m_back_painter);
            m_back_painter->add_clip_rect(dirty_rect);
            if (!backing_store)
                m_back_painter->fill_rect(dirty_rect, window.background_color());
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
            Rect backing_rect;
            backing_rect.set_size(backing_store->size());
            switch (WSWindowManager::the().resize_direction_of_window(window)) {
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

            Rect dirty_rect_in_backing_coordinates = dirty_rect
                                                         .intersected(window.rect())
                                                         .intersected(backing_rect)
                                                         .translated(-backing_rect.location());

            if (dirty_rect_in_backing_coordinates.is_empty())
                continue;
            auto dst = backing_rect.location().translated(dirty_rect_in_backing_coordinates.location());

            m_back_painter->blit(dst, *backing_store, dirty_rect_in_backing_coordinates, window.opacity());
            for (auto background_rect : window.rect().shatter(backing_rect))
                m_back_painter->fill_rect(background_rect, window.background_color());
        }
        return IterationDecision::Continue;
    };

    // Paint the window stack.
    if (auto* fullscreen_window = wm.active_fullscreen_window()) {
        compose_window(*fullscreen_window);
    } else {
        wm.for_each_visible_window_from_back_to_front([&](WSWindow& window) {
            return compose_window(window);
        });

        draw_geometry_label();
    }

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

void WSCompositor::flush(const Rect& a_rect)
{
    auto rect = Rect::intersection(a_rect, WSScreen::the().rect());

#ifdef DEBUG_COUNTERS
    dbgprintf("[WM] flush #%u (%d,%d %dx%d)\n", ++m_flush_count, rect.x(), rect.y(), rect.width(), rect.height());
#endif

    RGBA32* front_ptr = m_front_bitmap->scanline(rect.y()) + rect.x();
    RGBA32* back_ptr = m_back_bitmap->scanline(rect.y()) + rect.x();
    size_t pitch = m_back_bitmap->pitch();

    // NOTE: The meaning of a flush depends on whether we can flip buffers or not.
    //
    //       If flipping is supported, flushing means that we've flipped, and now we
    //       copy the changed bits from the front buffer to the back buffer, to keep
    //       them in sync.
    //
    //       If flipping is not supported, flushing means that we copy the changed
    //       rects from the backing bitmap to the display framebuffer.

    RGBA32* to_ptr;
    const RGBA32* from_ptr;

    if (m_screen_can_set_buffer) {
        to_ptr = back_ptr;
        from_ptr = front_ptr;
    } else {
        to_ptr = front_ptr;
        from_ptr = back_ptr;
    }

    for (int y = 0; y < rect.height(); ++y) {
        fast_u32_copy(to_ptr, from_ptr, rect.width());
        from_ptr = (const RGBA32*)((const u8*)from_ptr + pitch);
        to_ptr = (RGBA32*)((u8*)to_ptr + pitch);
    }
}

void WSCompositor::invalidate()
{
    m_dirty_rects.clear_with_capacity();
    invalidate(WSScreen::the().rect());
}

void WSCompositor::invalidate(const Rect& a_rect)
{
    auto rect = Rect::intersection(a_rect, WSScreen::the().rect());
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

bool WSCompositor::set_wallpaper(const String& path, Function<void(bool)>&& callback)
{
    LibThread::BackgroundAction<RefPtr<GraphicsBitmap>>::create(
        [path] {
            return load_png(path);
        },

        [this, path, callback = move(callback)](RefPtr<GraphicsBitmap> bitmap) {
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

void WSCompositor::flip_buffers()
{
    ASSERT(m_screen_can_set_buffer);
    swap(m_front_bitmap, m_back_bitmap);
    swap(m_front_painter, m_back_painter);
    WSScreen::the().set_buffer(m_buffers_are_flipped ? 0 : 1);
    m_buffers_are_flipped = !m_buffers_are_flipped;
}

void WSCompositor::set_resolution(int desired_width, int desired_height)
{
    auto screen_rect = WSScreen::the().rect();
    if (screen_rect.width() == desired_width && screen_rect.height() == desired_height)
        return;
    m_wallpaper_path = {};
    m_wallpaper = nullptr;
    // Make sure it's impossible to set an invalid resolution
    ASSERT(desired_width >= 640 && desired_height >= 480);
    WSScreen::the().set_resolution(desired_width, desired_height);
    init_bitmaps();
    compose();
}

Rect WSCompositor::current_cursor_rect() const
{
    auto& wm = WSWindowManager::the();
    return { WSScreen::the().cursor_location().translated(-wm.active_cursor().hotspot()), wm.active_cursor().size() };
}

void WSCompositor::invalidate_cursor()
{
    invalidate(current_cursor_rect());
}

void WSCompositor::draw_geometry_label()
{
    auto& wm = WSWindowManager::the();
    auto* window_being_moved_or_resized = wm.m_drag_window ? wm.m_drag_window.ptr() : (wm.m_resize_window ? wm.m_resize_window.ptr() : nullptr);
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
    auto geometry_label_rect = Rect { 0, 0, wm.font().width(geometry_string) + 16, wm.font().glyph_height() + 10 };
    geometry_label_rect.center_within(window_being_moved_or_resized->rect());
    m_back_painter->fill_rect(geometry_label_rect, Color::WarmGray);
    m_back_painter->draw_rect(geometry_label_rect, Color::DarkGray);
    m_back_painter->draw_text(geometry_label_rect, geometry_string, TextAlignment::Center);
    m_last_geometry_label_rect = geometry_label_rect;
}

void WSCompositor::draw_cursor()
{
    auto& wm = WSWindowManager::the();
    Rect cursor_rect = current_cursor_rect();
    m_back_painter->blit(cursor_rect.location(), wm.active_cursor().bitmap(), wm.active_cursor().rect());
    m_last_cursor_rect = cursor_rect;
}
