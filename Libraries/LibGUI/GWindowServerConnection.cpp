#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GClipboard.h>
#include <LibGUI/GDesktop.h>
#include <LibGUI/GEvent.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>
#include <LibGUI/GWindowServerConnection.h>

//#define GEVENTLOOP_DEBUG

GWindowServerConnection& GWindowServerConnection::the()
{
    static GWindowServerConnection* s_connection = nullptr;
    if (!s_connection)
        s_connection = new GWindowServerConnection;
    return *s_connection;
}

void GWindowServerConnection::handshake()
{
    auto response = send_sync<WindowServer::Greet>(getpid());
    set_server_pid(response->server_pid());
    set_my_client_id(response->client_id());
    GDesktop::the().did_receive_screen_rect({}, response->screen_rect());
}

void GWindowServerConnection::handle(const WindowClient::Paint& message)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%d Paint\n", message.window_id());
#endif
    if (auto* window = GWindow::from_window_id(message.window_id())) {
        Vector<Rect, 32> rects;
        for (auto& r : message.rects()) {
            rects.append(r);
        }
        CEventLoop::current().post_event(*window, make<GMultiPaintEvent>(rects, message.window_size()));
    }
}

void GWindowServerConnection::handle(const WindowClient::WindowResized& message)
{
    if (auto* window = GWindow::from_window_id(message.window_id())) {
        CEventLoop::current().post_event(*window, make<GResizeEvent>(message.old_rect().size(), message.new_rect().size()));
    }
}

void GWindowServerConnection::handle(const WindowClient::WindowActivated& message)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("(%d) WID=%d WindowActivated\n", getpid(), message.window_id());
#endif
    if (auto* window = GWindow::from_window_id(message.window_id()))
        CEventLoop::current().post_event(*window, make<GEvent>(GEvent::WindowBecameActive));
}

void GWindowServerConnection::handle(const WindowClient::WindowDeactivated& message)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("(%d) WID=%d WindowDeactivated\n", getpid(), message.window_id());
#endif
    if (auto* window = GWindow::from_window_id(message.window_id()))
        CEventLoop::current().post_event(*window, make<GEvent>(GEvent::WindowBecameInactive));
}

void GWindowServerConnection::handle(const WindowClient::WindowCloseRequest& message)
{
    if (auto* window = GWindow::from_window_id(message.window_id()))
        CEventLoop::current().post_event(*window, make<GEvent>(GEvent::WindowCloseRequest));
}

void GWindowServerConnection::handle(const WindowClient::WindowEntered& message)
{
    if (auto* window = GWindow::from_window_id(message.window_id()))
        CEventLoop::current().post_event(*window, make<GEvent>(GEvent::WindowEntered));
}

void GWindowServerConnection::handle(const WindowClient::WindowLeft& message)
{
    if (auto* window = GWindow::from_window_id(message.window_id()))
        CEventLoop::current().post_event(*window, make<GEvent>(GEvent::WindowLeft));
}

void GWindowServerConnection::handle(const WindowClient::KeyDown& message)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%d KeyDown character=0x%02x\n", message.window_id(), message.character());
#endif
    auto* window = GWindow::from_window_id(message.window_id());
    if (!window)
        return;

    auto key_event = make<GKeyEvent>(GEvent::KeyDown, message.key(), message.modifiers());
    if (message.character() != '\0') {
        char ch = message.character();
        key_event->m_text = String(&ch, 1);
    }

    if (auto* focused_widget = window->focused_widget()) {
        if (auto* action = focused_widget->action_for_key_event(*key_event)) {
            if (action->is_enabled()) {
                action->activate();
                return;
            }
        }
    }

    if (auto* action = GApplication::the().action_for_key_event(*key_event)) {
        if (action->is_enabled()) {
            action->activate();
            return;
        }
    }
    CEventLoop::current().post_event(*window, move(key_event));
}

void GWindowServerConnection::handle(const WindowClient::KeyUp& message)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%d KeyUp character=0x%02x\n", message.window_id(), message.character());
#endif
    auto* window = GWindow::from_window_id(message.window_id());
    if (!window)
        return;

    auto key_event = make<GKeyEvent>(GEvent::KeyUp, message.key(), message.modifiers());
    if (message.character() != '\0') {
        char ch = message.character();
        key_event->m_text = String(&ch, 1);
    }

    CEventLoop::current().post_event(*window, move(key_event));
}

GMouseButton to_gmousebutton(u32 button)
{
    switch (button) {
    case 0:
        return GMouseButton::None;
    case 1:
        return GMouseButton::Left;
    case 2:
        return GMouseButton::Right;
    case 4:
        return GMouseButton::Middle;
    default:
        ASSERT_NOT_REACHED();
        break;
    }
}

