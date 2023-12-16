/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/Error.h>
#include <AK/Function.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/OwnPtr.h>
#include <AK/Variant.h>
#include <AK/WeakPtr.h>
#include <LibGUI/FocusSource.h>
#include <LibGUI/Forward.h>
#include <LibGUI/Object.h>
#include <LibGUI/ResizeDirection.h>
#include <LibGUI/WindowMode.h>
#include <LibGUI/WindowType.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Rect.h>
#include <LibGfx/StandardCursor.h>

namespace GUI {

class WindowBackingStore;

class Window : public GUI::Object {
    C_OBJECT(Window)
public:
    virtual ~Window() override;

    static Window* from_window_id(int);

    bool is_modified() const;
    void set_modified(bool);

    bool is_modal() const { return m_window_mode != WindowMode::Modeless; }
    bool is_blocking() const { return m_window_mode == WindowMode::Blocking; }

    bool is_popup() const { return m_window_type == WindowType::Popup; }
    bool is_autocomplete() const { return m_window_type == WindowType::Autocomplete; }

    bool is_fullscreen() const { return m_fullscreen; }
    void set_fullscreen(bool);

    bool is_maximized() const { return m_maximized; }
    void set_maximized(bool);

    bool is_minimized() const { return m_minimized; }
    void set_minimized(bool);

    bool is_frameless() const { return m_frameless; }
    void set_frameless(bool);

    void set_forced_shadow(bool);

    bool is_resizable() const { return m_resizable; }
    void set_resizable(bool resizable) { m_resizable = resizable; }

    bool is_obeying_widget_min_size() { return m_obey_widget_min_size; }
    void set_obey_widget_min_size(bool);

    bool is_auto_shrinking() const { return m_auto_shrink; }
    void set_auto_shrink(bool);

    bool is_minimizable() const { return m_minimizable; }
    void set_minimizable(bool minimizable) { m_minimizable = minimizable; }

    bool is_closeable() const { return m_closeable; }
    void set_closeable(bool closeable) { m_closeable = closeable; }

    void set_double_buffering_enabled(bool);
    void set_has_alpha_channel(bool);
    bool has_alpha_channel() const { return m_has_alpha_channel; }

    void set_alpha_hit_threshold(float);
    float alpha_hit_threshold() const { return m_alpha_hit_threshold; }

    WindowType window_type() const { return m_window_type; }
    void set_window_type(WindowType);

    WindowMode window_mode() const { return m_window_mode; }
    void set_window_mode(WindowMode);

    int window_id() const { return m_window_id; }

    void make_window_manager(unsigned event_mask);

    ByteString title() const;
    void set_title(ByteString);

    enum class CloseRequestDecision {
        StayOpen,
        Close,
    };

    Function<void()> on_font_change;
    Function<void()> on_close;
    Function<CloseRequestDecision()> on_close_request;
    Function<void(bool is_preempted)> on_input_preemption_change;
    Function<void(bool is_active_window)> on_active_window_change;

    int x() const { return rect().x(); }
    int y() const { return rect().y(); }
    int width() const { return rect().width(); }
    int height() const { return rect().height(); }

    Gfx::IntRect rect() const;
    Gfx::IntRect floating_rect() const;
    Gfx::IntRect applet_rect_on_screen() const;
    Gfx::IntSize size() const { return rect().size(); }
    void set_rect(Gfx::IntRect const&);
    void set_rect(int x, int y, int width, int height) { set_rect({ x, y, width, height }); }

    Gfx::IntPoint position() const { return rect().location(); }

    Gfx::IntSize minimum_size() const;
    void set_minimum_size(Gfx::IntSize);
    void set_minimum_size(int width, int height) { set_minimum_size({ width, height }); }

    void move_to(int x, int y) { move_to({ x, y }); }
    void move_to(Gfx::IntPoint point) { set_rect({ point, size() }); }

    void resize(int width, int height) { resize({ width, height }); }
    void resize(Gfx::IntSize size) { set_rect({ position(), size }); }

    void center_on_screen();
    void constrain_to_desktop();

    void center_within(Window const&);
    void center_within(Gfx::IntRect const&);

    virtual void event(Core::Event&) override;

    bool is_visible() const;
    bool is_active() const;
    bool is_focusable() const { return is_active() || is_popup() || is_autocomplete(); }

