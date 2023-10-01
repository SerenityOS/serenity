/*
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Button.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/Label.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Window.h>

namespace LoginServer {

class LoginWindow final : public GUI::Window {
    C_OBJECT(LoginWindow);

public:
    virtual ~LoginWindow() override = default;

    Function<void()> on_submit;

    ByteString username() const { return m_username->text(); }
    void set_username(StringView username) { m_username->set_text(username); }

    ByteString password() const { return m_password->text(); }
    void set_password(StringView password) { m_password->set_text(password); }

    void set_fail_message(StringView message) { m_fail_message->set_text(String::from_utf8(message).release_value_but_fixme_should_propagate_errors()); }

private:
    LoginWindow(GUI::Window* parent = nullptr);

    RefPtr<GUI::ImageWidget> m_banner;
    RefPtr<GUI::TextBox> m_username;
    RefPtr<GUI::PasswordBox> m_password;
    RefPtr<GUI::Label> m_fail_message;
    RefPtr<GUI::Button> m_log_in_button;
};

}
