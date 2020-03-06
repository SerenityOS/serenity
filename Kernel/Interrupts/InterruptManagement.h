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

#pragma once

#include <AK/FixedArray.h>
#include <AK/Function.h>
#include <AK/NonnullOwnPtr.h>
#include <AK/OwnPtr.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/ACPI/Definitions.h>
#include <Kernel/Interrupts/GenericInterruptHandler.h>
#include <Kernel/Interrupts/IOAPIC.h>
#include <Kernel/Interrupts/IRQController.h>

namespace Kernel {

class ISAInterruptOverrideMetadata;

class InterruptManagement {
public:
    static InterruptManagement& the();
    static void initialize();
    static bool initialized();
    static u8 acquire_mapped_interrupt_number(u8);

    virtual void switch_to_pic_mode();
    virtual void switch_to_ioapic_mode();
    RefPtr<IRQController> get_responsible_irq_controller(u8 interrupt_vector);

    Vector<RefPtr<ISAInterruptOverrideMetadata>> isa_overrides();

    u8 get_mapped_vector_number(u8 original_vector);

    void enumerate_interrupt_handlers(Function<void(GenericInterruptHandler&)>);
    IRQController& get_interrupt_controller(int index);

private:
    InterruptManagement();
    PhysicalAddress search_for_madt();
    void locate_apic_data();
    void locate_pci_interrupt_overrides();
    bool m_smp_enabled { false };
    FixedArray<RefPtr<IRQController>> m_interrupt_controllers { 1 };
    Vector<RefPtr<ISAInterruptOverrideMetadata>> m_isa_interrupt_overrides;
    Vector<RefPtr<PCIInterruptOverrideMetadata>> m_pci_interrupt_overrides;
    PhysicalAddress m_madt;
};

class ISAInterruptOverrideMetadata : public RefCounted<ISAInterruptOverrideMetadata> {
public:
    ISAInterruptOverrideMetadata(u8 bus, u8 source, u32 global_system_interrupt, u16 flags);
    u8 bus() const;
    u8 source() const;
    u32 gsi() const;
    u16 flags() const;

private:
    u8 m_bus;
    u8 m_source;
    u32 m_global_system_interrupt;
    u16 m_flags;
};
}
