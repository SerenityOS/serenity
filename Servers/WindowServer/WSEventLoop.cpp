#include "WSClipboard.h"
#include <Kernel/KeyCode.h>
#include <Kernel/MousePacket.h>
#include <LibCore/CLocalSocket.h>
#include <LibCore/CObject.h>
#include <WindowServer/WSClientConnection.h>
#include <WindowServer/WSCursor.h>
#include <WindowServer/WSEvent.h>
#include <WindowServer/WSEventLoop.h>
#include <WindowServer/WSScreen.h>
#include <WindowServer/WSWindowManager.h>
#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <sys/select.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <time.h>
#include <unistd.h>

//#define WSMESSAGELOOP_DEBUG

WSEventLoop::WSEventLoop()
    : m_server(CLocalServer::construct())
{
    m_keyboard_fd = open("/dev/keyboard", O_RDONLY | O_NONBLOCK | O_CLOEXEC);
    m_mouse_fd = open("/dev/psaux", O_RDONLY | O_NONBLOCK | O_CLOEXEC);

    bool ok = m_server->take_over_from_system_server();
    ASSERT(ok);

    m_server->on_ready_to_accept = [this] {
        auto client_socket = m_server->accept();
        if (!client_socket) {
            dbg() << "WindowServer: accept failed.";
            return;
        }
        static int s_next_client_id = 0;
        int client_id = ++s_next_client_id;
        IPC::Server::new_connection_ng_for_client<WSClientConnection>(*client_socket, client_id);
    };

    ASSERT(m_keyboard_fd >= 0);
    ASSERT(m_mouse_fd >= 0);

    m_keyboard_notifier = CNotifier::construct(m_keyboard_fd, CNotifier::Read);
    m_keyboard_notifier->on_ready_to_read = [this] { drain_keyboard(); };

    m_mouse_notifier = CNotifier::construct(m_mouse_fd, CNotifier::Read);
    m_mouse_notifier->on_ready_to_read = [this] { drain_mouse(); };

    WSClipboard::the().on_content_change = [&] {
        WSClientConnection::for_each_client([&](auto& client) {
            client.notify_about_clipboard_contents_changed();
        });
    };
}

WSEventLoop::~WSEventLoop()
{
}

void WSEventLoop::drain_mouse()
{
    auto& screen = WSScreen::the();
    unsigned prev_buttons = screen.mouse_button_state();
    int dx = 0;
    int dy = 0;
    int dz = 0;
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
        dz += packet.dz;
        if (buttons != prev_buttons) {
            screen.on_receive_mouse_data(dx, dy, dz, buttons);
            dx = 0;
            dy = 0;
            dz = 0;
            prev_buttons = buttons;
        }
    }
    if (dx || dy || dz)
        screen.on_receive_mouse_data(dx, dy, dz, buttons);
}

void WSEventLoop::drain_keyboard()
{
    auto& screen = WSScreen::the();
    for (;;) {
        KeyEvent event;
        ssize_t nread = read(m_keyboard_fd, (u8*)&event, sizeof(KeyEvent));
        if (nread == 0)
            break;
        ASSERT(nread == sizeof(KeyEvent));
        screen.on_receive_keyboard_data(event);
    }
}
