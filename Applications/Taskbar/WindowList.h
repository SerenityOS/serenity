#pragma once

#include <AK/AKString.h>
#include <AK/HashMap.h>
#include <AK/Traits.h>
#include <SharedGraphics/Rect.h>
#include <LibGUI/GButton.h>

class WindowIdentifier {
public:
    WindowIdentifier(int client_id, int window_id)
        : m_client_id(client_id)
        , m_window_id(window_id)
    {
    }

    int client_id() const { return m_client_id; }
    int window_id() const { return m_window_id; }

    bool operator==(const WindowIdentifier& other) const
    {
        return m_client_id == other.m_client_id && m_window_id == other.m_window_id;
    }
private:
    int m_client_id { -1 };
    int m_window_id { -1 };
};

namespace AK {
template<>
struct Traits<WindowIdentifier> {
    static unsigned hash(const WindowIdentifier& w) { return pair_int_hash(w.client_id(), w.window_id()); }
    static void dump(const WindowIdentifier& w) { kprintf("WindowIdentifier(%d, %d)", w.client_id(), w.window_id()); }
};
}

class Window {
public:
    explicit Window(const WindowIdentifier& identifier)
        : m_identifier(identifier)
    {
    }

    ~Window()
    {
        delete m_button;
    }

    WindowIdentifier identifier() const { return m_identifier; }

    String title() const { return m_title; }
    void set_title(const String& title) { m_title = title; }

    Rect rect() const { return m_rect; }
    void set_rect(const Rect& rect) { m_rect = rect; }

    GButton* button() { return m_button; }
    void set_button(GButton* button) { m_button = button; }

    void set_active(bool active) { m_active = active; }
    bool is_active() const { return m_active; }

    void set_minimized(bool minimized) { m_minimized = minimized; }
    bool is_minimized() const { return m_minimized; }

private:
    WindowIdentifier m_identifier;
    String m_title;
    Rect m_rect;
    GButton* m_button { nullptr };
    bool m_active { false };
    bool m_minimized { false };
};

class WindowList {
public:
    template<typename Callback> void for_each_window(Callback callback)
    {
        for (auto& it : m_windows)
            callback(*it.value);
    }

    Window& ensure_window(const WindowIdentifier&);
    void remove_window(const WindowIdentifier&);

    Function<GButton*()> aid_create_button;

private:
    HashMap<WindowIdentifier, OwnPtr<Window>> m_windows;
};
