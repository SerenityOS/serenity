/*
 * Copyright (c) 2023, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NeverDestroyed.h>
#include <Kernel/Arch/Interrupts.h>
#include <Kernel/Arch/riscv64/InterruptManagement.h>
#include <Kernel/Arch/riscv64/Interrupts/PLIC.h>
#include <Kernel/Firmware/ACPI/Parser.h>
#include <Kernel/Interrupts/SharedIRQHandler.h>
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

    Optional<::ACPI::Structures::MADTEntries::RISCVInterruptController> boot_hart_interrupt_controller_entry;
    Optional<::ACPI::Structures::MADTEntries::RISCVPlatformLevelInterruptController> plic_entry;
    bool have_aplic_entry = false; // FIXME: Support APLICs.

    auto current_entry_vaddr = VirtualAddress { madt->entries };
    while (entry_array_size > 0) {
        auto const* entry_header = reinterpret_cast<::ACPI::Structures::MADTEntryHeader const*>(current_entry_vaddr.as_ptr());

        switch (entry_header->type) {
            using enum ::ACPI::Structures::MADTEntryType;

        case RISCV_Hart_Local_Interrupt_Controller: {
            auto const* entry = reinterpret_cast<::ACPI::Structures::MADTEntries::RISCVInterruptController const*>(entry_header);

            if (entry->hart_id == g_boot_info.arch_specific.boot_hart_id)
                boot_hart_interrupt_controller_entry = *entry;

            break;
        }

        case RISCV_Platform_Level_Interrupt_Controller: {
            auto const* entry = reinterpret_cast<::ACPI::Structures::MADTEntries::RISCVPlatformLevelInterruptController const*>(entry_header);

            if (plic_entry.has_value()) {
                dbgln("ACPI: FIXME: MADT table has multiple PLICs; this is currently not supported");
            } else {
                plic_entry = *entry;
            }

            break;
        }

        default:
            dbgln("ACPI: Unsupported RISC-V MADT table entry type: {:#x}", to_underlying(entry_header->type));

            if (entry_header->type == RISCV_Advanced_Platform_Level_Interrupt_Controller)
                have_aplic_entry = true;

            break;
        }

        current_entry_vaddr = current_entry_vaddr.offset(entry_header->length);
        entry_array_size -= entry_header->length;
    }

    // https://uefi.org/specs/ACPI/6.6/05_ACPI_Software_Programming_Model.html#risc-v-interrupt-controller-rintc-structure
    // "Each hart in the system is required to have a RINTC record in the MADT, and a processor
    //  device object in the DSDT."
    if (!boot_hart_interrupt_controller_entry.has_value()) {
        dmesgln("ACPI: No RISC-V Hart Local Interrupt Controller MADT entry for the boot hart");
        return EINVAL;
    }

    if (!has_flag(boot_hart_interrupt_controller_entry->flags, ::ACPI::Structures::MADTEntries::RISCVInterruptController::Flags::Enabled)) {
        dmesgln("ACPI: RISC-V Hart Local Interrupt Controller MADT entry for the boot hart is not enabled");
        return EINVAL;
    }

    if (plic_entry.has_value() && have_aplic_entry) {
        dmesgln("ACPI: MADT table has both an APLIC and a PLIC entry; this is not supported");
        return EINVAL;
    }

    if (plic_entry.has_value()) {
        // FIXME: Properly support GSIs on RISC-V.
        VERIFY(plic_entry->global_system_interrupt_base == 0);

        auto boot_hart_supervisor_mode_context_id = boot_hart_interrupt_controller_entry->external_interrupt_controller_id.plic.s_mode_context_id;

        auto plic_registers_paddr = PhysicalAddress { plic_entry->plic_address };
        auto plic_interrupt_count = plic_entry->total_external_interrupt_sources_supported;

        auto registers_mapping = TRY(Memory::map_typed_writable<PLIC::RegisterMap volatile>(plic_registers_paddr));
        auto plic = TRY(adopt_nonnull_lock_ref_or_enomem(new (nothrow) PLIC(move(registers_mapping), plic_interrupt_count, boot_hart_supervisor_mode_context_id)));
        TRY(s_interrupt_controllers->try_append(move(plic)));
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

InterruptNumber InterruptManagement::acquire_mapped_interrupt_number(InterruptNumber original_irq)
{
    return original_irq;
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

void InterruptManagement::enumerate_interrupt_handlers(Function<void(GenericInterruptHandler&)> callback)
{
    for (size_t i = 0; i < GENERIC_INTERRUPT_HANDLERS_COUNT; i++) {
        auto& handler = get_interrupt_handler(i);
        if (handler.type() == HandlerType::SharedIRQHandler) {
            static_cast<SharedIRQHandler&>(handler).enumerate_handlers(callback);
            continue;
        }
        if (handler.type() != HandlerType::UnhandledInterruptHandler)
            callback(handler);
    }
}

}