    void show();
    void hide();
    virtual void close();
    void move_to_front();

    void start_interactive_resize(ResizeDirection resize_direction);

    Widget* main_widget() { return m_main_widget; }
    Widget const* main_widget() const { return m_main_widget; }
    void set_main_widget(Widget*);

    template<class T, class... Args>
    inline NonnullRefPtr<T> set_main_widget(Args&&... args)
    {
        auto widget = T::construct(forward<Args>(args)...);
        set_main_widget(widget.ptr());
        return widget;
    }

    Widget* default_return_key_widget() { return m_default_return_key_widget; }
    Widget const* default_return_key_widget() const { return m_default_return_key_widget; }
    void set_default_return_key_widget(Widget*);

    Widget* focused_widget() { return m_focused_widget; }
    Widget const* focused_widget() const { return m_focused_widget; }
    void set_focused_widget(Widget*, FocusSource = FocusSource::Programmatic);

    void update();
    void update(Gfx::IntRect const&);

    void set_automatic_cursor_tracking_widget(Widget*);
    Widget* automatic_cursor_tracking_widget() { return m_automatic_cursor_tracking_widget.ptr(); }
    Widget const* automatic_cursor_tracking_widget() const { return m_automatic_cursor_tracking_widget.ptr(); }

    Widget* hovered_widget() { return m_hovered_widget.ptr(); }
    Widget const* hovered_widget() const { return m_hovered_widget.ptr(); }
    void set_hovered_widget(Widget*);

    Gfx::Bitmap* back_bitmap();

    Gfx::IntSize size_increment() const { return m_size_increment; }
    void set_size_increment(Gfx::IntSize);
    Gfx::IntSize base_size() const { return m_base_size; }
    void set_base_size(Gfx::IntSize);
    Optional<Gfx::IntSize> const& resize_aspect_ratio() const { return m_resize_aspect_ratio; }
    void set_resize_aspect_ratio(int width, int height) { set_resize_aspect_ratio(Gfx::IntSize(width, height)); }
    void set_no_resize_aspect_ratio() { set_resize_aspect_ratio({}); }
    void set_resize_aspect_ratio(Optional<Gfx::IntSize> const& ratio);

    void set_cursor(Gfx::StandardCursor);
    void set_cursor(NonnullRefPtr<Gfx::Bitmap const>);

    void set_icon(Gfx::Bitmap const*);
    void apply_icon();
    Gfx::Bitmap const* icon() const { return m_icon.ptr(); }

    Vector<Widget&> focusable_widgets(FocusSource) const;

    void schedule_relayout();

    void refresh_system_theme();

    static void for_each_window(Badge<ConnectionToWindowServer>, Function<void(Window&)>);
    static void update_all_windows(Badge<ConnectionToWindowServer>);
    void notify_state_changed(Badge<ConnectionToWindowServer>, bool minimized, bool maximized, bool occluded);

    virtual bool is_visible_for_timer_purposes() const override { return m_visible_for_timer_purposes; }

    Action* action_for_shortcut(Shortcut const&);

    void did_add_widget(Badge<Widget>, Widget&);
    void did_remove_widget(Badge<Widget>, Widget&);

    Window* find_parent_window();

    void set_progress(Optional<int>);

    void update_cursor(Badge<Widget>) { update_cursor(); }

    void did_disable_focused_widget(Badge<Widget>);

    [[nodiscard]] NonnullRefPtr<Menu> add_menu(String name);
    void add_menu(NonnullRefPtr<Menu> menu);
    void flash_menubar_menu_for(MenuItem const&);

    void flush_pending_paints_immediately();

    Menubar& menubar() { return *m_menubar; }
    Menubar const& menubar() const { return *m_menubar; }

    void set_blocks_emoji_input(bool b) { m_blocks_emoji_input = b; }
    bool blocks_emoji_input() const { return m_blocks_emoji_input; }

    void set_always_on_top(bool always_on_top = true);

    enum class ShortcutPropagationBoundary {
        Window,
        Application,
    };

    void propagate_shortcuts(KeyEvent& event, Widget* widget, ShortcutPropagationBoundary = ShortcutPropagationBoundary::Application);

