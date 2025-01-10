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

namespace Kernel::USB {

class xHCIPCIInterrupter final : public PCI::IRQHandler {
public:
    static ErrorOr<NonnullOwnPtr<xHCIPCIInterrupter>> create(PCIxHCIController&, u16 interrupter_id);

    virtual StringView purpose() const override { return "xHCI Interrupter"sv; }

private:
    xHCIPCIInterrupter(PCIxHCIController& controller, u16 interrupter_id, u16 irq);

    virtual bool handle_irq() override;

    PCIxHCIController& m_controller;
    u16 m_interrupter_id { 0 };
};

class xHCIDeviceTreeInterrupter final : public IRQHandler {
public:
    static ErrorOr<NonnullOwnPtr<xHCIDeviceTreeInterrupter>> create(DeviceTreexHCIController&, size_t irq, u16 interrupter_id);

    virtual StringView purpose() const override { return "xHCI Interrupter"sv; }

private:
    xHCIDeviceTreeInterrupter(DeviceTreexHCIController& controller, u16 interrupter_id, size_t irq);

    virtual bool handle_irq() override;

    DeviceTreexHCIController& m_controller;
    u16 m_interrupter_id { 0 };
};

}
