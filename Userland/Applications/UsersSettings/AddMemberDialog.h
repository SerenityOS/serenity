/*
 * Copyright (c) 2026, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/String.h>
#include <AK/Vector.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>

namespace UsersSettings {

class AddMemberDialog final : public GUI::Widget {
    C_OBJECT(AddMemberDialog)
public:
    static ErrorOr<NonnullRefPtr<AddMemberDialog>> try_create();
    ErrorOr<void> initialize();

    static ErrorOr<Optional<String>> show(GUI::Window* parent_window, Vector<String> const& available_usernames);

private:
    AddMemberDialog() = default;

    RefPtr<GUI::ComboBox> m_user_combobox;
};

}
