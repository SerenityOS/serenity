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

#include <AK/NonnullOwnPtrVector.h>
#include <AK/WeakPtr.h>
#include <LibCore/Object.h>
#include <LibGUI/Action.h>
#include <LibGUI/Forward.h>
#include <LibGfx/Forward.h>

namespace GUI {

class Menu final : public Core::Object {
    C_OBJECT(Menu)
public:
    explicit Menu(const StringView& name = "");
    virtual ~Menu() override;

    void realize_menu_if_needed();

    static Menu* from_menu_id(int);
    int menu_id() const { return m_menu_id; }

    const String& name() const { return m_name; }
    const Gfx::Bitmap* icon() const { return m_icon.ptr(); }
    void set_icon(const Gfx::Bitmap*);

    Action* action_at(size_t);

    void add_action(NonnullRefPtr<Action>);
    void add_separator();
    Menu& add_submenu(const String& name);

    void popup(const Gfx::IntPoint& screen_position, const RefPtr<Action>& default_action = nullptr);
    void dismiss();

private:
    friend class MenuBar;

    int realize_menu(RefPtr<Action> default_action = nullptr);
    void unrealize_menu();
    void realize_if_needed(const RefPtr<Action>& default_action);

    int m_menu_id { -1 };
    String m_name;
    RefPtr<Gfx::Bitmap> m_icon;
    NonnullOwnPtrVector<MenuItem> m_items;
    WeakPtr<Action> m_last_default_action;
};

}
