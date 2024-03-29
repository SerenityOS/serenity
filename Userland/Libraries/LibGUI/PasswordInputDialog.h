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
    ErrorOr<NonnullRefPtr<GUI::Widget>> try_create();

    virtual ~PasswordInputDialog() override = default;

    static ExecResult show(Window* parent_window, ByteString& text_value, ByteString title, ByteString server, ByteString username);

private:
    explicit PasswordInputDialog(Window* parent_window, ByteString title, ByteString server, ByteString username);

    ByteString m_password;
};

}
