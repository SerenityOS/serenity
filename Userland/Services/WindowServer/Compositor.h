/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <LibCore/EventReceiver.h>
#include <LibGfx/Color.h>
#include <LibGfx/DisjointRectSet.h>
#include <LibGfx/Font/Font.h>
#include <WindowServer/Overlays.h>

namespace WindowServer {

class Animation;
class ConnectionFromClient;
class Compositor;
class Cursor;
class MultiScaleBitmaps;
class Window;
class WindowManager;
class WindowStack;

enum class WallpaperMode {
    Tile,
    Center,
    Stretch,
    Unchecked
};

struct CompositorScreenData {
    RefPtr<Gfx::Bitmap> m_front_bitmap;
    RefPtr<Gfx::Bitmap> m_back_bitmap;
    RefPtr<Gfx::Bitmap> m_temp_bitmap;
    RefPtr<Gfx::Bitmap const> m_wallpaper_bitmap;
    OwnPtr<Gfx::Painter> m_back_painter;
    OwnPtr<Gfx::Painter> m_front_painter;
    OwnPtr<Gfx::Painter> m_temp_painter;
    RefPtr<Gfx::Bitmap> m_cursor_back_bitmap;
    OwnPtr<Gfx::Painter> m_cursor_back_painter;
    Gfx::IntRect m_last_cursor_rect;
    OwnPtr<ScreenNumberOverlay> m_screen_number_overlay;
    OwnPtr<WindowStackSwitchOverlay> m_window_stack_switch_overlay;
    bool m_buffers_are_flipped { false };
    bool m_screen_can_set_buffer { false };
    bool m_has_flipped { false };
    bool m_cursor_back_is_valid { false };
    bool m_have_flush_rects { false };

    Gfx::DisjointIntRectSet m_flush_rects;
    Gfx::DisjointIntRectSet m_flush_transparent_rects;
    Gfx::DisjointIntRectSet m_flush_special_rects;

    Gfx::Painter& overlay_painter() { return *m_temp_painter; }

    void init_bitmaps(Compositor&, Screen&);
    void flip_buffers(Screen&);
    void draw_cursor(Screen&, Gfx::IntRect const&);
    bool restore_cursor_back(Screen&, Gfx::IntRect&);
    void clear_wallpaper_bitmap();

    template<typename F>
    IterationDecision for_each_intersected_flushing_rect(Gfx::IntRect const& intersecting_rect, F f)
    {
        auto iterate_flush_rects = [&](Gfx::DisjointIntRectSet const& flush_rects) {
            for (auto& rect : flush_rects.rects()) {
                auto intersection = intersecting_rect.intersected(rect);
                if (intersection.is_empty())
                    continue;
                IterationDecision decision = f(intersection);
                if (decision != IterationDecision::Continue)
                    return decision;
            }
            return IterationDecision::Continue;
        };
        auto decision = iterate_flush_rects(m_flush_rects);
        if (decision != IterationDecision::Continue)
            return decision;
        // We do not have to iterate m_flush_special_rects here as these
        // technically should be removed anyway. m_flush_rects and
        // m_flush_transparent_rects should cover everything already
        return iterate_flush_rects(m_flush_transparent_rects);
    }
};

class Compositor final : public Core::EventReceiver {
    C_OBJECT(Compositor)
    friend struct CompositorScreenData;
    friend class Overlay;

public:
    static Compositor& the();

    void compose();
    void invalidate_window();
    void invalidate_screen();
    void invalidate_screen(Gfx::IntRect const&);
    void invalidate_screen(Gfx::DisjointIntRectSet const&);

    void screen_resolution_changed();

    bool set_background_color(ByteString const& background_color);

    bool set_wallpaper_mode(ByteString const& mode);

    bool set_wallpaper(RefPtr<Gfx::Bitmap const>);
    RefPtr<Gfx::Bitmap const> wallpaper_bitmap() const { return m_wallpaper; }

    void invalidate_cursor(bool = false);
    Gfx::IntRect current_cursor_rect() const;
    Cursor const* current_cursor() const { return m_current_cursor; }
    void current_cursor_was_reloaded(Cursor const* new_cursor) { change_cursor(new_cursor); }

    void increment_display_link_count(Badge<ConnectionFromClient>);
    void decrement_display_link_count(Badge<ConnectionFromClient>);

    void increment_show_screen_number(Badge<ConnectionFromClient>);
    void decrement_show_screen_number(Badge<ConnectionFromClient>);
    bool showing_screen_numbers() const { return m_show_screen_number_count > 0; }

    void invalidate_after_theme_or_font_change()
    {
        update_fonts();
        invalidate_occlusions();
        overlays_theme_changed();
        invalidate_screen();
    }

    void invalidate_occlusions() { m_occlusions_dirty = true; }
    void overlay_rects_changed();

