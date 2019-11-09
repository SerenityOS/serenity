#include <LibC/errno.h>
#include <LibC/fcntl.h>
#include <LibC/stdio.h>
#include <LibC/stdlib.h>
#include <LibC/string.h>
#include <LibC/sys/select.h>
#include <LibC/sys/socket.h>
#include <LibC/sys/time.h>
#include <LibC/time.h>
#include <LibC/unistd.h>
#include <LibCore/CNotifier.h>
#include <LibCore/CObject.h>
#include <LibGUI/GAction.h>
#include <LibGUI/GApplication.h>
#include <LibGUI/GClipboard.h>
#include <LibGUI/GDesktop.h>
#include <LibGUI/GEvent.h>
#include <LibGUI/GMenu.h>
#include <LibGUI/GWidget.h>
#include <LibGUI/GWindow.h>
#include <LibGUI/GWindowServerConnection.h>
#include <sys/uio.h>

//#define GEVENTLOOP_DEBUG
//#define COALESCING_DEBUG

GWindowServerConnection& GWindowServerConnection::the()
{
    static GWindowServerConnection* s_connection = nullptr;
    if (!s_connection) {
        s_connection = new GWindowServerConnection();
        s_connection->handshake();
    }
    return *s_connection;
}

void GWindowServerConnection::handshake()
{
    WSAPI_ClientMessage request;
    request.type = WSAPI_ClientMessage::Type::Greeting;
    request.greeting.client_pid = getpid();
    auto response = sync_request(request, WSAPI_ServerMessage::Type::Greeting);
    handle_greeting(response);
}

void GWindowServerConnection::handle_paint_event(const WSAPI_ServerMessage& event, GWindow& window, const ByteBuffer& extra_data)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%x Paint\n", event.window_id);
#endif
    Vector<Rect, 32> rects;
    for (int i = 0; i < min(WSAPI_ServerMessage::max_inline_rect_count, event.rect_count); ++i)
        rects.append(event.rects[i]);
    if (event.extra_size) {
        auto* extra_rects = reinterpret_cast<const WSAPI_Rect*>(extra_data.data());
        for (int i = 0; i < event.rect_count - WSAPI_ServerMessage::max_inline_rect_count; ++i)
            rects.append(extra_rects[i]);
    }
    CEventLoop::current().post_event(window, make<GMultiPaintEvent>(rects, event.paint.window_size));
}

void GWindowServerConnection::handle_resize_event(const WSAPI_ServerMessage& event, GWindow& window)
{
    CEventLoop::current().post_event(window, make<GResizeEvent>(event.window.old_rect.size, event.window.rect.size));
}

void GWindowServerConnection::handle_window_activation_event(const WSAPI_ServerMessage& event, GWindow& window)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%x WindowActivation\n", event.window_id);
#endif
    CEventLoop::current().post_event(window, make<GEvent>(event.type == WSAPI_ServerMessage::Type::WindowActivated ? GEvent::WindowBecameActive : GEvent::WindowBecameInactive));
}

void GWindowServerConnection::handle_window_close_request_event(const WSAPI_ServerMessage&, GWindow& window)
{
    CEventLoop::current().post_event(window, make<GEvent>(GEvent::WindowCloseRequest));
}

void GWindowServerConnection::handle_window_entered_or_left_event(const WSAPI_ServerMessage& message, GWindow& window)
{
    CEventLoop::current().post_event(window, make<GEvent>(message.type == WSAPI_ServerMessage::Type::WindowEntered ? GEvent::WindowEntered : GEvent::WindowLeft));
}

void GWindowServerConnection::handle_key_event(const WSAPI_ServerMessage& event, GWindow& window)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%x KeyEvent character=0x%b\n", event.window_id, event.key.character);
#endif
    auto key_event = make<GKeyEvent>(event.type == WSAPI_ServerMessage::Type::KeyDown ? GEvent::KeyDown : GEvent::KeyUp, event.key.key, event.key.modifiers);
    if (event.key.character != '\0')
        key_event->m_text = String(&event.key.character, 1);

    if (event.type == WSAPI_ServerMessage::Type::KeyDown) {
        if (auto* focused_widget = window.focused_widget()) {
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
    }
    CEventLoop::current().post_event(window, move(key_event));
}

