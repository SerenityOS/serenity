/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/SetOnce.h>
#include <Kernel/Arch/Interrupts.h>
#include <Kernel/Arch/x86_64/IO.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Initializer.h>
#include <Kernel/FileSystem/SysFS/Subsystems/Bus/PCI/BusDirectory.h>
#include <Kernel/Firmware/ACPI/Parser.h>
#include <Kernel/Library/Panic.h>
#include <Kernel/Sections.h>

namespace Kernel::PCI {

READONLY_AFTER_INIT SetOnce g_pci_access_io_probe_failed;
READONLY_AFTER_INIT SetOnce g_pci_access_is_disabled_from_commandline;

static bool test_pci_io();

UNMAP_AFTER_INIT static PCIAccessLevel detect_optimal_access_type()
{
    auto boot_determined = kernel_command_line().pci_access_level();
    if (!ACPI::is_enabled() || !ACPI::Parser::the()->find_table("MCFG"sv).has_value())
        return PCIAccessLevel::IOAddressing;

    if (boot_determined != PCIAccessLevel::IOAddressing)
        return boot_determined;

    if (!g_pci_access_io_probe_failed.was_set())
        return PCIAccessLevel::IOAddressing;

    PANIC("No PCI bus access method detected!");
}

UNMAP_AFTER_INIT void initialize()
{
    if (kernel_command_line().is_pci_disabled())
        g_pci_access_is_disabled_from_commandline.set();

    Optional<PhysicalAddress> possible_mcfg;
    // FIXME: There are other arch-specific methods to find the memory range
    // for accessing the PCI configuration space.
    // For example, the QEMU microvm machine type might expose an FDT so we could
    // parse it to find a PCI host bridge.
    if (ACPI::is_enabled()) {
        possible_mcfg = ACPI::Parser::the()->find_table("MCFG"sv);
        if ((!test_pci_io()) && (!possible_mcfg.has_value()))
            g_pci_access_io_probe_failed.set();
    } else {
        if (!test_pci_io())
            g_pci_access_io_probe_failed.set();
    }
    if (g_pci_access_is_disabled_from_commandline.was_set() || g_pci_access_io_probe_failed.was_set())
        return;
    switch (detect_optimal_access_type()) {
    case PCIAccessLevel::MemoryAddressing: {
        VERIFY(possible_mcfg.has_value());
        auto success = Access::initialize_for_multiple_pci_domains(possible_mcfg.value());
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

    PCIBusSysFSDirectory::initialize();

    // IRQ from pin-based interrupt should be set as reserved as soon as possible so that the PCI device
    // that chooses to use MSI(x) based interrupt can avoid sharing the IRQ with other devices.
    MUST(PCI::enumerate([&](DeviceIdentifier const& device_identifier) {
        // A simple sanity check to avoid getting a panic in get_interrupt_handler() before setting the IRQ as reserved.
        if (auto irq = device_identifier.interrupt_line().value(); irq < GENERIC_INTERRUPT_HANDLERS_COUNT) {
            auto& handler = get_interrupt_handler(irq);
            handler.set_reserved();
        }
    }));

    MUST(PCI::enumerate([&](DeviceIdentifier const& device_identifier) {
        dmesgln("{} {}", device_identifier.address(), device_identifier.hardware_id());
    }));
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
