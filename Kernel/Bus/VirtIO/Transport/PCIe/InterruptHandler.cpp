/*
 * Copyright (c) 2021-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/VirtIO/Transport/PCIe/InterruptHandler.h>

namespace Kernel::VirtIO {

ErrorOr<NonnullOwnPtr<PCIeTransportInterruptHandler>> PCIeTransportInterruptHandler::create(PCIeTransportLink& transport_link, VirtIO::Device& parent_device, u8 irq)
{
    return TRY(adopt_nonnull_own_or_enomem(new (nothrow) PCIeTransportInterruptHandler(transport_link, parent_device, irq)));
}

PCIeTransportInterruptHandler::PCIeTransportInterruptHandler(PCIeTransportLink& transport_link, VirtIO::Device& parent_device, u8 irq)
    : TransportInterruptHandler(parent_device)
    , PCI::IRQHandler(transport_link, irq)
{
}

bool PCIeTransportInterruptHandler::handle_irq()
{
    return notify_parent_device_on_interrupt();
}

}
