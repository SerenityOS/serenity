#include <WindowServer/WSMessageLoop.h>
#include <WindowServer/WSMessage.h>
#include <LibCore/CObject.h>
#include <WindowServer/WSWindowManager.h>
#include <WindowServer/WSScreen.h>
#include <WindowServer/WSClientConnection.h>
#include <WindowServer/WSAPITypes.h>
#include <WindowServer/WSCursor.h>
#include <Kernel/KeyCode.h>
#include <Kernel/MousePacket.h>
#include <sys/socket.h>
#include <sys/select.h>
#include <sys/time.h>
#include <time.h>
#include <unistd.h>
#include <fcntl.h>
#include <stdio.h>
#include <errno.h>

//#define WSMESSAGELOOP_DEBUG

WSMessageLoop::WSMessageLoop()
{
    m_keyboard_fd = open("/dev/keyboard", O_RDONLY | O_NONBLOCK | O_CLOEXEC);
    m_mouse_fd = open("/dev/psaux", O_RDONLY | O_NONBLOCK | O_CLOEXEC);

    unlink("/tmp/wsportal");

    m_server_fd = socket(AF_LOCAL, SOCK_STREAM | SOCK_NONBLOCK | SOCK_CLOEXEC, 0);
    ASSERT(m_server_fd >= 0);
    sockaddr_un address;
    address.sun_family = AF_LOCAL;
    strcpy(address.sun_path, "/tmp/wsportal");
    int rc = bind(m_server_fd, (const sockaddr*)&address, sizeof(address));
    ASSERT(rc == 0);
    rc = listen(m_server_fd, 5);
    ASSERT(rc == 0);

    ASSERT(m_keyboard_fd >= 0);
    ASSERT(m_mouse_fd >= 0);
}

WSMessageLoop::~WSMessageLoop()
{
}

WSMessageLoop& WSMessageLoop::the()
{
    return static_cast<WSMessageLoop&>(CEventLoop::current());
}

void WSMessageLoop::drain_server()
{
    sockaddr_un address;
    socklen_t address_size = sizeof(address);
    int client_fd = accept(m_server_fd, (sockaddr*)&address, &address_size);
    if (client_fd < 0) {
        dbgprintf("WindowServer: accept() failed: %s\n", strerror(errno));
    } else {
        new WSClientConnection(client_fd);
    }
}

void WSMessageLoop::drain_mouse()
{
    auto& screen = WSScreen::the();
    unsigned prev_buttons = screen.mouse_button_state();
    int dx = 0;
    int dy = 0;
    unsigned buttons = prev_buttons;
    for (;;) {
        MousePacket packet;
        ssize_t nread = read(m_mouse_fd, &packet, sizeof(MousePacket));
        if (nread == 0)
            break;
        ASSERT(nread == sizeof(packet));
        buttons = packet.buttons;

        dx += packet.dx;
        dy += -packet.dy;
        if (buttons != prev_buttons) {
            screen.on_receive_mouse_data(dx, dy, buttons);
            dx = 0;
            dy = 0;
            prev_buttons = buttons;
        }
    }
    if (dx || dy)
        screen.on_receive_mouse_data(dx, dy, buttons);
}

void WSMessageLoop::drain_keyboard()
{
    auto& screen = WSScreen::the();
    for (;;) {
        KeyEvent event;
        ssize_t nread = read(m_keyboard_fd, (byte*)&event, sizeof(KeyEvent));
        if (nread == 0)
            break;
        ASSERT(nread == sizeof(KeyEvent));
        screen.on_receive_keyboard_data(event);
    }
}

void WSMessageLoop::notify_client_disconnected(int client_id)
{
    auto* client = WSClientConnection::from_client_id(client_id);
    if (!client)
        return;
    post_event(*client, make<WSClientDisconnectedNotification>(client_id));
}

static WSWindowType from_api(WSAPI_WindowType api_type)
{
    switch (api_type) {
    case WSAPI_WindowType::Normal:
        return WSWindowType::Normal;
    case WSAPI_WindowType::Menu:
        return WSWindowType::Menu;
    case WSAPI_WindowType::WindowSwitcher:
        return WSWindowType::WindowSwitcher;
    case WSAPI_WindowType::Taskbar:
        return WSWindowType::Taskbar;
    case WSAPI_WindowType::Tooltip:
        return WSWindowType::Tooltip;
    default:
        ASSERT_NOT_REACHED();
    }
}

