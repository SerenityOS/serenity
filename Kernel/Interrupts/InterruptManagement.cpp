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

#include <AK/StringView.h>
#include <Kernel/ACPI/MultiProcessorParser.h>
#include <Kernel/API/Syscall.h>
#include <Kernel/Arch/i386/CPU.h>
#include <Kernel/CommandLine.h>
#include <Kernel/IO.h>
#include <Kernel/Interrupts/APIC.h>
#include <Kernel/Interrupts/IOAPIC.h>
#include <Kernel/Interrupts/InterruptManagement.h>
#include <Kernel/Interrupts/PIC.h>
#include <Kernel/Interrupts/SpuriousInterruptHandler.h>
#include <Kernel/Interrupts/UnhandledInterruptHandler.h>
#include <Kernel/VM/MemoryManager.h>
#include <Kernel/VM/TypedMapping.h>

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

    if (kernel_command_line().lookup("smp").value_or("off") == "on")
        InterruptManagement::the().switch_to_ioapic_mode();
    else
        InterruptManagement::the().switch_to_pic_mode();
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
    ASSERT(InterruptManagement::initialized());
    return InterruptManagement::the().get_irq_vector(mapped_interrupt_vector);
}

u8 InterruptManagement::get_mapped_interrupt_vector(u8 original_irq)
{
    // FIXME: For SMP configuration (with IOAPICs) use a better routing scheme to make redirections more efficient.
    // FIXME: Find a better way to handle conflict with Syscall interrupt gate.
    ASSERT((original_irq + IRQ_VECTOR_BASE) != syscall_vector);
    return original_irq;
}

u8 InterruptManagement::get_irq_vector(u8 mapped_interrupt_vector)
{
    // FIXME: For SMP configuration (with IOAPICs) use a better routing scheme to make redirections more efficient.
    return mapped_interrupt_vector;
}

RefPtr<IRQController> InterruptManagement::get_responsible_irq_controller(u8 interrupt_vector)
{
    if (m_interrupt_controllers.size() == 1 && m_interrupt_controllers[0]->type() == IRQControllerType::i8259) {
        return m_interrupt_controllers[0];
    }
    for (auto irq_controller : m_interrupt_controllers) {
        if (irq_controller->gsi_base() <= interrupt_vector)
            if (!irq_controller->is_hard_disabled())
                return irq_controller;
    }
    ASSERT_NOT_REACHED();
}

PhysicalAddress InterruptManagement::search_for_madt()
{
    dbg() << "Early access to ACPI tables for interrupt setup";
    auto rsdp = ACPI::StaticParsing::find_rsdp();
    if (!rsdp.has_value())
        return {};
    return ACPI::StaticParsing::find_table(rsdp.value(), "APIC");
}

InterruptManagement::InterruptManagement()
    : m_madt(search_for_madt())
{
    m_interrupt_controllers.resize(1);
}

void InterruptManagement::switch_to_pic_mode()
{
    klog() << "Interrupts: Switch to Legacy PIC mode";
    InterruptDisabler disabler;
    m_smp_enabled = false;
    m_interrupt_controllers[0] = adopt(*new PIC());
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

    if (m_madt.is_null()) {
        dbg() << "Interrupts: ACPI MADT is not available, reverting to PIC mode";
        switch_to_pic_mode();
        return;
    }

    dbg() << "Interrupts: MADT @ P " << m_madt.as_ptr();
    locate_apic_data();
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

    if (auto mp_parser = MultiProcessorParser::autodetect()) {
        m_pci_interrupt_overrides = mp_parser->get_pci_interrupt_redirections();
    }

    APIC::the().init_bsp();
}

void InterruptManagement::locate_apic_data()
{
    ASSERT(!m_madt.is_null());
    auto madt = map_typed<ACPI::Structures::MADT>(m_madt);

    int irq_controller_count = 0;
    if (madt->flags & PCAT_COMPAT_FLAG) {
        m_interrupt_controllers[0] = adopt(*new PIC());
        irq_controller_count++;
    }
    size_t entry_index = 0;
    size_t entries_length = madt->h.length - sizeof(ACPI::Structures::MADT);
    auto* madt_entry = madt->entries;
    while (entries_length > 0) {
        size_t entry_length = madt_entry->length;
        if (madt_entry->type == (u8)ACPI::Structures::MADTEntryType::IOAPIC) {
            auto* ioapic_entry = (const ACPI::Structures::MADTEntries::IOAPIC*)madt_entry;
            dbg() << "IOAPIC found @ MADT entry " << entry_index << ", MMIO Registers @ " << PhysicalAddress(ioapic_entry->ioapic_address);
            m_interrupt_controllers.resize(1 + irq_controller_count);
            m_interrupt_controllers[irq_controller_count] = adopt(*new IOAPIC(PhysicalAddress(ioapic_entry->ioapic_address), ioapic_entry->gsi_base));
            irq_controller_count++;
        }
        if (madt_entry->type == (u8)ACPI::Structures::MADTEntryType::InterruptSourceOverride) {
            auto* interrupt_override_entry = (const ACPI::Structures::MADTEntries::InterruptSourceOverride*)madt_entry;
            m_isa_interrupt_overrides.empend(
                interrupt_override_entry->bus,
                interrupt_override_entry->source,
                interrupt_override_entry->global_system_interrupt,
                interrupt_override_entry->flags);
            dbg() << "Interrupts: Overriding INT 0x" << String::format("%x", interrupt_override_entry->source) << " with GSI " << interrupt_override_entry->global_system_interrupt << ", for bus 0x" << String::format("%x", interrupt_override_entry->bus);
        }
        madt_entry = (ACPI::Structures::MADTEntryHeader*)(VirtualAddress(madt_entry).offset(entry_length).get());
        entries_length -= entry_length;
        entry_index++;
    }
}

}
