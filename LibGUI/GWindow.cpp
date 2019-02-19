#include "GWindow.h"
#include "GEvent.h"
#include "GEventLoop.h"
#include "GWidget.h"
#include <SharedGraphics/GraphicsBitmap.h>
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
    // FIXME: If we exit the event loop, we're never gonna deal with the delete_later request!
    //        This will become relevant once we support nested event loops.
    if (should_exit_app_on_close())
        GEventLoop::main().quit(0);
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
    request.window.opacity = m_opacity_when_windowless;
    ASSERT(m_title_when_windowless.length() < sizeof(request.text));
    strcpy(request.text, m_title_when_windowless.characters());
    request.text_length = m_title_when_windowless.length();
    auto response = GEventLoop::main().sync_request(request, WSAPI_ServerMessage::Type::DidCreateWindow);
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
    ASSERT(m_title_when_windowless.length() < sizeof(request.text));
    strcpy(request.text, m_title_when_windowless.characters());
    request.text_length = m_title_when_windowless.length();
    GEventLoop::main().post_message_to_server(request);
}

void GWindow::set_title(String&& title)
{
    m_title_when_windowless = move(title);
    if (!m_window_id)
        return;

    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::SetWindowTitle;
    request.window_id = m_window_id;
    ASSERT(m_title_when_windowless.length() < sizeof(request.text));
    strcpy(request.text, m_title_when_windowless.characters());
    request.text_length = m_title_when_windowless.length();
    GEventLoop::main().post_message_to_server(request);
}

String GWindow::title() const
{
    if (!m_window_id)
        return m_title_when_windowless;

    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::GetWindowTitle;
    request.window_id = m_window_id;
    ASSERT(m_title_when_windowless.length() < sizeof(request.text));
    strcpy(request.text, m_title_when_windowless.characters());
    request.text_length = m_title_when_windowless.length();
    auto response = GEventLoop::main().sync_request(request, WSAPI_ServerMessage::Type::DidGetWindowTitle);
    return String(response.text, response.text_length);
}

Rect GWindow::rect() const
{
    if (!m_window_id)
        return m_rect_when_windowless;

    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::GetWindowRect;
    request.window_id = m_window_id;
    auto response = GEventLoop::main().sync_request(request, WSAPI_ServerMessage::Type::DidGetWindowRect);
    ASSERT(response.window_id == m_window_id);
    return response.window.rect;
}

void GWindow::set_rect(const Rect& a_rect)
{
    m_rect_when_windowless = a_rect;
    if (!m_window_id)
        return;
    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::SetWindowRect;
    request.window_id = m_window_id;
    request.window.rect = a_rect;
    GEventLoop::main().post_message_to_server(request);
}

void GWindow::event(GEvent& event)
{
    if (event.is_mouse_event()) {
        if (m_global_cursor_tracking_widget) {
            // FIXME: This won't work for widgets-within-widgets.
            auto& mouse_event = static_cast<GMouseEvent&>(event);
            Point local_point { mouse_event.x() - m_global_cursor_tracking_widget->relative_rect().x(), mouse_event.y() - m_global_cursor_tracking_widget->relative_rect().y() };
            auto local_event = make<GMouseEvent>(event.type(), local_point, mouse_event.buttons(), mouse_event.button());
            m_global_cursor_tracking_widget->event(*local_event);
        }
        if (!m_main_widget)
            return;
        auto& mouse_event = static_cast<GMouseEvent&>(event);
        if (m_main_widget) {
            auto result = m_main_widget->hit_test(mouse_event.x(), mouse_event.y());
            auto local_event = make<GMouseEvent>(event.type(), Point { result.localX, result.localY }, mouse_event.buttons(), mouse_event.button());
            ASSERT(result.widget);
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
        if (rect.is_empty())
            rect = m_main_widget->rect();
        m_main_widget->event(*make<GPaintEvent>(rect));
        if (m_window_id) {
            WSAPI_ClientMessage message;
            message.type = WSAPI_ClientMessage::Type::DidFinishPainting;
            message.window_id = m_window_id;
            message.window.rect = rect;
            GEventLoop::main().post_message_to_server(message);
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

    GObject::event(event);
}

bool GWindow::is_visible() const
{
    return false;
}

void GWindow::update(const Rect& a_rect)
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
    m_pending_paint_event_rects.append(a_rect);

    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::InvalidateRect;
    request.window_id = m_window_id;
    request.window.rect = a_rect;
    GEventLoop::main().post_message_to_server(request);
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
        GEventLoop::main().post_event(m_focused_widget, make<GEvent>(GEvent::FocusOut));
        m_focused_widget->update();
    }
    m_focused_widget = widget;
    if (m_focused_widget) {
        GEventLoop::main().post_event(m_focused_widget, make<GEvent>(GEvent::FocusIn));
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
    GEventLoop::main().post_message_to_server(request);
}

void GWindow::set_has_alpha_channel(bool value)
{
    ASSERT(!m_window_id);
    m_has_alpha_channel = value;
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
    GEventLoop::main().post_message_to_server(request);
}
