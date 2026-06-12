/*
 * Copyright (c) 2025-2026, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/Forward.h>
#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/Interrupts/PCIIRQHandler.h>
#include <Kernel/Memory/TypedMapping.h>

namespace Kernel::RPi {

// 6.1. PCIe endpoint configuration registers
struct RP1PCIeEndpointRegisters {
    u32 dbi;
    u32 control;

    enum class MSIXConfiguration : u32 {
        Enable = 1 << 0,
        InterruptAcknowledge = 1 << 2,
        EnableInterruptAcknowledge = 1 << 3,
    };
    MSIXConfiguration msix_configuration[64];

    u32 interrupt_status_low;
    u32 interrupt_status_high;
    u32 phy_test;
    u32 phy_param_ctrl0;
    u32 phy_param_ctrl1;
    u32 phy_cr_acc_ctrl;
    u32 phy_cr_acc_data;
    u32 ltssm_state_new;
    u32 _;
    u32 ltssm_state_filter[4];
    u32 _;
    u32 axi_lookup[16];
    u32 vdm_config_reg_0;
    u32 vdm_config_reg_1;
    u32 vdm_panic_reg;
    u32 vdm_test;
    u32 vdm_header;
    u32 pm_control;
    u32 pm_status;
    u32 monitor_0;
    u32 monitor_1;
    u32 monitor_2;
    u32 raw_interrupts;
    u32 interrupt_enable;
    u32 interrupt_force;
    u32 interrupt_status;
};
static_assert(AssertSize<RP1PCIeEndpointRegisters, 0x1b8>());

AK_ENUM_BITWISE_OPERATORS(RP1PCIeEndpointRegisters::MSIXConfiguration)

class RP1 final
    : public PCI::Device
    , public PCI::IRQHandler
    , public AtomicRefCounted<RP1> {
public:
    static ErrorOr<NonnullRefPtr<RP1>> create(PCI::DeviceIdentifier const& pci_identifier);

    ErrorOr<void> initialize();

    // ^PCI::Device
    virtual StringView device_name() const override { return "RP1"sv; }

    // ^PCI::IRQHandler
    virtual StringView purpose() const override { return "RP1 IRQ Handler"sv; }

    using InterruptHandler = Function<bool(InterruptNumber)>;

    enum class InterruptTriggerMode {
        Level,
        Edge,
    };
    void register_interrupt_handler(InterruptNumber, InterruptTriggerMode, InterruptHandler handler);

private:
    RP1(PCI::DeviceIdentifier const&);

    // ^PCI::IRQHandler
    virtual bool handle_irq() override;

    Memory::TypedMapping<RP1PCIeEndpointRegisters volatile> m_pcie_ep_registers;

    Array<InterruptHandler, 64> m_interrupt_handlers {};
    u64 m_interrupt_level_triggered { 0 };
};

}
