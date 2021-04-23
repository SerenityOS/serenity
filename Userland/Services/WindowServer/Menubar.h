/*
 * Copyright (c) 2018-2020, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "Menu.h"
#include <AK/Vector.h>
#include <AK/WeakPtr.h>
#include <AK/Weakable.h>

namespace WindowServer {

class Menubar
    : public RefCounted<Menubar>
    , public Weakable<Menubar> {
public:
    static NonnullRefPtr<Menubar> create(ClientConnection& client, int menubar_id) { return adopt_ref(*new Menubar(client, menubar_id)); }
    ~Menubar();

    ClientConnection& client() { return m_client; }
    const ClientConnection& client() const { return m_client; }
    int menubar_id() const { return m_menubar_id; }
    void add_menu(Menu&);

    template<typename Callback>
    void for_each_menu(Callback callback)
    {
        for (auto& menu : m_menus) {
            if (callback(*menu) == IterationDecision::Break)
                return;
        }
    }

private:
    Menubar(ClientConnection&, int menubar_id);

    ClientConnection& m_client;
    int m_menubar_id { 0 };
    Vector<Menu*> m_menus;
};

}
