/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Debug.h>
#include <AK/HashMap.h>
#include <AK/IDAllocator.h>
#include <AK/JsonObject.h>
#include <AK/NeverDestroyed.h>
#include <AK/ScopeGuard.h>
#include <LibCore/EventLoop.h>
#include <LibCore/MimeData.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/Event.h>
#include <LibGUI/MenuItem.h>
#include <LibGUI/Menubar.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGUI/WindowManagerServerConnection.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibGfx/Bitmap.h>
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
    {
    }

    Gfx::Bitmap& bitmap() { return *m_bitmap; }
    const Gfx::Bitmap& bitmap() const { return *m_bitmap; }

    Gfx::IntSize size() const { return m_bitmap->size(); }

    i32 serial() const { return m_serial; }

private:
    NonnullRefPtr<Gfx::Bitmap> m_bitmap;
    const i32 m_serial;
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

Window::Window(Core::Object* parent)
    : Core::Object(parent)
    , m_menubar(Menubar::construct())
{
    all_windows->set(this);
    m_rect_when_windowless = { -5000, -5000, 140, 140 };
    m_title_when_windowless = "GUI::Window";

    register_property(
        "title",
        [this] { return title(); },
        [this](auto& value) {
            set_title(value.to_string());
            return true;
        });

    register_property("visible", [this] { return is_visible(); });
    register_property("active", [this] { return is_active(); });

    REGISTER_BOOL_PROPERTY("minimizable", is_minimizable, set_minimizable);
    REGISTER_BOOL_PROPERTY("resizable", is_resizable, set_resizable);
    REGISTER_BOOL_PROPERTY("fullscreen", is_fullscreen, set_fullscreen);
    REGISTER_RECT_PROPERTY("rect", rect, set_rect);
    REGISTER_SIZE_PROPERTY("base_size", base_size, set_base_size);
    REGISTER_SIZE_PROPERTY("size_increment", size_increment, set_size_increment);
}

Window::~Window()
{
    all_windows->remove(this);
    hide();
}

void Window::close()
{
    hide();
    if (on_close)
        on_close();
}

void Window::move_to_front()
{
    if (!is_visible())
        return;

    WindowServerConnection::the().async_move_window_to_front(m_window_id);
}

