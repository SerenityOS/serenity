/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#pragma once

#include <AK/Badge.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/OwnPtr.h>
#include <AK/String.h>

namespace GUI {

class Action;
class Menu;

class MenuItem {
public:
    enum class Type {
        Invalid,
        Action,
        Separator,
        Submenu,
    };

    MenuItem(unsigned menu_id, Type);
    MenuItem(unsigned menu_id, NonnullRefPtr<Action>&&);
    MenuItem(unsigned menu_id, NonnullRefPtr<Menu>&&);
    ~MenuItem();

    Type type() const { return m_type; }
    String text() const;
    const Action* action() const { return m_action.ptr(); }
    Action* action() { return m_action.ptr(); }
    unsigned identifier() const { return m_identifier; }

    Menu* submenu() { return m_submenu.ptr(); }
    const Menu* submenu() const { return m_submenu.ptr(); }

    bool is_checkable() const { return m_checkable; }
    void set_checkable(bool checkable) { m_checkable = checkable; }

    bool is_checked() const { return m_checked; }
    void set_checked(bool);

    bool is_enabled() const { return m_enabled; }
    void set_enabled(bool);

    void set_menu_id(Badge<Menu>, unsigned menu_id) { m_menu_id = menu_id; }
    void set_identifier(Badge<Menu>, unsigned identifier) { m_identifier = identifier; }

private:
    void update_window_server();

    Type m_type { Type::Invalid };
    int m_menu_id { -1 };
    unsigned m_identifier { 0 };
    bool m_enabled { true };
    bool m_checkable { false };
    bool m_checked { false };
    RefPtr<Action> m_action;
    RefPtr<Menu> m_submenu;
};

}
