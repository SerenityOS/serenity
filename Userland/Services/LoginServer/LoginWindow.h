/*
 * Copyright (c) 2021, Peter Elliott <pelliott@serenityos.org>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <LibGUI/Button.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Window.h>

#pragma once

class LoginWindow final : public GUI::Window {
    C_OBJECT(LoginWindow);

public:
    virtual ~LoginWindow() override { }

    Function<void()> on_submit;

    String username() const { return m_username->text(); }
    void set_username(StringView username) { m_username->set_text(username); }

    String password() const { return m_password->text(); }
    void set_password(StringView password) { m_password->set_text(password); }

private:
    LoginWindow(GUI::Window* parent = nullptr);

    RefPtr<GUI::ImageWidget> m_banner;
    RefPtr<GUI::TextBox> m_username;
    RefPtr<GUI::PasswordBox> m_password;
    RefPtr<GUI::Button> m_log_in_button;
};
