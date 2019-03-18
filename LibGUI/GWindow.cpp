#include "GWindow.h"
#include "GEvent.h"
#include "GEventLoop.h"
#include "GWidget.h"
#include <SharedGraphics/GraphicsBitmap.h>
#include <SharedGraphics/Painter.h>
#include <LibC/stdio.h>
#include <LibC/stdlib.h>
#include <LibC/unistd.h>
#include <AK/HashMap.h>

//#define UPDATE_COALESCING_DEBUG

static HashMap<int, GWindow*>* s_windows;

static HashMap<int, GWindow*>& windows()
{
    if (!s_windows)
        s_windows = new HashMap<int, GWindow*>;
    return *s_windows;
}

GWindow* GWindow::from_window_id(int window_id)
{
    auto it = windows().find(window_id);
    if (it != windows().end())
        return (*it).value;
    return nullptr;
}

GWindow::GWindow(GObject* parent)
    : GObject(parent)
{
    m_rect_when_windowless = { 100, 400, 140, 140 };
    m_title_when_windowless = "GWindow";
}

GWindow::~GWindow()
{
    if (m_main_widget)
        delete m_main_widget;
    hide();
}

void GWindow::close()
{
    if (should_exit_event_loop_on_close())
        GEventLoop::current().quit(0);
    delete_later();
}

void GWindow::show()
{
    if (m_window_id)
        return;

    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::CreateWindow;
    request.window_id = m_window_id;
    request.window.rect = m_rect_when_windowless;
    request.window.has_alpha_channel = m_has_alpha_channel;
    request.window.modal = m_modal;
    request.window.resizable = m_resizable;
    request.window.opacity = m_opacity_when_windowless;
    request.window.size_increment = m_size_increment;
    request.window.base_size = m_base_size;
    ASSERT(m_title_when_windowless.length() < (ssize_t)sizeof(request.text));
    strcpy(request.text, m_title_when_windowless.characters());
    request.text_length = m_title_when_windowless.length();
    auto response = GEventLoop::current().sync_request(request, WSAPI_ServerMessage::Type::DidCreateWindow);
    m_window_id = response.window_id;

    windows().set(m_window_id, this);
    update();
}

void GWindow::hide()
{
    if (!m_window_id)
        return;
    windows().remove(m_window_id);
    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::DestroyWindow;
    request.window_id = m_window_id;
    GEventLoop::current().post_message_to_server(request);
}

void GWindow::set_title(const String& title)
{
    m_title_when_windowless = title;
    if (!m_window_id)
        return;

    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::SetWindowTitle;
    request.window_id = m_window_id;
    ASSERT(m_title_when_windowless.length() < (ssize_t)sizeof(request.text));
    strcpy(request.text, m_title_when_windowless.characters());
    request.text_length = m_title_when_windowless.length();
    GEventLoop::current().post_message_to_server(request);
}

String GWindow::title() const
{
    if (!m_window_id)
        return m_title_when_windowless;

    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::GetWindowTitle;
    request.window_id = m_window_id;
    auto response = GEventLoop::current().sync_request(request, WSAPI_ServerMessage::Type::DidGetWindowTitle);
    return String(response.text, response.text_length);
}

Rect GWindow::rect() const
{
    if (!m_window_id)
        return m_rect_when_windowless;

    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::GetWindowRect;
    request.window_id = m_window_id;
    auto response = GEventLoop::current().sync_request(request, WSAPI_ServerMessage::Type::DidGetWindowRect);
    ASSERT(response.window_id == m_window_id);
    return response.window.rect;
}

void GWindow::set_rect(const Rect& a_rect)
{
    m_rect_when_windowless = a_rect;
    if (!m_window_id) {
        if (m_main_widget)
            m_main_widget->resize(m_rect_when_windowless.size());
        return;
    }
    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::SetWindowRect;
    request.window_id = m_window_id;
    request.window.rect = a_rect;
    GEventLoop::current().post_message_to_server(request);
}

