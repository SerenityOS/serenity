/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/VirtIO/Device.h>
#include <Kernel/Bus/VirtIO/Transport/InterruptHandler.h>
#include <Kernel/Bus/VirtIO/Transport/PCIe/TransportLink.h>
#include <Kernel/Interrupts/PCIIRQHandler.h>

namespace Kernel::VirtIO {

class PCIeTransportInterruptHandler final
    : public TransportInterruptHandler
    , public PCI::IRQHandler {
public:
    static ErrorOr<NonnullOwnPtr<PCIeTransportInterruptHandler>> create(PCIeTransportLink&, VirtIO::Device&, u8 irq);
    virtual ~PCIeTransportInterruptHandler() override = default;

    virtual StringView purpose() const override { return "VirtIO PCI IRQ Handler"sv; }

private:
    PCIeTransportInterruptHandler(PCIeTransportLink&, VirtIO::Device&, u8 irq);

    //^ IRQHandler
    virtual bool handle_irq() override;
};
}
