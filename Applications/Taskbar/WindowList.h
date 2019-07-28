#pragma once

#include "WindowIdentifier.h"
#include <AK/AKString.h>
#include <AK/HashMap.h>
#include <LibGUI/GButton.h>
#include <LibDraw/Rect.h>

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

    const GraphicsBitmap* icon() const { return m_icon.ptr(); }

private:
    WindowIdentifier m_identifier;
    String m_title;
    Rect m_rect;
    GButton* m_button { nullptr };
    RefPtr<GraphicsBitmap> m_icon;
    bool m_active { false };
    bool m_minimized { false };
};

class WindowList {
public:
    static WindowList& the();

    template<typename Callback>
    void for_each_window(Callback callback)
    {
        for (auto& it : m_windows)
            callback(*it.value);
    }

    Window* window(const WindowIdentifier&);
    Window& ensure_window(const WindowIdentifier&);
    void remove_window(const WindowIdentifier&);

    Function<GButton*(const WindowIdentifier&)> aid_create_button;

private:
    HashMap<WindowIdentifier, NonnullOwnPtr<Window>> m_windows;
};
