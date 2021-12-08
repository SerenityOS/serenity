/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/AnyOf.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Device.h>

namespace Kernel {
namespace PCI {

Device::Device(Address address)
    : m_pci_address(address)
{
}

bool Device::is_msi_capable() const
{
    return AK::any_of(
        PCI::get_device_identifier(pci_address()).capabilities(),
        [](auto const& capability) { return capability.id().value() == PCI::Capabilities::ID::MSI; });
}
bool Device::is_msix_capable() const
{
    return AK::any_of(
        PCI::get_device_identifier(pci_address()).capabilities(),
        [](auto const& capability) { return capability.id().value() == PCI::Capabilities::ID::MSIX; });
}

void Device::enable_pin_based_interrupts() const
{
    PCI::enable_interrupt_line(pci_address());
}
void Device::disable_pin_based_interrupts() const
{
    PCI::disable_interrupt_line(pci_address());
}

void Device::enable_message_signalled_interrupts()
{
    TODO();
}
void Device::disable_message_signalled_interrupts()
{
    TODO();
}
void Device::enable_extended_message_signalled_interrupts()
{
    TODO();
}
void Device::disable_extended_message_signalled_interrupts()
{
    TODO();
}

}
}
