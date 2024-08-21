/*
 * Copyright (c) 2024, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/StdLibExtras.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Bus/USB/xHCI/xHCIController.h>
#include <Kernel/Interrupts/PCIIRQHandler.h>

namespace Kernel::USB {

class xHCIInterrupter final : public PCI::IRQHandler {
public:
    static ErrorOr<NonnullOwnPtr<xHCIInterrupter>> create(xHCIController&, u16 interrupter_id);

    virtual StringView purpose() const override { return "xHCI Interrupter"sv; }

private:
    xHCIInterrupter(xHCIController& controller, u16 interrupter_id, u16 irq);

    virtual bool handle_irq() override;

    xHCIController& m_controller;
    u16 m_interrupter_id { 0 };
};

}
