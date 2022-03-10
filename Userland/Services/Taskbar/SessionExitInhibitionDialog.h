/*
 * Copyright (c) 2022, Nikita Utkin <shockck84@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Vector.h>
#include <LibGUI/Dialog.h>

class SessionExitInhibitionDialog : public GUI::Dialog {
    C_OBJECT(SessionExitInhibitionDialog);

public:
    enum ExecResult {
        ExecCancel,
        ExecIgnore,
    };

    static int show();

private:
    SessionExitInhibitionDialog();
    virtual ~SessionExitInhibitionDialog() override;

    int m_selected_option { -1 };
};
