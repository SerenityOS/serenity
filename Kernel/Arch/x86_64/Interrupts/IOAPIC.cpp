/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/Optional.h>
#include <Kernel/Arch/x86_64/InterruptManagement.h>
#include <Kernel/Arch/x86_64/Interrupts/APIC.h>
#include <Kernel/Arch/x86_64/Interrupts/IOAPIC.h>
#include <Kernel/Arch/x86_64/ProcessorInfo.h>
#include <Kernel/Debug.h>
#include <Kernel/Interrupts/InterruptDisabler.h>
#include <Kernel/Sections.h>

#define IOAPIC_REDIRECTION_ENTRY_OFFSET 0x10
namespace Kernel {
enum DeliveryMode {
    Normal = 0,
    LowPriority = 1,
    SMI = 2,
    NMI = 3,
    INIT = 4,
    External = 7
};

UNMAP_AFTER_INIT IOAPIC::IOAPIC(PhysicalAddress address, u32 gsi_base)
    : m_address(address)
    , m_regs(Memory::map_typed_writable<ioapic_mmio_regs>(m_address).release_value_but_fixme_should_propagate_errors())
    , m_gsi_base(gsi_base)
    , m_id((read_register(0x0) >> 24) & 0xFF)
    , m_version(read_register(0x1) & 0xFF)
    , m_redirection_entries_count((read_register(0x1) >> 16) + 1)
{
    InterruptDisabler disabler;
    dmesgln("IOAPIC ID: {:#x}", m_id);
    dmesgln("IOAPIC Version: {:#x}, redirection entries: {}", m_version, m_redirection_entries_count);
    dmesgln("IOAPIC Arbitration ID {:#x}", read_register(0x2));
    mask_all_redirection_entries();
}

UNMAP_AFTER_INIT void IOAPIC::initialize()
{
}

void IOAPIC::map_interrupt_redirection(u8 interrupt_vector)
{
    InterruptDisabler disabler;
    for (auto redirection_override : InterruptManagement::the().isa_overrides()) {
        if (redirection_override.source() != interrupt_vector)
            continue;
        bool active_low = false;
        // See https://uefi.org/specs/ACPI/6.5/05_ACPI_Software_Programming_Model.html#interrupt-source-override-structure
        // to learn more about Interrupt Overriding Flags.
        switch ((redirection_override.flags() & 0b11)) {
        case 0:
            active_low = false;
            break;
        case 1:
            active_low = false;
            break;
        case 2:
            VERIFY_NOT_REACHED(); // Reserved value
        case 3:
            active_low = true;
            break;
        }

        bool trigger_level_mode = false;
        // See https://uefi.org/specs/ACPI/6.5/05_ACPI_Software_Programming_Model.html#interrupt-source-override-structure
        // to learn more about Interrupt Overriding Flags.
        switch (((redirection_override.flags() >> 2) & 0b11)) {
        case 0:
            trigger_level_mode = false;
            break;
        case 1:
            trigger_level_mode = false;
            break;
        case 2:
            VERIFY_NOT_REACHED(); // Reserved value
        case 3:
            trigger_level_mode = true;
            break;
        }
        configure_redirection_entry(redirection_override.gsi() - gsi_base(), InterruptManagement::acquire_mapped_interrupt_number(redirection_override.source()) + IRQ_VECTOR_BASE, DeliveryMode::Normal, false, active_low, trigger_level_mode, true, Processor::by_id(0).info().apic_id());
        return;
    }
    isa_identity_map(interrupt_vector);
}

void IOAPIC::isa_identity_map(size_t index)
{
    InterruptDisabler disabler;
    configure_redirection_entry(index, InterruptManagement::acquire_mapped_interrupt_number(index) + IRQ_VECTOR_BASE, DeliveryMode::Normal, false, false, false, true, Processor::by_id(0).info().apic_id());
}

bool IOAPIC::is_enabled() const
{
    return !is_hard_disabled();
}

void IOAPIC::spurious_eoi(GenericInterruptHandler const& handler) const
{
    InterruptDisabler disabler;
    VERIFY(handler.type() == HandlerType::SpuriousInterruptHandler);
    VERIFY(handler.interrupt_number() == APIC::spurious_interrupt_vector());
    dbgln("IOAPIC: Spurious interrupt");
}

void IOAPIC::reset_all_redirection_entries() const
{
    InterruptDisabler disabler;
    for (size_t index = 0; index < m_redirection_entries_count; index++)
        reset_redirection_entry(index);
}

void IOAPIC::hard_disable()
{
    InterruptDisabler disabler;
    reset_all_redirection_entries();
    IRQController::hard_disable();
}

void IOAPIC::reset_redirection_entry(size_t index) const
{
    InterruptDisabler disabler;
    configure_redirection_entry(index, 0, 0, false, false, false, true, Processor::by_id(0).info().apic_id());
}

void IOAPIC::configure_redirection_entry(size_t index, u8 interrupt_vector, u8 delivery_mode, bool logical_destination, bool active_low, bool trigger_level_mode, bool masked, u8 destination) const
{
    InterruptDisabler disabler;
    VERIFY(index < m_redirection_entries_count);
    u32 redirection_entry1 = interrupt_vector | (delivery_mode & 0b111) << 8 | logical_destination << 11 | active_low << 13 | trigger_level_mode << 15 | masked << 16;
    u32 redirection_entry2 = destination << 24;
    write_register((index << 1) + IOAPIC_REDIRECTION_ENTRY_OFFSET, redirection_entry1);

    if constexpr (IOAPIC_DEBUG)
        dbgln("IOAPIC Value: {:#x}", read_register((index << 1) + IOAPIC_REDIRECTION_ENTRY_OFFSET));

    write_register((index << 1) + IOAPIC_REDIRECTION_ENTRY_OFFSET + 1, redirection_entry2);

    if constexpr (IOAPIC_DEBUG)
        dbgln("IOAPIC Value: {:#x}", read_register((index << 1) + 0x11));
}

void IOAPIC::mask_all_redirection_entries() const
{
    InterruptDisabler disabler;
    for (size_t index = 0; index < m_redirection_entries_count; index++)
        mask_redirection_entry(index);
}

void IOAPIC::mask_redirection_entry(u8 index) const
{
    VERIFY(index < m_redirection_entries_count);
    u32 redirection_entry = read_register((index << 1) + IOAPIC_REDIRECTION_ENTRY_OFFSET);
    if (redirection_entry & (1 << 16))
        return;
    write_register((index << 1) + IOAPIC_REDIRECTION_ENTRY_OFFSET, redirection_entry | (1 << 16));
}

bool IOAPIC::is_redirection_entry_masked(u8 index) const
{
    VERIFY(index < m_redirection_entries_count);
    return (read_register((index << 1) + IOAPIC_REDIRECTION_ENTRY_OFFSET) & (1 << 16)) != 0;
}

void IOAPIC::unmask_redirection_entry(u8 index) const
{
    VERIFY(index < m_redirection_entries_count);
    u32 redirection_entry = read_register((index << 1) + IOAPIC_REDIRECTION_ENTRY_OFFSET);
    if (!(redirection_entry & (1 << 16)))
        return;
    write_register((index << 1) + IOAPIC_REDIRECTION_ENTRY_OFFSET, redirection_entry & ~(1 << 16));
}

bool IOAPIC::is_vector_enabled(u8 interrupt_vector) const
{
    InterruptDisabler disabler;
    return is_redirection_entry_masked(interrupt_vector);
}

u8 IOAPIC::read_redirection_entry_vector(u8 index) const
{
    VERIFY(index < m_redirection_entries_count);
    return (read_register((index << 1) + IOAPIC_REDIRECTION_ENTRY_OFFSET) & 0xFF);
}

Optional<int> IOAPIC::find_redirection_entry_by_vector(u8 vector) const
{
    InterruptDisabler disabler;
    for (size_t index = 0; index < m_redirection_entries_count; index++) {
        if (read_redirection_entry_vector(index) == (InterruptManagement::acquire_mapped_interrupt_number(vector) + IRQ_VECTOR_BASE))
            return index;
    }
    return {};
}

void IOAPIC::disable(GenericInterruptHandler const& handler)
{
    InterruptDisabler disabler;
    VERIFY(!is_hard_disabled());
    u8 interrupt_vector = handler.interrupt_number();
    VERIFY(interrupt_vector >= gsi_base() && interrupt_vector < interrupt_vectors_count());
    auto found_index = find_redirection_entry_by_vector(interrupt_vector);
    if (!found_index.has_value()) {
        map_interrupt_redirection(interrupt_vector);
        found_index = find_redirection_entry_by_vector(interrupt_vector);
    }
    VERIFY(found_index.has_value());
    mask_redirection_entry(found_index.value());
}

void IOAPIC::enable(GenericInterruptHandler const& handler)
{
    InterruptDisabler disabler;
    VERIFY(!is_hard_disabled());
    u8 interrupt_vector = handler.interrupt_number();
    VERIFY(interrupt_vector >= gsi_base() && interrupt_vector < interrupt_vectors_count());
    auto found_index = find_redirection_entry_by_vector(interrupt_vector);
    if (!found_index.has_value()) {
        map_interrupt_redirection(interrupt_vector);
        found_index = find_redirection_entry_by_vector(interrupt_vector);
    }
    VERIFY(found_index.has_value());
    unmask_redirection_entry(found_index.value());
}

void IOAPIC::eoi(GenericInterruptHandler const& handler) const
{
    InterruptDisabler disabler;
    VERIFY(!is_hard_disabled());
    VERIFY(handler.interrupt_number() >= gsi_base() && handler.interrupt_number() < interrupt_vectors_count());
    VERIFY(handler.type() != HandlerType::SpuriousInterruptHandler);
    APIC::the().eoi();
}

u16 IOAPIC::get_isr() const
{
    InterruptDisabler disabler;
    VERIFY_NOT_REACHED();
}

u16 IOAPIC::get_irr() const
{
    InterruptDisabler disabler;
    VERIFY_NOT_REACHED();
}

void IOAPIC::write_register(u32 index, u32 value) const
{
    InterruptDisabler disabler;
    m_regs->select = index;
    m_regs->window = value;

    dbgln_if(IOAPIC_DEBUG, "IOAPIC Writing, Value {:#x} @ offset {:#x}", (u32)m_regs->window, (u32)m_regs->select);
}
u32 IOAPIC::read_register(u32 index) const
{
    InterruptDisabler disabler;
    m_regs->select = index;
    dbgln_if(IOAPIC_DEBUG, "IOAPIC Reading, Value {:#x} @ offset {:#x}", (u32)m_regs->window, (u32)m_regs->select);
    return m_regs->window;
}

}
