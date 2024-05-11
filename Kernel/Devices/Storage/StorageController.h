/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <Kernel/Bus/PCI/Access.h>
#include <Kernel/Bus/PCI/Device.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Locking/Mutex.h>
#include <Kernel/Memory/PhysicalAddress.h>
#include <Kernel/Memory/PhysicalRAMPage.h>
#include <Kernel/Security/Random.h>
#include <Kernel/Tasks/WaitQueue.h>

namespace Kernel {

class AsyncBlockDeviceRequest;
class StorageDevice;
class StorageController : public AtomicRefCounted<StorageController> {

public:
    virtual ~StorageController() = default;

    virtual LockRefPtr<StorageDevice> device(u32 index) const = 0;
    virtual size_t devices_count() const = 0;

    u32 controller_id() const { return m_controller_id; }
    u32 hardware_relative_controller_id() const { return m_hardware_relative_controller_id; }

protected:
    virtual void complete_current_request(AsyncDeviceRequest::RequestResult) = 0;

    explicit StorageController(u32 hardware_relative_controller_id);

private:
    u32 const m_controller_id { 0 };

    u32 const m_hardware_relative_controller_id { 0 };
};
}
