/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <AK/String.h>
#include <AK/WeakPtr.h>
#include <LibCore/Object.h>
#include <LibGfx/Font.h>
#include <LibGfx/FontDatabase.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Rect.h>
#include <WindowServer/Cursor.h>
#include <WindowServer/MenuItem.h>
#include <WindowServer/Window.h>

namespace WindowServer {

class ClientConnection;
class Menubar;

class Menu final : public Core::Object {
    C_OBJECT(Menu);

public:
    Menu(ClientConnection*, int menu_id, String name);
    virtual ~Menu() override;

    ClientConnection* client() { return m_client; }
    const ClientConnection* client() const { return m_client; }
    int menu_id() const { return m_menu_id; }

    u32 alt_shortcut_character() const { return m_alt_shortcut_character; }

    bool is_empty() const { return m_items.is_empty(); }
    int item_count() const { return m_items.size(); }
    const MenuItem& item(int index) const { return m_items.at(index); }
    MenuItem& item(int index) { return m_items.at(index); }

    MenuItem* item_by_identifier(unsigned identifier)
    {
        MenuItem* found_item = nullptr;
        for_each_item([&](auto& item) {
            if (item.identifier() == identifier) {
                found_item = &item;
                return IterationDecision::Break;
            }
            return IterationDecision::Continue;
        });
        return found_item;
    }

    void add_item(NonnullOwnPtr<MenuItem>);

    String name() const { return m_name; }

    template<typename Callback>
    IterationDecision for_each_item(Callback callback)
    {
        for (auto& item : m_items) {
            IterationDecision decision = callback(item);
            if (decision != IterationDecision::Continue)
                return decision;
        }
        return IterationDecision::Continue;
    }

    Gfx::IntRect rect_in_window_menubar() const { return m_rect_in_window_menubar; }
    void set_rect_in_window_menubar(const Gfx::IntRect& rect) { m_rect_in_window_menubar = rect; }

    Window* menu_window() { return m_menu_window.ptr(); }
    Window& ensure_menu_window();

    Window* window_menu_of() { return m_window_menu_of; }
    void set_window_menu_of(Window& window) { m_window_menu_of = window; }
    bool is_window_menu_open() { return m_is_window_menu_open; }
    void set_window_menu_open(bool is_open) { m_is_window_menu_open = is_open; }

    bool activate_default();

    int content_width() const;

    int item_height() const { return 22; }
    int frame_thickness() const { return 2; }
    int horizontal_padding() const { return left_padding() + right_padding(); }
    int left_padding() const { return 14; }
    int right_padding() const { return 14; }

    void draw();
    const Gfx::Font& font() const;

    MenuItem* item_with_identifier(unsigned);
    void redraw();

    MenuItem* hovered_item() const;

    void set_hovered_index(int index, bool make_input = false);

    void clear_hovered_item();

    Function<void(MenuItem&)> on_item_activation;

    void close();

    void set_visible(bool);

    void popup(const Gfx::IntPoint&);
    void do_popup(const Gfx::IntPoint&, bool make_input, bool as_submenu = false);

    bool is_menu_ancestor_of(const Menu&) const;

    void redraw_if_theme_changed();

    bool is_scrollable() const { return m_scrollable; }
    int scroll_offset() const { return m_scroll_offset; }

    void descend_into_submenu_at_hovered_item();
    void open_hovered_item(bool leave_menu_open);

    const Vector<size_t>* items_with_alt_shortcut(u32 alt_shortcut) const;

private:
    virtual void event(Core::Event&) override;

    void handle_mouse_move_event(const MouseEvent&);
    int visible_item_count() const;

    int item_index_at(const Gfx::IntPoint&);
    int padding_between_text_and_shortcut() const { return 50; }
    void did_activate(MenuItem&, bool leave_menu_open);
    void update_for_new_hovered_item(bool make_input = false);

    void start_activation_animation(MenuItem&);

    ClientConnection* m_client { nullptr };
    int m_menu_id { 0 };
    String m_name;
    u32 m_alt_shortcut_character { 0 };
    Gfx::IntRect m_rect_in_window_menubar;
    NonnullOwnPtrVector<MenuItem> m_items;
    RefPtr<Window> m_menu_window;

    WeakPtr<Window> m_window_menu_of;
    bool m_is_window_menu_open = { false };
    Gfx::IntPoint m_last_position_in_hover;
    int m_theme_index_at_last_paint { -1 };
    int m_hovered_item_index { -1 };

    bool m_scrollable { false };
    int m_scroll_offset { 0 };
    int m_max_scroll_offset { 0 };

    HashMap<u32, Vector<size_t>> m_alt_shortcut_character_to_item_indices;
};

u32 find_ampersand_shortcut_character(const StringView&);

}
