/*
 * Copyright (c) 2025, Sönke Holz <soenke.holz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/USB/xHCI/xHCIController.h>
#include <Kernel/Firmware/DeviceTree/Device.h>
#include <Kernel/Interrupts/IRQHandler.h>

namespace Kernel::USB::xHCI {

class DeviceTreexHCIController final : public xHCIController {
public:
    static ErrorOr<NonnullLockRefPtr<DeviceTreexHCIController>> try_to_initialize(DeviceTree::Device::Resource, StringView node_name, InterruptNumber interrupt_number);

private:
    DeviceTreexHCIController(Memory::TypedMapping<u8> registers_mapping, StringView node_name, InterruptNumber interrupt_number);

    // ^xHCIController
    virtual bool using_message_signalled_interrupts() const override { return m_using_message_signalled_interrupts; }
    virtual ErrorOr<NonnullOwnPtr<xHCIInterrupter>> create_interrupter(u16 interrupter_id) override;
    virtual ErrorOr<void> write_dmesgln_prefix(StringBuilder& builder) const override
    {
        TRY(builder.try_appendff("xHCI: {}: "sv, m_node_name));
        return {};
    }

    StringView m_node_name;
    InterruptNumber m_interrupt_number { 0 };
    bool m_using_message_signalled_interrupts { false };
};

class xHCIDeviceTreeInterrupter final
    : public xHCIInterrupter
    , public IRQHandler {
public:
    static ErrorOr<NonnullOwnPtr<xHCIDeviceTreeInterrupter>> create(DeviceTreexHCIController&, InterruptNumber irq, u16 interrupter_id);

    virtual StringView purpose() const override { return "xHCI Interrupter"sv; }

private:
    xHCIDeviceTreeInterrupter(DeviceTreexHCIController& controller, u16 interrupter_id, InterruptNumber irq);

    virtual bool handle_irq() override;
};

}
