/*
 * Copyright (c) 2021, Pankaj R <pankydev8@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <AK/kmalloc.h>
#include <Kernel/Devices/Storage/NVMe/NVMeDefinitions.h>
#include <Kernel/Devices/Storage/NVMe/NVMeQueue.h>
#include <Kernel/Devices/Storage/StorageDevice.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Library/NonnullLockRefPtr.h>
#include <Kernel/Locking/Spinlock.h>

namespace Kernel {

class NVMeController;
class NVMeNameSpace : public StorageDevice {
    friend class Device;

public:
    static ErrorOr<NonnullRefPtr<NVMeNameSpace>> create(NVMeController const&, Vector<NonnullLockRefPtr<NVMeQueue>> queues, u16 nsid, size_t storage_size, size_t lba_size);

    CommandSet command_set() const override { return CommandSet::NVMe; }
    void start_request(AsyncBlockDeviceRequest& request) override;

private:
    NVMeNameSpace(LUNAddress, u32 hardware_relative_controller_id, Vector<NonnullLockRefPtr<NVMeQueue>> queues, size_t storage_size, size_t lba_size, u16 nsid);

    u16 m_nsid;
    Vector<NonnullLockRefPtr<NVMeQueue>> m_queues;
};

}
