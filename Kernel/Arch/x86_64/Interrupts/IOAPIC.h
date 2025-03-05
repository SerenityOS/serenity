/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Arch/x86_64/IRQController.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel {
struct [[gnu::packed]] ioapic_mmio_regs {
    u32 volatile select;
    u32 reserved[3];
    u32 volatile window;
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
    u8 const m_bus_id;
    u8 const m_polarity;
    u8 const m_trigger_mode;
    u8 const m_pci_interrupt_pin;
    u8 const m_pci_device_number;
    u32 const m_ioapic_id;
    u16 const m_ioapic_interrupt_pin;
};

class IOAPIC final : public IRQController {
public:
    IOAPIC(PhysicalAddress, u32 gsi_base);
    virtual void enable(GenericInterruptHandler const&) override;
    virtual void disable(GenericInterruptHandler const&) override;
    virtual void hard_disable() override;
    virtual void eoi(GenericInterruptHandler const&) const override;
    virtual void spurious_eoi(GenericInterruptHandler const&) const override;
    virtual bool is_vector_enabled(u8 number) const override;
    virtual bool is_enabled() const override;
    virtual u16 get_isr() const override;
    virtual u16 get_irr() const override;
    virtual u32 gsi_base() const override { return m_gsi_base; }
    virtual size_t interrupt_vectors_count() const override { return m_redirection_entries_count; }
    virtual StringView model() const override { return "IOAPIC"sv; }
    virtual IRQControllerType type() const override { return IRQControllerType::i82093AA; }

private:
    void configure_redirection_entry(size_t index, u8 interrupt_vector, u8 delivery_mode, bool logical_destination, bool active_low, bool trigger_level_mode, bool masked, u8 destination) const;
    void reset_redirection_entry(size_t index) const;
    void map_interrupt_redirection(u8 interrupt_vector);
    void reset_all_redirection_entries() const;

    void mask_all_redirection_entries() const;
    void mask_redirection_entry(u8 index) const;
    void unmask_redirection_entry(u8 index) const;
    bool is_redirection_entry_masked(u8 index) const;

    u8 read_redirection_entry_vector(u8 index) const;
    Optional<int> find_redirection_entry_by_vector(u8 vector) const;

    void write_register(u32 index, u32 value) const;
    u32 read_register(u32 index) const;

    virtual void initialize() override;
    void isa_identity_map(size_t index);

    PhysicalAddress m_address;
    mutable Memory::TypedMapping<ioapic_mmio_regs> m_regs;
    u32 m_gsi_base;
    u8 m_id;
    u8 m_version;
    size_t m_redirection_entries_count;
};
}
