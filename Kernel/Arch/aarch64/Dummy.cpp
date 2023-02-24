/*
 * Copyright (c) 2021, Nico Weber <thakis@chromium.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Singleton.h>
#include <AK/Types.h>

#include <Kernel/Arch/Delay.h>
#include <Kernel/Bus/PCI/Initializer.h>
#include <Kernel/Sections.h>
#include <Kernel/Tasks/Process.h>
#include <Kernel/kstdio.h>

// Delay.cpp
namespace Kernel {

void microseconds_delay(u32)
{
    TODO_AARCH64();
}

}

// Initializer.cpp
namespace Kernel::PCI {

bool g_pci_access_io_probe_failed { false };
bool g_pci_access_is_disabled_from_commandline { true };

void initialize()
{
    dbgln("PCI: FIXME: Enable PCI for aarch64 platforms");
    g_pci_access_io_probe_failed = true;
}

}

// kprintf.cpp
void set_serial_debug_enabled(bool)
{
    dbgln("FIXME: Add support for changing state of serial debugging");
}
