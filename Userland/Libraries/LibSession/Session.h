/*
 * Copyright (c) 2022, Nikita Utkin <shockck84@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <LibGUI/Application.h>

namespace Session {

class Session {
public:
    static Session& the();
    ~Session();
    void inhibit_exit();
    void allow_exit();
    bool is_exit_inhibited();
    void report_inhibited_exit_prevention();

    Function<void()> on_inhibited_exit_prevented;
};

}
