#pragma once

#include <AK/AKString.h>
#include <AK/Function.h>
#include <SharedGraphics/Rect.h>

class WSMenu;

class WSMenuItem {
public:
    enum Type {
        None,
        Text,
        Separator,
    };

    WSMenuItem(WSMenu&, unsigned identifier, const String& text, const String& shortcut_text = { }, bool enabled = true);
    WSMenuItem(WSMenu&, Type);
    ~WSMenuItem();

    Type type() const { return m_type; }

    bool is_enabled() const { return m_enabled; }
    void set_enabled(bool);

    String text() const { return m_text; }
    void set_text(const String& text) { m_text = text; }

    String shortcut_text() const { return m_shortcut_text; }
    void set_shortcut_text(const String& text) { m_shortcut_text = text; }

    void set_rect(const Rect& rect) { m_rect = rect; }
    Rect rect() const { return m_rect; }

    unsigned identifier() const { return m_identifier; }

private:
    WSMenu& m_menu;
    Type m_type { None };
    bool m_enabled { true };
    unsigned m_identifier { 0 };
    String m_text;
    String m_shortcut_text;
    Rect m_rect;
};

