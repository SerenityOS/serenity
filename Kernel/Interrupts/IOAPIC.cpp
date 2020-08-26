/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include <AK/Optional.h>
#include <AK/StringView.h>
#include <Kernel/ACPI/MultiProcessorParser.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Interrupts/APIC.h>
#include <Kernel/Interrupts/IOAPIC.h>
#include <Kernel/Interrupts/InterruptManagement.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/TypedMapping.h>

//#define IOAPIC_DEBUG

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

IOAPIC::IOAPIC(PhysicalAddress address, u32 gsi_base)
    : m_address(address)
    , m_gsi_base(gsi_base)
    , m_id((read_register(0x0) >> 24) & 0xFF)
    , m_version(read_register(0x1) & 0xFF)
    , m_redirection_entries_count((read_register(0x1) >> 16) + 1)
{
    InterruptDisabler disabler;
    klog() << "IOAPIC ID: 0x" << String::format("%x", m_id);
    klog() << "IOAPIC Version: 0x" << String::format("%x", m_version) << ", Redirection Entries count - " << m_redirection_entries_count;
    klog() << "IOAPIC Arbitration ID 0x" << String::format("%x", read_register(0x2));
    mask_all_redirection_entries();
}

void IOAPIC::initialize()
{
}

void IOAPIC::map_interrupt_redirection(u8 interrupt_vector)
{
    InterruptDisabler disabler;
    for (auto redirection_override : InterruptManagement::the().isa_overrides()) {
        if (redirection_override.source() != interrupt_vector)
            continue;
        bool active_low;
        // See ACPI spec Version 6.2, page 205 to learn more about Interrupt Overriding Flags.
        switch ((redirection_override.flags() & 0b11)) {
        case 0:
            active_low = false;
            break;
        case 1:
            active_low = false;
            break;
        case 2:
            ASSERT_NOT_REACHED(); // Reserved value
        case 3:
            active_low = true;
            break;
        }

        bool trigger_level_mode;
        // See ACPI spec Version 6.2, page 205 to learn more about Interrupt Overriding Flags.
        switch (((redirection_override.flags() >> 2) & 0b11)) {
        case 0:
            trigger_level_mode = false;
            break;
        case 1:
            trigger_level_mode = false;
            break;
        case 2:
            ASSERT_NOT_REACHED(); // Reserved value
        case 3:
            trigger_level_mode = true;
            break;
        }
        configure_redirection_entry(redirection_override.gsi() - gsi_base(), InterruptManagement::acquire_mapped_interrupt_number(redirection_override.source()) + IRQ_VECTOR_BASE, DeliveryMode::Normal, false, active_low, trigger_level_mode, true, 0);
        return;
    }
    isa_identity_map(interrupt_vector);
}

void IOAPIC::isa_identity_map(int index)
{
    InterruptDisabler disabler;
    configure_redirection_entry(index, InterruptManagement::acquire_mapped_interrupt_number(index) + IRQ_VECTOR_BASE, DeliveryMode::Normal, false, false, false, true, 0);
}

void IOAPIC::map_pci_interrupts()
{
    InterruptDisabler disabler;
    configure_redirection_entry(11, 11 + IRQ_VECTOR_BASE, DeliveryMode::Normal, false, false, true, true, 0);
}

bool IOAPIC::is_enabled() const
{
    return !is_hard_disabled();
}

void IOAPIC::spurious_eoi(const GenericInterruptHandler& handler) const
{
    InterruptDisabler disabler;
    ASSERT(handler.type() == HandlerType::SpuriousInterruptHandler);
    ASSERT(handler.interrupt_number() == APIC::spurious_interrupt_vector());
    klog() << "IOAPIC::spurious_eoi - Spurious Interrupt occurred";
}

