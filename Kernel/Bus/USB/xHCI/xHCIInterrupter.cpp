/*
 * Copyright (c) 2024, Idan Horowitz <idan.horowitz@serenityos.org>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/USB/xHCI/xHCIInterrupter.h>

namespace Kernel::USB {

ErrorOr<NonnullOwnPtr<xHCIInterrupter>> xHCIInterrupter::create(xHCIController& controller, u16 interrupter_id)
{
    auto irq = TRY(controller.allocate_irq(0));
    return TRY(adopt_nonnull_own_or_enomem(new (nothrow) xHCIInterrupter(controller, interrupter_id, irq)));
}

xHCIInterrupter::xHCIInterrupter(xHCIController& controller, u16 interrupter_id, u16 irq)
    : PCI::IRQHandler(controller, irq)
    , m_controller(controller)
    , m_interrupter_id(interrupter_id)
{
    enable_irq();
}

bool xHCIInterrupter::handle_irq()
{
    m_controller.handle_interrupt({}, m_interrupter_id);
    return true;
}

}
