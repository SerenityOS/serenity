/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <Kernel/Devices/Serial/PCI/Serial8250Device.h>
#include <Kernel/Library/IOWindow.h>
#include <Kernel/Sections.h>

namespace Kernel {

ErrorOr<NonnullRefPtr<PCISerial8250Device>> PCISerial8250Device::create(Vector<NonnullRefPtr<SerialDevice>> devices)
{
    return TRY(adopt_nonnull_ref_or_enomem(new (nothrow) PCISerial8250Device(move(devices))));
}

PCISerial8250Device::PCISerial8250Device(Vector<NonnullRefPtr<SerialDevice>> devices)
    : m_attached_devices(move(devices))
{
}

}
