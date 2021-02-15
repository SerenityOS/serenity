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

#include <AK/Debug.h>
#include <AK/HashMap.h>
#include <AK/JsonObject.h>
#include <AK/NeverDestroyed.h>
#include <AK/ScopeGuard.h>
#include <LibCore/EventLoop.h>
#include <LibCore/MimeData.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Desktop.h>
#include <LibGUI/Event.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibGfx/Bitmap.h>
#include <fcntl.h>
#include <serenity.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

namespace GUI {

static i32 s_next_backing_store_serial;

class WindowBackingStore {
public:
    WindowBackingStore(NonnullRefPtr<Gfx::Bitmap> bitmap)
        : m_bitmap(bitmap)
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

    WindowServerConnection::the().send_sync<Messages::WindowServer::MoveWindowToFront>(m_window_id);
}

void Window::show()
{
    if (is_visible())
        return;

    auto* parent_window = find_parent_window();

    m_cursor = Gfx::StandardCursor::None;
    auto response = WindowServerConnection::the().send_sync<Messages::WindowServer::CreateWindow>(
        m_rect_when_windowless,
        !m_moved_by_client,
        m_has_alpha_channel,
        m_modal,
        m_minimizable,
        m_resizable,
        m_fullscreen,
        m_frameless,
        m_accessory,
        m_opacity_when_windowless,
        m_alpha_hit_threshold,
        m_base_size,
        m_size_increment,
        m_minimum_size_when_windowless,
        m_resize_aspect_ratio,
        (i32)m_window_type,
        m_title_when_windowless,
        parent_window ? parent_window->window_id() : 0);
    m_window_id = response->window_id();
    m_visible = true;

    apply_icon();

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
    auto response = WindowServerConnection::the().send_sync<Messages::WindowServer::DestroyWindow>(m_window_id);
    server_did_destroy();

    for (auto child_window_id : response->destroyed_window_ids()) {
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

void Window::set_title(const StringView& title)
{
    m_title_when_windowless = title;
    if (!is_visible())
        return;
    WindowServerConnection::the().send_sync<Messages::WindowServer::SetWindowTitle>(m_window_id, title);
}

String Window::title() const
{
    if (!is_visible())
        return m_title_when_windowless;
    return WindowServerConnection::the().send_sync<Messages::WindowServer::GetWindowTitle>(m_window_id)->title();
}

Gfx::IntRect Window::rect_in_menubar() const
{
    ASSERT(m_window_type == WindowType::MenuApplet);
    return WindowServerConnection::the().send_sync<Messages::WindowServer::GetWindowRectInMenubar>(m_window_id)->rect();
}

Gfx::IntRect Window::rect() const
{
    if (!is_visible())
        return m_rect_when_windowless;
    return WindowServerConnection::the().send_sync<Messages::WindowServer::GetWindowRect>(m_window_id)->rect();
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
    auto window_rect = WindowServerConnection::the().send_sync<Messages::WindowServer::SetWindowRect>(m_window_id, a_rect)->rect();
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

    return WindowServerConnection::the().send_sync<Messages::WindowServer::GetWindowMinimumSize>(m_window_id)->size();
}

void Window::set_minimum_size(const Gfx::IntSize& size)
{
    m_minimum_size_modified = true;
    m_minimum_size_when_windowless = size;

    if (is_visible())
        WindowServerConnection::the().send_sync<Messages::WindowServer::SetWindowMinimumSize>(m_window_id, size);
}

void Window::center_on_screen()
{
    auto window_rect = rect();
    window_rect.center_within(Desktop::the().rect());
    set_rect(window_rect);
}

void Window::center_within(const Window& other)
{
    if (this == &other)
        return;
    auto window_rect = rect();
    window_rect.center_within(other.rect());
    set_rect(window_rect);
}

void Window::set_window_type(WindowType window_type)
{
    m_window_type = window_type;

    if (!m_minimum_size_modified) {
        // Apply minimum size defaults.
        if (m_window_type == WindowType::Normal)
            m_minimum_size_when_windowless = { 50, 50 };
        else
            m_minimum_size_when_windowless = { 1, 1 };
    }
}

void Window::set_cursor(Gfx::StandardCursor cursor)
{
    if (m_cursor == cursor)
        return;
    m_cursor = cursor;
    m_custom_cursor = nullptr;
    update_cursor();
}

void Window::set_cursor(const Gfx::Bitmap& cursor)
{
    if (m_custom_cursor == &cursor)
        return;
    m_cursor = Gfx::StandardCursor::None;
    m_custom_cursor = &cursor;
    update_cursor();
}

void Window::handle_drop_event(DropEvent& event)
{
    if (!m_main_widget)
        return;
    auto result = m_main_widget->hit_test(event.position());
    auto local_event = make<DropEvent>(result.local_position, event.text(), event.mime_data());
    ASSERT(result.widget);
    result.widget->dispatch_event(*local_event, this);

    Application::the()->set_drag_hovered_widget({}, nullptr);
}

void Window::handle_mouse_event(MouseEvent& event)
{
    if (m_global_cursor_tracking_widget) {
        auto window_relative_rect = m_global_cursor_tracking_widget->window_relative_rect();
        Gfx::IntPoint local_point { event.x() - window_relative_rect.x(), event.y() - window_relative_rect.y() };
        auto local_event = make<MouseEvent>((Event::Type)event.type(), local_point, event.buttons(), event.button(), event.modifiers(), event.wheel_delta());
        m_global_cursor_tracking_widget->dispatch_event(*local_event, this);
        return;
    }
    if (m_automatic_cursor_tracking_widget) {
        auto window_relative_rect = m_automatic_cursor_tracking_widget->window_relative_rect();
        Gfx::IntPoint local_point { event.x() - window_relative_rect.x(), event.y() - window_relative_rect.y() };
        auto local_event = make<MouseEvent>((Event::Type)event.type(), local_point, event.buttons(), event.button(), event.modifiers(), event.wheel_delta());
        m_automatic_cursor_tracking_widget->dispatch_event(*local_event, this);
        if (event.buttons() == 0)
            m_automatic_cursor_tracking_widget = nullptr;
        return;
    }
    if (!m_main_widget)
        return;
    auto result = m_main_widget->hit_test(event.position());
    auto local_event = make<MouseEvent>((Event::Type)event.type(), result.local_position, event.buttons(), event.button(), event.modifiers(), event.wheel_delta());
    ASSERT(result.widget);
    set_hovered_widget(result.widget);
    if (event.buttons() != 0 && !m_automatic_cursor_tracking_widget)
        m_automatic_cursor_tracking_widget = *result.widget;
    if (result.widget != m_global_cursor_tracking_widget.ptr())
        return result.widget->dispatch_event(*local_event, this);
    return;
}

void Window::handle_multi_paint_event(MultiPaintEvent& event)
{
    if (!is_visible())
        return;
    if (!m_main_widget)
        return;
    auto rects = event.rects();
    ASSERT(!rects.is_empty());
    if (m_back_store && m_back_store->size() != event.window_size()) {
        // Eagerly discard the backing store if we learn from this paint event that it needs to be bigger.
        // Otherwise we would have to wait for a resize event to tell us. This way we don't waste the
        // effort on painting into an undersized bitmap that will be thrown away anyway.
        m_back_store = nullptr;
    }
    bool created_new_backing_store = !m_back_store;
    if (!m_back_store) {
        m_back_store = create_backing_store(event.window_size());
        ASSERT(m_back_store);
    } else if (m_double_buffering_enabled) {
        bool still_has_pixels = m_back_store->bitmap().set_nonvolatile();
        if (!still_has_pixels) {
            m_back_store = create_backing_store(event.window_size());
            ASSERT(m_back_store);
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

    if (is_visible()) {
        Vector<Gfx::IntRect> rects_to_send;
        for (auto& r : rects)
            rects_to_send.append(r);
        WindowServerConnection::the().post_message(Messages::WindowServer::DidFinishPainting(m_window_id, rects_to_send));
    }
}

void Window::handle_key_event(KeyEvent& event)
{
    if (!m_focused_widget && event.type() == Event::KeyDown && event.key() == Key_Tab && !event.ctrl() && !event.alt() && !event.logo()) {
        focus_a_widget_if_possible(FocusSource::Keyboard);
        return;
    }

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

void Window::handle_drag_move_event(DragEvent& event)
{
    if (!m_main_widget)
        return;
    auto result = m_main_widget->hit_test(event.position());
    ASSERT(result.widget);

    Application::the()->set_drag_hovered_widget({}, result.widget, result.local_position, event.mime_types());

    // NOTE: Setting the drag hovered widget may have executed arbitrary code, so re-check that the widget is still there.
    if (!result.widget)
        return;

    if (result.widget->has_pending_drop()) {
        DragEvent drag_move_event(static_cast<Event::Type>(event.type()), result.local_position, event.mime_types());
        result.widget->dispatch_event(drag_move_event, this);
    }
}

void Window::handle_left_event()
{
    set_hovered_widget(nullptr);
    Application::the()->set_drag_hovered_widget({}, nullptr);
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

    if (event.type() == Event::WindowLeft)
        return handle_left_event();

    if (event.type() == Event::Resize)
        return handle_resize_event(static_cast<ResizeEvent&>(event));

    if (event.type() > Event::__Begin_WM_Events && event.type() < Event::__End_WM_Events)
        return wm_event(static_cast<WMEvent&>(event));

    if (event.type() == Event::DragMove)
        return handle_drag_move_event(static_cast<DragEvent&>(event));

    if (event.type() == Event::ThemeChange)
        return handle_theme_change_event(static_cast<ThemeChangeEvent&>(event));

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
    WindowServerConnection::the().post_message(Messages::WindowServer::InvalidateRect(m_window_id, { { 0, 0, rect.width(), rect.height() } }, true));
}

void Window::update(const Gfx::IntRect& a_rect)
{
    if (!is_visible())
        return;

    for (auto& pending_rect : m_pending_paint_event_rects) {
        if (pending_rect.contains(a_rect)) {
#if UPDATE_COALESCING_DEBUG
            dbgln("Ignoring {} since it's contained by pending rect {}", a_rect, pending_rect);
#endif
            return;
        }
    }

    if (m_pending_paint_event_rects.is_empty()) {
        deferred_invoke([this](auto&) {
            auto rects = move(m_pending_paint_event_rects);
            if (rects.is_empty())
                return;
            Vector<Gfx::IntRect> rects_to_send;
            for (auto& r : rects)
                rects_to_send.append(r);
            WindowServerConnection::the().post_message(Messages::WindowServer::InvalidateRect(m_window_id, rects_to_send, false));
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

void Window::set_focused_widget(Widget* widget, FocusSource source)
{
    if (m_focused_widget == widget)
        return;

    WeakPtr<Widget> previously_focused_widget = m_focused_widget;
    m_focused_widget = widget;

    if (previously_focused_widget) {
        Core::EventLoop::current().post_event(*previously_focused_widget, make<FocusEvent>(Event::FocusOut, source));
        previously_focused_widget->update();
        if (previously_focused_widget && previously_focused_widget->on_focus_change)
            previously_focused_widget->on_focus_change(previously_focused_widget->is_focused(), source);
    }
    if (m_focused_widget) {
        Core::EventLoop::current().post_event(*m_focused_widget, make<FocusEvent>(Event::FocusIn, source));
        m_focused_widget->update();
        if (m_focused_widget && m_focused_widget->on_focus_change)
            m_focused_widget->on_focus_change(m_focused_widget->is_focused(), source);
    }
}

void Window::set_global_cursor_tracking_widget(Widget* widget)
{
    if (widget == m_global_cursor_tracking_widget)
        return;
    m_global_cursor_tracking_widget = widget;
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

    WindowServerConnection::the().send_sync<Messages::WindowServer::SetWindowHasAlphaChannel>(m_window_id, value);
    update();
}

void Window::set_double_buffering_enabled(bool value)
{
    ASSERT(!is_visible());
    m_double_buffering_enabled = value;
}

void Window::set_opacity(float opacity)
{
    m_opacity_when_windowless = opacity;
    if (!is_visible())
        return;
    WindowServerConnection::the().send_sync<Messages::WindowServer::SetWindowOpacity>(m_window_id, opacity);
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
    WindowServerConnection::the().send_sync<Messages::WindowServer::SetWindowAlphaHitThreshold>(m_window_id, threshold);
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
}

void Window::set_current_backing_store(WindowBackingStore& backing_store, bool flush_immediately)
{
    auto& bitmap = backing_store.bitmap();
    WindowServerConnection::the().send_sync<Messages::WindowServer::SetWindowBackingStore>(m_window_id, 32, bitmap.pitch(), bitmap.anon_fd(), backing_store.serial(), bitmap.has_alpha_channel(), bitmap.size(), flush_immediately);
}

void Window::flip(const Vector<Gfx::IntRect, 32>& dirty_rects)
{
    swap(m_front_store, m_back_store);

    set_current_backing_store(*m_front_store);

    if (!m_back_store || m_back_store->size() != m_front_store->size()) {
        m_back_store = create_backing_store(m_front_store->size());
        ASSERT(m_back_store);
        memcpy(m_back_store->bitmap().scanline(0), m_front_store->bitmap().scanline(0), m_front_store->bitmap().size_in_bytes());
        m_back_store->bitmap().set_volatile();
        return;
    }

    // Copy whatever was painted from the front to the back.
    Painter painter(m_back_store->bitmap());
    for (auto& dirty_rect : dirty_rects)
        painter.blit(dirty_rect.location(), m_front_store->bitmap(), dirty_rect);

    m_back_store->bitmap().set_volatile();
}

OwnPtr<WindowBackingStore> Window::create_backing_store(const Gfx::IntSize& size)
{
    auto format = m_has_alpha_channel ? Gfx::BitmapFormat::RGBA32 : Gfx::BitmapFormat::RGB32;

    ASSERT(!size.is_empty());
    size_t pitch = Gfx::Bitmap::minimum_pitch(size.width(), format);
    size_t size_in_bytes = size.height() * pitch;

    auto anon_fd = anon_create(round_up_to_power_of_two(size_in_bytes, PAGE_SIZE), O_CLOEXEC);
    if (anon_fd < 0) {
        perror("anon_create");
        return {};
    }

    // FIXME: Plumb scale factor here eventually.
    auto bitmap = Gfx::Bitmap::create_with_anon_fd(format, anon_fd, size, 1, {}, Gfx::Bitmap::ShouldCloseAnonymousFile::No);
    if (!bitmap)
        return {};
    return make<WindowBackingStore>(bitmap.release_nonnull());
}

void Window::set_modal(bool modal)
{
    ASSERT(!is_visible());
    m_modal = modal;
}

void Window::wm_event(WMEvent&)
{
}

void Window::set_icon(const Gfx::Bitmap* icon)
{
    if (m_icon == icon)
        return;

    Gfx::IntSize icon_size = icon ? icon->size() : Gfx::IntSize(16, 16);

    m_icon = Gfx::Bitmap::create(Gfx::BitmapFormat::RGBA32, icon_size);
    ASSERT(m_icon);
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

    WindowServerConnection::the().send_sync<Messages::WindowServer::SetWindowIconBitmap>(m_window_id, m_icon->to_shareable_bitmap());
}

void Window::start_interactive_resize()
{
    WindowServerConnection::the().post_message(Messages::WindowServer::StartWindowResize(m_window_id));
}

Vector<Widget*> Window::focusable_widgets(FocusSource source) const
{
    if (!m_main_widget)
        return {};

    HashTable<Widget*> seen_widgets;
    Vector<Widget*> collected_widgets;

    Function<void(Widget&)> collect_focusable_widgets = [&](auto& widget) {
        bool widget_accepts_focus = false;
        switch (source) {
        case FocusSource::Keyboard:
            widget_accepts_focus = ((unsigned)widget.focus_policy() & (unsigned)FocusPolicy::TabFocus);
            break;
        case FocusSource::Mouse:
            widget_accepts_focus = ((unsigned)widget.focus_policy() & (unsigned)FocusPolicy::ClickFocus);
            break;
        case FocusSource::Programmatic:
            widget_accepts_focus = widget.focus_policy() != FocusPolicy::NoFocus;
            break;
        }

        if (widget_accepts_focus) {
            auto& effective_focus_widget = widget.focus_proxy() ? *widget.focus_proxy() : widget;
            if (seen_widgets.set(&effective_focus_widget) == AK::HashSetResult::InsertedNewEntry)
                collected_widgets.append(&effective_focus_widget);
        }
        widget.for_each_child_widget([&](auto& child) {
            if (!child.is_visible())
                return IterationDecision::Continue;
            if (!child.is_enabled())
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
    WindowServerConnection::the().send_sync<Messages::WindowServer::SetFullscreen>(m_window_id, fullscreen);
}

bool Window::is_maximized() const
{
    if (!is_visible())
        return false;

    return WindowServerConnection::the().send_sync<Messages::WindowServer::IsMaximized>(m_window_id)->maximized();
}

void Window::schedule_relayout()
{
    if (m_layout_pending)
        return;
    m_layout_pending = true;
    deferred_invoke([this](auto&) {
        if (main_widget())
            main_widget()->do_layout();
        update();
        m_layout_pending = false;
    });
}

void Window::refresh_system_theme()
{
    WindowServerConnection::the().post_message(Messages::WindowServer::RefreshSystemTheme());
}

void Window::for_each_window(Badge<WindowServerConnection>, Function<void(Window&)> callback)
{
    for (auto& e : *reified_windows) {
        ASSERT(e.value);
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
        if (!store->bitmap().set_nonvolatile()) {
            store = nullptr;
            update();
        }
    }
}

Action* Window::action_for_key_event(const KeyEvent& event)
{
    Shortcut shortcut(event.modifiers(), (KeyCode)event.key());
    Action* found_action = nullptr;
    for_each_child_of_type<Action>([&](auto& action) {
        if (action.shortcut() == shortcut) {
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
        WindowServerConnection::the().send_sync<Messages::WindowServer::SetWindowBaseSizeAndSizeIncrement>(m_window_id, m_base_size, m_size_increment);
}

void Window::set_size_increment(const Gfx::IntSize& size_increment)
{
    if (m_size_increment == size_increment)
        return;
    m_size_increment = size_increment;
    if (is_visible())
        WindowServerConnection::the().send_sync<Messages::WindowServer::SetWindowBaseSizeAndSizeIncrement>(m_window_id, m_base_size, m_size_increment);
}

void Window::set_resize_aspect_ratio(const Optional<Gfx::IntSize>& ratio)
{
    if (m_resize_aspect_ratio == ratio)
        return;

    m_resize_aspect_ratio = ratio;
    if (is_visible())
        WindowServerConnection::the().send_sync<Messages::WindowServer::SetWindowResizeAspectRatio>(m_window_id, m_resize_aspect_ratio);
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
    if (m_global_cursor_tracking_widget == &widget)
        m_global_cursor_tracking_widget = nullptr;
    if (m_automatic_cursor_tracking_widget == &widget)
        m_automatic_cursor_tracking_widget = nullptr;
}

void Window::set_progress(int progress)
{
    ASSERT(m_window_id);
    WindowServerConnection::the().post_message(Messages::WindowServer::SetWindowProgress(m_window_id, progress));
}

void Window::update_cursor()
{
    Gfx::StandardCursor new_cursor;

    if (m_hovered_widget && m_hovered_widget->override_cursor() != Gfx::StandardCursor::None)
        new_cursor = m_hovered_widget->override_cursor();
    else
        new_cursor = m_cursor;

    if (m_effective_cursor == new_cursor)
        return;
    m_effective_cursor = new_cursor;

    if (m_custom_cursor)
        WindowServerConnection::the().send_sync<Messages::WindowServer::SetWindowCustomCursor>(m_window_id, m_custom_cursor->to_shareable_bitmap());
    else
        WindowServerConnection::the().send_sync<Messages::WindowServer::SetWindowCursor>(m_window_id, (u32)m_effective_cursor);
}

void Window::focus_a_widget_if_possible(FocusSource source)
{
    auto focusable_widgets = this->focusable_widgets(source);
    if (!focusable_widgets.is_empty())
        set_focused_widget(focusable_widgets[0], source);
}

void Window::did_disable_focused_widget(Badge<Widget>)
{
    focus_a_widget_if_possible(FocusSource::Mouse);
}

bool Window::is_active() const
{
    ASSERT(Application::the());
    return this == Application::the()->active_window();
}

Gfx::Bitmap* Window::back_bitmap()
{
    return m_back_store ? &m_back_store->bitmap() : nullptr;
}

}
