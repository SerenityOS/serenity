/*
 * Copyright (c) 2023, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Bus/PCI/IDs.h>
#include <Kernel/Devices/CharacterDevice.h>
#include <Kernel/Devices/SerialDevice.h>
#include <Kernel/Library/Driver.h>

namespace Kernel {

class PCISerial8250Device
    : public AtomicRefCounted<PCISerial8250Device> {
    KERNEL_MAKE_DRIVER_LISTABLE(PCISerial8250Device)

public:
    virtual ~PCISerial8250Device() = default;
    static ErrorOr<NonnullRefPtr<PCISerial8250Device>> create(Vector<NonnullRefPtr<SerialDevice>>);

private:
    explicit PCISerial8250Device(Vector<NonnullRefPtr<SerialDevice>>);
    Vector<NonnullRefPtr<SerialDevice>> const m_attached_devices;
};

}