    void restore_size_and_position(StringView domain, StringView group = "Window"sv, Optional<Gfx::IntSize> fallback_size = {}, Optional<Gfx::IntPoint> fallback_position = {});
    void save_size_and_position(StringView domain, StringView group = "Window"sv) const;
    void save_size_and_position_on_close(StringView domain, StringView group = "Window"sv);

protected:
    Window(Core::EventReceiver* parent = nullptr);
    virtual void wm_event(WMEvent&);
    virtual void screen_rects_change_event(ScreenRectsChangeEvent&);
    virtual void applet_area_rect_change_event(AppletAreaRectChangeEvent&);

    virtual void enter_event(Core::Event&);
    virtual void leave_event(Core::Event&);

private:
    void update_min_size();

    void update_cursor();
    void focus_a_widget_if_possible(FocusSource);

    void handle_drop_event(DropEvent&);
    void handle_mouse_event(MouseEvent&);
    void handle_multi_paint_event(MultiPaintEvent&);
    void handle_key_event(KeyEvent&);
    void handle_resize_event(ResizeEvent&);
    void handle_input_preemption_event(Core::Event&);
    void handle_became_active_or_inactive_event(Core::Event&);
    void handle_close_request();
    void handle_theme_change_event(ThemeChangeEvent&);
    void handle_fonts_change_event(FontsChangeEvent&);
    void handle_screen_rects_change_event(ScreenRectsChangeEvent&);
    void handle_applet_area_rect_change_event(AppletAreaRectChangeEvent&);
    void handle_drag_move_event(DragEvent&);
    void handle_entered_event(Core::Event&);
    void handle_left_event(Core::Event&);

    void server_did_destroy();

    ErrorOr<NonnullOwnPtr<WindowBackingStore>> create_backing_store(Gfx::IntSize);
    Gfx::IntSize backing_store_size(Gfx::IntSize) const;
    void set_current_backing_store(WindowBackingStore&, bool flush_immediately = false) const;
    void flip(Vector<Gfx::IntRect, 32> const& dirty_rects);
    void force_update();

    WeakPtr<Widget> m_previously_focused_widget;

    OwnPtr<WindowBackingStore> m_front_store;
    OwnPtr<WindowBackingStore> m_back_store;

    NonnullRefPtr<Menubar> m_menubar;

    RefPtr<Gfx::Bitmap const> m_icon;
    int m_window_id { 0 };
    float m_alpha_hit_threshold { 0.0f };
    RefPtr<Widget> m_main_widget;
    WeakPtr<Widget> m_default_return_key_widget;
    WeakPtr<Widget> m_focused_widget;
    WeakPtr<Widget> m_automatic_cursor_tracking_widget;
    WeakPtr<Widget> m_hovered_widget;
    Gfx::IntRect m_rect_when_windowless;
    Gfx::IntSize m_minimum_size_when_windowless { 0, 0 };
    Gfx::IntRect m_floating_rect;
    ByteString m_title_when_windowless;
    Vector<Gfx::IntRect, 32> m_pending_paint_event_rects;
    Gfx::IntSize m_size_increment;
    Gfx::IntSize m_base_size;
    WindowType m_window_type { WindowType::Normal };
    WindowMode m_window_mode { WindowMode::Modeless };
    AK::Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap const>> m_cursor { Gfx::StandardCursor::None };
    AK::Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap const>> m_effective_cursor { Gfx::StandardCursor::None };
    bool m_has_alpha_channel { false };
    bool m_double_buffering_enabled { true };
    bool m_resizable { true };
    bool m_obey_widget_min_size { true };
    Optional<Gfx::IntSize> m_resize_aspect_ratio {};
    bool m_minimizable { true };
    bool m_closeable { true };
    bool m_maximized { false };
    bool m_minimized { false };
    bool m_fullscreen { false };
    bool m_frameless { false };
    bool m_forced_shadow { false };
    bool m_layout_pending { false };
    bool m_visible_for_timer_purposes { true };
    bool m_visible { false };
    bool m_moved_by_client { false };
    bool m_blocks_emoji_input { false };
    bool m_resizing { false };
    bool m_auto_shrink { false };
    bool m_save_size_and_position_on_close { false };
    StringView m_save_domain;
    StringView m_save_group;

    pid_t m_pid;
};

}

template<>
struct AK::Formatter<GUI::Window> : Formatter<Core::EventReceiver> {
};
