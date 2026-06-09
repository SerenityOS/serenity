/*
 * Copyright (c) 2024, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtras.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Bus/USB/xHCI/DeviceTreexHCIController.h>
#include <Kernel/Bus/USB/xHCI/PCIxHCIController.h>
#include <Kernel/Interrupts/IRQHandler.h>
#include <Kernel/Interrupts/PCIIRQHandler.h>

namespace Kernel::USB::xHCI {

class xHCIPCIInterrupter final
    : public xHCIInterrupter
    , public PCI::IRQHandler {
public:
    static ErrorOr<NonnullOwnPtr<xHCIPCIInterrupter>> create(PCIxHCIController&, u16 interrupter_id);

    virtual StringView purpose() const override { return "xHCI Interrupter"sv; }

private:
    xHCIPCIInterrupter(PCIxHCIController& controller, u16 interrupter_id, InterruptNumber irq);

    virtual bool handle_irq() override;
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
