/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/NonnullOwnPtrVector.h>
#include <AK/WeakPtr.h>
#include <LibCore/Object.h>
#include <LibGUI/Action.h>
#include <LibGUI/Event.h>
#include <LibGUI/Forward.h>
#include <LibGfx/Forward.h>

namespace GUI {

class Menu final : public Core::Object {
    C_OBJECT(Menu)
public:
    virtual ~Menu() override;

    void realize_menu_if_needed();

    static Menu* from_menu_id(int);
    int menu_id() const { return m_menu_id; }

    const String& name() const { return m_name; }
    const Gfx::Bitmap* icon() const { return m_icon.ptr(); }
    void set_icon(const Gfx::Bitmap*);

    Action* action_at(size_t);

    ErrorOr<void> try_add_action(NonnullRefPtr<Action>);
    ErrorOr<void> try_add_separator();
    ErrorOr<NonnullRefPtr<Menu>> try_add_submenu(String name);

    void add_action(NonnullRefPtr<Action>);
    void add_separator();
    Menu& add_submenu(String name);

    void popup(const Gfx::IntPoint& screen_position, const RefPtr<Action>& default_action = nullptr);
    void dismiss();

    void visibility_did_change(Badge<WindowServerConnection>, bool visible);

    Function<void(bool)> on_visibility_change;

    bool is_visible() const { return m_visible; }

private:
    friend class Menubar;

    explicit Menu(String name = "");

    int realize_menu(RefPtr<Action> default_action = nullptr);
    void unrealize_menu();
    void realize_if_needed(const RefPtr<Action>& default_action);

    void realize_menu_item(MenuItem&, int item_id);

    int m_menu_id { -1 };
    String m_name;
    RefPtr<Gfx::Bitmap> m_icon;
    NonnullOwnPtrVector<MenuItem> m_items;
    WeakPtr<Action> m_current_default_action;
    bool m_visible { false };
};

}
