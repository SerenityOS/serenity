/*
 * Copyright (c) 2020, Liav A. <liavalb@hotmail.co.il>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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

    NonnullRefPtr<FS> root_filesystem() const;

    static int major_number();
    static int minor_number();

    NonnullRefPtrVector<StorageController> ide_controllers() const;

private:
    bool boot_argument_contains_partition_uuid();

    NonnullRefPtrVector<StorageController> enumerate_controllers(bool force_pio) const;
    NonnullRefPtrVector<StorageDevice> enumerate_storage_devices() const;
    NonnullRefPtrVector<DiskPartition> enumerate_disk_partitions() const;

    void determine_boot_device();
    void determine_boot_device_with_partition_uuid();

    OwnPtr<PartitionTable> try_to_initialize_partition_table(const StorageDevice&) const;

    RefPtr<BlockDevice> boot_block_device() const;

    String m_boot_argument;
    RefPtr<BlockDevice> m_boot_block_device { nullptr };
    NonnullRefPtrVector<StorageController> m_controllers;
    NonnullRefPtrVector<StorageDevice> m_storage_devices;
    NonnullRefPtrVector<DiskPartition> m_disk_partitions;
};

}
