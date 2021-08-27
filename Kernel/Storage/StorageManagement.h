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
    AK_MAKE_ETERNAL;

public:
    StorageManagement();
    static bool initialized();
    static void enumerate_disk_partitions_on_new_device(StorageDevice&);
    static OwnPtr<PartitionTable> try_to_initialize_partition_table(const StorageDevice&);

    void initialize(String boot_argument, bool force_pio);
    static StorageManagement& the();

    NonnullRefPtr<FileSystem> root_filesystem() const;

    static int major_number();
    static int minor_number();

private:
    bool boot_argument_contains_partition_uuid();

    void enumerate_controllers(bool force_pio);
    void enumerate_storage_devices();

    void determine_boot_device();
    void determine_boot_device_with_partition_uuid();

    void determine_boot_device_with_defined_prefix(StringView prefix);

    RefPtr<BlockDevice> boot_block_device() const;

    String m_boot_argument;
    WeakPtr<BlockDevice> m_boot_block_device;
    NonnullRefPtrVector<StorageController> m_controllers;
};

}
