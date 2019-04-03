#include "WindowList.h"

Window& WindowList::ensure_window(const WindowIdentifier& identifier)
{
    auto it = m_windows.find(identifier);
    if (it != m_windows.end())
        return *it->value;
    auto window = make<Window>(identifier);
    auto& window_ref = *window;
    m_windows.set(identifier, move(window));
    return window_ref;
}

void WindowList::remove_window(const WindowIdentifier& identifier)
{
    m_windows.remove(identifier);
}
