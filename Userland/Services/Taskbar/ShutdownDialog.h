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
    static Vector<char const*> show();

private:
    ShutdownDialog();
    virtual ~ShutdownDialog() override = default;

    int m_selected_option { -1 };
};
