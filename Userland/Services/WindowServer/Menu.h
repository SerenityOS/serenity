/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/ByteString.h>
#include <AK/WeakPtr.h>
#include <LibCore/EventReceiver.h>
#include <LibGfx/Font/Font.h>
#include <LibGfx/Font/FontDatabase.h>
#include <LibGfx/Forward.h>
#include <LibGfx/Rect.h>
#include <WindowServer/Cursor.h>
#include <WindowServer/Event.h>
#include <WindowServer/MenuItem.h>

namespace WindowServer {

class ConnectionFromClient;
class Menubar;
class Window;

class Menu final : public Core::EventReceiver {
    C_OBJECT(Menu);

public:
    virtual ~Menu() override = default;

    ConnectionFromClient* client() { return m_client; }
    ConnectionFromClient const* client() const { return m_client; }
    int menu_id() const { return m_menu_id; }

    bool is_open() const;

    u32 alt_shortcut_character() const { return m_alt_shortcut_character; }

    bool is_empty() const { return m_items.is_empty(); }
    size_t item_count() const { return m_items.size(); }
    MenuItem const& item(size_t index) const { return *m_items.at(index); }
    MenuItem& item(size_t index) { return *m_items.at(index); }

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

    void update_alt_shortcuts_for_items();
    void add_item(NonnullOwnPtr<MenuItem>);

    String const& name() const { return m_name; }
    void set_name(String);

    int minimum_width() const { return m_minimum_width; }
    void set_minimum_width(int);

    template<typename Callback>
    IterationDecision for_each_item(Callback callback)
    {
        for (auto& item : m_items) {
            IterationDecision decision = callback(*item);
            if (decision != IterationDecision::Continue)
                return decision;
        }
        return IterationDecision::Continue;
    }

    Gfx::IntRect rect_in_window_menubar() const { return m_rect_in_window_menubar; }
    void set_rect_in_window_menubar(Gfx::IntRect const& rect) { m_rect_in_window_menubar = rect; }

    Gfx::IntPoint unadjusted_position() const { return m_unadjusted_position; }
    void set_unadjusted_position(Gfx::IntPoint position) { m_unadjusted_position = position; }

    Window* menu_window() { return m_menu_window.ptr(); }
    Window& ensure_menu_window(Gfx::IntPoint);

    // Invalidates the menu window so that it gets rebuilt the next time it's showed.
    void invalidate_menu_window();

    Window* window_menu_of() { return m_window_menu_of; }
    void set_window_menu_of(Window& window) { m_window_menu_of = window; }
    bool is_window_menu_open() const { return m_is_window_menu_open; }
    void set_window_menu_open(bool is_open) { m_is_window_menu_open = is_open; }

    bool activate_default();

    int content_width() const;

    int item_height() const;
    static constexpr int frame_thickness() { return 2; }
    static constexpr int horizontal_padding() { return left_padding() + right_padding(); }
    static constexpr int left_padding() { return 14; }
    static constexpr int right_padding() { return 14; }

    void draw();
    void draw(MenuItem const&, bool = false);
    Gfx::Font const& font() const;

    MenuItem* item_with_identifier(unsigned);
    bool remove_item_with_identifier(unsigned);
    void redraw();
    void redraw(MenuItem const&);

    MenuItem* hovered_item() const;
    int hovered_item_index() const { return m_hovered_item_index; }

    void set_hovered_index(int index, bool make_input = false);

    void clear_hovered_item();

    Function<void(MenuItem&)> on_item_activation;

    void close();

    void set_visible(bool);

    void popup(Gfx::IntPoint);
    void do_popup(Gfx::IntPoint, bool make_input, bool as_submenu = false);
    void open_button_menu(Gfx::IntPoint position, Gfx::IntRect const& button_rect);

    bool is_menu_ancestor_of(Menu const&) const;

    void redraw_if_theme_changed();

    bool is_scrollable() const { return m_scrollable; }
    int scroll_offset() const { return m_scroll_offset; }

    void descend_into_submenu_at_hovered_item();
    void open_hovered_item(bool leave_menu_open);

    Vector<size_t> const* items_with_alt_shortcut(u32 alt_shortcut) const;

private:
    Menu(ConnectionFromClient*, int menu_id, String name, int minimum_width = 0);

    virtual void event(Core::Event&) override;

    void handle_mouse_move_event(MouseEvent const&);
    size_t visible_item_count() const;
    Gfx::IntRect stripe_rect();

    int item_index_at(Gfx::IntPoint);
    static constexpr int padding_between_text_and_shortcut() { return 50; }
    void did_activate(MenuItem&, bool leave_menu_open);
    void update_for_new_hovered_item(bool make_input = false);

    void start_activation_animation(MenuItem&);

    bool opens_to_the_left() const { return m_opens_to_the_left; }

    ConnectionFromClient* m_client { nullptr };
    int m_menu_id { 0 };
    String m_name;
    int m_minimum_width { 0 };
    u32 m_alt_shortcut_character { 0 };
    Gfx::IntRect m_rect_in_window_menubar;
    Gfx::IntPoint m_unadjusted_position;
    Vector<NonnullOwnPtr<MenuItem>> m_items;
    RefPtr<Window> m_menu_window;

    WeakPtr<Window> m_window_menu_of;
    bool m_is_window_menu_open = { false };
    Gfx::IntPoint m_last_position_in_hover;
    int m_theme_index_at_last_paint { -1 };
    int m_hovered_item_index { -1 };
    bool m_opens_to_the_left { false };

    bool m_scrollable { false };
    int m_scroll_offset { 0 };
    int m_max_scroll_offset { 0 };

    HashMap<u32, Vector<size_t>> m_alt_shortcut_character_to_item_indices;
};

u32 find_ampersand_shortcut_character(StringView);

}
