/*
 * Copyright (c) 2024, Aryan Baburajan <aryanbaburajan2007@gmail.com>.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <LibGUI/Frame.h>

namespace GUI {

class PasswordInputDialogWidget : public GUI::Widget {
    C_OBJECT_ABSTRACT(PasswordInputDialogWidget)

public:
    static ErrorOr<NonnullRefPtr<PasswordInputDialogWidget>> try_create();
    virtual ~PasswordInputDialogWidget() override = default;

private:
    PasswordInputDialogWidget() = default;
};

}
