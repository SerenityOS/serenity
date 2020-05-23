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

#include <Kernel/Interrupts/IRQController.h>

namespace Kernel {
struct [[gnu::packed]] ioapic_mmio_regs
{
    volatile u32 select;
    u32 reserved[3];
    volatile u32 window;
};

class PCIInterruptOverrideMetadata {
public:
    PCIInterruptOverrideMetadata(u8 bus_id, u8 polarity, u8 trigger_mode, u8 source_irq, u32 ioapic_id, u16 ioapic_int_pin);
    u8 bus() const { return m_bus_id; }
    u8 polarity() const { return m_polarity; }
    u8 trigger_mode() const { return m_trigger_mode; }
    u8 pci_interrupt_pin() const { return m_pci_interrupt_pin; }
    u8 pci_device_number() const { return m_pci_device_number; }
    u32 ioapic_id() const { return m_ioapic_id; }
    u16 ioapic_interrupt_pin() const { return m_ioapic_interrupt_pin; }

private:
    const u8 m_bus_id;
    const u8 m_polarity;
    const u8 m_trigger_mode;
    const u8 m_pci_interrupt_pin;
    const u8 m_pci_device_number;
    const u32 m_ioapic_id;
    const u16 m_ioapic_interrupt_pin;
};

class IOAPIC final : public IRQController {
public:
    IOAPIC(PhysicalAddress, u32 gsi_base);
    virtual void enable(const GenericInterruptHandler&) override;
    virtual void disable(const GenericInterruptHandler&) override;
    virtual void hard_disable() override;
    virtual void eoi(const GenericInterruptHandler&) const override;
    virtual void spurious_eoi(const GenericInterruptHandler&) const override;
    virtual bool is_vector_enabled(u8 number) const override;
    virtual bool is_enabled() const override;
    virtual u16 get_isr() const override;
    virtual u16 get_irr() const override;
    virtual u32 gsi_base() const override { return m_gsi_base; }
    virtual size_t interrupt_vectors_count() const { return m_redirection_entries_count; }
    virtual const char* model() const override { return "IOAPIC"; };
    virtual IRQControllerType type() const override { return IRQControllerType::i82093AA; }

private:
    void configure_redirection_entry(int index, u8 interrupt_vector, u8 delivery_mode, bool logical_destination, bool active_low, bool trigger_level_mode, bool masked, u8 destination) const;
    void reset_redirection_entry(int index) const;
    void map_interrupt_redirection(u8 interrupt_vector);
    void reset_all_redirection_entries() const;

    void mask_all_redirection_entries() const;
    void mask_redirection_entry(u8 index) const;
    void unmask_redirection_entry(u8 index) const;
    bool is_redirection_entry_masked(u8 index) const;

    u8 read_redirection_entry_vector(u8 index) const;
    Optional<int> find_redirection_entry_by_vector(u8 vector) const;
    void configure_redirections() const;

    void write_register(u32 index, u32 value) const;
    u32 read_register(u32 index) const;

    virtual void initialize() override;
    void map_isa_interrupts();
    void map_pci_interrupts();
    void isa_identity_map(int index);

    PhysicalAddress m_address;
    u32 m_gsi_base;
    u8 m_id;
    u8 m_version;
    size_t m_redirection_entries_count;
};
}
