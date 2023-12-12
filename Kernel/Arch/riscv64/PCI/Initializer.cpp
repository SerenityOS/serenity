/*
 * Copyright (c) 2023, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Format.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Controller/MemoryBackedHostBridge.h>
#include <Kernel/Bus/PCI/Initializer.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Bus/PCI/BusDirectory.h>
#include <Kernel/Library/Assertions.h>

namespace Kernel::PCI {

READONLY_AFTER_INIT bool g_pci_access_io_probe_failed { false };
READONLY_AFTER_INIT bool g_pci_access_is_disabled_from_commandline { true };

UNMAP_AFTER_INIT void initialize()
{
    g_pci_access_is_disabled_from_commandline = kernel_command_line().is_pci_disabled();

    if (g_pci_access_is_disabled_from_commandline) {
        return;
    }

    auto const qemu_ecam_addr = PhysicalAddress { 0x3000'0000 };

    Access::initialize_for_one_pci_domain(qemu_ecam_addr);

    PCIBusSysFSDirectory::initialize();

    MUST(PCI::enumerate([&](DeviceIdentifier const& device_identifier) {
        dmesgln("{} {}", device_identifier.address(), device_identifier.hardware_id());
    }));
}

}
