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
#include <WindowServer/Event.h>
#include <WindowServer/MenuItem.h>

namespace WindowServer {

class ConnectionFromClient;
class Menubar;
class Window;

class Menu final : public Core::Object {
    C_OBJECT(Menu);

public:
    virtual ~Menu() override;

    ConnectionFromClient* client() { return m_client; }
    const ConnectionFromClient* client() const { return m_client; }
    int menu_id() const { return m_menu_id; }

    bool is_open() const;

    u32 alt_shortcut_character() const { return m_alt_shortcut_character; }

    bool is_empty() const { return m_items.is_empty(); }
    size_t item_count() const { return m_items.size(); }
    const MenuItem& item(size_t index) const { return m_items.at(index); }
    MenuItem& item(size_t index) { return m_items.at(index); }

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

    String const& name() const { return m_name; }

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
    Window& ensure_menu_window(Gfx::IntPoint const&);

    Window* window_menu_of() { return m_window_menu_of; }
    void set_window_menu_of(Window& window) { m_window_menu_of = window; }
    bool is_window_menu_open() const { return m_is_window_menu_open; }
    void set_window_menu_open(bool is_open) { m_is_window_menu_open = is_open; }

    bool activate_default();

    int content_width() const;

    static constexpr int item_height() { return 22; }
    static constexpr int frame_thickness() { return 2; }
    static constexpr int horizontal_padding() { return left_padding() + right_padding(); }
    static constexpr int left_padding() { return 14; }
    static constexpr int right_padding() { return 14; }

    void draw();
    void draw(MenuItem const&, bool = false);
    const Gfx::Font& font() const;

    MenuItem* item_with_identifier(unsigned);
    bool remove_item_with_identifier(unsigned);
    void redraw();
    void redraw(MenuItem const&);

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
    Menu(ConnectionFromClient*, int menu_id, String name);

    virtual void event(Core::Event&) override;

    void handle_mouse_move_event(const MouseEvent&);
    size_t visible_item_count() const;
    Gfx::IntRect stripe_rect();

    int item_index_at(const Gfx::IntPoint&);
    static constexpr int padding_between_text_and_shortcut() { return 50; }
    void did_activate(MenuItem&, bool leave_menu_open);
    void update_for_new_hovered_item(bool make_input = false);

    void start_activation_animation(MenuItem&);

    ConnectionFromClient* m_client { nullptr };
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

u32 find_ampersand_shortcut_character(StringView);

}
