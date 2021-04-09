/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
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

#include "Menu.h"
#include "MenuBar.h"
#include "Window.h"
#include <AK/HashMap.h>
#include <AK/StringBuilder.h>

namespace WindowServer {

class MenuManager final : public Core::Object {
    C_OBJECT(MenuManager);

public:
    static MenuManager& the();

    MenuManager();
    virtual ~MenuManager() override;

    void refresh();

    bool is_open(const Menu&) const;
    bool has_open_menu() const { return !m_open_menu_stack.is_empty(); }

    Menu* current_menu() { return m_current_menu.ptr(); }
    void set_current_menu(Menu*);
    void clear_current_menu();
    void open_menu(Menu&, bool as_current_menu = true);

    void close_everyone();
    void close_everyone_not_in_lineage(Menu&);
    void close_menu_and_descendants(Menu&);

    void close_all_menus_from_client(Badge<ClientConnection>, ClientConnection&);

    int theme_index() const { return m_theme_index; }

    Menu* previous_menu(Menu* current);
    Menu* next_menu(Menu* current);

    void did_change_theme();

    void set_hovered_menu(Menu*);
    Menu* hovered_menu() { return m_hovered_menu; }

private:
    void close_menus(const Vector<Menu*>&);

    virtual void event(Core::Event&) override;
    void handle_mouse_event(MouseEvent&);

    WeakPtr<Menu> m_current_menu;
    WeakPtr<Window> m_previous_input_window;
    Vector<WeakPtr<Menu>> m_open_menu_stack;

    int m_theme_index { 0 };

    WeakPtr<Menu> m_hovered_menu;
};

}
