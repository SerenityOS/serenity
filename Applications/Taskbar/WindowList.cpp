#include "WindowList.h"
#include <LibGUI/GWindowServerConnection.h>

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
    window->button()->on_click = [window = window.ptr(), identifier](auto&) {
        if (window->is_minimized() || !window->is_active()) {
            GWindowServerConnection::the().post_message(WindowServer::WM_SetActiveWindow(identifier.client_id(), identifier.window_id()));
        } else {
            GWindowServerConnection::the().post_message(WindowServer::WM_SetWindowMinimized(identifier.client_id(), identifier.window_id(), true));
        }
    };
    auto& window_ref = *window;
    m_windows.set(identifier, move(window));
    return window_ref;
}

void WindowList::remove_window(const WindowIdentifier& identifier)
{
    m_windows.remove(identifier);
}