void GWindowServerConnection::handle_mouse_event(const WSAPI_ServerMessage& event, GWindow& window)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("WID=%x MouseEvent %d,%d,%d\n", event.window_id, event.mouse.position.x, event.mouse.position.y, event.mouse.wheel_delta);
#endif
    GMouseEvent::Type type;
    switch (event.type) {
    case WSAPI_ServerMessage::Type::MouseMove:
        type = GEvent::MouseMove;
        break;
    case WSAPI_ServerMessage::Type::MouseUp:
        type = GEvent::MouseUp;
        break;
    case WSAPI_ServerMessage::Type::MouseDown:
        type = GEvent::MouseDown;
        break;
    case WSAPI_ServerMessage::Type::MouseDoubleClick:
        type = GEvent::MouseDoubleClick;
        break;
    case WSAPI_ServerMessage::Type::MouseWheel:
        type = GEvent::MouseWheel;
        break;
    default:
        ASSERT_NOT_REACHED();
        break;
    }
    GMouseButton button { GMouseButton::None };
    switch (event.mouse.button) {
    case WSAPI_MouseButton::NoButton:
        button = GMouseButton::None;
        break;
    case WSAPI_MouseButton::Left:
        button = GMouseButton::Left;
        break;
    case WSAPI_MouseButton::Right:
        button = GMouseButton::Right;
        break;
    case WSAPI_MouseButton::Middle:
        button = GMouseButton::Middle;
        break;
    default:
        ASSERT_NOT_REACHED();
        break;
    }
    CEventLoop::current().post_event(window, make<GMouseEvent>(type, event.mouse.position, event.mouse.buttons, button, event.mouse.modifiers, event.mouse.wheel_delta));
}

void GWindowServerConnection::handle_menu_event(const WSAPI_ServerMessage& event)
{
    if (event.type == WSAPI_ServerMessage::Type::MenuItemActivated) {
        auto* menu = GMenu::from_menu_id(event.menu.menu_id);
        if (!menu) {
            dbgprintf("GEventLoop received event for invalid window ID %d\n", event.window_id);
            return;
        }
        if (auto* action = menu->action_at(event.menu.identifier))
            action->activate();
        return;
    }
    ASSERT_NOT_REACHED();
}

void GWindowServerConnection::handle_wm_event(const WSAPI_ServerMessage& event, GWindow& window)
{
#ifdef GEVENTLOOP_DEBUG
    dbgprintf("GEventLoop: handle_wm_event: %d\n", (int)event.type);
#endif
    if (event.type == WSAPI_ServerMessage::WM_WindowStateChanged)
        CEventLoop::current().post_event(window, make<GWMWindowStateChangedEvent>(event.wm.client_id, event.wm.window_id, String(event.text, event.text_length), event.wm.rect, event.wm.is_active, (GWindowType)event.wm.window_type, event.wm.is_minimized));
    else if (event.type == WSAPI_ServerMessage::WM_WindowRectChanged)
        CEventLoop::current().post_event(window, make<GWMWindowRectChangedEvent>(event.wm.client_id, event.wm.window_id, event.wm.rect));
    else if (event.type == WSAPI_ServerMessage::WM_WindowIconBitmapChanged)
        CEventLoop::current().post_event(window, make<GWMWindowIconBitmapChangedEvent>(event.wm.client_id, event.wm.window_id, event.wm.icon_buffer_id, event.wm.icon_size));
    else if (event.type == WSAPI_ServerMessage::WM_WindowRemoved)
        CEventLoop::current().post_event(window, make<GWMWindowRemovedEvent>(event.wm.client_id, event.wm.window_id));
    else
        ASSERT_NOT_REACHED();
}

