/*
 * Copyright (c) 2024, Idan Horowitz <idan.horowitz@serenityos.org>
 * Copyright (c) 2025, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/PCI/Definitions.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Bus/USB/xHCI/xHCIController.h>

namespace Kernel::USB {

class PCIxHCIController final
    : public xHCIController
    , public PCI::Device {

public:
    static ErrorOr<NonnullLockRefPtr<PCIxHCIController>> try_to_initialize(PCI::DeviceIdentifier const& pci_device_identifier);

    // ^PCI::Device
    virtual StringView device_name() const override { return "xHCI"sv; }

private:
    PCIxHCIController(PCI::DeviceIdentifier const&, Memory::TypedMapping<u8> registers_mapping);

    // ^xHCIController
    virtual bool using_message_signalled_interrupts() const override { return m_using_message_signalled_interrupts; }
    virtual ErrorOr<NonnullOwnPtr<GenericInterruptHandler>> create_interrupter(u16 interrupter_id) override;
    virtual ErrorOr<void> write_dmesgln_prefix(StringBuilder& builder) const override
    {
        TRY(builder.try_appendff("{}: {}: "sv, device_name(), device_identifier().address()));
        return {};
    }

    void intel_quirk_enable_xhci_ports();

    static constexpr PCI::RegisterOffset intel_xhci_usb2_port_routing_offset = static_cast<PCI::RegisterOffset>(0xD0);
    static constexpr PCI::RegisterOffset intel_xhci_usb2_port_routing_mask_offset = static_cast<PCI::RegisterOffset>(0xD4);
    static constexpr PCI::RegisterOffset intel_xhci_usb3_port_super_speed_enable_offset = static_cast<PCI::RegisterOffset>(0xD8);
    static constexpr PCI::RegisterOffset intel_xhci_usb3_port_routing_mask_offset = static_cast<PCI::RegisterOffset>(0xDC);

    bool m_using_message_signalled_interrupts { false };
};

}
