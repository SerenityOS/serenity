/*
 * Copyright (c) 2023, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/SetOnce.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Initializer.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Bus/PCI/BusDirectory.h>
#include <Kernel/Firmware/ACPI/Parser.h>
#include <Kernel/Firmware/DeviceTree/DeviceTree.h>

namespace Kernel::PCI {

SetOnce g_pci_access_io_probe_failed;
SetOnce g_pci_access_is_disabled_from_commandline;

void initialize()
{
    if (kernel_command_line().is_pci_disabled()) {
        g_pci_access_is_disabled_from_commandline.set();
        return;
    }

    if (DeviceTree::is_present()) {
        // Host controllers defined in the devicetree are added before PCI::initialize() is called.
        Access::the().rescan_hardware();
    } else if (ACPI::is_enabled()) {
        auto mcfg_table_paddr = ACPI::Parser::the()->find_table("MCFG"sv);

        if (mcfg_table_paddr.has_value()) {
            auto success = Access::initialize_for_multiple_pci_domains(mcfg_table_paddr.value());
            if (!success) {
                dmesgln("PCI: Failed to initialize via ACPI MCFG table");
            }
        }
    }

    PCIBusSysFSDirectory::initialize();

    // FIXME: X86_64 Reserves Interrupts here, maybe we need to do something like this here as well

    MUST(PCI::enumerate([&](DeviceIdentifier const& device_identifier) {
        dmesgln("{} {}", device_identifier.address(), device_identifier.hardware_id());
    }));
}

}
