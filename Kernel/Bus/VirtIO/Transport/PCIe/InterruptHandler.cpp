/*
 * Copyright (c) 2021-2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/VirtIO/Transport/PCIe/InterruptHandler.h>

namespace Kernel::VirtIO {

ErrorOr<NonnullOwnPtr<PCIeTransportInterruptHandler>> PCIeTransportInterruptHandler::create(PCI::Device& pci_device, VirtIO::Device& parent_device, u8 irq)
{
    return TRY(adopt_nonnull_own_or_enomem(new (nothrow) PCIeTransportInterruptHandler(pci_device, parent_device, irq)));
}

PCIeTransportInterruptHandler::PCIeTransportInterruptHandler(PCI::Device& pci_device, VirtIO::Device& parent_device, u8 irq)
    : TransportInterruptHandler(parent_device)
    , PCI::IRQHandler(pci_device, irq)
{
}

bool PCIeTransportInterruptHandler::handle_irq(RegisterState const&)
{
    return notify_parent_device_on_interrupt();
}

}