void Window::show()
{
    if (is_visible())
        return;

    auto* parent_window = find_parent_window();

    m_window_id = s_window_id_allocator.allocate();

    Gfx::IntRect launch_origin_rect;
    if (auto* launch_origin_rect_string = getenv("__libgui_launch_origin_rect")) {
        auto parts = StringView(launch_origin_rect_string).split_view(',');
        if (parts.size() == 4) {
            launch_origin_rect = Gfx::IntRect {
                parts[0].to_int().value_or(0),
                parts[1].to_int().value_or(0),
                parts[2].to_int().value_or(0),
                parts[3].to_int().value_or(0),
            };
        }
        unsetenv("__libgui_launch_origin_rect");
    }

    WindowServerConnection::the().async_create_window(
        m_window_id,
        m_rect_when_windowless,
        !m_moved_by_client,
        m_has_alpha_channel,
        m_modal,
        m_minimizable,
        m_closeable,
        m_resizable,
        m_fullscreen,
        m_frameless,
        m_forced_shadow,
        m_accessory,
        m_opacity_when_windowless,
        m_alpha_hit_threshold,
        m_base_size,
        m_size_increment,
        m_minimum_size_when_windowless,
        m_resize_aspect_ratio,
        (i32)m_window_type,
        m_title_when_windowless,
        parent_window ? parent_window->window_id() : 0,
        launch_origin_rect);
    m_visible = true;

    apply_icon();

    m_menubar->for_each_menu([&](Menu& menu) {
        menu.realize_menu_if_needed();
        WindowServerConnection::the().async_add_menu(m_window_id, menu.menu_id());
        return IterationDecision::Continue;
    });

    set_maximized(m_maximized_when_windowless);
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

    auto destroyed_window_ids = WindowServerConnection::the().destroy_window(m_window_id);
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

void Window::set_title(String title)
{
    m_title_when_windowless = move(title);
    if (!is_visible())
        return;
    WindowServerConnection::the().async_set_window_title(m_window_id, m_title_when_windowless);
}

String Window::title() const
{
    if (!is_visible())
        return m_title_when_windowless;
    return WindowServerConnection::the().get_window_title(m_window_id);
}

Gfx::IntRect Window::applet_rect_on_screen() const
{
    VERIFY(m_window_type == WindowType::Applet);
    return WindowServerConnection::the().get_applet_rect_on_screen(m_window_id);
}

Gfx::IntRect Window::rect() const
{
    if (!is_visible())
        return m_rect_when_windowless;
    return WindowServerConnection::the().get_window_rect(m_window_id);
}

void Window::set_rect(const Gfx::IntRect& a_rect)
{
    if (a_rect.location() != m_rect_when_windowless.location()) {
        m_moved_by_client = true;
    }

    m_rect_when_windowless = a_rect;
    if (!is_visible()) {
        if (m_main_widget)
            m_main_widget->resize(m_rect_when_windowless.size());
        return;
    }
    auto window_rect = WindowServerConnection::the().set_window_rect(m_window_id, a_rect);
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

    return WindowServerConnection::the().get_window_minimum_size(m_window_id);
}

void Window::set_minimum_size(const Gfx::IntSize& size)
{
    m_minimum_size_modified = true;
    m_minimum_size_when_windowless = size;

    if (is_visible())
        WindowServerConnection::the().async_set_window_minimum_size(m_window_id, size);
}

void Window::center_on_screen()
{
    set_rect(rect().centered_within(Desktop::the().rect()));
}

void Window::center_within(const Window& other)
{
    if (this == &other)
        return;
    set_rect(rect().centered_within(other.rect()));
}

void Window::set_window_type(WindowType window_type)
{
    m_window_type = window_type;

    if (!m_minimum_size_modified) {
        // Apply minimum size defaults.
        if (m_window_type == WindowType::Normal || m_window_type == WindowType::ToolWindow)
            m_minimum_size_when_windowless = { 50, 50 };
        else
            m_minimum_size_when_windowless = { 1, 1 };
    }
}

void Window::make_window_manager(unsigned event_mask)
{
    GUI::WindowManagerServerConnection::the().async_set_event_mask(event_mask);
    GUI::WindowManagerServerConnection::the().async_set_manager_window(m_window_id);
}

bool Window::are_cursors_the_same(AK::Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap>> const& left, AK::Variant<Gfx::StandardCursor, NonnullRefPtr<Gfx::Bitmap>> const& right) const
{
    if (left.has<Gfx::StandardCursor>() != right.has<Gfx::StandardCursor>())
        return false;
    if (left.has<Gfx::StandardCursor>())
        return left.get<Gfx::StandardCursor>() == right.get<Gfx::StandardCursor>();
    return left.get<NonnullRefPtr<Gfx::Bitmap>>().ptr() == right.get<NonnullRefPtr<Gfx::Bitmap>>().ptr();
}

void Window::set_cursor(Gfx::StandardCursor cursor)
{
    if (are_cursors_the_same(m_cursor, cursor))
        return;
    m_cursor = cursor;
    update_cursor();
}

void Window::set_cursor(NonnullRefPtr<Gfx::Bitmap> cursor)
{
    if (are_cursors_the_same(m_cursor, cursor))
        return;
    m_cursor = cursor;
    update_cursor();
}

void Window::handle_drop_event(DropEvent& event)
{
    if (!m_main_widget)
        return;
    auto result = m_main_widget->hit_test(event.position());
    auto local_event = make<DropEvent>(result.local_position, event.text(), event.mime_data());
    VERIFY(result.widget);
    result.widget->dispatch_event(*local_event, this);

    Application::the()->set_drag_hovered_widget({}, nullptr);
}

void Window::handle_mouse_event(MouseEvent& event)
{
    if (m_automatic_cursor_tracking_widget) {
        auto window_relative_rect = m_automatic_cursor_tracking_widget->window_relative_rect();
        Gfx::IntPoint local_point { event.x() - window_relative_rect.x(), event.y() - window_relative_rect.y() };
        auto local_event = MouseEvent((Event::Type)event.type(), local_point, event.buttons(), event.button(), event.modifiers(), event.wheel_delta_x(), event.wheel_delta_y());
        m_automatic_cursor_tracking_widget->dispatch_event(local_event, this);
        if (event.buttons() == 0)
            m_automatic_cursor_tracking_widget = nullptr;
        return;
    }
    if (!m_main_widget)
        return;
    auto result = m_main_widget->hit_test(event.position());
    auto local_event = MouseEvent((Event::Type)event.type(), result.local_position, event.buttons(), event.button(), event.modifiers(), event.wheel_delta_x(), event.wheel_delta_y());
    VERIFY(result.widget);
    set_hovered_widget(result.widget);
    if (event.buttons() != 0 && !m_automatic_cursor_tracking_widget)
        m_automatic_cursor_tracking_widget = *result.widget;
    result.widget->dispatch_event(local_event, this);
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
    if (m_back_store && m_back_store->size() != event.window_size()) {
        // Eagerly discard the backing store if we learn from this paint event that it needs to be bigger.
        // Otherwise we would have to wait for a resize event to tell us. This way we don't waste the
        // effort on painting into an undersized bitmap that will be thrown away anyway.
        m_back_store = nullptr;
    }
    bool created_new_backing_store = !m_back_store;
    if (!m_back_store) {
        m_back_store = create_backing_store(event.window_size());
        VERIFY(m_back_store);
    } else if (m_double_buffering_enabled) {
        bool was_purged = false;
        bool bitmap_has_memory = m_back_store->bitmap().set_nonvolatile(was_purged);
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

    auto rect = rects.first();
    if (rect.is_empty() || created_new_backing_store) {
        rects.clear();
        rects.append({ {}, event.window_size() });
    }

    for (auto& rect : rects) {
        PaintEvent paint_event(rect);
        m_main_widget->dispatch_event(paint_event, this);
    }

    if (m_double_buffering_enabled)
        flip(rects);
    else if (created_new_backing_store)
        set_current_backing_store(*m_back_store, true);

    if (is_visible())
        WindowServerConnection::the().async_did_finish_painting(m_window_id, rects);
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
        return m_focused_widget->dispatch_event(event, this);
    if (m_main_widget)
        return m_main_widget->dispatch_event(event, this);
}

void Window::handle_resize_event(ResizeEvent& event)
{
    auto new_size = event.size();
    if (m_back_store && m_back_store->size() != new_size)
        m_back_store = nullptr;
    if (!m_pending_paint_event_rects.is_empty()) {
        m_pending_paint_event_rects.clear_with_capacity();
        m_pending_paint_event_rects.append({ {}, new_size });
    }
    m_rect_when_windowless = { {}, new_size };
    if (m_main_widget)
        m_main_widget->set_relative_rect({ {}, new_size });
}

void Window::handle_input_entered_or_left_event(Core::Event& event)
{
    m_is_active_input = event.type() == Event::WindowInputEntered;
    if (on_active_input_change)
        on_active_input_change(m_is_active_input);
    if (m_main_widget)
        m_main_widget->dispatch_event(event, this);
    if (m_focused_widget)
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
    if (m_focused_widget)
        m_focused_widget->update();
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

    Application::the()->set_drag_hovered_widget({}, result.widget, result.local_position, event.mime_types());

    // NOTE: Setting the drag hovered widget may have executed arbitrary code, so re-check that the widget is still there.
    if (!result.widget)
        return;

    if (result.widget->has_pending_drop()) {
        DragEvent drag_move_event(static_cast<Event::Type>(event.type()), result.local_position, event.mime_types());
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

    if (event.type() == Event::WindowInputEntered || event.type() == Event::WindowInputLeft)
        return handle_input_entered_or_left_event(event);

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

    Core::Object::event(event);
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
    WindowServerConnection::the().async_invalidate_rect(m_window_id, { { 0, 0, rect.width(), rect.height() } }, true);
}

void Window::update(const Gfx::IntRect& a_rect)
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
            WindowServerConnection::the().async_invalidate_rect(m_window_id, rects, false);
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
        if (m_main_widget->min_width() >= 0)
            new_window_rect.set_width(max(new_window_rect.width(), m_main_widget->min_width()));
        if (m_main_widget->min_height() >= 0)
            new_window_rect.set_height(max(new_window_rect.height(), m_main_widget->min_height()));
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

    WindowServerConnection::the().async_set_window_has_alpha_channel(m_window_id, value);
    update();
}

void Window::set_double_buffering_enabled(bool value)
{
    VERIFY(!is_visible());
    m_double_buffering_enabled = value;
}

void Window::set_opacity(float opacity)
{
    m_opacity_when_windowless = opacity;
    if (!is_visible())
        return;
    WindowServerConnection::the().async_set_window_opacity(m_window_id, opacity);
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
    WindowServerConnection::the().async_set_window_alpha_hit_threshold(m_window_id, threshold);
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

void Window::set_current_backing_store(WindowBackingStore& backing_store, bool flush_immediately)
{
    auto& bitmap = backing_store.bitmap();
    WindowServerConnection::the().set_window_backing_store(m_window_id, 32, bitmap.pitch(), bitmap.anonymous_buffer().fd(), backing_store.serial(), bitmap.has_alpha_channel(), bitmap.size(), flush_immediately);
}

void Window::flip(const Vector<Gfx::IntRect, 32>& dirty_rects)
{
    swap(m_front_store, m_back_store);

    set_current_backing_store(*m_front_store);

    if (!m_back_store || m_back_store->size() != m_front_store->size()) {
        m_back_store = create_backing_store(m_front_store->size());
        VERIFY(m_back_store);
        memcpy(m_back_store->bitmap().scanline(0), m_front_store->bitmap().scanline(0), m_front_store->bitmap().size_in_bytes());
        m_back_store->bitmap().set_volatile();
        return;
    }

    // Copy whatever was painted from the front to the back.
    Painter painter(m_back_store->bitmap());
    for (auto& dirty_rect : dirty_rects)
        painter.blit(dirty_rect.location(), m_front_store->bitmap(), dirty_rect, 1.0f, false);

    m_back_store->bitmap().set_volatile();
}

OwnPtr<WindowBackingStore> Window::create_backing_store(const Gfx::IntSize& size)
{
    auto format = m_has_alpha_channel ? Gfx::BitmapFormat::BGRA8888 : Gfx::BitmapFormat::BGRx8888;

    VERIFY(!size.is_empty());
    size_t pitch = Gfx::Bitmap::minimum_pitch(size.width(), format);
    size_t size_in_bytes = size.height() * pitch;

    auto buffer_or_error = Core::AnonymousBuffer::create_with_size(round_up_to_power_of_two(size_in_bytes, PAGE_SIZE));
    if (buffer_or_error.is_error()) {
        perror("anon_create");
        return {};
    }

    // FIXME: Plumb scale factor here eventually.
    auto bitmap_or_error = Gfx::Bitmap::try_create_with_anonymous_buffer(format, buffer_or_error.release_value(), size, 1, {});
    if (bitmap_or_error.is_error()) {
        VERIFY(size.width() <= INT16_MAX);
        VERIFY(size.height() <= INT16_MAX);
        return {};
    }
    return make<WindowBackingStore>(bitmap_or_error.release_value());
}

void Window::set_modal(bool modal)
{
    VERIFY(!is_visible());
    m_modal = modal;
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

void Window::set_icon(const Gfx::Bitmap* icon)
{
    if (m_icon == icon)
        return;

    Gfx::IntSize icon_size = icon ? icon->size() : Gfx::IntSize(16, 16);

    m_icon = Gfx::Bitmap::try_create(Gfx::BitmapFormat::BGRA8888, icon_size).release_value_but_fixme_should_propagate_errors();
    if (icon) {
        Painter painter(*m_icon);
        painter.blit({ 0, 0 }, *icon, icon->rect());
    }

    apply_icon();
}

void Window::apply_icon()
{
    if (!m_icon)
        return;

    if (!is_visible())
        return;

    WindowServerConnection::the().async_set_window_icon_bitmap(m_window_id, m_icon->to_shareable_bitmap());
}

void Window::start_interactive_resize()
{
    WindowServerConnection::the().async_start_window_resize(m_window_id);
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
    WindowServerConnection::the().async_set_fullscreen(m_window_id, fullscreen);
}

void Window::set_frameless(bool frameless)
{
    if (m_frameless == frameless)
        return;
    m_frameless = frameless;
    if (!is_visible())
        return;
    WindowServerConnection::the().async_set_frameless(m_window_id, frameless);

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
    WindowServerConnection::the().async_set_forced_shadow(m_window_id, shadow);
}

bool Window::is_maximized() const
{
    if (!is_visible())
        return m_maximized_when_windowless;

    return WindowServerConnection::the().is_maximized(m_window_id);
}

void Window::set_maximized(bool maximized)
{
    m_maximized_when_windowless = maximized;
    if (!is_visible())
        return;

    WindowServerConnection::the().async_set_maximized(m_window_id, maximized);
}

void Window::schedule_relayout()
{
    if (m_layout_pending)
        return;
    m_layout_pending = true;
    deferred_invoke([this] {
        if (main_widget())
            main_widget()->do_layout();
        update();
        m_layout_pending = false;
    });
}

void Window::refresh_system_theme()
{
    WindowServerConnection::the().async_refresh_system_theme();
}

void Window::for_each_window(Badge<WindowServerConnection>, Function<void(Window&)> callback)
{
    for (auto& e : *reified_windows) {
        VERIFY(e.value);
        callback(*e.value);
    }
}

void Window::update_all_windows(Badge<WindowServerConnection>)
{
    for (auto& e : *reified_windows) {
        e.value->force_update();
    }
}

void Window::notify_state_changed(Badge<WindowServerConnection>, bool minimized, bool occluded)
{
    m_visible_for_timer_purposes = !minimized && !occluded;

    // When double buffering is enabled, minimization/occlusion means we can mark the front bitmap volatile (in addition to the back bitmap.)
    // When double buffering is disabled, there is only the back bitmap (which we can now mark volatile!)
    auto& store = m_double_buffering_enabled ? m_front_store : m_back_store;
    if (!store)
        return;
    if (minimized || occluded) {
        store->bitmap().set_volatile();
    } else {
        bool was_purged = false;
        bool bitmap_has_memory = store->bitmap().set_nonvolatile(was_purged);
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

Action* Window::action_for_key_event(const KeyEvent& event)
{
    Shortcut shortcut(event.modifiers(), (KeyCode)event.key());
    Action* found_action = nullptr;
    for_each_child_of_type<Action>([&](auto& action) {
        if (action.shortcut() == shortcut || action.alternate_shortcut() == shortcut) {
            found_action = &action;
            return IterationDecision::Break;
        }
        return IterationDecision::Continue;
    });
    return found_action;
}

void Window::set_base_size(const Gfx::IntSize& base_size)
{
    if (m_base_size == base_size)
        return;
    m_base_size = base_size;
    if (is_visible())
        WindowServerConnection::the().async_set_window_base_size_and_size_increment(m_window_id, m_base_size, m_size_increment);
}

void Window::set_size_increment(const Gfx::IntSize& size_increment)
{
    if (m_size_increment == size_increment)
        return;
    m_size_increment = size_increment;
    if (is_visible())
        WindowServerConnection::the().async_set_window_base_size_and_size_increment(m_window_id, m_base_size, m_size_increment);
}

void Window::set_resize_aspect_ratio(const Optional<Gfx::IntSize>& ratio)
{
    if (m_resize_aspect_ratio == ratio)
        return;

    m_resize_aspect_ratio = ratio;
    if (is_visible())
        WindowServerConnection::the().async_set_window_resize_aspect_ratio(m_window_id, m_resize_aspect_ratio);
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
    WindowServerConnection::the().async_set_window_progress(m_window_id, progress);
}

void Window::update_cursor()
{
    auto new_cursor = m_cursor;

    if (m_hovered_widget) {
        auto override_cursor = m_hovered_widget->override_cursor();
        if (override_cursor.has<NonnullRefPtr<Gfx::Bitmap>>() || override_cursor.get<Gfx::StandardCursor>() != Gfx::StandardCursor::None)
            new_cursor = move(override_cursor);
    }

    if (are_cursors_the_same(m_effective_cursor, new_cursor))
        return;
    m_effective_cursor = new_cursor;

    if (new_cursor.has<NonnullRefPtr<Gfx::Bitmap>>())
        WindowServerConnection::the().async_set_window_custom_cursor(m_window_id, new_cursor.get<NonnullRefPtr<Gfx::Bitmap>>()->to_shareable_bitmap());
    else
        WindowServerConnection::the().async_set_window_cursor(m_window_id, (u32)new_cursor.get<Gfx::StandardCursor>());
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

ErrorOr<NonnullRefPtr<Menu>> Window::try_add_menu(String name)
{
    auto menu = TRY(m_menubar->try_add_menu({}, move(name)));
    if (m_window_id) {
        menu->realize_menu_if_needed();
        WindowServerConnection::the().async_add_menu(m_window_id, menu->menu_id());
    }
    return menu;
}

Menu& Window::add_menu(String name)
{
    auto menu = MUST(try_add_menu(move(name)));
    return *menu;
}

void Window::flash_menubar_menu_for(const MenuItem& menu_item)
{
    auto menu_id = menu_item.menu_id();
    if (menu_id < 0)
        return;

    WindowServerConnection::the().async_flash_menubar_menu(m_window_id, menu_id);
}

bool Window::is_modified() const
{
    if (!m_window_id)
        return false;
    return WindowServerConnection::the().is_window_modified(m_window_id);
}

void Window::set_modified(bool modified)
{
    if (!m_window_id)
        return;
    WindowServerConnection::the().async_set_window_modified(m_window_id, modified);
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

}