void GWindow::event(GEvent& event)
{
    if (event.is_mouse_event()) {
        if (m_global_cursor_tracking_widget) {
            auto& mouse_event = static_cast<GMouseEvent&>(event);
            auto window_relative_rect = m_global_cursor_tracking_widget->window_relative_rect();
            Point local_point { mouse_event.x() - window_relative_rect.x(), mouse_event.y() - window_relative_rect.y() };
            auto local_event = make<GMouseEvent>(event.type(), local_point, mouse_event.buttons(), mouse_event.button(), mouse_event.modifiers());
            m_global_cursor_tracking_widget->event(*local_event);
        }
        if (!m_main_widget)
            return;
        auto& mouse_event = static_cast<GMouseEvent&>(event);
        if (m_main_widget) {
            auto result = m_main_widget->hit_test(mouse_event.x(), mouse_event.y());
            auto local_event = make<GMouseEvent>(event.type(), Point { result.localX, result.localY }, mouse_event.buttons(), mouse_event.button(), mouse_event.modifiers());
            ASSERT(result.widget);
            set_hovered_widget(result.widget);
            if (result.widget != m_global_cursor_tracking_widget.ptr())
                return result.widget->event(*local_event);
        }
        return;
    }

    if (event.is_paint_event()) {
        m_pending_paint_event_rects.clear();
        if (!m_main_widget)
            return;
        auto& paint_event = static_cast<GPaintEvent&>(event);
        auto rect = paint_event.rect();
        bool created_new_backing_store = !m_back_bitmap;
        if (!m_back_bitmap)
            m_back_bitmap = create_backing_bitmap(paint_event.window_size());
        if (rect.is_empty() || created_new_backing_store)
            rect = m_main_widget->rect();

        m_main_widget->event(*make<GPaintEvent>(rect));

        if (m_double_buffering_enabled)
            flip(rect);
        else if (created_new_backing_store)
            set_current_backing_bitmap(*m_back_bitmap, true);

        if (m_window_id) {
            WSAPI_ClientMessage message;
            message.type = WSAPI_ClientMessage::Type::DidFinishPainting;
            message.window_id = m_window_id;
            message.window.rect = rect;
            GEventLoop::current().post_message_to_server(message);
        }
        return;
    }

    if (event.is_key_event()) {
        if (m_focused_widget)
            return m_focused_widget->event(event);
        if (m_main_widget)
            return m_main_widget->event(event);
        return;
    }

    if (event.type() == GEvent::WindowBecameActive || event.type() == GEvent::WindowBecameInactive) {
        m_is_active = event.type() == GEvent::WindowBecameActive;
        if (m_main_widget)
            m_main_widget->event(event);
        if (m_focused_widget)
            m_focused_widget->update();
        return;
    }

    if (event.type() == GEvent::WindowCloseRequest) {
        close();
        return;
    }

    if (event.type() == GEvent::WindowLeft) {
        set_hovered_widget(nullptr);
        return;
    }

    if (event.type() == GEvent::Resize) {
        auto new_size = static_cast<GResizeEvent&>(event).size();
        if (m_back_bitmap && m_back_bitmap->size() != new_size)
            m_back_bitmap = nullptr;
        m_pending_paint_event_rects.clear();
        m_rect_when_windowless = { { }, new_size };
        m_main_widget->set_relative_rect({ { }, new_size });
        return;
    }

    GObject::event(event);
}

bool GWindow::is_visible() const
{
    return false;
}

void GWindow::update(const Rect& a_rect)
{
    if (a_rect.is_empty())
        return;
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
    m_pending_paint_event_rects.append(a_rect);

    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::InvalidateRect;
    request.window_id = m_window_id;
    request.window.rect = a_rect;
    GEventLoop::current().post_message_to_server(request);
}

void GWindow::set_main_widget(GWidget* widget)
{
    if (m_main_widget == widget)
        return;
    m_main_widget = widget;
    if (m_main_widget) {
        auto new_window_rect = rect();
        if (m_main_widget->horizontal_size_policy() == SizePolicy::Fixed)
            new_window_rect.set_width(m_main_widget->preferred_size().width());
        if (m_main_widget->vertical_size_policy() == SizePolicy::Fixed)
            new_window_rect.set_height(m_main_widget->preferred_size().height());
        set_rect(new_window_rect);
        m_main_widget->set_relative_rect({ { }, new_window_rect.size() });
        m_main_widget->set_window(this);
        if (m_main_widget->accepts_focus())
            m_main_widget->set_focus(true);
    }
    update();
}

