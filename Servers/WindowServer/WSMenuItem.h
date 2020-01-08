#pragma once

#include <AK/String.h>
#include <AK/Function.h>
#include <LibDraw/Rect.h>

class GraphicsBitmap;
class WSMenu;

class WSMenuItem {
public:
    enum Type {
        None,
        Text,
        Separator,
    };

    WSMenuItem(WSMenu&, unsigned identifier, const String& text, const String& shortcut_text = {}, bool enabled = true, bool checkable = false, bool checked = false, const GraphicsBitmap* icon = nullptr);
    WSMenuItem(WSMenu&, Type);
    ~WSMenuItem();

    Type type() const { return m_type; }

    bool is_enabled() const { return m_enabled; }
    void set_enabled(bool);

    bool is_checkable() const { return m_checkable; }
    void set_checkable(bool checkable) { m_checkable = checkable; }

    bool is_checked() const { return m_checked; }
    void set_checked(bool);

    String text() const { return m_text; }
    void set_text(const String& text) { m_text = text; }

    String shortcut_text() const { return m_shortcut_text; }
    void set_shortcut_text(const String& text) { m_shortcut_text = text; }

    void set_rect(const Rect& rect) { m_rect = rect; }
    Rect rect() const { return m_rect; }

    unsigned identifier() const { return m_identifier; }

    const GraphicsBitmap* icon() const { return m_icon; }
    void set_icon(const GraphicsBitmap* icon) { m_icon = icon; }

    bool is_submenu() const { return m_submenu_id != -1; }
    int submenu_id() const { return m_submenu_id; }
    void set_submenu_id(int submenu_id) { m_submenu_id = submenu_id; }

    WSMenu* submenu();

    bool is_exclusive() const { return m_exclusive; }
    void set_exclusive(bool exclusive) { m_exclusive = exclusive; }

private:
    WSMenu& m_menu;
    Type m_type { None };
    bool m_enabled { true };
    bool m_checkable { false };
    bool m_checked { false };
    unsigned m_identifier { 0 };
    String m_text;
    String m_shortcut_text;
    Rect m_rect;
    RefPtr<GraphicsBitmap> m_icon;
    int m_submenu_id { -1 };
    bool m_exclusive { false };
};