void WSMessageLoop::on_receive_from_client(int client_id, const WSAPI_ClientMessage& message)
{
    WSClientConnection& client = *WSClientConnection::from_client_id(client_id);
    switch (message.type) {
    case WSAPI_ClientMessage::Type::Greeting:
        client.set_client_pid(message.greeting.client_pid);
        break;
    case WSAPI_ClientMessage::Type::CreateMenubar:
        post_event(client, make<WSAPICreateMenubarRequest>(client_id));
        break;
    case WSAPI_ClientMessage::Type::DestroyMenubar:
        post_event(client, make<WSAPIDestroyMenubarRequest>(client_id, message.menu.menubar_id));
        break;
    case WSAPI_ClientMessage::Type::SetApplicationMenubar:
        post_event(client, make<WSAPISetApplicationMenubarRequest>(client_id, message.menu.menubar_id));
        break;
    case WSAPI_ClientMessage::Type::AddMenuToMenubar:
        post_event(client, make<WSAPIAddMenuToMenubarRequest>(client_id, message.menu.menubar_id, message.menu.menu_id));
        break;
    case WSAPI_ClientMessage::Type::CreateMenu:
        ASSERT(message.text_length < (ssize_t)sizeof(message.text));
        post_event(client, make<WSAPICreateMenuRequest>(client_id, String(message.text, message.text_length)));
        break;
    case WSAPI_ClientMessage::Type::PopupMenu:
        post_event(client, make<WSAPIPopupMenuRequest>(client_id, message.menu.menu_id, message.menu.position));
        break;
    case WSAPI_ClientMessage::Type::DismissMenu:
        post_event(client, make<WSAPIDismissMenuRequest>(client_id, message.menu.menu_id));
        break;
    case WSAPI_ClientMessage::Type::SetWindowIcon:
        ASSERT(message.text_length < (ssize_t)sizeof(message.text));
        post_event(client, make<WSAPISetWindowIconRequest>(client_id, message.window_id, String(message.text, message.text_length)));
        break;
    case WSAPI_ClientMessage::Type::DestroyMenu:
        post_event(client, make<WSAPIDestroyMenuRequest>(client_id, message.menu.menu_id));
        break;
    case WSAPI_ClientMessage::Type::AddMenuItem:
        ASSERT(message.text_length < (ssize_t)sizeof(message.text));
        ASSERT(message.menu.shortcut_text_length < (ssize_t)sizeof(message.menu.shortcut_text));
        post_event(client, make<WSAPIAddMenuItemRequest>(client_id, message.menu.menu_id, message.menu.identifier, String(message.text, message.text_length), String(message.menu.shortcut_text, message.menu.shortcut_text_length), message.menu.enabled));
        break;
    case WSAPI_ClientMessage::Type::UpdateMenuItem:
        ASSERT(message.text_length < (ssize_t)sizeof(message.text));
        ASSERT(message.menu.shortcut_text_length < (ssize_t)sizeof(message.menu.shortcut_text));
        post_event(client, make<WSAPIUpdateMenuItemRequest>(client_id, message.menu.menu_id, message.menu.identifier, String(message.text, message.text_length), String(message.menu.shortcut_text, message.menu.shortcut_text_length), message.menu.enabled));
        break;
    case WSAPI_ClientMessage::Type::AddMenuSeparator:
        post_event(client, make<WSAPIAddMenuSeparatorRequest>(client_id, message.menu.menu_id));
        break;
    case WSAPI_ClientMessage::Type::CreateWindow:
        ASSERT(message.text_length < (ssize_t)sizeof(message.text));
        post_event(client, make<WSAPICreateWindowRequest>(client_id, message.window.rect, String(message.text, message.text_length), message.window.has_alpha_channel, message.window.modal, message.window.resizable, message.window.opacity, message.window.base_size, message.window.size_increment, from_api(message.window.type), Color::from_rgba(message.window.background_color)));
        break;
    case WSAPI_ClientMessage::Type::DestroyWindow:
        post_event(client, make<WSAPIDestroyWindowRequest>(client_id, message.window_id));
        break;
    case WSAPI_ClientMessage::Type::SetWindowTitle:
        ASSERT(message.text_length < (ssize_t)sizeof(message.text));
        post_event(client, make<WSAPISetWindowTitleRequest>(client_id, message.window_id, String(message.text, message.text_length)));
        break;
    case WSAPI_ClientMessage::Type::GetWindowTitle:
        ASSERT(message.text_length < (ssize_t)sizeof(message.text));
        post_event(client, make<WSAPIGetWindowTitleRequest>(client_id, message.window_id));
        break;
    case WSAPI_ClientMessage::Type::SetWindowRect:
        post_event(client, make<WSAPISetWindowRectRequest>(client_id, message.window_id, message.window.rect));
        break;
    case WSAPI_ClientMessage::Type::GetWindowRect:
        post_event(client, make<WSAPIGetWindowRectRequest>(client_id, message.window_id));
        break;
    case WSAPI_ClientMessage::Type::SetClipboardContents:
        post_event(client, make<WSAPISetClipboardContentsRequest>(client_id, message.clipboard.shared_buffer_id, message.clipboard.contents_size));
        break;
    case WSAPI_ClientMessage::Type::GetClipboardContents:
        post_event(client, make<WSAPIGetClipboardContentsRequest>(client_id));
        break;
    case WSAPI_ClientMessage::Type::InvalidateRect:
        post_event(client, make<WSAPIInvalidateRectRequest>(client_id, message.window_id, message.window.rect));
        break;
    case WSAPI_ClientMessage::Type::DidFinishPainting:
        post_event(client, make<WSAPIDidFinishPaintingNotification>(client_id, message.window_id, message.window.rect));
        break;
    case WSAPI_ClientMessage::Type::GetWindowBackingStore:
        post_event(client, make<WSAPIGetWindowBackingStoreRequest>(client_id, message.window_id));
        break;
    case WSAPI_ClientMessage::Type::SetWindowBackingStore:
        post_event(client, make<WSAPISetWindowBackingStoreRequest>(client_id, message.window_id, message.backing.shared_buffer_id, message.backing.size, message.backing.bpp, message.backing.pitch, message.backing.has_alpha_channel, message.backing.flush_immediately));
        break;
    case WSAPI_ClientMessage::Type::SetGlobalCursorTracking:
        post_event(client, make<WSAPISetGlobalCursorTrackingRequest>(client_id, message.window_id, message.value));
        break;
    case WSAPI_ClientMessage::Type::SetWallpaper:
        ASSERT(message.text_length < (ssize_t)sizeof(message.text));
        post_event(client, make<WSAPISetWallpaperRequest>(client_id, String(message.text, message.text_length)));
        break;
    case WSAPI_ClientMessage::Type::GetWallpaper:
        post_event(client, make<WSAPIGetWallpaperRequest>(client_id));
        break;
    case WSAPI_ClientMessage::Type::SetWindowOverrideCursor:
        post_event(client, make<WSAPISetWindowOverrideCursorRequest>(client_id, message.window_id, (WSStandardCursor)message.cursor.cursor));
        break;
    case WSAPI_ClientMessage::Type::WM_SetActiveWindow:
        post_event(client, make<WSWMAPISetActiveWindowRequest>(client_id, message.wm.client_id, message.wm.window_id));
        break;
    default:
        break;
    }
}

