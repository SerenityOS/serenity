/*
 * Copyright (c) 2024, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/USB/xHCI/xHCIInterrupter.h>

namespace Kernel::USB {

ErrorOr<NonnullOwnPtr<xHCIPCIInterrupter>> xHCIPCIInterrupter::create(PCIxHCIController& controller, u16 interrupter_id)
{
    auto irq = TRY(controller.allocate_irq(0));
    return TRY(adopt_nonnull_own_or_enomem(new (nothrow) xHCIPCIInterrupter(controller, interrupter_id, irq)));
}

xHCIPCIInterrupter::xHCIPCIInterrupter(PCIxHCIController& controller, u16 interrupter_id, u16 irq)
    : PCI::IRQHandler(controller, irq)
    , m_controller(controller)
    , m_interrupter_id(interrupter_id)
{
    enable_irq();
}

bool xHCIPCIInterrupter::handle_irq()
{
    m_controller.handle_interrupt(m_interrupter_id);
    return true;
}

ErrorOr<NonnullOwnPtr<xHCIDeviceTreeInterrupter>> xHCIDeviceTreeInterrupter::create(DeviceTreexHCIController& controller, size_t irq, u16 interrupter_id)
{
    return TRY(adopt_nonnull_own_or_enomem(new (nothrow) xHCIDeviceTreeInterrupter(controller, interrupter_id, irq)));
}

xHCIDeviceTreeInterrupter::xHCIDeviceTreeInterrupter(DeviceTreexHCIController& controller, u16 interrupter_id, size_t irq)
    : IRQHandler(irq)
    , m_controller(controller)
    , m_interrupter_id(interrupter_id)
{
    enable_irq();
}

bool xHCIDeviceTreeInterrupter::handle_irq()
{
    m_controller.handle_interrupt(m_interrupter_id);
    return true;
}

}
