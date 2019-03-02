#pragma once

#include <AK/AKString.h>
#include <AK/Function.h>
#include <SharedGraphics/Rect.h>

class WSMenuItem {
public:
    enum Type {
        None,
        Text,
        Separator,
    };

    explicit WSMenuItem(unsigned identifier, const String& text, const String& shortcut_text = { });
    explicit WSMenuItem(Type);
    ~WSMenuItem();

    Type type() const { return m_type; }
    bool enabled() const { return m_enabled; }

    String text() const { return m_text; }
    String shortcut_text() const { return m_shortcut_text; }

    void set_rect(const Rect& rect) { m_rect = rect; }
    Rect rect() const { return m_rect; }

    unsigned identifier() const { return m_identifier; }

private:
    Type m_type { None };
    bool m_enabled { true };
    unsigned m_identifier { 0 };
    String m_text;
    String m_shortcut_text;
    Rect m_rect;
};

