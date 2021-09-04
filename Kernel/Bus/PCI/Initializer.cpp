/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/ACPI/Parser.h>
#include <Kernel/Bus/PCI/IOAccess.h>
#include <Kernel/Bus/PCI/Initializer.h>
#include <Kernel/Bus/PCI/MMIOAccess.h>
#include <Kernel/Bus/PCI/WindowedMMIOAccess.h>
#include <Kernel/CommandLine.h>
#include <Kernel/IO.h>
#include <Kernel/Panic.h>
#include <Kernel/Sections.h>

namespace Kernel {
namespace PCI {

static bool test_pci_io();

UNMAP_AFTER_INIT static PCIAccessLevel detect_optimal_access_type(PCIAccessLevel boot_determined)
{
    if (!ACPI::is_enabled() || ACPI::Parser::the()->find_table("MCFG").is_null())
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
    case PCIAccessLevel::MappingPerDevice:
        WindowedMMIOAccess::initialize(ACPI::Parser::the()->find_table("MCFG"));
        break;
    case PCIAccessLevel::MappingPerBus:
        MMIOAccess::initialize(ACPI::Parser::the()->find_table("MCFG"));
        break;
    case PCIAccessLevel::IOAddressing:
        IOAccess::initialize();
        break;
    default:
        VERIFY_NOT_REACHED();
    }

    PCI::PCIBusSysFSDirectory::initialize();

    PCI::enumerate([&](Address const& address, ID id) {
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
