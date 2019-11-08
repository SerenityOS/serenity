#include "WindowList.h"
#include <LibGUI/GWindowServerConnection.h>
#include <WindowServer/WSAPITypes.h>

WindowList& WindowList::the()
{
    static WindowList* s_the;
    if (!s_the)
        s_the = new WindowList;
    return *s_the;
}

Window* WindowList::window(const WindowIdentifier& identifier)
{
    auto it = m_windows.find(identifier);
    if (it != m_windows.end())
        return it->value;
    return nullptr;
}

Window& WindowList::ensure_window(const WindowIdentifier& identifier)
{
    auto it = m_windows.find(identifier);
    if (it != m_windows.end())
        return *it->value;
    auto window = make<Window>(identifier);
    window->set_button(aid_create_button(identifier));
    window->button()->on_click = [window = window.ptr(), identifier](GButton&) {
        WSAPI_ClientMessage message;
        if (window->is_minimized() || !window->is_active()) {
            message.type = WSAPI_ClientMessage::Type::WM_SetActiveWindow;
        } else {
            message.type = WSAPI_ClientMessage::Type::WM_SetWindowMinimized;
            message.wm.minimized = true;
        }
        message.wm.client_id = identifier.client_id();
        message.wm.window_id = identifier.window_id();
        bool success = GWindowServerConnection::the().post_message_to_server(message);
        ASSERT(success);
    };
    auto& window_ref = *window;
    m_windows.set(identifier, move(window));
    return window_ref;
}

void WindowList::remove_window(const WindowIdentifier& identifier)
{
    m_windows.remove(identifier);
}
