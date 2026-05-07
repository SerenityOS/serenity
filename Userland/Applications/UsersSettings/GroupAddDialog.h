/*
 * Copyright (c) 2026, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/String.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>

namespace UsersSettings {

class GroupAddDialog final : public GUI::Widget {
    C_OBJECT(GroupAddDialog)
public:
    static ErrorOr<NonnullRefPtr<GroupAddDialog>> try_create();
    ErrorOr<void> initialize();

    static ErrorOr<Optional<String>> show(GUI::Window* parent_window);

private:
    GroupAddDialog() = default;

    ErrorOr<void> add_group();

    RefPtr<GUI::TextBox> m_group_name_textbox;
};

}
