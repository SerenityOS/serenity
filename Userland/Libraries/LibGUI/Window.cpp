/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/HashMap.h>
#include <AK/IDAllocator.h>
#include <AK/JsonObject.h>
#include <AK/NeverDestroyed.h>
#include <AK/ScopeGuard.h>
#include <LibConfig/Client.h>
#include <LibCore/EventLoop.h>
#include <LibCore/MimeData.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/ConnectionToWindowManagerServer.h>
#include <LibGUI/ConnectionToWindowServer.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/Event.h>
#include <LibGUI/MenuItem.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGfx/Bitmap.h>
#include <LibGfx/Palette.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

namespace GUI {

static i32 s_next_backing_store_serial;
static IDAllocator s_window_id_allocator;

class WindowBackingStore {
public:
    explicit WindowBackingStore(NonnullRefPtr<Gfx::Bitmap> bitmap)
        : m_bitmap(move(bitmap))
        , m_serial(++s_next_backing_store_serial)
        , m_visible_size(m_bitmap->size())
    {
    }

    Gfx::Bitmap& bitmap() { return *m_bitmap; }
    Gfx::Bitmap const& bitmap() const { return *m_bitmap; }

    Gfx::IntSize size() const { return m_bitmap->size(); }

    i32 serial() const { return m_serial; }

    Gfx::IntSize visible_size() const { return m_visible_size; }
    void set_visible_size(Gfx::IntSize visible_size) { m_visible_size = visible_size; }

    [[nodiscard]] bool is_volatile() const { return m_volatile; }

    void set_volatile()
    {
        if (m_volatile)
            return;
#ifdef AK_OS_SERENITY
        int rc = madvise(m_bitmap->scanline_u8(0), m_bitmap->data_size(), MADV_SET_VOLATILE);
        if (rc < 0) {
            perror("madvise(MADV_SET_VOLATILE)");
            VERIFY_NOT_REACHED();
        }
#endif
        m_volatile = true;
    }

