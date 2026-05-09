/*
 * Copyright (c) 2026, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/Optional.h>
#include <LibCore/Account.h>
#include <LibGUI/Button.h>
#include <LibGUI/ComboBox.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>

namespace UsersSettings {

class UserDetailsWidget final : public GUI::Widget {
    C_OBJECT(UserDetailsWidget)

public:
    static ErrorOr<NonnullRefPtr<UserDetailsWidget>> create(Core::Account const& account);
    static ErrorOr<NonnullRefPtr<UserDetailsWidget>> try_create();

    ErrorOr<void> apply_changes();

    Function<void()> on_modified;

private:
    UserDetailsWidget() = default;

    ErrorOr<void> initialize(Core::Account const& account);
    ErrorOr<void> change_password();

    Optional<Core::Account> m_account;
    RefPtr<GUI::TextBox> m_username_textbox;
    RefPtr<GUI::TextBox> m_full_name_textbox;
    RefPtr<GUI::TextBox> m_shell_textbox;
    RefPtr<GUI::TextBox> m_home_dir_textbox;
    RefPtr<GUI::TextBox> m_uid_gid_textbox;
    RefPtr<GUI::ComboBox> m_account_type_combobox;
    RefPtr<GUI::Button> m_change_password_button;
};

}
