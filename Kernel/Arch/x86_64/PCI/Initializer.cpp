/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Arch/Interrupts.h>
#include <Kernel/Arch/x86_64/IO.h>
#include <Kernel/Arch/x86_64/PCI/Controller/PIIX4HostBridge.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Controller/MemoryBackedHostBridge.h>
#include <Kernel/Bus/PCI/Initializer.h>
#include <Kernel/Firmware/ACPI/Parser.h>
#include <Kernel/Library/Panic.h>
#include <Kernel/Sections.h>

namespace Kernel::PCI {

READONLY_AFTER_INIT bool g_pci_access_io_probe_failed;
READONLY_AFTER_INIT bool g_pci_access_is_disabled_from_commandline;

static bool test_pci_io();

UNMAP_AFTER_INIT static PCIAccessLevel detect_optimal_access_type()
{
    auto boot_determined = kernel_command_line().pci_access_level();
    if (!ACPI::is_enabled() || !ACPI::Parser::the()->find_table("MCFG"sv).has_value())
        return PCIAccessLevel::IOAddressing;

    if (boot_determined != PCIAccessLevel::IOAddressing)
        return boot_determined;

    if (!g_pci_access_io_probe_failed)
        return PCIAccessLevel::IOAddressing;

    PANIC("No PCI bus access method detected!");
}

UNMAP_AFTER_INIT static ErrorOr<void> find_and_register_pci_host_bridges_from_acpi_mcfg_table(PhysicalAddress mcfg_table)
{
    VERIFY(Access::is_initialized());
    u32 length = 0;
    u8 revision = 0;
    {
        auto mapped_mcfg_table_or_error = Memory::map_typed<ACPI::Structures::SDTHeader>(mcfg_table);
        if (mapped_mcfg_table_or_error.is_error()) {
            dbgln("Failed to map MCFG table");
            return ENXIO;
        }
        auto mapped_mcfg_table = mapped_mcfg_table_or_error.release_value();
        length = mapped_mcfg_table->length;
        revision = mapped_mcfg_table->revision;
    }

    if (length == sizeof(ACPI::Structures::SDTHeader))
        return EOVERFLOW;

    dbgln("PCI: MCFG, length: {}, revision: {}", length, revision);

    if (Checked<size_t>::addition_would_overflow(length, PAGE_SIZE)) {
        dbgln("Overflow when adding extra page to allocation of length {}", length);
        return EOVERFLOW;
    }
    length += PAGE_SIZE;
    auto region_size_or_error = Memory::page_round_up(length);
    if (region_size_or_error.is_error()) {
        dbgln("Failed to round up length of {} to pages", length);
        return EOVERFLOW;
    }
    auto mcfg_region_or_error = MM.allocate_kernel_region(mcfg_table.page_base(), region_size_or_error.value(), "PCI Parsing MCFG"sv, Memory::Region::Access::ReadWrite);
    if (mcfg_region_or_error.is_error())
        return ENOMEM;
    auto& mcfg = *(ACPI::Structures::MCFG*)mcfg_region_or_error.value()->vaddr().offset(mcfg_table.offset_in_page()).as_ptr();
    dbgln_if(PCI_DEBUG, "PCI: Checking MCFG @ {}, {}", VirtualAddress(&mcfg), mcfg_table);
    for (u32 index = 0; index < ((mcfg.header.length - sizeof(ACPI::Structures::MCFG)) / sizeof(ACPI::Structures::PCI_MMIO_Descriptor)); index++) {
        u8 start_bus = mcfg.descriptors[index].start_pci_bus;
        u8 end_bus = mcfg.descriptors[index].end_pci_bus;
        u64 start_addr = mcfg.descriptors[index].base_addr;

        Domain pci_domain { index, start_bus, end_bus };
        dmesgln("PCI: New PCI domain @ {}, PCI buses ({}-{})", PhysicalAddress { start_addr }, start_bus, end_bus);
        auto host_bridge = TRY(PCI::MemoryBackedHostBridge::create(pci_domain, nullptr, PhysicalAddress { start_addr }));
        TRY(PCI::Access::the().add_host_controller_and_scan_for_devices(move(host_bridge)));
    }

    return {};
}

UNMAP_AFTER_INIT static ErrorOr<void> initialize_for_one_pci_domain()
{
    VERIFY(Access::is_initialized());
    auto host_bridge = TRY(PCI::PIIX4HostBridge::create());
    TRY(PCI::Access::the().add_host_controller_and_scan_for_devices(move(host_bridge)));
    dbgln_if(PCI_DEBUG, "PCI: access for one PCI domain initialised.");
    return {};
}

UNMAP_AFTER_INIT void initialize()
{
    g_pci_access_is_disabled_from_commandline = kernel_command_line().is_pci_disabled();
    Optional<PhysicalAddress> possible_mcfg;
    // FIXME: There are other arch-specific methods to find the memory range
    // for accessing the PCI configuration space.
    // For example, the QEMU microvm machine type might expose an FDT so we could
    // parse it to find a PCI host bridge.
    if (ACPI::is_enabled()) {
        possible_mcfg = ACPI::Parser::the()->find_table("MCFG"sv);
        g_pci_access_io_probe_failed = (!test_pci_io()) && (!possible_mcfg.has_value());
    } else {
        g_pci_access_io_probe_failed = !test_pci_io();
    }
    if (g_pci_access_is_disabled_from_commandline || g_pci_access_io_probe_failed)
        return;

    PCI::Access::initialize();
    switch (detect_optimal_access_type()) {
    case PCIAccessLevel::MemoryAddressing: {
        VERIFY(possible_mcfg.has_value());
        MUST(find_and_register_pci_host_bridges_from_acpi_mcfg_table(possible_mcfg.value()));
        break;
    }
    case PCIAccessLevel::IOAddressing: {
        MUST(initialize_for_one_pci_domain());
        break;
    }
    default:
        VERIFY_NOT_REACHED();
    }
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
