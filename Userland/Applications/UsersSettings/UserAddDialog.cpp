/*
 * Copyright (c) 2026, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "UserAddDialog.h"
#include "Constants.h"
#include <LibCore/Account.h>
#include <LibCore/Command.h>
#include <LibCore/SecretString.h>
#include <LibCore/System.h>
#include <LibGUI/Button.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/MessageBox.h>

namespace UsersSettings {

ErrorOr<void> UserAddDialog::initialize()
{
    m_account_type_combobox = find_descendant_of_type_named<GUI::ComboBox>("account_type_combobox");
    m_full_name_textbox = find_descendant_of_type_named<GUI::TextBox>("full_name_textbox");
    m_username_textbox = find_descendant_of_type_named<GUI::TextBox>("username_textbox");
    m_password_textbox = find_descendant_of_type_named<GUI::PasswordBox>("password_textbox");

    m_account_type_combobox->set_model(*GUI::ItemListModel<StringView, Array<StringView, 2>>::create(ACCOUNT_TYPE_NAMES));
    m_account_type_combobox->set_only_allow_values_from_model(true);
    m_account_type_combobox->set_selected_index(0);
    return {};
}

ErrorOr<Optional<String>> UserAddDialog::show(GUI::Window* parent_window)
{
    auto dialog = TRY(GUI::Dialog::try_create(parent_window));
    dialog->set_title("Add User");
    dialog->resize(260, 136);
    dialog->set_resizable(false);

    auto widget = TRY(UserAddDialog::try_create());
    dialog->set_main_widget(widget);

    Optional<String> created_username;

    auto& ok_button = *widget->find_descendant_of_type_named<GUI::Button>("ok_button");
    ok_button.on_click = [&](auto) {
        auto username = MUST(String::from_byte_string(widget->m_username_textbox->text()));
        if (username.is_empty()) {
            GUI::MessageBox::show(dialog, "Username must not be empty."sv, "Error"sv, GUI::MessageBox::Type::Error);
            return;
        }

        if (auto result = widget->add_user(); result.is_error()) {
            GUI::MessageBox::show_error(dialog, MUST(String::formatted("Failed to add user: {}", result.error())));
            return;
        }

        created_username = username;
        dialog->done(GUI::Dialog::ExecResult::OK);
    };
    ok_button.set_default(true);

    auto& cancel_button = *widget->find_descendant_of_type_named<GUI::Button>("cancel_button");
    cancel_button.on_click = [dialog](auto) {
        dialog->done(GUI::Dialog::ExecResult::Cancel);
    };

    dialog->exec();
    return created_username;
}

ErrorOr<void> UserAddDialog::add_user()
{
    auto full_name = TRY(String::from_byte_string(m_full_name_textbox->text()));
    auto username = TRY(String::from_byte_string(m_username_textbox->text()));
    auto password = Core::SecretString::take_ownership(m_password_textbox->text().to_byte_buffer());
    bool is_admin = m_account_type_combobox->selected_index() == 1;

    // Create user via useradd.
    if (!full_name.is_empty()) {
        TRY(Core::command("useradd"sv, { "--create-home"sv, "--gecos"sv, full_name.bytes_as_string_view(), username.bytes_as_string_view() }, {}));
    } else {
        TRY(Core::command("useradd"sv, { "--create-home"sv, username.bytes_as_string_view() }, {}));
    }

    // Update groups and password via Core::Account.
    auto account = TRY(Core::Account::from_name(username));
    for (auto group_name : DEFAULT_USER_GROUPS) {
        auto maybe_group = Core::System::getgrnam(group_name);
        if (!maybe_group.is_error() && maybe_group.value().has_value())
            account.add_extra_gid(maybe_group.value()->gr_gid);
    }
    if (is_admin) {
        auto maybe_wheel = Core::System::getgrnam(WHEEL_GROUP_NAME);
        if (!maybe_wheel.is_error() && maybe_wheel.value().has_value())
            account.add_extra_gid(maybe_wheel.value()->gr_gid);
    }
    TRY(account.set_password(password));
    TRY(account.sync());

    return {};
}

}
