/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>
#include <AK/Variant.h>
#include <AK/WeakPtr.h>
#include <LibCore/Object.h>
#include <LibGUI/FocusSource.h>
#include <LibGUI/Forward.h>
#include <LibGUI/WindowType.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Rect.h>
#include <LibGfx/StandardCursor.h>

namespace GUI {

class WindowBackingStore;

class Window : public Core::Object {
    C_OBJECT(Window)
public:
    virtual ~Window() override;

    static Window* from_window_id(int);

    bool is_modified() const;
    void set_modified(bool);

    bool is_modal() const { return m_modal; }
    void set_modal(bool);

    bool is_fullscreen() const { return m_fullscreen; }
    void set_fullscreen(bool);

    bool is_maximized() const;
    void set_maximized(bool);

    bool is_frameless() const { return m_frameless; }
    void set_frameless(bool);

    void set_forced_shadow(bool);

    bool is_resizable() const { return m_resizable; }
    void set_resizable(bool resizable) { m_resizable = resizable; }

    bool is_minimizable() const { return m_minimizable; }
    void set_minimizable(bool minimizable) { m_minimizable = minimizable; }

    bool is_closeable() const { return m_closeable; }
    void set_closeable(bool closeable) { m_closeable = closeable; }

    void set_double_buffering_enabled(bool);
    void set_has_alpha_channel(bool);
    bool has_alpha_channel() const { return m_has_alpha_channel; }
    void set_opacity(float);
    float opacity() const { return m_opacity_when_windowless; }

    void set_alpha_hit_threshold(float);
    float alpha_hit_threshold() const { return m_alpha_hit_threshold; }

    WindowType window_type() const { return m_window_type; }
    void set_window_type(WindowType);

    int window_id() const { return m_window_id; }

    void make_window_manager(unsigned event_mask);

    String title() const;
    void set_title(String);

    enum class CloseRequestDecision {
        StayOpen,
        Close,
    };

    Function<void()> on_close;
    Function<CloseRequestDecision()> on_close_request;
    Function<void(bool is_active_input)> on_active_input_change;
    Function<void(bool is_active_window)> on_active_window_change;

    int x() const { return rect().x(); }
    int y() const { return rect().y(); }
    int width() const { return rect().width(); }
    int height() const { return rect().height(); }

    Gfx::IntRect rect() const;
    Gfx::IntRect applet_rect_on_screen() const;
    Gfx::IntSize size() const { return rect().size(); }
    void set_rect(const Gfx::IntRect&);
    void set_rect(int x, int y, int width, int height) { set_rect({ x, y, width, height }); }

    Gfx::IntPoint position() const { return rect().location(); }

    Gfx::IntSize minimum_size() const;
    void set_minimum_size(const Gfx::IntSize&);
    void set_minimum_size(int width, int height) { set_minimum_size({ width, height }); }

    void move_to(int x, int y) { move_to({ x, y }); }
    void move_to(const Gfx::IntPoint& point) { set_rect({ point, size() }); }

    void resize(int width, int height) { resize({ width, height }); }
    void resize(const Gfx::IntSize& size) { set_rect({ position(), size }); }

    void center_on_screen();
    void center_within(const Window&);

    virtual void event(Core::Event&) override;

    bool is_visible() const;
    bool is_active() const;
    bool is_active_input() const { return m_is_active_input; }

    bool is_accessory() const { return m_accessory; }
    void set_accessory(bool accessory) { m_accessory = accessory; }

    void show();
    void hide();
    virtual void close();
    void move_to_front();

    void start_interactive_resize();

    Widget* main_widget() { return m_main_widget; }
    const Widget* main_widget() const { return m_main_widget; }
    void set_main_widget(Widget*);

    template<class T, class... Args>
    inline ErrorOr<NonnullRefPtr<T>> try_set_main_widget(Args&&... args)
    {
        auto widget = TRY(T::try_create(forward<Args>(args)...));
        set_main_widget(widget.ptr());
        return widget;
    }

    template<class T, class... Args>
    inline T& set_main_widget(Args&&... args)
    {
        auto widget = T::construct(forward<Args>(args)...);
        set_main_widget(widget.ptr());
        return *widget;
    }

    Widget* default_return_key_widget() { return m_default_return_key_widget; }
    Widget const* default_return_key_widget() const { return m_default_return_key_widget; }
    void set_default_return_key_widget(Widget*);

    Widget* focused_widget() { return m_focused_widget; }
    const Widget* focused_widget() const { return m_focused_widget; }
    void set_focused_widget(Widget*, FocusSource = FocusSource::Programmatic);

    void update();
    void update(const Gfx::IntRect&);

    void set_automatic_cursor_tracking_widget(Widget*);
    Widget* automatic_cursor_tracking_widget() { return m_automatic_cursor_tracking_widget.ptr(); }
    const Widget* automatic_cursor_tracking_widget() const { return m_automatic_cursor_tracking_widget.ptr(); }

    Widget* hovered_widget() { return m_hovered_widget.ptr(); }
    const Widget* hovered_widget() const { return m_hovered_widget.ptr(); }
    void set_hovered_widget(Widget*);

    Gfx::Bitmap* back_bitmap();

