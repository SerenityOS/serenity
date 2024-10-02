/*
 * Copyright (c) 2023, Leon Albrecht <leon.a@serenityos.org>
 * Copyright (c) 2024, Sönke Holz <sholz8530@gmail.com>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <Kernel/Devices/Storage/StorageDevice.h>

namespace Kernel::USB {

class BulkSCSIInterface;

class BulkSCSIStorageDevice : public StorageDevice {
    friend class Device;

public:
    BulkSCSIStorageDevice(BulkSCSIInterface&, LUNAddress logical_unit_number_address, u32 hardware_relative_controller_id, size_t sector_size, u64 max_addressable_block);

private:
    BulkSCSIInterface& m_interface;

    virtual void start_request(AsyncBlockDeviceRequest&) override;
    virtual CommandSet command_set() const override { return CommandSet::SCSI; }

    u32 optimal_block_count(u32 blocks);

    ErrorOr<void> do_read(u32 block_index, u32 block_count, UserOrKernelBuffer& buffer, size_t buffer_size);
    ErrorOr<void> do_write(u32 block_index, u32 block_count, UserOrKernelBuffer& buffer, size_t buffer_size);

    ErrorOr<void> query_characteristics();

    Optional<u16> m_optimal_transfer_length;
    Optional<u32> m_optimal_transfer_length_granularity;
    Optional<u32> m_maximum_transfer_length;

    IntrusiveListNode<BulkSCSIStorageDevice, NonnullLockRefPtr<BulkSCSIStorageDevice>> m_list_node;

public:
    using List = IntrusiveList<&BulkSCSIStorageDevice::m_list_node>;
};

}
