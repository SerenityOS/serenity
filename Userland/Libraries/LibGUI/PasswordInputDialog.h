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

    static ExecResult show(Window* parent_window, DeprecatedString& text_value, DeprecatedString title, DeprecatedString server, DeprecatedString username);

private:
    explicit PasswordInputDialog(Window* parent_window, DeprecatedString title, DeprecatedString server, DeprecatedString username);

    DeprecatedString m_password;
};

}
