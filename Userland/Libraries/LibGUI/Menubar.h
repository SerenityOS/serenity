/*
 * Copyright (c) 2018-2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2021, sin-ack <sin-ack@protonmail.com>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Badge.h>
#include <AK/IterationDecision.h>
#include <LibCore/EventReceiver.h>
#include <LibGUI/Forward.h>
#include <LibGUI/Menu.h>

namespace GUI {

class Menubar : public Core::EventReceiver {
    C_OBJECT(Menubar);

public:
    virtual ~Menubar() override = default;

    ErrorOr<void> try_add_menu(Badge<Window>, NonnullRefPtr<Menu>);
    ErrorOr<NonnullRefPtr<Menu>> try_add_menu(Badge<Window>, String name);
    Menu& add_menu(Badge<Window>, String name);

    void for_each_menu(Function<IterationDecision(Menu&)>);

private:
    Menubar() = default;

    Vector<NonnullRefPtr<Menu>> m_menus;
};

}
