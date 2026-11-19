/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 * Copyright (c) 2022, Timon Kruiper <timonkruiper@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NeverDestroyed.h>
#include <Kernel/Arch/aarch64/InterruptManagement.h>
#include <Kernel/Arch/aarch64/Interrupts/GICv2.h>
#include <Kernel/Firmware/ACPI/Parser.h>
#include <Kernel/Library/Panic.h>

namespace Kernel {

static NeverDestroyed<Vector<NonnullLockRefPtr<IRQController>>> s_interrupt_controllers;
static InterruptManagement* s_interrupt_management;

bool InterruptManagement::initialized()
{
    return s_interrupt_management != nullptr;
}

InterruptManagement& InterruptManagement::the()
{
    VERIFY(InterruptManagement::initialized());
    return *s_interrupt_management;
}

static ErrorOr<void> find_interrupt_controllers_in_acpi_madt_table(PhysicalAddress madt_table_paddr)
{
    auto madt = TRY(Memory::map_typed<::ACPI::Structures::MADT const>(madt_table_paddr));

    if (madt->h.length < sizeof(::ACPI::Structures::MADT)) {
        dmesgln("ACPI: MADT table is too small");
        return EINVAL;
    }

    auto entry_array_size = madt->h.length - offsetof(::ACPI::Structures::MADT, entries);

    Optional<::ACPI::Structures::MADTEntries::GICCPUInterface> gic_boot_cpu_interface;
    Optional<::ACPI::Structures::MADTEntries::GICDistributor> gic_distributor;

    auto boot_cpu_mpidr = Aarch64::MPIDR_EL1::read();

    auto current_entry_vaddr = VirtualAddress { madt->entries };
    while (entry_array_size > 0) {
        auto const* entry_header = reinterpret_cast<::ACPI::Structures::MADTEntryHeader const*>(current_entry_vaddr.as_ptr());

        switch (entry_header->type) {
            using enum ::ACPI::Structures::MADTEntryType;

        case GIC_CPU_Interface: {
            auto const* entry = reinterpret_cast<::ACPI::Structures::MADTEntries::GICCPUInterface const*>(entry_header);

            auto mpidr = entry->mpidr;
            if ((((mpidr >> ::ACPI::Structures::MADTEntries::GICCPUInterface::MPDIR_AFF0_OFFSET) & 0xff) == boot_cpu_mpidr.Aff0)
                && (((mpidr >> ::ACPI::Structures::MADTEntries::GICCPUInterface::MPDIR_AFF1_OFFSET) & 0xff) == boot_cpu_mpidr.Aff1)
                && (((mpidr >> ::ACPI::Structures::MADTEntries::GICCPUInterface::MPDIR_AFF2_OFFSET) & 0xff) == boot_cpu_mpidr.Aff2)
                && (((mpidr >> ::ACPI::Structures::MADTEntries::GICCPUInterface::MPDIR_AFF3_OFFSET) & 0xff) == boot_cpu_mpidr.Aff3)) {
                gic_boot_cpu_interface = *entry;
            }

            break;
        }

        case GIC_Distributor: {
            auto const* entry = reinterpret_cast<::ACPI::Structures::MADTEntries::GICDistributor const*>(entry_header);

            if (gic_distributor.has_value()) {
                // https://uefi.org/specs/ACPI/6.6/05_ACPI_Software_Programming_Model.html#gic-distributor-gicd-structure
                // "One, and only one, GIC distributor structure must be present in the MADT for an ARM based system."
                dmesgln("ACPI: Multiple GIC distributor structures found in the MADT table");
                return EINVAL;
            }

            gic_distributor = *entry;

            break;
        }

        default:
            dbgln("ACPI: Unsupported AArch64 MADT table entry type: {:#x}", to_underlying(entry_header->type));
            break;
        }

        current_entry_vaddr = current_entry_vaddr.offset(entry_header->length);
        entry_array_size -= entry_header->length;
    }

    // https://uefi.org/specs/ACPI/6.6/05_ACPI_Software_Programming_Model.html#gic-cpu-interface-gicc-structure
    // "In the GIC interrupt model, logical processors are required to have a Processor Device object in the DSDT,
    //  and must convey each processor’s GIC information to the OS using the GICC structure."
    if (!gic_boot_cpu_interface.has_value()) {
        dmesgln("ACPI: No GIC CPU Interface MADT entry for the boot processor");
        return EINVAL;
    }

    if (!has_flag(gic_boot_cpu_interface->flags, ::ACPI::Structures::MADTEntries::GICCPUInterface::Flags::Enabled)) {
        dmesgln("ACPI: GIC CPU Interface MADT entry for the boot processor is not enabled");
        return EINVAL;
    }

    // https://uefi.org/specs/ACPI/6.6/05_ACPI_Software_Programming_Model.html#gic-distributor-gicd-structure
    // "One, and only one, GIC distributor structure must be present in the MADT for an ARM based system."
    if (!gic_distributor.has_value()) {
        dmesgln("ACPI: No GIC Distributor MADT entry found");
        return EINVAL;
    }

    if (gic_distributor->gic_version == 0) {
        // https://uefi.org/specs/ACPI/6.6/05_ACPI_Software_Programming_Model.html#gic-distributor-gicd-structure
        // "0x00: No GIC version is specified, fall back to hardware discovery for GIC version"
        dmesgln("ACPI: FIXME: GIC Distributor MADT entry doesn't specify the GIC version; this is currently not supported");
        return ENOTSUP;
    }

    if (gic_distributor->gic_version == 2) {
        auto distributor_paddr = PhysicalAddress { gic_distributor->physical_base_address };

        // https://uefi.org/specs/ACPI/6.6/05_ACPI_Software_Programming_Model.html#gic-cpu-interface-gicc-structure
        // "On GICv1/v2 systems and GICv3/4 systems in GICv2 compatibility mode, this field holds the 64-bit
        //  physical address at which the processor can access this GIC CPU Interface. If provided here, the “Local
        //  Interrupt Controller Address” field in the MADT must be ignored by the OSPM."
        auto boot_cpu_interface_paddr = PhysicalAddress { gic_boot_cpu_interface->physical_base_address };
        if (boot_cpu_interface_paddr.is_null())
            boot_cpu_interface_paddr = PhysicalAddress { madt->local_interrupt_controller_address };

        auto gic = TRY(GICv2::try_to_initialize(distributor_paddr, boot_cpu_interface_paddr));
        TRY(s_interrupt_controllers->try_append(move(gic)));
    } else {
        dmesgln("ACPI: Unsupported GIC Distributor version: {}", gic_distributor->gic_version);
        return ENOTSUP;
    }

    return {};
}

void InterruptManagement::initialize()
{
    VERIFY(!InterruptManagement::initialized());
    s_interrupt_management = new InterruptManagement;

    if (ACPI::is_enabled()) {
        auto madt_table_paddr = ACPI::Parser::the()->find_table("APIC"sv);
        if (madt_table_paddr.has_value())
            find_interrupt_controllers_in_acpi_madt_table(madt_table_paddr.release_value()).release_value_but_fixme_should_propagate_errors();
    }

    if (s_interrupt_controllers->is_empty())
        PANIC("InterruptManagement: No supported interrupt controller was found");
}

ErrorOr<void> InterruptManagement::register_interrupt_controller(NonnullLockRefPtr<IRQController> interrupt_controller)
{
    // This function has to be called before InterruptManagement is initialized,
    // as we do not support dynamic registration of interrupt controllers.
    VERIFY(!initialized());

    return s_interrupt_controllers->try_append(move(interrupt_controller));
}

InterruptNumber InterruptManagement::acquire_mapped_interrupt_number(InterruptNumber interrupt_number)
{
    return interrupt_number;
}

Vector<NonnullLockRefPtr<IRQController>> const& InterruptManagement::controllers()
{
    return *s_interrupt_controllers;
}

NonnullLockRefPtr<IRQController> InterruptManagement::get_responsible_irq_controller(InterruptNumber)
{
    // TODO: Support more interrupt controllers
    VERIFY(s_interrupt_controllers->size() == 1);
    return (*s_interrupt_controllers)[0];
}

void InterruptManagement::enumerate_interrupt_handlers(Function<void(GenericInterruptHandler&)>)
{
    TODO_AARCH64();
}

}