void GWindow::set_focused_widget(GWidget* widget)
{
    if (m_focused_widget == widget)
        return;
    if (m_focused_widget) {
        GEventLoop::current().post_event(*m_focused_widget, make<GEvent>(GEvent::FocusOut));
        m_focused_widget->update();
    }
    m_focused_widget = widget;
    if (m_focused_widget) {
        GEventLoop::current().post_event(*m_focused_widget, make<GEvent>(GEvent::FocusIn));
        m_focused_widget->update();
    }
}

void GWindow::set_global_cursor_tracking_widget(GWidget* widget)
{
    ASSERT(m_window_id);
    if (widget == m_global_cursor_tracking_widget.ptr())
        return;
    m_global_cursor_tracking_widget = widget ? widget->make_weak_ptr() : nullptr;

    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::SetGlobalCursorTracking;
    request.window_id = m_window_id;
    request.value = widget != nullptr;
    // FIXME: What if the cursor moves out of our interest range before the server can handle this?
    //        Maybe there could be a response that includes the current cursor location as of enabling.
    GEventLoop::current().post_message_to_server(request);
}

void GWindow::set_has_alpha_channel(bool value)
{
    ASSERT(!m_window_id);
    m_has_alpha_channel = value;
}

void GWindow::set_double_buffering_enabled(bool value)
{
    ASSERT(!m_window_id);
    m_double_buffering_enabled = value;
}

void GWindow::set_opacity(float opacity)
{
    m_opacity_when_windowless = opacity;
    if (!m_window_id)
        return;
    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::SetWindowOpacity;
    request.window_id = m_window_id;
    request.window.opacity = opacity;
    m_opacity_when_windowless = opacity;
    GEventLoop::current().post_message_to_server(request);
}

void GWindow::set_hovered_widget(GWidget* widget)
{
    if (widget == m_hovered_widget.ptr())
        return;

    if (m_hovered_widget)
        GEventLoop::current().post_event(*m_hovered_widget, make<GEvent>(GEvent::Leave));

    m_hovered_widget = widget ? widget->make_weak_ptr() : nullptr;

    if (m_hovered_widget)
        GEventLoop::current().post_event(*m_hovered_widget, make<GEvent>(GEvent::Enter));
}

void GWindow::set_current_backing_bitmap(GraphicsBitmap& bitmap, bool flush_immediately)
{
    WSAPI_ClientMessage message;
    message.type = WSAPI_ClientMessage::Type::SetWindowBackingStore;
    message.window_id = m_window_id;
    message.backing.bpp = 32;
    message.backing.pitch = bitmap.pitch();
    message.backing.shared_buffer_id = bitmap.shared_buffer_id();
    message.backing.has_alpha_channel = bitmap.has_alpha_channel();
    message.backing.size = bitmap.size();
    message.backing.flush_immediately = flush_immediately;
    GEventLoop::current().sync_request(message, WSAPI_ServerMessage::Type::DidSetWindowBackingStore);
}

void GWindow::flip(const Rect& dirty_rect)
{
    swap(m_front_bitmap, m_back_bitmap);

    set_current_backing_bitmap(*m_front_bitmap);

    if (!m_back_bitmap || m_back_bitmap->size() != m_front_bitmap->size()) {
        m_back_bitmap = create_backing_bitmap(m_front_bitmap->size());
        memcpy(m_back_bitmap->scanline(0), m_front_bitmap->scanline(0), m_front_bitmap->size().area() * sizeof(RGBA32));
        return;
    }

    // Copy whatever was painted from the front to the back.
    Painter painter(*m_back_bitmap);
    painter.blit(dirty_rect.location(), *m_front_bitmap, dirty_rect);
}

Retained<GraphicsBitmap> GWindow::create_backing_bitmap(const Size& size)
{
    ASSERT(GEventLoop::server_pid());
    ASSERT(!size.is_empty());
    size_t size_in_bytes = size.area() * sizeof(RGBA32);
    auto shared_buffer = SharedBuffer::create(GEventLoop::server_pid(), size_in_bytes);
    ASSERT(shared_buffer);
    auto format = m_has_alpha_channel ? GraphicsBitmap::Format::RGBA32 : GraphicsBitmap::Format::RGB32;
    return GraphicsBitmap::create_with_shared_buffer(format, *shared_buffer, size);
}

void GWindow::set_modal(bool modal)
{
    ASSERT(!m_window_id);
    m_modal = modal;
}
