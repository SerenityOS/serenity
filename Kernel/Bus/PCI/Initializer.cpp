/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/x86/IO.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Initializer.h>
#include <Kernel/Bus/PCI/SysFSPCI.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Firmware/ACPI/Parser.h>
#include <Kernel/Panic.h>
#include <Kernel/Sections.h>

namespace Kernel {
namespace PCI {

static bool test_pci_io();

UNMAP_AFTER_INIT static PCIAccessLevel detect_optimal_access_type()
{
    auto boot_determined = kernel_command_line().pci_access_level();
    if (!ACPI::is_enabled() || !ACPI::Parser::the()->find_table("MCFG").has_value())
        return PCIAccessLevel::IOAddressing;

    if (boot_determined != PCIAccessLevel::IOAddressing)
        return boot_determined;

    if (test_pci_io())
        return PCIAccessLevel::IOAddressing;

    PANIC("No PCI bus access method detected!");
}

UNMAP_AFTER_INIT void initialize()
{
    switch (detect_optimal_access_type()) {
    case PCIAccessLevel::MemoryAddressing: {
        auto mcfg = ACPI::Parser::the()->find_table("MCFG");
        VERIFY(mcfg.has_value());
        auto success = Access::initialize_for_multiple_pci_domains(mcfg.value());
        VERIFY(success);
        break;
    }
    case PCIAccessLevel::IOAddressing: {
        auto success = Access::initialize_for_one_pci_domain();
        VERIFY(success);
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }

    PCI::PCIBusSysFSDirectory::initialize();

    PCI::enumerate([&](DeviceIdentifier const& device_identifier) {
        dmesgln("{} {}", device_identifier.address(), device_identifier.hardware_id());
    });
}

UNMAP_AFTER_INIT bool test_pci_io()
{
    dmesgln("Testing PCI via manual probing...");
    u32 tmp = 0x80000000;
    IO::out32(PCI::address_port, tmp);
    tmp = IO::in32(PCI::address_port);
    if (tmp == 0x80000000) {
        dmesgln("PCI IO supported");
        return true;
    }

    dmesgln("PCI IO not supported");
    return false;
}

}
}
