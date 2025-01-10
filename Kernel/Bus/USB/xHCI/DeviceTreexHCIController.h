/*
 * Copyright (c) 2025, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/USB/xHCI/xHCIController.h>
#include <Kernel/Firmware/DeviceTree/Device.h>

namespace Kernel::USB {

class DeviceTreexHCIController final : public xHCIController {
public:
    static ErrorOr<NonnullLockRefPtr<DeviceTreexHCIController>> try_to_initialize(DeviceTree::Device::Resource, StringView node_name, size_t interrupt_number);

private:
    DeviceTreexHCIController(Memory::TypedMapping<u8> registers_mapping, StringView node_name, size_t interrupt_number);

    // ^xHCIController
    virtual bool using_message_signalled_interrupts() const override { return m_using_message_signalled_interrupts; }
    virtual ErrorOr<NonnullOwnPtr<GenericInterruptHandler>> create_interrupter(u16 interrupter_id) override;
    virtual ErrorOr<void> write_dmesgln_prefix(StringBuilder& builder) const override
    {
        TRY(builder.try_appendff("xHCI: {}: "sv, m_node_name));
        return {};
    }

    StringView m_node_name;
    size_t m_interrupt_number { 0 };
    bool m_using_message_signalled_interrupts { false };
};

}
