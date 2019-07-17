#include <Kernel/KeyCode.h>
#include <Kernel/MousePacket.h>
#include <LibCore/CObject.h>
#include <WindowServer/WSAPITypes.h>
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
{
    m_keyboard_fd = open("/dev/keyboard", O_RDONLY | O_NONBLOCK | O_CLOEXEC);
    m_mouse_fd = open("/dev/psaux", O_RDONLY | O_NONBLOCK | O_CLOEXEC);

    unlink("/tmp/wsportal");

    sockaddr_un address;
    address.sun_family = AF_LOCAL;
    strcpy(address.sun_path, "/tmp/wsportal");
    int rc = bind(m_server_sock.fd(), (const sockaddr*)&address, sizeof(address));
    ASSERT(rc == 0);
    rc = listen(m_server_sock.fd(), 5);
    ASSERT(rc == 0);

    ASSERT(m_server_sock.fd() >= 0);
    ASSERT(m_keyboard_fd >= 0);
    ASSERT(m_mouse_fd >= 0);

    m_server_notifier = make<CNotifier>(m_server_sock.fd(), CNotifier::Read);
    m_server_notifier->on_ready_to_read = [this] { drain_server(); };

    m_keyboard_notifier = make<CNotifier>(m_keyboard_fd, CNotifier::Read);
    m_keyboard_notifier->on_ready_to_read = [this] { drain_keyboard(); };

    m_mouse_notifier = make<CNotifier>(m_mouse_fd, CNotifier::Read);
    m_mouse_notifier->on_ready_to_read = [this] { drain_mouse(); };
}

WSEventLoop::~WSEventLoop()
{
}

void WSEventLoop::drain_server()
{
    sockaddr_un address;
    socklen_t address_size = sizeof(address);
    int client_fd = accept(m_server_sock.fd(), (sockaddr*)&address, &address_size);
    if (client_fd < 0) {
        dbgprintf("WindowServer: accept() failed: %s\n", strerror(errno));
    } else {
        static int s_next_client_id = 0;
        int client_id = ++s_next_client_id;
        IPC::Server::new_connection_for_client<WSClientConnection>(client_fd, client_id);
    }
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

