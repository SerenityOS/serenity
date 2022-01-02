/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGUI/Dialog.h>

class ShutdownDialog : public GUI::Dialog {
    C_OBJECT(ShutdownDialog);

public:
    enum class ActionCode {
        None = 0,
        Shutdown = 1 << 0,
        Reboot = 1 << 1,
        Logout = 1 << 2,
    };

    static Vector<char const*> show(int disabled_actions = static_cast<int>(ActionCode::None));

private:
    ShutdownDialog(int disabled_actions = static_cast<int>(ActionCode::None));
    virtual ~ShutdownDialog() override;

    int m_selected_option { -1 };
};
