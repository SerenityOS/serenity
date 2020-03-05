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

#include <AK/HashMap.h>
#include <AK/JsonObject.h>
#include <AK/NeverDestroyed.h>
#include <AK/SharedBuffer.h>
#include <LibCore/EventLoop.h>
#include <LibCore/MimeData.h>
#include <LibGUI/Action.h>
#include <LibGUI/Application.h>
#include <LibGUI/Event.h>
#include <LibGUI/Painter.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>
#include <LibGUI/WindowServerConnection.h>
#include <LibGfx/Bitmap.h>
#include <serenity.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

//#define UPDATE_COALESCING_DEBUG

namespace GUI {

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
    m_rect_when_windowless = { 100, 400, 140, 140 };
    m_title_when_windowless = "GUI::Window";
}

Window::~Window()
{
    all_windows->remove(this);
    hide();
}

void Window::close()
{
    hide();
}

void Window::move_to_front()
{
    if (!m_window_id)
        return;

    WindowServerConnection::the().send_sync<Messages::WindowServer::MoveWindowToFront>(m_window_id);
}

void Window::show()
{
    if (m_window_id)
        return;
    auto response = WindowServerConnection::the().send_sync<Messages::WindowServer::CreateWindow>(
        m_rect_when_windowless,
        m_has_alpha_channel,
        m_modal,
        m_minimizable,
        m_resizable,
        m_fullscreen,
        m_show_titlebar,
        m_opacity_when_windowless,
        m_base_size,
        m_size_increment,
        (i32)m_window_type,
        m_title_when_windowless);
    m_window_id = response->window_id();

    apply_icon();

    reified_windows->set(m_window_id, this);
    Application::the().did_create_window({});
    update();
}

void Window::hide()
{
    if (!m_window_id)
        return;
    reified_windows->remove(m_window_id);
    WindowServerConnection::the().send_sync<Messages::WindowServer::DestroyWindow>(m_window_id);
    m_window_id = 0;
    m_pending_paint_event_rects.clear();
    m_back_bitmap = nullptr;
    m_front_bitmap = nullptr;

    bool app_has_visible_windows = false;
    for (auto& window : *all_windows) {
        if (window->is_visible()) {
            app_has_visible_windows = true;
            break;
        }
    }
    if (!app_has_visible_windows)
        Application::the().did_delete_last_window({});
}

void Window::set_title(const StringView& title)
{
    m_title_when_windowless = title;
    if (!m_window_id)
        return;
    WindowServerConnection::the().send_sync<Messages::WindowServer::SetWindowTitle>(m_window_id, title);
}

String Window::title() const
{
    if (!m_window_id)
        return m_title_when_windowless;
    return WindowServerConnection::the().send_sync<Messages::WindowServer::GetWindowTitle>(m_window_id)->title();
}

Gfx::Rect Window::rect() const
{
    if (!m_window_id)
        return m_rect_when_windowless;
    return WindowServerConnection::the().send_sync<Messages::WindowServer::GetWindowRect>(m_window_id)->rect();
}

void Window::set_rect(const Gfx::Rect& a_rect)
{
    m_rect_when_windowless = a_rect;
    if (!m_window_id) {
        if (m_main_widget)
            m_main_widget->resize(m_rect_when_windowless.size());
        return;
    }
    WindowServerConnection::the().send_sync<Messages::WindowServer::SetWindowRect>(m_window_id, a_rect);
    if (m_back_bitmap && m_back_bitmap->size() != a_rect.size())
        m_back_bitmap = nullptr;
    if (m_front_bitmap && m_front_bitmap->size() != a_rect.size())
        m_front_bitmap = nullptr;
    if (m_main_widget)
        m_main_widget->resize(a_rect.size());
}

void Window::set_window_type(WindowType window_type)
{
    m_window_type = window_type;
}

void Window::set_override_cursor(StandardCursor cursor)
{
    if (!m_window_id)
        return;
    WindowServerConnection::the().send_sync<Messages::WindowServer::SetWindowOverrideCursor>(m_window_id, (u32)cursor);
}

