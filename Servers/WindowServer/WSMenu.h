#pragma once

#include <AK/String.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/WeakPtr.h>
#include <LibCore/CObject.h>
#include <LibDraw/Rect.h>
#include <WindowServer/WSMenuItem.h>

class WSClientConnection;
class WSMenuBar;
class WSEvent;
class WSWindow;
class Font;

class WSMenu final : public CObject {
    C_OBJECT(WSMenu)
public:
    WSMenu(WSClientConnection*, int menu_id, const String& name);
    virtual ~WSMenu() override;

    WSClientConnection* client() { return m_client; }
    const WSClientConnection* client() const { return m_client; }
    int menu_id() const { return m_menu_id; }

    WSMenuBar* menubar() { return m_menubar; }
    const WSMenuBar* menubar() const { return m_menubar; }
    void set_menubar(WSMenuBar* menubar) { m_menubar = menubar; }

    bool is_empty() const { return m_items.is_empty(); }
    int item_count() const { return m_items.size(); }

    void add_item(NonnullOwnPtr<WSMenuItem>&& item) { m_items.append(move(item)); }

    String name() const { return m_name; }

    template<typename Callback>
    void for_each_item(Callback callback) const
    {
        for (auto& item : m_items)
            callback(item);
    }

    Rect text_rect_in_menubar() const { return m_text_rect_in_menubar; }
    void set_text_rect_in_menubar(const Rect& rect) { m_text_rect_in_menubar = rect; }

    Rect rect_in_menubar() const { return m_rect_in_menubar; }
    void set_rect_in_menubar(const Rect& rect) { m_rect_in_menubar = rect; }

    WSWindow* menu_window() { return m_menu_window.ptr(); }
    WSWindow& ensure_menu_window();

    int width() const;
    int height() const;

    int item_height() const { return 20; }
    int frame_thickness() const { return 3; }
    int horizontal_padding() const { return left_padding() + right_padding(); }
    int left_padding() const { return 14; }
    int right_padding() const { return 14; }

    void draw();
    const Font& font() const;

    WSMenuItem* item_with_identifier(unsigned);
    WSMenuItem* item_at(const Point&);
    void redraw();

    const WSMenuItem* hovered_item() const { return m_hovered_item; }
    void clear_hovered_item();

    Function<void(WSMenuItem&)> on_item_activation;

    void close();

    void popup(const Point&, bool is_submenu = false);

    bool is_menu_ancestor_of(const WSMenu&) const;

private:
    virtual void event(CEvent&) override;

    int padding_between_text_and_shortcut() const { return 50; }
    void did_activate(WSMenuItem&);
    WSClientConnection* m_client { nullptr };
    int m_menu_id { 0 };
    String m_name;
    Rect m_rect_in_menubar;
    Rect m_text_rect_in_menubar;
    WSMenuBar* m_menubar { nullptr };
    WSMenuItem* m_hovered_item { nullptr };
    NonnullOwnPtrVector<WSMenuItem> m_items;
    RefPtr<WSWindow> m_menu_window;
};
