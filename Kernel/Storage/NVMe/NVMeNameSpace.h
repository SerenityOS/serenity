/*
 * Copyright (c) 2021, Pankaj R <pankydev8@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AK/kmalloc.h"
#include <AK/OwnPtr.h>
#include <AK/Types.h>
#include <Kernel/Library/LockRefPtr.h>
#include <Kernel/Library/NonnullLockRefPtr.h>
#include <Kernel/Library/NonnullLockRefPtrVector.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Storage/NVMe/NVMeDefinitions.h>
#include <Kernel/Storage/NVMe/NVMeQueue.h>
#include <Kernel/Storage/StorageDevice.h>

namespace Kernel {

class NVMeController;
class NVMeNameSpace : public StorageDevice {
    friend class DeviceManagement;

public:
    static ErrorOr<NonnullLockRefPtr<NVMeNameSpace>> try_create(NVMeController const&, NonnullLockRefPtrVector<NVMeQueue> queues, u8 controller_id, u16 nsid, size_t storage_size, size_t lba_size);

    CommandSet command_set() const override { return CommandSet::NVMe; };
    void start_request(AsyncBlockDeviceRequest& request) override;

private:
    NVMeNameSpace(LUNAddress, NonnullLockRefPtrVector<NVMeQueue> queues, size_t storage_size, size_t lba_size, size_t major_number, size_t minor_number, u16 nsid, NonnullOwnPtr<KString> early_device_name);

    u16 m_nsid;
    NonnullLockRefPtrVector<NVMeQueue> m_queues;
};

}