void Window::event(Core::Event& event)
{
    if (event.type() == Event::Drop) {
        auto& drop_event = static_cast<DropEvent&>(event);
        if (!m_main_widget)
            return;
        auto result = m_main_widget->hit_test(drop_event.position());
        auto local_event = make<DropEvent>(result.local_position, drop_event.text(), drop_event.mime_data());
        ASSERT(result.widget);
        return result.widget->dispatch_event(*local_event, this);
    }

    if (event.type() == Event::MouseUp || event.type() == Event::MouseDown || event.type() == Event::MouseDoubleClick || event.type() == Event::MouseMove || event.type() == Event::MouseWheel) {
        auto& mouse_event = static_cast<MouseEvent&>(event);
        if (m_global_cursor_tracking_widget) {
            auto window_relative_rect = m_global_cursor_tracking_widget->window_relative_rect();
            Gfx::Point local_point { mouse_event.x() - window_relative_rect.x(), mouse_event.y() - window_relative_rect.y() };
            auto local_event = make<MouseEvent>((Event::Type)event.type(), local_point, mouse_event.buttons(), mouse_event.button(), mouse_event.modifiers(), mouse_event.wheel_delta());
            m_global_cursor_tracking_widget->dispatch_event(*local_event, this);
            return;
        }
        if (m_automatic_cursor_tracking_widget) {
            auto window_relative_rect = m_automatic_cursor_tracking_widget->window_relative_rect();
            Gfx::Point local_point { mouse_event.x() - window_relative_rect.x(), mouse_event.y() - window_relative_rect.y() };
            auto local_event = make<MouseEvent>((Event::Type)event.type(), local_point, mouse_event.buttons(), mouse_event.button(), mouse_event.modifiers(), mouse_event.wheel_delta());
            m_automatic_cursor_tracking_widget->dispatch_event(*local_event, this);
            if (mouse_event.buttons() == 0)
                m_automatic_cursor_tracking_widget = nullptr;
            return;
        }
        if (!m_main_widget)
            return;
        auto result = m_main_widget->hit_test(mouse_event.position());
        auto local_event = make<MouseEvent>((Event::Type)event.type(), result.local_position, mouse_event.buttons(), mouse_event.button(), mouse_event.modifiers(), mouse_event.wheel_delta());
        ASSERT(result.widget);
        set_hovered_widget(result.widget);
        if (mouse_event.buttons() != 0 && !m_automatic_cursor_tracking_widget)
            m_automatic_cursor_tracking_widget = result.widget->make_weak_ptr();
        if (result.widget != m_global_cursor_tracking_widget.ptr())
            return result.widget->dispatch_event(*local_event, this);
        return;
    }

    if (event.type() == Event::MultiPaint) {
        if (!m_window_id)
            return;
        if (!m_main_widget)
            return;
        auto& paint_event = static_cast<MultiPaintEvent&>(event);
        auto rects = paint_event.rects();
        ASSERT(!rects.is_empty());
        if (m_back_bitmap && m_back_bitmap->size() != paint_event.window_size()) {
            // Eagerly discard the backing store if we learn from this paint event that it needs to be bigger.
            // Otherwise we would have to wait for a resize event to tell us. This way we don't waste the
            // effort on painting into an undersized bitmap that will be thrown away anyway.
            m_back_bitmap = nullptr;
        }
        bool created_new_backing_store = !m_back_bitmap;
        if (!m_back_bitmap) {
            m_back_bitmap = create_backing_bitmap(paint_event.window_size());
        } else if (m_double_buffering_enabled) {
            bool still_has_pixels = m_back_bitmap->shared_buffer()->set_nonvolatile();
            if (!still_has_pixels) {
                m_back_bitmap = create_backing_bitmap(paint_event.window_size());
                created_new_backing_store = true;
            }
        }

        auto rect = rects.first();
        if (rect.is_empty() || created_new_backing_store) {
            rects.clear();
            rects.append({ {}, paint_event.window_size() });
        }

        for (auto& rect : rects)
            m_main_widget->dispatch_event(*make<PaintEvent>(rect), this);

        if (m_double_buffering_enabled)
            flip(rects);
        else if (created_new_backing_store)
            set_current_backing_bitmap(*m_back_bitmap, true);

        if (m_window_id) {
            Vector<Gfx::Rect> rects_to_send;
            for (auto& r : rects)
                rects_to_send.append(r);
            WindowServerConnection::the().post_message(Messages::WindowServer::DidFinishPainting(m_window_id, rects_to_send));
        }
        return;
    }

    if (event.type() == Event::KeyUp || event.type() == Event::KeyDown) {
        if (m_focused_widget)
            return m_focused_widget->dispatch_event(event, this);
        if (m_main_widget)
            return m_main_widget->dispatch_event(event, this);
        return;
    }

    if (event.type() == Event::WindowBecameActive || event.type() == Event::WindowBecameInactive) {
        m_is_active = event.type() == Event::WindowBecameActive;
        if (m_main_widget)
            m_main_widget->dispatch_event(event, this);
        if (m_focused_widget)
            m_focused_widget->update();
        return;
    }

    if (event.type() == Event::WindowCloseRequest) {
        if (on_close_request) {
            if (on_close_request() == Window::CloseRequestDecision::StayOpen)
                return;
        }
        close();
        return;
    }

    if (event.type() == Event::WindowLeft) {
        set_hovered_widget(nullptr);
        return;
    }

    if (event.type() == Event::Resize) {
        auto new_size = static_cast<ResizeEvent&>(event).size();
        if (m_back_bitmap && m_back_bitmap->size() != new_size)
            m_back_bitmap = nullptr;
        if (!m_pending_paint_event_rects.is_empty()) {
            m_pending_paint_event_rects.clear_with_capacity();
            m_pending_paint_event_rects.append({ {}, new_size });
        }
        m_rect_when_windowless = { {}, new_size };
        m_main_widget->set_relative_rect({ {}, new_size });
        return;
    }

    if (event.type() > Event::__Begin_WM_Events && event.type() < Event::__End_WM_Events)
        return wm_event(static_cast<WMEvent&>(event));

    if (event.type() == Event::DragMove) {
        if (!m_main_widget)
            return;
        auto& drag_event = static_cast<DragEvent&>(event);
        auto result = m_main_widget->hit_test(drag_event.position());
        auto local_event = make<DragEvent>(static_cast<Event::Type>(drag_event.type()), result.local_position, drag_event.data_type());
        ASSERT(result.widget);
        return result.widget->dispatch_event(*local_event, this);
    }

    Core::Object::event(event);
}