void WSMessageLoop::add_file_descriptors_for_select(fd_set& fds, int& max_fd_added)
{
    auto add_fd_to_set = [&max_fd_added] (int fd, auto& set) {
        FD_SET(fd, &set);
        if (fd > max_fd_added)
            max_fd_added = fd;
    };
    add_fd_to_set(m_keyboard_fd, fds);
    add_fd_to_set(m_mouse_fd, fds);
    add_fd_to_set(m_server_fd, fds);
    WSClientConnection::for_each_client([&] (WSClientConnection& client) {
        add_fd_to_set(client.fd(), fds);
    });
}

void WSMessageLoop::process_file_descriptors_after_select(const fd_set& fds)
{
    if (FD_ISSET(m_server_fd, &fds))
        drain_server();
    if (FD_ISSET(m_keyboard_fd, &fds))
        drain_keyboard();
    if (FD_ISSET(m_mouse_fd, &fds))
        drain_mouse();
    WSClientConnection::for_each_client([&] (WSClientConnection& client) {
        if (!FD_ISSET(client.fd(), &fds))
            return;
        unsigned messages_received = 0;
        for (;;) {
            WSAPI_ClientMessage message;
            // FIXME: Don't go one message at a time, that's so much context switching, oof.
            ssize_t nread = read(client.fd(), &message, sizeof(WSAPI_ClientMessage));
            if (nread == 0) {
                if (!messages_received)
                    notify_client_disconnected(client.client_id());
                break;
            }
            if (nread < 0) {
                perror("read");
                ASSERT_NOT_REACHED();
            }
            on_receive_from_client(client.client_id(), message);
            ++messages_received;
        }
    });
}
