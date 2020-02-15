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

#include <AK/Function.h>
#include <AK/NonnullOwnPtrVector.h>
#include <AK/NonnullRefPtr.h>
#include <LibCore/Object.h>
#include <LibGfx/Forward.h>

namespace GUI {

class Action;
class MenuItem;

class Menu final : public Core::Object {
    C_OBJECT(Menu)
public:
    explicit Menu(const StringView& name = "");
    virtual ~Menu() override;

    static Menu* from_menu_id(int);

    const String& name() const { return m_name; }

    Action* action_at(int);

    void add_action(NonnullRefPtr<Action>);
    void add_separator();
    void add_submenu(NonnullRefPtr<Menu>);

    void popup(const Gfx::Point& screen_position);
    void dismiss();

    Function<void(unsigned)> on_item_activation;

private:
    friend class MenuBar;

    int menu_id() const { return m_menu_id; }
    int realize_menu();
    void unrealize_menu();
    void realize_if_needed();

    int m_menu_id { -1 };
    String m_name;
    NonnullOwnPtrVector<MenuItem> m_items;
};

}
