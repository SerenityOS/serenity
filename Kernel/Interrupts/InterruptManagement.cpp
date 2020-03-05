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
#include <Kernel/Interrupts/APIC.h>
#include <Kernel/Interrupts/IOAPIC.h>
#include <Kernel/Interrupts/InterruptManagement.h>
#include <Kernel/Interrupts/PIC.h>
#include <Kernel/Interrupts/SpuriousInterruptHandler.h>
#include <Kernel/Interrupts/UnhandledInterruptHandler.h>
#include <Kernel/VM/MemoryManager.h>
#include <LibBareMetal/IO.h>

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
    s_interrupt_management = new InterruptManagement();
}

void InterruptManagement::enumerate_interrupt_handlers(Function<void(GenericInterruptHandler&)> callback)
{
    for (int i = 0; i < GENERIC_INTERRUPT_HANDLERS_COUNT; i++) {
        auto& handler = get_interrupt_handler(i);
        if (handler.type() != HandlerType::UnhandledInterruptHandler)
            callback(handler);
    }
}

IRQController& InterruptManagement::get_interrupt_controller(int index)
{
    ASSERT(index >= 0);
    ASSERT(!m_interrupt_controllers[index].is_null());
    return *m_interrupt_controllers[index];
}

Vector<RefPtr<ISAInterruptOverrideMetadata>> InterruptManagement::isa_overrides()
{
    return m_isa_interrupt_overrides;
}

u8 InterruptManagement::acquire_mapped_interrupt_number(u8 number)
{
    if (!InterruptManagement::initialized()) {
        // This is necessary, because we install UnhandledInterruptHandlers before we actually initialize the Interrupt Management object...
        return number;
    }
    return InterruptManagement::the().get_mapped_vector_number(number);
}

u8 InterruptManagement::get_mapped_vector_number(u8 original_vector)
{
    // FIXME: For SMP configuration (with IOAPICs) use a better routing scheme to make redirections more efficient.
    return original_vector;
}

RefPtr<IRQController> InterruptManagement::get_responsible_irq_controller(u8 interrupt_vector)
{
    if (m_interrupt_controllers.size() == 1 && m_interrupt_controllers[0]->type() == IRQControllerType::i8259) {
        return m_interrupt_controllers[0];
    }
    for (auto irq_controller : m_interrupt_controllers) {
        if (irq_controller->get_gsi_base() <= interrupt_vector)
            if (!irq_controller->is_hard_disabled())
                return irq_controller;
    }
    ASSERT_NOT_REACHED();
}

PhysicalAddress InterruptManagement::search_for_madt()
{
    dbg() << "Early access to ACPI tables for interrupt setup";
    auto rsdp = ACPI::StaticParsing::search_rsdp();
    if (rsdp.is_null())
        return {};
    return ACPI::StaticParsing::search_table(rsdp, "APIC");
}

InterruptManagement::InterruptManagement()
    : m_madt(search_for_madt())
{
    if (m_madt.is_null()) {
        m_interrupt_controllers[0] = adopt(*new PIC());
        return;
    }

    // FIXME: Check what is the actual data size then map accordingly
    dbg() << "Interrupts: MADT @ P " << m_madt.as_ptr();
    locate_apic_data();
}

void InterruptManagement::switch_to_pic_mode()
{
    klog() << "Interrupts: Switch to Legacy PIC mode";
    InterruptDisabler disabler;
    m_smp_enabled = false;
    SpuriousInterruptHandler::initialize(7);
    SpuriousInterruptHandler::initialize(15);
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

void InterruptManagement::switch_to_ioapic_mode()
{
    klog() << "Interrupts: Switch to IOAPIC mode";
    InterruptDisabler disabler;
    m_smp_enabled = true;
    if (m_interrupt_controllers.size() == 1) {
        if (get_interrupt_controller(0).type() == IRQControllerType::i8259) {
            klog() << "Interrupts: NO IOAPIC detected, Reverting to PIC mode.";
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
    APIC::init();
    APIC::enable_bsp();
    MultiProcessorParser::initialize();
}

void InterruptManagement::locate_apic_data()
{
    ASSERT(!m_madt.is_null());
    auto region = MM.allocate_kernel_region(m_madt.page_base(), (PAGE_SIZE * 2), "Initializing Interrupts", Region::Access::Read);
    auto& madt = *(const ACPI::Structures::MADT*)region->vaddr().offset(m_madt.offset_in_page().get()).as_ptr();

    int irq_controller_count = 0;
    if (madt.flags & PCAT_COMPAT_FLAG) {
        m_interrupt_controllers[0] = adopt(*new PIC());
        irq_controller_count++;
    }
    size_t entry_index = 0;
    size_t entries_length = madt.h.length - sizeof(ACPI::Structures::MADT);
    auto* madt_entry = madt.entries;
    while (entries_length > 0) {
        size_t entry_length = madt_entry->length;
        if (madt_entry->type == (u8)ACPI::Structures::MADTEntryType::IOAPIC) {
            auto* ioapic_entry = (const ACPI::Structures::MADTEntries::IOAPIC*)madt_entry;
            dbg() << "IOAPIC found @ MADT entry " << entry_index << ", MMIO Registers @ Px" << String::format("%x", ioapic_entry->ioapic_address);
            m_interrupt_controllers.resize(1 + irq_controller_count);
            m_interrupt_controllers[irq_controller_count] = adopt(*new IOAPIC(*(ioapic_mmio_regs*)ioapic_entry->ioapic_address, ioapic_entry->gsi_base));
            irq_controller_count++;
        }
        if (madt_entry->type == (u8)ACPI::Structures::MADTEntryType::InterruptSourceOverride) {
            auto* interrupt_override_entry = (const ACPI::Structures::MADTEntries::InterruptSourceOverride*)madt_entry;
            m_isa_interrupt_overrides.append(adopt(*new ISAInterruptOverrideMetadata(
                interrupt_override_entry->bus,
                interrupt_override_entry->source,
                interrupt_override_entry->global_system_interrupt,
                interrupt_override_entry->flags)));
            dbg() << "Interrupts: Overriding INT 0x" << String::format("%x", interrupt_override_entry->source) << " with GSI " << interrupt_override_entry->global_system_interrupt << ", for bus 0x" << String::format("%x", interrupt_override_entry->bus);
        }
        madt_entry = (ACPI::Structures::MADTEntryHeader*)(VirtualAddress((u32)madt_entry).offset(entry_length).get());
        entries_length -= entry_length;
        entry_index++;
    }
}
void InterruptManagement::locate_pci_interrupt_overrides()
{
    // FIXME: calling the MultiProcessorParser causes a pagefault.
    ASSERT_NOT_REACHED();
    m_pci_interrupt_overrides = MultiProcessorParser::the().get_pci_interrupt_redirections();
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
