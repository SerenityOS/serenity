/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/RefPtr.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/DeviceController.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/IO.h>
#include <Kernel/Mutex.h>
#include <Kernel/PhysicalAddress.h>
#include <Kernel/Random.h>
#include <Kernel/VM/PhysicalPage.h>
#include <Kernel/WaitQueue.h>

namespace Kernel {

class AsyncBlockDeviceRequest;
class StorageDevice;
class StorageController : public RefCounted<StorageController> {
    AK_MAKE_ETERNAL

public:
    virtual ~StorageController() = default;

    virtual RefPtr<StorageDevice> device(u32 index) const = 0;
    virtual size_t devices_count() const = 0;

protected:
    virtual void start_request(const StorageDevice&, AsyncBlockDeviceRequest&) = 0;

protected:
    virtual bool reset() = 0;
    virtual bool shutdown() = 0;

    virtual void complete_current_request(AsyncDeviceRequest::RequestResult) = 0;
};
}
