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

#include <Kernel/ACPI/MultiProcessorParser.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Interrupts/APIC.h>
#include <Kernel/Interrupts/IOAPIC.h>
#include <Kernel/Interrupts/InterruptManagement.h>
#include <Kernel/VM/MemoryManager.h>

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

IOAPIC::IOAPIC(ioapic_mmio_regs& regs, u32 gsi_base, Vector<RefPtr<ISAInterruptOverrideMetadata>>& isa_overrides, Vector<RefPtr<PCIInterruptOverrideMetadata>>& pci_overrides)
    : m_physical_access_registers(regs)
    , m_gsi_base(gsi_base)
    , m_id((read_register(0x0) >> 24) & 0xFF)
    , m_version(read_register(0x1) & 0xFF)
    , m_redirection_entries((read_register(0x1) >> 16) + 1)
    , m_isa_interrupt_overrides(isa_overrides)
    , m_pci_interrupt_overrides(pci_overrides)
{
    klog() << "IOAPIC ID: 0x" << String::format("%x", m_id);
    klog() << "IOAPIC Version: 0x" << String::format("%x", m_version) << ", Redirection Entries count - " << m_redirection_entries;
    klog() << "IOAPIC Arbitration ID 0x" << String::format("%x", read_register(0x2));
    mask_all_redirection_entries();
}

void IOAPIC::initialize()
{
}