void IOAPIC::map_isa_interrupts()
{
    InterruptDisabler disabler;
    for (auto redirection_override : InterruptManagement::the().isa_overrides()) {
        if ((redirection_override.gsi() < gsi_base()) || (redirection_override.gsi() >= (gsi_base() + m_redirection_entries_count)))
            continue;
        bool active_low;
        // See ACPI spec Version 6.2, page 205 to learn more about Interrupt Overriding Flags.
        switch ((redirection_override.flags() & 0b11)) {
        case 0:
            active_low = false;
            break;
        case 1:
            active_low = false;
            break;
        case 2:
            ASSERT_NOT_REACHED();
        case 3:
            active_low = true;
            break;
        }

        bool trigger_level_mode;
        // See ACPI spec Version 6.2, page 205 to learn more about Interrupt Overriding Flags.
        switch (((redirection_override.flags() >> 2) & 0b11)) {
        case 0:
            trigger_level_mode = false;
            break;
        case 1:
            trigger_level_mode = false;
            break;
        case 2:
            ASSERT_NOT_REACHED();
        case 3:
            trigger_level_mode = true;
            break;
        }
        configure_redirection_entry(redirection_override.gsi() - gsi_base(), InterruptManagement::acquire_mapped_interrupt_number(redirection_override.source()) + IRQ_VECTOR_BASE, 0, false, active_low, trigger_level_mode, true, 0);
    }
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

void IOAPIC::reset_redirection_entry(int index) const
{
    InterruptDisabler disabler;
    configure_redirection_entry(index, 0, 0, false, false, false, true, 0);
}

void IOAPIC::configure_redirection_entry(int index, u8 interrupt_vector, u8 delivery_mode, bool logical_destination, bool active_low, bool trigger_level_mode, bool masked, u8 destination) const
{
    InterruptDisabler disabler;
    ASSERT((u32)index < m_redirection_entries_count);
    u32 redirection_entry1 = interrupt_vector | (delivery_mode & 0b111) << 8 | logical_destination << 11 | active_low << 13 | trigger_level_mode << 15 | masked << 16;
    u32 redirection_entry2 = destination << 24;
    write_register((index << 1) + IOAPIC_REDIRECTION_ENTRY_OFFSET, redirection_entry1);
#ifdef IOAPIC_DEBUG
    dbg() << "IOAPIC Value: 0x" << String::format("%x", read_register((index << 1) + IOAPIC_REDIRECTION_ENTRY_OFFSET));
#endif
    write_register((index << 1) + IOAPIC_REDIRECTION_ENTRY_OFFSET + 1, redirection_entry2);
#ifdef IOAPIC_DEBUG
    dbg() << "IOAPIC Value: 0x" << String::format("%x", read_register((index << 1) + 0x11));
#endif
}

void IOAPIC::mask_all_redirection_entries() const
{
    InterruptDisabler disabler;
    for (size_t index = 0; index < m_redirection_entries_count; index++)
        mask_redirection_entry(index);
}

void IOAPIC::mask_redirection_entry(u8 index) const
{
    ASSERT((u32)index < m_redirection_entries_count);
    u32 redirection_entry = read_register((index << 1) + IOAPIC_REDIRECTION_ENTRY_OFFSET);
    if (redirection_entry & (1 << 16))
        return;
    write_register((index << 1) + IOAPIC_REDIRECTION_ENTRY_OFFSET, redirection_entry | (1 << 16));
}

bool IOAPIC::is_redirection_entry_masked(u8 index) const
{
    ASSERT((u32)index < m_redirection_entries_count);
    return (read_register((index << 1) + IOAPIC_REDIRECTION_ENTRY_OFFSET) & (1 << 16)) != 0;
}

void IOAPIC::unmask_redirection_entry(u8 index) const
{
    ASSERT((u32)index < m_redirection_entries_count);
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
    ASSERT((u32)index < m_redirection_entries_count);
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

void IOAPIC::disable(const GenericInterruptHandler& handler)
{
    InterruptDisabler disabler;
    ASSERT(!is_hard_disabled());
    u8 interrupt_vector = handler.interrupt_number();
    ASSERT(interrupt_vector >= gsi_base() && interrupt_vector < interrupt_vectors_count());
    auto found_index = find_redirection_entry_by_vector(interrupt_vector);
    if (!found_index.has_value()) {
        map_interrupt_redirection(interrupt_vector);
        found_index = find_redirection_entry_by_vector(interrupt_vector);
    }
    ASSERT(found_index.has_value());
    mask_redirection_entry(found_index.value());
}

void IOAPIC::enable(const GenericInterruptHandler& handler)
{
    InterruptDisabler disabler;
    ASSERT(!is_hard_disabled());
    u8 interrupt_vector = handler.interrupt_number();
    ASSERT(interrupt_vector >= gsi_base() && interrupt_vector < interrupt_vectors_count());
    auto found_index = find_redirection_entry_by_vector(interrupt_vector);
    if (!found_index.has_value()) {
        map_interrupt_redirection(interrupt_vector);
        found_index = find_redirection_entry_by_vector(interrupt_vector);
    }
    ASSERT(found_index.has_value());
    unmask_redirection_entry(found_index.value());
}

void IOAPIC::eoi(const GenericInterruptHandler& handler) const
{
    InterruptDisabler disabler;
    ASSERT(!is_hard_disabled());
    ASSERT(handler.interrupt_number() >= gsi_base() && handler.interrupt_number() < interrupt_vectors_count());
    ASSERT(handler.type() != HandlerType::SpuriousInterruptHandler);
    APIC::the().eoi();
}

u16 IOAPIC::get_isr() const
{
    InterruptDisabler disabler;
    ASSERT_NOT_REACHED();
}

u16 IOAPIC::get_irr() const
{
    InterruptDisabler disabler;
    ASSERT_NOT_REACHED();
}

void IOAPIC::write_register(u32 index, u32 value) const
{
    InterruptDisabler disabler;
    auto regs = map_typed_writable<ioapic_mmio_regs>(m_address);
    regs->select = index;
    regs->window = value;
#ifdef IOAPIC_DEBUG
    dbg() << "IOAPIC Writing, Value 0x" << String::format("%x", regs->window) << " @ offset 0x" << String::format("%x", regs->select);
#endif
}
u32 IOAPIC::read_register(u32 index) const
{
    InterruptDisabler disabler;
    auto regs = map_typed_writable<ioapic_mmio_regs>(m_address);
    regs->select = index;
#ifdef IOAPIC_DEBUG
    dbg() << "IOAPIC Reading, Value 0x" << String::format("%x", regs->window) << " @ offset 0x" << String::format("%x", regs->select);
#endif
    return regs->window;
}
}
