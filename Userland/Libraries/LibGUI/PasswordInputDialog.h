/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 * Copyright (c) 2022, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Dialog.h>

namespace GUI {

class PasswordInputDialog : public Dialog {
    C_OBJECT(PasswordInputDialog);

public:
    virtual ~PasswordInputDialog() override = default;

    static ExecResult show(Window* parent_window, String& text_value, String title, String server, String username);

private:
    explicit PasswordInputDialog(Window* parent_window, String title, String server, String username);

    String m_password;
};

}
