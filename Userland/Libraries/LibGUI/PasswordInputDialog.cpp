/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Button.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/PasswordInputDialog.h>
#include <LibGUI/PasswordInputDialogWidget.h>
#include <LibGUI/TextBox.h>

namespace GUI {

PasswordInputDialog::PasswordInputDialog(Window* parent_window, ByteString title, ByteString server, ByteString username)
    : Dialog(parent_window)
{
    if (parent_window)
        set_icon(parent_window->icon());
    set_resizable(false);
    resize(340, 122);
    set_title(move(title));

    auto widget = PasswordInputDialogWidget::try_create().release_value_but_fixme_should_propagate_errors();
    set_main_widget(widget);

    auto& key_icon = *widget->find_descendant_of_type_named<GUI::ImageWidget>("key_icon");

    key_icon.set_bitmap(Gfx::Bitmap::load_from_file("/res/icons/32x32/key.png"sv).release_value_but_fixme_should_propagate_errors());

    auto& server_label = *widget->find_descendant_of_type_named<GUI::Label>("server_label");
    server_label.set_text(String::from_byte_string(server).release_value_but_fixme_should_propagate_errors());

    auto& username_label = *widget->find_descendant_of_type_named<GUI::Label>("username_label");
    username_label.set_text(String::from_byte_string(username).release_value_but_fixme_should_propagate_errors());

    auto& password_box = *widget->find_descendant_of_type_named<GUI::PasswordBox>("password_box");

    auto& ok_button = *widget->find_descendant_of_type_named<GUI::Button>("ok_button");
    ok_button.on_click = [&](auto) {
        dbgln("GUI::PasswordInputDialog: OK button clicked");
        m_password = password_box.text();
        done(ExecResult::OK);
    };
    ok_button.set_default(true);

    auto& cancel_button = *widget->find_descendant_of_type_named<GUI::Button>("cancel_button");
    cancel_button.on_click = [this](auto) {
        dbgln("GUI::PasswordInputDialog: Cancel button clicked");
        done(ExecResult::Cancel);
    };

    password_box.on_escape_pressed = [&] {
        cancel_button.click();
    };
    password_box.set_focus(true);
}

Dialog::ExecResult PasswordInputDialog::show(Window* parent_window, ByteString& text_value, ByteString title, ByteString server, ByteString username)
{
    auto box = PasswordInputDialog::construct(parent_window, move(title), move(server), move(username));
    auto result = box->exec();
    text_value = box->m_password;
    return result;
}

}
