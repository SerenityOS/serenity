/*
 * Copyright (c) 2023, Kirill Nikolaev <cyril7@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Bus/PCI/API.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Devices/Storage/StorageController.h>

namespace Kernel {

class VirtIOBlockDevice;

class VirtIOBlockController : public StorageController {
public:
    VirtIOBlockController();

    static bool is_handled(PCI::DeviceIdentifier const& device_identifier);
    ErrorOr<void> add_device(PCI::DeviceIdentifier const& device_identifier);

    // ^StorageController
    virtual LockRefPtr<StorageDevice> device(u32 index) const override;
    virtual size_t devices_count() const override { return m_devices.size(); }

protected:
    virtual void complete_current_request(AsyncDeviceRequest::RequestResult) override;

private:
    Vector<LockRefPtr<VirtIOBlockDevice>> m_devices;
};

}
