/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

#include <AK/IntrusiveList.h>
#include <AK/NonnullRefPtr.h>
#include <AK/NonnullRefPtrVector.h>
#include <AK/Types.h>
#include <Kernel/FileSystem/FileSystem.h>
#include <Kernel/Storage/Partition/DiskPartition.h>
#include <Kernel/Storage/StorageController.h>
#include <Kernel/Storage/StorageDevice.h>

namespace Kernel {

class PartitionTable;
class StorageManagement {

public:
    StorageManagement();
    static bool initialized();
    void initialize(StringView boot_argument, bool force_pio, bool nvme_poll);
    static StorageManagement& the();

    NonnullRefPtr<FileSystem> root_filesystem() const;

    static MajorNumber storage_type_major_number();
    static MinorNumber generate_storage_minor_number();

    void remove_device(StorageDevice&);

private:
    bool boot_argument_contains_partition_uuid();

    void enumerate_controllers(bool force_pio, bool nvme_poll);
    void enumerate_storage_devices();
    void enumerate_disk_partitions();

    void determine_boot_device();
    void determine_boot_device_with_partition_uuid();

    void dump_storage_devices_and_partitions() const;

    OwnPtr<PartitionTable> try_to_initialize_partition_table(const StorageDevice&) const;

    RefPtr<BlockDevice> boot_block_device() const;

    StringView m_boot_argument;
    WeakPtr<BlockDevice> m_boot_block_device;
    NonnullRefPtrVector<StorageController> m_controllers;
    IntrusiveList<&StorageDevice::m_list_node> m_storage_devices;
};

}