void IOAPIC::map_interrupt_redirection(u8 interrupt_vector)
{
    if (interrupt_vector == 11) {
        configure_redirection_entry(11, 11 + IRQ_VECTOR_BASE, DeliveryMode::LowPriority, false, true, true, true, 0);
        return;
    }
    for (auto redirection_override : m_isa_interrupt_overrides) {
        ASSERT(!redirection_override.is_null());
        if (redirection_override->source() != interrupt_vector)
            continue;
        bool active_low;
        // See ACPI spec Version 6.2, page 205 to learn more about Interrupt Overriding Flags.
        switch ((redirection_override->flags() & 0b11)) {
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
        switch (((redirection_override->flags() >> 2) & 0b11)) {
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
        configure_redirection_entry(redirection_override->gsi(), redirection_override->source() + IRQ_VECTOR_BASE, DeliveryMode::LowPriority, false, active_low, trigger_level_mode, true, 0);
        return;
    }
    isa_identity_map(interrupt_vector);
}

void IOAPIC::isa_identity_map(int index)
{
    configure_redirection_entry(index, index + IRQ_VECTOR_BASE, DeliveryMode::Normal, true, false, false, true, 1);
}


void IOAPIC::map_pci_interrupts()
{
    configure_redirection_entry(11, 11 + IRQ_VECTOR_BASE, DeliveryMode::Normal, false, false, true, true, 0);
}

void IOAPIC::map_isa_interrupts()
{
    InterruptDisabler disabler;
    for (auto redirection_override : m_isa_interrupt_overrides) {
        ASSERT(!redirection_override.is_null());
        bool active_low;
        // See ACPI spec Version 6.2, page 205 to learn more about Interrupt Overriding Flags.
        switch ((redirection_override->flags() & 0b11)) {
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
        switch (((redirection_override->flags() >> 2) & 0b11)) {
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
        configure_redirection_entry(redirection_override->gsi(), redirection_override->source() + IRQ_VECTOR_BASE, 0, false, active_low, trigger_level_mode, true, 0);
    }
}

void IOAPIC::reset_all_redirection_entries() const
{
    InterruptDisabler disabler;
    for (size_t index = 0; index < m_redirection_entries; index++)
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
    ASSERT((u32)index < m_redirection_entries);
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
    for (size_t index = 0; index < m_redirection_entries; index++)
        mask_redirection_entry(index);
}

void IOAPIC::mask_redirection_entry(u8 index) const
{
    ASSERT((u32)index < m_redirection_entries);
    u32 redirection_entry = read_register((index << 1) + IOAPIC_REDIRECTION_ENTRY_OFFSET) | (1 << 16);
    write_register((index << 1) + IOAPIC_REDIRECTION_ENTRY_OFFSET, redirection_entry);
}

bool IOAPIC::is_redirection_entry_masked(u8 index) const
{
    ASSERT((u32)index < m_redirection_entries);
    return (read_register((index << 1) + IOAPIC_REDIRECTION_ENTRY_OFFSET) & (1 << 16)) != 0;
}

void IOAPIC::unmask_redirection_entry(u8 index) const
{
    ASSERT((u32)index < m_redirection_entries);
    u32 redirection_entry = read_register((index << 1) + IOAPIC_REDIRECTION_ENTRY_OFFSET);
    write_register((index << 1) + IOAPIC_REDIRECTION_ENTRY_OFFSET, redirection_entry & ~(1 << 16));
}

bool IOAPIC::is_vector_enabled(u8 interrupt_vector) const
{
    InterruptDisabler disabler;
    return is_redirection_entry_masked(interrupt_vector);
}

u8 IOAPIC::read_redirection_entry_vector(u8 index) const
{
    ASSERT((u32)index < m_redirection_entries);
    return (read_register((index << 1) + IOAPIC_REDIRECTION_ENTRY_OFFSET) & 0xFF);
}

int IOAPIC::find_redirection_entry_by_vector(u8 vector) const
{
    InterruptDisabler disabler;
    for (size_t index = 0; index < m_redirection_entries; index++) {
        if (read_redirection_entry_vector(index) == (vector + IRQ_VECTOR_BASE))
            return index;
    }
    return -1;
}

void IOAPIC::disable(u8 interrupt_vector)
{
    InterruptDisabler disabler;
    int index = find_redirection_entry_by_vector(interrupt_vector);
    if (index == (-1)) {
        map_interrupt_redirection(interrupt_vector);
        index = find_redirection_entry_by_vector(interrupt_vector);
    }
    ASSERT(index != (-1));
    mask_redirection_entry(index);
}

void IOAPIC::enable(u8 interrupt_vector)
{
    InterruptDisabler disabler;
    int index = find_redirection_entry_by_vector(interrupt_vector);
    if (index == (-1)) {
        map_interrupt_redirection(interrupt_vector);
        index = find_redirection_entry_by_vector(interrupt_vector);
    }
    ASSERT(index != (-1));
    unmask_redirection_entry(index);
}

void IOAPIC::eoi(u8) const
{
    InterruptDisabler disabler;
    APIC::eoi();
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
    auto region = MM.allocate_kernel_region(PhysicalAddress(page_base_of(&m_physical_access_registers)), (PAGE_SIZE * 2), "IOAPIC Write", Region::Access::Read | Region::Access::Write);
    auto& regs = *(volatile ioapic_mmio_regs*)region->vaddr().offset(offset_in_page(&m_physical_access_registers)).as_ptr();
    regs.select = index;
    regs.window = value;
#ifdef IOAPIC_DEBUG
    dbg() << "IOAPIC Writing, Value 0x" << String::format("%x", regs.window) << " @ offset 0x" << String::format("%x", regs.select);
#endif
}
u32 IOAPIC::read_register(u32 index) const
{
    InterruptDisabler disabler;
    auto region = MM.allocate_kernel_region(PhysicalAddress(page_base_of(&m_physical_access_registers)), (PAGE_SIZE * 2), "IOAPIC Read", Region::Access::Read | Region::Access::Write);
    auto& regs = *(volatile ioapic_mmio_regs*)region->vaddr().offset(offset_in_page(&m_physical_access_registers)).as_ptr();
    regs.select = index;
#ifdef IOAPIC_DEBUG
    dbg() << "IOAPIC Reading, Value 0x" << String::format("%x", regs.window) << " @ offset 0x" << String::format("%x", regs.select);
#endif
    return regs.window;
}
}