void GWindowServerConnection::postprocess_bundles(Vector<IncomingMessageBundle>& bundles)
{
    int coalesced_paints = 0;
    int coalesced_resizes = 0;
    auto unprocessed_bundles = move(bundles);

    HashMap<int, Size> latest_size_for_window_id;
    for (auto& bundle : unprocessed_bundles) {
        auto& event = bundle.message;
        if (event.type == WSAPI_ServerMessage::Type::WindowResized) {
            latest_size_for_window_id.set(event.window_id, event.window.rect.size);
        }
    }

    int paint_count = 0;
    HashMap<int, Size> latest_paint_size_for_window_id;
    for (auto& bundle : unprocessed_bundles) {
        auto& event = bundle.message;
        if (event.type == WSAPI_ServerMessage::Type::Paint) {
            ++paint_count;
#ifdef COALESCING_DEBUG
            dbgprintf("    (window: %s)\n", Size(event.paint.window_size).to_string().characters());
#endif
            latest_paint_size_for_window_id.set(event.window_id, event.paint.window_size);
        }
    }
#ifdef COALESCING_DEBUG
    dbgprintf("paint_count: %d\n", paint_count);
#endif

    for (auto& bundle : unprocessed_bundles) {
        auto& event = bundle.message;
        if (event.type == WSAPI_ServerMessage::Type::Greeting) {
            // Shouldn't get a second greeting
            dbg() << "Got second Greeting!?";
            ASSERT_NOT_REACHED();
            continue;
        }

        if (event.type == WSAPI_ServerMessage::Type::ScreenRectChanged) {
            GDesktop::the().did_receive_screen_rect({}, event.screen.rect);
            continue;
        }

        if (event.type == WSAPI_ServerMessage::Type::ClipboardContentsChanged) {
            GClipboard::the().did_receive_clipboard_contents_changed({}, String(event.text, event.text_length));
            continue;
        }

        if (event.type == WSAPI_ServerMessage::Error) {
            dbgprintf("GEventLoop got error message from server\n");
            dbgprintf("  - error message: %s\n", String(event.text, event.text_length).characters());
            CEventLoop::current().quit(1);
            return;
        }

        switch (event.type) {
        case WSAPI_ServerMessage::MenuItemActivated:
            handle_menu_event(event);
            continue;
        default:
            break;
        }

        auto* window = GWindow::from_window_id(event.window_id);
        if (!window) {
            dbgprintf("GEventLoop received event for invalid window ID %d\n", event.window_id);
            continue;
        }
        switch (event.type) {
        case WSAPI_ServerMessage::Type::Paint:
            if (Size(event.paint.window_size) != latest_paint_size_for_window_id.get(event.window_id).value_or({})) {
                ++coalesced_paints;
                break;
            }
            handle_paint_event(event, *window, bundle.extra_data);
            break;
        case WSAPI_ServerMessage::Type::MouseDown:
        case WSAPI_ServerMessage::Type::MouseDoubleClick:
        case WSAPI_ServerMessage::Type::MouseUp:
        case WSAPI_ServerMessage::Type::MouseMove:
        case WSAPI_ServerMessage::Type::MouseWheel:
            handle_mouse_event(event, *window);
            break;
        case WSAPI_ServerMessage::Type::WindowActivated:
        case WSAPI_ServerMessage::Type::WindowDeactivated:
            handle_window_activation_event(event, *window);
            break;
        case WSAPI_ServerMessage::Type::WindowCloseRequest:
            handle_window_close_request_event(event, *window);
            break;
        case WSAPI_ServerMessage::Type::KeyDown:
        case WSAPI_ServerMessage::Type::KeyUp:
            handle_key_event(event, *window);
            break;
        case WSAPI_ServerMessage::Type::WindowEntered:
        case WSAPI_ServerMessage::Type::WindowLeft:
            handle_window_entered_or_left_event(event, *window);
            break;
        case WSAPI_ServerMessage::Type::WindowResized:
            if (Size(event.window.rect.size) != latest_size_for_window_id.get(event.window_id).value_or({})) {
                ++coalesced_resizes;
                break;
            }
            handle_resize_event(event, *window);
            break;
        default:
            if (event.type > WSAPI_ServerMessage::__Begin_WM_Events__ && event.type < WSAPI_ServerMessage::Type::__End_WM_Events__)
                handle_wm_event(event, *window);
            break;
        }
    }

#ifdef COALESCING_DEBUG
    if (coalesced_paints)
        dbgprintf("Coalesced %d paints\n", coalesced_paints);
    if (coalesced_resizes)
        dbgprintf("Coalesced %d resizes\n", coalesced_resizes);
#endif
}

void GWindowServerConnection::handle_greeting(WSAPI_ServerMessage& message)
{
    set_server_pid(message.greeting.server_pid);
    set_my_client_id(message.greeting.your_client_id);
    GDesktop::the().did_receive_screen_rect({}, message.greeting.screen_rect);
}
