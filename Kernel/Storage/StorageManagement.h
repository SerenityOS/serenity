/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 *
 * SPDX-License-Identifier: BSD-2-Clause
 */

#pragma once

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
    AK_MAKE_ETERNAL;

public:
    StorageManagement(String boot_argument, bool force_pio);
    static bool initialized();
    static void initialize(String boot_argument, bool force_pio);
    static StorageManagement& the();

    NonnullRefPtr<FileSystem> root_filesystem() const;

    static int major_number();
    static int minor_number();

private:
    bool boot_argument_contains_partition_uuid();

    NonnullRefPtrVector<StorageController> enumerate_controllers(bool force_pio) const;
    NonnullRefPtrVector<StorageDevice> enumerate_storage_devices() const;
    NonnullRefPtrVector<DiskPartition> enumerate_disk_partitions() const;

    void determine_boot_device();
    void determine_boot_device_with_partition_uuid();

    OwnPtr<PartitionTable> try_to_initialize_partition_table(StorageDevice const&) const;

    RefPtr<BlockDevice> boot_block_device() const;

    String m_boot_argument;
    RefPtr<BlockDevice> m_boot_block_device { nullptr };
    NonnullRefPtrVector<StorageController> m_controllers;
    NonnullRefPtrVector<StorageDevice> m_storage_devices;
    NonnullRefPtrVector<DiskPartition> m_disk_partitions;
};

}
