/*
 * Copyright (c) 2026, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "UserDetailsWidget.h"
#include "ChangePasswordDialog.h"
#include "Constants.h"
#include <AK/String.h>
#include <LibCore/SecretString.h>
#include <LibCore/System.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/ItemListModel.h>
#include <LibGUI/MessageBox.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Widget.h>

namespace UsersSettings {

ErrorOr<NonnullRefPtr<UserDetailsWidget>> UserDetailsWidget::create(Core::Account const& account)
{
    auto widget = TRY(try_create());
    TRY(widget->initialize(account));
    return widget;
}

// GECOS field is "Full Name,Room,Work,Home,Other" - extract just the full name part.
static ByteString gecos_full_name(ByteString const& gecos)
{
    auto comma = gecos.find(',');
    if (comma.has_value())
        return gecos.substring(0, comma.value());
    return gecos;
}

ErrorOr<void> UserDetailsWidget::initialize(Core::Account const& account)
{
    m_account = account;

    m_username_textbox = find_descendant_of_type_named<GUI::TextBox>("username_textbox");
    m_full_name_textbox = find_descendant_of_type_named<GUI::TextBox>("full_name_textbox");
    m_shell_textbox = find_descendant_of_type_named<GUI::TextBox>("shell_textbox");
    m_home_dir_textbox = find_descendant_of_type_named<GUI::TextBox>("home_dir_textbox");
    m_uid_gid_textbox = find_descendant_of_type_named<GUI::TextBox>("uid_gid_textbox");
    m_account_type_combobox = find_descendant_of_type_named<GUI::ComboBox>("account_type_combobox");
    m_change_password_button = find_descendant_of_type_named<GUI::Button>("change_password_button");

    m_username_textbox->set_text(account.username());

    m_full_name_textbox->set_text(gecos_full_name(account.gecos()));
    m_full_name_textbox->on_change = [this] {
        if (on_modified)
            on_modified();
    };

    m_shell_textbox->set_text(account.shell());
    m_shell_textbox->on_change = [this] {
        if (on_modified)
            on_modified();
    };

    m_home_dir_textbox->set_text(account.home_directory());
    m_uid_gid_textbox->set_text(ByteString::formatted("{} / {}", account.uid(), account.gid()));

    // Check wheel group membership to determine admin status.
    bool is_admin = false;
    auto maybe_wheel = Core::System::getgrnam(WHEEL_GROUP_NAME);
    if (!maybe_wheel.is_error() && maybe_wheel.value().has_value())
        is_admin = account.extra_gids().contains_slow(maybe_wheel.value()->gr_gid);
    m_account_type_combobox->set_model(*GUI::ItemListModel<StringView, Array<StringView, 2>>::create(ACCOUNT_TYPE_NAMES));
    m_account_type_combobox->set_only_allow_values_from_model(true);
    m_account_type_combobox->set_selected_index(is_admin ? 1 : 0);
    m_account_type_combobox->on_change = [this](auto&, auto) {
        if (on_modified)
            on_modified();
    };

    m_change_password_button->on_click = [this](auto) {
        if (auto result = change_password(); result.is_error())
            GUI::MessageBox::show_error(window(), "Failed to change password"sv);
    };

    return {};
}

ErrorOr<void> UserDetailsWidget::change_password()
{
    VERIFY(m_account.has_value());

    auto dialog = TRY(GUI::Dialog::try_create(window()));
    dialog->set_title(ByteString::formatted("Change Password for {}", m_account->username()));
    dialog->resize(280, 85);
    dialog->set_resizable(false);

    auto widget = TRY(ChangePasswordDialog::try_create());
    dialog->set_main_widget(widget);

    auto& new_password_textbox = *widget->find_descendant_of_type_named<GUI::PasswordBox>("new_password_textbox");
    auto& confirm_password_textbox = *widget->find_descendant_of_type_named<GUI::PasswordBox>("confirm_password_textbox");

    auto& ok_button = *widget->find_descendant_of_type_named<GUI::Button>("ok_button");
    ok_button.on_click = [&](auto) {
        if (new_password_textbox.text() != confirm_password_textbox.text()) {
            GUI::MessageBox::show(window(), "Passwords do not match."sv, "Error"sv, GUI::MessageBox::Type::Error);
            return;
        }
        auto password = Core::SecretString::take_ownership(new_password_textbox.text().to_byte_buffer());
        // Re-read account fresh from disk to only modify the password field.
        auto fresh_account_or_error = Core::Account::from_name(m_account->username());
        if (fresh_account_or_error.is_error()) {
            GUI::MessageBox::show(window(), "Failed to read account."sv, "Error"sv, GUI::MessageBox::Type::Error);
            return;
        }
        auto& fresh_account = fresh_account_or_error.value();
        if (fresh_account.set_password(password).is_error()) {
            GUI::MessageBox::show(window(), "Failed to set password."sv, "Error"sv, GUI::MessageBox::Type::Error);
            return;
        }
        if (fresh_account.sync().is_error()) {
            GUI::MessageBox::show(window(), "Failed to save changes."sv, "Error"sv, GUI::MessageBox::Type::Error);
            return;
        }
        // Reload m_account so a subsequent Apply does not overwrite with stale password hash.
        auto reloaded = Core::Account::from_name(m_account->username());
        if (!reloaded.is_error())
            m_account = reloaded.release_value();

        dialog->done(GUI::Dialog::ExecResult::OK);
    };
    ok_button.set_default(true);

    auto& cancel_button = *widget->find_descendant_of_type_named<GUI::Button>("cancel_button");
    cancel_button.on_click = [dialog](auto) {
        dialog->done(GUI::Dialog::ExecResult::Cancel);
    };

    dialog->exec();
    return {};
}

ErrorOr<void> UserDetailsWidget::apply_changes()
{
    VERIFY(m_account.has_value());

    // Preserve GECOS fields beyond the full name (Room, Work, Home, Other).
    auto existing_gecos = m_account->gecos();
    auto first_comma = existing_gecos.find(',');
    if (first_comma.has_value())
        m_account->set_gecos(ByteString::formatted("{}{}", m_full_name_textbox->text(), existing_gecos.substring_view(first_comma.value())));
    else
        m_account->set_gecos(m_full_name_textbox->text());
    m_account->set_shell(m_shell_textbox->text());

    // Update administrator (wheel group) membership.
    auto maybe_wheel = TRY(Core::System::getgrnam(WHEEL_GROUP_NAME));
    if (maybe_wheel.has_value()) {
        gid_t wheel_gid = maybe_wheel->gr_gid;
        bool is_admin_selected = m_account_type_combobox->selected_index() == 1;
        bool already_admin = m_account->extra_gids().contains_slow(wheel_gid);
        if (is_admin_selected && !already_admin)
            m_account->add_extra_gid(wheel_gid);
        else if (!is_admin_selected && already_admin)
            m_account->remove_extra_gid(wheel_gid);
    }

    TRY(m_account->sync());

    return {};
}

}
