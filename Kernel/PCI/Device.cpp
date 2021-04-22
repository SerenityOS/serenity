/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/PCI/Device.h>

namespace Kernel {
namespace PCI {

Device::Device(Address address)
    : IRQHandler(get_interrupt_line(address))
    , m_pci_address(address)
{
    // FIXME: Register PCI device somewhere...
}

Device::Device(Address address, u8 interrupt_vector)
    : IRQHandler(interrupt_vector)
    , m_pci_address(address)
{
    // FIXME: Register PCI device somewhere...
}

Device::~Device()
{
    // FIXME: Unregister the device
}

}
}