bool Window::is_visible() const
{
    return m_window_id != 0;
}

void Window::update()
{
    auto rect = this->rect();
    update({ 0, 0, rect.width(), rect.height() });
}

void Window::force_update()
{
    auto rect = this->rect();
    WindowServerConnection::the().post_message(Messages::WindowServer::InvalidateRect(m_window_id, { { 0, 0, rect.width(), rect.height() } }, true));
}

void Window::update(const Gfx::Rect& a_rect)
{
    if (!m_window_id)
        return;

    for (auto& pending_rect : m_pending_paint_event_rects) {
        if (pending_rect.contains(a_rect)) {
#ifdef UPDATE_COALESCING_DEBUG
            dbgprintf("Ignoring %s since it's contained by pending rect %s\n", a_rect.to_string().characters(), pending_rect.to_string().characters());
#endif
            return;
        }
    }

    if (m_pending_paint_event_rects.is_empty()) {
        deferred_invoke([this](auto&) {
            auto rects = move(m_pending_paint_event_rects);
            if (rects.is_empty())
                return;
            Vector<Gfx::Rect> rects_to_send;
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
    if (m_main_widget)
        remove_child(*m_main_widget);
    m_main_widget = widget;
    if (m_main_widget) {
        add_child(*widget);
        auto new_window_rect = rect();
        if (m_main_widget->horizontal_size_policy() == SizePolicy::Fixed)
            new_window_rect.set_width(m_main_widget->preferred_size().width());
        if (m_main_widget->vertical_size_policy() == SizePolicy::Fixed)
            new_window_rect.set_height(m_main_widget->preferred_size().height());
        set_rect(new_window_rect);
        m_main_widget->set_relative_rect({ {}, new_window_rect.size() });
        m_main_widget->set_window(this);
        if (m_main_widget->accepts_focus())
            m_main_widget->set_focus(true);
    }
    update();
}

void Window::set_focused_widget(Widget* widget)
{
    if (m_focused_widget == widget)
        return;
    if (m_focused_widget) {
        Core::EventLoop::current().post_event(*m_focused_widget, make<Event>(Event::FocusOut));
        m_focused_widget->update();
    }
    m_focused_widget = widget ? widget->make_weak_ptr() : nullptr;
    if (m_focused_widget) {
        Core::EventLoop::current().post_event(*m_focused_widget, make<Event>(Event::FocusIn));
        m_focused_widget->update();
    }
}

void Window::set_global_cursor_tracking_widget(Widget* widget)
{
    if (widget == m_global_cursor_tracking_widget)
        return;
    m_global_cursor_tracking_widget = widget ? widget->make_weak_ptr() : nullptr;
}

void Window::set_automatic_cursor_tracking_widget(Widget* widget)
{
    if (widget == m_automatic_cursor_tracking_widget)
        return;
    m_automatic_cursor_tracking_widget = widget ? widget->make_weak_ptr() : nullptr;
}

void Window::set_has_alpha_channel(bool value)
{
    if (m_has_alpha_channel == value)
        return;
    m_has_alpha_channel = value;
    if (!m_window_id)
        return;

    m_pending_paint_event_rects.clear();
    m_back_bitmap = nullptr;
    m_front_bitmap = nullptr;

    WindowServerConnection::the().send_sync<Messages::WindowServer::SetWindowHasAlphaChannel>(m_window_id, value);
    update();
}

void Window::set_double_buffering_enabled(bool value)
{
    ASSERT(!m_window_id);
    m_double_buffering_enabled = value;
}

void Window::set_opacity(float opacity)
{
    m_opacity_when_windowless = opacity;
    if (!m_window_id)
        return;
    WindowServerConnection::the().send_sync<Messages::WindowServer::SetWindowOpacity>(m_window_id, opacity);
}

void Window::set_hovered_widget(Widget* widget)
{
    if (widget == m_hovered_widget)
        return;

    if (m_hovered_widget)
        Core::EventLoop::current().post_event(*m_hovered_widget, make<Event>(Event::Leave));

    m_hovered_widget = widget ? widget->make_weak_ptr() : nullptr;

    if (m_hovered_widget)
        Core::EventLoop::current().post_event(*m_hovered_widget, make<Event>(Event::Enter));
}

void Window::set_current_backing_bitmap(Gfx::Bitmap& bitmap, bool flush_immediately)
{
    WindowServerConnection::the().send_sync<Messages::WindowServer::SetWindowBackingStore>(m_window_id, 32, bitmap.pitch(), bitmap.shbuf_id(), bitmap.has_alpha_channel(), bitmap.size(), flush_immediately);
}

void Window::flip(const Vector<Gfx::Rect, 32>& dirty_rects)
{
    swap(m_front_bitmap, m_back_bitmap);

    set_current_backing_bitmap(*m_front_bitmap);

    if (!m_back_bitmap || m_back_bitmap->size() != m_front_bitmap->size()) {
        m_back_bitmap = create_backing_bitmap(m_front_bitmap->size());
        memcpy(m_back_bitmap->scanline(0), m_front_bitmap->scanline(0), m_front_bitmap->size_in_bytes());
        m_back_bitmap->shared_buffer()->set_volatile();
        return;
    }

    // Copy whatever was painted from the front to the back.
    Painter painter(*m_back_bitmap);
    for (auto& dirty_rect : dirty_rects)
        painter.blit(dirty_rect.location(), *m_front_bitmap, dirty_rect);

    m_back_bitmap->shared_buffer()->set_volatile();
}

NonnullRefPtr<Gfx::Bitmap> Window::create_shared_bitmap(Gfx::BitmapFormat format, const Gfx::Size& size)
{
    ASSERT(WindowServerConnection::the().server_pid());
    ASSERT(!size.is_empty());
    size_t pitch = round_up_to_power_of_two(size.width() * sizeof(Gfx::RGBA32), 16);
    size_t size_in_bytes = size.height() * pitch;
    auto shared_buffer = SharedBuffer::create_with_size(size_in_bytes);
    ASSERT(shared_buffer);
    shared_buffer->share_with(WindowServerConnection::the().server_pid());
    return Gfx::Bitmap::create_with_shared_buffer(format, *shared_buffer, size);
}

NonnullRefPtr<Gfx::Bitmap> Window::create_backing_bitmap(const Gfx::Size& size)
{
    auto format = m_has_alpha_channel ? Gfx::BitmapFormat::RGBA32 : Gfx::BitmapFormat::RGB32;
    return create_shared_bitmap(format, size);
}

void Window::set_modal(bool modal)
{
    ASSERT(!m_window_id);
    m_modal = modal;
}

void Window::wm_event(WMEvent&)
{
}

void Window::set_icon(const Gfx::Bitmap* icon)
{
    if (m_icon == icon)
        return;

    m_icon = create_shared_bitmap(Gfx::BitmapFormat::RGBA32, icon->size());
    {
        Painter painter(*m_icon);
        painter.blit({ 0, 0 }, *icon, icon->rect());
    }

    apply_icon();
}

void Window::apply_icon()
{
    if (!m_icon)
        return;

    if (!m_window_id)
        return;

    int rc = shbuf_seal(m_icon->shbuf_id());
    ASSERT(rc == 0);

    rc = shbuf_allow_all(m_icon->shbuf_id());
    ASSERT(rc == 0);

    static bool has_set_process_icon;
    if (!has_set_process_icon)
        set_process_icon(m_icon->shbuf_id());

    WindowServerConnection::the().send_sync<Messages::WindowServer::SetWindowIconBitmap>(m_window_id, m_icon->shbuf_id(), m_icon->size());
}

void Window::start_wm_resize()
{
    WindowServerConnection::the().post_message(Messages::WindowServer::WM_StartWindowResize(WindowServerConnection::the().my_client_id(), m_window_id));
}

Vector<Widget*> Window::focusable_widgets() const
{
    if (!m_main_widget)
        return {};

    Vector<Widget*> collected_widgets;

    Function<void(Widget&)> collect_focusable_widgets = [&](auto& widget) {
        if (widget.accepts_focus())
            collected_widgets.append(&widget);
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

void Window::save_to(AK::JsonObject& json)
{
    json.set("title", title());
    json.set("visible", is_visible());
    json.set("active", is_active());
    json.set("minimizable", is_minimizable());
    json.set("resizable", is_resizable());
    json.set("fullscreen", is_fullscreen());
    json.set("rect", rect().to_string());
    json.set("base_size", base_size().to_string());
    json.set("size_increment", size_increment().to_string());
    Core::Object::save_to(json);
}

void Window::set_fullscreen(bool fullscreen)
{
    if (m_fullscreen == fullscreen)
        return;
    m_fullscreen = fullscreen;
    if (!m_window_id)
        return;
    WindowServerConnection::the().send_sync<Messages::WindowServer::SetFullscreen>(m_window_id, fullscreen);
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

void Window::update_all_windows(Badge<WindowServerConnection>)
{
    for (auto* window : *all_windows) {
        window->force_update();
    }
}

void Window::notify_state_changed(Badge<WindowServerConnection>, bool minimized, bool occluded)
{
    m_visible_for_timer_purposes = !minimized && !occluded;

    // When double buffering is enabled, minimization/occlusion means we can mark the front bitmap volatile (in addition to the back bitmap.)
    // When double buffering is disabled, there is only the back bitmap (which we can now mark volatile!)
    RefPtr<Gfx::Bitmap>& bitmap = m_double_buffering_enabled ? m_front_bitmap : m_back_bitmap;
    if (!bitmap)
        return;
    if (minimized || occluded) {
        bitmap->shared_buffer()->set_volatile();
    } else {
        if (!bitmap->shared_buffer()->set_nonvolatile()) {
            bitmap = nullptr;
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

void Window::set_base_size(const Gfx::Size& base_size)
{
    if (m_base_size == base_size)
        return;
    m_base_size = base_size;
    if (m_window_id)
        WindowServerConnection::the().send_sync<Messages::WindowServer::SetWindowBaseSizeAndSizeIncrement>(m_window_id, m_base_size, m_size_increment);
}

void Window::set_size_increment(const Gfx::Size& size_increment)
{
    if (m_size_increment == size_increment)
        return;
    m_size_increment = size_increment;
    if (m_window_id)
        WindowServerConnection::the().send_sync<Messages::WindowServer::SetWindowBaseSizeAndSizeIncrement>(m_window_id, m_base_size, m_size_increment);
}

void Window::did_remove_widget(Badge<Widget>, const Widget& widget)
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

}
