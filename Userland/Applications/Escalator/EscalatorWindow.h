/*
 * Copyright (c) 2022, Ashley N. <dev-serenity@ne0ndrag0n.com>
 * Copyright (c) 2022, the SerenityOS developers.
 * Copyright (c) 2023, Fabian Dellwing <fabian@dellwing.net>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StringView.h>
#include <AK/Vector.h>
#include <LibCore/Account.h>
#include <LibCore/EventReceiver.h>
#include <LibGUI/Button.h>
#include <LibGUI/ImageWidget.h>
#include <LibGUI/TextBox.h>
#include <LibGUI/Window.h>

namespace Escalator {
class EscalatorWindow final : public GUI::Window {
    C_OBJECT(EscalatorWindow)
public:
    struct Options {
        StringView description;
        Core::Account current_user;
        bool preserve_env { false };
        bool forward_stdin { false };
        bool forward_stdout { false };
    };

    virtual ~EscalatorWindow() override = default;

    ErrorOr<void> execute_command();

private:
    EscalatorWindow(StringView executable, Vector<StringView> arguments, Options const& options);

    bool check_password();

    Vector<StringView> m_arguments;
    StringView m_executable;
    Core::Account m_current_user;
    bool m_preserve_env { false };
    bool m_forward_stdin { false };
    bool m_forward_stdout { false };

    RefPtr<GUI::ImageWidget> m_icon_image_widget;
    RefPtr<GUI::Button> m_ok_button;
    RefPtr<GUI::Button> m_cancel_button;
    RefPtr<GUI::PasswordBox> m_password_input;
};
}
