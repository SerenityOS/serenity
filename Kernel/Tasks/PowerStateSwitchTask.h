/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <Kernel/Forward.h>

namespace Kernel {

enum class PowerStateCommand : uintptr_t {
    Shutdown,
    Reboot,
};
// We will pass the power state command to the task in place of a void* as to avoid the complications of raw allocations.
static_assert(sizeof(PowerStateCommand) == sizeof(void*));

extern bool g_in_system_shutdown;

class PowerStateSwitchTask {
public:
    static void shutdown() { spawn(PowerStateCommand::Shutdown); }
    static void reboot() { spawn(PowerStateCommand::Reboot); }

private:
    static void spawn(PowerStateCommand);

    static void power_state_switch_task(void* raw_entry_data);

    static ErrorOr<void> kill_all_user_processes();
    enum class DoReboot {
        No,
        Yes,
    };
    static ErrorOr<void> perform_shutdown(DoReboot);
};

}
