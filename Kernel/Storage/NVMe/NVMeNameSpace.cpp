/*
 * Copyright (c) 2021, Pankaj R <pankydev8@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include "NVMeNameSpace.h"
#include <AK/NonnullOwnPtr.h>
#include <Kernel/Devices/DeviceManagement.h>
#include <Kernel/Storage/StorageManagement.h>

namespace Kernel {

UNMAP_AFTER_INIT ErrorOr<NonnullRefPtr<NVMeNameSpace>> NVMeNameSpace::try_create(NonnullRefPtrVector<NVMeQueue> queues, u8 controller_id, u16 nsid, size_t storage_size, size_t lba_size)
{
    auto device_id = StorageManagement::generate_storage_device_id();
    auto device_name_kstring = TRY(KString::formatted("nvme{:d}n{:d}", controller_id, nsid));
    return TRY(DeviceManagement::try_create_device<NVMeNameSpace>(queues, storage_size, lba_size, device_id, nsid, move(device_name_kstring)));
}

UNMAP_AFTER_INIT NVMeNameSpace::NVMeNameSpace(NonnullRefPtrVector<NVMeQueue> queues, size_t max_addresable_block, size_t lba_size, DeviceID device_id, u16 nsid, NonnullOwnPtr<KString> dev_name)
    : StorageDevice(device_id, lba_size, max_addresable_block, move(dev_name))
    , m_nsid(nsid)
    , m_queues(move(queues))
{
}

void NVMeNameSpace::start_request(AsyncBlockDeviceRequest& request)
{
    auto index = Processor::current_id();
    auto& queue = m_queues.at(index);
    // TODO: For now we support only IO transfers of size PAGE_SIZE (Going along with the current constraint in the block layer)
    // Eventually remove this constraint by using the PRP2 field in the submission struct and remove block layer constraint for NVMe driver.
    VERIFY(request.block_count() <= (PAGE_SIZE / block_size()));

    if (request.request_type() == AsyncBlockDeviceRequest::Read) {
        queue.read(request, m_nsid, request.block_index(), request.block_count());
    } else {
        queue.write(request, m_nsid, request.block_index(), request.block_count());
    }
}
}
