/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/Initializer.h>
#include <Kernel/Library/Assertions.h>

namespace Kernel::PCI {

bool g_pci_access_io_probe_failed { false };
bool g_pci_access_is_disabled_from_commandline { true };

void initialize()
{
    TODO_RISCV64();
}

}
