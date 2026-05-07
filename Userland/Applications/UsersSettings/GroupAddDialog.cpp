/*
 * Copyright (c) 2026, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "GroupAddDialog.h"
#include <LibCore/Group.h>
#include <LibGUI/Button.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/MessageBox.h>

namespace UsersSettings {

ErrorOr<void> GroupAddDialog::initialize()
{
    m_group_name_textbox = find_descendant_of_type_named<GUI::TextBox>("group_name_textbox");
    return {};
}

ErrorOr<Optional<String>> GroupAddDialog::show(GUI::Window* parent_window)
{
    auto dialog = TRY(GUI::Dialog::try_create(parent_window));
    dialog->set_title("Add Group");
    dialog->resize(260, 64);
    dialog->set_resizable(false);

    auto widget = TRY(GroupAddDialog::try_create());
    dialog->set_main_widget(widget);

    Optional<String> created_group_name;

    auto& ok_button = *widget->find_descendant_of_type_named<GUI::Button>("ok_button");
    ok_button.on_click = [&](auto) {
        auto group_name = MUST(String::from_byte_string(widget->m_group_name_textbox->text()));
        if (group_name.is_empty()) {
            GUI::MessageBox::show(dialog, "Group name must not be empty."sv, "Error"sv, GUI::MessageBox::Type::Error);
            return;
        }

        if (auto result = widget->add_group(); result.is_error()) {
            GUI::MessageBox::show_error(dialog, MUST(String::formatted("Failed to add group: {}", result.error())));
            return;
        }

        created_group_name = group_name;
        dialog->done(GUI::Dialog::ExecResult::OK);
    };
    ok_button.set_default(true);

    auto& cancel_button = *widget->find_descendant_of_type_named<GUI::Button>("cancel_button");
    cancel_button.on_click = [dialog](auto) {
        dialog->done(GUI::Dialog::ExecResult::Cancel);
    };

    dialog->exec();
    return created_group_name;
}

ErrorOr<void> GroupAddDialog::add_group()
{
    auto name = TRY(String::from_byte_string(m_group_name_textbox->text()));
    Core::Group group { name.to_byte_string() };
    TRY(Core::Group::add_group(group));
    return {};
}

}
