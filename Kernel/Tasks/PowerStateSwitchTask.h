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

/**
 * @brief Represents a task responsible for managing power state transitions.
 *
 * The `PowerStateSwitchTask` class provides functionality to handle
 * system power state changes, such as shutdown and reboot. It includes
 * methods to initiate these transitions and manages the underlying
 * task execution.
 */
class PowerStateSwitchTask {
public:
    static void shutdown() { spawn(PowerStateCommand::Shutdown); }
    static void reboot() { spawn(PowerStateCommand::Reboot); }

private:
    /**
     * @brief Spawns a new task to handle the specified power state command.
     *
     * This function is used to create a task that processes a given
     * `PowerStateCommand`, such as shutting down or rebooting the system.
     *
     * @param command The `PowerStateCommand` to be executed by the spawned task.
     */
    static void spawn(PowerStateCommand command);

    /**
     * @brief Entry point for the power state switch task.
     *
     * This function serves as the main entry point for the task responsible
     * for handling power state transitions, such as shutdown or reboot.
     *
     * @param raw_entry_data A pointer to raw data passed to the task. This
     * data is expected to contain the `PowerStateCommand` specifying the
     * desired power state operation.
     */
    static void power_state_switch_task(void* raw_entry_data);

    /**
     * @brief Terminates all user processes in the system.
     *
     * This function attempts to kill all user-space processes currently
     * running on the system. It is typically used during system shutdown
     * or reboot to ensure a clean state before transitioning power states.
     *
     * @return An `ErrorOr<void>` indicating success or failure of the operation.
     */
    static ErrorOr<void> kill_all_user_processes();

    enum class DoReboot {
        No,
        Yes,
    };

    static ErrorOr<void> perform_shutdown(DoReboot);
};

}
