/*
 * Copyright (c) 2023, kleines Filmröllchen <filmroellchen@serenityos.org>
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <Kernel/Forward.h>

namespace Kernel {

extern bool g_in_system_shutdown;

class PowerManagementTask {
public:
    static void shutdown();
    static void reboot();
    static void spawn();

private:
    static void task(void*);

    enum class DoReboot {
        No,
        Yes,
    };
    static void perform_shutdown(DoReboot do_reboot);
};

}
