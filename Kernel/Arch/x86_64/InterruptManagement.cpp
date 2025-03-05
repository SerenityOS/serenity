/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/ByteReader.h>
#include <Kernel/API/Syscall.h>
#include <Kernel/Arch/Interrupts.h>
#include <Kernel/Arch/x86_64/InterruptManagement.h>
#include <Kernel/Arch/x86_64/Interrupts/APIC.h>
#include <Kernel/Arch/x86_64/Interrupts/IOAPIC.h>
#include <Kernel/Arch/x86_64/Interrupts/PIC.h>
#include <Kernel/Boot/CommandLine.h>
#include <Kernel/Firmware/ACPI/StaticParsing.h>
#include <Kernel/Interrupts/InterruptDisabler.h>
#include <Kernel/Interrupts/SharedIRQHandler.h>
#include <Kernel/Interrupts/SpuriousInterruptHandler.h>
#include <Kernel/Memory/TypedMapping.h>
#include <Kernel/Sections.h>

#define PCAT_COMPAT_FLAG 0x1

namespace Kernel {

static InterruptManagement* s_interrupt_management;

bool InterruptManagement::initialized()
{
    return (s_interrupt_management != nullptr);
}

InterruptManagement& InterruptManagement::the()
{
    VERIFY(InterruptManagement::initialized());
    return *s_interrupt_management;
}

UNMAP_AFTER_INIT void InterruptManagement::initialize()
{
    VERIFY(!InterruptManagement::initialized());
    s_interrupt_management = new InterruptManagement();
    if (kernel_command_line().is_smp_enabled_without_ioapic_enabled()) {
        dbgln("Can't enable SMP mode without IOAPIC mode being enabled");
    }
    if (!kernel_command_line().is_ioapic_enabled() && !kernel_command_line().is_smp_enabled())
        InterruptManagement::the().switch_to_pic_mode();
    else
        InterruptManagement::the().switch_to_ioapic_mode();
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

IRQController& InterruptManagement::get_interrupt_controller(size_t index)
{
    return *m_interrupt_controllers[index];
}

u8 InterruptManagement::acquire_mapped_interrupt_number(u8 original_irq)
{
    if (!InterruptManagement::initialized()) {
        // This is necessary, because we install UnhandledInterruptHandlers before we actually initialize the Interrupt Management object...
        return original_irq;
    }
    return InterruptManagement::the().get_mapped_interrupt_vector(original_irq);
}

u8 InterruptManagement::acquire_irq_number(u8 mapped_interrupt_vector)
{
    VERIFY(InterruptManagement::initialized());
    return InterruptManagement::the().get_irq_vector(mapped_interrupt_vector);
}

u8 InterruptManagement::get_mapped_interrupt_vector(u8 original_irq)
{
    // FIXME: For SMP configuration (with IOAPICs) use a better routing scheme to make redirections more efficient.
    return original_irq;
}

u8 InterruptManagement::get_irq_vector(u8 mapped_interrupt_vector)
{
    // FIXME: For SMP configuration (with IOAPICs) use a better routing scheme to make redirections more efficient.
    return mapped_interrupt_vector;
}

NonnullLockRefPtr<IRQController> InterruptManagement::get_responsible_irq_controller(IRQControllerType controller_type, u8 interrupt_vector)
{
    for (auto& irq_controller : m_interrupt_controllers) {
        if (irq_controller->gsi_base() <= interrupt_vector && irq_controller->type() == controller_type)
            return irq_controller;
    }
    VERIFY_NOT_REACHED();
}

NonnullLockRefPtr<IRQController> InterruptManagement::get_responsible_irq_controller(u8 interrupt_vector)
{
    if (m_interrupt_controllers.size() == 1 && m_interrupt_controllers[0]->type() == IRQControllerType::i8259) {
        return m_interrupt_controllers[0];
    }
    for (auto& irq_controller : m_interrupt_controllers) {
        if (irq_controller->gsi_base() <= interrupt_vector)
            if (!irq_controller->is_hard_disabled())
                return irq_controller;
    }
    VERIFY_NOT_REACHED();
}

UNMAP_AFTER_INIT ErrorOr<Optional<PhysicalAddress>> InterruptManagement::find_madt_physical_address()
{
    dbgln("Early access to ACPI tables for interrupt setup");
    auto possible_rsdp_physical_address = ACPI::StaticParsing::find_rsdp();
    if (!possible_rsdp_physical_address.has_value())
        return Optional<PhysicalAddress> {};
    auto possible_apic_physical_address = TRY(ACPI::StaticParsing::find_table(possible_rsdp_physical_address.value(), "APIC"sv));
    if (!possible_apic_physical_address.has_value())
        return Optional<PhysicalAddress> {};
    return possible_apic_physical_address.value();
}

UNMAP_AFTER_INIT InterruptManagement::InterruptManagement()
{
}

UNMAP_AFTER_INIT void InterruptManagement::switch_to_pic_mode()
{
    VERIFY(m_interrupt_controllers.is_empty());
    dmesgln("Interrupts: Switch to Legacy PIC mode");
    InterruptDisabler disabler;
    m_interrupt_controllers.append(adopt_lock_ref(*new PIC()));
    SpuriousInterruptHandler::initialize(7);
    SpuriousInterruptHandler::initialize(15);
    dbgln("Interrupts: Detected {}", m_interrupt_controllers[0]->model());
}

UNMAP_AFTER_INIT void InterruptManagement::switch_to_ioapic_mode()
{
    dmesgln("Interrupts: Switch to IOAPIC mode");
    InterruptDisabler disabler;

    m_madt_physical_address = MUST(find_madt_physical_address());

    if (!m_madt_physical_address.has_value()) {
        dbgln("Interrupts: ACPI MADT is not available, reverting to PIC mode");
        switch_to_pic_mode();
        return;
    }

    dbgln("Interrupts: MADT @ P {}", m_madt_physical_address.value().as_ptr());
    locate_apic_data();
    if (m_interrupt_controllers.size() == 1) {
        if (get_interrupt_controller(0).type() == IRQControllerType::i8259) {
            dmesgln("Interrupts: NO IOAPIC detected, Reverting to PIC mode.");
            return;
        }
    }
    for (auto& irq_controller : m_interrupt_controllers) {
        VERIFY(irq_controller);
        if (irq_controller->type() == IRQControllerType::i8259) {
            irq_controller->hard_disable();
            dbgln("Interrupts: Detected {} - Disabled", irq_controller->model());
            SpuriousInterruptHandler::initialize_for_disabled_master_pic();
            SpuriousInterruptHandler::initialize_for_disabled_slave_pic();
        } else {
            dbgln("Interrupts: Detected {}", irq_controller->model());
        }
    }

    APIC::initialize();
    APIC::the().init_bsp();
}

UNMAP_AFTER_INIT void InterruptManagement::locate_apic_data()
{
    VERIFY(m_madt_physical_address.has_value());
    auto madt = Memory::map_typed<ACPI::Structures::MADT>(m_madt_physical_address.value()).release_value_but_fixme_should_propagate_errors();

    if (madt->flags & PCAT_COMPAT_FLAG)
        m_interrupt_controllers.append(adopt_lock_ref(*new PIC()));
    size_t entry_index = 0;
    size_t entries_length = madt->h.length - sizeof(ACPI::Structures::MADT);
    auto* madt_entry = madt->entries;
    while (entries_length > 0) {
        size_t entry_length = madt_entry->length;
        if (madt_entry->type == (u8)ACPI::Structures::MADTEntryType::IOAPIC) {
            auto* ioapic_entry = (const ACPI::Structures::MADTEntries::IOAPIC*)madt_entry;
            dbgln("IOAPIC found @ MADT entry {}, MMIO Registers @ {}", entry_index, PhysicalAddress(ioapic_entry->ioapic_address));
            m_interrupt_controllers.append(adopt_lock_ref(*new IOAPIC(PhysicalAddress(ioapic_entry->ioapic_address), ioapic_entry->gsi_base)));
        }
        if (madt_entry->type == (u8)ACPI::Structures::MADTEntryType::InterruptSourceOverride) {
            auto* interrupt_override_entry = (const ACPI::Structures::MADTEntries::InterruptSourceOverride*)madt_entry;
            u32 global_system_interrupt = 0;
            ByteReader::load<u32>(reinterpret_cast<u8 const*>(&interrupt_override_entry->global_system_interrupt), global_system_interrupt);
            u16 flags = 0;
            ByteReader::load<u16>(reinterpret_cast<u8 const*>(&interrupt_override_entry->flags), flags);
            MUST(m_isa_interrupt_overrides.try_empend(
                interrupt_override_entry->bus,
                interrupt_override_entry->source,
                global_system_interrupt,
                flags));

            dbgln("Interrupts: Overriding INT {:#x} with GSI {}, for bus {:#x}",
                interrupt_override_entry->source,
                global_system_interrupt,
                interrupt_override_entry->bus);
        }
        madt_entry = (ACPI::Structures::MADTEntryHeader*)(VirtualAddress(madt_entry).offset(entry_length).get());
        entries_length -= entry_length;
        entry_index++;
    }
}

}