void GWindowServerConnection::handle(const WindowClient::MouseDown& message)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%d MouseDown %d,%d,%d\n", message.window_id(), message.mouse_position().x(), message.mouse_position().y(), message.wheel_delta();
#endif

    if (auto* window = GWindow::from_window_id(message.window_id()))
        CEventLoop::current().post_event(*window, make<GMouseEvent>(GEvent::MouseDown, message.mouse_position(), message.buttons(), to_gmousebutton(message.button()), message.modifiers(), message.wheel_delta()));
}

void GWindowServerConnection::handle(const WindowClient::MouseUp& message)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%d MouseUp %d,%d,%d\n", message.window_id(), message.mouse_position().x(), message.mouse_position().y(), message.wheel_delta();
#endif

    if (auto* window = GWindow::from_window_id(message.window_id()))
        CEventLoop::current().post_event(*window, make<GMouseEvent>(GEvent::MouseUp, message.mouse_position(), message.buttons(), to_gmousebutton(message.button()), message.modifiers(), message.wheel_delta()));
}

void GWindowServerConnection::handle(const WindowClient::MouseMove& message)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%d MouseMove %d,%d,%d\n", message.window_id(), message.mouse_position().x(), message.mouse_position().y(), message.wheel_delta();
#endif

    if (auto* window = GWindow::from_window_id(message.window_id()))
        CEventLoop::current().post_event(*window, make<GMouseEvent>(GEvent::MouseMove, message.mouse_position(), message.buttons(), to_gmousebutton(message.button()), message.modifiers(), message.wheel_delta()));
}

void GWindowServerConnection::handle(const WindowClient::MouseDoubleClick& message)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%d MouseDoubleClick %d,%d,%d\n", message.window_id(), message.mouse_position().x(), message.mouse_position().y(), message.wheel_delta();
#endif

    if (auto* window = GWindow::from_window_id(message.window_id()))
        CEventLoop::current().post_event(*window, make<GMouseEvent>(GEvent::MouseDoubleClick, message.mouse_position(), message.buttons(), to_gmousebutton(message.button()), message.modifiers(), message.wheel_delta()));
}

void GWindowServerConnection::handle(const WindowClient::MouseWheel& message)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%d MouseWheel %d,%d,%d\n", message.window_id(), message.mouse_position().x(), message.mouse_position().y(), message.wheel_delta();
#endif

    if (auto* window = GWindow::from_window_id(message.window_id()))
        CEventLoop::current().post_event(*window, make<GMouseEvent>(GEvent::MouseWheel, message.mouse_position(), message.buttons(), to_gmousebutton(message.button()), message.modifiers(), message.wheel_delta()));
}

void GWindowServerConnection::handle(const WindowClient::MenuItemActivated& message)
{
    auto* menu = GMenu::from_menu_id(message.menu_id());
    if (!menu) {
        dbgprintf("GEventLoop received event for invalid menu ID %d\n", message.menu_id());
        return;
    }
    if (auto* action = menu->action_at(message.identifier()))
        action->activate();
}

void GWindowServerConnection::handle(const WindowClient::WM_WindowStateChanged& message)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("GEventLoop: handle_wm_event: %d\n", (int)event.type);
#endif
    if (auto* window = GWindow::from_window_id(message.window_id()))
        CEventLoop::current().post_event(*window, make<GWMWindowStateChangedEvent>(message.client_id(), message.window_id(), message.title(), message.rect(), message.is_active(), (GWindowType)message.window_type(), message.is_minimized()));
}

void GWindowServerConnection::handle(const WindowClient::WM_WindowRectChanged& message)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("GEventLoop: handle_wm_event: %d\n", (int)event.type);
#endif
    if (auto* window = GWindow::from_window_id(message.window_id()))
        CEventLoop::current().post_event(*window, make<GWMWindowRectChangedEvent>(message.client_id(), message.window_id(), message.rect()));
}

void GWindowServerConnection::handle(const WindowClient::WM_WindowIconBitmapChanged& message)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("GEventLoop: handle_wm_event: %d\n", (int)event.type);
#endif
    if (auto* window = GWindow::from_window_id(message.window_id()))
        CEventLoop::current().post_event(*window, make<GWMWindowIconBitmapChangedEvent>(message.client_id(), message.window_id(), message.icon_buffer_id(), message.icon_size()));
}

void GWindowServerConnection::handle(const WindowClient::WM_WindowRemoved& message)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("GEventLoop: handle_wm_event: %d\n", (int)event.type);
#endif
    if (auto* window = GWindow::from_window_id(message.window_id()))
        CEventLoop::current().post_event(*window, make<GWMWindowRemovedEvent>(message.client_id(), message.window_id()));
}

void GWindowServerConnection::handle(const WindowClient::ScreenRectChanged& message)
{
    GDesktop::the().did_receive_screen_rect({}, message.rect());
}

void GWindowServerConnection::handle(const WindowClient::ClipboardContentsChanged& message)
{
    GClipboard::the().did_receive_clipboard_contents_changed({}, message.content_type());
}

void GWindowServerConnection::handle(const WindowClient::AsyncSetWallpaperFinished&)
{
    // This is handled manually by GDesktop::set_wallpaper().
}
