/*
 * Copyright (c) 2021, Pankaj R <pankydev8@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#include <AK/NonnullOwnPtr.h>
#include <Kernel/Devices/Device.h>
#include <Kernel/Devices/Storage/NVMe/NVMeController.h>
#include <Kernel/Devices/Storage/NVMe/NVMeNameSpace.h>
#include <Kernel/Devices/Storage/StorageManagement.h>

namespace Kernel {

UNMAP_AFTER_INIT ErrorOr<NonnullRefPtr<NVMeNameSpace>> NVMeNameSpace::create(NVMeController const& controller, Vector<NonnullLockRefPtr<NVMeQueue>> queues, u16 nsid, size_t storage_size, size_t lba_size)
{
    auto device = TRY(Device::try_create_device<NVMeNameSpace>(StorageDevice::LUNAddress { controller.controller_id(), nsid, 0 }, controller.hardware_relative_controller_id(), move(queues), storage_size, lba_size, nsid));
    return device;
}

UNMAP_AFTER_INIT NVMeNameSpace::NVMeNameSpace(LUNAddress logical_unit_number_address, u32 hardware_relative_controller_id, Vector<NonnullLockRefPtr<NVMeQueue>> queues, size_t max_addresable_block, size_t lba_size, u16 nsid)
    : StorageDevice(logical_unit_number_address, hardware_relative_controller_id, lba_size, max_addresable_block)
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
        queue->read(request, m_nsid, request.block_index(), request.block_count());
    } else {
        queue->write(request, m_nsid, request.block_index(), request.block_count());
    }
}
}
