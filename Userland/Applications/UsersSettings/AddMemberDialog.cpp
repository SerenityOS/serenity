/*
 * Copyright (c) 2026, Bastiaan van der Plaat <bastiaan.v.d.plaat@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "AddMemberDialog.h"
#include <LibGUI/Button.h>
#include <LibGUI/Dialog.h>
#include <LibGUI/ItemListModel.h>

namespace UsersSettings {

ErrorOr<void> AddMemberDialog::initialize()
{
    m_user_combobox = find_descendant_of_type_named<GUI::ComboBox>("user_combobox");
    return {};
}

ErrorOr<Optional<String>> AddMemberDialog::show(GUI::Window* parent_window, Vector<String> const& available_usernames)
{
    auto dialog = TRY(GUI::Dialog::try_create(parent_window));
    dialog->set_title("Add Member");
    dialog->resize(260, 64);
    dialog->set_resizable(false);

    auto widget = TRY(AddMemberDialog::try_create());
    widget->m_user_combobox->set_model(*GUI::ItemListModel<String>::create(available_usernames));
    widget->m_user_combobox->set_only_allow_values_from_model(true);
    widget->m_user_combobox->set_selected_index(0);
    dialog->set_main_widget(widget);

    auto& ok_button = *widget->find_descendant_of_type_named<GUI::Button>("ok_button");
    ok_button.on_click = [&dialog](auto) { dialog->done(GUI::Dialog::ExecResult::OK); };
    ok_button.set_default(true);

    auto& cancel_button = *widget->find_descendant_of_type_named<GUI::Button>("cancel_button");
    cancel_button.on_click = [&dialog](auto) { dialog->done(GUI::Dialog::ExecResult::Cancel); };

    if (dialog->exec() != GUI::Dialog::ExecResult::OK)
        return OptionalNone {};

    return available_usernames[widget->m_user_combobox->selected_index()];
}

}
