/*
 * Copyright (c) 2020, the SerenityOS developers.
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Optional.h>
#include <AK/Vector.h>
#include <LibGUI/Dialog.h>

class ShutdownDialog : public GUI::Dialog {
    C_OBJECT(ShutdownDialog);

public:
    struct Command {
        StringView executable;
        Vector<char const*, 2> arguments;
    };

    static Optional<Command const&> show();

private:
    ShutdownDialog();
    virtual ~ShutdownDialog() override = default;

    int m_selected_option { -1 };
};
