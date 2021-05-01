/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <AK/NonnullRefPtrVector.h>
#include <LibCore/Object.h>
#include <LibGUI/Forward.h>

namespace GUI {

class Menubar : public Core::Object {
    C_OBJECT(Menubar);

public:
    ~Menubar();

    Menu& add_menu(String name);

    void notify_added_to_window(Badge<Window>);
    void notify_removed_from_window(Badge<Window>);

    int menubar_id() const { return m_menubar_id; }

private:
    Menubar();

    int realize_menubar();
    void unrealize_menubar();

    int m_menubar_id { -1 };
    NonnullRefPtrVector<Menu> m_menus;
};

}
