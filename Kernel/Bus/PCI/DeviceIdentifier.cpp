/*
 * Copyright (c) 2022, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Bus/PCI/Definitions.h>

namespace Kernel::PCI {

RefPtr<PCIDeviceSysFSDirectory> DeviceIdentifier::sysfs_pci_device_directory() const
{
    return m_pci_device_directory;
}

void DeviceIdentifier::set_sysfs_pci_device_directory(Badge<PCIBusSysFSDirectory>, PCIDeviceSysFSDirectory const& pci_device_directory)
{
    m_pci_device_directory = pci_device_directory;
}

}
