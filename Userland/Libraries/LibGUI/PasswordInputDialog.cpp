/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Button.h>
#include <LibGUI/Label.h>
#include <LibGUI/PasswordInputDialog.h>
#include <LibGUI/PasswordInputDialogGML.h>
#include <LibGUI/TextBox.h>

namespace GUI {

PasswordInputDialog::PasswordInputDialog(Window* parent_window, String title, String server, String username)
    : Dialog(parent_window)
{
    if (parent_window)
        set_icon(parent_window->icon());
    set_resizable(false);
    resize(340, 122);
    set_title(move(title));

    auto& widget = set_main_widget<Widget>();

    widget.load_from_gml(password_input_dialog_gml);

    auto& key_icon_label = *widget.find_descendant_of_type_named<GUI::Label>("key_icon_label");

    key_icon_label.set_icon(Gfx::Bitmap::try_load_from_file("/res/icons/32x32/key.png").release_value_but_fixme_should_propagate_errors());

    auto& server_label = *widget.find_descendant_of_type_named<GUI::Label>("server_label");
    server_label.set_text(move(server));

    auto& username_label = *widget.find_descendant_of_type_named<GUI::Label>("username_label");
    username_label.set_text(move(username));

    auto& password_box = *widget.find_descendant_of_type_named<GUI::PasswordBox>("password_box");

    auto& ok_button = *widget.find_descendant_of_type_named<GUI::Button>("ok_button");
    ok_button.on_click = [&](auto) {
        dbgln("GUI::PasswordInputDialog: OK button clicked");
        m_password = password_box.text();
        done(ExecOK);
    };

    auto& cancel_button = *widget.find_descendant_of_type_named<GUI::Button>("cancel_button");
    cancel_button.on_click = [this](auto) {
        dbgln("GUI::PasswordInputDialog: Cancel button clicked");
        done(ExecCancel);
    };

    password_box.on_return_pressed = [&] {
        ok_button.click();
    };
    password_box.on_escape_pressed = [&] {
        cancel_button.click();
    };
    password_box.set_focus(true);
}

PasswordInputDialog::~PasswordInputDialog()
{
}

int PasswordInputDialog::show(Window* parent_window, String& text_value, String title, String server, String username)
{
    auto box = PasswordInputDialog::construct(parent_window, move(title), move(server), move(username));
    auto result = box->exec();
    text_value = box->m_password;
    return result;
}

}
