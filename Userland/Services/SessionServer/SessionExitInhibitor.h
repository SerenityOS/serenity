/*
 * Copyright (c) 2022, Nikita Utkin <shockck84@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AK/Function.h"
#include "AK/Vector.h"

class SessionExitInhibitor {
public:
    static SessionExitInhibitor& the();
    bool is_exit_inhibited() { return !m_exit_inhibiting_client_ids.is_empty(); }
    void inhibit_exit(int client_id);
    void allow_exit(int client_id);
    void on_inhibited_exit_prevented();

private:
    Vector<int> m_exit_inhibiting_client_ids;
};
