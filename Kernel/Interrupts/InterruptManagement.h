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

class InterruptManagement {
public:
    static InterruptManagement& the();
    static void initialize();
    static bool initialized();

    virtual void switch_to_pic_mode();
    virtual void switch_to_ioapic_mode();

    void enable(u8 interrupt_vector);
    void disable(u8 interrupt_vector);
    void eoi(u8 interrupt_vector);
    void enumerate_interrupt_handlers(Function<void(GenericInterruptHandler&)>);
    IRQController& get_interrupt_controller(int index);

protected:
    explicit InterruptManagement(bool create_default_controller);
    FixedArray<OwnPtr<IRQController>> m_interrupt_controllers { 1 };
};

class ISAInterruptOverrideMetadata;
class AdvancedInterruptManagement : public InterruptManagement {
    friend class IOAPIC;

public:
    static void initialize(ACPI_RAW::MADT& madt);
    virtual void switch_to_ioapic_mode() override;
    virtual void switch_to_pic_mode() override;

private:
    explicit AdvancedInterruptManagement(ACPI_RAW::MADT& madt);
    void locate_ioapics(ACPI_RAW::MADT& madt);
    void locate_isa_interrupt_overrides(ACPI_RAW::MADT& madt);
    void locate_pci_interrupt_overrides();
    Vector<RefPtr<ISAInterruptOverrideMetadata>> m_isa_interrupt_overrides;
    Vector<RefPtr<PCIInterruptOverrideMetadata>> m_pci_interrupt_overrides;
    ACPI_RAW::MADT& m_madt;
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
