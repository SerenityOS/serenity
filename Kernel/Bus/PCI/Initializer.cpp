/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Initializer.h>
#include <Kernel/Bus/PCI/SysFSPCI.h>
#include <Kernel/CommandLine.h>
#include <Kernel/Firmware/ACPI/Parser.h>
#include <Kernel/IO.h>
#include <Kernel/Panic.h>
#include <Kernel/Sections.h>

namespace Kernel {
namespace PCI {

static bool test_pci_io();

UNMAP_AFTER_INIT static PCIAccessLevel detect_optimal_access_type(PCIAccessLevel boot_determined)
{
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
    auto boot_determined = kernel_command_line().pci_access_level();

    switch (detect_optimal_access_type(boot_determined)) {
    case PCIAccessLevel::MemoryAddressing: {
        auto mcfg = ACPI::Parser::the()->find_table("MCFG");
        VERIFY(mcfg.has_value());
        auto success = Access::initialize_for_memory_access(mcfg.value());
        VERIFY(success);
        break;
    }
    case PCIAccessLevel::IOAddressing: {
        auto success = Access::initialize_for_io_access();
        VERIFY(success);
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }

    PCI::PCIBusSysFSDirectory::initialize();

    PCI::enumerate([&](const Address& address, ID id) {
        dmesgln("{} {}", address, id);
    });
}

UNMAP_AFTER_INIT bool test_pci_io()
{
    dmesgln("Testing PCI via manual probing...");
    u32 tmp = 0x80000000;
    IO::out32(PCI_ADDRESS_PORT, tmp);
    tmp = IO::in32(PCI_ADDRESS_PORT);
    if (tmp == 0x80000000) {
        dmesgln("PCI IO supported");
        return true;
    }

    dmesgln("PCI IO not supported");
    return false;
}

}
}
