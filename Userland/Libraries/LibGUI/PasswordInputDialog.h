/*
 * Copyright (c) 2021, Andreas Kling <kling@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Dialog.h>

namespace GUI {

class PasswordInputDialog : public Dialog {
    C_OBJECT(PasswordInputDialog);

public:
    virtual ~PasswordInputDialog() override;

    static int show(Window* parent_window, String& text_value, String title, String server, String username);

private:
    explicit PasswordInputDialog(Window* parent_window, String title, String server, String username);

    String m_password;
};

}
