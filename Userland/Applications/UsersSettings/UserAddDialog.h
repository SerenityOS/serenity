/*
 * Copyright (c) 2026, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/String.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>
#include <LibGUI/Window.h>

namespace UsersSettings {

class UserAddDialog final : public GUI::Widget {
    C_OBJECT(UserAddDialog)
public:
    static ErrorOr<NonnullRefPtr<UserAddDialog>> try_create();
    ErrorOr<void> initialize();

    static ErrorOr<Optional<String>> show(GUI::Window* parent_window);

private:
    UserAddDialog() = default;

    ErrorOr<void> add_user();

    RefPtr<GUI::ComboBox> m_account_type_combobox;
    RefPtr<GUI::TextBox> m_full_name_textbox;
    RefPtr<GUI::TextBox> m_username_textbox;
    RefPtr<GUI::PasswordBox> m_password_textbox;
};

}