    template<typename T, typename... Args>
    OwnPtr<T> create_overlay(Args&&... args)
    {
        return adopt_own(*new T(forward<Args>(args)...));
    }

    template<typename F>
    IterationDecision for_each_overlay(F f)
    {
        for (auto& overlay : m_overlay_list) {
            IterationDecision decision = f(overlay);
            if (decision != IterationDecision::Continue)
                return decision;
        }
        return IterationDecision::Continue;
    }

    template<typename F>
    IterationDecision for_each_rendering_window_stack(F f)
    {
        VERIFY(m_current_window_stack);
        IterationDecision decision = f(*m_current_window_stack);
        if (decision != IterationDecision::Continue)
            return decision;
        if (m_transitioning_to_window_stack)
            decision = f(*m_transitioning_to_window_stack);
        return decision;
    }

    [[nodiscard]] WindowStack& get_rendering_window_stacks(WindowStack*& transitioning_window_stack)
    {
        transitioning_window_stack = m_transitioning_to_window_stack;
        return *m_current_window_stack;
    }

    bool is_switching_window_stacks() const { return m_transitioning_to_window_stack != nullptr; }
    void switch_to_window_stack(WindowStack&, bool);
    void set_current_window_stack_no_transition(WindowStack&);
    void invalidate_for_window_stack_merge_or_change();

    void did_construct_window_manager(Badge<WindowManager>);

    Gfx::Bitmap const* cursor_bitmap_for_screenshot(Badge<ConnectionFromClient>, Screen&) const;
    Gfx::Bitmap const& front_bitmap_for_screenshot(Badge<ConnectionFromClient>, Screen&) const;
    Gfx::Color color_at_position(Badge<ConnectionFromClient>, Screen&, Gfx::IntPoint) const;

    void register_animation(Badge<Animation>, Animation&);
    void unregister_animation(Badge<Animation>, Animation&);

    void set_flash_flush(bool b) { m_flash_flush = b; }

    static NonnullOwnPtr<CompositorScreenData> create_screen_data(Badge<Screen>)
    {
        return adopt_own(*new CompositorScreenData());
    }

private:
    Compositor();
    void init_bitmaps();
    void invalidate_current_screen_number_rects();
    void overlays_theme_changed();

    void render_overlays();
    void add_overlay(Overlay&);
    void remove_overlay(Overlay&);
    void update_fonts();
    void notify_display_links();
    void start_compose_async_timer();
    void recompute_overlay_rects();
    void recompute_occlusions();
    void change_cursor(Cursor const*);
    void flush(Screen&);
    Gfx::IntPoint window_transition_offset(Window&);
    void update_animations(Screen&, Gfx::DisjointIntRectSet& flush_rects);
    void create_window_stack_switch_overlay(WindowStack&);
    void remove_window_stack_switch_overlays();
    void stop_window_stack_switch_overlay_timer();
    void start_window_stack_switch_overlay_timer();
    void finish_window_stack_switch();
    void update_wallpaper_bitmap();

    RefPtr<Core::Timer> m_compose_timer;
    RefPtr<Core::Timer> m_immediate_compose_timer;
    bool m_flash_flush { false };
    bool m_occlusions_dirty { true };
    bool m_invalidated_any { true };
    bool m_invalidated_window { false };
    bool m_invalidated_cursor { false };
    bool m_overlay_rects_changed { false };
    bool m_animations_running { false };

    IntrusiveList<&Overlay::m_list_node> m_overlay_list;
    Gfx::DisjointIntRectSet m_overlay_rects;
    Gfx::DisjointIntRectSet m_last_rendered_overlay_rects;
    Gfx::DisjointIntRectSet m_dirty_screen_rects;
    Gfx::DisjointIntRectSet m_opaque_wallpaper_rects;
    Gfx::DisjointIntRectSet m_transparent_wallpaper_rects;

    WallpaperMode m_wallpaper_mode { WallpaperMode::Unchecked };
    RefPtr<Gfx::Bitmap const> m_wallpaper;

    Cursor const* m_current_cursor { nullptr };
    Screen* m_current_cursor_screen { nullptr };
    unsigned m_current_cursor_frame { 0 };
    RefPtr<Core::Timer> m_cursor_timer;

    RefPtr<Core::Timer> m_display_link_notify_timer;
    size_t m_display_link_count { 0 };

    WindowStack* m_current_window_stack { nullptr };
    WindowStack* m_transitioning_to_window_stack { nullptr };
    RefPtr<Animation> m_window_stack_transition_animation;
    RefPtr<Core::Timer> m_stack_switch_overlay_timer;

    size_t m_show_screen_number_count { 0 };
    Optional<Gfx::Color> m_custom_background_color;

    HashTable<Animation*> m_animations;
};

}
