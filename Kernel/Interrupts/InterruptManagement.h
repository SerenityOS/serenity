/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Function.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/OwnPtr.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/Firmware/ACPI/Definitions.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>
#include <Kernel/Interrupts/IOAPIC.h>
#include <Kernel/Interrupts/IRQController.h>

namespace Kernel {

class ISAInterruptOverrideMetadata {
public:
    ISAInterruptOverrideMetadata(u8 bus, u8 source, u32 global_system_interrupt, u16 flags)
        : m_bus(bus)
        , m_source(source)
        , m_global_system_interrupt(global_system_interrupt)
        , m_flags(flags)
    {
    }

    u8 bus() const { return m_bus; }
    u8 source() const { return m_source; }
    u32 gsi() const { return m_global_system_interrupt; }
    u16 flags() const { return m_flags; }

private:
    const u8 m_bus;
    const u8 m_source;
    const u32 m_global_system_interrupt;
    const u16 m_flags;
};

class InterruptManagement {
public:
    static InterruptManagement& the();
    static void initialize();
    static bool initialized();
    static u8 acquire_mapped_interrupt_number(u8 original_irq);
    static u8 acquire_irq_number(u8 mapped_interrupt_vector);

    virtual void switch_to_pic_mode();
    virtual void switch_to_ioapic_mode();

    bool smp_enabled() const { return m_smp_enabled; }
    RefPtr<IRQController> get_responsible_irq_controller(u8 interrupt_vector);
    RefPtr<IRQController> get_responsible_irq_controller(IRQControllerType controller_type, u8 interrupt_vector);

    const Vector<ISAInterruptOverrideMetadata>& isa_overrides() const { return m_isa_interrupt_overrides; }

    u8 get_mapped_interrupt_vector(u8 original_irq);
    u8 get_irq_vector(u8 mapped_interrupt_vector);

    void enumerate_interrupt_handlers(Function<void(GenericInterruptHandler&)>);
    IRQController& get_interrupt_controller(int index);

protected:
    virtual ~InterruptManagement() = default;

private:
    InterruptManagement();
    PhysicalAddress search_for_madt();
    void locate_apic_data();
    bool m_smp_enabled { false };
    Vector<RefPtr<IRQController>> m_interrupt_controllers;
    Vector<ISAInterruptOverrideMetadata> m_isa_interrupt_overrides;
    Vector<PCIInterruptOverrideMetadata> m_pci_interrupt_overrides;
    PhysicalAddress m_madt;
};

}
