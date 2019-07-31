#include "WSCompositor.h"
#include "WSEvent.h"
#include "WSEventLoop.h"
#include "WSScreen.h"
#include "WSWindow.h"
#include "WSWindowManager.h"
#include <LibDraw/Font.h>
#include <LibDraw/PNGLoader.h>
#include <LibDraw/Painter.h>

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
    auto size = WSScreen::the().size();
    m_front_bitmap = GraphicsBitmap::create_wrapper(GraphicsBitmap::Format::RGB32, size, WSScreen::the().scanline(0));
    m_back_bitmap = GraphicsBitmap::create_wrapper(GraphicsBitmap::Format::RGB32, size, WSScreen::the().scanline(size.height()));

    m_front_painter = make<Painter>(*m_front_bitmap);
    m_back_painter = make<Painter>(*m_back_bitmap);

    m_wallpaper_path = "/res/wallpapers/retro.rgb";
    m_wallpaper = GraphicsBitmap::load_from_file(GraphicsBitmap::Format::RGBA32, m_wallpaper_path, { 1024, 768 });

    m_compose_timer.on_timeout = [=]() {
#if defined(COMPOSITOR_DEBUG)
        dbgprintf("WSCompositor: delayed frame callback: %d rects\n", m_dirty_rects.size());
#endif
        compose();
    };
    m_compose_timer.set_single_shot(true);
    m_compose_timer.set_interval(1000 / 60);
    m_immediate_compose_timer.on_timeout = [=]() {
#if defined(COMPOSITOR_DEBUG)
        dbgprintf("WSCompositor: immediate frame callback: %d rects\n", m_dirty_rects.size());
#endif
        compose();
    };
    m_immediate_compose_timer.set_single_shot(true);
    m_immediate_compose_timer.set_interval(0);
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

    for (auto& dirty_rect : dirty_rects.rects()) {
        if (wm.any_opaque_window_contains_rect(dirty_rect))
            continue;
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
                m_back_painter->blit_tiled(dirty_rect.location(), *m_wallpaper, dirty_rect);
            } else {
                float hscale = (float)m_wallpaper->size().width() / (float)ws.size().width();
                float vscale = (float)m_wallpaper->size().height() / (float)ws.size().height();

                m_back_painter->blit_scaled(dirty_rect, *m_wallpaper, dirty_rect, hscale, vscale);
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
            Rect dirty_rect_in_window_coordinates = Rect::intersection(dirty_rect, window.rect());
            if (dirty_rect_in_window_coordinates.is_empty())
                continue;
            dirty_rect_in_window_coordinates.move_by(-window.position());
            auto dst = window.position();
            dst.move_by(dirty_rect_in_window_coordinates.location());

            m_back_painter->blit(dst, *backing_store, dirty_rect_in_window_coordinates, window.opacity());

            if (backing_store->width() < window.width()) {
                Rect right_fill_rect { window.x() + backing_store->width(), window.y(), window.width() - backing_store->width(), window.height() };
                m_back_painter->fill_rect(right_fill_rect, window.background_color());
            }

            if (backing_store->height() < window.height()) {
                Rect bottom_fill_rect { window.x(), window.y() + backing_store->height(), window.width(), window.height() - backing_store->height() };
                m_back_painter->fill_rect(bottom_fill_rect, window.background_color());
            }
        }
        return IterationDecision::Continue;
    };

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

    const RGBA32* front_ptr = m_front_bitmap->scanline(rect.y()) + rect.x();
    RGBA32* back_ptr = m_back_bitmap->scanline(rect.y()) + rect.x();
    size_t pitch = m_back_bitmap->pitch();

    for (int y = 0; y < rect.height(); ++y) {
        fast_u32_copy(back_ptr, front_ptr, rect.width());
        front_ptr = (const RGBA32*)((const u8*)front_ptr + pitch);
        back_ptr = (RGBA32*)((u8*)back_ptr + pitch);
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
    if (!m_compose_timer.is_active()) {
#if defined(COMPOSITOR_DEBUG)
        dbgprintf("Invalidated (starting immediate frame): %dx%d %dx%d\n", a_rect.x(), a_rect.y(), a_rect.width(), a_rect.height());
#endif
        m_compose_timer.start();
        m_immediate_compose_timer.start();
    } else {
#if defined(COMPOSITOR_DEBUG)
        dbgprintf("Invalidated (frame callback pending): %dx%d %dx%d\n", a_rect.x(), a_rect.y(), a_rect.width(), a_rect.height());
#endif
    }
}

bool WSCompositor::set_wallpaper(const String& path, Function<void(bool)>&& callback)
{
    struct Context {
        String path;
        RefPtr<GraphicsBitmap> bitmap;
        Function<void(bool)> callback;
    };
    auto context = make<Context>();
    context->path = path;
    context->callback = move(callback);

    int rc = create_thread([](void* ctx) -> int {
        OwnPtr<Context> context((Context*)ctx);
        context->bitmap = load_png(context->path);
        if (!context->bitmap) {
            context->callback(false);
            exit_thread(0);
            return 0;
        }
        the().deferred_invoke([context = move(context)](auto&) {
            the().finish_setting_wallpaper(context->path, *context->bitmap);
            context->callback(true);
        });
        exit_thread(0);
        return 0;
    },
        context.leak_ptr());
    ASSERT(rc > 0);

    return true;
}

void WSCompositor::finish_setting_wallpaper(const String& path, NonnullRefPtr<GraphicsBitmap>&& bitmap)
{
    m_wallpaper_path = path;
    m_wallpaper = move(bitmap);
    invalidate();
}

void WSCompositor::flip_buffers()
{
    swap(m_front_bitmap, m_back_bitmap);
    swap(m_front_painter, m_back_painter);
    int new_y_offset = m_buffers_are_flipped ? 0 : WSScreen::the().height();
    WSScreen::the().set_y_offset(new_y_offset);
    m_buffers_are_flipped = !m_buffers_are_flipped;
}

void WSCompositor::set_resolution(int width, int height)
{
    auto screen_rect = WSScreen::the().rect();
    if (screen_rect.width() == width && screen_rect.height() == height)
        return;
    m_wallpaper_path = {};
    m_wallpaper = nullptr;
    WSScreen::the().set_resolution(width, height);
    m_front_bitmap = GraphicsBitmap::create_wrapper(GraphicsBitmap::Format::RGB32, { width, height }, WSScreen::the().scanline(0));
    m_back_bitmap = GraphicsBitmap::create_wrapper(GraphicsBitmap::Format::RGB32, { width, height }, WSScreen::the().scanline(height));
    m_front_painter = make<Painter>(*m_front_bitmap);
    m_back_painter = make<Painter>(*m_back_bitmap);
    m_buffers_are_flipped = false;
    invalidate();
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
    Color inner_color = Color::White;
    Color outer_color = Color::Black;
    if (WSScreen::the().mouse_button_state() & (unsigned)MouseButton::Left)
        swap(inner_color, outer_color);
    m_back_painter->blit(cursor_rect.location(), wm.active_cursor().bitmap(), wm.active_cursor().rect());
    m_last_cursor_rect = cursor_rect;
}