    // Returns true if making the bitmap non-volatile succeeded. `was_purged` indicates status of contents.
    // Returns false if there was not enough memory.
    [[nodiscard]] bool set_nonvolatile(bool& was_purged)
    {
        if (!m_volatile) {
            was_purged = false;
            return true;
        }

#ifdef AK_OS_SERENITY
        int rc = madvise(m_bitmap->scanline_u8(0), m_bitmap->data_size(), MADV_SET_NONVOLATILE);
        if (rc < 0) {
            if (errno == ENOMEM) {
                was_purged = true;
                return false;
            }
            perror("madvise(MADV_SET_NONVOLATILE)");
            VERIFY_NOT_REACHED();
        }
        was_purged = rc != 0;
#endif
        m_volatile = false;
        return true;
    }

private:
    NonnullRefPtr<Gfx::Bitmap> m_bitmap;
    i32 const m_serial;
    Gfx::IntSize m_visible_size;
    bool m_volatile { false };
};

static NeverDestroyed<HashTable<Window*>> all_windows;
static NeverDestroyed<HashMap<int, Window*>> reified_windows;

Window* Window::from_window_id(int window_id)
{
    auto it = reified_windows->find(window_id);
    if (it != reified_windows->end())
        return (*it).value;
    return nullptr;
}

Window::Window(Core::EventReceiver* parent)
    : GUI::Object(parent)
    , m_menubar(Menubar::construct())
    , m_pid(getpid())
{
    if (parent)
        set_window_mode(WindowMode::Passive);

    all_windows->set(this);
    m_rect_when_windowless = { -5000, -5000, 0, 0 };
    m_floating_rect = { -5000, -5000, 0, 0 };
    m_title_when_windowless = "GUI::Window";

    REGISTER_DEPRECATED_STRING_PROPERTY("title", title, set_title)

    register_property(
        "visible"sv, [this] { return is_visible(); }, nullptr, nullptr);
    register_property(
        "active"sv, [this] { return is_active(); }, nullptr, nullptr);

    REGISTER_BOOL_PROPERTY("minimizable", is_minimizable, set_minimizable);
    REGISTER_BOOL_PROPERTY("resizable", is_resizable, set_resizable);
    REGISTER_BOOL_PROPERTY("fullscreen", is_fullscreen, set_fullscreen);
    REGISTER_RECT_PROPERTY("rect", rect, set_rect);
    REGISTER_SIZE_PROPERTY("base_size", base_size, set_base_size);
    REGISTER_SIZE_PROPERTY("size_increment", size_increment, set_size_increment);
    REGISTER_BOOL_PROPERTY("obey_widget_min_size", is_obeying_widget_min_size, set_obey_widget_min_size);
}

Window::~Window()
{
    all_windows->remove(this);
    hide();
}

void Window::close()
{
    hide();
    if (m_save_size_and_position_on_close)
        save_size_and_position(m_save_domain, m_save_group);
    if (on_close)
        on_close();
}

void Window::move_to_front()
{
    if (!is_visible())
        return;

    ConnectionToWindowServer::the().async_move_window_to_front(m_window_id);
}

void Window::show()
{
    if (is_visible())
        return;

    auto* parent_window = find_parent_window();

    m_window_id = s_window_id_allocator.allocate();

    Gfx::IntRect launch_origin_rect;
    if (auto* launch_origin_rect_string = getenv("__libgui_launch_origin_rect")) {
        auto parts = StringView { launch_origin_rect_string, strlen(launch_origin_rect_string) }.split_view(',');
        if (parts.size() == 4) {
            launch_origin_rect = Gfx::IntRect {
                parts[0].to_number<int>().value_or(0),
                parts[1].to_number<int>().value_or(0),
                parts[2].to_number<int>().value_or(0),
                parts[3].to_number<int>().value_or(0),
            };
        }
        unsetenv("__libgui_launch_origin_rect");
    }

    update_min_size();

    ConnectionToWindowServer::the().async_create_window(
        m_window_id,
        m_pid,
        m_rect_when_windowless,
        !m_moved_by_client,
        m_has_alpha_channel,
        m_minimizable,
        m_closeable,
        m_resizable,
        m_fullscreen,
        m_frameless,
        m_forced_shadow,
        m_alpha_hit_threshold,
        m_base_size,
        m_size_increment,
        m_minimum_size_when_windowless,
        m_resize_aspect_ratio,
        (i32)m_window_type,
        (i32)m_window_mode,
        m_title_when_windowless,
        parent_window ? parent_window->window_id() : 0,
        launch_origin_rect);
    m_visible = true;
    m_visible_for_timer_purposes = true;

    apply_icon();

    m_menubar->for_each_menu([&](Menu& menu) {
        menu.realize_menu_if_needed();
        ConnectionToWindowServer::the().async_add_menu(m_window_id, menu.menu_id());
        return IterationDecision::Continue;
    });

    set_maximized(m_maximized);
    reified_windows->set(m_window_id, this);
    Application::the()->did_create_window({});
    update();
}

Window* Window::find_parent_window()
{
    for (auto* ancestor = parent(); ancestor; ancestor = ancestor->parent()) {
        if (is<Window>(ancestor))
            return static_cast<Window*>(ancestor);
    }
    return nullptr;
}

void Window::server_did_destroy()
{
    reified_windows->remove(m_window_id);
    m_window_id = 0;
    m_visible = false;
    m_pending_paint_event_rects.clear();
    m_back_store = nullptr;
    m_front_store = nullptr;
    m_cursor = Gfx::StandardCursor::None;
}

void Window::hide()
{
    if (!is_visible())
        return;

    // NOTE: Don't bother asking WindowServer to destroy windows during application teardown.
    //       All our windows will be automatically garbage-collected by WindowServer anyway.
    if (GUI::Application::in_teardown())
        return;

    m_rect_when_windowless = rect();
    m_floating_rect = floating_rect();

    auto destroyed_window_ids = ConnectionToWindowServer::the().destroy_window(m_window_id);
    server_did_destroy();

    for (auto child_window_id : destroyed_window_ids) {
        if (auto* window = Window::from_window_id(child_window_id)) {
            window->server_did_destroy();
        }
    }

    if (auto* app = Application::the()) {
        bool app_has_visible_windows = false;
        for (auto& window : *all_windows) {
            if (window->is_visible()) {
                app_has_visible_windows = true;
                break;
            }
        }
        if (!app_has_visible_windows)
            app->did_delete_last_window({});
    }
}

void Window::set_title(ByteString title)
{
    m_title_when_windowless = move(title);
    if (!is_visible())
        return;
    ConnectionToWindowServer::the().async_set_window_title(m_window_id, m_title_when_windowless);
}

ByteString Window::title() const
{
    if (!is_visible())
        return m_title_when_windowless;
    return ConnectionToWindowServer::the().get_window_title(m_window_id);
}

Gfx::IntRect Window::applet_rect_on_screen() const
{
    VERIFY(m_window_type == WindowType::Applet);
    return ConnectionToWindowServer::the().get_applet_rect_on_screen(m_window_id);
}

Gfx::IntRect Window::rect() const
{
    if (!is_visible())
        return m_rect_when_windowless;
    return ConnectionToWindowServer::the().get_window_rect(m_window_id);
}

Gfx::IntRect Window::floating_rect() const
{
    if (!is_visible())
        return m_floating_rect;
    return ConnectionToWindowServer::the().get_window_floating_rect(m_window_id);
}

void Window::set_rect(Gfx::IntRect const& a_rect)
{
    if (a_rect.location() != m_rect_when_windowless.location()) {
        m_moved_by_client = true;
    }

    m_rect_when_windowless = a_rect;
    m_floating_rect = a_rect;

    if (!is_visible()) {
        if (m_main_widget)
            m_main_widget->resize(m_rect_when_windowless.size());
        return;
    }
    auto window_rect = ConnectionToWindowServer::the().set_window_rect(m_window_id, a_rect);
    if (m_back_store && m_back_store->size() != window_rect.size())
        m_back_store = nullptr;
    if (m_front_store && m_front_store->size() != window_rect.size())
        m_front_store = nullptr;
    if (m_main_widget)
        m_main_widget->resize(window_rect.size());
}

Gfx::IntSize Window::minimum_size() const
{
    if (!is_visible())
        return m_minimum_size_when_windowless;

    return ConnectionToWindowServer::the().get_window_minimum_size(m_window_id);
}

void Window::set_minimum_size(Gfx::IntSize size)
{
    VERIFY(size.width() >= 0 && size.height() >= 0);
    VERIFY(!is_obeying_widget_min_size());
    m_minimum_size_when_windowless = size;

    if (is_visible())
        ConnectionToWindowServer::the().async_set_window_minimum_size(m_window_id, size);
}

void Window::center_on_screen()
{
    set_rect(rect().centered_within(Desktop::the().rect()));
}

void Window::center_within(Window const& other)
{
    if (this == &other)
        return;
    set_rect(rect().centered_within(other.rect()));
}

void Window::center_within(Gfx::IntRect const& other)
{
    set_rect(rect().centered_within(other));
}

void Window::constrain_to_desktop()
{
    auto desktop_rect = Desktop::the().rect().shrunken(0, 0, Desktop::the().taskbar_height(), 0);
    auto titlebar = Application::the()->palette().window_title_height();
    auto border = Application::the()->palette().window_border_thickness();
    auto constexpr margin = 1;

    auto framed_rect = rect().inflated(border + titlebar + margin, border, border, border);
    if (desktop_rect.contains(framed_rect))
        return;

    auto constrained = framed_rect.constrained_to(desktop_rect);
    constrained.shrink(border + titlebar + margin, border, border, border);
    set_rect(constrained.x(), constrained.y(), rect().width(), rect().height());
}

void Window::set_window_type(WindowType window_type)
{
    m_window_type = window_type;
}

void Window::set_window_mode(WindowMode mode)
{
    VERIFY(!is_visible());
    m_window_mode = mode;
}

void Window::make_window_manager(unsigned event_mask)
{
    GUI::ConnectionToWindowManagerServer::the().async_set_event_mask(event_mask);
    GUI::ConnectionToWindowManagerServer::the().async_set_manager_window(m_window_id);
}

void Window::set_cursor(Gfx::StandardCursor cursor)
{
    if (m_cursor == cursor)
        return;
    m_cursor = cursor;
    update_cursor();
}

void Window::set_cursor(NonnullRefPtr<Gfx::Bitmap const> cursor)
{
    if (m_cursor == cursor)
        return;
    m_cursor = cursor;
    update_cursor();
}

void Window::handle_drop_event(DropEvent& event)
{
    if (!m_main_widget)
        return;
    auto result = m_main_widget->hit_test(event.position());
    auto local_event = make<DropEvent>(Event::Type::Drop, result.local_position, event.button(), event.buttons(), event.modifiers(), event.text(), event.mime_data());
    VERIFY(result.widget);
    result.widget->dispatch_event(*local_event, this);

    Application::the()->set_drag_hovered_widget({}, nullptr);
}

void Window::handle_mouse_event(MouseEvent& event)
{
    if (!m_main_widget)
        return;
    auto result = m_main_widget->hit_test(event.position());
    VERIFY(result.widget);

    if (m_automatic_cursor_tracking_widget) {
        auto window_relative_rect = m_automatic_cursor_tracking_widget->window_relative_rect();
        Gfx::IntPoint local_point { event.x() - window_relative_rect.x(), event.y() - window_relative_rect.y() };
        auto local_event = MouseEvent((Event::Type)event.type(), local_point, event.buttons(), event.button(), event.modifiers(), event.wheel_delta_x(), event.wheel_delta_y(), event.wheel_raw_delta_x(), event.wheel_raw_delta_y());
        m_automatic_cursor_tracking_widget->dispatch_event(local_event, this);
        if (event.buttons() == 0) {
            m_automatic_cursor_tracking_widget = nullptr;
        } else {
            auto is_hovered = m_automatic_cursor_tracking_widget.ptr() == result.widget.ptr();
            set_hovered_widget(is_hovered ? m_automatic_cursor_tracking_widget.ptr() : nullptr);
        }
        return;
    }
    set_hovered_widget(result.widget);
    if (event.buttons() != 0 && !m_automatic_cursor_tracking_widget)
        m_automatic_cursor_tracking_widget = *result.widget;
    auto local_event = MouseEvent((Event::Type)event.type(), result.local_position, event.buttons(), event.button(), event.modifiers(), event.wheel_delta_x(), event.wheel_delta_y(), event.wheel_raw_delta_x(), event.wheel_raw_delta_y());
    result.widget->dispatch_event(local_event, this);
}

Gfx::IntSize Window::backing_store_size(Gfx::IntSize window_size) const
{
    if (!m_resizing)
        return window_size;

    int const backing_margin_during_resize = 64;
    return { window_size.width() + backing_margin_during_resize, window_size.height() + backing_margin_during_resize };
}

void Window::handle_multi_paint_event(MultiPaintEvent& event)
{
    if (!is_visible())
        return;
    if (!m_main_widget)
        return;
    auto rects = event.rects();
    if (!m_pending_paint_event_rects.is_empty()) {
        // It's possible that there had been some calls to update() that
        // haven't been flushed. We can handle these right now, avoiding
        // another round trip.
        rects.extend(move(m_pending_paint_event_rects));
    }
    VERIFY(!rects.is_empty());

    // Throw away our backing store if its size is different, and we've stopped resizing or double buffering is disabled.
    // This ensures that we shrink the backing store after a resize, and that we do not get flickering artifacts when
    // directly painting into a shared active backing store.
    if (m_back_store && (!m_resizing || !m_double_buffering_enabled) && m_back_store->size() != event.window_size())
        m_back_store = nullptr;

    // Discard our backing store if it's unable to contain the new window size. Smaller is fine though, that prevents
    // lots of backing store allocations during a resize.
    if (m_back_store && !m_back_store->size().contains(event.window_size()))
        m_back_store = nullptr;

    bool created_new_backing_store = false;
    if (!m_back_store) {
        m_back_store = create_backing_store(backing_store_size(event.window_size())).release_value_but_fixme_should_propagate_errors();
        created_new_backing_store = true;
    } else if (m_double_buffering_enabled) {
        bool was_purged = false;
        bool bitmap_has_memory = m_back_store->set_nonvolatile(was_purged);
        if (!bitmap_has_memory) {
            // We didn't have enough memory to make the bitmap non-volatile!
            // Fall back to single-buffered mode for this window.
            // FIXME: Once we have a way to listen for system memory pressure notifications,
            //        it would be cool to transition back into double-buffered mode once
            //        the coast is clear.
            dbgln("Not enough memory to make backing store non-volatile. Falling back to single-buffered mode.");
            m_double_buffering_enabled = false;
            m_back_store = move(m_front_store);
            created_new_backing_store = true;
        } else if (was_purged) {
            // The backing store bitmap was cleared, but it does have memory.
            // Act as if it's a new backing store so the entire window gets repainted.
            created_new_backing_store = true;
        }
    }

    if (created_new_backing_store) {
        rects.clear();
        rects.append({ {}, event.window_size() });
    }

    for (auto& rect : rects) {
        PaintEvent paint_event(rect);
        m_main_widget->dispatch_event(paint_event, this);
    }
    m_back_store->set_visible_size(event.window_size());

    if (m_double_buffering_enabled)
        flip(rects);
    else if (created_new_backing_store)
        set_current_backing_store(*m_back_store, true);

    if (is_visible())
        ConnectionToWindowServer::the().async_did_finish_painting(m_window_id, rects);
}

void Window::propagate_shortcuts(KeyEvent& event, Widget* widget, ShortcutPropagationBoundary boundary)
{
    VERIFY(event.type() == Event::KeyDown);
    auto shortcut = Shortcut(event.modifiers(), event.key());
    Action* action = nullptr;

    if (widget) {
        VERIFY(widget->window() == this);

        do {
            action = widget->action_for_shortcut(shortcut);
            if (action)
                break;

            widget = widget->parent_widget();
        } while (widget);
    }

    if (!action && boundary >= ShortcutPropagationBoundary::Window)
        action = action_for_shortcut(shortcut);
    if (!action && boundary >= ShortcutPropagationBoundary::Application)
        action = Application::the()->action_for_shortcut(shortcut);

    if (action) {
        action->process_event(*this, event);
        return;
    }

    event.ignore();
}

void Window::restore_size_and_position(StringView domain, StringView group, Optional<Gfx::IntSize> fallback_size, Optional<Gfx::IntPoint> fallback_position)
{
    int x = Config::read_i32(domain, group, "X"sv, INT_MIN);
    int y = Config::read_i32(domain, group, "Y"sv, INT_MIN);
    if (x != INT_MIN && y != INT_MIN) {
        move_to(x, y);
    } else if (fallback_position.has_value()) {
        move_to(fallback_position.release_value());
    }

    int width = Config::read_i32(domain, group, "Width"sv, INT_MIN);
    int height = Config::read_i32(domain, group, "Height"sv, INT_MIN);
    if (width != INT_MIN && height != INT_MIN) {
        resize(width, height);
    } else if (fallback_size.has_value()) {
        resize(fallback_size.release_value());
    }

    set_maximized(Config::read_bool(domain, group, "Maximized"sv, false));
}

void Window::save_size_and_position(StringView domain, StringView group) const
{
    auto rect_to_save = floating_rect();
    Config::write_i32(domain, group, "X"sv, rect_to_save.x());
    Config::write_i32(domain, group, "Y"sv, rect_to_save.y());
    Config::write_i32(domain, group, "Width"sv, rect_to_save.width());
    Config::write_i32(domain, group, "Height"sv, rect_to_save.height());
    Config::write_bool(domain, group, "Maximized"sv, is_maximized());
}

void Window::save_size_and_position_on_close(StringView domain, StringView group)
{
    m_save_size_and_position_on_close = true;
    m_save_domain = domain;
    m_save_group = group;
}

void Window::handle_key_event(KeyEvent& event)
{
    if (!m_focused_widget && event.type() == Event::KeyDown && event.key() == Key_Tab && !event.ctrl() && !event.alt() && !event.super()) {
        focus_a_widget_if_possible(FocusSource::Keyboard);
    }

    if (m_default_return_key_widget && event.key() == Key_Return)
        if (!m_focused_widget || !is<Button>(m_focused_widget.ptr()))
            return default_return_key_widget()->dispatch_event(event, this);

    if (m_focused_widget)
        m_focused_widget->dispatch_event(event, this);
    else if (m_main_widget)
        m_main_widget->dispatch_event(event, this);

    if (event.is_accepted())
        return;

    // Only process shortcuts if this is a keydown event.
    if (event.type() == Event::KeyDown) {
        auto const boundary = (is_blocking() || is_popup()) ? ShortcutPropagationBoundary::Window : ShortcutPropagationBoundary::Application;
        propagate_shortcuts(event, nullptr, boundary);
    }
}

void Window::handle_resize_event(ResizeEvent& event)
{
    auto new_size = event.size();

    // When the user is done resizing, we receive a last resize event with our actual size.
    m_resizing = new_size != m_rect_when_windowless.size();

    if (!m_pending_paint_event_rects.is_empty()) {
        m_pending_paint_event_rects.clear_with_capacity();
        m_pending_paint_event_rects.append({ {}, new_size });
    }
    m_rect_when_windowless.set_size(new_size);
    if (m_main_widget)
        m_main_widget->set_relative_rect({ {}, new_size });
}

void Window::handle_input_preemption_event(Core::Event& event)
{
    if (on_input_preemption_change)
        on_input_preemption_change(event.type() == Event::WindowInputPreempted);
    if (!m_focused_widget)
        return;
    m_focused_widget->set_focus_preempted(event.type() == Event::WindowInputPreempted);
    m_focused_widget->update();
}

void Window::handle_became_active_or_inactive_event(Core::Event& event)
{
    if (event.type() == Event::WindowBecameActive)
        Application::the()->window_did_become_active({}, *this);
    else
        Application::the()->window_did_become_inactive({}, *this);
    if (on_active_window_change)
        on_active_window_change(event.type() == Event::WindowBecameActive);
    if (m_main_widget)
        m_main_widget->dispatch_event(event, this);
    if (m_focused_widget) {
        if (event.type() == Event::WindowBecameActive)
            m_focused_widget->set_focus_preempted(false);
        m_focused_widget->update();
    }
}

void Window::handle_close_request()
{
    if (on_close_request) {
        if (on_close_request() == Window::CloseRequestDecision::StayOpen)
            return;
    }
    close();
}

void Window::handle_theme_change_event(ThemeChangeEvent& event)
{
    if (!m_main_widget)
        return;
    auto dispatch_theme_change = [&](auto& widget, auto recursive) {
        widget.dispatch_event(event, this);
        widget.for_each_child_widget([&](auto& widget) -> IterationDecision {
            widget.dispatch_event(event, this);
            recursive(widget, recursive);
            return IterationDecision::Continue;
        });
    };
    dispatch_theme_change(*m_main_widget.ptr(), dispatch_theme_change);
}

void Window::handle_fonts_change_event(FontsChangeEvent& event)
{
    if (!m_main_widget)
        return;
    auto dispatch_fonts_change = [&](auto& widget, auto recursive) {
        widget.dispatch_event(event, this);
        widget.for_each_child_widget([&](auto& widget) -> IterationDecision {
            widget.dispatch_event(event, this);
            recursive(widget, recursive);
            return IterationDecision::Continue;
        });
    };
    dispatch_fonts_change(*m_main_widget.ptr(), dispatch_fonts_change);

    if (is_auto_shrinking())
        schedule_relayout();

    if (on_font_change)
        on_font_change();
}

void Window::handle_screen_rects_change_event(ScreenRectsChangeEvent& event)
{
    if (!m_main_widget)
        return;
    auto dispatch_screen_rects_change = [&](auto& widget, auto recursive) {
        widget.dispatch_event(event, this);
        widget.for_each_child_widget([&](auto& widget) -> IterationDecision {
            widget.dispatch_event(event, this);
            recursive(widget, recursive);
            return IterationDecision::Continue;
        });
    };
    dispatch_screen_rects_change(*m_main_widget.ptr(), dispatch_screen_rects_change);
    screen_rects_change_event(event);
}

void Window::handle_applet_area_rect_change_event(AppletAreaRectChangeEvent& event)
{
    if (!m_main_widget)
        return;
    auto dispatch_applet_area_rect_change = [&](auto& widget, auto recursive) {
        widget.dispatch_event(event, this);
        widget.for_each_child_widget([&](auto& widget) -> IterationDecision {
            widget.dispatch_event(event, this);
            recursive(widget, recursive);
            return IterationDecision::Continue;
        });
    };
    dispatch_applet_area_rect_change(*m_main_widget.ptr(), dispatch_applet_area_rect_change);
    applet_area_rect_change_event(event);
}

void Window::handle_drag_move_event(DragEvent& event)
{
    if (!m_main_widget)
        return;
    auto result = m_main_widget->hit_test(event.position());
    VERIFY(result.widget);

    Application::the()->set_drag_hovered_widget({}, result.widget, result.local_position, event);

    // NOTE: Setting the drag hovered widget may have executed arbitrary code, so re-check that the widget is still there.
    if (!result.widget)
        return;

    if (result.widget->has_pending_drop()) {
        DragEvent drag_move_event(static_cast<Event::Type>(event.type()), result.local_position, event.button(), event.buttons(), event.modifiers(), event.text(), event.mime_data());
        result.widget->dispatch_event(drag_move_event, this);
    }
}

void Window::enter_event(Core::Event&)
{
}

void Window::leave_event(Core::Event&)
{
}

void Window::handle_entered_event(Core::Event& event)
{
    enter_event(event);
}

void Window::handle_left_event(Core::Event& event)
{
    set_hovered_widget(nullptr);
    Application::the()->set_drag_hovered_widget({}, nullptr);
    leave_event(event);
}

void Window::event(Core::Event& event)
{
    ScopeGuard guard([&] {
        // Accept the event so it doesn't bubble up to parent windows!
        event.accept();
    });
    if (event.type() == Event::Drop)
        return handle_drop_event(static_cast<DropEvent&>(event));

    if (event.type() == Event::MouseUp || event.type() == Event::MouseDown || event.type() == Event::MouseDoubleClick || event.type() == Event::MouseMove || event.type() == Event::MouseWheel)
        return handle_mouse_event(static_cast<MouseEvent&>(event));

    if (event.type() == Event::MultiPaint)
        return handle_multi_paint_event(static_cast<MultiPaintEvent&>(event));

    if (event.type() == Event::KeyUp || event.type() == Event::KeyDown)
        return handle_key_event(static_cast<KeyEvent&>(event));

    if (event.type() == Event::WindowBecameActive || event.type() == Event::WindowBecameInactive)
        return handle_became_active_or_inactive_event(event);

    if (event.type() == Event::WindowInputPreempted || event.type() == Event::WindowInputRestored)
        return handle_input_preemption_event(event);

    if (event.type() == Event::WindowCloseRequest)
        return handle_close_request();

    if (event.type() == Event::WindowEntered)
        return handle_entered_event(event);

    if (event.type() == Event::WindowLeft)
        return handle_left_event(event);

    if (event.type() == Event::Resize)
        return handle_resize_event(static_cast<ResizeEvent&>(event));

    if (event.type() > Event::__Begin_WM_Events && event.type() < Event::__End_WM_Events)
        return wm_event(static_cast<WMEvent&>(event));

    if (event.type() == Event::DragMove)
        return handle_drag_move_event(static_cast<DragEvent&>(event));

    if (event.type() == Event::ThemeChange)
        return handle_theme_change_event(static_cast<ThemeChangeEvent&>(event));

    if (event.type() == Event::FontsChange)
        return handle_fonts_change_event(static_cast<FontsChangeEvent&>(event));

    if (event.type() == Event::ScreenRectsChange)
        return handle_screen_rects_change_event(static_cast<ScreenRectsChangeEvent&>(event));

    if (event.type() == Event::AppletAreaRectChange)
        return handle_applet_area_rect_change_event(static_cast<AppletAreaRectChangeEvent&>(event));

    Core::EventReceiver::event(event);
}

bool Window::is_visible() const
{
    return m_visible;
}

void Window::update()
{
    auto rect = this->rect();
    update({ 0, 0, rect.width(), rect.height() });
}

void Window::force_update()
{
    if (!is_visible())
        return;
    auto rect = this->rect();
    ConnectionToWindowServer::the().async_invalidate_rect(m_window_id, { { 0, 0, rect.width(), rect.height() } }, true);
}

void Window::update(Gfx::IntRect const& a_rect)
{
    if (!is_visible())
        return;

    for (auto& pending_rect : m_pending_paint_event_rects) {
        if (pending_rect.contains(a_rect)) {
            dbgln_if(UPDATE_COALESCING_DEBUG, "Ignoring {} since it's contained by pending rect {}", a_rect, pending_rect);
            return;
        }
    }

    if (m_pending_paint_event_rects.is_empty()) {
        deferred_invoke([this] {
            auto rects = move(m_pending_paint_event_rects);
            if (rects.is_empty())
                return;
            ConnectionToWindowServer::the().async_invalidate_rect(m_window_id, rects, false);
        });
    }
    m_pending_paint_event_rects.append(a_rect);
}

void Window::set_main_widget(Widget* widget)
{
    if (m_main_widget == widget)
        return;
    if (m_main_widget) {
        m_main_widget->set_window(nullptr);
        remove_child(*m_main_widget);
    }
    m_main_widget = widget;
    if (m_main_widget) {
        add_child(*widget);
        auto new_window_rect = rect();
        auto new_widget_min_size = m_main_widget->effective_min_size();
        new_window_rect.set_width(max(new_window_rect.width(), MUST(new_widget_min_size.width().shrink_value())));
        new_window_rect.set_height(max(new_window_rect.height(), MUST(new_widget_min_size.height().shrink_value())));
        set_rect(new_window_rect);
        m_main_widget->set_relative_rect({ {}, new_window_rect.size() });
        m_main_widget->set_window(this);
        if (m_main_widget->focus_policy() != FocusPolicy::NoFocus)
            m_main_widget->set_focus(true);
    }
    update();
}

void Window::set_default_return_key_widget(Widget* widget)
{
    if (m_default_return_key_widget == widget)
        return;
    m_default_return_key_widget = widget;
}

void Window::set_focused_widget(Widget* widget, FocusSource source)
{
    if (m_focused_widget == widget)
        return;

    WeakPtr<Widget> previously_focused_widget = m_focused_widget;
    m_focused_widget = widget;

    if (!m_focused_widget && m_previously_focused_widget)
        m_focused_widget = m_previously_focused_widget;

    if (m_default_return_key_widget && m_default_return_key_widget->on_focus_change)
        m_default_return_key_widget->on_focus_change(m_default_return_key_widget->is_focused(), source);

    if (previously_focused_widget) {
        Core::EventLoop::current().post_event(*previously_focused_widget, make<FocusEvent>(Event::FocusOut, source));
        previously_focused_widget->update();
        if (previously_focused_widget && previously_focused_widget->on_focus_change)
            previously_focused_widget->on_focus_change(previously_focused_widget->is_focused(), source);
        m_previously_focused_widget = previously_focused_widget;
    }
    if (m_focused_widget) {
        Core::EventLoop::current().post_event(*m_focused_widget, make<FocusEvent>(Event::FocusIn, source));
        m_focused_widget->update();
        if (m_focused_widget && m_focused_widget->on_focus_change)
            m_focused_widget->on_focus_change(m_focused_widget->is_focused(), source);
    }
}

void Window::set_automatic_cursor_tracking_widget(Widget* widget)
{
    if (widget == m_automatic_cursor_tracking_widget)
        return;
    m_automatic_cursor_tracking_widget = widget;
}

void Window::set_has_alpha_channel(bool value)
{
    if (m_has_alpha_channel == value)
        return;
    m_has_alpha_channel = value;
    if (!is_visible())
        return;

    m_pending_paint_event_rects.clear();
    m_back_store = nullptr;
    m_front_store = nullptr;

    ConnectionToWindowServer::the().async_set_window_has_alpha_channel(m_window_id, value);
    update();
}

void Window::set_double_buffering_enabled(bool value)
{
    VERIFY(!is_visible());
    m_double_buffering_enabled = value;
}

void Window::set_alpha_hit_threshold(float threshold)
{
    if (threshold < 0.0f)
        threshold = 0.0f;
    else if (threshold > 1.0f)
        threshold = 1.0f;
    if (m_alpha_hit_threshold == threshold)
        return;
    m_alpha_hit_threshold = threshold;
    if (!is_visible())
        return;
    ConnectionToWindowServer::the().async_set_window_alpha_hit_threshold(m_window_id, threshold);
}

void Window::set_hovered_widget(Widget* widget)
{
    if (widget == m_hovered_widget)
        return;

    if (m_hovered_widget)
        Core::EventLoop::current().post_event(*m_hovered_widget, make<Event>(Event::Leave));

    m_hovered_widget = widget;

    if (m_hovered_widget)
        Core::EventLoop::current().post_event(*m_hovered_widget, make<Event>(Event::Enter));

    auto* app = Application::the();
    if (app && app->hover_debugging_enabled())
        update();
}

void Window::set_current_backing_store(WindowBackingStore& backing_store, bool flush_immediately) const
{
    auto& bitmap = backing_store.bitmap();
    ConnectionToWindowServer::the().set_window_backing_store(
        m_window_id,
        32,
        bitmap.pitch(),
        MUST(IPC::File::clone_fd(bitmap.anonymous_buffer().fd())),
        backing_store.serial(),
        bitmap.has_alpha_channel(),
        bitmap.size(),
        backing_store.visible_size(),
        flush_immediately);
}

void Window::flip(Vector<Gfx::IntRect, 32> const& dirty_rects)
{
    swap(m_front_store, m_back_store);

    set_current_backing_store(*m_front_store);

    if (!m_back_store || m_back_store->size() != m_front_store->size()) {
        m_back_store = create_backing_store(m_front_store->size()).release_value_but_fixme_should_propagate_errors();
        memcpy(m_back_store->bitmap().scanline(0), m_front_store->bitmap().scanline(0), m_front_store->bitmap().size_in_bytes());
        m_back_store->set_volatile();
        return;
    }

    // Copy whatever was painted from the front to the back.
    Painter painter(m_back_store->bitmap());
    for (auto& dirty_rect : dirty_rects)
        painter.blit(dirty_rect.location(), m_front_store->bitmap(), dirty_rect, 1.0f, false);

    m_back_store->set_volatile();
}

ErrorOr<NonnullOwnPtr<WindowBackingStore>> Window::create_backing_store(Gfx::IntSize size)
{
    auto format = m_has_alpha_channel ? Gfx::BitmapFormat::BGRA8888 : Gfx::BitmapFormat::BGRx8888;

    VERIFY(!size.is_empty());
    size_t pitch = Gfx::Bitmap::minimum_pitch(size.width(), format);
    size_t size_in_bytes = size.height() * pitch;

    auto buffer = TRY(Core::AnonymousBuffer::create_with_size(round_up_to_power_of_two(size_in_bytes, PAGE_SIZE)));

    // FIXME: Plumb scale factor here eventually.
    auto bitmap = TRY(Gfx::Bitmap::create_with_anonymous_buffer(format, buffer, size, 1));
    return make<WindowBackingStore>(bitmap);
}

void Window::wm_event(WMEvent&)
{
}

void Window::screen_rects_change_event(ScreenRectsChangeEvent&)
{
}

void Window::applet_area_rect_change_event(AppletAreaRectChangeEvent&)
{
}

void Window::set_icon(Gfx::Bitmap const* icon)
{
    if (m_icon == icon)
        return;

    Gfx::IntSize icon_size = icon ? icon->size() : Gfx::IntSize(16, 16);

    auto new_icon = Gfx::Bitmap::create(Gfx::BitmapFormat::BGRA8888, icon_size).release_value_but_fixme_should_propagate_errors();
    if (icon) {
        Painter painter(*new_icon);
        painter.blit({ 0, 0 }, *icon, icon->rect());
    }
    m_icon = move(new_icon);

    apply_icon();
}

void Window::apply_icon()
{
    if (!m_icon)
        return;

    if (!is_visible())
        return;

    ConnectionToWindowServer::the().async_set_window_icon_bitmap(m_window_id, m_icon->to_shareable_bitmap());
}

void Window::start_interactive_resize(ResizeDirection resize_direction)
{
    ConnectionToWindowServer::the().async_start_window_resize(m_window_id, (i32)resize_direction);
}

Vector<Widget&> Window::focusable_widgets(FocusSource source) const
{
    if (!m_main_widget)
        return {};

    HashTable<Widget*> seen_widgets;
    Vector<Widget&> collected_widgets;

    Function<void(Widget&)> collect_focusable_widgets = [&](auto& widget) {
        bool widget_accepts_focus = false;
        switch (source) {
        case FocusSource::Keyboard:
            widget_accepts_focus = has_flag(widget.focus_policy(), FocusPolicy::TabFocus);
            break;
        case FocusSource::Mouse:
            widget_accepts_focus = has_flag(widget.focus_policy(), FocusPolicy::ClickFocus);
            break;
        case FocusSource::Programmatic:
            widget_accepts_focus = widget.focus_policy() != FocusPolicy::NoFocus;
            break;
        }

        if (widget_accepts_focus) {
            auto& effective_focus_widget = widget.focus_proxy() ? *widget.focus_proxy() : widget;
            if (seen_widgets.set(&effective_focus_widget) == AK::HashSetResult::InsertedNewEntry)
                collected_widgets.append(effective_focus_widget);
        }
        widget.for_each_child_widget([&](auto& child) {
            if (!child.is_visible())
                return IterationDecision::Continue;
            if (!child.is_enabled())
                return IterationDecision::Continue;
            if (!child.is_auto_focusable())
                return IterationDecision::Continue;
            collect_focusable_widgets(child);
            return IterationDecision::Continue;
        });
    };

    collect_focusable_widgets(const_cast<Widget&>(*m_main_widget));
    return collected_widgets;
}

void Window::set_fullscreen(bool fullscreen)
{
    if (m_fullscreen == fullscreen)
        return;
    m_fullscreen = fullscreen;
    if (!is_visible())
        return;
    ConnectionToWindowServer::the().async_set_fullscreen(m_window_id, fullscreen);
}

void Window::set_frameless(bool frameless)
{
    if (m_frameless == frameless)
        return;
    m_frameless = frameless;
    if (!is_visible())
        return;
    ConnectionToWindowServer::the().async_set_frameless(m_window_id, frameless);

    if (!frameless)
        apply_icon();
}

void Window::set_forced_shadow(bool shadow)
{
    if (m_forced_shadow == shadow)
        return;
    m_forced_shadow = shadow;
    if (!is_visible())
        return;
    ConnectionToWindowServer::the().async_set_forced_shadow(m_window_id, shadow);
}

void Window::set_obey_widget_min_size(bool obey_widget_min_size)
{
    if (m_obey_widget_min_size != obey_widget_min_size) {
        m_obey_widget_min_size = obey_widget_min_size;
        schedule_relayout();
    }
}

void Window::set_auto_shrink(bool shrink)
{
    if (m_auto_shrink == shrink)
        return;
    m_auto_shrink = shrink;
    schedule_relayout();
}

void Window::set_maximized(bool maximized)
{
    m_maximized = maximized;
    if (!is_visible())
        return;

    ConnectionToWindowServer::the().async_set_maximized(m_window_id, maximized);
}

void Window::set_minimized(bool minimized)
{
    if (!is_minimizable())
        return;

    m_minimized = minimized;
    if (!is_visible())
        return;

    ConnectionToWindowServer::the().async_set_minimized(m_window_id, minimized);
}

void Window::update_min_size()
{
    if (!main_widget())
        return;
    main_widget()->do_layout();

    auto min_size = main_widget()->effective_min_size();
    Gfx::IntSize size = { MUST(min_size.width().shrink_value()), MUST(min_size.height().shrink_value()) };
    if (is_obeying_widget_min_size()) {
        m_minimum_size_when_windowless = size;
        if (is_visible())
            ConnectionToWindowServer::the().async_set_window_minimum_size(m_window_id, size);
    }
    if (is_auto_shrinking())
        resize(size);
}

void Window::schedule_relayout()
{
    if (m_layout_pending || !is_visible())
        return;
    m_layout_pending = true;
    deferred_invoke([this] {
        update_min_size();
        update();
        m_layout_pending = false;
    });
}

void Window::refresh_system_theme()
{
    ConnectionToWindowServer::the().async_refresh_system_theme();
}

void Window::for_each_window(Badge<ConnectionToWindowServer>, Function<void(Window&)> callback)
{
    for (auto& e : *reified_windows) {
        VERIFY(e.value);
        callback(*e.value);
    }
}

void Window::update_all_windows(Badge<ConnectionToWindowServer>)
{
    for (auto& e : *reified_windows) {
        e.value->force_update();
    }
}

void Window::notify_state_changed(Badge<ConnectionToWindowServer>, bool minimized, bool maximized, bool occluded)
{
    m_visible_for_timer_purposes = !minimized && !occluded;

    m_maximized = maximized;

    // When double buffering is enabled, minimization/occlusion means we can mark the front bitmap volatile (in addition to the back bitmap.)
    // When double buffering is disabled, there is only the back bitmap (which we can now mark volatile!)
    auto& store = m_double_buffering_enabled ? m_front_store : m_back_store;
    if (!store)
        return;
    if (minimized || occluded) {
        store->set_volatile();
    } else {
        bool was_purged = false;
        bool bitmap_has_memory = store->set_nonvolatile(was_purged);
        if (!bitmap_has_memory) {
            // Not enough memory to make the bitmap non-volatile. Lose the bitmap and schedule an update.
            // Let the paint system figure out what to do.
            store = nullptr;
            update();
        } else if (was_purged) {
            // The bitmap memory was purged by the kernel, but we have all-new zero-filled pages.
            // Schedule an update to regenerate the bitmap.
            update();
        }
    }
}

Action* Window::action_for_shortcut(Shortcut const& shortcut)
{
    return Action::find_action_for_shortcut(*this, shortcut);
}

void Window::set_base_size(Gfx::IntSize base_size)
{
    if (m_base_size == base_size)
        return;
    m_base_size = base_size;
    if (is_visible())
        ConnectionToWindowServer::the().async_set_window_base_size_and_size_increment(m_window_id, m_base_size, m_size_increment);
}

void Window::set_size_increment(Gfx::IntSize size_increment)
{
    if (m_size_increment == size_increment)
        return;
    m_size_increment = size_increment;
    if (is_visible())
        ConnectionToWindowServer::the().async_set_window_base_size_and_size_increment(m_window_id, m_base_size, m_size_increment);
}

void Window::set_resize_aspect_ratio(Optional<Gfx::IntSize> const& ratio)
{
    if (m_resize_aspect_ratio == ratio)
        return;

    m_resize_aspect_ratio = ratio;
    if (is_visible())
        ConnectionToWindowServer::the().async_set_window_resize_aspect_ratio(m_window_id, m_resize_aspect_ratio);
}

void Window::did_add_widget(Badge<Widget>, Widget&)
{
    if (!m_focused_widget)
        focus_a_widget_if_possible(FocusSource::Mouse);
}

void Window::did_remove_widget(Badge<Widget>, Widget& widget)
{
    if (m_focused_widget == &widget)
        m_focused_widget = nullptr;
    if (m_hovered_widget == &widget)
        m_hovered_widget = nullptr;
    if (m_automatic_cursor_tracking_widget == &widget)
        m_automatic_cursor_tracking_widget = nullptr;
}

void Window::set_progress(Optional<int> progress)
{
    VERIFY(m_window_id);
    ConnectionToWindowServer::the().async_set_window_progress(m_window_id, progress);
}

void Window::update_cursor()
{
    auto new_cursor = m_cursor;

    auto is_usable_cursor = [](auto& cursor) {
        return cursor.template has<NonnullRefPtr<Gfx::Bitmap const>>() || cursor.template get<Gfx::StandardCursor>() != Gfx::StandardCursor::None;
    };

    // NOTE: If there's an automatic cursor tracking widget, we retain its cursor until tracking stops.
    if (auto widget = m_automatic_cursor_tracking_widget) {
        if (is_usable_cursor(widget->override_cursor()))
            new_cursor = widget->override_cursor();
    } else if (auto widget = m_hovered_widget) {
        if (is_usable_cursor(widget->override_cursor()))
            new_cursor = widget->override_cursor();
    }

    if (m_effective_cursor == new_cursor)
        return;
    m_effective_cursor = new_cursor;

    if (new_cursor.has<NonnullRefPtr<Gfx::Bitmap const>>())
        ConnectionToWindowServer::the().async_set_window_custom_cursor(m_window_id, new_cursor.get<NonnullRefPtr<Gfx::Bitmap const>>()->to_shareable_bitmap());
    else
        ConnectionToWindowServer::the().async_set_window_cursor(m_window_id, (u32)new_cursor.get<Gfx::StandardCursor>());
}

void Window::focus_a_widget_if_possible(FocusSource source)
{
    auto focusable_widgets = this->focusable_widgets(source);
    if (!focusable_widgets.is_empty())
        set_focused_widget(&focusable_widgets[0], source);
}

void Window::did_disable_focused_widget(Badge<Widget>)
{
    focus_a_widget_if_possible(FocusSource::Mouse);
}

bool Window::is_active() const
{
    VERIFY(Application::the());
    return this == Application::the()->active_window();
}

Gfx::Bitmap* Window::back_bitmap()
{
    return m_back_store ? &m_back_store->bitmap() : nullptr;
}

void Window::add_menu(NonnullRefPtr<Menu> menu)
{
    m_menubar->add_menu({}, move(menu));
    if (m_window_id) {
        menu->realize_menu_if_needed();
        ConnectionToWindowServer::the().async_add_menu(m_window_id, menu->menu_id());
    }
}

NonnullRefPtr<Menu> Window::add_menu(String name)
{
    auto menu = m_menubar->add_menu({}, move(name));
    if (m_window_id) {
        menu->realize_menu_if_needed();
        ConnectionToWindowServer::the().async_add_menu(m_window_id, menu->menu_id());
    }
    return menu;
}

void Window::flash_menubar_menu_for(MenuItem const& menu_item)
{
    if (!Desktop::the().system_effects().flash_menus())
        return;
    auto menu_id = menu_item.menu_id();
    if (menu_id < 0)
        return;

    ConnectionToWindowServer::the().async_flash_menubar_menu(m_window_id, menu_id);
}

bool Window::is_modified() const
{
    if (!m_window_id)
        return false;
    return ConnectionToWindowServer::the().is_window_modified(m_window_id);
}

void Window::set_modified(bool modified)
{
    if (!m_window_id)
        return;
    ConnectionToWindowServer::the().async_set_window_modified(m_window_id, modified);
}

void Window::flush_pending_paints_immediately()
{
    if (!m_window_id)
        return;
    if (m_pending_paint_event_rects.is_empty())
        return;
    MultiPaintEvent paint_event(move(m_pending_paint_event_rects), size());
    handle_multi_paint_event(paint_event);
}

void Window::set_always_on_top(bool always_on_top)
{
    if (!m_window_id)
        return;
    ConnectionToWindowServer::the().set_always_on_top(m_window_id, always_on_top);
}

}
