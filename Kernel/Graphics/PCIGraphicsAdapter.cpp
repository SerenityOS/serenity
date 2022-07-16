/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Graphics/PCIGraphicsAdapter.h>

namespace Kernel {

PCIGraphicsAdapter::PCIGraphicsAdapter(PCI::DeviceIdentifier const& pci_device_identifier)
    : PCI::Device(pci_device_identifier.address())
{
}

}
