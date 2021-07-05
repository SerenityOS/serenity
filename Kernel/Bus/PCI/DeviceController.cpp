/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/DeviceController.h>

namespace Kernel {
namespace PCI {

DeviceController::DeviceController(Address address)
    : m_pci_address(address)
{
}

bool DeviceController::is_msi_capable() const
{
    for (const auto& capability : PCI::get_physical_id(pci_address()).capabilities()) {
        if (capability.id() == PCI_CAPABILITY_MSI)
            return true;
    }
    return false;
}
bool DeviceController::is_msix_capable() const
{
    for (const auto& capability : PCI::get_physical_id(pci_address()).capabilities()) {
        if (capability.id() == PCI_CAPABILITY_MSIX)
            return true;
    }
    return false;
}

void DeviceController::enable_pin_based_interrupts() const
{
    PCI::enable_interrupt_line(pci_address());
}
void DeviceController::disable_pin_based_interrupts() const
{
    PCI::disable_interrupt_line(pci_address());
}

void DeviceController::enable_message_signalled_interrupts()
{
    TODO();
}
void DeviceController::disable_message_signalled_interrupts()
{
    TODO();
}
void DeviceController::enable_extended_message_signalled_interrupts()
{
    TODO();
}
void DeviceController::disable_extended_message_signalled_interrupts()
{
    TODO();
}

}
}
