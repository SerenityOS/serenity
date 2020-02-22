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

#include <AK/FixedArray.h>
#include <Kernel/ACPI/MultiProcessorParser.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/Interrupts/IOAPIC.h>
#include <Kernel/Interrupts/InterruptManagement.h>
#include <Kernel/Interrupts/PIC.h>
#include <Kernel/VM/MemoryManager.h>

#define PCAT_COMPAT_FLAG 0x1

namespace Kernel {

static InterruptManagement* s_interrupt_management;

bool InterruptManagement::initialized()
{
    return (s_interrupt_management != nullptr);
}

InterruptManagement& InterruptManagement::the()
{
    ASSERT(InterruptManagement::initialized());
    return *s_interrupt_management;
}

void InterruptManagement::initialize()
{
    ASSERT(!InterruptManagement::initialized());
    s_interrupt_management = new InterruptManagement(true);
}

InterruptManagement::InterruptManagement(bool create_default_controller)
{
    if (create_default_controller)
        m_interrupt_controllers[0] = make<PIC>();
}

void InterruptManagement::enable(u8 interrupt_vector)
{
    for (auto& irq_controller : InterruptManagement::the().m_interrupt_controllers) {
        if (irq_controller->get_gsi_base() <= interrupt_vector)
            if (!irq_controller->is_hard_disabled())
                irq_controller->enable(interrupt_vector);
    }
}

void InterruptManagement::enumerate_interrupt_handlers(Function<void(GenericInterruptHandler&)> callback)
{
    for (int i = 0; i < GENERIC_INTERRUPT_HANDLERS_COUNT; i++) {
        auto& handler = get_interrupt_handler(i);
        if (handler.get_invoking_count() > 0)
            callback(handler);
    }
}

void InterruptManagement::disable(u8 interrupt_vector)
{
    for (auto& irq_controller : InterruptManagement::the().m_interrupt_controllers) {
        ASSERT(irq_controller != nullptr);
        if (irq_controller->get_gsi_base() <= interrupt_vector)
            if (!irq_controller->is_hard_disabled())
                irq_controller->disable(interrupt_vector);
    }
}

void InterruptManagement::eoi(u8 interrupt_vector)
{
    for (auto& irq_controller : InterruptManagement::the().m_interrupt_controllers) {
        ASSERT(irq_controller != nullptr);
        if (irq_controller->get_gsi_base() <= interrupt_vector)
            if (!irq_controller->is_hard_disabled())
                irq_controller->eoi(interrupt_vector);
    }
}

IRQController& InterruptManagement::get_interrupt_controller(int index)
{
    ASSERT(index >= 0);
    ASSERT(m_interrupt_controllers[index] != nullptr);
    return *m_interrupt_controllers[index];
}

void InterruptManagement::switch_to_pic_mode()
{
    kprintf("Interrupts: PIC mode by default\n");
}

void InterruptManagement::switch_to_ioapic_mode()
{
    kprintf("Interrupts: Switch to IOAPIC mode failed, Reverting to PIC mode\n");
}

void AdvancedInterruptManagement::initialize(ACPI_RAW::MADT& p_madt)
{
    ASSERT(!InterruptManagement::initialized());
    s_interrupt_management = new AdvancedInterruptManagement(p_madt);
}

AdvancedInterruptManagement::AdvancedInterruptManagement(ACPI_RAW::MADT& p_madt)
    : InterruptManagement(false)
    , m_madt(p_madt)
{
    // FIXME: Check what is the actual data size then map accordingly
    dbg() << "Interrupts: MADT @ P " << &p_madt;
    locate_isa_interrupt_overrides(p_madt);
    locate_ioapics(p_madt);
}

void AdvancedInterruptManagement::switch_to_pic_mode()
{
    kprintf("Interrupts: Switch to Legacy PIC mode\n");
    for (auto& irq_controller : m_interrupt_controllers) {
        ASSERT(irq_controller);
        if (irq_controller->type() == IRQControllerType::i82093AA) {
            irq_controller->hard_disable();
            dbg() << "Interrupts: Detected " << irq_controller->model() << " - Disabled";
        } else {
            dbg() << "Interrupts: Detected " << irq_controller->model();
        }
    }
}

void AdvancedInterruptManagement::switch_to_ioapic_mode()
{
    kprintf("Interrupts: Switch to IOAPIC mode\n");
    if (m_interrupt_controllers.size() == 1) {
        if (get_interrupt_controller(0).type() == IRQControllerType::i8259) {
            kprintf("Interrupts: NO IOAPIC detected, Reverting to PIC mode.\n");
            return;
        }
    }
    for (auto& irq_controller : m_interrupt_controllers) {
        ASSERT(irq_controller);
        if (irq_controller->type() == IRQControllerType::i8259) {
            irq_controller->hard_disable();
            dbg() << "Interrupts: Detected " << irq_controller->model() << " Disabled";
        } else {
            dbg() << "Interrupts: Detected " << irq_controller->model();
        }
    }
}

void AdvancedInterruptManagement::locate_ioapics(ACPI_RAW::MADT& p_madt)
{
    auto region = MM.allocate_kernel_region(PhysicalAddress(page_base_of(&p_madt)), (PAGE_SIZE * 2), "Initializing Interrupts", Region::Access::Read);
    auto& madt = *(const ACPI_RAW::MADT*)region->vaddr().offset(offset_in_page(&p_madt)).as_ptr();

    int index = 0;
    if (madt.flags & PCAT_COMPAT_FLAG) {
        m_interrupt_controllers[0] = make<PIC>();
        index++;
    }
    size_t entry_index = 0;
    size_t entries_length = madt.h.length - sizeof(ACPI_RAW::MADT);
    auto* madt_entry = madt.entries;
    while (entries_length > 0) {
        size_t entry_length = madt_entry->length;
        if (madt_entry->type == (u8)ACPI_RAW::MADTEntryType::IOAPIC) {
            auto* ioapic_entry = (const ACPI_RAW::MADT_IOAPIC*)madt_entry;
            dbg() << "IOAPIC found @ MADT entry " << entry_index << ", MMIO Registers @ Px" << String::format("%x", ioapic_entry->ioapic_address);
            m_interrupt_controllers.resize(1 + index);
            m_interrupt_controllers[index] = make<IOAPIC>(*(ioapic_mmio_regs*)ioapic_entry->ioapic_address, ioapic_entry->gsi_base, m_isa_interrupt_overrides, m_pci_interrupt_overrides);
            index++;
        }
        madt_entry = (ACPI_RAW::MADTEntryHeader*)(VirtualAddress((u32)madt_entry).offset(entry_length).get());
        entries_length -= entry_length;
        entry_index++;
    }
}
void AdvancedInterruptManagement::locate_pci_interrupt_overrides()
{
    // FIXME: calling the MultiProcessorParser causes a pagefault.
    ASSERT_NOT_REACHED();
    m_pci_interrupt_overrides = MultiProcessorParser::the().get_pci_interrupt_redirections();
}

void AdvancedInterruptManagement::locate_isa_interrupt_overrides(ACPI_RAW::MADT& p_madt)
{
    auto region = MM.allocate_kernel_region(PhysicalAddress(page_base_of(&p_madt)), (PAGE_SIZE * 2), "Initializing Interrupts", Region::Access::Read);
    auto& madt = *(const ACPI_RAW::MADT*)region->vaddr().offset(offset_in_page(&p_madt)).as_ptr();

    size_t entry_index = 0;
    size_t entries_length = madt.h.length - sizeof(ACPI_RAW::MADT);
    auto* madt_entry = madt.entries;
    while (entries_length > 0) {
        size_t entry_length = madt_entry->length;
        if (madt_entry->type == (u8)ACPI_RAW::MADTEntryType::InterruptSourceOverride) {
            auto* interrupt_override_entry = (const ACPI_RAW::MADT_InterruptSourceOverride*)madt_entry;
            m_isa_interrupt_overrides.append(adopt(*new ISAInterruptOverrideMetadata(
                interrupt_override_entry->bus,
                interrupt_override_entry->source,
                interrupt_override_entry->global_system_interrupt,
                interrupt_override_entry->flags)));
            dbg() << "Interrupts: Overriding INT 0x" << String::format("%x", interrupt_override_entry->source) << " with GSI " << interrupt_override_entry->global_system_interrupt << ", for bus 0x" << String::format("%x", interrupt_override_entry->bus);
        }
        madt_entry = (ACPI_RAW::MADTEntryHeader*)(VirtualAddress((u32)madt_entry).offset(entry_length).get());
        entries_length -= entry_length;
        entry_index++;
    }
}

ISAInterruptOverrideMetadata::ISAInterruptOverrideMetadata(u8 bus, u8 source, u32 global_system_interrupt, u16 flags)
    : m_bus(bus)
    , m_source(source)
    , m_global_system_interrupt(global_system_interrupt)
    , m_flags(flags)
{
}

u8 ISAInterruptOverrideMetadata::bus() const
{
    return m_bus;
}
u8 ISAInterruptOverrideMetadata::source() const
{
    return m_source;
}
u32 ISAInterruptOverrideMetadata::gsi() const
{
    return m_global_system_interrupt;
}
u16 ISAInterruptOverrideMetadata::flags() const
{
    return m_flags;
}
}
