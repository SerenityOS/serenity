#pragma once

#include <AK/AKString.h>
#include <AK/Vector.h>
#include <SharedGraphics/Rect.h>
#include "WSMenuItem.h"

class WSMenuBar;
class WSMessage;
class WSWindow;
class Font;

class WSMenu {
public:
    WSMenu(const String& name);
    ~WSMenu();

    WSMenuBar* menu_bar() { return m_menubar; }
    const WSMenuBar* menu_bar() const { return m_menubar; }

    int item_count() const { return m_items.size(); }
    WSMenuItem* item(int i) { return m_items[i].ptr(); }
    const WSMenuItem* item(int i) const { return m_items[i].ptr(); }

    void add_item(OwnPtr<WSMenuItem>&& item) { m_items.append(move(item)); }

    String name() const { return m_name; }

    template<typename Callback>
    void for_each_item(Callback callback) const
    {
        for (auto& item : m_items)
            callback(*item);
    }

    Rect text_rect_in_menubar() const { return m_text_rect_in_menubar; }
    void set_text_rect_in_menubar(const Rect& rect) { m_text_rect_in_menubar = rect; }

    Rect rect_in_menubar() const { return m_rect_in_menubar; }
    void set_rect_in_menubar(const Rect& rect) { m_rect_in_menubar = rect; }

    WSWindow* menu_window() { return m_menu_window.ptr(); }
    void set_menu_window(OwnPtr<WSWindow>&&);

    WSWindow& ensure_menu_window();

    int width() const;
    int height() const;

    int item_height() const { return 16; }
    int padding() const { return 4; }

    void on_window_message(WSMessage&);
    void draw();
    const Font& font() const;

    WSMenuItem* item_at(const Point&);
    void redraw();

    const WSMenuItem* hovered_item() const { return m_hovered_item; }
    void clear_hovered_item();

    Function<void(WSMenuItem&)> on_item_activation;

private:
    void did_activate(WSMenuItem&);

    String m_name;
    Rect m_rect_in_menubar;
    Rect m_text_rect_in_menubar;
    WSMenuBar* m_menubar { nullptr };
    WSMenuItem* m_hovered_item { nullptr };
    Vector<OwnPtr<WSMenuItem>> m_items;
    OwnPtr<WSWindow> m_menu_window;
};

