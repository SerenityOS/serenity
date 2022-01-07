/*
 * Copyright (c) 2021, Pankaj R <pankydev8@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include "AK/kmalloc.h"
#include <AK/NonnullRefPtr.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/OwnPtr.h>
#include <AK/RefCounted.h>
#include <AK/RefPtr.h>
#include <AK/Types.h>
#include <Kernel/Locking/Spinlock.h>
#include <Kernel/Storage/NVMe/NVMeDefinitions.h>
#include <Kernel/Storage/NVMe/NVMeQueue.h>
#include <Kernel/Storage/StorageDevice.h>

namespace Kernel {
class NVMeNameSpace : public StorageDevice {

public:
    static ErrorOr<NonnullRefPtr<NVMeNameSpace>> try_create(NonnullRefPtrVector<NVMeQueue> queues, u8 controller_id, u16 nsid, size_t storage_size, size_t lba_size);
    explicit NVMeNameSpace(NonnullRefPtrVector<NVMeQueue> queues, size_t storage_size, size_t lba_size, size_t major_number, size_t minor_number, u16 nsid, NonnullOwnPtr<KString> early_device_name);

    CommandSet command_set() const override { return CommandSet::NVMe; };
    void start_request(AsyncBlockDeviceRequest& request) override;

private:
    u16 m_nsid;
    NonnullRefPtrVector<NVMeQueue> m_queues;
};

}
