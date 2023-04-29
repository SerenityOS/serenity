/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/AnyOf.h>
#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Device.h>

namespace Kernel::PCI {

Device::Device(DeviceIdentifier const& pci_identifier)
    : m_pci_identifier(pci_identifier)
{
    m_pci_identifier->initialize();
}

bool Device::is_msi_capable() const
{
    return AK::any_of(
        m_pci_identifier->capabilities(),
        [](auto const& capability) { return capability.id().value() == PCI::Capabilities::ID::MSI; });
}
bool Device::is_msix_capable() const
{
    return m_pci_identifier->is_msix_capable();
}

void Device::enable_pin_based_interrupts() const
{
    PCI::enable_interrupt_line(m_pci_identifier);
}
void Device::disable_pin_based_interrupts() const
{
    PCI::disable_interrupt_line(m_pci_identifier);
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
    for (auto& capability : m_pci_identifier->capabilities()) {
        if (capability.id().value() == PCI::Capabilities::ID::MSIX)
            capability.write16(msi_control_offset, capability.read16(msi_control_offset) | msix_control_enable);
    }
}

void Device::disable_extended_message_signalled_interrupts()
{
    for (auto& capability : m_pci_identifier->capabilities()) {
        if (capability.id().value() == PCI::Capabilities::ID::MSIX)
            capability.write16(msi_control_offset, capability.read16(msi_control_offset) & ~(msix_control_enable));
    }
}

}
