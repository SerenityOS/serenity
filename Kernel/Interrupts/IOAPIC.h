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

class ISAInterruptOverrideMetadata;
class PCIInterruptOverrideMetadata;

class IOAPIC final : public IRQController {
public:
    IOAPIC(ioapic_mmio_regs& regs, u32 gsi_base, Vector<RefPtr<ISAInterruptOverrideMetadata>>& overrides, Vector<RefPtr<PCIInterruptOverrideMetadata>>& pci_overrides);
    virtual void enable(u8 number) override;
    virtual void disable(u8 number) override;
    virtual void hard_disable() override;
    virtual void eoi(u8 number) const override;
    virtual bool is_vector_enabled(u8 number) const override;
    virtual u16 get_isr() const override;
    virtual u16 get_irr() const override;
    virtual u32 get_gsi_base() const override { return m_gsi_base; }
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
    int find_redirection_entry_by_vector(u8 vector) const;
    void configure_redirections() const;

    void write_register(u32 index, u32 value) const;
    u32 read_register(u32 index) const;

    virtual void initialize() override;
    void map_isa_interrupts();
    void map_pci_interrupts();
    void isa_identity_map(int index);

    ioapic_mmio_regs& m_physical_access_registers;
    u32 m_gsi_base;
    u8 m_id;
    u8 m_version;
    u32 m_redirection_entries;
    Vector<RefPtr<ISAInterruptOverrideMetadata>> m_isa_interrupt_overrides;
    Vector<RefPtr<PCIInterruptOverrideMetadata>> m_pci_interrupt_overrides;
};
}