    Gfx::IntSize size_increment() const { return m_size_increment; }
    void set_size_increment(const Gfx::IntSize&);
    Gfx::IntSize base_size() const { return m_base_size; }
    void set_base_size(const Gfx::IntSize&);
    const Optional<Gfx::IntSize>& resize_aspect_ratio() const { return m_resize_aspect_ratio; }
    void set_resize_aspect_ratio(int width, int height) { set_resize_aspect_ratio(Gfx::IntSize(width, height)); }
    void set_no_resize_aspect_ratio() { set_resize_aspect_ratio({}); }
    void set_resize_aspect_ratio(const Optional<Gfx::IntSize>& ratio);

    void set_cursor(Gfx::StandardCursor);
    void set_cursor(NonnullRefPtr<Gfx::Bitmap>);

    void set_icon(const Gfx::Bitmap*);
    void apply_icon();
    const Gfx::Bitmap* icon() const { return m_icon.ptr(); }

    Vector<Widget&> focusable_widgets(FocusSource) const;

    void schedule_relayout();

    void refresh_system_theme();

    static void for_each_window(Badge<ConnectionToWindowServer>, Function<void(Window&)>);
    static void update_all_windows(Badge<ConnectionToWindowServer>);
    void notify_state_changed(Badge<ConnectionToWindowServer>, bool minimized, bool occluded);

    virtual bool is_visible_for_timer_purposes() const override { return m_visible_for_timer_purposes; }

    Action* action_for_key_event(const KeyEvent&);

    void did_add_widget(Badge<Widget>, Widget&);
    void did_remove_widget(Badge<Widget>, Widget&);

    Window* find_parent_window();

    void set_progress(Optional<int>);

    void update_cursor(Badge<Widget>) { update_cursor(); }

    void did_disable_focused_widget(Badge<Widget>);

    Menu& add_menu(String name);
    ErrorOr<NonnullRefPtr<Menu>> try_add_menu(String name);
    void flash_menubar_menu_for(const MenuItem&);

    void flush_pending_paints_immediately();

    Menubar& menubar() { return *m_menubar; }
    Menubar const& menubar() const { return *m_menubar; }

protected:
    Window(Core::Object* parent = nullptr);
    virtual void wm_event(WMEvent&);
    virtual void screen_rects_change_event(ScreenRectsChangeEvent&);
    virtual void applet_area_rect_change_event(AppletAreaRectChangeEvent&);

    virtual void enter_event(Core::Event&);
    virtual void leave_event(Core::Event&);

private:
    void update_cursor();
    void focus_a_widget_if_possible(FocusSource);

    void handle_drop_event(DropEvent&);
    void handle_mouse_event(MouseEvent&);
    void handle_multi_paint_event(MultiPaintEvent&);
    void handle_key_event(KeyEvent&);
    void handle_resize_event(ResizeEvent&);
    void handle_input_entered_or_left_event(Core::Event&);
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

    OwnPtr<WindowBackingStore> create_backing_store(const Gfx::IntSize&);
    void set_current_backing_store(WindowBackingStore&, bool flush_immediately = false);
    void flip(const Vector<Gfx::IntRect, 32>& dirty_rects);
    void force_update();

    bool are_cursors_the_same(AK::Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap>> const&, AK::Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap>> const&) const;

    WeakPtr<Widget> m_previously_focused_widget;

    OwnPtr<WindowBackingStore> m_front_store;
    OwnPtr<WindowBackingStore> m_back_store;

    NonnullRefPtr<Menubar> m_menubar;

    RefPtr<Gfx::Bitmap> m_icon;
    int m_window_id { 0 };
    float m_opacity_when_windowless { 1.0f };
    float m_alpha_hit_threshold { 0.0f };
    RefPtr<Widget> m_main_widget;
    WeakPtr<Widget> m_default_return_key_widget;
    WeakPtr<Widget> m_focused_widget;
    WeakPtr<Widget> m_automatic_cursor_tracking_widget;
    WeakPtr<Widget> m_hovered_widget;
    Gfx::IntRect m_rect_when_windowless;
    Gfx::IntSize m_minimum_size_when_windowless { 50, 50 };
    bool m_minimum_size_modified { false };
    String m_title_when_windowless;
    Vector<Gfx::IntRect, 32> m_pending_paint_event_rects;
    Gfx::IntSize m_size_increment;
    Gfx::IntSize m_base_size;
    WindowType m_window_type { WindowType::Normal };
    AK::Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap>> m_cursor { Gfx::StandardCursor::None };
    AK::Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap>> m_effective_cursor { Gfx::StandardCursor::None };
    bool m_is_active_input { false };
    bool m_has_alpha_channel { false };
    bool m_double_buffering_enabled { true };
    bool m_modal { false };
    bool m_resizable { true };
    Optional<Gfx::IntSize> m_resize_aspect_ratio {};
    bool m_minimizable { true };
    bool m_closeable { true };
    bool m_maximized_when_windowless { false };
    bool m_fullscreen { false };
    bool m_frameless { false };
    bool m_forced_shadow { false };
    bool m_layout_pending { false };
    bool m_visible_for_timer_purposes { true };
    bool m_visible { false };
    bool m_accessory { false };
    bool m_moved_by_client { false };
};

}

template<>
struct AK::Formatter<GUI::Window> : Formatter<Core::Object> {
};
