/*
 * Copyright (c) 2018-2023, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/String.h>
#include <AK/WeakPtr.h>
#include <LibCore/EventReceiver.h>
#include <LibGUI/Action.h>
#include <LibGUI/ColorFilterer.h>
#include <LibGUI/Event.h>
#include <LibGUI/Forward.h>
#include <LibGfx/Forward.h>

namespace GUI {

namespace CommonMenus {

[[nodiscard]] NonnullRefPtr<Menu> make_accessibility_menu(GUI::ColorFilterer&);

};

class Menu final : public Core::EventReceiver {
    C_OBJECT(Menu)
public:
    virtual ~Menu() override;

    void realize_menu_if_needed();

    static Menu* from_menu_id(int);
    int menu_id() const { return m_menu_id; }

    String const& name() const { return m_name; }
    void set_name(String);

    int minimum_width() const { return m_minimum_width; }
    void set_minimum_width(int);

    Gfx::Bitmap const* icon() const { return m_icon.ptr(); }
    void set_icon(Gfx::Bitmap const*);

    Action* action_at(size_t);

    void add_action(NonnullRefPtr<Action>);
    void add_separator();
    [[nodiscard]] NonnullRefPtr<Menu> add_submenu(String name);
    void remove_all_actions();

    enum class AddTrailingSeparator {
        No,
        Yes,
    };
    void add_recent_files_list(Function<void(Action&)>, AddTrailingSeparator = AddTrailingSeparator::Yes);

    void popup(Gfx::IntPoint screen_position, RefPtr<Action> const& default_action = nullptr, Gfx::IntRect const& button_rect = {});
    void dismiss();

    void visibility_did_change(Badge<ConnectionToWindowServer>, bool visible);

    void set_children_actions_enabled(bool enabled);

    Function<void(bool)> on_visibility_change;

    bool is_visible() const { return m_visible; }

    Vector<NonnullOwnPtr<MenuItem>> const& items() const { return m_items; }

private:
    friend class Menubar;

    explicit Menu(String name = {});

    int realize_menu(RefPtr<Action> default_action = nullptr);
    void unrealize_menu();
    void realize_if_needed(RefPtr<Action> const& default_action);

    void realize_menu_item(MenuItem&, int item_id);

    void set_parent(Menu& menu, int submenu_index);
    void update_parent_menu_item();

    int m_menu_id { -1 };
    String m_name;
    int m_minimum_width { 0 };
    RefPtr<Gfx::Bitmap const> m_icon;
    Vector<NonnullOwnPtr<MenuItem>> m_items;
    WeakPtr<Action> m_current_default_action;
    bool m_visible { false };
    WeakPtr<Menu> m_parent_menu;
    int m_index_in_parent_menu { -1 };

    Function<void(Action&)> m_recent_files_callback;
};

}
